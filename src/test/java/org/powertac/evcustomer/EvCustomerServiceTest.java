/*
 * Copyright 2013 the original author or authors.
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

package org.powertac.evcustomer;

import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.ActivityDetail;
import org.powertac.evcustomer.beans.Car;
import org.powertac.evcustomer.beans.SocialClassDetail;
import org.powertac.evcustomer.beans.SocialGroup;
import org.powertac.evcustomer.beans.SocialGroupDetail;
import org.powertac.evcustomer.customers.EvCustomer;
import org.powertac.evcustomer.customers.EvSocialClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;


/**
 * @author Govert Buijs
 * @version 0.5, Date: 2013.11.28
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class EvCustomerServiceTest
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
  private EvCustomerService evCustomerService;

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

  private Configurator config;
  private Instant exp;
  private Instant now;
  private Broker broker1;
  private TariffSpecification defaultTariffSpec;
  private Tariff defaultTariff;
  private List<Object[]> accountingArgs;
  private Competition comp;

  @Before
  public void setUp ()
  {
    customerRepo.recycle();
    tariffRepo.recycle();
    tariffSubscriptionRepo.recycle();
    timeslotRepo.recycle();
    weatherReportRepo.recycle();
    weatherReportRepo.runOnce();
    reset(mockAccounting);
    reset(mockServerProperties);

    // create a Competition, needed for initialization
    comp = Competition.newInstance("ev-customer-test");

    now = comp.getSimulationBaseTime();
    timeService.setCurrentTime(now);
    timeService.setBase(now.getMillis());
    exp = now.plus(TimeService.WEEK * 10);
    broker1 = new Broker("Joe");

    defaultTariffSpec =
        new TariffSpecification(broker1, PowerType.CONSUMPTION)
            .withExpiration(exp).addRate(new Rate().withValue(-0.5));
    defaultTariff = new Tariff(defaultTariffSpec);
    defaultTariff.init();
    defaultTariff.setState(Tariff.State.OFFERED);

    tariffRepo.setDefaultTariff(defaultTariffSpec);

    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
        .thenReturn(defaultTariff);

    when(mockTariffMarket.getDefaultTariff(PowerType.ELECTRIC_VEHICLE))
        .thenReturn(defaultTariff);

    config = new Configurator();
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
    }).when(mockAccounting)
        .addTariffTransaction(isA(TariffTransaction.Type.class),
            isA(Tariff.class), isA(CustomerInfo.class),
            anyInt(), anyDouble(), anyDouble());

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
  }

  @After
  public void tearDown ()
  {
    broker1 = null;
    now = null;
    exp = null;
    customerRepo = null;
    tariffRepo = null;
    tariffSubscriptionRepo = null;
    defaultTariffSpec = null;
    defaultTariff = null;
    accountingArgs = null;
    comp = null;
  }

  private String initializeService ()
  {
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    inits.add("TariffMarket");
    return evCustomerService.initialize(comp, inits);
  }

  private void subscribeDefault ()
  {
    for (EvSocialClass socialClass : evCustomerService.evSocialClassList) {
      for (CustomerInfo customerInfo : socialClass.getCustomerInfo()) {
        TariffSubscription defaultSub =
            tariffSubscriptionRepo.getSubscription(customerInfo, defaultTariff);
        defaultSub.subscribe(customerInfo.getPopulation());
      }
    }
  }

  @Test
  public void testNormalInitialization ()
  {
    String result = initializeService();
    assertEquals("EvCustomer", result);
  }

  @Test
  public void testBogusInitialization ()
  {
    List<String> inits = new ArrayList<String>();
    String result = evCustomerService.initialize(comp, inits);
    assertEquals(null, result);

    inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    result = evCustomerService.initialize(comp, inits);
    assertEquals(null, result);

    inits = new ArrayList<String>();
    inits.add("TariffMarket");
    result = evCustomerService.initialize(comp, inits);
    assertEquals(null, result);
  }

  @Test
  public void testServiceInitialization ()
  {
    initializeService();

    assertEquals(9, evCustomerService.evSocialClassList.size());

    subscribeDefault();

    for (EvSocialClass socialClass : evCustomerService.evSocialClassList) {
      for (CustomerInfo customerInfo : socialClass.getCustomerInfo()) {
        assertEquals("one subscription for CONSUMPTION customerInfo", 1,
            tariffSubscriptionRepo
                .findSubscriptionsForCustomer(customerInfo).size());
        assertEquals("customer on DefaultTariff",
            mockTariffMarket.getDefaultTariff(customerInfo
                .getPowerType()), tariffSubscriptionRepo
            .findSubscriptionsForCustomer(customerInfo).get(0)
            .getTariff());
      }
    }
  }

  @Test
  public void testCars ()
  {
    initializeService();

    List<Car> cars1 = EvCustomerService.loadCarTypes();

    assertEquals(cars1.size(), evCustomerService.cars.size());

    for (int i = 0; i < cars1.size(); i++) {
      Car car1 = cars1.get(i);
      Car car2 = evCustomerService.cars.get(i);

      assertEquals(car1.getName(), car2.getName());
      assertEquals(car1.getAwayCharging(), car2.getAwayCharging(), 1E-06);
      assertEquals(car1.getHomeCharging(), car2.getHomeCharging(), 1E-06);
      assertEquals(car1.getMaxCapacity(), car2.getMaxCapacity(), 1E-06);
      assertEquals(car1.getCurrentCapacity(), car2.getCurrentCapacity(), 1E-06);
      assertEquals(car1.getRange(), car2.getRange(), 1E-06);
    }
  }

  @Test
  public void testSocialGroups ()
  {
    initializeService();

    Map<Integer, SocialGroup> socialGroups = EvCustomerService.loadSocialGroups();

    assertEquals(socialGroups.size(), evCustomerService.socialGroups.size());

    for (SocialGroup sg1 : socialGroups.values()) {
      SocialGroup sg2 = evCustomerService.socialGroups.get(sg1.getId());

      assertEquals(sg1.getId(), sg2.getId());
      assertEquals(sg1.getName(), sg2.getName());
    }
  }

  @Test
  public void testActivities ()
  {
    initializeService();

    Map<Integer, Activity> activities = EvCustomerService.loadActivities();

    assertEquals(activities.size(), evCustomerService.activities.size());

    for (Activity act1 : activities.values()) {
      Activity act2 = evCustomerService.activities.get(act1.getId());

      assertEquals(act1.getId(), act2.getId());
      assertEquals(act1.getName(), act2.getName());
      assertEquals(act1.getWeekdayWeight(), act2.getWeekdayWeight(), 1E-6);
      assertEquals(act1.getWeekendWeight(), act2.getWeekendWeight(), 1E-6);
    }
  }

  @Test
  public void testActivityDetails ()
  {
    initializeService();

    Map<Integer, Map<Integer, ActivityDetail>> activityDetails =
        EvCustomerService.loadActivityDetails();

    assertEquals(activityDetails.size(),
        evCustomerService.allActivityDetails.size());

    for (Map.Entry<Integer, Map<Integer, ActivityDetail>> entry :
        activityDetails.entrySet()) {
      int groupId = entry.getKey();
      Map<Integer, ActivityDetail> actDetails1 = entry.getValue();
      Map<Integer, ActivityDetail> actDetails2 =
          evCustomerService.allActivityDetails.get(groupId);

      assertEquals(actDetails1.size(), actDetails2.size());

      for (ActivityDetail actDetail1 : actDetails1.values()) {
        ActivityDetail actDetail2 = actDetails2.get(actDetail1.getActivityId());

        assertEquals(actDetail1.getActivityId(), actDetail2.getActivityId());
        assertEquals(actDetail1.getFemaleDailyKm(), actDetail2.getFemaleDailyKm(), 1E-6);
        assertEquals(actDetail1.getMaleDailyKm(), actDetail2.getMaleDailyKm(), 1E-6);
        assertEquals(actDetail1.getFemaleProbability(), actDetail2.getFemaleProbability(), 1E-6);
        assertEquals(actDetail1.getFemaleProbability(), actDetail2.getFemaleProbability(), 1E-6);
        assertEquals(actDetail1.getMaleProbability(), actDetail2.getMaleProbability(), 1E-6);
      }
    }
  }

  @Test
  public void testSocialClassDetails ()
  {
    initializeService();

    Map<String, SocialClassDetail> socialClassDetails =
        EvCustomerService.loadSocialClassesDetails();

    assertEquals(socialClassDetails.size(),
        evCustomerService.socialClassDetails.size());

    for (SocialClassDetail scd1 : socialClassDetails.values()) {
      SocialClassDetail scd2 =
          evCustomerService.socialClassDetails.get(scd1.getName());

      assertEquals(scd1.getName(), scd2.getName());
      assertEquals(scd1.getMinCount(), scd2.getMinCount());
      assertEquals(scd1.getMaxCount(), scd2.getMaxCount());

      Map<Integer, SocialGroupDetail> sgds1 = scd1.getSocialGroupDetails();
      Map<Integer, SocialGroupDetail> sgds2 = scd2.getSocialGroupDetails();

      for (SocialGroupDetail sgd1 : sgds1.values()) {
        SocialGroupDetail sgd2 = sgds2.get(sgd1.getId());

        assertEquals(sgd1.getId(), sgd2.getId());
        assertEquals(sgd1.getProbability(), sgd2.getProbability(), 1E-6);
        assertEquals(sgd1.getMaleProbability(), sgd2.getMaleProbability(), 1E-6);
      }
    }
  }

  @Test
  public void testSocialClasses ()
  {
    initializeService();

    Map<String, SocialClassDetail> socialClassDetails =
        EvCustomerService.loadSocialClassesDetails();

    assertEquals(socialClassDetails.size(),
        evCustomerService.socialClassDetails.size());

    for (SocialClassDetail scd1 : socialClassDetails.values()) {
      SocialClassDetail scd2 =
          evCustomerService.socialClassDetails.get(scd1.getName());

      assertEquals(scd1.getName(), scd2.getName());
      assertEquals(scd1.getMinCount(), scd2.getMinCount());
      assertEquals(scd1.getMaxCount(), scd2.getMaxCount());

      Map<Integer, SocialGroupDetail> sgds1 = scd1.getSocialGroupDetails();
      Map<Integer, SocialGroupDetail> sgds2 = scd2.getSocialGroupDetails();

      for (SocialGroupDetail sgd1 : sgds1.values()) {
        SocialGroupDetail sgd2 = sgds2.get(sgd1.getId());

        assertEquals(sgd1.getId(), sgd2.getId());
        assertEquals(sgd1.getProbability(), sgd2.getProbability(), 1E-6);
        assertEquals(sgd1.getMaleProbability(), sgd2.getMaleProbability(), 1E-6);
      }
    }
  }

  @Test
  public void testSocialClassList ()
  {
    initializeService();

    assertEquals(9, evCustomerService.evSocialClassList.size());
  }

  @Test
  public void testPowerConsumption ()
  {
    initializeService();
    for (EvSocialClass evSocialClass : evCustomerService.evSocialClassList) {
      for (EvCustomer evCustomer : evSocialClass.getEvCustomers()) {
        evCustomer.makeDayPlanning(0);
      }
    }

    subscribeDefault();

    timeService.setCurrentTime(now.plus(18 * TimeService.HOUR));

    evCustomerService.activate(timeService.getCurrentTime(), 1);

    for (EvSocialClass socialClass : evCustomerService.evSocialClassList) {
      for (CustomerInfo customerInfo : socialClass.getCustomerInfo()) {
        List<TariffSubscription> subscriptions = tariffSubscriptionRepo
            .findActiveSubscriptionsForCustomer(customerInfo);

        assertFalse("EvSocialClass consumed power for each customerInfo",
            subscriptions == null || subscriptions.get(0).getTotalUsage() < 0);
      }
    }

    assertEquals(4 * evCustomerService.evSocialClassList.size(),
        accountingArgs.size());
  }

  @Test
  public void testPublishAndEvaluatingTariffs ()
  {
    initializeService();
    subscribeDefault();

    Rate rate = new Rate().withValue(-0.222);

    TariffSpecification tsc1 = createTariffSpec(PowerType.ELECTRIC_VEHICLE,
        now.plus(1 * TimeService.DAY), TimeService.WEEK * 8, rate);
    TariffSpecification tsc2 = createTariffSpec(PowerType.ELECTRIC_VEHICLE,
        now.plus(2 * TimeService.DAY), TimeService.WEEK * 8, rate);
    TariffSpecification tsc3 = createTariffSpec(PowerType.ELECTRIC_VEHICLE,
        now.plus(3 * TimeService.DAY), TimeService.WEEK * 8, rate);

    Tariff tariff1 = createTariff(tsc1);
    Tariff tariff2 = createTariff(tsc2);
    Tariff tariff3 = createTariff(tsc3);

    assertEquals(4, tariffRepo.findAllTariffs().size());
    assertNotNull("first tariff found", tariff1);
    assertNotNull("second tariff found", tariff2);
    assertNotNull("third tariff found", tariff3);
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.CONSUMPTION).size());
    assertEquals(3, tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).size());

    // Test the function with different inputs, in order to get the same
    evCustomerService.publishNewTariffs(
        tariffRepo.findActiveTariffs(PowerType.CONSUMPTION));

    tariff1.setState(Tariff.State.KILLED);
    assertTrue("tariff revoked", tariff1.isRevoked());
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.CONSUMPTION).size());
    assertEquals(2, tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).size());
    assertTrue(tariff2.getId() == tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).get(0).getId() ||
        tariff2.getId() == tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).get(1).getId());
    assertTrue(tariff3.getId() == tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).get(0).getId() ||
        tariff3.getId() == tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).get(1).getId());

    tariff2.setState(Tariff.State.KILLED);
    assertTrue("tariff revoked", tariff2.isRevoked());
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.CONSUMPTION).size());
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).size());
    assertEquals(tariff3.getId(),
        tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).get(0).getId());

    tariff3.setState(Tariff.State.KILLED);
    assertTrue("tariff revoked", tariff3.isRevoked());
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.CONSUMPTION).size());
    assertEquals(0, tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).size());

    List<Tariff> tcactivelist = new ArrayList<Tariff>();
    for (Tariff tariff : tariffRepo.findActiveTariffs(PowerType.CONSUMPTION)) {
      if (!tariff.isRevoked()) {
        tcactivelist.add(tariff);
      }
    }

    evCustomerService.publishNewTariffs(tcactivelist);
  }

  @Test
  public void testPublishAndEvaluatingRidiculousTariffs ()
  {
    initializeService();
    subscribeDefault();

    double rateValue = -500;
    Rate r1 = new Rate().withValue(rateValue / 2.0);
    Rate r2 = new Rate().withValue(rateValue / 2.0).withFixed(false)
        .withMaxValue(rateValue * 2.0).withExpectedMean(rateValue);

    TariffSpecification tsc1 = createTariffSpec(PowerType.ELECTRIC_VEHICLE,
        now.plus(1 * TimeService.DAY), null, r1).withSignupPayment(-500);
    TariffSpecification tsc2 = createTariffSpec(PowerType.ELECTRIC_VEHICLE,
        null, null, r2).withSignupPayment(-500);

    Tariff tariff1 = createTariff(tsc1);
    Tariff tariff2 = createTariff(tsc2);

    assertEquals(3, tariffRepo.findAllTariffs().size());
    assertNotNull("first tariff found", tariff1);
    assertNotNull("second tariff found", tariff2);
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.CONSUMPTION).size());
    assertEquals(2, tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).size());

    // Test the function with different inputs, in order to get the same
    evCustomerService.publishNewTariffs(tariffRepo.findActiveTariffs(PowerType.CONSUMPTION));

    tariff1.setState(Tariff.State.KILLED);
    assertTrue("tariff revoked", tariff1.isRevoked());
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.CONSUMPTION).size());
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).size());
    assertEquals(tariff2.getId(),
        tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).get(0).getId());

    tariff2.setState(Tariff.State.KILLED);
    assertTrue("tariff revoked", tariff2.isRevoked());
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.CONSUMPTION).size());
    assertEquals(0, tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).size());

    List<Tariff> tcactivelist = new ArrayList<Tariff>();
    for (Tariff tariff : tariffRepo.findActiveTariffs(PowerType.CONSUMPTION)) {
      if (!tariff.isRevoked()) {
        tcactivelist.add(tariff);
      }
    }

    evCustomerService.publishNewTariffs(tcactivelist);
  }

  @Test
  public void testPublishAndEvaluatingVariableTariffs ()
  {
    initializeService();
    subscribeDefault();

    Rate r1 = new Rate().withFixed(false).withValue(.1).withMaxValue(.2)
        .withExpectedMean(.15).withDailyBegin(0).withDailyEnd(13);
    Rate r2 = new Rate().withFixed(false).withValue(.15).withMaxValue(.9)
        .withExpectedMean(.2).withDailyBegin(14).withDailyEnd(23);

    TariffSpecification tsc1 = createTariffSpec(PowerType.ELECTRIC_VEHICLE,
        now.plus(1 * TimeService.DAY), TimeService.WEEK * 8, r2).addRate(r1);

    Tariff tariff1 = createTariff(tsc1);

    assertEquals(2, tariffRepo.findAllTariffs().size());
    assertNotNull("first tariff found", tariff1);
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.CONSUMPTION).size());
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).size());

    // Test the function with different inputs, in order to get the same
    evCustomerService.publishNewTariffs(
        tariffRepo.findActiveTariffs(PowerType.CONSUMPTION));

    tariff1.setState(Tariff.State.KILLED);
    assertTrue("tariff revoked", tariff1.isRevoked());
    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.CONSUMPTION).size());
    assertEquals(0, tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).size());

    List<Tariff> tcactivelist = new ArrayList<Tariff>();
    for (Tariff tariff : tariffRepo.findActiveTariffs(PowerType.CONSUMPTION)) {
      if (!tariff.isRevoked()) {
        tcactivelist.add(tariff);
      }
    }

    evCustomerService.publishNewTariffs(tcactivelist);
  }

  @Test
  public void testSupersedingTariffs ()
  {
    initializeService();
    subscribeDefault();

    Rate r1 = new Rate().withValue(-0.222);
    Rate r2 = new Rate().withValue(-0.111);

    TariffSpecification tsc1 = createTariffSpec(PowerType.ELECTRIC_VEHICLE,
        now.plus(1 * TimeService.DAY), TimeService.WEEK * 8, r1);
    TariffSpecification tsc2 = createTariffSpec(PowerType.ELECTRIC_VEHICLE,
        now.plus(2 * TimeService.DAY), TimeService.WEEK * 8, r1);
    TariffSpecification tsc3 = createTariffSpec(PowerType.ELECTRIC_VEHICLE,
        now.plus(3 * TimeService.DAY), TimeService.WEEK * 8, r1);
    TariffSpecification tsc4 = createTariffSpec(PowerType.ELECTRIC_VEHICLE,
        now.plus(3 * TimeService.DAY), TimeService.WEEK * 8, r2);

    Tariff tariff1 = createTariff(tsc1);
    Tariff tariff2 = createTariff(tsc2);
    Tariff tariff3 = createTariff(tsc3);
    Tariff tariff4 = createTariff(tsc4);

    tsc4.addSupersedes(tsc3.getId());

    assertEquals(1, tsc4.getSupersedes().size());
    assertEquals(tsc3.getId(), (long) tsc4.getSupersedes().get(0));
    assertNotNull("first tariff found", tariff1);
    assertNotNull("second tariff found", tariff2);
    assertNotNull("third tariff found", tariff3);
    assertEquals(5, tariffRepo.findAllTariffs().size());

    // Test the function with different inputs, in order to get the same
    evCustomerService.publishNewTariffs(
        tariffRepo.findActiveTariffs(PowerType.CONSUMPTION));

    timeService.setCurrentTime(new Instant(
        timeService.getCurrentTime().getMillis() + TimeService.HOUR));

    tariff3.setState(Tariff.State.KILLED);
    assertTrue("tariff revoked", tariff3.isRevoked());

    timeService.setCurrentTime(new Instant(
        timeService.getCurrentTime().getMillis() + TimeService.HOUR));

    assertEquals(1, tariffRepo.findActiveTariffs(PowerType.CONSUMPTION).size());
    assertEquals(3, tariffRepo.findActiveTariffs(PowerType.ELECTRIC_VEHICLE).size());

    List<Tariff> tcactivelist = new ArrayList<Tariff>();
    for (Tariff tariff : tariffRepo.findActiveTariffs(PowerType.CONSUMPTION)) {
      if (!tariff.isRevoked()) {
        tcactivelist.add(tariff);
      }
    }

    evCustomerService.publishNewTariffs(tcactivelist);
  }

  private Tariff createTariff (TariffSpecification tsc)
  {
    Tariff tariff = new Tariff(tsc);
    tariff.init();
    tariff.setState(Tariff.State.OFFERED);
    return tariff;
  }

  private TariffSpecification createTariffSpec (PowerType powerType,
                                                Instant instant,
                                                Long minDuration,
                                                Rate rate)
  {
    TariffSpecification tsc = new TariffSpecification(broker1, powerType)
        .withExpiration(instant).addRate(rate);

    if (minDuration != null) {
      tsc = tsc.withMinDuration(minDuration);
    }

    return tsc;
  }
}