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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.Competition;
import org.powertac.common.IdGenerator;
import org.powertac.common.TimeService;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.BootstrapDataCollector;
import org.powertac.common.interfaces.BootstrapState;
import org.powertac.common.interfaces.CompetitionSetup;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.repo.DomainRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
  //static private ApplicationContext context;

  @Autowired
  private CompetitionControlService cc;

  @Autowired
  private BootstrapDataCollector defaultBroker;

  @Autowired
  private ServerPropertiesService serverProps;

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
  private int gameId = 0;
  private URL controllerURL;
  private String seedSource = null;
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
  private void processCli (String[] args)
  {
    // set up command-line options
    OptionParser parser = new OptionParser();
    parser.accepts("sim");
    OptionSpec<String> bootOutput = 
        parser.accepts("boot").withRequiredArg().ofType(String.class);
    OptionSpec<URL> controllerOption =
        parser.accepts("control").withRequiredArg().ofType(URL.class);
    OptionSpec<Integer> gameOpt = 
    	parser.accepts("game-id").withRequiredArg().ofType(Integer.class);
    OptionSpec<String> serverConfigUrl =
        parser.accepts("config").withRequiredArg().ofType(String.class);
    OptionSpec<String> logSuffixOption =
        parser.accepts("log-suffix").withRequiredArg();
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

    // do the parse
    OptionSet options = parser.parse(args);

    try {
      // process common options
      seedSource = null;
      String logSuffix = options.valueOf(logSuffixOption);
      controllerURL = options.valueOf(controllerOption);
      Integer game = options.valueOf(gameOpt);
      String serverConfig = options.valueOf(serverConfigUrl);
      
      // process tournament scheduler based info
      if (controllerURL != null) {
        if (null == game) {
          log.error("controller URL " + controllerURL + " without gameId");
          gameId = 0;
        }
        else {
          gameId = game;
        }
        tss.setTournamentSchedulerUrl(controllerURL.toString());
        tss.setGameId(game);
        serverConfig = tss.getConfigUrl().toExternalForm();
      }
      
      if (options.has(bootOutput)) {
        // bootstrap session
        bootSession(options.valueOf(bootOutput),
                    serverConfig,
                    logSuffix,
                    options.valueOf(seedData),
                    options.valueOf(weatherData));
      }
      else if (options.has("sim")) {
        // sim session
        simSession(options.valueOf(bootData),
                   serverConfig,
                   options.valueOf(jmsUrl),
                   logSuffix,
                   options.valuesOf(brokerList),
                   options.valueOf(seedData),
                   options.valueOf(weatherData),
                   options.valueOf(inputQueue));
      }
      else {
        // Must be either boot or sim
        System.err.println("Must provide either --boot or --sim to run server");
        System.exit(1);
      }
    }
    catch (OptionException e) {
      System.err.println("Bad command argument: " + e.toString());
    }
  }

  // sets up the logfile name suffix
  private void setLogSuffix (String logSuffix, String defaultSuffix)
                                 throws IOException
  {
    if (logSuffix == null)
      logSuffix = defaultSuffix;
    serverProps.setProperty("server.logfileSuffix", logSuffix);
  }

  // ---------- top-level boot and sim session control ----------
  @Override
  public String bootSession (String bootFilename, String config,
                             String logSuffix,
                             String seedData,
                             String weatherData)
  {
    String error = null;
    try {
      log.info("bootSession: bootFilename=" + bootFilename
          + ", config=" + config
          + ", logSuffix=" + logSuffix);
      // process serverConfig now, because other options may override
      // parts of it
      serverProps.recycle();
      setConfigMaybe(config);

      // Use weather file instead of webservice, this sets baseTime also
      useWeatherDataMaybe(weatherData, true);

      // load random seeds if requested
      seedSource = seedData;

      // Set min- and expectedTsCount if seed-data is given
      loadTimeslotCountsMaybe();

      // set the logfile suffix
      setLogSuffix(logSuffix, "boot");

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
  public String simSession (String bootData, String config, String jmsUrl,
                     String logfileSuffix,
                     List<String> brokerUsernames,
                     String seedData,
                     String weatherData,
                     String inputQueueName)
  {
    String error = null;
    try {
      log.info("simSession: bootData=" + bootData
               + ", config=" + config
               + ", jmsUrl=" + jmsUrl
               + ", seedData=" + seedData
               + ", weatherData=" + weatherData
               + ", inputQueue=" + inputQueueName);
      // process serverConfig now, because other options may override
      // parts of it
      serverProps.recycle();
      setConfigMaybe(config);

      // Use weather file instead of webservice, this sets baseTime also
      useWeatherDataMaybe(weatherData, false);
      
      // load random seeds if requested
      seedSource = seedData;

      // Set min- and expectedTsCount if seed-data is given
      loadTimeslotCountsMaybe();

      // set the logfile suffix
      setLogSuffix(logfileSuffix, "sim-" + gameId);

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

  private void setConfigMaybe (String config)
          throws ConfigurationException, IOException
  {
    if (config == null)
      return;
    log.info("Reading configuration from " + config);
    serverProps.setUserConfig(makeUrl(config));
  }

  private void loadTimeslotCountsMaybe ()
  {
    if (seedSource == null)
      return;

    log.info("Getting minimumTimeslotCount and expectedTimeslotCount from "
        + seedSource);

    int minCount = -1;
    int expCount = -1;
    try {
      BufferedReader br = new BufferedReader(new FileReader(seedSource));
      String line;
      while((line = br.readLine()) != null) {
        if (line.contains("withMinimumTimeslotCount")) {
          String[] s = line.split("::");
          minCount = Integer.valueOf(s[s.length-1]);
        }
        if (line.contains("withExpectedTimeslotCount")) {
          String[] s = line.split("::");
          expCount = Integer.valueOf(s[s.length-1]);
        }

        if (minCount != -1 && expCount != -1) {
          break;
        }
      }
      br.close();
      if (minCount != -1) {
        serverProps.setProperty("common.competition.minimumTimeslotCount",
            minCount);
      }
      if (expCount != -1) {
        serverProps.setProperty("common.competition.expectedTimeslotCount",
            expCount);
      }
    }
    catch(IOException e) {
      log.error("Cannot load minimumTimeslotCount and "
          + "expectedTimeslotCount from " + seedSource);
    }
  }
  
  private void loadSeedsMaybe ()
  {
    if (seedSource == null)
      return;
    log.info("Reading random seeds from " + seedSource);
    InputStreamReader stream;
    try {
      stream = new InputStreamReader(makeUrl(seedSource).openStream());
      randomSeedRepo.loadSeeds(stream);
    }
    catch (Exception e) {
      log.error("Cannot load seeds from " + seedSource);
    }
  }

  /*
   * If weather data-file is used (instead of the URL-based weather server)
   * extract the first data, and set that as simulationBaseTime.
   */
  private void useWeatherDataMaybe(String weatherData, boolean bootstrapMode)
  {
    if (weatherData == null || weatherData.isEmpty()) {
      return;
    }

    log.info("Getting BaseTime from " + weatherData);
    String baseTime = null;
    if (weatherData.endsWith(".xml")) {
      baseTime = getBaseTimeXML(weatherData);
    } else if (weatherData.endsWith(".state")) {
      baseTime = getBaseTimeState(weatherData);
    } else {
      log.warn("Only XML and state files are allowed for weather data");
    }

    if (baseTime != null) {
      if (bootstrapMode) {
        serverProps.setProperty("common.competition.simulationBaseTime",
            baseTime);
      }
      serverProps.setProperty("server.weatherService.weatherData",
          weatherData);
    }
  }

  private String getBaseTimeXML(String weatherData)
  {
    try {
      DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = domFactory.newDocumentBuilder();
      Document doc = builder.parse(weatherData);
      XPathFactory factory = XPathFactory.newInstance();
      XPath xPath = factory.newXPath();
      XPathExpression expr =
          xPath.compile("/data/weatherReports/weatherReport/@date");

      String earliest = "ZZZZ-ZZ-ZZ";
      NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
      for (int i = 0; i < nodes.getLength(); i++) {
        String date = nodes.item(i).toString().split(" ")[0].split("\"")[1];
        earliest = date.compareTo(earliest) < 0 ? date : earliest;
      }
      return earliest;
    } catch (Exception e) {
      log.error("Error extracting BaseTime from : " + weatherData);
      e.printStackTrace();
    }
    return null;
  }

  private String getBaseTimeState(String weatherData)
  {
    BufferedReader br = null;
    try {
      br = new BufferedReader(
          new InputStreamReader(
              makeUrl(weatherData).openStream()));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.contains("withSimulationBaseTime")) {
          String millis = line.substring(line.lastIndexOf("::") + 2);
          Date date = new Date(Long.parseLong(millis));
          return new SimpleDateFormat("yyyy-MM-dd").format(date.getTime());
        }
      }
    } catch (Exception e) {
      log.error("Error extracting BaseTime from : " + weatherData);
      e.printStackTrace();
    } finally {
      try {
        if (br != null) {
          br.close();
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    log.error("Error extracting BaseTime from : " + weatherData);
    log.error("No 'withSimulationBaseTime' found!");
    return null;
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
  private void startBootSession (File bootstrapFile) throws IOException
  {
    final FileWriter bootWriter = new FileWriter(bootstrapFile);
    session = new Thread() {
      @Override
      public void run () {
        cc.setAuthorizedBrokerList(new ArrayList<String>());
        preGame();
        cc.runOnce(true);
        saveBootstrapData(bootWriter);
      }
    };
    session.start();
  }

  // Runs a simulation session
  private void startSimSession (final List<String> brokers,
                                final String inputQueueName,
                                final URL bootDataset)
  {
    session = new Thread() {
      @Override
      public void run () {
        cc.setAuthorizedBrokerList(brokers);
        cc.setInputQueueName(inputQueueName);
        if (preGame(bootDataset)) {
          cc.setBootstrapDataset(processBootDataset(bootDataset));
          cc.runOnce(false);
          gameId += 1;
        }        
      }
    };
    session.start();
  }  

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
    String suffix = serverProps.getProperty("server.logfileSuffix", "x");
    String pomId = serverProps.getProperty("server.pomId", "0");
    logService.startLog(suffix);
    log.info("preGame() - start game " + gameId);
    log.info("POM version ID: " + pomId);
    IdGenerator.recycle();
    // Create competition instance
    competition = Competition.newInstance("game-" + gameId);
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
  public boolean preGame (URL bootFile)
  {
    log.info("preGame(File) - start");
    // run the basic pre-game setup
    preGame();

    // read the config info from the bootReader - We need to find a Competition
    Competition bootstrapCompetition = null;
    XPathFactory factory = XPathFactory.newInstance();
    XPath xPath = factory.newXPath();
    try {
      // first grab the Competition
      XPathExpression exp =
          xPath.compile("/powertac-bootstrap-data/config/competition");
      NodeList nodes = (NodeList)exp.evaluate(new InputSource(bootFile.openStream()),
                                     XPathConstants.NODESET);
      String xml = nodeToString(nodes.item(0));
      bootstrapCompetition = (Competition)messageConverter.fromXML(xml);

      // next, grab the bootstrap-state and add it to the config
      exp = xPath.compile("/powertac-bootstrap-data/bootstrap-state/properties");
      nodes = (NodeList)exp.evaluate(new InputSource(bootFile.openStream()),
                                     XPathConstants.NODESET);
      if (null != nodes && nodes.getLength() > 0) {
        // handle the case where there is no bootstrap-state clause
        xml = nodeToString(nodes.item(0));
        Properties bootState = (Properties)messageConverter.fromXML(xml);
        serverProps.addProperties(bootState);
      }
    }
    catch (XPathExpressionException xee) {
      log.error("preGame: Error reading boot dataset: " + xee.toString());
      System.out.println("preGame: Error reading boot dataset: " + xee.toString());
      return false;
    }
    catch (IOException ioe) {
      log.error("preGame: Error opening file " + bootFile + ": " + ioe.toString());
      System.out.println("preGame: Error opening file " + bootFile + ": " + ioe.toString());
      return false;
    }
    // update the existing Competition - should be the current competition
    Competition.currentCompetition().update(bootstrapCompetition);
    timeService.setClockParameters(competition);
    timeService.setCurrentTime(competition.getSimulationBaseTime());
    return true;
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
      collector.saveBootstrapState();
    }
    Properties result = serverProps.getBootstrapState();
    String output = messageConverter.toXML(result);
    return output;
  }

  // Extracts a bootstrap dataset from its file
  private ArrayList<Object> processBootDataset (URL bootDataset)
  {
    // Read and convert the bootstrap dataset
    ArrayList<Object> result = new ArrayList<Object>();
    XPathFactory factory = XPathFactory.newInstance();
    XPath xPath = factory.newXPath();
    try {
      InputSource source = new InputSource(bootDataset.openStream());
      // we want all the children of the bootstrap node
      XPathExpression exp =
          xPath.compile("/powertac-bootstrap-data/bootstrap/*");
      NodeList nodes = (NodeList)exp.evaluate(source, XPathConstants.NODESET);
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
    catch (IOException ioe) {
      log.error("runOnce: reset fault: " + ioe.toString());
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
}
