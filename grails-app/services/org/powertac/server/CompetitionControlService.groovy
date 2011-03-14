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
package org.powertac.server

import org.joda.time.Instant
import org.powertac.common.interfaces.TimeslotPhaseProcessor

/**
 * This is the competition controller.
 * @author jcollins
 */
class CompetitionControlService {

  static transactional = false
  
  int timeslotPhaseCount = 3 // # of phases/timeslot
  int timeslotLength = 5 // seconds
  boolean running = false
  
  def timeService // inject simulation time service dependency
  
  def phaseRegistrations

  /**
   * Sign up for notifications
   */
  void registerTimeslotPhase(TimeslotPhaseProcessor thing, 
                             int phase)
  {
    if (phaseRegistrations == null) {
      phaseRegistrations = new List[timeslotPhaseCount]
    }
    phaseRegistrations[phase].add(thing)
  }
  
  /**
   * Starts the simulation.  
   */
  void start ()
  {
    running = true
  }
  
  /**
   * Runs a step of the simulation
   */
  void step ()
  {
    if (!running) {
      log.info("Stop simulation")
      return
    }
    def time = timeService.currentTime
    phaseRegistrations.eachWithIndex { fnList, index ->
      fnList*.activate(time, index)
    }
    timeService.addAction(new Instant(time.millis + TimeService.HOUR),
        { this.step() })
  }
  
  /**
   * Stops the simulation.
   */
  void stop ()
  {
    running = false
  }
}
