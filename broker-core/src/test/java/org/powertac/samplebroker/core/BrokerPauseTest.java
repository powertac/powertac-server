/*
 * Copyright (c) 2013 by the original author
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
package org.powertac.samplebroker.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Competition;
import org.powertac.common.TimeService;
import org.powertac.common.msg.SimPause;
import org.powertac.common.msg.SimResume;
import org.powertac.common.msg.TimeslotComplete;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author jcollins
 */
public class BrokerPauseTest
{
  private PowerTacBroker broker;
  private Competition competition;
  private TimeService timeService;
  private Instant baseTime;

  /**
   * Create the uut, TimeService, TimeslotRepo, and wire them together
   */
  @BeforeEach
  public void setUp () throws Exception
  {
    broker = new PowerTacBroker();
    Competition.setCurrent(Competition.newInstance("test"));
    competition = Competition.currentCompetition();
    baseTime = ZonedDateTime.of(2010, 6, 21, 0, 0, 0, 0, 
                            ZoneOffset.UTC).toInstant();
    competition.withSimulationBaseTime(baseTime)
        .withSimulationRate(1800); // 2-second timeslots
    timeService = new TimeService(competition.getSimulationBaseTime().toEpochMilli(),
                                  Instant.now().toEpochMilli(),
                                  competition.getSimulationRate(),
                                  competition.getSimulationModulo());
    timeService.updateTime();
    TimeslotRepo tsr = new TimeslotRepo();
    ReflectionTestUtils.setField(tsr, "timeService", timeService);
    ReflectionTestUtils.setField(broker, "timeService", timeService);
    ReflectionTestUtils.setField(broker, "timeslotRepo", tsr);
    tsr.makeTimeslot(timeService.getCurrentTime());
  }

  /**
   * Normal msg sequence tsu ... tc.
   */
  @Test
  public void noPause ()
  {
    try {
      Thread.sleep(2001); // 2.001 seconds
      // it's now the start of ts1
      TimeslotUpdate tsu = new TimeslotUpdate(baseTime.plusMillis(2000),
                                              2, 12);
      broker.handleMessage(tsu);
      assertEquals(baseTime.plusMillis(TimeService.HOUR).toEpochMilli(),
                   timeService.getCurrentTime().toEpochMilli(), "correct time");
      Thread.sleep(1000); // delay by half a timeslot
      TimeslotComplete tc = new TimeslotComplete(1);
      broker.handleMessage(tc);
      assertEquals(1, broker.getTimeslotCompleted(), "correct timeslot index");
    }
    catch (InterruptedException e) {
      fail("interrupted " + e.toString());
    }
  }

  /**
   * Pause within ts. Msg sequence is tsu - pause - tc - release
   */
  @Test
  public void normalPause ()
  {
    try {
      Thread.sleep(2001); // 2.001 seconds
      // it's now the start of ts1
      TimeslotUpdate tsu = new TimeslotUpdate(baseTime.plusMillis(2000),2, 12);
      broker.handleMessage(tsu);
      Thread.sleep(500); // short delay
      SimPause sp = new SimPause();
      broker.handleMessage(sp);
      Thread.sleep(1000); // 1000 msec pause
      TimeslotComplete tc = new TimeslotComplete(1);
      broker.handleMessage(tc);
      assertEquals(1, broker.getTimeslotCompleted(), "correct timeslot index");
      SimResume sr = new SimResume(baseTime.plusMillis(1000));
      broker.handleMessage(sr);
      assertEquals(1, broker.getTimeslotCompleted(), "correct timeslot index");
    }
    catch (InterruptedException e) {
      fail("interrupted " + e.toString());
    }
  }

  /**
   * Pause into next ts. Msg sequence is tsu - pause - tc - release
   */
  @Test
  public void longPause ()
  {
    try {
      Thread.sleep(2001); // 2.001 seconds
      // it's now the start of ts1
      TimeslotUpdate tsu = new TimeslotUpdate(baseTime.plusMillis(2000), 2, 12);
      broker.handleMessage(tsu);
      Thread.sleep(1100); // short delay
      SimPause sp = new SimPause();
      broker.handleMessage(sp);
      Thread.sleep(3000); // 3000 msec pause
      TimeslotComplete tc = new TimeslotComplete(1);
      broker.handleMessage(tc);
      assertEquals(1, broker.getTimeslotCompleted(), "correct timeslot index");
      SimResume sr = new SimResume(baseTime.plusMillis(3000));
      broker.handleMessage(sr);
      assertEquals(1, broker.getTimeslotCompleted(), "correct timeslot index");
    }
    catch (InterruptedException e) {
      fail("interrupted " + e.toString());
    }
  }
}
