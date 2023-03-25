/*
* Copyright (c) 2011, 2020 by John Collins
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.metadata.StateLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Support for per-game logging. Two logs are generated: a trace log, and a state log.
 * The trace log is called "hhhxxx.trace" where hhh is the prefix provided as the argument
 * to setPrefix(), and xxx is the id of the Competition instance. Ideally, hhh is the
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
 * <pre>  msec:class::id::op::arg1::...
 * </pre>
 * where id is the identifier of the
 * object, op is the operation, and the args are the arguments for
 * that operation.</p>
 * @author John Collins
 */
@Service
public class LogService
{
  @Autowired
  private StateLogService stateLogService;
  
  @Autowired
  private ServerConfiguration configService;

  private String filenamePrefix = "powertac";

  @ConfigurableValue(valueType = "Boolean",
          description = "if true, then abbreviate package names in the state log")
  private boolean abbreviateClassnames = false;
  
  public LogService ()
  {
    super();
  }
  
  /**
   * Sets the filename prefix. This should be set to the hostname
   * or some other distinguishing value.
   * Note that this method does not appear to be used anywhere other than in unit tests.
   */
  @Deprecated
  public void setPrefix (String prefix)
  {
    filenamePrefix = prefix;
  }
  
  public String getPrefix ()
  {
    return filenamePrefix;
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
    configService.configureMe(this);
    try {
      String filename = filenamePrefix;
      if (id != null && id.length() > 0) {
        filename += "-" + id;
      }
      
      String logDir = System.getProperty("logdir", "log");
      System.setProperty("logfile", logDir + "/" + filename + ".trace");
      System.setProperty("statefile", logDir + "/" + filename + ".state");
      
      ((LoggerContext) LogManager.getContext(false)).reconfigure();

      stateLogService.init(abbreviateClassnames);
    }
    catch (Exception ioe) {
      ioe.printStackTrace();
      System.out.println("Can't open log file");
      //System.exit(0);
    }
  }

  public void stopLog ()
  {
    // Removing the system props causes log4j2 to revert to the ones
    // given in log4j2.xml (typically init.state and init.trace)
    System.getProperties().remove("logfile");
    System.getProperties().remove("statefile");
    ((LoggerContext) LogManager.getContext(false)).reconfigure();
  }

}
