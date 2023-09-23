/*
 * Copyright (c) 2011-2013 by John Collins.
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

import java.io.File;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.enumerations.PowerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class TariffTests
{
  @Autowired
  private TimeService timeService; // dependency injection

  private TariffSpecification tariffSpec; // instance var
  private TariffSpecification productionSpec; // instance var

  private Instant start;
  private Instant exp;
  private Broker broker;

  @AfterAll
  public static void saveLogs () throws Exception
  {
    File state = new File("log/test.state");
    state.renameTo(new File("log/TariffTests.state"));
    File trace = new File("log/test.trace");
    trace.renameTo(new File("log/TariffTests.trace"));
  }

  @BeforeEach
  public void setUp () 
  {
    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(start);
    Competition comp = Competition.newInstance("test");
    broker = new Broker ("testBroker");
    exp = new DateTime(2011, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    tariffSpec = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8);
    productionSpec = new TariffSpecification(broker, PowerType.PRODUCTION)
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
    assertNotNull(te, "non-null result");
    assertEquals(tariffSpec, te.getTariffSpecification(), "correct TariffSpec");
    assertEquals(0.0, te.getRealizedPrice(), 1e-6, "correct initial realized price");
    assertEquals(exp, te.getTariffSpecification().getExpiration(), "correct expiration in spec");
    assertEquals(exp, te.getExpiration(), "correct expiration");
    assertEquals(start, te.getOfferDate(), "correct publication time");
    assertFalse(te.isExpired(), "not expired");
    assertTrue(te.isCovered(), "covered");
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
    assertEquals(-99.8/501.2, te.getRealizedPrice(), 1.0e-6, "Correct realized price");
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
    assertEquals(-0.121, te.getUsageCharge(1.0, false), 1e-6, "correct charge, default case");
    assertEquals(-1.21, te.getUsageCharge(10.0, false), 1e-6, "correct charge, today");
    assertEquals(-2.42, te.getUsageCharge(now.minus(TimeService.DAY), 20.0), 1e-6, "correct charge yesterday");
    assertEquals(-12.1, te.getUsageCharge(now.plus(TimeService.DAY), 100.0), 1e-6, "correct charge tomorrow");
    assertEquals(-3.63, te.getUsageCharge(now.minus(TimeService.HOUR), 30.0), 1e-6, "correct charge an hour ago");
    assertEquals(-1.21, te.getUsageCharge(now.plus(TimeService.HOUR), 10.0), 1e-6, "correct charge an hour from now");
    //assertEquals("daily rate map", 1, te.rateMap.size())
    //assertEquals("rate map has 24 entries", 24, te.rateMap[0].size())
    assertTrue(te.isCovered(), "covered");
  }
  
  @Test
  public void testSimpleProduction ()
  {
    Rate r1 = new Rate().withValue(0.121);
    productionSpec.addRate(r1);
    Instant now = timeService.getCurrentTime();
    Tariff te = new Tariff(productionSpec);
    te.init();
    assertEquals(0.121, te.getUsageCharge(-1.0, false), 1e-6, "correct charge, default case");
    assertEquals(1.21, te.getUsageCharge(-10.0, false), 1e-6, "correct charge, today");
    assertEquals(2.42, te.getUsageCharge(now.minus(TimeService.DAY), -20.0), 1e-6, "correct charge yesterday");
    assertEquals(12.1, te.getUsageCharge(now.plus(TimeService.DAY), -100.0), 1e-6, "correct charge tomorrow");
    assertEquals(3.63, te.getUsageCharge(now.minus(TimeService.HOUR), -30.0), 1e-6, "correct charge an hour ago");
    assertEquals(1.21, te.getUsageCharge(now.plus(TimeService.HOUR), -10.0), 1e-6, "correct charge an hour from now");
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
    te.getUsageCharge(20.0, true);
    assertEquals(-0.131, te.getRealizedPrice(), 1e-6, "realized price 1");
    te.getUsageCharge(10.0, true);
    assertEquals(-0.131, te.getRealizedPrice(), 1e-6, "realized price 2");
    te.getUsageCharge(3.0, true);
    assertEquals(-0.131, te.getRealizedPrice(), 1e-6, "realized price 3");
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
    assertTrue(te.init(), "valid Tariff");
    assertEquals(-3.0, te.getUsageCharge(20.0, true), 1e-6, "noon price");
    assertEquals(-0.15, te.getRealizedPrice(), 1e-6, "realized price");
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.8, te.getUsageCharge(10.0, true), 1e-6, "18:00 price");
    assertEquals(-3.8/30.0, te.getRealizedPrice(), 1e-6, "realized price 2");
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.4, te.getUsageCharge(5.0, true), 1e-6, "midnight price");
    assertEquals(-4.2/35.0, te.getRealizedPrice(), 1e-6, "realized price 3");
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.6, te.getUsageCharge(4.0, true), 1e-6, "7:00 price");
    assertEquals(-4.8/39.0, te.getRealizedPrice(), 1e-6, "realized price 4");
    assertTrue(te.isCovered(), "covered");
  }

  // time-of-use rates: -0.15/kwh 7:00-18:00, -0.08/kwh 18:00-7:00
  @Test
  public void testTimeOfUseDailyOverlap ()
  {
    Rate r1 = new Rate().withDailyBegin(0).withDailyEnd(6).withValue(-0.2);
    Rate r2 = new Rate().withDailyBegin(6).withDailyEnd(21).withValue(-0.5);
    Rate r3 = new Rate().withDailyBegin(21).withDailyEnd(0).withValue(-0.2); 
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariffSpec.addRate(r3);
    Tariff te = new Tariff(tariffSpec);
    assertTrue(te.init(), "valid");
    assertEquals(-10.0, te.getUsageCharge(20.0, true), 1e-6, "noon price");
    assertEquals(-0.5, te.getRealizedPrice(), 1e-6, "realized price");
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-5.0, te.getUsageCharge(10.0, true), 1e-6, "18:00 price");
    assertEquals(-0.5, te.getRealizedPrice(), 1e-6, "realized price 2");
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-1.0, te.getUsageCharge(5.0, true), 1e-6, "midnight price");
    assertEquals(-16.0/35.0, te.getRealizedPrice(), 1e-6, "realized price 3");
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-2.0, te.getUsageCharge(4.0, true), 1e-6, "7:00 price");
    assertEquals(-18.0/39.0, te.getRealizedPrice(), 1e-6, "realized price 4");
    assertTrue(te.isCovered(), "covered");
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
    assertFalse(te.isCovered(), "not covered");
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
    assertEquals(-1.2, te.getUsageCharge(20.0, true), 1e-6, "noon price Sat");
    assertEquals(-0.06, te.getRealizedPrice(), 1e-6, "realized price");
    assertTrue(te.isWeekly(), "weekly map");
    //assertEquals("rate map row has 168 entries", 168, te.rateMap[0].size())
    assertTrue(te.isCovered(), "covered");
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.6, te.getUsageCharge(10.0, true), 1e-6, "18:00 price Sat");
    assertEquals(-1.8/30.0, te.getRealizedPrice(), 1e-6, "realized price 2");
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.3, te.getUsageCharge(5.0, true), 1e-6, "midnight price Sun");
    assertEquals(-2.1/35.0, te.getRealizedPrice(), 1e-6, "realized price 3");
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.24, te.getUsageCharge(4.0, true), 1e-6, "7:00 price Sun");
    assertEquals(-2.34/39.0, te.getRealizedPrice(), 1e-6, "realized price 4");
    timeService.setCurrentTime(new DateTime(2011, 1, 3, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.32, te.getUsageCharge(4.0, true), 1e-6, "midnight Mon");
    assertEquals(-2.66/43.0, te.getRealizedPrice(), 1e-6, "realized price 5");
    timeService.setCurrentTime(new DateTime(2011, 1, 3, 6, 59, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.48, te.getUsageCharge(6.0, true), 1e-6, "6:59 Mon");
    assertEquals(-3.14/49.0, te.getRealizedPrice(), 1e-6, "realized price 6");
    timeService.setCurrentTime(new DateTime(2011, 1, 3, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-1.2, te.getUsageCharge(8.0, true), 1e-6, "7:00 Mon");
    assertEquals(-4.34/57.0, te.getRealizedPrice(), 1e-6, "realized price 7");
    timeService.setCurrentTime(new DateTime(2011, 1, 4, 12, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-1.5, te.getUsageCharge(10.0, true), 1e-6, "noon Tue");
    assertEquals(-5.84/67.0, te.getRealizedPrice(), 1e-6, "realized price 8");
    timeService.setCurrentTime(new DateTime(2011, 1, 5, 17, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-1.05, te.getUsageCharge(7.0, true), 1e-6, "17:00 Wed");
    assertEquals(-6.89/74.0, te.getRealizedPrice(), 1e-6, "realized price 9");
    timeService.setCurrentTime(new DateTime(2011, 1, 6, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.72, te.getUsageCharge(9.0, true), 1e-6, "18:00 Thu");
    assertEquals(-7.61/83.0, te.getRealizedPrice(), 1e-6, "realized price 10");
    timeService.setCurrentTime(new DateTime(2011, 1, 7, 23, 59, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.96, te.getUsageCharge(12.0, true), 1e-6, "23:59 Fri");
    assertEquals(-8.57/95.0, te.getRealizedPrice(), 1e-6, "realized price 11");
    timeService.setCurrentTime(new DateTime(2011, 1, 8, 12, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.18, te.getUsageCharge(3.0, true), 1e-6, "midnight Sat");
    assertEquals(-8.75/98.0, te.getRealizedPrice(), 1e-6, "realized price 12");
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
    assertEquals(-0.8, te.getUsageCharge(10.0, true), 1e-6, "23:50 Sat");
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.3, te.getUsageCharge(5.0, true), 1e-6, "midnight Sun");
    timeService.setCurrentTime(new DateTime(2011, 1, 3, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.24, te.getUsageCharge(4.0, true), 1e-6, "7:00 price Mon");
    timeService.setCurrentTime(new DateTime(2011, 1, 3, 20, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.48, te.getUsageCharge(8.0, true), 1e-6, "20:00 Mon");
    timeService.setCurrentTime(new DateTime(2011, 1, 4, 1, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.12, te.getUsageCharge(2.0, true), 1e-6, "1:00 Tue");
    timeService.setCurrentTime(new DateTime(2011, 1, 4, 12, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.3, te.getUsageCharge(5.0, true), 1e-6, "noon Tue");
    timeService.setCurrentTime(new DateTime(2011, 1, 4, 23, 59, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-3.0, te.getUsageCharge(50.0, true), 1e-6, "23:56 Tue");
    timeService.setCurrentTime(new DateTime(2011, 1, 5, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.64, te.getUsageCharge(8.0, true), 1e-6, "midnight Wed");
    timeService.setCurrentTime(new DateTime(2011, 1, 5, 12, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-2.25, te.getUsageCharge(15.0, true), 1e-6, "noon Wed");
  }
//
//  // tiers
//  @Test
//  public void testTimeOfUseTier ()
//  {
//    Rate r1 = new Rate().withValue(-0.15).withDailyBegin(7).withDailyEnd(17);
//    Rate r2 = new Rate().withValue(-0.08).withDailyBegin(18).withDailyEnd(6);
//    Rate r3 = new Rate().withValue(-0.2).withTierThreshold(20);
//    tariffSpec.addRate(r1);
//    tariffSpec.addRate(r2);
//    tariffSpec.addRate(r3);
//    Tariff te = new Tariff(tariffSpec);
//    te.init();
//    assertEquals(-1.5, te.getUsageCharge(10.0, 5.0, true), 1e-6, "noon price, below");
//    assertEquals(-2.0, te.getUsageCharge(10.0, 25.0, true), 1e-6, "noon price, above");
//    assertEquals(-1.75, te.getUsageCharge(10.0, 15.0, true), 1e-6, "noon price, split");
//    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
//    assertEquals(-0.4, te.getUsageCharge(5.0, 12.0, true), 1e-6, "midnight price, below");
//    assertEquals(-1.0, te.getUsageCharge(5.0, 22.0, true), 1e-6, "midnight price, above");
//    assertEquals(-0.76, te.getUsageCharge(5.0, 18.0, true), 1e-6, "midnight price, split");
//  }
//
//  // multiple TOU tiers
//  @Test
//  public void testTimeOfUseTier2 ()
//  {
//    Rate r1 = new Rate().withValue(-0.15).withDailyBegin(7).withDailyEnd(17);
//    Rate r2 = new Rate().withValue(-0.08).withDailyBegin(18).withDailyEnd(6);
//    Rate r3 = new Rate().withValue(-0.25).withTierThreshold(20)
//            .withDailyBegin(16).withDailyEnd(21);
//    Rate r4 = new Rate().withValue(-0.21).withTierThreshold(20)
//            .withDailyBegin(22).withDailyEnd(15);
//    tariffSpec.addRate(r1);
//    tariffSpec.addRate(r2);
//    tariffSpec.addRate(r4);
//    tariffSpec.addRate(r3);
//    Tariff te = new Tariff(tariffSpec);
//    te.init();
//    assertEquals(-1.5, te.getUsageCharge(10.0, 5.0, true), 1e-6, "noon price, below");
//    assertEquals(-2.1, te.getUsageCharge(10.0, 25.0, true), 1e-6, "noon price, above");
//    assertEquals(-1.8, te.getUsageCharge(10.0, 15.0, true), 1e-6, "noon price, split");
//    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
//    assertEquals(-0.4, te.getUsageCharge(5.0, 12.0, true), 1e-6, "midnight price, below");
//    assertEquals(-1.05, te.getUsageCharge(5.0, 22.0, true), 1e-6, "midnight price, above");
//    assertEquals(-0.79, te.getUsageCharge(5.0, 18.0, true), 1e-6, "midnight price, split");
//    timeService.setCurrentTime(new DateTime(2011, 1, 2, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
//    assertEquals(-0.8, te.getUsageCharge(10.0, 5.0, true), 1e-6, "evening price, below");
//    assertEquals(-2.5, te.getUsageCharge(10.0, 25.0, true), 1e-6, "evening price, above");
//    assertEquals(-1.65, te.getUsageCharge(10.0, 15.0, true), 1e-6, "evening price, split");
//  }
//
//  // multiple TOU tiers - production
//  @Test
//  public void testTimeOfUseTierProd ()
//  {
//    Rate r1 = new Rate().withValue(0.15).withDailyBegin(7).withDailyEnd(17);
//    Rate r2 = new Rate().withValue(0.08).withDailyBegin(18).withDailyEnd(6);
//    Rate r3 = new Rate().withValue(0.25).withTierThreshold(-20.0)
//            .withDailyBegin(16).withDailyEnd(21);
//    Rate r4 = new Rate().withValue(0.21).withTierThreshold(-20.0)
//            .withDailyBegin(22).withDailyEnd(15);
//    productionSpec.addRate(r1);
//    productionSpec.addRate(r2);
//    productionSpec.addRate(r4);
//    productionSpec.addRate(r3);
//    Tariff te = new Tariff(productionSpec);
//    te.init();
//    assertEquals(1.5, te.getUsageCharge(-10.0, -5.0, true), 1e-6, "noon price, below");
//    assertEquals(2.1, te.getUsageCharge(-10.0, -25.0, true), 1e-6, "noon price, above");
//    assertEquals(1.8, te.getUsageCharge(-10.0, -15.0, true), 1e-6, "noon price, split");
//    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
//    assertEquals(0.4, te.getUsageCharge(-5.0, -12.0, true), 1e-6, "midnight price, below");
//    assertEquals(1.05, te.getUsageCharge(-5.0, -22.0, true), 1e-6, "midnight price, above");
//    assertEquals(0.79, te.getUsageCharge(-5.0, -18.0, true), 1e-6, "midnight price, split");
//    timeService.setCurrentTime(new DateTime(2011, 1, 2, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
//    assertEquals(0.8, te.getUsageCharge(-10.0, -5.0, true), 1e-6, "evening price, below");
//    assertEquals(2.5, te.getUsageCharge(-10.0, -25.0, true), 1e-6, "evening price, above");
//    assertEquals(1.65, te.getUsageCharge(-10.0, -15.0, true), 1e-6, "evening price, split");
//  }
//
//  // multiple tiers
//  @Test
//  public void testMultiTiers ()
//  {
//    Rate r1 = new Rate().withValue(-0.15).withTierThreshold(10);
//    Rate r2 = new Rate().withValue(-0.1).withTierThreshold(5);
//    Rate r3 = new Rate().withValue(-0.2).withTierThreshold(20);
//    Rate r4 = new Rate().withValue(-0.07);
//    tariffSpec.addRate(r1);
//    tariffSpec.addRate(r2);
//    tariffSpec.addRate(r3);
//    tariffSpec.addRate(r4);
//    Tariff te = new Tariff(tariffSpec);
//    te.init();
//    assertEquals(-0.14, te.getUsageCharge(2.0, 2.0, true), 1e-6, "first tier");
//    assertEquals(-0.41, te.getUsageCharge(5.0, 2.0, true), 1e-6, "first-second tier");
//    assertEquals(-0.2, te.getUsageCharge(2.0, 6.0, true), 1e-6, "second tier");
//    assertEquals(-0.6, te.getUsageCharge(5.0, 7.0, true), 1e-6, "second-third tier");
//    assertEquals(-0.3, te.getUsageCharge(2.0, 12.0, true), 1e-6, "third tier");
//    assertEquals(-0.85, te.getUsageCharge(5.0, 17.0, true), 1e-6, "third-fourth tier");
//    assertEquals(-0.4, te.getUsageCharge(2.0, 22.0, true), 1e-6, "fourth tier");
//    assertEquals(-2.1, te.getUsageCharge(14.0, 8.0, true), 1e-6, "second-fourth tier");
//  }

  // variable
  @Test
  public void testVarRate ()
  {
    Rate r1 = new Rate().withFixed(false).withMinValue(-0.05).withMaxValue(-0.50)
                        .withNoticeInterval(3).withExpectedMean(-0.10).withDailyBegin(7).withDailyEnd(17);
    Rate r2 = new Rate().withValue(-0.08).withDailyBegin(18).withDailyEnd(6);

    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    Tariff te = new Tariff(tariffSpec);
    te.init();
    
    // test r1 without hourly charge, uses expected mean
    assertEquals(-1.0, te.getUsageCharge(10.0, false), 1e-6, "current charge, noon Sunday");

    // test with hourly charges
    r1.addHourlyCharge(new HourlyCharge(new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant(), -0.09), true);
    r1.addHourlyCharge(new HourlyCharge(new DateTime(2011, 1, 1, 13, 0, 0, 0, DateTimeZone.UTC).toInstant(), -0.11), true);
    r1.addHourlyCharge(new HourlyCharge(new DateTime(2011, 1, 1, 14, 0, 0, 0, DateTimeZone.UTC).toInstant(), -0.13), true);
    r1.addHourlyCharge(new HourlyCharge(new DateTime(2011, 1, 1, 15, 0, 0, 0, DateTimeZone.UTC).toInstant(), -0.14));
    assertEquals(-0.9, te.getUsageCharge(10.0, false), 1e-6, "current charge, noon Sunday");
    assertEquals(-1.1, te.getUsageCharge(new DateTime(2011, 1, 1, 13, 0, 0, 0, DateTimeZone.UTC).toInstant(), 10.0), 1e-6, "13:00 charge, noon Sunday");
    assertEquals(-1.3, te.getUsageCharge(new DateTime(2011, 1, 1, 14, 0, 0, 0, DateTimeZone.UTC).toInstant(), 10.0), 1e-6, "14:00 charge, noon Sunday");
    assertEquals(-1.4, te.getUsageCharge(new DateTime(2011, 1, 1, 15, 0, 0, 0, DateTimeZone.UTC).toInstant(), 10.0), 1e-6, "15:00 charge, noon Sunday");
    assertEquals(-1.0, te.getUsageCharge(new DateTime(2011, 1, 1, 16, 0, 0, 0, DateTimeZone.UTC).toInstant(), 10.0), 1e-6, "16:00 charge, noon Sunday");
    assertEquals(-0.8, te.getUsageCharge(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant(), 10.0), 1e-6, "18:00 charge, noon Sunday");
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
    assertEquals(9.0, te.getMaxUpRegulation(30.0), 1e-6, "correct max curtailment 1");
    assertEquals(9.0, te.getMaxUpRegulation(30.0), 1e-6, "correct max curtailment 1");
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(9.0, te.getMaxUpRegulation(30.0), 1e-6, "correct max curtailment 1");
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
    assertEquals(0.0, te.getMaxUpRegulation(30.0), 1e-6, "correct max curtailment 1");
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
    assertEquals(-3.0, te.getUsageCharge(20.0, true), 1e-6, "noon price");
    assertEquals(3.0, te.getMaxUpRegulation(30.0), 1e-6, "noon max curtailment");
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 17, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-3.0, te.getUsageCharge(20.0, true), 1e-6, "17:00 price");
    assertEquals(3.0, te.getMaxUpRegulation(30.0), 1e-6, "17:00 max curtailment");
    timeService.setCurrentTime(new DateTime(2011, 1, 1, 18, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.8, te.getUsageCharge(10.0, true), 1e-6, "18:00 price");
    assertEquals(15.0, te.getMaxUpRegulation(30.0), 1e-6, "18:00 max curtailment");
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.4, te.getUsageCharge(5.0, true), 1e-6, "midnight price");
    assertEquals(15.0, te.getMaxUpRegulation(30.0), 1e-6, "midnight max curtailment");
    timeService.setCurrentTime(new DateTime(2011, 1, 2, 7, 0, 0, 0, DateTimeZone.UTC).toInstant());
    assertEquals(-0.6, te.getUsageCharge(4.0, true), 1e-6, "7:00 price");
    assertEquals(3.0, te.getMaxUpRegulation(30.0), 1e-6, "7:00 max curtailment");
    assertEquals((11.0 * -.15 + 13.0 * -.08) / 24.0, te.getMeanConsumptionPrice(), 1e-6, "mean price");
  }

  // multiple rates, multiple tiers, upper tier interruptible
//  @Test
//  public void testMultiTierCurtailment ()
//  {
//    TariffSpecification spec =
//        new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
//            .withExpiration(exp)
//            .withMinDuration(TimeService.WEEK * 8);
//    Rate r1 = new Rate().withValue(-0.15)
//                        .withTierThreshold(10)
//                        .withMaxCurtailment(0.5);
//    Rate r2 = new Rate().withValue(-0.08)
//                        .withMaxCurtailment(0.1);
//    spec.addRate(r1);
//    spec.addRate(r2);
//    Tariff te = new Tariff(spec);
//    te.init();
//    assertEquals(0.3, te.getMaxUpRegulation(3.0, 0.0), 1e-6, "3.0 curtailment");
//    assertEquals(0.3, te.getMaxUpRegulation(3.0, 6.9), 1e-6, "9.9 curtailment");
//    assertEquals(1.2, te.getMaxUpRegulation(4.0, 8.0), 1e-6, "cross-boundary curtailment");
//    assertEquals(2.0, te.getMaxUpRegulation(4.0, 11.0), 1e-6, "high curtailment");
//  }

  // single rate, no regulation rate, balancing order, interruptible
  @Test
  public void testBOregulation ()
  {
    TariffSpecification spec =
            new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
                .withExpiration(exp)
                .withMinDuration(TimeService.WEEK * 8);
    Rate r1 = new Rate()
            .withValue(-0.120)
            .withMaxCurtailment(0.3);
    spec.addRate(r1);
    Tariff te = new Tariff(spec);
    te.init();
    assertEquals(2.4, te.getRegulationCharge(-20.0, false), 1e-6, "correct up-reg charge");
    assertEquals(-3.6, te.getRegulationCharge(30.0, false), 1e-6, "correct down-reg charge");
  }

  // single rate, regulation rate, storage
  @Test
  public void testRegRate ()
  {
    TariffSpecification spec =
        new TariffSpecification(broker, PowerType.THERMAL_STORAGE_CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 8);
    Rate r1 = new Rate()
                  .withValue(-0.121)
                  .withMaxCurtailment(0.3);
    spec.addRate(r1);
    RegulationRate rr1 = new RegulationRate()
            .withResponse(RegulationRate.ResponseTime.MINUTES)
            .withUpRegulationPayment(0.2)
            .withDownRegulationPayment(-0.1);
    spec.addRate(rr1);
    Tariff te = new Tariff(spec);
    te.init();
    // no longer tests anything -- see Issue #1040
  }

  // single rate, regulation rate, storage
  @Test
  public void testBogusRegRate ()
  {
    TariffSpecification spec =
        new TariffSpecification(broker, PowerType.THERMAL_STORAGE_CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 8);
    Rate r1 = new Rate()
                  .withValue(-0.1)
                  .withMaxCurtailment(0.3);
    spec.addRate(r1);
    RegulationRate rr1 = new RegulationRate()
            .withResponse(RegulationRate.ResponseTime.MINUTES)
            .withUpRegulationPayment(2.0)
            .withDownRegulationPayment(-1.0);
    spec.addRate(rr1);
    Tariff te = new Tariff(spec);
    te.init();
    // no longer tests anything
  }
}
