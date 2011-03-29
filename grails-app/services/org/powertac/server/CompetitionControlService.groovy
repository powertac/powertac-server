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
import org.powertac.common.Competition
import org.powertac.common.Timeslot
import org.powertac.common.TimeService
import org.powertac.common.ClockDriveJob
import org.powertac.common.interfaces.TimeslotPhaseProcessor

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
 * @author John Collins
 */
class CompetitionControlService {

  static transactional = false
  
  Competition competition
  
  int timeslotPhaseCount = 3 // # of phases/timeslot
  boolean running = false
  
  def quartzScheduler
  def clockDriveJob
  def timeService // inject simulation time service dependency
  def jmsManagementService

  def phaseRegistrations
  int timeslotCounter = 0
  long timeslotMillis
  
  /**
   * Runs the initialization process and starts the simulation.
   */
  void init ()
  {
    // to enhance testability, initialization is split into a static phase
    // followed by starting the clock
    if (setup() == false)
      return
    // TODO - other initialization code goes here
    start(competition.timeslotLength * TimeService.MINUTE / competition.simulationRate)
  }

  /**
   * Sign up for notifications
   */
  void registerTimeslotPhase(TimeslotPhaseProcessor thing, int phase)
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
  void start (long scheduleMillis)
  {
    // TODO - wait for start time
    running = true
    // Start up the clock
    timeService.updateTime()
    quartzScheduler.start()
    ClockDriveJob.schedule(scheduleMillis)
    scheduleStep()
  }

  /**
   * Schedules a step of the simulation
   */
  void scheduleStep ()
  {
    timeService.addAction(new Instant(timeService.currentTime.millis + timeslotMillis),
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
    log.info "step at $time"
    phaseRegistrations.eachWithIndex { fnList, index ->
      fnList*.activate(time, index + 1)
    }
    if (--timeslotCounter <= 0) {
      log.info "Stopping simulation after ${timeslotCount} steps"
      // TODO - variable length game (optional?)
      running = false
    }
    else {
      activateNextTimeslot()
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
  
  //--------- local methods -------------
  boolean setup ()
  {
    if (Competition.count() > 1) {
      log.error "more than one Competition instance in db - cannot start"
      return false
    }
    competition = Competition.list().first()
    if (competition == null) {
      log.error "no competition instance available - cannot start"
      return false
    }
    // set up broker queues (are they logged in already?)
    jmsManagementService.createQueues()
    
    // grab setup parameters, set up initial timeslots, including zero timeslot
    timeslotMillis = competition.timeslotLength * TimeService.MINUTE
    timeslotCounter = competition.minimumTimeslotCount
    timeService.currentTime = competition.simulationBaseTime
    createInitialTimeslots(competition.simulationBaseTime,
                           competition.deactivateTimeslotsAhead,
                           competition.timeslotsOpen)
    // TODO - send open timeslots to brokers

    // set simulation time parameters, making sure that simulationStartTime
    // is still sufficiently in the future.
    timeService.base = competition.simulationBaseTime.millis
    timeService.start = competition.simulationStartTime.millis
    long rate = competition.simulationRate
    long rem = rate % competition.timeslotLength
    if (rem > 0) {
      long mult = competition.simulationRate / competition.timeslotLength
      log.warn "Simulation rate ${rate} not a multiple of ${competition.timeslotLength}; adjust to ${(mult + 1) * competition.timeslotLength}"
      rate = (mult + 1) * competition.timeslotLength
    }
    timeService.rate = rate
    timeService.modulo = competition.timeslotLength * TimeService.MINUTE
    return true
  }
  
  void createInitialTimeslots (Instant base, int initialSlots,
                                       int openSlots)
  {
    long start = base.millis - timeslotMillis // first step happens before first clock update
    for (i in 0..<initialSlots) {
      Timeslot ts = 
          new Timeslot(serialNumber: i, 
                       startInstant: new Instant(start + i * timeslotMillis), 
                       endInstant: new Instant(start + (i + 1) * timeslotMillis))
      ts.save()
    }
    for (i in initialSlots..<(initialSlots + openSlots)) {
      Timeslot ts =
          new Timeslot(serialNumber: i,
                       enabled: true,
                       startInstant: new Instant(start + i * timeslotMillis),
                       endInstant: new Instant(start + (i + 1) * timeslotMillis))
      ts.save()
    }
  }
  
  void activateNextTimeslot ()
  {
    // first, deactivate the oldest active timeslot
    Timeslot current = Timeslot.currentTimeslot()
    if (current == null) {
      log.error "current timeslot is null at ${timeService.currentTime}!"
      return
    }
    int oldSerial = current.serialNumber + competition.deactivateTimeslotsAhead
    Timeslot oldTs = Timeslot.findBySerialNumber(oldSerial)
    oldTs.enabled = false
    oldTs.save()
    log.info "Deactivated timeslot $oldSerial, start ${oldTs.startInstant}"
    
    // then create if necessary and activate the newest timeslot
    int newSerial = current.serialNumber + competition.deactivateTimeslotsAhead + competition.timeslotsOpen
    Timeslot newTs = Timeslot.findBySerialNumber(newSerial)
    if (newTs == null) {
      long start = current.startInstant.millis + (newSerial - current.serialNumber) * timeslotMillis
      newTs = new Timeslot(serialNumber: lastTimeslotSerial,
                           enabled: true,
                           startInstant: new Instant(start),
                           endInstant: new Instant(start + timeslotMillis))
    }
    else {
      newTs.enabled = true
    }
    newTs.save()
    log.info "Activated timeslot $newSerial, start ${newTs.startInstant}"
    // TODO - communicate timeslot updates to brokers
  }
}
