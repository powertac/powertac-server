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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TariffRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class TariffTests
{
  @Autowired
  private TimeService timeService; // dependency injection
  
  @Autowired
  private TariffRepo repo;
  
  private TariffSpecification tariffSpec; // instance var

  private Instant start;
  private Instant exp;
  private Broker broker;
  
  @AfterClass
  public static void saveLogs () throws Exception
  {
    File state = new File("log/test.state");
    state.renameTo(new File("log/TariffTests.state"));
    File trace = new File("log/test.trace");
    trace.renameTo(new File("log/TariffTests.trace"));
  }

  @Before
  public void setUp () 
  {
    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(start);
    broker = new Broker ("testBroker");
    repo = new TariffRepo();
    exp = new DateTime(2011, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    tariffSpec = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8);
  }

  // create a Tariff and inspect it
  @Test
  public void testCreate () 
  {
    Rate r1 = new Rate().withValue(-0.121);
    tariffSpec.addRate(r1);
    Tariff te = new Tariff(tariffSpec);
    te.init();
    assertNotNull("non-null result", te);
    assertEquals("correct TariffSpec", tariffSpec, te.getTariffSpecification());
    assertEquals("correct initial realized price", 0.0, te.getRealizedPrice(), 1e-6);
    assertEquals("correct expiration in spec", exp, te.getTariffSpecification().getExpiration());
    assertEquals("correct expiration", exp, te.getExpiration());
    assertEquals("correct publication time", start, te.getOfferDate());
    assertFalse("not expired", te.isExpired());
    assertTrue("covered", te.isCovered());
  }
  
  // check the realized price calculation
  @Test
  public void testRealizedPrice ()
  {
    Rate r1 = new Rate().withValue(-0.121);
    tariffSpec.addRate(r1);
    Tariff te = new Tariff(tariffSpec);
    te.init();
    te.totalUsage = 501.2;
    te.totalCost = -99.8;
    assertEquals("Correct realized price", -99.8/501.2, te.getRealizedPrice(), 1.0e-6);
  }

  // single fixed rate, check charges in past and future  
  @Test
  public void testSimpleRate ()
  {
    Rate r1 = new Rate().withValue(-0.121);
    tariffSpec.addRate(r1);
    Instant now = timeService.getCurrentTime();
    Tariff te = new Tariff(tariffSpec);
    te.init();
    assertEquals("correct charge, default case", -0.121, te.getUsageCharge(1.0, 0.0, false), 1e-6);
    assertEquals("correct charge, today", -1.21, te.getUsageCharge(10.0, 0.0, false), 1e-6);
    assertEquals("correct charge yesterday", -2.42, te.getUsageCharge(now.minus(TimeService.DAY), 20.0, 0.0), 1e-6);
    assertEquals("correct charge tomorrow", -12.1, te.getUsageCharge(now.plus(TimeService.DAY), 100.0, 0.0), 1e-6);
    assertEquals("correct charge an hour ago", -3.63, te.getUsageCharge(now.minus(TimeService.HOUR), 30.0, 0.0), 1e-6);
    assertEquals("correct charge an hour from now", -1.21, te.getUsageCharge(now.plus(TimeService.HOUR), 10.0, 0.0), 1e-6);
    //assertEquals("daily rate map", 1, te.rateMap.size())
    //assertEquals("rate map has 24 entries", 24, te.rateMap[0].size())
    assertTrue("covered", te.isCovered());
  }
  
  // single fixed rate, check realized price after multiple rounds
  @Test
  public void testSimpleRateRealizedPrice ()
  {
    Rate r1 = new Rate().withValue(-0.131);
    tariffSpec.addRate(r1);
    //Instant now = timeService.getCurrentTime();
    Tariff te = new Tariff(tariffSpec);
    te.init();
    te.getUsageCharge(20.0, 200.0, true);
    assertEquals("realized price 1", -0.131, te.getRealizedPrice(), 1e-6);
    te.getUsageCharge(10.0, 1000.0, true);
    assertEquals("realized price 2", -0.131, te.getRealizedPrice(), 1e-6);
    te.getUsageCharge(3.0, 20.0, true);
    assertEquals("realized price 3", -0.131, te.getRealizedPrice(), 1e-6);
  }
  
  // time-of-use rates: -0.15/kwh 7:00-18:00, -0.08/kwh 18:00-7:00
  @Test
  public void testTimeOfUseDaily ()
  {
    Rate r1 = new Rate().withValue(-0.15).withDailyBegin(7).withDailyEnd(17);
    Rate r2 = new Rate().withValue(-0.08).withDailyBegin(18).withDailyEnd(6);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    Tariff te = new Tariff(tariffSpec);
    te.init();
    assertEquals("noon price", -3.0, te.getUsageCharge(20.0, 200.0, true), 1e-6);
    assertEquals("realized price", -0.15, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("18:00 price", -0.8, te.getUsageCharge(10.0, 220.0, true), 1e-6);
    assertEquals("realized price 2", -3.8/30.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("midnight price", -0.4, te.getUsageCharge(5.0, 230.0, true), 1e-6);
    assertEquals("realized price 3", -4.2/35.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("7:00 price", -0.6, te.getUsageCharge(4.0, 235.0, true), 1e-6);
    assertEquals("realized price 4", -4.8/39.0, te.getRealizedPrice(), 1e-6);
    assertTrue("covered", te.isCovered());
  }
  
  // time-of-use rates: 0.15/kwh 7:00-18:00, 0.08/kwh 19:00-7:00
  @Test
  public void testTimeOfUseDailyGap ()
  {
    Rate r1 = new Rate().withValue(-0.15).withDailyBegin(7).withDailyEnd(17);
    Rate r2 = new Rate().withValue(-0.08).withDailyBegin(19).withDailyEnd(6);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    Tariff te = new Tariff(tariffSpec);
    te.init();
    assertFalse("not covered", te.isCovered());
  }

  // time-of-use weekly: 
  // - weekdays are 0.15/kwh 7:00-18:00, 0.08/kwh 18:00-7:00
  // - weekends are 0.06
  @Test
  public void testTimeOfUseWeekly ()
  {
    Rate r1 = new Rate().withValue(-0.15).withDailyBegin(7).withDailyEnd(17);
    Rate r2 = new Rate().withValue(-0.08).withDailyBegin(18).withDailyEnd(6);
    Rate r3 = new Rate().withValue(-0.06).withWeeklyBegin(6).withWeeklyEnd(7);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariffSpec.addRate(r3);
    Tariff te = new Tariff(tariffSpec);
    te.init();
    assertEquals("noon price Sat", -1.2, te.getUsageCharge(20.0, 200.0, true), 1e-6);
    assertEquals("realized price", -0.06, te.getRealizedPrice(), 1e-6);
    assertTrue("weekly map", te.isWeekly());
    //assertEquals("rate map row has 168 entries", 168, te.rateMap[0].size())
    assertTrue("covered", te.isCovered());
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("18:00 price Sat", -0.6, te.getUsageCharge(10.0, 220.0, true), 1e-6);
    assertEquals("realized price 2", -1.8/30.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("midnight price Sun", -0.3, te.getUsageCharge(5.0, 230.0, true), 1e-6);
    assertEquals("realized price 3", -2.1/35.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("7:00 price Sun", -0.24, te.getUsageCharge(4.0, 235.0, true), 1e-6);
    assertEquals("realized price 4", -2.34/39.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 3, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("midnight Mon", -0.32, te.getUsageCharge(4.0, 235.0, true), 1e-6);
    assertEquals("realized price 5", -2.66/43.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 3, 6, 59, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("6:59 Mon", -0.48, te.getUsageCharge(6.0, 235.0, true), 1e-6);
    assertEquals("realized price 6", -3.14/49.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 3, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("7:00 Mon", -1.2, te.getUsageCharge(8.0, 235.0, true), 1e-6);
    assertEquals("realized price 7", -4.34/57.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 4, 12, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("noon Tue", -1.5, te.getUsageCharge(10.0, 235.0, true), 1e-6);
    assertEquals("realized price 8", -5.84/67.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 5, 17, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("17:00 Wed", -1.05, te.getUsageCharge(7.0, 235.0, true), 1e-6);
    assertEquals("realized price 9", -6.89/74.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 6, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("18:00 Thu", -0.72, te.getUsageCharge(9.0, 235.0, true), 1e-6);
    assertEquals("realized price 10", -7.61/83.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 7, 23, 59, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("23:59 Fri", -0.96, te.getUsageCharge(12.0, 235.0, true), 1e-6);
    assertEquals("realized price 11", -8.57/95.0, te.getRealizedPrice(), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 8, 12, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("midnight Sat", -0.18, te.getUsageCharge(3.0, 235.0, true), 1e-6);
    assertEquals("realized price 12", -8.75/98.0, te.getRealizedPrice(), 1e-6);
  }

  // time-of-use weekly wrap-around
  @Test
  public void testTimeOfUseWeeklyWrap ()
  {
    Rate r1 = new Rate().withValue(-0.15).withDailyBegin(6).withDailyEnd(17);
    Rate r2 = new Rate().withValue(-0.08).withDailyBegin(18).withDailyEnd(5);
    Rate r3 = new Rate().withValue(-0.06).withWeeklyBegin(7).withWeeklyEnd(2); // Sun-Tue
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariffSpec.addRate(r3);
    Tariff te = new Tariff(tariffSpec);
    te.init();
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 23, 50, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("23:50 Sat", -0.8, te.getUsageCharge(10.0, 220.0, true), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("midnight Sun", -0.3, te.getUsageCharge(5.0, 230.0, true), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 3, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("7:00 price Mon", -0.24, te.getUsageCharge(4.0, 235.0, true), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 3, 20, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("20:00 Mon", -0.48, te.getUsageCharge(8.0, 235.0, true), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 4, 1, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("1:00 Tue", -0.12, te.getUsageCharge(2.0, 235.0, true), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 4, 12, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("noon Tue", -0.3, te.getUsageCharge(5.0, 235.0, true), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 4, 23, 59, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("23:56 Tue", -3.0, te.getUsageCharge(50.0, 235.0, true), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 5, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("midnight Wed", -0.64, te.getUsageCharge(8.0, 235.0, true), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 5, 12, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("noon Wed", -2.25, te.getUsageCharge(15.0, 235.0, true), 1e-6);
  }
  
  // tiers
  @Test
  public void testTimeOfUseTier ()
  {
    Rate r1 = new Rate().withValue(-0.15).withDailyBegin(7).withDailyEnd(17);
    Rate r2 = new Rate().withValue(-0.08).withDailyBegin(18).withDailyEnd(6);
    Rate r3 = new Rate().withValue(-0.2).withTierThreshold(20);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariffSpec.addRate(r3);
    Tariff te = new Tariff(tariffSpec);
    te.init();
    assertEquals("noon price, below", -1.5, te.getUsageCharge(10.0, 5.0, true), 1e-6);
    assertEquals("noon price, above", -2.0, te.getUsageCharge(10.0, 25.0, true), 1e-6);
    assertEquals("noon price, split", -1.75, te.getUsageCharge(10.0, 15.0, true), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("midnight price, below", -0.4, te.getUsageCharge(5.0, 12.0, true), 1e-6);
    assertEquals("midnight price, above", -1.0, te.getUsageCharge(5.0, 22.0, true), 1e-6);
    assertEquals("midnight price, split", -0.76, te.getUsageCharge(5.0, 18.0, true), 1e-6);
  }
  
  // multiple tiers
  @Test
  public void testMultiTiers ()
  {
    Rate r1 = new Rate().withValue(-0.15).withTierThreshold(10);
    Rate r2 = new Rate().withValue(-0.1).withTierThreshold(5);
    Rate r3 = new Rate().withValue(-0.2).withTierThreshold(20);
    Rate r4 = new Rate().withValue(-0.07);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariffSpec.addRate(r3);
    tariffSpec.addRate(r4);
    Tariff te = new Tariff(tariffSpec);
    te.init();
    assertEquals("first tier", -0.14, te.getUsageCharge(2.0, 2.0, true), 1e-6);
    assertEquals("first-second tier", -0.41, te.getUsageCharge(5.0, 2.0, true), 1e-6);
    assertEquals("second tier", -0.2, te.getUsageCharge(2.0, 6.0, true), 1e-6);
    assertEquals("second-third tier", -0.6, te.getUsageCharge(5.0, 7.0, true), 1e-6);
    assertEquals("third tier", -0.3, te.getUsageCharge(2.0, 12.0, true), 1e-6);
    assertEquals("third-fourth tier", -0.85, te.getUsageCharge(5.0, 17.0, true), 1e-6);
    assertEquals("fourth tier", -0.4, te.getUsageCharge(2.0, 22.0, true), 1e-6);
    assertEquals("second-fourth tier", -2.1, te.getUsageCharge(14.0, 8.0, true), 1e-6);
  }

  // variable
  @Test
  public void testVarRate ()
  {
    Rate r1 = new Rate().withFixed(false).withMinValue(-0.05).withMaxValue(-0.50)
                        .withNoticeInterval(3).withExpectedMean(-0.10).withDailyBegin(7).withDailyEnd(17);
    Rate r2 = new Rate().withValue(-0.08).withDailyBegin(18).withDailyEnd(6);

    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    r1.addHourlyCharge(new HourlyCharge(new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant(), -0.09), true);
    r1.addHourlyCharge(new HourlyCharge(new DateTime(2011, 1, 1, 13, 0, 0, 0, DateTimeZone.UTC).toInstant(), -0.11));
    r1.addHourlyCharge(new HourlyCharge(new DateTime(2011, 1, 1, 14, 0, 0, 0, DateTimeZone.UTC).toInstant(), -0.13));
    r1.addHourlyCharge(new HourlyCharge(new DateTime(2011, 1, 1, 15, 0, 0, 0, DateTimeZone.UTC).toInstant(), -0.14));
    Tariff te = new Tariff(tariffSpec);
    te.init();
    assertEquals("current charge, noon Sunday", -0.9, te.getUsageCharge(10.0, 0.0, false), 1e-6);
    assertEquals("13:00 charge, noon Sunday", -1.1,
      te.getUsageCharge(new DateTime(2011, 1, 1, 13, 0, 0, 0, DateTimeZone.UTC).toInstant(), 10.0, 0.0), 1e-6);
    assertEquals("14:00 charge, noon Sunday", -1.3,
      te.getUsageCharge(new DateTime(2011, 1, 1, 14, 0, 0, 0, DateTimeZone.UTC).toInstant(), 10.0, 0.0), 1e-6);
    assertEquals("15:00 charge, noon Sunday", -1.4,
      te.getUsageCharge(new DateTime(2011, 1, 1, 15, 0, 0, 0, DateTimeZone.UTC).toInstant(), 10.0, 0.0), 1e-6);
    assertEquals("16:00 charge, noon Sunday", -1.0,
      te.getUsageCharge(new DateTime(2011, 1, 1, 16, 0, 0, 0, DateTimeZone.UTC).toInstant(), 10.0, 0.0), 1e-6);
    assertEquals("18:00 charge, noon Sunday", -0.8,
      te.getUsageCharge(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant(), 10.0, 0.0), 1e-6);
  }
  
  // single rate, interruptible
  @Test
  public void testSimpleCurtailment ()
  {
    TariffSpecification spec =
        new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 8);
    Rate r1 = new Rate()
                  .withValue(-0.121)
                  .withMaxCurtailment(0.3);
    spec.addRate(r1);
    Tariff te = new Tariff(spec);
    te.init();
    assertEquals("correct max curtailment 1", 9.0,
                 te.getMaxCurtailment(30.0, 0.0), 1e-6);
    assertEquals("correct max curtailment 1", 9.0,
                 te.getMaxCurtailment(30.0, 1000.0), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("correct max curtailment 1", 9.0,
                 te.getMaxCurtailment(30.0, 0.0), 1e-6);
  }
  
  // single rate, not interruptible
  @Test
  public void testNoCurtailment ()
  {
    TariffSpecification spec =
        new TariffSpecification(broker, PowerType.CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 8);
    Rate r1 = new Rate()
                  .withValue(-0.121)
                  .withMaxCurtailment(0.3);
    spec.addRate(r1);
    Tariff te = new Tariff(spec);
    te.init();
    assertEquals("correct max curtailment 1", 0.0,
                 te.getMaxCurtailment(30.0, 0.0), 1e-6);
  }
  
  // multiple rates, single tier, one rate interruptible
  @Test
  public void testMultiRateCurtailment ()
  {
    TariffSpecification spec =
        new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 8);
    Rate r1 = new Rate().withValue(-0.15)
                        .withDailyBegin(7)
                        .withDailyEnd(17)
                        .withMaxCurtailment(0.1);
    Rate r2 = new Rate().withValue(-0.08)
                        .withDailyBegin(18)
                        .withDailyEnd(6)
                        .withMaxCurtailment(0.5);
    spec.addRate(r1);
    spec.addRate(r2);
    Tariff te = new Tariff(spec);
    te.init();
    assertEquals("noon price", -3.0, te.getUsageCharge(20.0, 200.0, true), 1e-6);
    assertEquals("noon max curtailment", 3.0,
                 te.getMaxCurtailment(30.0, 0.0), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("18:00 price", -0.8, te.getUsageCharge(10.0, 220.0, true), 1e-6);
    assertEquals("18:00 max curtailment", 15.0,
                 te.getMaxCurtailment(30.0, 0.0), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("midnight price", -0.4, te.getUsageCharge(5.0, 230.0, true), 1e-6);
    assertEquals("midnight max curtailment", 15.0,
                 te.getMaxCurtailment(30.0, 0.0), 1e-6);
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals("7:00 price", -0.6, te.getUsageCharge(4.0, 235.0, true), 1e-6);
    assertEquals("7:00 max curtailment", 3.0,
                 te.getMaxCurtailment(30.0, 0.0), 1e-6);
  }
  
  // multiple rates, multiple tiers, upper tier interruptible
  @Test
  public void testMultiTierCurtailment ()
  {
    TariffSpecification spec =
        new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 8);
    Rate r1 = new Rate().withValue(-0.15)
                        .withTierThreshold(10)
                        .withMaxCurtailment(0.5);
    Rate r2 = new Rate().withValue(-0.08)
                        .withMaxCurtailment(0.1);
    spec.addRate(r1);
    spec.addRate(r2);
    Tariff te = new Tariff(spec);
    te.init();
    assertEquals("3.0 curtailment", 0.3,
                 te.getMaxCurtailment(3.0, 0.0), 1e-6);
    assertEquals("9.9 curtailment", 0.3,
                 te.getMaxCurtailment(3.0, 6.9), 1e-6);
    assertEquals("cross-boundary curtailment", 1.2,
                 te.getMaxCurtailment(4.0, 8.0), 1e-6);
    assertEquals("high curtailment", 2.0,
                 te.getMaxCurtailment(4.0, 11.0), 1e-6);
  }
}
