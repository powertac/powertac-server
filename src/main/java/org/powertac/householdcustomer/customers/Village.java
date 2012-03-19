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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.AbstractCustomer;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
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
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadSS = new Vector<Vector<Long>>();

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
  Vector<Vector<Long>> aggDailyControllableLoadInHoursNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursSS = new Vector<Vector<Long>>();

  /**
   * These are the aggregated vectors containing each day's weather sensitive
   * load of all the households in hours.
   **/
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursSS = new Vector<Vector<Long>>();

  /**
   * This is an vector containing the days of the competition that the household
   * model will use in order to check which of the tariffs that are available at
   * any given moment are the optimal for their consumption or production.
   **/
  Vector<Integer> daysList = new Vector<Integer>();

  /**
   * This variable is utilized for the creation of the random numbers and is
   * taken from the service.
   */
  Random gen;

  /**
   * These variables are mapping of the characteristics of the types of houses.
   * The first is used to keep track of their subscription at any given time.
   * The second is the inertia parameter for each type of houses. The third is
   * the period that they are evaluating the available tariffs and choose the
   * best for their type. The forth is setting the lamda variable for the
   * possibility function of the evaluation.
   */
  HashMap<String, TariffSubscription> subscriptionMap = new HashMap<String, TariffSubscription>();
  HashMap<String, Double> inertiaMap = new HashMap<String, Double>();
  HashMap<String, Integer> periodMap = new HashMap<String, Integer>();
  HashMap<String, Double> lamdaMap = new HashMap<String, Double>();

  /**
   * These vectors contain the houses of type in the village. There are 4 types
   * available: 1) Not Shifting Houses: They do not change the tariff
   * subscriptions during the game. 2) Randomly Shifting Houses: They change
   * their tariff subscriptions in a random way. 3) Regularly Shifting Houses:
   * They change their tariff subscriptions during the game in regular time
   * periods. 4) Smart Shifting Houses: They change their tariff subscriptions
   * in a smart way in order to minimize their costs.
   */
  Vector<Household> notShiftingHouses = new Vector<Household>();
  Vector<Household> randomlyShiftingHouses = new Vector<Household>();
  Vector<Household> regularlyShiftingHouses = new Vector<Household>();
  Vector<Household> smartShiftingHouses = new Vector<Household>();

  /** This is the constructor function of the Village customer */
  public Village (CustomerInfo customerInfo)
  {
    super(customerInfo);

    timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    timeService = (TimeService) SpringApplicationContext.getBean("timeService");
    weatherReportRepo = (WeatherReportRepo) SpringApplicationContext.getBean("weatherReportRepo");

    ArrayList<String> typeList = new ArrayList<String>();
    typeList.add("NS");
    typeList.add("RaS");
    typeList.add("ReS");
    typeList.add("SS");

    for (String type : typeList) {
      subscriptionMap.put(type, null);
      inertiaMap.put(type, null);
      periodMap.put(type, null);
      lamdaMap.put(type, null);
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
  public void initialize (Properties conf, Random generator)
  {
    // Initializing variables

    int nshouses = Integer.parseInt(conf.getProperty("NotShiftingCustomers"));
    int rashouses = Integer.parseInt(conf.getProperty("RegularlyShiftingCustomers"));
    int reshouses = Integer.parseInt(conf.getProperty("RandomlyShiftingCustomers"));
    int sshouses = Integer.parseInt(conf.getProperty("SmartShiftingCustomers"));
    int days = Integer.parseInt(conf.getProperty("PublicVacationDuration"));

    gen = generator;

    createCostEstimationDaysList(VillageConstants.RANDOM_DAYS_NUMBER);

    Vector<Integer> publicVacationVector = createPublicVacationVector(days);

    for (int i = 0; i < nshouses; i++) {
      log.info("Initializing " + customerInfo.toString() + " NSHouse " + i);
      Household hh = new Household();
      hh.initialize(customerInfo.toString() + " NSHouse" + i, conf, publicVacationVector, gen);
      notShiftingHouses.add(hh);
      hh.householdOf = this;
    }

    for (int i = 0; i < rashouses; i++) {
      log.info("Initializing " + customerInfo.toString() + " RaSHouse " + i);
      Household hh = new Household();
      hh.initialize(customerInfo.toString() + " RaSHouse" + i, conf, publicVacationVector, gen);
      randomlyShiftingHouses.add(hh);
      hh.householdOf = this;
    }

    for (int i = 0; i < reshouses; i++) {
      log.info("Initializing " + customerInfo.toString() + " ReSHouse " + i);
      Household hh = new Household();
      hh.initialize(customerInfo.toString() + " ReSHouse" + i, conf, publicVacationVector, gen);
      regularlyShiftingHouses.add(hh);
      hh.householdOf = this;
    }

    for (int i = 0; i < sshouses; i++) {
      log.info("Initializing " + customerInfo.toString() + " SSHouse " + i);
      Household hh = new Household();
      hh.initialize(customerInfo.toString() + " SSHouse" + i, conf, publicVacationVector, gen);
      smartShiftingHouses.add(hh);
      hh.householdOf = this;
    }

    for (String type : subscriptionMap.keySet()) {
      fillAggWeeklyLoad(type);
      inertiaMap.put(type, Double.parseDouble(conf.getProperty(type + "Inertia")));
      periodMap.put(type, Integer.parseInt(conf.getProperty(type + "Period")));
      lamdaMap.put(type, Double.parseDouble(conf.getProperty(type + "Lamda")));
    }

    /*
    System.out.println("Subscriptions:" + subscriptionMap.toString());
    System.out.println("Inertia:" + inertiaMap.toString());
    System.out.println("Period:" + periodMap.toString());
    System.out.println("Lamda:" + lamdaMap.toString());

    
    for (String type : subscriptionMap.keySet()) {
      showAggLoad(type);
    }
    */
  }

  // =====SUBSCRIPTION FUNCTIONS===== //

  @Override
  public void subscribeDefault ()
  {
    super.subscribeDefault();

    List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(this.getCustomerInfo());

    if (subscriptions.size() > 0) {
      log.debug(subscriptions.toString());

      for (String type : subscriptionMap.keySet()) {
        subscriptionMap.put(type, subscriptions.get(0));
      }
      log.debug(this.toString() + " Default");
      log.debug(subscriptionMap.toString());
    }
  }

  /**
   * The first implementation of the changing subscription function. Here we
   * just put the tariff we want to change and the whole population is moved to
   * another random tariff.
   * 
   * @param tariff
   */
  public void changeSubscription (Tariff tariff)
  {

    TariffSubscription ts = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff, customerInfo);
    int populationCount = ts.getCustomersCommitted();
    unsubscribe(ts, populationCount);

    Tariff newTariff = selectTariff(tariff.getTariffSpec().getPowerType());
    subscribe(newTariff, populationCount);

    updateSubscriptions(tariff, newTariff);
  }

  /**
   * The second implementation of the changing subscription function only for
   * certain type of the households.
   * 
   * @param tariff
   */
  public void changeSubscription (Tariff tariff, String type)
  {
    TariffSubscription ts = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff, customerInfo);
    int populationCount = getHouses(type).size();
    unsubscribe(ts, populationCount);

    Tariff newTariff = selectTariff(tariff.getTariffSpec().getPowerType());
    subscribe(newTariff, populationCount);

    updateSubscriptions(tariff, newTariff, type);

  }

  /**
   * In this overloaded implementation of the changing subscription function,
   * Here we just put the tariff we want to change and the whole population is
   * moved to another random tariff.
   * 
   * @param tariff
   */
  public void changeSubscription (Tariff tariff, Tariff newTariff)
  {
    TariffSubscription ts = tariffSubscriptionRepo.getSubscription(customerInfo, tariff);
    int populationCount = ts.getCustomersCommitted();
    unsubscribe(ts, populationCount);
    subscribe(newTariff, populationCount);

    updateSubscriptions(tariff, newTariff);
  }

  /**
   * In this overloaded implementation of the changing subscription function
   * only certain type of the households.
   * 
   * @param tariff
   */
  public void changeSubscription (Tariff tariff, Tariff newTariff, String type)
  {
    TariffSubscription ts = tariffSubscriptionRepo.getSubscription(customerInfo, tariff);
    int populationCount = getHouses(type).size();
    unsubscribe(ts, populationCount);
    subscribe(newTariff, populationCount);

    updateSubscriptions(tariff, newTariff, type);
  }

  /**
   * This function is used to update the subscriptionMap variable with the
   * changes made.
   * 
   */
  private void updateSubscriptions (Tariff tariff, Tariff newTariff)
  {

    TariffSubscription ts = tariffSubscriptionRepo.getSubscription(customerInfo, tariff);
    TariffSubscription newTs = tariffSubscriptionRepo.getSubscription(customerInfo, newTariff);

    log.info(this.toString() + " Changing");
    log.info("Old:" + ts.toString() + "  New:" + newTs.toString());

    if (subscriptionMap.get("NS") == ts)
      subscriptionMap.put("NS", newTs);
    if (subscriptionMap.get("RaS") == ts)
      subscriptionMap.put("RaS", newTs);
    if (subscriptionMap.get("ReS") == ts)
      subscriptionMap.put("ReS", newTs);
    if (subscriptionMap.get("SS") == ts)
      subscriptionMap.put("SS", newTs);

    log.info(subscriptionMap.toString());

  }

  /**
   * This function is overloading the previous one and is used when only certain
   * types of houses changed tariff.
   * 
   */
  private void updateSubscriptions (Tariff tariff, Tariff newTariff, String type)
  {

    TariffSubscription ts = tariffSubscriptionRepo.getSubscription(customerInfo, tariff);
    TariffSubscription newTs = tariffSubscriptionRepo.getSubscription(customerInfo, newTariff);

    if (type.equals("NS")) {
      subscriptionMap.put("NS", newTs);
    } else if (type.equals("RaS")) {
      subscriptionMap.put("RaS", newTs);
    } else if (type.equals("ReS")) {
      subscriptionMap.put("ReS", newTs);
    } else {
      subscriptionMap.put("SS", newTs);
    }

    log.debug(this.toString() + " Changing Only " + type);
    log.debug("Old:" + ts.toString() + "  New:" + newTs.toString());
    log.debug(subscriptionMap.toString());

  }

  @Override
  public void checkRevokedSubscriptions ()
  {
    List<TariffSubscription> revoked = tariffSubscriptionRepo.getRevokedSubscriptionList(customerInfo);

    log.debug(revoked.toString());
    for (TariffSubscription revokedSubscription : revoked) {
      revokedSubscription.handleRevokedTariff();

      Tariff tariff = revokedSubscription.getTariff();
      Tariff newTariff = revokedSubscription.getTariff().getIsSupersededBy();
      Tariff defaultTariff = tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION);

      log.debug("Tariff:" + tariff.toString());
      if (newTariff != null)
        log.debug("New Tariff:" + newTariff.toString());
      else
        log.debug("New Tariff is Null");
      log.debug("Default Tariff:" + defaultTariff.toString());

      if (newTariff == null)
        updateSubscriptions(tariff, defaultTariff);
      else
        updateSubscriptions(tariff, newTariff);

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
      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK * (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadNS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadNS.add(fillAggDailyControllableLoad(i, type));
        aggDailyWeatherSensitiveLoadNS.add(fillAggDailyWeatherSensitiveLoad(i, type));
        aggDailyBaseLoadInHoursNS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursNS.add(fillAggDailyControllableLoadInHours(i, type));
        aggDailyWeatherSensitiveLoadInHoursNS.add(fillAggDailyWeatherSensitiveLoadInHours(i, type));
      }
    } else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK * (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadRaS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadRaS.add(fillAggDailyControllableLoad(i, type));
        aggDailyWeatherSensitiveLoadRaS.add(fillAggDailyWeatherSensitiveLoad(i, type));
        aggDailyBaseLoadInHoursRaS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursRaS.add(fillAggDailyControllableLoadInHours(i, type));
        aggDailyWeatherSensitiveLoadInHoursRaS.add(fillAggDailyWeatherSensitiveLoadInHours(i, type));
      }
    } else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK * (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadReS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadReS.add(fillAggDailyControllableLoad(i, type));
        aggDailyWeatherSensitiveLoadReS.add(fillAggDailyWeatherSensitiveLoad(i, type));
        aggDailyBaseLoadInHoursReS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursReS.add(fillAggDailyControllableLoadInHours(i, type));
        aggDailyWeatherSensitiveLoadInHoursReS.add(fillAggDailyWeatherSensitiveLoadInHours(i, type));
      }
    } else {
      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK * (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadSS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadSS.add(fillAggDailyControllableLoad(i, type));
        aggDailyWeatherSensitiveLoadSS.add(fillAggDailyWeatherSensitiveLoad(i, type));
        aggDailyBaseLoadInHoursSS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursSS.add(fillAggDailyControllableLoadInHours(i, type));
        aggDailyWeatherSensitiveLoadInHoursSS.add(fillAggDailyWeatherSensitiveLoadInHours(i, type));
      }
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
    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);
    if (type.equals("NS")) {
      aggDailyWeatherSensitiveLoadNS.set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
      aggDailyWeatherSensitiveLoadInHoursNS.set(dayTemp, fillAggDailyWeatherSensitiveLoadInHours(dayTemp, type));
    } else if (type.equals("RaS")) {
      aggDailyWeatherSensitiveLoadRaS.set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
      aggDailyWeatherSensitiveLoadInHoursRaS.set(dayTemp, fillAggDailyWeatherSensitiveLoadInHours(dayTemp, type));
    } else if (type.equals("ReS")) {
      aggDailyWeatherSensitiveLoadReS.set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
      aggDailyWeatherSensitiveLoadInHoursReS.set(dayTemp, fillAggDailyWeatherSensitiveLoadInHours(dayTemp, type));
    } else {
      aggDailyWeatherSensitiveLoadSS.set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
      aggDailyWeatherSensitiveLoadInHoursSS.set(dayTemp, fillAggDailyWeatherSensitiveLoadInHours(dayTemp, type));
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
    } else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    } else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    } else {
      houses = smartShiftingHouses;
    }

    Vector<Long> v = new Vector<Long>(VillageConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Household house : houses) {
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
    } else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    } else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    } else {
      houses = smartShiftingHouses;
    }

    Vector<Long> v = new Vector<Long>(VillageConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Household house : houses) {
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
    } else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    } else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    } else {
      houses = smartShiftingHouses;
    }

    Vector<Long> v = new Vector<Long>(VillageConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Household house : houses) {
        sum = sum + house.weeklyWeatherSensitiveLoad.get(day).get(i);
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
        sum = aggDailyBaseLoadNS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyBaseLoadNS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyBaseLoadNS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyBaseLoadNS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyBaseLoadRaS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyBaseLoadRaS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyBaseLoadRaS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyBaseLoadRaS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyBaseLoadReS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyBaseLoadReS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyBaseLoadReS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyBaseLoadReS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyBaseLoadSS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyBaseLoadSS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyBaseLoadSS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyBaseLoadSS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
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
        sum = aggDailyControllableLoadNS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyControllableLoadNS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyControllableLoadNS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyControllableLoadNS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyControllableLoadRaS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyControllableLoadRaS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyControllableLoadRaS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyControllableLoadRaS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyControllableLoadReS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyControllableLoadReS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyControllableLoadReS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyControllableLoadReS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyControllableLoadSS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyControllableLoadSS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyControllableLoadSS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyControllableLoadSS.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
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

    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);
    Vector<Long> daily = new Vector<Long>();
    long sum = 0;

    if (type.equals("NS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyWeatherSensitiveLoadNS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyWeatherSensitiveLoadNS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyWeatherSensitiveLoadNS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyWeatherSensitiveLoadNS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyWeatherSensitiveLoadRaS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyWeatherSensitiveLoadRaS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyWeatherSensitiveLoadRaS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyWeatherSensitiveLoadRaS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyWeatherSensitiveLoadReS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyWeatherSensitiveLoadReS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyWeatherSensitiveLoadReS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyWeatherSensitiveLoadReS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else {
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyWeatherSensitiveLoadSS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR) + aggDailyWeatherSensitiveLoadSS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyWeatherSensitiveLoadSS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) + aggDailyWeatherSensitiveLoadSS.get(dayTemp).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
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
      for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.debug("Day " + i);
        for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
          log.debug("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursNS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursNS.get(i).get(j) + " Weather Sensitive Load: "
              + aggDailyWeatherSensitiveLoadInHoursNS.get(i).get(j));
        }
      }
    } else if (type.equals("RaS")) {
      for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.debug("Day " + i);
        for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
          log.debug("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursRaS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursRaS.get(i).get(j)
              + " Weather Sensitive Load: " + aggDailyWeatherSensitiveLoadInHoursRaS.get(i).get(j));
        }
      }
    } else if (type.equals("ReS")) {
      for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.debug("Day " + i);
        for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
          log.debug("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursReS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursReS.get(i).get(j)
              + " Weather Sensitive Load: " + aggDailyWeatherSensitiveLoadInHoursReS.get(i).get(j));
        }
      }
    } else {
      for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.debug("Day " + i);
        for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
          log.debug("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursSS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursSS.get(i).get(j) + " Weather Sensitive Load: "
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
        log.debug("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursNS.get(day).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursNS.get(day).get(j)
            + " Weather Sensitive Load: " + aggDailyWeatherSensitiveLoadInHoursNS.get(day).get(j));
      }

    } else if (type.equals("RaS")) {
      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
        log.debug("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursRaS.get(day).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursRaS.get(day).get(j)
            + " Weather Sensitive Load: " + aggDailyWeatherSensitiveLoadInHoursRaS.get(day).get(j));
      }

    } else if (type.equals("ReS")) {
      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
        log.debug("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursReS.get(day).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursReS.get(day).get(j)
            + " Weather Sensitive Load: " + aggDailyWeatherSensitiveLoadInHoursReS.get(day).get(j));
      }

    } else {
      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
        log.debug("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursSS.get(day).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursSS.get(day).get(j)
            + " Weather Sensitive Load: " + aggDailyWeatherSensitiveLoadInHoursSS.get(day).get(j));
      }
    }
  }

  // =====CONSUMPTION FUNCTIONS===== //

  @Override
  public void consumePower ()
  {
    Timeslot ts = timeslotRepo.currentTimeslot();
    double summary = 0;

    for (String type : subscriptionMap.keySet()) {
      TariffSubscription sub = subscriptionMap.get(type);
      if (ts == null) {
        log.debug("Current timeslot is null");
        int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
        summary = getConsumptionByTimeslot(serial, type);
      } else {
        summary = getConsumptionByTimeslot(ts.getSerialNumber(), type);
      }
      log.info("Consumption Load for " + type + ": " + summary);
      sub.usePower(summary);
    }

  }

  /**
   * This method takes as an input the time-slot serial number (in order to know
   * in the current time) and estimates the consumption for this time-slot over
   * the population under the Village Household Consumer.
   */
  double getConsumptionByTimeslot (int serial, String type)
  {

    int day = (int) (serial / VillageConstants.HOURS_OF_DAY);
    int hour = (int) (serial % VillageConstants.HOURS_OF_DAY);
    long summary = 0;

    log.debug("Serial : " + serial + " Day: " + day + " Hour: " + hour);

    summary = (getBaseConsumptions(day, hour, type) + getControllableConsumptions(day, hour, type) + getWeatherSensitiveConsumptions(day, hour, type));

    return (double) summary / VillageConstants.THOUSAND;
  }

  // =====GETTER FUNCTIONS===== //

  /** This function returns the subscription Map variable of the village. */
  public HashMap<String, TariffSubscription> getSubscriptionMap ()
  {
    return subscriptionMap;
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

  /**
   * This function returns the quantity of base load for a specific day and hour
   * of that day for a specific type of households.
   */
  long getBaseConsumptions (int day, int hour, String type)
  {
    long summaryBase = 0;
    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      summaryBase = aggDailyBaseLoadInHoursNS.get(dayTemp).get(hour);
    } else if (type.equals("RaS")) {
      summaryBase = aggDailyBaseLoadInHoursRaS.get(dayTemp).get(hour);
    } else if (type.equals("ReS")) {
      summaryBase = aggDailyBaseLoadInHoursReS.get(dayTemp).get(hour);
    } else {
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
    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      summaryControllable = aggDailyControllableLoadInHoursNS.get(dayTemp).get(hour);
    } else if (type.equals("RaS")) {
      summaryControllable = aggDailyControllableLoadInHoursRaS.get(dayTemp).get(hour);
    } else if (type.equals("ReS")) {
      summaryControllable = aggDailyControllableLoadInHoursReS.get(dayTemp).get(hour);
    } else {
      summaryControllable = aggDailyControllableLoadInHoursSS.get(dayTemp).get(hour);
    }

    log.debug("Controllable Load for " + type + ":" + summaryControllable);
    return summaryControllable;
  }

  /**
   * This function returns the quantity of weather sensitive load for a specific
   * day and hour of that day for a specific type of household.
   */
  long getWeatherSensitiveConsumptions (int day, int hour, String type)
  {
    long summaryWeatherSensitive = 0;
    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      summaryWeatherSensitive = aggDailyWeatherSensitiveLoadInHoursNS.get(dayTemp).get(hour);
    } else if (type.equals("RaS")) {
      summaryWeatherSensitive = aggDailyWeatherSensitiveLoadInHoursRaS.get(dayTemp).get(hour);
    } else if (type.equals("ReS")) {
      summaryWeatherSensitive = aggDailyWeatherSensitiveLoadInHoursReS.get(dayTemp).get(hour);
    } else {
      summaryWeatherSensitive = aggDailyWeatherSensitiveLoadInHoursSS.get(dayTemp).get(hour);
    }

    log.debug("WeatherSensitive Load for " + type + ":" + summaryWeatherSensitive);
    return summaryWeatherSensitive;
  }

  /**
   * This function returns the quantity of controllable load for a specific day
   * in form of a vector for a certain type of households.
   */
  Vector<Long> getControllableConsumptions (int day, String type)
  {

    Vector<Long> controllableVector = new Vector<Long>();
    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      controllableVector = aggDailyControllableLoadInHoursNS.get(dayTemp);
    } else if (type.equals("RaS")) {
      controllableVector = aggDailyControllableLoadInHoursRaS.get(dayTemp);
    } else if (type.equals("ReS")) {
      controllableVector = aggDailyControllableLoadInHoursReS.get(dayTemp);
    } else {
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
    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      weatherSensitiveVector = aggDailyWeatherSensitiveLoadInHoursNS.get(dayTemp);
    } else if (type.equals("RaS")) {
      weatherSensitiveVector = aggDailyWeatherSensitiveLoadInHoursRaS.get(dayTemp);
    } else if (type.equals("ReS")) {
      weatherSensitiveVector = aggDailyWeatherSensitiveLoadInHoursReS.get(dayTemp);
    } else {
      weatherSensitiveVector = aggDailyWeatherSensitiveLoadInHoursSS.get(dayTemp);
    }

    return weatherSensitiveVector;
  }

  /**
   * This function returns a vector with all the houses that are present in this
   * village.
   */
  public Vector<Household> getHouses ()
  {

    Vector<Household> houses = new Vector<Household>();

    for (Household house : notShiftingHouses)
      houses.add(house);
    for (Household house : regularlyShiftingHouses)
      houses.add(house);
    for (Household house : randomlyShiftingHouses)
      houses.add(house);
    for (Household house : smartShiftingHouses)
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
      for (Household house : notShiftingHouses) {
        houses.add(house);
      }
    } else if (type.equals("RaS")) {
      for (Household house : regularlyShiftingHouses) {
        houses.add(house);
      }
    } else if (type.equals("ReS")) {
      for (Household house : randomlyShiftingHouses) {
        houses.add(house);
      }
    } else {
      for (Household house : smartShiftingHouses) {
        houses.add(house);
      }
    }

    return houses;

  }

  // =====EVALUATION FUNCTIONS===== //

  /**
   * This is the basic evaluation function, taking into consideration the
   * minimum cost without shifting the appliances' load but the tariff chosen is
   * picked up randomly by using a possibility pattern. The better tariffs have
   * more chances to be chosen.
   */
  public void possibilityEvaluationNewTariffs (List<Tariff> newTariffs, String type)
  {

    List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(this.getCustomerInfo());

    if (subscriptions == null || subscriptions.size() == 0) {
      subscribeDefault();
      return;
    }

    Vector<Double> estimation = new Vector<Double>();

    // getting the active tariffs for evaluation
    ArrayList<Tariff> evaluationTariffs = new ArrayList<Tariff>(newTariffs);

    log.debug("Estimation size for " + this.toString() + " = " + evaluationTariffs.size());

    if (evaluationTariffs.size() > 1) {
      for (Tariff tariff : evaluationTariffs) {
        log.debug("Tariff : " + tariff.toString() + " Tariff Type : " + tariff.getTariffSpecification().getPowerType());
        if (tariff.isExpired() == false && tariff.getTariffSpecification().getPowerType() == PowerType.CONSUMPTION) {
          estimation.add(-(costEstimation(tariff, type)));
        } else
          estimation.add(Double.NEGATIVE_INFINITY);
      }

      int minIndex = logitPossibilityEstimation(estimation, type);

      TariffSubscription sub = subscriptionMap.get(type);

      log.debug("Equality: " + sub.getTariff().getTariffSpec().toString() + " = " + evaluationTariffs.get(minIndex).getTariffSpec().toString());
      if (!(sub.getTariff().getTariffSpec() == evaluationTariffs.get(minIndex).getTariffSpec())) {
        log.debug("Changing From " + sub.toString() + " After Evaluation");
        changeSubscription(sub.getTariff(), evaluationTariffs.get(minIndex), type);
      }

    }
  }

  /**
   * This function estimates the overall cost, taking into consideration the
   * fixed payments as well as the variable that are depending on the tariff
   * rates
   */
  double costEstimation (Tariff tariff, String type)
  {
    double costVariable = 0;

    /* if it is NotShifting Houses the evaluation is done without shifting devices 
       if it is RandomShifting Houses the evaluation is may be done without shifting devices or maybe shifting will be taken into consideration
       In any other case shifting will be done. */
    if (type.equals("NS")) {
      // System.out.println("Simple Evaluation for " + type);
      log.debug("Simple Evaluation for " + type);
      costVariable = estimateVariableTariffPayment(tariff, type);
    } else if (type.equals("RaS")) {
      Double rand = gen.nextDouble();
      // System.out.println(rand);
      if (rand < getInertiaMap().get(type)) {
        // System.out.println("Simple Evaluation for " + type);
        log.debug("Simple Evaluation for " + type);
        costVariable = estimateShiftingVariableTariffPayment(tariff, type);
      } else {
        // System.out.println("Shifting Evaluation for " + type);
        log.debug("Shifting Evaluation for " + type);
        costVariable = estimateVariableTariffPayment(tariff, type);
      }
    } else {
      // System.out.println("Shifting Evaluation for " + type);
      log.debug("Shifting Evaluation for " + type);
      costVariable = estimateShiftingVariableTariffPayment(tariff, type);
    }

    double costFixed = estimateFixedTariffPayments(tariff);
    return (costVariable + costFixed) / VillageConstants.MILLION;
  }

  /**
   * This function estimates the fixed cost, comprised by fees, bonuses and
   * penalties that are the same no matter how much you consume
   */
  double estimateFixedTariffPayments (Tariff tariff)
  {
    double lifecyclePayment = -tariff.getEarlyWithdrawPayment() - tariff.getSignupPayment();
    double minDuration;

    // When there is not a Minimum Duration of the contract, you cannot divide
    // with the duration
    // because you don't know it.
    if (tariff.getMinDuration() == 0)
      minDuration = VillageConstants.MEAN_TARIFF_DURATION * TimeService.DAY;
    else
      minDuration = tariff.getMinDuration();

    log.debug("Minimum Duration: " + minDuration);
    return (-tariff.getPeriodicPayment() + (lifecyclePayment / minDuration));
  }

  /**
   * This function estimates the variable cost, depending only to the load
   * quantity you consume
   */
  double estimateVariableTariffPayment (Tariff tariff, String type)
  {

    double finalCostSummary = 0;

    int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    Instant base = new Instant(timeService.getCurrentTime().getMillis() - serial * TimeService.HOUR);
    int daylimit = (int) (serial / VillageConstants.HOURS_OF_DAY) + 1;

    for (int day : daysList) {
      if (day < daylimit)
        day = (int) (day + (daylimit / VillageConstants.RANDOM_DAYS_NUMBER));
      Instant now = base.plus(day * TimeService.DAY);
      double costSummary = 0;
      double summary = 0, cumulativeSummary = 0;

      for (int hour = 0; hour < VillageConstants.HOURS_OF_DAY; hour++) {

        summary = getBaseConsumptions(day, hour, type) + getControllableConsumptions(day, hour, type);

        log.debug("Cost for hour " + hour + ":" + tariff.getUsageCharge(now, 1, 0));
        cumulativeSummary += summary;
        costSummary -= tariff.getUsageCharge(now, summary, cumulativeSummary);
        now = now.plus(TimeService.HOUR);
      }
      log.debug("Variable Cost Summary: " + finalCostSummary);
      finalCostSummary += costSummary;
    }
    return finalCostSummary / VillageConstants.RANDOM_DAYS_NUMBER;
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

    int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    Instant base = timeService.getCurrentTime().minus(serial * TimeService.HOUR);
    int daylimit = (int) (serial / VillageConstants.HOURS_OF_DAY) + 1;

    for (int day : daysList) {
      if (day < daylimit)
        day = (int) (day + (daylimit / VillageConstants.RANDOM_DAYS_NUMBER));
      Instant now = base.plus(day * TimeService.DAY);
      double costSummary = 0;
      double summary = 0, cumulativeSummary = 0;

      long[] newControllableLoad = dailyShifting(tariff, now, day, type);

      for (int hour = 0; hour < VillageConstants.HOURS_OF_DAY; hour++) {
        summary = getBaseConsumptions(day, hour, type) + newControllableLoad[hour];
        cumulativeSummary += summary;
        costSummary -= tariff.getUsageCharge(now, summary, cumulativeSummary);
        now = now.plus(TimeService.HOUR);
      }
      log.debug("Variable Cost Summary: " + finalCostSummary);
      finalCostSummary += costSummary;
    }
    return finalCostSummary / VillageConstants.RANDOM_DAYS_NUMBER;
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
      summedEstimations += Math.pow(VillageConstants.EPSILON, lamda * estimation.get(i));
      log.debug("Cost variable: " + estimation.get(i));
      log.debug("Summary of Estimation: " + summedEstimations);
    }

    for (int i = 0; i < estimation.size(); i++) {
      possibilities.add((int) (VillageConstants.PERCENTAGE * (Math.pow(VillageConstants.EPSILON, lamda * estimation.get(i)) / summedEstimations)));
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
  long[] dailyShifting (Tariff tariff, Instant now, int day, String type)
  {

    long[] newControllableLoad = new long[VillageConstants.HOURS_OF_DAY];
    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    Vector<Household> houses = new Vector<Household>();

    if (type.equals("NS")) {
      houses = notShiftingHouses;
    } else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    } else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    } else {
      houses = smartShiftingHouses;
    }

    for (Household house : houses) {
      long[] temp = house.dailyShifting(tariff, now, dayTemp, gen);
      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++)
        newControllableLoad[j] += temp[j];
    }

    log.debug("New Controllable Load of Village " + toString() + " type " + type + " for Tariff " + tariff.toString());

    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      log.debug("Hour: " + i + " Cost: " + tariff.getUsageCharge(now, 1, 0) + " Load For Type " + type + " : " + newControllableLoad[i]);
      now = new Instant(now.getMillis() + TimeService.HOUR);
    }
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
    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      houses = notShiftingHouses;
    } else if (type.equals("RaS")) {
      houses = randomlyShiftingHouses;
    } else if (type.equals("ReS")) {
      houses = regularlyShiftingHouses;
    } else {
      houses = smartShiftingHouses;
    }

    log.debug("Day " + day);

    for (Household house : houses) {
      house.printDailyLoad(dayTemp);
    }

  }

  // =====VECTOR CREATION===== //

  /**
   * This function is creating a certain number of random days that will be
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
      int x = gen.nextInt(VillageConstants.DAYS_OF_COMPETITION + VillageConstants.DAYS_OF_BOOTSTRAP);
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
      int x = gen.nextInt(VillageConstants.DAYS_OF_COMPETITION + VillageConstants.DAYS_OF_BOOTSTRAP);
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
    int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    int day = (int) (serial / VillageConstants.HOURS_OF_DAY);
    int hour = timeService.getHourOfDay();
    Instant now = new Instant(timeService.getCurrentTime().getMillis());

    weatherCheck(day, hour, now);

    checkRevokedSubscriptions();
    consumePower();

    if (hour == 23) {

      for (String type : subscriptionMap.keySet()) {
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

    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);
    WeatherReport wr = weatherReportRepo.currentWeatherReport();
    if (wr != null) {
      double temperature = wr.getTemperature();
      // log.debug("Temperature: " + temperature);

      Vector<Household> houses = getHouses();

      for (Household house : houses) {
        house.weatherCheck(dayTemp, hour, now, temperature);
      }

      for (String type : subscriptionMap.keySet()) {
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
   * This function is utilized in order to reschedule the consumption load for
   * the next day of the competition according to the tariff rates of the
   * subscriptions under contract.
   */
  void rescheduleNextDay (String type)
  {
    int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    int day = (int) (serial / VillageConstants.HOURS_OF_DAY) + 1;
    Instant now = new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR);

    int dayTemp = day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    Vector<Long> controllableVector = new Vector<Long>();

    TariffSubscription sub = subscriptionMap.get(type);

    log.debug("Old Consumption for day " + day + ": " + getControllableConsumptions(dayTemp, type).toString());
    long[] newControllableLoad = dailyShifting(sub.getTariff(), now, dayTemp, type);
    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++)
      controllableVector.add(newControllableLoad[i]);
    log.debug("New Consumption for day " + day + ": " + controllableVector.toString());

    if (type.equals("RaS")) {
      aggDailyBaseLoadInHoursRaS.set(dayTemp, controllableVector);
    } else if (type.equals("ReS")) {
      aggDailyBaseLoadInHoursReS.set(dayTemp, controllableVector);
    } else {
      aggDailyBaseLoadInHoursSS.set(dayTemp, controllableVector);
    }

  }

  @Override
  public String toString ()
  {
    return customerInfo.toString();
  }

}
