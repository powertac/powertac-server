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

import java.util.ArrayList;
import java.util.List;

import org.mockito.ArgumentCaptor;

import javax.annotation.Resource;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
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
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class TariffSubscriptionTests
{
  @Autowired
  TimeService timeService;
  
  @Autowired
  TimeslotRepo timeslotRepo;
  
  @Autowired
  TariffRepo tariffRepo;
  
  @Resource
  Accounting mockAccounting;
  
  @Resource
  TariffMarket mockTariffMarket;
  
  Instant baseTime;
  Broker broker;
  //CustomerInfo info;
  CustomerInfo customer;
  
  TariffSpecification spec;
  Tariff tariff;

  @Before
  public void setUp () throws Exception
  {
    //timeService = new TimeService();
    reset(mockAccounting);
    baseTime = new DateTime(1972, 9, 6, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(baseTime);
    Competition.newInstance("tst").withSimulationBaseTime(baseTime);
    broker = new Broker("Jenny");
    customer = new CustomerInfo("Podunk", 23).withPowerType(PowerType.CONSUMPTION);
    //customer = new AbstractCustomer(info);
    spec = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .withExpiration(baseTime.plus(TimeService.DAY * 10))
        .withMinDuration(TimeService.DAY * 5)
        .addRate(new Rate().withValue(-0.11));
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
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer, 3, 0.0, -0.0);
    assertEquals("3 committed", 3, sub.getCustomersCommitted());
  }

  @Test
  public void testUnsubscribe ()
  {
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(33);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer, 33, 0.0, -0.0);
    assertEquals("33 committed", 33, sub.getCustomersCommitted());
    sub.unsubscribe(8);
    verify(mockAccounting, never()).addTariffTransaction(TariffTransaction.Type.WITHDRAW, 
                                                         tariff, customer, 8, 0.0, 0.0);
    verify(mockTariffMarket).subscribeToTariff(tariff, customer, -8);
    assertEquals("33 still committed", 33, sub.getCustomersCommitted());
    sub.deferredUnsubscribe(8);
    assertEquals("25 now committed", 25, sub.getCustomersCommitted());
  }

  // TODO - test subscribe and unsubscribe with non-zero signup and withdraw payments
  
  @Test
  public void testHandleRevokedTariffDefault ()
  {
    List<List<Object>>results = new ArrayList<List<Object>>();
    // set up default tariff, install in tariff market
    TariffSpecification defaultSpec = new TariffSpecification(broker, PowerType.CONSUMPTION)
        .addRate(new Rate().withValue(-0.21));
    Tariff defaultTariff = new Tariff(defaultSpec);
    defaultTariff.init();
    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
        .thenReturn(defaultTariff);

    // capture subscription method args
//    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
//    ArgumentCaptor<CustomerInfo> customerArg= ArgumentCaptor.forClass(CustomerInfo.class);
//    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
//    // tariff market returns subscription to default tariff
//    TariffSubscription defaultSub = new TariffSubscription(customer, defaultTariff);
//    when(mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//        .thenReturn(defaultSub);

    // subscribe some customers to the original tariff
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(33);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer, 33, 0.0, -0.0);

    // revoke the original subscription
    tariff.setState(Tariff.State.KILLED);
    sub.handleRevokedTariff();
    // should have called tariff market twice
    verify(mockTariffMarket).getDefaultTariff(PowerType.CONSUMPTION);
    verify(mockTariffMarket).subscribeToTariff(tariff, customer, -33);
    verify(mockTariffMarket).subscribeToTariff(defaultTariff, customer, 33);
  }
  
  @Test
  public void testHandleRevokedTariffDefaultPT ()
  {
    List<List<Object>>results = new ArrayList<List<Object>>();
    CustomerInfo ptcustomer = new CustomerInfo("Podunk", 21).withPowerType(PowerType.SOLAR_PRODUCTION);
    TariffSpecification ptspec = new TariffSpecification(broker, PowerType.SOLAR_PRODUCTION)
      .withExpiration(baseTime.plus(TimeService.DAY * 10))
      .withMinDuration(TimeService.DAY * 5)
      .addRate(new Rate().withValue(0.11));
    Tariff pttariff = new Tariff(ptspec);
    pttariff.init();
 // set up default tariff, install in tariff market
    TariffSpecification defaultSpec = new TariffSpecification(broker, PowerType.PRODUCTION)
        .addRate(new Rate().withValue(0.09));
    Tariff defaultTariff = new Tariff(defaultSpec);
    defaultTariff.init();
    when(mockTariffMarket.getDefaultTariff(PowerType.PRODUCTION))
        .thenReturn(defaultTariff);

    // subscribe some customers to the original tariff
    TariffSubscription sub = new TariffSubscription(ptcustomer, pttariff);
    sub.subscribe(19);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                pttariff, ptcustomer, 19, 0.0, -0.0);

    // revoke the original subscription
    pttariff.setState(Tariff.State.KILLED);
    sub.handleRevokedTariff();
    // should have called tariff market twice
    verify(mockTariffMarket).getDefaultTariff(PowerType.PRODUCTION);
    verify(mockTariffMarket).subscribeToTariff(pttariff, ptcustomer, -19);
    verify(mockTariffMarket).subscribeToTariff(defaultTariff, ptcustomer, 19);
  }
  
  // TODO - public void handleRevokedTariffSuperseded ()

  @Test
  public void testUsePower ()
  {
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(33);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer, 33, 0.0, -0.0);
    sub.usePower(330.0);
    ArgumentCaptor<Double> chargeArg = ArgumentCaptor.forClass(Double.class);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransaction.Type.CONSUME),
                                                eq(tariff), eq(customer), eq(33), eq(-330.0), 
                                                chargeArg.capture());
    assertEquals("correct charge", 330.0 * 0.11, chargeArg.getValue(), 1e-6);
    assertEquals("correct total", 10.0, sub.getTotalUsage(), 1e-6);
  }

  @Test
  public void testUsePowerPeriodic ()
  {
    spec.withPeriodicPayment(-1.0);
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(33);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer, 33, 0.0, -0.0);
    sub.usePower(330.0);
    ArgumentCaptor<Double> chargeArg = ArgumentCaptor.forClass(Double.class);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransaction.Type.CONSUME),
                                                eq(tariff), eq(customer), eq(33), eq(-330.0), 
                                                chargeArg.capture());
    assertEquals("correct charge", 330.0 * 0.11, chargeArg.getValue(), 1e-6);
    assertEquals("correct total", 10.0, sub.getTotalUsage(), 1e-6);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransaction.Type.PERIODIC),
                                                eq(tariff), eq(customer), eq(33), eq(0.0), 
                                                chargeArg.capture());
    assertEquals("correct periodic charge", 33.0 / 24.0, chargeArg.getValue(), 1e-6);
  }

  @Test
  public void testGetExpiredCustomerCount ()
  {
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(33);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer, 33, 0.0, -0.0);
    assertEquals("no expired customers", 0, sub.getExpiredCustomerCount());
    // move forward 3 days and subscribe some more
    Instant now = timeService.getCurrentTime();
    timeService.setCurrentTime(now.plus(TimeService.DAY * 3));
    assertEquals("still no expired customers", 0, sub.getExpiredCustomerCount());
    sub.subscribe(22);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer, 22, 0.0, -0.0);
    assertEquals("55 subscriptions", 55, sub.getCustomersCommitted());
    // move forward another three days, there should now be 33 expired
    timeService.setCurrentTime(now.plus(TimeService.DAY * 6));
    assertEquals("33 expired customers", 33, sub.getExpiredCustomerCount());
  }
  
  @Test
  public void testBalancingControl ()
  {
    spec = new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
               .withExpiration(baseTime.plus(TimeService.DAY * 10))
               .withMinDuration(TimeService.DAY * 5)
               .addRate(new Rate().withValue(-0.09).withMaxCurtailment(0.5));
    tariff = new Tariff(spec);
    tariff.init();
    tariffRepo.addSpecification(tariff.getTariffSpec());
    tariffRepo.addTariff(tariff);
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    ArgumentCaptor<Double> chargeArg = ArgumentCaptor.forClass(Double.class);
    sub.subscribe(10);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer, 10, 0.0, -0.0);
    timeslotRepo.findOrCreateBySerialNumber(10);
    sub.usePower(100.0);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransaction.Type.CONSUME),
                                                eq(tariff), eq(customer), eq(10), eq(-100.0), 
                                                chargeArg.capture());
    assertEquals("correct charge", 9.0, chargeArg.getValue(), 1e-6);
    sub.postBalancingControl(30.0);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransaction.Type.PRODUCE),
                                                eq(tariff), eq(customer), eq(10), eq(30.0), 
                                                chargeArg.capture());
    assertEquals("correct charge", -2.7, chargeArg.getValue(), 1e-6);
    assertEquals("correct curtailment", 30.0, sub.getCurtailment(), 1e-6);
    assertEquals("no curtailment", 0.0, sub.getCurtailment(), 1e-6);
  }
  
  @Test
  public void testEconomicControl ()
  {
    spec = new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
               .withExpiration(baseTime.plus(TimeService.DAY * 10))
               .withMinDuration(TimeService.DAY * 5)
               .addRate(new Rate().withValue(-0.09).withMaxCurtailment(0.5));
    tariff = new Tariff(spec);
    tariff.init();
    tariffRepo.addSpecification(tariff.getTariffSpec());
    tariffRepo.addTariff(tariff);
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    ArgumentCaptor<Double> chargeArg = ArgumentCaptor.forClass(Double.class);
    sub.subscribe(10);
    verify(mockAccounting).addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                                tariff, customer, 10, 0.0, -0.0);
    timeslotRepo.findOrCreateBySerialNumber(10);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    assertEquals("correct timeslot", 1, timeslotRepo.currentTimeslot().getSerialNumber());
    
    sub.usePower(100.0);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransaction.Type.CONSUME),
                                                eq(tariff), eq(customer), eq(10), eq(-100.0), 
                                                chargeArg.capture());
    assertEquals("correct charge", 9.0, chargeArg.getValue(), 1e-6);
    assertEquals("full curtailment available", 50.0, 
                 sub.getMaxRemainingCurtailment(), 1e-6);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    sub.postRatioControl(0.2);
    sub.usePower(100.0);
    verify(mockAccounting).addTariffTransaction(eq(TariffTransaction.Type.CONSUME),
                                                eq(tariff), eq(customer), eq(10), eq(-80.0), 
                                                chargeArg.capture());
    assertEquals("correct charge", 7.2, chargeArg.getValue(), 1e-6);
    assertEquals("partial curtailment available", 30.0, 
                 sub.getMaxRemainingCurtailment(), 1e-6);
    assertEquals("correct curtailment", 20.0, sub.getCurtailment(), 1e-6);
    assertEquals("no curtailment", 0.0, sub.getCurtailment(), 1e-6);
  }
  
  @Test
  public void testCurtailmentUnsubscribe1 ()
  {
    spec = new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
      .withExpiration(baseTime.plus(TimeService.DAY * 10))
      .withMinDuration(TimeService.DAY * 5)
      .addRate(new Rate().withValue(-0.09).withMaxCurtailment(0.5));
    tariff = new Tariff(spec);
    tariff.init();
    tariffRepo.addSpecification(tariff.getTariffSpec());
    tariffRepo.addTariff(tariff);
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(10);
    timeslotRepo.findOrCreateBySerialNumber(10);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));

    sub.usePower(100.0);
    assertEquals("correct remaining curtailment",
                 50.0, sub.getMaxRemainingCurtailment(), 1e-6);
  }
  
  @Test
  public void testCurtailmentUnsubscribe2 ()
  {
    spec = new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
      .withExpiration(baseTime.plus(TimeService.DAY * 10))
      .withMinDuration(TimeService.DAY * 5)
      .addRate(new Rate().withValue(-0.09).withMaxCurtailment(0.5));
    tariff = new Tariff(spec);
    tariff.init();
    tariffRepo.addSpecification(tariff.getTariffSpec());
    tariffRepo.addTariff(tariff);
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(10);
    timeslotRepo.findOrCreateBySerialNumber(10);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));

    sub.usePower(100.0);
    sub.unsubscribe(2);
    assertEquals("correct remaining curtailment",
                 40.0, sub.getMaxRemainingCurtailment(), 1e-6);
  }
  
  @Test
  public void testCurtailmentUnsubscribe3 ()
  {
    spec = new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
      .withExpiration(baseTime.plus(TimeService.DAY * 10))
      .withMinDuration(TimeService.DAY * 5)
      .addRate(new Rate().withValue(-0.09).withMaxCurtailment(0.5));
    tariff = new Tariff(spec);
    tariff.init();
    tariffRepo.addSpecification(tariff.getTariffSpec());
    tariffRepo.addTariff(tariff);
    TariffSubscription sub = new TariffSubscription(customer, tariff);
    sub.subscribe(10);
    timeslotRepo.findOrCreateBySerialNumber(10);
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));

    sub.usePower(100.0);
    sub.unsubscribe(2);
    sub.unsubscribe(8);
    assertEquals("correct remaining curtailment",
                 0.0, sub.getMaxRemainingCurtailment(), 1e-6);
  }
}
