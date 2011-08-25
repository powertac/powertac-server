/*
 * Copyright 2009-2010 the original author or authors.
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

package org.powertac.common.enumerations;

/**
 * Describes the status of a competition
 */
public enum CompetitionStatus {
  /**
   * The competition was created but there is no configuration available from either a (remote) file or database
   */
  Created(1),
  /**
   * Once a configuration has been read, the competition is scheduled
   */
  Scheduled(2),
  /**
   * All modules have been given the opportunity to initialize themselves. The initialization process
   * should load large data files or fetch data from remote servers
   */
  Initialized(3),
  /**
   * The competition is running
   */
  Running(4),
  /**
   * The competition is finished
   */
  Finished(5),
  /**
   * The competition has been interrupted (i.e. paused or cancelled)
   */
  Interrupted(6);

  private final int idVal;

  CompetitionStatus(int id) {
    this.idVal = id;
  }

  public int getId() {
    return idVal;
  }
}

