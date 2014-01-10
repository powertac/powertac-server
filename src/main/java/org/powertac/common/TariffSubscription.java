/*
 * Copyright (c) 2011-2014 by the original author or authors.
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

//import org.codehaus.groovy.grails.commons.ApplicationHolder
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * A TariffSubscription is an entity representing an association between a Customer
 * and a Tariff. Instances of this class are not intended to be serialized.
 * You get one by calling the subscribe() method on Tariff. If there is no
 * current subscription for that Customer (which in most cases is actually
 * a population model), then a new TariffSubscription is created and
 * returned from the Tariff.  
 * @author John Collins, Carsten Block
 */
public class TariffSubscription 
{
  static private Logger log = Logger.getLogger(TariffSubscription.class.getName());

  long id = IdGenerator.createId();

  private TimeService timeService;

  private Accounting accountingService;

  private TariffMarket tariffMarketService;

  /** The customer who has this Subscription */
  private CustomerInfo customer;

  /** The tariff for which this subscription applies */
  private Tariff tariff;

  /** Total number of customers within a customer model that are committed 
   * to this tariff subscription. This needs to be a count, otherwise tiered 
   * rates cannot be applied properly. */
  private int customersCommitted = 0 ;
  
  /** List of expiration dates. This is used only if the Tariff has a minDuration,
   *  before which a subscribed Customer cannot back out without a penalty. Each
   *  entry in this list is a pair [expiration-date, customer-count]. New entries
   *  are added chronologically at the end of the list, so the front of the list
   *  holds the oldest subscriptions - the ones that can be unsubscribed soonest
   *  without penalty. */
  private List<ExpirationRecord> expirations;

  /** Total usage so far in the current day, needed to compute charges for
   *  tiered rates. */
  private double totalUsage = 0.0;
  
  /** Count of customers who will not be subscribers in the next timeslot */
  private int pendingUnsubscribeCount = 0; 

  // ------------- Controllable and storage capacity ----------------
  /** Pending economic curtailment (from phase 1) */
  double pendingCurtailmentRatio = 0.0;

  /** Actual curtailment from previous timeslot. Should always be zero
   *  after the customer model has run. */
  double curtailment = 0.0;

  /** Maximum remaining curtailment after phase 2 */
  double maxRemainingCurtailment = 0.0;

  /** Current state-of-charge of customer storage capacity */
  double stateOfCharge = 0.0;

  /** Pending up-regulation (positive) or down-regulation (negative)
   *  from previous timeslot. Should always be zero after the customer
   *  model has run. */
  double pendingRegulation = 0.0;

  /**
   * You need a CustomerInfo and a Tariff to create one of these.
   */
  public TariffSubscription (CustomerInfo customer, Tariff tariff)
  {
    super();
    this.customer = customer;
    this.tariff = tariff;
    expirations = new ArrayList<ExpirationRecord>();
  }

  public long getId ()
  {
    return id;
  }

  public CustomerInfo getCustomer ()
  {
    return customer;
  }

  public Tariff getTariff ()
  {
    return tariff;
  }

  public int getCustomersCommitted ()
  {
    return customersCommitted;
  }

  public double getTotalUsage ()
  {
    return totalUsage;
  }

  /**
   * Subscribes some number of discrete customers. This is typically some portion of the population in a
   * population model. We assume this is called from Tariff, as a result of calling tariff.subscribe().
   * Also, we record the expiration date of the tariff contract, just in case the tariff has a
   * minDuration. For the purpose of computing expiration, all contracts are assumed to begin at
   * 00:00 on the day of the subscription.
   */
  public void subscribe (int customerCount)
  {
    // first, update the customer count
    customersCommitted += customerCount;
    
    // if the Tariff has a minDuration, then we have to record the expiration date.
    // we do this by adding an entry to end of list, or updating the entry at the end.
    // An entry is a pair [Instant, count]
    long minDuration = tariff.getMinDuration();
    //if (minDuration > 0) {
    // Compute the 00:00 Instant for the current time
    Instant start =
            getTimeService().truncateInstant(getTimeService().getCurrentTime(),
                                             TimeService.DAY);
    if (expirations.size() > 0 &&
        expirations.get(expirations.size() - 1).getHorizon() == start.getMillis() + minDuration) {
      // update existing entry
      expirations.get(expirations.size() - 1).updateCount(customerCount);
    }
    else {
      // need a new entry
      expirations.add(new ExpirationRecord(start.getMillis() + minDuration,
                                           customerCount));
    }
    //}
    // post the signup bonus
    if (tariff.getSignupPayment() != 0.0) {
      log.debug("signup bonus: " + customerCount + 
                " customers, total = " + customerCount * tariff.getSignupPayment());
    }
    // signup payment is positive for a bonus, to it's a debit for the broker.
    getAccounting().addTariffTransaction(TariffTransaction.Type.SIGNUP,
                                         tariff, customer, 
                                         customerCount, 0.0,
                                         customerCount * -tariff.getSignupPayment());
  }

  /**
   * Removes customerCount customers (at most) from this subscription,
   * posts early-withdrawal fees if appropriate. 
   */
  public void unsubscribe (int customerCount)
  {
    getTariffMarket().subscribeToTariff(getTariff(),
                                          getCustomer(),
                                          -customerCount);
    pendingUnsubscribeCount += customerCount;
  }

  /**
   * Handles the actual unsubscribe operation. Intended to be called by
   * the TariffMarket to avoid subscription changes between customer
   * consumption/production and balancing.
   */
  public void deferredUnsubscribe (int customerCount)
  {
    pendingUnsubscribeCount = 0;
    maxRemainingCurtailment = 0;
    // first, make customerCount no larger than the subscription count
    customerCount = Math.min(customerCount, customersCommitted);
    // find the number of customers who can withdraw without penalty
    int freeAgentCount = getExpiredCustomerCount();
    int penaltyCount = Math.max (customerCount - freeAgentCount, 0);
    // update the expirations list
    int expCount = customerCount;
    while (expCount > 0 && expirations.get(0) != null) {
      int cec = expirations.get(0).getCount();
      if (cec <= expCount) {
        expCount -= cec;
        expirations.remove(0);
      }
      else {
        expirations.get(0).updateCount(-expCount);
        expCount = 0;
      }
    }
    customersCommitted -= customerCount;
    // Post withdrawal and possible penalties
    //if (tariff.getEarlyWithdrawPayment() != 0.0 && penaltyCount > 0) {
      getAccounting().addTariffTransaction(TariffTransaction.Type.WITHDRAW,
          tariff, customer, customerCount, 0.0,
          penaltyCount * -tariff.getEarlyWithdrawPayment());
    //}
  }
  
  /**
   * Handles the subscription switch in case the underlying Tariff has been
   * revoked. The actual processing of tariff revocations, including switching
   * subscriptions to superseding tariffs, is deferred to be handled by the
   * tariff market.
   */
  public Tariff handleRevokedTariff ()
  {
    // if the tariff is not revoked, then just return this subscription
    if (!tariff.isRevoked()) {
      log.warn("Tariff " + tariff.getId() + " is not revoked.");
      return tariff;
    }
    // if no subscribers, we can ignore this
    if (0 == customersCommitted) {
      return null;
    }
    // if the tariff has already been superseded, then switch subscription to
    // that new tariff
    Tariff newTariff = null;
    //Tariff newTariff = tariff.getIsSupersededBy();
    if (newTariff == null) {
      // there is no superseding tariff, so we have to revert to the default tariff.
      newTariff =
        getTariffMarket().getDefaultTariff(tariff.getTariffSpec()
                .getPowerType());
    }
    if (newTariff == null) {
      // there is no exact match for original power type - choose generic
      newTariff =
        getTariffMarket().getDefaultTariff(tariff.getTariffSpec()
                .getPowerType().getGenericType());
    }

    getTariffMarket().subscribeToTariff(tariff, customer,
                                          -customersCommitted);
    getTariffMarket().subscribeToTariff(newTariff, customer,
                                          customersCommitted);
    log.info("Tariff " + tariff.getId() + " superseded by " + newTariff.getId()
             + " for " + customersCommitted + " customers");
    // customersCommitted = 0;
    return newTariff;
  }

  /**
   * Generates and returns a TariffTransaction instance for the current timeslot that
   * represents the amount of production (negative amount) or consumption
   * (positive amount), along with the credit/debit that results. Also generates
   * a separate TariffTransaction for the fixed periodic payment if it's non-zero.
   * Note that this value is the aggregate across the subscribed population,
   * and not a per-member value.
   */
  public void usePower (double kwh)
  {
    // do curtailment first
    double actualKwh = kwh - getCurtailedKwh(kwh, totalUsage);
    log.info("usePower " + kwh + ", actual " + actualKwh + 
             ", customer=" + customer.getName());
    // generate the usage transaction
    TariffTransaction.Type txType =
        actualKwh < 0 ? TariffTransaction.Type.PRODUCE: TariffTransaction.Type.CONSUME;
    getAccounting().addTariffTransaction(txType, tariff,
        customer, customersCommitted, -actualKwh,
        customersCommitted * -tariff.getUsageCharge(actualKwh / customersCommitted, totalUsage, true));
    if (getTimeService().getHourOfDay() == 0) {
      //reset the daily usage counter
      totalUsage = 0.0;
    }
    totalUsage += actualKwh / customersCommitted;
    // generate the periodic payment if necessary
    if (tariff.getPeriodicPayment() != 0.0) {
      getAccounting().addTariffTransaction(TariffTransaction.Type.PERIODIC,
          tariff, customer, customersCommitted, 0.0,
          customersCommitted * -tariff.getPeriodicPayment() / 24.0);
    }
  }

  /**
   * Posts the ratio for an EconomicControlEvent to the subscription for the
   * current timeslot. .
   */
  public synchronized void postRatioControl (double ratio)
  {
    pendingCurtailmentRatio = ratio;
  }

  /**
   * Posts a BalancingControlEvent to the subscription. This simply updates
   * the curtailment for the current timeslot by the amout of the control.
   * A positive value for kwh represents a reduction in consumption, or an
   * increase in production - in other words, a net gain for the broker's
   * energy account balance.
   */
  public synchronized void postBalancingControl (double kwh)
  {
    addCurtailment(kwh);
    // issue compensating tariff transaction
    TariffTransaction.Type txType =
        kwh > 0 ? TariffTransaction.Type.PRODUCE: TariffTransaction.Type.CONSUME;
    getAccounting().addTariffTransaction(txType, tariff,
        customer, customersCommitted, kwh,
        customersCommitted *
        tariff.getUsageCharge(kwh / customersCommitted, 
                              totalUsage, true));
    totalUsage -= kwh / customersCommitted;
  }

  /**
   * Returns the curtailment in kwh for the previous timeslot. 
   * Intended to be called by Customer models only. Note that this
   * method is not idempotent; if you call it twice in the same timeslot, the
   * second time returns zero.
   */
  public synchronized double getCurtailment ()
  {
    double result = curtailment;
    curtailment = 0.0;
    return result;
  }

  /**
   * Returns the maximum curtailment possible after the customer model has
   * run and possibly applied economic controls. Since this is potentially 
   * accessed after customers have updated their subscriptions, it's possible
   * that the value will have to be changed.
   */
  public double getMaxRemainingCurtailment ()
  {
    double result = maxRemainingCurtailment;
    if (0 == pendingUnsubscribeCount) {
      return result;
    }
    else {
      // need to adjust 
      double ratio = (double)(customersCommitted - pendingUnsubscribeCount)
                              / customersCommitted;
      log.info("remaining curtailment reduced by " + ratio);
      return (result * ratio);
    }
  }

  /**
   * Returns the ratio curtailment in kwh for the current timeslot.
   * Value is 0.0 for no curtailment, 1.0 for full curtailment.
   * Value is the minimum of what's requested and what's allowed by the
   * rates in effect given the time and cumulative usage.
   */
  double getCurtailedKwh (double proposedUsage, double cumulativeUsage)
  {
    // check for the usual case of no curtailment first
    //if (0.0 == pendingCurtailmentRatio)
    //  return 0.0;
    
    // otherwise find the minimum of what's asked for and what's allowed.
    double proposedCurtailment = proposedUsage * pendingCurtailmentRatio;
    maxRemainingCurtailment =
        tariff.getMaxCurtailment(proposedUsage, cumulativeUsage);
    double result = Math.min(proposedCurtailment, maxRemainingCurtailment);
    log.debug("proposedCurtailment=" + proposedCurtailment +
              ", maxCurtailment=" + maxRemainingCurtailment);
    pendingCurtailmentRatio = 0.0;
    curtailment += result; // saved until next timeslot
    maxRemainingCurtailment -= result;
    return result;
  }

  /**
   * Adds kwh to the curtailment in the current timeslot.
   */
  void addCurtailment (double kwh)
  {
    curtailment += kwh;
  }

  // access to Spring components
  private TimeService getTimeService ()
  {
    if (null == timeService)
      timeService = (TimeService)SpringApplicationContext.getBean("timeService");
    return timeService;
  }

  private Accounting getAccounting ()
  {
    if (null == accountingService)
      accountingService = (Accounting)SpringApplicationContext.getBean("accountingService");
    return accountingService;
  }

  private TariffMarket getTariffMarket ()
  {
    if (null == tariffMarketService)
      tariffMarketService = (TariffMarket)SpringApplicationContext.getBean("tariffMarketService");
    return tariffMarketService;
  }
  // -------------------- Expiration data -------------------
  /**
   * Returns the number of individual customers who may withdraw from this
   * subscription without penalty. Should return the total customer count
   * for a non-expiring tariff.
   */
  public int getExpiredCustomerCount ()
  {
    int cc = 0;
    Instant today =
            getTimeService().truncateInstant(getTimeService().getCurrentTime(),
                                             TimeService.DAY);
    for (ExpirationRecord exp : expirations) {
      if (exp.getHorizon() <= today.getMillis()) {
        cc += exp.getCount();
      }
    }
    return cc;
  }

  private class ExpirationRecord
  {
    private long horizon;
    private int count;

    ExpirationRecord (long horizon, int count)
    {
      super();
      this.horizon = horizon;
      this.count = count;
    }

    long getHorizon ()
    {
      return horizon;
    }
    
    int getCount ()
    {
      return count;
    }

    int updateCount (int increment)
    {
      count += increment;
      return count;
    }
  }
}
