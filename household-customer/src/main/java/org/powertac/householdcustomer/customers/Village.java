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
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.Instant;
import org.powertac.common.CapacityProfile;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluationHelper;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.customer.AbstractCustomer;
import org.powertac.householdcustomer.configurations.VillageConstants;

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
  static protected Logger log = LogManager.getLogger(Village.class.getName());

  int seedId = 1;

  /**
   * These are the vectors containing aggregated each day's base load from the
   * appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyBaseLoadNS = new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyBaseLoadRaS = new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyBaseLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's controllable load
   * from the appliances installed inside the households.
   **/
  Vector<Vector<Long>> aggDailyControllableLoadNS = new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyControllableLoadRaS = new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyControllableLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's weather sensitive
   * load from the appliances installed inside the households.
   **/
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadNS =
    new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadRaS =
//    new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadReS =
//    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadSS =
    new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's dominant load from
   * the appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyDominantLoadNS = new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyDominantLoadRaS = new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyDominantLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyDominantLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's non dominant load
   * from the appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyNonDominantLoadNS = new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyNonDominantLoadRaS = new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyNonDominantLoadReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyNonDominantLoadSS = new Vector<Vector<Long>>();

  /**
   * These are the aggregated vectors containing each day's base load of all the
   * households in hours.
   **/
  Vector<Vector<Long>> aggDailyBaseLoadInHoursNS = new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyBaseLoadInHoursRaS = new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyBaseLoadInHoursReS = new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyBaseLoadInHoursSS = new Vector<Vector<Long>>();

  /**
   * These are the aggregated vectors containing each day's controllable load of
   * all the households in hours.
   **/
  Vector<Vector<Long>> aggDailyControllableLoadInHoursNS =
    new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyControllableLoadInHoursRaS =
//    new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyControllableLoadInHoursReS =
//    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyControllableLoadInHoursSS =
    new Vector<Vector<Long>>();

  /**
   * These are the aggregated vectors containing each day's weather sensitive
   * load of all the households in hours.
   **/
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursNS =
    new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursRaS =
//    new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursReS =
//    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyWeatherSensitiveLoadInHoursSS =
    new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's dominant load from
   * the appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyDominantLoadInHoursNS =
    new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyDominantLoadInHoursRaS =
//    new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyDominantLoadInHoursReS =
//    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyDominantLoadInHoursSS =
    new Vector<Vector<Long>>();

  /**
   * These are the vectors containing aggregated each day's non dominant load
   * from the appliances installed inside the households of each type.
   **/
  Vector<Vector<Long>> aggDailyNonDominantLoadInHoursNS =
    new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyNonDominantLoadInHoursRaS =
//    new Vector<Vector<Long>>();
//  Vector<Vector<Long>> aggDailyNonDominantLoadInHoursReS =
//    new Vector<Vector<Long>>();
  Vector<Vector<Long>> aggDailyNonDominantLoadInHoursSS =
    new Vector<Vector<Long>>();

  /**
   * These are the mean consumption of the village types for the days with the
   * dominant appliances working.
   **/
  double[] dominantLoadNS = new double[VillageConstants.HOURS_OF_DAY];
//  double[] dominantLoadRaS = new double[VillageConstants.HOURS_OF_DAY];
//  double[] dominantLoadReS = new double[VillageConstants.HOURS_OF_DAY];
  double[] dominantLoadSS = new double[VillageConstants.HOURS_OF_DAY];

  /**
   * These are the mean consumption of the village types for the days with the
   * dominant appliances not working.
   **/
  double[] nonDominantLoadNS = new double[VillageConstants.HOURS_OF_DAY];
//  double[] nonDominantLoadRaS = new double[VillageConstants.HOURS_OF_DAY];
//  double[] nonDominantLoadReS = new double[VillageConstants.HOURS_OF_DAY];
  double[] nonDominantLoadSS = new double[VillageConstants.HOURS_OF_DAY];

  // presumably this is not used for actual tariff evaluation
  protected final TariffEvaluationHelper tariffEvalHelper =
    new TariffEvaluationHelper();

  /**
   * This variable is utilized for the creation of the RandomSeed numbers and is
   * taken from the service.
   */
  RandomSeed gen;

  /**
   * These variables are mapping of the characteristics of the types of houses.
   */
  Map<String, Integer> numberOfHouses = new TreeMap<String, Integer>();
  Map<CustomerInfo, TariffEvaluator> tariffEvaluators;
  Map<CustomerInfo, String> houseMapping = new TreeMap<CustomerInfo, String>();

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
  //Vector<Household> randomlyShiftingHouses = new Vector<Household>();
  //Vector<Household> regularlyShiftingHouses = new Vector<Household>();
  Vector<Household> smartShiftingHouses = new Vector<Household>();

  /** This is the constructor function of the Village customer */
  public Village (String name)
  {
    super(name);

    ArrayList<String> typeList = new ArrayList<String>();
    typeList.add("NS");
    //typeList.add("RaS");
    //typeList.add("ReS");
    typeList.add("SS");

    Comparator<CustomerInfo> comp = new Comparator<CustomerInfo>() {
      @Override
      public int compare (CustomerInfo customer1, CustomerInfo customer2)
      {
        return customer1.getName().compareToIgnoreCase(customer2.getName());
      }
    };

    for (String type: typeList) {
      numberOfHouses.put(type, null);
      tariffEvaluators = new TreeMap<CustomerInfo, TariffEvaluator>(comp);
    }
  }

  @Override
  public void initialize ()
  {
    super.initialize();
  }

  /**
   * This is the initialization function. It uses the variable values for the
   * configuration file to create the village with its households and then fill
   * them with persons and appliances.
   * 
   * @param conf
   * @param seed
   * @param mapping
   */
  public void initialize (Properties conf, int seed,
                          Map<CustomerInfo, String> mapping)
  {
    super.initialize();
    // Initializing variables
    houseMapping = mapping;

    numberOfHouses.put("NS", Integer.parseInt(conf
            .getProperty("NotShiftingCustomers")));
    //numberOfHouses.put("RaS", Integer.parseInt(conf
    //        .getProperty("RegularlyShiftingCustomers")));
    //numberOfHouses.put("ReS", Integer.parseInt(conf
    //        .getProperty("RandomlyShiftingCustomers")));
    numberOfHouses.put("SS", Integer.parseInt(conf
            .getProperty("SmartShiftingCustomers")));
    int days = Integer.parseInt(conf.getProperty("PublicVacationDuration"));

    gen = service.getRandomSeedRepo()
        .getRandomSeed(toString(), seed, "Village Model" + seed);

    Vector<Integer> publicVacationVector = createPublicVacationVector(days);

    for (int i = 0; i < numberOfHouses.get("NS"); i++) {
      log.info("Initializing " + toString() + " NSHouse " + i);
      Household hh = new Household();
      hh.initialize(toString() + " NSHouse" + i, conf, publicVacationVector,
                    seedId++);
      notShiftingHouses.add(hh);
      hh.householdOf = this;
    }

//    for (int i = 0; i < numberOfHouses.get("RaS"); i++) {
//      log.info("Initializing " + toString() + " RaSHouse " + i);
//      Household hh = new Household();
//      hh.initialize(toString() + " RaSHouse" + i, conf, publicVacationVector,
//                    seedId++);
//      randomlyShiftingHouses.add(hh);
//      hh.householdOf = this;
//    }
//
//    for (int i = 0; i < numberOfHouses.get("ReS"); i++) {
//      log.info("Initializing " + toString() + " ReSHouse " + i);
//      Household hh = new Household();
//      hh.initialize(toString() + " ReSHouse" + i, conf, publicVacationVector,
//                    seedId++);
//      regularlyShiftingHouses.add(hh);
//      hh.householdOf = this;
//    }

    for (int i = 0; i < numberOfHouses.get("SS"); i++) {
      log.info("Initializing " + toString() + " SSHouse " + i);
      Household hh = new Household();
      hh.initialize(toString() + " SSHouse" + i, conf, publicVacationVector,
                    seedId++);
      smartShiftingHouses.add(hh);
      hh.householdOf = this;
    }

    for (String type: numberOfHouses.keySet()) {
      fillAggWeeklyLoad(type);

      double weight = gen.nextDouble() * VillageConstants.WEIGHT_INCONVENIENCE;

      double weeks =
        gen.nextInt(VillageConstants.MAX_DEFAULT_DURATION
                    - VillageConstants.MIN_DEFAULT_DURATION)
                + VillageConstants.MIN_DEFAULT_DURATION;

      // TODO - Fragile use of arbitrary naming convention. Perhaps Village
      // should keep track of its own Customer instances?
      List<CustomerInfo> customer =
        service.getCustomerRepo().findByName(name + " " + type + " Base");

      TariffEvaluationWrapper wrapper =
        new TariffEvaluationWrapper(type, customer.get(0));

      TariffEvaluator te = createTariffEvaluator(wrapper);

      te.initializeInconvenienceFactors(VillageConstants.TOU_FACTOR,
                                        VillageConstants.TIERED_RATE_FACTOR,
                                        VillageConstants.VARIABLE_PRICING_FACTOR,
                                        VillageConstants.INTERRUPTIBILITY_FACTOR);
      te.withInconvenienceWeight(weight)
              .withInertia(Double.parseDouble(conf
                                   .getProperty(type + "Inertia")))
              .withPreferredContractDuration(weeks
                                             * VillageConstants.DAYS_OF_WEEK)
              .withRationality(Double.parseDouble(conf
                                       .getProperty(type + "Rationality")))
              .withTariffEvalDepth(VillageConstants.TARIFF_COUNT)
              .withTariffSwitchFactor(VillageConstants.TARIFF_SWITCH_FACTOR);

      tariffEvaluators.put(customer.get(0), te);

      customer = service.getCustomerRepo()
          .findByName(name + " " + type + " Controllable");

      wrapper = new TariffEvaluationWrapper(type, customer.get(0));

      te = createTariffEvaluator(wrapper);

      te.initializeInconvenienceFactors(VillageConstants.TOU_FACTOR,
                                        VillageConstants.TIERED_RATE_FACTOR,
                                        VillageConstants.VARIABLE_PRICING_FACTOR,
                                        VillageConstants.INTERRUPTIBILITY_FACTOR);
      te.withInconvenienceWeight(weight)
              .withInertia(Double.parseDouble(conf
                                   .getProperty(type + "Inertia")))
              .withPreferredContractDuration(weeks
                                                     * VillageConstants.DAYS_OF_WEEK)
              .withRationality(Double.parseDouble(conf
                                       .getProperty(type + "Rationality")))
              .withTariffEvalDepth(VillageConstants.TARIFF_COUNT)
              .withTariffSwitchFactor(VillageConstants.TARIFF_SWITCH_FACTOR);

      tariffEvaluators.put(customer.get(0), te);
    }
  }

  // =====SUBSCRIPTION FUNCTIONS===== //

  //@Override
  public void subscribeDefault (TariffMarket tariffMarketService)
  {
    for (CustomerInfo customer: getCustomerInfos()) {
      Tariff candidate =
          tariffMarketService.getDefaultTariff(customer.getPowerType());
      if (null == candidate) {
        log.error("No default tariff for " + customer.getPowerType().toString());
      }
      else {
        log.info("Subscribe " + customer.getName()
                 + " to " + candidate.getPowerType().toString());
      }
      tariffMarketService.subscribeToTariff(candidate, customer,
                                            customer.getPopulation());
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
//    else if (type.equals("RaS")) {
//      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK
//                          * (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP); i++) {
//        aggDailyBaseLoadRaS.add(fillAggDailyBaseLoad(i, type));
//        aggDailyControllableLoadRaS.add(fillAggDailyControllableLoad(i, type));
//        aggDailyWeatherSensitiveLoadRaS
//                .add(fillAggDailyWeatherSensitiveLoad(i, type));
//        aggDailyBaseLoadInHoursRaS.add(fillAggDailyBaseLoadInHours(i, type));
//        aggDailyControllableLoadInHoursRaS
//                .add(fillAggDailyControllableLoadInHours(i, type));
//        aggDailyWeatherSensitiveLoadInHoursRaS
//                .add(fillAggDailyWeatherSensitiveLoadInHours(i, type));
//
//        aggDailyDominantLoadRaS.add(fillAggDailyDominantLoad(i, type));
//        aggDailyNonDominantLoadRaS.add(fillAggDailyNonDominantLoad(i, type));
//        aggDailyDominantLoadInHoursRaS
//                .add(fillAggDailyDominantLoadInHours(i, type));
//        aggDailyNonDominantLoadInHoursRaS
//                .add(fillAggDailyNonDominantLoadInHours(i, type));
//      }
//    }
//    else if (type.equals("ReS")) {
//      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK
//                          * (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP); i++) {
//        aggDailyBaseLoadReS.add(fillAggDailyBaseLoad(i, type));
//        aggDailyControllableLoadReS.add(fillAggDailyControllableLoad(i, type));
//        aggDailyWeatherSensitiveLoadReS
//                .add(fillAggDailyWeatherSensitiveLoad(i, type));
//        aggDailyBaseLoadInHoursReS.add(fillAggDailyBaseLoadInHours(i, type));
//        aggDailyControllableLoadInHoursReS
//                .add(fillAggDailyControllableLoadInHours(i, type));
//        aggDailyWeatherSensitiveLoadInHoursReS
//                .add(fillAggDailyWeatherSensitiveLoadInHours(i, type));
//
//        aggDailyDominantLoadReS.add(fillAggDailyDominantLoad(i, type));
//        aggDailyNonDominantLoadReS.add(fillAggDailyNonDominantLoad(i, type));
//        aggDailyDominantLoadInHoursReS
//                .add(fillAggDailyDominantLoadInHours(i, type));
//        aggDailyNonDominantLoadInHoursReS
//                .add(fillAggDailyNonDominantLoadInHours(i, type));
//      }
//    }
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
//    else if (type.equals("RaS")) {
//
//      dominantLoadRaS = dominant;
//      nonDominantLoadRaS = nonDominant;
//    }
//    else if (type.equals("ReS")) {
//
//      dominantLoadReS = dominant;
//      nonDominantLoadReS = nonDominant;
//
//    }
    else {

      dominantLoadSS = dominant;
      nonDominantLoadSS = nonDominant;

    }
  }

  private double[] getDominantLoad (String type)
  {

    if (type.equals("NS"))
      return dominantLoadNS;
//    else if (type.equals("RaS"))
//      return dominantLoadRaS;
//    else if (type.equals("ReS"))
//      return dominantLoadReS;
    else
      return dominantLoadSS;

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
//    else if (type.equals("RaS")) {
//      aggDailyWeatherSensitiveLoadRaS
//              .set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
//      aggDailyWeatherSensitiveLoadInHoursRaS
//              .set(dayTemp,
//                   fillAggDailyWeatherSensitiveLoadInHours(dayTemp, type));
//    }
//    else if (type.equals("ReS")) {
//      aggDailyWeatherSensitiveLoadReS
//              .set(dayTemp, fillAggDailyWeatherSensitiveLoad(dayTemp, type));
//      aggDailyWeatherSensitiveLoadInHoursReS
//              .set(dayTemp,
//                   fillAggDailyWeatherSensitiveLoadInHours(dayTemp, type));
//    }
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
//    else if (type.equals("RaS")) {
//      houses = randomlyShiftingHouses;
//    }
//    else if (type.equals("ReS")) {
//      houses = regularlyShiftingHouses;
//    }
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
//    else if (type.equals("RaS")) {
//      houses = randomlyShiftingHouses;
//    }
//    else if (type.equals("ReS")) {
//      houses = regularlyShiftingHouses;
//    }
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
//    else if (type.equals("RaS")) {
//      houses = randomlyShiftingHouses;
//    }
//    else if (type.equals("ReS")) {
//      houses = regularlyShiftingHouses;
//    }
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
//    else if (type.equals("RaS")) {
//      houses = randomlyShiftingHouses;
//    }
//    else if (type.equals("ReS")) {
//      houses = regularlyShiftingHouses;
//    }
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
//    else if (type.equals("RaS")) {
//      houses = randomlyShiftingHouses;
//    }
//    else if (type.equals("ReS")) {
//      houses = regularlyShiftingHouses;
//    }
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
//    else if (type.equals("RaS")) {
//      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
//        sum = 0;
//        sum =
//          aggDailyBaseLoadRaS.get(day)
//                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
//                  + aggDailyBaseLoadRaS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
//                  + aggDailyBaseLoadRaS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
//                  + aggDailyBaseLoadRaS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
//        daily.add(sum);
//      }
//    }
//    else if (type.equals("ReS")) {
//      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
//        sum = 0;
//        sum =
//          aggDailyBaseLoadReS.get(day)
//                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
//                  + aggDailyBaseLoadReS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
//                  + aggDailyBaseLoadReS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
//                  + aggDailyBaseLoadReS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
//        daily.add(sum);
//      }
//    }
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
//    else if (type.equals("RaS")) {
//      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
//        sum = 0;
//        sum =
//          aggDailyControllableLoadRaS.get(day)
//                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
//                  + aggDailyControllableLoadRaS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
//                  + aggDailyControllableLoadRaS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
//                  + aggDailyControllableLoadRaS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
//        daily.add(sum);
//      }
//    }
//    else if (type.equals("ReS")) {
//      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
//        sum = 0;
//        sum =
//          aggDailyControllableLoadReS.get(day)
//                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
//                  + aggDailyControllableLoadReS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
//                  + aggDailyControllableLoadReS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
//                  + aggDailyControllableLoadReS.get(day)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
//        daily.add(sum);
//      }
//    }
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
//    else if (type.equals("RaS")) {
//      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
//        sum = 0;
//        sum =
//          aggDailyWeatherSensitiveLoadRaS.get(dayTemp)
//                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
//                  + aggDailyWeatherSensitiveLoadRaS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
//                  + aggDailyWeatherSensitiveLoadRaS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
//                  + aggDailyWeatherSensitiveLoadRaS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
//        daily.add(sum);
//      }
//    }
//    else if (type.equals("ReS")) {
//      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
//        sum = 0;
//        sum =
//          aggDailyWeatherSensitiveLoadReS.get(dayTemp)
//                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
//                  + aggDailyWeatherSensitiveLoadReS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
//                  + aggDailyWeatherSensitiveLoadReS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
//                  + aggDailyWeatherSensitiveLoadReS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
//        daily.add(sum);
//      }
//    }
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
//    else if (type.equals("RaS")) {
//      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
//        sum = 0;
//        sum =
//          aggDailyDominantLoadRaS.get(dayTemp)
//                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
//                  + aggDailyDominantLoadRaS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
//                  + aggDailyDominantLoadRaS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
//                  + aggDailyDominantLoadRaS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
//        daily.add(sum);
//      }
//    }
//    else if (type.equals("ReS")) {
//      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
//        sum = 0;
//        sum =
//          aggDailyDominantLoadReS.get(dayTemp)
//                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
//                  + aggDailyDominantLoadReS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
//                  + aggDailyDominantLoadReS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
//                  + aggDailyDominantLoadReS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
//        daily.add(sum);
//      }
//    }
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
//    else if (type.equals("RaS")) {
//      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
//        sum = 0;
//        sum =
//          aggDailyNonDominantLoadRaS.get(dayTemp)
//                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
//                  + aggDailyNonDominantLoadRaS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
//                  + aggDailyNonDominantLoadRaS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
//                  + aggDailyNonDominantLoadRaS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
//        daily.add(sum);
//      }
//    }
//    else if (type.equals("ReS")) {
//      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
//        sum = 0;
//        sum =
//          aggDailyNonDominantLoadReS.get(dayTemp)
//                  .get(i * VillageConstants.QUARTERS_OF_HOUR)
//                  + aggDailyNonDominantLoadReS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
//                  + aggDailyNonDominantLoadReS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
//                  + aggDailyNonDominantLoadReS.get(dayTemp)
//                          .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
//        daily.add(sum);
//      }
//    }
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

  // // =====CONSUMPTION FUNCTIONS===== //

  public void consumePower ()
  {
    Timeslot ts = service.getTimeslotRepo().currentTimeslot();
    int serial;

    for (CustomerInfo customer: getCustomerInfos()) {

      List<TariffSubscription> subscriptions =
        service.getTariffSubscriptionRepo()
        .findActiveSubscriptionsForCustomer(customer);

      String temp = houseMapping.get(customer);

      String type = temp.substring(0, 2);

      boolean controllable = temp.contains("Controllable");

      if (ts == null) {
        log.error("Current timeslot is null");
        serial = 0;
      }
      else {
        log.debug("Timeslot Serial: " + ts.getSerialNumber());
        serial = ts.getSerialNumber();
      }

      double load = getConsumptionByTimeslot(serial, type, controllable);

      log.debug("Consumption Load for Customer " + customer.toString() + ": "
                + load + " for subscriptions " + subscriptions.toString());

      if (subscriptions == null || subscriptions.size() < 1) {
        log.error("No subscriptions for customer {}", customer.toString());
      }
      else if (subscriptions.size() > 1) {
        log.error("Multiple subscriptions for customer {}", customer.toString());
      }
      else {
        subscriptions.get(0).usePower(load);
      }
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

  /** This function returns the inertia Map variable of the village. */
  public Map<CustomerInfo, String> getHouseMapping ()
  {
    return houseMapping;
  }

  /** This function returns the period Map variable of the village. */
  public Map<CustomerInfo, TariffEvaluator> getTariffEvaluators ()
  {
    return tariffEvaluators;
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
//    else if (type.equals("RaS")) {
//      summaryBase = aggDailyBaseLoadInHoursRaS.get(dayTemp).get(hour);
//    }
//    else if (type.equals("ReS")) {
//      summaryBase = aggDailyBaseLoadInHoursReS.get(dayTemp).get(hour);
//    }
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
//    else if (type.equals("RaS")) {
//      summaryControllable =
//        aggDailyControllableLoadInHoursRaS.get(dayTemp).get(hour);
//    }
//    else if (type.equals("ReS")) {
//      summaryControllable =
//        aggDailyControllableLoadInHoursReS.get(dayTemp).get(hour);
//    }
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
//    else if (type.equals("RaS")) {
//      summaryNonDominant =
//        aggDailyNonDominantLoadInHoursRaS.get(dayTemp).get(hour);
//    }
//    else if (type.equals("ReS")) {
//      summaryNonDominant =
//        aggDailyNonDominantLoadInHoursReS.get(dayTemp).get(hour);
//    }
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
//    else if (type.equals("RaS")) {
//      summaryWeatherSensitive =
//        aggDailyWeatherSensitiveLoadInHoursRaS.get(dayTemp).get(hour);
//    }
//    else if (type.equals("ReS")) {
//      summaryWeatherSensitive =
//        aggDailyWeatherSensitiveLoadInHoursReS.get(dayTemp).get(hour);
//    }
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
//    else if (type.equals("RaS")) {
//      before = aggDailyControllableLoadInHoursRaS.get(dayTemp).get(hour);
//      aggDailyControllableLoadInHoursRaS.get(dayTemp).set(hour,
//                                                          before + curtail);
//      after = aggDailyControllableLoadInHoursRaS.get(dayTemp).get(hour);
//    }
//    else if (type.equals("ReS")) {
//      before = aggDailyControllableLoadInHoursReS.get(dayTemp).get(hour);
//      aggDailyControllableLoadInHoursReS.get(dayTemp).set(hour,
//                                                          before + curtail);
//      after = aggDailyControllableLoadInHoursReS.get(dayTemp).get(hour);
//    }
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
//    else if (type.equals("RaS")) {
//      controllableVector = aggDailyControllableLoadInHoursRaS.get(dayTemp);
//    }
//    else if (type.equals("ReS")) {
//      controllableVector = aggDailyControllableLoadInHoursReS.get(dayTemp);
//    }
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
//    else if (type.equals("RaS")) {
//      weatherSensitiveVector =
//        aggDailyWeatherSensitiveLoadInHoursRaS.get(dayTemp);
//    }
//    else if (type.equals("ReS")) {
//      weatherSensitiveVector =
//        aggDailyWeatherSensitiveLoadInHoursReS.get(dayTemp);
//    }
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
//    else if (type.equals("RaS")) {
//      nonDominantVector = aggDailyNonDominantLoadInHoursRaS.get(dayTemp);
//    }
//    else if (type.equals("ReS")) {
//      nonDominantVector = aggDailyNonDominantLoadInHoursReS.get(dayTemp);
//    }
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
//    for (Household house: regularlyShiftingHouses)
//      houses.add(house);
//    for (Household house: randomlyShiftingHouses)
//      houses.add(house);
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
//    else if (type.equals("RaS")) {
//      for (Household house: regularlyShiftingHouses) {
//        houses.add(house);
//      }
//    }
//    else if (type.equals("ReS")) {
//      for (Household house: randomlyShiftingHouses) {
//        houses.add(house);
//      }
//    }
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
   * minimum cost without shifting the appliances' load but the tariff chosen
   * is
   * picked up randomly by using a possibility pattern. The better tariffs
   * have
   * more chances to be chosen.
   */
  @Override
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    for (CustomerInfo customer: getCustomerInfos()) {
      log.info("Customer " + customer.toString()
               + " evaluating tariffs for timeslot "
               + service.getTimeslotRepo().currentTimeslot().getId());
      TariffEvaluator evaluator = tariffEvaluators.get(customer);
      evaluator.evaluateTariffs();
    }
  }

  // =====SHIFTING FUNCTIONS===== //

  /**
   * This is the function that takes every household in the village and reads
   * the shifted Controllable Consumption for the needs of the tariff
   * evaluation.
   * 
   * @param tariff
   * @param day
   * @param type
   * @param start 
   * @return
   */
  double[] dailyShifting (Tariff tariff, double[] nonDominantUsage, int day,
                          String type, Instant start)
  {

    double[] newControllableLoad = nonDominantUsage;
    int dayTemp =
      day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    Vector<Household> houses = new Vector<Household>();

    if (type.equals("NS")) {
      houses = notShiftingHouses;
    }
//    else if (type.equals("RaS")) {
//      houses = randomlyShiftingHouses;
//    }
//    else if (type.equals("ReS")) {
//      houses = regularlyShiftingHouses;
//    }
    else {
      houses = smartShiftingHouses;
    }

    for (Household house: houses) {
      double[] temp =
        house.dailyShifting(tariff, newControllableLoad, tariffEvalHelper,
                            dayTemp, gen, start);

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

  // =====STEP FUNCTIONS===== //

  @Override
  public void step ()
  {
    int serial = service.getTimeslotRepo().currentSerialNumber();
    Timeslot ts = service.getTimeslotRepo().currentTimeslot();
    // TODO - this code assumes that games start at midnight. Bad assumption.
    int day = (int) (serial / VillageConstants.HOURS_OF_DAY);
    int hour = ts.getStartTime().getHourOfDay();
    Instant now = ts.getStartInstant();

    weatherCheck(day, hour, now);

    checkCurtailment(serial, day, hour);

    consumePower();

    // for (Household house: getHouses())
    // house.test();

    if (hour == 23) {

      for (String type: numberOfHouses.keySet()) {
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
    wr = service.getWeatherReportRepo().currentWeatherReport();

    if (wr != null) {
      double temperature = wr.getTemperature();
      // log.debug("Temperature: " + temperature);

      Vector<Household> houses = getHouses();

      for (Household house: houses) {
        house.weatherCheck(dayTemp, hour, now, temperature);
      }

      for (String type: numberOfHouses.keySet()) {
        updateAggDailyWeatherSensitiveLoad(type, day);
        if (dayTemp + 1 < VillageConstants.DAYS_OF_COMPETITION) {
          updateAggDailyWeatherSensitiveLoad(type, dayTemp + 1);
        }

      }
    }
  }

  /**
   * This function is utilized in order to check the subscriptions
   * curtailments
   * for each time tick and move the controllable load at the nexttimeslot.
   */
  void checkCurtailment (int serial, int day, int hour)
  {
    int nextSerial = service.getTimeslotRepo().currentSerialNumber() + 1;
      // (int) ((timeService.getCurrentTime().getMillis() - timeService.getBase()) / TimeService.HOUR) + 1;
    int nextDay = (int) (nextSerial / VillageConstants.HOURS_OF_DAY);
    int nextHour = (int) (nextSerial % VillageConstants.HOURS_OF_DAY);

    int dayTemp =
      day
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);
    int nextDayTemp =
      nextDay
              % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    for (CustomerInfo customer: getCustomerInfos()) {
      if (customer.getPowerType() == PowerType.INTERRUPTIBLE_CONSUMPTION) {
        List<TariffSubscription> subs =
          service.getTariffSubscriptionRepo()
          .findActiveSubscriptionsForCustomer(customer);

        long curt =
          (long) (subs.get(0).getRegulation()
              * customer.getPopulation()
              * VillageConstants.THOUSAND);
        log.debug(this.toString() + " Subscription " + subs.get(0).toString()
                  + " Curtailment " + curt);

        if (curt > 0) {
          String temp = houseMapping.get(customer);
          String type = temp.substring(0, 2);
          curtailControllableConsumption(dayTemp, hour, type, -curt);
          curtailControllableConsumption(nextDayTemp, nextHour, type, curt);
        }
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
    int serial = service.getTimeslotRepo().currentSerialNumber();
    int day = (int) (serial / VillageConstants.HOURS_OF_DAY) + 1;

    int dayTemp =
      day % (VillageConstants.DAYS_OF_BOOTSTRAP + VillageConstants.DAYS_OF_COMPETITION);

    double[] nonDominantUsage = getNonDominantUsage(dayTemp, type);

    Vector<Long> controllableVector = new Vector<Long>();

    CustomerInfo customer = service.getCustomerRepo()
        .findByNameAndPowerType(name + " " + type + " Controllable",
                                PowerType.INTERRUPTIBLE_CONSUMPTION);

    TariffSubscription sub = service.getTariffSubscriptionRepo()
        .findActiveSubscriptionsForCustomer(customer).get(0);

    log.debug("Old Consumption for day " + day + ": "
              + getControllableConsumptions(dayTemp, type).toString());
    double[] newControllableLoad =
      dailyShifting(sub.getTariff(), nonDominantUsage,
                    dayTemp, type, nextStartOfDay());

    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      String newControllableLoadString =
        Double.toString(newControllableLoad[i]);
      newControllableLoadString = newControllableLoadString.replace(".0", "");
      controllableVector.add(Long.parseLong(newControllableLoadString));
    }

    log.debug("New Consumption for day " + day + ": "
              + controllableVector.toString());

    if (type.equals("SS")) {
//      aggDailyControllableLoadInHoursRaS.set(dayTemp, controllableVector);
//    }
//    else if (type.equals("ReS")) {
//      aggDailyControllableLoadInHoursReS.set(dayTemp, controllableVector);
//    }
//    else {
      aggDailyControllableLoadInHoursSS.set(dayTemp, controllableVector);
    }

  }

  @Override
  public String toString ()
  {
    return name;
  }

  public class TariffEvaluationWrapper implements CustomerModelAccessor
  {
    private String type;
    private int day;
    private CustomerInfo customerInfo;

    public TariffEvaluationWrapper (String type, CustomerInfo customer)
    {
      this.type = type;
      customerInfo = customer;
      day =
        gen.nextInt(VillageConstants.DAYS_OF_BOOTSTRAP
                    + VillageConstants.DAYS_OF_COMPETITION);
    }

    @Override
    public CustomerInfo getCustomerInfo ()
    {
      return customerInfo;
    }

    public String getType ()
    {
      return type;
    }

    public int getPopulation ()
    {
      return getHouses(type).size();
    }

    @Override
    public CapacityProfile getCapacityProfile (Tariff tariff)
    {
      double[] result = new double[VillageConstants.HOURS_OF_DAY];

      if (type.equalsIgnoreCase("NS"))
        result =
          Arrays.copyOf(getDominantLoad(type), getDominantLoad(type).length);

      else {
        double[] nonDominantUsage = getNonDominantUsage(day, type);

        result = dailyShifting(tariff, nonDominantUsage,
                               day, type, nextStartOfDay());
      }

      log.debug(Arrays.toString(result));

      for (int i = 0; i < result.length; i++)
        result[i] /= (VillageConstants.THOUSAND * getPopulation());

      log.info("Usage: " + Arrays.toString(result));

      return new CapacityProfile(result, nextStartOfDay());
    }

    @Override
    public double getBrokerSwitchFactor (boolean isSuperseding)
    {
      double result = VillageConstants.BROKER_SWITCH_FACTOR;
      if (isSuperseding)
        return result * 5.0;
      return result;
    }

    @Override
    public double getTariffChoiceSample ()
    {
      return gen.nextDouble();
    }

    @Override
    public double getInertiaSample ()
    {

      return gen.nextDouble();
    }

    @Override
    public double getShiftingInconvenienceFactor(Tariff tariff) {
      // TODO Auto-generated method stub
      return VillageConstants.TOU_FACTOR;
    }

    @Override
    public void notifyCustomer (TariffSubscription oldsub,
                                TariffSubscription newsub, int population)
    {
      // method stub
    }
  }

}
