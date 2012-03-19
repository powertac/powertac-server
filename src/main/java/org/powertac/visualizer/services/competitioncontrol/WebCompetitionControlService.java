package org.powertac.visualizer.services.competitioncontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.TimeService;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerProperties;
import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.common.repo.DomainRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.server.CompetitionControlService;
import org.powertac.server.LogService;
import org.powertac.server.ServerPropertiesService;
import org.powertac.server.VisualizerProxyService;
import org.powertac.visualizer.services.VisualizerResourceLoaderService;
import org.powertac.visualizer.services.VisualizerService;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Purpose of this service is to allow (very) primitive control of PowerTAC
 * simulator. Feel free to change this class to match your requirements.
 * 
 * @author Jurica Babic
 * 
 */
@Service
public class WebCompetitionControlService {

	static private Logger log = Logger.getLogger(WebCompetitionControlService.class);

	@Autowired
	private VisualizerResourceLoaderService resourceLoader;

	private CompetitionControlService cc;

	@Autowired
	private VisualizerService visualizerService; 

	private AbstractApplicationContext context;
	private ServerPropertiesService serverProps;
	private Competition competition;
	private boolean simRunning = false;
	private LogService logService;
	private XMLMessageConverter messageConverter;
    private PluginConfigRepo pluginConfigRepo;

    @Deprecated
	public String runSim() throws IOException {

//		if (!simRunning) {
//			System.out.println("java.class.path ===>" + System.getProperty("java.class.path"));
//
//			context = new FileSystemXmlApplicationContext(resourceLoader.getResource("WEB-INF/spring/powertac.xml")
//					.getFile().getCanonicalPath());
//
//			context.registerShutdownHook();
//
//			// find the CompetitionControl bean
//			cc = (CompetitionControlService) context.getBeansOfType(CompetitionControl.class).values().toArray()[0];
//			logService = (LogService) context.getBeansOfType(LogService.class).values().toArray()[0];
//			messageConverter = (XMLMessageConverter) context.getBeansOfType(XMLMessageConverter.class).values().toArray()[0];
//			pluginConfigRepo = (PluginConfigRepo) context.getBeansOfType(PluginConfigRepo.class).values().toArray()[0];
//
//
//			
//			
//			serverProps = (ServerPropertiesService) context.getBeansOfType(ServerProperties.class).values().toArray()[0];
//
//			// set user config
//			URL testPropertiesPath = resourceLoader.getResource("WEB-INF/config/test.properties").getURL();
//			
//			serverProps.setUserConfig(testPropertiesPath);
//
//			// UPDATE FOR NEW SIMULATOR VERSION
//			log.info("In Simulation mode!!!");
//			// String bootstrapFilename =
//			// serverProps.getProperty("server.bootstrapDataFile",
//			// "bd-noname.xml");
//			String bootstrapFilename = resourceLoader.getResource("WEB-INF/config/boot-data.xml").getFile()
//					.getCanonicalPath();
//			File bootFile = new File(bootstrapFilename);
//			if (!bootFile.canRead()) {
//				System.out.println("Cannot read bootstrap data file " + bootstrapFilename);
//			} else {
//				// collect broker names, hand to CC for login control
//				ArrayList<String> brokerList = new ArrayList<String>();
//				brokerList.add("Sample");
//				brokerList.add("Dragec");
//				brokerList.add("Markec");
//				cc.setAuthorizedBrokerList(brokerList);
//				
//				cc.init();
//				
//
//				if (preGame(bootFile)) {
//					cc.setBootstrapDataset(processBootDataset(bootFile));
//					//cc.runOnce(false);
//				} 
//			}
//
//			// time to initialize Visualizer:
//			VisualizerProxy visualizerProxy = (VisualizerProxy) context.getBeansOfType(VisualizerProxyService.class)
//					.values().toArray()[0];
//			visualizerService.init(visualizerProxy);
//
//			CompetitionStartThread competitionStartThread = new CompetitionStartThread();
//			new Thread(competitionStartThread).start();
//
//			return "Simulation started";
//		} else {
//			return "Can't do that, sim is running.";
//		}
		return "deprecated";
	}

	// to avoid GUI blocking
	class CompetitionStartThread implements Runnable {

		public void run() {
				
				simRunning = true;
				cc.runOnce(false);
				context.close();
				simRunning = false;
				// start new competition
			//runSim();
			
		}

	}

	public void preGame() {
		log.info("preGame() - start");
		// Create default competition
		competition = Competition.newInstance("defaultCompetition");
		// competitionId = competition.getId();
		String suffix = serverProps.getProperty("server.logfileSuffix", "x");
		logService.startLog(suffix);

		// Set up all the plugin configurations
		log.info("pre-game initialization");
		configureCompetition(competition);

		// Handle pre-game initializations by clearing out the repos,
		// then creating the PluginConfig instances
		List<DomainRepo> repos = SpringApplicationContext.listBeansOfType(DomainRepo.class);
		log.debug("found " + repos.size() + " repos");
		for (DomainRepo repo : repos) {
			repo.recycle();
		}
		List<InitializationService> initializers = SpringApplicationContext
				.listBeansOfType(InitializationService.class);
		log.debug("found " + initializers.size() + " initializers");
		for (InitializationService init : initializers) {
			init.setDefaults();
		}
	}
	
	// configures a Competition from server.properties
	  private void configureCompetition (Competition competition)
	  {
	    // get game length
	    int minimumTimeslotCount =
	        serverProps.getIntegerProperty("competition.minimumTimeslotCount",
	                                       competition.getMinimumTimeslotCount());
	    int expectedTimeslotCount =
	        serverProps.getIntegerProperty("competition.expectedTimeslotCount",
	                                       competition.getExpectedTimeslotCount());
	    if (expectedTimeslotCount < minimumTimeslotCount) {
	      log.warn("competition expectedTimeslotCount " + expectedTimeslotCount
	               + " < minimumTimeslotCount " + minimumTimeslotCount);
	      expectedTimeslotCount = minimumTimeslotCount;
	    }
	    int bootstrapTimeslotCount =
	        serverProps.getIntegerProperty("competition.bootstrapTimeslotCount",
	                                       competition.getBootstrapTimeslotCount());
	    int bootstrapDiscardedTimeslots =
	        serverProps.getIntegerProperty("competition.bootstrapDiscardedTimeslots",
	                                       competition.getBootstrapDiscardedTimeslots());

	    // get trading parameters
	    int timeslotsOpen =
	        serverProps.getIntegerProperty("competition.timeslotsOpen",
	                                       competition.getTimeslotsOpen());
	    int deactivateTimeslotsAhead =
	        serverProps.getIntegerProperty("competition.deactivateTimeslotsAhead",
	                                       competition.getDeactivateTimeslotsAhead());

	    // get time parameters
	    int timeslotLength =
	        serverProps.getIntegerProperty("competition.timeslotLength",
	                                       competition.getTimeslotLength());
	    int simulationTimeslotSeconds =
	        timeslotLength * 60 / (int)competition.getSimulationRate();
	    simulationTimeslotSeconds =
	        serverProps.getIntegerProperty("competition.simulationTimeslotSeconds",
	                                       simulationTimeslotSeconds);
	    int simulationRate = timeslotLength * 60 / simulationTimeslotSeconds;
	    DateTimeZone.setDefault(DateTimeZone.UTC);
	    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
	    Instant start = null;
	    try {
	      start =
	        fmt.parseDateTime(serverProps.getProperty("competition.baseTime")).toInstant();
	    }
	    catch (Exception e) {
	      log.error("Exception reading base time: " + e.toString());
	    }
	    if (start == null)
	      start = competition.getSimulationBaseTime();

	    // populate the competition instance
	    competition
	      .withMinimumTimeslotCount(minimumTimeslotCount)
	      .withExpectedTimeslotCount(expectedTimeslotCount)
	      .withSimulationBaseTime(start)
	      .withSimulationRate(simulationRate)
	      .withTimeslotLength(timeslotLength)
	      .withSimulationModulo(timeslotLength * TimeService.MINUTE)
	      .withTimeslotsOpen(timeslotsOpen)
	      .withDeactivateTimeslotsAhead(deactivateTimeslotsAhead)
	      .withBootstrapTimeslotCount(bootstrapTimeslotCount)
	      .withBootstrapDiscardedTimeslots(bootstrapDiscardedTimeslots);
	    
	    // bootstrap timeslot timing is a local parameter
	    int bootstrapTimeslotSeconds =
	        serverProps.getIntegerProperty("competition.bootstrapTimeslotSeconds",
	                                       (int)(2000
	                                             / TimeService.SECOND));
	    cc.setBootstrapTimeslotMillis(bootstrapTimeslotSeconds * TimeService.SECOND);
	  }
	  
	  public boolean preGame (File bootFile)
	  {
	    log.info("preGame(File) - start");
	    // run the basic pre-game setup
	    preGame();
	    
	    // read the config info from the bootReader - 
	    // We need to find a Competition and a set of PluginConfig instances
	    Competition bootstrapCompetition = null;
	    ArrayList<PluginConfig> configList = new ArrayList<PluginConfig>();
	    //InputSource source = new InputSource(bootReader);
	    XPathFactory factory = XPathFactory.newInstance();
	    XPath xPath = factory.newXPath();
	    try {
	      // first grab the Competition
	      XPathExpression exp =
	          xPath.compile("/powertac-bootstrap-data/config/competition");
	      NodeList nodes = (NodeList)exp.evaluate(new InputSource(new FileReader(bootFile)),
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
	        return false;
	      }
	      // if we found it, then we need to update it.
	      match.update(next);
	    }
	    
	    // we currently ignore cases where there's a config in the server that's
	    // not in the file; there might be use cases for which this would
	    // be useful.
	    return true;
	  }
	  
	  private ArrayList<Object> processBootDataset (File datasetFile)
	  {
	    // Read and convert the bootstrap dataset
	    ArrayList<Object> result = new ArrayList<Object>();
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
	    String result = sw.toString();
	    //log.info("xml node: " + result);
	    return result;
	  }

}
