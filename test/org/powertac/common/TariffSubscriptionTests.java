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
package org.powertac.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

import javax.annotation.Resource;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.enumerations.TariffTransactionType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.TariffMarket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test cases for TariffSubscription. Uses a Spring application context
 * to access autowired components.
 * 
 * Need to mock: Accounting, TariffMarket
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/test-config.xml"})
public class TariffSubscriptionTests
{
  @Autowired
  TimeService timeService;
  
  @Resource
  Accounting mockAccounting;
  
  @Resource
  TariffMarket mockTariffMarket;
  
  Instant baseTime;
  Broker broker;
  CustomerInfo info;
  AbstractCustomer customer;
  
  TariffSpecification spec;
  Tariff tariff;

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    //timeService = new TimeService();
    baseTime = new DateTime(1972, 9, 6, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(baseTime);
    broker = new Broker("Jenny");
    info = new CustomerInfo("Podunk", 23).addPowerType(PowerType.CONSUMPTION);
    customer = new AbstractCustomer(info);
    spec = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(baseTime.plus(TimeService.DAY * 10))
        .withMinDuration(TimeService.DAY * 5)
        .addRate(new Rate().withValue(0.11));
    tariff = new Tariff(spec);
    tariff.init();
  }

  @Test
  public void testTariffSubscription ()
  {
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    assertNotNull("not null", sub);
    assertEquals("correct customer", customer, sub.getCustomer());
    assertEquals("correct tariff", tariff, sub.getTariff());
    assertEquals("no customers committed", 0, sub.getCustomersCommitted());
  }

  @Test
  public void testGetTotalUsage ()
  {
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    assertEquals("correct initially", 0.0, sub.getTotalUsage(), 1e-6);
  }

  @Test
  public void testSubscribe ()
  {
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(3);
    verify(mockAccounting).addTariffTransaction(TariffTransactionType.SIGNUP,
                                                tariff, info, 3, 0.0, 0.0);
    assertEquals("3 committed", 3, sub.getCustomersCommitted());
  }

  @Test
  public void testUnsubscribe ()
  {
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(33);
    verify(mockAccounting).addTariffTransaction(TariffTransactionType.SIGNUP,
                                                tariff, info, 33, 0.0, 0.0);
    assertEquals("33 committed", 33, sub.getCustomersCommitted());
    sub.unsubscribe(8);
    verify(mockAccounting, never()).addTariffTransaction(TariffTransactionType.WITHDRAW, 
                                                         tariff, info, 8, 0.0, 0.0);
    assertEquals("25 committed", 25, sub.getCustomersCommitted());
  }

  // TODO - test subscribe and unsubscribe with non-zero signup and withdraw payments
  
  @Test
  public void testHandleRevokedTariffDefault ()
  {
    // set up default tariff, install in tariff market
    TariffSpecification defaultSpec = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .addRate(new Rate().withValue(0.21));
    Tariff defaultTariff = new Tariff(defaultSpec);
    defaultTariff.init();
    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION)).thenReturn(defaultTariff);

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<AbstractCustomer> customerArg= ArgumentCaptor.forClass(AbstractCustomer.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    // tariff market returns subscription to default tariff
    TariffSubscription defaultSub = new TariffSubscription(customer, defaultTariff);
    when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture()))
        .thenReturn(defaultSub);

    // subscribe some customers to the original tariff
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(33);
    verify(mockAccounting).addTariffTransaction(TariffTransactionType.SIGNUP,
                                                tariff, info, 33, 0.0, 0.0);

    // revoke the original subscription
    tariff.setState(Tariff.State.KILLED);
    TariffSubscription newSub = sub.handleRevokedTariff();
    // should have called tariff market twice
    verify(mockTariffMarket).getDefaultTariff(PowerType.CONSUMPTION);
    verify(mockTariffMarket).subscribeToTariff(defaultTariff, customer, 33);
    assertEquals("no subscribers to sub", 0, sub.getCustomersCommitted());
    assertEquals("correct tariff arg", defaultTariff, tariffArg.getValue());
    assertEquals("correct customer arg", customer, customerArg.getValue());
    assertEquals("correct customerCount arg", 33, (int)countArg.getValue());
    assertEquals("correct new sub", newSub, defaultSub);
  }
  
  // TODO - public void handleRevokedTariffSuperseded ()

  @Test
  public void testUsePower ()
  {
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(33);
    verify(mockAccounting).addTariffTransaction(TariffTransactionType.SIGNUP,
                                                tariff, info, 33, 0.0, 0.0);
    sub.usePower(330.0);
    ArgumentCaptor<Double> chargeArg = ArgumentCaptor.forClass(Double.class);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransactionType.CONSUME),
                                                eq(tariff), eq(info), eq(33), eq(330.0), 
                                                chargeArg.capture());
    assertEquals("correct charge", 330.0 * 0.11, chargeArg.getValue(), 1e-6);
    assertEquals("correct total", 10.0, sub.getTotalUsage(), 1e-6);
  }

  @Test
  public void testUsePowerPeriodic ()
  {
    spec.withPeriodicPayment(1.0);
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(33);
    verify(mockAccounting).addTariffTransaction(TariffTransactionType.SIGNUP,
                                                tariff, info, 33, 0.0, 0.0);
    sub.usePower(330.0);
    ArgumentCaptor<Double> chargeArg = ArgumentCaptor.forClass(Double.class);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransactionType.CONSUME),
                                                eq(tariff), eq(info), eq(33), eq(330.0), 
                                                chargeArg.capture());
    assertEquals("correct charge", 330.0 * 0.11, chargeArg.getValue(), 1e-6);
    assertEquals("correct total", 10.0, sub.getTotalUsage(), 1e-6);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransactionType.PERIODIC),
                                                eq(tariff), eq(info), eq(33), eq(0.0), 
                                                chargeArg.capture());
    assertEquals("correct periodic charge", 33.0 / 24.0, chargeArg.getValue(), 1e-6);
  }

  @Test
  public void testGetExpiredCustomerCount ()
  {
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(33);
    verify(mockAccounting).addTariffTransaction(TariffTransactionType.SIGNUP,
                                                tariff, info, 33, 0.0, 0.0);
    assertEquals("no expired customers", 0, sub.getExpiredCustomerCount());
    // move forward 3 days and subscribe some more
    Instant now = timeService.getCurrentTime();
    timeService.setCurrentTime(now.plus(TimeService.DAY * 3));
    assertEquals("still no expired customers", 0, sub.getExpiredCustomerCount());
    sub.subscribe(22);
    verify(mockAccounting).addTariffTransaction(TariffTransactionType.SIGNUP,
                                                tariff, info, 22, 0.0, 0.0);
    assertEquals("55 subscriptions", 55, sub.getCustomersCommitted());
    // move forward another three days, there should now be 33 expired
    timeService.setCurrentTime(now.plus(TimeService.DAY * 6));
    assertEquals("33 expired customers", 33, sub.getExpiredCustomerCount());
  }
}
