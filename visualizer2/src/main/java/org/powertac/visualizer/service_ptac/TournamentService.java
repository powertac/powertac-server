package org.powertac.visualizer.service_ptac;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.powertac.common.Competition;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.msg.BrokerAccept;
import org.powertac.common.msg.BrokerAuthentication;
import org.powertac.common.msg.VisualizerStatusRequest;
import org.powertac.common.repo.DomainRepo;
import org.powertac.visualizer.config.Constants;
import org.powertac.visualizer.service_ptac.VisualizerService.VisualizerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.xml.parsers.DocumentBuilder;
import jakarta.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@EnableScheduling
@Service
@Configuration
public class TournamentService implements MessageListener {

    static private Logger log = LoggerFactory.getLogger(TournamentService.class.getName());

    @Autowired
    private VisualizerService visualizerService;

    @Autowired
    private ApplicationContext context;

    @Resource(name = "jmsFactory")
    private CachingConnectionFactory connectionFactory;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private MessageDispatcher dispatcher;

    @Autowired
    private XMLMessageConverter converter;

    // Timers, Threads and Runnables that may need to be killed
    private MessageFeeder messageFeeder = null;
    private StateRunner stateRunner = null;

    // event queue
    private BlockingQueue<TournamentEvent> eventQueue = new LinkedBlockingQueue<>();

    // message queue
    private BlockingQueue<Object> messageQueue = new LinkedBlockingQueue<>();

    // visualizer interaction
    private LocalVisualizerProxy proxy;
    private boolean initialized = false;

    // state parameters
    private long maxMsgInterval = 120000L; // 2 min
    private long maxGameReadyInterval = 300000L; // 5 min
    private long gameReadyAt = 0L;
    private long lastMsgTime = 0L;

    // Connection params, set via config if not in embedded mode
    private String mode;
    private String tournamentUrl;
    private String visualizerLoginContext;
    private String machineName;
    private String serverUrl;
    // Connection params, set via the TS once a game is ready
    private String serverQueue = "serverInput";
    private String queueName = "remote-visualizer";

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        mode = visualizerService.getMode();
        if (!mode.equals(Constants.MODE_TOURNAMENT)) {
            return;
        } else {
            tournamentUrl = visualizerService.getTournamentUrl();
            visualizerLoginContext = visualizerService.getTournamentPath();
            machineName = visualizerService.getMachineName();
            serverUrl = visualizerService.getServerUrl();
        }

        // delay to let deployment complete
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                init();
            }
        }, 10 * 1000);
    }

    /**
     * Called on initialization to start message feeder and state machine.
     */
    public void init() {
        // TODO Also reset logger

        // Start the message feeder
        messageFeeder = new MessageFeeder();
        messageFeeder.setDaemon(true);
        messageFeeder.start();

        // Start the state runner
        stateRunner = new StateRunner();
        stateRunner.setDaemon(true);
        stateRunner.start();
    }

    // once-per-game initialization
    private void initOnce() {
        initialized = true;

        log.info("initOnce()");
        visualizerService.newRun();

        // registrations for message listeners:
        Collection<MessageHandler> handlers = context.getBeansOfType(MessageHandler.class).values();
        for (MessageHandler handler : handlers) {
            log.debug("initializing..." + handler.getClass().getName());
            handler.initialize();
        }

        Collection<DomainRepo> repos = context.getBeansOfType(DomainRepo.class).values();
        for (DomainRepo repo : repos) {
            log.debug("recycling..." + repos.getClass().getName());
            repo.recycle();
        }
    }

    @SuppressWarnings("deprecation")
    @PreDestroy
    private void cleanUp() throws Exception {
      try {
        // Shutdown the proxy if needed
        if (proxy != null) {
            proxy.shutdown();
        }

        // Kill the message pump from within
        messageQueue.clear();
        putMessage(TournamentEvent.QUIT);
        if (messageFeeder != null) {
          messageFeeder.join();
        }

        // Kill the state machine from within
        eventQueue.clear();
        putEvent(TournamentEvent.QUIT);
        if (stateRunner != null) {
          stateRunner.join();
        }

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().contains("Timer-") || t.getName().contains("ActiveMQ")) {
                synchronized (t) {
                    t.stop();
                }
            }
        }
      } catch (Exception x) {
        log.warn("Error in TournamentService.cleanUp()", x);
        throw x;
      }
    }

    // shut down the proxy at end-of-game, wait a few seconds, go again.
    private void recycle() {
        // no longer initialized
        initialized = false;

        // shutdown proxy and force re-creation
        if (proxy != null) {
            log.info("shut down proxy");
            proxy.shutdown();
        }
        proxy = null;

        // force recreation of the connection
        connectionFactory.destroy();

        // wait for sim process to quit
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getTournamentState() {
        if (!visualizerService.getMode().equals(Constants.MODE_TOURNAMENT)) {
            return "";
        } else if (stateRunner != null
                && stateRunner.getTournamentState() != null) {
            return stateRunner.getTournamentState().toString();
        }
        return "Not ready";
    }

    private boolean isInactive() {
        long now = new Date().getTime();
        long silence = now - lastMsgTime;
        if (silence > maxMsgInterval) {
            // declare inactivity
            log.info("Inactivity declared");
            return true;
        }
        return false;
    }

    // convience functions for handling the event queue
    private void putEvent(TournamentEvent event) {
        try {
            eventQueue.put(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private TournamentEvent getEvent() {
        try {
            return eventQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return TournamentEvent.TICK; // default event, harmless enough
        }
    }

    // Logs into the TS to get the queue name for the upcoming game
    private void tournamentLogin() {
        if (tournamentUrl.isEmpty()) {
            // No TM, just connect to server
            putEvent(TournamentEvent.NOTM);
            return;
        }

        String urlString = tournamentUrl + visualizerLoginContext
                + "?machineName=" + machineName + "&machineLoad=" + getLoad();
        log.info("tourney url=" + urlString);

        URL url;
        try {
            url = new URL(urlString);
            URLConnection conn = url.openConnection();
            InputStream input = conn.getInputStream();
            log.info("Parsing message..");
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(input);
            doc.getDocumentElement().normalize();

            // Two different message types
            Node retryNode = doc.getElementsByTagName("retry").item(0);
            Node loginNode = doc.getElementsByTagName("login").item(0);

            if (retryNode != null) {
                String checkRetry = retryNode.getFirstChild().getNodeValue();
                log.info("Retry in " + checkRetry + " seconds");
                try {
                    Thread.sleep(Integer.parseInt(checkRetry) * 1000);
                } catch (InterruptedException ignored) {
                }
            } else if (loginNode != null) {
                log.info("Login response received! ");
                queueName = doc.getElementsByTagName("queueName").item(0)
                        .getFirstChild().getNodeValue();
                serverQueue = doc.getElementsByTagName("serverQueue").item(0)
                        .getFirstChild().getNodeValue();
                log.info(String.format(
                        "Login message received: queueName=%s, serverQueue=%s",
                        queueName, serverQueue));

                putEvent(TournamentEvent.ACCEPT);
            } else {
                // this is not working
                log.info("Invalid response from TS");
            }
        } catch (Exception e) {
            // should we have an event here?
            e.printStackTrace();
        }
    }

    // Get the current load on this machine
    private double getLoad() {
        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
        return mxBean.getSystemLoadAverage();
    }

    // Attempt to log into a game.
    private void gameLogin() {
        log.info("Ping sim server");
        if (null == proxy) {
            // no proxy yet for this game
            proxy = new LocalVisualizerProxy();
            proxy.init(this);
        }
        proxy.sendMessage(new VisualizerStatusRequest());
    }

    // The main thread receives the messages, put them in the queue
    @Override
    public void onMessage(Message message) {
        lastMsgTime = new Date().getTime();
        if (message instanceof TextMessage) {
            try {
                onMessage(((TextMessage) message).getText());
            } catch (JMSException e) {
                log.info("ERROR: viz failed to extract text from TextMessage: "+ e.toString());
            }
        }
    }

    // runs in JMS thread
    private void onMessage(String xml) {
        log.debug("onMessage(String) - received message:\n" + xml);

        Object message = converter.fromXML(xml);

        log.debug("onMessage(String) - received message of type "
                + message.getClass().getSimpleName());

        if (message instanceof VisualizerStatusRequest) {
            log.info("Received vsr");
            putEvent(TournamentEvent.VSR);
        } else if (message instanceof BrokerAccept || message instanceof BrokerAuthentication) {
            // hack to ignore these
        } else {
            try {
                putMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // convenience functions for handling the event queue
    private void putMessage(Object message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 30000)
    public void addTick() {
        // Only add ticks if staterunner handled all previous ones
        if (!eventQueue.contains(TournamentEvent.TICK)) {
            putEvent(TournamentEvent.TICK);
        }
    }

    // ---------------- Message handling ------------------
    // Pumps messages from incoming JMS messages into the visualizer in
    // a single thread. The queue avoids potential race conditions on
    // input.
    private class MessageFeeder extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    Object msg = messageQueue.take();
                    if (msg instanceof TournamentEvent && msg == TournamentEvent.QUIT) {
                        break;
                    }
                    receiveMessage(msg);
                }
            } catch (InterruptedException ignored) {

            }
        }

        private void receiveMessage(Object msg) {
            // once-per-game initialization...
            if (msg instanceof Competition) {
                // Competition must be first message.
                // If we see something else first, it's an error.
                initOnce();
            } else if (!initialized) {
                log.info("ERROR: msg of type " + msg.getClass().getName()
                        + ", but not initialized. Ignoring.");
                return;
            }

            if (msg != null) {
                dispatcher.handleNewObject(msg);
            } else {
                log.info("Received message is NULL!");
            }
            // end-of-game check
            if (visualizerService.getState() == VisualizerState.FINISHED) {
                log.info("Game finished");
                putEvent(TournamentEvent.SIMEND);
            }
        }
    }

    /** possible event types */
    enum TournamentEvent {
        TICK, NOTM, ACCEPT, VSR, SIMEND, QUIT
    };

    /**
     * State handlers for the remote visualizer service.
     *
     * @author John Collins
     */
    interface TournamentState {
        /**
         * Come here on state entry
         */
        void entry();

        /**
         * Handle an event
         */
        void handleEvent(TournamentEvent event);
    }

    // Run the viz state machine -- called from timer thread.
    private class StateRunner extends Thread {
        private boolean runningStates;
        private TournamentState initial, loginWait, gameWait, gameReady, loggedIn;
        private TournamentState currentState;

        @Override
        public void run() {
            initStates();

            while (runningStates) {
                TournamentEvent event = getEvent();

                if (event == TournamentEvent.QUIT) {
                    runningStates = false;
                    break;
                }

                currentState.handleEvent(event);
            }
        }

        private void initStates() {
            runningStates = true;

            initial = new TournamentState() {
                @Override
                public void entry() {
                    log.info("state initial");
                    if (null != proxy) {
                        recycle();
                    }
                    setTournamentState(loginWait);
                }

                @Override
                public void handleEvent(TournamentEvent event) {
                    if (event == TournamentEvent.TICK) {
                        // safety valve
                        setTournamentState(loginWait);
                    }
                }

                public String toString() {
                    return "initial";
                }
            };

            loginWait = new TournamentState() {
                @Override
                public void entry() {
                    log.info("state loginWait");
                    tournamentLogin();
                }

                @Override
                public void handleEvent(TournamentEvent event) {
                    if (event == TournamentEvent.NOTM) {
                        setTournamentState(gameWait);
                    } else if (event == TournamentEvent.ACCEPT) {
                        setTournamentState(gameReady);
                    } else if (event == TournamentEvent.TICK) {
                        tournamentLogin();
                    }
                }

                public String toString() {
                    return "loginWait";
                }
            };

            gameWait = new TournamentState() {
                @Override
                public void entry() {
                    log.info("state gameWait");
                    gameLogin();
                }

                @Override
                public void handleEvent(TournamentEvent event) {
                    if (event == TournamentEvent.VSR) {
                        setTournamentState(loggedIn);
                        visualizerService.setState(VisualizerState.WAITING);
                    } else if (event == TournamentEvent.TICK) {
                        gameLogin();
                    }
                }

                public String toString() {
                    return "gameWait";
                }
            };

            gameReady = new TournamentState() {
                @Override
                public void entry() {
                    log.info("state gameReady");
                    gameReadyAt = new Date().getTime();
                    gameLogin();
                }

                @Override
                public void handleEvent(TournamentEvent event) {
                    if (event == TournamentEvent.VSR) {
                        setTournamentState(loggedIn);
                        visualizerService.setState(VisualizerState.WAITING);
                    } else if (event == TournamentEvent.TICK) {
                        long now = new Date().getTime();
                        // limit harrassment of running game
                        if (now > (gameReadyAt + maxGameReadyInterval)) {
                            setTournamentState(initial);
                        } else {
                            gameLogin();
                        }
                    }
                }

                public String toString() {
                    return "gameReady";
                }
            };

            loggedIn = new TournamentState() {
                @Override
                public void entry() {
                    log.info("state loggedIn");
                }

                @Override
                public void handleEvent(TournamentEvent event) {

                    if (event == TournamentEvent.SIMEND) {
                        setTournamentState(initial);
                    } else if (event == TournamentEvent.TICK && isInactive()) {
                        visualizerService.setState(VisualizerState.FINISHED);
                        setTournamentState(initial);
                    }
                }

                public String toString() {
                    return "loggedIn";
                }
            };

            setTournamentState(initial);
        }

        private void setTournamentState(TournamentState newState) {
            currentState = newState;
            newState.entry();
        }

        public TournamentState getTournamentState() {
            return currentState;
        }
    }

    private class LocalVisualizerProxy {
        boolean connectionOpen = false;
        DefaultMessageListenerContainer container;

        // set up the jms queue
        void init(TournamentService host) {
            log.info("Server URL: " + serverUrl + ", queue: " + queueName);

            // Can't set this earlier, need the params from visualizerService
            ActiveMQConnectionFactory amqFactory =
                    (ActiveMQConnectionFactory) connectionFactory.getTargetConnectionFactory();
            amqFactory.setBrokerURL(serverUrl);

            // register host as listener
            container = new DefaultMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            container.setDestinationName(queueName);
            container.setMessageListener(host);
            container.setTaskExecutor(taskExecutor);
            container.afterPropertiesSet();
            container.start();

            connectionOpen = true;
        }

        private void sendMessage(Object msg) {
            try {
                final String text = converter.toXML(msg);
                MessageCreator creator = session -> session.createTextMessage(text);
                JmsTemplate template = context.getBean(JmsTemplate.class);
                template.setConnectionFactory(connectionFactory);
                template.send(serverQueue, creator);
            } catch (Exception e) {
                log.warn("Exception " + e.toString() + " sending message - ignoring");
            }
        }

        private synchronized void shutdown() {
            // Stop the container
            final LocalVisualizerProxy proxy = this;
            Runnable callback = proxy::closeConnection;
            container.stop(callback);

            while (connectionOpen) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private synchronized void closeConnection() {
            connectionOpen = false;
            notifyAll();
        }
    }
}
