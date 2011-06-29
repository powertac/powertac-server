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
import org.powertac.common.interfaces.CompetitionControl
import org.powertac.common.interfaces.Customer
import org.powertac.common.interfaces.InitializationService
import org.powertac.common.interfaces.TimeslotPhaseProcessor
import org.powertac.common.msg.SimEnd
import org.powertac.common.msg.SimPause
import org.powertac.common.msg.SimResume
import org.powertac.common.msg.SimStart
import org.powertac.common.msg.TimeslotUpdate
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.powertac.common.*
import greenbill.dbstuff.DbCreate
import greenbill.dbstuff.DataExport
import org.hibernate.*
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.ConfigurationHolder

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

  Competition competition // convenience var, invalid across sessions
  String competitionId

  int timeslotPhaseCount = 3 // # of phases/quartzScheduler.start()timeslot
  boolean running = false

  SimulationClockControl clock
  SessionFactory sessionFactory
  def timeService // inject simulation time service dependency
  def jmsManagementService
  def springSecurityService
  def brokerProxyService
  def tariffMarketService
  def randomSeedService
  def abstractCustomerService
  def logService

  def applicationContext
  def grailsApplication

  def dataSource

  def phaseRegistrations
  int timeslotCount = 0
  long timeslotMillis
  Random randomGen

  String dumpFilePrefix = (ConfigurationHolder.config.powertac?.dumpFilePrefix) ?: "logs/PowerTAC-dump-"

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

    // Create default competition
    competition = new Competition(name: "defaultCompetition")
    if (!competition.save()) {
      log.error("could not save competition")
    }
    competitionId = competition.id

    // Set up all the plugin configurations
    phaseRegistrations = null
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
    competition = Competition.get(competitionId)
    if (setup() == false)
      return

    start((long) (competition.timeslotLength * TimeService.MINUTE /
		  competition.simulationRate))
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
        phaseRegistrations[phase - 1] = []
      }
      phaseRegistrations[phase - 1].add(thing)
    }
  }

  /**
   * Starts the simulation.  
   */
  void start (long scheduleMillis)
  {
    logService.start()
    runAsync {
      SimulationClockControl.initialize(this, timeService)
      clock = SimulationClockControl.getInstance()
      // wait for start time
      long now = new Date().getTime()
      //long start = now + scheduleMillis * 2 - now % scheduleMillis
      long start = now + TimeService.SECOND // start in one second
      // communicate start time to brokers
      SimStart startMsg = new SimStart(start: new Instant(start))
      brokerProxyService.broadcastMessage(startMsg)

      // Start up the clock at the correct time
      clock.setStart(start)
      timeService.init()
      //timeService.start = start
      //Thread.sleep(start - new Date().getTime() + 10l)
      //timeService.updateTime()
      if (sessionFactory == null) {
        log.error "could not find hibernate session factory"
      }
      else {

        // Set final paramaters
        running = true

        // run the simulation
        int slot = 0
        clock.scheduleTick()
        while (running) {
          log.info("Wait for tick $slot")
          clock.waitForTick(slot++)
          step()
          clock.complete()
          def hibSession = sessionFactory.getCurrentSession()
          if (hibSession == null) {
            log.error "null hibernate session"
          }
          else {
            hibSession.flush()
          }
        }
      }

      // simulation is complete
      log.info("Stop simulation")
      clock.stop()
      shutDown()
    }
  }

  /**
   * Runs a step of the simulation
   */
  void step ()
  {
    competition = Competition.get(competitionId)
    def time = timeService.currentTime
    log.info "step at $time"
    phaseRegistrations.eachWithIndex { fnList, index ->
      log.info "activate phase ${index + 1}: ${fnList}"
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
    //quartzScheduler.shutdown()

    SimEnd endMsg = new SimEnd()
    brokerProxyService.broadcastMessage(endMsg)

    File dumpfile = new File("${dumpFilePrefix}${competitionId}.xml")

    DataExport de = new DataExport()
    de.dataSource = dataSource
    de.export(dumpfile, 'powertac')

    logService.stop()

    // refresh DB
    DbCreate dc = new DbCreate()
    dc.dataSource = dataSource
    dc.create(grailsApplication)

    sessionFactory.currentSession.clear()

    // reinit game
    preGame()
  }

  //--------- local methods -------------

  boolean setup ()
  {
    // set the clock before configuring plugins - some of them need to
    // know the time.
    if (timeService == null) {
      log.error "autowire failure: timeService"
    }
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

    setTimeParameters()

    // Publish Competition object at right place - when exactly?
    if (!competition.isAttached()) {
      log.warn "Competition ${competitionId} is detached"
      competition.attach()
    }
    competition.brokers = Broker.list().collect { it.username }
    competition.save()
    brokerProxyService.broadcastMessage(competition)

    // Publish default tariffs - they should have been created above
    // in the call to configurePlugins()
    tariffMarketService.publishTariffs()

    // grab setup parameters, set up initial timeslots, including zero timeslot
    timeslotMillis = competition.timeslotLength * TimeService.MINUTE
    timeslotCount = computeGameLength(competition.minimumTimeslotCount,
                                      competition.expectedTimeslotCount)
    List<Timeslot> slots =
        createInitialTimeslots(competition.simulationBaseTime,
                               competition.deactivateTimeslotsAhead,
                               competition.timeslotsOpen)
    TimeslotUpdate msg = new TimeslotUpdate(enabled: slots)
    msg.save()
    brokerProxyService.broadcastMessage(msg)

    // publish customer info
    List<CustomerInfo> customers = abstractCustomerService.generateCustomerInfoList()
    brokerProxyService.broadcastMessage(customers)
    return true
  }

  // set simulation time parameters, making sure that simulationStartTime
  // is still sufficiently in the future.
  void setTimeParameters()
  {
    timeService.base = competition.simulationBaseTime.millis
    timeService.currentTime = competition.simulationBaseTime
    long rate = competition.simulationRate
    long rem = rate % competition.timeslotLength
    if (rem > 0) {
      long mult = competition.simulationRate / competition.timeslotLength
      log.warn "Simulation rate ${rate} not a multiple of ${competition.timeslotLength}; adjust to ${(mult + 1) * competition.timeslotLength}"
      rate = (mult + 1) * competition.timeslotLength
    }
    timeService.rate = rate
    timeService.modulo = competition.timeslotLength * TimeService.MINUTE
  }

  List<Timeslot> createInitialTimeslots (Instant base,
					 int initialSlots,
					 int openSlots)
  {
    List<Timeslot> result = []
    long start = base.millis //- timeslotMillis
                             // first step happens before first clock update
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
      log.error "current timeslot is null at ${timeService.currentTime} !"
      return
    }
    int oldSerial = (current.serialNumber +
		     competition.deactivateTimeslotsAhead)
    Timeslot oldTs = Timeslot.findBySerialNumber(oldSerial)
    oldTs.enabled = false
    oldTs.save()
    log.info "Deactivated timeslot $oldSerial, start ${oldTs.startInstant}"

    // then create if necessary and activate the newest timeslot
    int newSerial = (current.serialNumber +
		     competition.deactivateTimeslotsAhead +
		     competition.timeslotsOpen)
    Timeslot newTs = Timeslot.findBySerialNumber(newSerial)
    if (newTs == null) {
      long start = (current.startInstant.millis +
		    (newSerial - current.serialNumber) * timeslotMillis)
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
    TimeslotUpdate msg = new TimeslotUpdate(enabled: [newTs], disabled: [oldTs])
    msg.save()
    brokerProxyService.broadcastMessage(msg)
  }

  int computeGameLength (minLength, expLength)
  {
    double roll = randomGen.nextDouble()
    // compute k = ln(1-roll)/ln(1-p) where p = 1/(exp-min)
    double k = (Math.log(1.0 - roll) /
		Math.log(1.0 - 1.0 /
			 (expLength - minLength + 1)))
    int length = minLength + (int) Math.floor(k)
    log.info("game-length ${length} (k=${k}, roll=${roll})")
    return length
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
    while (remaining.size() > 0 && tryCounter > 0) {
      InitializationService initializer = remaining[0]
      remaining = (remaining.size() > 1) ?
                   remaining[1..(remaining.size() - 1)] : []
      String success = initializer.initialize(competition, completedPlugins)
      if (success == null) {
        // defer this one
        log.info("deferring ${initializer}")
        remaining << initializer
        tryCounter -= 1
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
    classMap.collect { it.value } // return only the object, which is
				  // the maps' value
  }
  
  // ------- pause-mode broker communication -------
  /**
   * Signals that the clock is paused due to server overrun. The pause
   * must be communicated to brokers.
   */
  void pause ()
  {
    log.info "pause"
    // create and post the pause message
    SimPause msg = new SimPause()
    brokerProxyService.broadcastMessage(msg)
  }
  
  /**
   * Signals that the clock is resumed. Brokers must be informed of the new
   * start time in order to sync their own clocks.
   */
  void resume (long newStart)
  {
    log.info "resume"
    // create and post the resume message
    SimResume msg = new SimResume(start: new Instant(newStart))
    brokerProxyService.broadcastMessage(msg)
  }
  
  /**
   * Allows a broker to request a pause. It may or may not be allowed.
   * If allowed, then the pause will take effect when the current simulation
   * cycle has finished, or immediately if no simulation cycle is currently
   * in progress.
   */
  void requestPause (Broker requester)
  {
    
  }
  
  /**
   * Releases a broker-initiated pause. After the clock is re-started, the
   * resume() method will be called to communicate a new start time.
   */
  void releasePause (Broker requester)
  {
    
  }
}
