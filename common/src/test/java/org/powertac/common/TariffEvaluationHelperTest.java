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

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.msg.MarketBootstrapData;
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
  private MarketBootstrapData mbd;
  private Instant start;
  
  @BeforeEach
  public void setUp () throws Exception
  {
    Competition.newInstance("test");
    teh = new TariffEvaluationHelper();
    timeService = new TimeService();
    tariffRepo = new TariffRepo();
    ReflectionTestUtils.setField(teh, "timeService", timeService);
    start = ZonedDateTime.of(2011, 1, 1, 12, 0, 0, 0,  ZoneOffset.UTC).toInstant();
    timeService.init(start.plusMillis(TimeService.HOUR)); // init subtracts one hour
    broker = new Broker ("testBroker");
    tariffSpec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    mbd = new MarketBootstrapData(new double[] {1.0,1.0,1.0},
                                  new double[] {-31.0,-32.0,-33.0});

  }

  /**
   * Test method for {@link org.powertac.common.TariffEvaluationHelper#init(double, double, double, double)}.
   */
  @Test
  public void testInit ()
  {
    teh.init(.6, .4, .5, 10000.0);
    assertEquals(0.6, teh.getWtExpected(), 1e-6, "correct wtExpected");
    assertEquals(0.4, teh.getWtMax(), 1e-6, "correct wtMax");
    assertEquals(0.5, teh.getWtRealized(), 1e-6, "correct wtRealized");
    assertEquals(10000.0, teh.getSoldThreshold(), 1e-3, "correct threshold");
  }

  /**
   * Test method for {@link org.powertac.common.TariffEvaluationHelper#getNormWtExpected()}.
   */
  @Test
  public void testGetNormWtExpected ()
  {
    teh.init(1.2, .8, .5, 10000.0);
    assertEquals(0.6, teh.getNormWtExpected(), 1e-6, "correct wtExpected");
    assertEquals(0.4, teh.getNormWtMax(), 1e-6, "correct wtMax");
    teh.init(.6, .4, .5, 10000.0);
    teh.setWtExpected(1.6);
    assertEquals(0.8, teh.getNormWtExpected(), 1e-6, "correct wtExpected");
    assertEquals(0.2, teh.getNormWtMax(), 1e-6, "correct wtMax");
  }

  /**
   * Test method for {@link org.powertac.common.TariffEvaluationHelper#getNormWtMax()}.
   */
  @Test
  public void testGetNormWtMax ()
  {
    teh.init(.6, .4, .5, 10000.0);
    teh.setWtMax(1.4);
    assertEquals(0.3, teh.getNormWtExpected(), 1e-6, "correct wtExpected");
    assertEquals(0.7, teh.getNormWtMax(), 1e-6, "correct wtMax");
  }
  
  /**
   * Test bare init case
   */
  @Test
  public void testGetNormWtBareInit ()
  {
    teh.init();
    assertEquals(0.6, teh.getNormWtExpected(), 1e-6, "correct wtExpected");
    assertEquals(0.4, teh.getNormWtMax(), 1e-6, "correct wtMax");
  }

  /**
   * Test method for {@link org.powertac.common.TariffEvaluationHelper#getWeightedValue(org.powertac.common.Rate)}.
   */
  @Test
  public void testGetWeightedValue0Usage ()
  {
    Rate r = new Rate().withFixed(false)
            .withValue(-.1).withMaxValue(-.2).withExpectedMean(-.15);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = new double[1];
    usage[0] = 0.0;
    teh.estimateCost(tariff, usage, start);
    double result = teh.getWeightedValue(r);
    assertEquals((.6 * -.15 + .4 * -.2), result, 1e-6, "correct result");
  }

  @Test
  public void testGetWeightedValueMidUsage ()
  {
    Rate r = new Rate().withFixed(false)
            .withValue(-.1).withMaxValue(-.2).withExpectedMean(-.15);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
    HourlyCharge hc = new HourlyCharge(start, -0.18);
    tariff.addHourlyCharge(hc, r.getId());
    tariff.getUsageCharge(10000.0, true);

    // alpha = 0.75
    teh.init(.6, .4, .5, 10000.0);
    double[] usage = new double[1];
    usage[0] = 0.0;
    teh.estimateCost(tariff, usage, start);
    double result = teh.getWeightedValue(r);
    double expected = 0.75 * (.6 * -.15 + .4 * -.2) + 0.25 * -.18;
    assertEquals(expected, result, 1e-6, "correct result");
  }

  @Test
  public void testEstimateCostSimpleRateZeroUsage ()
  {
    Rate r = new Rate().withFixed(true).withValue(-.1);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double result = teh.estimateCost(tariff, usage, start);
    assertEquals(-30.0, result, 1e-6, "correct result");
    result = teh.estimateCost(tariff, usage);
    assertEquals(-30.0, result, 1e-6, "correct result");
  }

  @Test
  public void testEstimateCostSimpleRateMidUsage ()
  {
    Rate r = new Rate().withFixed(true).withValue(-.1);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
    tariff.getUsageCharge(10000.0, true);

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double result = teh.estimateCost(tariff, usage, start);
    assertEquals(-30.0, result, 1e-6, "correct result");
  }

  // Issue #792
  @Test
  public void testEstimateCostSimple48Usage ()
  {
    Rate r = new Rate().withFixed(true).withValue(-.1);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
    tariff.getUsageCharge(10000.0, true);

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0,200.0,100.0,200.0,100.0,200.0,100.0,200.0,100.0,200.0,100.0,200.0,
                      0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    double result = teh.estimateCost(tariff, usage, start);
    assertEquals(-30.0*6, result, 1e-6, "correct result");
    result = teh.estimateCost(tariff, usage);
    assertEquals(-30.0*6, result, 1e-6, "correct result");
  }

  @Test
  public void testEstimatedCostTOU ()
  {
    Rate r1 = new Rate().withFixed(true)
        .withValue(-.1).withDailyBegin(12).withDailyEnd(16);
    Rate r2 = new Rate().withFixed(true)
        .withValue(-.2).withDailyBegin(17).withDailyEnd(11);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r1, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 100.0, 200.0, 200.0, 200.0, 300.0};
    double expected = 600.0 * -.1 + 500.0 * -.2;
    double result = teh.estimateCost(tariff, usage, start);
    assertEquals(expected, result, 1e-6, "correct result");
    result = teh.estimateCost(tariff, usage);
    assertEquals(expected, result, 1e-6, "correct result");
  }

  @Test
  public void testEstimateCostVarRateMidUsage ()
  {
    Rate r = new Rate().withFixed(false)
            .withValue(-.1).withMaxValue(-.2).withExpectedMean(-.15);
    tariffSpec.addRate(r);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
    HourlyCharge hc = new HourlyCharge(start, -0.18);
    tariff.addHourlyCharge(hc, r.getId());
    tariff.getUsageCharge(10000.0, true);

    // estimate with no hourly charges
    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double expected = 300.0 * (0.75 * (.6 * -.15 + .4 * -.2) + 0.25 * -.18);
    double result = teh.estimateCost(tariff, usage, start);
    assertEquals(expected, result, 1e-6, "correct result");
    
    // add a single hourly charge -- it should be ignored
    hc = new HourlyCharge(start.plusMillis(TimeService.HOUR), 0.12);
    tariff.addHourlyCharge(hc, r.getId());
    teh.init(.6, .4, .5, 10000.0);
    double[] usage1 = {100.0, 200.0};
    expected = 300.0 * (0.75 * (.6 * -.15 + .4 * -.2) + 0.25 * -.18);
    result = teh.estimateCost(tariff, usage1, start);
    assertEquals(expected, result, 1e-6, "correct result");
  }

  @Test
  public void testEstimateCostTwoVarZeroUsage ()
  {
    Rate r1 = new Rate().withFixed(false)
            .withValue(-.1).withMaxValue(-.2).withExpectedMean(-.15)
            .withDailyBegin(0).withDailyEnd(13);
    Rate r2 = new Rate().withFixed(false)
            .withValue(-.15).withMaxValue(-.9).withExpectedMean(-.2)
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
    double expected = 100.0 * (.6 * -.15 + .4 * -.2)
                      + 200.0 * (.6 * -.2 + .4 * -.9);
    double result = teh.estimateCost(tariff, usage, start);
    assertEquals(expected, result, 1e-6, "correct result");
  }

  @Test
  public void testEstimateCostTwoVarZeroUsagePeriodic ()
  {
    Rate r1 = new Rate().withFixed(false)
            .withValue(-.1).withMaxValue(-.2).withExpectedMean(-.15)
            .withDailyBegin(0).withDailyEnd(13);
    Rate r2 = new Rate().withFixed(false)
            .withValue(-.15).withMaxValue(-.9).withExpectedMean(-.2)
            .withDailyBegin(14).withDailyEnd(23);
    tariffSpec.withPeriodicPayment(-0.24);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r1, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double expected = 100.0 * (.6 * -.15 + .4 * -.2)
                      + 200.0 * (.6 * -.2 + .4 * -.9);
    assertEquals(expected, teh.estimateCost(tariff, usage, start, false), 1e-6, "correct result");
    assertEquals(expected - 0.02, teh.estimateCost(tariff, usage, start), 1e-6, "correct result");
  }

  @Test
  public void testEstimateCostTwoVarZeroUsageArray ()
  {
    Rate r1 = new Rate().withFixed(false)
            .withValue(-.1).withMaxValue(-.2).withExpectedMean(-.15)
            .withDailyBegin(0).withDailyEnd(13);
    Rate r2 = new Rate().withFixed(false)
            .withValue(-.15).withMaxValue(-.9).withExpectedMean(-.2)
            .withDailyBegin(14).withDailyEnd(23);
    tariffSpec.withPeriodicPayment(-0.24);
    tariffSpec.addRate(r1);
    tariffSpec.addRate(r2);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r1, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double[] expected = {100.0 * (.6 * -.15 + .4 * -.2),
                         200.0 * (.6 * -.2 + .4 * -.9)};
    double[] result = teh.estimateCostArray(tariff, usage, false);
    assertEquals(expected[0], result[0], 1e-6, "correct first");
    assertEquals(expected[1], result[1], 1e-6, "correct result");
    result = teh.estimateCostArray(tariff, usage);
    assertEquals(expected[0] - 0.01, result[0], 1e-6, "correct first");
    assertEquals(expected[1] - 0.01, result[1], 1e-6, "correct result");
  }
  
  @Test
  public void testEstimateCostMixedZeroUsage ()
  {
    Rate r1 = new Rate().withFixed(false)
            .withValue(-.1).withMaxValue(-.2).withExpectedMean(-.15)
            .withDailyBegin(0).withDailyEnd(13);
    Rate r2 = new Rate().withFixed(true).withValue(-.18)
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
    double expected = 100.0 * (.6 * -.15 + .4 * -.2)
                      + 200.0 * -.18;
    double result = teh.estimateCost(tariff, usage, start);
    assertEquals(expected, result, 1e-6, "correct result");
  }

  // regulation discount ratios
  @Test
  public void testUpRegulationDiscount ()
  {
    assertEquals(0.9999, teh.upRegulationDiscount(1.0), 1e-4, "low price");
    assertEquals(0.9526, teh.upRegulationDiscount(3.4), 1e-4, "3.4 x price");
    assertEquals(0.5, teh.upRegulationDiscount(4.4), 1e-4, "4.4 x price");
    assertEquals(0.001,teh.upRegulationDiscount(6.7), 1e-4, "6.7 x price");
  }

  // regulation discount ratios
  @Test
  public void testDownRegulationDiscount ()
  {
    assertEquals(1.0, teh.downRegulationDiscount(0.0), 1e-4, "very low");
    assertEquals(0.9998, teh.downRegulationDiscount(0.14), 1e-4, "zero price");
    assertEquals(0.8581, teh.downRegulationDiscount(0.58), 1e-4, "low price");
    assertEquals(0.5, teh.downRegulationDiscount(0.7), 1e-4, "halfway");
    assertEquals(0.1824, teh.downRegulationDiscount(0.8), 1e-4, "high price");
    assertEquals(0.0110, teh.downRegulationDiscount(1.0), 1e-4, "very high price");
  }

  // storage example, no estimates
  @Test
  public void testEstimateCostSimpleRateStorage1 ()
  {
    TariffSpecification storageSpec =
            new TariffSpecification(broker, PowerType.STORAGE);

    Rate r = new Rate().withFixed(true).withValue(-.1);
    storageSpec.addRate(r);
    RegulationRate rr =
      new RegulationRate().withUpRegulationPayment(0.2)
          .withDownRegulationPayment(-0.05);
    storageSpec.addRate(rr);
    tariff = new Tariff(storageSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
    Competition.currentCompetition().setMarketBootstrapData(mbd);
    tariff.getUsageCharge(10000.0, true);

    teh.init(.6, .4, .5, 10000.0);
    double[] usage = {100.0, 200.0};
    double result = teh.estimateCost(tariff, usage, start);
    assertEquals(-30.0, result, 1e-6, "correct result");
  }

  // thermal storage example, estimates {2, 0, 1}
  @Test
  public void testEstimateCostSimpleRateStorage2 ()
  {
    Rate r = new Rate().withFixed(true).withValue(-.1);
    tariffSpec.addRate(r);
    RegulationRate rr =
      new RegulationRate().withUpRegulationPayment(0.1)
          .withDownRegulationPayment(-0.04);
    tariffSpec.addRate(rr);
    tariff = new Tariff(tariffSpec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(r, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
    Competition.currentCompetition().setMarketBootstrapData(mbd);
    // mean market price = .032
    // up-regulation price 0.2 ratio is 6.25: discount = .00117
    // down-regulation price -0.04 ratio should be 1.0
    double result = tariff.getUsageCharge(10000.0, true);
    assertEquals(10000.0 * -0.1, result, 1.0e-6, "correct usage charge");

    teh.init(.6, .4, .5, 10000.0);
    teh.initializeRegulationFactors(-2.0, 0.0, 1.0);
    assertEquals(0.1, tariff.getRegulationCharge(-1.0, false), 1e-6);
    double[] usage = {100.0, 200.0};
    result = teh.estimateCost(tariff, usage, start);
    double up = 0.97865; // approx. discount
    double down = 0.32082;
    assertEquals(((100.0 - 2.0 + 1.0) * -0.1
            + 2.0 * 0.1 * up - 1.0 * 0.04 * down
            + (200.0 - 2.0 + 1.0) * -0.1
            + 2.0 * 0.1 * up- 1.0 * 0.04 * down),
                 result, 1e-4, "correct result");
  }
}
