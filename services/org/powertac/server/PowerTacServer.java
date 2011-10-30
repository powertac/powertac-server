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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.powertac.common.interfaces.CompetitionControl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This is the top level of the Power TAC server.
 * @author John Collins
 */
public class PowerTacServer
{
  private static CompetitionControlService cc = null;

  /**
   * Sets up the container, sets up logging, and starts the
   * CompetitionControl service.
   * <p>
   * A simple configuration file can be used in lieu of a web interface to
   * configure and run games. Each line in the file must be in one of two
   * forms:<br/>
   * <code>&nbsp;&nbsp;bootstrap bootstrap-filename boot-config</code><br/>
   * <code>&nbsp;&nbsp;sim bootstrap-filename n</code><br/>
   * where 
   * <dl>
   *  <dt><code>boot-config</code></dt>
   *  <dd>is the optional name of a file containing a
   *   sequence of serialized PluginConfig instances that specifies the
   *   bootstrap setup,</dd>
   *  <dt><code>bootstrap-filename</code></dt>
   *  <dd>is the name of a file to write out a bootstrap dataset in bootstrap
   *   mode, or the name to read from for sim mode,</dd>
   *  <dt><code>n</code></dt>
   *  <dd>is the number of broker logins to expect in sim mode before starting
   *   the simulation.</dd>
   * </dl>
   * To use a configuration file, simply give the filename as a command-line
   * argument.
   *  
   */
  public static void main (String[] args)
  {
    ApplicationContext context =
      new ClassPathXmlApplicationContext("development.xml");
    
    // TODO - temp debug code?
    String[] allBeanNames = context.getBeanNamesForType(Object.class);
    if (allBeanNames != null) {
      for (String beanName : allBeanNames) {
        System.out.println(beanName);
      }
    }
    
    // find the CompetitionControl bean
    cc = (CompetitionControlService)context.getBeansOfType(CompetitionControl.class).values().toArray()[0];

    // pick up and process the command-line arg if it's there
    if (args.length == 1) {
      // running from config file
      try {
        BufferedReader config = new BufferedReader(new FileReader(args[0]));
        String input;
        while ((input = config.readLine()) != null) {
          String[] tokens = input.split("\\s+");
          if ("bootstrap".equals(tokens[0])) {
            
            // bootstrap mode - dataset fn is tokens[1], config fn is tokens[2]
            if (tokens.length < 2) {
              System.out.println("Bad input " + input);
            }
            else {
              FileWriter bootWriter = new FileWriter(tokens[1]);
              if (tokens.length > 2) {
                FileReader configReader = new FileReader(tokens[2]);
                cc.preGame(configReader);
              }
              else {
                cc.preGame();
              }
              cc.runOnce(bootWriter);
            }
          }
          else if ("sim".equals(tokens[0])) {
            
            // sim mode, dataset fn is tokens[1]
            if (tokens.length != 2) {
              System.out.println("Bad input " + input);
            }
            else {
              FileReader bootReader = new FileReader(tokens[1]);
              if (cc.preGame(bootReader)) {
                cc.runOnce(bootReader);
              }
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
    else {
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
  }
}
