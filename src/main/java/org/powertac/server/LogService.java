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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
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
  private String filenamePrefix = "powertac";
  
  public LogService ()
  {
    super();
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
    return LogManager.getLogger("State");
  }

  public void startLog () {
    startLog(null);
  }

  public void startLog (String id)
  {
    try {
      String filename = filenamePrefix;
      if (id != null && id.length() > 0) {
        filename += "-" + id;
      }
      
      Logger root = LogManager.getRootLogger();
      Logger state = getStateLogger();
      
      LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
      Configuration cfg = ctx.getConfiguration();
      
      PatternLayout logLayout = PatternLayout.createLayout(
          "%r %-5p %c{2}: %m%n",
          null, cfg, null, null, true, false, null, null
      );
      FileAppender logFile = FileAppender.createAppender(
          "log/" + filename + ".trace",
          "false", "false", root.getName() + "File", "true", "true", "true", "8192",
          logLayout, null, "false", null, cfg
      );
      LoggerConfig newRoot = LoggerConfig.createLogger(
          "false", Level.INFO, root.getName(), "true",
          new AppenderRef[] {
              AppenderRef.createAppenderRef(root.getName() + "File", null, null)
          },
          null, cfg, null
      );
      newRoot.addAppender(logFile, null, null);
      
      PatternLayout stateLayout = PatternLayout.createLayout(
          "%r:%m%n",
          null, cfg, null, null, true, false, null, null
      );
      FileAppender stateFile = FileAppender.createAppender(
          "log/" + filename + ".state",
          "false", "false", state.getName() + "File", "true", "true", "true", "8192",
          stateLayout, null, "false", null, cfg
      );
      LoggerConfig newState = LoggerConfig.createLogger(
          "false", Level.INFO, state.getName(), "true",
          new AppenderRef[] {
              AppenderRef.createAppenderRef(state.getName() + "File", null, null)
          },
          null, cfg, null
      );
      newState.addAppender(stateFile, null, null);
      
      logFile.start();
      cfg.addAppender(logFile);
      
      stateFile.start();
      cfg.addAppender(stateFile);
      
      cfg.removeLogger(root.getName());
      cfg.addLogger(root.getName(), newRoot);
      
      cfg.removeLogger(state.getName());
      cfg.addLogger(state.getName(), newState);
      
      ctx.updateLoggers(cfg);
    }
    catch (Exception ioe) {
      System.out.println("Can't open log file");
      System.exit(0);
    }
  }

  public void stopLog ()
  {
    reset();
  }
  
  private void reset() {
    Logger root = LogManager.getRootLogger();
    Logger state = getStateLogger();
    
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration cfg = ctx.getConfiguration();
    
    ConsoleAppender console = ConsoleAppender.createAppender(
        null, null, "SYSTEM_OUT", "Console", "true", "true"
    );
    AppenderRef[] refs = new AppenderRef[] {
        AppenderRef.createAppenderRef("Console", null, null)
    };
    
    LoggerConfig newRoot = LoggerConfig.createLogger(
        "false", Level.OFF, root.getName(), "true", refs, null, cfg, null
    );
    newRoot.addAppender(console, null, null);
    
    LoggerConfig newState = LoggerConfig.createLogger(
        "false", Level.OFF, state.getName(), "true", refs, null, cfg, null
    );
    newState.addAppender(console, null, null);
    
    console.start();
    cfg.addAppender(console);
    
    cfg.removeLogger(root.getName());
    cfg.addLogger(root.getName(), newRoot);
    
    cfg.removeLogger(state.getName());
    cfg.addLogger(state.getName(), newState);
    
    ctx.updateLoggers(cfg);
  }

}
