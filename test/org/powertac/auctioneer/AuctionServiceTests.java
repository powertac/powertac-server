package org.powertac.auctioneer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Competition;
import org.powertac.common.Orderbook;
import org.powertac.common.Order;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/test-config.xml"})
@DirtiesContext
public class AuctionServiceTests
{
  @Autowired
  private AuctionService svc;
  
  @Autowired
  private AuctionInitializationService auctionInitializationService;
  
  @Autowired
  private TimeService timeService;

  @Autowired
  private Accounting accountingService;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  // get access to the mock services
  @Autowired
  private BrokerProxy mockProxy;
  
  @Autowired
  private CompetitionControl mockControl;
  
  private Competition competition;
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
  
  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @SuppressWarnings("rawtypes")
  @Before
  public void setUp () throws Exception
  {
    // clean up from previous tests
    pluginConfigRepo.recycle();
    timeslotRepo.recycle();
    reset(mockProxy);
    reset(mockControl);
    accountingArgs = new ArrayList<Object[]>();
    brokerMsgs = new ArrayList<Object>();
    
    // create a Competition, needed for initialization
    competition = Competition.newInstance("auctioneer-test");

    // Configure the AuctionService
    auctionInitializationService.setDefaults();
    auctionInitializationService.initialize(competition,
                                            new ArrayList<String>());
    
    // Create some brokers who can trade
    b1 = new Broker("Buyer #1");
    b2 = new Broker("Buyer #2");
    s1 = new Broker("Seller #1");
    s2 = new Broker("Seller #2");
    
    // set the clock, create some useful timeslots
    Instant now = new DateTime(2011, 1, 26, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(now);
    ts0 = timeslotRepo.makeTimeslot(now);
    ts0.disable();
    ts1 = timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR));
    ts2 = timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR * 2));
    timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR * 3));
    timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR * 4));
    
    // mock the AccountingService, capture args
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        accountingArgs.add(args);
        return null;
      }
    }).when(accountingService).addMarketTransaction(isA(Broker.class), 
                                                    isA(Timeslot.class),
                                                    anyDouble(),
                                                    anyDouble());
    // mock the Broker Proxy, capture messages
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        brokerMsgs.add(args[0]);
        return null;
      }
    }).when(mockProxy).broadcastMessage(anyObject());
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
    verify(mockProxy).registerBrokerMarketListener(svc);
    verify(mockControl).registerTimeslotPhase(svc, 1);
  }

  @Test
  public void testReceiveMessage ()
  {
    // first try a bogus message
    Object bogus = new Object();
    svc.receiveMessage(bogus);
    assertEquals("nothing received", 0, svc.getIncoming().size());
    // try a good one
    Order good = new Order(b1, ts1, 1.0, -22.0);
    svc.receiveMessage(good);
    assertEquals("one order received", 1, svc.getIncoming().size());
  }

  @Test
  public void testValidateShout ()
  {
    Order bogus= new Order(b1, ts0, 1.0, -22.0);
    assertFalse("ts0 not enabled", ts0.isEnabled());
    assertFalse("current timeslot not valid", svc.validateOrder(bogus));
    Order good = new Order(b1, ts1, 1.0, -22.0);
    assertTrue("ts1 enabled", ts1.isEnabled());
    assertTrue("next timeslot valid", svc.validateOrder(good));
  }

  // one ask, one bid, equal qty, tradeable
  @Test
  public void testActivate1 ()
  {
    Order sell = new Order(s1, ts1, -1.0, 20.0);
    Order buy = new Order(b1, ts1, 1.0, -22.0);
    svc.receiveMessage(sell);
    svc.receiveMessage(buy);
    assertEquals("two orders received", 2, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting called twice", 2, accountingArgs.size());
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts1", ts1, args[1]);
    assertEquals("price", 21.0, (Double)args[2], 1e-6);
    assertEquals("mWh", -1.0, (Double)args[3], 1e-6);
    args = accountingArgs.get(1);
    assertEquals("b1", b1, args[0]);
    assertEquals("ts1", ts1, args[1]);
    assertEquals("price", -21.0, (Double)args[2], 1e-6);
    assertEquals("mWh", 1.0, (Double)args[3], 1e-6);
    // two broker messages
    assertEquals("2 messages", 2, brokerMsgs.size());
    assertTrue("first is orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook)brokerMsgs.get(0);
    assertEquals("no uncleared asks", 0, ob.getAsks().size());
    assertEquals("no uncleared bids", 0, ob.getBids().size());
    assertEquals("correct timeslot", ts1, ob.getTimeslot());
    assertEquals("correct clearing", 21.0, ob.getClearingPrice(), 1e-6);
    assertTrue("second is clearedTrade", brokerMsgs.get(1) instanceof ClearedTrade);
    ClearedTrade ct = (ClearedTrade)brokerMsgs.get(1);
    assertEquals("correct timeslot", ts1, ct.getTimeslot());
    assertEquals("correct mWh", 1.0, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", 21.0, ct.getExecutionPrice(), 1e-6);
  }

  // one ask, one bid, equal qty, not tradeable
  @Test
  public void testActivate1_no ()
  {
    Order sell = new Order(s1, ts1, -1.0, 23.0);
    Order buy = new Order(b1, ts1, 1.0, -22.0);
    svc.receiveMessage(sell);
    svc.receiveMessage(buy);
    assertEquals("two shouts received", 2, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting not called", 0, accountingArgs.size());
    // one broker message
    assertEquals("one message", 1, brokerMsgs.size());
    assertTrue("first is orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook)brokerMsgs.get(0);
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
  }

  // one ask, one bid, equal qty, different timeslots
  @Test
  public void testActivate1_ts ()
  {
    Order sell = new Order(s1, ts1, -1.0, 23.0);
    Order buy = new Order(b1, ts2, 1.0, -22.0);
    svc.receiveMessage(sell);
    svc.receiveMessage(buy);
    assertEquals("two shouts received", 2, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting not called", 0, accountingArgs.size());
    // two broker messages, one for each timeslot
    assertEquals("two messages", 2, brokerMsgs.size());
    assertTrue("ts1 orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook)brokerMsgs.get(0);
    assertEquals("ts1", ts1, ob.getTimeslot());
    assertNull("null clearing price", ob.getClearingPrice());
    assertEquals("one uncleared ask", 1, ob.getAsks().size());
    assertEquals("no uncleared bids", 0, ob.getBids().size());
    assertEquals("correct qty", -1.0,
                 ob.getAsks().first().getMWh(), 1e-6);
    assertEquals("correct price", 23.0,
                 ob.getAsks().first().getLimitPrice(), 1e-6);

    assertTrue("ts2 orderbook", brokerMsgs.get(0) instanceof Orderbook);
    ob = (Orderbook)brokerMsgs.get(1);
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
    svc.receiveMessage(sell);
    svc.receiveMessage(buy1);
    svc.receiveMessage(buy2);
    assertEquals("three shouts received", 3, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 4 calls", 4, accountingArgs.size());
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts1", ts1, args[1]);
    assertEquals("price", 20.5, (Double)args[2], 1e-6);
    assertEquals("mWh", -0.6, (Double)args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b2", b2, args[0]); // b2 had the higher bid price
    assertEquals("ts1", ts1, args[1]);
    assertEquals("price", -20.5, (Double)args[2], 1e-6);
    assertEquals("mWh", 0.6, (Double)args[3], 1e-6);

    args = accountingArgs.get(2);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts1", ts1, args[1]);
    assertEquals("price", 20.5, (Double)args[2], 1e-6);
    assertEquals("mWh", -0.4, (Double)args[3], 1e-6);

    args = accountingArgs.get(3);
    assertEquals("b1", b1, args[0]); // b1 had the lower bid price
    assertEquals("ts1", ts1, args[1]);
    assertEquals("price", -20.5, (Double)args[2], 1e-6);
    assertEquals("mWh", 0.4, (Double)args[3], 1e-6);
    // two broker messages
    assertEquals("2 messages", 2, brokerMsgs.size());
    assertTrue("first is orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook)brokerMsgs.get(0);
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
    ClearedTrade ct = (ClearedTrade)brokerMsgs.get(1);
    assertEquals("correct timeslot", ts1, ct.getTimeslot());
    assertEquals("correct mWh", 1.0, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", 20.5, ct.getExecutionPrice(), 1e-6);
  }
  
  // one ask, two bids, all tradeable
  @Test
  public void testActivate2_2_tradeable ()
  {
    Order sell1 = new Order(s1, ts2, -0.9, 18.0);
    Order sell2 = new Order(s2, ts2, -1.0, 20.0);
    Order sell3 = new Order(s2, ts2, -1.0, 21.5);
    Order buy1 = new Order(b1, ts2, 1.4, -21.0);
    Order buy2 = new Order(b2, ts2, 0.6, -22.0);
    svc.receiveMessage(sell1);
    svc.receiveMessage(sell2);
    svc.receiveMessage(sell3);
    svc.receiveMessage(buy1);
    svc.receiveMessage(buy2);
    assertEquals("five shouts received", 5, svc.getIncoming().size());
    svc.activate(timeService.getCurrentTime(), 2);
    assertEquals("accounting: 6 calls", 6, accountingArgs.size());
    // first tx should be ask, second bid
    Object[] args = accountingArgs.get(0);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("price", 20.5, (Double)args[2], 1e-6);
    assertEquals("mWh", -0.6, (Double)args[3], 1e-6);

    args = accountingArgs.get(1);
    assertEquals("b2", b2, args[0]); // b2 had the higher bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("price", -20.5, (Double)args[2], 1e-6);
    assertEquals("mWh", 0.6, (Double)args[3], 1e-6);
    
    args = accountingArgs.get(2);
    assertEquals("s1", s1, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("price", 20.5, (Double)args[2], 1e-6);
    assertEquals("mWh", -0.3, (Double)args[3], 1e-6);
    
    args = accountingArgs.get(3);
    assertEquals("b1", b1, args[0]); // b1 had the lower bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("price", -20.5, (Double)args[2], 1e-6);
    assertEquals("mWh", 0.3, (Double)args[3], 1e-6);
    
    args = accountingArgs.get(4);
    assertEquals("s2", s2, args[0]);
    assertEquals("ts2", ts2, args[1]);
    assertEquals("price", 20.5, (Double)args[2], 1e-6);
    assertEquals("mWh", -1.0, (Double)args[3], 1e-6);
    
    args = accountingArgs.get(5);
    assertEquals("b1", b1, args[0]); // b1 had the lower bid price
    assertEquals("ts2", ts2, args[1]);
    assertEquals("price", -20.5, (Double)args[2], 1e-6);
    assertEquals("mWh", 1.0, (Double)args[3], 1e-6);

    // two broker messages
    assertEquals("2 messages", 2, brokerMsgs.size());
    assertTrue("first is orderbook", brokerMsgs.get(0) instanceof Orderbook);
    Orderbook ob = (Orderbook)brokerMsgs.get(0);
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
    ClearedTrade ct = (ClearedTrade)brokerMsgs.get(1);
    assertEquals("correct timeslot", ts2, ct.getTimeslot());
    assertEquals("correct mWh", 1.9, ct.getExecutionMWh(), 1e-6);
    assertEquals("correct price", 20.5, ct.getExecutionPrice(), 1e-6);
  }
}
