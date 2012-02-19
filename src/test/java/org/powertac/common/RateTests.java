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
package org.powertac.common;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class RateTests
{
  @Autowired
  private TimeService timeService;

  @Before
  public void setUp() 
  {
    //timeService = new TimeService();
    timeService.setCurrentTime(new DateTime());
  }
  
  @Test
  public void testFixedRate() 
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,0,0,0,0,DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    
    assertNotNull("Rate not null", r);
    assertTrue("Rate is fixed", r.isFixed());
    assertEquals("Correct fixed rate", 0.121, r.getValue(), 1e-6);
    assertEquals("Correct notice interval", 0, r.getNoticeInterval());
  }
  
  // Test a rate that applies between 6:00 and 8:00
  @Test
  public void testDailyRate()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,5,0, 0, 0, DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121)
        .withDailyBegin(new DateTime(2011, 1, 1, 6, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 1, 8, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue("Rate is fixed", r.isFixed());
    assertFalse("Does not apply now", r.applies());
    assertTrue("Applies at 6:00", r.applies(new DateTime(2012, 2, 2, 6, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies at 7:59", r.applies(new DateTime(2012, 2, 3, 7, 59, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply at 9:00", r.applies(new DateTime(2012, 3, 3, 9, 0, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply at 8:00", r.applies(new DateTime(2012, 1, 3, 8, 0, 0, 0, DateTimeZone.UTC)));
  }
  
  // Test a rate that applies between 22:00 and 5:00
  @Test
  public void testDailyRateOverMidnight()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,21,0,0,0,DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121) 
        .withDailyBegin(new DateTime(2011, 1, 1, 22, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 2, 5, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue("Rate is fixed", r.isFixed());
    assertFalse("Does not apply now", r.applies());
    assertTrue("Applies at 22:00", r.applies(new DateTime(2012, 2, 2, 22, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies at 4:59", r.applies(new DateTime(2012, 2, 3, 4, 59, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply at 6:00", r.applies(new DateTime(2012, 3, 3, 6, 0, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply at 5:00", r.applies(new DateTime(2012, 1, 3, 5, 0, 0, 0, DateTimeZone.UTC)));
  }
  
  // Test a weekly rate that applies on Saturday and Sunday (days 6 and 7)
  @Test
  public void testWeeklyRateWeekend()
  {
    Rate r = new Rate().withValue(0.121)
        .withWeeklyBegin(new DateTime(2011, 1, 15, 22, 0, 0, 0, DateTimeZone.UTC))
        .withWeeklyEnd(new DateTime(2011, 1, 16, 5, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue("Rate is fixed", r.isFixed());
    assertFalse("Does not apply on Friday", r.applies(new DateTime(2011, 1, 21, 22, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies on Saturday", r.applies(new DateTime(2011, 1, 22, 22, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies on Sunday", r.applies(new DateTime(2011, 1, 23, 4, 59, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply on Monday", r.applies(new DateTime(2011, 1, 24, 6, 0, 0, 0, DateTimeZone.UTC)));
  }
  
  // Test a weekly rate that applies on Sunday only (day 7)
  @Test
  public void testWeeklyRateSun()
  {
    Rate r = new Rate().withValue(0.121)
        .withWeeklyBegin(new DateTime(2011, 1, 16, 22, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue("Rate is fixed", r.isFixed());
    assertFalse("Does not apply on Friday", r.applies(new DateTime(2011, 1, 21, 22, 0, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply on Saturday", r.applies(new DateTime(2011, 1, 22, 22, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies on Sunday", r.applies(new DateTime(2011, 1, 23, 4, 59, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply on Monday", r.applies(new DateTime(2011, 1, 24, 6, 0, 0, 0, DateTimeZone.UTC)));
  }

  // Test a weekly rate that applies on Saturday and Sunday (days 6 and 7)
  @Test
  public void testWeeklyRateSunMon()
  {
    Rate r = new Rate().withValue(0.121)
        .withWeeklyBegin(new DateTime(2011, 1, 16, 22, 0, 0, 0, DateTimeZone.UTC))
        .withWeeklyEnd(new DateTime(2011, 1, 17, 5, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue("Rate is fixed", r.isFixed());
    assertFalse("Does not apply on Saturday", r.applies(new DateTime(2011, 1, 22, 22, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies on Sunday", r.applies(new DateTime(2011, 1, 23, 4, 59, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies on Monday", r.applies(new DateTime(2011, 1, 24, 4, 59, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply on Tuesday", r.applies(new DateTime(2011, 1, 25, 6, 0, 0, 0, DateTimeZone.UTC)));
  }
  
  // Test a rate that applies only during the day on weekdays
  @Test
  public void testWeekdayRate()
  {
    Rate r = new Rate().withValue(0.121)
        .withWeeklyBegin(new DateTime(2011, 1, 10, 2, 0, 0, 0, DateTimeZone.UTC)) //Monday
        .withWeeklyEnd(new DateTime(2011, 1, 14, 5, 0, 0, 0, DateTimeZone.UTC))   //Friday
        .withDailyBegin(new DateTime(2011, 1, 14, 8, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 14, 17, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertFalse("Does not apply on Saturday", r.applies(new DateTime(2011, 1, 22, 12, 0, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply on Sunday", r.applies(new DateTime(2011, 1, 30, 12, 0, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply Mon morning", r.applies(new DateTime(2011, 1, 31, 7, 59, 0, 0, DateTimeZone.UTC)));
    assertTrue("Starts Mon 8:00", r.applies(new DateTime(2011, 1, 31, 8, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies Mon 16:59", r.applies(new DateTime(2011, 1, 31, 16, 59, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply Mon 17:00", r.applies(new DateTime(2011, 1, 31, 17, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies Fri 16:59", r.applies(new DateTime(2011, 1, 28, 16, 59, 0, 0, DateTimeZone.UTC)));
  }
  
  // Test a rate that applies between 6:00 and 8:00,
  // tierThreshold = 0
  @Test
  public void testDailyRateT0()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,7,0, 0, 0, DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121) 
        .withDailyBegin(new DateTime(2011, 1, 1, 6, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 1, 8, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue("Applies now", r.applies(200.0));
    assertTrue("Applies at 6:00", r.applies(1.0, new DateTime(2012, 2, 2, 6, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies at 7:59", r.applies(2.0, new DateTime(2012, 2, 3, 7, 59, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply at 9:00", r.applies(2.0, new DateTime(2012, 3, 3, 9, 0, 0, 0, DateTimeZone.UTC)));
  }
  
  // Test a rate that applies between 6:00 and 8:00,
  // tierThreshold = 100
  @Test
  public void testDailyRateT1()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,7,0, 0, 0, DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121) 
        .withDailyBegin(new DateTime(2011, 1, 1, 6, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 1, 8, 0, 0, 0, DateTimeZone.UTC))
        .withTierThreshold(100.0);
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertFalse("Does not apply at 99", r.applies(99.0));
    assertTrue("Applies at 6:00, 100", r.applies(100.0, new DateTime(2012, 2, 2, 6, 0, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply at 7:00, 80", r.applies(80.0, new DateTime(2012, 3, 3, 7, 0, 0, 0, DateTimeZone.UTC)));
  }

  @Test
  public void xmlSerializationTest ()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,7,0, 0, 0, DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121) 
        .withDailyBegin(new DateTime(2011, 1, 1, 6, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 1, 8, 0, 0, 0, DateTimeZone.UTC))
        .withTierThreshold(100.0);

    XStream xstream = new XStream();
    xstream.processAnnotations(Rate.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(r));
    //System.out.println(serialized.toString());
    Rate xr= (Rate)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xr);
    ReflectionTestUtils.setField(xr, "timeService", timeService);
    assertEquals("correct value", 0.121, xr.getValue(), 1e-6);
    assertEquals("correct tier threshold", 100.0, xr.getTierThreshold(), 1e-6);
  }

  @Test
  public void xmlSerializationTestHc ()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,7,0, 0, 0, DateTimeZone.UTC));
    Rate r = new Rate().withFixed(false)
        .withExpectedMean(0.10)
        // applies from 6:00-8:00
        .withDailyBegin(new DateTime(2011, 1, 1, 6, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 1, 8, 0, 0, 0, DateTimeZone.UTC))
        .withMaxCurtailment(0.4)
        .withTierThreshold(100.0);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    Instant now = timeService.getCurrentTime();
    //rate tomorrow at 6:00 = 0.22
    assertTrue("add rate", r.addHourlyCharge(new HourlyCharge(now.plus(TimeService.HOUR * 23), 0.22)));
    //rate tomorrow at 7:00 = 0.18
    assertTrue("add rate 2", r.addHourlyCharge(new HourlyCharge(now.plus(TimeService.HOUR * 24), 0.18)));

    // check original rates
    assertEquals("correct value now", 0.10, r.getValue(now), 1e-6);
    //assertEquals("correct value tomorrow at 6:00", 0.22, r.getValue(now.plus(TimeService.HOUR * 23)), 1e-6);
    //assertEquals("correct value tomorrow at 7:00", 0.18, r.getValue(now.plus(TimeService.HOUR * 24)), 1e-6);

    XStream xstream = new XStream();
    xstream.processAnnotations(Rate.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(r));
    //System.out.println(serialized.toString());
    Rate xr= (Rate)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xr);
    ReflectionTestUtils.setField(xr, "timeService", timeService);
    assertEquals("correct value", 0.10, xr.getValue(), 1e-6);
    assertEquals("correct curtailment", 0.4, xr.getMaxCurtailment(), 1e-6);
    assertEquals("correct tier threshold", 100.0, xr.getTierThreshold(), 1e-6);
    assertEquals("correct value tomorrow at 6:00", 0.22, xr.getValue(now.plus(TimeService.HOUR * 23)), 1e-6);
    assertEquals("correct value tomorrow at 7:00", 0.18, xr.getValue(now.plus(TimeService.HOUR * 24)), 1e-6);
  }
}
