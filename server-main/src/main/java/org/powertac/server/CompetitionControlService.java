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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
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
import org.powertac.common.msg.BrokerAccept;
import org.powertac.common.msg.BrokerAuthentication;
import org.powertac.common.msg.PauseRelease;
import org.powertac.common.msg.PauseRequest;
import org.powertac.common.msg.SimEnd;
import org.powertac.common.msg.SimPause;
import org.powertac.common.msg.SimResume;
import org.powertac.common.msg.SimStart;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.DomainRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
  private CustomerRepo customerRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private BootstrapDataCollector defaultBroker;
  
  @Autowired
  private XMLMessageConverter messageConverter;
  
  @Autowired
  private JmsManagementService jmsManagementService;
  
  @Autowired
  private ServerMessageReceiver serverMessageReceiver;

  private ArrayList<List<TimeslotPhaseProcessor>> phaseRegistrations;
  private int timeslotCount = 0;
  private int currentSlot = 0;
  private int currentSlotOffset = 0;
  private RandomSeed randomGen; // used to compute game length

  // broker interaction state
  private ArrayList<String> alwaysAuthorizedBrokers;
  private ArrayList<String> authorizedBrokerList;
  private int idPrefix = 0;
  
  // Server JMS Queue Name
  private String serverQueueName = "serverInput";
  
  // if we don't have a bootstrap dataset, we are in bootstrap mode.
  private boolean bootstrapMode = true;
  private List<Object> bootstrapDataset = null;
  private long bootstrapTimeslotMillis = 2000;
  private int bootstrapDiscardedTimeslots = 24;
  
  private boolean simRunning = false;

  /**
   * Pre-game server setup - creates the basic configuration elements
   * to make them accessible to the web-based game-setup functions.
   * This method must be called when the server is started, and again at
   * the completion of each simulation. The actual simulation is started
   * with a call to init().
   */
  @SuppressWarnings("unchecked")
  public void preGame ()
  {
    // Create default competition
    competition = Competition.newInstance("defaultCompetition");
    competitionId = competition.getId();
    logService.startLog(competitionId);
    log.setLevel(Level.DEBUG);

    // Set up all the plugin configurations
    log.info("pre-game initialization");

    phaseRegistrations = null;
    idPrefix = 0;

    // Handle pre-game initializations by clearing out the repos,
    // then creating the PluginConfig instances
    Map<String, DomainRepo> repos =
      applicationContext.getBeansOfType(DomainRepo.class);
    log.debug("found " + repos.size() + " repos");
    for (DomainRepo repo : repos.values()) {
      repo.recycle();
    }
    Map<String, InitializationService> initializers =
      applicationContext.getBeansOfType(InitializationService.class);
    log.debug("found " + initializers.size() + " initializers");
    for (InitializationService init : initializers.values()) {
      init.setDefaults();
    }

    // register with JMS Server
    jmsManagementService.registerMessageListener(serverQueueName, serverMessageReceiver);
    
    // broker message registration for clock-control messages
    brokerProxyService.registerSimListener(this);
  }
  
  /**
   * Sets up the simulator, with config overrides provided in a file
   * containing a sequence of PluginConfig instances. Errors are logged
   * if one or more PluginConfig instances cannot be used in the current
   * server setup.
   */
  public boolean preGame (File bootFile)
  {
    // turn off bootstrap mode, and run the basic pre-game setup
    bootstrapMode = false;
    preGame();
    
    // read the config info from the bootReader - 
    // We need to find a Competition and a set of PluginConfig instances
    Competition bootstrapCompetition = null;
    ArrayList<PluginConfig> configList = new ArrayList<PluginConfig>();
    //InputSource source = new InputSource(bootReader);
    XPathFactory factory = XPathFactory.newInstance();
    XPath xPath = factory.newXPath();
    try {
      // first grab the bootstrap offset
      XPathExpression exp =
          xPath.compile("/powertac-bootstrap-data/config/bootstrap-offset/@value");
      NodeList nodes = (NodeList)exp.evaluate(new InputSource(new FileReader(bootFile)),
                                              XPathConstants.NODESET);
      String value = nodes.item(0).getNodeValue();
      bootstrapDiscardedTimeslots = Integer.parseInt(value);
      log.info("offset: " + bootstrapDiscardedTimeslots + " timeslots");
      
      // then pull out the Competition
      exp =
          xPath.compile("/powertac-bootstrap-data/config/competition");
      nodes = (NodeList)exp.evaluate(new InputSource(new FileReader(bootFile)),
                                     XPathConstants.NODESET);
      String xml = nodeToString(nodes.item(0));
      bootstrapCompetition = (Competition)messageConverter.fromXML(xml);

      // then get the configs
      exp = xPath.compile("/powertac-bootstrap-data/config/plugin-config");
      nodes = (NodeList)exp.evaluate(new InputSource(new FileReader(bootFile)),
                                     XPathConstants.NODESET);
      // Each node is a plugin-config
      for (int i = 0; i < nodes.getLength(); i++) {
        Node node = nodes.item(i);
        xml = nodeToString(node);
        PluginConfig pic = (PluginConfig)messageConverter.fromXML(xml);
        configList.add(pic);
      }
    }
    catch (XPathExpressionException xee) {
      log.error("preGame: Error reading config file: " + xee.toString());
      System.out.println("Error reading config file: " + xee.toString());
      return false;
    }
    catch (IOException ioe) {
      log.error("preGame: Error opening file " + bootFile + ": " + ioe.toString());
    }
    // update the existing Competition - should be the current competition
    Competition.currentCompetition().update(bootstrapCompetition);
    
    // update the existing config, and make sure the bootReader has the
    // same set of PluginConfig instances as the running server
    for (Iterator<PluginConfig> pics = configList.iterator(); pics.hasNext(); ) {
      // find the matching one in the server and update it, then remove
      // the current element from the configList
      PluginConfig next = pics.next();
      PluginConfig match = pluginConfigRepo.findMatching(next);
      if (match == null) {
        // there's a pic in the file that's not in the server
        log.error("no matching PluginConfig found for " + next.toString());
        System.out.println("no matching PluginConfig found for " + next.toString());
        return false;
      }
      // if we found it, then we need to update it.
      match.update(next);
    }
    
    // Create the timeslots from the bootstrap period - they will be needed to 
    // instantiate weather reports. All are disabled.
    currentSlotOffset = competition.getBootstrapTimeslotCount()
                        + bootstrapDiscardedTimeslots;
    createInitialTimeslots(competition.getSimulationBaseTime(),
                           (currentSlotOffset + 1),
                           0);
    log.info("created " + timeslotRepo.count() + " timeslots");
    
    // we currently ignore cases where there's a config in the server that's
    // not in the file; there might be use cases for which this would
    // be useful.
    return true;
  }

  // Converts an xml node into a string that can be converted by XStream
  private String nodeToString(Node node) {
    StringWriter sw = new StringWriter();
    try {
      Transformer t = TransformerFactory.newInstance().newTransformer();
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      t.setOutputProperty(OutputKeys.INDENT, "no");
      t.transform(new DOMSource(node), new StreamResult(sw));
    }
    catch (TransformerException te) {
      log.error("nodeToString Transformer Exception " + te.toString());
    }
    String result = sw.toString();
    //log.info("xml node: " + result);
    return result;
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
    preGame();
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
  public void setAuthorizedBrokerList (ArrayList<String> brokerList)
  {
    authorizedBrokerList = new ArrayList<String>(alwaysAuthorizedBrokers);
    for (String broker : brokerList) {
      authorizedBrokerList.add(broker);
    }
  }
  
  /**
   * Sets the number of timeslots to discard at the beginning of a bootstrap
   * run. Length of the sim will be the bootstrap length plus this length.
   * Default value is 24.
   */
  public void setBootstrapDiscardedTimeslots (int count)
  {
    bootstrapDiscardedTimeslots = count;
  }
  
  /**
   * Runs a simulation that is already set up. This is intended to be called
   * from a method that knows whether we are running a bootstrap sim or a 
   * normal sim.
   */
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
      log.error("null competition instance");
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
    logService.stopLog();
  }
  
  /**
   * Loads a bootstrap dataset, starts a simulation
   */
  public void runOnce (File datasetFile)
  {
    // load the bootstrap dataset, and start the sim
    bootstrapMode = false;
    bootstrapDataset = new ArrayList<Object>();
    XPathFactory factory = XPathFactory.newInstance();
    XPath xPath = factory.newXPath();
    try {
      InputSource source = new InputSource(new FileReader(datasetFile));
      // we want all the children of the bootstrap node
      XPathExpression exp =
          xPath.compile("/powertac-bootstrap-data/bootstrap/*");
      NodeList nodes = (NodeList)exp.evaluate(source, XPathConstants.NODESET);
      log.info("Found " + nodes.getLength() + " bootstrap nodes");
      // Each node is a bootstrap data item
      for (int i = 0; i < nodes.getLength(); i++) {
        String xml = nodeToString(nodes.item(i));
        Object msg = messageConverter.fromXML(xml);
        bootstrapDataset.add(msg);
      }
    }
    catch (XPathExpressionException xee) {
      log.error("runOnce: Error reading config file: " + xee.toString());
    }
    catch (IOException ioe) {
      log.error("runOnce: reset fault: " + ioe.toString());
    }
    runOnce();
  }
  
  /**
   * Runs a bootstrap sim, saves the bootstrap dataset in a file
   */
  public void runOnce (Writer datasetWriter)
  {
    bootstrapMode = true;
    runOnce();
    // save the dataset
    saveBootstrapData(datasetWriter);
  }

  // method broken out to simplify testing
  void saveBootstrapData (Writer datasetWriter)
  {
    BufferedWriter output = new BufferedWriter(datasetWriter);
    List<Object> data = 
        defaultBroker.collectBootstrapData(competition.getBootstrapTimeslotCount());
    try {
      // write the config data
      output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      output.newLine();
      output.write("<powertac-bootstrap-data>");
      output.newLine();
      output.write("<config>");
      output.newLine();
      // bootstrap offset
      output.write("<bootstrap-offset value=\"" 
                   + bootstrapDiscardedTimeslots + "\" />");
      output.newLine();
      // current competition
      output.write(messageConverter.toXML(competition));
      output.newLine();
      // next the PluginConfig instances
      for (PluginConfig pic : pluginConfigRepo.list()) {
        output.write(messageConverter.toXML(pic));
        output.newLine();
      }
      output.write("</config>");
      output.newLine();
      // finally the bootstrap data
      output.write("<bootstrap>");
      output.newLine();
      for (Object item : data) {
        output.write(messageConverter.toXML(item));
        output.newLine();
      }
      output.write("</bootstrap>");
      output.newLine();
      output.write("</powertac-bootstrap-data>");
      output.newLine();
      output.close();
    }
    catch (IOException ioe) {
      log.error("Error writing bootstrap file: " + ioe.toString());
    }
  }

  // ------------------ simulation setup -------------------
  // Sets up simulation state in preparation for a sim run. At this point,
  // all brokers should already be logged in
  private boolean setup ()
  {
    // set up random sequence for new simulation run
    randomGen = randomSeedRepo.getRandomSeed("CompetitionControlService",
                                             competition.getId(),
                                             "game-setup");

    // set up the simulation clock
    setTimeParameters();
    //log.info("start at timeslot " + timeslotRepo.currentTimeslot().getSerialNumber());
    
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

    // get brokers logged in
    //jmsManagementService.createQueues()
    waitForBrokerLogin();

    // Publish Competition object at right place - after plugins
    // are initialized. This is necessary because some may need to
    // see the broadcast after they are initialized (visualizer, for example)
    for (String retailer : brokerRepo.findRetailBrokerNames()) {
      competition.addBroker(retailer);
    }
    
    // send the Competition instance, then the public PluginConfig instances,
    // and finally broadcast deferred messages
    brokerProxyService.setDeferredBroadcast(false);
    brokerProxyService.broadcastMessage(competition);
    brokerProxyService.broadcastMessages(pluginConfigRepo.findAllPublic());
    if (!bootstrapMode) {
      brokerProxyService.broadcastMessages(bootstrapDataset);
    }
    brokerProxyService.broadcastDeferredMessages();

    // sim length for bootstrap mode comes from the competition instance;
    // for non-bootstrap mode, it is computed from competition parameters.
    if (!bootstrapMode) {
      timeslotCount = computeGameLength(competition.getMinimumTimeslotCount(),
                                        competition.getExpectedTimeslotCount());
    }
    else {
      timeslotCount = currentSlotOffset;
    }

    // Send out the first timeslot update
    TimeslotUpdate msg = new TimeslotUpdate(timeService.getCurrentTime(),
                                            timeslotRepo.enabledTimeslots());
    brokerProxyService.broadcastMessage(msg);
    return true;
  }
  
  // blocks until all brokers have logged in.
  // TODO -- add a timeout
  private synchronized void waitForBrokerLogin ()
  {
    if (authorizedBrokerList == null || authorizedBrokerList.size() == 0) {
      // nothing to do here
      return;
    }
    if (log.isInfoEnabled()) {
      StringBuffer msg = new StringBuffer();
      msg.append("waiting for logins from");
      for (String name : authorizedBrokerList) {
        msg.append(" ").append(name);
      }
      log.info(msg.toString());
    }
    try {
      while (authorizedBrokerList.size() > 0) {
        wait();
      }  
    }
    catch (InterruptedException ie) {
      authorizedBrokerList.clear();
    }
  }
  
  /**
   * Logs in a broker, just in case the broker is on the authorizedBrokerList.
   * Returns true if the broker is authorized, otherwise false.
   */
  public synchronized boolean loginBroker (String username)
  {
    // cannot log in if there's no list, or if the broker is not on the list
    if (authorizedBrokerList == null
        || authorizedBrokerList.size() == 0
        || !authorizedBrokerList.contains(username)) {
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
    log.info("Log in broker " + username);
    Broker broker = brokerRepo.findByUsername(username);
    if (broker == null) {
      broker = new Broker(username);
      brokerRepo.add(broker);
      // assign prefix...
      brokerProxyService.sendMessage(broker, new BrokerAccept(++idPrefix));
    }
    
    // only enabled brokers get messages
    broker.setEnabled(true);
    
    // clear the broker from the list, and if the list is now empty, then
    // notify the simulation to start
    authorizedBrokerList.remove(username);
    if (authorizedBrokerList.size() == 0) {
      notifyAll();
    }
    return true;
  }
  
  // set simulation time parameters, making sure that simulationStartTime
  // is still sufficiently in the future.
  private void setTimeParameters()
  {
    Instant base = competition.getSimulationBaseTime();
    // if we are not in bootstrap mode, we have to add the bootstrap interval
    // to the base
    long rate = competition.getSimulationRate();
    if (!bootstrapMode) {
      int slotCount = (currentSlotOffset);
      log.info("first slot: " + slotCount);
      base = base.plus(slotCount * competition.getTimeslotDuration());
    }
    else {
      // compute rate from bootstrapTimeslotMillis
      rate = competition.getTimeslotDuration() / bootstrapTimeslotMillis;
    }
    long rem = rate % competition.getTimeslotLength();
    if (rem > 0) {
      long mult = competition.getSimulationRate() / competition.getTimeslotLength();
      log.warn("Simulation rate " + rate + 
               " not a multiple of " + competition.getTimeslotLength() + 
               "; adjust to " + (mult + 1) * competition.getTimeslotLength());
      rate = (mult + 1) * competition.getTimeslotLength();
    }
    timeService.setClockParameters(base.getMillis(), rate,
                                   competition.getTimeslotDuration());
    timeService.setCurrentTime(base);
  }

  // Computes a random game length as outlined in the game specification
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

  // Runs the initialization protocol on each plugin, supports precedence
  // relationships among them.
  @SuppressWarnings("unchecked")
  private boolean configurePlugins ()
  {
    Map<String, InitializationService> initializers =
      applicationContext.getBeansOfType(InitializationService.class);

    ArrayList<String> completedPlugins = new ArrayList<String>();
    ArrayList<InitializationService> deferredInitializers = new ArrayList<InitializationService>();
    for (InitializationService initializer : initializers.values()) {
      log.info("attempt to initialize " + initializer.toString());
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
      log.info("additional attempt to initialize " + initializer.toString());
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

  // Creates the initial complement of timeslots
  private void createInitialTimeslots (Instant base,
                                       int initialSlots,
                                       int openSlots)
  {
    long timeslotMillis = competition.getTimeslotDuration();
    // set timeslot index according to bootstrap mode
    for (int i = 0; i < initialSlots - 1; i++) {
      Timeslot ts = timeslotRepo.makeTimeslot(base.plus(i * timeslotMillis));
      ts.disable();
    }
    for (int i = initialSlots - 1; i < (initialSlots + openSlots - 1); i++) {
      timeslotRepo.makeTimeslot(base.plus(i * timeslotMillis));
    }
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
    Instant time = timeService.getCurrentTime();
    Date started = new Date();
    activateNextTimeslot();
    log.info("step at " + time.toString());
    for (int index = 0; index < phaseRegistrations.size(); index++) {
      log.info("activate phase " + (index + 1));
      for (TimeslotPhaseProcessor fn : phaseRegistrations.get(index)) {
        fn.activate(time, index + 1);
      }
    }
    Date ended = new Date();
    log.info("Elapsed time: " + (ended.getTime() - started.getTime()));
    if (--timeslotCount <= 0) {
      log.info("Stopping simulation");
      stop();
    }
  }

  // activates the next timeslot - called once/timeslot
  private void activateNextTimeslot ()
  {
    long timeslotMillis = competition.getTimeslotDuration();
    TimeslotUpdate msg;
    // first, deactivate the oldest active timeslot
    // remember that this runs at the beginning of a timeslot, so the current
    // timeslot is the first one we consider.
    Timeslot current = timeslotRepo.currentTimeslot();
    if (current == null) {
      log.error("current timeslot is null at " + timeService.getCurrentTime());
      return;
    }
    if (current.getSerialNumber() != currentSlot + currentSlotOffset) {
      log.error("current timeslot serial is " + current.getSerialNumber() +
                ", should be " + (currentSlot + currentSlotOffset));
    }
    int oldSerial = (current.getSerialNumber() +
            competition.getDeactivateTimeslotsAhead() - 1);
    Timeslot oldTs = timeslotRepo.findBySerialNumber(oldSerial);
    oldTs.disable();
    log.info("Deactivated timeslot " + oldSerial + ", start " + oldTs.getStartInstant().toString());

    // then create if necessary and activate the newest timeslot
    int newSerial = (current.getSerialNumber() +
            competition.getDeactivateTimeslotsAhead() - 1 +
            competition.getTimeslotsOpen());
    Timeslot newTs = timeslotRepo.findBySerialNumber(newSerial);
    if (newTs == null) {
      long start = (current.getStartInstant().getMillis() +
              (newSerial - current.getSerialNumber()) * timeslotMillis);
      newTs = timeslotRepo.makeTimeslot(new Instant(start));
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

  // ------------ simulation shutdown ------------
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

  // ---------------- API contract -------------
  /**
   * Allows instances of TimeslotPhaseProcessor to register themselves
   * to be activated during one of the processing phases in each timeslot.
   */
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
      phaseRegistrations.get(phase - 1).add(thing);
    }
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
  
  /**
   * Authenticate Broker.
   * TODO: add auth-token processing
   */
  public void receiveMessage(BrokerAuthentication msg) {
    Broker broker = msg.getBroker();
    if (broker != null) {
      loginBroker(broker.getUsername());
    }
  }
  
  /**
   * Allows Spring to set the boostrap timeslot length
   */
  public void setBootstrapTimeslotMillis (long length)
  {
    bootstrapTimeslotMillis = length;
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
      clock.scheduleTick();
      while (running) {
        log.info("Wait for tick " + currentSlot);
        clock.waitForTick(currentSlot);
        step();
        currentSlot += 1;
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
    } else if (msg instanceof BrokerAuthentication) {
      receiveMessage((BrokerAuthentication)msg);
    } else {
      log.error("receiveMessage - unexpected message:" + msg);
    }
  }
}
