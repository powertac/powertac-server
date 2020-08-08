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

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.powertac.common.metadata.StateLogService;
import org.springframework.test.util.ReflectionTestUtils;

public class LogServiceTests
{
  static private LogService logService;
  static private Logger staticLog = LogManager.getLogger(LogService.class.getName());

  private Logger log;
  private Logger stateLog;

  public LogServiceTests ()
  {
    super();
  }
  
  // one-time setup
  @BeforeAll
  static public void initialize ()
  {
    // create an instance of ServerPropertiesService to inject
    ServerPropertiesService serverProps = new ServerPropertiesService();
    // initialize the log service (config src/test/resources/log4j2-test.xml)
    logService = new LogService();
    ReflectionTestUtils.setField(logService, "configService", serverProps);
    ReflectionTestUtils.setField(logService, "stateLogService", new StateLogService());
  }
  
  // per-test setup
  @BeforeEach
  public void setup ()
  {
    // get references to the loggers
    log = LogManager.getLogger(LogServiceTests.class);
    stateLog = logService.getStateLogger();
  }
  
  // Log something to the default loggers
  @Test
  public void testDefaultLogging ()
  {
    logService.setPrefix("test");
    logService.startLog();
    
    // log to the default trace file, check file
    log.info("first message");
    File traceFile = new File("log/test.trace");
    assertTrue(traceFile.exists(), "trace file exists");
    try {
      BufferedReader traceReader = new BufferedReader(new FileReader(traceFile));
      String line1 = "";
      while (line1 != null && line1.indexOf("first message") == -1) {
        line1 = traceReader.readLine();
      }
      assertNotNull(line1, "line one in file");
      String[] fields = line1.split("\\s+");
      assertEquals(5, fields.length, "5 fields");
      assertTrue(fields[0].matches("\\d+"), "first field is a number");
      assertTrue(fields[1].matches("INFO"), "second fields is INFO");
      traceReader.close();
    }
    catch (Exception e) {
      fail(e.toString());
    }
    
    // log to the state file, check file
    stateLog.info("state-change");
    assertTrue(new File("log/test.state").exists(), "state file exists");
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
    assertTrue(traceFile.exists(), "trace file exists");
    
    // write to the state log and check
    stateLog.info("sim-state");
    File stateFile = new File("log/test-42.state");
    assertTrue(stateFile.exists(), "state file exists");
    try {
      BufferedReader stateReader = new BufferedReader(new FileReader(stateFile));
      String line;
      String[] fields;
      confirmSchemaHeader(stateReader);
      line = stateReader.readLine();
      fields = line.split(":");
      assertEquals(2, fields.length, "two fields");
      assertTrue(fields[0].matches("\\d+"), "first field is a number");
      assertTrue(fields[1].matches("sim-state"));
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
    assertTrue(traceFile.exists(), "trace file exists");
    
    // write to the state log and check
    stateLog.info("sim-state-43");
    File stateFile = new File("log/test-43.state");
    assertTrue(stateFile.exists(), "state file exists");
    try {
      BufferedReader stateReader = new BufferedReader(new FileReader(stateFile));
      confirmSchemaHeader(stateReader);
      String line1 = stateReader.readLine();
      assertNotNull(line1, "line one in file");
      String[] fields = line1.split(":");
      assertEquals(2, fields.length, "2 fields");
      assertTrue(fields[0].matches("\\d+"), "first field is a number");
      assertTrue(fields[1].matches("sim-state-43"), "second field is state");
      stateReader.close();
    }
    catch (Exception e) {
      fail(e.toString());
    }
    
    logService.stopLog();
  }

  private void confirmSchemaHeader (BufferedReader stateReader)
    throws IOException
  {
    String line = stateReader.readLine();
    //System.out.println(line1);
    assertNotNull(line, "line one in file");
    String[] fields = line.split(":");
    assertEquals(3, fields.length, "3 fields schema prefix");
    assertTrue(fields[0].matches("\\d+"), "first field is a number");
    assertTrue(fields[1].matches("Domain-schema-version"), "second field is schema tag");
    while (null != (line = stateReader.readLine())) {
      fields = line.split(":");
      if (fields[1].matches("schema.end"))
        break;
    }
  }
}
