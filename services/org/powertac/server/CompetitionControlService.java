/*
 * Copyright 2011 the original author or authors.
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

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.RandomSeed;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.BootstrapDataCollector;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.PauseRelease;
import org.powertac.common.msg.PauseRequest;
import org.powertac.common.msg.SimEnd;
import org.powertac.common.msg.SimPause;
import org.powertac.common.msg.SimResume;
import org.powertac.common.msg.SimStart;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.DomainRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * This is the competition controller. It has three major roles in the
 * server:
 * <ol>
 * <li>At server startup, the <code>preGame()</code> method must be called to
 * set up the environment and allow configuration of the next game, through
 * a web (or REST) interface.</li>
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
  implements ApplicationContextAware, CompetitionControl, BrokerMessageListener
{
  static private Logger log = Logger.getLogger(CompetitionControlService.class.getName());
  
  private ApplicationContext applicationContext = null;

  private Competition competition; // convenience var, invalid across sessions
  private long competitionId;

  private int timeslotPhaseCount = 4; // # of phases/timeslot
  private boolean running = false;

  private SimulationClockControl clock;
  
  @Autowired
  private TimeService timeService; // inject simulation time service dependency
  //def jmsManagementService
  //def springSecurityService

  @Autowired
  private BrokerProxy brokerProxyService;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private LogService logService;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private BootstrapDataCollector defaultBroker;
  
  @Autowired
  private XMLMessageConverter messageConverter;

  private ArrayList<List<TimeslotPhaseProcessor>> phaseRegistrations;
  private int timeslotCount = 0;
  private long timeslotMillis;
  private RandomSeed randomGen;
  
  // if we don't have a bootstrap dataset, we are in bootstrap mode.
  private boolean bootstrapMode = true;
  
  private boolean simRunning = false;

  /**
   * Pre-game server setup - creates the basic configuration elements
   * to make them accessible to the web-based game-setup functions.
   * This method must be called when the server is started, and again at
   * the completion of each simulation. The actual simulation is started
   * with a call to init().
   */
  public void preGame ()
  {
    // Create default competition
    competition = Competition.newInstance("defaultCompetition");
    competitionId = competition.getId();
    logService.startLog(competitionId);

    // Set up all the plugin configurations
    log.info("pre-game initialization");

    phaseRegistrations = null;

    // Handle pre-game initializations by clearing out the repos,
    // then creating the PluginConfig instances
    Map<String, DomainRepo> repos =
      applicationContext.getBeansOfType(DomainRepo.class);
    for (DomainRepo repo : repos.values()) {
      repo.recycle();
    }
    Map<String, InitializationService> initializers =
      applicationContext.getBeansOfType(InitializationService.class);
    for (InitializationService init : initializers.values()) {
      init.setDefaults();
    }

    // add broker message registration
    brokerProxyService.registerSimListener(this);

    // configure competition instance
    for (PluginConfig config : pluginConfigRepo.list()) {
      log.info("adding plugin " + config.toString());
      competition.addPluginConfig(config);
    }
  }
  
  /**
   * Sets up the simulator, with config overrides provided in a file
   * containing a sequence of PluginConfig instances. Errors are logged
   * if one or more PluginConfig instances cannot be used in the current
   * server setup.
   */
  public void preGame (FileReader config)
  {
    // TODO -- implement this
    log.error("Cannot handle pre-game config file");
  }

  /**
   * Runs the initialization process, starts the simulation thread, waits
   * for it to complete, then shuts down and prepares for the next simulation
   * run. This is probably only useful for a web-interface controller.
   */
  public void init ()
  {
    runOnce();
    // prepare for the next run
    logService.stopLog();
    preGame();
  }
  
  public void runOnce ()
  {
    // to enhance testability, initialization is split into a static setup()
    // phase, followed by calling runSimulation() to start the sim thread.
    if (simRunning) {
      log.warn("attempt to start sim on top of running sim");
      return;
    }
    // -- note small race condition here --
    simRunning = true;
    if (competition == null) {
      log.error("null competition instance for id $competitionId");
    }
    if (!setup()) {
      simRunning = false;
      return;
    }
    
    // run the simulation, wait for completion
    runSimulation((long) (competition.getTimeslotLength() * TimeService.MINUTE /
		  competition.getSimulationRate()));

    // wrap up
    shutDown();
    simRunning = false;
  }
  
  /**
   * Loads a bootstrap dataset, starts a simulation
   */
  public void runOnce (FileReader datasetReader)
  {
    // load the dataset
    // verify dataset against current server setup
    
    // turn off bootstrap mode and start the sim
    bootstrapMode = false;
    runOnce();
  }
  
  /**
   * Runs a bootstrap sim, saves the bootstrap dataset in a file
   */
  public void runOnce (FileWriter datasetWriter)
  {
    bootstrapMode = true;
    runOnce();
    // save the dataset
    BufferedWriter output = new BufferedWriter(datasetWriter);
    List<Object> data = defaultBroker.collectBootstrapData();
    try {
      for (Object item : data) {
        String xml = messageConverter.toXML(item);
        output.write(xml);
      }
      output.close();
    }
    catch (IOException ioe) {
      log.error("Error writing bootstrap file: " + ioe.toString());
    }
  }

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
    Instant time = timeService.getCurrentTime();
    activateNextTimeslot();
    log.info("step at " + time.toString());
    for (int index = 0; index < phaseRegistrations.size(); index++) {
      log.info("activate phase " + (index + 1));
      for (TimeslotPhaseProcessor fn : phaseRegistrations.get(index)) {
        fn.activate(time, index + 1);
      }
    }
    if (--timeslotCount <= 0) {
      log.info("Stopping simulation");
      stop();
    }
  }

  /**
   * Allows instances of TimeslotPhaseProcessor to register themselves
   * to be activated during one of the processing phases in each timeslot.
   */
  public void registerTimeslotPhase (TimeslotPhaseProcessor thing, int phase)
  {
    if (phase <= 0 || phase > timeslotPhaseCount) {
      log.error("phase ${phase} out of range (1..${timeslotPhaseCount})");
    }
    else {
      if (phaseRegistrations == null) {
        phaseRegistrations = new ArrayList<List<TimeslotPhaseProcessor>>();
        for (int index = 0; index < timeslotPhaseCount; index++) {
          phaseRegistrations.add(new ArrayList<TimeslotPhaseProcessor>());
        }
      }
      phaseRegistrations.get(phase - 1).add(thing);
    }
  }

  /**
   * Signals the simulation thread to stop after processing is completed in
   * the current timeslot.
   */
  public void stop ()
  {
    running = false;
  }

  /**
   * Shuts down the simulation and cleans up.
   */
  private void shutDown ()
  {
    running = false;

    SimEnd endMsg = new SimEnd();
    brokerProxyService.broadcastMessage(endMsg);
  }

  /** True just in case the sim is running in bootstrap mode */
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
  public void receiveMessage (PauseRequest msg)
  {
    if (pauseRequester != null) {
      log.info("Pause request by ${msg.broker.username} rejected; already paused by ${pauseRequester}");
      return;
    }
    pauseRequester = msg.getBroker().getUsername();
    log.info("Pause request by ${msg.broker.username}");
    clock.requestPause();
  }
  
  /**
   * Releases a broker-initiated pause. After the clock is re-started, the
   * resume() method will be called to communicate a new start time.
   */
  public void receiveMessage (PauseRelease msg)
  {
    if (pauseRequester == null) {
      log.info("Release request by ${msg.broker.username}, but no pause currently requested");
      return;
    }
    if (pauseRequester != msg.getBroker().getUsername()) {
      log.info("Release request by ${msg.broker.username}, but pause request was by ${pauseRequester}");
      return;
    }
    log.info("Pause released by ${msg.broker.username}");
    clock.releasePause();
    pauseRequester = null;
  }

  //--------- local methods -------------

  private boolean setup ()
  {
    setTimeParameters();

    // set up random sequence for CCS
    randomGen = randomSeedRepo.getRandomSeed("CompetitionControlService",
                                             competition.getId(),
                                             "game-setup");

    // TODO set up broker queues -- they should be logged in already
    //jmsManagementService.createQueues()

    // Publish Competition object at right place - after plugins
    // are initialized. This is necessary because some may need to
    // see the broadcast after they are initialized (visualizer, for example)
    for (String retailer : brokerRepo.findRetailBrokerNames()) {
      competition.addBroker(retailer);
    }
    
    // configure plugins, but don't allow them to broadcast to brokers
    brokerProxyService.setDeferredBroadcast(true);
    if (!configurePlugins()) {
      log.error("failed to configure plugins");
      return false;
    }
    // send the Competition instance, then broadcast deferred messages
    brokerProxyService.setDeferredBroadcast(false);
    brokerProxyService.broadcastMessage(competition);
    if (!bootstrapMode) {
      broadcastBootstrapDataset();
    }
    brokerProxyService.broadcastDeferredMessages();

    // sim length for bootstrap mode comes from the competition instance;
    // for non-bootstrap mode, it is computed from competition parameters.
    if (!bootstrapMode) {
      timeslotCount = computeGameLength(competition.getMinimumTimeslotCount(),
                                        competition.getExpectedTimeslotCount());
    }
    else {
      timeslotCount = competition.getBootstrapTimeslotCount();
    }

    // grab setup parameters, set up initial timeslots, including zero timeslot
    timeslotMillis = competition.getTimeslotLength() * TimeService.MINUTE;
    createInitialTimeslots(competition.getSimulationBaseTime(),
                           competition.getDeactivateTimeslotsAhead(),
                           competition.getTimeslotsOpen());
    TimeslotUpdate msg = new TimeslotUpdate(timeService.getCurrentTime(),
                                            timeslotRepo.enabledTimeslots());
    brokerProxyService.broadcastMessage(msg);
    return true;
  }

  // set simulation time parameters, making sure that simulationStartTime
  // is still sufficiently in the future.
  private void setTimeParameters()
  {
    long rate = competition.getSimulationRate();
    long rem = rate % competition.getTimeslotLength();
    if (rem > 0) {
      long mult = competition.getSimulationRate() / competition.getTimeslotLength();
      log.warn("Simulation rate " + rate + 
               " not a multiple of " + competition.getTimeslotLength() + 
               "; adjust to " + (mult + 1) * competition.getTimeslotLength());
      rate = (mult + 1) * competition.getTimeslotLength();
    }
    timeService.setClockParameters(competition.getSimulationBaseTime().getMillis(),
                                   rate,
                                   competition.getTimeslotLength() * TimeService.MINUTE);
    timeService.setCurrentTime(competition.getSimulationBaseTime());
  }

  //
  private void broadcastBootstrapDataset ()
  {
    
  }
  
  // Creates the initial complement of timeslots
  private void createInitialTimeslots (Instant base,
                                                 int initialSlots,
                                                 int openSlots)
  {
    for (int i = 0; i < initialSlots - 1; i++) {
      Timeslot ts = timeslotRepo.makeTimeslot(base.plus(i * timeslotMillis),
                                              base.plus((i + 1) * timeslotMillis));
      ts.disable();
    }
    for (int i = initialSlots - 1; i < (initialSlots + openSlots - 1); i++) {
      timeslotRepo.makeTimeslot(base.plus(i * timeslotMillis),
                                              base.plus((i + 1) * timeslotMillis));
    }
  }

  private void activateNextTimeslot ()
  {
    TimeslotUpdate msg;
    // first, deactivate the oldest active timeslot
    Timeslot current = timeslotRepo.currentTimeslot();
    if (current == null) {
      log.error("current timeslot is null at " + timeService.getCurrentTime());
      return;
    }
    int oldSerial = (current.getSerialNumber() +
            competition.getDeactivateTimeslotsAhead());
    Timeslot oldTs = timeslotRepo.findBySerialNumber(oldSerial);
    oldTs.disable();
    log.info("Deactivated timeslot " + oldSerial + ", start " + oldTs.getStartInstant().toString());

    // then create if necessary and activate the newest timeslot
    int newSerial = (current.getSerialNumber() +
            competition.getDeactivateTimeslotsAhead() +
            competition.getTimeslotsOpen());
    Timeslot newTs = timeslotRepo.findBySerialNumber(newSerial);
    if (newTs == null) {
      long start = (current.getStartInstant().getMillis() +
              (newSerial - current.getSerialNumber()) * timeslotMillis);
      newTs = timeslotRepo.makeTimeslot(new Instant(start), new Instant(start + timeslotMillis));
    }
    else {
      newTs.enable();
    }
    log.info("Activated timeslot " + newSerial + ", start " + newTs.getStartInstant());
    // Communicate timeslot updates to brokers
    msg = new TimeslotUpdate(timeService.getCurrentTime(),
                             timeslotRepo.enabledTimeslots());
    brokerProxyService.broadcastMessage(msg);
  }

  private int computeGameLength (int minLength, int expLength)
  {
    double roll = randomGen.nextDouble();
    // compute k = ln(1-roll)/ln(1-p) where p = 1/(exp-min)
    double k = (Math.log(1.0 - roll) /
		Math.log(1.0 - 1.0 /
			 (expLength - minLength + 1)));
    int length = minLength + (int) Math.floor(k);
    log.info("game-length " + length + "(k=" + k + ", roll=" + roll + ")");
    return length;
  }

  private boolean configurePlugins ()
  {
    Map<String, InitializationService> initializers =
      applicationContext.getBeansOfType(InitializationService.class);

    ArrayList<String> completedPlugins = new ArrayList<String>();
    ArrayList<InitializationService> deferredInitializers = new ArrayList<InitializationService>();
    for (InitializationService initializer : initializers.values()) {
      String success = initializer.initialize(competition, completedPlugins);
      if (success == null) {
        // defer this one
        log.info("deferring " + initializer.toString());
        deferredInitializers.add(initializer);
      }
      else if (success == "fail") {
        log.error("Failed to initialize plugin " + initializer.toString());
        return false;
      }
      else {
        log.info("completed " + success);
        completedPlugins.add(success);
      }
    }
    int tryCounter = deferredInitializers.size();
    List<InitializationService> remaining = deferredInitializers;
    while (remaining.size() > 0 && tryCounter > 0) {
      InitializationService initializer = remaining.get(0);
      if (remaining.size() > 1) {
        remaining.remove(0);
      }
      else {
        remaining.clear();
      }
      String success = initializer.initialize(competition, completedPlugins);
      if (success == null) {
        // defer this one
        log.info("deferring " + initializer.toString());
        remaining.add(initializer);
        tryCounter -= 1;
      }
      else {
        log.info("completed " + success);
        completedPlugins.add(success);
      }
    }
    for (InitializationService initializer : remaining) {
      log.error("Failed to initialize " + initializer.toString());
    }
    return true;
  }

  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException
  {
    this.applicationContext = applicationContext;
  }
  
  /**
   * This is the simulation thread. It sets up the clock, waits for ticks,
   * and runs the processing steps. The thread can be stopped in an orderly
   * way simply by setting the running flag to false.
   */
  class SimRunner extends Thread
  {
    CompetitionControlService parent;
    
    public SimRunner (CompetitionControlService instance)
    {
      super();
      parent = instance;
    }
    
    public void run ()
    {
      SimulationClockControl.initialize(parent, timeService);
      clock = SimulationClockControl.getInstance();
      // wait for start time
      long now = new Date().getTime();
      //long start = now + scheduleMillis * 2 - now % scheduleMillis
      long start = now + TimeService.SECOND; // start in one second
      // communicate start time to brokers
      SimStart startMsg = new SimStart(new Instant(start));
      brokerProxyService.broadcastMessage(startMsg);

      // Start up the clock at the correct time
      clock.setStart(start);
      timeService.init();
      // run the simulation
      running = true;
      int slot = 0;
      clock.scheduleTick();
      while (running) {
        log.info("Wait for tick " + slot);
        clock.waitForTick(slot++);
        step();
        clock.complete();
      }
      // simulation is complete
      log.info("Stop simulation");
      clock.stop();
    }
  }

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.BrokerMessageListener#receiveMessage(java.lang.Object)
   */
  public void receiveMessage(Object msg)
  {
    if (msg instanceof PauseRelease) {
      receiveMessage((PauseRelease)msg);
    } else if (msg instanceof PauseRequest) {
      receiveMessage((PauseRequest)msg);
    } else {
      log.error("receiveMessage - unexpected message:" + msg);
    }
  }
}
