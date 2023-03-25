/*
 * Copyright (c) 2014 by the original author
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
package org.powertac.genco;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.Order;
import org.powertac.common.RandomSeed;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.ContextService;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author jcollins
 */
public class CpGencoTest
{
  private BrokerProxy mockProxy;

  private TimeslotRepo timeslotRepo;
  private WeatherReportRepo mockReportRepo;
  private WeatherForecastRepo mockForecastRepo;

  private CpGenco genco;
  private Competition comp;
  private Instant start;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;
  private TimeService timeService;

  /**
   *
   */
  @BeforeEach
  public void setUp () throws Exception
  {
    comp = Competition.newInstance("Genco test").withTimeslotsOpen(4);
    Competition.setCurrent(comp);
    comp.withTimeslotsOpen(4);
    mockProxy = mock(BrokerProxy.class);
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(eq(CpGenco.class.getName()),
                                    anyLong(),
                                    anyString())).thenReturn(seed);
    timeslotRepo = new TimeslotRepo();
    mockReportRepo = mock(WeatherReportRepo.class);
    mockForecastRepo = mock(WeatherForecastRepo.class);
    genco = new CpGenco("Test");
    start = comp.getSimulationBaseTime().plusMillis(TimeService.DAY);
    timeService = new TimeService();
    timeService.setCurrentTime(start);
    ReflectionTestUtils.setField(timeslotRepo, "timeService", timeService);
  }

  private void init ()
  {
    ContextService svc = mock(ContextService.class);
    when(svc.getBean("timeslotRepo")).thenReturn(timeslotRepo);
    when(svc.getBean("randomSeedRepo")).thenReturn(mockSeedRepo);
    when(svc.getBean("weatherReportRepo")).thenReturn(mockReportRepo);
    when(svc.getBean("weatherForecastRepo")).thenReturn(mockForecastRepo);
    when(seed.nextLong()).thenReturn(1l);
    genco.init(mockProxy, 0, svc);
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#CpGenco(java.lang.String)}.
   */
  @Test
  public void testCpGenco ()
  {
    assertNotNull(genco, "created something");
    assertEquals(genco.getUsername(), "Test", "correct name");
    assertEquals(4.0, genco.getPriceInterval(), 1e-6, "correct price interval");
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#init(org.powertac.common.interfaces.BrokerProxy, int, org.powertac.common.repo.RandomSeedRepo,  org.powertac.common.repo.TimeslotRepo)}.
   */
  @Test
  public void testInit ()
  {
    init();
    verify(mockSeedRepo).getRandomSeed(eq(CpGenco.class.getName()),
                                       anyLong(), eq("bid"));
    double[] ca = genco.getCoefficientArray();
    assertNotNull(ca, "initialized array");
    assertEquals(3, ca.length, "3 elements");
    assertEquals(0.007, ca[0], 1e-6, "correct 1st coeff");
    assertEquals(0.1, ca[1], 1e-6, "correct 1st coeff");
    assertEquals(16.0, ca[2], 1e-6, "correct 1st coeff");
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#withCoefficients(List)}.
   */
  @Test
  public void testWithCoefficients ()
  {
    init();
    List<String> coefficients = Arrays.asList("1.0", "1.1", "1.2");
    genco.withCoefficients(coefficients);
    assertEquals(3, genco.getCoefficients().size(), "3-element list");
    assertEquals(genco.getCoefficients().get(0), "1.0", "first element");
    double[] ca = genco.getCoefficientArray();
    assertEquals(3, ca.length, "3 elements");
    assertEquals(1.0, ca[0], 1e-6, "correct 1st coeff");
    assertEquals(1.1, ca[1], 1e-6, "correct 1st coeff");
    assertEquals(1.2, ca[2], 1e-6, "correct 1st coeff");
  }

  // bogus coefficients should not change defaults
  @Test
  public void bogusCoefficients2 ()
  {
    init();
    List<String> coefficients = Arrays.asList("1.0", "3.0");
    genco.withCoefficients(coefficients);
    double[] ca = genco.getCoefficientArray();
    assertEquals(3, ca.length, "3 elements");
    assertEquals(0.007, ca[0], 1e-6, "correct 1st coeff");
    assertEquals(0.1, ca[1], 1e-6, "correct 1st coeff");
    assertEquals(16.0, ca[2], 1e-6, "correct 1st coeff");
  }

  // bogus coefficients should not change defaults
  @Test
  public void bogusCoefficients4 ()
  {
    init();
    List<String> coefficients = Arrays.asList("1.0", "3.0", "1.0", "3.0");
    genco.withCoefficients(coefficients);
    double[] ca = genco.getCoefficientArray();
    assertEquals(3, ca.length, "3 elements");
    assertEquals(0.007, ca[0], 1e-6, "correct 1st coeff");
    assertEquals(0.1, ca[1], 1e-6, "correct 1st coeff");
    assertEquals(16.0, ca[2], 1e-6, "correct 1st coeff");
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#withPSigma(double)}.
   */
  @Test
  public void testWithPSigma ()
  {
    init();
    assertEquals(0.1, genco.getPSigma(), 1e-6, "correct initial");
    genco.withPSigma(0.01);
    assertEquals(0.01, genco.getPSigma(), 1e-6, "correct post");
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#withQSigma(double)}.
   */
  @Test
  public void testWithQSigma ()
  {
    init();
    assertEquals(0.1, genco.getQSigma(), 1e-6, "correct initial");
    genco.withQSigma(0.01);
    assertEquals(0.01, genco.getQSigma(), 1e-6, "correct post");
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#withPriceInterval(double)}.
   */
  @Test
  public void testWithPriceInterval ()
  {
    init();
    assertEquals(4.0, genco.getPriceInterval(), 1e-6, "correct initial");
    genco.withPriceInterval(6.0);
    assertEquals(6.0, genco.getPriceInterval(), 1e-6, "correct post");
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#withMinQuantity(double)}.
   */
  @Test
  public void testWithMinQuantity ()
  {
    init();
    assertEquals(120.0, genco.getMinQuantity(), 1e-6, "correct initial");
    genco.withMinQuantity(150.0);
    assertEquals(150.0, genco.getMinQuantity(), 1e-6, "correct post");
  }

  // config test
  @Test
  public void configTest ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("genco.cpGenco.coefficients", "1.0, 2.0, 3.0");
    map.put("genco.cpGenco.pSigma", "0.22");
    init();
    MapConfiguration conf = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(conf);
    configurator.configureSingleton(genco);
    assertEquals(3, genco.getCoefficients().size(), "3-element list");
    assertEquals(genco.getCoefficients().get(0), "1.0", "correct 1st");
    assertEquals(genco.getCoefficients().get(2), "3.0", "correct last");
    assertEquals(0.22, genco.getPSigma(), 1e-6, "correct pSigma");
  }

  // Default weather conditions
  private void defaultWeather ()
  {
    WeatherReport wr = new WeatherReport(0, 15.0, 0.0, 0.0, 0.0);
    when(mockReportRepo.currentWeatherReport()).thenReturn(wr);
    List<WeatherForecastPrediction> wfs = new ArrayList<>();
    for (int i = 0; i < comp.getTimeslotsOpen(); i += 1) {
      WeatherForecastPrediction wfp =
          new WeatherForecastPrediction(i + 1, 14.0 + i, 0.0, 0.0, 0.0);
      wfs.add(wfp);
    }
    WeatherForecast wf = new WeatherForecast(0, wfs);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);

  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#generateOrders(org.joda.time.Instant, java.util.List)}.
   */
  @SuppressWarnings("unused")
  @Test
  public void generateFixedOrders ()
  {
    init();
    defaultWeather();
    genco.withMinQuantity(100.0);
    genco.withPriceInterval(10.0);
    genco.withMinBidQuantity(4.0);
    // mock the normal distribution
    NormalDistribution mockNorm = mock(NormalDistribution.class);
    ReflectionTestUtils.setField(genco, "normal01", mockNorm);
    double[] samples = new double[2];
    samples[0] = 0.0;
    samples[1] = 0.0;
    when(mockNorm.sample(2)).thenReturn(samples);
    // capture orders
    final ArrayList<Order> orderList = new ArrayList<Order>(); 
    doAnswer(new Answer<Object>() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        orderList.add((Order)args[0]);
        return null;
      }
    }).when(mockProxy).routeMessage(isA(Order.class));
    // set up some timeslots
    Timeslot ts1 = timeslotRepo.makeTimeslot(start);
    Timeslot ts2 = timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR));
    Timeslot ts3 = timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR * 2));
    assertEquals(4, timeslotRepo.enabledTimeslots().size(), "4 enabled timeslots");
    // 50 mwh already sold in ts2
    MarketPosition posn2 = new MarketPosition(genco, ts2, -50.0);
    genco.addMarketPosition(posn2, ts2.getSerialNumber());
    // generate orders and check
    genco.generateOrders(start, timeslotRepo.enabledTimeslots());
    //System.out.println(orderList);
    assertEquals(64, orderList.size(), "64 orders");
  }

  @SuppressWarnings("unused")
  @Test
  public void generateVarOrders ()
  {
    init();
    defaultWeather();
    genco.withMinQuantity(100.0);
    genco.withPriceInterval(10.0);
    genco.withMinBidQuantity(1.0);
    // capture orders
    final ArrayList<Order> orderList = new ArrayList<Order>(); 
    doAnswer(new Answer<Object>() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        orderList.add((Order)args[0]);
        return null;
      }
    }).when(mockProxy).routeMessage(isA(Order.class));
    // set up some timeslots
    Timeslot ts1 = timeslotRepo.makeTimeslot(start);
    Timeslot ts2 = timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR));
    Timeslot ts3 = timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR * 2));
    assertEquals(4, timeslotRepo.enabledTimeslots().size(), "4 enabled timeslots");
    // 50 mwh already sold in ts2
    MarketPosition posn2 = new MarketPosition(genco, ts2, -50.0);
    genco.addMarketPosition(posn2, ts2.getSerialNumber());
    // generate orders and check
    genco.generateOrders(start, timeslotRepo.enabledTimeslots());
    assertEquals(73, orderList.size(), "73 orders");
  }

  // test temperature adjustment
  @Test
  public void tempAdjustment ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("genco.cpGenco.minQuantity", "300.0");
    map.put("genco.cpGenco.coolingThreshold", "40.0");
    map.put("genco.cpGenco.coolingSlope", "10.0");
    map.put("genco.cpGenco.heatingThreshold", "0.0");
    map.put("genco.cpGenco.heatingSlope", "5.0");
    map.put("genco.cpGenco.misoScaleFactor", "0.5");
    MapConfiguration conf = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(conf);
    configurator.configureSingleton(genco);
    assertEquals(0.0, genco.getEmergencyAdjustment(20.0), 1e-6, "no adjustment");
    assertEquals(0.0, genco.getEmergencyAdjustment(40.0), 1e-6, "no adjustment");
    assertEquals(5.0, genco.getEmergencyAdjustment(41.0), 1e-6, "cooling adjustment");
    assertEquals(0.0, genco.getEmergencyAdjustment(0.0), 1e-6, "no adjustment");
    assertEquals(2.5, genco.getEmergencyAdjustment(-1.0), 1e-6, "heating adjustment");
  }

  // test extreme cooling adjustment
  @Test
  public void extremeCoolingAdjustment ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("genco.cpGenco.minQuantity", "200.0");
    map.put("genco.cpGenco.coolingThreshold", "35.0");
    map.put("genco.cpGenco.coolingSlope", "10.0");
    map.put("genco.cpGenco.misoScaleFactor", "1.0");
    MapConfiguration conf = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(conf);
    configurator.configureSingleton(genco);
    init();
    Timeslot ts0 = timeslotRepo.makeTimeslot(start);
    assertEquals(24, timeslotRepo.currentSerialNumber(), "serial # first timeslot");
    assertEquals(200.0, genco.getMinQuantity(), 1e-6, "correct min quantity");
    List<WeatherForecastPrediction> wfs = new ArrayList<>();
    for (int i = 0; i < comp.getTimeslotsOpen(); i += 1) {
      WeatherForecastPrediction wfp =
          new WeatherForecastPrediction(i + 1, 35.0 + i, 0.0, 0.0, 0.0);
      wfs.add(wfp);
    }
    WeatherForecast wf = new WeatherForecast(24, wfs);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);
    assertEquals(200.0, genco.getAdjustedMinQty(25));
    assertEquals(210.0, genco.getAdjustedMinQty(26));
    assertEquals(220.0, genco.getAdjustedMinQty(27));
    assertEquals(230.0, genco.getAdjustedMinQty(28));
  }

  // test extreme heating adjustment
  @Test
  public void extremeHeatingAdjustment ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("genco.cpGenco.minQuantity", "400.0");
    map.put("genco.cpGenco.heatingThreshold", "0.0");
    map.put("genco.cpGenco.heatingSlope", "12.0");
    map.put("genco.cpGenco.misoScaleFactor", "1.0");
    MapConfiguration conf = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(conf);
    configurator.configureSingleton(genco);
    init();
    Timeslot ts0 = timeslotRepo.makeTimeslot(start);
    assertEquals(24, timeslotRepo.currentSerialNumber(), "serial # first timeslot");
    assertEquals(400.0, genco.getMinQuantity(), 1e-6, "correct min quantity");
    List<WeatherForecastPrediction> wfs = new ArrayList<>();
    for (int i = 0; i < comp.getTimeslotsOpen(); i += 1) {
      WeatherForecastPrediction wfp =
          new WeatherForecastPrediction(i + 1, 0.0 - i, 0.0, 0.0, 0.0);
      wfs.add(wfp);
    }
    WeatherForecast wf = new WeatherForecast(24, wfs);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);
    assertEquals(400.0, genco.getAdjustedMinQty(25));
    assertEquals(412.0, genco.getAdjustedMinQty(26));
    assertEquals(424.0, genco.getAdjustedMinQty(27));
    assertEquals(436.0, genco.getAdjustedMinQty(28));
  }
}
