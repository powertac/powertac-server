/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.server;

//import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This is the top level of the Power TAC server.
 * @author John Collins
 */
public class PowerTacServer
{
  //static private Logger log = LogManager.getLogger(PowerTacServer.class);

  private static CompetitionSetupService css = null;

  /**
   * Sets up the container, sets up logging, and starts the
   * CompetitionControl service.
   * <p>
   * A simple script file can be used in lieu of a web interface to
   * configure and run games. Each line in the file must be in one of two
   * forms:<br/>
   * <code>&nbsp;&nbsp;bootstrap [--config boot-config]</code><br/>
   * <code>&nbsp;&nbsp;sim [--config sim-config] broker1 broker2 ...</code><br/>
   * where 
   * <dl>
   *  <dt><code>boot-config</code></dt>
   *  <dd>is the optional name of a properties file that specifies the
   *    bootstrap setup. If not provided, then the default
   *    server.properties will be used.</dd>
   *  <dt><code>sim-config</code></dt>
   *  <dd>is the optional name of a properties file that specifies the
   *    simulation setup. If not provided, then the default server.properties
   *    file will be used. It is possible for the sim-config to be different
   *    from the boot-config that produced the bootstrap data file used
   *    in the sim, but many properties will be carried over from the
   *    bootstrap session regardless of the contents of sim-config.</dd>
   *  <dt><code>brokern</code></dt>
   *  <dd>are the broker usernames of the brokers that will be logged in
   *    before the simulation starts. They must attempt to log in after the
   *    server starts.</dd>
   * </dl>
   * To use a configuration file, simply give the filename as a command-line
   * argument.
   *  
   */
  public static void main (String[] args)
  {
    AbstractApplicationContext context = new ClassPathXmlApplicationContext("powertac.xml");
    context.registerShutdownHook();
    
    // find the CompetitionControl and ServerProperties beans
    css = (CompetitionSetupService)context.getBeansOfType(CompetitionSetupService.class).values().toArray()[0];
    
    css.processCmdLine(args);
    // if we get here, it's time to exit
    System.exit(0);
  }
}
