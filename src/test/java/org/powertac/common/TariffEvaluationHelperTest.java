/*
 * Copyright (c) 2012 by the original author
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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TariffRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 */
public class TariffEvaluationHelperTest
{
  private TariffEvaluationHelper teh;
  private TimeService timeService;
  private Broker broker;
  private TariffSpecification tariffSpec;
  private Tariff tariff;
  private TariffRepo tariffRepo;
  private Instant start;
  
  @Before
  public void setUp () throws Exception
  {
    teh = new TariffEvaluationHelper();
    timeService = new TimeService();
    tariffRepo = new TariffRepo();
    ReflectionTestUtils.setField(teh, "timeService", timeService);
    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(start);
    broker = new Broker ("testBroker");
    tariffSpec = new TariffSpecification(broker, PowerType.CONSUMPTION);
  }

  /**
   * Test method for {@link org.powertac.common.TariffEvaluationHelper#init(double, double, double, double)}.
   */
  @Test
  public void testInit ()
  {
    teh.init(.6, .4, .5, 10000.0);
    assertEquals("correct wtExpected", 0.6, teh.getWtExpected(), 1e-6);
    assertEquals("correct wtMax", 0.4, teh.getWtMax(), 1e-6);
    assertEquals("correct wtRealized", 0.5, teh.getWtRealized(), 1e-6);
    assertEquals("correct threshold", 10000.0, teh.getSoldThreshold(), 1e-3);
  }

  /**
   * Test method for {@link org.powertac.common.TariffEvaluationHelper#getNormWtExpected()}.
   */
  @Test
  public void testGetNormWtExpected ()
  {
    teh.init(1.2, .8, .5, 10000.0);
    assertEquals("correct wtExpected", 0.6, teh.getNormWtExpected(), 1e-6);
    assertEquals("correct wtMax", 0.4, teh.getNormWtMax(), 1e-6);
    teh.init(.6, .4, .5, 10000.0);
    teh.setWtExpected(1.6);
    assertEquals("correct wtExpected", 0.8, teh.getNormWtExpected(), 1e-6);
    assertEquals("correct wtMax", 0.2, teh.getNormWtMax(), 1e-6);
  }

  /**
   * Test method for {@link org.powertac.common.TariffEvaluationHelper#getNormWtMax()}.
   */
  @Test
  public void testGetNormWtMax ()
  {
    teh.init(.6, .4, .5, 10000.0);
    teh.setWtMax(1.4);
    assertEquals("correct wtExpected", 0.3, teh.getNormWtExpected(), 1e-6);
    assertEquals("correct wtMax", 0.7, teh.getNormWtMax(), 1e-6);
  }

  /**
   * Test method for {@link org.powertac.common.TariffEvaluationHelper#getWeightedValue(org.powertac.common.Rate)}.
   */
  @Test
  public void testGetWeightedValue0Usage ()
  {
    Rate r = new Rate().withFixed(false)
            .withValue(.1).withMaxValue(.2).withExpectedMean(.15);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = new double[1];
    usage[0] = 0.0;
    teh.estimateCost(tariff, usage);
    double result = teh.getWeightedValue(r);
    assertEquals("correct result", (.6 * .15 + .4 * .2), result, 1e-6);
  }

  @Test
  public void testGetWeightedValueMidUsage ()
  {
    Rate r = new Rate().withFixed(false)
            .withValue(.1).withMaxValue(.2).withExpectedMean(.15);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
    HourlyCharge hc = new HourlyCharge(start, 0.18);
    tariff.addHourlyCharge(hc, r.getId());
    tariff.getUsageCharge(10000.0, 0.0, true);

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = new double[1];
    usage[0] = 0.0;
    teh.estimateCost(tariff, usage);
    double result = teh.getWeightedValue(r);
    double expected = 0.75 * (.6 * .15 + .4 * .2) + 0.25 * .18;
    assertEquals("correct result", expected, result, 1e-6);
  }

  @Test
  public void testEstimateCostSimpleRateZeroUsage ()
  {
    Rate r = new Rate().withFixed(true).withValue(.1);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double result = teh.estimateCost(tariff, usage);
    assertEquals("correct result", 30.0, result, 1e-6);
  }

  @Test
  public void testEstimateCostSimpleRateMidUsage ()
  {
    Rate r = new Rate().withFixed(true).withValue(.1);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
    tariff.getUsageCharge(10000.0, 0.0, true);

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double result = teh.estimateCost(tariff, usage);
    assertEquals("correct result", 30.0, result, 1e-6);
  }

  @Test
  public void testEstimateCostVarRateMidUsage ()
  {
    Rate r = new Rate().withFixed(false)
            .withValue(.1).withMaxValue(.2).withExpectedMean(.15);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
    HourlyCharge hc = new HourlyCharge(start, 0.18);
    tariff.addHourlyCharge(hc, r.getId());
    tariff.getUsageCharge(10000.0, 0.0, true);

    // estimate with no hourly charges
    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double expected = 300.0 * (0.75 * (.6 * .15 + .4 * .2) + 0.25 * .18);
    double result = teh.estimateCost(tariff, usage);
    assertEquals("correct result", expected, result, 1e-6);
    
    // add a single hourly charge -- it should be ignored
    hc = new HourlyCharge(start.plus(TimeService.HOUR), 0.12);
    tariff.addHourlyCharge(hc, r.getId());
    teh.init(.6, .4, .5, 10000.0);
    double[] usage1 = {100.0, 200.0};
    expected = 300.0 * (0.75 * (.6 * .15 + .4 * .2) + 0.25 * .18);
    result = teh.estimateCost(tariff, usage1);
    assertEquals("correct result", expected, result, 1e-6);
  }

  @Test
  public void testEstimateCostTwoVarZeroUsage ()
  {
    Rate r1 = new Rate().withFixed(false)
            .withValue(.1).withMaxValue(.2).withExpectedMean(.15)
            .withDailyBegin(0).withDailyEnd(13);
    Rate r2 = new Rate().withFixed(false)
            .withValue(.15).withMaxValue(.9).withExpectedMean(.2)
            .withDailyBegin(14).withDailyEnd(23);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r1, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double expected = 100.0 * (.6 * .15 + .4 * .2)
                      + 200.0 * (.6 * .2 + .4 * .9);
    double result = teh.estimateCost(tariff, usage);
    assertEquals("correct result", expected, result, 1e-6);
  }

  @Test
  public void testEstimateCostTwoVarZeroUsagePeriodic ()
  {
    Rate r1 = new Rate().withFixed(false)
            .withValue(.1).withMaxValue(.2).withExpectedMean(.15)
            .withDailyBegin(0).withDailyEnd(13);
    Rate r2 = new Rate().withFixed(false)
            .withValue(.15).withMaxValue(.9).withExpectedMean(.2)
            .withDailyBegin(14).withDailyEnd(23);
    tariffSpec.withPeriodicPayment(0.24);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r1, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double expected = 100.0 * (.6 * .15 + .4 * .2)
                      + 200.0 * (.6 * .2 + .4 * .9);
    assertEquals("correct result", expected,
                 teh.estimateCost(tariff, usage, false), 1e-6);
    assertEquals("correct result", expected + 0.02,
                 teh.estimateCost(tariff, usage), 1e-6);
  }

  @Test
  public void testEstimateCostTwoVarZeroUsageArray ()
  {
    Rate r1 = new Rate().withFixed(false)
            .withValue(.1).withMaxValue(.2).withExpectedMean(.15)
            .withDailyBegin(0).withDailyEnd(13);
    Rate r2 = new Rate().withFixed(false)
            .withValue(.15).withMaxValue(.9).withExpectedMean(.2)
            .withDailyBegin(14).withDailyEnd(23);
    tariffSpec.withPeriodicPayment(0.24);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r1, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double[] expected = {100.0 * (.6 * .15 + .4 * .2),
                         200.0 * (.6 * .2 + .4 * .9)};
    double[] result = teh.estimateCostArray(tariff, usage, false);
    assertEquals("correct first", expected[0], result[0], 1e-6);
    assertEquals("correct result", expected[1], result[1], 1e-6);
    result = teh.estimateCostArray(tariff, usage);
    assertEquals("correct first", expected[0] + 0.01, result[0], 1e-6);
    assertEquals("correct result", expected[1] + 0.01, result[1], 1e-6);
  }
  
  @Test
  public void testEstimateCostMixedZeroUsage ()
  {
    Rate r1 = new Rate().withFixed(false)
            .withValue(.1).withMaxValue(.2).withExpectedMean(.15)
            .withDailyBegin(0).withDailyEnd(13);
    Rate r2 = new Rate().withFixed(true).withValue(.18)
            .withDailyBegin(14).withDailyEnd(23);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r1, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double expected = 100.0 * (.6 * .15 + .4 * .2)
                      + 200.0 * .18;
    double result = teh.estimateCost(tariff, usage);
    assertEquals("correct result", expected, result, 1e-6);
  }
}
