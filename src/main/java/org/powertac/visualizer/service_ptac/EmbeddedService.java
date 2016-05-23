package org.powertac.visualizer.service_ptac;

import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.visualizer.config.Constants;
import org.powertac.server.CompetitionControlService;
import org.powertac.server.CompetitionSetupService;
import org.powertac.visualizer.domain.File;
import org.powertac.visualizer.domain.Game;
import org.powertac.visualizer.logtool.LogtoolExecutor;
import org.powertac.visualizer.service_ptac.VisualizerService.VisualizerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;

import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * This service runs Power TAC games (sim, boot and replay).
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Service
public class EmbeddedService implements ApplicationListener<ApplicationContextEvent> {

    private final Logger log = LoggerFactory.getLogger(EmbeddedService.class);

    @Inject
    private VisualizerService visualizerService;

    private MessageDispatcher messageDispatcher;

    private CompetitionSetupService competitionSetupService;

    private CompetitionControlService competitionControlService;

    private VisualizerProxy visualizerProxy;

    private Thread replayGameThread;
    
    private ClassPathXmlApplicationContext context;

    /*
     * (non-Javadoc)
     *
     * @see org.powertac.visualizer.service.ptac.VisualizerCompetitionServiceIf#
     * runBootGame(org.powertac.visualizer.domain.Game)
     */
    public String runBootGame(Game game) {
        String error = checkRun();
        if (error != null) {
            return error;
        }

        refreshContext(false);

        visualizerService.recycleAll();
        visualizerService.setState(VisualizerState.WAITING);

        // launch a boot session
        error = competitionSetupService.bootSession(
                game.getBootFilePath(),
                game.getConfigFilePath(),
                game.getName(),
                null,
                game.getSeedFilePath(),
                game.getWeatherFilePath()
        );
        if (error != null) {
            visualizerService.setState(VisualizerState.FAILED);
        }
        return error;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.powertac.visualizer.service.ptac.VisualizerCompetitionServiceIf#
     * runSimGame(org.powertac.visualizer.domain.Game)
     */
    public String runSimGame(Game game) {
        String error = checkRun();
        if (error != null) {
            return error;
        }

        refreshContext(false);

        visualizerService.recycleAll();
        visualizerService.setState(VisualizerState.WAITING);

        // launch a sim session
        error = competitionSetupService.simSession(
                game.getBootFilePath(),
                game.getConfigFilePath(),
                visualizerService.getJmsUrl(),
                game.getName(),
                null,
                game.getBrokerList(),
                game.getSeedFilePath(),
                game.getWeatherFilePath(),
                null
        );
        if (error != null) {
            visualizerService.setState(VisualizerState.FAILED);
        }
        return error;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.powertac.visualizer.service.ptac.VisualizerCompetitionServiceIf#
     * runReplayGame(org.powertac.visualizer.domain.Game)
     */
    public String runReplayGame(File file) {
        closeGame();

        replayGameThread = new Thread() {
            @Override
            public void run() {
                // No SimStart and SimEnd when extracting log
                visualizerService.setState(VisualizerState.RUNNING);
                LogtoolExecutor logtoolExecutor = new LogtoolExecutor();
                logtoolExecutor.readLog(file.getPath(), messageDispatcher);
                replayGameThread = null;
                visualizerService.setState(VisualizerState.FINISHED);
            }
        };
        replayGameThread.start();

        return "";
    }

    public void closeGame() {
        // relevant for sim and boot games:
        if (competitionControlService.isRunning()) {
            competitionControlService.shutDown();
        }

        // relevant for replay games
        if (replayGameThread != null) {
            try {
                replayGameThread.interrupt();
                replayGameThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            replayGameThread = null;
        }
    }

    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
        if (event instanceof ContextClosedEvent) {
            log.info("Context closed");
        }
    }

    private void refreshContext(boolean force) {
        if (visualizerService.getMode().equals(Constants.MODE_TOURNAMENT)) {
            return;
        }

        if (context == null) {
            context = new ClassPathXmlApplicationContext("powertac.xml");
        } else if (force) {
            if (competitionControlService != null) {
                competitionControlService.shutDown();
            }
            context.close();
            context.refresh();
        } else {
            return;
        }

        context.registerShutdownHook();
        context.addApplicationListener(this);

        competitionSetupService = context.getBean(CompetitionSetupService.class);
        competitionControlService = context.getBean(CompetitionControlService.class);

        // register with a visualizer proxy in order to get messages
        visualizerProxy = context.getBean(VisualizerProxy.class);
        visualizerProxy.registerVisualizerMessageListener(messageDispatcher);
    }

    private String checkRun() {
        if (visualizerService.getMode().equals(Constants.MODE_TOURNAMENT)) {
            return "Can't run game in Tournament mode";
        }
        if (competitionControlService != null && competitionControlService.isRunning() || replayGameThread != null) {
            return "Can't run game, already running";
        }
        return null;
    }
}
