/*
 * Copyright 2009-2011 the original author or authors.
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

package org.powertac.accounting;

import static org.junit.Assert.*;
import static org.powertac.util.ListTools.*;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.*;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.util.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class AccountingServiceTests
{
  @Autowired
  private TimeService timeService; // dependency injection
  
  @Autowired
  private AccountingService accountingService;
  
  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;
  
  // get access to the mock services
  @Autowired
  private BrokerProxy mockProxy;
  
  @Autowired
  private ServerConfiguration mockServerProperties;

  private Configurator config;
  private Competition comp;
  private CustomerInfo customerInfo1;
  private CustomerInfo customerInfo2;
  private CustomerInfo customerInfo3;
  private Tariff tariffB1;
  private Tariff tariffB2;
  private Tariff tariffJ1;
  private Broker bob;
  private Broker jim;
  //private int nameCounter = 0;

  @SuppressWarnings("rawtypes")
  @Before
  public void setUp() 
  {
    // Clean up from previous tests
    tariffRepo.recycle();
    timeslotRepo.recycle();
    brokerRepo.recycle();
    reset(mockProxy);
    reset(mockServerProperties);

    // create a Competition, needed for initialization
    comp = Competition.newInstance("accounting-test");
    
    // set the clock
    //Instant now = new DateTime(2011, 1, 26, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    Instant now = Competition.currentCompetition().getSimulationBaseTime(); 
    now = now.plus(TimeService.HOUR);
    timeService.setCurrentTime(now);
    
    // set up brokers and customers
    bob = new Broker("Bob");
    brokerRepo.add(bob);
    jim = new Broker("Jim");
    brokerRepo.add(jim);

    customerInfo1 = new CustomerInfo("downtown", 42)
        .withPowerType(PowerType.CONSUMPTION);
    customerInfo2 = new CustomerInfo("suburbs", 21)
        .withPowerType(PowerType.CONSUMPTION);
    customerInfo3 = new CustomerInfo("exburbs", 11)
         .withPowerType(PowerType.CONSUMPTION);

    // set up tariffs - tariff1 for consumption, tariff2 for production
    Instant exp = now.plus(TimeService.WEEK * 10);
    TariffSpecification tariffSpec = new TariffSpecification(bob, PowerType.CONSUMPTION)
        .withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8)
        .withPeriodicPayment(0.02)
        .addRate(new Rate().withValue(0.121));
    tariffRepo.addSpecification(tariffSpec);
    tariffB1 = new Tariff(tariffSpec);
    tariffB1.init();
    tariffRepo.addTariff(tariffB1);

    tariffSpec = new TariffSpecification(bob, PowerType.CONSUMPTION)
        .withMinDuration(TimeService.WEEK * 8)
        .withExpiration(exp)
        .addRate(new Rate().withValue(0.09));
    tariffRepo.addSpecification(tariffSpec);
    tariffB2 = new Tariff(tariffSpec);
    tariffB2.init();
    tariffRepo.addTariff(tariffB2);
    tariffSpec = new TariffSpecification(jim, PowerType.CONSUMPTION)
        .withMinDuration(TimeService.WEEK * 8)
        .withExpiration(exp)
        .withPeriodicPayment(0.01)
        .addRate(new Rate().withValue(0.123));
    tariffRepo.addSpecification(tariffSpec);
    tariffJ1 = new Tariff(tariffSpec);
    tariffJ1.init();
    tariffRepo.addTariff(tariffJ1);
    
    // set up some timeslots
    timeslotRepo.makeTimeslot(now.minus(TimeService.HOUR));
    timeslotRepo.makeTimeslot(now);
    timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR));
    timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR * 2));

    // Set up serverProperties mock
    config = new Configurator();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(mockServerProperties).configureMe(anyObject());
  }
  
  private void initializeService () 
  {
    String result = accountingService.initialize(comp, new ArrayList<String>());
    assertEquals("correct return", "AccountingService", result);
  }

  @Test
  public void testAccountingServiceNotNull() 
  {
    assertNotNull(accountingService);
  }
  
  // initialization without a configuration
  @Test
  public void testNormalInitialization ()
  {
    String result = accountingService.initialize(comp, new ArrayList<String>());
    assertEquals("correct return value", "AccountingService", result);
    assertTrue("correct bank interest",
       accountingService.getMinInterest() <= accountingService.getBankInterest());
    assertTrue("correct bank interest",
       accountingService.getMaxInterest() >= accountingService.getBankInterest());
  }
  
  // config max/min
  @Test
  public void testMaxMinInitialization ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("accounting.accountingService.minInterest", "0.01");
    map.put("accounting.accountingService.maxInterest", "0.20");
    Configuration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);

    String result = accountingService.initialize(comp, new ArrayList<String>());
    assertEquals("correct return value", "AccountingService", result);
    assertEquals("correct min value", 0.01, accountingService.getMinInterest(), 1e-6);
    assertEquals("correct max value", 0.20, accountingService.getMaxInterest(), 1e-6);
    assertTrue("correct bank interest",
       accountingService.getMinInterest() <= accountingService.getBankInterest());
    assertTrue("correct bank interest",
       accountingService.getMaxInterest() >= accountingService.getBankInterest());
  }
  
  // config interest rate directly
  @Test
  public void testInterestInitialization ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("accounting.accountingService.minInterest", "0.01");
    map.put("accounting.accountingService.maxInterest", "0.20");
    map.put("accounting.accountingService.bankInterest", "0.008");
    Configuration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);

    accountingService.initialize(comp, new ArrayList<String>());
    assertEquals("correct bank interest", 0.008, accountingService.getBankInterest(), 1e-6);
  }

  @Test
  public void testBrokerDb ()
  {
    Broker b1 = brokerRepo.findById(bob.getId());
    assertEquals("bob in db by id", bob, b1);
    Broker b2 = brokerRepo.findByUsername("Bob");
    assertEquals("bob in db by name", bob, b2);
  }

  // create and test tariff transactions
  @Test
  public void testTariffTransaction ()
  {
    initializeService();
    accountingService.addTariffTransaction(TariffTransaction.Type.SIGNUP,
      tariffB1, customerInfo1, 2, 0.0, 42.1);
    accountingService.addTariffTransaction(TariffTransaction.Type.CONSUME,
      tariffB1, customerInfo2, 7, -77.0, 7.7);
    accountingService.addRegulationTransaction(tariffB1, customerInfo1,
                                               2, 10.0, -2.5);
    accountingService.addRegulationTransaction(tariffB1, customerInfo2,
                                               7, -10.0, 2.0);
    assertEquals("correct number in list", 4, accountingService.getPendingTransactions().size());
    assertEquals("correct number in ttx list", 4, accountingService.getPendingTariffTransactions().size());
    List<BrokerTransaction> pending = accountingService.getPendingTransactions();
    List<BrokerTransaction> signups = filter(pending,
                                             new Predicate<BrokerTransaction>() {
      public boolean apply (BrokerTransaction tx) {
        return (tx instanceof TariffTransaction &&
            ((TariffTransaction)tx).getTxType() == TariffTransaction.Type.SIGNUP);
      }
    });
    assertEquals("one signup", 1, signups.size());
    TariffTransaction ttx = (TariffTransaction)signups.get(0);
    assertNotNull("first ttx not null", ttx);
    assertEquals("correct charge id 0", 42.1, ttx.getCharge(), 1e-6);
    Broker b1 = ttx.getBroker();
    Broker b2 = brokerRepo.findById(bob.getId());
    assertEquals("same broker", b1, b2);
    List<BrokerTransaction> consumes = filter(pending,
                                              new Predicate<BrokerTransaction>() {
      public boolean apply (BrokerTransaction tx) {
        return (tx instanceof TariffTransaction &&
            ((TariffTransaction)tx).getTxType() == TariffTransaction.Type.CONSUME);
      }
    });
    assertEquals("two consumes", 2, consumes.size());
    ttx = (TariffTransaction)consumes.get(0);
    assertNotNull("second ttx not null", ttx);
    TariffTransaction ttx1 = (TariffTransaction)consumes.get(1);
    assertNotNull("third ttx not null", ttx1);
    if (ttx.isRegulation()) {
      // swap
      ttx = ttx1;
      ttx1 = (TariffTransaction)consumes.get(0);
    }
    assertEquals("correct amount 1", -77.0, ttx.getKWh(), 1e-6);
    assertFalse("not regulation", ttx.isRegulation());
    assertEquals("correct amount 2", -10.0, ttx1.getKWh(), 1e-6);
    assertTrue("regulation", ttx1.isRegulation());
    List<BrokerTransaction> produces = filter(pending,
                                              new Predicate<BrokerTransaction>() {
      public boolean apply (BrokerTransaction tx) {
        return (tx instanceof TariffTransaction &&
            ((TariffTransaction)tx).getTxType() == TariffTransaction.Type.PRODUCE);
      }
    });
    assertEquals("one produce", 1, produces.size());
    ttx = (TariffTransaction)produces.get(0);
    assertEquals("correct amount 3", 10.0, ttx.getKWh(), 1e-6);
    assertTrue("regulation", ttx.isRegulation());
  }

  @Test
  public void testCurrentNetLoad ()
  {
    initializeService();
    // some usage for Bob
    accountingService.addTariffTransaction(TariffTransaction.Type.CONSUME,
      tariffB1, customerInfo1, 7, -77.0, 7.7);
    accountingService.addTariffTransaction(TariffTransaction.Type.CONSUME,
      tariffB1, customerInfo2, 6, -83.0, 8.0);
    accountingService.addTariffTransaction(TariffTransaction.Type.PRODUCE,
      tariffB2, customerInfo3, 3, 55.0, -4.5);
    // some usage for Jim
    accountingService.addTariffTransaction(TariffTransaction.Type.CONSUME,
      tariffJ1, customerInfo2, 12, -120.0, 8.4);
    assertEquals("correct net load for Bob", (-77.0 - 83.0 + 55.0),
                  accountingService.getCurrentNetLoad(bob), 1e-6);
    assertEquals("correct net load for Jim", -120.0,
                  accountingService.getCurrentNetLoad(jim), 1e-6);
  }
  
  @Test
  public void testCurrentSupplyDemand ()
  {
    initializeService();
    // some usage for Bob
    accountingService.addTariffTransaction(TariffTransaction.Type.CONSUME,
      tariffB1, customerInfo1, 7, -77.0, 7.7);
    accountingService.addTariffTransaction(TariffTransaction.Type.CONSUME,
      tariffB1, customerInfo2, 6, -83.0, 8.0);
    accountingService.addTariffTransaction(TariffTransaction.Type.PRODUCE,
      tariffB2, customerInfo3, 3, 55.0, -4.5);
    // some usage for Jim
    accountingService.addTariffTransaction(TariffTransaction.Type.CONSUME,
      tariffJ1, customerInfo2, 12, -120.0, 8.4);
    // retrieve the map
    Map<Broker, Map<TariffTransaction.Type, Double>> sd =
            accountingService.getCurrentSupplyDemandByBroker();
    // check data for Bob
    Map<TariffTransaction.Type, Double> bsd = sd.get(bob);
    assertEquals("correct consumption for Bob", (-77.0 - 83.0),
                  bsd.get(TariffTransaction.Type.CONSUME), 1e-6);
    assertEquals("correct production for Bob", 55.0,
                 bsd.get(TariffTransaction.Type.PRODUCE), 1e-6);
    // check data for Jim
    bsd = sd.get(jim);
    assertEquals("correct consumption for Jim", -120.0,
                 bsd.get(TariffTransaction.Type.CONSUME), 1e-6);
    assertEquals("correct production for Jim", 0.0,
                 bsd.get(TariffTransaction.Type.PRODUCE), 1e-6);
  }
  
  // create and test market transactions
  @Test
  public void testMarketTransaction ()
  {
    initializeService();
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(2), 0.5, -45.0);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(3), 0.7, -43.0);
    assertEquals("no tariff tx", 0, accountingService.getPendingTariffTransactions().size());
    List<BrokerTransaction> pending = accountingService.getPendingTransactions();
    assertEquals("correct number in list", 2, pending.size());
    MarketTransaction mtx = (MarketTransaction)pending.get(0);
    assertNotNull("first mtx not null", mtx);
    assertEquals("correct timeslot id 0", 2, mtx.getTimeslot().getSerialNumber());
    assertEquals("correct price id 0", -45.0, mtx.getPrice(), 1e-6);
    Broker b1 = mtx.getBroker();
    Broker b2 = brokerRepo.findById(bob.getId());
    assertEquals("same broker", b1, b2);
    mtx = (MarketTransaction)pending.get(1);
    assertNotNull("second mtx not null", mtx);
    assertEquals("correct quantity id 1", 0.7, mtx.getMWh(), 1e-6);
    // broker market positions should have been updated already
    MarketPosition mp2 = bob.findMarketPositionByTimeslot(2);
    assertNotNull("should be a market position in slot 2", mp2);
    assertEquals(".5 mwh in ts2", 0.5, mp2.getOverallBalance(), 1e-6);
    MarketPosition mp3 = bob.findMarketPositionByTimeslot(3);
    assertNotNull("should be a market position in slot 3", mp3);
    assertEquals(".7 mwh in ts3", 0.7, mp3.getOverallBalance(), 1e-6);
  }
  
  // simple activation
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testSimpleActivate ()
  {
    initializeService();
    // Need to set the interest rate in the Accounting config
    accountingService.setBankInterest(0.12);
        
    // add a couple of transactions
    accountingService.addMarketTransaction(bob,
      timeslotRepo.findBySerialNumber(1), 0.6, -55.0);
    accountingService.addMarketTransaction(bob,
      timeslotRepo.findBySerialNumber(2), 0.5, -45.0);
    accountingService.addMarketTransaction(bob,
      timeslotRepo.findBySerialNumber(3), 0.7, -43.0);

    final Map<Broker, List> msgMap = new HashMap<Broker, List>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        msgMap.put((Broker)args[0], (List)args[1]);
        return null;
      }
    }).when(mockProxy).sendMessages(isA(Broker.class), anyList());
    // activate and check messages
    accountingService.activate(timeService.getCurrentTime(), 3);
    verify(mockProxy, times(2)).sendMessages(isA(Broker.class), anyList());
    
    // should have cash position, no market positions for jim
    assertEquals("one message", 1, msgMap.get(jim).size());
    Object msg = msgMap.get(jim).get(0);
    assertTrue("it's a CashPosition", msg instanceof CashPosition);
    assertEquals("no balance", 0.0, ((CashPosition)msg).getBalance(), 1e-6);

    // should be 3 market transactions, and cash and 3 mkt positions for bob
    List bobMsgs = msgMap.get(bob);
    assertEquals("seven messages", 7, bobMsgs.size());

    Object obj = findFirst(bobMsgs, new Predicate<Object>() {
      public boolean apply (Object item) {
        Timeslot ts5 = timeslotRepo.findBySerialNumber(1);
        return (item instanceof MarketTransaction &&
                ((MarketTransaction)item).getBroker() == bob &&
                ((MarketTransaction)item).getTimeslot() == ts5);
      }
    });
    MarketTransaction mtx1 = (MarketTransaction)obj;
    assertNotNull("found 1st tx", mtx1);
    assertEquals("correct quantity", 0.6, mtx1.getMWh(), 1e-6);

    obj = findFirst(bobMsgs, new Predicate<Object>() {
      public boolean apply (Object item) {
        Timeslot ts5 = timeslotRepo.findBySerialNumber(2);
        return (item instanceof MarketPosition &&
                ((MarketPosition)item).getBroker() == bob &&
                ((MarketPosition)item).getTimeslot() == ts5);
      }
    });
    MarketPosition mp1 = (MarketPosition)obj;
    assertEquals("correct balance for ts5", 0.5, mp1.getOverallBalance(), 1e-6);

    obj = findFirst(bobMsgs, new Predicate<Object>() {
      public boolean apply (Object item) {
        return (item instanceof CashPosition &&
                ((CashPosition)item).getBroker() == bob);
      }
    });
    CashPosition cp1 = (CashPosition)obj;
    assertNotNull("non-null CashPosition", cp1);
    assertEquals("correct cash position", -55.0 * 0.6, cp1.getBalance(), 1e-6);
  }

  // test activation
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testActivate ()
  {
    initializeService();
    // market transactions
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(1), 0.6, -55.0);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(2), 0.5, -45.0);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(2), 0.3, -31.0);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(3), 0.7, -43.0);
    accountingService.addMarketTransaction(jim,
        timeslotRepo.findBySerialNumber(2), 0.4, -35.0);
    accountingService.addMarketTransaction(jim,
        timeslotRepo.findBySerialNumber(2), -0.2, 20.0);
    // tariff transactions
    accountingService.addTariffTransaction(TariffTransaction.Type.CONSUME,
        tariffB1, customerInfo1, 7, 77.0, 7.7);
    accountingService.addTariffTransaction(TariffTransaction.Type.CONSUME,
        tariffB1, customerInfo2, 6, 83.0, 8.0);
    accountingService.addTariffTransaction(TariffTransaction.Type.PRODUCE,
        tariffB2, customerInfo3, 3, -55.0, -4.5);
    accountingService.addTariffTransaction(TariffTransaction.Type.CONSUME,
        tariffJ1, customerInfo2, 12, 120.0, 8.4);
    assertEquals("correct number in list", 10, accountingService.getPendingTransactions().size());
    
    // activate, gather messages, check cash and market positions
    final Map<Broker, List> msgMap = new HashMap<Broker, List>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        msgMap.put((Broker)args[0], (List)args[1]);
        return null;
      }
    }).when(mockProxy).sendMessages(isA(Broker.class), anyList());
    accountingService.activate(timeService.getCurrentTime(), 3);
    verify(mockProxy, times(2)).sendMessages(isA(Broker.class), anyList());

    assertEquals("correct cash balance, Bob",
        (-55.0 * 0.6 + 7.7 + 8.0 -4.5), bob.getCashBalance(), 1e-6);
    assertEquals("correct cash balance, Jim",
        8.4, jim.getCashBalance(), 1e-6);
    
    List<Object> bobMkts = filter(msgMap.get(bob), new Predicate<Object>() {
      public boolean apply (Object item) {
        return (item instanceof MarketPosition);
      }
    });
    assertEquals("Bob has 3 mkt positions", 3, bobMkts.size());
    
    Object mkt = findFirst(bobMkts, new Predicate<Object>() {
      public boolean apply (Object thing) {
        Timeslot ts5 = timeslotRepo.findBySerialNumber(1);
        return (((MarketPosition)thing).getTimeslot() == ts5);
      }
    });
    assertNotNull("found market position b5", mkt);
    assertEquals("correct mkt position, Bob, ts5",  0.6, ((MarketPosition)mkt).getOverallBalance(), 1e-6);
    mkt = findFirst(bobMkts, new Predicate<Object>() {
      public boolean apply (Object thing) {
        Timeslot ts6 = timeslotRepo.findBySerialNumber(2);
        return (((MarketPosition)thing).getTimeslot() == ts6);
      }
    });
    assertNotNull("found market position b6", mkt);
    assertEquals("correct mkt position, Bob, ts6",  0.8, ((MarketPosition)mkt).getOverallBalance(), 1e-6);
    
    List<Object> jimMkts = filter(msgMap.get(jim), new Predicate<Object>() {
      public boolean apply (Object item) {
        return (item instanceof MarketPosition);
      }
    });
    assertEquals("Jim has 1 mkt position", 1, jimMkts.size());

    mkt = jimMkts.get(0);
    assertNotNull("found market position j5", mkt);
    assertEquals("correct timeslot", timeslotRepo.findBySerialNumber(2),
                 ((MarketPosition)mkt).getTimeslot());
    assertEquals("correct mkt position, Jim, ts5",  0.2, ((MarketPosition)mkt).getOverallBalance(), 1e-6);
    
    // activate in the next timeslot, see that market transactions for ts 2
    // are posted
    double bobCash1 = bob.getCashBalance();
    msgMap.clear();
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    accountingService.activate(timeService.getCurrentTime(), 3);

    double bobCash2 = bob.getCashBalance();
    assertEquals("bob's ts2 power deliveries posted",
                 bobCash1 - 0.5 * 45.0 - 0.3 * 31.0, bobCash2, 1e-6);
  }
  
  // net market position only works after activation
  @Test
  public void testCurrentMarketPosition ()
  {
    initializeService();
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(2), 0.5, -45.0);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(2), 0.3, -31.0);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(3), 0.7, -43.0);
    accountingService.addMarketTransaction(jim,
        timeslotRepo.findBySerialNumber(2), 0.4, -35.0);
    accountingService.addMarketTransaction(jim,
        timeslotRepo.findBySerialNumber(2), -0.2, 20.0);
    assertEquals("correct number in list", 5, accountingService.getPendingTransactions().size());
    accountingService.activate(timeService.getCurrentTime(), 3);
    // current timeslot is 4, should be 0 mkt posn
    assertEquals("correct position, bob, ts4", 0.0,
        accountingService.getCurrentMarketPosition (bob), 1e-6);
    assertEquals("correct position, jim, ts4", 0.0,
        accountingService.getCurrentMarketPosition (jim), 1e-6);
    // move forward to timeslot 5 and try again
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    assertEquals("correct position, bob, ts5", 0.8,
        accountingService.getCurrentMarketPosition (bob), 1e-6);
    assertEquals("correct position, jim, ts5", 0.2,
        accountingService.getCurrentMarketPosition (jim), 1e-6);
    // another hour and try again
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    assertEquals("correct position, bob, ts5", 0.7,
        accountingService.getCurrentMarketPosition (bob), 1e-6);
    assertEquals("correct position, jim, ts5", 0.0,
        accountingService.getCurrentMarketPosition (jim), 1e-6);
  }
  
  // interest should be paid/charged at midnight activation
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testInterestPayment ()
  {
    initializeService();
    //assertEquals("interest set by bootstrap", 0.10, accountingService.bankInterest)
    
    // set the interest to 12%
    accountingService.setBankInterest(0.12);

    // bob is in the hole
    bob.updateCash(-1000.0);

    // move to midnight, activate and check messages
    timeService.setCurrentTime(new DateTime(2011, 1, 27, 0, 0, 0, 0, DateTimeZone.UTC).toInstant());
    final Map<Broker, List> msgMap = new HashMap<Broker, List>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        msgMap.put((Broker)args[0], (List)args[1]);
        return null;
      }
    }).when(mockProxy).sendMessages(isA(Broker.class), anyList());
    accountingService.activate(timeService.getCurrentTime(), 3);
    verify(mockProxy, times(2)).sendMessages(isA(Broker.class), anyList());

    // should have bank tx and cash position for Bob
    assertEquals("two messages", 2, msgMap.get(bob).size());
    Object btx1 = findFirst(msgMap.get(bob), new Predicate<Object>() {
      public boolean apply (Object thing) {
        return (thing instanceof BankTransaction);
      }
    });
    assertNotNull("found bank tx", btx1);
    assertEquals("correct amount", -1000.0 * 0.12 / 365.0, 
                 ((BankTransaction)btx1).getAmount(), 1e-6);
    Object cp1 = findFirst(msgMap.get(bob), new Predicate<Object>() {
      public boolean apply (Object thing) {
        return (thing instanceof CashPosition);
      }
    });
    assertNotNull("found cash posn", cp1);
    assertEquals("correct amount", -1000.0 * (1.0 + 0.12 / 365.0), 
                 ((CashPosition)cp1).getBalance(), 1e-6);
  }
}
