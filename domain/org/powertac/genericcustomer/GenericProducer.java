/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.genericcustomer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.AbstractCustomer;
import org.powertac.common.Broker;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.configurations.GenericConstants;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.Domain;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Genric Producer implementation
 * @author Antonios Chrysopoulos
 */
@Domain
// causes calls to the constructor to be logged
public class GenericProducer extends AbstractCustomer
{
  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error() appropriately. Use
   * log.debug() for output you want to see in testing or debugging.
   */
  static protected Logger log = Logger.getLogger(AbstractCustomer.class.getName());

  @Autowired
  TimeService timeService;

  @Autowired
  TimeslotRepo timeslotRepo;

  /** This is the constructor initializing Generic Producer */
  public GenericProducer (CustomerInfo customerInfo)
  {
    super(customerInfo);

    timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    timeService = (TimeService) SpringApplicationContext.getBean("timeService");
  }

  // =============================CONSUMPTION-PRODUCTION=================================================

  @Override
  public void producePower ()
  {
    Timeslot ts = timeslotRepo.currentTimeslot();
    double summary = 0;
    List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(this.getCustomerInfo());

    for (TariffSubscription sub : subscriptions) {
      if (ts == null)
        summary = getProductionByTimeslot(sub);
      else
        summary = getProductionByTimeslot(ts.getSerialNumber());
      log.info("Production Load: " + summary + "/" + subscriptions.size());
      sub.usePower(-(summary / subscriptions.size()));
    }
  }

  /**
   * In this function, the broker calls for a certain amount of energy as it has predicted and the
   * producer produces this amount in return
   */
  void producePower (Broker broker, double amount)
  {

    ArrayList<TariffSubscription> subs = new ArrayList<TariffSubscription>();
    List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(this.getCustomerInfo());

    for (TariffSubscription sub : subscriptions) {
      if (sub.getTariff().getBroker() == broker)
        subs.add(sub);
    }

    if (subs.size() == 0)
      log.info("Not subscribed to broker " + broker.toString());
    else {
      for (TariffSubscription sub : subs) {
        log.info("Production Load: " + amount + "/" + subs.size());
        sub.usePower(-(amount / subs.size()));
      }
    }
  }

  /**
   * In this function, the broker calls for a certain amount of energy as it has predicted and the
   * producer produces this amount in return
   */
  void producePower (Tariff tariff, double amount)
  {

    ArrayList<TariffSubscription> subs = new ArrayList<TariffSubscription>();
    List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(this.getCustomerInfo());

    for (TariffSubscription sub : subscriptions) {
      if (sub.getTariff() == tariff)
        subs.add(sub);
    }

    if (subs.size() == 0)
      log.info("Not subscribed to tariff " + tariff.toString());
    else {
      for (TariffSubscription sub : subs) {
        log.info("Production Load: " + amount + "/" + subs.size());
        sub.usePower(-(amount / subs.size()));
      }
    }
  }

  /**
   * This method takes as an input the timeslot serial number (in order to know in the current time)
   * and estimates the production for this timeslot over the population under the Generic Producer.
   */
  double getProductionByTimeslot (int serial)
  {
    int hour = (int) (serial % GenericConstants.HOURS_OF_DAY);

    log.info("Hour: " + hour);
    return 100 * rs1.nextFloat();
  }

  /** This method is an overload of the function above, using as input the TariffSubscription. */
  double getProductionByTimeslot (TariffSubscription sub)
  {
    int hour = timeService.getHourOfDay();

    log.info("Hour: " + hour);
    return 100 * rs1.nextFloat();
  }

  /**
   * This is the basic evaluation function, taking into consideration the maximum payment without
   * shifting the appliances' load and taking the optimal tariff.
   */
  void simpleEvaluationNewTariffs (List<Tariff> newTariffs)
  {

    // if there are no current subscriptions, then this is the
    // initial publication of default tariffs

    List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(this.getCustomerInfo());

    if (subscriptions == null || subscriptions.size() == 0) {
      subscribeDefault();
      return;
    }

    double minEstimation = Double.POSITIVE_INFINITY;
    int index = 0, minIndex = 0;

    // adds current subscribed tariffs for reevaluation
    ArrayList<Tariff> evaluationTariffs = new ArrayList<Tariff>(newTariffs);
    Collections.copy(evaluationTariffs, newTariffs);

    for (TariffSubscription sub : subscriptions) {
      evaluationTariffs.add(sub.getTariff());
    }
    log.debug("Estimation size for this.toString = " + evaluationTariffs.size());

    if (evaluationTariffs.size() > 1) {
      for (Tariff tariff : evaluationTariffs) {
        log.info("Tariff : " + tariff.toString() + " Tariff Type : " + tariff.getTariffSpecification().getPowerType());
        if (tariff.isExpired() == false && tariff.getTariffSpecification().getPowerType() == PowerType.PRODUCTION) {
          minEstimation = (double) Math.min(minEstimation, this.paymentEstimation(tariff));
          minIndex = index;
        }
        index++;
      }
      log.info("Tariff: " + evaluationTariffs.get(minIndex).toString() + " Estimation = " + minEstimation);

      for (TariffSubscription sub : subscriptions) {
        log.info("Equality: " + sub.getTariff().getTariffSpec().toString() + " = " + evaluationTariffs.get(minIndex).getTariffSpec().toString());
        if (!(sub.getTariff().getTariffSpec() == evaluationTariffs.get(minIndex).getTariffSpec())) {
          log.info("Existing subscription " + sub.toString());
          int populationCount = sub.getCustomersCommitted();
          subscribe(evaluationTariffs.get(minIndex), populationCount);
          unsubscribe(sub, populationCount);
        }
      }
    }
  }

  /**
   * This is the basic evaluation function, taking into consideration the maximum payment without
   * shifting the appliances' load but the tariff chosen is picked up randomly by using a
   * possibility pattern. The better tariffs have more chances to be chosen.
   */
  void possibilityEvaluationNewTariffs (List<Tariff> newTariffs)
  {

    List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(this.getCustomerInfo());

    if (subscriptions == null || subscriptions.size() == 0) {
      subscribeDefault();
      return;
    }

    Vector<Double> estimation = new Vector<Double>();

    // adds current subscribed tariffs for reevaluation
    ArrayList<Tariff> evaluationTariffs = new ArrayList<Tariff>(newTariffs);
    Collections.copy(evaluationTariffs, newTariffs);

    log.debug("Estimation size for " + this.toString() + " = " + evaluationTariffs.size());

    if (evaluationTariffs.size() > 1) {
      for (Tariff tariff : evaluationTariffs) {
        log.info("Tariff : " + tariff.toString() + " Tariff Type : " + tariff.getTariffSpecification().getPowerType());
        if (tariff.isExpired() == false && tariff.getTariffSpecification().getPowerType() == PowerType.PRODUCTION) {
          estimation.add(paymentEstimation(tariff));
        } else
          estimation.add(Double.POSITIVE_INFINITY);
      }

      int maxIndex = logitPossibilityEstimation(estimation);

      for (TariffSubscription sub : subscriptions) {
        log.info("Equality: " + sub.getTariff().getTariffSpec().toString() + " = " + evaluationTariffs.get(maxIndex).getTariffSpec().toString());
        if (!(sub.getTariff().getTariffSpec() == evaluationTariffs.get(maxIndex).getTariffSpec())) {
          log.info("Existing subscription " + sub.toString());
          int populationCount = sub.getCustomersCommitted();
          subscribe(evaluationTariffs.get(maxIndex), populationCount);
          unsubscribe(sub, populationCount);
        }
      }
    }
  }

  /**
   * This function estimates the overall payment, taking into consideration the fixed payments as
   * well as the variable that are depending on the tariff rates
   */
  double paymentEstimation (Tariff tariff)
  {
    double costVariable = estimateVariableTariffPayment(tariff);
    double costFixed = estimateFixedTariffPayments(tariff);
    return (costVariable + costFixed) / GenericConstants.MILLION;
  }

  /**
   * This function estimates the fixed payments, comprised by fees, bonuses and penalties that are
   * the same no matter how much you produce
   */
  double estimateFixedTariffPayments (Tariff tariff)
  {
    double lifecyclePayment = (double) tariff.getEarlyWithdrawPayment() + (double) tariff.getSignupPayment();
    double minDuration;

    // When there is not a Minimum Duration of the contract, you cannot divide with the duration
    // because you don't know it.
    if (tariff.getMinDuration() == 0)
      minDuration = GenericConstants.MEAN_TARIFF_DURATION * TimeService.DAY;
    else
      minDuration = tariff.getMinDuration();

    log.info("Minimum Duration: " + minDuration);
    return ((double) tariff.getPeriodicPayment() + (lifecyclePayment / minDuration));
  }

  /**
   * This function estimates the variable payment, depending only to the load quantity you produce
   */
  double estimateVariableTariffPayment (Tariff tariff)
  {

    double paymentSummary = 0;
    double summary = 0, cumulativeSummary = 0;

    int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    Instant base = new Instant(timeService.getCurrentTime().getMillis() - serial * TimeService.HOUR);
    int day = (int) (serial / GenericConstants.HOURS_OF_DAY) + 1; // this will be changed to one or
                                                                  // more random numbers
    Instant now = new Instant(base.getMillis() + day * TimeService.DAY);

    for (int i = 0; i < GenericConstants.HOURS_OF_DAY; i++) {
      summary = getProductionByTimeslot(i);
      cumulativeSummary += summary;
      paymentSummary += tariff.getUsageCharge(now, summary, cumulativeSummary);
      log.info("Time: " + now.toString() + " paymentSummary: " + paymentSummary);
      now = new Instant(now.getMillis() + TimeService.HOUR);
    }
    log.info("Variable payment Summary: " + paymentSummary);
    return paymentSummary;
  }

  /**
   * This is the function that realizes the mathematical possibility formula for the choice of
   * tariff.
   */
  int logitPossibilityEstimation (Vector<Double> estimation)
  {

    double lamda = -2500; // 0 the random - 10 the logic
    double summedEstimations = 0;
    log.info(estimation.toString());
    Vector<Integer> randomizer = new Vector<Integer>();
    Vector<Integer> possibilities = new Vector<Integer>();

    for (int i = 0; i < estimation.size(); i++) {
      summedEstimations += Math.pow(GenericConstants.EPSILON, lamda * estimation.get(i));
      log.info("Payment variable: " + estimation.get(i));
      log.info("Summary of Estimation: " + summedEstimations);
    }

    for (int i = 0; i < estimation.size(); i++) {
      possibilities.add((int) (GenericConstants.PERCENTAGE * (Math.pow(GenericConstants.EPSILON, lamda * estimation.get(i)) / summedEstimations)));
      for (int j = 0; j < possibilities.get(i); j++) {
        randomizer.add(i);
      }
    }

    log.info("Randomizer Vector: " + randomizer);
    log.info("Possibility Vector: " + possibilities.toString());
    int index = randomizer.get((int) (randomizer.size() * rs1.nextDouble()));
    log.info("Resulting Index = " + index);
    return index;
  }

  /** Useful for trace logging */
  public String toString ()
  {
    return ("GenericProducer " + custId);
  }

  @Override
  public void step ()
  {
    this.checkRevokedSubscriptions();
    this.producePower();
  }

}
