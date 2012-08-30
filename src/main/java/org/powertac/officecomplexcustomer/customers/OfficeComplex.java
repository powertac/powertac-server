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
package org.powertac.officecomplexcustomer.customers;

import java.util.ArrayList;
import java.util.Collection;
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
import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The office complex domain class is a set of offices that comprise a office
 * building that consumes aggregated energy by the appliances installed in each
 * office.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class OfficeComplex extends AbstractCustomer
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static protected Logger log = Logger.getLogger(OfficeComplex.class.getName());

  @Autowired
  TimeService timeService;

  @Autowired
  TimeslotRepo timeslotRepo;

  @Autowired
  WeatherReportRepo weatherReportRepo;

  /**
   * These are the vectors containing aggregated each day's base load from the
   * appliances installed inside the offices of each type.
   **/
  Vector<Vector<Long>> aggDailyBaseLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's controllable load
   * from the appliances installed inside the offices.
   **/
  Vector<Vector<Long>> aggDailyControllableLoadNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's weather sensitive
   * load from the appliances installed inside the offices.
   **/
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadNS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadSS =
    new Vector<Vector<Long>>();

  /**
   * These are the aggregated vectors containing each day's base load of all the
   * offices in hours.
   **/
  Vector<Vector<Long>> aggDailyBaseLoadInHoursNS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursSS = new Vector<Vector<Long>>();

  /**
   * These are the aggregated vectors containing each day's controllable load of
   * all the offices in hours.
   **/
  Vector<Vector<Long>> aggDailyControllableLoadInHoursNS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursSS =
    new Vector<Vector<Long>>();

  /**
   * These are the aggregated vectors containing each day's weather sensitive
   * load of all the offices in hours.
   **/
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursNS =
    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursSS =
    new Vector<Vector<Long>>();

  /**
   * This is an vector containing the days of the competition that the office
   * complex model will use in order to check which of the tariffs that are
   * available at any given moment are the optimal for their consumption or
   * production.
   **/
  Vector<Integer> daysList = new Vector<Integer>();

  /**
   * This variable is utilized for the creation of the random numbers and is
   * taken from the service.
   */
  Random gen;

  /**
   * These variables are mapping of the characteristics of the types of offices.
   * The first is used to keep track of their subscription at any given time.
   * The second is the inertia parameter for each type of offices. The third is
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

  /**
   * These vectors contain the offices of type in the office complex. There are
   * 2 types available: 1) Not Shifting offices: They do not change the tariff
   * subscriptions during the game. 2) Smart Shifting offices: They change their
   * tariff subscriptions in a smart way in order to minimize their costs.
   */
  Vector<Office> notShiftingoffices = new Vector<Office>();
  Vector<Office> smartShiftingoffices = new Vector<Office>();

  /** This is the constructor function of the OfficeComplex customer */
  public OfficeComplex (String name)
  {
    super(name);

    timeslotRepo =
      (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    timeService = (TimeService) SpringApplicationContext.getBean("timeService");
    weatherReportRepo =
      (WeatherReportRepo) SpringApplicationContext.getBean("weatherReportRepo");

    ArrayList<String> typeList = new ArrayList<String>();
    typeList.add("NS");
    typeList.add("SS");

    for (String type: typeList) {
      subscriptionMap.put(type, null);
      controllableSubscriptionMap.put(type, null);
      inertiaMap.put(type, null);
      periodMap.put(type, null);
      lamdaMap.put(type, null);
    }
  }

  /** This is the second constructor function of the Village customer */
  public OfficeComplex (String name, ArrayList<CustomerInfo> customerInfo)
  {
    super(name, customerInfo);

    timeslotRepo =
      (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    timeService = (TimeService) SpringApplicationContext.getBean("timeService");
    weatherReportRepo =
      (WeatherReportRepo) SpringApplicationContext.getBean("weatherReportRepo");

    ArrayList<String> typeList = new ArrayList<String>();
    typeList.add("NS");
    typeList.add("SS");

    for (String type: typeList) {
      subscriptionMap.put(type, null);
      controllableSubscriptionMap.put(type, null);
      inertiaMap.put(type, null);
      periodMap.put(type, null);
      lamdaMap.put(type, null);
    }
  }

  /**
   * This is the initialization function. It uses the variable values for the
   * configuration file to create the office complex with its offices and then
   * fill them with persons and appliances.
   * 
   * @param conf
   * @param gen
   */
  public void initialize (Properties conf, Random generator)
  {
    // Initializing variables

    int nsoffices = Integer.parseInt(conf.getProperty("NotShiftingCustomers"));
    int ssoffices =
      Integer.parseInt(conf.getProperty("SmartShiftingCustomers"));
    int days = Integer.parseInt(conf.getProperty("PublicVacationDuration"));

    gen = generator;

    createCostEstimationDaysList(OfficeComplexConstants.RANDOM_DAYS_NUMBER);

    Vector<Integer> publicVacationVector = createPublicVacationVector(days);

    for (int i = 0; i < nsoffices; i++) {
      log.info("Initializing " + toString() + " NSoffice " + i);
      Office of = new Office();
      of.initialize(toString() + " NSoffice" + i, conf, publicVacationVector,
                    gen);
      notShiftingoffices.add(of);
      of.officeOf = this;
    }

    for (int i = 0; i < ssoffices; i++) {
      log.info("Initializing " + toString() + " SSoffice " + i);
      Office hh = new Office();
      hh.initialize(toString() + " SSoffice" + i, conf, publicVacationVector,
                    gen);
      smartShiftingoffices.add(hh);
      hh.officeOf = this;
    }

    for (String type: subscriptionMap.keySet()) {
      fillAggWeeklyLoad(type);
      inertiaMap.put(type,
                     Double.parseDouble(conf.getProperty(type + "Inertia")));
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
        log.debug(subscriptions.toString());

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

    log.debug("Base Load Subscriptions:" + subscriptionMap.toString());
    log.debug("Controllable Load Subscriptions:"
              + controllableSubscriptionMap.toString());
  }

  /**
   * The first implementation of the changing subscription function. Here we
   * just put the tariff we want to change and the whole population is moved to
   * another random tariff.
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

    updateSubscriptions(tariff, newTariff, customer);

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
    int populationCount = getOffices(type).size();
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
   * moved to another random tariff.
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

    updateSubscriptions(tariff, newTariff, customer);
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
    int populationCount = getOffices(type).size();
    unsubscribe(ts, populationCount);
    subscribe(newTariff, populationCount, customer);

    updateSubscriptions(tariff, newTariff, type, customer);
  }

  /**
   * This function is used to update the subscriptionMap variable with the
   * changes made.
   * 
   */
  private void updateSubscriptions (Tariff tariff, Tariff newTariff,
                                    CustomerInfo customer)
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
          || controllableSubscriptionMap.get("NS") == null)
        controllableSubscriptionMap.put("NS", newTs);
      if (controllableSubscriptionMap.get("SS") == ts
          || controllableSubscriptionMap.get("SS") == null)
        controllableSubscriptionMap.put("SS", newTs);

      log.debug("Controllable Subscription Map: "
                + controllableSubscriptionMap.toString());
    }
    else {

      if (subscriptionMap.get("NS") == ts || subscriptionMap.get("NS") == null)
        subscriptionMap.put("NS", newTs);
      if (subscriptionMap.get("SS") == ts || subscriptionMap.get("SS") == null)
        subscriptionMap.put("SS", newTs);

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
        revokedSubscription.handleRevokedTariff();

        Tariff tariff = revokedSubscription.getTariff();
        Tariff newTariff = revokedSubscription.getTariff().getIsSupersededBy();
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
          updateSubscriptions(tariff, defaultTariff, customer);
        else
          updateSubscriptions(tariff, newTariff, customer);
      }
    }
  }

  // =====LOAD FUNCTIONS===== //

  /**
   * This function is used in order to fill each week day of the aggregated
   * daily Load of the office complex offices for each quarter of the hour.
   * 
   * @param type
   * @return
   */
  void fillAggWeeklyLoad (String type)
  {

    if (type.equals("NS")) {
      for (int i = 0; i < OfficeComplexConstants.DAYS_OF_WEEK
                          * (OfficeComplexConstants.WEEKS_OF_COMPETITION + OfficeComplexConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadNS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadNS.add(fillAggDailyControllableLoad(i, type));
        aggDailyWeatherSensitiveLoadNS
                .add(fillAggDailyWeatherSensitiveLoad(i, type));
        aggDailyBaseLoadInHoursNS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursNS
                .add(fillAggDailyControllableLoadInHours(i, type));
        aggDailyWeatherSensitiveLoadInHoursNS
                .add(fillAggDailyWeatherSensitiveLoadInHours(i, type));
      }
    }
    else {
      for (int i = 0; i < OfficeComplexConstants.DAYS_OF_WEEK
                          * (OfficeComplexConstants.WEEKS_OF_COMPETITION + OfficeComplexConstants.WEEKS_OF_BOOTSTRAP); i++) {
        aggDailyBaseLoadSS.add(fillAggDailyBaseLoad(i, type));
        aggDailyControllableLoadSS.add(fillAggDailyControllableLoad(i, type));
        aggDailyWeatherSensitiveLoadSS
                .add(fillAggDailyWeatherSensitiveLoad(i, type));
        aggDailyBaseLoadInHoursSS.add(fillAggDailyBaseLoadInHours(i, type));
        aggDailyControllableLoadInHoursSS
                .add(fillAggDailyControllableLoadInHours(i, type));
        aggDailyWeatherSensitiveLoadInHoursSS
                .add(fillAggDailyWeatherSensitiveLoadInHours(i, type));
      }
    }
  }

  /**
   * This function is used in order to update the daily aggregated Load in case
   * there are changes in the weather sensitive loads of the office complex's
   * offices.
   * 
   * @param type
   * @return
   */
  void updateAggDailyWeatherSensitiveLoad (String type, int day)
  {
    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);
    if (type.equals("NS")) {
      aggDailyWeatherSensitiveLoadNS
              .set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
      aggDailyWeatherSensitiveLoadInHoursNS
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
   * the office complex's offices for each quarter of the hour.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyBaseLoad (int day, String type)
  {

    Vector<Office> offices = new Vector<Office>();

    if (type.equals("NS")) {
      offices = notShiftingoffices;
    }
    else {
      offices = smartShiftingoffices;
    }

    Vector<Long> v = new Vector<Long>(OfficeComplexConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Office office: offices) {
        sum = sum + office.weeklyBaseLoad.get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the aggregated daily Controllable
   * Load of the office complex's offices for each quarter of the hour.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyControllableLoad (int day, String type)
  {

    Vector<Office> offices = new Vector<Office>();

    if (type.equals("NS")) {
      offices = notShiftingoffices;
    }
    else {
      offices = smartShiftingoffices;
    }

    Vector<Long> v = new Vector<Long>(OfficeComplexConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Office office: offices) {
        sum = sum + office.weeklyControllableLoad.get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the aggregated daily weather
   * sensitive Load of the office complex's offices for each quarter of the
   * hour.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyWeatherSensitiveLoad (int day, String type)
  {

    Vector<Office> offices = new Vector<Office>();

    if (type.equals("NS")) {
      offices = notShiftingoffices;
    }
    else {
      offices = smartShiftingoffices;
    }

    Vector<Long> v = new Vector<Long>(OfficeComplexConstants.QUARTERS_OF_DAY);
    long sum = 0;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Office office: offices) {
        sum = sum + office.weeklyWeatherSensitiveLoad.get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the daily Base Load of the office
   * for each hour for a certain type of offices.
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
      for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyBaseLoadNS.get(day)
                  .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                  + aggDailyBaseLoadNS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyBaseLoadNS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyBaseLoadNS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else {
      for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyBaseLoadSS.get(day)
                  .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                  + aggDailyBaseLoadSS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyBaseLoadSS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyBaseLoadSS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }

    return daily;
  }

  /**
   * This function is used in order to fill the daily Controllable Load of the
   * office for each hour for a certain type of offices.
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
      for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyControllableLoadNS.get(day)
                  .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                  + aggDailyControllableLoadNS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyControllableLoadNS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyControllableLoadNS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else {
      for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyControllableLoadSS.get(day)
                  .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                  + aggDailyControllableLoadSS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyControllableLoadSS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyControllableLoadSS.get(day)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }

    return daily;
  }

  /**
   * This function is used in order to fill the daily weather sensitive Load of
   * the office for each hour for a certain type of offices.
   * 
   * @param day
   * @param type
   * @return
   */
  Vector<Long> fillAggDailyWeatherSensitiveLoadInHours (int day, String type)
  {

    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);
    Vector<Long> daily = new Vector<Long>();
    long sum = 0;

    if (type.equals("NS")) {
      for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyWeatherSensitiveLoadNS.get(dayTemp)
                  .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                  + aggDailyWeatherSensitiveLoadNS.get(dayTemp)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyWeatherSensitiveLoadNS.get(dayTemp)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyWeatherSensitiveLoadNS.get(dayTemp)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }
    else {
      for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
        sum = 0;
        sum =
          aggDailyWeatherSensitiveLoadSS.get(dayTemp)
                  .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                  + aggDailyWeatherSensitiveLoadSS.get(dayTemp)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                  + aggDailyWeatherSensitiveLoadSS.get(dayTemp)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                  + aggDailyWeatherSensitiveLoadSS.get(dayTemp)
                          .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
        daily.add(sum);
      }
    }

    return daily;
  }

  /**
   * This function is used in order to print the aggregated hourly load of the
   * office complex's offices.
   * 
   * @param type
   * @return
   */
  void showAggLoad (String type)
  {

    log.info("Portion " + type + " Weekly Aggregated Load");

    if (type.equals("NS")) {
      for (int i = 0; i < OfficeComplexConstants.DAYS_OF_COMPETITION
                          + OfficeComplexConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.debug("Day " + i);
        for (int j = 0; j < OfficeComplexConstants.HOURS_OF_DAY; j++) {
          log.debug("Hour : " + j + " Base Load : "
                    + aggDailyBaseLoadInHoursNS.get(i).get(j)
                    + " Controllable Load: "
                    + aggDailyControllableLoadInHoursNS.get(i).get(j)
                    + " Weather Sensitive Load: "
                    + aggDailyWeatherSensitiveLoadInHoursNS.get(i).get(j));
        }
      }
    }
    else {
      for (int i = 0; i < OfficeComplexConstants.DAYS_OF_COMPETITION
                          + OfficeComplexConstants.DAYS_OF_BOOTSTRAP; i++) {
        log.debug("Day " + i);
        for (int j = 0; j < OfficeComplexConstants.HOURS_OF_DAY; j++) {
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
   * office complex offices for a certain type of offices.
   * 
   * @param type
   * @return
   */
  public void showAggDailyLoad (String type, int day)
  {

    log.debug("Portion " + type + " Daily Aggregated Load");
    log.debug("Day " + day);

    if (type.equals("NS")) {
      for (int j = 0; j < OfficeComplexConstants.HOURS_OF_DAY; j++) {
        log.debug("Hour : " + j + " Base Load : "
                  + aggDailyBaseLoadInHoursNS.get(day).get(j)
                  + " Controllable Load: "
                  + aggDailyControllableLoadInHoursNS.get(day).get(j)
                  + " Weather Sensitive Load: "
                  + aggDailyWeatherSensitiveLoadInHoursNS.get(day).get(j));
      }
    }
    else {
      for (int j = 0; j < OfficeComplexConstants.HOURS_OF_DAY; j++) {
        log.debug("Hour : " + j + " Base Load : "
                  + aggDailyBaseLoadInHoursSS.get(day).get(j)
                  + " Controllable Load: "
                  + aggDailyControllableLoadInHoursSS.get(day).get(j)
                  + " Weather Sensitive Load: "
                  + aggDailyWeatherSensitiveLoadInHoursSS.get(day).get(j));
      }
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
    // log.debug("Subscription Map : " + subscriptionMap.toString());
    // log.debug("Controllable Subscription Map : " +
    // controllableSubscriptionMap.toString());
    // log.debug("Subscription Keys : " + subscriptionMap.keySet());
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

    int day = (int) (serial / OfficeComplexConstants.HOURS_OF_DAY);
    int hour = (int) (serial % OfficeComplexConstants.HOURS_OF_DAY);
    long summary = 0;

    log.debug("Serial : " + serial + " Day: " + day + " Hour: " + hour);

    if (controllable)
      summary = getControllableConsumptions(day, hour, type);
    else
      summary =
        getBaseConsumptions(day, hour, type)
                + getWeatherSensitiveConsumptions(day, hour, type);

    return (double) summary / OfficeComplexConstants.THOUSAND;
  }

  // =====GETTER FUNCTIONS===== //

  /** This function returns the subscription Map variable of the office complex. */
  public HashMap<String, TariffSubscription> getSubscriptionMap ()
  {
    return subscriptionMap;
  }

  /** This function returns the subscription Map variable of the village. */
  public HashMap<String, TariffSubscription> getControllableSubscriptionMap ()
  {
    return controllableSubscriptionMap;
  }

  /** This function returns the inertia Map variable of the office complex. */
  public HashMap<String, Double> getInertiaMap ()
  {
    return inertiaMap;
  }

  /** This function returns the period Map variable of the office complex. */
  public HashMap<String, Integer> getPeriodMap ()
  {
    return periodMap;
  }

  /**
   * This function returns the quantity of base load for a specific day and hour
   * of that day for a specific type of offices.
   */
  long getBaseConsumptions (int day, int hour, String type)
  {
    long summaryBase = 0;
    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      summaryBase = aggDailyBaseLoadInHoursNS.get(dayTemp).get(hour);
    }
    else {
      summaryBase = aggDailyBaseLoadInHoursSS.get(dayTemp).get(hour);
    }

    log.debug("Base Load for " + type + ":" + summaryBase);
    return summaryBase;
  }

  /**
   * This function returns the quantity of controllable load for a specific day
   * and hour of that day for a specific type of offices.
   */
  long getControllableConsumptions (int day, int hour, String type)
  {
    long summaryControllable = 0;
    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      summaryControllable =
        aggDailyControllableLoadInHoursNS.get(dayTemp).get(hour);
    }
    else {
      summaryControllable =
        aggDailyControllableLoadInHoursSS.get(dayTemp).get(hour);
    }

    log.debug("Controllable Load for " + type + ":" + summaryControllable);
    return summaryControllable;
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
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      before = aggDailyControllableLoadInHoursNS.get(dayTemp).get(hour);
      aggDailyControllableLoadInHoursNS.get(dayTemp)
              .set(hour, before + curtail);
      after = aggDailyControllableLoadInHoursNS.get(dayTemp).get(hour);
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
   * This function returns the quantity of weather sensitive load for a specific
   * day and hour of that day for a specific type of office.
   */
  long getWeatherSensitiveConsumptions (int day, int hour, String type)
  {
    long summaryWeatherSensitive = 0;
    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      summaryWeatherSensitive =
        aggDailyWeatherSensitiveLoadInHoursNS.get(dayTemp).get(hour);
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
   * This function returns the quantity of controllable load for a specific day
   * in form of a vector for a certain type of offices.
   */
  Vector<Long> getControllableConsumptions (int day, String type)
  {

    Vector<Long> controllableVector = new Vector<Long>();
    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      controllableVector = aggDailyControllableLoadInHoursNS.get(dayTemp);
    }
    else {
      controllableVector = aggDailyControllableLoadInHoursSS.get(dayTemp);
    }

    return controllableVector;
  }

  /**
   * This function returns the quantity of weather sensitive load for a specific
   * day in form of a vector for a certain type of offices.
   */
  Vector<Long> getWeatherSensitiveConsumptions (int day, String type)
  {

    Vector<Long> weatherSensitiveVector = new Vector<Long>();
    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      weatherSensitiveVector =
        aggDailyWeatherSensitiveLoadInHoursNS.get(dayTemp);
    }
    else {
      weatherSensitiveVector =
        aggDailyWeatherSensitiveLoadInHoursSS.get(dayTemp);
    }

    return weatherSensitiveVector;
  }

  /**
   * This function returns a vector with all the offices that are present in
   * this office complex.
   */
  public Vector<Office> getOffices ()
  {

    Vector<Office> offices = new Vector<Office>();

    for (Office office: notShiftingoffices)
      offices.add(office);
    for (Office office: smartShiftingoffices)
      offices.add(office);

    return offices;

  }

  /**
   * This function returns a vector with all the offices of a certain type that
   * are present in this office complex.
   */
  public Vector<Office> getOffices (String type)
  {

    Vector<Office> offices = new Vector<Office>();

    if (type.equals("NS")) {
      for (Office office: notShiftingoffices) {
        offices.add(office);
      }
    }
    else {
      for (Office office: smartShiftingoffices) {
        offices.add(office);
      }
    }

    return offices;

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

      Vector<Double> estimation = new Vector<Double>();

      // getting the active tariffs for evaluation
      ArrayList<Tariff> evaluationTariffs = new ArrayList<Tariff>(newTariffs);

      log.debug("Estimation size for " + this.toString() + " = "
                + evaluationTariffs.size());

      if (evaluationTariffs.size() > 1) {
        for (Tariff tariff: evaluationTariffs) {
          log.debug("Tariff : " + tariff.toString() + " Tariff Type : "
                    + tariff.getTariffSpecification().getPowerType());
          if (tariff.isExpired() == false
              && (tariff.getTariffSpecification().getPowerType() == customer
                      .getPowerType() || (customer.getPowerType() == PowerType.INTERRUPTIBLE_CONSUMPTION && tariff
                      .getTariffSpecification().getPowerType() == PowerType.CONSUMPTION))) {
            estimation.add(-(costEstimation(tariff, type)));
          }
          else
            estimation.add(Double.NEGATIVE_INFINITY);
        }

        int minIndex = logitPossibilityEstimation(estimation, type);

        TariffSubscription sub = subscriptionMap.get(type);

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
  double costEstimation (Tariff tariff, String type)
  {
    double costVariable = 0;

    /* if it is NotShifting offices the evaluation is done without shifting devices 
       if it is RandomShifting offices the evaluation is may be done without shifting devices or maybe shifting will be taken into consideration
       In any other case shifting will be done. */
    if (type.equals("NS")) {
      // System.out.println("Simple Evaluation for " + type);
      log.debug("Simple Evaluation for " + type);
      costVariable = estimateVariableTariffPayment(tariff, type);
    }
    else if (type.equals("RaS")) {
      Double rand = gen.nextDouble();
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

    double costFixed = estimateFixedTariffPayments(tariff);
    return (costVariable + costFixed) / OfficeComplexConstants.MILLION;
  }

  /**
   * This function estimates the fixed cost, comprised by fees, bonuses and
   * penalties that are the same no matter how much you consume
   */
  double estimateFixedTariffPayments (Tariff tariff)
  {
    double lifecyclePayment =
      -tariff.getEarlyWithdrawPayment() - tariff.getSignupPayment();
    double minDuration;

    // When there is not a Minimum Duration of the contract, you cannot divide
    // with the duration
    // because you don't know it.
    if (tariff.getMinDuration() == 0)
      minDuration = OfficeComplexConstants.MEAN_TARIFF_DURATION;
    else
      minDuration = tariff.getMinDuration() / TimeService.DAY;

    log.debug("Minimum Duration: " + minDuration);
    return ((-tariff.getPeriodicPayment() * OfficeComplexConstants.HOURS_OF_DAY) + (lifecyclePayment / minDuration));
  }

  /**
   * This function estimates the variable cost, depending only to the load
   * quantity you consume
   */
  double estimateVariableTariffPayment (Tariff tariff, String type)
  {

    double finalCostSummary = 0;

    int serial =
      (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    Instant base =
      new Instant(timeService.getCurrentTime().getMillis() - serial
                  * TimeService.HOUR);
    int daylimit = (int) (serial / OfficeComplexConstants.HOURS_OF_DAY) + 1;

    for (int day: daysList) {
      if (day < daylimit)
        day =
          (int) (day + (daylimit / OfficeComplexConstants.RANDOM_DAYS_NUMBER));
      Instant now = base.plus(day * TimeService.DAY);
      double costSummary = 0;
      double summary = 0, cumulativeSummary = 0;

      for (int hour = 0; hour < OfficeComplexConstants.HOURS_OF_DAY; hour++) {

        summary =
          getBaseConsumptions(day, hour, type)
                  + getControllableConsumptions(day, hour, type);

        log.debug("Cost for hour " + hour + ":"
                  + tariff.getUsageCharge(now, 1, 0));
        cumulativeSummary += summary;
        costSummary -= tariff.getUsageCharge(now, summary, cumulativeSummary);
        now = now.plus(TimeService.HOUR);
      }
      log.debug("Variable Cost Summary: " + finalCostSummary);
      finalCostSummary += costSummary;
    }
    return finalCostSummary / OfficeComplexConstants.RANDOM_DAYS_NUMBER;
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

    int serial =
      (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR);
    Instant base =
      timeService.getCurrentTime().minus(serial * TimeService.HOUR);
    int daylimit = (int) (serial / OfficeComplexConstants.HOURS_OF_DAY) + 1;

    for (int day: daysList) {
      if (day < daylimit)
        day =
          (int) (day + (daylimit / OfficeComplexConstants.RANDOM_DAYS_NUMBER));
      Instant now = base.plus(day * TimeService.DAY);
      double costSummary = 0;
      double summary = 0, cumulativeSummary = 0;

      long[] newControllableLoad = dailyShifting(tariff, now, day, type);

      for (int hour = 0; hour < OfficeComplexConstants.HOURS_OF_DAY; hour++) {
        summary =
          getBaseConsumptions(day, hour, type) + newControllableLoad[hour];
        cumulativeSummary += summary;
        costSummary -= tariff.getUsageCharge(now, summary, cumulativeSummary);
        now = now.plus(TimeService.HOUR);
      }
      log.debug("Variable Cost Summary: " + finalCostSummary);
      finalCostSummary += costSummary;
    }
    return finalCostSummary / OfficeComplexConstants.RANDOM_DAYS_NUMBER;
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
      summedEstimations +=
        Math.pow(OfficeComplexConstants.EPSILON, lamda * estimation.get(i));
      log.debug("Cost variable: " + estimation.get(i));
      log.debug("Summary of Estimation: " + summedEstimations);
    }

    for (int i = 0; i < estimation.size(); i++) {
      possibilities
              .add((int) (OfficeComplexConstants.PERCENTAGE * (Math
                      .pow(OfficeComplexConstants.EPSILON,
                           lamda * estimation.get(i)) / summedEstimations)));
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
   * This is the function that takes every office in the office complex and
   * reads the shifted Controllable Consumption for the needs of the tariff
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

    long[] newControllableLoad = new long[OfficeComplexConstants.HOURS_OF_DAY];
    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);

    Vector<Office> offices = new Vector<Office>();

    if (type.equals("NS")) {
      offices = notShiftingoffices;
    }
    else {
      offices = smartShiftingoffices;
    }

    for (Office office: offices) {
      long[] temp = office.dailyShifting(tariff, now, dayTemp, gen);
      for (int j = 0; j < OfficeComplexConstants.HOURS_OF_DAY; j++)
        newControllableLoad[j] += temp[j];
    }

    log.debug("New Controllable Load of OfficeComplex " + toString() + " type "
              + type + " for Tariff " + tariff.toString());

    for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
      log.debug("Hour: " + i + " Cost: " + tariff.getUsageCharge(now, 1, 0)
                + " Load For Type " + type + " : " + newControllableLoad[i]);
      now = new Instant(now.getMillis() + TimeService.HOUR);
    }
    return newControllableLoad;
  }

  // =====STATUS FUNCTIONS===== //

  /**
   * This function prints to the screen the daily load of the office complex's
   * offices for the weekday at hand.
   * 
   * @param day
   * @param type
   * @return
   */
  void printDailyLoad (int day, String type)
  {

    Vector<Office> offices = new Vector<Office>();
    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);

    if (type.equals("NS")) {
      offices = notShiftingoffices;
    }
    else {
      offices = smartShiftingoffices;
    }

    log.debug("Day " + day);

    for (Office office: offices) {
      office.printDailyLoad(dayTemp);
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
      int x =
        gen.nextInt(OfficeComplexConstants.DAYS_OF_COMPETITION
                    + OfficeComplexConstants.DAYS_OF_BOOTSTRAP);
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
   * This function is creating the list of days for each office complex that
   * will be utilized for the tariff evaluation.
   * 
   * @param days
   * @param gen
   * @return
   */
  void createCostEstimationDaysList (int days)
  {

    for (int i = 0; i < days; i++) {
      int x =
        gen.nextInt(OfficeComplexConstants.DAYS_OF_COMPETITION
                    + OfficeComplexConstants.DAYS_OF_BOOTSTRAP);
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
    int day = (int) (serial / OfficeComplexConstants.HOURS_OF_DAY);
    int hour = timeService.getHourOfDay();
    Instant now = new Instant(timeService.getCurrentTime().getMillis());

    weatherCheck(day, hour, now);

    checkRevokedSubscriptions();

    checkCurtailment(serial, day, hour);

    consumePower();

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
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);
    WeatherReport wr = weatherReportRepo.currentWeatherReport();
    if (wr != null) {
      double temperature = wr.getTemperature();
      // log.debug("Temperature: " + temperature);

      Vector<Office> offices = getOffices();

      for (Office office: offices) {
        office.weatherCheck(dayTemp, hour, now, temperature);
      }

      for (String type: subscriptionMap.keySet()) {
        updateAggDailyWeatherSensitiveLoad(type, day);
        if (dayTemp + 1 < OfficeComplexConstants.DAYS_OF_COMPETITION) {
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
    int nextDay = (int) (nextSerial / OfficeComplexConstants.HOURS_OF_DAY);
    int nextHour = (int) (nextSerial % OfficeComplexConstants.HOURS_OF_DAY);

    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);
    int nextDayTemp =
      nextDay
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);

    Collection<TariffSubscription> tempCol =
      controllableSubscriptionMap.values();
    ArrayList<TariffSubscription> subs = new ArrayList<TariffSubscription>();

    for (TariffSubscription sub: tempCol)
      if (!subs.contains(sub))
        subs.add(sub);

    log.debug(this.toString() + " " + subs.toString());

    for (TariffSubscription sub: subs) {

      long curt = (long) sub.getCurtailment() * OfficeComplexConstants.THOUSAND;
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
    int day = (int) (serial / OfficeComplexConstants.HOURS_OF_DAY) + 1;
    Instant now =
      new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR);

    int dayTemp =
      day
              % (OfficeComplexConstants.DAYS_OF_BOOTSTRAP + OfficeComplexConstants.DAYS_OF_COMPETITION);

    Vector<Long> controllableVector = new Vector<Long>();

    TariffSubscription sub = subscriptionMap.get(type);

    log.debug("Old Consumption for day " + day + ": "
              + getControllableConsumptions(dayTemp, type).toString());
    long[] newControllableLoad =
      dailyShifting(sub.getTariff(), now, dayTemp, type);
    for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++)
      controllableVector.add(newControllableLoad[i]);
    log.debug("New Consumption for day " + day + ": "
              + controllableVector.toString());

    aggDailyControllableLoadInHoursSS.set(dayTemp, controllableVector);

  }

  @Override
  public String toString ()
  {
    return name;
  }

}
