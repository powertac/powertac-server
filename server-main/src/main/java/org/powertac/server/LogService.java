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

import java.io.IOException;
import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.stereotype.Service;

/**
 * Support for per-game logging. Two logs are generated: a trace log, and a state log.
 * The trace log is called "hhhxxx.trace" where hhh is the prefix provided as the argument
 * to setPrefix(), and xxx is the id if the Competition instance. Ideally, hhh is the
 * hostname of the machine running the sim.
 * <p>
 * The contents of the trace are intended to be error, warn, info, or debug messages that
 * help developers or users understand what the server is doing. Loggers are named after
 * the classes in which they are generated. Each class that will use a logger must include
 * a statement
 * <pre>  static private Logger log = Logger.getLogger(ClassName.class.getName());
 * </pre>
 * where ClassName is the name of the class.</p>
 * <p>
 * The state log is a record of state changes, intended to allow complete reconstruction
 * of a simulation. You get one with the statement
 * <pre>  static private Logger stateLog = Logger.getLogger("State");
 * </pre>
 * Entries in the state log are of the form
 * <pre>  type:class:id:op:arg1:...
 * </pre>
 * where type is one of [c,u,d] for create, update, delete; id is the identifier of the
 * object, op (used only for update) is the operation, and the args are the arguments for
 * that operation. The logger format will prepend the current offset from the beginning 
 * of the simulation in milliseconds.</p>
 * @author John Collins
 */
@Service
public class LogService
{
  //private String configFilename = "src/main/resources/log.config";
  private String filenamePrefix = "powertac";
  private String outputPath = "log"; //added
  
  public LogService ()
  {
    super();
    //PropertyConfigurator.configure(configFilename);
  }
  
  public LogService (String config)
  {
    super();
    PropertyConfigurator.configure(config);
  }
  
  	//added:
	public void setOutputPath(String outputPath) 
	{
	    if(outputPath!=null)
	    this.outputPath = outputPath;
	}
  
  /**
   * Sets the filename prefix. This should be set to the hostname
   * or some other distinguishing value.
   */
  public void setPrefix (String prefix)
  {
    filenamePrefix = prefix;
  }
  
 //modified 
  public Logger getStateLogger() 
  {
	    Logger stateLogger = Logger.getLogger("State");
	    //Don't send logs from this logger to "higher" loggers (root, for instance):
	    stateLogger.setAdditivity(false);   //helps to avoid writing logs on the web-app console
	    return stateLogger;
  }
  //added
  private Logger getPowertacLogger(){
	    Logger powertacLogger = Logger.getLogger("org.powertac");
	    //Don't send logs from this logger to "higher" loggers (root, for instance):
	    powertacLogger.setAdditivity(false);
	     return powertacLogger;
  }
  //modified
  public void startLog(long id) 
  {
	   
		
		Logger state = getStateLogger();
		getPowertacLogger().removeAllAppenders();
		state.removeAllAppenders();
		try {
		 PatternLayout logLayout = new PatternLayout("%r %-5p %c{2}: %m%n");
		 
		 
		String traceAppenderName= outputPath+ "/" + filenamePrefix + id +".trace";
		 System.out.print(traceAppenderName);
		
		 FileAppender logFile
		 = new FileAppender(logLayout, traceAppenderName, false);
		 getPowertacLogger().addAppender(logFile); 
		 
		 PatternLayout stateLayout = new PatternLayout("%r:%m%n");
		 
		 String stateAppenderName= outputPath+ "/" + filenamePrefix + id + ".state";
		 FileAppender stateFile
		 = new FileAppender(stateLayout, stateAppenderName, false);
		 state.addAppender(stateFile);
		 }
		 catch (IOException ioe) {
		 System.out.println("Can't open log file");
		 System.exit(0);
		 }
	}

  public void stopLog ()
  {
	  stopLogger(getPowertacLogger()); //modified
	  stopLogger(Logger.getLogger("State"));
  }
  
  @SuppressWarnings("unchecked")
  private void stopLogger (Logger logger)
  {
    for (Enumeration<Appender> apps = logger.getAllAppenders(); apps.hasMoreElements(); ) {
      apps.nextElement().close();
    }
  }
}
