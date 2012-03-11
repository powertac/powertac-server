package org.powertac.visualizer.services;

import java.io.IOException;
import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.powertac.visualizer.beans.VisualizerBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Purpose of this service is to enable log file appender for Visualizer.
 * 
 * @author Jurica Babic
 * 
 */

@Service
public class VisualizerLogService {

    private Logger visualizerLogger;
    private String outputFolder;
    private String filenamePrefix;
    private FileAppender logFile;

    @Autowired
    VisualizerBean visualizerBean;

    public VisualizerLogService(Resource outputFolderResource,
	    String filenamePrefix) {

	this.filenamePrefix = filenamePrefix;
	visualizerLogger = Logger.getLogger("org.powertac.visualizer");
	visualizerLogger.setLevel(Level.DEBUG);

	try {
	    outputFolder = outputFolderResource.getFile().getCanonicalPath();

	} catch (IOException e) {
	    System.out.println("Can't find output folder for Visualizer Log");
	}

    }

    /**
     * Starts file appender for Visualizer. Should be called before each
     * competition run.
     * 
     * @param id
     *            Used as suffix for log filename.
     */
    public void startLog(long id) {

	// should be called from somewhere else
	stopLog();

	

	try {

	    PatternLayout logLayout = new PatternLayout("%r %-5p %c{2}: %m%n");
	    String fullPath = outputFolder + "/" + filenamePrefix + id + ".log";
	    logFile = new FileAppender(logLayout, fullPath, false);
	    visualizerLogger.addAppender(logFile);

	} catch (IOException ioe) {
	    System.out.println("Can't open log file");
	    System.exit(0);
	}
    }

    /**
     * Closes Visualizer log file appender. Should be called after competition
     * instance completion.
     */
    public void stopLog() {

	// add Visualizer summary at the end of log
	logVisualizerSummary();
	
	
	visualizerLogger.removeAppender(logFile);

	
    }

    private void logVisualizerSummary() {

	visualizerLogger.info("SUMMARY:");
	visualizerLogger.info("Number of messages received: "
		+ visualizerBean.getMessageCount());

    }
}
