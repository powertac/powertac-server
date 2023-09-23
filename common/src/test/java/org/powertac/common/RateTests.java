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

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.enumerations.PowerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import com.thoughtworks.xstream.XStream;

@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class RateTests
{
  @Autowired
  private TimeService timeService;

  @BeforeEach
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

    assertNotNull(r, "Rate not null");
    assertTrue(r.isFixed(), "Rate is fixed");
    assertEquals(0.121, r.getValue(), 1e-6, "Correct fixed rate");
    assertEquals(0, r.getNoticeInterval(), "Correct notice interval");
  }

  // Test validity tests
  @Test
  public void testRateValidity ()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,0,0,0,0,DateTimeZone.UTC));
    Rate r = new Rate().withValue(Double.NaN);
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertFalse(r.isValid(PowerType.CONSUMPTION), "Invalid numeric value");

    r.withValue(Double.POSITIVE_INFINITY);
    assertFalse(r.isValid(PowerType.CONSUMPTION), "Invalid Infinity");

    r.withValue(Double.NEGATIVE_INFINITY);
    assertFalse(r.isValid(PowerType.CONSUMPTION), "Invalid Infinity 2");
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

    assertTrue(r.isFixed(), "Rate is fixed");
    assertFalse(r.applies(), "Does not apply now");
    assertTrue(r.applies(new DateTime(2012, 2, 2, 6, 0, 0, 0, DateTimeZone.UTC)), "Applies at 6:00");
    assertTrue(r.applies(new DateTime(2012, 2, 3, 7, 59, 0, 0, DateTimeZone.UTC)), "Applies at 7:59");
    assertFalse(r.applies(new DateTime(2012, 3, 3, 9, 0, 0, 0, DateTimeZone.UTC)), "Does not apply at 9:00");
    assertFalse(r.applies(new DateTime(2012, 1, 3, 8, 0, 0, 0, DateTimeZone.UTC)), "Does not apply at 8:00");
  }

  // Test a rate that applies between 6:00 and 8:00
  @Test
  public void testDailyRateOverflow ()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,5,0,0,0, DateTimeZone.UTC));
    assertNull(new Rate().withValue(0.121).withDailyBegin(30), "invalid time");
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

    assertTrue(r.isFixed(), "Rate is fixed");
    assertFalse(r.applies(), "Does not apply now");
    assertTrue(r.applies(new DateTime(2012, 2, 2, 22, 0, 0, 0, DateTimeZone.UTC)), "Applies at 22:00");
    assertTrue(r.applies(new DateTime(2012, 2, 3, 4, 59, 0, 0, DateTimeZone.UTC)), "Applies at 4:59");
    assertFalse(r.applies(new DateTime(2012, 3, 3, 6, 0, 0, 0, DateTimeZone.UTC)), "Does not apply at 6:00");
    assertFalse(r.applies(new DateTime(2012, 1, 3, 5, 0, 0, 0, DateTimeZone.UTC)), "Does not apply at 5:00");
  }
  
  // Test a weekly rate that applies on Saturday and Sunday (days 6 and 7)
  @Test
  public void testWeeklyRateWeekend()
  {
    Rate r = new Rate().withValue(0.121)
        .withWeeklyBegin(new DateTime(2011, 1, 15, 22, 0, 0, 0, DateTimeZone.UTC))
        .withWeeklyEnd(new DateTime(2011, 1, 16, 5, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue(r.isFixed(), "Rate is fixed");
    assertFalse(r.applies(new DateTime(2011, 1, 21, 22, 0, 0, 0, DateTimeZone.UTC)), "Does not apply on Friday");
    assertTrue(r.applies(new DateTime(2011, 1, 22, 22, 0, 0, 0, DateTimeZone.UTC)), "Applies on Saturday");
    assertTrue(r.applies(new DateTime(2011, 1, 23, 4, 59, 0, 0, DateTimeZone.UTC)), "Applies on Sunday");
    assertFalse(r.applies(new DateTime(2011, 1, 24, 6, 0, 0, 0, DateTimeZone.UTC)), "Does not apply on Monday");
  }
  
  // Test a weekly rate that applies on Sunday only (day 7)
  @Test
  public void testWeeklyRateSun()
  {
    Rate r = new Rate().withValue(0.121)
        .withWeeklyBegin(new DateTime(2011, 1, 16, 22, 0, 0, 0, DateTimeZone.UTC))
        .withWeeklyEnd(new DateTime(2011, 1, 16, 22, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue(r.isFixed(), "Rate is fixed");
    assertFalse(r.applies(new DateTime(2011, 1, 21, 22, 0, 0, 0, DateTimeZone.UTC)), "Does not apply on Friday");
    assertFalse(r.applies(new DateTime(2011, 1, 22, 22, 0, 0, 0, DateTimeZone.UTC)), "Does not apply on Saturday");
    assertTrue(r.applies(new DateTime(2011, 1, 23, 4, 59, 0, 0, DateTimeZone.UTC)), "Applies on Sunday");
    assertFalse(r.applies(new DateTime(2011, 1, 24, 6, 0, 0, 0, DateTimeZone.UTC)), "Does not apply on Monday");
  }

  // Test a weekly rate that applies on Saturday and Sunday (days 6 and 7)
  @Test
  public void testWeeklyRateSunMon()
  {
    Rate r = new Rate().withValue(0.121)
        .withWeeklyBegin(new DateTime(2011, 1, 16, 22, 0, 0, 0, DateTimeZone.UTC))
        .withWeeklyEnd(new DateTime(2011, 1, 17, 5, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue(r.isFixed(), "Rate is fixed");
    assertFalse(r.applies(new DateTime(2011, 1, 22, 22, 0, 0, 0, DateTimeZone.UTC)), "Does not apply on Saturday");
    assertTrue(r.applies(new DateTime(2011, 1, 23, 4, 59, 0, 0, DateTimeZone.UTC)), "Applies on Sunday");
    assertTrue(r.applies(new DateTime(2011, 1, 24, 4, 59, 0, 0, DateTimeZone.UTC)), "Applies on Monday");
    assertFalse(r.applies(new DateTime(2011, 1, 25, 6, 0, 0, 0, DateTimeZone.UTC)), "Does not apply on Tuesday");
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

    assertFalse(r.applies(new DateTime(2011, 1, 22, 12, 0, 0, 0, DateTimeZone.UTC)), "Does not apply on Saturday");
    assertFalse(r.applies(new DateTime(2011, 1, 30, 12, 0, 0, 0, DateTimeZone.UTC)), "Does not apply on Sunday");
    assertFalse(r.applies(new DateTime(2011, 1, 31, 7, 59, 0, 0, DateTimeZone.UTC)), "Does not apply Mon morning");
    assertTrue(r.applies(new DateTime(2011, 1, 31, 8, 0, 0, 0, DateTimeZone.UTC)), "Starts Mon 8:00");
    assertTrue(r.applies(new DateTime(2011, 1, 31, 17, 59, 0, 0, DateTimeZone.UTC)), "Applies Mon 17:59");
    assertFalse(r.applies(new DateTime(2011, 1, 31, 18, 0, 0, 0, DateTimeZone.UTC)), "Does not apply Mon 18:00");
    assertTrue(r.applies(new DateTime(2011, 1, 28, 17, 59, 0, 0, DateTimeZone.UTC)), "Applies Fri 17:59");
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

    assertTrue(r.applies(200.0), "Applies now");
    assertTrue(r.applies(1.0, new DateTime(2012, 2, 2, 6, 0, 0, 0, DateTimeZone.UTC)), "Applies at 6:00");
    assertTrue(r.applies(2.0, new DateTime(2012, 2, 3, 7, 59, 0, 0, DateTimeZone.UTC)), "Applies at 7:59");
    assertFalse(r.applies(2.0, new DateTime(2012, 3, 3, 9, 0, 0, 0, DateTimeZone.UTC)), "Does not apply at 9:00");
  }
  
  // Test a rate that applies between 6:00 and 8:00,
  // tierThreshold = 100 -- no longer used as of #1152
  @Test
  public void testDailyRateT1()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,7,0, 0, 0, DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121) 
        .withDailyBegin(new DateTime(2011, 1, 1, 6, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 1, 8, 0, 0, 0, DateTimeZone.UTC));
    ReflectionTestUtils.setField(r, "timeService", timeService);

    assertTrue(r.applies(99.0), "Does not apply at 99");
    assertTrue(r.applies(100.0, new DateTime(2012, 2, 2, 6, 0, 0, 0, DateTimeZone.UTC)), "Applies at 6:00, 100");
    assertTrue(r.applies(80.0, new DateTime(2012, 3, 3, 7, 0, 0, 0, DateTimeZone.UTC)), "Does not apply at 7:00, 80");
  }
  
  // Validity test: curtailment - note that it's supposed to be impossible to
  // set maxCurtailment outside the valid range.
  @Test
  public void testCurtailmentValidity ()
  {
    Rate r = new Rate().withValue(-0.121) 
        .withMaxCurtailment(-1.0);
    TariffSpecification spec = new TariffSpecification(null, PowerType.CONSUMPTION);
    assertTrue(r.isValid(spec), "curtailment value valid");
    assertEquals(0.0, r.getMaxCurtailment(), 1e-6, "value 0.0");
    r.withMaxCurtailment(1.01);
    assertTrue(r.isValid(spec), "curtailment value valid");
    assertEquals(1.0, r.getMaxCurtailment(), 1e-6, "value 1.0");
    r.withMaxCurtailment(0.5);
    assertTrue(r.isValid(spec), "curtailment value valid");
    assertEquals(0.5, r.getMaxCurtailment(), 1e-6, "value 0.5");
    r.withMaxCurtailment(0.0);
    assertTrue(r.isValid(spec), "curtailment value valid");
    r.withMaxCurtailment(1.0);
    assertTrue(r.isValid(spec), "curtailment value valid");
  }
  
  // Validity test: CONSUMPTION tier value
  // no longer supported as of #1152
//  @Test
//  public void testTierCons ()
//  {
//    Rate r = new Rate().withValue(-0.121) 
//            .withTierThreshold(-1.0);
//    TariffSpecification spec = new TariffSpecification(null, PowerType.INTERRUPTIBLE_CONSUMPTION);
//    assertFalse(r.isValid(spec), "tier theshold invalid");
//    r.withTierThreshold(0.0);
//    assertTrue(r.isValid(spec), "tier theshold valid");    
//    r.withTierThreshold(100000.0);
//    assertTrue(r.isValid(spec), "tier theshold valid");    
//  }
  
  // Validity test: PRODUCTION tier value
//  @Test
//  public void testTierProd ()
//  {
//    Rate r = new Rate().withValue(0.121) 
//            .withTierThreshold(1.0);
//    TariffSpecification spec = new TariffSpecification(null, PowerType.SOLAR_PRODUCTION);
//    assertFalse(r.isValid(spec), "tier theshold invalid");
//    r.withTierThreshold(0.0);
//    assertTrue(r.isValid(spec), "tier theshold valid");    
//    r.withTierThreshold(-100000.0);
//    assertTrue(r.isValid(spec), "tier theshold valid");    
//  }
  
  // Validity test: maxValue, expectedMean, consumption
  @Test
  public void testMaxValueConsumption () {
    Rate r = new Rate().withValue(-0.121).withFixed(false);
    TariffSpecification spec = new TariffSpecification(null, PowerType.CONSUMPTION);
    assertFalse(r.isValid(spec), "Invalid - maxValue, expectedMean not given");
    r.withMaxValue(-0.5);
    assertFalse(r.isValid(spec), "invalid - expectedMean not given");
    r.withExpectedMean(-0.3);
    assertTrue(r.isValid(spec), "valid");
    r.withMaxValue(-0.121);
    assertFalse(r.isValid(spec), "invalid - expectedMean OOR");
    r.withExpectedMean(-0.121);
    assertTrue(r.isValid(spec), "valid");
  }
  
  // Validity test:
  @Test
  public void testMaxValueProduction () {
    Rate r = new Rate().withValue(0.121).withFixed(false);
    TariffSpecification spec = new TariffSpecification(null, PowerType.SOLAR_PRODUCTION);
    assertFalse(r.isValid(spec), "Invalid - maxValue, expectedMean not given");
    r.withMaxValue(0.5);
    assertFalse(r.isValid(spec), "invalid - expectedMean not given");
    r.withExpectedMean(0.3);
    assertTrue(r.isValid(spec), "valid");
    r.withMaxValue(0.121);
    assertFalse(r.isValid(spec), "invalid - expectedMean OOR");
    r.withExpectedMean(0.121);
    assertTrue(r.isValid(spec), "valid");
  }
  
  // Validity test: noticeInterval
  @Test
  public void testNoticeInterval ()
  {
    Rate r = new Rate().withFixed(false).withValue(-0.1)
            .withMaxValue(-0.3).withExpectedMean(-0.2);
    TariffSpecification spec = new TariffSpecification(null, PowerType.CONSUMPTION);
    assertTrue(r.isValid(spec), "valid");
    r.withNoticeInterval(-1);
    assertFalse(r.isValid(spec), "invalid noticeInterval");
    r.withNoticeInterval(1);
    assertTrue(r.isValid(spec), "valid noticeInterval");
  }
  
  // test HC notice interval
  @Test
  public void addHCNotice ()
  {
    Rate r = new Rate().withFixed(false).withValue(-0.1)
            .withMaxValue(-0.3).withExpectedMean(-0.2).withNoticeInterval(2);
    Instant now = timeService.getCurrentTime();
    HourlyCharge hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.25);
    assertTrue(r.addHourlyCharge(hc), "valid hc");
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 2), -0.25);
    assertTrue(r.addHourlyCharge(hc), "still valid - boundary case");
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 1), -0.25);
    assertFalse(r.addHourlyCharge(hc), "invalid - too short");
  }

  // test HC charge limits
  @Test
  public void addHCLimit ()
  {
    Rate r = new Rate().withFixed(false).withValue(-0.1)
            .withMaxValue(-0.3).withExpectedMean(-0.2).withNoticeInterval(2);
    Instant now = timeService.getCurrentTime();
    HourlyCharge hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.25);
    assertTrue(r.addHourlyCharge(hc), "valid hc");
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.1);
    assertTrue(r.addHourlyCharge(hc), "lower boundary case");
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.3);
    assertTrue(r.addHourlyCharge(hc), "upper boundary case");
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.09);
    assertFalse(r.addHourlyCharge(hc), "low out of bounds");
    hc = new HourlyCharge(now.plus(TimeService.HOUR * 3), -0.31);
    assertFalse(r.addHourlyCharge(hc), "high out of bounds");
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
    assertEquals(-0.25, r.getValue(now.plus(TimeService.HOUR * 3)), 1e-6, "hc applies 3h");
    assertEquals(-0.2, r.getValue(now.plus(TimeService.HOUR * 4)), 1e-6, "hc does not apply applies 4h");
  }

  @Test
  public void xmlSerializationTest ()
  {
    timeService.setCurrentTime(new DateTime(2011,1,10,7,0, 0, 0, DateTimeZone.UTC));
    Rate r = new Rate().withValue(0.121) 
        .withDailyBegin(new DateTime(2011, 1, 1, 6, 0, 0, 0, DateTimeZone.UTC))
        .withDailyEnd(new DateTime(2011, 1, 1, 8, 0, 0, 0, DateTimeZone.UTC))
        .withTierThreshold(100.0);

    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(Rate.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(r));
    //System.out.println(serialized.toString());
    Rate xr= (Rate)xstream.fromXML(serialized.toString());
    assertNotNull(xr, "deserialized something");
    ReflectionTestUtils.setField(xr, "timeService", timeService);
    assertEquals(0.121, xr.getValue(), 1e-6, "correct value");
    assertEquals(100.0, xr.getTierThreshold(), 1e-6, "correct tier threshold");
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
    assertTrue(r.addHourlyCharge(new HourlyCharge(now.plus(TimeService.HOUR * 23), 0.22)), "add rate");
    //rate tomorrow at 7:00 = 0.18
    assertTrue(r.addHourlyCharge(new HourlyCharge(now.plus(TimeService.HOUR * 24), 0.18)), "add rate 2");

    // check original rates
    assertEquals(0.10, r.getValue(now), 1e-6, "correct value now");
    //assertEquals(0.22, r.getValue(now.plus(TimeService.HOUR * 23)), 1e-6, "correct value tomorrow at 6:00");
    //assertEquals(0.18, r.getValue(now.plus(TimeService.HOUR * 24)), 1e-6, "correct value tomorrow at 7:00");

    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(Rate.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(r));
    //System.out.println(serialized.toString());
    Rate xr= (Rate)xstream.fromXML(serialized.toString());
    assertNotNull(xr, "deserialized something");
    ReflectionTestUtils.setField(xr, "timeService", timeService);
    assertEquals(0.10, xr.getValue(), 1e-6, "correct value");
    assertEquals(0.4, xr.getMaxCurtailment(), 1e-6, "correct curtailment");
    assertEquals(100.0, xr.getTierThreshold(), 1e-6, "correct tier threshold");
    assertEquals(0.22, xr.getValue(now.plus(TimeService.HOUR * 23)), 1e-6, "correct value tomorrow at 6:00");
    assertEquals(0.18, xr.getValue(now.plus(TimeService.HOUR * 24)), 1e-6, "correct value tomorrow at 7:00");
  }
}
