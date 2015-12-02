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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Competition;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;


/**
 * Test cases for AuctionService
 *
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class AuctionServiceTests
{
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
  private Timeslot ts2;
  //private Timeslot ts3;
  //private Timeslot ts4;

  private List<Object[]> accountingArgs;
  private List<Object> brokerMsgs;

  @SuppressWarnings("rawtypes")
  @Before
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
    }).when(mockServerProps).configureMe(anyObject());

    // Create some brokers who can trade
    b1 = new Broker("Buyer #1");
    b2 = new Broker("Buyer #2");
    s1 = new Broker("Seller #1");
    s2 = new Broker("Seller #2");

    // set the clock, create some useful timeslots
    Instant now = Competition.currentCompetition().getSimulationBaseTime();
    timeService.setCurrentTime(now);
    ts0 = timeslotRepo.makeTimeslot(now);
    ts1 = timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR));
    ts2 = timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR * 2));
    //timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR * 3));
    //timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR * 4));
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
    }).when(mockProxy).broadcastMessage(anyObject());

    // Configure the AuctionService
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("auctioneer.auctionService.sellerSurplusRatio", "0.5");
    map.put("auctioneer.auctionService.defaultMargin", "0.2");
    map.put("auctioneer.auctionService.defaultClearingPrice", "40.0");
    Configuration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    svc.initialize(competition, new ArrayList<String>());
  }

  @Test
  public void testAuctionService ()
  {
    assertNotNull("auction service created", svc);
    assertEquals("correct surplus", 0.5, svc.getSellerSurplusRatio(), 1e-6);
    assertEquals("empty incoming", 0, svc.getIncoming().size());
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
    assertEquals("one order received", 1, svc.getIncoming().size());
  }

  @Test
  public void testValidateOrder ()
  {
    // mock the Broker Proxy for b1, capture messages
    doAnswer(new Answer()
    {
      @Override
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        brokerMsgs.add(args[1]);
        return null;
      }
    }).when(mockProxy).sendMessage(eq(b1), anyObject());

    competition.withMinimumOrderQuantity(0.1);
    Order good = new Order(b1, ts1.getSerialNumber(), 1.0, -22.0);
    assertTrue("ts1 enabled", timeslotRepo.isTimeslotEnabled(ts1));
    assertTrue("next timeslot valid", svc.validateOrder(good));

    Order bogus = new Order(b1, ts0.getSerialNumber(), 1.0, -22.0);
    assertFalse("ts0 not enabled", timeslotRepo.isTimeslotEnabled(ts0));
    assertFalse("current timeslot not valid", svc.validateOrder(bogus));
    assertEquals("1 message sent", 1, brokerMsgs.size());
    OrderStatus status = (OrderStatus) brokerMsgs.get(0);
    assertNotNull("status got sent", status);
    assertEquals("correct broker", b1, status.getBroker());
    assertEquals("correct order", bogus.getId(), status.getOrderId());
    
    Order smallSell = new Order(b1, ts1.getSerialNumber(), 0.09, -22.0);
    assertFalse("too small buy", svc.validateOrder(smallSell));
    Order smallBuy = new Order(b1, ts1.getSerialNumber(), -0.08, 22.0);
    assertFalse("too small buy", svc.validateOrder(smallBuy));
  }

  // one ask, one bid, equal qty, tradeable
  @Test
  public void testActivate1 ()
  {
    Order sell = new Order(s1, ts1.getSerialNumber(), -1.0, 20.0);
    Order buy = new Order(b1, ts1.getSerialNumber(), 1.0, -22.0);
    svc.handleMessage(sell);
    svc.handleMessage(buy);
    assertEquals("two orders received", 2, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting called twice", 2, accountingArgs.size());
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts1", ts1, args[1]);
    assertEquals("mWh", -1.0, (Double) args[2], 1e-6);
    assertEquals("price", 21.0, (Double) args[3], 1e-6);
    args = accountingArgs.get(1);
    assertEquals("b1", b1, args[0]);
    assertEquals("ts1", ts1, args[1]);
    assertEquals("mWh", 1.0, (Double) args[2], 1e-6);
    assertEquals("price", -21.0, (Double) args[3], 1e-6);
    // two broker messages
    assertEquals("2 messages", 2, brokerMsgs.size());
    assertTrue("first is orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals("no uncleared asks", 0, ob.getAsks().size());
    assertEquals("no uncleared bids", 0, ob.getBids().size());
    assertEquals("correct timeslot", ts1, ob.getTimeslot());
    assertEquals("correct clearing", 21.0, ob.getClearingPrice(), 1e-6);
    assertTrue("second is clearedTrade", brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals("correct timeslot", ts1, ct.getTimeslot());
    assertEquals("correct mWh", 1.0, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", 21.0, ct.getExecutionPrice(), 1e-6);
    // check minAsk data
    Double[] minAsks = orderbookRepo.getMinAskPrices();
    assertEquals("four prices", 4, minAsks.length);
    assertEquals("correct first price", 20.0, minAsks[0], 1e-6);
    assertNull("second price null", minAsks[1]);
    assertNull("third price null", minAsks[2]);
    assertNull("fourth price null", minAsks[3]);
  }

  // one ask, one bid, equal qty, not tradeable
  @Test
  public void testActivate1_no ()
  {
    Order sell = new Order(s1, ts1.getSerialNumber(), -1.0, 23.0);
    Order buy = new Order(b1, ts1.getSerialNumber(), 1.0, -22.0);
    svc.handleMessage(sell);
    svc.handleMessage(buy);
    assertEquals("two orders received", 2, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting not called", 0, accountingArgs.size());
    // one broker message
    assertEquals("one message", 1, brokerMsgs.size());
    assertTrue("first is orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertNull("null clearing price", ob.getClearingPrice());
    assertEquals("one uncleared ask", 1, ob.getAsks().size());
    assertEquals("correct qty", -1.0,
        ob.getAsks().first().getMWh(), 1e-6);
    assertEquals("correct price", 23.0,
        ob.getAsks().first().getLimitPrice(), 1e-6);
    assertEquals("one uncleared bid", 1, ob.getBids().size());
    assertEquals("correct qty", 1.0,
        ob.getBids().first().getMWh(), 1e-6);
    assertEquals("correct price", -22.0,
        ob.getBids().first().getLimitPrice(), 1e-6);
    // check minAsk data
    Double[] minAsks = orderbookRepo.getMinAskPrices();
    assertEquals("four prices", 4, minAsks.length);
    assertEquals("correct first price", 23.0, minAsks[0], 1e-6);
    assertNull("second price null", minAsks[1]);
    assertNull("third price null", minAsks[2]);
    assertNull("fourth price null", minAsks[3]);
  }

  // one ask, one bid, equal qty, different timeslots
  @Test
  public void testActivate1_ts ()
  {
    Order sell = new Order(s1, ts1.getSerialNumber(), -1.0, 23.0);
    Order buy = new Order(b1, ts2.getSerialNumber(), 1.0, -22.0);
    svc.handleMessage(sell);
    svc.handleMessage(buy);
    assertEquals("two orders received", 2, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting not called", 0, accountingArgs.size());
    // two broker messages, one for each timeslot
    assertEquals("two messages", 2, brokerMsgs.size());
    assertTrue("ts1 orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals("ts1", ts1, ob.getTimeslot());
    assertNull("null clearing price", ob.getClearingPrice());
    assertEquals("one uncleared ask", 1, ob.getAsks().size());
    assertEquals("no uncleared bids", 0, ob.getBids().size());
    assertEquals("correct qty", -1.0,
        ob.getAsks().first().getMWh(), 1e-6);
    assertEquals("correct price", 23.0,
        ob.getAsks().first().getLimitPrice(), 1e-6);

    assertTrue("ts2 orderbook", brokerMsgs.get(0) instanceof Orderbook);
    ob = (Orderbook) brokerMsgs.get(1);
    assertEquals("ts2", ts2, ob.getTimeslot());
    assertNull("null clearing price", ob.getClearingPrice());
    assertEquals("no uncleared asks", 0, ob.getAsks().size());
    assertEquals("one uncleared bid", 1, ob.getBids().size());
    assertEquals("correct qty", 1.0,
        ob.getBids().first().getMWh(), 1e-6);
    assertEquals("correct price", -22.0,
        ob.getBids().first().getLimitPrice(), 1e-6);
  }

  // one ask, two bids, all tradeable
  @Test
  public void testActivate1_2_tradeable ()
  {
    Order sell = new Order(s1, ts1, -1.0, 20.0);
    Order buy1 = new Order(b1, ts1, 0.6, -21.0);
    Order buy2 = new Order(b2, ts1, 0.6, -22.0);
    svc.handleMessage(sell);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals("three orders received", 3, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 4 calls", 4, accountingArgs.size());
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts1", ts1, args[1]);
    assertEquals("mWh", -0.6, (Double) args[2], 1e-6);
    assertEquals("price", 20.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b2", b2, args[0]); // b2 had the higher bid price
    assertEquals("ts1", ts1, args[1]);
    assertEquals("mWh", 0.6, (Double) args[2], 1e-6);
    assertEquals("price", -20.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(2);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts1", ts1, args[1]);
    assertEquals("mWh", -0.4, (Double) args[2], 1e-6);
    assertEquals("price", 20.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(3);
    assertEquals("b1", b1, args[0]); // b1 had the lower bid price
    assertEquals("ts1", ts1, args[1]);
    assertEquals("mWh", 0.4, (Double) args[2], 1e-6);
    assertEquals("price", -20.5, (Double) args[3], 1e-6);
    // two broker messages
    assertEquals("2 messages", 2, brokerMsgs.size());
    assertTrue("first is orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals("no uncleared asks", 0, ob.getAsks().size());
    assertEquals("one uncleared bid", 1, ob.getBids().size());
    assertEquals("correct qty", 0.2,
        ob.getBids().first().getMWh(), 1e-6);
    assertEquals("correct price", -21.0,
        ob.getBids().first().getLimitPrice(), 1e-6);
    assertEquals("correct timeslot", ts1, ob.getTimeslot());
    assertEquals("correct clearing", 20.5, ob.getClearingPrice(), 1e-6);

    assertTrue("second is clearedTrade",
        brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals("correct timeslot", ts1, ct.getTimeslot());
    assertEquals("correct mWh", 1.0, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", 20.5, ct.getExecutionPrice(), 1e-6);
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
    assertEquals("five orders received", 5, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 6 calls", 6, accountingArgs.size());
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.6, (Double) args[2], 1e-6);
    assertEquals("price", 20.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b2", b2, args[0]); // b2 had the higher bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", 0.6, (Double) args[2], 1e-6);
    assertEquals("price", -20.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(2);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.3, (Double) args[2], 1e-6);
    assertEquals("price", 20.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(3);
    assertEquals("b1", b1, args[0]); // b1 had the lower bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", 0.3, (Double) args[2], 1e-6);
    assertEquals("price", -20.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(4);
    assertEquals("s2", s2, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -1.0, (Double) args[2], 1e-6);
    assertEquals("price", 20.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(5);
    assertEquals("b1", b1, args[0]); // b1 had the lower bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", 1.0, (Double) args[2], 1e-6);
    assertEquals("price", -20.5, (Double) args[3], 1e-6);

    // two broker messages
    assertEquals("2 messages", 2, brokerMsgs.size());
    assertTrue("first is orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals("correct timeslot", ts2, ob.getTimeslot());
    assertEquals("correct clearing", 20.5, ob.getClearingPrice(), 1e-6);
    assertEquals("one uncleared ask", 1, ob.getAsks().size());
    assertEquals("correct qty", -1.0,
        ob.getAsks().first().getMWh(), 1e-6);
    assertEquals("correct price", 21.5,
        ob.getAsks().first().getLimitPrice(), 1e-6);
    assertEquals("one uncleared bid", 1, ob.getBids().size());
    assertEquals("correct qty", 0.1,
        ob.getBids().first().getMWh(), 1e-6);
    assertEquals("correct price", -21.0,
        ob.getBids().first().getLimitPrice(), 1e-6);

    assertTrue("second is clearedTrade",
        brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals("correct timeslot", ts2, ct.getTimeslot());
    assertEquals("correct mWh", 1.9, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", 20.5, ct.getExecutionPrice(), 1e-6);
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
    assertEquals("eight orders received", 8, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 14 calls", 14, accountingArgs.size());
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.6, (Double) args[2], 1e-6);
    assertEquals("price", 21.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b2", b2, args[0]); // b2 had the higher bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", 0.6, (Double) args[2], 1e-6);
    assertEquals("price", -21.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(2);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.3, (Double) args[2], 1e-6);
    assertEquals("price", 21.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(3);
    assertEquals("b1", b1, args[0]); // b1 had the lower bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", 0.3, (Double) args[2], 1e-6);
    assertEquals("price", -21.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(4);
    assertEquals("s2", s2, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.5, (Double) args[2], 1e-6);
    assertEquals("price", 21.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(5);
    assertEquals("b1", b1, args[0]); // b1 had the lower bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", 0.5, (Double) args[2], 1e-6);
    assertEquals("price", -21.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(6);
    assertEquals("s2", s2, args[0]); // b1 had the lower bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.2, (Double) args[2], 1e-6);
    assertEquals("price", 21.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(7);
    assertEquals("b1", b1, args[0]); // b1 had the lower bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", 0.2, (Double) args[2], 1e-6);
    assertEquals("price", -21.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(8);
    assertEquals("s2", s2, args[0]); // b1 had the lower bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.2, (Double) args[2], 1e-6);
    assertEquals("price", 21.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(9);
    assertEquals("b1", b1, args[0]); // b1 had the lower bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", 0.2, (Double) args[2], 1e-6);
    assertEquals("price", -21.0, (Double) args[3], 1e-6);

    // two broker messages
    assertEquals("2 messages", 2, brokerMsgs.size());
    assertTrue("first is orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook) brokerMsgs.get(0);
    assertEquals("correct timeslot", ts2, ob.getTimeslot());
    assertEquals("correct clearing", 21.0, ob.getClearingPrice(), 1e-6);
    assertEquals("one uncleared ask", 1, ob.getAsks().size());
    assertEquals("correct qty", -0.9,
        ob.getAsks().first().getMWh(), 1e-6);
    assertEquals("correct price", 21.0,
        ob.getAsks().first().getLimitPrice(), 1e-6);
    assertEquals("no uncleared bids", 0, ob.getBids().size());

    assertTrue("second is clearedTrade",
        brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals("correct timeslot", ts2, ct.getTimeslot());
    assertEquals("correct mWh", 2.0, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", 21.0, ct.getExecutionPrice(), 1e-6);
  }

  // two asks, two bids, market order on one ask
  @Test
  public void marketAskTest ()
  {
    Order sell1 = new Order(s1, ts2, -0.9, 18.0);
    Order sell2 = new Order(s2, ts2, -1.0, null);
    Order buy1 = new Order(b1, ts2, 1.4, -21.0);
    Order buy2 = new Order(b2, ts2, 0.6, -22.0);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals("four orders received", 4, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 6 calls", 6, accountingArgs.size());
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s2", s2, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.6, (Double) args[2], 1e-6);
    assertEquals("price", 19.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b2", b2, args[0]);
    assertEquals("mWh", 0.6, (Double) args[2], 1e-6);
    assertEquals("price", -19.5, (Double) args[3], 1e-6);

    // check the cleared trade
    assertTrue("ClearedTrade sent",
        brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals("correct timeslot", ts2, ct.getTimeslot());
    assertEquals("correct mWh", 1.9, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", 19.5, ct.getExecutionPrice(), 1e-6);
  }

  // two asks, two bids, market order on one ask, zero qty bid
  @Test
  public void marketAskZeroTest ()
  {
    Order sell1 = new Order(s1, ts2, -1.9, 18.0);
    Order sell2 = new Order(s2, ts2, -1.0, null);
    Order buy1 = new Order(b1, ts2, 0.0, -24.0);
    Order buy2 = new Order(b2, ts2, 4.0, -16.0);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals("four orders received", 3, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 2 calls", 2, accountingArgs.size());
    // first tx should be ask, second bid

    double expectedPrice = 16.0 / 1.2;
    Object[] args = accountingArgs.get(0);
    assertEquals("s2", s2, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -1.0, (Double) args[2], 1e-6);
    assertEquals("price", expectedPrice, (Double) args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b2", b2, args[0]);
    assertEquals("mWh", 1.0, (Double) args[2], 1e-6);
    assertEquals("price", -expectedPrice, (Double) args[3], 1e-6);

    // check the cleared trade
    assertTrue("ClearedTrade sent",
        brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals("correct timeslot", ts2, ct.getTimeslot());
    assertEquals("correct mWh", 1.0, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", expectedPrice, ct.getExecutionPrice(), 1e-6);
  }

  // two asks, two bids, market order on one bid
  @Test
  public void marketBidTest ()
  {
    Order sell1 = new Order(s1, ts2, -0.9, 18.0);
    Order sell2 = new Order(s2, ts2, -1.0, 20.0);
    Order buy1 = new Order(b1, ts2, 1.4, -21.0);
    Order buy2 = new Order(b2, ts2, 0.6, null);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals("four orders received", 4, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 6 calls", 6, accountingArgs.size());
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.6, (Double) args[2], 1e-6);
    assertEquals("price", 20.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b2", b2, args[0]);
    assertEquals("mWh", 0.6, (Double) args[2], 1e-6);
    assertEquals("price", -20.5, (Double) args[3], 1e-6);

    args = accountingArgs.get(2);
    assertEquals("s1", s1, args[0]);
    assertEquals("mWh", -0.3, (Double) args[2], 1e-6);
    assertEquals("price", 20.5, (Double) args[3], 1e-6);

    // check the cleared trade
    assertTrue("ClearedTrade sent",
        brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals("correct timeslot", ts2, ct.getTimeslot());
    assertEquals("correct mWh", 1.9, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", 20.5, ct.getExecutionPrice(), 1e-6);
  }

  // two asks, one market bid
  @Test
  public void marketBidClear ()
  {
    Order sell1 = new Order(s1, ts2, -0.9, 18.0);
    Order sell2 = new Order(s2, ts2, -1.0, 20.0);
    Order buy1 = new Order(b1, ts2, 1.4, null);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(buy1);
    assertEquals("three orders received", 3, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 4 calls", 4, accountingArgs.size());
    double expectedPrice = 20 * 1.2;

    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.9, (Double) args[2], 1e-6);
    assertEquals("price", expectedPrice, (Double) args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b1", b1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", 0.9, (Double) args[2], 1e-6);
    assertEquals("price", -expectedPrice, (Double) args[3], 1e-6);

    // check the cleared trade
    assertTrue("ClearedTrade sent",
        brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals("correct timeslot", ts2, ct.getTimeslot());
    assertEquals("correct mWh", 1.4, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", expectedPrice,
        ct.getExecutionPrice(), 1e-6);
  }

  // two bids, one market ask
  @Test
  public void marketAskClear ()
  {
    Order sell1 = new Order(s1, ts2, -1.0, null);
    Order buy1 = new Order(b1, ts2, 1.4, -21.0);
    Order buy2 = new Order(b2, ts2, 0.6, -22.0);
    svc.handleMessage(sell1);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    assertEquals("three orders received", 3, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 4 calls", 4, accountingArgs.size());
    double expectedPrice = 21.0 / 1.2;

    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -0.6, (Double) args[2], 1e-6);
    assertEquals("price", expectedPrice, (Double) args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b2", b2, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", 0.6, (Double) args[2], 1e-6);
    assertEquals("price", -expectedPrice, (Double) args[3], 1e-6);

    // check the cleared trade
    assertTrue("ClearedTrade sent",
        brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade) brokerMsgs.get(1);
    assertEquals("correct timeslot", ts2, ct.getTimeslot());
    assertEquals("correct mWh", 1.0, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", expectedPrice,
        ct.getExecutionPrice(), 1e-6);
  }

  // one market ask, one market bid
  @Test
  public void marketClear ()
  {
    Order sell1 = new Order(s1, ts2, -1.0, null);
    Order buy1 = new Order(b1, ts2, 1.4, null);
    svc.handleMessage(sell1);
    svc.handleMessage(buy1);
    assertEquals("two orders received", 2, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 2 calls", 2, accountingArgs.size());

    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("mWh", -1.0, (Double) args[2], 1e-6);
    assertEquals("price", svc.getDefaultClearingPrice(), (Double) args[3], 1e-6);
  }

  // three asks, five bids, wide numeric range
  @Test
  public void testNumericRange ()
  {
    competition.withMinimumOrderQuantity(0.001);
    Order sell1 = new Order(s1, ts2, -0.036040484378997206, 20.0);
    Order sell2 = new Order(s2, ts2, -0.3961457798682808, 21.8);
    Order sell3 = new Order(s2, ts2, -26.185209758164312, 35.0);
    Order buy1 = new Order(b1, ts2, 6.0, -35.0);
    Order buy2 = new Order(b2, ts2, 0.35, -50.0);
    Order buy3 = new Order(b2, ts2, 8.728125, null);
    Order buy4 = new Order(b2, ts2, 0.0075, -37.0);
    Order buy5 = new Order(b2, ts2, 7.875, -35.0);
    svc.handleMessage(sell1);
    svc.handleMessage(sell2);
    svc.handleMessage(sell3);
    svc.handleMessage(buy1);
    svc.handleMessage(buy2);
    svc.handleMessage(buy3);
    svc.handleMessage(buy4);
    svc.handleMessage(buy5);
    assertEquals("eight orders received", 8, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 14 calls", 14, accountingArgs.size());
    // first tx should be ask, second bid
    // sell1, buy3, finish off sell1
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("mWh", -0.036040484378997206, (Double) args[2], 1e-6);
    assertEquals("price", 35.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b2", b2, args[0]); // b2 had market order
    assertEquals("mWh", 0.036040484378997206, (Double) args[2], 1e-6);
    assertEquals("price", -35.0, (Double) args[3], 1e-6);

    // sell2, buy3, finish off sell2
    args = accountingArgs.get(2);
    assertEquals("s2", s2, args[0]);
    assertEquals("mWh", -0.3961457798682808, (Double) args[2], 1e-6);
    assertEquals("price", 35.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(3);
    assertEquals("b2", b2, args[0]); // still working on buy3
    assertEquals("mWh", 0.3961457798682808, (Double) args[2], 1e-6);
    assertEquals("price", -35.0, (Double) args[3], 1e-6);

    // sell3, buy3, finish off buy3
    args = accountingArgs.get(4);
    assertEquals("s2", s2, args[0]);
    assertEquals("mWh", -8.295938736, (Double) args[2], 1e-6);
    assertEquals("price", 35.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(5);
    assertEquals("b2", b2, args[0]); // finish up market order
    assertEquals("mWh", 8.295938736, (Double) args[2], 1e-6);
    assertEquals("price", -35.0, (Double) args[3], 1e-6);

    // sell3, buy2, finish off buy2
    args = accountingArgs.get(6);
    assertEquals("s2", s2, args[0]);
    assertEquals("mWh", -0.35, (Double) args[2], 1e-6);
    assertEquals("price", 35.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(7);
    assertEquals("b2", b2, args[0]); // finish up market order
    assertEquals("mWh", 0.35, (Double) args[2], 1e-6);
    assertEquals("price", -35.0, (Double) args[3], 1e-6);

    // sell3, buy4, finish off buy4
    args = accountingArgs.get(8);
    assertEquals("s2", s2, args[0]);
    assertEquals("mWh", -0.0075, (Double) args[2], 1e-6);
    assertEquals("price", 35.0, (Double) args[3], 1e-6);

    args = accountingArgs.get(9);
    assertEquals("b2", b2, args[0]); // finish up market order
    assertEquals("mWh", 0.0075, (Double) args[2], 1e-6);
    assertEquals("price", -35.0, (Double) args[3], 1e-6);

    // sell3, buy1/5
    args = accountingArgs.get(10);
    Object[] buyArgs = accountingArgs.get(11);
    Broker b11 = (Broker) buyArgs[0];
    if (b11 == b2) {
      // buy5
      assertEquals("mWh", -7.875, (Double) args[2], 1e-6);
    }
    else {
      // buy1
      assertEquals("mWh", -6.0, (Double) args[2], 1e-6);
    }

    // sell3, buy 1/5
    args = accountingArgs.get(12);
    buyArgs = accountingArgs.get(13);
    Broker b13 = (Broker) buyArgs[0];
    assertTrue("b1 != b2", b11 != b13);
    if (b13 == b2) {
      // buy5
      assertEquals("mWh", -7.875, (Double) args[2], 1e-6);
    }
    else {
      // buy1
      assertEquals("mWh", -6.0, (Double) args[2], 1e-6);
    }

    // check minAsk data
    Double[] minAsks = orderbookRepo.getMinAskPrices();
    assertEquals("four prices", 4, minAsks.length);
    assertNull("first price null", minAsks[0]);
    assertEquals("correct first min price", sell1.getLimitPrice(), minAsks[1], 1e-6);
    assertNull("third price null", minAsks[2]);
    assertNull("fourth price null", minAsks[3]);

    // check maxAsk data
    Double[] maxAsks = orderbookRepo.getMaxAskPrices();
    assertEquals("four prices", 4, maxAsks.length);
    assertNull("first price null", maxAsks[0]);
    assertEquals("correct first max price", sell3.getLimitPrice(), maxAsks[1], 1e-6);
    assertNull("third price null", maxAsks[2]);
    assertNull("fourth price null", maxAsks[3]);
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

    assertEquals("one order validated", 1, svc.getIncoming().size());
  }
}
