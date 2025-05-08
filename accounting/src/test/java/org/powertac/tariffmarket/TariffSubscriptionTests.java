/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.tariffmarket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:tariff-test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class TariffSubscriptionTests
{
  @Autowired
  private TimeService timeService;  // autowire the time service

  @Autowired
  private TariffMarketService tariffMarketService;  // autowire the market

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  private Accounting mockAccounting;
  
  private Tariff tariff;
  private Broker broker;
  private CustomerInfo customer;
  //private AbstractCustomer customer;
  private Instant now;

  @BeforeEach
  public void setUp()
  {
    Competition comp = Competition.newInstance("tariff-sub-test");
    broker = new Broker("Joe");
    now = ZonedDateTime.of(2011, 1, 10, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();;
    timeService.setCurrentTime(now);
    List<String> inits = new ArrayList<String>();
    inits.add("AccountingService");
    tariffMarketService.initialize(comp, inits);
    Instant exp = now.plusMillis(TimeService.WEEK * 10);
    TariffSpecification tariffSpec =
        new TariffSpecification(broker, PowerType.CONSUMPTION)
          .withExpiration(exp)
          .withMinDuration(TimeService.WEEK * 8)
          .addRate(new Rate().withValue(0.121));
    tariff = new Tariff(tariffSpec);
    tariff.init();
    customer = new CustomerInfo("Charley", 100);
    //customer = new AbstractCustomer(customerInfo);
    reset(mockAccounting);
  }

  // create a Subscription from a Tariff
  @Test
  public void testSimpleSub ()
  {
    tariffMarketService.subscribeToTariff(tariff, customer, 3);
    tariffMarketService.activate(now, 4);
    TariffSubscription ts = 
            tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff, customer);
    assertNotNull(ts, "non-null subscription");
    assertEquals(customer, ts.getCustomer(), "correct customer");
    assertEquals(tariff, ts.getTariff(), "correct tariff");
    assertEquals(3, ts.getCustomersCommitted(), "correct customer count");
  }
  
  // subscription with non-zero signup bonus
  @Test
  public void testSignupBonus ()
  {
    Instant exp = now.plusMillis(TimeService.WEEK * 10);
    TariffSpecification tariffSpec =
        new TariffSpecification(broker, PowerType.CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 4)
            .withSignupPayment(33.2)
            .addRate(new Rate().withValue(-0.121));
    tariff = new Tariff(tariffSpec);
    tariff.init();

    tariffMarketService.subscribeToTariff(tariff, customer, 5);
    tariffMarketService.activate(now, 4);
    TariffSubscription tsub = 
            tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff, customer);
    assertNotNull(tsub, "non-null subscription");
    assertEquals(5, tsub.getCustomersCommitted(), "five customers committed");
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer,
                                                5, 0.0, -33.2 * 5);
  }

  // subscription withdrawal without and with penalty
  @Test
  public void testEarlyWithdraw ()
  {
    Instant exp = now.plusMillis(TimeService.WEEK * 10);
    TariffSpecification tariffSpec =
        new TariffSpecification(broker, PowerType.CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 4)
            .withSignupPayment(33.2)
            .withEarlyWithdrawPayment(-42.1)
            .addRate(new Rate().withValue(-0.121));
    tariff = new Tariff(tariffSpec);
    tariff.init();
    tariffMarketService.subscribeToTariff(tariff, customer, 5);
    tariffMarketService.activate(now, 4);
    TariffSubscription tsub =
            tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff, customer);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer,
                                                5, 0.0, -33.2*5);

    // move time forward 2 weeks, withdraw 2 customers
    Instant wk2 = now.plusMillis(TimeService.WEEK * 2);
    timeService.setCurrentTime(wk2);
    tsub.unsubscribe(2);
    tariffMarketService.activate(wk2, 4);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.WITHDRAW,
                                                tariff, customer,
                                                2, 0.0, 42.1*2);
    //def txs = TariffTransaction.findAllByPostedTime(wk2)
    //assertEquals(1, txs.size(), "one transaction")
    //assertEquals(TariffTransactionType.WITHDRAW, txs[0].txType, "correct txType")
    //assertEquals(42.1*2, txs[0].charge, "correct charge")
    assertEquals(3, tsub.getCustomersCommitted(), "three customers committed");
    
    // move time forward another week, add 4 customers and drop 1
    Instant wk3 = now.plusMillis(TimeService.WEEK * 2 + TimeService.HOUR * 6);
    timeService.setCurrentTime(wk3);
    tariffMarketService.subscribeToTariff(tariff, customer, 4);
    TariffSubscription tsub1 = 
            tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff, customer);
    tsub1.unsubscribe(1);
    tariffMarketService.activate(wk3, 4);
    assertEquals(tsub, tsub1, "same subscription");
    //txs = TariffTransaction.findAllByPostedTime(wk3)
    //assertEquals(2, txs.size(), "two transactions")
    //TariffTransaction ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime,
    //                                                                    TariffTransactionType.SIGNUP)
    //assertNotNull(ttx, "found signup tx")
    //assertEquals(-33.2 * 4, ttx.charge, "correct charge")
    //ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime,
    //                                                  TariffTransactionType.WITHDRAW)
    //assertNotNull(ttx, "found withdraw tx")
    //assertEquals(42.1, ttx.charge, "correct charge")
    assertEquals(6, tsub1.getCustomersCommitted(), "six customers committed");
  }

  // subscription withdrawal for revoked tariff
  @Test
  public void testEarlyWithdrawRevoke ()
  {
    Instant exp = now.plusMillis(TimeService.WEEK * 10);
    TariffSpecification tariffSpec =
        new TariffSpecification(broker, PowerType.CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 4)
            .withSignupPayment(33.2)
            .withEarlyWithdrawPayment(-42.1)
            .addRate(new Rate().withValue(-0.121));
    tariff = new Tariff(tariffSpec);
    tariff.init();
    tariffMarketService.subscribeToTariff(tariff, customer, 5);
    tariffMarketService.activate(now, 4);
    TariffSubscription tsub =
            tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff, customer);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer,
                                                5, 0.0, -33.2*5);

    // move time forward 2 weeks, revoke tariff, withdraw 2 customers
    Instant wk2 = now.plusMillis(TimeService.WEEK * 2);
    timeService.setCurrentTime(wk2);
    TariffRevoke tex = new TariffRevoke(tariffSpec.getBroker(), tariffSpec);
    tariffMarketService.handleMessage(tex);
    tariffMarketService.activate(wk2, 4);
    assertTrue(tariff.isRevoked(), "tariff revoked");
    tsub.unsubscribe(2);
    tariffMarketService.activate(wk2, 4);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransaction.Type.WITHDRAW),
                                                eq(tariff), eq(customer),
                                                anyInt(), anyDouble(), eq(0.0));
  }

  // Check consumption transactions
  @Test
  public void testConsumption ()
  {
    Instant exp = now.plusMillis(TimeService.WEEK * 10);
    TariffSpecification tariffSpec =
        new TariffSpecification(broker, PowerType.CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 4)
            .withSignupPayment(33.2)
            .addRate(new Rate().withValue(-0.121));
    tariff = new Tariff(tariffSpec);
    tariff.init();

    // subscribe and consume in the first timeslot
    tariffMarketService.subscribeToTariff(tariff, customer, 4);
    tariffMarketService.activate(now, 4);
    TariffSubscription tsub = 
            tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff, customer);
    assertEquals(4, tsub.getCustomersCommitted(), "four customers committed");
    tsub.usePower(24.4); // consumption
    //assertEquals(24.4 / 4, tsub.getTotalUsage(), 1e-6, "correct total usage");
    assertEquals(-0.121, tariff.getRealizedPrice(), 1e-6, "correct realized price");
    //def txs = TariffTransaction.findAllByPostedTime(timeService.getCurrentTime());
    //assertEquals(2, txs.size(), "two transactions")
    //TariffTransaction ttx = 
    //    TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.SIGNUP)
    //assertNotNull(ttx, "found signup tx")
    //assertEquals(-33.2 * 4, ttx.charge, 1e-6, "correct charge")
    //ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.CONSUME)
    //assertNotNull(ttx, "found consumption tx")
    //assertEquals(24.4, ttx.quantity, "correct amount")
    //assertEquals(0.121 * 24.4, ttx.charge, 1e-6, "correct charge")

    // just consume in the second timeslot
    Instant hour = now.plusMillis(TimeService.HOUR);
    timeService.setCurrentTime(hour);
    tsub.usePower(32.8); // consumption
    //assertEquals((24.4 + 32.8) / 4, tsub.getTotalUsage(), 1e-6, "correct total usage");
    assertEquals(-0.121, tariff.getRealizedPrice(), 1e-6, "correct realized price");
    //txs = TariffTransaction.findAllByPostedTime(timeService.getCurrentTime())
    //assertEquals(1, txs.size(), "one transaction")
    //ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.getCurrentTime(), TariffTransactionType.CONSUME)
    //assertNotNull(ttx, "found consumption tx")
    //assertEquals(32.8, ttx.quantity, "correct amount")
    //assertEquals(0.121 * 32.8, ttx.charge, 1e-6, "correct charge")
  }
  
  // Check two-part tariff
  @Test
  public void testTwoPart()
  {
    Instant exp = now.plusMillis(TimeService.WEEK * 10);
    TariffSpecification tariffSpec =
        new TariffSpecification(broker, PowerType.CONSUMPTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 4)
            .withSignupPayment(31.2)
            .withPeriodicPayment(-1.3)
            .addRate(new Rate().withValue(-0.112));
    tariff = new Tariff(tariffSpec);
    tariff.init();

    // subscribe and consume in the first timeslot
    tariffMarketService.subscribeToTariff(tariff, customer, 6);
    tariffMarketService.activate(now, 4);
    TariffSubscription tsub = 
            tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff, customer);
    assertEquals(6, tsub.getCustomersCommitted(), "six customers committed");
    tsub.usePower(28.8); // consumption
    //assertEquals(28.8 / 6, tsub.getTotalUsage(), 1e-6, "correct total usage");
    assertEquals(-0.112, tariff.getRealizedPrice(), 1e-6, "correct realized price");
    //def txs = TariffTransaction.findAllByPostedTime(timeService.currentTime);
    //assertEquals(3, txs.size(), "two transactions")
    //TariffTransaction ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.SIGNUP)
    //assertNotNull(ttx, "found signup tx")
    //assertEquals(-31.2 * 6, ttx.charge, 1e-6, "correct charge")
    //ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.CONSUME)
    //assertNotNull(ttx, "found consumption tx")
    //assertEquals(28.8, ttx.quantity, "correct amount")
    //assertEquals(0.112 * 28.8, ttx.charge, 1e-6, "correct charge")
    //ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.PERIODIC)
    //assertNotNull(ttx, "found periodoc tx")
    //assertEquals(6 * 1.3, ttx.charge, 1e-6, "correct charge")
  }
    
  // Check production transactions
  @Test
  public void testProduction ()
  {
    Instant exp = now.plusMillis(TimeService.WEEK * 10);
    TariffSpecification tariffSpec =
        new TariffSpecification(broker, PowerType.PRODUCTION)
            .withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 4)
            .withSignupPayment(34.2)
            .withEarlyWithdrawPayment(-35.0)
            .addRate(new Rate().withValue(0.102));
    tariff = new Tariff(tariffSpec);
    tariff.init();

    // subscribe and consume in the first timeslot
    tariffMarketService.subscribeToTariff(tariff, customer, 4);
    tariffMarketService.activate(now, 4);
    TariffSubscription tsub = 
            tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff, customer);
    assertEquals(4, tsub.getCustomersCommitted(), "four customers committed");
    tsub.usePower(-244.6); // production
    //assertEquals(-244.6 / 4, tsub.getTotalUsage(), 1e-6, "correct total usage");
    assertEquals(0.102, tariff.getRealizedPrice(), 1e-6, "correct realized price");
    //def txs = TariffTransaction.findAllByPostedTime(timeService.currentTime);
    //assertEquals(2, txs.size(), "two transactions")
    //TariffTransaction ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.SIGNUP)
    //assertNotNull(ttx, "found signup tx")
    //assertEquals(-34.2 * 4, ttx.charge, 1e-6, "correct charge")
    //ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.PRODUCE)
    //assertNotNull(ttx, "found production tx")
    //assertEquals(-244.6, ttx.quantity, "correct amount")
    //assertEquals(-0.102 * 244.6, ttx.charge, 1e-6, "correct charge")
  }
}
