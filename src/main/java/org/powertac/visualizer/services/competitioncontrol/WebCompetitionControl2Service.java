package org.powertac.visualizer.services.competitioncontrol;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.server.CompetitionSetupService;
import org.powertac.server.VisualizerProxyService;
import org.powertac.visualizer.services.VisualizerResourceLoaderService;
import org.powertac.visualizer.services.VisualizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Purpose of this service is to allow (very) primitive control of PowerTAC
 * simulator. Feel free to change this class to match your requirements.
 * 
 * @author Jurica Babic
 * 
 */
@Service
public class WebCompetitionControl2Service {

	static private Logger log = Logger.getLogger(WebCompetitionControl2Service.class);

	@Autowired
	private VisualizerResourceLoaderService resourceLoader;

	private CompetitionSetupService css;

	@Autowired
	private VisualizerService visualizerService;

	@Autowired
	private GameParamatersBean gameParamaters;

	private AbstractApplicationContext context;
	private boolean simRunning = false;
	private boolean bootRunning = false;
	private String[] args;

	public String runSim() throws IOException {

		if (!simRunning) {
			log.info("java.class.path ===>" + System.getProperty("java.class.path"));

			context = new FileSystemXmlApplicationContext(resourceLoader.getResource("WEB-INF/spring/powertac.xml").getURL().toString());

			context.registerShutdownHook();

			// find the CompetitionControlService bean
			css = (CompetitionSetupService) context.getBeansOfType(CompetitionSetupService.class).values().toArray()[0];

			// URL bootstrapURL =
			// resourceLoader.getResource("WEB-INF/config/boot1.xml").getURL();
			// log.info("BOOTSTRAP_url:"+bootstrapURL);
			//
			// List<String> argList = new ArrayList<String>();
			// argList.add("--sim");
			// argList.add("--boot-data="+bootstrapURL.toString());
			// argList.add("--brokers=Sample");
			//

			// args=(String[]) argList.toArray(new String [argList.size()]);
			//
			//
			// time to initialize Visualizer:
			VisualizerProxy visualizerProxy = (VisualizerProxy) context.getBeansOfType(VisualizerProxyService.class).values().toArray()[0];
			visualizerService.init(visualizerProxy);

			// CompetitionStartThread competitionStartThread = new
			// CompetitionStartThread();
			// new Thread(competitionStartThread).start();

			// return "Simulation started";
			// } else {
			// return "Can't do that, sim is running.";
			// }
			List<String> names = new ArrayList<String>();
			for (Iterator iterator = gameParamaters.getBrokers().iterator(); iterator.hasNext();) {
				FakeBroker type = (FakeBroker) iterator.next();
				names.add(type.getName());
			}
			// web components treat empty forms as "", not null.
			String boot = gameParamaters.getBootstrapData().equals("") ? null : gameParamaters.getBootstrapData();
			String serverConfig = gameParamaters.getServerConfig().equals("") ? null : gameParamaters.getServerConfig();
			String jmsUrl = gameParamaters.getJmsUrl().equals("") ? null : gameParamaters.getJmsUrl();
			String logSuffix = gameParamaters.getLogSuffix().equals("") ? null : gameParamaters.getLogSuffix();

			System.out.println(boot + serverConfig + jmsUrl + logSuffix + names);

			String result = css.simSession(boot, serverConfig, jmsUrl, logSuffix, names);
			

			if (result == null) {
				return "Simulation started.";
			} else {
				context.close();
				return "ERROR: " + result;
			}
		}
		return "Sim mode in progress.";
	}

	public String runBoot() throws IOException {
	

		if (!bootRunning) {
			log.info("java.class.path ===>" + System.getProperty("java.class.path"));

			context = new FileSystemXmlApplicationContext(resourceLoader.getResource("WEB-INF/spring/powertac.xml").getURL().toString());

			context.registerShutdownHook();
			css = (CompetitionSetupService) context.getBeansOfType(CompetitionSetupService.class).values().toArray()[0];

			// web components treat empty forms as "", not null.
			String bootFilename = gameParamaters.getBootstrapFilename().equals("") ? null : gameParamaters.getBootstrapFilename();
			String serverConfig = gameParamaters.getServerConfig().equals("") ? null : gameParamaters.getServerConfig();
			String logSuffix = gameParamaters.getLogSuffix().equals("") ? null : gameParamaters.getLogSuffix();
			css.bootSession(bootFilename, serverConfig, logSuffix);

			System.out.println(bootFilename + serverConfig + logSuffix);

			String result = css.bootSession(bootFilename, serverConfig, logSuffix);

			if (result == null) {

				return "Bootstrap mode started.";
			} else {
				context.close();
				return "ERROR: " + result;
			}
		}

		return "Bootstrap mode in progress.";
	}

	// to avoid GUI blocking
	// class CompetitionStartThread implements Runnable {
	//
	// public void run() {
	//
	// simRunning = true;
	//
	// css.processCmdLine(args);
	// context.close();
	// simRunning = false;
	// // start new competition
	// // runSim();
	//
	// }
	// }

	public String forceClose() {
		
		if (context!=null) {
			context.close();	
		}
		bootRunning=false;
		simRunning = false;
		return "Force close completed.";

	}

}
