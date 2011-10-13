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
package org.powertac.du;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.Shout;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Auctioneer;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.du.DefaultBrokerInitializationService;
import org.powertac.du.DefaultBrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for a DefaultBrokerService. See AuctionServiceTests for a detailed
 * example. We use the Spring test runner for integration testing. Mocks
 * may be created in the test code, or may be instantiated by Spring and
 * autowired into the test. Test component configuration is in test-config.xml.
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/test-config.xml"})
public class DefaultBrokerServiceTests
{
  @Autowired
  private TimeService timeService;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private BrokerProxy mockProxy;
  
  private TariffMarket mockMarket; // not autowired
  
  private DefaultBrokerInitializationService initializer;
  private DefaultBrokerService service;
  private CustomerInfo customer1;
  private CustomerInfo customer2;
  private Competition competition;
  private Instant start;

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    // clean up from previous tests
    pluginConfigRepo.recycle();
    timeslotRepo.recycle();
    reset(mockProxy);
    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(start);
    customer1 = new CustomerInfo("town", 1000);
    customer2 = new CustomerInfo("village", 200);
    
    service = new DefaultBrokerService();
    initializer = new DefaultBrokerInitializationService();
    ReflectionTestUtils.setField(initializer,
                                 "defaultBrokerService",
                                 service);
    ReflectionTestUtils.setField(initializer,
                                 "brokerRepo",
                                 brokerRepo);
    ReflectionTestUtils.setField(initializer,
                                 "pluginConfigRepo",
                                 pluginConfigRepo);
    mockMarket = mock(TariffMarket.class);
    ReflectionTestUtils.setField(service, "tariffMarketService", mockMarket);
    ReflectionTestUtils.setField(service, "brokerProxyService", mockProxy);
    ReflectionTestUtils.setField(service, "timeslotRepo", timeslotRepo);

    
    competition = Competition.newInstance("broker-test");
  }

  private Broker init ()
  {
    initializer.setDefaults();
    initializer.initialize(competition, new ArrayList<String>());
    Broker face = brokerRepo.findByUsername("default broker");
    return face;
  }

  @Test
  public void testService ()
  {
    Broker face = init();
    assertNotNull("found face", face);
    assertEquals("correct face", face, service.getFace());
  }
  
  @SuppressWarnings("rawtypes")
  @Test
  public void testDefaultTariffPublication ()
  {
    final ArrayList<TariffSpecification> specs = new ArrayList<TariffSpecification>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        specs.add((TariffSpecification)args[0]);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));
    
    Broker face = init();

    // should now have two tariff specs, one for production and
    // one for consumption
    boolean foundProduction = false;
    boolean foundConsumption = false;
    
    assertEquals("two default tariffs", 2, specs.size());
    for (TariffSpecification spec : specs) {
      if (spec.getPowerType() == PowerType.CONSUMPTION) {
        foundConsumption = true;
        assertEquals("correct issuer", face, spec.getBroker());
        List<Rate> rates = spec.getRates();
        assertEquals("just one rate", 1, rates.size());
        assertTrue("fixed rate", rates.get(0).isFixed());
        assertEquals("correct rate", 0.5, rates.get(0).getValue(), 1e-6);
      }
      else if (spec.getPowerType() == PowerType.PRODUCTION) {
        foundProduction = true;
        assertEquals("correct issuer", face, spec.getBroker());
        List<Rate> rates = spec.getRates();
        assertEquals("just one rate", 1, rates.size());
        assertTrue("fixed rate", rates.get(0).isFixed());
        assertEquals("correct rate", -0.02, rates.get(0).getValue(), 1e-6);
      }
    }
    assertTrue("found a consumption tariff", foundConsumption);
    assertTrue("found a production tariff", foundProduction);
  }

  // incoming messages drive all activity. These include TariffTransaction,
  // MarketPosition, and TimeslotUpdate.
  @SuppressWarnings("rawtypes")
  @Test
  public void testReceiveSignupMessage ()
  {
    final HashMap<PowerType, TariffSpecification> specs = 
      new HashMap<PowerType, TariffSpecification>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        TariffSpecification spec =(TariffSpecification)args[0];
        specs.put(spec.getPowerType(), spec);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));

    Broker face = init();
    HashMap<String, Integer> customerCounts = service.getCustomerCounts();
    assertEquals("no customers yet", 0, customerCounts.size());
    
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.SIGNUP, 
                                              specs.get(PowerType.CONSUMPTION),
                                              customer1, 
                                              customer1.getPopulation(),
                                              0.0, 4.2));
    // now one customer, population=1000
    customerCounts = service.getCustomerCounts();
    assertEquals("one customer", 1, customerCounts.size());
    int count = customerCounts.get(customer1.getName() + PowerType.CONSUMPTION);
    assertEquals("1000 individuals", 1000, count);
  }

  @Test
  public void testReceiveWithdrawMessage ()
  {
    final HashMap<PowerType, TariffSpecification> specs = 
      new HashMap<PowerType, TariffSpecification>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        TariffSpecification spec =(TariffSpecification)args[0];
        specs.put(spec.getPowerType(), spec);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));

    Broker face = init();
    HashMap<String, Integer> customerCounts = service.getCustomerCounts();
    assertEquals("no customers yet", 0, customerCounts.size());
    
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.SIGNUP, 
                                              specs.get(PowerType.CONSUMPTION),
                                              customer1, 
                                              customer1.getPopulation(),
                                              0.0, 4.2));
    // now one customer, population=1000
    customerCounts = service.getCustomerCounts();
    assertEquals("one customer", 1, customerCounts.size());
    int count = customerCounts.get(customer1.getName() + PowerType.CONSUMPTION);
    assertEquals("1000 individuals", 1000, count);
    
    // add another customer, then withdraw some from the first
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.SIGNUP, 
                                              specs.get(PowerType.PRODUCTION),
                                              customer2, 
                                              42, // population
                                              0.0, 4.2));
    customerCounts = service.getCustomerCounts();
    assertEquals("two customers", 2, customerCounts.size());
    count = customerCounts.get(customer1.getName() + PowerType.CONSUMPTION);
    assertEquals("still 1000 individuals", 1000, count);
    count = customerCounts.get(customer2.getName() + PowerType.PRODUCTION);
    assertEquals("42 individuals", 42, count);
    
    // now withdraw some from customer1
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.WITHDRAW, 
                                              specs.get(PowerType.CONSUMPTION),
                                              customer1, 
                                              400,
                                              0.0, 4.2));
    customerCounts = service.getCustomerCounts();
    assertEquals("still two customers", 2, customerCounts.size());
    count = customerCounts.get(customer1.getName() + PowerType.CONSUMPTION);
    assertEquals("600 individuals remain", 600, count);
    count = customerCounts.get(customer2.getName() + PowerType.PRODUCTION);
    assertEquals("still 42 individuals", 42, count);
  }

  @Test
  public void testReceiveConsumeMessage ()
  {
    final HashMap<PowerType, TariffSpecification> specs = 
      new HashMap<PowerType, TariffSpecification>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        TariffSpecification spec =(TariffSpecification)args[0];
        specs.put(spec.getPowerType(), spec);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));

    Broker face = init();
    HashMap<String, Integer> customerCounts = service.getCustomerCounts();
    assertEquals("no customers yet", 0, customerCounts.size());
    TariffSpecification spec = specs.get(PowerType.CONSUMPTION);
    
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.SIGNUP, 
                                              spec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              0.0, 4.2));
    // now one customer, population=1000. Consume some power.
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.CONSUME, 
                                              spec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              500.0, 4.2));
    // check usage in current timeslot
    double usage = service.getUsageForCustomer(customer1, spec, 0);
    assertEquals("500 kwh", 500.0, usage, 1e-6);
    // check usage in next timeslot
    usage = service.getUsageForCustomer(customer1, spec, 1);
    assertEquals("no usage", 0.0, usage, 1e-6);
    
    // move the clock ahead and use some more power
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.CONSUME, 
                                              spec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              450.0, 4.2));
    usage = service.getUsageForCustomer(customer1, spec, 0);
    assertEquals("500 kwh", 500.0, usage, 1e-6);
    // check usage in next two timeslots
    usage = service.getUsageForCustomer(customer1, spec, 1);
    assertEquals("no usage", 450.0, usage, 1e-6);
    usage = service.getUsageForCustomer(customer1, spec, 2);
    assertEquals("no usage", 0.0, usage, 1e-6);
    
  }

  @Test
  public void testShoutTS0 ()
  {
    Broker face = init();
    TimeslotUpdate tsu = createTimeslots();
    Timeslot current = timeslotRepo.currentTimeslot(); 
    assertEquals("current timeslot has serial 0", 0, current.getSerialNumber());
    final ArrayList<Shout> shoutList = new ArrayList<Shout>(); 
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        shoutList.add((Shout)args[0]);
        return null;
      }
    }).when(mockProxy).routeMessage(isA(Shout.class));
    
    // activate the trading function by sending a timeslot update msg
    face.receiveMessage(tsu);
    
    assertEquals("23 shouts", 23, shoutList.size());
    Shout firstShout = shoutList.get(0);
    assertNotNull("first shout not null", firstShout);
    assertEquals("bid flag", Shout.OrderType.BUY, firstShout.getOrderType());
    assertEquals("correct mwh", 1.0, firstShout.getMWh(), 1e-6);
    assertEquals("correct price", 100.0, firstShout.getLimitPrice(), 1e-6);
    Shout lastShout = shoutList.get(22);
    assertNotNull("last shout not null", lastShout);
    assertEquals("bid flag", Shout.OrderType.BUY, lastShout.getOrderType());
    assertEquals("correct mwh", 1.0, lastShout.getMWh(), 1e-6);
    assertEquals("correct price", 100.0, lastShout.getLimitPrice(), 1e-6);
  }
  
  // in timeslot 3, we should have 3 days of records on which to base the last
  // three bids
  @Test
  public void testShoutTS4 ()
  {
    // collect the tariff specs
    final HashMap<PowerType, TariffSpecification> specs = 
      new HashMap<PowerType, TariffSpecification>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        TariffSpecification spec =(TariffSpecification)args[0];
        specs.put(spec.getPowerType(), spec);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));
    
    // collect shouts when they are submitted
    final ArrayList<Shout> shoutList = new ArrayList<Shout>(); 
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        shoutList.add((Shout)args[0]);
        return null;
      }
    }).when(mockProxy).routeMessage(isA(Shout.class));

    // initialize the default broker
    Broker face = init();
    TimeslotUpdate tsu = createTimeslots(); // 0-22 enabled
    assertFalse("ts0 disabled", timeslotRepo.findBySerialNumber(0).isEnabled());
    assertTrue("ts1 enabled", timeslotRepo.findBySerialNumber(1).isEnabled());
    assertTrue("ts23 enabled", timeslotRepo.findBySerialNumber(23).isEnabled());
    assertNull("ts24 null", timeslotRepo.findBySerialNumber(24));
    face.receiveMessage(tsu); // timeslot -1

    // ---- timeslot 0 ----
    // now we need some subscriptions
    TariffSpecification cspec = specs.get(PowerType.CONSUMPTION);
    TariffSpecification pspec = specs.get(PowerType.PRODUCTION);
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.SIGNUP, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              0.0, 4.2));
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.SIGNUP, 
                                              pspec,
                                              customer2, 
                                              customer2.getPopulation(),
                                              0.0, 4.2));
    
    // Then we use power for four cycles
    // usage = 500 in ts0
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.CONSUME, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              500.0, 4.2));
    // market clears for ts 1-23
    assertEquals("23 shouts", 23, shoutList.size());
    shoutList.clear();
    // TODO - add market positions

    tsu = nextTimeslot(); // end of ts0: 1 disabled, 24 enabled
    assertFalse("ts0 disabled", timeslotRepo.findBySerialNumber(0).isEnabled());
    assertFalse("ts1 disabled", timeslotRepo.findBySerialNumber(1).isEnabled());
    assertTrue("ts2 enabled", timeslotRepo.findBySerialNumber(2).isEnabled());
    assertTrue("ts24 enabled", timeslotRepo.findBySerialNumber(24).isEnabled());
    assertNull("ts25 null", timeslotRepo.findBySerialNumber(25));
    
    assertEquals("correct usage index 0", 500.0,
                 service.getUsageForCustomer(customer1, cspec, 0), 1e-6);
    assertEquals("correct usage index 1", 0.0,
                 service.getUsageForCustomer(customer1, cspec, 1), 1e-6);
    
    face.receiveMessage(tsu);
    
    // enable next timeslot, usage = 420 in ts1
    timeService.setCurrentTime(timeslotRepo.currentTimeslot().getEndInstant());
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.CONSUME, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              450.0, 4.2));
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.PRODUCE, 
                                              pspec,
                                              customer2, 
                                              customer2.getPopulation(),
                                              -30.0, -0.15));
    // market clears in ts1
    assertEquals("23 shouts ts1", 23, shoutList.size());
    Shout shout = shoutList.get(0);
    assertNotNull("first shout not null", shout);
    assertEquals("ts2 is first", 
                 timeslotRepo.findBySerialNumber(2),
                 shout.getTimeslot());
    assertEquals("bid flag", Shout.OrderType.BUY, shout.getOrderType());
    assertEquals("correct mwh", 1.0, shout.getMWh(), 1e-6);
    assertEquals("correct price", 100.0, shout.getLimitPrice(), 1e-6);
    shout = shoutList.get(22);
    assertNotNull("last shout not null", shout);
    assertEquals("ts24 is last", 
                 timeslotRepo.findBySerialNumber(24),
                 shout.getTimeslot());
    assertEquals("bid flag", Shout.OrderType.BUY, shout.getOrderType());
    assertEquals("correct mwh", 1.0, shout.getMWh(), 1e-6);
    assertEquals("correct price", 100.0, shout.getLimitPrice(), 1e-6);
    shoutList.clear();

    tsu = nextTimeslot();
    face.receiveMessage(tsu);
    
    // usage 510 in ts2
    timeService.setCurrentTime(timeslotRepo.currentTimeslot().getEndInstant());
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.CONSUME, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              550.0, 4.2));
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.PRODUCE, 
                                              pspec,
                                              customer2, 
                                              customer2.getPopulation(),
                                              -40.0, -0.15));
    // market clears ts2
    assertEquals("23 shouts ts2", 23, shoutList.size());
    shout = shoutList.get(0);
    assertNotNull("first shout not null", shout);
    assertEquals("ts3 is first", 
                 timeslotRepo.findBySerialNumber(3),
                 shout.getTimeslot());
    assertEquals("bid flag", Shout.OrderType.BUY, shout.getOrderType());
    assertEquals("correct mwh", 0.42, shout.getMWh(), 1e-6);
    assertEquals("correct price", 100.0, shout.getLimitPrice(), 1e-6);
    shout = shoutList.get(20);
    assertEquals("correct mwh", 0.42, shout.getMWh(), 1e-6);
    shout = shoutList.get(21);
    assertEquals("correct mwh", 0.5, shout.getMWh(), 1e-6);
    shout = shoutList.get(22);
    assertNotNull("last shout not null", shout);
    assertEquals("ts25 is last", 
                 timeslotRepo.findBySerialNumber(25),
                 shout.getTimeslot());
    assertEquals("bid flag", Shout.OrderType.BUY, shout.getOrderType());
    assertEquals("correct mwh", 0.42, shout.getMWh(), 1e-6);
    assertEquals("correct price", 100.0, shout.getLimitPrice(), 1e-6);
    shoutList.clear();
  }
  
  // set up some timeslots
  private TimeslotUpdate createTimeslots()
  {
    ArrayList<Timeslot> oldTs = new ArrayList<Timeslot>();
    ArrayList<Timeslot> newTs = new ArrayList<Timeslot>();
    Instant now = timeService.getCurrentTime();
    Timeslot ts = timeslotRepo.makeTimeslot(now, now.plus(TimeService.HOUR));
    ts.disable();
    oldTs.add(ts);
    for (int i = 1; i < 24; i++) {
      ts = timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR * i),
                                              now.plus(TimeService.HOUR * (i + 1)));
      newTs.add(ts);
    }
    return new TimeslotUpdate(newTs, oldTs);
  }

  private TimeslotUpdate nextTimeslot ()
  {
    Timeslot current = timeslotRepo.currentTimeslot();
    Timeslot oldTs = timeslotRepo.findBySerialNumber(current.getSerialNumber() + 1);
    oldTs.disable();
    Instant start = oldTs.getStartInstant().plus(TimeService.HOUR * 23);
    Timeslot newTs = timeslotRepo.makeTimeslot(start, start.plus(TimeService.HOUR));
    newTs.enable();
    return new TimeslotUpdate(newTs, oldTs);
  }
}
