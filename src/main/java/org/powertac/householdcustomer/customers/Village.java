/*
 * Copyright 2009-2011 the original author or authors.
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
import org.powertac.common.configurations.HouseholdConstants;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The village domain class in this first version is a set of households that comprise a small village that consumes agreggated energy by the appliances installed in each household.Later on other
 * types of building will be added.
 * 
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class Village extends AbstractCustomer
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error() appropriately. Use log.debug() for output you want to see in testing or debugging.
   */
  static protected Logger log = Logger.getLogger(Village.class.getName());

  @Autowired
  TimeService timeService;

  @Autowired
  TimeslotRepo timeslotRepo;

  /**
   * These are the vectors containing aggregated each day's base load from the appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyBaseLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's controllable load from the appliances installed inside the households.
   **/
  Vector<Vector<Long>> aggDailyControllableLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the agreggated vectors containing each day's base load of all the households in hours.
   **/
  Vector<Vector<Long>> aggDailyBaseLoadInHoursNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursSS = new Vector<Vector<Long>>();

  /**
   * These are the agreggated vectors containing each day's controllable load of all the households in hours.
   **/
  Vector<Vector<Long>> aggDailyControllableLoadInHoursNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursSS = new Vector<Vector<Long>>();

  /**
   * This is an vector containing the days of the competition that the household model will use in order to check which of the tariffs that are available at any given moment are the optimal for their
   * consumption or production.
   **/
  Vector<Integer> daysList = new Vector<Integer>();

  /**
   * This variable is utilized for the creation of the random numbers and is taken from the service.
   */
  Random gen;

  /**
   * These variables are mapping of the characteristics of the types of houses. The first is used to keep track of their subscription at any given time. The second is the inertia parameter for each
   * type of houses. The third is the period that they are evaluating the available tariffs and choose the best for their type. The forth is setting the lamda variable for the possibility function of
   * the evaluation.
   */
  HashMap<String, TariffSubscription> subscriptionMap = new HashMap<String, TariffSubscription>();
  HashMap<String, Double> inertiaMap = new HashMap<String, Double>();
  HashMap<String, Integer> periodMap = new HashMap<String, Integer>();
  HashMap<String, Double> lamdaMap = new HashMap<String, Double>();

  /**
   * These vectors contain the houses of type in the village. There are 4 types available: 1) Not Shifting Houses: They do not change the tariff subscriptions during the game. 2) Randomly Shifting
   * Houses: They change their tariff subscriptions in a random way. 3) Regularly Shifting Houses: They change their tariff subscriptions during the game in regular time periods. 4) Smart Shifting
   * Houses: They change their tariff subscriptions in a smart way in order to minimize their costs.
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
   * This is the initialization function. It uses the variable values for the configuration file to create the village with its households and then fill them with persons and appliances.
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

    createCostEstimationDaysList(HouseholdConstants.RANDOM_DAYS_NUMBER);

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

  // =============================SUBSCRIPTION FUNCTIONS================================================= //

  @Override
  public void subscribeDefault ()
  {
    super.subscribeDefault();

    List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(this.getCustomerInfo());

    if (subscriptions.size() > 0) {
      // log.info(subscriptions.toString());

      for (String type : subscriptionMap.keySet()) {
        subscriptionMap.put(type, subscriptions.get(0));
      }
      // log.info(this.toString() + " Default");
      // log.info(subscriptionMap.toString());
    }
  }

  /**
   * The first implementation of the changing subscription function. Here we just put the tariff we want to change and the whole population is moved to another random tariff.
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
   * The first implementation of the changing subscription function only for a portion of the households.
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
   * In this overloaded implementation of the changing subscription function, Here we just put the tariff we want to change and the whole population is moved to another random tariff.
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
   * In this overloaded implementation of the changing subscription function only for a portion of the households.
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
   * This function is used to update the subscriptionMap variable with the changes made.
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
   * This function is overloading the previous one and is used when only certain portion of houses changed tariff.
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

    log.info(this.toString() + " Changing Only " + type);
    log.info("Old:" + ts.toString() + "  New:" + newTs.toString());
    log.info(subscriptionMap.toString());

  }

  @Override
  public void checkRevokedSubscriptions ()
  {
    List<TariffSubscription> revoked = tariffSubscriptionRepo.getRevokedSubscriptionList(customerInfo);

    // log.info(revoked.toString());
    for (TariffSubscription revokedSubscription : revoked) {
      revokedSubscription.handleRevokedTariff();

      Tariff tariff = revokedSubscription.getTariff();
      Tariff newTariff = revokedSubscription.getTariff().getIsSupersededBy();
      Tariff defaultTariff = tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION);
      /*   
            log.info("Tariff:" + tariff.toString());
            if (newTariff != null)
              log.info("New Tariff:" + newTariff.toString());
            else
              log.info("New Tariff is Null");
            log.info("Default Tariff:" + defaultTariff.toString());
      */
      if (newTariff == null)
        updateSubscriptions(tariff, defaultTariff);
      else
        updateSubscriptions(tariff, newTariff);

    }
  }

  // =============================LOAD FUNCTIONS=================================================

  /**
   * This function is used in order to fill each week day of the aggregated daily Load of the village households for each quarter of the hour.
   * 
   * @param type
   * @return
   */
  void fillAggWeeklyLoad (String type)
  {

    if (type.equals("NS")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK * (HouseholdConstants.WEEKS_OF_COMPETITION + HouseholdConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadNS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadNS.add(fillAggDailyControllableLoad(i, type));
        aggDailyBaseLoadInHoursNS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursNS.add(fillAggDailyControllableLoadInHours(i, type));
      }
    } else if (type.equals("RaS")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK * (HouseholdConstants.WEEKS_OF_COMPETITION + HouseholdConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadRaS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadRaS.add(fillAggDailyControllableLoad(i, type));
        aggDailyBaseLoadInHoursRaS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursRaS.add(fillAggDailyControllableLoadInHours(i, type));
      }
    } else if (type.equals("ReS")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK * (HouseholdConstants.WEEKS_OF_COMPETITION + HouseholdConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadReS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadReS.add(fillAggDailyControllableLoad(i, type));
        aggDailyBaseLoadInHoursReS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursReS.add(fillAggDailyControllableLoadInHours(i, type));
      }
    } else {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK * (HouseholdConstants.WEEKS_OF_COMPETITION + HouseholdConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadSS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadSS.add(fillAggDailyControllableLoad(i, type));
        aggDailyBaseLoadInHoursSS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursSS.add(fillAggDailyControllableLoadInHours(i, type));
      }
    }
  }

  /**
   * This function is used in order to fill the aggregated daily Base Load of the village households for each quarter of the hour.
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

    Vector<Long> v = new Vector<Long>(HouseholdConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Household house : houses) {
        sum = sum + house.weeklyBaseLoad.get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the aggregated daily Controllable Load of the village households for each quarter of the hour.
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

    Vector<Long> v = new Vector<Long>(HouseholdConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Household house : houses) {
        sum = sum + house.weeklyControllableLoad.get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the daily Base Load of the household for each hour.
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
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyBaseLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyBaseLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyBaseLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyBaseLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (type.equals("RaS")) {
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyBaseLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyBaseLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyBaseLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyBaseLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (type.equals("ReS")) {
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyBaseLoadReS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyBaseLoadReS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyBaseLoadReS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyBaseLoadReS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else {
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyBaseLoadSS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyBaseLoadSS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyBaseLoadSS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyBaseLoadSS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }

    return daily;
  }

  /**
   * This function is used in order to fill the daily Base Load of the household for each hour.
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
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyControllableLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyControllableLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyControllableLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyControllableLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (type.equals("RaS")) {
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyControllableLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyControllableLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyControllableLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyControllableLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (type.equals("ReS")) {
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyControllableLoadReS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyControllableLoadReS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyControllableLoadReS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyControllableLoadReS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else {
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyControllableLoadSS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyControllableLoadSS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyControllableLoadSS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyControllableLoadSS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }

    return daily;
  }

  /**
   * This function is used in order to print the aggregated load of the village households.
   * 
   * @param type
   * @return
   */
  void showAggLoad (String type)
  {

    log.info("Portion " + type + " Weekly Aggregated Load");

    if (type.equals("NS")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION + HouseholdConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.info("Day " + i);
        for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++) {
          log.info("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursNS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursNS.get(i).get(j));
        }
      }
    } else if (type.equals("RaS")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION + HouseholdConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.info("Day " + i);
        for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++) {
          log.info("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursRaS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursRaS.get(i).get(j));
        }
      }
    } else if (type.equals("ReS")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION + HouseholdConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.info("Day " + i);
        for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++) {
          log.info("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursReS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursReS.get(i).get(j));
        }
      }
    } else {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION + HouseholdConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.info("Day " + i);
        for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++) {
          log.info("Hour : " + j + " Base Load : " + aggDailyBaseLoadInHoursSS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursSS.get(i).get(j));
        }
      }
    }
  }

  // =============================CONSUMPTION FUNCTIONS=================================================

  @Override
  public void consumePower ()
  {
    Timeslot ts = timeslotRepo.currentTimeslot();
    double summary = 0;

    for (String type : subscriptionMap.keySet()) {
      TariffSubscription sub = subscriptionMap.get(type);
      if (ts == null) {
        log.error("Current timeslot is null");
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
   * This method takes as an input the timeslot serial number (in order to know in the current time) and estimates the consumption for this timeslot over the population under the Generic Consumer.
   */
  double getConsumptionByTimeslot (int serial, String type)
  {

    int day = (int) (serial / HouseholdConstants.HOURS_OF_DAY);
    int hour = (int) (serial % HouseholdConstants.HOURS_OF_DAY);
    long summary = 0;

    log.info("Serial : " + serial + " Day: " + day + " Hour: " + hour);

    summary = (getBaseConsumptions(day, hour, type) + getControllableConsumptions(day, hour, type));

    return (double) summary / HouseholdConstants.THOUSAND;
  }

  // =============================GETTER FUNCTIONS=================================================

  /** This function returns the subscriptionMap variable of the village */
  public HashMap<String, TariffSubscription> getSubscriptionMap ()
  {
    return subscriptionMap;
  }

  /** This function returns the subscriptionMap variable of the village */
  public HashMap<String, Double> getInertiaMap ()
  {
    return inertiaMap;
  }

  /** This function returns the subscriptionMap variable of the village */
  public HashMap<String, Integer> getPeriodMap ()
  {
    return periodMap;
  }

  /** This function returns the quantity of base load for a specific day and hour of that day for a specific type of houses */
  long getBaseConsumptions (int day, int hour, String type)
  {
    long summaryBase = 0;

    if (type.equals("NS")) {
      summaryBase = aggDailyBaseLoadInHoursNS.get(day).get(hour);
    } else if (type.equals("RaS")) {
      summaryBase = aggDailyBaseLoadInHoursRaS.get(day).get(hour);
    } else if (type.equals("ReS")) {
      summaryBase = aggDailyBaseLoadInHoursReS.get(day).get(hour);
    } else {
      summaryBase = aggDailyBaseLoadInHoursSS.get(day).get(hour);
    }

    log.info("Base Load for " + type + ":" + summaryBase);
    return summaryBase;
  }

  /** This function returns the quantity of controllable load for a specific day and hour of that day for a specific type of houses */
  long getControllableConsumptions (int day, int hour, String type)
  {
    long summaryControllable = 0;

    if (type.equals("NS")) {
      summaryControllable = aggDailyControllableLoadInHoursNS.get(day).get(hour);
    } else if (type.equals("RaS")) {
      summaryControllable = aggDailyControllableLoadInHoursRaS.get(day).get(hour);
    } else if (type.equals("ReS")) {
      summaryControllable = aggDailyControllableLoadInHoursReS.get(day).get(hour);
    } else {
      summaryControllable = aggDailyControllableLoadInHoursSS.get(day).get(hour);
    }

    log.info("Controllable Load for " + type + ":" + summaryControllable);
    return summaryControllable;
  }

  /**
   * This function returns the quantity of controllable load for a specific day in form of a vector for a certain type of houses.
   */
  Vector<Long> getControllableConsumptions (int day, String type)
  {

    Vector<Long> controllableVector = new Vector<Long>();

    if (type.equals("NS")) {
      controllableVector = aggDailyControllableLoadInHoursNS.get(day);
    } else if (type.equals("RaS")) {
      controllableVector = aggDailyControllableLoadInHoursRaS.get(day);
    } else if (type.equals("ReS")) {
      controllableVector = aggDailyControllableLoadInHoursReS.get(day);
    } else {
      controllableVector = aggDailyControllableLoadInHoursSS.get(day);
    }

    return controllableVector;
  }

  /**
   * This function returns a vector with all the houses of a certain type that are present in this village
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

  // =============================EVALUATION FUNCTIONS=================================================

  /**
   * This is the basic evaluation function, taking into consideration the minimum cost without shifting the appliances' load but the tariff chosen is picked up randomly by using a possibility pattern.
   * The better tariffs have more chances to be chosen.
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
        log.info("Tariff : " + tariff.toString() + " Tariff Type : " + tariff.getTariffSpecification().getPowerType());
        if (tariff.isExpired() == false && tariff.getTariffSpecification().getPowerType() == PowerType.CONSUMPTION) {
          estimation.add(-(costEstimation(tariff, type)));
        } else
          estimation.add(Double.NEGATIVE_INFINITY);
      }

      int minIndex = logitPossibilityEstimation(estimation, type);

      TariffSubscription sub = subscriptionMap.get(type);

      log.info("Equality: " + sub.getTariff().getTariffSpec().toString() + " = " + evaluationTariffs.get(minIndex).getTariffSpec().toString());
      if (!(sub.getTariff().getTariffSpec() == evaluationTariffs.get(minIndex).getTariffSpec())) {
        log.info("Changing From " + sub.toString() + " After Evaluation");
        changeSubscription(sub.getTariff(), evaluationTariffs.get(minIndex), type);
      }

    }
  }

  /**
   * This function estimates the overall cost, taking into consideration the fixed payments as well as the variable that are depending on the tariff rates
   */
  double costEstimation (Tariff tariff, String type)
  {
    double costVariable = 0;
    if (type.equals("NS"))
      costVariable = estimateVariableTariffPayment(tariff);
    else
      costVariable = estimateShiftingVariableTariffPayment(tariff, type);

    double costFixed = estimateFixedTariffPayments(tariff);
    return (costVariable + costFixed) / HouseholdConstants.MILLION;
  }

  /**
   * This function estimates the fixed cost, comprised by fees, bonuses and penalties that are the same no matter how much you consume
   */
  double estimateFixedTariffPayments (Tariff tariff)
  {
    double lifecyclePayment = -tariff.getEarlyWithdrawPayment() - tariff.getSignupPayment();
    double minDuration;

    // When there is not a Minimum Duration of the contract, you cannot divide with the duration
    // because you don't know it.
    if (tariff.getMinDuration() == 0)
      minDuration = HouseholdConstants.MEAN_TARIFF_DURATION * TimeService.DAY;
    else
      minDuration = tariff.getMinDuration();

    log.info("Minimum Duration: " + minDuration);
    return (-tariff.getPeriodicPayment() + (lifecyclePayment / minDuration));
  }

  /**
   * This function estimates the variable cost, depending only to the load quantity you consume
   */
  double estimateVariableTariffPayment (Tariff tariff)
  {

    double finalCostSummary = 0;

    int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    Instant base = new Instant(timeService.getCurrentTime().getMillis() - serial * TimeService.HOUR);
    int daylimit = (int) (serial / HouseholdConstants.HOURS_OF_DAY) + 1; // this will be changed to
                                                                         // one

    for (int day : daysList) {
      if (day < daylimit)
        day = (int) (day + (daylimit / HouseholdConstants.RANDOM_DAYS_NUMBER));
      Instant now = base.plus(day * TimeService.DAY);
      double costSummary = 0;
      double summary = 0, cumulativeSummary = 0;

      for (int hour = 0; hour < HouseholdConstants.HOURS_OF_DAY; hour++) {

        summary = getBaseConsumptions(day, hour, "NS") + getControllableConsumptions(day, hour, "NS");

        log.debug("Cost for hour " + hour + ":" + tariff.getUsageCharge(now, 1, 0));
        cumulativeSummary += summary;
        costSummary -= tariff.getUsageCharge(now, summary, cumulativeSummary);
        now = now.plus(TimeService.HOUR);
      }
      log.debug("Variable Cost Summary: " + finalCostSummary);
      finalCostSummary += costSummary;
    }
    return finalCostSummary / HouseholdConstants.RANDOM_DAYS_NUMBER;
  }

  /**
   * This is the new function, used in order to find the most cost efficient tariff over the available ones. It is using Daily shifting in order to put the appliances operation in most suitable hours
   * of the day.
   * 
   * @param tariff
   * @return
   */
  double estimateShiftingVariableTariffPayment (Tariff tariff, String type)
  {

    double finalCostSummary = 0;

    int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    Instant base = timeService.getCurrentTime().minus(serial * TimeService.HOUR);
    int daylimit = (int) (serial / HouseholdConstants.HOURS_OF_DAY) + 1; // this will be changed to

    for (int day : daysList) {
      if (day < daylimit)
        day = (int) (day + (daylimit / HouseholdConstants.RANDOM_DAYS_NUMBER));
      Instant now = base.plus(day * TimeService.DAY);
      double costSummary = 0;
      double summary = 0, cumulativeSummary = 0;

      long[] newControllableLoad = dailyShifting(tariff, now, day, type);

      for (int hour = 0; hour < HouseholdConstants.HOURS_OF_DAY; hour++) {
        summary = getBaseConsumptions(day, hour, type) + newControllableLoad[hour];
        cumulativeSummary += summary;
        costSummary -= tariff.getUsageCharge(now, summary, cumulativeSummary);
        now = now.plus(TimeService.HOUR);
      }
      log.debug("Variable Cost Summary: " + finalCostSummary);
      finalCostSummary += costSummary;
    }
    return finalCostSummary / HouseholdConstants.RANDOM_DAYS_NUMBER;
  }

  /**
   * This is the function that realizes the mathematical possibility formula for the choice of tariff.
   */
  int logitPossibilityEstimation (Vector<Double> estimation, String type)
  {

    double lamda = lamdaMap.get(type);
    double summedEstimations = 0;
    Vector<Integer> randomizer = new Vector<Integer>();
    Vector<Integer> possibilities = new Vector<Integer>();

    for (int i = 0; i < estimation.size(); i++) {
      summedEstimations += Math.pow(HouseholdConstants.EPSILON, lamda * estimation.get(i));
      log.info("Cost variable: " + estimation.get(i));
      log.info("Summary of Estimation: " + summedEstimations);
    }

    for (int i = 0; i < estimation.size(); i++) {
      possibilities.add((int) (HouseholdConstants.PERCENTAGE * (Math.pow(HouseholdConstants.EPSILON, lamda * estimation.get(i)) / summedEstimations)));
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

  // =============================SHIFTING FUNCTIONS=================================================

  /**
   * This is the function that takes every household in the village and readies the shifted Controllable Consumption for the needs of the tariff evaluation.
   * 
   * @param tariff
   * @param now
   * @param day
   * @param type
   * @return
   */
  long[] dailyShifting (Tariff tariff, Instant now, int day, String type)
  {

    long[] newControllableLoad = new long[HouseholdConstants.HOURS_OF_DAY];

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
      long[] temp = house.dailyShifting(tariff, now, day, gen);
      for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++)
        newControllableLoad[j] += temp[j];
    }

    log.debug("New Controllable Load of Village " + toString() + " type " + type + " for Tariff " + tariff.toString());

    for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
      log.debug("Hour: " + i + " Cost: " + tariff.getUsageCharge(now, 1, 0) + " Load For Type " + type + " : " + newControllableLoad[i]);
      now = new Instant(now.getMillis() + TimeService.HOUR);
    }
    return newControllableLoad;
  }

  // =============================STATUS FUNCTIONS=================================================

  /**
   * This function prints to the screen the daily load of the village's households for the weekday at hand.
   * 
   * @param day
   * @param type
   * @return
   */
  void printDailyLoad (int day, String type)
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

    log.info("Day " + day);

    for (Household house : houses) {
      house.printDailyLoad(day);
    }

  }

  /**
   * This function represents the function that shows the status of all the households in the village each moment in time.
   * 
   * @param day
   * @param quarter
   * @return
   */
  void stepStatus (int day, int quarter, String type)
  {
    Vector<Household> houses = getHouses(type);
    for (Household house : houses) {
      house.stepStatus(day, quarter);
    }
  }

  // =============================VECTOR CREATION=================================================

  /**
   * This function is creating a certain number of random days that will be public vacation for the people living in the environment.
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
      int x = gen.nextInt(HouseholdConstants.DAYS_OF_COMPETITION + HouseholdConstants.DAYS_OF_BOOTSTRAP);
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
   * This function is creating the list of days for each village that will be utilized for the tariff evaluation.
   * 
   * @param days
   * @param gen
   * @return
   */
  void createCostEstimationDaysList (int days)
  {

    for (int i = 0; i < days; i++) {
      int x = gen.nextInt(HouseholdConstants.DAYS_OF_COMPETITION + HouseholdConstants.DAYS_OF_BOOTSTRAP);
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

  // =============================STEP FUNCTIONS=================================================

  @Override
  public void step ()
  {
    checkRevokedSubscriptions();
    consumePower();
    if (timeService.getHourOfDay() == 23) {
      for (String type : subscriptionMap.keySet()) {
        if (!(type.equals("NS"))) {
          log.info("Rescheduling " + type);
          rescheduleNextDay(type);
        }
      }
    }

  }

  /**
   * This function is utilized in order to reschedule the consumption load for the next day of the competition according to the tariff rates of the subscriptions under contract.
   */
  void rescheduleNextDay (String type)
  {

    int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    int day = (int) (serial / HouseholdConstants.HOURS_OF_DAY) + 1; // this will be changed to one
    Instant now = new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR);
    Vector<Long> controllableVector = new Vector<Long>();

    TariffSubscription sub = subscriptionMap.get(type);

    log.info("Old Consumption for day " + day + ": " + getControllableConsumptions(day, type).toString());
    long[] newControllableLoad = dailyShifting(sub.getTariff(), now, day, type);
    for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++)
      controllableVector.add(newControllableLoad[i]);
    log.info("New Consumption for day " + day + ": " + controllableVector.toString());

    if (type.equals("RaS")) {
      aggDailyBaseLoadInHoursRaS.set(day, controllableVector);
    } else if (type.equals("ReS")) {
      aggDailyBaseLoadInHoursReS.set(day, controllableVector);
    } else {
      aggDailyBaseLoadInHoursSS.set(day, controllableVector);
    }

  }

  public String toString ()
  {
    return customerInfo.toString();
  }

}
