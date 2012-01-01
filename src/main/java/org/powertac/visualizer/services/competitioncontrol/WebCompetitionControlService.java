package org.powertac.visualizer.services.competitioncontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.powertac.common.PluginConfig;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.ServerProperties;
import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.common.repo.PluginConfigRepo;
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
	private boolean simRunning = false;

	public String run() throws IOException {

		if (!simRunning) {
			System.out.println("java.class.path ===>" + System.getProperty("java.class.path"));

				context = new FileSystemXmlApplicationContext(resourceLoader.getResource("WEB-INF/spring/powertac.xml")
						.getFile().getCanonicalPath());

				context.registerShutdownHook();
			
			// find the CompetitionControl bean
			cc = (CompetitionControlService) context.getBeansOfType(CompetitionControl.class).values().toArray()[0];

			serverProps = (ServerPropertiesService) context.getBeansOfType(ServerProperties.class).values().toArray()[0];

			// set user config
			String testPropertiesPath = resourceLoader.getResource("WEB-INF/config/test.properties").getFile()
					.getCanonicalPath();

			serverProps.setUserConfig(testPropertiesPath);

			ArrayList<String> brokerList = new ArrayList<String>();
			// brokerList.add("grailsDemo");

			cc.setAuthorizedBrokerList(brokerList);
			cc.preGame();

			// time to initialize Visualizer:
			VisualizerProxy visualizerProxy = (VisualizerProxy) context.getBeansOfType(VisualizerProxyService.class)
					.values().toArray()[0];
			visualizerService.init(visualizerProxy);
			
			CompetitionStartThread competitionStartThread = new CompetitionStartThread();
			new Thread(competitionStartThread).start();

			
			return "Simulation started";
		} else {
			return "Can't do that, sim is running.";
		}
	}

	// to avoid GUI blocking
	class CompetitionStartThread implements Runnable {

		public void run() {
			try {
				File bootdata = new File(resourceLoader.getOutputCanonicalPath(), "bootdata.xml");
				FileWriter bootWriter = new FileWriter(bootdata);
				// cc.setAuthorizedBrokerList(new ArrayList<String>());
				simRunning = true;
				cc.runOnce(bootWriter);
				context.close();
				simRunning = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
