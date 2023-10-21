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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;

import org.joda.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.powertac.common.Broker;
import org.powertac.common.CashPosition;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.Order;
import org.powertac.common.RegulationRate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.msg.TimeslotComplete;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for a DefaultBrokerService. See AuctionServiceTests for a detailed
 * example. We use the Spring test runner for integration testing. Mocks
 * may be created in the test code, or may be instantiated by Spring and
 * autowired into the test. Test component configuration is in test-config.xml.
 * @author John Collins
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class DefaultBrokerServiceTests
{
  @Autowired
  private TimeService timeService;

  @Autowired 
  private CompetitionControl mockCompetitionControl;

  @Autowired
  private ServerConfiguration serverPropertiesService;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private BrokerProxy mockProxy;

  private TariffMarket mockMarket; // not autowired
  private RandomSeedRepo mockRandom; // not autowired

  private DefaultBrokerService service;
  private CustomerInfo customer1;
  private CustomerInfo customer2;
  private Competition competition;
  private Configurator config;
  private Instant start;

  @SuppressWarnings("rawtypes")
  @BeforeEach
  public void setUp () throws Exception
  {
    // clean up from previous tests
    timeslotRepo.recycle();
    reset(mockProxy);
    reset(mockCompetitionControl);
    competition = Competition.newInstance("db-test");
    start = competition.getSimulationBaseTime();
    timeService.setCurrentTime(start);
    customerRepo.recycle();
    customer1 = new CustomerInfo("town", 1000);
    customerRepo.add(customer1);
    customer2 = new CustomerInfo("village", 200);
    customerRepo.add(customer2);

    service = new DefaultBrokerService();
    mockMarket = mock(TariffMarket.class);
    mockRandom = mock(RandomSeedRepo.class);
    when(mockRandom.getRandomSeed(anyString(), anyLong(), anyString()))
        .thenReturn(new MockRandomSeed("broker", 0, ""));
    ReflectionTestUtils.setField(service,
                                 "competitionControlService", 
                                 mockCompetitionControl);
    ReflectionTestUtils.setField(service, "tariffMarketService", mockMarket);
    ReflectionTestUtils.setField(service, "brokerProxyService", mockProxy);
    ReflectionTestUtils.setField(service, "timeslotRepo", timeslotRepo);
    ReflectionTestUtils.setField(service, "brokerRepo", brokerRepo);
    ReflectionTestUtils.setField(service, "customerRepo", customerRepo);
    ReflectionTestUtils.setField(service, "serverPropertiesService",
                                 serverPropertiesService);
    ReflectionTestUtils.setField(service, "randomSeedRepo", mockRandom);

    // Set up serverProperties mock
    config = new Configurator();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(serverPropertiesService).configureMe(any());
    
    createTimeslots();
    
    // set bootstrap mode - otherwise we need a bootstrap record
    when(mockCompetitionControl.isBootstrapMode())
      .thenReturn(true);
  }

  private Broker init ()
  {
    List<String> completedInits = new ArrayList<String>();
    completedInits.add("TariffMarket");
    String answer = service.initialize(competition, completedInits);
    assertEquals(answer, "DefaultBroker", "correct response");
    Broker face = brokerRepo.findByUsername("default broker");
    face.setEnabled(true);
    return face;
  }

  @Test
  public void testBogusInit ()
  {
    List<String> completedInits = new ArrayList<String>();
    String answer = service.initialize(competition, completedInits);
    assertNull(answer, "cannot proceed");
  }
  
  @Test
  public void testService ()
  {
    Broker face = init();
    assertNotNull(face, "found face");
    assertEquals(face, service.getFace(), "correct face");
    assertTrue(face.isEnabled(), "face is enabled");
    assertTrue(service.isBootstrapMode(), "bootstrap mode");
  }
  
  @Test
  public void testInit ()
  {
    Broker face = init();
    assertNotNull(face, "found face");
    assertEquals(-1.0, service.getConsumptionRate(), 1e-6, "correct consumption rate");
    assertEquals(0.01, service.getProductionRate(), 1e-6, "correct production rate");
    assertEquals(500.0, service.getInitialBidKWh(), 1e-6, "correct initial kwh");
  }
  
  @Test
  public void testConfig ()
  {
    List<String> completedInits = new ArrayList<String>();
    completedInits.add("TariffMarket");
    
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("du.defaultBrokerService.consumptionRate", "-0.50");
    map.put("du.defaultBrokerService.productionRate", "0.02");
    map.put("du.defaultBrokerService.initialBidKWh", "1000.0");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    service.initialize(competition, completedInits);
    assertEquals(-0.5, service.getConsumptionRate(), 1e-6, "correct consumption rate");
    assertEquals(0.02, service.getProductionRate(), 1e-6, "correct production rate");
    assertEquals(1000.0, service.getInitialBidKWh(), 1e-6, "correct initial kwh");
  }
  
  @SuppressWarnings("rawtypes")
  @Test
  public void testDefaultTariffPublication ()
  {
    final ArrayList<TariffSpecification> specs = new ArrayList<TariffSpecification>();
    doAnswer(new Answer() {
      @Override
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
    boolean foundStorage = false;

    assertEquals(3, specs.size(), "three default tariffs");
    for (TariffSpecification spec : specs) {
      if (spec.getPowerType() == PowerType.CONSUMPTION) {
        foundConsumption = true;
        assertEquals(face, spec.getBroker(), "correct issuer");
        List<Rate> rates = spec.getRates();
        assertEquals(1, rates.size(), "just one rate");
        assertTrue(rates.get(0).isFixed(), "fixed rate");
        assertEquals(-1.0, rates.get(0).getValue(), 1e-6, "correct rate");
      }
      else if (spec.getPowerType() == PowerType.PRODUCTION) {
        foundProduction = true;
        assertEquals(face, spec.getBroker(), "correct issuer");
        List<Rate> rates = spec.getRates();
        assertEquals(1, rates.size(), "just one rate");
        assertTrue(rates.get(0).isFixed(), "fixed rate");
        assertEquals(0.01, rates.get(0).getValue(), 1e-6, "correct rate");
      }
      else if (spec.getPowerType() == PowerType.STORAGE) {
        foundStorage = true;
        assertEquals(face, spec.getBroker(), "correct issuer");
        List<RegulationRate> rrates = spec.getRegulationRates();
        assertEquals(0, rrates.size(), "no regulation rates");
        List<Rate> rates = spec.getRates();
        assertEquals(1, rates.size(), "one normal rate");
      }
    }
    assertTrue(foundConsumption, "found a consumption tariff");
    assertTrue(foundProduction, "found a production tariff");
    assertTrue(foundStorage, "found a storage tariff");
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
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        TariffSpecification spec =(TariffSpecification)args[0];
        specs.put(spec.getPowerType(), spec);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));

    Broker face = init();
    HashMap<String, Integer> customerCounts = service.getCustomerCounts();
    assertEquals(0, customerCounts.size(), "no customers yet");
    
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.SIGNUP, 
                                              specs.get(PowerType.CONSUMPTION),
                                              customer1, 
                                              customer1.getPopulation(),
                                              0.0, 4.2, false));
    // now one customer, population=1000
    customerCounts = service.getCustomerCounts();
    assertEquals(1, customerCounts.size(), "one customer");
    int count = customerCounts.get(customer1.getName() + PowerType.CONSUMPTION);
    assertEquals(1000, count, "1000 individuals");
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testReceiveWithdrawMessage ()
  {
    final HashMap<PowerType, TariffSpecification> specs = 
      new HashMap<PowerType, TariffSpecification>();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        TariffSpecification spec =(TariffSpecification)args[0];
        specs.put(spec.getPowerType(), spec);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));

    Broker face = init();
    HashMap<String, Integer> customerCounts = service.getCustomerCounts();
    assertEquals(0, customerCounts.size(), "no customers yet");
    
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.SIGNUP, 
                                              specs.get(PowerType.CONSUMPTION),
                                              customer1, 
                                              customer1.getPopulation(),
                                              0.0, 4.2, false));
    // now one customer, population=1000
    customerCounts = service.getCustomerCounts();
    assertEquals(1, customerCounts.size(), "one customer");
    int count = customerCounts.get(customer1.getName() + PowerType.CONSUMPTION);
    assertEquals(1000, count, "1000 individuals");
    
    // add another customer, then withdraw some from the first
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.SIGNUP, 
                                              specs.get(PowerType.PRODUCTION),
                                              customer2, 
                                              42, // population
                                              0.0, 4.2, false));
    customerCounts = service.getCustomerCounts();
    assertEquals(2, customerCounts.size(), "two customers");
    count = customerCounts.get(customer1.getName() + PowerType.CONSUMPTION);
    assertEquals(1000, count, "still 1000 individuals");
    count = customerCounts.get(customer2.getName() + PowerType.PRODUCTION);
    assertEquals(42, count, "42 individuals");
    
    // now withdraw some from customer1
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.WITHDRAW, 
                                              specs.get(PowerType.CONSUMPTION),
                                              customer1, 
                                              400,
                                              0.0, 4.2, false));
    customerCounts = service.getCustomerCounts();
    assertEquals(2, customerCounts.size(), "still two customers");
    count = customerCounts.get(customer1.getName() + PowerType.CONSUMPTION);
    assertEquals(600, count, "600 individuals remain");
    count = customerCounts.get(customer2.getName() + PowerType.PRODUCTION);
    assertEquals(42, count, "still 42 individuals");
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testReceiveConsumeMessage ()
  {
    final HashMap<PowerType, TariffSpecification> specs = 
      new HashMap<PowerType, TariffSpecification>();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        TariffSpecification spec =(TariffSpecification)args[0];
        specs.put(spec.getPowerType(), spec);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));

    Broker face = init();
    HashMap<String, Integer> customerCounts = service.getCustomerCounts();
    assertEquals(0, customerCounts.size(), "no customers yet");
    TariffSpecification spec = specs.get(PowerType.CONSUMPTION);
    
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.SIGNUP, 
                                              spec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              0.0, 4.2, false));
    // now one customer, population=1000. Consume some power.
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.CONSUME, 
                                              spec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              500.0, 4.2, false));
    // check usage in current timeslot
    double usage = service.getUsageForCustomer(customer1, spec, 0);
    assertEquals(500.0, usage, 1e-6, "500 kwh");
    // check usage in next timeslot
    usage = service.getUsageForCustomer(customer1, spec, 1);
    assertEquals(0.0, usage, 1e-6, "no usage");
    
    // move the clock ahead and use some more power
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.CONSUME, 
                                              spec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              450.0, 4.2, false));
    usage = service.getUsageForCustomer(customer1, spec, 0);
    assertEquals(500.0, usage, 1e-6, "500 kwh");
    // check usage in next two timeslots
    usage = service.getUsageForCustomer(customer1, spec, 1);
    assertEquals(450.0, usage, 1e-6, "no usage");
    usage = service.getUsageForCustomer(customer1, spec, 2);
    assertEquals(0.0, usage, 1e-6, "no usage");
    
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testOrderTS0 ()
  {
    Broker face = init();
    Timeslot current = timeslotRepo.currentTimeslot(); 
    assertEquals(0, current.getSerialNumber(), "current timeslot has serial 0");
    final ArrayList<Order> orderList = new ArrayList<Order>(); 
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        orderList.add((Order)args[0]);
        return null;
      }
    }).when(mockProxy).routeMessage(isA(Order.class));
    
    // set bootstrap mode
    when(mockCompetitionControl.isBootstrapMode())
      .thenReturn(true);
    
    // activate the trading function by sending a cash position msg
    CashPosition cp = new CashPosition(face, 0.0,
                                       timeslotRepo.currentSerialNumber());
    face.receiveMessage(cp); // timeslot -1

    // without any subscriptions or consumption, we don't expect any orders
    assertEquals(0, orderList.size(), "0 orders");
  }
  
  // in timeslot 3, we should have 3 days of records on which to base the last
  @SuppressWarnings("rawtypes")
  // three bids
  @Test
  public void testOrderTS3 ()
  {
    // collect the tariff specs
    final HashMap<PowerType, TariffSpecification> specs = 
      new HashMap<PowerType, TariffSpecification>();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        TariffSpecification spec =(TariffSpecification)args[0];
        specs.put(spec.getPowerType(), spec);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));
    
    // collect orders when they are submitted
    final ArrayList<Order> orderList = new ArrayList<Order>(); 
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        orderList.add((Order)args[0]);
        return null;
      }
    }).when(mockProxy).routeMessage(isA(Order.class));

    // initialize the default broker
    Broker face = init();
    TariffSpecification cspec = specs.get(PowerType.CONSUMPTION);
    TariffSpecification pspec = specs.get(PowerType.PRODUCTION);

    //TimeslotComplete tsu = new TimeslotComplete(0); // 0-22 enabled
    assertFalse(timeslotRepo.isTimeslotEnabled(0), "ts0 disabled");
    assertTrue(timeslotRepo.isTimeslotEnabled(1), "ts1 enabled");
    assertTrue(timeslotRepo.isTimeslotEnabled(24), "ts24 enabled");
    assertFalse(timeslotRepo.isTimeslotEnabled(25), "ts25 not enabled");

    // ---- timeslot 0 ----
    //face.receiveMessage(tsu);
    // market clears for ts 0-22, but broker has not yet submitted bids
    // customer model runs, generating subscriptions and production/consumption
    // accounting runs, generating transactions
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.SIGNUP, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              0.0, 4.2, false));
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.SIGNUP, 
                                              pspec,
                                              customer2, 
                                              customer2.getPopulation(),
                                              0.0, 4.2, false));
    // usage = 500 in ts0
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.CONSUME, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              -500.0, 4.2, false));
    //TimeslotComplete tc = new TimeslotComplete(0);
    face.receiveMessage(endTimeslot()); // last message in ts0
    assertEquals(24, orderList.size(), "24 orders");
    Order order = orderList.get(0);
    assertNotNull(order, "first order not null");
    assertEquals(timeslotRepo.findBySerialNumber(1), order.getTimeslot(), "ts1 is first");
    assertEquals(0.5, order.getMWh(), 1e-6, "correct mwh");
    assertNull(order.getLimitPrice(), "correct price");
    order = orderList.get(22);
    assertNotNull(order, "last order not null");
    assertEquals(timeslotRepo.findBySerialNumber(23), order.getTimeslot(), "ts24 is last");
    assertEquals(0.5, order.getMWh(), 1e-6, "correct mwh");
    assertEquals((-1 - 99.0/22.0), order.getLimitPrice(), 1e-6, "correct price");
    orderList.clear();

    //timeService.setCurrentTime(timeslotRepo.currentTimeslot().getEndInstant());
    nextTimeslot();
    assertEquals(-500.0, service.getUsageForCustomer(customer1, cspec, 0), 1e-6, "correct usage index 0");
    assertEquals(0.0, service.getUsageForCustomer(customer1, cspec, 1), 1e-6, "correct usage index 1");
    
    // market clears ts1
    // customer model runs, usage = 420 in ts1
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.CONSUME, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              -450.0, 4.2, false));
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.PRODUCE, 
                                              pspec,
                                              customer2, 
                                              customer2.getPopulation(),
                                              30.0, -0.15, false));
    // accounting runs ts1
    face.receiveMessage(endTimeslot()); // end of ts0: 1 disabled, 24 enabled

    assertFalse(timeslotRepo.isTimeslotEnabled(0), "ts0 disabled");
    assertFalse(timeslotRepo.isTimeslotEnabled(1), "ts1 disabled");
    assertTrue(timeslotRepo.isTimeslotEnabled(2), "ts2 enabled");
    assertTrue(timeslotRepo.isTimeslotEnabled(25), "ts25 enabled");
    assertFalse(timeslotRepo.isTimeslotEnabled(26), "ts26 not enabled");
    // broker sends bids for ts2...ts24
    assertEquals(24, orderList.size(), "24 orders ts1");
    order = orderList.get(0);
    assertNotNull(order, "first order not null");
    assertEquals(timeslotRepo.findBySerialNumber(2), order.getTimeslot(), "ts2 is first");
    assertEquals(0.42, order.getMWh(), 1e-6, "correct mwh");
    assertNull(order.getLimitPrice(), "correct price");
    order = orderList.get(20);
    assertEquals(timeslotRepo.findBySerialNumber(22), order.getTimeslot(), "ts22 in list[20]");
    assertEquals(0.42, order.getMWh(), 1e-6, "correct mwh");
    order = orderList.get(22);
    assertEquals(0.5, order.getMWh(), 1e-6, "correct mwh");
    order = orderList.get(23);
    assertNotNull(order, "last order not null");
    assertEquals(timeslotRepo.findBySerialNumber(25), order.getTimeslot(), "ts25 is last");
    assertEquals(0.42, order.getMWh(), 1e-6, "correct mwh");
    assertEquals((-1.0 - 99.0/23.0), order.getLimitPrice(), 1e-6, "correct price");
    orderList.clear();

    nextTimeslot();
    //timeService.setCurrentTime(timeslotRepo.currentTimeslot().getEndInstant());
    // market clears ts2
    // customer model runs, usage 510 in ts2
    //timeService.setCurrentTime(timeslotRepo.currentTimeslot().getEndInstant());
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.CONSUME, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              -550.0, 4.2, false));
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.PRODUCE, 
                                              pspec,
                                              customer2, 
                                              customer2.getPopulation(),
                                              40.0, -0.15, false));
    // accounting runs ts2
    face.receiveMessage(endTimeslot()); // ts2 disabled, ts26 enabled
    // broker sends bids for ts3...ts26
    assertEquals(24, orderList.size(), "24 orders ts2");
    order = orderList.get(0);
    assertNotNull(order, "first order not null");
    assertEquals(timeslotRepo.findBySerialNumber(3), order.getTimeslot(), "ts3 is first");
    assertEquals(0.51, order.getMWh(), 1e-6, "correct mwh");
    assertNull(order.getLimitPrice(), "correct price");
    order = orderList.get(20);
    assertEquals(timeslotRepo.findBySerialNumber(23), order.getTimeslot(), "ts23 in list[20]");
    assertEquals(0.51, order.getMWh(), 1e-6, "correct mwh");
    order = orderList.get(21);
    assertEquals(0.50, order.getMWh(), 1e-6, "correct mwh");
    order = orderList.get(23);
    assertNotNull(order, "last order not null");
    assertEquals(timeslotRepo.findBySerialNumber(26), order.getTimeslot(), "ts26 is last");
    assertEquals(0.51, order.getMWh(), 1e-6, "correct mwh");
    assertEquals((-1.0 - 99.0/23.0), order.getLimitPrice(), 1e-6, "correct price");
    orderList.clear();
  }
  
  // Generate three days of bootstrap data
  @SuppressWarnings("rawtypes")
  @Test
  public void testOrderTS3Bootstrap ()
  {
    // collect the tariff specs
    final HashMap<PowerType, TariffSpecification> specs = 
      new HashMap<PowerType, TariffSpecification>();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        TariffSpecification spec =(TariffSpecification)args[0];
        specs.put(spec.getPowerType(), spec);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));

    // initialize the default broker
    Broker face = init();
    TariffSpecification cspec = specs.get(PowerType.CONSUMPTION);
    TariffSpecification pspec = specs.get(PowerType.PRODUCTION);

    //TimeslotComplete tsu = new TimeslotComplete(0); // 1-24 enabled

    // ---- timeslot 0 ----
    face.receiveMessage(endTimeslot());
    // market clears for ts 0-22, but broker has not yet submitted bids
    // customer model runs, generating subscriptions and production/consumption
    // accounting runs, generating transactions
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.SIGNUP, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              0.0, 4.2, false));
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.SIGNUP, 
                                              pspec,
                                              customer2, 
                                              customer2.getPopulation(),
                                              0.0, 4.2, false));
    // usage = 500 in ts0
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.CONSUME, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              -500.0, 4.2, false));
    CashPosition cp = new CashPosition(face, 0.0, timeslotRepo.currentSerialNumber());
    face.receiveMessage(cp); // last message in ts0

    nextTimeslot();

    // Should be two customer records, each should have one entry
    //List<CustomerBootstrapData> cbd = service.getCustomerBootstrapData(2);
    //

    //timeService.setCurrentTime(timeslotRepo.currentTimeslot().getEndInstant());
    
    // market clears ts1
    // customer model runs, usage = 420 in ts1
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.CONSUME, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              -450.0, 4.2, false));
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.PRODUCE, 
                                              pspec,
                                              customer2, 
                                              customer2.getPopulation(),
                                              30.0, -0.15, false));
    // accounting runs ts1
    face.receiveMessage(endTimeslot());
    // broker sends bids for ts2...ts24

    nextTimeslot();
    //timeService.setCurrentTime(timeslotRepo.currentTimeslot().getEndInstant());
    //face.receiveMessage(nextTimeslot()); // ts2 disabled, ts25 enabled
    // market clears ts2
    // customer model runs, usage 510 in ts2
    //timeService.setCurrentTime(timeslotRepo.currentTimeslot().getEndInstant());
    face.receiveMessage(new TariffTransaction(face,
                                              timeslotRepo.currentSerialNumber(),
                                              TariffTransaction.Type.CONSUME, 
                                              cspec,
                                              customer1, 
                                              customer1.getPopulation(),
                                              -550.0, 4.2, false));
//    face.receiveMessage(new TariffTransaction(face,
//                                              timeslotRepo.currentSerialNumber(),
//                                              TariffTransaction.Type.PRODUCE, 
//                                              pspec,
//                                              customer2, 
//                                              customer2.getPopulation(),
//                                              40.0, -0.15, false));
    // accounting runs ts2
    face.receiveMessage(endTimeslot());
    
    // check the customer bootstrap data
    List<CustomerBootstrapData> cbd = service.getCustomerBootstrapData(3);
    assertEquals(2, cbd.size(), "Two entries in cbd");
    CustomerBootstrapData first = cbd.get(0);
    assertEquals(3, first.getNetUsage().length, "Three usage records 1");
    CustomerBootstrapData second = cbd.get(1);
    assertEquals(3, second.getNetUsage().length, "Three usage records 2");
  }
  
  // set up some timeslots - ts0 is disabled, then 23 enabled slots
  private void createTimeslots()
  {
    Instant now = timeService.getCurrentTime();
    timeslotRepo.makeTimeslot(now);
  }

  // called to make the clock tick
  private void nextTimeslot ()
  {
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
  }
  
  // called to end a timeslot
  private TimeslotComplete endTimeslot ()
  {
    return new TimeslotComplete(timeslotRepo.currentTimeslot().getSerialNumber());
  }
  
  @SuppressWarnings("serial")
  class MockRandomSeed extends RandomSeed
  {

    public MockRandomSeed (String classname, long requesterId, String purpose)
    {
      super(classname, requesterId, purpose);
    }
    
    @Override
    public double nextDouble ()
    {
      return 0.5;
    }
  }
}
