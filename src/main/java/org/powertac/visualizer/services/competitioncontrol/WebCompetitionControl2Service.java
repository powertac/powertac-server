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

	private String message;

	public void runSim() throws IOException {

		log.info("java.class.path ===>" + System.getProperty("java.class.path"));

		context = new FileSystemXmlApplicationContext(resourceLoader.getResource("WEB-INF/spring/powertac.xml").getURL().toString());

		context.registerShutdownHook();

		// find the CompetitionControlService bean
		css = (CompetitionSetupService) context.getBeansOfType(CompetitionSetupService.class).values().toArray()[0];
				
		// time to initialize Visualizer:
		VisualizerProxy visualizerProxy = (VisualizerProxy) context.getBeansOfType(VisualizerProxyService.class).values().toArray()[0];
		visualizerService.init(visualizerProxy);

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
			message = "Simulation started.";
		} else {
			context.close();
			message = "ERROR: " + result;
		}

		message = "Sim mode in progress.";
	}

	public void runBoot() throws IOException {

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

			message = "Bootstrap mode started.";
		} else {
			context.close();
			message = "ERROR: " + result;
		}
	}

	public void forceClose() {

		if (context != null) {
			context.close();
		}
		message = "Force close completed.";

	}
	
	public String getMessage() {
		return message;
	}

}
