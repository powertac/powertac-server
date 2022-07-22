/*
 * Copyright (c) 2011 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.server;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.Competition;
import org.powertac.common.IdGenerator;
import org.powertac.common.RandomSeed;
import org.powertac.common.TimeService;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.BootstrapDataCollector;
import org.powertac.common.interfaces.BootstrapState;
import org.powertac.common.interfaces.CompetitionSetup;
import org.powertac.common.msg.MarketBootstrapData;
import org.powertac.common.repo.BootstrapDataRepo;
import org.powertac.common.repo.DomainRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.logtool.LogtoolContext;
import org.powertac.logtool.LogtoolCore;
import org.powertac.logtool.common.NewObjectListener;
import org.powertac.logtool.ifc.Analyzer;
import org.powertac.logtool.ifc.ObjectReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Manages command-line and file processing for pre-game simulation setup. 
 * A simulation can be started in one of two ways:
 * <ul>
 * <li>By running the process with command-line arguments as specified in
 * {@link PowerTacServer}, or</li>
 * <li> by calling the <code>preGame()</code> method to
 * set up the environment and allow configuration of the next game, through
 * a web (or REST) interface.</li>
 * </ul>
 * @author John Collins
 */
@Service
public class CompetitionSetupService
  implements CompetitionSetup//, ApplicationContextAware
{
  static private Logger log = LogManager.getLogger(CompetitionSetupService.class);
  static private ApplicationContext context;

  @Autowired
  private CompetitionControlService cc;

  @Autowired
  private BootstrapDataCollector defaultBroker;

  @Autowired
  private ServerPropertiesService serverProps;

  @Autowired
  private BootstrapDataRepo bootstrapDataRepo;

  @Autowired
  private LogtoolCore logtoolCore;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private MessageRouter messageRouter;

  @Autowired
  private XMLMessageConverter messageConverter;

  @Autowired
  private LogService logService;
  
  @Autowired
  private TournamentSchedulerService tss;
  
  @Autowired
  private TimeService timeService;

  private Competition competition;
  private SeedLoader seedLoader;
  //private int sessionCount = 0;
  private String gameId = null;
  private URL controllerURL;
  private Thread session = null;

  /**
   * Standard constructor
   */
  public CompetitionSetupService ()
  {
    super();
  }

  /**
   * Processes command-line arguments, which means looking for the specified 
   * script file and processing that
   */
  public void processCmdLine (String[] args)
  {
    // if there's an "abort" file sitting around, delete it
    File abortFile = new File("abort");
    if (abortFile.exists()) {
      abortFile.delete();
    }

    // pick up and process the command-line arg if it's there
    if (args.length > 1) {
      // cli setup
      processCli(args);
      waitForSession();
    }
  }

  private void waitForSession ()
  {
    if (session != null)
      try {
        session.join();
      }
      catch (InterruptedException e) {
        System.out.println("Error waiting for session completion: " + e.toString());
      }
  }

  // handles the server CLI as described at
  // https://github.com/powertac/powertac-server/wiki/Server-configuration
  // Remember that this can be called once/session, without a shutdown.
  private void processCli (String[] args)
  {
    // set up command-line options
    OptionParser parser = new OptionParser();
    parser.accepts("sim");
    OptionSpec<String> bootOutput = 
        parser.accepts("boot").withRequiredArg().ofType(String.class);
    OptionSpec<URL> controllerOption =
        parser.accepts("control").withRequiredArg().ofType(URL.class);
    OptionSpec<String> gameOpt = 
    	parser.accepts("game-id").withRequiredArg().ofType(String.class);
    OptionSpec<String> serverConfigUrl =
        parser.accepts("config").withRequiredArg().ofType(String.class);
    OptionSpec<String> bootData =
        parser.accepts("boot-data").withRequiredArg().ofType(String.class);
    OptionSpec<String> seedData =
        parser.accepts("random-seeds").withRequiredArg().ofType(String.class);
    OptionSpec<String> weatherData =
        parser.accepts("weather-data").withRequiredArg().ofType(String.class);
    OptionSpec<String> jmsUrl =
        parser.accepts("jms-url").withRequiredArg().ofType(String.class);
    OptionSpec<String> inputQueue =
        parser.accepts("input-queue").withRequiredArg().ofType(String.class);
    OptionSpec<String> brokerList =
        parser.accepts("brokers").withRequiredArg().withValuesSeparatedBy(',');
    OptionSpec<String> configDump =
        parser.accepts("config-dump").withRequiredArg().ofType(String.class);

    // do the parse
    OptionSet options = parser.parse(args);

    try {
      // process common options
      controllerURL = options.valueOf(controllerOption);
      
      String game = options.valueOf(gameOpt);
      // It's game 0 by default
      if (null == game) {
        game = "0";
      }
      String serverConfig = options.valueOf(serverConfigUrl);

      // process tournament scheduler based info
      if (controllerURL != null) {
        tss.setTournamentSchedulerUrl(controllerURL.toString());
        tss.setGameId(game);
        serverConfig = tss.getConfigUrl().toExternalForm();
      }
      
      if (options.has(bootOutput)) {
        // bootstrap session
        bootSession(options.valueOf(bootOutput),
                    serverConfig,
                    game,
                    options.valueOf(configDump));
      }
      else if (options.has("sim")) {
        // sim session
        simSession(options.valueOf(bootData),
                   serverConfig,
                   options.valueOf(jmsUrl),
                   game,
                   options.valuesOf(brokerList),
                   options.valueOf(seedData),
                   options.valueOf(weatherData),
                   options.valueOf(inputQueue),
                   options.valueOf(configDump));
      }
      else if (options.has("config-dump")) {
        // just set up and dump config using a truncated boot session
        bootSession(null, serverConfig, game, options.valueOf(configDump));
      }
      else {
        // Must be one of boot, sim, or config-dump
        System.err.println("Must provide either --boot or --sim to run server");
        System.exit(1);
      }
    }
    catch (OptionException e) {
      System.err.println("Bad command argument: " + e.toString());
    }
  }

  // ---------- top-level boot and sim session control ----------
  /**
   * Starts a boot session with the given arguments. If the bootFilename is
   * null, then runs just far enough to dump the configuration and quits.
   */
  @Override
  public String bootSession (String bootFilename,
                             String config,
                             String game,
                             String configDump)
  {
    // start the logs
    String error = null;
    gameId = game;
    String sessionType = (null != config) ? "config-" : "boot-";
    logService.startLog(sessionType + game);

    try {
      if (null != bootFilename) {
        log.info("bootSession: bootFilename={}, config={}, game={}",
                 bootFilename, config, game);
      }
      else if (null == configDump) {
        log.error("Nothing to do here, both bootFilename and configDump are null");
        return ("Invalid boot session");
      }
      else {
        log.info("config dump: config={}, configDump={}",
                 config, configDump);
      }
      // process serverConfig now, because other options may override
      // parts of it
      setupConfig(config, configDump);

      // set the logfile suffix
      //setLogSuffix(logSuffix, logPrefix + gameId);

      if (null != bootFilename) {
        File bootFile = new File(bootFilename);
        if (!bootFile
            .getAbsoluteFile()
            .getParentFile()
            .canWrite()) {
          error = "Cannot write to bootstrap data file " + bootFilename;
          System.out.println(error);
        }
        else {
          startBootSession(bootFile);
        }
      }
      else {
        // handle config-dump-only session
        startBootSession(null);
      }
    }
    catch (NullPointerException npe) {
      error = "Bootstrap filename not given";
    }
    catch (MalformedURLException e) {
      // Note that this should not happen from the web interface
      error = "Malformed URL: " + e.toString();
      System.out.println(error);
    }
    catch (IOException e) {
      error = "Error reading configuration";
    }
    catch (ConfigurationException e) {
      error = "Error setting configuration";
    }
    return error;
  }

  @Override
  public String bootSession (String bootFilename, String configFilename, String gameId)
  {
    return bootSession(bootFilename, configFilename, gameId, null);
  }

  @Override
  public String simSession (String bootData,
                            String config,
                            String jmsUrl,
                            String game,
                            List<String> brokerUsernames,
                            String seedData,
                            String weatherData,
                            String inputQueueName,
                            String configOutput)
  {
    // start the logs
    String error = null;
    gameId = game;
    logService.startLog("sim-" + game);

    try {
      log.info("simSession: bootData=" + bootData
               + ", config=" + config
               + ", jmsUrl=" + jmsUrl
               + ", game=" + game
               + ", seedData=" + seedData
               + ", weatherData=" + weatherData
               + ", inputQueue=" + inputQueueName);
      // process serverConfig now, because other options may override
      // parts of it
      setupConfig(config, configOutput);

      // extract sim time info from Competition instance in seed or weather data
      // if either is in use
      loadCompetitionMaybe(seedData, weatherData);

      // Use weather file instead of webservice
      useWeatherDataMaybe(weatherData);

      // load random seed data if asked
      createSeedLoader(seedData);

      // jms setup overrides config
      if (jmsUrl != null) {
        serverProps.setProperty("server.jmsManagementService.jmsBrokerUrl",
                                jmsUrl);
      }

      // boot data access
      URL bootUrl = null;
      if (controllerURL != null) {
        bootUrl = tss.getBootUrl();
      }
      else if (bootData != null) {
        if (!bootData.contains(":"))
          bootData = "file:" + bootData;
        bootUrl = new URL(bootData);
      }
      if (null == bootUrl) {
        error = "bootstrap data source not given";
        System.out.println(error);        
      }
      else {
        log.info("bootUrl=" + bootUrl.toExternalForm());
        startSimSession(brokerUsernames, inputQueueName, bootUrl);
      }
    }
    catch (MalformedURLException e) {
      // Note that this should not happen from the web interface
      error = "Malformed URL: " + e.toString();
      System.out.println(error);
    }
    catch (IOException e) {
      error = "Error reading configuration " + config;
    }
    catch (ConfigurationException e) {
      error = "Error setting configuration " + config;
    }
    return error;
  }

  @Override
  public String simSession (String bootData, String config, String jmsUrl, String gameId,
                            List<String> brokerUsernames, String seedData,
                            String weatherData, String inputQueueName)
  {
    return simSession(bootData, config, jmsUrl, gameId,
                      brokerUsernames, seedData,
                      weatherData, inputQueueName, null);
  }

  // Digs out the Competition instance from old logs and sets time data as needed
  // for the current session. If we cannot retrieve the data, the server will exit.
  private void loadCompetitionMaybe (String seedData, String weatherData)
  {
    CompetitionLoader loader = null;
    Competition tempCompetition = null;
    if (null != seedData) {
      log.info("Loading seeds from {}", seedData);
      loader = new CompetitionLoader(seedData);
      tempCompetition = loader.extractCompetition();
      if (null == tempCompetition) {
        System.exit(1);
      }
      loadTimeslotCounts(tempCompetition);
    }
    if (null != weatherData) {
      log.info("Loading weather data from {}", weatherData);
      loader = new CompetitionLoader(weatherData);
      tempCompetition = loader.extractCompetition();
      if (null == tempCompetition) {
        System.exit(1);
      }
      loadStartTime(tempCompetition);
    }
  }

  private void setupConfig (String config, String configDump)
          throws ConfigurationException, IOException
  {
    serverProps.recycle();
    if (null != configDump) {
      serverProps.setConfigOutput(configDump);
    }

    if (config == null)
      return;
    log.info("Reading configuration from " + config);
    serverProps.setUserConfig(makeUrl(config));
    log.info("Server version {}", System.getProperty("server.pomId"));
  }

  // Loads the sim start time from an old state log that we are also using for seeds
  // and/or weather data
  private void loadStartTime (Competition comp)
  {
    serverProps.setProperty("common.competition.simulationBaseTime",
                            comp.getSimulationBaseTime().getMillis());
  }

  // Sets game-length parameters from seed data
  private void loadTimeslotCounts (Competition comp)
  {
    log.info("Getting minimumTimeslotCount and expectedTimeslotCount from Competition");
    serverProps.setProperty("common.competition.minimumTimeslotCount",
                            comp.getMinimumTimeslotCount());
    serverProps.setProperty("common.competition.expectedTimeslotCount",
                            comp.getExpectedTimeslotCount()); 
  }
  
  // package visibility to support testing
  void createSeedLoader (String seedSource)
  {
    if (null == seedSource || 0 == seedSource.length()) {
      return;
    }
    seedLoader = new SeedLoader(seedSource);
  }

  void loadSeedsMaybe ()
  {
    if (null == seedLoader) {
      return;
    }
    log.info("Reading random seeds");
    seedLoader.loadSeeds();
  }

  /*
   * If weather data-file is used (instead of the URL-based weather server)
   * extract the first data, and set that as simulationBaseTime.
   */
  private void useWeatherDataMaybe(String weatherData)
  {
    if (weatherData == null || weatherData.isEmpty()) {
      return;
    }
    log.info("Using weather data from {}", weatherData);
    serverProps.setProperty("server.weatherService.weatherData", weatherData);
  }

  private URL makeUrl (String name) throws MalformedURLException
  {
    String urlName = name;
    if (!urlName.contains(":")) {
      urlName = "file:" + urlName;
    }
    return new URL(urlName);
  }

  // Runs a bootstrap session
  // If the bootstrapFile is null, then just configure and quit
  private void startBootSession (File bootstrapFile) throws IOException
  {
    boolean dumpOnly = (null == bootstrapFile);
    final FileWriter bootWriter =
        dumpOnly ? null : new FileWriter(bootstrapFile);
    session = new Thread() {
      @Override
      public void run () {
        cc.setAuthorizedBrokerList(new ArrayList<String>());
        preGame();
        cc.runOnce(true, dumpOnly);
        if (null != bootWriter) {
          saveBootstrapData(bootWriter);
        }
      }
    };
    session.start();
  }

  // Runs a simulation session
  private void startSimSession (final List<String> brokers,
                                final String inputQueueName,
                                final URL bootUrl)
  {
    session = new Thread() {
      @Override
      public void run () {
        cc.setAuthorizedBrokerList(brokers);
        cc.setInputQueueName(inputQueueName);
        Document document = getDocument(bootUrl);
        if (document != null) {
          if (preGame(document)) {
            bootstrapDataRepo.add(processBootDataset(document));
            List<Object> mbd =
                    bootstrapDataRepo.getData(MarketBootstrapData.class);
            if (null == mbd) {
              log.error("marketBootstrapData is null");
              return;
            }
            if (0 == mbd.size()) {
              log.error("marketBootstrapData is empty");
              return;
            }
            Competition.currentCompetition()
                .setMarketBootstrapData((MarketBootstrapData)mbd.get(0));
            cc.runOnce(false);
          }
        }
      }
    };
    session.start();
  }

  // copied to BootstrapDataRepo
  private Document getDocument (URL bootUrl)
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder;
    Document doc = null;
    try {
      builder = factory.newDocumentBuilder();
      doc = builder.parse(bootUrl.openStream());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return doc;
  }

  // Create a gameId if it's not already set (mainly for Viz-driven games)
//  private void ensureGameId (String game)
//  {
//    gameId = null == game ? Integer.toString(sessionCount) : game;
//  }
//
//  // Names of multi-session games use the default session prefix
//  private void nextGameId ()
//  {
//    sessionCount += 1;
//    gameId = Integer.toString(sessionCount);
//  }

  /**
   * Pre-game server setup - creates the basic configuration elements
   * to make them accessible to the web-based game-setup functions.
   * This method must be called when the server is started, and again at
   * the completion of each simulation. The actual simulation is started
   * with a call to competitionControlService.runOnce().
   */
  @Override
  public void preGame ()
  {
    extractPomId();
    log.info("preGame() - start game " + gameId);
    log.info("POM version ID: {}",
             serverProps.getProperty("common.competition.pomId"));
    IdGenerator.recycle();
    // Create competition instance
    competition = Competition.newInstance(gameId);
    Competition.setCurrent(competition);
    
    // Set up all the plugin configurations
    log.info("pre-game initialization");
    configureCompetition(competition);
    timeService.setClockParameters(competition);
    timeService.setCurrentTime(competition.getSimulationBaseTime());

    // Handle pre-game initializations by clearing out the repos
    List<DomainRepo> repos =
      SpringApplicationContext.listBeansOfType(DomainRepo.class);
    log.debug("found " + repos.size() + " repos");
    for (DomainRepo repo : repos) {
      repo.recycle();
    }
    // Message router also needs pre-game initialization
    messageRouter.recycle();

    // Init random seeds after clearing repos and before initializing services
    loadSeedsMaybe();
  }

  // configures a Competition from server.properties
  private void configureCompetition (Competition competition)
  {
    serverProps.configureMe(competition);
  }

  /**
   * Sets up the simulator, with config overrides provided in a file.
   */
  private boolean preGame (Document document)
  {
    log.info("preGame(File) - start");
    // run the basic pre-game setup
    preGame();

    // read the config info from the bootReader - We need to find a Competition
    Competition bootstrapCompetition = readBootRecord(document);
    if (null == bootstrapCompetition)
      return false;

    // update the existing Competition - should be the current competition
    Competition.currentCompetition().update(bootstrapCompetition);
    timeService.setClockParameters(competition);
    timeService.setCurrentTime(competition.getSimulationBaseTime());
    return true;
  }

  Competition readBootRecord (Document document)
  {
    XPathFactory factory = XPathFactory.newInstance();
    XPath xPath = factory.newXPath();
    Competition bootstrapCompetition = null;
    try {
      // first grab the Competition
      XPathExpression exp =
          xPath.compile("/powertac-bootstrap-data/config/competition");
      NodeList nodes = (NodeList) exp.evaluate(document,
          XPathConstants.NODESET);
      String xml = nodeToString(nodes.item(0));
      bootstrapCompetition = (Competition) messageConverter.fromXML(xml);

      // next, grab the bootstrap-state and add it to the config
      exp = xPath.compile("/powertac-bootstrap-data/bootstrap-state/properties");
      nodes = (NodeList) exp.evaluate(document, XPathConstants.NODESET);
      if (null != nodes && nodes.getLength() > 0) {
        // handle the case where there is no bootstrap-state clause
        xml = nodeToString(nodes.item(0));
        Properties bootState = (Properties) messageConverter.fromXML(xml);
        serverProps.addProperties(bootState);
      }
    }
    catch (XPathExpressionException xee) {
      log.error("preGame: Error reading boot dataset: " + xee.toString());
      System.out.println("preGame: Error reading boot dataset: " + xee.toString());
    }
    return bootstrapCompetition;
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
      // current competition
      output.write(messageConverter.toXML(competition));
      output.newLine();
      output.write("</config>");
      output.newLine();
      // bootstrap state
      output.write("<bootstrap-state>");
      output.newLine();
      output.write(gatherBootstrapState());
      output.newLine();
      output.write("</bootstrap-state>");
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

  private String gatherBootstrapState ()
  {
    List<BootstrapState> collectors =
        SpringApplicationContext.listBeansOfType(BootstrapState.class);
    for (BootstrapState collector : collectors) {
      log.info("Calling saveBootstrapState() on collector {}", collector.getClass().getName());
      collector.saveBootstrapState();
    }
    Properties result = serverProps.getBootstrapState();
    String output = messageConverter.toXML(result);
    return output;
  }

  // Extracts a bootstrap dataset from its file
  private ArrayList<Object> processBootDataset (Document document)
  {
    // Read and convert the bootstrap dataset
    ArrayList<Object> result = new ArrayList<>();
    XPathFactory factory = XPathFactory.newInstance();
    XPath xPath = factory.newXPath();
    try {
      // we want all the children of the bootstrap node
      XPathExpression exp =
          xPath.compile("/powertac-bootstrap-data/bootstrap/*");
      NodeList nodes = (NodeList)exp.evaluate(document, XPathConstants.NODESET);
      log.info("Found " + nodes.getLength() + " bootstrap nodes");
      // Each node is a bootstrap data item
      for (int i = 0; i < nodes.getLength(); i++) {
        String xml = nodeToString(nodes.item(i));
        Object msg = messageConverter.fromXML(xml);
        result.add(msg);
      }
    }
    catch (XPathExpressionException xee) {
      log.error("runOnce: Error reading config file: " + xee.toString());
    }
    return result;
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
    return sw.toString();
  }

  // Extracts the pom ID from the manifest, adds it to server properties
  private void extractPomId ()
  {
    try {
      Properties props = new Properties();
      InputStream is =
          getClass().getResourceAsStream("/META-INF/maven/org.powertac/server-main/pom.properties");
      if (null != is) {
        props.load(is);
        serverProps.setProperty("common.competition.pomId",
                                props.getProperty("version"));;
      }
    } catch (Exception e) {
      log.error("Failed to load properties from manifest");
    }
  }

  public class CompetitionLoader extends LogtoolContext
  {
    private String source;
    private Competition tempCompetition;
    
    // This should be called before other calls that might want logtoolCore to be non-null
    public CompetitionLoader (String source)
    {
      super();
      this.source = source;
      this.core = logtoolCore;
      this.dor = logtoolCore.getDOR();
    }
    
    Competition extractCompetition ()
    {
      logtoolCore.resetDOR(true);
      logtoolCore.includeClassname("org.powertac.common.RandomSeed");
      logtoolCore.includeClassname("org.powertac.common.Competition");
      ObjectReader reader = logtoolCore.getObjectReader(source);
      if (null == reader) {
        log.error("Cannot read from {}", source);
        return null;
      }
      boolean flag = false; // completion flag
      while (!flag) {
        // now we read the file. Note that we cannot use the Competition instance
        // at the front of the file until all the fields have been set through
        // a series of method calls. Once we see an object that's not a Competition,
        // we can assume the fields are all set. Usually the next object in the file
        // is the first RandomSeed.
        Object thing = reader.getNextObject();
        if (thing.getClass() == RandomSeed.class) {
          log.debug("extractCompetition found RandomSeed");
          // we've done enough
          flag = true;
        }
        else if (thing.getClass() == Competition.class) {
          tempCompetition = (Competition) thing;
          log.info("extractCompetition found Competition {}", tempCompetition.getName());
        }
      }
      // At this point, the tempCompetition object has the time info we need
      // Before returning, we also need to reset the DOR
      logtoolCore.resetDOR(false);
      return tempCompetition;
    }
  }
  
  public class SeedLoader extends LogtoolContext implements Analyzer
  {
    private String source;

    // Constructor gets the seedSource info
    public SeedLoader(String seedSource)
    {
      super();
      source = seedSource;
      this.core = logtoolCore;
      this.dor = logtoolCore.getDOR();      
    }
    
    void loadSeeds ()
    {
      logtoolCore.resetDOR(false);
      logtoolCore.includeClassname("org.powertac.common.RandomSeed");
      registerMessageHandlers();
      Analyzer[] tools = new Analyzer[1];
      tools[0] = this;
      logtoolCore.readStateLog(source, tools);
    }
    
    //logtool message handlers

    public void handleMessage(RandomSeed thing)
    {
      log.info("Restoring RandomSeed {}", thing.getRequesterClass());
      randomSeedRepo.restoreRandomSeed(thing);
    }

    @Override
    public void setup () throws FileNotFoundException
    {
      // Not needed      
    }

    @Override
    public void report ()
    {
      // nothing to do here
    }
  }
}
