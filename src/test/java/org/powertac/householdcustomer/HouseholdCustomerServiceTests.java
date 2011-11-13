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
package org.powertac.householdcustomer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.accounting.AccountingInitializationService;
import org.powertac.accounting.AccountingService;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.PauseRelease;
import org.powertac.common.msg.PauseRequest;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.householdcustomer.customers.Village;
import org.powertac.tariffmarket.TariffMarketInitializationService;
import org.powertac.tariffmarket.TariffMarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Antonios Chrysopoulos
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:src/test/resources/test-config.xml" })
@DirtiesContext
public class HouseholdCustomerServiceTests
{

  @Autowired
  private TimeService timeService;

  @Autowired
  private AccountingService accountingService;

  @Autowired
  private AccountingInitializationService accountingInitializationService;

  @Autowired
  private TariffMarketService tariffMarketService;

  @Autowired
  private TariffMarketInitializationService tariffMarketInitializationService;

  @Autowired
  private HouseholdCustomerService householdCustomerService;

  @Autowired
  private HouseholdCustomerInitializationService householdCustomerInitializationService;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private PluginConfigRepo pluginConfigRepo;

  private Instant exp;
  private Broker broker1;
  private Broker broker2;
  private Instant now;
  private TariffSpecification defaultTariffSpec;
  private Competition comp;

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("src/test/resources/log.config");
  }

  @Before
  public void setUp ()
  {
    customerRepo.recycle();
    brokerRepo.recycle();
    tariffRepo.recycle();
    tariffSubscriptionRepo.recycle();
    pluginConfigRepo.recycle();
    randomSeedRepo.recycle();
    timeslotRepo.recycle();

    // create a Competition, needed for initialization
    comp = Competition.newInstance("household-customer-test");

    broker1 = new Broker("Joe");
    broker2 = new Broker("Anna");

    now = new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(now);
    timeService.setBase(now.getMillis());
    //timeService.setStart(now.getMillis());
    exp = now.plus(TimeService.WEEK * 10);

    List<String> inits = new ArrayList<String>();

    accountingInitializationService.setDefaults();
    accountingInitializationService.initialize(comp, inits);

    inits.add("AccountingService");

    tariffMarketInitializationService.setDefaults();
    tariffMarketInitializationService.initialize(comp, inits);

    defaultTariffSpec = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(exp).withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(0.222));
    tariffMarketService.setDefaultTariff(defaultTariffSpec);

  }

  @After
  public void shutDown ()
  {
    // VillageService.shutDown();
  }

  public void initializeService ()
  {
    householdCustomerInitializationService.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("HouseholdCustomer");
    config.getConfiguration().put("cofigFile", "src/main/resources/Household.properties");
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    householdCustomerInitializationService.initialize(comp, inits);
  }

  @Test
  public void testNormalInitialization ()
  {
    householdCustomerInitializationService.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("HouseholdCustomer");
    config.getConfiguration().put("cofigFile", "src/main/resources/Household.properties");
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    String result = householdCustomerInitializationService.initialize(comp, inits);
    assertEquals("correct return value", "HouseholdCustomer", result);
    assertEquals("correct configuration file", "src/main/resources/Household.properties", householdCustomerService.getConfigFile());
  }

  @Test
  public void testBogusInitialization ()
  {
    PluginConfig config = pluginConfigRepo.findByRoleName("HouseholdCustomer");
    assertNull("config not created", config);
    List<String> inits = new ArrayList<String>();
    String result = householdCustomerInitializationService.initialize(comp, inits);
    assertNull("needs DefaultBrokerService in the list", result);
    inits.add("DefaultBroker");
    result = householdCustomerInitializationService.initialize(comp, inits);
    assertEquals("failure return value", "fail", result);
    householdCustomerInitializationService.setDefaults();
  }

  @Test
  public void testServiceInitialization ()
  {
    initializeService();
    assertEquals("Two Consumers Created", 2, householdCustomerService.getVillageList().size());
    for (Village customer : householdCustomerService.getVillageList()) {
      // System.out.println(tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff().toString());
      assertFalse(customer.toString() + " subscribed", tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()) == null);
      assertEquals("customer on DefaultTariff", tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION), tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(0)
          .getTariff());
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPowerConsumption ()
  {
    initializeService();
    timeService.setCurrentTime(now.plus(TimeService.HOUR));
    householdCustomerService.activate(timeService.getCurrentTime(), 1);
    for (Village customer : householdCustomerService.getVillageList()) {
      assertFalse("Household consumed power",
          tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()) == null
              || tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTotalUsage() == 0);
    }

    Object temp = ReflectionTestUtils.getField(accountingService, "pendingTransactions");
    List<TariffTransaction> temp2 = (List<TariffTransaction>) temp;
    int transactions = temp2.size();

    assertEquals("Tariff Transactions Created", 2 * householdCustomerService.getVillageList().size(), transactions);

  }

  @Test
  public void changeSubscription ()
  {
    initializeService();

    Rate r2 = new Rate().withValue(-0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(2 * TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + 3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();

    assertEquals("Four tariffs", 4, tariffRepo.findAllTariffs().size());

    for (Village customer : householdCustomerService.getVillageList()) {
      customer.changeSubscription(tariffMarketService.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));
      assertFalse("Changed from default tariff",
          tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(1).getTariff() == tariffMarketService.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

      // Changing back from the new tariff to the default one in order to check every
      // changeSubscription Method
      Tariff lastTariff = tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(1).getTariff();

      customer.changeSubscription(lastTariff, tariffMarketService.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

      assertTrue("Changed from default tariff",
          tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff() == tariffMarketService.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

      // Last changeSubscription Method checked
      customer.changeSubscription(tariffMarketService.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)), lastTariff, 5);

      assertFalse("Changed from default tariff",
          tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(1).getTariff() == tariffMarketService.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

    }
  }

  @Test
  public void revokeSubscription ()
  {
    initializeService();

    for (Village customer : householdCustomerService.getVillageList()) {
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());
    }

    Rate r2 = new Rate().withValue(-0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(2 * TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(3 * TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);

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

    for (Village customer : householdCustomerService.getVillageList()) {
      TariffSubscription tsd = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION), customer.getCustomerInfo());
      customer.unsubscribe(tsd, 3);
      customer.subscribe(tariff1, 3);
      customer.subscribe(tariff2, 3);
      customer.subscribe(tariff3, 4);
      TariffSubscription ts1 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff1, customer.getCustomerInfo());
      customer.unsubscribe(ts1, 2);
      TariffSubscription ts2 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff2, customer.getCustomerInfo());
      customer.unsubscribe(ts2, 1);
      TariffSubscription ts3 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff3, customer.getCustomerInfo());
      customer.unsubscribe(ts3, 2);
      assertEquals("4 Subscriptions for customer", 4, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());
      timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    }

    TariffRevoke tex = new TariffRevoke(tariff2.getBroker(), tariff2.getTariffSpec());
    TariffStatus status = tariffMarketService.processTariff(tex);
    assertNotNull("non-null status", status);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertTrue("tariff revoked", tariff2.isRevoked());
    // should now be just two active tariffs
    assertEquals("3 consumption tariffs", 3, tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION).size());

    for (Village customer : householdCustomerService.getVillageList()) {
      // retrieve revoked-subscription list
      TariffSubscription ts2 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff2, customer.getCustomerInfo());
      List<TariffSubscription> revokedCustomer = tariffSubscriptionRepo.getRevokedSubscriptionList(customer.getCustomerInfo());
      assertEquals("one item in list", 1, revokedCustomer.size());
      assertEquals("it's the correct one", ts2, revokedCustomer.get(0));
    }

    householdCustomerService.activate(timeService.getCurrentTime(), 1);
    for (Village customer : householdCustomerService.getVillageList()) {
      assertEquals("3 Subscriptions for customer", 3, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());
    }

    TariffRevoke tex2 = new TariffRevoke(tariff3.getBroker(), tariff3.getTariffSpec());
    TariffStatus status2 = tariffMarketService.processTariff(tex2);
    assertNotNull("non-null status", status2);
    assertEquals("success", TariffStatus.Status.success, status2.getStatus());
    assertTrue("tariff revoked", tariff3.isRevoked());
    // should now be just two active tariffs
    assertEquals("2 consumption tariffs", 2, tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION).size());

    for (Village customer : householdCustomerService.getVillageList()) {
      // retrieve revoked-subscription list
      TariffSubscription ts3 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff3, customer.getCustomerInfo());
      List<TariffSubscription> revokedCustomer = tariffSubscriptionRepo.getRevokedSubscriptionList(customer.getCustomerInfo());
      assertEquals("one item in list", 1, revokedCustomer.size());
      assertEquals("it's the correct one", ts3, revokedCustomer.get(0));
    }

    householdCustomerService.activate(timeService.getCurrentTime(), 1);
    for (Village customer : householdCustomerService.getVillageList()) {
      assertEquals("2 Subscriptions for customer", 2, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());
    }
  }

  @Test
  public void testTariffPublication ()
  {
    // test competitionControl registration
    MockCC mockCC = new MockCC(tariffMarketService, 2);
    assertEquals("correct thing", tariffMarketService, mockCC.processor);
    assertEquals("correct phase", 2, mockCC.timeslotPhase);

    //Instant start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    initializeService();

    // current time is noon. Set pub interval to 3 hours.
    ReflectionTestUtils.setField(tariffMarketService, "publicationInterval", 3);
    assertEquals("newTariffs list has only default tariff", 1, tariffRepo.findTariffsByState(Tariff.State.PENDING).size());

    Object newtemp = ReflectionTestUtils.getField(tariffMarketService, "registrations");
    List<NewTariffListener> newtemp2 = (List<NewTariffListener>) newtemp;
    int listeners = newtemp2.size();

    assertEquals("one registration", 1, listeners);
    assertEquals("no tariffs at 12:00", 0, householdCustomerService.publishedTariffs.size());

    // publish some tariffs over a period of three hours, check for publication
    TariffSpecification tsc1 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(-0.222));
    tariffMarketService.processTariff(tsc1);
    TariffSpecification tsc1a = new TariffSpecification(broker1,
                                                        PowerType.CONSUMPTION)
        .withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(-0.223));
    tariffMarketService.processTariff(tsc1a);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 13:00
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals("no tariffs at 13:00", 0, householdCustomerService.publishedTariffs.size());

    TariffSpecification tsc2 = new TariffSpecification(broker2,
                                                       PowerType.CONSUMPTION)
        .withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(-0.222));
    tariffMarketService.processTariff(tsc2);
    TariffSpecification tsc3 = new TariffSpecification(broker2,
                                                       PowerType.CONSUMPTION)
        .withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(-0.222));
    tariffMarketService.processTariff(tsc3);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 14:00
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals("no tariffs at 14:00", 0, householdCustomerService.publishedTariffs.size());

    TariffSpecification tsp1 = new TariffSpecification(broker1,
                                                       PowerType.PRODUCTION)
        .withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.119));
    TariffSpecification tsp2 = new TariffSpecification(broker1,
                                                       PowerType.PRODUCTION)
         .withExpiration(exp)
         .withMinDuration(TimeService.WEEK * 8)
         .addRate(new Rate().withValue(0.119));
    tariffMarketService.processTariff(tsp1);
    tariffMarketService.processTariff(tsp2);
    assertEquals("seven tariffs", 7, tariffRepo.findAllTariffs().size());

    Tariff tariff = tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION);

    TariffRevoke tex = new TariffRevoke(tariff.getBroker(), tariff.getTariffSpec());
    tariffMarketService.processTariff(tex);

    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 15:00 - time to publish
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals("6 tariffs at 15:00", 6, householdCustomerService.publishedTariffs.size());
    List<Tariff> pendingTariffs = tariffRepo.findTariffsByState(Tariff.State.PENDING);
    assertEquals("newTariffs list is again empty", 0, pendingTariffs.size());

  }

  @Test
  public void testEvaluatingTariffs ()
  {
    initializeService();

    Rate r2 = new Rate().withValue(-0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(2 * TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(3 * TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);

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

    List<Tariff> tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals("4 consumption tariffs", 4, tclist.size());

    for (Village customer : householdCustomerService.getVillageList()) {
      customer.possibilityEvaluationNewTariffs(tclist);
    }
  }

  @Test
  public void testDailyShifting ()
  {
    initializeService();
    // for (int i = 0; i < 10; i++) {

    Rate r0 = new Rate().withValue(-Math.random())
        .withDailyBegin(0).withDailyEnd(0);
    Rate r1 = new Rate().withValue(-Math.random())
        .withDailyBegin(1).withDailyEnd(1);
    Rate r2 = new Rate().withValue(-Math.random())
        .withDailyBegin(2).withDailyEnd(2);
    Rate r3 = new Rate().withValue(-Math.random())
        .withDailyBegin(3).withDailyEnd(3);
    Rate r4 = new Rate().withValue(-Math.random())
        .withDailyBegin(4).withDailyEnd(4);
    Rate r5 = new Rate().withValue(-Math.random())
        .withDailyBegin(5).withDailyEnd(5);
    Rate r6 = new Rate().withValue(-Math.random())
        .withDailyBegin(6).withDailyEnd(6);
    Rate r7 = new Rate().withValue(-Math.random())
        .withDailyBegin(7).withDailyEnd(7);
    Rate r8 = new Rate().withValue(-Math.random())
        .withDailyBegin(8).withDailyEnd(8);
    Rate r9 = new Rate().withValue(-Math.random())
        .withDailyBegin(9).withDailyEnd(9);
    Rate r10 = new Rate().withValue(-Math.random())
        .withDailyBegin(10).withDailyEnd(10);
    Rate r11 = new Rate().withValue(-Math.random())
        .withDailyBegin(11).withDailyEnd(11);
    Rate r12 = new Rate().withValue(-Math.random())
        .withDailyBegin(12).withDailyEnd(12);
    Rate r13 = new Rate().withValue(-Math.random())
        .withDailyBegin(13).withDailyEnd(13);
    Rate r14 = new Rate().withValue(-Math.random())
        .withDailyBegin(14).withDailyEnd(14);
    Rate r15 = new Rate().withValue(-Math.random())
        .withDailyBegin(15).withDailyEnd(15);
    Rate r16 = new Rate().withValue(-Math.random())
        .withDailyBegin(16).withDailyEnd(16);
    Rate r17 = new Rate().withValue(-Math.random())
        .withDailyBegin(17).withDailyEnd(17);
    Rate r18 = new Rate().withValue(-Math.random())
        .withDailyBegin(18).withDailyEnd(18);
    Rate r19 = new Rate().withValue(-Math.random())
        .withDailyBegin(19).withDailyEnd(19);
    Rate r20 = new Rate().withValue(-Math.random())
        .withDailyBegin(20).withDailyEnd(20);
    Rate r21 = new Rate().withValue(-Math.random())
        .withDailyBegin(21).withDailyEnd(21);
    Rate r22 = new Rate().withValue(-Math.random())
        .withDailyBegin(22).withDailyEnd(22);
    Rate r23 = new Rate().withValue(-Math.random())
        .withDailyBegin(23).withDailyEnd(23);

    TariffSpecification tsc1 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(TimeService.DAY))
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

    assertNotNull("first tariff found", tariff1);

    List<Tariff> tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    for (Village customer : householdCustomerService.getVillageList()) {
      customer.possibilityEvaluationNewTariffs(tclist);
    }
    // }
    timeService.setBase(now.getMillis());
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 23));
    householdCustomerService.activate(timeService.getCurrentTime(), 1);
  }

  class MockCC implements CompetitionControl
  {

    public MockCC (TimeslotPhaseProcessor thing, int phase)
    {
      processor = thing;
      timeslotPhase = phase;
    }

    TimeslotPhaseProcessor processor;
    int timeslotPhase;

    public void registerTimeslotPhase (TimeslotPhaseProcessor thing, int phase)
    {
      processor = thing;
      timeslotPhase = phase;
    }

    public boolean isBootstrapMode ()
    {
      return false;
    }

    public void receiveMessage (PauseRequest msg)
    {
    }

    public void receiveMessage (PauseRelease msg)
    {
    }

    @Override
    public void preGame ()
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void setAuthorizedBrokerList (ArrayList<String> brokerList)
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void runOnce ()
    {
      // TODO Auto-generated method stub
      
    }
  }

}
