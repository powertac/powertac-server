/*
 * Copyright (c) 2013 by the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.auctioneer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.apache.commons.configuration2.MapConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.msg.OrderStatus;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;



/**
 * Test cases for AuctionService
 *
 * @author John Collins
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class AuctionServiceTests
{
  static private Logger log = LogManager.getLogger(AuctionServiceTests.class.getName());

  @Autowired
  private AuctionService svc;

  @Autowired
  private TimeService timeService;

  @Autowired
  private Accounting accountingService;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private OrderbookRepo orderbookRepo;

  // get access to the mock services
  @Autowired
  private BrokerProxy mockProxy;

  @Autowired
  private CompetitionControl mockControl;

  @Autowired
  private ServerConfiguration mockServerProps;

  private Competition competition;
  private Configurator config;
  private Broker b1;
  private Broker b2;
  private Broker s1;
  private Broker s2;
  private Timeslot ts0;
  private Timeslot ts1;
  private int ts1Num;
  private Timeslot ts2;
  private int ts2Num;
  //private Timeslot ts3;
  //private Timeslot ts4;
  
  private List<Object[]> accountingArgs;
  private List<Object> brokerMsgs;

  @SuppressWarnings("rawtypes")
  @BeforeEach
  public void setUp () throws Exception
  {
    // clean up from previous tests
    timeslotRepo.recycle();
    reset(mockProxy);
    reset(mockControl);
    reset(mockServerProps);
    accountingArgs = new ArrayList<Object[]>();
    brokerMsgs = new ArrayList<Object>();

    // create a Competition, needed for initialization
    competition = Competition.newInstance("auctioneer-test").withTimeslotsOpen(4);
    Competition.setCurrent(competition);

    // mock the ServerProperties

    // Set up serverProperties mock
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
    }).when(mockServerProps).configureMe(any());

    // Create some brokers who can trade
    b1 = new Broker("Buyer #1");
    b2 = new Broker("Buyer #2");
    s1 = new Broker("Seller #1");
    s2 = new Broker("Seller #2");

    // set the clock, create some useful timeslots
    Instant now = Competition.currentCompetition().getSimulationBaseTime();
    timeService.setCurrentTime(now);
    ts0 = timeslotRepo.makeTimeslot(now);
    ts1 = timeslotRepo.makeTimeslot(now.plusMillis(TimeService.HOUR));
    ts1Num = ts1.getSerialNumber();
    ts2 = timeslotRepo.makeTimeslot(now.plusMillis(TimeService.HOUR * 2));
    ts2Num = ts2.getSerialNumber();
    //timeslotRepo.makeTimeslot(now.plusMillis(TimeService.HOUR * 3));
    //timeslotRepo.makeTimeslot(now.plusMillis(TimeService.HOUR * 4));
    svc.clearEnabledTimeslots();

    // mock the AccountingService, capture args
    doAnswer(new Answer()
    {
      @Override
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        accountingArgs.add(args);
        return null;
      }
    }).when(accountingService).addMarketTransaction(isA(Broker.class),
        isA(Timeslot.class),
        anyDouble(),
        anyDouble());
    // mock the Broker Proxy, capture messages
    doAnswer(new Answer()
    {
      @Override
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        brokerMsgs.add(args[0]);
        return null;
      }
    }).when(mockProxy).broadcastMessage(any());

    // Configure the AuctionService
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("auctioneer.auctionService.sellerSurplusRatio", "0.5");
    map.put("auctioneer.auctionService.defaultMargin", "0.2");
    map.put("auctioneer.auctionService.defaultClearingPrice", "40.0");
    map.put("auctioneer.auctionService.mktPosnLimitInitial", "90.0");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    svc.initialize(competition, new ArrayList<String>());
  }

  @Test
  public void testAuctionService ()
  {
    assertNotNull(svc, "auction service created");
    assertEquals(0.5, svc.getSellerSurplusRatio(), 1e-6, "correct surplus");
    assertEquals(0, svc.getIncoming().size(), "empty incoming");
  }

  @Test
  public void testInit ()
  {
    verify(mockProxy).registerBrokerMessageListener(svc, Order.class);
    verify(mockControl).registerTimeslotPhase(svc, 1);
  }

  @Test
  public void testReceiveMessage ()
  {
    // try a good one
    Order good = new Order(b1, ts1.getSerialNumber(), 1.0, -22.0);
    svc.handleMessage(good);
    assertEquals(1, svc.getIncoming().size(), "one order received");
  }

  @Test
  public void testValidateOrder ()
  {
    // mock the Broker Proxy for b1, capture messages
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        brokerMsgs.add(args[1]);
        return null;
      }
    }).when(mockProxy).sendMessage(eq(b1), any());

    competition.withMinimumOrderQuantity(0.1);
    Order good = new Order(b1, ts1.getSerialNumber(), 1.0, -22.0);
    assertTrue(timeslotRepo.isTimeslotEnabled(ts1), "ts1 enabled");
    assertTrue(svc.validateOrder(good), "next timeslot valid");

    Order bogus = new Order(b1, ts0.getSerialNumber(), 1.0, -22.0);
    assertFalse(timeslotRepo.isTimeslotEnabled(ts0), "ts0 not enabled");
    assertFalse(svc.validateOrder(bogus), "current timeslot not valid");
    assertEquals(1, brokerMsgs.size(), "1 message sent");
    OrderStatus status = (OrderStatus) brokerMsgs.get(0);
    assertNotNull(status, "status got sent");
    assertEquals(b1, status.getBroker(), "correct broker");
    assertEquals(bogus.getId(), status.getOrderId(), "correct order");
    
    Order smallSell = new Order(b1, ts1.getSerialNumber(), 0.09, -22.0);
    assertFalse(svc.validateOrder(smallSell), "too small buy");
    Order smallBuy = new Order(b1, ts1.getSerialNumber(), -0.08, 22.0);
    assertFalse(svc.validateOrder(smallBuy), "too small buy");
  }

  // one ask, one bid, equal qty, tradeable
  @Test
  public void testActivate1 ()
  {
    Order sell = new Order(s1, ts1.getSerialNumber(), -1.0, 20.0);
    Order buy = new Order(b1, ts1.getSerialNumber(), 1.0, -22.0);
    svc.handleMessage(sell);
    svc.handleMessage(buy);
    assertEquals(2, svc.getIncoming().size(), "two orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(2, accountingArgs.size(), "accounting called twice");
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts1, args[1], "ts1");
    assertEquals(-1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");
    args = accountingArgs.get(1);
    assertEquals(b1, args[0], "b1");
    assertEquals(ts1, args[1], "ts1");
    assertEquals(1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");
    // two broker messages
    assertEquals(2, brokerMsgs.size(), "2 messages");
    assertTrue(brokerMsgs.get(0) instanceof Orderbook, "first is orderbook");
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals(0, ob.getAsks().size(), "no uncleared asks");
    assertEquals(0, ob.getBids().size(), "no uncleared bids");
    assertEquals(ts1, ob.getTimeslot(), "correct timeslot");
    assertEquals(21.0, ob.getClearingPrice(), 1e-6, "correct clearing");
    assertTrue(brokerMsgs.get(1) instanceof ClearedTrade, "second is clearedTrade");
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals(ts1, ct.getTimeslot(), "correct timeslot");
    assertEquals(1.0, ct.getExecutionMWh(), 1e-6, "correct mWh");
    assertEquals(21.0, ct.getExecutionPrice(), 1e-6, "correct price");
    // check minAsk data
    Double[] minAsks = orderbookRepo.getMinAskPrices();
    assertEquals(4, minAsks.length, "four prices");
    assertEquals(20.0, minAsks[0], 1e-6, "correct first price");
    assertNull(minAsks[1], "second price null");
    assertNull(minAsks[2], "third price null");
    assertNull(minAsks[3], "fourth price null");
  }

  // one ask, one bid, equal qty, tradeable
  @Test
  public void testActivate1HighMargin ()
  {
    Order sell = new Order(s1, ts1.getSerialNumber(), -1.0, 20.0);
    Order buy = new Order(b1, ts1.getSerialNumber(), 1.0, -32.0);
    svc.handleMessage(sell);
    svc.handleMessage(buy);
    assertEquals(2, svc.getIncoming().size(), "two orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(2, accountingArgs.size(), "accounting called twice");
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts1, args[1], "ts1");
    assertEquals(-1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");
    args = accountingArgs.get(1);
    assertEquals(b1, args[0], "b1");
    assertEquals(ts1, args[1], "ts1");
    assertEquals(1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");
    // two broker messages
    assertEquals(2, brokerMsgs.size(), "2 messages");
    assertTrue(brokerMsgs.get(0) instanceof Orderbook, "first is orderbook");
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals(0, ob.getAsks().size(), "no uncleared asks");
    assertEquals(0, ob.getBids().size(), "no uncleared bids");
    assertEquals(ts1, ob.getTimeslot(), "correct timeslot");
    assertEquals(21.0, ob.getClearingPrice(), 1e-6, "correct clearing");
    assertTrue(brokerMsgs.get(1) instanceof ClearedTrade, "second is clearedTrade");
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals(ts1, ct.getTimeslot(), "correct timeslot");
    assertEquals(1.0, ct.getExecutionMWh(), 1e-6, "correct mWh");
    assertEquals(21.0, ct.getExecutionPrice(), 1e-6, "correct price");
    // check minAsk data
    Double[] minAsks = orderbookRepo.getMinAskPrices();
    assertEquals(4, minAsks.length, "four prices");
    assertEquals(20.0, minAsks[0], 1e-6, "correct first price");
    assertNull(minAsks[1], "second price null");
    assertNull(minAsks[2], "third price null");
    assertNull(minAsks[3], "fourth price null");
  }

  // one ask, one bid, equal qty, not tradeable
  @Test
  public void testActivate1_no ()
  {
    Order sell = new Order(s1, ts1.getSerialNumber(), -1.0, 23.0);
    Order buy = new Order(b1, ts1.getSerialNumber(), 1.0, -22.0);
    svc.handleMessage(sell);
    svc.handleMessage(buy);
    assertEquals(2, svc.getIncoming().size(), "two orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(0, accountingArgs.size(), "accounting not called");
    // one broker message
    assertEquals(1, brokerMsgs.size(), "one message");
    assertTrue(brokerMsgs.get(0) instanceof Orderbook, "first is orderbook");
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertNull(ob.getClearingPrice(), "null clearing price");
    assertEquals(1, ob.getAsks().size(), "one uncleared ask");
    assertEquals(-1.0, ob.getAsks().first().getMWh(), 1e-6, "correct qty");
    assertEquals(23.0, ob.getAsks().first().getLimitPrice(), 1e-6, "correct price");
    assertEquals(1, ob.getBids().size(), "one uncleared bid");
    assertEquals(1.0, ob.getBids().first().getMWh(), 1e-6, "correct qty");
    assertEquals(-22.0, ob.getBids().first().getLimitPrice(), 1e-6, "correct price");
    // check minAsk data
    Double[] minAsks = orderbookRepo.getMinAskPrices();
    assertEquals(4, minAsks.length, "four prices");
    assertEquals(23.0, minAsks[0], 1e-6, "correct first price");
    assertNull(minAsks[1], "second price null");
    assertNull(minAsks[2], "third price null");
    assertNull(minAsks[3], "fourth price null");
  }

  // one ask, one bid, equal qty, different timeslots
  @Test
  public void testActivate1_ts ()
  {
    Order sell = new Order(s1, ts1.getSerialNumber(), -1.0, 23.0);
    Order buy = new Order(b1, ts2.getSerialNumber(), 1.0, -22.0);
    svc.handleMessage(sell);
    svc.handleMessage(buy);
    assertEquals(2, svc.getIncoming().size(), "two orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(0, accountingArgs.size(), "accounting not called");
    // two broker messages, one for each timeslot
    assertEquals(2, brokerMsgs.size(), "two messages");
    assertTrue(brokerMsgs.get(0) instanceof Orderbook, "ts1 orderbook");
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals(ts1, ob.getTimeslot(), "ts1");
    assertNull(ob.getClearingPrice(), "null clearing price");
    assertEquals(1, ob.getAsks().size(), "one uncleared ask");
    assertEquals(0, ob.getBids().size(), "no uncleared bids");
    assertEquals(-1.0, ob.getAsks().first().getMWh(), 1e-6, "correct qty");
    assertEquals(23.0, ob.getAsks().first().getLimitPrice(), 1e-6, "correct price");

    assertTrue(brokerMsgs.get(0) instanceof Orderbook, "ts2 orderbook");
    ob = (Orderbook) brokerMsgs.get(1);
    assertEquals(ts2, ob.getTimeslot(), "ts2");
    assertNull(ob.getClearingPrice(), "null clearing price");
    assertEquals(0, ob.getAsks().size(), "no uncleared asks");
    assertEquals(1, ob.getBids().size(), "one uncleared bid");
    assertEquals(1.0, ob.getBids().first().getMWh(), 1e-6, "correct qty");
    assertEquals(-22.0, ob.getBids().first().getLimitPrice(), 1e-6, "correct price");
  }

  // one ask, two bids, all tradeable
  @Test
  public void testActivate1_2_tradeable ()
  {
    Order sell = new Order(s1, ts1Num, -1.0, 20.0);
    Order buy1 = new Order(b1, ts1Num, 0.6, -21.0);
    Order buy2 = new Order(b2, ts1Num, 0.6, -22.0);
    svc.handleMessage(sell);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals(3, svc.getIncoming().size(), "three orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(4, accountingArgs.size(), "accounting: 4 calls");
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts1, args[1], "ts1");
    assertEquals(-0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(20.5, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(1);
    assertEquals(b2, args[0], "b2"); // b2 had the higher bid price
    assertEquals(ts1, args[1], "ts1");
    assertEquals(0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(-20.5, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(2);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts1, args[1], "ts1");
    assertEquals(-0.4, (Double) args[2], 1e-6, "mWh");
    assertEquals(20.5, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(3);
    assertEquals(b1, args[0], "b1"); // b1 had the lower bid price
    assertEquals(ts1, args[1], "ts1");
    assertEquals(0.4, (Double) args[2], 1e-6, "mWh");
    assertEquals(-20.5, (Double) args[3], 1e-6, "price");
    // two broker messages
    assertEquals(2, brokerMsgs.size(), "2 messages");
    assertTrue(brokerMsgs.get(0) instanceof Orderbook, "first is orderbook");
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals(0, ob.getAsks().size(), "no uncleared asks");
    assertEquals(1, ob.getBids().size(), "one uncleared bid");
    assertEquals(0.2, ob.getBids().first().getMWh(), 1e-6, "correct qty");
    assertEquals(-21.0, ob.getBids().first().getLimitPrice(), 1e-6, "correct price");
    assertEquals(ts1, ob.getTimeslot(), "correct timeslot");
    assertEquals(20.5, ob.getClearingPrice(), 1e-6, "correct clearing");

    assertTrue(brokerMsgs.get(1) instanceof ClearedTrade, "second is clearedTrade");
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals(ts1, ct.getTimeslot(), "correct timeslot");
    assertEquals(1.0, ct.getExecutionMWh(), 1e-6, "correct mWh");
    assertEquals(20.5, ct.getExecutionPrice(), 1e-6, "correct price");
  }

  // three asks, two bids, all tradeable
  @Test
  public void testActivate2_2_tradeable ()
  {
    Order sell1 = new Order(s1, ts2.getSerialNumber(), -0.9, 18.0);
    Order sell2 = new Order(s2, ts2.getSerialNumber(), -1.0, 20.0);
    Order sell3 = new Order(s2, ts2.getSerialNumber(), -1.0, 21.5);
    Order buy1 = new Order(b1, ts2.getSerialNumber(), 1.4, -21.0);
    Order buy2 = new Order(b2, ts2.getSerialNumber(), 0.6, -22.0);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(sell3);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals(5, svc.getIncoming().size(), "five orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(6, accountingArgs.size(), "accounting: 6 calls");
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(20.5, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(1);
    assertEquals(b2, args[0], "b2"); // b2 had the higher bid price
    assertEquals(ts2, args[1], "ts2");
    assertEquals(0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(-20.5, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(2);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.3, (Double) args[2], 1e-6, "mWh");
    assertEquals(20.5, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(3);
    assertEquals(b1, args[0], "b1"); // b1 had the lower bid price
    assertEquals(ts2, args[1], "ts2");
    assertEquals(0.3, (Double) args[2], 1e-6, "mWh");
    assertEquals(-20.5, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(4);
    assertEquals(s2, args[0], "s2");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(20.5, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(5);
    assertEquals(b1, args[0], "b1"); // b1 had the lower bid price
    assertEquals(ts2, args[1], "ts2");
    assertEquals(1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(-20.5, (Double) args[3], 1e-6, "price");

    // two broker messages
    assertEquals(2, brokerMsgs.size(), "2 messages");
    assertTrue(brokerMsgs.get(0) instanceof Orderbook, "first is orderbook");
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals(ts2, ob.getTimeslot(), "correct timeslot");
    assertEquals(20.5, ob.getClearingPrice(), 1e-6, "correct clearing");
    assertEquals(1, ob.getAsks().size(), "one uncleared ask");
    assertEquals(-1.0, ob.getAsks().first().getMWh(), 1e-6, "correct qty");
    assertEquals(21.5, ob.getAsks().first().getLimitPrice(), 1e-6, "correct price");
    assertEquals(1, ob.getBids().size(), "one uncleared bid");
    assertEquals(0.1, ob.getBids().first().getMWh(), 1e-6, "correct qty");
    assertEquals(-21.0, ob.getBids().first().getLimitPrice(), 1e-6, "correct price");

    assertTrue(brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals(ts2, ct.getTimeslot(), "correct timeslot");
    assertEquals(1.9, ct.getExecutionMWh(), 1e-6, "correct mWh");
    assertEquals(20.5, ct.getExecutionPrice(), 1e-6, "correct price");
  }

  // three asks, two bids, all tradeable
  @Test
  public void testActivate3_eq_tradeable ()
  {
    Order sell1 = new Order(s1, ts2.getSerialNumber(), -0.9, 18.0);
    Order sell2 = new Order(s2, ts2.getSerialNumber(), -1.0, 20.0);
    Order sell3 = new Order(s2, ts2.getSerialNumber(), -1.0, 21.0);
    Order buy1 = new Order(b1, ts2.getSerialNumber(), 0.8, -21.0);
    Order buy1a = new Order(b1, ts2.getSerialNumber(), 0.2, -21.0);
    Order buy1b = new Order(b1, ts2.getSerialNumber(), 0.2, -21.0);
    Order buy1c = new Order(b1, ts2.getSerialNumber(), 0.2, -21.0);
    Order buy2 = new Order(b2, ts2.getSerialNumber(), 0.6, -22.0);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(sell3);
    svc.handleMessage(buy1);
    svc.handleMessage(buy1a);
    svc.handleMessage(buy1b);
    svc.handleMessage(buy1c);
    svc.handleMessage(buy2);
    assertEquals(8, svc.getIncoming().size(), "eight orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(14, accountingArgs.size(), "accounting: 14 calls");
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(1);
    assertEquals(b2, args[0], "b2"); // b2 had the higher bid price
    assertEquals(ts2, args[1], "ts2");
    assertEquals(0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(2);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.3, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(3);
    assertEquals(b1, args[0], "b1"); // b1 had the lower bid price
    assertEquals(ts2, args[1], "ts2");
    assertEquals(0.3, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(4);
    assertEquals(s2, args[0], "s2");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.5, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(5);
    assertEquals(b1, args[0], "b1"); // b1 had the lower bid price
    assertEquals(ts2, args[1], "ts2");
    assertEquals(0.5, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(6);
    assertEquals(s2, args[0], "s2"); // b1 had the lower bid price
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.2, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(7);
    assertEquals(b1, args[0], "b1"); // b1 had the lower bid price
    assertEquals(ts2, args[1], "ts2");
    assertEquals(0.2, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(8);
    assertEquals(s2, args[0], "s2"); // b1 had the lower bid price
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.2, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(9);
    assertEquals(b1, args[0], "b1"); // b1 had the lower bid price
    assertEquals(ts2, args[1], "ts2");
    assertEquals(0.2, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");

    // two broker messages
    assertEquals(2, brokerMsgs.size(), "2 messages");
    assertTrue(brokerMsgs.get(0) instanceof Orderbook, "first is orderbook");
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals(ts2, ob.getTimeslot(), "correct timeslot");
    assertEquals(21.0, ob.getClearingPrice(), 1e-6, "correct clearing");
    assertEquals(1, ob.getAsks().size(), "one uncleared ask");
    assertEquals(-0.9, ob.getAsks().first().getMWh(), 1e-6, "correct qty");
    assertEquals(21.0, ob.getAsks().first().getLimitPrice(), 1e-6, "correct price");
    assertEquals(0, ob.getBids().size(), "no uncleared bids");

    assertTrue(brokerMsgs.get(1) instanceof ClearedTrade, "second is clearedTrade");
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals(ts2, ct.getTimeslot(), "correct timeslot");
    assertEquals(2.0, ct.getExecutionMWh(), 1e-6, "correct mWh");
    assertEquals(21.0, ct.getExecutionPrice(), 1e-6, "correct price");
  }

  // two asks, two bids, market order on one ask
  @Test
  public void marketAskTest ()
  {
    Order sell1 = new Order(s1, ts2Num, -0.9, 18.0);
    Order sell2 = new Order(s2, ts2Num, -1.0, null);
    Order buy1 = new Order(b1, ts2Num, 1.4, -21.0);
    Order buy2 = new Order(b2, ts2Num, 0.6, -22.0);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals(4, svc.getIncoming().size(), "four orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(6, accountingArgs.size(), "accounting: 6 calls");
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals(s2, args[0], "s2");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.6, (Double) args[2], 1e-6, "mWh");
    double exPrice = 18 * 1.05;
    assertEquals(exPrice, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(1);
    assertEquals(b2, args[0], "b2");
    assertEquals(0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(-exPrice, (Double) args[3], 1e-6, "price");

    // check the cleared trade
    assertTrue(brokerMsgs.get(1) instanceof ClearedTrade, "ClearedTrade sent");
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals(ts2, ct.getTimeslot(), "correct timeslot");
    assertEquals(1.9, ct.getExecutionMWh(), 1e-6, "correct mWh");
    assertEquals(exPrice, ct.getExecutionPrice(), 1e-6, "correct price");
  }

  // two asks, two bids, market order on one ask, zero qty bid
  @Test
  public void marketAskZeroTest ()
  {
    Order sell1 = new Order(s1, ts2Num, -1.9, 18.0);
    Order sell2 = new Order(s2, ts2Num, -1.0, null);
    Order buy1 = new Order(b1, ts2Num, 0.0, -24.0);
    Order buy2 = new Order(b2, ts2Num, 4.0, -16.0);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals(3, svc.getIncoming().size(), "four orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(2, accountingArgs.size(), "accounting: 2 calls");
    // first tx should be ask, second bid

    double expectedPrice = 16.0 / 1.2;
    Object[] args = accountingArgs.get(0);
    assertEquals(s2, args[0], "s2");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(expectedPrice, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(1);
    assertEquals(b2, args[0], "b2");
    assertEquals(1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(-expectedPrice, (Double) args[3], 1e-6, "price");

    // check the cleared trade
    assertTrue(brokerMsgs.get(1) instanceof ClearedTrade, "ClearedTrade sent");
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals(ts2, ct.getTimeslot(), "correct timeslot");
    assertEquals(1.0, ct.getExecutionMWh(), 1e-6, "correct mWh");
    assertEquals(expectedPrice, ct.getExecutionPrice(), 1e-6, "correct price");
  }

  // two asks, two bids, market order on one bid
  @Test
  public void marketBidTest ()
  {
    Order sell1 = new Order(s1, ts2Num, -0.9, 18.0);
    Order sell2 = new Order(s2, ts2Num, -1.0, 20.0);
    Order buy1 = new Order(b1, ts2Num, 1.4, -21.0);
    Order buy2 = new Order(b2, ts2Num, 0.6, null);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals(4, svc.getIncoming().size(), "four orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(6, accountingArgs.size(), "accounting: 6 calls");
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(20.5, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(1);
    assertEquals(b2, args[0], "b2");
    assertEquals(0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(-20.5, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(2);
    assertEquals(s1, args[0], "s1");
    assertEquals(-0.3, (Double) args[2], 1e-6, "mWh");
    assertEquals(20.5, (Double) args[3], 1e-6, "price");

    // check the cleared trade
    assertTrue(brokerMsgs.get(1) instanceof ClearedTrade, "ClearedTrade sent");
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals(ts2, ct.getTimeslot(), "correct timeslot");
    assertEquals(1.9, ct.getExecutionMWh(), 1e-6, "correct mWh");
    assertEquals(20.5, ct.getExecutionPrice(), 1e-6, "correct price");
  }

  // two asks, one market bid
  @Test
  public void marketBidClear ()
  {
    Order sell1 = new Order(s1, ts2Num, -0.9, 18.0);
    Order sell2 = new Order(s2, ts2Num, -1.0, 20.0);
    Order buy1 = new Order(b1, ts2Num, 1.4, null);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(buy1);
    assertEquals(3, svc.getIncoming().size(), "three orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(4, accountingArgs.size(), "accounting: 4 calls");
    double expectedPrice = 20 * 1.2;

    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.9, (Double) args[2], 1e-6, "mWh");
    assertEquals(expectedPrice, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(1);
    assertEquals(b1, args[0], "b1");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(0.9, (Double) args[2], 1e-6, "mWh");
    assertEquals(-expectedPrice, (Double) args[3], 1e-6, "price");

    // check the cleared trade
    assertTrue(brokerMsgs.get(1) instanceof ClearedTrade, "ClearedTrade sent");
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals(ts2, ct.getTimeslot(), "correct timeslot");
    assertEquals(1.4, ct.getExecutionMWh(), 1e-6, "correct mWh");
    assertEquals(expectedPrice, ct.getExecutionPrice(), 1e-6, "correct price");
  }

  // two bids, one market ask
  @Test
  public void marketAskClear ()
  {
    Order sell1 = new Order(s1, ts2Num, -1.0, null);
    Order buy1 = new Order(b1, ts2Num, 1.4, -21.0);
    Order buy2 = new Order(b2, ts2Num, 0.6, -22.0);
    svc.handleMessage(sell1);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals(3, svc.getIncoming().size(), "three orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(4, accountingArgs.size(), "accounting: 4 calls");
    double expectedPrice = 21.0 / 1.2;

    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(expectedPrice, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(1);
    assertEquals(b2, args[0], "b2");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(0.6, (Double) args[2], 1e-6, "mWh");
    assertEquals(-expectedPrice, (Double) args[3], 1e-6, "price");

    // check the cleared trade
    assertTrue(brokerMsgs.get(1) instanceof ClearedTrade, "ClearedTrade sent");
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals(ts2, ct.getTimeslot(), "correct timeslot");
    assertEquals(1.0, ct.getExecutionMWh(), 1e-6, "correct mWh");
    assertEquals(expectedPrice, ct.getExecutionPrice(), 1e-6, "correct price");
  }

  // one market ask, one market bid
  @Test
  public void marketClear ()
  {
    Order sell1 = new Order(s1, ts2Num, -1.0, null);
    Order buy1 = new Order(b1, ts2Num, 1.4, null);
    svc.handleMessage(sell1);
    svc.handleMessage(buy1);
    assertEquals(2, svc.getIncoming().size(), "two orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(2, accountingArgs.size(), "accounting: 2 calls");

    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(ts2, args[1], "ts2");
    assertEquals(-1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(svc.getDefaultClearingPrice(), (Double) args[3], 1e-6, "price");
  }

  // three asks, five bids, wide numeric range
  @Test
  public void testNumericRange ()
  {
    competition.withMinimumOrderQuantity(0.001);
    Order sell1 = new Order(s1, ts2Num, -0.036040484378997206, 20.0);
    Order sell2 = new Order(s2, ts2Num, -0.3961457798682808, 21.8);
    Order sell3 = new Order(s2, ts2Num, -26.185209758164312, 35.0);
    Order buy1 = new Order(b1, ts2Num, 6.0, -35.0);
    Order buy2 = new Order(b2, ts2Num, 0.35, -50.0);
    Order buy3 = new Order(b2, ts2Num, 8.728125, null);
    Order buy4 = new Order(b2, ts2Num, 0.0075, -37.0);
    Order buy5 = new Order(b2, ts2Num, 7.875, -35.0);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(sell3);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    svc.handleMessage(buy3);
    svc.handleMessage(buy4);
    svc.handleMessage(buy5);
    assertEquals(8, svc.getIncoming().size(), "eight orders received");
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals(14, accountingArgs.size(), "accounting: 14 calls");
    // first tx should be ask, second bid
    // sell1, buy3, finish off sell1
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(-0.036040484378997206, (Double) args[2], 1e-6, "mWh");
    assertEquals(35.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(1);
    assertEquals(b2, args[0], "b2"); // b2 had market order
    assertEquals(0.036040484378997206, (Double) args[2], 1e-6, "mWh");
    assertEquals(-35.0, (Double) args[3], 1e-6, "price");

    // sell2, buy3, finish off sell2
    args = accountingArgs.get(2);
    assertEquals(s2, args[0], "s2");
    assertEquals(-0.3961457798682808, (Double) args[2], 1e-6, "mWh");
    assertEquals(35.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(3);
    assertEquals(b2, args[0], "b2"); // still working on buy3
    assertEquals(0.3961457798682808, (Double) args[2], 1e-6, "mWh");
    assertEquals(-35.0, (Double) args[3], 1e-6, "price");

    // sell3, buy3, finish off buy3
    args = accountingArgs.get(4);
    assertEquals(s2, args[0], "s2");
    assertEquals(-8.295938736, (Double) args[2], 1e-6, "mWh");
    assertEquals(35.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(5);
    assertEquals(b2, args[0], "b2"); // finish up market order
    assertEquals(8.295938736, (Double) args[2], 1e-6, "mWh");
    assertEquals(-35.0, (Double) args[3], 1e-6, "price");

    // sell3, buy2, finish off buy2
    args = accountingArgs.get(6);
    assertEquals(s2, args[0], "s2");
    assertEquals(-0.35, (Double) args[2], 1e-6, "mWh");
    assertEquals(35.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(7);
    assertEquals(b2, args[0], "b2"); // finish up market order
    assertEquals(0.35, (Double) args[2], 1e-6, "mWh");
    assertEquals(-35.0, (Double) args[3], 1e-6, "price");

    // sell3, buy4, finish off buy4
    args = accountingArgs.get(8);
    assertEquals(s2, args[0], "s2");
    assertEquals(-0.0075, (Double) args[2], 1e-6, "mWh");
    assertEquals(35.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(9);
    assertEquals(b2, args[0], "b2"); // finish up market order
    assertEquals(0.0075, (Double) args[2], 1e-6, "mWh");
    assertEquals(-35.0, (Double) args[3], 1e-6, "price");

    // sell3, buy1/5
    args = accountingArgs.get(10);
    Object[] buyArgs = accountingArgs.get(11);
    Broker b11 = (Broker) buyArgs[0];
    if (b11 == b2) {
      // buy5
      assertEquals(-7.875, (Double) args[2], 1e-6, "mWh");
    }
    else {
      // buy1
      assertEquals(-6.0, (Double) args[2], 1e-6, "mWh");
    }

    // sell3, buy 1/5
    args = accountingArgs.get(12);
    buyArgs = accountingArgs.get(13);
    Broker b13 = (Broker) buyArgs[0];
    assertTrue(b11 != b13, "b1 != b2");
    if (b13 == b2) {
      // buy5
      assertEquals(-7.875, (Double) args[2], 1e-6, "mWh");
    }
    else {
      // buy1
      assertEquals(-6.0, (Double) args[2], 1e-6, "mWh");
    }

    // check minAsk data
    Double[] minAsks = orderbookRepo.getMinAskPrices();
    assertEquals(4, minAsks.length, "four prices");
    assertNull(minAsks[0], "first price null");
    assertEquals(sell1.getLimitPrice(), minAsks[1], 1e-6, "correct first min price");
    assertNull(minAsks[2], "third price null");
    assertNull(minAsks[3], "fourth price null");

    // check maxAsk data
    Double[] maxAsks = orderbookRepo.getMaxAskPrices();
    assertEquals(4, maxAsks.length, "four prices");
    assertNull(maxAsks[0], "first price null");
    assertEquals(sell3.getLimitPrice(), maxAsks[1], 1e-6, "correct first max price");
    assertNull(maxAsks[2], "third price null");
    assertNull(maxAsks[3], "fourth price null");
  }

  // three asks, five bids, wide numeric range
  @Test
  public void testPositionLimit24h ()
  {
    log.info("testPositionLimit24h");
    competition.withMinimumOrderQuantity(0.001);
    // activate once to get enabledTimeslots initialized correctly
    svc.activate(timeService.getCurrentTime(), 1);

    // supply is 150 in each ts
    // use ts1, ts2, and t4
    int ts1 = 1;
    int ts2 = 2;
    int ts4 = 4;
    svc.handleMessage(new Order(s1, ts1, -36.0, 20.0));
    svc.handleMessage(new Order(s2, ts1, -39.0, 30.0));
    svc.handleMessage(new Order(s2, ts1, -75.0, 50.0));
    svc.handleMessage(new Order(s1, ts2, -36.0, 20.0));
    svc.handleMessage(new Order(s2, ts2, -39.0, 30.0));
    svc.handleMessage(new Order(s2, ts2, -75.0, 50.0));
    svc.handleMessage(new Order(s1, ts4, -36.0, 20.0));
    svc.handleMessage(new Order(s2, ts4, -39.0, 30.0));
    svc.handleMessage(new Order(s2, ts4, -75.0, 50.0));

    // max for ts1 is 143
    MarketPosition mp1 =
        new MarketPosition(b2, ts1, 130.0);
    b2.addMarketPosition(mp1, ts1);
    svc.handleMessage(new Order(b2, ts1, 6.0, -55.0));
    svc.handleMessage(new Order(b2, ts1, 1.0, -60.0));
    svc.handleMessage(new Order(b2, ts1, 8.0, null));
    // these should clear as 8.0, 1.0, 4.0 to stay under 143

    // max for ts2 is 90 + 2 * (143 - 90) / 3 = 125.3333...
    MarketPosition mp2 =
        new MarketPosition(b2, ts2, 115.0);
    b2.addMarketPosition(mp2, ts2);
    svc.handleMessage(new Order(b2, ts2, 10.0, -37.0));
    svc.handleMessage(new Order(b2, ts2, 10.0, -35.0));
    // these should clear as 10.0, 0.333333333

    // max for ts4 is 90.0
    MarketPosition mp4 =
        new MarketPosition(b2, ts4, 0.0);
    b2.addMarketPosition(mp4, ts4);
    svc.handleMessage(new Order(b2, ts4, 40.0, -56.0));
    svc.handleMessage(new Order(b2, ts4, 35.0, -60.0));
    svc.handleMessage(new Order(b2, ts4, 30.0, -70.0));
    // these should clear as 30.0, 35.0, 25.0

    assertEquals(17, svc.getIncoming().size(), "17 orders received");
    // Advance time before activation, otherwise offsets are incorrect
    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(TimeService.HOUR));
    svc.activate(timeService.getCurrentTime(), 1);
    assertEquals(20, accountingArgs.size(), "accounting: 20 calls");
    // first tx should be ask, second bid
    // For ts1, we should have quantities 8, 1, 4
    Object[] args = accountingArgs.get(0);
    assertEquals(s1, args[0], "s1");
    assertEquals(-8.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(1);
    assertEquals(b2, args[0], "b2"); // b2 had market order
    assertEquals(8, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(2);
    assertEquals(s1, args[0], "s1");
    assertEquals(-1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(3);
    assertEquals(b2, args[0], "b2");
    assertEquals(1.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(4);
    assertEquals(s1, args[0], "s1");
    assertEquals(-4.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(5);
    assertEquals(b2, args[0], "b2");
    assertEquals(4.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");

    // for ts2, quantities should be 10.0, 4.75, all from s1@21
    args = accountingArgs.get(6);
    assertEquals(s1, args[0], "s1");
    assertEquals(-10.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(7);
    assertEquals(b2, args[0], "b2"); 
    assertEquals(10.0, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(8);
    assertEquals(s1, args[0], "s1");
    assertEquals(-0.33333333, (Double) args[2], 1e-6, "mWh");
    assertEquals(21.0, (Double) args[3], 1e-6, "price");

    args = accountingArgs.get(9);
    assertEquals(b2, args[0], "b2");
    assertEquals(0.333333333, (Double) args[2], 1e-6, "mWh");
    assertEquals(-21.0, (Double) args[3], 1e-6, "price");

    // ts4 should be 30, 35, 25 at 52.5
    // from s1 we get 30 + 6, from s2 we get 29 + 10 + 25
    args = accountingArgs.get(10);
    assertEquals(s1, args[0], "s1");
    assertEquals(-30.0, (Double) args[2], 1e-6, "30 MWh");
    assertEquals(52.5, (Double) args[3], 1e-6, "price 52.5");

    args = accountingArgs.get(11);
    assertEquals(b2, args[0], "b2");
    assertEquals(30.0, (Double) args[2], 1e-6, "30 MWh");
    assertEquals(-52.5, (Double) args[3], 1e-6, "price 52.5");

    args = accountingArgs.get(12);
    assertEquals(s1, args[0], "s1");
    assertEquals(-6.0, (Double) args[2], 1e-6, "6 MWh");
    assertEquals(52.5, (Double) args[3], 1e-6, "price 52.5");

    args = accountingArgs.get(13);
    assertEquals(b2, args[0], "b2");
    assertEquals(6.0, (Double) args[2], 1e-6, "6 MWh");
    assertEquals(-52.5, (Double) args[3], 1e-6, "price 52.5");

    args = accountingArgs.get(14);
    assertEquals(s2, args[0], "s2");
    assertEquals(-29.0, (Double) args[2], 1e-6, "29 MWh");
    assertEquals(52.5, (Double) args[3], 1e-6, "price 52.5");

    args = accountingArgs.get(15);
    assertEquals(b2, args[0], "b2");
    assertEquals(29.0, (Double) args[2], 1e-6, "29 MWh");
    assertEquals(-52.5, (Double) args[3], 1e-6, "price 52.5");

    args = accountingArgs.get(16);
    assertEquals(s2, args[0], "s2");
    assertEquals(-10.0, (Double) args[2], 1e-6, "10 MWh");
    assertEquals(52.5, (Double) args[3], 1e-6, "price 52.5");

    args = accountingArgs.get(17);
    assertEquals(b2, args[0], "b2");
    assertEquals(10.0, (Double) args[2], 1e-6, "10 MWh");
    assertEquals(-52.5, (Double) args[3], 1e-6, "price 52.5");

    args = accountingArgs.get(18);
    assertEquals(s2, args[0], "s2");
    assertEquals(-15.0, (Double) args[2], 1e-6, "15 MWh");
    assertEquals(52.5, (Double) args[3], 1e-6, "price 52.5");

    args = accountingArgs.get(19);
    assertEquals(b2, args[0], "b2");
    assertEquals(15.0, (Double) args[2], 1e-6, "15 MWh");
    assertEquals(-52.5, (Double) args[3], 1e-6, "price 52.5");

  }

  @Test
  public void testQuantitytValidity ()
  {
    Order buy1 = new Order(b2, ts2.getSerialNumber(), 0.75, -37.0);
    Order buy2 = new Order(b2, ts2.getSerialNumber(), Double.NaN, -35.0);
    Order buy3 = new Order(b2, ts2.getSerialNumber(), Double.POSITIVE_INFINITY, -35.0);
    Order buy4 = new Order(b2, ts2.getSerialNumber(), Double.NEGATIVE_INFINITY, -35.0);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    svc.handleMessage(buy3);
    svc.handleMessage(buy4);

    assertEquals(1, svc.getIncoming().size(), "one order validated");
  }
}
