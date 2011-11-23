/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.genco;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.PluginConfig;
import org.powertac.common.RandomSeed;
import org.powertac.common.Order;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for the Genco broker type
 * @author John Collins
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"file:src/test/resources/test-config.xml"})
public class GencoTests
{
  //@Autowired
  private BrokerProxy mockProxy;
  
  //@Autowired
  private TimeslotRepo timeslotRepo;

  private Genco genco;
  private Instant start;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("src/test/resources/log.config");
  }
  
  @Before
  public void setUp () throws Exception
  {
    Competition.newInstance("Genco test");
    mockProxy = mock(BrokerProxy.class);
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(eq(Genco.class.getName()),
                                    anyInt(),
                                    anyString())).thenReturn(seed);
    timeslotRepo = new TimeslotRepo();
    genco = new Genco("Test");
    genco.init(mockProxy, mockSeedRepo);
    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
  }

  @Test
  public void testGenco ()
  {
    assertNotNull("created something", genco);
    assertEquals("correct name", "Test", genco.getUsername());
  }
  
  @Test
  public void testInit()
  {
    // it has already had init() called, should have requested a seed
    verify(mockSeedRepo).getRandomSeed(eq(Genco.class.getName()),
                                       anyInt(), eq("update"));
  }

  @Test
  public void testUpdateModel ()
  {
    when(seed.nextDouble()).thenReturn(0.5);
    PluginConfig config = new PluginConfig("Genco", "");
    genco.configure(config); // all defaults
    assertEquals("correct initial capacity",
                 100.0, genco.getCurrentCapacity(), 1e-6);
    assertTrue("initially in operation", genco.isInOperation());
    genco.updateModel(start);
    assertEquals("correct updated capacity",
                 100.0, genco.getCurrentCapacity(), 1e-6);
    assertTrue("still in operation", genco.isInOperation());
  }

  @Test
  public void testGenerateOrders ()
  {
    // set up the genco
    PluginConfig config = new PluginConfig("Genco", "");
    genco.configure(config); // all defaults
    // capture orders
    final ArrayList<Order> orderList = new ArrayList<Order>(); 
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        orderList.add((Order)args[1]);
        return null;
      }
    }).when(mockProxy).routeMessage(isA(Broker.class), isA(Order.class));
    // set up some timeslots
    Timeslot ts1 = timeslotRepo.makeTimeslot(start);
    ts1.disable();
    Timeslot ts2 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR));
    Timeslot ts3 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 2));
    assertEquals("2 enabled timeslots", 2, timeslotRepo.enabledTimeslots().size());
    // 50 mwh already sold in ts2
    MarketPosition posn2 = new MarketPosition(genco, ts2, -50.0);
    genco.addMarketPosition(posn2, ts2);
    // generate orders and check
    genco.generateOrders(start, timeslotRepo.enabledTimeslots());
    assertEquals("two orders", 2, orderList.size());
    Order first = orderList.get(0);
    assertEquals("first order for ts2", ts2, first.getTimeslot());
    assertEquals("first order price", 1.0, first.getLimitPrice(), 1e-6);
    assertEquals("first order for 50 mwh", -50.0, first.getMWh(), 1e-6);
    Order second = orderList.get(1);
    assertEquals("second order for ts3", ts3, second.getTimeslot());
    assertEquals("second order price", 1.0, second.getLimitPrice(), 1e-6);
    assertEquals("second order for 100 mwh", -100.0, second.getMWh(), 1e-6);
  }

  // set commitment leadtime to a larger number and make sure ordering
  // behavior is correct
  @Test
  public void testGenerateOrders2 ()
  {
    // set up the genco with commitment leadtime=3
    PluginConfig config = new PluginConfig("Genco", "")
      .addConfiguration("commitmentLeadtime", "3");
    genco.configure(config); // all defaults
    // capture orders
    final ArrayList<Order> orderList = new ArrayList<Order>(); 
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        orderList.add((Order)args[1]);
        return null;
      }
    }).when(mockProxy).routeMessage(isA(Broker.class), isA(Order.class));
    // set up some timeslots
    Timeslot ts0 = timeslotRepo.makeTimeslot(start);
    ts0.disable();
    assertEquals("first ts has sn=0", 0, ts0.getSerialNumber());
    timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR));
    timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 2));
    timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 3));
    timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 4));
    assertEquals("4 enabled timeslots", 4, timeslotRepo.enabledTimeslots().size());

    // generate orders and check
    genco.generateOrders(start, timeslotRepo.enabledTimeslots());
    assertEquals("two orders", 2, orderList.size());
    Order first = orderList.get(0);
    assertEquals("first order for ts3", 3, first.getTimeslot().getSerialNumber());
    assertEquals("first order price", 1.0, first.getLimitPrice(), 1e-6);
    assertEquals("first order for 100 mwh", -100.0, first.getMWh(), 1e-6);
    Order second = orderList.get(1);
    assertEquals("second order for ts4", 4, second.getTimeslot().getSerialNumber());
    assertEquals("second order price", 1.0, second.getLimitPrice(), 1e-6);
    assertEquals("second order for 100 mwh", -100.0, second.getMWh(), 1e-6);
  }

  // set commitment leadtime & market position and make sure ordering
  // behavior is correct
  @Test
  public void testGenerateOrders3 ()
  {
    // set up the genco with commitment leadtime=3
    PluginConfig config = new PluginConfig("Genco", "")
      .addConfiguration("commitmentLeadtime", "3");
    genco.configure(config); // all defaults
    // capture orders
    final ArrayList<Order> orderList = new ArrayList<Order>(); 
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        orderList.add((Order)args[1]);
        return null;
      }
    }).when(mockProxy).routeMessage(isA(Broker.class), isA(Order.class));
    // set up some timeslots
    Timeslot ts0 = timeslotRepo.makeTimeslot(start);
    ts0.disable();
    assertEquals("first ts has sn=0", 0, ts0.getSerialNumber());
    timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR));
    timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 2));
    timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 3));
    timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 4));
    assertEquals("4 enabled timeslots", 4, timeslotRepo.enabledTimeslots().size());

    // 50 mwh already sold in ts2
    Timeslot ts2 = timeslotRepo.findBySerialNumber(2);
    MarketPosition posn2 = new MarketPosition(genco, ts2, -50.0);
    genco.addMarketPosition(posn2, ts2);

    // generate orders and check
    genco.generateOrders(start, timeslotRepo.enabledTimeslots());
    assertEquals("two orders", 3, orderList.size());
    Order order = orderList.get(0);
    assertEquals("second order for ts2", 2, order.getTimeslot().getSerialNumber());
    assertEquals("second order price", 1.0, order.getLimitPrice(), 1e-6);
    assertEquals("second order for 50 mwh", -50.0, order.getMWh(), 1e-6);
    order = orderList.get(1);
    assertEquals("second order for ts3", 3, order.getTimeslot().getSerialNumber());
    assertEquals("second order price", 1.0, order.getLimitPrice(), 1e-6);
    assertEquals("second order for 100 mwh", -100.0, order.getMWh(), 1e-6);
    order = orderList.get(2);
    assertEquals("third order for ts4", 4, order.getTimeslot().getSerialNumber());
    assertEquals("third order price", 1.0, order.getLimitPrice(), 1e-6);
    assertEquals("third order for 100 mwh", -100.0, order.getMWh(), 1e-6);
  }
}
