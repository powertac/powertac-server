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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.RandomSeed;
import org.powertac.common.Order;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for the Genco broker type
 * @author John Collins
 */
public class GencoTests
{
  private BrokerProxy mockProxy;

  private TimeslotRepo timeslotRepo;

  private Genco genco;
  private Instant start;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;
  private ServerConfiguration serverConfig;
  private Configurator config;
  private TimeService timeService;
  
  @BeforeEach
  public void setUp () throws Exception
  {
    Competition comp = Competition.newInstance("Genco test").withTimeslotsOpen(4);
    Competition.setCurrent(comp);
    mockProxy = mock(BrokerProxy.class);
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(eq(Genco.class.getName()),
                                    anyLong(),
                                    anyString())).thenReturn(seed);
    timeslotRepo = new TimeslotRepo();
    genco = new Genco("Test");
    genco.init(mockProxy, 0, mockSeedRepo);
    start = comp.getSimulationBaseTime().plusMillis(TimeService.DAY);
    timeService = new TimeService();
    timeService.setCurrentTime(start);
    ReflectionTestUtils.setField(timeslotRepo, "timeService", timeService);

    // Set up serverProperties mock
    serverConfig = mock(ServerConfiguration.class);
    config = new Configurator();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(serverConfig).configureMe(any());
  }

  @Test
  public void testGenco ()
  {
    assertNotNull(genco, "created something");
    assertEquals(genco.getUsername(), "Test", "correct name");
  }

  @Test
  public void testInit()
  {
    // it has already had init() called, should have requested a armaSeed
    verify(mockSeedRepo).getRandomSeed(eq(Genco.class.getName()),
                                       anyLong(), eq("update"));
  }

  @Test
  public void testUpdateModel ()
  {
    when(seed.nextDouble()).thenReturn(0.5);
    assertEquals(100.0, genco.getCurrentCapacity(), 1e-6, "correct initial capacity");
    assertTrue(genco.isInOperation(), "initially in operation");
    genco.updateModel(start);
    assertEquals(100.0, genco.getCurrentCapacity(), 1e-6, "correct updated capacity");
    assertTrue(genco.isInOperation(), "still in operation");
  }

  @SuppressWarnings("unused")
  @Test
  public void testGenerateOrders ()
  {
    // set up the genco
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
    assertEquals(4, orderList.size(), "four orders");
    Order first = orderList.get(0);
    assertEquals(ts2.getSerialNumber(), first.getTimeslotIndex(), "first order for ts2");
    assertEquals(1.0, first.getLimitPrice(), 1e-6, "first order price");
    assertEquals(-50.0, first.getMWh(), 1e-6, "first order for 50 mwh");
    Order second = orderList.get(1);
    assertEquals(ts3.getSerialNumber(), second.getTimeslotIndex(), "second order for ts3");
    assertEquals(1.0, second.getLimitPrice(), 1e-6, "second order price");
    assertEquals(-100.0, second.getMWh(), 1e-6, "second order for 100 mwh");
  }

  // set commitment leadtime to a larger number and make sure ordering
  // behavior is correct
  @Test
  public void testGenerateOrders2 ()
  {
    // set up the genco with commitment leadtime=3
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("genco.genco.commitmentLeadtime", "3");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    serverConfig.configureMe(genco);
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
    Timeslot ts0 = timeslotRepo.makeTimeslot(start);
    assertEquals(24, ts0.getSerialNumber(), "first ts has sn=24");
    timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR));
    timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR * 2));
    timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR * 3));
    timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR * 4));
    assertEquals(4, timeslotRepo.enabledTimeslots().size(), "4 enabled timeslots");

    // generate orders and check
    genco.generateOrders(start, timeslotRepo.enabledTimeslots());
    assertEquals(2, orderList.size(), "two orders");
    Order first = orderList.get(0);
    assertEquals(27, first.getTimeslotIndex(), "first order for ts3");
    assertEquals(1.0, first.getLimitPrice(), 1e-6, "first order price");
    assertEquals(-100.0, first.getMWh(), 1e-6, "first order for 100 mwh");
    Order second = orderList.get(1);
    assertEquals(28, second.getTimeslotIndex(), "second order for ts4");
    assertEquals(1.0, second.getLimitPrice(), 1e-6, "second order price");
    assertEquals(-100.0, second.getMWh(), 1e-6, "second order for 100 mwh");
  }

  // set commitment leadtime & market position and make sure ordering
  // behavior is correct
  @Test
  public void testGenerateOrders3 ()
  {
    // set up the genco with commitment leadtime=3
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("genco.genco.commitmentLeadtime", "3");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    serverConfig.configureMe(genco);
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
    Timeslot ts0 = timeslotRepo.makeTimeslot(start);
    assertEquals(24, ts0.getSerialNumber(), "first ts has sn=24");
    //timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR));
    //timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR * 2));
    //timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR * 3));
    //timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR * 4));
    assertEquals(4, timeslotRepo.enabledTimeslots().size(), "4 enabled timeslots");

    // 50 mwh already sold in ts2
    Timeslot ts2 = timeslotRepo.findBySerialNumber(26);
    MarketPosition posn2 = new MarketPosition(genco, ts2, -50.0);
    genco.addMarketPosition(posn2, ts2.getSerialNumber());

    // generate orders and check
    genco.generateOrders(start, timeslotRepo.enabledTimeslots());
    assertEquals(3, orderList.size(), "three orders");
    Order order = orderList.get(0);
    assertEquals(26, order.getTimeslotIndex(), "first order for ts2");
    assertEquals(1.0, order.getLimitPrice(), 1e-6, "first order price");
    assertEquals(-50.0, order.getMWh(), 1e-6, "first order for 50 mwh");
    order = orderList.get(1);
    assertEquals(27, order.getTimeslotIndex(), "second order for ts3");
    assertEquals(1.0, order.getLimitPrice(), 1e-6, "second order price");
    assertEquals(-100.0, order.getMWh(), 1e-6, "second order for 100 mwh");
    order = orderList.get(2);
    assertEquals(28, order.getTimeslotIndex(), "third order for ts4");
    assertEquals(1.0, order.getLimitPrice(), 1e-6, "third order price");
    assertEquals(-100.0, order.getMWh(), 1e-6, "third order for 100 mwh");
  }
}
