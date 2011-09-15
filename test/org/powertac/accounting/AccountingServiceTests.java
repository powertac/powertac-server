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
import static org.powertac.util.Tools.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.enumerations.TariffTransactionType;
import org.powertac.common.exceptions.PositionUpdateException;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.RandomSeedService;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.*;
import org.powertac.util.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/test-config.xml"})
public class AccountingServiceTests
{
  @Autowired
  private TimeService timeService; // dependency injection
  
  @Autowired
  private AccountingService accountingService;

  @Autowired
  private AccountingInitializationService accountingInitializationService;
  
  @Autowired
  private TariffRepo tariffRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;
  
  // get access to the mock services
  @Autowired
  private BrokerProxy mockProxy;
  
  @Autowired
  private CompetitionControl mockCompetitionControl;
  
  @Autowired
  private RandomSeedService mockRandom;

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

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @Before
  public void setUp() 
  {
    // Clean up from previous tests
    tariffRepo.recycle();
    timeslotRepo.recycle();
    brokerRepo.recycle();
    pluginConfigRepo.recycle();
    reset(mockProxy);

    // create a Competition, needed for initialization
    comp = Competition.newInstance("accounting-test");
    
    // set the clock
    Instant now = new DateTime(2011, 1, 26, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(now);
    
    // set up brokers and customers
    bob = new Broker("Bob");
    brokerRepo.add(bob);
    jim = new Broker("Jim");
    brokerRepo.add(jim);

    customerInfo1 = new CustomerInfo("downtown", 42)
        .setCustomerType(CustomerType.CustomerHousehold)
        .addPowerType(PowerType.CONSUMPTION);
    customerInfo2 = new CustomerInfo("suburbs", 21)
        .setCustomerType(CustomerType.CustomerHousehold)
        .addPowerType(PowerType.CONSUMPTION);
    customerInfo3 = new CustomerInfo("exburbs", 11)
         .setCustomerType(CustomerType.CustomerHousehold)
         .addPowerType(PowerType.CONSUMPTION);

    // set up tariffs - tariff1 for consumption, tariff2 for production
    Instant exp = now.plus(TimeService.WEEK * 10);
    TariffSpecification tariffSpec = new TariffSpecification(bob, PowerType.CONSUMPTION)
        .setExpiration(exp)
        .setMinDuration(TimeService.WEEK * 8)
        .setPeriodicPayment(0.02)
        .addRate(new Rate().setValue(0.121));
    tariffRepo.addSpecification(tariffSpec);
    tariffB1 = new Tariff(tariffSpec);
    tariffB1.init();
    tariffRepo.addTariff(tariffB1);

    tariffSpec = new TariffSpecification(bob, PowerType.CONSUMPTION)
        .setMinDuration(TimeService.WEEK * 8)
        .setExpiration(exp)
        .addRate(new Rate().setValue(0.09));
    tariffRepo.addSpecification(tariffSpec);
    tariffB2 = new Tariff(tariffSpec);
    tariffB2.init();
    tariffRepo.addTariff(tariffB2);
    tariffSpec = new TariffSpecification(jim, PowerType.CONSUMPTION)
        .setMinDuration(TimeService.WEEK * 8)
        .setExpiration(exp)
        .setPeriodicPayment(0.01)
        .addRate(new Rate().setValue(0.123));
    tariffRepo.addSpecification(tariffSpec);
    tariffJ1 = new Tariff(tariffSpec);
    tariffJ1.init();
    tariffRepo.addTariff(tariffJ1);
    
    // set up some timeslots
    Timeslot ts = timeslotRepo.makeTimeslot(now.minus(TimeService.HOUR), now);
    ts = timeslotRepo.makeTimeslot(now, now.plus(TimeService.HOUR));
    ts = timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR), now.plus(TimeService.HOUR * 2));
    ts = timeslotRepo.makeTimeslot(now.plus(TimeService.HOUR * 2), now.plus(TimeService.HOUR * 3));
  }
  
  private void initializeService () {
    accountingInitializationService.setDefaults();
    accountingInitializationService.initialize(comp, new ArrayList());
  }

  @Test
  public void testAccountingServiceNotNull() 
  {
    assertNotNull(accountingInitializationService);
    assertNotNull(accountingService);
  }
  
  @Test
  public void testNormalInitialization ()
  {
    accountingInitializationService.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("AccountingService");
    assertNotNull("config created correctly", config);
    String result = accountingInitializationService.initialize(comp, new ArrayList());
    assertEquals("correct return value", "AccountingService", result);
    assertTrue("correct bank interest",
       accountingInitializationService.getMinInterest() <= accountingService.getBankInterest());
    assertTrue("correct bank interest",
       accountingInitializationService.getMaxInterest() >= accountingService.getBankInterest());
  }
  
  @Test
  public void testBogusInitialization ()
  {
    PluginConfig config = pluginConfigRepo.findByRoleName("AccountingService");
    assertNull("config not created", config);
    String result = accountingInitializationService.initialize(comp, new ArrayList());
    assertEquals("failure return value", "fail", result);
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
    accountingService.addTariffTransaction(TariffTransactionType.SIGNUP,
      tariffB1, customerInfo1, 2, 0.0, 42.1);
    accountingService.addTariffTransaction(TariffTransactionType.CONSUME,
      tariffB1, customerInfo2, 7, 77.0, 7.7);
    assertEquals("correct number in list", 2, accountingService.getPendingTransactions().size());
    List<BrokerTransaction> pending = accountingService.getPendingTransactions();
    List<BrokerTransaction> signups = filter(pending,
                                             new Predicate<BrokerTransaction>() {
      public boolean apply (BrokerTransaction tx) {
        return (tx instanceof TariffTransaction &&
            ((TariffTransaction)tx).getTxType() == TariffTransactionType.SIGNUP);
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
            ((TariffTransaction)tx).getTxType() == TariffTransactionType.CONSUME);
      }
    });
    assertEquals("one signup", 1, consumes.size());
    ttx = (TariffTransaction)consumes.get(0);
    assertNotNull("second ttx not null", ttx);
    assertEquals("correct amount id 1", 77.0, ttx.getQuantity(), 1e-6);
  }
  
  @Test
  public void testCurrentNetLoad ()
  {
    initializeService();
    // some usage for Bob
    accountingService.addTariffTransaction(TariffTransactionType.CONSUME,
      tariffB1, customerInfo1, 7, 77.0, 7.7);
    accountingService.addTariffTransaction(TariffTransactionType.CONSUME,
      tariffB1, customerInfo2, 6, 83.0, 8.0);
    accountingService.addTariffTransaction(TariffTransactionType.PRODUCE,
      tariffB2, customerInfo3, 3, -55.0, -4.5);
    // some usage for Jim
    accountingService.addTariffTransaction(TariffTransactionType.CONSUME,
      tariffJ1, customerInfo2, 12, 120.0, 8.4);
    assertEquals("correct net load for Bob", (77.0 + 83.0 - 55.0),
                  accountingService.getCurrentNetLoad(bob), 1e-6);
    assertEquals("correct net load for Jim", 120.0,
                  accountingService.getCurrentNetLoad(jim), 1e-6);
  }
  
  // create and test market transactions
  @Test
  public void testMarketTransaction ()
  {
    initializeService();
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(2), 45.0, 0.5);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(3), 43.0, 0.7);
    List<BrokerTransaction> pending = accountingService.getPendingTransactions();
    assertEquals("correct number in list", 2, pending.size());
    //assertEquals("correct number in db", 2, MarketTransaction.count());
    MarketTransaction mtx = (MarketTransaction)pending.get(0);
    assertNotNull("first mtx not null", mtx);
    assertEquals("correct timeslot id 0", 2, mtx.getTimeslot().getSerialNumber());
    assertEquals("correct price id 0", 45.0, mtx.getPrice(), 1e-6);
    Broker b1 = mtx.getBroker();
    Broker b2 = brokerRepo.findById(bob.getId());
    assertEquals("same broker", b1, b2);
    mtx = (MarketTransaction)pending.get(1);
    assertNotNull("second mtx not null", mtx);
    assertEquals("correct quantity id 1", 0.7, mtx.getQuantity(), 1e-6);
  }
  
  // simple activation
  @Test
  public void testSimpleActivate ()
  {
    initializeService();
    // Need to set the interest rate in the Accounting config
    accountingService.setBankInterest(0.12);
        
    // add a couple of transactions
    accountingService.addMarketTransaction(bob,
      timeslotRepo.findBySerialNumber(2), 45.0, 0.5);
    accountingService.addMarketTransaction(bob,
      timeslotRepo.findBySerialNumber(3), 43.0, 0.7);

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

    // should be market transactions, and cash and mkt positions for bob
    List bobMsgs = msgMap.get(bob);
    assertEquals("five messages", 5, bobMsgs.size());

    Object obj = findFirst(bobMsgs, new Predicate<Object>() {
      public boolean apply (Object item) {
        Timeslot ts5 = timeslotRepo.findBySerialNumber(2);
        return (item instanceof MarketTransaction &&
                ((MarketTransaction)item).getBroker() == bob &&
                ((MarketTransaction)item).getTimeslot() == ts5);
      }
    });
    MarketTransaction mtx1 = (MarketTransaction)obj;
    assertNotNull("found 1st tx", mtx1);
    assertEquals("correct quantity", 0.5, mtx1.getQuantity(), 1e-6);

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
    assertEquals("correct cash position", -45.0 * 0.5 + -43.0 * 0.7, cp1.getBalance(), 1e-6);
  }

  // test activation
  @Test
  public void testActivate ()
  {
    initializeService();
    // market transactions
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(2), 45.0, 0.5);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(2), 31.0, 0.3);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(3), 43.0, 0.7);
    accountingService.addMarketTransaction(jim,
        timeslotRepo.findBySerialNumber(2), 35.0, 0.4);
    accountingService.addMarketTransaction(jim,
        timeslotRepo.findBySerialNumber(2), -20.0, -0.2);
    // tariff transactions
    accountingService.addTariffTransaction(TariffTransactionType.CONSUME,
        tariffB1, customerInfo1, 7, 77.0, 7.7);
    accountingService.addTariffTransaction(TariffTransactionType.CONSUME,
        tariffB1, customerInfo2, 6, 83.0, 8.0);
    accountingService.addTariffTransaction(TariffTransactionType.PRODUCE,
        tariffB2, customerInfo3, 3, -55.0, -4.5);
    accountingService.addTariffTransaction(TariffTransactionType.CONSUME,
        tariffJ1, customerInfo2, 12, 120.0, 8.4);
    assertEquals("correct number in list", 9, accountingService.getPendingTransactions().size());
    
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
        (-45.0 * 0.5 - 31.0 * 0.3 - 43.0 * 0.7 + 7.7 + 8.0 -4.5), bob.getCash().getBalance(), 1e-6);
    assertEquals("correct cash balance, Jim",
        (-35.0 * 0.4 + 20.0 * 0.2 + 8.4), jim.getCash().getBalance(), 1e-6);
    
    List<Object> bobMkts = filter(msgMap.get(bob), new Predicate<Object>() {
      public boolean apply (Object item) {
        return (item instanceof MarketPosition);
      }
    });
    assertEquals("Bob has 2 mkt positions", 2, bobMkts.size());
    
    Object mkt = findFirst(bobMkts, new Predicate<Object>() {
      public boolean apply (Object thing) {
        Timeslot ts5 = timeslotRepo.findBySerialNumber(2);
        return (((MarketPosition)thing).getTimeslot() == ts5);
      }
    });
    assertNotNull("found market position b5", mkt);
    assertEquals("correct mkt position, Bob, ts5",  0.8, ((MarketPosition)mkt).getOverallBalance(), 1e-6);
    mkt = findFirst(bobMkts, new Predicate<Object>() {
      public boolean apply (Object thing) {
        Timeslot ts6 = timeslotRepo.findBySerialNumber(3);
        return (((MarketPosition)thing).getTimeslot() == ts6);
      }
    });
    assertNotNull("found market position b6", mkt);
    assertEquals("correct mkt position, Bob, ts6",  0.7, ((MarketPosition)mkt).getOverallBalance(), 1e-6);
    
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
  }
  
  // net market position only works after activation
  @Test
  public void testCurrentMarketPosition ()
  {
    initializeService();
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(2), 45.0, 0.5);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(2), 31.0, 0.3);
    accountingService.addMarketTransaction(bob,
        timeslotRepo.findBySerialNumber(3), 43.0, 0.7);
    accountingService.addMarketTransaction(jim,
        timeslotRepo.findBySerialNumber(2), 35.0, 0.4);
    accountingService.addMarketTransaction(jim,
        timeslotRepo.findBySerialNumber(2), -20.0, -0.2);
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
  @Test
  public void testInterestPayment ()
  {
    initializeService();
    //assertEquals("interest set by bootstrap", 0.10, accountingService.bankInterest)
    
    // set the interest to 12%
    accountingService.setBankInterest(0.12);

    // bob is in the hole
    bob.getCash().deposit(-1000.0);

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
