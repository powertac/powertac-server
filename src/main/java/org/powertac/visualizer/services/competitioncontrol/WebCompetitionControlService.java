package org.powertac.visualizer.services.competitioncontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.powertac.common.PluginConfig;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.server.CompetitionControlService;
import org.powertac.server.LogService;
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

	static private Logger log = Logger
			.getLogger(WebCompetitionControlService.class);

	@Autowired
	private VisualizerResourceLoaderService resourceLoader;

	// @Autowired
	private CompetitionControlService cc;

	// @Autowired
	private PluginConfigRepo pluginConfigRepo;

	@Autowired
	private VisualizerService visualizerService;

	private AbstractApplicationContext context;

	public String preGame() {

		String args[] = {"WEB-INF/config/bootstrap.txt"};
		Resource res = resourceLoader.getResource(args[0]);

		try {
			context = new FileSystemXmlApplicationContext(resourceLoader
					.getResource("WEB-INF/spring/powertac.xml").getFile()
					.getCanonicalPath());

//			 fix output path for simulator log: 
			 Resource res2 = resourceLoader.getResource("WEB-INF/output/log");
			 LogService logService = (LogService) context
			 .getBeansOfType(LogService.class).values().toArray()[0];
			 String outputPath = res2.getFile().getCanonicalPath();
			 logService.setOutputPath(outputPath);

		} catch (BeansException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		context.registerShutdownHook();

		// find the CompetitionControl bean
		cc = (CompetitionControlService) context
				.getBeansOfType(CompetitionControl.class).values().toArray()[0];

		if (args.length == 1) {

			try {
				System.out.println(res.getFile().getAbsolutePath());

				BufferedReader config = new BufferedReader(new FileReader(
						res.getFile()));
				String input;
				while ((input = config.readLine()) != null) {
					String[] tokens = input.split("\\s+");
					if ("bootstrap".equals(tokens[0])) {

						if (tokens.length < 2) {
							System.out.println("Bad input " + input);
						} else {

							cc.setAuthorizedBrokerList(new ArrayList<String>());
							if (tokens.length > 2) {
								File configFile = new File(tokens[2]);
								cc.preGame(configFile);
							} else {
								cc.preGame();

							}

						}
					}

				}
			} catch (FileNotFoundException fnf) {
				System.out.println("Cannot find file " + args[0]);
			} catch (IOException ioe) {
				System.out.println("Error reading file " + args[0]);
			}
		} else if (args.length == 0) {

			System.out.println("Server BootStrap");

			cc.preGame();

		} else {
			System.out.println("Usage: powertac-server [filename]");
		}

		// time to initialize Visualizer:
		VisualizerProxy visualizerProxy = (VisualizerProxy) context
				.getBeansOfType(VisualizerProxyService.class).values()
				.toArray()[0];
		visualizerService.init(visualizerProxy);

		return "preGame completed";
	}

	public String configPlugins() {

		try {

			pluginConfigRepo = (PluginConfigRepo) context
					.getBeansOfType(PluginConfigRepo.class).values().toArray()[0];

			String path;

			// factored-customer
			path = resourceLoader
					.getResource(
							"WEB-INF/config/plugins/factored-customer/FactoredCustomers.xml")
					.getFile().getCanonicalPath();
			PluginConfig newConfig = new PluginConfig("FactoredCustomer", "");
			newConfig.addConfiguration("configResource", path);
			pluginConfigRepo.findByRoleName("FactoredCustomer").update(
					newConfig);

			// household-customer
			path = resourceLoader
					.getResource(
							"WEB-INF/config/plugins/household-customer/Household.properties")
					.getFile().getCanonicalPath();
			newConfig = new PluginConfig("HouseholdCustomer", "");
			newConfig.addConfiguration("configFile", path);
			pluginConfigRepo.findByRoleName("HouseholdCustomer").update(
					newConfig);

			System.out
					.println(pluginConfigRepo
							.findByRoleName("FactoredCustomer")
							.getConfigurationValue("configResource"));

		} catch (IOException e) {
			System.out.println("Error reading config file ");
			e.printStackTrace();
		}

		return "plugin configuration completed";
	}

	public String runOnce() {

		CompetitionStartThread competitionStartThread = new CompetitionStartThread();
		new Thread(competitionStartThread).start();

		return "runOnce completed";
	}

	// to avoid GUI blocking
	class CompetitionStartThread implements Runnable {

		public void run() {
			try {
				File bootdata = new File(
						resourceLoader.getOutputCanonicalPath(), "bootdata.xml");
				FileWriter bootWriter = new FileWriter(bootdata);
				cc.setAuthorizedBrokerList(new ArrayList<String>());

				cc.runOnce(bootWriter);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public String stop() {

		if (cc != null) {
			cc.stop();
			// Because we like brutal:
			context.destroy();
		}
		return "stop completed";
	}

}
