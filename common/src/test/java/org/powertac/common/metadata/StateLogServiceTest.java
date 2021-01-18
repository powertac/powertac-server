/*
 * Copyright (c) 2019 by John Collins
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
package org.powertac.common.metadata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author John Collins
 */
class StateLogServiceTest
{
  StateLogService uut;

//  @BeforeAll
//  public void setLog ()
//  {
//    
//  }

  @BeforeEach
  public void setup ()
  {
//    // delete the existing state log file
//    File oldLog = new File("log/test.state");
//    oldLog.delete();
//    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
//    ctx.reconfigure();
    uut = new StateLogService();
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterEach
  void tearDown () throws Exception
  {
    //System.setProperty("statefile", "log/test.state");
  }

  @Test
  void prefixTest ()
  {
    // re-initialize the state log for this test
    //System.setProperty("statefile", "log/metadata-test.state");
    //((LoggerContext) LogManager.getContext(false)).reconfigure();
    
    // this should put the domain prefix at the front of the log.
    uut.init();

    // add a couple log messages
    Logger stateLog = LogManager.getLogger("State");
    stateLog.info("line1");
    stateLog.info("line2");

    // shut down the logger so we can read it
    LogManager.shutdown();

    // open the logfile and inspect contents
//    try {
//      BufferedReader rdr =
//              new BufferedReader(new FileReader("log/test.state"));
//      String header = rdr.readLine();
//      assertNotNull(header);
//      System.out.println(header);
//      assert(header.contains("Domain-schema-version"));
//      rdr.close();
//    }
//    catch (FileNotFoundException e) {
//      fail("File not found");
//    }
//    catch (IOException ioe) {
//      fail("IO Exception");
//    }
  }

}
