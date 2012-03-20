package org.powertac.visualizer.services.competitioncontrol;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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

	private AbstractApplicationContext context;
	private boolean simRunning = false;
    private String[] args;

	public String runSim() throws IOException {

		if (!simRunning) {
			log.info("java.class.path ===>" + System.getProperty("java.class.path"));

			context = new FileSystemXmlApplicationContext(resourceLoader.getResource("WEB-INF/spring/powertac.xml")
					.getFile().getCanonicalPath());

			context.registerShutdownHook();

			// find the CompetitionControlService bean
			css = (CompetitionSetupService) context.getBeansOfType(CompetitionSetupService.class).values().toArray()[0];
		
						
			URL bootstrapURL = resourceLoader.getResource("WEB-INF/config/boot-data.xml").getURL();
			log.info("BOOTSTRAP_url:"+bootstrapURL);

			List<String> argList = new ArrayList<String>();
			argList.add("--sim");
			argList.add("--boot-data="+bootstrapURL.toString());
			argList.add("--brokers=Markec");
			
						
		//	String userConfURL = resourceLoader.getResource("WEB-INF/config/banana.txt").getURL().toString();
			 
			
			
		//	log.info("USERCONF:"+userConfURL);
			
		//	argList.add("--config="+userConfURL);
			
			args=(String[]) argList.toArray(new String [argList.size()]);
			
			
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
				 
				simRunning = true;
				
				css.processCmdLine(args);
				context.close();
				simRunning = false;
				// start new competition
			//runSim();
			
		}

	}
	
	
	public String stopSim() {
		
		context.close();
		simRunning=false;
		return "return from stopSim";
		
	}
	

}
