package org.powertac.server;

import java.io.IOException;
import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.stereotype.Service;

@Service
public class LogService
{
  private String configFilename = "config/log.config";
  private String filenamePrefix = "powertac";
  
  public LogService ()
  {
    super();
    PropertyConfigurator.configure(configFilename);
  }
  
  public LogService (String config)
  {
    super();
    PropertyConfigurator.configure(config);
  }
  
  /**
   * Sets the filename prefix. This should be set to the hostname
   * or some other distinguishing value.
   */
  public void setPrefix (String prefix)
  {
    filenamePrefix = prefix;
  }
  
  public Logger getStateLogger ()
  {
    return Logger.getLogger("State");
  }

  public void startLog (String id)
  {
    Logger root = Logger.getRootLogger();
    Logger state = getStateLogger();
    root.removeAllAppenders();
    state.removeAllAppenders();
    try {
      PatternLayout logLayout = new PatternLayout("%r %-5p %c{2}: %m%n");
      FileAppender logFile
          = new FileAppender(logLayout, ("log/" + filenamePrefix + id + ".trace"), false);
      root.addAppender(logFile);
      PatternLayout stateLayout = new PatternLayout("%r:%m%n");
      FileAppender stateFile
          = new FileAppender(stateLayout, ("log/" + filenamePrefix + id + ".state"), false);
      state.addAppender(stateFile);
    }
    catch (IOException ioe) {
      System.out.println("Can't open log file");
      System.exit(0);
    }
  }

  public void stopLog ()
  {
    stopLogger(Logger.getRootLogger());
    stopLogger(Logger.getLogger("State"));
  }
  
  private void stopLogger (Logger logger)
  {
    for (Enumeration<Appender> apps = logger.getAllAppenders(); apps.hasMoreElements(); ) {
      apps.nextElement().close();
    }
  }
}
