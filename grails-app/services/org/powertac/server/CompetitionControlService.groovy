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

import greenbill.dbstuff.DataExport
import org.joda.time.Instant
import org.powertac.common.interfaces.CompetitionControl
import org.powertac.common.interfaces.Customer
import org.powertac.common.interfaces.InitializationService
import org.powertac.common.interfaces.TimeslotPhaseProcessor
import org.powertac.common.msg.SimStart
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.powertac.common.*

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
class CompetitionControlService
implements ApplicationContextAware, CompetitionControl
{
  static transactional = false

  Competition competition

  int timeslotPhaseCount = 3 // # of phases/quartzScheduler.start()timeslot
  boolean running = false

  def quartzScheduler
  def clockDriveJob
  def timeService // inject simulation time service dependency
  def jmsManagementService
  def springSecurityService
  def brokerProxyService
  def randomSeedService

  def applicationContext

  def dataSource

  def phaseRegistrations
  int timeslotCount = 0
  long timeslotMillis
  Random randomGen

  String dumpFilePrefix = "logs/PowerTAC-dump-"

  /**
   * Pre-game server setup - creates the basic configuration elements
   * to make them accessible to the web-based game-setup functions.
   */
  void preGame ()
  {
    log.info "pre-game initialization"
    // Create admin role
    def adminRole = Role.findByAuthority('ROLE_ADMIN') ?: new Role(authority: 'ROLE_ADMIN')
    assert adminRole.save()

    // Create default broker which is admin at the same time
    /*def defaultBroker = Broker.findByUsername('defaultBroker') ?: new Broker(
        username: 'defaultBroker', local: true,
        password: springSecurityService.encodePassword('password'),
        enabled: true)
    if (!defaultBroker.save()) {
      log.error("could not save default broker")
    }
*/
    // Add default broker to admin role
//    if (!defaultBroker.authorities.contains(adminRole)) {
//      BrokerRole.create defaultBroker, adminRole
//    }

    // Create default competition
    competition = new Competition(name: "defaultCompetition")
    if (!competition.save()) {
      log.error("could not save competition")
    }

    // Set up all the plugin configurations
    def initializers = getObjectsForInterface(InitializationService)
    initializers?.each { it.setDefaults() }
    // configure competition instance
    PluginConfig.list().each { config ->
      log.info("adding plugin ${config}")
      competition.addToPlugins(config)
    }
    if (!competition.save()) {
      log.error("could not save competition with plugins")
    }
  }

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

    start((long) (competition.timeslotLength * TimeService.MINUTE / competition.simulationRate))
  }

  /**
   * Sign up for notifications
   */
  void registerTimeslotPhase (TimeslotPhaseProcessor thing, int phase)
  {
    if (phase <= 0 || phase > timeslotPhaseCount) {
      log.error "phase ${phase} out of range (1..${timeslotPhaseCount})"
    }
    else {
      if (phaseRegistrations == null) {
        phaseRegistrations = new List[timeslotPhaseCount]
      }
      if (phaseRegistrations[phase - 1] == null) {
        phaseRegistrations[phase - 1] = [] // do we really have to do this?
      }
      phaseRegistrations[phase - 1].add(thing)
    }
  }

  /**
   * Starts the simulation.  
   */
  void start (long scheduleMillis)
  {
    quartzScheduler.start()
    // wait for start time
    long now = new Date().getTime()
    long start = now + scheduleMillis * 2 - now % scheduleMillis
    // communicate start time to brokers
    SimStart startMsg = new SimStart(start: new Instant(start), brokers: Broker.list().collect { it.username })
    brokerProxyService.broadcastMessage(startMsg)

    // Start up the clock at the correct time
    timeService.start = start
    Thread.sleep(start - new Date().getTime())
    ClockDriveJob.schedule(scheduleMillis)
    timeService.updateTime()
    // Set final paramaters
    running = true
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
      shutDown()
    }
    def time = timeService.currentTime
    log.info "step at $time"
    phaseRegistrations.eachWithIndex { fnList, index ->
      fnList*.activate(time, index + 1)
    }
    if (--timeslotCount <= 0) {
      log.info "Stopping simulation"
      // 
      running = false
      shutDown()
    }
    else {
      activateNextTimeslot()
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

  /**
   * Shuts down the simulation and cleans up
   */
  void shutDown ()
  {
    running = false
    quartzScheduler.shutdown()
    //File dumpfile = new File(dumpFile)
    DataExport de = new DataExport()
    de.dataSource = dataSource
    de.export("*", dumpFilePrefix, 'powertac')
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

    // set the clock before configuring plugins - some of them need to know the time.
    timeService.currentTime = competition.simulationBaseTime
    
    // configure plugins
    if (!configurePlugins()) {
      log.error "failed to configure plugins"
      return false
    }

    // set up random sequence for CCS
    long randomSeed = randomSeedService.nextSeed('CompetitionControlService',
                                                 competition.id, 'game-setup')
    randomGen = new Random(randomSeed)

    // set up broker queues (are they logged in already?)
    jmsManagementService.createQueues()

    // Publish Competition object at right place - when exactly?
    brokerProxyService.broadcastMessage(competition)

    // grab setup parameters, set up initial timeslots, including zero timeslot
    timeslotMillis = competition.timeslotLength * TimeService.MINUTE
    timeslotCount = computeGameLength(competition.minimumTimeslotCount,
                                      competition.expectedTimeslotCount)
    List<Timeslot> slots =
        createInitialTimeslots(competition.simulationBaseTime,
                               competition.deactivateTimeslotsAhead,
                               competition.timeslotsOpen)
    brokerProxyService.broadcastMessage(slots)

    // set simulation time parameters, making sure that simulationStartTime
    // is still sufficiently in the future.
    timeService.base = competition.simulationBaseTime.millis
    long rate = competition.simulationRate
    long rem = rate % competition.timeslotLength
    if (rem > 0) {
      long mult = competition.simulationRate / competition.timeslotLength
      log.warn "Simulation rate ${rate} not a multiple of ${competition.timeslotLength}; adjust to ${(mult + 1) * competition.timeslotLength}"
      rate = (mult + 1) * competition.timeslotLength
    }
    timeService.rate = rate
    timeService.modulo = competition.timeslotLength * TimeService.MINUTE

    // publish customer info
    def customerServiceImplementations = getObjectsForInterface(Customer)
    customerServiceImplementations?.each { Customer customer ->
      CustomerInfo customerInfo = customer.generateCustomerInfo()
      brokerProxyService.broadcastMessage(customerInfo)
    }
    return true
  }

  List<Timeslot> createInitialTimeslots (Instant base, int initialSlots, int openSlots)
  {
    List<Timeslot> result = []
    long start = base.millis //- timeslotMillis // first step happens before first clock update
    for (i in 0..<initialSlots) {
      Timeslot ts =
      new Timeslot(serialNumber: i,
          startInstant: new Instant(start + i * timeslotMillis),
          endInstant: new Instant(start + (i + 1) * timeslotMillis))
      ts.save()
      //result << ts
    }
    for (i in initialSlots..<(initialSlots + openSlots)) {
      Timeslot ts =
      new Timeslot(serialNumber: i,
          enabled: true,
          startInstant: new Instant(start + i * timeslotMillis),
          endInstant: new Instant(start + (i + 1) * timeslotMillis))
      ts.save()
      result << ts
    }
    return result
  }

  void activateNextTimeslot ()
  {
    // first, deactivate the oldest active timeslot
    Timeslot current = Timeslot.currentTimeslot()
    if (current == null) {
      log.error "current timeslot is null at ${timeService.currentTime}!"
      return
    }
    int oldSerial = current.serialNumber + competition.deactivateTimeslotsAhead - 1
    Timeslot oldTs = Timeslot.findBySerialNumber(oldSerial)
    oldTs.enabled = false
    oldTs.save()
    log.info "Deactivated timeslot $oldSerial, start ${oldTs.startInstant}"

    // then create if necessary and activate the newest timeslot
    int newSerial = current.serialNumber + competition.deactivateTimeslotsAhead + competition.timeslotsOpen
    Timeslot newTs = Timeslot.findBySerialNumber(newSerial)
    if (newTs == null) {
      long start = current.startInstant.millis + (newSerial - current.serialNumber) * timeslotMillis
      newTs = new Timeslot(serialNumber: newSerial,
          enabled: true,
          startInstant: new Instant(start),
          endInstant: new Instant(start + timeslotMillis))
    }
    else {
      newTs.enabled = true
    }
    newTs.save()
    log.info "Activated timeslot $newSerial, start ${newTs.startInstant}"
    // Communicate timeslot updates to brokers
    brokerProxyService.broadcastMessage([oldTs, newTs])
  }

  int computeGameLength (minLength, expLength)
  {
    double roll = randomGen.nextDouble()
    // compute k = ln(1-roll)/ln(1-p) where p = 1/(exp-min)
    double k = Math.log(1.0 - roll) / Math.log(1.0 - 1.0 / (expLength - minLength + 1))
    //log.info('game-length k=${k}, roll=${roll}')
    return minLength + (int) Math.floor(k)
  }

  boolean configurePlugins ()
  {
    def initializers = getObjectsForInterface(InitializationService)
    List completedPlugins = []
    List deferredInitializers = []
    for (initializer in initializers) {
      String success = initializer.initialize(competition, completedPlugins)
      if (success == null) {
        // defer this one
        log.info("deferring ${initializer}")
        deferredInitializers << initializer
      }
      else if (success == 'fail') {
        log.error "Failed to initialize plugin ${initializer}"
        return false
      }
      else {
        log.info("completed ${success}")
        completedPlugins << success
      }
    }
    int tryCounter = deferredInitializers.size()
    List remaining = deferredInitializers
    while (deferredInitializers.size() > 0 && tryCounter > 0) {
      InitializationService initializer = remaining[0]
      remaining = (remaining.size() > 1) ? remaining[1..(remaining.size() - 1)] : []
      tryCounter -= 1
      String success = initializer.initialize(competition, completedPlugins)
      if (success == null) {
        // defer this one
        log.info("deferring ${initializer}")
        remaining << initializer
      }
      else {
        log.info("completed ${success}")
        completedPlugins << success
      }
    }
    remaining*.each { initializer ->
      log.error("Failed to initialize ${initializer}")
    }
    return true
  }

  void setApplicationContext (ApplicationContext applicationContext)
  {
    this.applicationContext = applicationContext
  }

  def getObjectsForInterface (iface)
  {
    def classMap = applicationContext.getBeansOfType(iface)
    classMap.collect { it.value } // return only the object, which is the maps' value
  }
}
