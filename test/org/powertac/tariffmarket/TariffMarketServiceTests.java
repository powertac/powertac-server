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

package org.powertac.tariffmarket;

import static org.junit.Assert.*;
import static org.powertac.util.ListTools.*;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.HourlyCharge;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.AbstractCustomer;
import org.powertac.common.BrokerTransaction;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffMessage;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.msg.PauseRelease;
import org.powertac.common.msg.PauseRequest;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.VariableRateUpdate;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.TimeService;
import org.powertac.common.PluginConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/tariff-test-config.xml"})
public class TariffMarketServiceTests
{
  @Autowired
  private TimeService timeService;

  @Autowired
  private TariffMarketService tariffMarketService;
  
  @Autowired
  private Accounting accountingService;

  @Autowired
  private TariffMarketInitializationService tariffMarketInitializationService;
  
  @Autowired
  private TariffRepo tariffRepo;
  
  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;
  
  // get access to the mock services
  @Autowired
  private BrokerProxy mockProxy;
  
  private TariffSpecification tariffSpec; // instance var

  private Instant start;
  private Instant exp;
  private Broker broker;
  private List txs;
  private List msgs;
  private Competition comp;
  
  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @Before
  public void setUp ()
  {
    // Clean up from previous tests
    tariffRepo.recycle();
    timeslotRepo.recycle();
    brokerRepo.recycle();
    pluginConfigRepo.recycle();
    reset(mockProxy);

    txs = new ArrayList<BrokerTransaction>();
    msgs = new ArrayList<Object>();
    
    // create a Competition, needed for initialization
    comp = Competition.newInstance("accounting-test");

    // mock the brokerProxyService, capturing the messages sent out
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        msgs.add(args[1]);
        return null;
      }
    }).when(mockProxy).sendMessage(isA(Broker.class), anyObject());

    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        for (Object msg : (List)args[1]) {
          msgs.add(msg);
        }
        return null;
      }
    }).when(mockProxy).sendMessages(isA(Broker.class), anyList());

    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        msgs.add(args[0]);
        return null;
      }
    }).when(mockProxy).broadcastMessage(anyObject());

    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        for (Object msg : (List)args[0]) {
          msgs.add(msg);
        }
        return null;
      }
    }).when(mockProxy).broadcastMessages(anyList());
    
    // init time service
    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(start);
    
    // create useful objects, set parameters
    broker = new Broker("testBroker");
    exp = new DateTime(2011, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    tariffSpec = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.121));
  }
  
  @After
  public void shutDown()
  {
    //tariffMarketService.shutDown();
  }
  
  @Test
  public void initializeService () {
    tariffMarketInitializationService.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("TariffMarket");
    config.getConfiguration().put("tariffPublicationFee", "42.0");
    config.getConfiguration().put("tariffRevocationFee", "420.0");
    List<String> inits = new ArrayList<String>();
    inits.add("AccountingService");
    tariffMarketInitializationService.initialize(comp, inits);
  }
  
  @Test
  public void testNormalInitialization ()
  {
    tariffMarketInitializationService.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("TariffMarket");
    assertNotNull("config created correctly", config);
    List<String> inits = new ArrayList<String>();
    inits.add("AccountingService");
    String result = tariffMarketInitializationService.initialize(comp, inits);
    assertEquals("correct return value", "TariffMarket", result);
    assertEquals("correct publication fee", -100.0, tariffMarketService.getTariffPublicationFee(), 1e-6);
  }
  
  @Test
  public void testBogusInitialization ()
  {
    PluginConfig config = pluginConfigRepo.findByRoleName("TariffMarket");
    assertNull("config not created", config);
    List<String> inits = new ArrayList<String>();
    String result = tariffMarketInitializationService.initialize(comp, inits);
    assertNull("needs AccountingService in the list", result);
    inits.add("AccountingService");
    result = tariffMarketInitializationService.initialize(comp, inits);
    assertEquals("failure return value", "fail", result);
    tariffMarketInitializationService.setDefaults();
  }
  
  // bogus message, not an instance of TariffMessage
  @Test
  public void testBogusMessage1 ()
  {
    initializeService();
    tariffMarketService.receiveMessage(broker);
    assertEquals("no messages sent", 0, msgs.size());
  }
  
  // valid tariffSpec
  @Test
  public void testProcessTariffSpec ()
  {
    initializeService();
    tariffMarketService.receiveMessage(tariffSpec);
    assertEquals("one message sent", 1, msgs.size());
    TariffStatus status = (TariffStatus)msgs.get(0);
    // check the status return
    assertNotNull("non-null status", status);
    assertEquals("broker", tariffSpec.getBroker(), status.getBroker());
    assertEquals("tariff ID", tariffSpec.getId(), status.getTariffId());
    assertEquals("status ID", tariffSpec.getId(), status.getUpdateId());
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    // find and check the tariff
    assertEquals("one tariff", 1, tariffRepo.findAllTariffs().size());
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertNotNull("found a tariff", tf);
    // find and check the transaction
    verify(accountingService).addTariffTransaction(TariffTransaction.Type.PUBLISH,
                                                   tf, null, 0, 0.0, 42.0);
  }

  // bogus expiration
  @Test
  public void testProcessTariffExpireBogus ()
  {
    initializeService();
    TariffStatus status = tariffMarketService.processTariff(tariffSpec);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertEquals("Correct expiration", exp, tf.getExpiration());

    TariffSpecification unpublished = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .addRate(new Rate().withValue(0.121));
    Instant newExp = new DateTime(2011, 3, 1, 10, 0, 0, 0, DateTimeZone.UTC).toInstant();
    TariffExpire tex = new TariffExpire(tariffSpec.getBroker(), unpublished, newExp);
    status = tariffMarketService.processTariff(tex);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", tex.getId(), status.getUpdateId());
    assertEquals("No such tariff", TariffStatus.Status.noSuchTariff, status.getStatus());
    assertEquals("tariff not updated", exp, tf.getExpiration());
  }
  
  // exp time in the past
  @Test
  public void testProcessTariffExpirePast ()
  {
    initializeService();
    TariffStatus status = tariffMarketService.processTariff(tariffSpec);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertEquals("Correct expiration", exp, tf.getExpiration());
    //Instant newExp = new DateTime(2011, 3, 1, 8, 0, 0, 0, DateTimeZone.UTC).toInstant();
    //timeService.setCurrentTime(newExp);
    Instant newExp = timeService.getCurrentTime().minus(TimeService.HOUR);
    TariffExpire tex = new TariffExpire(tariffSpec.getBroker(), tariffSpec, newExp);
    status = tariffMarketService.processTariff(tex);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", tex.getId(), status.getUpdateId());
    assertEquals("invalid", TariffStatus.Status.invalidUpdate, status.getStatus());
  }

  // normal expiration
  @Test
  public void testProcessTariffExpire ()
  {
    initializeService();
    TariffStatus status = tariffMarketService.processTariff(tariffSpec);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertEquals("Correct expiration", exp, tf.getExpiration());
    Instant newExp = new DateTime(2011, 3, 1, 10, 0, 0, 0, DateTimeZone.UTC).toInstant();
    TariffExpire tex = new TariffExpire(tariffSpec.getBroker(), tariffSpec, newExp);
    status = tariffMarketService.processTariff(tex);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", tex.getId(), status.getUpdateId());
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertEquals("tariff updated", newExp, tf.getExpiration());
    assertFalse("tariff not expired", tf.isExpired());
    timeService.setCurrentTime(newExp);
    assertTrue("tariff is expired", tf.isExpired());
  }

  // TODO - bogus revoke
  
  // normal revoke
  @Test
  public void testProcessTariffRevoke ()
  {
    initializeService();
    TariffStatus status = tariffMarketService.processTariff(tariffSpec);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertFalse("not revoked", tf.isRevoked());
    TariffRevoke tex = new TariffRevoke(tariffSpec.getBroker(), tariffSpec);
    status = tariffMarketService.processTariff(tex);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", tex.getId(), status.getUpdateId());
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertTrue("tariff revoked", tf.isRevoked());
  }

  // variable rate update - nominal case, 2 tariffs
  @Test
  public void testVariableRateUpdate ()
  {
    initializeService();
    // what the broker does...
    //TariffSpecification unpublished = new TariffSpecification(broker, PowerType.CONSUMPTION)
    //.addRate(new Rate().setValue(0.121));
    TariffSpecification ts2 =
          new TariffSpecification(broker, PowerType.CONSUMPTION)
              .withExpiration(new DateTime(2011, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant())
              .withMinDuration(TimeService.WEEK * 4);
    Rate r1 = new Rate()
        .withFixed(false)
        .withMinValue(0.05)
        .withMaxValue(0.50)
        .withNoticeInterval(0)
        .withExpectedMean(0.10);
    ts2.addRate(r1);
    Instant lastHr = start.minus(TimeService.HOUR);
    r1.addHourlyCharge(new HourlyCharge(lastHr, 0.07), true);

    // send to market
    TariffStatus status1 = tariffMarketService.processTariff(tariffSpec);
    TariffStatus status2 = tariffMarketService.processTariff(ts2);

    // check the status return2
    assertNotNull("non-null status 1", status1);
    assertNotNull("non-null status 2", status2);
    assertEquals("broker ID 1", tariffSpec.getBroker(), status1.getBroker());
    assertEquals("broker ID 2", ts2.getBroker(), status2.getBroker());
    assertEquals("tariff ID 1", tariffSpec.getId(), status1.getTariffId());
    assertEquals("tariff ID 2", ts2.getId(), status2.getTariffId());
    assertEquals("status ID 1", tariffSpec.getId(), status1.getUpdateId());
    assertEquals("status ID 2", ts2.getId(), status2.getUpdateId());
    assertEquals("success 1", TariffStatus.Status.success, status1.getStatus());
    assertEquals("success 2", TariffStatus.Status.success, status2.getStatus());
    // find and check the tariffs
    assertEquals("two tariffs", 2, tariffRepo.findAllTariffs().size());
    Tariff tf1 = tariffRepo.findTariffById(tariffSpec.getId());
    Tariff tf2 = tariffRepo.findTariffById(ts2.getId());
    assertEquals("found tariff 1", tariffSpec.getId(), tf1.getId());
    assertEquals("found tariff 2", ts2.getId(), tf2.getId());
    // make sure r1 is in the repo
    assertEquals("found r1", r1, tariffRepo.findRateById(r1.getId()));
    
    // update the hourly rate on tariff 2
    HourlyCharge hc = new HourlyCharge(start, 0.09);
    VariableRateUpdate vru = new VariableRateUpdate(broker, r1, hc);
    TariffStatus vrs = tariffMarketService.processTariff(vru);
    assertNotNull("non-null vru status", vrs);
    assertEquals("success vru", TariffStatus.Status.success, vrs.getStatus());
    
    assertEquals("Correct rate at 11:00", 0.07, tf2.getUsageCharge(lastHr, 1.0, 0.0), 1e-6);
    assertEquals("Correct rate at 12:00", 0.09, tf2.getUsageCharge(start, 1.0, 0.0), 1e-6);

    // make sure both tariffs are on the output list
    // TODO - this should be replaced with a check on an output channel.
    //assertEquals("correct number of notifications queued", 3, tariffMarketService.broadcast.size())
    //assertEquals("correct first element", tariffSpec, tariffMarketService.broadcast.remove(0))
    //assertEquals("correct second element", ts2, tariffMarketService.broadcast.remove(0))
    //assertEquals("correct third element", vru, tariffMarketService.broadcast.remove(0))
  }
  
  // TODO - invalid variable-rate update

  // check evolution of active tariff list
  @Test
  public void testGetActiveTariffList ()
  {
    initializeService();
    // initially, there should be no active tariffs
    assertEquals("no initial tariffs", 0,
          tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION).size());
    // first, add multiple tariffs for more than one power type, multiple expirations
    TariffSpecification tsc1 = new TariffSpecification(broker, PowerType.CONSUMPTION)
          .withExpiration(start.plus(TimeService.DAY))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    TariffSpecification tsc2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
          .withExpiration(start.plus(TimeService.DAY * 2))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    TariffSpecification tsc3 = new TariffSpecification(broker, PowerType.CONSUMPTION)
          .withExpiration(start.plus(TimeService.DAY * 3))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    tariffMarketService.processTariff(tsc1);
    tariffMarketService.processTariff(tsc2);
    tariffMarketService.processTariff(tsc3);
    TariffSpecification tsp1 = new TariffSpecification(broker, PowerType.PRODUCTION)
          .withExpiration(start.plus(TimeService.DAY))
          .withMinDuration(TimeService.WEEK* 8)
          .addRate(new Rate().withValue(0.119));
    TariffSpecification tsp2 = new TariffSpecification(broker, PowerType.PRODUCTION)
          .withExpiration(start.plus(TimeService.DAY * 2))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.119));
    tariffMarketService.processTariff(tsp1);
    tariffMarketService.processTariff(tsp2);
    assertEquals("five tariffs", 5, tariffRepo.findAllTariffs().size());
    
    // make sure all tariffs are active
    List<Tariff> tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals("3 consumption tariffs", 3, tclist.size());
    List<Tariff> tplist = tariffMarketService.getActiveTariffList(PowerType.PRODUCTION);
    assertEquals("2 production tariffs", 2, tplist.size());
    
    // forward one day, try again
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.DAY));
    tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals("2 consumption tariffs", 2, tclist.size());
    tplist = tariffMarketService.getActiveTariffList(PowerType.PRODUCTION);
    assertEquals("1 production tariffs", 1, tplist.size());
    
    // forward another day, try again
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.DAY));
    tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals("1 consumption tariff", 1, tclist.size());
    tplist = tariffMarketService.getActiveTariffList(PowerType.PRODUCTION);
    assertEquals("no production tariffs", 0, tplist.size());
    
    // forward another day, try again
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.DAY));
    tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals("no consumption tariffs", 0, tclist.size());
  }
  
  // test batch-publication of new tariffs
  @Test
  public void testBatchPublication ()
  {
    // test competitionControl registration
    MockCC mockCC = new MockCC();
    ReflectionTestUtils.setField(tariffMarketService,
                                 "competitionControlService",
                                 mockCC);
    initializeService();
    assertEquals("correct thing", tariffMarketService, mockCC.processor);
    assertEquals("correct phase",
                 tariffMarketService.getSimulationPhase(),
                 mockCC.timeslotPhase);
        
    // current time is noon. Set pub interval to 3 hours.
    ReflectionTestUtils.setField(tariffMarketService, "publicationInterval", 3);
    assertEquals("newTariffs list is empty", 0, tariffRepo.findTariffsByState(Tariff.State.PENDING).size());
    // register a NewTariffListener 
    //List<Tariff> publishedTariffs = new ArrayList<Tariff>();
    MockTariffListener listener = new MockTariffListener();
    tariffMarketService.registerNewTariffListener(listener);
    assertEquals("one registration", 1, tariffMarketService.getRegistrations().size());
    assertEquals("no tariffs at 12:00", 0, listener.publishedTariffs.size());
    // publish some tariffs over a period of three hours, check for publication
    TariffSpecification tsc1 = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plus(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.processTariff(tsc1);
    TariffSpecification tsc1a = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plus(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.223));
    tariffMarketService.processTariff(tsc1a);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 13:00
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals("no tariffs at 13:00", 0, listener.publishedTariffs.size());
    
    TariffSpecification tsc2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plus(TimeService.DAY * 2))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.processTariff(tsc2);
    TariffSpecification tsc3 = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plus(TimeService.DAY * 3))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.processTariff(tsc3);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 14:00
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals("no tariffs at 14:00", 0, listener.publishedTariffs.size());

    TariffSpecification tsp1 = new TariffSpecification(broker, PowerType.PRODUCTION)
        .withExpiration(start.plus(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.119));
    TariffSpecification tsp2 = new TariffSpecification(broker, PowerType.PRODUCTION)
        .withExpiration(start.plus(TimeService.DAY * 2))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.119));
    tariffMarketService.processTariff(tsp1);
    tariffMarketService.processTariff(tsp2);
    assertEquals("six tariffs", 6, tariffRepo.findAllTariffs().size());
    
    TariffRevoke tex = new TariffRevoke(tsc1a.getBroker(), tsc1a);
    tariffMarketService.processTariff(tex);

    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 15:00 - time to publish
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals("5 tariffs at 15:00", 5, listener.publishedTariffs.size());
    List pendingTariffs = tariffRepo.findTariffsByState(Tariff.State.PENDING);
    assertEquals("newTariffs list is again empty", 0, pendingTariffs.size());
  }

  // create some subscriptions and then revoke a tariff
  @Test
  public void testGetRevokedSubscriptionList ()
  {
    initializeService();
    // create some tariffs
    TariffSpecification tsc1 = new TariffSpecification(broker, PowerType.CONSUMPTION)
          .withExpiration(start.plus(TimeService.DAY * 5))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    TariffSpecification tsc2 = new TariffSpecification(broker,  PowerType.CONSUMPTION)
          .withExpiration(start.plus(TimeService.DAY * 7))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    TariffSpecification tsc3 = new TariffSpecification(broker,  PowerType.CONSUMPTION)
          .withExpiration(start.plus(TimeService.DAY * 9))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    tariffMarketService.processTariff(tsc1);
    tariffMarketService.processTariff(tsc2);
    tariffMarketService.processTariff(tsc3);
    Tariff tc1 = tariffRepo.findTariffById(tsc1.getId());
    assertNotNull("first tariff found", tc1);
    Tariff tc2 = tariffRepo.findTariffById(tsc2.getId());
    assertNotNull("second tariff found", tc2);
    Tariff tc3 = tariffRepo.findTariffById(tsc3.getId());
    assertNotNull("third tariff found", tc3);
    
    // create two customers who can subscribe
    CustomerInfo charleyInfo = new CustomerInfo("Charley", 100);
    AbstractCustomer charley = new AbstractCustomer(charleyInfo);
    //charley.init();
    CustomerInfo sallyInfo = new CustomerInfo("Sally", 100);
    AbstractCustomer sally = new AbstractCustomer(sallyInfo);
    //sally.init();
	
    // make sure we have three active tariffs
    List<Tariff> tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals("3 consumption tariffs", 3, tclist.size());
    //assertEquals("three transaction", 3, TariffTransaction.count());
    
    // create some subscriptions
    TariffSubscription cs1 = tariffMarketService.subscribeToTariff(tc1, charley, 3);
    TariffSubscription cs2 = tariffMarketService.subscribeToTariff(tc2, charley, 31);
    TariffSubscription cs3 = tariffMarketService.subscribeToTariff(tc3, charley, 13);
    TariffSubscription ss1 = tariffMarketService.subscribeToTariff(tc1, sally, 4);
    TariffSubscription ss2 = tariffMarketService.subscribeToTariff(tc2, sally, 24); 
    TariffSubscription ss3 = tariffMarketService.subscribeToTariff(tc3, sally, 42);
    assertEquals("3 customers for cs1", 3, cs1.getCustomersCommitted());
    assertEquals("42 customers for ss3", 42, ss3.getCustomersCommitted());
    assertEquals("Charley has 3 subscriptions", 3, tariffSubscriptionRepo.findSubscriptionsForCustomer(charley).size());
    
    // forward an hour, revoke the second tariff
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    TariffRevoke tex = new TariffRevoke(tc2.getBroker(), tsc2);
    TariffStatus status = tariffMarketService.processTariff(tex);
    assertNotNull("non-null status", status);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertTrue("tariff revoked", tc2.isRevoked());

    // should now be just two active tariffs
    tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals("2 consumption tariffs", 2, tclist.size());

    // retrieve Charley's revoked-subscription list
    List<TariffSubscription> revokedCharley = 
      tariffMarketService.getRevokedSubscriptionList(charley);
    assertEquals("one item in list", 1, revokedCharley.size());
    assertEquals("it's cs2", cs2, revokedCharley.get(0));

    // find and check the transaction
    // TODO - we would need to mock AccountingService to get at these objects
    //assertEquals("one more transaction", 4, TariffTransaction.count());
    //TariffTransaction ttx = TariffTransaction.findByPostedTime(timeService.getCurrentTime());
    //assertNotNull("found transaction", ttx);
    //assertEquals("correct tariff", tc2.getTariffSpec(), ttx.getTariffSpec());
    //assertEquals("correct type", TariffTransactionType.REVOKE, ttx.getTxType());
    //assertEquals("correct amount", 420.0, ttx.getCharge(), 1e-6);
  }

  // check default tariffs
  @Test
  public void testGetDefaultTariff ()
  {
    initializeService();
    // set defaults for consumption and production
    TariffSpecification tsc1 = new TariffSpecification(broker,PowerType.CONSUMPTION) 
          .withExpiration(start.plus(TimeService.WEEK))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    assertTrue("add consumption default", tariffMarketService.setDefaultTariff(tsc1));
    TariffSpecification tsp1 = new TariffSpecification(broker, PowerType.PRODUCTION) 
          .withExpiration(start.plus(TimeService.WEEK))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.122));
    assertTrue("add production default", tariffMarketService.setDefaultTariff(tsp1));
    
    // find the resulting tariffs
    Tariff tc1 = tariffRepo.findTariffById(tsc1.getId());
    assertNotNull("consumption tariff found", tc1);
    assertEquals("correct consumption tariff", tsc1.getId(), tc1.getSpecId());
    Tariff tp1 = tariffRepo.findTariffById(tsp1.getId());
    assertNotNull("production tariff found", tp1);
    assertEquals("correct production tariff", tsp1.getId(), tp1.getSpecId());

    // retrieve and check the defaults
    assertEquals("default consumption tariff", tc1, tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION));
    assertEquals("default production tariff", tp1, tariffMarketService.getDefaultTariff(PowerType.PRODUCTION));
    assertNull("no solar tariff", tariffMarketService.getDefaultTariff(PowerType.SOLAR_PRODUCTION));
  }
  
  class MockCC implements CompetitionControl
  {
    TimeslotPhaseProcessor processor;
    int timeslotPhase;

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
  
  class MockTariffListener implements NewTariffListener
  {
    List<Tariff> publishedTariffs = new ArrayList<Tariff>();
    
    public void publishNewTariffs (List<Tariff> tariffs)
    {
      publishedTariffs = tariffs;
    }   
  }
}
