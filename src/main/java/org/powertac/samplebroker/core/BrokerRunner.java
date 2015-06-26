/*
 * Copyright 2012 the original author or authors.
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
package org.powertac.samplebroker.core;

import java.io.File;
import java.util.Date;
import java.util.Enumeration;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Multi-session broker runner. The Spring context is re-built for each
 * session.
 * @author John Collins
 */
public class BrokerRunner
{
  private AbstractApplicationContext context;
  private PowerTacBroker broker;
  
  public BrokerRunner ()
  {
    super();
  }
  
  public void processCmdLine (String[] args)
  {
    OptionParser parser = new OptionParser();
    OptionSpec<String> jmsUrlOption =
            parser.accepts("jms-url").withRequiredArg().ofType(String.class);
    OptionSpec<File> configOption = 
            parser.accepts("config").withRequiredArg().ofType(File.class);
    OptionSpec<Integer> repeatCountOption = 
            parser.accepts("repeat-count").withRequiredArg().ofType(Integer.class);
    OptionSpec<Integer> repeatHoursOption = 
            parser.accepts("repeat-hours").withRequiredArg().ofType(Integer.class);
    OptionSpec<String> queueNameOption =
            parser.accepts("queue-name").withRequiredArg().ofType(String.class);
    OptionSpec<String> serverQueueOption =
            parser.accepts("server-queue").withRequiredArg().ofType(String.class);
    parser.accepts("no-ntp");

    // do the parse
    OptionSet options = parser.parse(args);

    File configFile = null;
    String jmsUrl = null;
    boolean noNtp = false;
    String queueName = null;
    String serverQueue = null;
    Integer repeatCount = 1;
    long end = 0l;
    
    try {
      // process broker options
      System.out.println("Options: ");
      if (options.has(configOption)) {
        configFile = options.valueOf(configOption);
        System.out.println("  config=" + configFile.getName());
      }
      if (options.has(jmsUrlOption)) {
        jmsUrl = options.valueOf(jmsUrlOption);
        System.out.println("  jms-url=" + jmsUrl);
      }
      if (options.has("no-ntp")) {
        noNtp = true;
        System.out.println("  no ntp - estimate offset");
      }
      if (options.has(repeatCountOption)) {
        repeatCount = options.valueOf(repeatCountOption);
        System.out.println("  repeat " + repeatCount + " times");
      }
      else if (options.has(repeatHoursOption)) {
        Integer repeatHours = options.valueOf(repeatCountOption);
        System.out.println("  repeat for " + repeatHours + " hours");
        long now = new Date().getTime();
        end = now + 1000 * 3600 * repeatHours;
      }
      if (options.has(queueNameOption)) {
        queueName = options.valueOf(queueNameOption);
        System.out.println("  queue-name=" + queueName);
      }
      if (options.has(serverQueueOption)) {
        serverQueue = options.valueOf(serverQueueOption);
        System.out.println("  server-queue=" + serverQueue);
      }
      
      // at this point, we are either done, or we need to repeat
      int counter = 0;
      while ((null != repeatCount && repeatCount > 0) ||
              (new Date().getTime() < end)) {
        counter += 1;

        // Re-open the logfiles
        reopenLogs(counter);
        
        // initialize and run
        if (null == context) {
          context = new ClassPathXmlApplicationContext("broker.xml");
        }
        else {
          context.close();
          context.refresh();
        }
        // get the broker reference and delegate the rest
        context.registerShutdownHook();
        broker = (PowerTacBroker)context.getBeansOfType(PowerTacBroker.class).values().toArray()[0];
        System.out.println("Starting session " + counter);
        broker.startSession(configFile, jmsUrl, noNtp, queueName, serverQueue, end);
        if (null != repeatCount)
          repeatCount -= 1;
      }
    }
    catch (OptionException e) {
      System.err.println("Bad command argument: " + e.toString());
    }
  }

  // reopen the logfiles for each session
  private void reopenLogs(int counter)
  {
    Logger root = Logger.getRootLogger();
    @SuppressWarnings("unchecked")
    Enumeration<Appender> rootAppenders = root.getAllAppenders();
    FileAppender logOutput = (FileAppender) rootAppenders.nextElement();
    // assume there's only the one, and that it's a file appender
    logOutput.setFile("log/broker" + counter + ".trace");
    logOutput.activateOptions();
    
    Logger state = Logger.getLogger("State");
    @SuppressWarnings("unchecked")
    Enumeration<Appender> stateAppenders = state.getAllAppenders();
    FileAppender stateOutput = (FileAppender) stateAppenders.nextElement();
    // assume there's only the one, and that it's a file appender
    stateOutput.setFile("log/broker" + counter + ".state");
    stateOutput.activateOptions();
  }
}
