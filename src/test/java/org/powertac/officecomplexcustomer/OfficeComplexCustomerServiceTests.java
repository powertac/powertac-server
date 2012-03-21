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
package org.powertac.officecomplexcustomer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;
import org.powertac.officecomplexcustomer.customers.OfficeComplex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
@DirtiesContext
public class OfficeComplexCustomerServiceTests
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
  private OfficeComplexCustomerService officeComplexCustomerService;

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

  @Before
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
    officeComplexCustomerService.clearConfiguration();
    reset(mockAccounting);
    reset(mockServerProperties);

    // create a Competition, needed for initialization
    comp = Competition.newInstance("officeComplex-customer-test");

    broker1 = new Broker("Joe");

    now = new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(now);
    timeService.setBase(now.getMillis());
    exp = now.plus(TimeService.WEEK * 10);

    defaultTariffSpec = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(exp).withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(-0.222));

    defaultTariff = new Tariff(defaultTariffSpec);
    defaultTariff.init();

    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION)).thenReturn(defaultTariff);

    accountingArgs = new ArrayList<Object[]>();

    // mock the AccountingService, capture args
    doAnswer(new Answer()
    {
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        accountingArgs.add(args);
        return null;
      }
    }).when(mockAccounting).addTariffTransaction(isA(TariffTransaction.Type.class), isA(Tariff.class), isA(CustomerInfo.class), anyInt(), anyDouble(), anyDouble());

    // Set up serverProperties mock

    ReflectionTestUtils.setField(officeComplexCustomerService, "serverPropertiesService", mockServerProperties);
    config = new Configurator();

    doAnswer(new Answer()
    {
      @Override
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(mockServerProperties).configureMe(anyObject());

    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("officecomplexcustomer.officeComplexCustomerService.configFile1", "OfficeComplexType1.properties");
    map.put("officecomplexcustomer.officeComplexCustomerService.configFile2", "OfficeComplexType2.properties");
    map.put("common.competition.expectedTimeslotCount", "1440");
    Configuration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    config.configureSingleton(comp);
  }

  public void initializeService ()
  {
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    officeComplexCustomerService.initialize(comp, inits);
    assertEquals("correct first configuration file", "OfficeComplexType1.properties", officeComplexCustomerService.getConfigFile1());
    assertEquals("correct second configuration file", "OfficeComplexType2.properties", officeComplexCustomerService.getConfigFile2());
    assertTrue(officeComplexCustomerService.getDaysOfCompetition() >= Competition.currentCompetition().getExpectedTimeslotCount() / OfficeComplexConstants.HOURS_OF_DAY);
  }

  // @Repeat(20)
  @Test
  public void testNormalInitialization ()
  {
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    String result = officeComplexCustomerService.initialize(comp, inits);
    assertEquals("correct return value", "OfficeComplexCustomer", result);
    assertEquals("correct configuration file", "OfficeComplexType1.properties", officeComplexCustomerService.getConfigFile1());
    assertEquals("correct second configuration file", "OfficeComplexType2.properties", officeComplexCustomerService.getConfigFile2());
    assertTrue(officeComplexCustomerService.getDaysOfCompetition() >= Competition.currentCompetition().getExpectedTimeslotCount() / OfficeComplexConstants.HOURS_OF_DAY);

  }

  // @Repeat(20)
  @Test
  public void testNormalInitializationWithoutConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("officecomplexcustomer.officeComplexCustomerService.configFile1", null);
    map.put("officecomplexcustomer.officeComplexCustomerService.configFile2", null);
    map.put("common.competition.expectedTimeslotCount", "1440");
    Configuration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    config.configureSingleton(comp);
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    String result = officeComplexCustomerService.initialize(comp, inits);
    assertEquals("correct return value", "OfficeComplexCustomer", result);
    assertEquals("correct configuration file", "OfficeComplexDefault.properties", officeComplexCustomerService.getConfigFile1());
    assertEquals("correct configuration file", "OfficeComplexDefault.properties", officeComplexCustomerService.getConfigFile2());
    assertTrue(officeComplexCustomerService.getDaysOfCompetition() >= Competition.currentCompetition().getExpectedTimeslotCount() / OfficeComplexConstants.HOURS_OF_DAY);
  }

  // @Repeat(20)
  @Test
  public void testBogusInitialization ()
  {
    List<String> inits = new ArrayList<String>();
    String result = officeComplexCustomerService.initialize(comp, inits);
    assertNull("return null value", result);
    inits.add("DefaultBroker");
  }

  // @Repeat(20)
  @Test
  public void testServiceInitialization ()
  {
    initializeService();
    assertEquals("Four Consumers Created", 4, officeComplexCustomerService.getOfficeComplexList().size());
    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {

      // capture subscription method args
      ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
      ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
      ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();

      // System.out.println(tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff().toString());
      assertEquals("one subscription for our customer", 1, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());
      assertEquals("customer on DefaultTariff", mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)),
          tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff());
    }
  }

  // @Repeat(20)
  @Test
  public void testPowerConsumption ()
  {
    initializeService();

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {

      // capture subscription method args
      ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
      ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
      ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();

    }

    timeService.setCurrentTime(now.plus(TimeService.HOUR));
    officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);
    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {
      assertFalse("Office consumed power",
          tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()) == null
              || tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTotalUsage() == 0);
    }

    assertEquals("Tariff Transactions Created", 3 * officeComplexCustomerService.getOfficeComplexList().size(), accountingArgs.size());

  }

  // @Repeat(20)
  @Test
  public void changeSubscription ()
  {
    initializeService();

    Rate r2 = new Rate().withValue(-0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();

    assertEquals("Two Tariffs", 2, tariffRepo.findAllTariffs().size());
    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {
      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();

      // Changing from default to another tariff
      TariffSubscription sub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff1);
      sub.subscribe(customer.getCustomerInfo().getPopulation());

      assertEquals("Two Subscriptions Active", 2, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());
    }

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);

      // Changing from default to another tariff
      TariffSubscription sub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff1);
      sub.subscribe(customer.getCustomerInfo().getPopulation());

      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(sub);
      when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tariffRepo.findAllTariffs());

      // System.out.println("Subscriptions: " +
      // tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).toString());

      customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));
      assertFalse(
          "Changed from default tariff",
          tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff() == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes()
              .get(0)));

      // System.out.println("Subscriptions: " +
      // tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).toString());

      // Changing back from the new tariff to the default one in order to check
      // every changeSubscription Method
      Tariff lastTariff = sub.getTariff();

      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      customer.changeSubscription(lastTariff, mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());

      assertTrue(
          "Changed to default tariff",
          tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff() == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes()
              .get(0)));

      sub.subscribe(customer.getOffices("SS").size());

      // Single type changeSubscription Method checked
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(sub);
      customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)), lastTariff, "SS");

      assertFalse("Changed SS from default tariff", customer.getSubscriptionMap().get("SS").getTariff() == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

      sub.subscribe(customer.getOffices("NS").size());

      // Changing back from the new tariff to the default one in order to check
      // every changeSubscription Method
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(sub);
      when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tariffRepo.findAllTariffs());

      customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)), "NS");

      assertFalse("Changed NS from default tariff", customer.getSubscriptionMap().get("NS").getTariff() == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

    }
  }

  // @Repeat(20)
  @Test
  public void revokeSubscription ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<TariffRevoke> tariffRevokeArg = ArgumentCaptor.forClass(TariffRevoke.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();
    }

    Rate r2 = new Rate().withValue(-0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(2 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();

    assertNotNull("first tariff found", tariff1);
    assertNotNull("second tariff found", tariff2);
    assertNotNull("third tariff found", tariff3);
    assertEquals("Four consumption tariffs", 4, tariffRepo.findAllTariffs().size());

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {

      TariffSubscription tsd = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION), customer.getCustomerInfo());
      customer.unsubscribe(tsd, 3);
      TariffSubscription sub1 = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff1);
      sub1.subscribe(3);
      TariffSubscription sub2 = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff2);
      sub2.subscribe(3);
      TariffSubscription sub3 = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff3);
      sub3.subscribe(4);

      TariffSubscription ts1 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff1, customer.getCustomerInfo());
      customer.unsubscribe(ts1, 2);
      TariffSubscription ts2 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff2, customer.getCustomerInfo());
      customer.unsubscribe(ts2, 1);
      TariffSubscription ts3 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff3, customer.getCustomerInfo());
      customer.unsubscribe(ts3, 2);
      assertEquals("4 Subscriptions for customer", 4, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());
      timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));

    }

    timeService.setCurrentTime(new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR));
    TariffRevoke tex = new TariffRevoke(tsc2.getBroker(), tsc2);

    tariff2.setState(Tariff.State.KILLED);

    assertTrue("tariff revoked", tariff2.isRevoked());

    officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);
    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {
      assertEquals("3 Subscriptions for customer", 3, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());
    }

    TariffRevoke tex2 = new TariffRevoke(tariff3.getBroker(), tariff3.getTariffSpec());

    tariff3.setState(Tariff.State.KILLED);
    assertTrue("tariff revoked", tariff3.isRevoked());

    officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);
    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {
      assertEquals("2 Subscriptions for customer", 2, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());
    }

  }

  // @Repeat(20)
  @Test
  public void testPublishAndEvaluatingTariffs ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();
    }

    Rate r2 = new Rate().withValue(-0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(2 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();

    assertNotNull("first tariff found", tariff1);
    assertNotNull("second tariff found", tariff2);
    assertNotNull("third tariff found", tariff3);
    assertEquals("Four consumption tariffs", 4, tariffRepo.findAllTariffs().size());

    List<Tariff> tclist = tariffRepo.findAllTariffs();
    assertEquals("4 consumption tariffs", 4, tclist.size());

    when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tclist);

    // Test the function with different inputs, in order to get the same result.
    officeComplexCustomerService.publishNewTariffs(tclist);
    List<Tariff> tclist2 = new ArrayList<Tariff>();
    officeComplexCustomerService.publishNewTariffs(tclist2);

    officeComplexCustomerService.publishNewTariffs(tclist);
    officeComplexCustomerService.publishNewTariffs(tclist2);
  }

  // @Repeat(20)
  @Test
  public void testSupersedingTariffs ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<TariffRevoke> tariffRevokeArg = ArgumentCaptor.forClass(TariffRevoke.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();

    }

    Rate r2 = new Rate().withValue(-0.222);
    Rate r3 = new Rate().withValue(-0.111);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(2 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc4 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r3);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();
    Tariff tariff4 = new Tariff(tsc4);
    tariff4.init();

    tsc4.addSupersedes(tsc3.getId());
    assertEquals("correct length", 1, tsc4.getSupersedes().size());
    assertEquals("correct first element", tsc3.getId(), (long) tsc4.getSupersedes().get(0));

    assertNotNull("first tariff found", tariff1);
    assertNotNull("second tariff found", tariff2);
    assertNotNull("third tariff found", tariff3);
    assertEquals("Five consumption tariffs", 5, tariffRepo.findAllTariffs().size());

    List<Tariff> tclist = tariffRepo.findAllTariffs();
    assertEquals("5 consumption tariffs", 5, tclist.size());

    when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tclist);

    // Test the function with different inputs, in order to get the same result.
    officeComplexCustomerService.publishNewTariffs(tclist);

    timeService.setCurrentTime(new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR));
    TariffRevoke tex = new TariffRevoke(tsc3.getBroker(), tsc3);

    tariff3.setState(Tariff.State.KILLED);

    assertTrue("tariff revoked", tariff3.isRevoked());

    timeService.setCurrentTime(new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR));

    tclist = tariffRepo.findAllTariffs();
    assertEquals("5 consumption tariffs", 5, tclist.size());
    List<Tariff> tcactivelist = new ArrayList<Tariff>();
    for (Tariff tariff : tclist) {
      if (tariff.isRevoked() == false)
        tcactivelist.add(tariff);
    }

    when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tcactivelist);

    officeComplexCustomerService.publishNewTariffs(tcactivelist);
  }

  // @Repeat(20)
  @Test
  public void testDailyShifting ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();
    }

    // for (int i = 0; i < 10; i++) {

    Rate r0 = new Rate().withValue(-Math.random()).withDailyBegin(0).withDailyEnd(0);
    Rate r1 = new Rate().withValue(-Math.random()).withDailyBegin(1).withDailyEnd(1);
    Rate r2 = new Rate().withValue(-Math.random()).withDailyBegin(2).withDailyEnd(2);
    Rate r3 = new Rate().withValue(-Math.random()).withDailyBegin(3).withDailyEnd(3);
    Rate r4 = new Rate().withValue(-Math.random()).withDailyBegin(4).withDailyEnd(4);
    Rate r5 = new Rate().withValue(-Math.random()).withDailyBegin(5).withDailyEnd(5);
    Rate r6 = new Rate().withValue(-Math.random()).withDailyBegin(6).withDailyEnd(6);
    Rate r7 = new Rate().withValue(-Math.random()).withDailyBegin(7).withDailyEnd(7);
    Rate r8 = new Rate().withValue(-Math.random()).withDailyBegin(8).withDailyEnd(8);
    Rate r9 = new Rate().withValue(-Math.random()).withDailyBegin(9).withDailyEnd(9);
    Rate r10 = new Rate().withValue(-Math.random()).withDailyBegin(10).withDailyEnd(10);
    Rate r11 = new Rate().withValue(-Math.random()).withDailyBegin(11).withDailyEnd(11);
    Rate r12 = new Rate().withValue(-Math.random()).withDailyBegin(12).withDailyEnd(12);
    Rate r13 = new Rate().withValue(-Math.random()).withDailyBegin(13).withDailyEnd(13);
    Rate r14 = new Rate().withValue(-Math.random()).withDailyBegin(14).withDailyEnd(14);
    Rate r15 = new Rate().withValue(-Math.random()).withDailyBegin(15).withDailyEnd(15);
    Rate r16 = new Rate().withValue(-Math.random()).withDailyBegin(16).withDailyEnd(16);
    Rate r17 = new Rate().withValue(-Math.random()).withDailyBegin(17).withDailyEnd(17);
    Rate r18 = new Rate().withValue(-Math.random()).withDailyBegin(18).withDailyEnd(18);
    Rate r19 = new Rate().withValue(-Math.random()).withDailyBegin(19).withDailyEnd(19);
    Rate r20 = new Rate().withValue(-Math.random()).withDailyBegin(20).withDailyEnd(20);
    Rate r21 = new Rate().withValue(-Math.random()).withDailyBegin(21).withDailyEnd(21);
    Rate r22 = new Rate().withValue(-Math.random()).withDailyBegin(22).withDailyEnd(22);
    Rate r23 = new Rate().withValue(-Math.random()).withDailyBegin(23).withDailyEnd(23);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(TimeService.DAY)).withMinDuration(TimeService.WEEK * 8);
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

    assertNotNull("first tariff found", tariff1);

    List<Tariff> tclist = tariffRepo.findAllTariffs();
    when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tclist);

    officeComplexCustomerService.publishNewTariffs(tclist);

    // }
    timeService.setBase(now.getMillis());
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 23));
    officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);
  }

  // @Repeat(20)
  @Test
  public void testWeather ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();
    }

    // for (int i = 0; i < 10; i++) {
    timeService.setBase(now.getMillis());
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 5));

    Timeslot ts1 = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
    // log.debug(ts1.toString());
    double temperature = 40 * Math.random();
    WeatherReport wr = new WeatherReport(ts1, temperature, 2, 3, 4);
    weatherReportRepo.add(wr);
    officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);

    for (int i = 0; i < 1000; i++) {
      timeService.setBase(now.getMillis());
      timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 1));
      ts1 = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
      // log.debug(ts1.toString());
      temperature = 40 * Math.random();
      wr = new WeatherReport(ts1, temperature, 2, 3, 4);
      weatherReportRepo.add(wr);
      officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);
    }

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {
      customer.showAggDailyLoad("SS", 0);
    }

    // for (int i = 0; i < 10; i++) {
    // timeService.setBase(now.getMillis());
    // timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR
    // * 12));
    // officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);

    // }

  }

  // @Test
  public void testAfterDaysOfCompetition ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();
    }

    for (OfficeComplex customer : officeComplexCustomerService.getOfficeComplexList()) {
      customer.showAggDailyLoad("SS", 0);
    }

    timeService.setBase(now.getMillis());
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.DAY * 1020));
    officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);

    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 23));
    officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);

    Timeslot ts1 = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
    // log.debug(ts1.toString());
    double temperature = 40 * Math.random();
    WeatherReport wr = new WeatherReport(ts1, temperature, 2, 3, 4);
    weatherReportRepo.add(wr);
    officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);

    for (int i = 0; i < 2000; i++) {
      timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 1));
      ts1 = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
      // log.debug(ts1.toString());
      if (i > 1700) {
        temperature = 40 * Math.random();
        wr = new WeatherReport(ts1, temperature, 2, 3, 4);
        weatherReportRepo.add(wr);
        officeComplexCustomerService.activate(timeService.getCurrentTime(), 1);
      }
    }

  }

}
