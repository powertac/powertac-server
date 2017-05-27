/*
 * Copyright (c) 2011 - 2017 by John Collins.
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
import org.powertac.common.enumerations.PowerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
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
  public void testFixedRate () 
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,0,0,0,0,DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121);
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertNotNull("Rate not null", r);
    assertTrue("Rate is fixed", r.isFixed());
    assertEquals("Correct fixed rate", 0.121, r.getValue(), 1e-6);
    assertEquals("Correct notice interval", 0, r.getNoticeInterval());
  }

  // Test validity tests
  @Test
  public void testRateValidity ()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,0,0,0,0,DateTimeZone.UTC));
    Rate r = new Rate().withValue(Double.NaN);
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertFalse("Invalid numeric value", r.isValid(PowerType.CONSUMPTION));

    r.withValue(Double.POSITIVE_INFINITY);
    assertFalse("Invalid Infinity", r.isValid(PowerType.CONSUMPTION));

    r.withValue(Double.NEGATIVE_INFINITY);
    assertFalse("Invalid Infinity 2", r.isValid(PowerType.CONSUMPTION));
  }

  // Test a rate that applies between 6:00 and 8:00
  @Test
  public void testDailyRate ()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,5,0, 0, 0, DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121)
        .withDailyBegin(new DateTime(2011, 1, 1, 6, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 1, 7, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue("Rate is fixed", r.isFixed());
    assertFalse("Does not apply now", r.applies());
    assertTrue("Applies at 6:00", r.applies(new DateTime(2012, 2, 2, 6, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies at 7:59", r.applies(new DateTime(2012, 2, 3, 7, 59, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply at 9:00", r.applies(new DateTime(2012, 3, 3, 9, 0, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply at 8:00", r.applies(new DateTime(2012, 1, 3, 8, 0, 0, 0, DateTimeZone.UTC)));
  }

  // Test a rate that applies between 6:00 and 8:00
  @Test
  public void testDailyRateOverflow ()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,5,0,0,0, DateTimeZone.UTC));
    assertNull("invalid time", new Rate().withValue(0.121).withDailyBegin(30));
  }

  // Test a rate that applies between 22:00 and 5:00
  @Test
  public void testDailyRateOverMidnight ()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,21,0,0,0,DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121) 
        .withDailyBegin(new DateTime(2011, 1, 1, 22, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 2, 4, 0, 0, 0, DateTimeZone.UTC));
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
        .withWeeklyBegin(new DateTime(2011, 1, 16, 22, 0, 0, 0, DateTimeZone.UTC))
        .withWeeklyEnd(new DateTime(2011, 1, 16, 22, 0, 0, 0, DateTimeZone.UTC));
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
    assertTrue("Applies Mon 17:59", r.applies(new DateTime(2011, 1, 31, 17, 59, 0, 0, DateTimeZone.UTC)));
    assertFalse("Does not apply Mon 18:00", r.applies(new DateTime(2011, 1, 31, 18, 0, 0, 0, DateTimeZone.UTC)));
    assertTrue("Applies Fri 17:59", r.applies(new DateTime(2011, 1, 28, 17, 59, 0, 0, DateTimeZone.UTC)));
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
  
  // Validity test: curtailment - note that it's supposed to be impossible to
  // set maxCurtailment outside the valid range.
  @Test
  public void testCurtailmentValidity ()
  {
    Rate r = new Rate().withValue(-0.121) 
        .withMaxCurtailment(-1.0);
    TariffSpecification spec = new TariffSpecification(null, PowerType.CONSUMPTION);
    assertTrue("curtailment value valid", r.isValid(spec));
    assertEquals("value 0.0", 0.0, r.getMaxCurtailment(), 1e-6);
    r.withMaxCurtailment(1.01);
    assertTrue("curtailment value valid", r.isValid(spec));
    assertEquals("value 1.0", 1.0, r.getMaxCurtailment(), 1e-6);
    r.withMaxCurtailment(0.5);
    assertTrue("curtailment value valid", r.isValid(spec));
    assertEquals("value 0.5", 0.5, r.getMaxCurtailment(), 1e-6);
    r.withMaxCurtailment(0.0);
    assertTrue("curtailment value valid", r.isValid(spec));
    r.withMaxCurtailment(1.0);
    assertTrue("curtailment value valid", r.isValid(spec));
  }
  
  // Validity test: CONSUMPTION tier value
  @Test
  public void testTierCons ()
  {
    Rate r = new Rate().withValue(-0.121) 
            .withTierThreshold(-1.0);
    TariffSpecification spec = new TariffSpecification(null, PowerType.INTERRUPTIBLE_CONSUMPTION);
    assertFalse("tier theshold invalid", r.isValid(spec));
    r.withTierThreshold(0.0);
    assertTrue("tier theshold valid", r.isValid(spec));    
    r.withTierThreshold(100000.0);
    assertTrue("tier theshold valid", r.isValid(spec));    
  }
  
  // Validity test: PRODUCTION tier value
  @Test
  public void testTierProd ()
  {
    Rate r = new Rate().withValue(0.121) 
            .withTierThreshold(1.0);
    TariffSpecification spec = new TariffSpecification(null, PowerType.SOLAR_PRODUCTION);
    assertFalse("tier theshold invalid", r.isValid(spec));
    r.withTierThreshold(0.0);
    assertTrue("tier theshold valid", r.isValid(spec));    
    r.withTierThreshold(-100000.0);
    assertTrue("tier theshold valid", r.isValid(spec));    
  }
  
  // Validity test: maxValue, expectedMean, consumption
  @Test
  public void testMaxValueConsumption () {
    Rate r = new Rate().withValue(-0.121).withFixed(false);
    TariffSpecification spec = new TariffSpecification(null, PowerType.CONSUMPTION);
    assertFalse("Invalid - maxValue, expectedMean not given", r.isValid(spec));
    r.withMaxValue(-0.5);
    assertFalse("invalid - expectedMean not given", r.isValid(spec));
    r.withExpectedMean(-0.3);
    assertTrue("valid", r.isValid(spec));
    r.withMaxValue(-0.121);
    assertFalse("invalid - expectedMean OOR", r.isValid(spec));
    r.withExpectedMean(-0.121);
    assertTrue("valid", r.isValid(spec));
  }
  
  // Validity test:
  @Test
  public void testMaxValueProduction () {
    Rate r = new Rate().withValue(0.121).withFixed(false);
    TariffSpecification spec = new TariffSpecification(null, PowerType.SOLAR_PRODUCTION);
    assertFalse("Invalid - maxValue, expectedMean not given", r.isValid(spec));
    r.withMaxValue(0.5);
    assertFalse("invalid - expectedMean not given", r.isValid(spec));
    r.withExpectedMean(0.3);
    assertTrue("valid", r.isValid(spec));
    r.withMaxValue(0.121);
    assertFalse("invalid - expectedMean OOR", r.isValid(spec));
    r.withExpectedMean(0.121);
    assertTrue("valid", r.isValid(spec));
  }
  
  // Validity test: noticeInterval
  @Test
  public void testNoticeInterval ()
  {
    Rate r = new Rate().withFixed(false).withValue(-0.1)
            .withMaxValue(-0.3).withExpectedMean(-0.2);
    TariffSpecification spec = new TariffSpecification(null, PowerType.CONSUMPTION);
    assertTrue("valid", r.isValid(spec));
    r.withNoticeInterval(-1);
    assertFalse("invalid noticeInterval", r.isValid(spec));
    r.withNoticeInterval(1);
    assertTrue("valid noticeInterval", r.isValid(spec));
  }
  
  // test HC notice interval
  @Test
  public void addHCNotice ()
  {
    Rate r = new Rate().withFixed(false).withValue(-0.1)
            .withMaxValue(-0.3).withExpectedMean(-0.2).withNoticeInterval(2);
    Instant now = timeService.getCurrentTime();
    HourlyCharge hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.25);
    assertTrue("valid hc", r.addHourlyCharge(hc));
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 2), -0.25);
    assertTrue("still valid - boundary case", r.addHourlyCharge(hc));
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 1), -0.25);
    assertFalse("invalid - too short", r.addHourlyCharge(hc));
  }

  // test HC charge limits
  @Test
  public void addHCLimit ()
  {
    Rate r = new Rate().withFixed(false).withValue(-0.1)
            .withMaxValue(-0.3).withExpectedMean(-0.2).withNoticeInterval(2);
    Instant now = timeService.getCurrentTime();
    HourlyCharge hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.25);
    assertTrue("valid hc", r.addHourlyCharge(hc));
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.1);
    assertTrue("lower boundary case", r.addHourlyCharge(hc));
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.3);
    assertTrue("upper boundary case", r.addHourlyCharge(hc));
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.09);
    assertFalse("low out of bounds", r.addHourlyCharge(hc));
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.31);
    assertFalse("high out of bounds", r.addHourlyCharge(hc));
  }

  // test HC persistence
  @Test
  public void hcPersistence ()
  {
    Rate r = new Rate().withFixed(false).withValue(-0.1)
        .withMaxValue(-0.3).withExpectedMean(-0.2).withNoticeInterval(2);
    Instant now = timeService.getCurrentTime();
    HourlyCharge hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.25);
    r.addHourlyCharge(hc);
    assertEquals("hc applies 3h", -0.25,
                 r.getValue(now.plus(TimeService.HOUR * 3)), 1e-6);
    assertEquals("hc does not apply applies 4h", -0.2,
                 r.getValue(now.plus(TimeService.HOUR * 4)), 1e-6);
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
