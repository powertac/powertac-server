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

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.Order;
import org.powertac.common.RandomSeed;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author jcollins
 */
public class CpGencoTest
{
  private BrokerProxy mockProxy;

  private TimeslotRepo timeslotRepo;

  private CpGenco genco;
  private Instant start;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;
  private TimeService timeService;

  /**
   *
   */
  @Before
  public void setUp () throws Exception
  {
    Competition comp = Competition.newInstance("Genco test").withTimeslotsOpen(4);
    Competition.setCurrent(comp);
    comp.withTimeslotsOpen(4);
    mockProxy = mock(BrokerProxy.class);
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(eq(CpGenco.class.getName()),
                                    anyInt(),
                                    anyString())).thenReturn(seed);
    timeslotRepo = new TimeslotRepo();
    genco = new CpGenco("Test");
    start = comp.getSimulationBaseTime().plus(TimeService.DAY);
    timeService = new TimeService();
    timeService.setCurrentTime(start);
    ReflectionTestUtils.setField(timeslotRepo, "timeService", timeService);
  }

  private void init ()
  {
    when(seed.nextLong()).thenReturn(1l);
    genco.init(mockProxy, 0, mockSeedRepo, timeslotRepo);
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#CpGenco(java.lang.String)}.
   */
  @Test
  public void testCpGenco ()
  {
    assertNotNull("created something", genco);
    assertEquals("correct name", "Test", genco.getUsername());
    assertEquals("correct price interval", 4.0, genco.getPriceInterval(), 1e-6);
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#init(org.powertac.common.interfaces.BrokerProxy, int, org.powertac.common.repo.RandomSeedRepo)}.
   */
  @Test
  public void testInit ()
  {
    init();
    verify(mockSeedRepo).getRandomSeed(eq(CpGenco.class.getName()),
                                       anyInt(), eq("bid"));
    double[] ca = genco.getCoefficientArray();
    assertNotNull("initialized array", ca);
    assertEquals("3 elements", 3, ca.length);
    assertEquals("correct 1st coeff", 0.007, ca[0], 1e-6);
    assertEquals("correct 1st coeff", 0.1, ca[1], 1e-6);
    assertEquals("correct 1st coeff", 16.0, ca[2], 1e-6);
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#withCoefficients(java.lang.String)}.
   */
  @Test
  public void testWithCoefficients ()
  {
    init();
    List<String> coefficients = Arrays.asList("1.0", "1.1", "1.2");
    genco.withCoefficients(coefficients);
    assertEquals("3-element list", 3, genco.getCoefficients().size());
    assertEquals("first element", "1.0", genco.getCoefficients().get(0));
    double[] ca = genco.getCoefficientArray();
    assertEquals("3 elements", 3, ca.length);
    assertEquals("correct 1st coeff", 1.0, ca[0], 1e-6);
    assertEquals("correct 1st coeff", 1.1, ca[1], 1e-6);
    assertEquals("correct 1st coeff", 1.2, ca[2], 1e-6);
  }

  // bogus coefficients should not change defaults
  @Test
  public void bogusCoefficients2 ()
  {
    init();
    List<String> coefficients = Arrays.asList("1.0", "3.0");
    genco.withCoefficients(coefficients);
    double[] ca = genco.getCoefficientArray();
    assertEquals("3 elements", 3, ca.length);
    assertEquals("correct 1st coeff", 0.007, ca[0], 1e-6);
    assertEquals("correct 1st coeff", 0.1, ca[1], 1e-6);
    assertEquals("correct 1st coeff", 16.0, ca[2], 1e-6);
  }

  // bogus coefficients should not change defaults
  @Test
  public void bogusCoefficients4 ()
  {
    init();
    List<String> coefficients = Arrays.asList("1.0", "3.0", "1.0", "3.0");
    genco.withCoefficients(coefficients);
    double[] ca = genco.getCoefficientArray();
    assertEquals("3 elements", 3, ca.length);
    assertEquals("correct 1st coeff", 0.007, ca[0], 1e-6);
    assertEquals("correct 1st coeff", 0.1, ca[1], 1e-6);
    assertEquals("correct 1st coeff", 16.0, ca[2], 1e-6);
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#withPSigma(double)}.
   */
  @Test
  public void testWithPSigma ()
  {
    init();
    assertEquals("correct initial", 0.1, genco.getPSigma(), 1e-6);
    genco.withPSigma(0.01);
    assertEquals("correct post", 0.01, genco.getPSigma(), 1e-6);
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#withQSigma(double)}.
   */
  @Test
  public void testWithQSigma ()
  {
    init();
    assertEquals("correct initial", 0.1, genco.getQSigma(), 1e-6);
    genco.withQSigma(0.01);
    assertEquals("correct post", 0.01, genco.getQSigma(), 1e-6);
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#withPriceInterval(double)}.
   */
  @Test
  public void testWithPriceInterval ()
  {
    init();
    assertEquals("correct initial", 4.0, genco.getPriceInterval(), 1e-6);
    genco.withPriceInterval(6.0);
    assertEquals("correct post", 6.0, genco.getPriceInterval(), 1e-6);
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#withMinQuantity(int)}.
   */
  @Test
  public void testWithMinQuantity ()
  {
    init();
    assertEquals("correct initial", 120.0, genco.getMinQuantity(), 1e-6);
    genco.withMinQuantity(150.0);
    assertEquals("correct post", 150.0, genco.getMinQuantity(), 1e-6);
  }

  // config test
  @Test
  public void configTest ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("genco.cpGenco.coefficients", "1.0, 2.0, 3.0");
    map.put("genco.cpGenco.pSigma", "0.22");
    init();
    Configuration conf = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(conf);
    configurator.configureSingleton(genco);
    assertEquals("3-element list", 3, genco.getCoefficients().size());
    assertEquals("correct 1st", "1.0", genco.getCoefficients().get(0));
    assertEquals("correct last", "3.0", genco.getCoefficients().get(2));
    assertEquals("correct pSigma", 0.22, genco.getPSigma(), 1e-6);
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#generateOrders(org.joda.time.Instant, java.util.List)}.
   */
  @SuppressWarnings("unused")
  @Test
  public void generateFixedOrders ()
  {
    init();
    genco.withMinQuantity(100.0);
    genco.withPriceInterval(10.0);
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
    Timeslot ts2 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR));
    Timeslot ts3 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 2));
    assertEquals("4 enabled timeslots", 4, timeslotRepo.enabledTimeslots().size());
    // 50 mwh already sold in ts2
    MarketPosition posn2 = new MarketPosition(genco, ts2, -50.0);
    genco.addMarketPosition(posn2, ts2.getSerialNumber());
    // generate orders and check
    genco.generateOrders(start, timeslotRepo.enabledTimeslots());
    assertEquals("72 orders", 72, orderList.size());
  }

  @SuppressWarnings("unused")
  @Test
  public void generateVarOrders ()
  {
    init();
    genco.withMinQuantity(100.0);
    genco.withPriceInterval(10.0);
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
    Timeslot ts2 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR));
    Timeslot ts3 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 2));
    assertEquals("4 enabled timeslots", 4, timeslotRepo.enabledTimeslots().size());
    // 50 mwh already sold in ts2
    MarketPosition posn2 = new MarketPosition(genco, ts2, -50.0);
    genco.addMarketPosition(posn2, ts2.getSerialNumber());
    // generate orders and check
    genco.generateOrders(start, timeslotRepo.enabledTimeslots());
    assertEquals("73 orders", 73, orderList.size());
  }
}
