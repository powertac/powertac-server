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
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

import org.powertac.common.Competition
import org.powertac.common.Timeslot
import org.powertac.common.TimeService
import org.powertac.common.ClockDriveJob
import org.powertac.common.interfaces.TimeslotPhaseProcessor

import grails.test.*

/**
 * Tests the CompetitionControlService
 * @author John Collins
 */
class CompetitionControlServiceTests extends GrailsUnitTestCase 
{
  def timeService // source of time information
  def competitionControlService // unit under test
  
  Competition competition
  Instant base, start

  boolean queuesCreated = false
  boolean schedulerStarted = false
  int updateCounter = 0
  
  protected void setUp() 
  {
    super.setUp()
    base = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant()
    competition = new Competition(name: "testCompetition")
    //competition = Competition.currentCompetition()
    competitionControlService.competition = competition
    competitionControlService.competitionId = competition.id
    competition.simulationBaseTime = base
    competition.save()
    timeService.currentTime = base

    // mock all needed services other than timeService
    competitionControlService.jmsManagementService = 
      [createQueues: { -> queuesCreated = true }]
    competitionControlService.quartzScheduler = [start: { -> schedulerStarted = true }]
    //clockDriveJob.timeService = [updateTime: { -> updateCounter += 1 }]
  }

  protected void tearDown() 
  {
    super.tearDown()
  }

  void testInit() 
  {
    assertTrue('successful setup', competitionControlService.setup())
    assertTrue('queues created', queuesCreated)
    assertEquals('current time updated', base, timeService.currentTime)
    assertEquals('24 timeslots created', 24, Timeslot.count())
  }
  
  // you have to look at std out to see the results here. Need to mock
  // the random number generator before this will be a proper test.
  void testGameLength ()
  {
    competitionControlService.setup()
    int ml = 100
    int el = 110
    int sum = 0
    int counter = 10000
    for (i in 0..<counter) {
      int gl = competitionControlService.computeGameLength(ml, el)
      //println "count=${gl}"
      sum += gl
    }
    println "mean value: ${sum / (double)counter}"
    assertEquals("reasonable mean", (double)el, (sum / (double)counter), 0.3)
  }
}
