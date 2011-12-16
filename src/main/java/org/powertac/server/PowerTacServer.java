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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.ServerProperties;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This is the top level of the Power TAC server.
 * @author John Collins
 */
public class PowerTacServer
{
  static private Logger log = Logger.getLogger(PowerTacServer.class);

  private static CompetitionControlService cc = null;
  private static ServerPropertiesService serverProps = null;

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
    cc = (CompetitionControlService)context.getBeansOfType(CompetitionControl.class).values().toArray()[0];
    serverProps = (ServerPropertiesService)context.getBeansOfType(ServerProperties.class).values().toArray()[0];

    // pick up and process the command-line arg if it's there
    if (args.length == 1) {
      // running from config file
      try {
        BufferedReader config = new BufferedReader(new FileReader(args[0]));
        String input;
        while ((input = config.readLine()) != null) {
          String[] tokens = input.split("\\s+");
          if ("bootstrap".equals(tokens[0])) {
            // bootstrap mode - optional config fn is tokens[2]
            if (tokens.length == 2 || tokens.length > 3) {
              System.out.println("Bad input " + input);
            }
            else {
              if (tokens.length == 3 && "--config".equals(tokens[1])) {
                // explicit config file
                serverProps.setUserConfig(tokens[2]);
              }
              FileWriter bootWriter =
                  new FileWriter(serverProps.getProperty("server.bootstrapDataFile",
                                                         "bootstrapData.xml"));
              cc.setAuthorizedBrokerList(new ArrayList<String>());
              cc.preGame();
              cc.runOnce(bootWriter);
            }
          }
          else if ("sim".equals(tokens[0])) {
            int brokerIndex = 1;
            // sim mode, check for --config in tokens[1]
            if (tokens.length < 2) {
              System.out.println("Bad input: " + input);
            }
            else if ("--config".equals(tokens[1])) {
              if (tokens.length < 4) {
                System.out.println("No brokers given for sim: " + input);
              }
              else {
                // explicit config file in tokens[2]
                serverProps.setUserConfig(tokens[2]);
              }
              brokerIndex = 3;
            }
            log.info("In Simulation mode!!!");
            File bootFile =
                new File(serverProps.getProperty("server.bootstrapDataFile",
                                                 "bd-noname.xml"));
            // collect broker names, hand to CC for login control
            ArrayList<String> brokerList = new ArrayList<String>();
            for (int i = brokerIndex; i < tokens.length; i++) {
              brokerList.add(tokens[i]);
            }
            if (brokerList.size() > 0) {
              cc.setAuthorizedBrokerList(brokerList);
              
              if (cc.preGame(bootFile)) {
                cc.runOnce(bootFile);
              }
            }
            else {
              System.out.println("Cannot run sim without brokers");
            }
          }
        }
      }
      catch (FileNotFoundException fnf) {
        System.out.println("Cannot find file " + args[0]);
      }
      catch (IOException ioe ) {
        System.out.println("Error reading file " + args[0]);
      }
    }
    else if (args.length == 0) {
      // running from web interface
      System.out.println("Server BootStrap");
      //participantManagementService.initialize();
      cc.preGame();

      // idle while the web interface controls the simulator
      while(true) {
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {

        }
      }
    }
    else { // usage problem
      System.out.println("Usage: powertac-server [filename]");
    }
    // if we get here, it's time to exit
    System.exit(0);
  }
}
