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

import static org.junit.jupiter.api.Assertions.*;
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
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 */
@SpringJUnitConfig(locations = {"classpath:tariff-test-config.xml"})
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
  @BeforeEach
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
    start = ZonedDateTime.of(2011, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();;
    timeService.setCurrentTime(start);
    timeslotRepo.makeTimeslot(start);
    
    // create useful objects, set parameters
    broker = new Broker("testBroker");
    exp = ZonedDateTime.of(2011, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();;
    tariffSpec = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.121));
  }
  
  @AfterEach
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
    assertEquals(-42.0, tariffMarketService.getPublicationFee(), 1e-6, "correct publication fee");

  }
  
  @Test
  public void testNormalInitialization ()
  {
    List<String> inits = new ArrayList<String>();
    inits.add("AccountingService");
    String result = tariffMarketService.initialize(comp, inits);
    assertEquals(result,  "TariffMarket", "correct return value");
    assertTrue((-100.0 >= tariffMarketService.getPublicationFee() && tariffMarketService.getPublicationFee() >= -500.0), "correct publication fee");
  }

  @Test
  public void testBogusInitialization ()
  {
    List<String> inits = new ArrayList<String>();
    String result = tariffMarketService.initialize(comp, inits);
    assertNull(result, "needs AccountingService in the list");
    inits.add("AccountingService");
  }

  // valid tariffSpec
  @Test
  public void testProcessTariffSpec ()
  {
    initializeService();
    tariffMarketService.handleMessage(tariffSpec);
    assertEquals(1, msgs.size(), "one message sent");
    TariffStatus status = (TariffStatus)msgs.get(0);
    // check the status return
    assertNotNull(status, "non-null status");
    assertEquals(tariffSpec.getBroker(), status.getBroker(), "broker");
    assertEquals(tariffSpec.getId(), status.getTariffId(), "tariff ID");
    assertEquals(tariffSpec.getId(), status.getUpdateId(), "status ID");
    assertEquals(TariffStatus.Status.success, status.getStatus(), "success");
    // find and check the tariff
    assertEquals(1, tariffRepo.findAllTariffs().size(), "one tariff");
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertNotNull(tf, "found a tariff");
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
    assertEquals(TariffStatus.Status.success, status.getStatus(), "success");
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertEquals(exp, tf.getExpiration(), "Correct expiration");

    TariffSpecification unpublished = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .addRate(new Rate().withValue(0.121));
    Instant newExp = ZonedDateTime.of(2011, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC).toInstant();;
    TariffExpire tex = new TariffExpire(tariffSpec.getBroker(), unpublished, newExp);
    tariffMarketService.handleMessage(tex);
    status = (TariffStatus)msgs.get(1);
    assertNotNull(status, "non-null status");
    assertEquals(tex.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.noSuchTariff, status.getStatus(), "No such tariff");
    assertEquals(exp, tf.getExpiration(), "tariff not updated");
  }
  
  // exp time in the past
  @Test
  public void testProcessTariffExpirePast ()
  {
    initializeService();
    tariffMarketService.handleMessage(tariffSpec);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertEquals(TariffStatus.Status.success, status.getStatus(), "success");
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertEquals(exp, tf.getExpiration(), "Correct expiration");
    //Instant newExp = ZonedDateTime.of(2011, 3, 1, 8, 0, 0, 0, ZoneOffset.UTC).toInstant();;
    //timeService.setCurrentTime(newExp);
    Instant newExp = timeService.getCurrentTime().minusMillis(TimeService.HOUR);
    TariffExpire tex = new TariffExpire(tariffSpec.getBroker(), tariffSpec, newExp);
    tariffMarketService.handleMessage(tex);
    status = (TariffStatus)msgs.get(1);
    assertNotNull(status, "non-null status");
    assertEquals(tex.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.invalidUpdate, status.getStatus(), "invalid");
  }

  // normal expiration
  @Test
  public void testProcessTariffExpire ()
  {
    initializeService();
    tariffMarketService.handleMessage(tariffSpec);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertEquals(TariffStatus.Status.success, status.getStatus(), "success");
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertEquals(exp, tf.getExpiration(), "Correct expiration");
    Instant newExp = ZonedDateTime.of(2011, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC).toInstant();;
    TariffExpire tex = new TariffExpire(tariffSpec.getBroker(), tariffSpec, newExp);
    tariffMarketService.handleMessage(tex);
    status = (TariffStatus)msgs.get(1);
    assertNotNull(status, "non-null status");
    assertEquals(tex.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.success, status.getStatus(), "success");
    assertEquals(newExp, tf.getExpiration(), "tariff updated");
    assertFalse(tf.isExpired(), "tariff not expired");
    timeService.setCurrentTime(newExp);
    assertTrue(tf.isExpired(), "tariff is expired");
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

    assertEquals(2, msgs.size(), "one message sent");
    TariffStatus status = (TariffStatus)msgs.get(0);
    // check the status return
    assertNotNull(status, "non-null status 0");
    assertEquals(tariffSpec.getBroker(), status.getBroker(), "broker");
    assertEquals(TariffStatus.Status.success, status.getStatus(), "success");

    status = (TariffStatus) msgs.get(1);
    assertNotNull(status, "non-null status 1");
    assertEquals(b2, status.getBroker(), "broker 2");
    assertEquals(TariffStatus.Status.invalidTariff, status.getStatus(), "invalid");
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
    assertNotNull(status, "non-null status");
    assertEquals(ts2.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.invalidTariff, status.getStatus(), "invalid");
    assertEquals(1, msgs.size(), "only one message received");
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
    assertNotNull(status, "non-null status");
    assertEquals(ts2.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.success, status.getStatus(), "valid");
  }

  // normal revoke
  @Test
  public void testProcessTariffRevoke ()
  {
    initializeService();
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    tariffMarketService.handleMessage(tariffSpec);
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertEquals(TariffStatus.Status.success, status.getStatus(), "success");
    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    assertFalse(tf.isRevoked(), "not revoked");
    TariffRevoke tex = new TariffRevoke(tariffSpec.getBroker(), tariffSpec);
    tariffMarketService.handleMessage(tex);
    status = (TariffStatus)msgs.get(1);
    assertNotNull(status, "non-null status");
    assertEquals(tex.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.success, status.getStatus(), "success");
    assertFalse(tf.isRevoked(), "tariff not yet revoked");

    // forward an hour and activate, still should not be revoked.
    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(TimeService.HOUR));
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertFalse(tf.isRevoked(), "tariff not yet revoked");

    // forward two more hours and activate, should do the trick
    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(TimeService.HOUR * 2));
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertTrue(tf.isRevoked(), "tariff revoked");
    TariffRevoke revoke = (TariffRevoke)msgs.get(2);
    assertNotNull(revoke, "revoke msg non-null");
    assertEquals(tariffSpec.getBroker(), revoke.getBroker(), "correct broker");
    assertEquals(tariffSpec.getId(), revoke.getTariffId(), "correct tariff");
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
    assertEquals(TariffStatus.Status.success, status.getStatus(), "success");

    tariffMarketService.handleMessage(spec2);
    TariffStatus status2 = (TariffStatus)msgs.get(1);
    assertEquals(TariffStatus.Status.success, status2.getStatus(), "success");

    Tariff tf = tariffRepo.findTariffById(tariffSpec.getId());
    Tariff tf2 = tariffRepo.findTariffById(spec2.getId());
    assertFalse(tf.isRevoked(), "s1 not revoked");
    assertFalse(tf2.isRevoked(), "s2 not revoked");

    // revoke the otherBroker's tariff
    TariffRevoke tex = new TariffRevoke(tariffSpec.getBroker(), spec2);
    tariffMarketService.handleMessage(tex);
    status = (TariffStatus) msgs.get(2);
    assertNotNull(status, "non-null status");
    assertEquals(tex.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.invalidTariff, status.getStatus(), "invalid");
    assertFalse(tf.isRevoked(), "tariff 1 not yet revoked");
    assertFalse(tf2.isRevoked(), "tariff 2 not yet revoked");

    // forward an hour and activate, still should not be revoked.
    timeService.setCurrentTime(timeService.getCurrentTime()
        .plusMillis(TimeService.HOUR));
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertFalse(tf.isRevoked(), "tariff 1 not yet revoked");
    assertFalse(tf2.isRevoked(), "tariff 2 not yet revoked");

    // forward two more hours and activate, should do the trick
    timeService.setCurrentTime(timeService.getCurrentTime()
        .plusMillis(TimeService.HOUR * 2));
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertFalse(tf.isRevoked(), "tariff 1 not revoked");
    assertFalse(tf2.isRevoked(), "tariff 2 not revoked");
    Object revoke = msgs.get(3);
    assertTrue((null == revoke || !(revoke instanceof TariffRevoke)), "no revoke msg");
  }

  // variable rate update - nominal case, 2 tariffs
  @Test
  public void testVariableRateUpdate ()
  {
    initializeService();
    // what the broker does...
    TariffSpecification ts2 =
          new TariffSpecification(broker, PowerType.CONSUMPTION)
              .withExpiration(ZonedDateTime.of(2011, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant())
              .withMinDuration(TimeService.WEEK * 4);
    Rate r1 = new Rate()
        .withFixed(false)
        .withMinValue(-0.05)
        .withMaxValue(-0.50)
        .withNoticeInterval(0)
        .withExpectedMean(-0.10);
    ts2.addRate(r1);
    Instant lastHr = start.minusMillis(TimeService.HOUR);
    r1.addHourlyCharge(new HourlyCharge(lastHr, -0.07), true);    

    // send to market
    tariffMarketService.handleMessage(tariffSpec);
    tariffMarketService.handleMessage(ts2);
    TariffStatus status1 = (TariffStatus)msgs.get(0);
    TariffStatus status2 = (TariffStatus)msgs.get(1);

    // check the status return2
    assertNotNull(status1, "non-null status 1");
    assertNotNull(status2, "non-null status 2");
    assertEquals(tariffSpec.getBroker(), status1.getBroker(), "broker ID 1");
    assertEquals(ts2.getBroker(), status2.getBroker(), "broker ID 2");
    assertEquals(tariffSpec.getId(), status1.getTariffId(), "tariff ID 1");
    assertEquals(ts2.getId(), status2.getTariffId(), "tariff ID 2");
    assertEquals(tariffSpec.getId(), status1.getUpdateId(), "status ID 1");
    assertEquals(ts2.getId(), status2.getUpdateId(), "status ID 2");
    assertEquals(TariffStatus.Status.success, status1.getStatus(), "success 1");
    assertEquals(TariffStatus.Status.success, status2.getStatus(), "success 2");
    // find and check the tariffs
    assertEquals(2, tariffRepo.findAllTariffs().size(), "two tariffs");
    Tariff tf1 = tariffRepo.findTariffById(tariffSpec.getId());
    Tariff tf2 = tariffRepo.findTariffById(ts2.getId());
    assertEquals(tariffSpec.getId(), tf1.getId(), "found tariff 1");
    assertEquals(ts2.getId(), tf2.getId(), "found tariff 2");
    // make sure r1 is in the repo
    assertEquals(r1, tariffRepo.findRateById(r1.getId()), "found r1");
    
    // update the hourly rate on tariff 2
    HourlyCharge hc = new HourlyCharge(start, -0.09);
    VariableRateUpdate vru = new VariableRateUpdate(broker, r1, hc);

    // check for correct times
    assertEquals(start, hc.getAtTime(), "correct hc start");
    assertEquals(start, timeService.getCurrentTime(), "correct current time");
    
    int msgsize = msgs.size();
    tariffMarketService.handleMessage(vru);
    assertEquals(msgsize, msgs.size(), "no messages yet");

    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertEquals(msgsize + 3, msgs.size(), "one new message");

    TariffStatus vrs = (TariffStatus)msgs.get(msgsize - 1);
    assertNotNull(vrs, "non-null vru status");
    assertEquals(TariffStatus.Status.success, vrs.getStatus(), "success vru");
    assertEquals(start, timeService.getCurrentTime(), "correct current time");
    
    assertEquals(-0.07, tf2.getUsageCharge(lastHr, 1.0), 1e-6, "Correct rate at 11:00");
    assertEquals(-0.09, tf2.getUsageCharge(start, 1.0), 1e-6, "Correct rate at 12:00");

    // make sure both tariffs are on the output list
    // TODO - this should be replaced with a check on an output channel.
    //assertEquals(3, tariffMarketService.broadcast.size(), "correct number of notifications queued")
    //assertEquals(tariffSpec, tariffMarketService.broadcast.remove(0), "correct first element")
    //assertEquals(ts2, tariffMarketService.broadcast.remove(0), "correct second element")
    //assertEquals(vru, tariffMarketService.broadcast.remove(0), "correct third element")
  }
  
  // invalid rate
  @Test
  public void testBogusRate ()
  {
    initializeService();
    TariffSpecification ts2 =
          new TariffSpecification(broker, PowerType.CONSUMPTION)
              .withExpiration(ZonedDateTime.of(2011, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant())
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
    assertNotNull(status, "non-null status");
    assertEquals(ts2.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.invalidTariff, status.getStatus(), "invalid");
  }
  
  // invalid VRU
  @Test
  public void testBogusVRU ()
  {
    initializeService();
    TariffSpecification ts2 =
          new TariffSpecification(broker, PowerType.CONSUMPTION)
              .withExpiration(ZonedDateTime.of(2011, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant())
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
    assertNotNull(status, "non-null status");
    assertEquals(ts2.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.success, status.getStatus(), "valid");
    
    // update the hourly rate on tariff 2
    HourlyCharge hc = new HourlyCharge(start.plusMillis(TimeService.HOUR), -0.9);
    VariableRateUpdate vru = new VariableRateUpdate(broker, r1, hc);
    
    int msgsize = msgs.size();
    tariffMarketService.handleMessage(vru);
    assertEquals(msgsize + 1, msgs.size(), "Oe new message");

    TariffStatus vrs = (TariffStatus)msgs.get(msgsize);
    assertNotNull(vrs, "non-null vru status");
    assertEquals(TariffStatus.Status.invalidUpdate, vrs.getStatus(), "bogus vru");
  }

  // check evolution of active tariff list
  @Test
  public void testGetActiveTariffList ()
  {
    initializeService();
    // initially, there should be no active tariffs
    assertEquals(0, tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION).size(), "no initial tariffs");
    // first, add multiple tariffs for more than one power type, multiple expirations
    TariffSpecification tsc1 = new TariffSpecification(broker, PowerType.CONSUMPTION)
          .withExpiration(start.plusMillis(TimeService.DAY))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    TariffSpecification tsc2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
          .withExpiration(start.plusMillis(TimeService.DAY * 2))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    TariffSpecification tsc3 = new TariffSpecification(broker, PowerType.CONSUMPTION)
          .withExpiration(start.plusMillis(TimeService.DAY * 3))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    tariffMarketService.handleMessage(tsc1);
    tariffMarketService.handleMessage(tsc2);
    tariffMarketService.handleMessage(tsc3);
    TariffSpecification tsp1 = new TariffSpecification(broker, PowerType.PRODUCTION)
          .withExpiration(start.plusMillis(TimeService.DAY))
          .withMinDuration(TimeService.WEEK* 8)
          .addRate(new Rate().withValue(0.119));
    TariffSpecification tsp2 = new TariffSpecification(broker, PowerType.PRODUCTION)
          .withExpiration(start.plusMillis(TimeService.DAY * 2))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.119));
    tariffMarketService.handleMessage(tsp1);
    tariffMarketService.handleMessage(tsp2);
    assertEquals(5, tariffRepo.findAllTariffs().size(), "five tariffs");
    
    // no active tariffs yet; they are all pending
    List<Tariff> tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals(0, tclist.size(), "0 consumption tariffs");
    List<Tariff> tplist = tariffMarketService.getActiveTariffList(PowerType.PRODUCTION);
    assertEquals(0, tplist.size(), "0 production tariffs");

    // make them offered
    long[] ids = {tsc1.getId(), tsc2.getId(), tsc3.getId(), tsp1.getId(), tsp2.getId()};
    for (long id : ids) {
      Tariff tf = tariffRepo.findTariffById(id);
      assertNotNull(tf);
      tf.setState(Tariff.State.OFFERED);
    }
    
    // make sure all tariffs are active
    tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals(3, tclist.size(), "3 consumption tariffs");
    tplist = tariffMarketService.getActiveTariffList(PowerType.PRODUCTION);
    assertEquals(2, tplist.size(), "2 production tariffs");
    
    // forward one day, try again
    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(TimeService.DAY));
    tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals(2, tclist.size(), "2 consumption tariffs");
    tplist = tariffMarketService.getActiveTariffList(PowerType.PRODUCTION);
    assertEquals(1, tplist.size(), "1 production tariffs");
    
    // forward another day, try again
    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(TimeService.DAY));
    tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals(1, tclist.size(), "1 consumption tariff");
    tplist = tariffMarketService.getActiveTariffList(PowerType.PRODUCTION);
    assertEquals(0, tplist.size(), "no production tariffs");
    
    // forward another day, try again
    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(TimeService.DAY));
    tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    assertEquals(0, tclist.size(), "no consumption tariffs");
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
    assertEquals(tariffMarketService, mockCC.processor, "correct thing");
    assertEquals(3, mockCC.timeslotPhase, "correct phase");
        
    // current time is noon.
    assertEquals(0, tariffRepo.findTariffsByState(Tariff.State.PENDING).size(), "newTariffs list is empty");
    // register a NewTariffListener 
    //List<Tariff> publishedTariffs = new ArrayList<Tariff>();
    MockTariffListener listener = new MockTariffListener();
    tariffMarketService.registerNewTariffListener(listener);
    assertEquals(1, tariffMarketService.getRegistrations().size(), "one registration");
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertEquals(0, listener.publishedTariffs.size(), "no tariffs at 12:00");
    // publish some tariffs over a period of three hours, check for publication
    TariffSpecification tsc1 = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plusMillis(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.handleMessage(tsc1);
    TariffSpecification tsc1a = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plusMillis(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.223));
    tariffMarketService.handleMessage(tsc1a);
    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(TimeService.HOUR));
    // it's 13:00
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals(0, listener.publishedTariffs.size(), "no tariffs at 13:00");
    
    TariffSpecification tsc2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plusMillis(TimeService.DAY * 2))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.handleMessage(tsc2);
    TariffSpecification tsc3 = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(start.plusMillis(TimeService.DAY * 3))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.handleMessage(tsc3);
    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(TimeService.HOUR));
    // it's 14:00
    tariffMarketService.activate(timeService.getCurrentTime(), 2);
    assertEquals(0, listener.publishedTariffs.size(), "no tariffs at 14:00");

    TariffSpecification tsp1 = new TariffSpecification(broker, PowerType.PRODUCTION)
        .withExpiration(start.plusMillis(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.119));
    TariffSpecification tsp2 = new TariffSpecification(broker, PowerType.PRODUCTION)
        .withExpiration(start.plusMillis(TimeService.DAY * 2))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.119));
    tariffMarketService.handleMessage(tsp1);
    tariffMarketService.handleMessage(tsp2);
    assertEquals(6, tariffRepo.findAllTariffs().size(), "six tariffs");
    
    TariffRevoke tex = new TariffRevoke(tsc1a.getBroker(), tsc1a);
    tariffMarketService.handleMessage(tex);
    tariffMarketService.processRevokedTariffs();

    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(TimeService.HOUR));
    // it's 15:00 - time to publish
    tariffMarketService.activate(timeService.getCurrentTime(), 3);
    assertEquals(5, listener.publishedTariffs.size(), "5 tariffs at 15:00");
    List<Tariff> pendingTariffs = tariffRepo.findTariffsByState(Tariff.State.PENDING);
    assertEquals(0, pendingTariffs.size(), "newTariffs list is again empty");
  }

  // create some subscriptions and then revoke a tariff
  //@Test
  public void testGetRevokedSubscriptionList ()
  {
    initializeService();
    // create some tariffs
    TariffSpecification tsc1 = new TariffSpecification(broker, PowerType.CONSUMPTION)
          .withExpiration(start.plusMillis(TimeService.DAY * 5))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    TariffSpecification tsc2 = new TariffSpecification(broker,  PowerType.CONSUMPTION)
          .withExpiration(start.plusMillis(TimeService.DAY * 7))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    TariffSpecification tsc3 = new TariffSpecification(broker,  PowerType.CONSUMPTION)
          .withExpiration(start.plusMillis(TimeService.DAY * 9))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    tariffMarketService.handleMessage(tsc1);
    tariffMarketService.handleMessage(tsc2);
    tariffMarketService.handleMessage(tsc3);
    Tariff tc1 = tariffRepo.findTariffById(tsc1.getId());
    assertNotNull(tc1, "first tariff found");
    Tariff tc2 = tariffRepo.findTariffById(tsc2.getId());
    assertNotNull(tc2, "second tariff found");
    Tariff tc3 = tariffRepo.findTariffById(tsc3.getId());
    assertNotNull(tc3, "third tariff found");
    
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
    assertEquals(3, tclist.size(), "3 consumption tariffs");
    //assertEquals(3, TariffTransaction.count(), "three transaction");
    
    // create some subscriptions
    tariffMarketService.subscribeToTariff(tc1, charley, 3);
    tariffMarketService.subscribeToTariff(tc2, charley, 31);
    tariffMarketService.subscribeToTariff(tc3, charley, 13);
    tariffMarketService.subscribeToTariff(tc1, sally, 4);
    tariffMarketService.subscribeToTariff(tc2, sally, 24); 
    tariffMarketService.subscribeToTariff(tc3, sally, 42);
    //assertNull(tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tc1, charley), "no subscription yet");
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
    assertEquals(3, cs1.getCustomersCommitted(), "3 customers for cs1");
    assertEquals(42, ss3.getCustomersCommitted(), "42 customers for ss3");
    assertEquals(3, tariffSubscriptionRepo.findSubscriptionsForCustomer(charley).size(), "Charley has 3 subscriptions");
    
    // forward an hour, revoke the second tariff
    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(TimeService.HOUR));
    TariffRevoke tex = new TariffRevoke(tc2.getBroker(), tsc2);
    int index = msgs.size();
    tariffMarketService.handleMessage(tex);
    TariffStatus status = (TariffStatus)msgs.get(index);
    assertNotNull(status, "non-null status");
    assertEquals(TariffStatus.Status.success, status.getStatus(), "success");
    assertFalse(tc2.isRevoked(), "tariff not yet revoked");
    //tariffMarketService.processRevokedTariffs();
    //assertTrue(tc2.isRevoked(), "tariff revoked");

    // should now be just two active tariffs
    //tclist = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);
    //assertEquals(2, tclist.size(), "2 consumption tariffs");

    // retrieve Charley's revoked-subscription list
    //List<TariffSubscription> revokedCharley = 
    //  tariffSubscriptionRepo.getRevokedSubscriptionList(charley);
    //assertEquals(1, revokedCharley.size(), "one item in list");
    //assertEquals(cs2, revokedCharley.get(0), "it's cs2");

    // find and check the transaction
    // TODO - we would need to mock AccountingService to get at these objects
    //assertEquals(4, TariffTransaction.count(), "one more transaction");
    //TariffTransaction ttx = TariffTransaction.findByPostedTime(timeService.getCurrentTime());
    //assertNotNull(ttx, "found transaction");
    //assertEquals(tc2.getTariffSpec(), ttx.getTariffSpec(), "correct tariff");
    //assertEquals(TariffTransactionType.REVOKE, ttx.getTxType(), "correct type");
    //assertEquals(420.0, ttx.getCharge(), 1e-6, "correct amount");
  }

  // check default tariffs
  @Test
  public void testGetDefaultTariff ()
  {
    initializeService();
    // set defaults for consumption and production
    TariffSpecification tsc1 = new TariffSpecification(broker,PowerType.CONSUMPTION) 
          .withExpiration(start.plusMillis(TimeService.WEEK))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.222));
    assertTrue(tariffMarketService.setDefaultTariff(tsc1), "add consumption default");
    TariffSpecification tsp1 = new TariffSpecification(broker, PowerType.PRODUCTION) 
          .withExpiration(start.plusMillis(TimeService.WEEK))
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.122));
    assertTrue(tariffMarketService.setDefaultTariff(tsp1), "add production default");
    
    // find the resulting tariffs
    Tariff tc1 = tariffRepo.findTariffById(tsc1.getId());
    assertNotNull(tc1, "consumption tariff found");
    assertEquals(tsc1.getId(), tc1.getSpecId(), "correct consumption tariff");
    Tariff tp1 = tariffRepo.findTariffById(tsp1.getId());
    assertNotNull(tp1, "production tariff found");
    assertEquals(tsp1.getId(), tp1.getSpecId(), "correct production tariff");

    // retrieve and check the defaults
    assertEquals(tc1, tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION), "default consumption tariff");
    assertEquals(tp1, tariffMarketService.getDefaultTariff(PowerType.PRODUCTION), "default production tariff");
    assertEquals(tp1, tariffMarketService.getDefaultTariff(PowerType.SOLAR_PRODUCTION), "solar tariff is default production");
  }

  // test balancing orders
  @Test
  public void testBalancingOrder ()
  {
    initializeService();
    Collection<BalancingOrder> orders = tariffRepo.getBalancingOrders();
    assertEquals(0, orders.size(), "no orders yet");

    TariffSpecification tsc1 = new TariffSpecification(broker,
                                                       PowerType.INTERRUPTIBLE_CONSUMPTION) 
        .withExpiration(start.plusMillis(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222).withMaxCurtailment(0.1));
    tariffMarketService.handleMessage(tsc1);

    TariffSpecification tsc2 = new TariffSpecification(broker,
                                                       PowerType.INTERRUPTIBLE_CONSUMPTION) 
        .withExpiration(start.plusMillis(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.20).withMaxCurtailment(0.5));
    tariffMarketService.handleMessage(tsc2);

    // add one order, check
    BalancingOrder bo1 = new BalancingOrder(broker, tsc2, 0.6, 0.18);
    tariffMarketService.handleMessage(bo1);

    orders = tariffRepo.getBalancingOrders();
    assertEquals(1, orders.size(), "one order");
    assertTrue(orders.contains(bo1), "correct item");

    // add second order, check
    BalancingOrder bo2  = new BalancingOrder(broker, tsc1, 0.7, 0.19);
    tariffMarketService.handleMessage(bo2);

    orders = tariffRepo.getBalancingOrders();
    assertEquals(2, orders.size(), "two orders");
    assertTrue(orders.contains(bo1), "contains first");
    assertTrue(orders.contains(bo2), "contains second");

    // replace first order, check
    BalancingOrder bo3 = new BalancingOrder(broker, tsc2, 0.3, 0.18);
    tariffMarketService.handleMessage(bo3);
    orders = tariffRepo.getBalancingOrders();
    assertEquals(2, orders.size(), "two orders");
    assertFalse(orders.contains(bo1), "does not contain first");
    assertTrue(orders.contains(bo2), "contains second");
    assertTrue(orders.contains(bo3), "contains third");
  }

  // bogus balancing order, not interruptible
  @Test
  public void testInvalidBalancingOrder1 ()
  {
    initializeService();
    Collection<BalancingOrder> orders = tariffRepo.getBalancingOrders();
    assertEquals(0, orders.size(), "no orders yet");

    TariffSpecification tsc1 = new TariffSpecification(broker,
                                                       PowerType.INTERRUPTIBLE_CONSUMPTION)
        .withExpiration(start.plusMillis(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222));
    tariffMarketService.handleMessage(tsc1);
    msgs.clear();

    // add one order, check
    BalancingOrder bo1 = new BalancingOrder(broker, tsc1, 0.6, 0.18);
    tariffMarketService.handleMessage(bo1);

    assertEquals(1, msgs.size(), "one message");
    TariffStatus status = (TariffStatus)msgs.get(0);
    assertNotNull(status, "non-null status");
    assertEquals(bo1.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.unsupported, status.getStatus(), "invalid");
    //System.out.println(status.getMessage());

    orders = tariffRepo.getBalancingOrders();
    assertEquals(0, orders.size(), "no orders");
  }

  // bogus balancing order, invalid exercise ratio
  @Test
  public void testInvalidBalancingOrder2 ()
  {
    initializeService();
    Collection<BalancingOrder> orders = tariffRepo.getBalancingOrders();
    assertEquals(0, orders.size(), "no orders yet");

    TariffSpecification tsc1 = new TariffSpecification(broker,
                                                       PowerType.CONSUMPTION) 
        .withExpiration(start.plusMillis(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222).withMaxCurtailment(0.5));
    tariffMarketService.handleMessage(tsc1);
    msgs.clear();

    // add one order, check
    BalancingOrder bo1 = new BalancingOrder(broker, tsc1, -0.6, 0.18);
    tariffMarketService.handleMessage(bo1);

    TariffStatus status = (TariffStatus)msgs.get(0);
    assertNotNull(status, "non-null status");
    assertEquals(bo1.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.unsupported, status.getStatus(), "invalid");
    //System.out.println(status.getMessage());

    orders = tariffRepo.getBalancingOrders();
    assertEquals(0, orders.size(), "no orders");
  }

  // bogus balancing order, invalid exercise ratio
  @Test
  public void testInvalidBalancingOrder3 ()
  {
    initializeService();
    Collection<BalancingOrder> orders = tariffRepo.getBalancingOrders();
    assertEquals(0, orders.size(), "no orders yet");

    TariffSpecification tsc1 = new TariffSpecification(broker,
                                                       PowerType.CONSUMPTION) 
        .withExpiration(start.plusMillis(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222).withMaxCurtailment(0.5))
        .addRate(new RegulationRate().withUpRegulationPayment(.10));
    tariffMarketService.handleMessage(tsc1);
    msgs.clear();

    // add one order, check
    BalancingOrder bo1 = new BalancingOrder(broker, tsc1, -0.6, 0.18);
    tariffMarketService.handleMessage(bo1);

    TariffStatus status = (TariffStatus)msgs.get(0);
    assertNotNull(status, "non-null status");
    assertEquals(bo1.getId(), status.getUpdateId(), "correct status ID");
    assertEquals(TariffStatus.Status.unsupported, status.getStatus(), "invalid");
    //System.out.println(status.getMessage());

    orders = tariffRepo.getBalancingOrders();
    assertEquals(0, orders.size(), "no orders");
  }

  // test balancing orders
  @Test
  public void testRegRateStorage ()
  {
    initializeService();
    Collection<BalancingOrder> orders = tariffRepo.getBalancingOrders();
    assertEquals(0, orders.size(), "no orders yet");

    TariffSpecification tsc1 = new TariffSpecification(broker,
                                                       PowerType.ELECTRIC_VEHICLE) 
        .withExpiration(start.plusMillis(TimeService.WEEK))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.222))
        .addRate(new RegulationRate()
                     .withUpRegulationPayment(0.2)
                     .withDownRegulationPayment(-0.02));
    tariffMarketService.handleMessage(tsc1);

    orders = tariffRepo.getBalancingOrders();
    assertEquals(2, orders.size(), "two orders");
    // make the first one be the up-regulation BO
    ArrayList<BalancingOrder> bos = new ArrayList<>(orders);
    if (bos.get(0).getExerciseRatio() < 0.0) {
      Collections.reverse(bos);
    }
    assertEquals(2.0, bos.get(0).getExerciseRatio(), 1e-6, "up-reg exercise ratio");
    assertEquals(0.2, bos.get(0).getPrice(), 1e-6, "up-reg price");
    assertEquals(tsc1.getId(), bos.get(0).getTariffId(), "up-reg tariff");
    assertEquals(-1.0, bos.get(1).getExerciseRatio(), 1e-6, "down-reg exercise ratio");
    assertEquals(-0.02, bos.get(1).getPrice(), 1e-6, "down-reg price");
    assertEquals(tsc1.getId(), bos.get(1).getTariffId(), "up-reg tariff");
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
    public void setBrokerSync (boolean setting)
    {
    }

    @Override
    public void runOnce (boolean bootstrapMode)
    {
    }

    @Override
    public void runOnce (boolean bootstrapMode, boolean dumpConfigOnly)
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
