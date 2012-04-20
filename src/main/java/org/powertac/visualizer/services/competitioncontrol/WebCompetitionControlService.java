package org.powertac.visualizer.services.competitioncontrol;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.CompetitionSetup;
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
public class WebCompetitionControlService {

	static private Logger log = Logger.getLogger(WebCompetitionControlService.class);

	@Autowired
	private VisualizerResourceLoaderService resourceLoader;

	@Autowired
	private CompetitionSetup css;

	@Autowired
	private CompetitionControl competitionControl;

	@Autowired
	private VisualizerService visualizerService;

	@Autowired
	private GameParamatersBean gameParamaters;

	@Autowired
	private VisualizerProxy visualizerProxy;

	private String message;
	

	public void runSim() {

		if (!competitionControl.isRunning()) {

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
				message = "ERROR: " + result;
			}

		} else {
			message = "Unable to run a sim game. The competition is already in progress.";
		}
	}

	public void runBoot() {

		if (!competitionControl.isRunning()) {

			visualizerService.init(visualizerProxy);

			// web components treat empty forms as "", not null.
			String bootFilename = gameParamaters.getBootstrapFilename().equals("") ? null : gameParamaters.getBootstrapFilename();
			String serverConfig = gameParamaters.getServerConfig().equals("") ? null : gameParamaters.getServerConfig();
			String logSuffix = gameParamaters.getLogSuffix().equals("") ? null : gameParamaters.getLogSuffix();

			System.out.println(bootFilename + serverConfig + logSuffix);

			String result = css.bootSession(bootFilename, serverConfig, logSuffix);

			if (result == null) {

				message = "Bootstrap mode started.";
			} else {
				message = "ERROR: " + result;
			}
		} else {
			message = "Unable to run a bootstrap game. The competition is already in progress.";
		}
	}

	public void shutDown() {
		if (competitionControl.isRunning()) {
			competitionControl.shutDown();
			message = "Shut down is complete.";
		} else {
			message = "There is no running game to shut down.";
		}
	}

	public String getMessage() {
		return message;
	}



}
