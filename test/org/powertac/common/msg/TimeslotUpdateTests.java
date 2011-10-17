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
package org.powertac.common.msg;

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.HourlyCharge;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.test.util.ReflectionTestUtils;

import com.thoughtworks.xstream.XStream;

/**
 * Tests for the TimeslotUpdate message type.
 * @author John Collins
 */
public class TimeslotUpdateTests
{
  TimeService timeService;
  TimeslotRepo timeslotRepo;
  
  @Before
  public void setUp () throws Exception
  {
    timeService = new TimeService();
    Instant start = new DateTime(2011,1,10,0,0,0,0,DateTimeZone.UTC).toInstant();
    timeslotRepo = new TimeslotRepo();
    ReflectionTestUtils.setField(timeslotRepo, "timeService", timeService);
    Timeslot ts;
    for (int i = 0; i < 15; i++) {
      ts = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * i),
                                     start.plus(TimeService.HOUR * (i + 1)));
      ts.enable();
    }

  }

  @Test
  public void testTimeslotUpdate ()
  {
    TimeslotUpdate tsu = new TimeslotUpdate(timeService.getCurrentTime(),
                                            timeslotRepo.enabledTimeslots());
    assertNotNull("message not null", tsu);
    assertEquals("15 timeslots", 15, tsu.size());
    assertEquals("correct posted time",
                 timeService.getCurrentTime(),
                 tsu.getPostedTime());
    List<Timeslot> result = tsu.getEnabled();
    assertNotNull("non-null result", result);
    assertEquals("15 elements in list", 15, result.size());
    assertEquals("correct first element",
                 timeslotRepo.enabledTimeslots().get(0),
                 result.get(0));
  }

  @Test
  public void xmlSerializationTest ()
  {
    TimeslotUpdate tsu = new TimeslotUpdate(timeService.getCurrentTime(),
                                            timeslotRepo.enabledTimeslots());
    XStream xstream = new XStream();
    xstream.processAnnotations(TimeslotUpdate.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(tsu));
    //System.out.println(serialized.toString());
    TimeslotUpdate xtsu= (TimeslotUpdate)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xtsu);
    assertEquals("correct time",
                 timeService.getCurrentTime(),
                 xtsu.getPostedTime());
    assertEquals("correct list length", 15, xtsu.size());
  }
}
