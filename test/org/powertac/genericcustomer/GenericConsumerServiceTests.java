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
package org.powertac.genericcustomer;

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
import org.powertac.tariffmarket.TariffMarketInitializationService;
import org.powertac.tariffmarket.TariffMarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Antonios Chrysopoulos
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:test/test-config.xml" })
public class GenericConsumerServiceTests
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
  private GenericConsumerService genericConsumerService;

  @Autowired
  private GenericConsumerInitializationService genericConsumerInitializationService;

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
  private DateTime now;
  private TariffSpecification defaultTariffSpec;
  private Competition comp;

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
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
    comp = Competition.newInstance("generic-customer-test");

    broker1 = new Broker("Joe");
    broker2 = new Broker("Anna");

    now = new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC);
    timeService.setCurrentTime(now.toInstant());
    timeService.setBase(now.getMillis());
    exp = new Instant(now.getMillis() + TimeService.WEEK * 10);

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
    // GenericConsumerService.shutDown();
  }

  public void initializeService ()
  {
    genericConsumerInitializationService.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("GenericConsumer");
    config.getConfiguration().put("population", "100");
    config.getConfiguration().put("numberOfConsumers", "2");
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    genericConsumerInitializationService.initialize(comp, inits);
  }

  @Test
  public void testNormalInitialization ()
  {
    genericConsumerInitializationService.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("GenericConsumer");
    config.getConfiguration().put("population", "100");
    config.getConfiguration().put("numberOfConsumers", "2");
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    String result = genericConsumerInitializationService.initialize(comp, inits);
    assertEquals("correct return value", "GenericConsumer", result);
    assertEquals("correct population", 100, genericConsumerService.getPopulation());
    assertEquals("correct number of consumers", 2, genericConsumerService.getNumberOfConsumers());
  }

  @Test
  public void testBogusInitialization ()
  {
    PluginConfig config = pluginConfigRepo.findByRoleName("GenericConsumer");
    assertNull("config not created", config);
    List<String> inits = new ArrayList<String>();
    String result = genericConsumerInitializationService.initialize(comp, inits);
    assertNull("needs DefaultBrokerService in the list", result);
    inits.add("DefaultBroker");
    result = genericConsumerInitializationService.initialize(comp, inits);
    assertEquals("failure return value", "fail", result);
    genericConsumerInitializationService.setDefaults();
  }

  @Test
  public void testServiceInitialization ()
  {
    initializeService();
    assertEquals("Two Consumers Created", 2, genericConsumerService.getGenericConsumersList().size());
    for (GenericConsumer consumer : genericConsumerService.getGenericConsumersList()) {
      // System.out.println(tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).get(0).getTariff().toString());
      assertFalse(consumer.toString() + " subscribed", tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()) == null);
      assertEquals("customer on DefaultTariff", tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION), tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).get(0)
          .getTariff());
    }
  }

  @Test
  public void testPowerConsumption ()
  {
    initializeService();
    timeService.setCurrentTime(new Instant(now.getMillis() + (TimeService.HOUR)));
    genericConsumerService.activate(timeService.getCurrentTime(), 1);
    for (GenericConsumer consumer : genericConsumerService.getGenericConsumersList()) {
      assertFalse("Consumer consumed power",
          tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()) == null
              || tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).get(0).getTotalUsage() == 0);
    }

    Object temp = ReflectionTestUtils.getField(accountingService, "pendingTransactions");
    List<TariffTransaction> temp2 = (List<TariffTransaction>) temp;
    int transactions = temp2.size();

    assertEquals("Tariff Transactions Created", 2 * genericConsumerService.getGenericConsumersList().size(), transactions);

  }

  @Test
  public void changeSubscription ()
  {
    initializeService();

    Rate r2 = new Rate().withValue(0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + 2 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8)
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

    for (GenericConsumer consumer : genericConsumerService.getGenericConsumersList()) {
      consumer.changeSubscription(tariffMarketService.getDefaultTariff(consumer.getCustomerInfo().getPowerTypes().get(0)));
      assertFalse("Changed from default tariff",
          tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).get(1).getTariff() == tariffMarketService.getDefaultTariff(consumer.getCustomerInfo().getPowerTypes().get(0)));

      // Changing back from the new tariff to the default one in order to check every
      // changeSubscription Method
      Tariff lastTariff = tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).get(1).getTariff();

      consumer.changeSubscription(lastTariff, tariffMarketService.getDefaultTariff(consumer.getCustomerInfo().getPowerTypes().get(0)));

      assertTrue("Changed from default tariff",
          tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).get(0).getTariff() == tariffMarketService.getDefaultTariff(consumer.getCustomerInfo().getPowerTypes().get(0)));

      // Last changeSubscription Method checked
      consumer.changeSubscription(tariffMarketService.getDefaultTariff(consumer.getCustomerInfo().getPowerTypes().get(0)), lastTariff, 5);

      assertFalse("Changed from default tariff",
          tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).get(1).getTariff() == tariffMarketService.getDefaultTariff(consumer.getCustomerInfo().getPowerTypes().get(0)));

    }
  }

  @Test
  public void revokeSubscription ()
  {
    initializeService();

    for (GenericConsumer consumer : genericConsumerService.getGenericConsumersList()) {
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).size());
    }

    Rate r2 = new Rate().withValue(0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + 2 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + 3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8)
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

    for (GenericConsumer consumer : genericConsumerService.getGenericConsumersList()) {
      TariffSubscription tsd = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION), consumer.getCustomerInfo());
      consumer.unsubscribe(tsd, 3);
      consumer.subscribe(tariff1, 3);
      consumer.subscribe(tariff2, 3);
      consumer.subscribe(tariff3, 4);
      TariffSubscription ts1 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff1, consumer.getCustomerInfo());
      consumer.unsubscribe(ts1, 2);
      TariffSubscription ts2 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff2, consumer.getCustomerInfo());
      consumer.unsubscribe(ts2, 1);
      TariffSubscription ts3 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff3, consumer.getCustomerInfo());
      consumer.unsubscribe(ts3, 2);
      assertEquals("4 Subscriptions for consumer", 4, tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).size());
      timeService.setCurrentTime(new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR));
    }

    TariffRevoke tex = new TariffRevoke(tariff2.getBroker(), tariff2.getTariffSpec());
    TariffStatus status = tariffMarketService.processTariff(tex);
    assertNotNull("non-null status", status);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertTrue("tariff revoked", tariff2.isRevoked());
    // should now be just two active tariffs
    assertEquals("3 consumption tariffs", 3, tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION).size());

    for (GenericConsumer consumer : genericConsumerService.getGenericConsumersList()) {
      // retrieve revoked-subscription list
      TariffSubscription ts2 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff2, consumer.getCustomerInfo());
      List<TariffSubscription> revokedCustomer = tariffSubscriptionRepo.getRevokedSubscriptionList(consumer.getCustomerInfo());
      assertEquals("one item in list", 1, revokedCustomer.size());
      assertEquals("it's the correct one", ts2, revokedCustomer.get(0));
    }

    genericConsumerService.activate(timeService.getCurrentTime(), 1);
    for (GenericConsumer consumer : genericConsumerService.getGenericConsumersList()) {
      assertEquals("3 Subscriptions for consumer", 3, tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).size());
    }

    TariffRevoke tex2 = new TariffRevoke(tariff3.getBroker(), tariff3.getTariffSpec());
    TariffStatus status2 = tariffMarketService.processTariff(tex2);
    assertNotNull("non-null status", status2);
    assertEquals("success", TariffStatus.Status.success, status2.getStatus());
    assertTrue("tariff revoked", tariff3.isRevoked());
    // should now be just two active tariffs
    assertEquals("2 consumption tariffs", 2, tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION).size());

    for (GenericConsumer consumer : genericConsumerService.getGenericConsumersList()) {
      // retrieve revoked-subscription list
      TariffSubscription ts3 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff3, consumer.getCustomerInfo());
      List<TariffSubscription> revokedCustomer = tariffSubscriptionRepo.getRevokedSubscriptionList(consumer.getCustomerInfo());
      assertEquals("one item in list", 1, revokedCustomer.size());
      assertEquals("it's the correct one", ts3, revokedCustomer.get(0));
    }

    genericConsumerService.activate(timeService.getCurrentTime(), 1);
    for (GenericConsumer consumer : genericConsumerService.getGenericConsumersList()) {
      assertEquals("2 Subscriptions for consumer", 2, tariffSubscriptionRepo.findSubscriptionsForCustomer(consumer.getCustomerInfo()).size());
    }
  }

  @Test
  public void testTariffPublication ()
  {
    // test competitionControl registration
    MockCC mockCC = new MockCC(tariffMarketService, 2);
    assertEquals("correct thing", tariffMarketService, mockCC.processor);
    assertEquals("correct phase", 2, mockCC.timeslotPhase);

    Instant start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    initializeService();

    // current time is noon. Set pub interval to 3 hours.
    ReflectionTestUtils.setField(tariffMarketService, "publicationInterval", 3);
    assertEquals("newTariffs list has only default tariff", 1, tariffRepo.findTariffsByState(Tariff.State.PENDING).size());

    Object newtemp = ReflectionTestUtils.getField(tariffMarketService, "registrations");
    List<NewTariffListener> newtemp2 = (List<NewTariffListener>) newtemp;
    int listeners = newtemp2.size();

    assertEquals("one registration", 1, listeners);
    assertEquals("no tariffs at 12:00", 0, genericConsumerService.publishedTariffs.size());

    // publish some tariffs over a period of three hours, check for publication
    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(exp).withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(0.222));
    tariffMarketService.processTariff(tsc1);
    TariffSpecification tsc1a = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(exp).withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(0.223));
    tariffMarketService.processTariff(tsc1a);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 13:00
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals("no tariffs at 13:00", 0, genericConsumerService.publishedTariffs.size());

    TariffSpecification tsc2 = new TariffSpecification(broker2, PowerType.CONSUMPTION).withExpiration(exp).withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(0.222));
    tariffMarketService.processTariff(tsc2);
    TariffSpecification tsc3 = new TariffSpecification(broker2, PowerType.CONSUMPTION).withExpiration(exp).withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(0.222));
    tariffMarketService.processTariff(tsc3);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 14:00
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals("no tariffs at 14:00", 0, genericConsumerService.publishedTariffs.size());

    TariffSpecification tsp1 = new TariffSpecification(broker1, PowerType.PRODUCTION).withExpiration(exp).withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(0.119));
    TariffSpecification tsp2 = new TariffSpecification(broker1, PowerType.PRODUCTION).withExpiration(exp).withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(0.119));
    tariffMarketService.processTariff(tsp1);
    tariffMarketService.processTariff(tsp2);
    assertEquals("seven tariffs", 7, tariffRepo.findAllTariffs().size());

    Tariff tariff = tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION);

    TariffRevoke tex = new TariffRevoke(tariff.getBroker(), tariff.getTariffSpec());
    tariffMarketService.processTariff(tex);

    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 15:00 - time to publish
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals("6 tariffs at 15:00", 6, genericConsumerService.publishedTariffs.size());
    List<Tariff> pendingTariffs = tariffRepo.findTariffsByState(Tariff.State.PENDING);
    assertEquals("newTariffs list is again empty", 0, pendingTariffs.size());

  }

  @Test
  public void testEvaluatingTariffs ()
  {
    initializeService();

    Rate r2 = new Rate().withValue(0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + 2 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + 3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8)
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

    for (GenericConsumer consumer : genericConsumerService.getGenericConsumersList()) {
      consumer.possibilityEvaluationNewTariffs(tclist);
    }
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

    public boolean isBootstrapMode ()
    {
      return false;
    }

    public void registerTimeslotPhase (TimeslotPhaseProcessor thing, int phase)
    {
      processor = thing;
      timeslotPhase = phase;
    }

    public void receiveMessage (PauseRequest msg)
    {
    }

    public void receiveMessage (PauseRelease msg)
    {
    }
  }

}
