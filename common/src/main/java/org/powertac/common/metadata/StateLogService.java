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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.state.StateLogging;
import org.springframework.stereotype.Component;

/**
 * Initializes state log files by copying the current domain schema to the log.
 * The expectation is that the init() method be called after the state log is
 * created, and before any log entries are added.
 * 
 * This service lives in the common module because this is where the metadata
 * resource file resides, and because the metadata in that file is derived from
 * this module.
 * 
 * @author John Collins
 */
@Component
public class StateLogService
{
  static private Logger log = LogManager.getLogger(StateLogService.class);
  private Logger stateLog = LogManager.getLogger("State");

  public StateLogService ()
  {
    super();
  }

  /**
   * Initializes the state log without abbreviating classnames.
   */
  public void init ()
  {
    init(false);
  }
  
  /**
   * Initializes the state log by writing the log schema at the top.
   * If abbreviateClassnames is true, then the state log will have
   * org.powertac classnames abbreviated.
   */
  public void init (boolean abbreviateClassnames)
  {
    InputStream schema =
            getClass().getClassLoader().getResourceAsStream("metadata/domain.schema");
    log.debug("found schema");
    Reader rdr = new InputStreamReader(schema);
    BufferedReader reader = new BufferedReader(rdr);
    String line;
    try {
      while (null != (line = reader.readLine())) {
        stateLog.info(line);
        log.debug("Schema line {}", line);
      }
    }
    catch (IOException ioe) {
      log.error("failed to read from schema");
    }

    if (abbreviateClassnames) {
      StateLogging.setClassnameAbbreviation(true);
    }
  }
}
