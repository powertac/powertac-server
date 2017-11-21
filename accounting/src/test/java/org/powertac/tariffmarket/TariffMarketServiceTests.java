/*
 * Copyright 2011-2017 the original author or authors.
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
//import static org.powertac.util.ListTools.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.HourlyCharge;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.RegulationRate;
import org.powertac.common.Tariff;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.VariableRateUpdate;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:tariff-test-config.xml"})
@DirtiesContext
public class TariffMarketServiceTests
{
  @Autowired
  private TimeService timeService;

  @Autowired
  private TariffMarketService tariffMarketService;
  
  @Autowired
  private Accounting accountingService;
  
  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  private BrokerRepo brokerRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  // get access to the mock services
  @Autowired
  private BrokerProxy mockProxy;
  
  @Autowired
  private ServerConfiguration mockServerProperties;
  
  private TariffSpecification tariffSpec; // instance var

  private Instant start;
  private Instant exp;
  private Broker broker;
  //private List<Object> txs;
  private List<Object> msgs;
  private Competition comp;
  private Configurator config;

  @SuppressWarnings("rawtypes")
  @Before
  public void setUp ()
  {
    // Clean up from previous tests
    tariffRepo.recycle();
    tariffSubscriptionRepo.recycle();
    //timeslotRepo.recycle();
    brokerRepo.recycle();
    reset(mockProxy);
    reset(accountingService);
    reset(mockServerProperties);

    //txs = new ArrayList<BrokerTransaction>();
    msgs = new ArrayList<Object>();
    
    // create a Competition, needed for initialization
    comp = Competition.newInstance("tariff-market-test");

    // mock the brokerProxyService, capturing the messages sent out
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        msgs.add(args[1]);
        return null;
      }
    }).when(mockProxy).sendMessage(isA(Broker.class), any());

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        for (Object msg : (List)args[1]) {
          msgs.add(msg);
        }
        return null;
      }
    }).when(mockProxy).sendMessages(isA(Broker.class), anyList());

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        msgs.add(args[0]);
        return null;
      }
    }).when(mockProxy).broadcastMessage(any());

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        for (Object msg : (List)args[0]) {
          msgs.add(msg);
        }
        return null;
      }
    }).when(mockProxy).broadcastMessages(anyList());

    // Set up serverProperties mock
    config = new Configurator();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(mockServerProperties).configureMe(any());
    
    // init time service
    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(start);
    timeslotRepo.makeTimeslot(start);
    
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
  public void initializeService () 
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("tariffmarket.tariffMarketService.publicationFee", "-42.0");
    map.put("tariffmarket.tariffMarketService.revocationFee", "-420.0");
    map.put("tariffmarket.tariffMarketService.publicationInterval", "3");
    map.put("tariffmarket.tariffMarketService.publicationOffset", "0");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    List<String> inits = new ArrayList<String>();
    inits.add("AccountingService");
    tariffMarketService.initialize(comp, inits);
    assertEquals("correct publication fee", -42.0,
                 tariffMarketService.getPublicationFee(), 1e-6);

  }
  
  @Test
  public void testNormalInitialization ()
  {
    List<String> inits = new ArrayList<String>();
    inits.add("AccountingService");
    String result = tariffMarketService.initialize(comp, inits);
    assertEquals("correct return value", "TariffMarket", result);
    assertTrue("correct publication fee",
               (-100.0 >= tariffMarketService.getPublicationFee() &&
               tariffMarketService.getPublicationFee() >= -500.0));
  }

  @Test
  public void testBogusInitialization ()
  {
    List<String> inits = new ArrayList<String>();
    String result = tariffMarketService.initialize(comp, inits);
    assertNull("needs AccountingService in the list", result);
    inits.add("AccountingService");
  }

  // valid tariffSpec
  @Test
  public void testProcessTariffSpec ()
  {
    initializeService();
    tariffMarketService.handleMessage(tariffSpec);
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
                                                   tf, null, 0, 0.0, -42.0);
  }

  // bogus expiration
  @Test
  public void testProcessTariffExpireBogus ()
  {
    initializeService();
    tariffMarketService.handleMessage(tariffSpec);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertEquals("Correct expiration", exp, tf.getExpiration());

    TariffSpecification unpublished = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .addRate(new Rate().withValue(0.121));
    Instant newExp = new DateTime(2011, 3, 1, 10, 0, 0, 0, DateTimeZone.UTC).toInstant();
    TariffExpire tex = new TariffExpire(tariffSpec.getBroker(), unpublished, newExp);
    tariffMarketService.handleMessage(tex);
    status = (TariffStatus)msgs.get(1);
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
    tariffMarketService.handleMessage(tariffSpec);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertEquals("Correct expiration", exp, tf.getExpiration());
    //Instant newExp = new DateTime(2011, 3, 1, 8, 0, 0, 0, DateTimeZone.UTC).toInstant();
    //timeService.setCurrentTime(newExp);
    Instant newExp = timeService.getCurrentTime().minus(TimeService.HOUR);
    TariffExpire tex = new TariffExpire(tariffSpec.getBroker(), tariffSpec, newExp);
    tariffMarketService.handleMessage(tex);
    status = (TariffStatus)msgs.get(1);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", tex.getId(), status.getUpdateId());
    assertEquals("invalid", TariffStatus.Status.invalidUpdate, status.getStatus());
  }

  // normal expiration
  @Test
  public void testProcessTariffExpire ()
  {
    initializeService();
    tariffMarketService.handleMessage(tariffSpec);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertEquals("Correct expiration", exp, tf.getExpiration());
    Instant newExp = new DateTime(2011, 3, 1, 10, 0, 0, 0, DateTimeZone.UTC).toInstant();
    TariffExpire tex = new TariffExpire(tariffSpec.getBroker(), tariffSpec, newExp);
    tariffMarketService.handleMessage(tex);
    status = (TariffStatus)msgs.get(1);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", tex.getId(), status.getUpdateId());
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertEquals("tariff updated", newExp, tf.getExpiration());
    assertFalse("tariff not expired", tf.isExpired());
    timeService.setCurrentTime(newExp);
    assertTrue("tariff is expired", tf.isExpired());
  }

  // bogus supersede
  @Test
  public void bogusTariffSupersede ()
  {
    initializeService();
    Broker b2 = new Broker("otherBroker");
    TariffSpecification spec2 =
      new TariffSpecification(b2, PowerType.CONSUMPTION).withExpiration(exp)
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.121));

    // spec2 supersedes tariffSpec??
    spec2.addSupersedes(tariffSpec.getId());
    tariffMarketService.handleMessage(tariffSpec);
    tariffMarketService.handleMessage(spec2);

    assertEquals("one message sent", 2, msgs.size());
    TariffStatus status = (TariffStatus)msgs.get(0);
    // check the status return
    assertNotNull("non-null status 0", status);
    assertEquals("broker", tariffSpec.getBroker(), status.getBroker());
    assertEquals("success", TariffStatus.Status.success, status.getStatus());

    status = (TariffStatus) msgs.get(1);
    assertNotNull("non-null status 1", status);
    assertEquals("broker 2", b2, status.getBroker());
    assertEquals("invalid", TariffStatus.Status.invalidTariff,
                 status.getStatus());
  }

  // TOU rate with gap should be invalid
  @Test
  public void gapTimeOfDay ()
  {
    initializeService();
    TariffSpecification ts2 =
            new TariffSpecification(broker, PowerType.CONSUMPTION)
                .withMinDuration(TimeService.WEEK * 4);
    Rate r1 = new Rate()
          .withFixed(true)
          .withMinValue(0.1)
          .withDailyBegin(23)
          .withDailyEnd(5);
    ts2.addRate(r1);
    Rate r2 = new Rate()
    .withFixed(true)
    .withMinValue(0.2)
    .withDailyBegin(7)
    .withDailyEnd(22);
    ts2.addRate(r2);
    tariffMarketService.handleMessage(ts2);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", ts2.getId(), status.getUpdateId());
    assertEquals("invalid", TariffStatus.Status.invalidTariff, status.getStatus());
    assertEquals("only one message received", 1, msgs.size());
  }

  // TOU rate without gap should be valid
  @Test
  public void coveredTimeOfDay ()
  {
    initializeService();
    TariffSpecification ts2 =
            new TariffSpecification(broker, PowerType.CONSUMPTION)
                .withMinDuration(TimeService.WEEK * 4);
    Rate r1 = new Rate()
          .withFixed(true)
          .withMinValue(0.1)
          .withDailyBegin(23)
          .withDailyEnd(6);
    ts2.addRate(r1);
    Rate r2 = new Rate()
    .withFixed(true)
    .withMinValue(0.2)
    .withDailyBegin(7)
    .withDailyEnd(22);
    ts2.addRate(r2);
    tariffMarketService.handleMessage(ts2);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", ts2.getId(), status.getUpdateId());
    assertEquals("valid", TariffStatus.Status.success, status.getStatus());
  }

  // normal revoke
  @Test
  public void testProcessTariffRevoke ()
  {
    initializeService();
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    tariffMarketService.handleMessage(tariffSpec);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertFalse("not revoked", tf.isRevoked());
    TariffRevoke tex = new TariffRevoke(tariffSpec.getBroker(), tariffSpec);
    tariffMarketService.handleMessage(tex);
    status = (TariffStatus)msgs.get(1);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", tex.getId(), status.getUpdateId());
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertFalse("tariff not yet revoked", tf.isRevoked());

    // forward an hour and activate, still should not be revoked.
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertFalse("tariff not yet revoked", tf.isRevoked());

    // forward two more hours and activate, should do the trick
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 2));
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertTrue("tariff revoked", tf.isRevoked());
    TariffRevoke revoke = (TariffRevoke)msgs.get(2);
    assertNotNull("revoke msg non-null", revoke);
    assertEquals("correct broker", tariffSpec.getBroker(), revoke.getBroker());
    assertEquals("correct tariff", tariffSpec.getId(), revoke.getTariffId());
  }

  // bogus revoke -- wrong broker
  @Test
  public void bogusTariffRevoke ()
  {
    initializeService();
    Broker b2 = new Broker("otherBroker");
    TariffSpecification spec2 =
      new TariffSpecification(b2, PowerType.CONSUMPTION).withExpiration(exp)
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.121));
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    tariffMarketService.handleMessage(tariffSpec);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());

    tariffMarketService.handleMessage(spec2);
    TariffStatus status2 = (TariffStatus)msgs.get(1);
    assertEquals("success", TariffStatus.Status.success, status2.getStatus());

    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    Tariff tf2 = tariffRepo.findTariffById(spec2.getId());
    assertFalse("s1 not revoked", tf.isRevoked());
    assertFalse("s2 not revoked", tf2.isRevoked());

    // revoke the otherBroker's tariff
    TariffRevoke tex = new TariffRevoke(tariffSpec.getBroker(), spec2);
    tariffMarketService.handleMessage(tex);
    status = (TariffStatus) msgs.get(2);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", tex.getId(), status.getUpdateId());
    assertEquals("invalid", TariffStatus.Status.invalidTariff,
                 status.getStatus());
    assertFalse("tariff 1 not yet revoked", tf.isRevoked());
    assertFalse("tariff 2 not yet revoked", tf2.isRevoked());

    // forward an hour and activate, still should not be revoked.
    timeService.setCurrentTime(timeService.getCurrentTime()
        .plus(TimeService.HOUR));
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertFalse("tariff 1 not yet revoked", tf.isRevoked());
    assertFalse("tariff 2 not yet revoked", tf2.isRevoked());

    // forward two more hours and activate, should do the trick
    timeService.setCurrentTime(timeService.getCurrentTime()
        .plus(TimeService.HOUR * 2));
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertFalse("tariff 1 not revoked", tf.isRevoked());
    assertFalse("tariff 2 not revoked", tf2.isRevoked());
    Object revoke = msgs.get(3);
    assertTrue("no revoke msg",
               (null == revoke || !(revoke instanceof TariffRevoke)));
  }

  // variable rate update - nominal case, 2 tariffs
  @Test
  public void testVariableRateUpdate ()
  {
    initializeService();
    // what the broker does...
    TariffSpecification ts2 =
          new TariffSpecification(broker, PowerType.CONSUMPTION)
              .withExpiration(new DateTime(2011, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant())
              .withMinDuration(TimeService.WEEK * 4);
    Rate r1 = new Rate()
        .withFixed(false)
        .withMinValue(-0.05)
        .withMaxValue(-0.50)
        .withNoticeInterval(0)
        .withExpectedMean(-0.10);
    ts2.addRate(r1);
    Instant lastHr = start.minus(TimeService.HOUR);
    r1.addHourlyCharge(new HourlyCharge(lastHr, -0.07), true);    

    // send to market
    tariffMarketService.handleMessage(tariffSpec);
    tariffMarketService.handleMessage(ts2);
    TariffStatus status1 = (TariffStatus)msgs.get(0);
    TariffStatus status2 = (TariffStatus)msgs.get(1);

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
    HourlyCharge hc = new HourlyCharge(start, -0.09);
    VariableRateUpdate vru = new VariableRateUpdate(broker, r1, hc);

    // check for correct times
    assertEquals("correct hc start", start, hc.getAtTime());
    assertEquals("correct current time", start, timeService.getCurrentTime());
    
    int msgsize = msgs.size();
    tariffMarketService.handleMessage(vru);
    assertEquals("no messages yet", msgsize, msgs.size());

    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertEquals("one new message", msgsize + 3, msgs.size());

    TariffStatus vrs = (TariffStatus)msgs.get(msgsize - 1);
    assertNotNull("non-null vru status", vrs);
    assertEquals("success vru", TariffStatus.Status.success, vrs.getStatus());
    assertEquals("correct current time", start, timeService.getCurrentTime());
    
    assertEquals("Correct rate at 11:00", -0.07, tf2.getUsageCharge(lastHr, 1.0, 0.0), 1e-6);
    assertEquals("Correct rate at 12:00", -0.09, tf2.getUsageCharge(start, 1.0, 0.0), 1e-6);

    // make sure both tariffs are on the output list
    // TODO - this should be replaced with a check on an output channel.
    //assertEquals("correct number of notifications queued", 3, tariffMarketService.broadcast.size())
    //assertEquals("correct first element", tariffSpec, tariffMarketService.broadcast.remove(0))
    //assertEquals("correct second element", ts2, tariffMarketService.broadcast.remove(0))
    //assertEquals("correct third element", vru, tariffMarketService.broadcast.remove(0))
  }
  
  // invalid rate
  @Test
  public void testBogusRate ()
  {
    initializeService();
    TariffSpecification ts2 =
          new TariffSpecification(broker, PowerType.CONSUMPTION)
              .withExpiration(new DateTime(2011, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant())
              .withMinDuration(TimeService.WEEK * 4);
    Rate r1 = new Rate()
        .withFixed(false)
        .withMinValue(-0.05)
        .withMaxValue(-0.50)
        .withNoticeInterval(0);
        //.withExpectedMean(0.10); // bogus
    ts2.addRate(r1);
    tariffMarketService.handleMessage(ts2);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", ts2.getId(), status.getUpdateId());
    assertEquals("invalid", TariffStatus.Status.invalidTariff, status.getStatus());
  }
  
  // invalid VRU
  @Test
  public void testBogusVRU ()
  {
    initializeService();
    TariffSpecification ts2 =
          new TariffSpecification(broker, PowerType.CONSUMPTION)
              .withExpiration(new DateTime(2011, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant())
              .withMinDuration(TimeService.WEEK * 4);
    Rate r1 = new Rate()
        .withFixed(false)
        .withMinValue(-0.05)
        .withMaxValue(-0.50)
        .withNoticeInterval(0)
        .withExpectedMean(-0.10);
    ts2.addRate(r1);
    tariffMarketService.handleMessage(ts2);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", ts2.getId(), status.getUpdateId());
    assertEquals("valid", TariffStatus.Status.success, status.getStatus());
    
    // update the hourly rate on tariff 2
    HourlyCharge hc = new HourlyCharge(start.plus(TimeService.HOUR), -0.9);
    VariableRateUpdate vru = new VariableRateUpdate(broker, r1, hc);
    
    int msgsize = msgs.size();
    tariffMarketService.handleMessage(vru);
    assertEquals("Oe new message", msgsize + 1, msgs.size());

    TariffStatus vrs = (TariffStatus)msgs.get(msgsize);
    assertNotNull("non-null vru status", vrs);
    assertEquals("bogus vru", TariffStatus.Status.invalidUpdate, vrs.getStatus());
  }

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
    tariffMarketService.handleMessage(tsc1);
    tariffMarketService.handleMessage(tsc2);
    tariffMarketService.handleMessage(tsc3);
    TariffSpecification tsp1 = new TariffSpecification(broker, PowerType.PRODUCTION)
          .withExpiration(start.plus(TimeService.DAY))
          .withMinDuration(TimeService.WEEK* 8)
          .addRate(new Rate().withValue(0.119));
    TariffSpecification tsp2 = new TariffSpecification(broker, PowerType.PRODUCTION)
          .withExpiration(start.plus(TimeService.DAY * 2))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.119));
    tariffMarketService.handleMessage(tsp1);
    tariffMarketService.handleMessage(tsp2);
    assertEquals("five tariffs", 5, tariffRepo.findAllTariffs().size());
    
    // no active tariffs yet; they are all pending
    List<Tariff> tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals("0 consumption tariffs", 0, tclist.size());
    List<Tariff> tplist = tariffMarketService.getActiveTariffList(PowerType.PRODUCTION);
    assertEquals("0 production tariffs", 0, tplist.size());

    // make them offered
    long[] ids = {tsc1.getId(), tsc2.getId(), tsc3.getId(), tsp1.getId(), tsp2.getId()};
    for (long id : ids) {
      Tariff tf = tariffRepo.findTariffById(id);
      assertNotNull(tf);
      tf.setState(Tariff.State.OFFERED);
    }
    
    // make sure all tariffs are active
    tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals("3 consumption tariffs", 3, tclist.size());
    tplist = tariffMarketService.getActiveTariffList(PowerType.PRODUCTION);
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
    assertEquals("correct phase", 3, mockCC.timeslotPhase);
        
    // current time is noon.
    assertEquals("newTariffs list is empty", 0, tariffRepo.findTariffsByState(Tariff.State.PENDING).size());
    // register a NewTariffListener 
    //List<Tariff> publishedTariffs = new ArrayList<Tariff>();
    MockTariffListener listener = new MockTariffListener();
    tariffMarketService.registerNewTariffListener(listener);
    assertEquals("one registration", 1, tariffMarketService.getRegistrations().size());
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertEquals("no tariffs at 12:00", 0, listener.publishedTariffs.size());
    // publish some tariffs over a period of three hours, check for publication
    TariffSpecification tsc1 = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plus(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.handleMessage(tsc1);
    TariffSpecification tsc1a = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plus(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.223));
    tariffMarketService.handleMessage(tsc1a);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 13:00
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals("no tariffs at 13:00", 0, listener.publishedTariffs.size());
    
    TariffSpecification tsc2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plus(TimeService.DAY * 2))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.handleMessage(tsc2);
    TariffSpecification tsc3 = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plus(TimeService.DAY * 3))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.handleMessage(tsc3);
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
    tariffMarketService.handleMessage(tsp1);
    tariffMarketService.handleMessage(tsp2);
    assertEquals("six tariffs", 6, tariffRepo.findAllTariffs().size());
    
    TariffRevoke tex = new TariffRevoke(tsc1a.getBroker(), tsc1a);
    tariffMarketService.handleMessage(tex);
    tariffMarketService.processRevokedTariffs();

    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    // it's 15:00 - time to publish
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertEquals("5 tariffs at 15:00", 5, listener.publishedTariffs.size());
    List<Tariff> pendingTariffs = tariffRepo.findTariffsByState(Tariff.State.PENDING);
    assertEquals("newTariffs list is again empty", 0, pendingTariffs.size());
  }

  // create some subscriptions and then revoke a tariff
  //@Test
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
    tariffMarketService.handleMessage(tsc1);
    tariffMarketService.handleMessage(tsc2);
    tariffMarketService.handleMessage(tsc3);
    Tariff tc1 = tariffRepo.findTariffById(tsc1.getId());
    assertNotNull("first tariff found", tc1);
    Tariff tc2 = tariffRepo.findTariffById(tsc2.getId());
    assertNotNull("second tariff found", tc2);
    Tariff tc3 = tariffRepo.findTariffById(tsc3.getId());
    assertNotNull("third tariff found", tc3);
    
    // create two customers who can subscribe
    CustomerInfo charley = new CustomerInfo("Charley", 100);
    //AbstractCustomer charley = new AbstractCustomer(charleyInfo);
    //charley.init();
    CustomerInfo sally = new CustomerInfo("Sally", 100);
    //AbstractCustomer sally = new AbstractCustomer(sallyInfo);
    //sally.init();

    // make them offered
    long[] ids = {tsc1.getId(), tsc2.getId(), tsc3.getId()};
    for (long id : ids) {
      Tariff tf = tariffRepo.findTariffById(id);
      assertNotNull(tf);
      tf.setState(Tariff.State.OFFERED);
    }
	
    // make sure we have three active tariffs
    List<Tariff> tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals("3 consumption tariffs", 3, tclist.size());
    //assertEquals("three transaction", 3, TariffTransaction.count());
    
    // create some subscriptions
    tariffMarketService.subscribeToTariff(tc1, charley, 3);
    tariffMarketService.subscribeToTariff(tc2, charley, 31);
    tariffMarketService.subscribeToTariff(tc3, charley, 13);
    tariffMarketService.subscribeToTariff(tc1, sally, 4);
    tariffMarketService.subscribeToTariff(tc2, sally, 24); 
    tariffMarketService.subscribeToTariff(tc3, sally, 42);
    //assertNull("no subscription yet", tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tc1, charley));
    tariffMarketService.activate(start, 4);
    
    TariffSubscription cs1 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tc1, charley);
    assertNotNull(cs1);
    TariffSubscription cs2 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tc2, charley);
    assertNotNull(cs2);
    TariffSubscription cs3 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tc3, charley);
    assertNotNull(cs3);
    TariffSubscription ss1 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tc1, sally);
    assertNotNull(ss1);
    TariffSubscription ss2 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tc2, sally); 
    assertNotNull(ss2);
    TariffSubscription ss3 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tc3, sally);
    assertNotNull(ss3);
    assertEquals("3 customers for cs1", 3, cs1.getCustomersCommitted());
    assertEquals("42 customers for ss3", 42, ss3.getCustomersCommitted());
    assertEquals("Charley has 3 subscriptions", 3, tariffSubscriptionRepo.findSubscriptionsForCustomer(charley).size());
    
    // forward an hour, revoke the second tariff
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    TariffRevoke tex = new TariffRevoke(tc2.getBroker(), tsc2);
    int index = msgs.size();
    tariffMarketService.handleMessage(tex);
    TariffStatus status = (TariffStatus)msgs.get(index);
    assertNotNull("non-null status", status);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertFalse("tariff not yet revoked", tc2.isRevoked());
    //tariffMarketService.processRevokedTariffs();
    //assertTrue("tariff revoked", tc2.isRevoked());

    // should now be just two active tariffs
    //tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    //assertEquals("2 consumption tariffs", 2, tclist.size());

    // retrieve Charley's revoked-subscription list
    //List<TariffSubscription> revokedCharley = 
    //  tariffSubscriptionRepo.getRevokedSubscriptionList(charley);
    //assertEquals("one item in list", 1, revokedCharley.size());
    //assertEquals("it's cs2", cs2, revokedCharley.get(0));

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
    assertEquals("solar tariff is default production", tp1, tariffMarketService.getDefaultTariff(PowerType.SOLAR_PRODUCTION));
  }

  // test balancing orders
  @Test
  public void testBalancingOrder ()
  {
    initializeService();
    Collection<BalancingOrder> orders = tariffRepo.getBalancingOrders();
    assertEquals("no orders yet", 0, orders.size());

    TariffSpecification tsc1 = new TariffSpecification(broker,
                                                       PowerType.INTERRUPTIBLE_CONSUMPTION) 
        .withExpiration(start.plus(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222).withMaxCurtailment(0.1));
    tariffMarketService.handleMessage(tsc1);

    TariffSpecification tsc2 = new TariffSpecification(broker,
                                                       PowerType.INTERRUPTIBLE_CONSUMPTION) 
        .withExpiration(start.plus(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.20).withMaxCurtailment(0.5));
    tariffMarketService.handleMessage(tsc2);

    // add one order, check
    BalancingOrder bo1 = new BalancingOrder(broker, tsc2, 0.6, 0.18);
    tariffMarketService.handleMessage(bo1);

    orders = tariffRepo.getBalancingOrders();
    assertEquals("one order", 1, orders.size());
    assertTrue("correct item", orders.contains(bo1));

    // add second order, check
    BalancingOrder bo2  = new BalancingOrder(broker, tsc1, 0.7, 0.19);
    tariffMarketService.handleMessage(bo2);

    orders = tariffRepo.getBalancingOrders();
    assertEquals("two orders", 2, orders.size());
    assertTrue("contains first", orders.contains(bo1));
    assertTrue("contains second", orders.contains(bo2));

    // replace first order, check
    BalancingOrder bo3 = new BalancingOrder(broker, tsc2, 0.3, 0.18);
    tariffMarketService.handleMessage(bo3);
    orders = tariffRepo.getBalancingOrders();
    assertEquals("two orders", 2, orders.size());
    assertFalse("does not contain first", orders.contains(bo1));
    assertTrue("contains second", orders.contains(bo2));
    assertTrue("contains third", orders.contains(bo3));
  }

  // bogus balancing order, not interruptible
  @Test
  public void testInvalidBalancingOrder1 ()
  {
    initializeService();
    Collection<BalancingOrder> orders = tariffRepo.getBalancingOrders();
    assertEquals("no orders yet", 0, orders.size());

    TariffSpecification tsc1 = new TariffSpecification(broker,
                                                       PowerType.INTERRUPTIBLE_CONSUMPTION)
        .withExpiration(start.plus(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.handleMessage(tsc1);
    msgs.clear();

    // add one order, check
    BalancingOrder bo1 = new BalancingOrder(broker, tsc1, 0.6, 0.18);
    tariffMarketService.handleMessage(bo1);

    assertEquals("one message", 1, msgs.size());
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", bo1.getId(), status.getUpdateId());
    assertEquals("invalid", TariffStatus.Status.unsupported, status.getStatus());
    //System.out.println(status.getMessage());

    orders = tariffRepo.getBalancingOrders();
    assertEquals("no orders", 0, orders.size());
  }

  // bogus balancing order, invalid exercise ratio
  @Test
  public void testInvalidBalancingOrder2 ()
  {
    initializeService();
    Collection<BalancingOrder> orders = tariffRepo.getBalancingOrders();
    assertEquals("no orders yet", 0, orders.size());

    TariffSpecification tsc1 = new TariffSpecification(broker,
                                                       PowerType.CONSUMPTION) 
        .withExpiration(start.plus(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222).withMaxCurtailment(0.5));
    tariffMarketService.handleMessage(tsc1);
    msgs.clear();

    // add one order, check
    BalancingOrder bo1 = new BalancingOrder(broker, tsc1, -0.6, 0.18);
    tariffMarketService.handleMessage(bo1);

    TariffStatus status = (TariffStatus)msgs.get(0);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", bo1.getId(), status.getUpdateId());
    assertEquals("invalid", TariffStatus.Status.unsupported, status.getStatus());
    //System.out.println(status.getMessage());

    orders = tariffRepo.getBalancingOrders();
    assertEquals("no orders", 0, orders.size());
  }

  // bogus balancing order, invalid exercise ratio
  @Test
  public void testInvalidBalancingOrder3 ()
  {
    initializeService();
    Collection<BalancingOrder> orders = tariffRepo.getBalancingOrders();
    assertEquals("no orders yet", 0, orders.size());

    TariffSpecification tsc1 = new TariffSpecification(broker,
                                                       PowerType.CONSUMPTION) 
        .withExpiration(start.plus(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222).withMaxCurtailment(0.5))
        .addRate(new RegulationRate().withUpRegulationPayment(.10));
    tariffMarketService.handleMessage(tsc1);
    msgs.clear();

    // add one order, check
    BalancingOrder bo1 = new BalancingOrder(broker, tsc1, -0.6, 0.18);
    tariffMarketService.handleMessage(bo1);

    TariffStatus status = (TariffStatus)msgs.get(0);
    assertNotNull("non-null status", status);
    assertEquals("correct status ID", bo1.getId(), status.getUpdateId());
    assertEquals("invalid", TariffStatus.Status.unsupported, status.getStatus());
    //System.out.println(status.getMessage());

    orders = tariffRepo.getBalancingOrders();
    assertEquals("no orders", 0, orders.size());
  }

  // test balancing orders
  @Test
  public void testRegRateStorage ()
  {
    initializeService();
    Collection<BalancingOrder> orders = tariffRepo.getBalancingOrders();
    assertEquals("no orders yet", 0, orders.size());

    TariffSpecification tsc1 = new TariffSpecification(broker,
                                                       PowerType.ELECTRIC_VEHICLE) 
        .withExpiration(start.plus(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222))
        .addRate(new RegulationRate()
                     .withUpRegulationPayment(0.2)
                     .withDownRegulationPayment(-0.02));
    tariffMarketService.handleMessage(tsc1);

    orders = tariffRepo.getBalancingOrders();
    assertEquals("two orders", 2, orders.size());
    // make the first one be the up-regulation BO
    ArrayList<BalancingOrder> bos = new ArrayList<>(orders);
    if (bos.get(0).getExerciseRatio() < 0.0) {
      Collections.reverse(bos);
    }
    assertEquals("up-reg exercise ratio", 2.0,
                 bos.get(0).getExerciseRatio(), 1e-6);
    assertEquals("up-reg price", 0.2,
                 bos.get(0).getPrice(), 1e-6);
    assertEquals("up-reg tariff", tsc1.getId(), bos.get(0).getTariffId());
    assertEquals("down-reg exercise ratio", -1.0,
                 bos.get(1).getExerciseRatio(), 1e-6);
    assertEquals("down-reg price", -0.02,
                 bos.get(1).getPrice(), 1e-6);
    assertEquals("up-reg tariff", tsc1.getId(), bos.get(1).getTariffId());
  }

  class MockCC implements CompetitionControl
  {
    TimeslotPhaseProcessor processor;
    int timeslotPhase;

    @Override
    public void registerTimeslotPhase (TimeslotPhaseProcessor thing, int phase)
    {
      processor = thing;
      timeslotPhase = phase;
    }

    @Override
    public boolean isBootstrapMode ()
    {
      return false;
    }

    public void preGame ()
    {
    }

    @Override
    public void setAuthorizedBrokerList (List<String> brokerList)
    {
    }

    public void runOnce ()
    {
    }

    @Override
    public boolean loginBroker (String username)
    {
      return false;
    }

    @Override
    public void runOnce (boolean bootstrapMode)
    { 
    }

    @Override
    public boolean isRunning ()
    {
      return false;
    }

    @Override
    public void shutDown ()
    {
    }
  }
  
  class MockTariffListener implements NewTariffListener
  {
    List<Tariff> publishedTariffs = new ArrayList<Tariff>();
    
    @Override
    public void publishNewTariffs (List<Tariff> tariffs)
    {
      publishedTariffs = tariffs;
    }   
  }
}
