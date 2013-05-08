/*
 * Copyright 2013 the original author or authors.
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

package org.powertac.evcustomer.customers;

import org.apache.log4j.Logger;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.powertac.common.*;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.ActivityDetail;
import org.powertac.evcustomer.beans.CarType;
import org.powertac.evcustomer.beans.SocialGroup;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;


/**
 * TODO
 *
 * @author Konstantina Valogianni, Govert Buijs
 * @version 0.1, Date: 2013.03.21
 */
public class EvSocialClass extends AbstractCustomer
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  private static Logger log = Logger.getLogger(EvSocialClass.class.getName());

  @Autowired
  private TimeService timeService;

  @Autowired
  private TimeslotRepo timeslotRepo;

  public static double EPSILON = 2.7;
  public static double LAMDA = 20;
  public static double PERCENTAGE = 100;
  public static long MEAN_TARIFF_DURATION = 7;
  public static int HOURS_OF_DAY = 24;
  public static int DAYS_OF_BOOTSTRAP = 14;
  public static int DAYS_OF_COMPETITION = 0;

  private final TariffEvaluationHelper tariffEvalHelper =
      new TariffEvaluationHelper();

  private HashMap<String, TariffSubscription> subscriptionMap =
      new HashMap<String, TariffSubscription>();
  // TODO Rename
  private HashMap<String, TariffSubscription> storageSubscriptionMap =
      new HashMap<String, TariffSubscription>();

  private Vector<EvCustomer> evCustomers;

  public EvSocialClass (String name) {
    super(name);

    timeslotRepo =
        (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    timeService = (TimeService) SpringApplicationContext.getBean("timeService");

    String[] typeList = new String[] { "NS"};
    for (String type: typeList) {
      subscriptionMap.put(type, null);
      storageSubscriptionMap.put(type, null);
    }
  }

  /**
   * TODO
   */
  public void initialize (List<SocialGroup> socialGroups,
                          List<Activity> activities,
                          List<List<ActivityDetail>> activityDetails,
                          List<CarType> carTypes,
                          int count, Random generator)
  {
    evCustomers = new Vector<EvCustomer>();
    for (int i = 0; i < count; i++) {
      EvCustomer evCustomer = new EvCustomer();
      evCustomer.initialize(socialGroups, activities, activityDetails, carTypes, generator);

      evCustomers.add(evCustomer);
    }
  }

  // =====EVALUATION FUNCTIONS===== //

  /**
   * The better tariffs have more chance to be chosen.
   * Run every 6 timeslots == 6 hours
   */
  public void possibilityEvaluationNewTariffs (List<Tariff> newTariffs,
                                               String type)
  {
    for (CustomerInfo customer: customerInfos) {
      List<TariffSubscription> subscriptions =
          tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer);

      if (subscriptions == null || subscriptions.size() == 0) {
        subscribeDefault();
        return;
      }

      log.debug("Estimation size for " + this.toString() + " = "
          + newTariffs.size());

      if (newTariffs.size() <= 1) {
        return;
      }

      Vector<Double> estimation = new Vector<Double>();
      for (Tariff tariff: newTariffs) {
        log.debug("Tariff : " + tariff.toString() + " Tariff Type : "
            + tariff.getTariffSpecification().getPowerType()
            + " Broker: " + tariff.getBroker().toString());

        // TODO Check this. Rewrite?
        boolean case1 = customer.getPowerType() ==
            tariff.getTariffSpecification().getPowerType();
        boolean case2 = customer.getPowerType() == EvCustomer.powerType;
        boolean case2B = (
            customer.getPowerType() == EvCustomer.powerType &&
            tariff.getTariffSpecification().getPowerType() == PowerType.CONSUMPTION);

        if (!tariff.isExpired() && (case1 || case2B) ) {
          estimation.add(-costEstimation(tariff, type));
        }
        else {
          estimation.add(Double.NEGATIVE_INFINITY);
        }
      }

      int minIndex = logitPossibilityEstimation(estimation);

      TariffSubscription sub = subscriptionMap.get(type);
      if (customer.getPowerType() == EvCustomer.powerType) {
        sub = storageSubscriptionMap.get(type);
      }

      log.debug("Equality: "
          + sub.getTariff().getTariffSpec().toString() + " = "
          + newTariffs.get(minIndex).getTariffSpec().toString());

      if (sub.getTariff().getTariffSpec() !=
          newTariffs.get(minIndex).getTariffSpec()) {
        log.debug("Changing From " + sub.toString() + " For PowerType "
            + customer.getPowerType() + " After Evaluation");

        changeSubscription(
            sub.getTariff(), newTariffs.get(minIndex), type, customer);
      }
    }
  }

  public void changeSubscription (Tariff tariff, Tariff newTariff, String type,
                                  CustomerInfo customer)
  {
    TariffSubscription ts =
        tariffSubscriptionRepo.getSubscription(customer, tariff);
    int populationCount = ts.getCustomersCommitted();
    unsubscribe(ts, populationCount);
    subscribe(newTariff, populationCount, customer);

    updateSubscriptions(tariff, newTariff, type, customer);
  }

  private void updateSubscriptions (Tariff tariff, Tariff newTariff,
                                    String type, CustomerInfo customer)
  {
    if (type.equals("NS")) {
      return;
    }

    TariffSubscription ts =
        tariffSubscriptionRepo.getSubscription(customer, tariff);
    TariffSubscription newTs =
        tariffSubscriptionRepo.getSubscription(customer, newTariff);

    log.debug(this.toString() + " Changing Only " + type);
    log.debug("Old:" + ts.toString() + "  New:" + newTs.toString());

    if (customer.getPowerType() == EvCustomer.powerType) {
      log.debug("For " + customer.getPowerType().toString());
      log.debug("Controllable Subscription Map: " + storageSubscriptionMap.toString());
      storageSubscriptionMap.put("NS", newTs);
    }
    else {
      log.debug("Subscription Map: " + subscriptionMap.toString());
      subscriptionMap.put("NS", newTs);
    }
  }

  /**
   * This function estimates the overall cost, taking into consideration the
   * fixed payments as well as the variable that are dependent on the tariff
   * rates
   */
  private double costEstimation (Tariff tariff, String type)
  {
    // all the customers with EVs are non shifting with respect to EV charging.
    // They charge whenever they are available and need charged battery

    double costVariable = estimateVariableTariffPayment(tariff, type);
    double costFixed = estimateFixedTariffPayments(tariff) * evCustomers.size();

    log.debug("Simple Evaluation for " + type);
    log.debug("Cost Variable: " + costVariable + " Cost Fixed: " + costFixed);

    return (costVariable + costFixed) / 1000000;
  }

  /**
   * This function estimates the variable cost, depending only to the load
   * quantity you consume
   */
  private double estimateVariableTariffPayment (Tariff tariff, String type)
  {
    // EVs are non shifting
    double[] dominantUsage = new double[HOURS_OF_DAY];
    if (type.equals("NS")) {
      for (EvCustomer evCustomer: evCustomers) {
        for (int i=0; i<dominantUsage.length; i++) {
          dominantUsage[i] += evCustomer.getDominantLoad() / HOURS_OF_DAY;
        }
      }
    }

    double dominantCostSummary = tariffEvalHelper.estimateCost(tariff, dominantUsage);
    log.debug("Dominant Cost Summary: " + dominantCostSummary);

    return -dominantCostSummary;
  }

  /**
   * This function estimates the fixed cost, comprised by fees, bonuses and
   * penalties that are the same no matter how much you consume
   */
  private double estimateFixedTariffPayments (Tariff tariff)
  {
    double lifecyclePayment =
        -tariff.getEarlyWithdrawPayment() - tariff.getSignupPayment();

    // When there is not a Minimum Duration of the contract, you cannot divide
    // with the duration because you don't know it.
    double minDuration;
    if (tariff.getMinDuration() == 0) {
      minDuration = MEAN_TARIFF_DURATION;
    }
    else {
      minDuration = (double) tariff.getMinDuration() / TimeService.DAY;
    }

    log.debug("Minimum Duration: " + minDuration);
    return lifecyclePayment / minDuration;
  }

  /**
   * This is the function that realizes the mathematical possibility formula
   * for the choice of tariff.
   */
  private int logitPossibilityEstimation (Vector<Double> estimation)
  {
    double summedEstimations = 0;
    Vector<Integer> randomizer = new Vector<Integer>();
    Vector<Integer> possibilities = new Vector<Integer>();

    for (int i = 0; i < estimation.size(); i++) {
      log.debug("Estimation for " + i + ": " + estimation.get(i));
      summedEstimations += Math.pow(EPSILON, LAMDA * estimation.get(i));
      log.debug("Cost variable: " + 50 * estimation.get(i));
      log.debug("Summary of Estimation: " + summedEstimations);
    }

    for (int i = 0; i < estimation.size(); i++) {
      possibilities
          .add((int) (PERCENTAGE * (Math
              .pow(EPSILON, LAMDA * estimation.get(i)) / summedEstimations)));
      for (int j = 0; j < possibilities.get(i); j++) {
        randomizer.add(i);
      }
    }

    int index = randomizer.get((int) (randomizer.size() * rs1.nextDouble()));
    log.debug("Randomizer Vector: " + randomizer);
    log.debug("Possibility Vector: " + possibilities.toString());
    log.debug("Resulting Index = " + index);
    return index;
  }

  @Override
  public void subscribeDefault ()
  {
    super.subscribeDefault();

    for (CustomerInfo customer: customerInfos) {
      if (customer.getPowerType() == EvCustomer.powerType &&
          tariffMarketService
              .getDefaultTariff(EvCustomer.powerType) == null) {

        log.debug("No Default Tariff for "
            + EvCustomer.powerType.toString() + " so the customer "
            + customer.toString()
            + " subscribe to CONSUMPTION Default Tariff instead");
        tariffMarketService.subscribeToTariff(tariffMarketService
            .getDefaultTariff(PowerType.CONSUMPTION), customer, customer
            .getPopulation());
        log.info("CustomerInfo of type "
            + EvCustomer.powerType.toString() + " of "
            + toString()
            + " was subscribed to the default CONSUMPTION tariff successfully.");
      }

      List<TariffSubscription> subscriptions =
          tariffSubscriptionRepo.findSubscriptionsForCustomer(customer);

      if (subscriptions.size() > 0) {
        log.info(subscriptions.toString());

        for (String type: subscriptionMap.keySet()) {
          if (customer.getPowerType() == PowerType.CONSUMPTION) {
            subscriptionMap.put(type, subscriptions.get(0));
          }
          if (customer.getPowerType() == EvCustomer.powerType) {
            storageSubscriptionMap.put(type, subscriptions.get(0));
          }
        }
      }
    }

    log.info("Consume Subscriptions:" + subscriptionMap.toString());
    log.info("Storage Subscriptions:" + storageSubscriptionMap.toString());
  }

  @Override
  public void step ()
  {
    long millis = timeService.getCurrentTime().getMillis();
    int hour = timeService.getHourOfDay();
    int dayOfWeek = new Instant(millis).get(DateTimeFieldType.dayOfWeek());

    // TODO Replace with household-customer or factored-customer version?
    checkRevokedSubscriptions();

    doActivities(dayOfWeek, hour);

    consumePower();
  }

  @Override
  public void consumePower ()
  {
    int serial;
    Timeslot ts = timeslotRepo.currentTimeslot();
    if (ts == null) {
      log.debug("Current timeslot is null");
      serial = (int)
          ((timeService.getCurrentTime().getMillis() - timeService.getBase())
              / TimeService.HOUR);
    }
    else {
      log.debug("Timeslot Serial: " + ts.getSerialNumber());
      serial = ts.getSerialNumber();
    }

    HashMap<TariffSubscription, Double> subs =
        new HashMap<TariffSubscription, Double>();
    for (TariffSubscription sub: subscriptionMap.values()) {
      subs.put(sub, 0.0);
    }

    for (TariffSubscription sub: storageSubscriptionMap.values()) {
      subs.put(sub, getConsumptionByTimeslot(serial));
    }

    for (TariffSubscription sub: subs.keySet()) {
      if (sub == null) {
        continue;
      }

      double usage = subs.get(sub);

      log.debug("Consumption Load for Customer " + sub.getCustomer().toString()
          + ": \n" + subs.get(sub));

      if (sub.getCustomersCommitted() > 0) {
        sub.usePower(usage);
      }
    }
  }

  /*
   * TODO
   */
  private double getConsumptionByTimeslot (int serial)
  {
    int hour = serial % HOURS_OF_DAY;
    long millis = timeService.getCurrentTime().getMillis();
    int dayOfWeek = new Instant(millis).get(DateTimeFieldType.dayOfWeek());

    double totalConsumption = 0.0;
    for (EvCustomer evCustomer: evCustomers) {
      totalConsumption += evCustomer.charge(dayOfWeek, hour);
    }

    return totalConsumption;
  }

  /*
   * TODO
   */
  private void doActivities (int day, int hour)
  {
    for (EvCustomer evCustomer: evCustomers) {
      evCustomer.doActivities(day, hour);
    }
  }

  @Override
  public String toString ()
  {
    return name;
  }

  public HashMap<String, TariffSubscription> getStorageSubscriptionMap ()
  {
    return storageSubscriptionMap;
  }
}
