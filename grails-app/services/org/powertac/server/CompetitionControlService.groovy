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
package org.powertac.server

import org.joda.time.Instant
import org.powertac.common.interfaces.TimeslotPhaseProcessor
import org.powertac.common.Competition

/**
 * This is the competition controller. It has three major roles in the
 * server:
 * <ol>
 * <li>At server startup, it sets up the environment and manages the
 * broker login process, most of which is delegated to the BrokerProxy.</li>
 * <li>Once the simulation starts, it is awakened every 
 * <code>timeslotLength</code> seconds and runs through
 * <code>timeslotPhaseCount</code> phases, calling the <code>activate()</code>
 * methods on registered components. Phases start at 1; by default there
 * are 3 phases.</li>
 * <li>When the number of timeslots equals <code>timeslotCount</code>, the
 * simulation is ended and the database is dumped.</li>
 * </ol>
 * @author jcollins
 */
class CompetitionControlService {

  static transactional = false
  
  int timeslotPhaseCount = 3 // # of phases/timeslot
  int timeslotLength = 5 // seconds
  int timeslotCount = 60 // default length of game
  boolean running = false
  
  def timeService // inject simulation time service dependency
  def jmsManagementService

  def phaseRegistrations
  int timeslotCounter = 0
  
  /**
   * Runs the initialization process and starts the simulation.
   */
  void init ()
  {
    // TODO - other initialization code goes here

    jmsManagementService.createQueues(Competition.currentCompetition())
    
    timeslotCounter = timeslotCount
    start()
  }

  /**
   * Sign up for notifications
   */
  void registerTimeslotPhase(TimeslotPhaseProcessor thing, 
                             int phase)
  {
    if (phase <= 0 || phase > timeslotPhaseCount) {
      log.error "phase ${phase} out of range (1..${timeslotPhaseCount})"
    }
    else {
      if (phaseRegistrations == null) {
        phaseRegistrations = new List[timeslotPhaseCount]
      }
      phaseRegistrations[phase - 1].add(thing)
    }
  }
  
  /**
   * Starts the simulation.  
   */
  void start ()
  {
    running = true
    scheduleStep()
  }

  /**
   * Schedules a step of the simulation
   */
  void scheduleStep ()
  {
    timeService.addAction(new Instant(time.millis + TimeService.HOUR),
      { this.step() })
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
      fnList*.activate(time, index + 1)
    }
    if (--timeslotCounter <= 0) {
      log.info "Stopping simulation after ${timeslotCount} steps"
      running = false
    }
    else {
      log.info "Schedule step"
      scheduleStep()
    }
  }
  
  /**
   * Stops the simulation.
   */
  void stop ()
  {
    running = false
  }
}
