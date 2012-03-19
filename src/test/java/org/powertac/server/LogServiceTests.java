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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class LogServiceTests
{
  static private LogService logService;
  static private Logger staticLog = Logger.getLogger(LogService.class.getName());
  private Logger log;
  private Logger stateLog;
  
  public LogServiceTests ()
  {
    super();
  }
  
  // one-time setup
  @BeforeClass
  static public void initialize ()
  {
    // delete old logfiles
    new File("log/test.trace").delete();
    new File("log/test.state").delete();
    
    // initialize the log service
    logService = new LogService("src/test/resources/log4j.properties");
  }
  
  // per-test setup
  @Before
  public void setup ()
  {
    // get references to the loggers
    log = Logger.getLogger("test.org.powertac.server.LogServiceTests");
    stateLog = logService.getStateLogger();
  }
  
  // Log something to the default loggers
  @Test
  public void testDefaultLogging ()
  {
    // reinitialize the log service
    logService = new LogService("src/test/resources/log4j.properties");
    assertNotNull("log service got created", logService);
    
    // log to the default trace file, check file
    log.info("first message");
    File traceFile = new File("log/test.trace");
    assertTrue("trace file exists", traceFile.exists());
    try {
      BufferedReader traceReader = new BufferedReader(new FileReader(traceFile));
      String line1 = traceReader.readLine();
      assertNotNull("line one in file", line1);
      String[] fields = line1.split("\\s+");
      assertEquals("5 fields", 5, fields.length);
      assertTrue("first field is a number", fields[0].matches("\\d+"));
      assertTrue("second fields is INFO", fields[1].matches("INFO"));
      traceReader.close();
    }
    catch (Exception e) {
      fail(e.toString());
    }
    
    // log to the state file, check file
    stateLog.info("state-change");
    assertTrue("state file exists", new File("log/test.state").exists());
  }
  
  // set up a sim logger and check it out
  @Test
  public void testSimLogging ()
  {
    logService.setPrefix("test");
    logService.startLog("42");
    
    // write to the trace log and check
    log.info("start sim 42");
    staticLog.warn("warning message");
    File traceFile = new File("log/test-42.trace");
    assertTrue("trace file exists", traceFile.exists());
    
    // write to the state log and check
    stateLog.info("sim-state");
    File stateFile = new File("log/test-42.state");
    assertTrue("state file exists", stateFile.exists());
    try {
      BufferedReader stateReader = new BufferedReader(new FileReader(stateFile));
      String line1 = stateReader.readLine();
      assertNotNull("line one in file", line1);
      String[] fields = line1.split(":");
      assertEquals("2 fields", 2, fields.length);
      assertTrue("first field is a number", fields[0].matches("\\d+"));
      assertTrue("second field is state", fields[1].matches("sim-state"));
      stateReader.close();
    }
    catch (Exception e) {
      fail(e.toString());
    }
    
    logService.stopLog();
  }
  
  // set up a second sim logger and check it out
  @Test
  public void simLoggingAgain ()
  {
    logService.setPrefix("test");
    logService.startLog("43");
    
    // write to the trace log and check
    log.info("start sim 43");
    staticLog.warn("warning message 43");
    File traceFile = new File("log/test-43.trace");
    assertTrue("trace file exists", traceFile.exists());
    
    // write to the state log and check
    stateLog.info("sim-state-43");
    File stateFile = new File("log/test-43.state");
    assertTrue("state file exists", stateFile.exists());
    try {
      BufferedReader stateReader = new BufferedReader(new FileReader(stateFile));
      String line1 = stateReader.readLine();
      assertNotNull("line one in file", line1);
      String[] fields = line1.split(":");
      assertEquals("2 fields", 2, fields.length);
      assertTrue("first field is a number", fields[0].matches("\\d+"));
      assertTrue("second field is state", fields[1].matches("sim-state-43"));
      stateReader.close();
    }
    catch (Exception e) {
      fail(e.toString());
    }
    
    logService.stopLog();
  }
}
