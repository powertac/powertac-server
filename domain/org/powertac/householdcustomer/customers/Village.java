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
import java.util.Collections;
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
 * The village domain class in this first version is a set of households that comprise a small
 * village that consumes agreggated energy by the appliances installed in each household.Later on
 * other types of building will be added.
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class Village extends AbstractCustomer
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error() appropriately. Use
   * log.debug() for output you want to see in testing or debugging.
   */
  static protected Logger log = Logger.getLogger(Village.class.getName());

  @Autowired
  TimeService timeService;

  @Autowired
  TimeslotRepo timeslotRepo;

  /**
   * These are the vectors containing aggregated each day's base load from the appliances installed
   * inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyBaseLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's controllable load from the appliances
   * installed inside the households.
   **/
  Vector<Vector<Long>> aggDailyControllableLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the agreggated vectors containing each day's base load of all the households in
   * hours.
   **/
  Vector<Vector<Long>> aggDailyBaseLoadInHoursNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursSS = new Vector<Vector<Long>>();

  /**
   * These are the agreggated vectors containing each day's controllable load of all the households
   * in hours.
   **/
  Vector<Vector<Long>> aggDailyControllableLoadInHoursNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursRaS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursSS = new Vector<Vector<Long>>();

  /**
   * This is an vector containing the days of the competition that the household model will use in
   * order to check which of the tariffs that are available at any given moment are the optimal for
   * their consumption or production.
   **/
  Vector<Integer> daysList = new Vector<Integer>();

  /**
   * This variable is utilized for the creation of the random numbers and is taken from the service.
   */
  Random gen;

  /** This is the constructor function of the Village customer */
  public Village (CustomerInfo customerInfo)
  {
    super(customerInfo);

    timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    timeService = (TimeService) SpringApplicationContext.getBean("timeService");
  }

  /**
   * These vectors contain the houses of type in the village. There are 4 types available: 1) Not
   * Shifting Houses: They do not change the tariff subscriptions during the game. 2) Randomly
   * Shifting Houses: They change their tariff subscriptions in a random way. 3) Regularly Shifting
   * Houses: They change their tariff subscriptions during the game in regular time periods. 4)
   * Smart Shifting Houses: They change their tariff subscriptions in a smart way in order to
   * minimize their costs.
   */
  Vector<Household> notShiftingHouses = new Vector<Household>();
  Vector<Household> randomlyShiftingHouses = new Vector<Household>();
  Vector<Household> regularlyShiftingHouses = new Vector<Household>();
  Vector<Household> smartShiftingHouses = new Vector<Household>();

  /**
   * This is the initialization function. It uses the variable values for the configuration file to
   * create the village with its households and then fill them with persons and appliances.
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

    fillAggWeeklyLoad("NotShifting");
    fillAggWeeklyLoad("RandomlyShifting");
    fillAggWeeklyLoad("RegularlyShifting");
    fillAggWeeklyLoad("SmartShifting");

    showAggLoad("NotShifting");
    showAggLoad("RandomlyShifting");
    showAggLoad("RegularlyShifting");
    showAggLoad("SmartShifting");

  }

  /**
   * This function is used in order to fill each week day of the aggregated daily Load of the
   * village households for each quarter of the hour.
   * @param portion
   * @return
   */
  void fillAggWeeklyLoad (String portion)
  {

    if (portion.equals("NotShifting")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK * (HouseholdConstants.WEEKS_OF_COMPETITION + HouseholdConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadNS.add(fillAggDailyBaseLoad(i, portion));
        aggDailyControllableLoadNS.add(fillAggDailyControllableLoad(i, portion));
        aggDailyBaseLoadInHoursNS.add(fillAggDailyBaseLoadInHours(i, portion));
        aggDailyControllableLoadInHoursNS.add(fillAggDailyControllableLoadInHours(i, portion));
      }
    } else if (portion.equals("RandomlyShifting")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK * (HouseholdConstants.WEEKS_OF_COMPETITION + HouseholdConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadRaS.add(fillAggDailyBaseLoad(i, portion));
        aggDailyControllableLoadRaS.add(fillAggDailyControllableLoad(i, portion));
        aggDailyBaseLoadInHoursRaS.add(fillAggDailyBaseLoadInHours(i, portion));
        aggDailyControllableLoadInHoursRaS.add(fillAggDailyControllableLoadInHours(i, portion));
      }
    } else if (portion.equals("RegularlyShifting")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK * (HouseholdConstants.WEEKS_OF_COMPETITION + HouseholdConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadReS.add(fillAggDailyBaseLoad(i, portion));
        aggDailyControllableLoadReS.add(fillAggDailyControllableLoad(i, portion));
        aggDailyBaseLoadInHoursReS.add(fillAggDailyBaseLoadInHours(i, portion));
        aggDailyControllableLoadInHoursReS.add(fillAggDailyControllableLoadInHours(i, portion));
      }
    } else {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK * (HouseholdConstants.WEEKS_OF_COMPETITION + HouseholdConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadSS.add(fillAggDailyBaseLoad(i, portion));
        aggDailyControllableLoadSS.add(fillAggDailyControllableLoad(i, portion));
        aggDailyBaseLoadInHoursSS.add(fillAggDailyBaseLoadInHours(i, portion));
        aggDailyControllableLoadInHoursSS.add(fillAggDailyControllableLoadInHours(i, portion));
      }
    }
  }

  /**
   * This function is used in order to print the aggregated load of the village households.
   * @param portion
   * @return
   */
  void showAggLoad (String portion)
  {

    log.info("Portion " + portion + " Weekly Aggregated Load");

    if (portion.equals("NotShifting")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION + HouseholdConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.info("Day " + i);
        for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++) {
          log.info("Hour : " + j + 1 + " Base Load : " + aggDailyBaseLoadInHoursNS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursNS.get(i).get(j));
        }
      }
    } else if (portion.equals("RandomlyShifting")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION + HouseholdConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.info("Day " + i);
        for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++) {
          log.info("Hour : " + j + 1 + " Base Load : " + aggDailyBaseLoadInHoursRaS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursRaS.get(i).get(j));
        }
      }
    } else if (portion.equals("RegularlyShifting")) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION + HouseholdConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.info("Day " + i);
        for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++) {
          log.info("Hour : " + j + 1 + " Base Load : " + aggDailyBaseLoadInHoursReS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursReS.get(i).get(j));
        }
      }
    } else {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION + HouseholdConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.info("Day " + i);
        for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++) {
          log.info("Hour : " + j + 1 + " Base Load : " + aggDailyBaseLoadInHoursSS.get(i).get(j) + " Controllable Load: " + aggDailyControllableLoadInHoursSS.get(i).get(j));
        }
      }
    }
  }

  @Override
  public void consumePower ()
  {
    Timeslot ts = timeslotRepo.currentTimeslot();
    double summary = 0;
    List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(this.getCustomerInfo());

    for (TariffSubscription sub : subscriptions) {
      if (ts == null) {
        int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
        summary = getConsumptionByTimeslot(serial);
      } else {
        summary = getConsumptionByTimeslot(ts.getSerialNumber());
      }
      log.info("Consumption Load: " + summary + "/" + subscriptions.size());
      sub.usePower(summary / subscriptions.size());
    }
  }

  /**
   * This method takes as an input the timeslot serial number (in order to know in the current time)
   * and estimates the consumption for this timeslot over the population under the Generic Consumer.
   */
  double getConsumptionByTimeslot (int serial)
  {

    int day = (int) (serial / HouseholdConstants.HOURS_OF_DAY);
    int hour = (int) (serial % HouseholdConstants.HOURS_OF_DAY);
    long summary = 0;

    log.info("Serial : " + serial + " Day: " + day + " Hour: " + hour);

    summary = (getBaseConsumptions(day, hour) + getControllableConsumptions(day, hour));

    return (double) summary / HouseholdConstants.THOUSAND;
  }

  /** This function returns the quantity of base load for a specific day and hour of that day */
  long getBaseConsumptions (int day, int hour)
  {

    long summaryBase = aggDailyBaseLoadNS.get(day).get(hour) + aggDailyBaseLoadRaS.get(day).get(hour) + aggDailyBaseLoadReS.get(day).get(hour) + aggDailyBaseLoadSS.get(day).get(hour);

    log.info("Base Load " + summaryBase);
    return summaryBase;
  }

  /**
   * This function returns the quantity of controllable load for a specific day in form of a vector
   */
  Vector<Long> getControllableConsumptions (int day)
  {
    Vector<Household> houses = getHouses();
    Vector<Long> controllableVector = new Vector<Long>();

    for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++) {
      long summaryControllable = 0;
      for (Household house : houses) {
        summaryControllable += house.weeklyControllableLoadInHours.get(day).get(j);
      }
      controllableVector.add(summaryControllable);
    }

    return controllableVector;
  }

  /**
   * This function returns the quantity of controllable load for a specific day and hour of that day
   */
  long getControllableConsumptions (int day, int hour)
  {
    long summaryControllable = aggDailyControllableLoadNS.get(day).get(hour) + aggDailyControllableLoadRaS.get(day).get(hour) + aggDailyControllableLoadReS.get(day).get(hour)
        + aggDailyControllableLoadSS.get(day).get(hour);
    log.info("Controllable Load " + summaryControllable);
    return summaryControllable;
  }

  /**
   * This function returns a vector with all the houses of a certain type that are present in this
   * village
   */
  Vector<Household> getHouses (String portion)
  {

    Vector<Household> houses = new Vector<Household>();

    if (portion.equals("NotShifting")) {
      for (Household house : notShiftingHouses) {
        houses.add(house);
      }
    } else if (portion.equals("RandomlyShifting")) {
      for (Household house : regularlyShiftingHouses) {
        houses.add(house);
      }
    } else if (portion.equals("RegularlyShifting")) {
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

  /** This function returns a vector with all the houses that are present in this village */
  Vector<Household> getHouses ()
  {

    Vector<Household> houses = new Vector<Household>();

    for (Household house : notShiftingHouses) {
      houses.add(house);
    }

    for (Household house : randomlyShiftingHouses) {
      houses.add(house);
    }

    for (Household house : regularlyShiftingHouses) {
      houses.add(house);
    }

    for (Household house : smartShiftingHouses) {
      houses.add(house);
    }

    return houses;

  }

  /**
   * This function is used in order to fill the aggregated daily Base Load of the village households
   * for each quarter of the hour.
   * @param day
   * @param portion
   * @return
   */
  Vector<Long> fillAggDailyBaseLoad (int day, String portion)
  {

    Vector<Household> houses = new Vector<Household>();

    if (portion.equals("NotShifting")) {
      houses = notShiftingHouses;
    } else if (portion.equals("RandomlyShifting")) {
      houses = randomlyShiftingHouses;
    } else if (portion.equals("RegularlyShifting")) {
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
   * This function is used in order to fill the aggregated daily Controllable Load of the village
   * households for each quarter of the hour.
   * @param day
   * @param portion
   * @return
   */
  Vector<Long> fillAggDailyControllableLoad (int day, String portion)
  {

    Vector<Household> houses = new Vector<Household>();

    if (portion.equals("NotShifting")) {
      houses = notShiftingHouses;
    } else if (portion.equals("RandomlyShifting")) {
      houses = randomlyShiftingHouses;
    } else if (portion.equals("RegularlyShifting")) {
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
   * @param day
   * @param portion
   * @return
   */
  Vector<Long> fillAggDailyBaseLoadInHours (int day, String portion)
  {

    Vector<Long> daily = new Vector<Long>();
    long sum = 0;

    if (portion.equals("NotShifting")) {
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyBaseLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyBaseLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyBaseLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyBaseLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (portion.equals("RandomlyShifting")) {
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyBaseLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyBaseLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyBaseLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyBaseLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (portion.equals("RegularlyShifting")) {
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
   * @param day
   * @param portion
   * @return
   */
  Vector<Long> fillAggDailyControllableLoadInHours (int day, String portion)
  {

    Vector<Long> daily = new Vector<Long>();
    long sum = 0;

    if (portion.equals("NotShifting")) {
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyControllableLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyControllableLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyControllableLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyControllableLoadNS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (portion.equals("RandomlyShifting")) {
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum = aggDailyControllableLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR) + aggDailyControllableLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
            + aggDailyControllableLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + aggDailyControllableLoadRaS.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    } else if (portion.equals("RegularlyShifting")) {
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
   * This is the basic evaluation function, taking into consideration the minimum cost without
   * shifting the appliances' load but the tariff chosen is picked up randomly by using a
   * possibility pattern. The better tariffs have more chances to be chosen.
   */
  public void possibilityEvaluationNewTariffs (List<Tariff> newTariffs)
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
        if (tariff.isExpired() == false && tariff.getTariffSpecification().getPowerType() == PowerType.CONSUMPTION) {
          estimation.add(-(costEstimation(tariff)));
        } else
          estimation.add(Double.NEGATIVE_INFINITY);
      }

      int minIndex = logitPossibilityEstimation(estimation);

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
   * This function estimates the overall cost, taking into consideration the fixed payments as well
   * as the variable that are depending on the tariff rates
   */
  double costEstimation (Tariff tariff)
  {
    double costVariable = estimateShiftingVariableTariffPayment(tariff);
    double costFixed = estimateFixedTariffPayments(tariff);
    return (costVariable + costFixed) / HouseholdConstants.MILLION;
  }

  /**
   * This function estimates the fixed cost, comprised by fees, bonuses and penalties that are the
   * same no matter how much you consume
   */
  double estimateFixedTariffPayments (Tariff tariff)
  {
    double lifecyclePayment = (double) tariff.getEarlyWithdrawPayment() + (double) tariff.getSignupPayment();
    double minDuration;

    // When there is not a Minimum Duration of the contract, you cannot divide with the duration
    // because you don't know it.
    if (tariff.getMinDuration() == 0)
      minDuration = HouseholdConstants.MEAN_TARIFF_DURATION * TimeService.DAY;
    else
      minDuration = tariff.getMinDuration();

    log.info("Minimum Duration: " + minDuration);
    return ((double) tariff.getPeriodicPayment() + (lifecyclePayment / minDuration));
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
      Instant now = new Instant(base.getMillis() + day * TimeService.DAY);
      double costSummary = 0;
      double summary = 0, cumulativeSummary = 0;

      for (int hour = 0; hour < HouseholdConstants.HOURS_OF_DAY; hour++) {

        summary = getBaseConsumptions(day, hour) + getControllableConsumptions(day, hour);

        log.debug("Cost for hour " + hour + ":" + tariff.getUsageCharge(now, 1, 0));
        cumulativeSummary += summary;
        costSummary += tariff.getUsageCharge(now, summary, cumulativeSummary);
        now = new Instant(now.getMillis() + TimeService.HOUR);
      }
      log.debug("Variable Cost Summary: " + finalCostSummary);
      finalCostSummary += costSummary;
    }
    return finalCostSummary / HouseholdConstants.RANDOM_DAYS_NUMBER;
  }

  /**
   * This is the new function, used in order to find the most cost efficient tariff over the
   * available ones. It is using Daily shifting in order to put the appliances operation in most
   * suitable hours of the day.
   * @param tariff
   * @return
   */
  double estimateShiftingVariableTariffPayment (Tariff tariff)
  {

    double finalCostSummary = 0;

    int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    Instant base = new Instant(timeService.getCurrentTime().getMillis() - serial * TimeService.HOUR);
    int daylimit = (int) (serial / HouseholdConstants.HOURS_OF_DAY) + 1; // this will be changed to

    for (int day : daysList) {
      if (day < daylimit)
        day = (int) (day + (daylimit / HouseholdConstants.RANDOM_DAYS_NUMBER));
      Instant now = new Instant(base.getMillis() + day * TimeService.DAY);
      double costSummary = 0;
      double summary = 0, cumulativeSummary = 0;

      long[] newControllableLoad = dailyShifting(tariff, now, day);

      for (int hour = 0; hour < HouseholdConstants.HOURS_OF_DAY; hour++) {
        summary = getBaseConsumptions(day, hour) + newControllableLoad[hour];
        cumulativeSummary += summary;
        costSummary += tariff.getUsageCharge(now, summary, cumulativeSummary);
        now = new Instant(now.getMillis() + TimeService.HOUR);
      }
      log.debug("Variable Cost Summary: " + finalCostSummary);
      finalCostSummary += costSummary;
    }
    return finalCostSummary / HouseholdConstants.RANDOM_DAYS_NUMBER;
  }

  /**
   * This is the function that realizes the mathematical possibility formula for the choice of
   * tariff.
   */
  int logitPossibilityEstimation (Vector<Double> estimation)
  {

    double lamda = 2500; // 0 the random - 10 the logic
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

  /**
   * This is the function that takes every household in the village and readies the shifted
   * Controllable Consumption for the needs of the tariff evaluation.
   * @param tariff
   * @param now
   * @param day
   * @return
   */
  long[] dailyShifting (Tariff tariff, Instant now, int day)
  {

    long[] newControllableLoad = new long[HouseholdConstants.HOURS_OF_DAY];

    Vector<Household> houses = getHouses();

    for (Household house : houses) {
      long[] temp = house.dailyShifting(tariff, now, day, gen);
      for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++)
        newControllableLoad[j] += temp[j];
    }

    log.debug("New Controllable Load of Village " + toString() + " for Tariff " + tariff.toString());

    for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
      log.debug("Hour: " + i + " Cost: " + tariff.getUsageCharge(now, 1, 0) + " Load: " + newControllableLoad[i]);
      now = new Instant(now.getMillis() + TimeService.HOUR);
    }

    return newControllableLoad;

  }

  /**
   * This is the function that takes every household in the village and readies the shifted
   * Controllable Consumption for the needs of the tariff evaluation.
   * @param tariff
   * @param now
   * @param day
   * @param portion
   * @return
   */
  long[] dailyShifting (Tariff tariff, Instant now, int day, String portion)
  {

    long[] newControllableLoad = new long[HouseholdConstants.HOURS_OF_DAY];

    Vector<Household> houses = new Vector<Household>();

    if (portion.equals("NotShifting")) {
      houses = notShiftingHouses;
    } else if (portion.equals("RandomlyShifting")) {
      houses = randomlyShiftingHouses;
    } else if (portion.equals("RegularlyShifting")) {
      houses = regularlyShiftingHouses;
    } else {
      houses = smartShiftingHouses;
    }

    for (Household house : houses) {
      long[] temp = house.dailyShifting(tariff, now, day, gen);
      for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++)
        newControllableLoad[j] += temp[j];
    }

    log.debug("New Controllable Load of Village " + toString() + " for Tariff " + tariff.toString());

    for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
      log.debug("Hour: " + i + " Cost: " + tariff.getUsageCharge(now, 1, 0) + " Load: " + newControllableLoad[i]);
      now = new Instant(now.getMillis() + TimeService.HOUR);
    }
    return newControllableLoad;
  }

  /**
   * This function prints to the screen the daily load of the village's households for the weekday
   * at hand.
   * @param day
   * @param portion
   * @return
   */
  void printDailyLoad (int day, String portion)
  {

    Vector<Household> houses = new Vector<Household>();

    if (portion.equals("NotShifting")) {
      houses = notShiftingHouses;
    } else if (portion.equals("RandomlyShifting")) {
      houses = randomlyShiftingHouses;
    } else if (portion.equals("RegularlyShifting")) {
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
   * This function represents the function that shows the status of all the households in the
   * village each moment in time.
   * @param day
   * @param quarter
   * @return
   */
  void stepStatus (int day, int quarter)
  {
    Vector<Household> houses = getHouses();

    for (Household house : houses) {
      house.stepStatus(day, quarter);
    }
  }

  /**
   * This function is creating a certain number of random days that will be public vacation for the
   * people living in the environment.
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
   * This function is creating the list of days for each village that will be utilized for the
   * tariff evaluation.
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

  @Override
  public void step ()
  {
    checkRevokedSubscriptions();
    consumePower();
    if (timeService.getHourOfDay() == 23) {
      rescheduleNextDay("RandomlyShifting");
      rescheduleNextDay("RegularlyShifting");
      rescheduleNextDay("SmartShifting");
    }

  }

  /**
   * This function is utilized in order to reschedule the consumption load for the next day of the
   * competition according to the tariff rates of the subscriptions under contract.
   */
  void rescheduleNextDay (String portion)
  {
    int serial = (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    int day = (int) (serial / HouseholdConstants.HOURS_OF_DAY) + 1; // this will be changed to one
    Instant now = new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR);
    Vector<Long> controllableVector = new Vector<Long>();

    List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(this.getCustomerInfo());
    for (TariffSubscription sub : subscriptions) {
      log.info("Old Consumption for day " + day + ": " + getControllableConsumptions(day).toString());
      long[] newControllableLoad = dailyShifting(sub.getTariff(), now, day, portion);
      for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++)
        controllableVector.add(newControllableLoad[i]);
      log.info("New Consumption for day " + day + ": " + controllableVector.toString());

      if (portion.equals("RandomlyShifting")) {
        aggDailyBaseLoadInHoursRaS.set(day, controllableVector);
      } else if (portion.equals("RegularlyShifting")) {
        aggDailyBaseLoadInHoursReS.set(day, controllableVector);
      } else if (portion.equals("SmartShifting")) {
        aggDailyBaseLoadInHoursSS.set(day, controllableVector);
      } else {

      }

    }
  }

  public String toString ()
  {
    return customerInfo.toString();
  }

}
