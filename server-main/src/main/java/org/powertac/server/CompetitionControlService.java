/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.server;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.Instant;
import org.powertac.common.*;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.*;
import org.powertac.common.repo.BootstrapDataRepo;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * This is the competition controller. It has two major roles in the
 * server:
 * <ol>
 * <li>Once the game is configured, the <code>init()</code> method must be
 * called. There are two versions of this method; the <code>init()</code>
 * version runs a "bootstrap" simulation that runs the customer models and
 * the wholesale market for a limited period of time to collect an initial
 * dataset from which brokers can bootstrap their internal models. During
 * a bootstrap simulation, external brokers cannot log in; only the default
 * broker is active. The <code>init(filename)</code> version loads bootstrap
 * data from the named file, validates it, and then opens up the
 * broker login process, most of which is delegated to the BrokerProxy.</li>
 * <li>Once the simulation starts, the <code>step() method is called every 
 * <code>timeslotLength</code> seconds. This runs through
 * <code>timeslotPhaseCount</code> phases, calling the <code>activate()</code>
 * methods on registered components. Phases start at 1; by default there
 * are four phases.</li>
 * <li>When the number of timeslots equals <code>timeslotCount</code>, the
 * simulation is ended.</li>
 * </ol>
 * <p>
 * 
 * @author John Collins
 */
@Service
public class CompetitionControlService
  implements CompetitionControl
{
  static private Logger log = LogManager.getLogger(CompetitionControlService.class);

  private Competition competition;

  private SimulationClockControl clock;

  @Autowired
  private TimeService timeService; // inject simulation time service dependency

  @Autowired
  private BrokerProxy brokerProxyService;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private BootstrapDataRepo bootstrapDataRepo;

  @Autowired
  private LogService logService;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private WeatherReportRepo weatherReportRepo;
  
  @Autowired
  private ServerPropertiesService configService;

  @Autowired
  private ServerMessageReceiver serverMessageReceiver;

  @Autowired
  private JmsManagementService jmsManagementService;

  @Autowired
  private TournamentSchedulerService tournamentSchedulerService;

  @Autowired 
  private VisualizerProxyService visualizerProxyService;

  // Server JMS Queue Name
  private String serverQueueName = "serverInput";

  private boolean running = false;

  // timeslotPhaseCount is set as part of the Spring configuration
  private int timeslotPhaseCount = 1;
  private ArrayList<List<TimeslotPhaseProcessor>> phaseRegistrations;

  private int timeslotCount = 0;
  private int currentSlot = 0;
  private int bootstrapOffset = 0; // non-zero for sim sessions
  private RandomSeed randomGen; // used to compute game length

  // broker interaction state
  private ArrayList<String> alwaysAuthorizedBrokers;
  private HashMap<String, String> authorizedBrokerMap;
  private List<String> brokerNames;

  @ConfigurableValue(valueType = "Integer",
      description = "Maximum time in msec to wait for first broker login")
  private int firstLoginTimeout = 0;
  
  @ConfigurableValue(valueType = "Integer",
      description = "Maximum time in msec to wait for subsequent broker login")
  private int loginTimeout = 0;

  @ConfigurableValue(valueType = "Boolean",
      description = "If true, then brokers can send PauseRequest messages")
  private boolean brokerPauseAllowed = false;

  private ArrayList<String> pendingLogins; // external logins expected
  private int loginCount = 0; // number of external brokers logged in so far

  @ConfigurableValue(valueType = "Long",
      description = "Milliseconds/timeslot in boot mode. Should be > 300.")
  private long bootstrapTimeslotMillis = 2000;
  
  @ConfigurableValue(valueType = "String",
      description = "Name of abort file")
  private String abortFileName = "abort";

  @ConfigurableValue(valueType="Integer",
      description = "depth of stack trace on exception")
  private int stackTraceDepth = 5;

  // if we don't have a bootstrap dataset, we are in bootstrap mode.
  private boolean bootstrapMode = true;
  //private List<Object> bootstrapDataset = null;
  //private int bootstrapDiscardedTimeslots = 24;
  
  private boolean simRunning = false;

  /**
   * Initializes the service in preparation for a new simulation
   */
  public void init ()
  {
    phaseRegistrations = null;

    // register with JMS Server
    if (!bootstrapMode) {
      jmsManagementService.initializeServerQueue(serverQueueName);
      jmsManagementService.registerMessageListener(serverQueueName,
          serverMessageReceiver);
    }
    
    // broker message registration for clock-control messages
    //brokerProxyService.registerSimListener(this);
    for (Class<?> messageType: Arrays.asList(BrokerAuthentication.class,
                                             PauseRequest.class,
                                             PauseRelease.class)) {
      brokerProxyService.registerBrokerMessageListener(this, messageType);
    }
  }
  
  /**
   * Sets the number of phases into which a timeslot is divided for processing.
   * Intended to be called by Spring initialization. The value must be at least as
   * large as the maximum value of timeslotPhase among the service modules.
   */
  public void setTimeslotPhaseCount (int count)
  {
    if (count <= 0)
      log.error("TimeslotPhaseCount must be >= 0");
    else
      timeslotPhaseCount = count;
  }

  /**
   * Sets the list of broker usernames that are always authorized, even in
   * bootstrap mode. Normally this is just "defaultBroker". This method is
   * intended to be called by Spring initialization.
   */
  public void setAlwaysAuthorizedBrokers (List<String> brokerList)
  {
    // copy the list out of Spring space
    alwaysAuthorizedBrokers = new ArrayList<String>(brokerList);
  }

  /**
   * Sets the list of brokers allowed and expected to log in before
   * starting a simulation. The simulation will not start until all 
   * brokers in the list are logged in, unless a timeout is configured.
   */
  @Override
  public void setAuthorizedBrokerList (List<String> brokerList)
  {
    //this.brokerNames = brokerList;
    this.brokerNames = new ArrayList<>();
    loginCount = brokerList.size();
    pendingLogins = new ArrayList<>();
    authorizedBrokerMap = new HashMap<String, String>();
    for (String brokerName : alwaysAuthorizedBrokers) {
      authorizedBrokerMap.put(brokerName, brokerName);
      brokerNames.add(brokerName);
      log.info("pre-authorized " + brokerName);
    }
    for (String broker : brokerList) {
      // check for broker spec of the form name/queue
      String[] components = broker.split("/");
      if (components.length < 1)
        log.error("Bad broker spec " + broker);
      else {
        String brokerName = components[0];
        brokerNames.add(brokerName);
        String queueName = brokerName;
        if (components.length > 1)
          queueName = components[1];
        authorizedBrokerMap.put(brokerName, queueName);
        log.info("Authorized broker " + brokerName + " / " + queueName);
        pendingLogins.add(brokerName);
      }
    }
  }

  /**
   * Sets up the bootstrap dataset extracted from its external source.
   */
  //void setBootstrapDataset (List<Object> dataset)
  //{
  //  bootstrapDataset = dataset;
  //}
  
  /**
   * Sets the name of the server's JMS input queue.
   */
  void setInputQueueName (String queueName)
  {
    if (null != queueName && !queueName.isEmpty())
      serverQueueName = queueName;
  }

  /**
   * Runs a simulation that is already set up. This is intended to be called
   * from a method that knows whether we are running a bootstrap sim or a 
   * normal sim.
   */
  @Override
  public void runOnce (boolean bootstrapMode)
  {
    runOnce(bootstrapMode, false);
  }

  @Override
  public void runOnce (boolean bootstrapMode, boolean dumpConfigOnly)
  {
    log.info("runOnce " + bootstrapMode);
    this.bootstrapMode = bootstrapMode;
    competition = Competition.currentCompetition();

    // to enhance testability, initialization is split into a static setup()
    // phase, followed by calling runSimulation() to start the sim thread.
    if (simRunning) {
      log.warn("attempt to start sim on top of running sim");
      return;
    }
    // -- note small race condition here --
    simRunning = true;
    if (competition == null) {
      log.error("null competition instance");
    }

    // start JMS provider for sims
    if (!bootstrapMode) {
      jmsManagementService.start();
    }

    init();

    // enable remote broker login here
    if (!bootstrapMode) {
      tournamentSchedulerService.ready();
    }

    if (!setup()) {
      simRunning = false;
      return;
    }
    else if (dumpConfigOnly) {
      // we are done
      configService.finishConfigOutput();
      return;
    }

    // run the simulation, wait for completion
    runSimulation((long) (competition.getTimeslotLength() * TimeService.MINUTE /
        competition.getSimulationRate()));

    // log and post broker stats
    logBrokerStats();
    postBrokerStats();

    // wrap up
    shutDown();
  }

  // ------------------ simulation setup -------------------
  // Sets up simulation state in preparation for a sim run. When finished,
  // all brokers are logged in, they have the bootstrap data, and 
  // we are ready to start the clock.
  private boolean setup ()
  {
    // set up random sequence for new simulation run
    randomGen = randomSeedRepo.getRandomSeed("CompetitionControlService",
                                             competition.getId(),
                                             "game-setup");

    configService.configureMe(this);

    if (!bootstrapMode) {
      // Create the timeslots from the bootstrap period - they will be needed to 
      // instantiate weather reports. All are disabled.
      bootstrapOffset = competition.getBootstrapTimeslotCount()
                          + competition.getBootstrapDiscardedTimeslots();
      createInitialTimeslots(competition.getSimulationBaseTime(),
                             (bootstrapOffset + 1),
                             0);
      log.info("created " + timeslotRepo.count() + " bootstrap timeslots");
    }

    // set up the simulation clock
    setTimeParameters();
    log.info("start at timeslot " + timeslotRepo.currentTimeslot().getSerialNumber());
    
    // configure plugins, but don't allow them to broadcast to brokers
    brokerProxyService.setDeferredBroadcast(true);
    if (!configurePlugins()) {
      log.error("failed to configure plugins");
      return false;
    }

    // set up the initial timeslots - some initialization processes may need to
    // see a non-null current timeslot.
    createInitialTimeslots(timeService.getCurrentTime(),
                           competition.getDeactivateTimeslotsAhead(),
                           competition.getTimeslotsOpen());
    
    // add CustomerInfo instances to the Competition instance
    for (CustomerInfo customer : customerRepo.list()) {
      competition.addCustomer(customer);
    }

    // sim length for bootstrap mode comes from the competition instance;
    // for non-bootstrap mode, it is computed from competition parameters.
    timeslotCount = competition.getBootstrapTimeslotCount()
                    + competition.getBootstrapDiscardedTimeslots();
    if (!bootstrapMode) {
      // #486 - add bootstrap count to computed game length
      timeslotCount = computeGameLength(competition.getFixedTimeslotCount(),
                                        competition.getMinimumTimeslotCount(),
                                        competition.getExpectedTimeslotCount());
      log.info("timeslotCount = " + timeslotCount);
    }
    
    // #660 read bootstrap dataset here, before blocking for
    // broker login

    if (!bootstrapMode) {
      waitForBrokerLogin();
      visualizerProxyService.waitForRemoteViz(loginTimeout);
    }

    // Publish Competition object at right place - after plugins
    // are initialized. This is necessary because some may need to
    // see the broadcast after they are initialized (visualizer, for example)
    for (String retailer : brokerRepo.findRetailBrokerNames()) {
      competition.addBroker(retailer);
    }
    
    // notify tournament scheduler that game is starting, assuming there
    // is a tournament scheduler to notify.
    if (!bootstrapMode) {
      tournamentSchedulerService.inProgress(timeslotCount);
    }

    // send the Competition instance, then the broadcast deferred messages
    brokerProxyService.setDeferredBroadcast(false);
    brokerProxyService.broadcastMessage(competition);
    Properties published = configService.getPublishedConfiguration();
    brokerProxyService.broadcastMessage(published);
    if (!bootstrapMode) {
      log.info("Published configuration: {}", published.toString());
      List<Object> bootstrapDataset = bootstrapDataRepo.getData();
      brokerProxyService.broadcastMessages(bootstrapDataset);
      // pull out the weather reports and stick them in their repo
      for (Object msg : bootstrapDataset) {
        if (msg instanceof WeatherReport) {
          weatherReportRepo.add((WeatherReport) msg);
        }
      }
    }
    brokerProxyService.broadcastDeferredMessages();

    // Send out the first timeslot update
    brokerProxyService.broadcastMessage(makeTimeslotUpdate());
    return true;
  }

  private TimeslotUpdate makeTimeslotUpdate ()
  {
    List<Timeslot> enabled = timeslotRepo.enabledTimeslots();
    TimeslotUpdate msg = new TimeslotUpdate(timeService.getCurrentTime(),
                                            enabled.get(0).getSerialNumber(),
                                            enabled.get(enabled.size() - 1).getSerialNumber());
    return msg;
  }
  
  // blocks until all brokers have logged in.
  private synchronized void waitForBrokerLogin ()
  {
    if (authorizedBrokerMap == null || authorizedBrokerMap.size() == 0) {
      // nothing to do here
      return;
    }
    if (log.isInfoEnabled()) {
      StringBuffer msg = new StringBuffer();
      msg.append("waiting for logins from");
      for (String name : authorizedBrokerMap.keySet()) {
        msg.append(" ").append(name);
      }
      log.info(msg.toString());
    }
    log.info("pendingLogins.size()=" + pendingLogins.size() + ", loginCount="+ loginCount);
    if (loginCount == pendingLogins.size()) {
      // no external brokers logged in yet
      try {
        // wait longer for the first login
        wait(firstLoginTimeout);
        log.info("first login observed");
      }
      catch (InterruptedException ie) {
        authorizedBrokerMap.clear();
        log.info("first login wait is interrupted");
      }
    }
    // need to wait for additional logins
    int sz = authorizedBrokerMap.size();
    try {
      // limit the wait time for subsequent timeouts
      while (sz >= authorizedBrokerMap.size()
             && authorizedBrokerMap.size() > 0) {
        wait(loginTimeout);
        sz -= 1;
      }
    }
    catch (InterruptedException ie) {
      authorizedBrokerMap.clear();
    }
    // too late - no more logins
    if (authorizedBrokerMap.size() > 0) {
      log.warn("Some brokers did not log in: " + authorizedBrokerMap);
      authorizedBrokerMap.clear();
    }
    // if nobody logged in, then abort the game.
    if (loginCount == pendingLogins.size()) {
      timeslotCount = 1;
    }    
  }

  /**
   * Logs in a broker, just in case the broker is on the authorizedBrokerMap.
   * Returns true if the broker is authorized, otherwise false.
   */
  @Override
  public synchronized boolean loginBroker (String username)
  {
    // cannot log in if there's no list, or if the broker is not on the list
    if (authorizedBrokerMap == null
        || authorizedBrokerMap.size() == 0
        || !authorizedBrokerMap.containsKey(username)) {
      log.info("Unauthorized attempt to log in " + username);
      return false;
    }
    // otherwise we log the broker in. Note that the broker's queue must
    // be set up and acknowledgment sent before returning, because as
    // soon as the last broker logs in, the simulation starts. If the broker
    // is not already logged in at that point, it will likely miss one or more
    // startup messages.
    
    // Brokers can be local, in which case the Broker instance already exists.
    // If that's the case, we don't need to create the broker, send a message,
    // or create a new ID prefix.
    Broker broker = brokerRepo.findByUsername(username);
    log.info("Log in " + ((null == broker)?"":"existing ") +
             "broker " + username + ", queue " + authorizedBrokerMap.get(username));
    if (null == broker) {
      broker = new Broker(username);
      brokerRepo.add(broker);
    }
    
    // only enabled brokers get messages
    broker.setEnabled(true);
    if (!broker.isLocal()) {
      // non-local brokers need queues and keys
      String queueName = authorizedBrokerMap.get(username);
      broker.setQueueName(authorizedBrokerMap.get(username));
      jmsManagementService.createQueue(queueName);
      computeBrokerKey(broker);
    }
    // assign prefix and key with accept message
    int prefix = getBrokerPrefix(broker);
    broker.setIdPrefix(prefix);
    log.info("Broker " + broker.getUsername()
             + " key: " + broker.getKey() + ", prefix: " + prefix);
    brokerProxyService.sendMessage(broker, new BrokerAccept(prefix, broker.getKey()));
    
    // clear the broker from the list, and if the list is now empty, then
    // notify the simulation to start
    authorizedBrokerMap.remove(username);
    if (pendingLogins.contains(username))
      loginCount -= 1;
    notifyAll();
    return true;
  }

  /**
   * Get a prefix based on the list of brokers
   * Default broker (not in the list) must have prefix == 1
   */
  private int getBrokerPrefix (Broker broker)
  {
    int posn = brokerNames.indexOf(broker.getUsername());
    if (-1 == posn)
      log.error("Broker {} not found in brokerNames", broker.getUsername());
    return posn + 1;
  }

  private void computeBrokerKey (Broker broker)
  {
    long time = new Date().getTime() & 0xffffffff;
    int hash = broker.hashCode();
    int code = (int)((hash * time) & 0x7fffffff);
    String key = Integer.toString(code, 36);
    broker.setKey(key);
  }

  // set simulation time parameters, making sure that simulationStartTime
  // is still sufficiently in the future.
  private void setTimeParameters()
  {
    Instant base = competition.getSimulationBaseTime();
    // if we are not in bootstrap mode, we have to add the bootstrap interval
    // to the base
    long rate = competition.getSimulationRate();
    
    // reset the slot counting mechanism
    // we want the first tick to get us to the start of the sim
    currentSlot = 0;
    int slotCount = 0;
    
    if (!bootstrapMode) {
      slotCount = bootstrapOffset;
      log.info("first slot: " + slotCount);
      //base = base.plus(slotCount * competition.getTimeslotDuration());
    }
    else {
      // compute rate from bootstrapTimeslotMillis
      log.info("bootstrapTimeslotMillis=" + bootstrapTimeslotMillis);
      rate = competition.getTimeslotDuration() / bootstrapTimeslotMillis;
      log.info("bootstrap mode clock rate: " + rate);
    }
    timeService.setClockParameters(base.getMillis(), rate,
                                   competition.getTimeslotDuration());
    timeService.setCurrentTime(base.plus(slotCount * competition.getTimeslotDuration()));
  }

  // Computes a random game length as outlined in the game specification
  private int computeGameLength (Integer fixedLength,
                                 int minLength, int expLength)
  {
    if (null != fixedLength) {
      log.info("game-length fixed externally to {}", fixedLength);
      return fixedLength;
    }
    else if (expLength == minLength) {
      log.info("game-length fixed: {}", minLength);
      return minLength;
    }
    else {
      double roll = randomGen.nextDouble();
      // compute k = ln(1-roll)/ln(1-p) where p = 1/(exp-min)
      double k =
        (Math.log(1.0 - roll) / Math
            .log(1.0 - 1.0 / (expLength - minLength + 1)));
      int length = minLength + (int) Math.floor(k);
      log.info("game-length " + length + "(k=" + k + ", roll=" + roll + ")");
      return length;
    }
  }

  // Runs the initialization protocol on each plugin, supports precedence
  // relationships among them.
  private boolean configurePlugins ()
  {
    List<InitializationService> initializers =
        SpringApplicationContext.listBeansOfType(InitializationService.class);

    ArrayList<String> completedPlugins = new ArrayList<String>();
    ArrayList<InitializationService> deferredInitializers = new ArrayList<InitializationService>();
    for (InitializationService initializer : initializers) {
      if (bootstrapMode) {
        if (initializer.equals(visualizerProxyService)) {
        	//|| initializer.equals(jmsManagementService)) { // not an InitializationService
          log.info("Skipping initialization of " + initializer.toString());
          continue;
        }
      }

      log.info("attempt to initialize " + initializer.toString());
      String success = initializer.initialize(competition, completedPlugins);
      if (success == null) {
        // defer this one
        log.info("deferring " + initializer.toString());
        deferredInitializers.add(initializer);
      }
      else if (success.equals("fail")) {
        log.error("Failed to initialize plugin " + initializer.toString());
        return false;
      }
      else {
        log.info("completed " + success);
        completedPlugins.add(success);
      }
    }

    while (deferredInitializers.size() > 0) {
      int startSize = deferredInitializers.size();

      for (Iterator<InitializationService> it = deferredInitializers.iterator();
           it.hasNext();) {
        InitializationService initializer = it.next();
        log.info("additional attempt to initialize " + initializer.toString());
        String success = initializer.initialize(competition, completedPlugins);

        if (success == null) {
          // defer this one
          log.info("deferring " + initializer.toString());
        }
        else {
          log.info("completed " + success);
          completedPlugins.add(success);
          it.remove();
        }
      }

      if (deferredInitializers.size() == startSize) {
        for (InitializationService initializer : deferredInitializers) {
          log.error("Failed to initialize " + initializer.toString());
        }
        return false;
      }
    }

    return true;
  }

  // Creates the initial complement of timeslots
  // but the timeslotRepo creates them as needed now
  private void createInitialTimeslots (Instant base,
                                       int initialSlots,
                                       int openSlots)
  {
    log.info("createInitialTimeslots(" + base + ", " + initialSlots
             + ", " + openSlots + "), at " + timeService.getCurrentTime());
    //long timeslotMillis = competition.getTimeslotDuration();
    // set timeslot index according to bootstrap mode
    //for (int i = 0; i < initialSlots - 1; i++) {
    //  timeslotRepo.makeTimeslot(base);
    //}
    //for (int i = initialSlots - 1; i < (initialSlots + openSlots - 1); i++) {
    //  timeslotRepo.makeTimeslot(base.plus(i * timeslotMillis));
    //}
  }
  
  // Dumps final broker statistics to the trace log
  private void logBrokerStats ()
  {
    StringBuffer buf = new StringBuffer();
    buf.append("Final balance (brokername:balance) [");
    for (String brokerName : competition.getBrokers()) {
      Broker broker = brokerRepo.findByUsername(brokerName);
      buf.append(" \"").append(brokerName).append("\":");
      buf.append(broker.getCashBalance());
    }
    buf.append(" ]");
    log.info(buf.toString());
  }

  // Posts broker stats to TS as a string of the form
  // username:balance,...
  private void postBrokerStats ()
  {
    tournamentSchedulerService.sendResults(composeBrokerStats());
  }

  // Generates a String containing names of external brokers and their
  // current standings.
  private String composeBrokerStats ()
  {
    StringBuffer buf = new StringBuffer();
    String delimiter = "";
    for (String brokerName : competition.getBrokers()) {
      Broker broker = brokerRepo.findByUsername(brokerName);
      buf.append(delimiter).append(brokerName).append(":");
      buf.append(broker.getCashBalance());
      delimiter = ",";
    }
    return buf.toString();
  }

  // ------------- simulation start and run ----------------
  /**
   * Starts the simulation.  
   */
  private void runSimulation (long scheduleMillis)
  {
    SimRunner runner = new SimRunner(this);
    runner.start();
    try {
      runner.join();
    }
    catch (InterruptedException ie) {
      log.warn("sim interrupted", ie);
    }
  }

  /**
   * Runs a step of the simulation
   */
  private void step ()
  {
    // allow for controlled shutdown
    if (checkAbort()) {
      log.info("Session aborted");
      stop();
      return;
    }

    Date started = new Date();
    
    // make sure the clock has not drifted
    clock.checkClockDrift();

    int ts = activateNextTimeslot();
    if (!running)
      return;
    Instant time = timeService.getCurrentTime();
    log.info("step at " + time.toString());
    
    // check queue status before sending new messages
    detectAndKillHangingQueues();

    for (int phase = 1; phase <= timeslotPhaseCount; phase++) {
      log.info("activate phase " + phase);
      for (TimeslotPhaseProcessor fn : phaseRegistrations.get(phase - 1)) {
        fn.activate(time, phase);
      }
    }
    TimeslotComplete msg = new TimeslotComplete(ts);
    brokerProxyService.broadcastMessage(msg);
    Date ended = new Date();
    long elapsed = ended.getTime() - started.getTime();
    if (!bootstrapMode) {
      tournamentSchedulerService.heartbeat(ts, composeBrokerStats(), elapsed);
    }
    log.info("Elapsed time: " + elapsed);
    if (--timeslotCount <= 0) {
      log.info("Stopping simulation");
      stop();
    }
  }

  private void detectAndKillHangingQueues() {
    Set<String> badQueues = jmsManagementService.processQueues();
    if (badQueues != null && badQueues.size() > 0) {
      for (Broker broker : brokerRepo.list()) {
        if (badQueues.contains(broker.toQueueName())) {
          // disable broker and revoke all its tariffs
          log.warn("Disabling unresponsive broker " + broker.getUsername());
          broker.setEnabled(false);
        }
      }
      if (badQueues.contains(visualizerProxyService.getVisualizerQueueName())) {
        visualizerProxyService.setRemoteVisualizer(false);
      }
    }
  }

  private boolean checkAbort ()
  {
    File abortFile = new File(abortFileName);
    if (abortFile.canRead()) {
      log.warn("Abort file detected - shutting down");
      abortFile.delete();
      return true;
    }
    return false;
  }

  // activates the next timeslot - called once/timeslot. Returns the index
  // of the current timeslot
  private int activateNextTimeslot ()
  {
    long timeslotMillis = competition.getTimeslotDuration();
    Timeslot current = findCurrentTimeslot();
    if (current == null) {
      log.error("current timeslot is null at " + timeService.getCurrentTime());
      return -1;
    }

    // first, deactivate the oldest active timeslot
    // remember that this runs at the beginning of a timeslot, so the current
    // timeslot is the first one we consider.
    int oldSerial = (current.getSerialNumber() +
            competition.getDeactivateTimeslotsAhead() - 1);
    Timeslot oldTs = timeslotRepo.findBySerialNumber(oldSerial);
    log.info("Deactivated timeslot " + oldSerial + ", start " + oldTs.getStartInstant().toString());

    // then create if necessary and activate the newest timeslot
    int newSerial = (current.getSerialNumber() +
            competition.getDeactivateTimeslotsAhead() - 1 +
            competition.getTimeslotsOpen());
    Timeslot newTs = timeslotRepo.findBySerialNumber(newSerial);
    if (newTs == null) {
      log.info("newTS null in activateNextTimeslot");
      long start = (current.getStartInstant().getMillis() +
              (newSerial - current.getSerialNumber()) * timeslotMillis);
      newTs = timeslotRepo.makeTimeslot(new Instant(start));
    }
    log.info("Activated timeslot " + newSerial + ", start " + newTs.getStartInstant());
    // Communicate timeslot updates to brokers
    brokerProxyService.broadcastMessage(makeTimeslotUpdate());
    return current.getSerialNumber();
  }

  // Finds and returns the timeslot with the correct index, adjusting
  // the clock if necessary
  private synchronized Timeslot findCurrentTimeslot ()
  {
    // note that currentSlot got updated after the last call to step();
    int expectedIndex = currentSlot + bootstrapOffset;
    Timeslot currentTimeslot = timeslotRepo.findBySerialNumber(expectedIndex);
    if (currentTimeslot == null) {
      return null;
    }

    Timeslot next = timeslotRepo.currentTimeslot();
    if (next.getSerialNumber() > expectedIndex) {
      // time has disappeared somewhere - may need to re-sync clocks
      // unfortunately, this does not work, so we need to abort the game
      // -- see issue #729
      int missingTicks = next.getSerialNumber() - expectedIndex;
      log.error("Missed {} ticks, expected {} but see {}",
                missingTicks, expectedIndex, next.getSerialNumber());
      stop();
//      long newStart =
//              new Date().getTime()
//              - (currentTimeslot.getStartInstant().getMillis()
//                 - timeService.getBase()) / timeService.getRate();
//      timeService.setStart(newStart);
//      timeService.setCurrentTime();
//      resume(newStart);
//      
//      // go again, just in case...
//      next = timeslotRepo.currentTimeslot();
    }

    return currentTimeslot;
  }

  // ------------ simulation shutdown ------------
  /**
   * Expose simulation-running flag
   */
  @Override
  public boolean isRunning()
  {
    return simRunning;
  }

  /**
   * Signals the simulation thread to stop after processing is completed in
   * the current timeslot.
   */
  public void stop ()
  {
    log.info("sim stop");
    running = false;
  }

  /**
   * Shuts down the simulation and cleans up.
   */
  @Override
  public void shutDown ()
  {
    log.info("shutdown");
    running = false;

    SimEnd endMsg = new SimEnd();
    brokerProxyService.broadcastMessage(endMsg);

    simRunning = false;
    
    // need to wait for clock control stop before shutting down JMS provider
    if (clock != null) {
      clock.waitUntilStop();
    }
    jmsManagementService.stop();
    
    //logService.stopLog(); -- see Issue #1138
  }

  // ---------------- API contract -------------
  /**
   * Allows instances of TimeslotPhaseProcessor to register themselves
   * to be activated during one of the processing phases in each timeslot.
   */
  @Override
  public void registerTimeslotPhase (TimeslotPhaseProcessor thing, int phase)
  {
    if (phase <= 0 || phase > timeslotPhaseCount) {
      log.error("phase " + phase + " out of range (1.." +
                timeslotPhaseCount + ")");
    }
    else {
      if (phaseRegistrations == null) {
        phaseRegistrations = new ArrayList<List<TimeslotPhaseProcessor>>();
        for (int index = 0; index < timeslotPhaseCount; index++) {
          phaseRegistrations.add(new ArrayList<TimeslotPhaseProcessor>());
        }
      }
      log.info("register TimeslotPhaseProcessor {}, phase {}",
               thing.getClass().getName(), phase);
      phaseRegistrations.get(phase - 1).add(thing);
    }
  }

  /** True just in case the sim is running in bootstrap mode */
  @Override
  public boolean isBootstrapMode ()
  {
    return bootstrapMode;
  }

  // ------- pause-mode broker communication -------
  /**
   * Signals that the clock is paused due to server overrun. The pause
   * must be communicated to brokers.
   */
  public void pause ()
  {
    log.info("pause");
    // create and post the pause message
    SimPause msg = new SimPause();
    brokerProxyService.broadcastMessage(msg);
  }

  /**
   * Signals that the clock is resumed. Brokers must be informed of the new
   * start time in order to sync their own clocks.
   */
  public void resume (long newStart)
  {
    log.info("resume");
    // create and post the resume message
    SimResume msg = new SimResume(new Instant(newStart));
    brokerProxyService.broadcastMessage(msg);
  }

  String pauseRequester;

  /**
   * Allows a broker to request a pause. It may or may not be allowed.
   * If allowed, then the pause will take effect when the current simulation
   * cycle has finished, or immediately if no simulation cycle is currently
   * in progress.
   */
  public synchronized void handleMessage (PauseRequest msg)
  {
    if (!brokerPauseAllowed) {
      log.info("Pause request by " + msg.getBroker().getUsername()
               + " disallowed");
      return;
    }
    if (pauseRequester != null) {
      log.info("Pause request by " + msg.getBroker().getUsername() + 
               " rejected; already paused by " + pauseRequester);
      return;
    }
    pauseRequester = msg.getBroker().getUsername();
    log.info("Pause request by " + msg.getBroker().getUsername());
    clock.requestPause();
  }

  /**
   * Releases a broker-initiated pause. After the clock is re-started, the
   * resume() method will be called to communicate a new start time.
   */
  public synchronized void handleMessage (PauseRelease msg)
  {
    if (pauseRequester == null) {
      log.info("Release request by " + msg.getBroker().getUsername() + 
               ", but no pause currently requested");
      return;
    }
    if (pauseRequester != msg.getBroker().getUsername()) {
      log.info("Release request by " + msg.getBroker().getUsername() + 
               ", but pause request was by " + pauseRequester);
      return;
    }
    log.info("Pause released by " + msg.getBroker().getUsername());
    clock.releasePause();
    pauseRequester = null;
  }

  /**
   * Authenticate Broker.
   */
  public void handleMessage(BrokerAuthentication msg) {
    log.info("receiveMessage(BrokerAuthentication) " + msg.getUsername()
             + ", time offset = " + (msg.getBrokerTime() - new Date().getTime()));
    loginBroker(msg.getUsername());
  }

  /**
   * Allows Spring to set the boostrap timeslot length
   */
  public void setBootstrapTimeslotMillis (long length)
  {
    bootstrapTimeslotMillis = length;
  }

  long getBootstrapTimeslotMillis ()
  {
    return bootstrapTimeslotMillis;
  }

  /**
   * This is the simulation thread. It sets up the clock, waits for ticks,
   * and runs the processing steps. The thread can be stopped in an orderly
   * way simply by setting the running flag to false.
   */
  class SimRunner extends Thread
  {
    CompetitionControlService parent;
    int maxSequentialExceptions = 4;

    public SimRunner (CompetitionControlService instance)
    {
      super();
      parent = instance;
    }

    @Override
    public void run ()
    {
      int sequentialExceptions = 0;

      SimulationClockControl.initialize(parent, timeService);
      clock = SimulationClockControl.getInstance();
      clock.adjustAgentWindow(competition.getSimulationTimeslotSeconds());
      
      // wait for start time
      long now = new Date().getTime();
      // start is beginning of boot
      long startOffset = 0;
      if (!bootstrapMode) {
        // back up start to beginning of boot record
        startOffset = 
                bootstrapOffset
                * competition.getTimeslotDuration()
                / competition.getSimulationRate();
      }
      long start = now - startOffset + TimeService.SECOND * 3; // start in three seconds
      // communicate start time to brokers
      SimStart startMsg = new SimStart(new Instant(start));
      brokerProxyService.broadcastMessage(startMsg);

      // Start up the clock at the correct time, so first tick gets us to
      // the start time. In this case, the clock is currently set to the start
      // time, so this will back it up by one notch.
      clock.setStart(start);
      log.info("sim start at " + timeService.getCurrentTime());
      timeService.init(timeService.getCurrentTime());
      // run the simulation
      running = true;
      clock.scheduleTick();
      while (running) {
        log.info("Wait for tick {}", currentSlot);
        clock.waitForTick(currentSlot);
        try {
          step();
          sequentialExceptions = 0;
        }
        catch (Exception e) {
          try {
            StackTraceElement[] trace = e.getStackTrace();
            StringBuffer sb = new StringBuffer();
            sb.append(e.toString());
            int depth = Math.min(stackTraceDepth, trace.length);
            for (int index = 0; index < depth; index++) {
              sb.append("\n.. " + trace[index].toString());
            }
            log.error(sb.toString());
          }
          catch (Exception e1) {
            log.error("Exception " + e1.toString()
                      + " trying to log exception " + e.toString());
          }
          if (++sequentialExceptions >= maxSequentialExceptions)
            running = false;
        }
        currentSlot += 1;
        clock.complete();
      }
      // simulation is complete
      log.info("Stop simulation");
      clock.stop();
    }
  }

  // Test support
  List<String> getBrokerNames()
  {
    return brokerNames;
  }
}
