/*
 * Copyright (c) 2011-2023 by the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

/**
 * A TariffSubscription is an entity representing an association between a Customer
 * and a Tariff. Instances of this class are not intended to be serialized.
 * You get one by calling the subscribe() method on Tariff. If there is no
 * current subscription for that Customer (which in most cases is actually
 * a population model), then a new TariffSubscription is created and
 * returned from the Tariff.  
 * @author John Collins, Carsten Block
 */
@Domain
public class TariffSubscription 
{
  static private Logger log = LogManager.getLogger(TariffSubscription.class.getName());

  long id = IdGenerator.createId();

  private TimeService timeService;

  private Accounting accountingService;

  private TariffMarket tariffMarketService;

  /** The customer who has this Subscription */
  private CustomerInfo customer;

  /** The tariff for which this subscription applies */
  private Tariff tariff;

  // id of tariff to allow construction by log analyzer
  private long tariffId;

  /** Total number of customers within a customer model that are committed 
   * to this tariff subscription. */
  private int customersCommitted = 0 ;

  /** Arbitrary data needed by population customers who may be divided among multiple
   * subscriptions and need to keep data on the join. */
  private Map<String, Object> customerDecorators;

  /** List of expiration dates. This is used only if the Tariff has a minDuration,
   *  before which a subscribed Customer cannot back out without a penalty. Each
   *  entry in this list is a pair [expiration-date, customer-count]. New entries
   *  are added chronologically at the end of the list, so the front of the list
   *  holds the oldest subscriptions - the ones that can be unsubscribed soonest
   *  without penalty. */
  private List<ExpirationRecord> expirations;

  /** Total usage so far in the current day, needed to compute charges for
   *  tiered rates.
   *  ** Not needed as of #1152 */
  //private double totalUsage = 0.0;
  
  /** Count of customers who will not be subscribers in the next timeslot */
  private int pendingUnsubscribeCount = 0; 

  // ------------- Regulation capacity ----------------
  /** Pending economic regulation (from phase 1) */
  private double pendingRegulationRatio = 0.0;

  /** Available regulation capacity for the current timeslot. */
  RegulationAccumulator regulationAccumulator;

  /** Actual up-regulation (positive) or down-regulation (negative)
   * from previous timeslot.
   * Should always be zero after the customer model has run. */
  private double regulation = 0.0;

  /**
   * Usage charge and per-member kWh computed in usePower() prior to regulation
   */
  private double originalKWh = 0.0;
  private double originalCharge = 0.0;

  /**
   * You need a CustomerInfo and a Tariff to create one of these.
   */
  public TariffSubscription (CustomerInfo customer, Tariff tariff)
  {
    super();
    this.customer = customer;
    this.tariff = tariff;
    this.tariffId = tariff.getId();
    expirations = new ArrayList<ExpirationRecord>();
    setRegulationCap(new RegulationAccumulator(0.0, 0.0));
  }

  /**
   * Alternate constructor for logtool analyzers in which Tariffs cannot
   * be reconstructed. Many features won't work if the Tariff does not exist.
   */
  public TariffSubscription (CustomerInfo customer, long tariffId)
  {
    super();
    this.customer = customer;
    this.tariffId = tariffId;
    expirations = new ArrayList<ExpirationRecord>();
    setRegulationCap(new RegulationAccumulator(0.0, 0.0));
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

  public long getTariffId ()
  {
    return tariffId;
  }

  public int getCustomersCommitted ()
  {
    return customersCommitted;
  }

  @StateChange
  public void setCustomersCommitted (int value)
  {
    customersCommitted = value;
  }

//  public double getTotalUsage ()
//  {
//    return totalUsage;
//  }

  // ============================ Customer API ===============================

  /**
   * Subscribes some number of discrete customers. This is typically some portion of the population in a
   * population model. We assume this is called from Tariff, as a result of calling tariff.subscribe().
   * Also, we record the expiration date of the tariff contract, just in case the tariff has a
   * minDuration. For the purpose of computing expiration, all contracts are assumed to begin at
   * 00:00 on the day of the subscription.
   */
  @StateChange
  public void subscribe (int customerCount)
  {
    // first, update the customer count
    setCustomersCommitted(getCustomersCommitted() + customerCount);
    
    // if the Tariff has a minDuration, then we have to record the expiration date.
    // we do this by adding an entry to end of list, or updating the entry at the end.
    // An entry is a pair [Instant, count]
    long minDuration = tariff.getMinDuration();
    //if (minDuration > 0) {
    // Compute the 00:00 Instant for the current time
    Instant start = getTimeService().getCurrentTime();
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
    // signup payment is positive for a bonus, so it's a debit for the broker.
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
   * the TariffMarket (phase 4) to avoid subscription changes between customer
   * consumption/production and balancing.
   */
  @StateChange
  public void deferredUnsubscribe (int customerCount)
  {
    pendingUnsubscribeCount = 0;
    if (customerCount == customersCommitted) {
      // common case
      setRegulationCap(new RegulationAccumulator(0.0, 0.0));
      setRegulation(0.0);
    }

    // first, make customerCount no larger than the subscription count
    if (customerCount > customersCommitted) {
      log.error("tariff " + tariff.getId() +
                " customer " + customer.getName() +
                ": attempt to unsubscribe " + customerCount +
                " from subscription of " + customersCommitted);
      customerCount = customersCommitted;
    }
//    customerCount = Math.min(customerCount, customersCommitted);
//    adjustRegulationCapacity((double)(customersCommitted - customerCount)
//                             / customersCommitted);
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
    setCustomersCommitted(getCustomersCommitted() - customerCount);
    // if count is now zero, set regulation capacity to zero
    if (0 == getCustomersCommitted()) {
      regulationAccumulator.setDownRegulationCapacity(0.0);
      regulationAccumulator.setUpRegulationCapacity(0.0);
    }
    // Post withdrawal and possible penalties
    double withdrawPayment = -tariff.getEarlyWithdrawPayment();
    if (tariff.isRevoked()) {
      withdrawPayment = 0.0;
    }
    getAccounting().addTariffTransaction(TariffTransaction.Type.WITHDRAW,
                                         tariff, customer, customerCount, 0.0,
                                         penaltyCount * withdrawPayment);
    if (tariff.getSignupPayment() < 0.0) {
      // Refund signup payment
      getAccounting().addTariffTransaction(TariffTransaction.Type.REFUND,
                                           tariff, customer,
                                           customerCount, 0.0,
                                           customerCount * tariff.getSignupPayment());
    }
  }

//  private void adjustRegulationCapacity (double ratio)
//  {
//    regulationAccumulator.setUpRegulationCapacity(regulationAccumulator
//        .getUpRegulationCapacity() * ratio);
//    regulationAccumulator.setDownRegulationCapacity(regulationAccumulator
//        .getDownRegulationCapacity() * ratio);
//  }

  /**
   * Adds a (name, value) pair to the CustomerDecorators map
   */
  public void addCustomerDecorator (String name, Object value)
  {
    if (null == customerDecorators) {
      customerDecorators = new HashMap<>();
    }
    customerDecorators.put(name, value);
  }

  /**
   * Returns the named customerDecorator, if any
   */
  public Object getCustomerDecorator (String name)
  {
    if (null == customerDecorators) {
      return null;
    }
    else {
      return customerDecorators.get(name);
    }
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
   * Generates and posts a TariffTransaction instance for the current timeslot that
   * represents the amount of production (negative amount) or consumption
   * (positive amount), along with the credit/debit that results. Also generates
   * a separate TariffTransaction for the fixed periodic payment if it's non-zero.
   * Note that the power usage value and the numbers in the
   * TariffTransaction are aggregated across the subscribed population,
   * not per-member values. This is where the signs on energy and cost are inverted
   * to convert from the customer-centric view in the tariff to the broker-centric
   * view in the transactions.
   */
  public void usePower (double kWh)
  {
    // deal with no-regulation customers
    ensureRegulationCapacity();
    // do economic control first
    //double kWhPerMember = kWh / customersCommitted;
    originalKWh = (kWh - getEconomicRegulation(kWh));
    log.info("usePower " + kWh + ", actual " + originalKWh + 
             ", customer=" + customer.getName());
    // generate the usage transaction
    TariffTransaction.Type txType =
        originalKWh < 0 ? TariffTransaction.Type.PRODUCE: TariffTransaction.Type.CONSUME;
    originalCharge =
            tariff.getUsageCharge(originalKWh, true);
    getAccounting().addTariffTransaction(txType, tariff,
        customer, customersCommitted, -originalKWh, -originalCharge);
//    if (getTimeService().getHourOfDay() == 0) {
//      //reset the daily usage counter
//      totalUsage = 0.0;
//    }
//    totalUsage += actualKwh / customersCommitted;
    // generate the periodic payment if necessary
    if (tariff.getPeriodicPayment() != 0.0) {
      getAccounting().addTariffTransaction(TariffTransaction.Type.PERIODIC,
          tariff, customer, customersCommitted, 0.0,
          customersCommitted * -tariff.getPeriodicPayment() / 24.0);
    }
  }

  /**
   * Returns the regulation in kwh, aggregated across the subscribed population,
   * for the previous timeslot. 
   * Intended to be called by Customer models only. Value is non-negative for
   * consumption power types, non-positive for production types. For storage
   * types it may be positive or negative.
   * 
   * NOTE: this method is not idempotent; if you call it twice
   * in the same timeslot, the second time returns zero.
   */
  public synchronized double getCurtailment ()
  {
    double sgn = 1.0;
    if (tariff.getPowerType().isProduction())
      sgn = -1.0;
    double result = sgn * Math.max(sgn * regulation, 0.0) * customersCommitted;
    setRegulation(0.0);
    return result;
  }

  /**
   * Returns the regulation quantity exercised per member
   * in the previous timeslot. For non-storage devices,
   * only up-regulation through curtailment is supported, 
   * and the result will be a non-negative value.
   * For storage devices, it may be positive (up-regulation) or negative
   * (down-regulation). 
   * Intended to be called by customer models.
   * 
   * NOTE: This method is not idempotent,
   * because the regulation quantity is reset to zero after it's accessed.
   */
  public synchronized double getRegulation ()
  {
    double result = regulation;
    setRegulation(0.0);
    return result;
  }

  //@StateChange
  public void setRegulation (double newValue)
  {
    regulation = newValue;
  }

  /**
   * Communicates the ability of the customer model to handle regulation
   * requests. Quantities are per-member.
   * 
   * NOTE: This method must be called once/timeslot for any customer that
   * offers regulation capacity, because otherwise the capacity will be
   * carried over from the previous timeslot.
   */
  //@StateChange
  public void setRegulationCapacity (RegulationCapacity capacity)
  {
    double upRegulation = capacity.getUpRegulationCapacity();
    double downRegulation = capacity.getDownRegulationCapacity();
    regulationAccumulator =
            new RegulationAccumulator(upRegulation, downRegulation);
  }

  // Local Regulation management
  void setRegulationCap (RegulationAccumulator capacity)
  {
    regulationAccumulator = capacity;
  }

  /**
   * Ensures that regulationAccumulator is non-null -
   * needed for non-regulatable customer models
   */
  public void ensureRegulationCapacity ()
  {
    if (null == regulationAccumulator) {
      setRegulationCap(new RegulationAccumulator(0.0, 0.0));
    }
  }

  /**
   * True just in case this subscription allows regulation.
   */
  public boolean hasRegulationRate ()
  {
    return this.tariff.hasRegulationRate();
  }

  /**
   * Returns the result of economic control in kwh for the current timeslot.
   * Value is the minimum of what's requested and what's allowed by the
   * Rates in effect given the time and cumulative usage. Intended to be
   * called within usePower() to implement economic control.
   * The parameters and return value are per-member values, not aggregated
   * across the subscription.
   * 
   * Depending on the value of pendingRegulationRatio, there are three possible
   * outcomes:
   * <ul>
   *  <li>(0.0 <= pendingRegulationRatio <= 1.0) represents simple curtailment.
   *    The returned value will be the minimum of the proposedUsage.
   *    This is the only possible result for a tariff without
   *    RegulationRates.</li>
   *  <li>(-1.0 <= pendingRegulationRatio < 0.0) represents down-regulation,
   *    dumping energy into a thermal or electrical storage device. Amount is
   *    limited by the available regulation capacity. This case is only
   *    supported under a RegulationRate.</li>
   *  <li>(1.0 < pendingRegulationRatio <= 2.0) represents discharge of an
   *    electrical storage device. Amount is
   *    limited by the available regulation capacity. This case is only
   *    supported under a RegulationRate.</li>
   * </ul>
   * 
   * Note that this method is not idempotent -- it should be called at most
   * once in each timeslot; this scheme makes one call every time the customer
   * uses power.
   */
  double getEconomicRegulation (double proposedUsage) //, double cumulativeUsage)
  {
    // reset the regulation qty here
    setRegulation(0.0);
    double result = 0.0;
    if (getTariff().hasRegulationRate()) {
      if (pendingRegulationRatio < 0.0) {
        // down-regulation - negative result
        result =
          (-pendingRegulationRatio)
              * regulationAccumulator.getDownRegulationCapacity();
        regulationAccumulator.setDownRegulationCapacity(regulationAccumulator
            .getDownRegulationCapacity() - result);
      }
      else if (pendingRegulationRatio > 1.0) {
        // discharge: between proposed usage and up-regulation capacity
        if (regulationAccumulator.getUpRegulationCapacity() > proposedUsage) {
          double excess =
            regulationAccumulator.getUpRegulationCapacity() - proposedUsage;
          result =
            proposedUsage + (pendingRegulationRatio - 1.0) * excess;
          regulationAccumulator.setUpRegulationCapacity(regulationAccumulator
              .getUpRegulationCapacity() - result);
        }
      }
      else {
        // curtailment based on regulation capacity
        result =
          pendingRegulationRatio * regulationAccumulator.getUpRegulationCapacity();
        regulationAccumulator.setUpRegulationCapacity(regulationAccumulator
            .getUpRegulationCapacity() - result);
      }
    }
    else {
      // find the minimum of what's asked for and what's allowed.
      double proposedUpRegulation = proposedUsage * pendingRegulationRatio;
      double mur = tariff.getMaxUpRegulation(proposedUsage); //, cumulativeUsage);
      result = Math.min(proposedUpRegulation, mur);
      log.debug("proposedUpRegulation=" + proposedUpRegulation
                + ", maxUpRegulation=" + mur);
      regulationAccumulator.setUpRegulationCapacity(mur - result);
    }
    if (0.0 != result)
      log.info("Economic control of {} by {}",
               customer.getName(), result);
    addRegulation(result); // saved until next timeslot
    pendingRegulationRatio = 0.0;
    return result;
  }

  // ===================== Demand Response / Balancing API ====================

  /**
   * Posts the ratio for an EconomicControlEvent to the subscription for the
   * current timeslot.
   */
  @StateChange
  public synchronized void postRatioControl (double ratio)
  {
    pendingRegulationRatio = ratio;
  }

  /**
   * Posts a BalancingControlEvent to the subscription and generates the correct
   * TariffTransactions.
   * Regulation for the current timeslot is updated by the amount of the control.
   * The kWh value is a population value, not a per-member value, and
   * the sign is from the perspective of the customer.
   * So a negative value of kWh represents up-regulation, or an
   * reduction in consumption, which of course is a net loss of energy for
   * the customer and a net gain for the broker.
   * The net usage for the timeslot is then the value from the original TTx minus this one,
   * but that's not quite true for the cost. The cost needs to include not only the
   * value attached to the balancing control, but also needs to correct the cost of
   * the original usage/production transaction for the current timeslot, just in case
   * the sign of the kWh is opposite to the sign of the kWh in the original TTx.
   */
  @StateChange
  public synchronized void postBalancingControl (double kWh)
  {
    // start by computing the correction to the cost value in the original TTx
    //double kWhPerMember = kWh / customersCommitted; 
    double correction = 0.0;
    if (tariff.hasRegulationRate() &&
            (Math.signum(kWh) != Math.signum(originalKWh))) {
      correction = -tariff.getUsageCharge(kWh, true);
      log.info("regulation charge adjustment = {}", correction);
    }
    
    // then issue compensating tariff transaction
    double regCharge = -tariff.getRegulationCharge(kWh, true);
    double updatedCharge =
             regCharge - correction;
    getAccounting().addRegulationTransaction(tariff,
        customer, customersCommitted, -kWh, -updatedCharge);
    addRegulation(kWh);
    if (kWh <= 0.0) {
      // up-regulation, kWh is negative
      regulationAccumulator.setUpRegulationCapacity(regulationAccumulator
          .getUpRegulationCapacity() + kWh);
    }
    else {
      // down-regulation, kWh is positive
      regulationAccumulator.setDownRegulationCapacity(regulationAccumulator
          .getDownRegulationCapacity() + kWh);
    }
    //totalUsage += kWh/customersCommitted;
  }

  /**
   * Returns the maximum aggregate up-regulation possible after the
   * customer model has run and possibly applied economic controls.
   * Since this is potentially 
   * accessed through the balancing market after customers have updated their
   * subscriptions, it's possible
   * that the value will have to be changed due to a change in customer count.
   * TODO: may need to be modified -- see issue #733.
   */
  public RegulationAccumulator getRemainingRegulationCapacity ()
  {
    if (0 == customersCommitted) {
      // nothing to do here...
      return new RegulationAccumulator(0.0, 0.0);
    }
    // generate aggregate value here
    double up =
      regulationAccumulator.getUpRegulationCapacity();
    double down =
      regulationAccumulator.getDownRegulationCapacity();
    if (0 == pendingUnsubscribeCount) {
      log.info("regulation capacity for " + getCustomer().getName()
               + ":" + this.getTariff().getId()
               + " (" + up + ", " + down + ")");
      return new RegulationAccumulator(up, down);
    }
    else {
      // we have some unsubscribes - need to adjust 
      double ratio = (double)(customersCommitted - pendingUnsubscribeCount)
                              / customersCommitted;
      log.info("remaining regulation capacity for "
               + getCustomer().getName() + ":" + this.getTariff().getId()
               + " reduced by " + ratio
               + " to (" + up * ratio + ", " + down * ratio + ")");
      return new RegulationAccumulator(up * ratio, down * ratio);
    }
  }

  /**
   * Adds kwh to the regulation exercised in the current timeslot.
   * Intended to be called during exercise of economic or balancing controls.
   * The kwh argument is a per-member value; positive for up-regulation,
   * negative for down-regulation.
   */
  void addRegulation (double kwh)
  {
    setRegulation(regulation + kwh);
  }

  // ================= access to Spring components =======================
  
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
    Instant now = getTimeService().getCurrentTime();
    for (ExpirationRecord exp : expirations) {
      if (exp.getHorizon() <= now.getMillis()) {
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
