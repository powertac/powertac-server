/*
 * Copyright 2010-2012 the original author or authors.
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
package org.powertac.householdcustomer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.householdcustomer.customers.Village;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class HouseholdCustomerServiceTests
{

  @Autowired
  private TimeService timeService;

  @Autowired
  private Accounting mockAccounting;

  @Autowired
  private TariffMarket mockTariffMarket;

  @Autowired
  private ServerConfiguration mockServerProperties;

  @Autowired
  private HouseholdCustomerService householdCustomerService;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private WeatherReportRepo weatherReportRepo;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  private Configurator config;
  private Instant exp;
  private Broker broker1;
  private Instant now;
  private TariffSpecification defaultTariffSpec;
  private Tariff defaultTariff;
  private Competition comp;
  private List<Object[]> accountingArgs;

  @BeforeEach
  public void setUp ()
  {
    customerRepo.recycle();
    brokerRepo.recycle();
    tariffRepo.recycle();
    tariffSubscriptionRepo.recycle();
    randomSeedRepo.recycle();
    timeslotRepo.recycle();
    weatherReportRepo.recycle();
    weatherReportRepo.runOnce();
    householdCustomerService.clearConfiguration();
    reset(mockAccounting);
    reset(mockServerProperties);

    // create a Competition, needed for initialization
    comp = Competition.newInstance("household-customer-test");

    broker1 = new Broker("Joe");

    // now = new ZonedDateTime(2009, 10, 10, 0, 0, 0, 0,
    // DateTimeZone.UTC).toInstant();
    now = comp.getSimulationBaseTime();
    timeService.setCurrentTime(now);
    timeService.setBase(now.toEpochMilli());
    exp = now.plusMillis(TimeService.WEEK * 10);

    defaultTariffSpec =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(exp).addRate(new Rate().withValue(-0.5));
    defaultTariff = new Tariff(defaultTariffSpec);
    defaultTariff.init();
    defaultTariff.setState(Tariff.State.OFFERED);

    tariffRepo.setDefaultTariff(defaultTariffSpec);

    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
            .thenReturn(defaultTariff);

    when(mockTariffMarket.getDefaultTariff(PowerType.INTERRUPTIBLE_CONSUMPTION))
            .thenReturn(defaultTariff);

    accountingArgs = new ArrayList<Object[]>();

    // mock the AccountingService, capture args
    doAnswer(new Answer<Object>() {
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        accountingArgs.add(args);
        return null;
      }
    }).when(mockAccounting)
            .addTariffTransaction(isA(TariffTransaction.Type.class),
                                  isA(Tariff.class), isA(CustomerInfo.class),
                                  anyInt(), anyDouble(), anyDouble());

    // Set up serverProperties mock

    ReflectionTestUtils.setField(householdCustomerService,
                                 "serverPropertiesService",
                                 mockServerProperties);
    config = new Configurator();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(mockServerProperties).configureMe(any());

    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("householdcustomer.householdCustomerService.configFile1",
            "VillageType1.properties");
    map.put("common.competition.expectedTimeslotCount", "1440");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    config.configureSingleton(comp);

  }

  @AfterEach
  public void tearDown ()
  {
    timeService = null;
    mockAccounting = null;
    mockTariffMarket = null;
    mockServerProperties = null;
    householdCustomerService = null;
    tariffRepo = null;
    customerRepo = null;
    tariffSubscriptionRepo = null;
    timeslotRepo = null;
    weatherReportRepo = null;
    brokerRepo = null;
    randomSeedRepo = null;
    config = null;
    exp = null;
    broker1 = null;
    now = null;
    defaultTariffSpec = null;
    defaultTariff = null;
    comp = null;
    accountingArgs = null;
  }

  public void initializeService ()
  {
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    inits.add("TariffMarket");
    householdCustomerService.initialize(comp, inits);
    assertEquals("VillageType1.properties", householdCustomerService.getConfigFile1(), "correct first configuration file");
  }

  // @Repeat(20)
  @Test
  public void testNormalInitialization ()
  {
    initializeService();
  }

  // @Repeat(20)
  @Test
  public void testNormalInitializationWithoutConfig ()
  {
    TreeMap<String, String> map2 = new TreeMap<String, String>();
    map2.put("householdcustomer.householdCustomerService.configFile1", null);

    MapConfiguration mapConfig = new MapConfiguration(map2);
    config.setConfiguration(mapConfig);
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    inits.add("TariffMarket");
    String result = householdCustomerService.initialize(comp, inits);
    assertEquals(result, "HouseholdCustomer", "correct return value");
    assertEquals("VillageDefault.properties", householdCustomerService.getConfigFile1(), "correct configuration file");
  }

  // @Repeat(20)
  @Test
  public void testBogusInitialization ()
  {
    List<String> inits = new ArrayList<String>();
    String result = householdCustomerService.initialize(comp, inits);
    assertNull(result, "return null value");
    inits.add("DefaultBroker");
  }

  // @Repeat(20)
  @Test
  public void testServiceInitialization ()
  {
    initializeService();
    assertEquals(2, householdCustomerService.getVillageList().size(), "Two Consumers Created");

    for (Village customer: householdCustomerService.getVillageList()) {

      for (CustomerInfo customerInfo: customer.getCustomerInfos()) {

        TariffSubscription defaultSub =
          tariffSubscriptionRepo.getSubscription(customerInfo, defaultTariff);
        defaultSub.subscribe(customerInfo.getPopulation());
      }

      for (CustomerInfo customerInfo: customer.getCustomerInfos()) {
        assertEquals(1, tariffSubscriptionRepo.findSubscriptionsForCustomer(customerInfo).size(), "one subscription for CONSUMPTION customerInfo");

        assertEquals(mockTariffMarket.getDefaultTariff(customerInfo
                             .getPowerType()), tariffSubscriptionRepo
                             .findSubscriptionsForCustomer(customerInfo).get(0)
                             .getTariff(),
                "customer on DefaultTariff");
      }
    }
  }

  // @Repeat(20)
  @Test
  public void testPowerConsumption ()
  {
    initializeService();

    for (Village customer: householdCustomerService.getVillageList()) {
      for (CustomerInfo customerInfo: customer.getCustomerInfos()) {
        TariffSubscription defaultSub =
          tariffSubscriptionRepo.getSubscription(customerInfo, defaultTariff);
        defaultSub.subscribe(customerInfo.getPopulation());
      }
    }

    timeService.setCurrentTime(now.plusMillis(18 * TimeService.HOUR));
    householdCustomerService.activate(timeService.getCurrentTime(), 1);

    for (Village customer: householdCustomerService.getVillageList())
      for (CustomerInfo customerInfo: customer.getCustomerInfos())
        assertFalse(tariffSubscriptionRepo
                            .findActiveSubscriptionsForCustomer(customerInfo) == null,
//                            || tariffSubscriptionRepo
//                                    .findActiveSubscriptionsForCustomer(customerInfo)
//                                    .get(0).getTotalUsage() < 0,
                "Household consumed power for each customerInfo");

    //assertEquals(16 * householdCustomerService.getVillageList().size(), accountingArgs.size(), "Tariff Transactions Created");
    assertEquals(8 * householdCustomerService.getVillageList().size(), accountingArgs.size(), "Tariff Transactions Created");
  }

  // @Repeat(20)
  @Test
  public void testPublishAndEvaluatingTariffs ()
  {
    initializeService();

    ArgumentCaptor<PowerType> powerArg =
      ArgumentCaptor.forClass(PowerType.class);

    for (Village customer: householdCustomerService.getVillageList()) {
      for (CustomerInfo customerInfo: customer.getCustomerInfos()) {
        TariffSubscription defaultSub =
          tariffSubscriptionRepo.getSubscription(customerInfo, defaultTariff);
        defaultSub.subscribe(customerInfo.getPopulation());
      }
    }

    Rate r2 = new Rate().withValue(-0.222);

    TariffSpecification tsc1 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plusMillis(TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plusMillis(TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc3 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plusMillis(3 * TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    tariff1.setState(Tariff.State.OFFERED);
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    tariff2.setState(Tariff.State.OFFERED);
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();
    tariff3.setState(Tariff.State.OFFERED);

    assertEquals(4, tariffRepo.findAllTariffs().size(), "Four consumption tariffs");

    assertNotNull(tariff1, "first tariff found");
    assertNotNull(tariff2, "second tariff found");
    assertNotNull(tariff3, "third tariff found");

    List<Tariff> tclist1 = tariffRepo.findActiveTariffs(PowerType.CONSUMPTION);
    List<Tariff> tclist2 =
      tariffRepo.findActiveTariffs(PowerType.INTERRUPTIBLE_CONSUMPTION);

    assertEquals(4, tclist1.size(), "4 consumption tariffs");
    assertEquals(0, tclist2.size(), "0 interruptible consumption tariffs");

    when(mockTariffMarket.getActiveTariffList(powerArg.capture()))
            .thenReturn(tclist1).thenReturn(tclist2);

    // Test the function with different inputs, in order to get the same
    householdCustomerService.publishNewTariffs(tclist1);
  }

  // @Repeat(20)
  @Test
  public void testPublishAndEvaluatingRidiculousTariffs ()
  {
    initializeService();

    ArgumentCaptor<PowerType> powerArg =
      ArgumentCaptor.forClass(PowerType.class);

    for (Village customer: householdCustomerService.getVillageList()) {
      for (CustomerInfo customerInfo: customer.getCustomerInfos()) {
        TariffSubscription defaultSub =
          tariffSubscriptionRepo.getSubscription(customerInfo, defaultTariff);
        defaultSub.subscribe(customerInfo.getPopulation());
      }
    }

    double rateValue = -500;

    Rate r2 = new Rate().withValue(-(0.5 * 500));
    Rate rate =
      new Rate().withValue(rateValue / 2.0).withFixed(false)
              .withMaxValue(rateValue * 2.0).withExpectedMean(rateValue);

    TariffSpecification tsc1 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plusMillis(TimeService.DAY))
              .withSignupPayment(-500).addRate(r2);

    TariffSpecification tsc2 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
      // .withPeriodicPayment(defaultPeriodicPayment)
              .withSignupPayment(-500.0).addRate(rate);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    tariff1.setState(Tariff.State.OFFERED);
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    tariff2.setState(Tariff.State.OFFERED);

    assertEquals(3, tariffRepo.findAllTariffs().size(), "Three consumption tariffs");

    assertNotNull(tariff1, "first tariff found");
    assertNotNull(tariff2, "second tariff found");

    List<Tariff> tclist1 = tariffRepo.findActiveTariffs(PowerType.CONSUMPTION);
    List<Tariff> tclist2 =
      tariffRepo.findActiveTariffs(PowerType.INTERRUPTIBLE_CONSUMPTION);

    assertEquals(3, tclist1.size(), "3 consumption tariffs");
    assertEquals(0, tclist2.size(), "0 interruptible consumption tariffs");

    when(mockTariffMarket.getActiveTariffList(powerArg.capture()))
            .thenReturn(tclist1).thenReturn(tclist2);

    // Test the function with different inputs, in order to get the same
    householdCustomerService.publishNewTariffs(tclist1);
  }

  // @Repeat(20)
  @Test
  public void testPublishAndEvaluatingVariableTariffs ()
  {
    initializeService();

    ArgumentCaptor<PowerType> powerArg =
      ArgumentCaptor.forClass(PowerType.class);

    for (Village customer: householdCustomerService.getVillageList()) {
      for (CustomerInfo customerInfo: customer.getCustomerInfos()) {
        TariffSubscription defaultSub =
          tariffSubscriptionRepo.getSubscription(customerInfo, defaultTariff);
        defaultSub.subscribe(customerInfo.getPopulation());
      }
    }

    Rate r1 =
      new Rate().withFixed(false).withValue(-.1).withMaxValue(-.2)
              .withExpectedMean(-.15).withDailyBegin(0).withDailyEnd(13);
    assertTrue(r1.isValid(PowerType.CONSUMPTION));
    Rate r2 =
      new Rate().withFixed(false).withValue(-.15).withMaxValue(-.9)
              .withExpectedMean(-.2).withDailyBegin(14).withDailyEnd(23);
    assertTrue(r2.isValid(PowerType.CONSUMPTION));

    TariffSpecification tsc1 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plusMillis(TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2).addRate(r1);

    Tariff tariff1 = new Tariff(tsc1);
    assertTrue(tariff1.init(), "valid tariff1");
    tariff1.setState(Tariff.State.OFFERED);

    assertEquals(2, tariffRepo.findAllTariffs().size(), "Two consumption tariffs");

    assertNotNull(tariff1, "first tariff found");

    List<Tariff> tclist1 = tariffRepo.findActiveTariffs(PowerType.CONSUMPTION);
    List<Tariff> tclist2 =
      tariffRepo.findActiveTariffs(PowerType.INTERRUPTIBLE_CONSUMPTION);

    assertEquals(2, tclist1.size(), "2 consumption tariffs");
    assertEquals(0, tclist2.size(), "0 interruptible consumption tariffs");

    when(mockTariffMarket.getActiveTariffList(powerArg.capture()))
            .thenReturn(tclist1).thenReturn(tclist2);

    // Test the function with different inputs, in order to get the same
    householdCustomerService.publishNewTariffs(tclist1);
  }

  // @Repeat(20)
  @Test
  public void testSupersedingTariffs ()
  {
    initializeService();

    ArgumentCaptor<PowerType> powerArg =
      ArgumentCaptor.forClass(PowerType.class);

    for (Village customer: householdCustomerService.getVillageList()) {
      for (CustomerInfo customerInfo: customer.getCustomerInfos()) {
        TariffSubscription defaultSub =
          tariffSubscriptionRepo.getSubscription(customerInfo, defaultTariff);
        defaultSub.subscribe(customerInfo.getPopulation());
      }
    }

    Rate r2 = new Rate().withValue(-0.222);
    Rate r3 = new Rate().withValue(-0.111);

    TariffSpecification tsc1 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plusMillis(TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plusMillis(2 * TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc3 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plusMillis(3 * TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc4 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plusMillis(3 * TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r3);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    tariff1.setState(Tariff.State.OFFERED);
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    tariff2.setState(Tariff.State.OFFERED);
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();
    tariff3.setState(Tariff.State.OFFERED);
    Tariff tariff4 = new Tariff(tsc4);
    tariff4.init();
    tariff4.setState(Tariff.State.OFFERED);

    tsc4.addSupersedes(tsc3.getId());
    assertEquals(1, tsc4.getSupersedes().size(), "correct length");
    assertEquals(tsc3.getId(), (long) tsc4.getSupersedes().get(0), "correct first element");

    assertNotNull(tariff1, "first tariff found");
    assertNotNull(tariff2, "second tariff found");
    assertNotNull(tariff3, "third tariff found");
    assertEquals(5, tariffRepo.findAllTariffs().size(), "Five consumption tariffs");

    List<Tariff> tclist1 = tariffRepo.findActiveTariffs(PowerType.CONSUMPTION);
    List<Tariff> tclist2 =
      tariffRepo.findActiveTariffs(PowerType.INTERRUPTIBLE_CONSUMPTION);

    when(mockTariffMarket.getActiveTariffList(powerArg.capture()))
            .thenReturn(tclist1).thenReturn(tclist2);

    // Test the function with different inputs, in order to get the same
    householdCustomerService.publishNewTariffs(tclist1);

    timeService.setCurrentTime(Instant.ofEpochMilli(timeService.getCurrentTime()
            .toEpochMilli() + TimeService.HOUR));
    tariff3.setState(Tariff.State.KILLED);
    assertTrue(tariff3.isRevoked(), "tariff revoked");

    timeService.setCurrentTime(Instant.ofEpochMilli(timeService.getCurrentTime()
            .toEpochMilli() + TimeService.HOUR));

    tclist1 = tariffRepo.findActiveTariffs(PowerType.CONSUMPTION);
    tclist2 = tariffRepo.findActiveTariffs(PowerType.INTERRUPTIBLE_CONSUMPTION);

    assertEquals(4, tclist1.size(), "4 consumption tariffs");
    List<Tariff> tcactivelist = new ArrayList<Tariff>();
    for (Tariff tariff: tclist1) {
      if (tariff.isRevoked() == false)
        tcactivelist.add(tariff);
    }

    when(mockTariffMarket.getActiveTariffList(powerArg.capture()))
            .thenReturn(tcactivelist).thenReturn(tclist2);

    householdCustomerService.publishNewTariffs(tcactivelist);
  }

  // @Repeat(20)
  @Test
  public void testDailyShifting ()
  {
    initializeService();

    ArgumentCaptor<PowerType> powerArg =
      ArgumentCaptor.forClass(PowerType.class);

    for (Village customer: householdCustomerService.getVillageList()) {
      for (CustomerInfo customerInfo: customer.getCustomerInfos()) {
        TariffSubscription defaultSub =
          tariffSubscriptionRepo.getSubscription(customerInfo, defaultTariff);
        defaultSub.subscribe(customerInfo.getPopulation());
      }
    }

    Rate r0 =
      new Rate().withValue(-Math.random()).withDailyBegin(0).withDailyEnd(0);
    Rate r1 =
      new Rate().withValue(-Math.random()).withDailyBegin(1).withDailyEnd(1);
    Rate r2 =
      new Rate().withValue(-Math.random()).withDailyBegin(2).withDailyEnd(2);
    Rate r3 =
      new Rate().withValue(-Math.random()).withDailyBegin(3).withDailyEnd(3);
    Rate r4 =
      new Rate().withValue(-Math.random()).withDailyBegin(4).withDailyEnd(4);
    Rate r5 =
      new Rate().withValue(-Math.random()).withDailyBegin(5).withDailyEnd(5);
    Rate r6 =
      new Rate().withValue(-Math.random()).withDailyBegin(6).withDailyEnd(6);
    Rate r7 =
      new Rate().withValue(-Math.random()).withDailyBegin(7).withDailyEnd(7);
    Rate r8 =
      new Rate().withValue(-Math.random()).withDailyBegin(8).withDailyEnd(8);
    Rate r9 =
      new Rate().withValue(-Math.random()).withDailyBegin(9).withDailyEnd(9);
    Rate r10 =
      new Rate().withValue(-Math.random()).withDailyBegin(10).withDailyEnd(10);
    Rate r11 =
      new Rate().withValue(-Math.random()).withDailyBegin(11).withDailyEnd(11);
    Rate r12 =
      new Rate().withValue(-Math.random()).withDailyBegin(12).withDailyEnd(12);
    Rate r13 =
      new Rate().withValue(-Math.random()).withDailyBegin(13).withDailyEnd(13);
    Rate r14 =
      new Rate().withValue(-Math.random()).withDailyBegin(14).withDailyEnd(14);
    Rate r15 =
      new Rate().withValue(-Math.random()).withDailyBegin(15).withDailyEnd(15);
    Rate r16 =
      new Rate().withValue(-Math.random()).withDailyBegin(16).withDailyEnd(16);
    Rate r17 =
      new Rate().withValue(-Math.random()).withDailyBegin(17).withDailyEnd(17);
    Rate r18 =
      new Rate().withValue(-Math.random()).withDailyBegin(18).withDailyEnd(18);
    Rate r19 =
      new Rate().withValue(-Math.random()).withDailyBegin(19).withDailyEnd(19);
    Rate r20 =
      new Rate().withValue(-Math.random()).withDailyBegin(20).withDailyEnd(20);
    Rate r21 =
      new Rate().withValue(-Math.random()).withDailyBegin(21).withDailyEnd(21);
    Rate r22 =
      new Rate().withValue(-Math.random()).withDailyBegin(22).withDailyEnd(22);
    Rate r23 =
      new Rate().withValue(-Math.random()).withDailyBegin(23).withDailyEnd(23);

    TariffSpecification tsc1 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plusMillis(TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8);
    tsc1.addRate(r0);
    tsc1.addRate(r1);
    tsc1.addRate(r2);
    tsc1.addRate(r3);
    tsc1.addRate(r4);
    tsc1.addRate(r5);
    tsc1.addRate(r6);
    tsc1.addRate(r7);
    tsc1.addRate(r8);
    tsc1.addRate(r9);
    tsc1.addRate(r10);
    tsc1.addRate(r11);
    tsc1.addRate(r12);
    tsc1.addRate(r13);
    tsc1.addRate(r14);
    tsc1.addRate(r15);
    tsc1.addRate(r16);
    tsc1.addRate(r17);
    tsc1.addRate(r18);
    tsc1.addRate(r19);
    tsc1.addRate(r20);
    tsc1.addRate(r21);
    tsc1.addRate(r22);
    tsc1.addRate(r23);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    tariff1.setState(Tariff.State.OFFERED);

    assertNotNull(tariff1, "first tariff found");

    List<Tariff> tclist1 = tariffRepo.findActiveTariffs(PowerType.CONSUMPTION);
    List<Tariff> tclist2 =
      tariffRepo.findActiveTariffs(PowerType.INTERRUPTIBLE_CONSUMPTION);

    assertEquals(2, tclist1.size(), "2 consumption tariffs");
    assertEquals(0, tclist2.size(), "0 interruptible consumption tariffs");

    when(mockTariffMarket.getActiveTariffList(powerArg.capture()))
            .thenReturn(tclist1).thenReturn(tclist2);

    householdCustomerService.publishNewTariffs(tclist1);

    timeService.setBase(now.toEpochMilli());
    timeService.setCurrentTime(timeService.getCurrentTime()
            .plusMillis(TimeService.HOUR * 23));
    householdCustomerService.activate(timeService.getCurrentTime(), 1);
  }

  // @Repeat(20)
  @Test
  public void testWeather ()
  {
    initializeService();

    for (Village customer: householdCustomerService.getVillageList()) {
      for (CustomerInfo customerInfo: customer.getCustomerInfos()) {
        TariffSubscription defaultSub =
          tariffSubscriptionRepo.getSubscription(customerInfo, defaultTariff);
        defaultSub.subscribe(customerInfo.getPopulation());
      }
    }

    // for (int i = 0; i < 10; i++) {
    timeService.setBase(now.toEpochMilli());
    timeService.setCurrentTime(timeService.getCurrentTime()
            .plusMillis(TimeService.HOUR * 5));

    Timeslot ts1 = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
    // log.debug(ts1.toString());
    double temperature = 40 * Math.random();
    WeatherReport wr = new WeatherReport(ts1.getSerialNumber(), temperature, 2, 3, 4);
    weatherReportRepo.add(wr);
    householdCustomerService.activate(timeService.getCurrentTime(), 1);

    for (int i = 0; i < 30; i++) {
      timeService.setBase(now.toEpochMilli());
      timeService.setCurrentTime(timeService.getCurrentTime()
              .plusMillis(TimeService.HOUR * 1));
      ts1 = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
      // log.debug(ts1.toString());
      temperature = 40 * Math.random();
      wr = new WeatherReport(ts1.getSerialNumber(), temperature, 2, 3, 4);
      weatherReportRepo.add(wr);
      householdCustomerService.activate(timeService.getCurrentTime(), 1);
    }
  }

  @Test
  public void testAfterDaysOfCompetition ()
  {
    initializeService();

    for (Village customer: householdCustomerService.getVillageList()) {
      for (CustomerInfo customerInfo: customer.getCustomerInfos()) {
        TariffSubscription defaultSub =
          tariffSubscriptionRepo.getSubscription(customerInfo, defaultTariff);
        defaultSub.subscribe(customerInfo.getPopulation());
      }
    }

    timeService.setBase(now.toEpochMilli());
    timeService.setCurrentTime(timeService.getCurrentTime()
            .plusMillis(TimeService.DAY * 1020));
    householdCustomerService.activate(timeService.getCurrentTime(), 1);

    timeService.setCurrentTime(timeService.getCurrentTime()
            .plusMillis(TimeService.HOUR * 23));
    householdCustomerService.activate(timeService.getCurrentTime(), 1);

    Timeslot ts1 = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
    // log.debug(ts1.toString());
    double temperature = 40 * Math.random();
    WeatherReport wr = new WeatherReport(ts1.getSerialNumber(), temperature, 2, 3, 4);
    weatherReportRepo.add(wr);
    householdCustomerService.activate(timeService.getCurrentTime(), 1);

    for (int i = 1700; i < 1730; i++) {
      timeService.setCurrentTime(timeService.getCurrentTime()
              .plusMillis(TimeService.HOUR * 1));
      ts1 = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
      // log.debug(ts1.toString());

      temperature = 40 * Math.random();
      wr = new WeatherReport(ts1.getSerialNumber(), temperature, 2, 3, 4);
      weatherReportRepo.add(wr);
      householdCustomerService.activate(timeService.getCurrentTime(), 1);
    }
  }
}
