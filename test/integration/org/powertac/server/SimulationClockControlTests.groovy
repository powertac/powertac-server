/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.server

import grails.test.*
import org.joda.time.Instant
import org.powertac.common.TimeService

/**
 * Tests for the timer-based clock control.
 * @author John Collins
 */
class SimulationClockControlTests extends GrailsUnitTestCase 
{
  def timeService // autowire

  long current
  long start
  long base
  long rate
  long modulo

  // mock CompetitionControl
  def ccs = [
    'start': { },
    'resume': { start -> }] as CompetitionControlService
  
  protected void setUp ()
  {
    super.setUp()
    current = new Date().getTime()
    start = current + TimeService.SECOND * 2 // two seconds from now
    base = 10000l
    modulo = TimeService.MINUTE * 30 // 2.5 sec timeslot
    rate = 720l // 5 sec/hr
    timeService.base = base
    timeService.rate = rate
    timeService.modulo = modulo
  }

  protected void tearDown ()
  {
    super.tearDown()
  }
  
  SimulationClockControl getInstance ()
  {
    SimulationClockControl.initialize(ccs, timeService)
    return SimulationClockControl.getInstance()
  }

  void testInit ()
  {
    SimulationClockControl scc = getInstance()
    assertNotNull("instance created", scc)
  }
  
  void testStart ()
  {
    SimulationClockControl scc = getInstance()
    long start = 1000l
    assertFalse("start not set", timeService.start == start)
    scc.setStart start
    assertTrue("start now set", timeService.start == start)
  }
  
  void testFirstTick ()
  {
    SimulationClockControl scc = getInstance()
    scc.setStart(start)
    assertEquals("clear", SimulationClockControl.Status.CLEAR, scc.state)
    scc.scheduleTick()
    // now we wait, and check the notify time
    scc.waitForTick(0)
    long wakeup = new Date().getTime()
    assertEquals("correct time", wakeup, start, 20)
    // stop the clock
    scc.stop()
    assertEquals("stopped", SimulationClockControl.Status.STOPPED, scc.state)
  }
  
  void testMultipleTicks ()
  {
    SimulationClockControl scc = getInstance()
    scc.setStart(start)
    long interval = modulo / timeService.rate
    assertEquals("clear", SimulationClockControl.Status.CLEAR, scc.state)
    scc.scheduleTick()
    // now we wait, and check the notify time
    scc.waitForTick(0)
    long wakeup = new Date().getTime()
    assertEquals ("correct time, tick 0", start, wakeup, 20)
    // short interval - wait one sec and finish
    Thread.sleep(1000)
    scc.complete()
    
    // next timeslot should be at start + interval
    long due = start + interval
    scc.waitForTick(1)
    wakeup = new Date().getTime()
    assertEquals("correct time, tick 1", due, wakeup, 20)
    // long interval - wait 3 sec and finish
    Thread.sleep(4000)
    assertEquals("paused", SimulationClockControl.Status.PAUSED, scc.state)
    scc.complete()
    
    // start should have moved back about 500 msec + post-pause delay
    long newStart = timeService.start
    assertEquals("start pushed back", start + 1000, newStart, 20)
    // another short interval
    due = newStart + interval * 2
    scc.waitForTick(2)
    wakeup = new Date().getTime()
    assertEquals("correct time, tick 2", due, wakeup, 20)
    // stop the clock
    scc.stop()
    assertEquals("stopped", SimulationClockControl.Status.STOPPED, scc.state)
  }
}
