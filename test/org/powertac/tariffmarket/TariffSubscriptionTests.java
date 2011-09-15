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
package org.powertac.tariffmarket

import grails.test.GrailsUnitTestCase

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import org.joda.time.Instant

import org.powertac.common.Broker
import org.powertac.common.AbstractCustomer
import org.powertac.common.CustomerInfo
import org.powertac.common.MarketTransaction
import org.powertac.common.Rate
import org.powertac.common.Tariff
import org.powertac.common.TariffSpecification
import org.powertac.common.TariffSubscription
import org.powertac.common.TariffTransaction
import org.powertac.common.TimeService
import org.powertac.common.enumerations.CustomerType
import org.powertac.common.enumerations.PowerType
import org.powertac.common.enumerations.TariffTransactionType

class TariffSubscriptionTests extends GrailsUnitTestCase 
{
  def timeService  // autowire the time service
  def tariffMarketService  // autowire the market
  def tariffSubscriptionService
  
  Tariff tariff
  Broker broker
  CustomerInfo customerInfo
  AbstractCustomer customer
  DateTime now
  int idCount = 0

  protected void setUp()
  {
    TariffSpecification.list()*.delete()
    Tariff.list()*.delete()
    super.setUp()
    broker = new Broker(username: "Joe")
    broker.save()
    now = new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC)
    timeService.currentTime = now.toInstant()
    Instant exp = new Instant(now.millis + TimeService.WEEK * 10)
    TariffSpecification tariffSpec =
        new TariffSpecification(broker: broker,
        expiration: exp,
        minDuration: TimeService.WEEK * 8)
    tariffSpec.addToRates(new Rate(value: 0.121))
    tariffSpec.save()
    tariff = new Tariff(tariffSpec: tariffSpec)
    tariff.init()
    assert(tariff.save())
    customerInfo = new CustomerInfo(name:"Charley", customerType: CustomerType.CustomerHousehold)
    if (!customerInfo.validate()) {
      customerInfo.errors.each { println it.toString() }
      fail("Could not save customer")
    }
    assert(customerInfo.save())

    customer = new AbstractCustomer(customerInfo: customerInfo)
    customer.init()
    if (!customer.validate()) {
      customer.errors.each { println it.toString() }
      fail("Could not save customer")
    }
    assert(customer.save())
  }

  protected void tearDown() 
  {
    super.tearDown()
    TariffTransaction.list()*.delete()
  }

  // create a Subscription from a Tariff
  void testSimpleSub ()
  {
    TariffSubscription ts = 
        tariffMarketService.subscribeToTariff(tariff, customer, 3)
    assertNotNull("non-null subscription", ts)
    assertEquals("correct customer", customer, ts.customer)
    assertEquals("correct tariff", tariff, ts.tariff)
    assertEquals("correct customer count", 3, ts.customersCommitted)
  }
  
  // subscription with non-zero signup bonus
  void testSignupBonus ()
  {
    Instant exp = new Instant(now.millis + TimeService.WEEK * 10)
    TariffSpecification tariffSpec =
        new TariffSpecification(broker: broker,
                                expiration: exp,
                                minDuration: TimeService.WEEK * 4,
                                signupPayment: -33.2)
    tariffSpec.addToRates(new Rate(value: 0.121))
    tariffSpec.save()
    tariff = new Tariff(tariffSpec: tariffSpec)
    tariff.init()
    tariff.save()

    TariffSubscription tsub = 
      tariffMarketService.subscribeToTariff(tariff, customer, 5)
    assertNotNull("non-null subscription", tsub)
    assertTrue("subscription saves", tsub.validate() && tsub.save())
    assertEquals("five customers committed", 5, tsub.customersCommitted)
    List expirations = tariffSubscriptionService.getExpirations(tsub)
    assertEquals("one expiration record", 1, expirations.size())
    Instant ex = new Instant(now.millis + TimeService.WEEK * 4)
    assertEquals("correct contract interval", ex, expirations[0][0])
    assertEquals("one transaction exists", 1, TariffTransaction.count())
    def txs = TariffTransaction.list()
    assertNotNull("transactions present", txs)
    assertEquals("one transaction", 1, txs.size())
    assertEquals("correct txType", TariffTransactionType.SIGNUP, txs[0].txType)
    assertEquals("correct charge", -33.2*5, txs[0].charge)
  }
  
  // subscription withdrawal without and with penalty
  void testEarlyWithdraw ()
  {
    DateTime exp = new DateTime(now.millis + TimeService.WEEK * 10, DateTimeZone.UTC)
    TariffSpecification tariffSpec =
        new TariffSpecification(broker: broker,
                                expiration: exp.toInstant(),
                                minDuration: TimeService.WEEK * 4,
                                signupPayment: -33.2,
                                earlyWithdrawPayment: 42.1)
    tariffSpec.addToRates(new Rate(value: 0.121))
    tariffSpec.save()
    tariff = new Tariff(tariffSpec: tariffSpec)
    tariff.init()
    tariff.save()
    TariffSubscription tsub =
        tariffMarketService.subscribeToTariff(tariff, customer, 5)
    assertTrue("subscription saves", tsub.validate() && tsub.save())
    assertNotNull("non-null subscription id", tsub.id)

    // move time forward 2 weeks, withdraw 2 customers
    Instant wk2 = new Instant(now.millis + TimeService.WEEK * 2)
    timeService.currentTime = wk2
    tsub.unsubscribe(2)
    def txs = TariffTransaction.findAllByPostedTime(wk2)
    assertEquals("one transaction", 1, txs.size())
    assertEquals("correct txType", TariffTransactionType.WITHDRAW, txs[0].txType)
    assertEquals("correct charge", 42.1*2, txs[0].charge)
    assertEquals("three customers committed", 3, tsub.customersCommitted)
    
    // move time forward another week, add 4 customers and drop 1
    Instant wk3 = new Instant(now.millis + TimeService.WEEK * 2 + TimeService.HOUR * 6)
    timeService.currentTime = wk3
    TariffSubscription tsub1 = 
        tariffMarketService.subscribeToTariff(tariff, customer, 4)
    assertEquals("same subscription", tsub, tsub1)
    tsub1.unsubscribe(1)
    txs = TariffTransaction.findAllByPostedTime(wk3)
    assertEquals("two transactions", 2, txs.size())
    TariffTransaction ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime,
                                                                        TariffTransactionType.SIGNUP)
    assertNotNull("found signup tx", ttx)
    assertEquals("correct charge", -33.2 * 4, ttx.charge)
    ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime,
                                                      TariffTransactionType.WITHDRAW)
    assertNotNull("found withdraw tx", ttx)
    assertEquals("correct charge", 42.1, ttx.charge)
    assertEquals("six customers committed", 6, tsub1.customersCommitted)
    List expirations = tariffSubscriptionService.getExpirations(tsub)
    assertEquals("two expiration records", 2, expirations.size())
  }
  
  // Check consumption transactions
  void testConsumption ()
  {
    Instant exp = new Instant(now.millis + TimeService.WEEK * 10)
    TariffSpecification tariffSpec =
        new TariffSpecification(broker: broker,
                                expiration: exp,
                                minDuration: TimeService.WEEK * 4,
                                signupPayment: -33.2)
    tariffSpec.addToRates(new Rate(value: 0.121))
    tariffSpec.save()
    tariff = new Tariff(tariffSpec: tariffSpec)
    tariff.init()
    tariff.save()

    // subscribe and consume in the first timeslot
    TariffSubscription tsub = 
        tariffMarketService.subscribeToTariff(tariff, customer, 4)
    assertTrue("subscription saved", tsub.validate() && tsub.save())
    assertEquals("four customers committed", 4, tsub.customersCommitted)
    tsub.usePower(24.4) // consumption
    assertEquals("correct total usage", 24.4 / 4, tsub.totalUsage)
    assertEquals("correct realized price", 0.121, tariff.realizedPrice, 1e-6)
    def txs = TariffTransaction.findAllByPostedTime(timeService.currentTime)
    assertEquals("two transactions", 2, txs.size())
    TariffTransaction ttx = 
        TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.SIGNUP)
    assertNotNull("found signup tx", ttx)
    assertEquals("correct charge", -33.2 * 4, ttx.charge, 1e-6)
    ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.CONSUME)
    assertNotNull("found consumption tx", ttx)
    assertEquals("correct amount", 24.4, ttx.quantity)
    assertEquals("correct charge", 0.121 * 24.4, ttx.charge, 1e-6)

    // just consume in the second timeslot
    Instant hour = new Instant(now.millis + TimeService.HOUR)
    timeService.currentTime = hour
    tsub.usePower(32.8) // consumption
    assertEquals("correct total usage", (24.4 + 32.8) / 4, tsub.totalUsage, 1e-6)
    assertEquals("correct realized price", 0.121, tariff.realizedPrice, 1e-6)
    txs = TariffTransaction.findAllByPostedTime(timeService.currentTime)
    assertEquals("one transaction", 1, txs.size())
    ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.CONSUME)
    assertNotNull("found consumption tx", ttx)
    assertEquals("correct amount", 32.8, ttx.quantity)
    assertEquals("correct charge", 0.121 * 32.8, ttx.charge, 1e-6)
  }
  
  // Check two-part tariff
  void testTwoPart()
  {
    Instant exp = new Instant(now.millis + TimeService.WEEK * 10)
    TariffSpecification tariffSpec =
        new TariffSpecification(broker: broker,
                                expiration: exp,
                                minDuration: TimeService.WEEK * 4,
                                signupPayment: -31.2,
                                periodicPayment: 1.3)
    tariffSpec.addToRates(new Rate(value: 0.112))
    tariffSpec.save()
    tariff = new Tariff(tariffSpec: tariffSpec)
    tariff.init()
    tariff.save()

    // subscribe and consume in the first timeslot
    TariffSubscription tsub = 
        tariffMarketService.subscribeToTariff(tariff, customer, 6)
    assertTrue("subscription saved", tsub.validate() && tsub.save())
    assertEquals("six customers committed", 6, tsub.customersCommitted)
    tsub.usePower(28.8) // consumption
    assertEquals("correct total usage", 28.8 / 6, tsub.totalUsage)
    assertEquals("correct realized price", (0.112 * 28.8 + 6 * 1.3) / 28.8, tariff.realizedPrice, 1e-6)
    def txs = TariffTransaction.findAllByPostedTime(timeService.currentTime)
    assertEquals("two transactions", 3, txs.size())
    TariffTransaction ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.SIGNUP)
    assertNotNull("found signup tx", ttx)
    assertEquals("correct charge", -31.2 * 6, ttx.charge, 1e-6)
    ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.CONSUME)
    assertNotNull("found consumption tx", ttx)
    assertEquals("correct amount", 28.8, ttx.quantity)
    assertEquals("correct charge", 0.112 * 28.8, ttx.charge, 1e-6)
    ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.PERIODIC)
    assertNotNull("found periodoc tx", ttx)
    assertEquals("correct charge", 6 * 1.3, ttx.charge, 1e-6)
  }
    
  // Check production transactions
  void testProduction ()
  {
    Instant exp = new Instant(now.millis + TimeService.WEEK * 10)
    TariffSpecification tariffSpec =
        new TariffSpecification(broker: broker,
                                powerType: PowerType.PRODUCTION,
                                expiration: exp,
                                minDuration: TimeService.WEEK * 4,
                                signupPayment: -34.2,
                                earlyWithdrawPayment: 35.0)
    tariffSpec.addToRates(new Rate(value: 0.102))
    tariffSpec.save()
    tariff = new Tariff(tariffSpec: tariffSpec)
    tariff.init()
    tariff.save()

    // subscribe and consume in the first timeslot
    TariffSubscription tsub = 
        tariffMarketService.subscribeToTariff(tariff, customer, 4)
    assertTrue("subscription saved", tsub.validate() && tsub.save())
    assertEquals("four customers committed", 4, tsub.customersCommitted)
    tsub.usePower(-244.6) // production
    assertEquals("correct total usage", -244.6 / 4, tsub.totalUsage)
    assertEquals("correct realized price", 0.102, tariff.realizedPrice, 1e-6)
    def txs = TariffTransaction.findAllByPostedTime(timeService.currentTime)
    assertEquals("two transactions", 2, txs.size())
    TariffTransaction ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.SIGNUP)
    assertNotNull("found signup tx", ttx)
    assertEquals("correct charge", -34.2 * 4, ttx.charge, 1e-6)
    ttx = TariffTransaction.findByPostedTimeAndTxType(timeService.currentTime, TariffTransactionType.PRODUCE)
    assertNotNull("found production tx", ttx)
    assertEquals("correct amount", -244.6, ttx.quantity)
    assertEquals("correct charge", -0.102 * 244.6, ttx.charge, 1e-6)
  }
}
