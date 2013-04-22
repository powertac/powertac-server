/*
 * Copyright 2009-2012 the original author or authors.
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
package org.powertac.householdcustomer.customers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.AbstractCustomer;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluationHelper;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.householdcustomer.configurations.VillageConstants;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The village domain class is a set of households that comprise a small village
 * that consumes aggregated energy by the appliances installed in each
 * household.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Village extends AbstractCustomer
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static protected Logger log = Logger.getLogger(Village.class.getName());

  @Autowired
  TimeService timeService;

  @Autowired
  TimeslotRepo timeslotRepo;

  @Autowired
  WeatherReportRepo weatherReportRepo;

  int seedId = 1;

  /**
   * These are the vectors containing aggregated each day's base load from the
   * appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyBaseLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's controllable load
   * from the appliances installed inside the households.
   **/
  Vector<Vector<Long>> aggDailyControllableLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's weather sensitive
   * load from the appliances installed inside the households.
   **/
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadNS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadRaS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadReS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadSS =
    new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's dominant load from
   * the appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyDominantLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyDominantLoadRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyDominantLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyDominantLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's non dominant load
   * from the appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyNonDominantLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyNonDominantLoadRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyNonDominantLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyNonDominantLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the aggregated vectors containing each day's base load of all the
   * households in hours.
   **/
  Vector<Vector<Long>> aggDailyBaseLoadInHoursNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursSS = new Vector<Vector<Long>>();

  /**
   * These are the aggregated vectors containing each day's controllable load of
   * all the households in hours.
   **/
  Vector<Vector<Long>> aggDailyControllableLoadInHoursNS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursRaS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursReS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursSS =
    new Vector<Vector<Long>>();

  /**
   * These are the aggregated vectors containing each day's weather sensitive
   * load of all the households in hours.
   **/
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursNS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursRaS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursReS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursSS =
    new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's dominant load from
   * the appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyDominantLoadInHoursNS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyDominantLoadInHoursRaS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyDominantLoadInHoursReS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyDominantLoadInHoursSS =
    new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's non dominant load
   * from the appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyNonDominantLoadInHoursNS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyNonDominantLoadInHoursRaS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyNonDominantLoadInHoursReS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyNonDominantLoadInHoursSS =
    new Vector<Vector<Long>>();

  /**
   * These are the mean consumption of the village types for the days with the
   * dominant appliances working.
   **/
  double[] dominantLoadNS = new double[VillageConstants.HOURS_OF_DAY];
  double[] dominantLoadRaS = new double[VillageConstants.HOURS_OF_DAY];
  double[] dominantLoadReS = new double[VillageConstants.HOURS_OF_DAY];
  double[] dominantLoadSS = new double[VillageConstants.HOURS_OF_DAY];

  /**
   * These are the mean consumption of the village types for the days with the
   * dominant appliances not working.
   **/
  double[] nonDominantLoadNS = new double[VillageConstants.HOURS_OF_DAY];
  double[] nonDominantLoadRaS = new double[VillageConstants.HOURS_OF_DAY];
  double[] nonDominantLoadReS = new double[VillageConstants.HOURS_OF_DAY];
  double[] nonDominantLoadSS = new double[VillageConstants.HOURS_OF_DAY];

  /**
   * This is an vector containing the days of the competition that the household
   * model will use in order to check which of the tariffs that are available at
   * any given moment are the optimal for their consumption or production.
   **/
  Vector<Integer> daysList = new Vector<Integer>();

  protected final TariffEvaluationHelper tariffEvalHelper =
    new TariffEvaluationHelper();

  /**
   * This variable is utilized for the creation of the RandomSeed numbers and is
   * taken from the service.
   */
  RandomSeed gen;

  /**
   * These variables are mapping of the characteristics of the types of houses.
   * The first is used to keep track of their subscription at any given time.
   * The second is the inertia parameter for each type of houses. The third is
   * the period that they are evaluating the available tariffs and choose the
   * best for their type. The forth is setting the lamda variable for the
   * possibility function of the evaluation.
   */
  HashMap<String, TariffSubscription> subscriptionMap =
    new HashMap<String, TariffSubscription>();
  HashMap<String, TariffSubscription> controllableSubscriptionMap =
    new HashMap<String, TariffSubscription>();
  HashMap<String, Double> inertiaMap = new HashMap<String, Double>();
  HashMap<String, Integer> periodMap = new HashMap<String, Integer>();
  HashMap<String, Double> lamdaMap = new HashMap<String, Double>();
  HashMap<String, Boolean> superseded = new HashMap<String, Boolean>();
  HashMap<String, Double> riskMap = new HashMap<String, Double>();
  HashMap<String, Double> withdrawalMap = new HashMap<String, Double>();

  /**
   * These vectors contain the houses of type in the village. There are 4 types
   * available: 1) Not Shifting Houses: They do not change the tariff
   * subscriptions during the game. 2) Randomly Shifting Houses: They change
   * their tariff subscriptions in a RandomSeed way. 3) Regularly Shifting
   * Houses:
   * They change their tariff subscriptions during the game in regular time
   * periods. 4) Smart Shifting Houses: They change their tariff subscriptions
   * in a smart way in order to minimize their costs.
   */
  Vector<Household> notShiftingHouses = new Vector<Household>();
  Vector<Household> randomlyShiftingHouses = new Vector<Household>();
  Vector<Household> regularlyShiftingHouses = new Vector<Household>();
  Vector<Household> smartShiftingHouses = new Vector<Household>();

  /** This is the constructor function of the Village customer */
  public Village (String name)
  {
    super(name);

    timeslotRepo =
      (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    timeService = (TimeService) SpringApplicationContext.getBean("timeService");
    weatherReportRepo =
      (WeatherReportRepo) SpringApplicationContext.getBean("weatherReportRepo");

    ArrayList<String> typeList = new ArrayList<String>();
    typeList.add("NS");
    typeList.add("RaS");
    typeList.add("ReS");
    typeList.add("SS");

    for (String type: typeList) {
      subscriptionMap.put(type, null);
      controllableSubscriptionMap.put(type, null);
      inertiaMap.put(type, null);
      periodMap.put(type, null);
      lamdaMap.put(type, null);
      superseded.put(type, null);
      riskMap.put(type, null);
      withdrawalMap.put(type, null);
    }
  }

  /** This is the second constructor function of the Village customer */
  public Village (String name, ArrayList<CustomerInfo> customerInfo)
  {
    super(name, customerInfo);

    timeslotRepo =
      (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    timeService = (TimeService) SpringApplicationContext.getBean("timeService");
    weatherReportRepo =
      (WeatherReportRepo) SpringApplicationContext.getBean("weatherReportRepo");

    ArrayList<String> typeList = new ArrayList<String>();
    typeList.add("NS");
    typeList.add("RaS");
    typeList.add("ReS");
    typeList.add("SS");

    for (String type: typeList) {
      subscriptionMap.put(type, null);
      controllableSubscriptionMap.put(type, null);
      inertiaMap.put(type, null);
      periodMap.put(type, null);
      lamdaMap.put(type, null);
      superseded.put(type, null);
      riskMap.put(type, null);
      withdrawalMap.put(type, null);
    }
  }

  /**
   * This is the initialization function. It uses the variable values for the
   * configuration file to create the village with its households and then fill
   * them with persons and appliances.
   * 
   * @param conf
   * @param gen
   */
  public void initialize (Properties conf, int seed)
  {
    // Initializing variables

    int nshouses = Integer.parseInt(conf.getProperty("NotShiftingCustomers"));
    int rashouses =
      Integer.parseInt(conf.getProperty("RegularlyShiftingCustomers"));
    int reshouses =
      Integer.parseInt(conf.getProperty("RandomlyShiftingCustomers"));
    int sshouses = Integer.parseInt(conf.getProperty("SmartShiftingCustomers"));
    int days = Integer.parseInt(conf.getProperty("PublicVacationDuration"));

    gen =
      randomSeedRepo.getRandomSeed(toString(), seed, "Village Model" + seed);

    createCostEstimationDaysList(VillageConstants.RANDOM_DAYS_NUMBER);

    Vector<Integer> publicVacationVector = createPublicVacationVector(days);

    for (int i = 0; i < nshouses; i++) {
      log.info("Initializing " + toString() + " NSHouse " + i);
      Household hh = new Household();
      hh.initialize(toString() + " NSHouse" + i, conf, publicVacationVector,
                    seedId++);
      notShiftingHouses.add(hh);
      hh.householdOf = this;
    }

    for (int i = 0; i < rashouses; i++) {
      log.info("Initializing " + toString() + " RaSHouse " + i);
      Household hh = new Household();
      hh.initialize(toString() + " RaSHouse" + i, conf, publicVacationVector,
                    seedId++);
      randomlyShiftingHouses.add(hh);
      hh.householdOf = this;
    }

    for (int i = 0; i < reshouses; i++) {
      log.info("Initializing " + toString() + " ReSHouse " + i);
      Household hh = new Household();
      hh.initialize(toString() + " ReSHouse" + i, conf, publicVacationVector,
                    seedId++);
      regularlyShiftingHouses.add(hh);
      hh.householdOf = this;
    }

    for (int i = 0; i < sshouses; i++) {
      log.info("Initializing " + toString() + " SSHouse " + i);
      Household hh = new Household();
      hh.initialize(toString() + " SSHouse" + i, conf, publicVacationVector,
                    seedId++);
      smartShiftingHouses.add(hh);
      hh.householdOf = this;
    }

    for (String type: subscriptionMap.keySet()) {
      fillAggWeeklyLoad(type);
      inertiaMap.put(type,
                     Double.parseDouble(conf.getProperty(type + "Inertia")));
      periodMap.put(type, Integer.parseInt(conf.getProperty(type + "Period")));
      lamdaMap.put(type, Double.parseDouble(conf.getProperty(type + "Lamda")));
      superseded.put(type, false);
      double weight = gen.nextDouble() * VillageConstants.WEIGHT_RISK;
      double risk = gen.nextDouble() * VillageConstants.RISK_FACTOR;
      riskMap.put(type, weight * risk);

      double weeks =
        gen.nextInt(VillageConstants.MAX_DEFAULT_DURATION
                    - VillageConstants.MIN_DEFAULT_DURATION)
                + VillageConstants.MIN_DEFAULT_DURATION;

      withdrawalMap.put(type, weeks);

      /*
            
            System.out.println(toString() + " " + type);
            System.out.println("Dominant Consumption:"
            + Arrays.toString(getDominantLoad(type)));
            System.out.println("Non Dominant Consumption:"
            + Arrays.toString(getNonDominantLoad(type)));
            */
    }

    // System.out.println(toString() + " "
    // + aggDailyDominantLoadInHoursNS.get(0).toString());
    //
    // System.out.println(toString() + " "
    // + aggDailyNonDominantLoadInHoursNS.get(0).toString());
    //
    //
    // System.out.println("Subscriptions:" + subscriptionMap.toString());
    // System.out.println("Controllable Subscriptions:" +
    // controllableSubscriptionMap.toString());
    // System.out.println("Inertia:" + inertiaMap.toString());
    // System.out.println("Period:" + periodMap.toString());
    // System.out.println("Lamda:" + lamdaMap.toString());
    // System.out.println("Risk:" + riskMap.toString());
    // System.out.println("Withdrawal:" + withdrawalMap.toString())
    //
    //
    // for (String type : subscriptionMap.keySet()) {
    // showAggLoad(type);
    // }

  }

  // =====SUBSCRIPTION FUNCTIONS===== //

  @Override
  public void subscribeDefault ()
  {
    super.subscribeDefault();

    for (CustomerInfo customer: customerInfos) {

      if (customer.getPowerType() == PowerType.INTERRUPTIBLE_CONSUMPTION
          && tariffMarketService
                  .getDefaultTariff(PowerType.INTERRUPTIBLE_CONSUMPTION) == null) {

        log.debug("No Default Tariff for INTERRUPTIBLE_CONSUMPTION so the customer "
                  + customer.toString()
                  + " subscribe to CONSUMPTION Default Tariff instead");
        tariffMarketService.subscribeToTariff(tariffMarketService
                .getDefaultTariff(PowerType.CONSUMPTION), customer, customer
                .getPopulation());
        log.info("CustomerInfo of type INTERRUPTIBLE_CONSUMPTION of "
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
          else if (customer.getPowerType() == PowerType.INTERRUPTIBLE_CONSUMPTION) {
            controllableSubscriptionMap.put(type, subscriptions.get(0));
          }
        }
      }
    }

    log.info("Base Load Subscriptions:" + subscriptionMap.toString());
    log.info("Controllable Load Subscriptions:"
             + controllableSubscriptionMap.toString());
  }

  /**
   * The first implementation of the changing subscription function. Here we
   * just put the tariff we want to change and the whole population is moved to
   * another RandomSeed tariff.
   * 
   * @param tariff
   */
  public void changeSubscription (Tariff tariff, CustomerInfo customer)
  {

    TariffSubscription ts =
      tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff,
                                                                  customer);
    int populationCount = ts.getCustomersCommitted();
    unsubscribe(ts, populationCount);

    Tariff newTariff;

    if (customer.getPowerType() == PowerType.INTERRUPTIBLE_CONSUMPTION
        && tariffMarketService
                .getActiveTariffList(PowerType.INTERRUPTIBLE_CONSUMPTION)
                .size() > 1)
      newTariff = selectTariff(PowerType.INTERRUPTIBLE_CONSUMPTION);
    else
      newTariff = selectTariff(PowerType.CONSUMPTION);

    subscribe(newTariff, populationCount, customer);

    updateSubscriptions(tariff, newTariff, customer, false);

  }

  /**
   * The second implementation of the changing subscription function only for
   * certain type of the households.
   * 
   * @param tariff
   */
  public void changeSubscription (Tariff tariff, String type,
                                  CustomerInfo customer)
  {
    TariffSubscription ts =
      tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff,
                                                                  customer);
    int populationCount = getHouses(type).size();
    unsubscribe(ts, populationCount);

    Tariff newTariff;

    if (customer.getPowerType() == PowerType.INTERRUPTIBLE_CONSUMPTION
        && tariffMarketService
                .getActiveTariffList(PowerType.INTERRUPTIBLE_CONSUMPTION)
                .size() > 1)
      newTariff = selectTariff(PowerType.INTERRUPTIBLE_CONSUMPTION);
    else
      newTariff = selectTariff(PowerType.CONSUMPTION);

    subscribe(newTariff, populationCount, customer);

    updateSubscriptions(tariff, newTariff, type, customer);
  }

  /**
   * In this overloaded implementation of the changing subscription function,
   * Here we just put the tariff we want to change and the whole population is
   * moved to another RandomSeed tariff.
   * 
   * @param tariff
   */
  public void changeSubscription (Tariff tariff, Tariff newTariff,
                                  CustomerInfo customer)
  {
    TariffSubscription ts =
      tariffSubscriptionRepo.getSubscription(customer, tariff);
    int populationCount = ts.getCustomersCommitted();
    unsubscribe(ts, populationCount);

    subscribe(newTariff, populationCount, customer);

    updateSubscriptions(tariff, newTariff, customer, false);
  }

  /**
   * In this overloaded implementation of the changing subscription function
   * only certain type of the households.
   * 
   * @param tariff
   */
  public void changeSubscription (Tariff tariff, Tariff newTariff, String type,
                                  CustomerInfo customer)
  {
    TariffSubscription ts =
      tariffSubscriptionRepo.getSubscription(customer, tariff);
    int populationCount = getHouses(type).size();
    unsubscribe(ts, populationCount);
    subscribe(newTariff, populationCount, customer);

    updateSubscriptions(tariff, newTariff, type, customer);
  }

  /**
   * This function is used to update the subscriptionMap variable with the
   * changes made.
   * 
   */
  public void updateSubscriptions (Tariff tariff, Tariff newTariff,
                                   CustomerInfo customer, boolean superseded)
  {

    TariffSubscription ts =
      tariffSubscriptionRepo.getSubscription(customer, tariff);
    TariffSubscription newTs =
      tariffSubscriptionRepo.getSubscription(customer, newTariff);
    boolean controllable =
      (customer.getPowerType() == PowerType.INTERRUPTIBLE_CONSUMPTION);
    log.debug(this.toString() + " Changing");
    log.debug("Old:" + ts.toString() + "  New:" + newTs.toString());

    if (controllable) {

      if (controllableSubscriptionMap.get("NS") == ts
          || controllableSubscriptionMap.get("NS") == null) {
        controllableSubscriptionMap.put("NS", newTs);
        if (superseded)
          this.superseded.put("NS", true);
      }
      if (controllableSubscriptionMap.get("RaS") == ts
          || controllableSubscriptionMap.get("RaS") == null) {
        controllableSubscriptionMap.put("RaS", newTs);
        if (superseded)
          this.superseded.put("RaS", true);
      }
      if (controllableSubscriptionMap.get("ReS") == ts
          || controllableSubscriptionMap.get("ReS") == null) {
        controllableSubscriptionMap.put("ReS", newTs);
        if (superseded)
          this.superseded.put("ReS", true);
      }
      if (controllableSubscriptionMap.get("SS") == ts
          || controllableSubscriptionMap.get("SS") == null) {
        controllableSubscriptionMap.put("SS", newTs);
        if (superseded)
          this.superseded.put("SS", true);
      }
      log.debug("Controllable Subscription Map: "
                + controllableSubscriptionMap.toString());
    }
    else {

      if (subscriptionMap.get("NS") == ts || subscriptionMap.get("NS") == null) {
        subscriptionMap.put("NS", newTs);
        if (superseded)
          this.superseded.put("NS", true);
      }
      if (subscriptionMap.get("RaS") == ts
          || subscriptionMap.get("RaS") == null) {
        subscriptionMap.put("RaS", newTs);
        if (superseded)
          this.superseded.put("RaS", true);
      }
      if (subscriptionMap.get("ReS") == ts
          || subscriptionMap.get("ReS") == null) {
        subscriptionMap.put("ReS", newTs);
        if (superseded)
          this.superseded.put("ReS", true);
      }
      if (subscriptionMap.get("SS") == ts || subscriptionMap.get("SS") == null) {
        subscriptionMap.put("SS", newTs);
        if (superseded)
          this.superseded.put("SS", true);
      }

      log.debug("Subscription Map: " + subscriptionMap.toString());
    }
  }

  /**
   * This function is overloading the previous one and is used when only certain
   * types of houses changed tariff.
   * 
   */
  private void updateSubscriptions (Tariff tariff, Tariff newTariff,
                                    String type, CustomerInfo customer)
  {

    TariffSubscription ts =
      tariffSubscriptionRepo.getSubscription(customer, tariff);
    TariffSubscription newTs =
      tariffSubscriptionRepo.getSubscription(customer, newTariff);
    boolean controllable =
      (customer.getPowerType() == PowerType.INTERRUPTIBLE_CONSUMPTION);
    log.debug(this.toString() + " Changing Only " + type);
    log.debug("Old:" + ts.toString() + "  New:" + newTs.toString());

    if (controllable) {

      log.debug("For Controllable");

      if (type.equals("NS")) {
        controllableSubscriptionMap.put("NS", newTs);
      }
      else if (type.equals("RaS")) {
        controllableSubscriptionMap.put("RaS", newTs);
      }
      else if (type.equals("ReS")) {
        controllableSubscriptionMap.put("ReS", newTs);
      }
      else {
        controllableSubscriptionMap.put("SS", newTs);
      }

      log.debug("Controllable Subscription Map: "
                + controllableSubscriptionMap.toString());

    }
    else {

      if (type.equals("NS")) {
        subscriptionMap.put("NS", newTs);
      }
      else if (type.equals("RaS")) {
        subscriptionMap.put("RaS", newTs);
      }
      else if (type.equals("ReS")) {
        subscriptionMap.put("ReS", newTs);
      }
      else {
        subscriptionMap.put("SS", newTs);
      }

      log.debug("Subscription Map: " + subscriptionMap.toString());

    }

  }

  @Override
  public void checkRevokedSubscriptions ()
  {

    for (CustomerInfo customer: customerInfos) {
      List<TariffSubscription> revoked =
        tariffSubscriptionRepo.getRevokedSubscriptionList(customer);

      log.debug(revoked.toString());

      for (TariffSubscription revokedSubscription: revoked) {
        Tariff tariff = revokedSubscription.getTariff();
        Tariff newTariff = revokedSubscription.handleRevokedTariff();

        // Tariff newTariff =
        // revokedSubscription.getTariff().getIsSupersededBy();
        Tariff defaultTariff =
          tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION);

        log.debug("Tariff:" + tariff.toString() + " PowerType: "
                  + tariff.getPowerType());
        if (newTariff != null)
          log.debug("New Tariff:" + newTariff.toString());
        else {
          log.debug("New Tariff is Null");
          log.debug("Default Tariff:" + defaultTariff.toString());
        }

        if (newTariff == null)
          updateSubscriptions(tariff, defaultTariff, customer, true);
        else
          updateSubscriptions(tariff, newTariff, customer, true);
      }
    }
  }

  // =====LOAD FUNCTIONS===== //

  /**
   * This function is used in order to fill each week day of the aggregated
   * daily Load of the village households for each quarter of the hour.
   * 
   * @param type
   * @return
   */
  void fillAggWeeklyLoad (String type)
  {

    if (type.equals("NS")) {
      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK
                          * (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadNS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadNS.add(fillAggDailyControllableLoad(i, type));
        aggDailyWeatherSensitiveLoadNS
                .add(fillAggDailyWeatherSensitiveLoad(i, type));

        aggDailyBaseLoadInHoursNS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursNS
                .add(fillAggDailyControllableLoadInHours(i, type));
        aggDailyWeatherSensitiveLoadInHoursNS
                .add(fillAggDailyWeatherSensitiveLoadInHours(i, type));

        aggDailyDominantLoadNS.add(fillAggDailyDominantLoad(i, type));
        aggDailyNonDominantLoadNS.add(fillAggDailyNonDominantLoad(i, type));
        aggDailyDominantLoadInHoursNS
                .add(fillAggDailyDominantLoadInHours(i, type));
        aggDailyNonDominantLoadInHoursNS
                .add(fillAggDailyNonDominantLoadInHours(i, type));
      }
    }
    else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK
                          * (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadRaS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadRaS.add(fillAggDailyControllableLoad(i, type));
        aggDailyWeatherSensitiveLoadRaS
                .add(fillAggDailyWeatherSensitiveLoad(i, type));
        aggDailyBaseLoadInHoursRaS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursRaS
                .add(fillAggDailyControllableLoadInHours(i, type));
        aggDailyWeatherSensitiveLoadInHoursRaS
                .add(fillAggDailyWeatherSensitiveLoadInHours(i, type));

        aggDailyDominantLoadRaS.add(fillAggDailyDominantLoad(i, type));
        aggDailyNonDominantLoadRaS.add(fillAggDailyNonDominantLoad(i, type));
        aggDailyDominantLoadInHoursRaS
                .add(fillAggDailyDominantLoadInHours(i, type));
        aggDailyNonDominantLoadInHoursRaS
                .add(fillAggDailyNonDominantLoadInHours(i, type));
      }
    }
    else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK
                          * (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadReS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadReS.add(fillAggDailyControllableLoad(i, type));
        aggDailyWeatherSensitiveLoadReS
                .add(fillAggDailyWeatherSensitiveLoad(i, type));
        aggDailyBaseLoadInHoursReS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursReS
                .add(fillAggDailyControllableLoadInHours(i, type));
        aggDailyWeatherSensitiveLoadInHoursReS
                .add(fillAggDailyWeatherSensitiveLoadInHours(i, type));

        aggDailyDominantLoadReS.add(fillAggDailyDominantLoad(i, type));
        aggDailyNonDominantLoadReS.add(fillAggDailyNonDominantLoad(i, type));
        aggDailyDominantLoadInHoursReS
                .add(fillAggDailyDominantLoadInHours(i, type));
        aggDailyNonDominantLoadInHoursReS
                .add(fillAggDailyNonDominantLoadInHours(i, type));
      }
    }
    else {
      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK
                          * (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadSS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadSS.add(fillAggDailyControllableLoad(i, type));
        aggDailyWeatherSensitiveLoadSS
                .add(fillAggDailyWeatherSensitiveLoad(i, type));
        aggDailyBaseLoadInHoursSS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursSS
                .add(fillAggDailyControllableLoadInHours(i, type));
        aggDailyWeatherSensitiveLoadInHoursSS
                .add(fillAggDailyWeatherSensitiveLoadInHours(i, type));

        aggDailyDominantLoadSS.add(fillAggDailyDominantLoad(i, type));
        aggDailyNonDominantLoadSS.add(fillAggDailyNonDominantLoad(i, type));
        aggDailyDominantLoadInHoursSS
                .add(fillAggDailyDominantLoadInHours(i, type));
        aggDailyNonDominantLoadInHoursSS
                .add(fillAggDailyNonDominantLoadInHours(i, type));
      }
    }

    fillAggDominantLoads(type);
  }

  private void fillAggDominantLoads (String type)
  {

    double[] dominant = new double[VillageConstants.HOURS_OF_DAY];
    double[] nonDominant = new double[VillageConstants.HOURS_OF_DAY];

    Vector<Household> houses = getHouses(type);

    for (int i = 0; i < houses.size(); i++) {
      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {

        dominant[j] += houses.get(i).getDominantConsumption(j);
        nonDominant[j] += houses.get(i).getNonDominantConsumption(j);

      }
    }

    if (type.equals("NS")) {

      dominantLoadNS = dominant;
      nonDominantLoadNS = nonDominant;

    }
    else if (type.equals("RaS")) {

      dominantLoadRaS = dominant;
      nonDominantLoadRaS = nonDominant;
    }
    else if (type.equals("ReS")) {

      dominantLoadReS = dominant;
      nonDominantLoadReS = nonDominant;

    }
    else {

      dominantLoadSS = dominant;
      nonDominantLoadSS = nonDominant;

    }
  }

  /**
   * This function is used in order to update the daily aggregated Load in case
   * there are changes in the weather sensitive loads of the village's
   * households.
   * 
   * @param type
   * @return
   */
  void updateAggDailyWeatherSensitiveLoad (String type, int day)
  {
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);
    if (type.equals("NS")) {
      aggDailyWeatherSensitiveLoadNS
              .set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
      aggDailyWeatherSensitiveLoadInHoursNS
              .set(dayTemp,
                   fillAggDailyWeatherSensitiveLoadInHours(dayTemp, type));
    }
    else if (type.equals("RaS")) {
      aggDailyWeatherSensitiveLoadRaS
              .set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
      aggDailyWeatherSensitiveLoadInHoursRaS
              .set(dayTemp,
                   fillAggDailyWeatherSensitiveLoadInHours(dayTemp, type));
    }
    else if (type.equals("ReS")) {
      aggDailyWeatherSensitiveLoadReS
              .set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
      aggDailyWeatherSensitiveLoadInHoursReS
              .set(dayTemp,
                   fillAggDailyWeatherSensitiveLoadInHours(dayTemp, type));
    }
    else {
      aggDailyWeatherSensitiveLoadSS
              .set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
      aggDailyWeatherSensitiveLoadInHoursSS
              .set(dayTemp,
                   fillAggDailyWeatherSensitiveLoadInHours(dayTemp, type));
    }
  }

  /**
   * This function is used in order to fill the aggregated daily Base Load of
   * the village's households for each quarter of the hour.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyBaseLoad (int day, String type)
  {

    Vector<Household> houses = new Vector<Household>();

    if (type.equals("NS")) {
      houses = notShiftingHouses;
    }
    else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    }
    else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    }
    else {
      houses = smartShiftingHouses;
    }

    Vector<Long> v = new Vector<Long>(VillageConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Household house: houses) {
        sum = sum + house.weeklyBaseLoad.get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the aggregated daily Controllable
   * Load of the village's households for each quarter of the hour.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyControllableLoad (int day, String type)
  {

    Vector<Household> houses = new Vector<Household>();

    if (type.equals("NS")) {
      houses = notShiftingHouses;
    }
    else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    }
    else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    }
    else {
      houses = smartShiftingHouses;
    }

    Vector<Long> v = new Vector<Long>(VillageConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Household house: houses) {
        sum = sum + house.weeklyControllableLoad.get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the aggregated daily weather
   * sensitive Load of the village's households for each quarter of the hour.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyWeatherSensitiveLoad (int day, String type)
  {

    Vector<Household> houses = new Vector<Household>();

    if (type.equals("NS")) {
      houses = notShiftingHouses;
    }
    else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    }
    else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    }
    else {
      houses = smartShiftingHouses;
    }

    Vector<Long> v = new Vector<Long>(VillageConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Household house: houses) {
        sum = sum + house.weeklyWeatherSensitiveLoad.get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the aggregated daily dominant
   * Load of the village's households for each quarter of the hour.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyDominantLoad (int day, String type)
  {

    Vector<Household> houses = new Vector<Household>();

    if (type.equals("NS")) {
      houses = notShiftingHouses;
    }
    else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    }
    else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    }
    else {
      houses = smartShiftingHouses;
    }

    Vector<Long> v = new Vector<Long>(VillageConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Household house: houses) {
        sum = sum + house.weeklyDominantLoad.get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the aggregated daily non dominant
   * Load of the village's households for each quarter of the hour.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyNonDominantLoad (int day, String type)
  {

    Vector<Household> houses = new Vector<Household>();

    if (type.equals("NS")) {
      houses = notShiftingHouses;
    }
    else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    }
    else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    }
    else {
      houses = smartShiftingHouses;
    }

    Vector<Long> v = new Vector<Long>(VillageConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Household house: houses) {
        sum = sum + house.weeklyNonDominantLoad.get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the daily Base Load of the household
   * for each hour for a certain type of households.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyBaseLoadInHours (int day, String type)
  {

    Vector<Long> daily = new Vector<Long>();
    long sum = 0;

    if (type.equals("NS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyBaseLoadNS.get(day)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyBaseLoadNS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyBaseLoadNS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyBaseLoadNS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyBaseLoadRaS.get(day)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyBaseLoadRaS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyBaseLoadRaS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyBaseLoadRaS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyBaseLoadReS.get(day)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyBaseLoadReS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyBaseLoadReS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyBaseLoadReS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyBaseLoadSS.get(day)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyBaseLoadSS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyBaseLoadSS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyBaseLoadSS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }

    return daily;
  }

  /**
   * This function is used in order to fill the daily Controllable Load of the
   * household for each hour for a certain type of households.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyControllableLoadInHours (int day, String type)
  {

    Vector<Long> daily = new Vector<Long>();
    long sum = 0;

    if (type.equals("NS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyControllableLoadNS.get(day)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyControllableLoadNS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyControllableLoadNS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyControllableLoadNS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyControllableLoadRaS.get(day)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyControllableLoadRaS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyControllableLoadRaS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyControllableLoadRaS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyControllableLoadReS.get(day)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyControllableLoadReS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyControllableLoadReS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyControllableLoadReS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyControllableLoadSS.get(day)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyControllableLoadSS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyControllableLoadSS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyControllableLoadSS.get(day)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }

    return daily;
  }

  /**
   * This function is used in order to fill the daily weather sensitive Load of
   * the household for each hour for a certain type of households.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyWeatherSensitiveLoadInHours (int day, String type)
  {

    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);
    Vector<Long> daily = new Vector<Long>();
    long sum = 0;

    if (type.equals("NS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyWeatherSensitiveLoadNS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyWeatherSensitiveLoadNS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyWeatherSensitiveLoadNS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyWeatherSensitiveLoadNS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyWeatherSensitiveLoadRaS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyWeatherSensitiveLoadRaS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyWeatherSensitiveLoadRaS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyWeatherSensitiveLoadRaS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyWeatherSensitiveLoadReS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyWeatherSensitiveLoadReS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyWeatherSensitiveLoadReS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyWeatherSensitiveLoadReS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyWeatherSensitiveLoadSS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyWeatherSensitiveLoadSS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyWeatherSensitiveLoadSS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyWeatherSensitiveLoadSS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }

    return daily;
  }

  /**
   * This function is used in order to fill the daily dominant Load of
   * the household for each hour for a certain type of households.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyDominantLoadInHours (int day, String type)
  {

    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);
    Vector<Long> daily = new Vector<Long>();
    long sum = 0;

    if (type.equals("NS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyDominantLoadNS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyDominantLoadNS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyDominantLoadNS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyDominantLoadNS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyDominantLoadRaS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyDominantLoadRaS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyDominantLoadRaS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyDominantLoadRaS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyDominantLoadReS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyDominantLoadReS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyDominantLoadReS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyDominantLoadReS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyDominantLoadSS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyDominantLoadSS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyDominantLoadSS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyDominantLoadSS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }

    return daily;
  }

  /**
   * This function is used in order to fill the daily non dominant Load of
   * the household for each hour for a certain type of households.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyNonDominantLoadInHours (int day, String type)
  {

    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);
    Vector<Long> daily = new Vector<Long>();
    long sum = 0;

    if (type.equals("NS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyNonDominantLoadNS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyNonDominantLoadNS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyNonDominantLoadNS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyNonDominantLoadNS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyNonDominantLoadRaS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyNonDominantLoadRaS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyNonDominantLoadRaS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyNonDominantLoadRaS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyNonDominantLoadReS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyNonDominantLoadReS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyNonDominantLoadReS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyNonDominantLoadReS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyNonDominantLoadSS.get(dayTemp)
                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
                  + aggDailyNonDominantLoadSS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyNonDominantLoadSS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyNonDominantLoadSS.get(dayTemp)
                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }

    return daily;
  }

  /**
   * This function is used in order to print the aggregated hourly load of the
   * village's households.
   * 
   * @param type
   * @return
   */
  void showAggLoad (String type)
  {

    log.info("Portion " + type + " Weekly Aggregated Load");

    if (type.equals("NS")) {
      for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION
                          + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.debug("Day " + i);
        for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
          log.debug("Hour : " + j + " Base Load : "
                    + aggDailyBaseLoadInHoursNS.get(i).get(j)
                    + " Controllable Load: "
                    + aggDailyControllableLoadInHoursNS.get(i).get(j)
                    + " Weather Sensitive Load: "
                    + aggDailyWeatherSensitiveLoadInHoursNS.get(i).get(j));
        }
      }
    }
    else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION
                          + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.debug("Day " + i);
        for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
          log.debug("Hour : " + j + " Base Load : "
                    + aggDailyBaseLoadInHoursRaS.get(i).get(j)
                    + " Controllable Load: "
                    + aggDailyControllableLoadInHoursRaS.get(i).get(j)
                    + " Weather Sensitive Load: "
                    + aggDailyWeatherSensitiveLoadInHoursRaS.get(i).get(j));
        }
      }
    }
    else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION
                          + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.debug("Day " + i);
        for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
          log.debug("Hour : " + j + " Base Load : "
                    + aggDailyBaseLoadInHoursReS.get(i).get(j)
                    + " Controllable Load: "
                    + aggDailyControllableLoadInHoursReS.get(i).get(j)
                    + " Weather Sensitive Load: "
                    + aggDailyWeatherSensitiveLoadInHoursReS.get(i).get(j));
        }
      }
    }
    else {
      for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION
                          + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.debug("Day " + i);
        for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
          log.debug("Hour : " + j + " Base Load : "
                    + aggDailyBaseLoadInHoursSS.get(i).get(j)
                    + " Controllable Load: "
                    + aggDailyControllableLoadInHoursSS.get(i).get(j)
                    + " Weather Sensitive Load: "
                    + aggDailyWeatherSensitiveLoadInHoursSS.get(i).get(j));
        }
      }
    }
  }

  /**
   * This function is used in order to print the aggregated hourly load of the
   * village households for a certain type of households.
   * 
   * @param type
   * @return
   */
  public void showAggDailyLoad (String type, int day)
  {

    log.debug("Portion " + type + " Daily Aggregated Load");
    log.debug("Day " + day);

    if (type.equals("NS")) {
      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
        log.debug("Hour : " + j + " Base Load : "
                  + aggDailyBaseLoadInHoursNS.get(day).get(j)
                  + " Controllable Load: "
                  + aggDailyControllableLoadInHoursNS.get(day).get(j)
                  + " Weather Sensitive Load: "
                  + aggDailyWeatherSensitiveLoadInHoursNS.get(day).get(j));
      }

    }
    else if (type.equals("RaS")) {
      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
        log.debug("Hour : " + j + " Base Load : "
                  + aggDailyBaseLoadInHoursRaS.get(day).get(j)
                  + " Controllable Load: "
                  + aggDailyControllableLoadInHoursRaS.get(day).get(j)
                  + " Weather Sensitive Load: "
                  + aggDailyWeatherSensitiveLoadInHoursRaS.get(day).get(j));
      }

    }
    else if (type.equals("ReS")) {
      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
        log.debug("Hour : " + j + " Base Load : "
                  + aggDailyBaseLoadInHoursReS.get(day).get(j)
                  + " Controllable Load: "
                  + aggDailyControllableLoadInHoursReS.get(day).get(j)
                  + " Weather Sensitive Load: "
                  + aggDailyWeatherSensitiveLoadInHoursReS.get(day).get(j));
      }

    }
    else {
      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
        log.debug("Hour : " + j + " Base Load : "
                  + aggDailyBaseLoadInHoursSS.get(day).get(j)
                  + " Controllable Load: "
                  + aggDailyControllableLoadInHoursSS.get(day).get(j)
                  + " Weather Sensitive Load: "
                  + aggDailyWeatherSensitiveLoadInHoursSS.get(day).get(j));
      }
    }
  }

  /**
   * This function is used in order to print the aggregated hourly load of the
   * village households for a certain type of households.
   * 
   * @param type
   * @return
   */
  public void showAggDailyLoad (String type, int day, int hour)
  {

    log.info("Portion " + type + " Hourly Aggregated Load");
    log.info("Day " + day + " Hour " + hour);

    if (type.equals("NS")) {

      log.info("Hour : " + hour + " Base Load : "
               + aggDailyBaseLoadInHoursNS.get(day).get(hour)
               + " Controllable Load: "
               + aggDailyControllableLoadInHoursNS.get(day).get(hour)
               + " Weather Sensitive Load: "
               + aggDailyWeatherSensitiveLoadInHoursNS.get(day).get(hour));

    }
    else if (type.equals("RaS")) {

      log.info("Hour : " + hour + " Base Load : "
               + aggDailyBaseLoadInHoursRaS.get(day).get(hour)
               + " Controllable Load: "
               + aggDailyControllableLoadInHoursRaS.get(day).get(hour)
               + " Weather Sensitive Load: "
               + aggDailyWeatherSensitiveLoadInHoursRaS.get(day).get(hour));

    }
    else if (type.equals("ReS")) {

      log.info("Hour : " + hour + " Base Load : "
               + aggDailyBaseLoadInHoursReS.get(day).get(hour)
               + " Controllable Load: "
               + aggDailyControllableLoadInHoursReS.get(day).get(hour)
               + " Weather Sensitive Load: "
               + aggDailyWeatherSensitiveLoadInHoursReS.get(day).get(hour));

    }
    else {

      log.info("Hour : " + hour + " Base Load : "
               + aggDailyBaseLoadInHoursSS.get(day).get(hour)
               + " Controllable Load: "
               + aggDailyControllableLoadInHoursSS.get(day).get(hour)
               + " Weather Sensitive Load: "
               + aggDailyWeatherSensitiveLoadInHoursSS.get(day).get(hour));

    }
  }

  // =====CONSUMPTION FUNCTIONS===== //

  @Override
  public void consumePower ()
  {
    Timeslot ts = timeslotRepo.currentTimeslot();
    double summary = 0;
    double summaryControllable = 0;

    HashMap<TariffSubscription, Double> subs =
      new HashMap<TariffSubscription, Double>();

    for (TariffSubscription sub: subscriptionMap.values()) {

      subs.put(sub, Double.valueOf(0));

    }

    for (TariffSubscription sub: controllableSubscriptionMap.values()) {

      subs.put(sub, Double.valueOf(0));

    }

    // log.debug(subs.toString());

    for (String type: subscriptionMap.keySet()) {

      if (ts == null) {
        log.debug("Current timeslot is null");
        int serial =
          (int) ((timeService.getCurrentTime().getMillis() - timeService
                  .getBase()) / TimeService.HOUR);
        summary = getConsumptionByTimeslot(serial, type, false);
        summaryControllable = getConsumptionByTimeslot(serial, type, true);
      }
      else {
        log.debug("Timeslot Serial: " + ts.getSerialNumber());
        summary = getConsumptionByTimeslot(ts.getSerialNumber(), type, false);
        summaryControllable =
          getConsumptionByTimeslot(ts.getSerialNumber(), type, true);
      }

      // log.debug("Consumption Load for " + type + ": Base Load " + summary
      // + " Controllable Load " + summaryControllable);

      TariffSubscription tempSub = subscriptionMap.get(type);
      TariffSubscription tempContSub = controllableSubscriptionMap.get(type);

      subs.put(tempSub, summary + subs.get(tempSub));
      subs.put(tempContSub, summaryControllable + subs.get(tempContSub));

    }

    for (TariffSubscription sub: subs.keySet()) {

      log.debug("Consumption Load for Customer " + sub.getCustomer().toString()
                + ": " + subs.get(sub));

      if (sub.getCustomersCommitted() > 0)
        sub.usePower(subs.get(sub));

    }

  }

  /**
   * This method takes as an input the time-slot serial number (in order to know
   * in the current time) and estimates the consumption for this time-slot over
   * the population under the Village Household Consumer.
   */
  double
    getConsumptionByTimeslot (int serial, String type, boolean controllable)
  {

    int day = (int) (serial / VillageConstants.HOURS_OF_DAY);
    int hour = (int) (serial % VillageConstants.HOURS_OF_DAY);
    long summary = 0;

    log.debug("Serial : " + serial + " Day: " + day + " Hour: " + hour);

    if (controllable)
      summary = getControllableConsumptions(day, hour, type);
    else
      summary =
        getBaseConsumptions(day, hour, type)
                + getWeatherSensitiveConsumptions(day, hour, type);

    return (double) summary / VillageConstants.THOUSAND;
  }

  // =====GETTER FUNCTIONS===== //

  /** This function returns the subscription Map variable of the village. */
  public HashMap<String, TariffSubscription> getSubscriptionMap ()
  {
    return subscriptionMap;
  }

  /** This function returns the subscription Map variable of the village. */
  public HashMap<String, TariffSubscription> getControllableSubscriptionMap ()
  {
    return controllableSubscriptionMap;
  }

  /** This function returns the inertia Map variable of the village. */
  public HashMap<String, Double> getInertiaMap ()
  {
    return inertiaMap;
  }

  /** This function returns the period Map variable of the village. */
  public HashMap<String, Integer> getPeriodMap ()
  {
    return periodMap;
  }

  /** This function returns the inertia Map variable of the village. */
  public HashMap<String, Boolean> getSuperseded ()
  {
    return superseded;
  }

  /**
   * This function set the superseded map in t the inertia Map variable of the
   * village.
   */
  public void setSuperseded (String type, boolean value)
  {
    superseded.put(type, value);
  }

  /**
   * This function returns the quantity of base load for a specific day and hour
   * of that day for a specific type of households.
   */
  long getBaseConsumptions (int day, int hour, String type)
  {
    long summaryBase = 0;
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      summaryBase = aggDailyBaseLoadInHoursNS.get(dayTemp).get(hour);
    }
    else if (type.equals("RaS")) {
      summaryBase = aggDailyBaseLoadInHoursRaS.get(dayTemp).get(hour);
    }
    else if (type.equals("ReS")) {
      summaryBase = aggDailyBaseLoadInHoursReS.get(dayTemp).get(hour);
    }
    else {
      summaryBase = aggDailyBaseLoadInHoursSS.get(dayTemp).get(hour);
    }

    log.debug("Base Load for " + type + ":" + summaryBase);
    return summaryBase;
  }

  /**
   * This function returns the quantity of controllable load for a specific day
   * and hour of that day for a specific type of households.
   */
  long getControllableConsumptions (int day, int hour, String type)
  {
    long summaryControllable = 0;
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      summaryControllable =
        aggDailyControllableLoadInHoursNS.get(dayTemp).get(hour);
    }
    else if (type.equals("RaS")) {
      summaryControllable =
        aggDailyControllableLoadInHoursRaS.get(dayTemp).get(hour);
    }
    else if (type.equals("ReS")) {
      summaryControllable =
        aggDailyControllableLoadInHoursReS.get(dayTemp).get(hour);
    }
    else {
      summaryControllable =
        aggDailyControllableLoadInHoursSS.get(dayTemp).get(hour);
    }

    log.debug("Controllable Load for " + type + ":" + summaryControllable);
    return summaryControllable;
  }

  /**
   * This function returns the quantity of weather sensitive load for a specific
   * day and hour of that day for a specific type of household.
   */
  long getNonDominantConsumptions (int day, int hour, String type)
  {
    long summaryNonDominant = 0;
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      summaryNonDominant =
        aggDailyNonDominantLoadInHoursNS.get(dayTemp).get(hour);
    }
    else if (type.equals("RaS")) {
      summaryNonDominant =
        aggDailyNonDominantLoadInHoursRaS.get(dayTemp).get(hour);
    }
    else if (type.equals("ReS")) {
      summaryNonDominant =
        aggDailyNonDominantLoadInHoursReS.get(dayTemp).get(hour);
    }
    else {
      summaryNonDominant =
        aggDailyNonDominantLoadInHoursSS.get(dayTemp).get(hour);
    }

    log.debug("NonDominant Load for " + type + ":" + summaryNonDominant);
    return summaryNonDominant;
  }

  /**
   * This function returns the quantity of non dominant load for a specific
   * day and hour of that day for a specific type of household.
   */
  long getWeatherSensitiveConsumptions (int day, int hour, String type)
  {
    long summaryWeatherSensitive = 0;
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      summaryWeatherSensitive =
        aggDailyWeatherSensitiveLoadInHoursNS.get(dayTemp).get(hour);
    }
    else if (type.equals("RaS")) {
      summaryWeatherSensitive =
        aggDailyWeatherSensitiveLoadInHoursRaS.get(dayTemp).get(hour);
    }
    else if (type.equals("ReS")) {
      summaryWeatherSensitive =
        aggDailyWeatherSensitiveLoadInHoursReS.get(dayTemp).get(hour);
    }
    else {
      summaryWeatherSensitive =
        aggDailyWeatherSensitiveLoadInHoursSS.get(dayTemp).get(hour);
    }

    log.debug("WeatherSensitive Load for " + type + ":"
              + summaryWeatherSensitive);
    return summaryWeatherSensitive;
  }

  /**
   * This function curtails the quantity of controllable load given by the
   * subscription, by reducing current timeslots consumption and adding it to
   * the next timeslot.
   */
  void curtailControllableConsumption (int day, int hour, String type,
                                       long curtail)
  {
    long before = 0, after = 0;
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      before = aggDailyControllableLoadInHoursNS.get(dayTemp).get(hour);
      aggDailyControllableLoadInHoursNS.get(dayTemp)
              .set(hour, before + curtail);
      after = aggDailyControllableLoadInHoursNS.get(dayTemp).get(hour);
    }
    else if (type.equals("RaS")) {
      before = aggDailyControllableLoadInHoursRaS.get(dayTemp).get(hour);
      aggDailyControllableLoadInHoursRaS.get(dayTemp).set(hour,
                                                          before + curtail);
      after = aggDailyControllableLoadInHoursRaS.get(dayTemp).get(hour);
    }
    else if (type.equals("ReS")) {
      before = aggDailyControllableLoadInHoursReS.get(dayTemp).get(hour);
      aggDailyControllableLoadInHoursReS.get(dayTemp).set(hour,
                                                          before + curtail);
      after = aggDailyControllableLoadInHoursReS.get(dayTemp).get(hour);
    }
    else {
      before = aggDailyControllableLoadInHoursSS.get(dayTemp).get(hour);
      aggDailyControllableLoadInHoursSS.get(dayTemp)
              .set(hour, before + curtail);
      after = aggDailyControllableLoadInHoursSS.get(dayTemp).get(hour);
    }

    log.debug("Controllable Load for " + type + ": Before Curtailment "
              + before + " After Curtailment " + after);

  }

  /**
   * This function returns the quantity of controllable load for a specific day
   * in form of a vector for a certain type of households.
   */
  Vector<Long> getControllableConsumptions (int day, String type)
  {

    Vector<Long> controllableVector = new Vector<Long>();
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      controllableVector = aggDailyControllableLoadInHoursNS.get(dayTemp);
    }
    else if (type.equals("RaS")) {
      controllableVector = aggDailyControllableLoadInHoursRaS.get(dayTemp);
    }
    else if (type.equals("ReS")) {
      controllableVector = aggDailyControllableLoadInHoursReS.get(dayTemp);
    }
    else {
      controllableVector = aggDailyControllableLoadInHoursSS.get(dayTemp);
    }

    return controllableVector;
  }

  /**
   * This function returns the quantity of weather sensitive load for a specific
   * day in form of a vector for a certain type of households.
   */
  Vector<Long> getWeatherSensitiveConsumptions (int day, String type)
  {

    Vector<Long> weatherSensitiveVector = new Vector<Long>();
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      weatherSensitiveVector =
        aggDailyWeatherSensitiveLoadInHoursNS.get(dayTemp);
    }
    else if (type.equals("RaS")) {
      weatherSensitiveVector =
        aggDailyWeatherSensitiveLoadInHoursRaS.get(dayTemp);
    }
    else if (type.equals("ReS")) {
      weatherSensitiveVector =
        aggDailyWeatherSensitiveLoadInHoursReS.get(dayTemp);
    }
    else {
      weatherSensitiveVector =
        aggDailyWeatherSensitiveLoadInHoursSS.get(dayTemp);
    }

    return weatherSensitiveVector;
  }

  /**
   * This function returns the quantity of weather sensitive load for a specific
   * day in form of a vector for a certain type of households.
   */
  Vector<Long> getNonDominantConsumptions (int day, String type)
  {

    Vector<Long> nonDominantVector = new Vector<Long>();
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      nonDominantVector = aggDailyNonDominantLoadInHoursNS.get(dayTemp);
    }
    else if (type.equals("RaS")) {
      nonDominantVector = aggDailyNonDominantLoadInHoursRaS.get(dayTemp);
    }
    else if (type.equals("ReS")) {
      nonDominantVector = aggDailyNonDominantLoadInHoursReS.get(dayTemp);
    }
    else {
      nonDominantVector = aggDailyNonDominantLoadInHoursSS.get(dayTemp);
    }

    return nonDominantVector;
  }

  /**
   * This function returns a vector with all the houses that are present in this
   * village.
   */
  public Vector<Household> getHouses ()
  {

    Vector<Household> houses = new Vector<Household>();

    for (Household house: notShiftingHouses)
      houses.add(house);
    for (Household house: regularlyShiftingHouses)
      houses.add(house);
    for (Household house: randomlyShiftingHouses)
      houses.add(house);
    for (Household house: smartShiftingHouses)
      houses.add(house);

    return houses;

  }

  /**
   * This function returns a vector with all the households of a certain type
   * that are present in this village.
   */
  public Vector<Household> getHouses (String type)
  {

    Vector<Household> houses = new Vector<Household>();

    if (type.equals("NS")) {
      for (Household house: notShiftingHouses) {
        houses.add(house);
      }
    }
    else if (type.equals("RaS")) {
      for (Household house: regularlyShiftingHouses) {
        houses.add(house);
      }
    }
    else if (type.equals("ReS")) {
      for (Household house: randomlyShiftingHouses) {
        houses.add(house);
      }
    }
    else {
      for (Household house: smartShiftingHouses) {
        houses.add(house);
      }
    }

    return houses;

  }

  double[] getNonDominantUsage (int day, String type)
  {

    double[] nonDominantUsage = new double[VillageConstants.HOURS_OF_DAY];

    for (int hour = 0; hour < VillageConstants.HOURS_OF_DAY; hour++) {

      if (hour == VillageConstants.HOURS_OF_DAY - 1)
        nonDominantUsage[hour] = getNonDominantConsumptions(day, 0, type);
      else
        nonDominantUsage[hour] =
          getNonDominantConsumptions(day, hour + 1, type);
      log.debug("Non Dominant Usage for hour " + hour + ":"
                + nonDominantUsage[hour]);

    }

    return nonDominantUsage;
  }

  // =====EVALUATION FUNCTIONS===== //

  /**
   * This is the basic evaluation function, taking into consideration the
   * minimum cost without shifting the appliances' load but the tariff chosen is
   * picked up randomly by using a possibility pattern. The better tariffs have
   * more chances to be chosen.
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

      TariffSubscription sub = subscriptionMap.get(type);

      Vector<Double> estimation = new Vector<Double>();
      Double rand = gen.nextDouble();

      // getting the active tariffs for evaluation
      ArrayList<Tariff> evaluationTariffs = new ArrayList<Tariff>(newTariffs);

      log.debug("Estimation size for " + this.toString() + " = "
                + evaluationTariffs.size());

      if (evaluationTariffs.size() > 1) {
        for (Tariff tariff: evaluationTariffs) {
          log.debug("Tariff : " + tariff.toString() + " Tariff Type : "
                    + tariff.getTariffSpecification().getPowerType()
                    + " Broker: " + tariff.getBroker().toString());

          if (tariff.isExpired() == false
              && (tariff.getTariffSpecification().getPowerType() == customer
                      .getPowerType() || (customer.getPowerType() == PowerType.INTERRUPTIBLE_CONSUMPTION && tariff
                      .getTariffSpecification().getPowerType() == PowerType.CONSUMPTION))) {

            boolean same =
              (sub.getTariff().getTariffSpec() == tariff.getTariffSpec());

            System.out.println("Now: "
                               + sub.getTariff().getTariffSpec().toString()
                               + " Evaluated: "
                               + tariff.getTariffSpec().toString() + " Same:"
                               + same);

            estimation.add(-(costEstimation(tariff, type, rand, same) - riskMap
                    .get(type)));
          }
          else
            estimation.add(Double.NEGATIVE_INFINITY);
        }

        int minIndex = logitPossibilityEstimation(estimation, type);

        if (customer.getPowerType() == PowerType.INTERRUPTIBLE_CONSUMPTION)
          sub = controllableSubscriptionMap.get(type);

        log.debug("Equality: " + sub.getTariff().getTariffSpec().toString()
                  + " = "
                  + evaluationTariffs.get(minIndex).getTariffSpec().toString());
        if (!(sub.getTariff().getTariffSpec() == evaluationTariffs
                .get(minIndex).getTariffSpec())) {
          log.debug("Changing From " + sub.toString() + " For PowerType "
                    + customer.getPowerType() + " After Evaluation");
          changeSubscription(sub.getTariff(), evaluationTariffs.get(minIndex),
                             type, customer);
        }
      }
    }
  }

  /**
   * This function estimates the overall cost, taking into consideration the
   * fixed payments as well as the variable that are depending on the tariff
   * rates
   */
  double costEstimation (Tariff tariff, String type, Double rand, boolean same)
  {
    double costVariable = 0;

    /*
     * if it is NotShifting Houses the evaluation is done without shifting
     * devices
     * if it is RandomShifting Houses the evaluation is may be done without
     * shifting devices or maybe shifting will be taken into consideration
     * In any other case shifting will be done.
     */
    if (type.equals("NS")) {
      // System.out.println("Simple Evaluation for " + type);
      log.debug("Simple Evaluation for " + type);
      costVariable = estimateVariableTariffPayment(tariff, type);
    }
    else if (type.equals("RaS")) {

      // System.out.println(rand);
      if (rand < getInertiaMap().get(type)) {
        // System.out.println("Simple Evaluation for " + type);
        log.debug("Simple Evaluation for " + type);
        costVariable = estimateShiftingVariableTariffPayment(tariff, type);
      }
      else {
        // System.out.println("Shifting Evaluation for " + type);
        log.debug("Shifting Evaluation for " + type);
        costVariable = estimateVariableTariffPayment(tariff, type);
      }
    }
    else {
      // System.out.println("Shifting Evaluation for " + type);
      log.debug("Shifting Evaluation for " + type);
      costVariable = estimateShiftingVariableTariffPayment(tariff, type);
    }
    double costFixed = 0.0;
    // costVariable = estimateVariableTariffPayment(tariff, type);
    if (!same)
      costFixed =
        estimateFixedTariffPayments(tariff, type) * getHouses(type).size();

    log.debug("Cost Variable: " + costVariable + " Cost Fixed: " + costFixed);
    return (costVariable + costFixed) / VillageConstants.MILLION;
  }

  /**
   * This function estimates the fixed cost, comprised by fees, bonuses and
   * penalties that are the same no matter how much you consume
   */
  double estimateFixedTariffPayments (Tariff tariff, String type)
  {
    double minDuration =
      (double) (tariff.getMinDuration()) / (double) (TimeService.DAY);
    double ff = minDuration / withdrawalMap.get(type);

    // System.out.println("FF for type " + type + ":" + ff);

    return -tariff.getSignupPayment() - ff * tariff.getEarlyWithdrawPayment();

  }

  /**
   * This function estimates the variable cost, depending only to the load
   * quantity you consume
   */
  double estimateVariableTariffPayment (Tariff tariff, String type)
  {

    double finalCostSummary = 0;

    double dominantCostSummary = 0, nonDominantCostSummary = 0;

    double[] dominantUsage = new double[VillageConstants.HOURS_OF_DAY];
    double[] nonDominantUsage = new double[VillageConstants.HOURS_OF_DAY];

    if (type.equals("NS")) {

      dominantUsage = dominantLoadNS;
      nonDominantUsage = nonDominantLoadNS;

    }
    else if (type.equals("RaS")) {

      dominantUsage = dominantLoadRaS;
      nonDominantUsage = nonDominantLoadRaS;
    }
    else if (type.equals("ReS")) {

      dominantUsage = dominantLoadReS;
      nonDominantUsage = nonDominantLoadReS;

    }
    else {

      dominantUsage = dominantLoadSS;
      nonDominantUsage = nonDominantLoadSS;

    }

    dominantCostSummary = tariffEvalHelper.estimateCost(tariff, dominantUsage);
    nonDominantCostSummary =
      tariffEvalHelper.estimateCost(tariff, nonDominantUsage);

    log.debug("Dominant Cost Summary: " + dominantCostSummary);
    log.debug("Non Dominant Cost Summary: " + nonDominantCostSummary);
    finalCostSummary = dominantCostSummary + nonDominantCostSummary;

    return -finalCostSummary;

  }

  /**
   * This is the new function, used in order to find the most cost efficient
   * tariff over the available ones. It is using Daily shifting in order to put
   * the appliances operation in most suitable hours (less costly) of the day.
   * 
   * @param tariff
   * @return
   */
  double estimateShiftingVariableTariffPayment (Tariff tariff, String type)
  {

    double finalCostSummary = 0;
    double costSummary = 0;

    int serial =
      (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);

    int daylimit = (int) (serial / VillageConstants.HOURS_OF_DAY) + 1;

    for (int day: daysList) {
      if (day < daylimit)
        day = (int) (day + (daylimit / VillageConstants.RANDOM_DAYS_NUMBER));

      double[] nonDominantUsage = getNonDominantUsage(day, type);

      double[] overallUsage =
        dailyShifting(tariff, nonDominantUsage, day, type);

      costSummary = tariffEvalHelper.estimateCost(tariff, overallUsage);
      log.debug("Variable Dominant Cost Summary: " + costSummary);

      finalCostSummary += costSummary;
    }
    log.debug("Variable Cost Summary: " + finalCostSummary);
    return -finalCostSummary / VillageConstants.RANDOM_DAYS_NUMBER;
  }

  /**
   * This is the function that realizes the mathematical possibility formula for
   * the choice of tariff.
   */
  int logitPossibilityEstimation (Vector<Double> estimation, String type)
  {

    double lamda = lamdaMap.get(type);
    double summedEstimations = 0;
    Vector<Integer> randomizer = new Vector<Integer>();
    Vector<Integer> possibilities = new Vector<Integer>();

    for (int i = 0; i < estimation.size(); i++) {
      log.debug("Estimation for " + i + ": " + estimation.get(i));
      summedEstimations +=
        Math.pow(VillageConstants.EPSILON, lamda * estimation.get(i));
      log.debug("Cost variable: " + 50 * estimation.get(i));
      log.debug("Summary of Estimation: " + summedEstimations);
    }

    for (int i = 0; i < estimation.size(); i++) {

      possibilities
              .add((int) (VillageConstants.PERCENTAGE * (Math
                      .pow(VillageConstants.EPSILON, lamda * estimation.get(i)) / summedEstimations)));
      for (int j = 0; j < possibilities.get(i); j++) {
        randomizer.add(i);
      }
    }

    log.debug("Randomizer Vector: " + randomizer);
    log.debug("Possibility Vector: " + possibilities.toString());
    int index = randomizer.get((int) (randomizer.size() * rs1.nextDouble()));
    log.debug("Resulting Index = " + index);
    return index;
  }

  // =====SHIFTING FUNCTIONS===== //

  /**
   * This is the function that takes every household in the village and reads
   * the shifted Controllable Consumption for the needs of the tariff
   * evaluation.
   * 
   * @param tariff
   * @param now
   * @param day
   * @param type
   * @return
   */
  double[] dailyShifting (Tariff tariff, double[] nonDominantUsage, int day,
                          String type)
  {

    double[] newControllableLoad = nonDominantUsage;
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    Vector<Household> houses = new Vector<Household>();

    if (type.equals("NS")) {
      houses = notShiftingHouses;
    }
    else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    }
    else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    }
    else {
      houses = smartShiftingHouses;
    }

    for (Household house: houses) {
      double[] temp =
        house.dailyShifting(tariff, newControllableLoad, tariffEvalHelper,
                            dayTemp, gen);

      log.debug("New Dominant Load for house " + house.toString()
                + " for Tariff " + tariff.toString() + ": "
                + Arrays.toString(temp));

      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++)
        newControllableLoad[j] += temp[j];

    }

    log.debug("New Overall Load of Village " + toString() + " type " + type
              + " for Tariff " + tariff.toString() + ": "
              + Arrays.toString(newControllableLoad));

    return newControllableLoad;
  }

  // =====STATUS FUNCTIONS===== //

  /**
   * This function prints to the screen the daily load of the village's
   * households for the weekday at hand.
   * 
   * @param day
   * @param type
   * @return
   */
  void printDailyLoad (int day, String type)
  {

    Vector<Household> houses = new Vector<Household>();
    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      houses = notShiftingHouses;
    }
    else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    }
    else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    }
    else {
      houses = smartShiftingHouses;
    }

    log.debug("Day " + day);

    for (Household house: houses) {
      house.printDailyLoad(dayTemp);
    }

  }

  // =====VECTOR CREATION===== //

  /**
   * This function is creating a certain number of RandomSeed days that will be
   * public vacation for the people living in the environment.
   * 
   * @param days
   * @param gen
   * @return
   */
  Vector<Integer> createPublicVacationVector (int days)
  {
    // Creating auxiliary variables
    Vector<Integer> v = new Vector<Integer>(days);

    for (int i = 0; i < days; i++) {
      int x =
        gen.nextInt(VillageConstants.DAYS_OF_COMPETITION
                    + VillageConstants.DAYS_OF_BOOTSTRAP);
      ListIterator<Integer> iter = v.listIterator();
      while (iter.hasNext()) {
        int temp = (int) iter.next();
        if (x == temp) {
          x = x + 1;
          iter = v.listIterator();
        }
      }
      v.add(x);
    }
    java.util.Collections.sort(v);
    return v;
  }

  /**
   * This function is creating the list of days for each village that will be
   * utilized for the tariff evaluation.
   * 
   * @param days
   * @param gen
   * @return
   */
  void createCostEstimationDaysList (int days)
  {

    for (int i = 0; i < days; i++) {
      int x =
        gen.nextInt(VillageConstants.DAYS_OF_COMPETITION
                    + VillageConstants.DAYS_OF_BOOTSTRAP);
      ListIterator<Integer> iter = daysList.listIterator();
      while (iter.hasNext()) {
        int temp = (int) iter.next();
        if (x == temp) {
          x = x + 1;
          iter = daysList.listIterator();
        }
      }
      daysList.add(x);
    }
    java.util.Collections.sort(daysList);

  }

  // =====STEP FUNCTIONS===== //

  @Override
  public void step ()
  {

    int serial =
      (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    int day = (int) (serial / VillageConstants.HOURS_OF_DAY);
    int hour = timeService.getHourOfDay();
    Instant now = new Instant(timeService.getCurrentTime().getMillis());

    weatherCheck(day, hour, now);

    checkRevokedSubscriptions();

    checkCurtailment(serial, day, hour);

    consumePower();

    // System.out.println("Timeslot:" + timeslotRepo.currentSerialNumber());
    //
    // for (Household house: getHouses())
    // house.test();

    if (hour == 23) {

      for (String type: subscriptionMap.keySet()) {
        if (!(type.equals("NS"))) {
          log.info("Rescheduling " + type);
          rescheduleNextDay(type);
        }

      }

    }

  }

  /**
   * This function is utilized in order to check the weather at each time tick
   * of the competition clock and reschedule the appliances that are weather
   * sensitive to work.
   */
  void weatherCheck (int day, int hour, Instant now)
  {

    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    WeatherReport wr = null;
    wr = weatherReportRepo.currentWeatherReport();

    if (wr != null) {
      double temperature = wr.getTemperature();
      // log.debug("Temperature: " + temperature);

      Vector<Household> houses = getHouses();

      for (Household house: houses) {
        house.weatherCheck(dayTemp, hour, now, temperature);
      }

      for (String type: subscriptionMap.keySet()) {
        updateAggDailyWeatherSensitiveLoad(type, day);
        if (dayTemp + 1 < VillageConstants.DAYS_OF_COMPETITION) {
          updateAggDailyWeatherSensitiveLoad(type, dayTemp + 1);
        }
        // showAggDailyLoad(type, dayTemp);
        // showAggDailyLoad(type, dayTemp + 1);
      }
    }
  }

  /**
   * This function is utilized in order to check the subscriptions curtailments
   * for each time tick and move the controllable load at the next timeslot.
   */
  void checkCurtailment (int serial, int day, int hour)
  {

    int nextSerial =
      (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR) + 1;
    int nextDay = (int) (nextSerial / VillageConstants.HOURS_OF_DAY);
    int nextHour = (int) (nextSerial % VillageConstants.HOURS_OF_DAY);

    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);
    int nextDayTemp =
      nextDay
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    Collection<TariffSubscription> tempCol =
      controllableSubscriptionMap.values();
    ArrayList<TariffSubscription> subs = new ArrayList<TariffSubscription>();

    for (TariffSubscription sub: tempCol)
      if (!subs.contains(sub))
        subs.add(sub);

    log.debug(this.toString() + " " + subs.toString());

    for (TariffSubscription sub: subs) {

      long curt = (long) sub.getCurtailment() * VillageConstants.THOUSAND;
      log.debug(this.toString() + " Subscription " + sub + " Curtailment "
                + curt);

      if (curt > 0) {

        ArrayList<String> temp = new ArrayList<String>();

        for (String type: controllableSubscriptionMap.keySet())
          if (controllableSubscriptionMap.get(type) == sub)
            temp.add(type);

        for (int i = 0; i < temp.size(); i++) {
          String type = temp.get(i);
          curtailControllableConsumption(dayTemp, hour, type,
                                         -(long) (curt / temp.size()));
          curtailControllableConsumption(nextDayTemp, nextHour, type,
                                         (long) (curt / temp.size()));
        }

      }

    }

    // showAggDailyLoad(type, dayTemp);
    // showAggDailyLoad(type, dayTemp + 1);

  }

  /**
   * This function is utilized in order to reschedule the consumption load for
   * the next day of the competition according to the tariff rates of the
   * subscriptions under contract.
   */
  void rescheduleNextDay (String type)
  {

    int serial =
      (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    int day = (int) (serial / VillageConstants.HOURS_OF_DAY) + 1;

    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    double[] nonDominantUsage = getNonDominantUsage(dayTemp, type);

    Vector<Long> controllableVector = new Vector<Long>();

    TariffSubscription sub = subscriptionMap.get(type);

    log.debug("Old Consumption for day " + day + ": "
              + getControllableConsumptions(dayTemp, type).toString());
    double[] newControllableLoad =
      dailyShifting(sub.getTariff(), nonDominantUsage, dayTemp, type);

    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      String newControllableLoadString =
        Double.toString(newControllableLoad[i]);
      newControllableLoadString = newControllableLoadString.replace(".0", "");
      controllableVector.add(Long.parseLong(newControllableLoadString));
    }

    log.debug("New Consumption for day " + day + ": "
              + controllableVector.toString());

    if (type.equals("RaS")) {
      aggDailyControllableLoadInHoursRaS.set(dayTemp, controllableVector);
    }
    else if (type.equals("ReS")) {
      aggDailyControllableLoadInHoursReS.set(dayTemp, controllableVector);
    }
    else {
      aggDailyControllableLoadInHoursSS.set(dayTemp, controllableVector);
    }

  }

  @Override
  public String toString ()
  {
    return name;
  }

}
