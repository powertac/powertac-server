/*
 * Copyright 2010-2012 the original author or authors.
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
package org.powertac.householdcustomer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.householdcustomer.configurations.VillageConstants;
import org.powertac.householdcustomer.customers.Village;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implements the Household Consumer Model. It creates Household Consumers that
 * can subscribe to tariffs, evaluate them in order to choose the best one for
 * its interests, shift their load in order to minimize their costs and many
 * others. They contain different types of households with respect to the way
 * they choose the tariffs and they shift their loads.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
@Service
public class HouseholdCustomerService extends TimeslotPhaseProcessor
  implements NewTariffListener, InitializationService
{
  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static private Logger log = Logger.getLogger(HouseholdCustomerService.class
          .getName());

  @Autowired
  private TariffMarket tariffMarketService;

  @Autowired
  private ServerConfiguration serverPropertiesService;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  /** Random Number Generator */
  private RandomSeed rs1;

  // read this from configurator
  private String configFile1 = null;
  private String configFile2 = null;
  private String configFile3 = null;
  private String configFile4 = null;
  private int daysOfCompetition = 0;

  /**
   * This is the configuration file that will be utilized to pass the parameters
   * that can be adjusted by user
   */
  Properties configuration = new Properties();

  /** List of the Household Customers in the competition */
  ArrayList<Village> villageList;

  /** The Tariffs that will receive while registered as New Tariff Listener */
  List<Tariff> publishedTariffs = new ArrayList<Tariff>();

  /**
   * Counter of the publishing periods, useful for customers that won't check
   * for tariffs each time they are published.
   */
  int publishingPeriods;

  /** This is the constructor of the Household Consumer Service. */
  public HouseholdCustomerService ()
  {
    super();
    publishingPeriods = 0;
    villageList = new ArrayList<Village>();
  }

  /**
   * This function called once at the beginning of each game by the server
   * initialization service. Here is where you do pre-game setup. This will read
   * the server properties file to take the competition input variables needed
   * (configuration files, days of competition), create a listener for our
   * service, in order to get the new tariff, as well as create the household
   * Consumers that will be running in the game.
   */
  @Override
  public String
    initialize (Competition competition, List<String> completedInits)
  {
    int index = completedInits.indexOf("DefaultBroker");
    if (index == -1) {
      return null;
    }

    serverPropertiesService.configureMe(this);

    villageList.clear();
    tariffMarketService.registerNewTariffListener(this);
    rs1 =
      randomSeedRepo.getRandomSeed("HouseholdCustomerService", 1,
                                   "Household Customer Models");

    if (configFile1 == null) {
      log.info("No Config File for VillageType1 Taken");
      configFile1 = "VillageDefault.properties";
    }
    if (configFile2 == null) {
      log.info("No Config File for VillageType2 Taken");
      configFile2 = "VillageDefault.properties";
    }
    if (configFile3 == null) {
      log.info("No Config File for VillageType3 Taken");
      configFile3 = "VillageDefault.properties";
    }
    if (configFile4 == null) {
      log.info("No Config File for VillageType4 Taken");
      configFile4 = "VillageDefault.properties";
    }

    super.init();
    daysOfCompetition =
      Competition.currentCompetition().getExpectedTimeslotCount()
              / VillageConstants.HOURS_OF_DAY;
    VillageConstants.setDaysOfCompetition(daysOfCompetition);
    daysOfCompetition = VillageConstants.DAYS_OF_COMPETITION;

    if (daysOfCompetition == 0) {
      log.info("No Days Of Competition Taken");
      daysOfCompetition = 63;
    }

    // =======FIRST VILLAGE TYPE=========//

    InputStream cfgFile = null;
    // cfgFile = new FileInputStream(configFile);
    cfgFile =
      Thread.currentThread().getContextClassLoader()
              .getResourceAsStream(configFile1);
    try {
      configuration.load(cfgFile);
      cfgFile.close();
    }
    catch (IOException e) {
      e.printStackTrace();

    }

    int numberOfVillages =
      Integer.parseInt(configuration.getProperty("NumberOfVillages"));

    int nshouses =
      Integer.parseInt(configuration.getProperty("NotShiftingCustomers"));
    int rashouses =
      Integer.parseInt(configuration.getProperty("RandomlyShiftingCustomers"));
    int reshouses =
      Integer.parseInt(configuration.getProperty("RegularlyShiftingCustomers"));
    int sshouses =
      Integer.parseInt(configuration.getProperty("SmartShiftingCustomers"));

    int villagePopulation = nshouses + rashouses + reshouses + sshouses;

    for (int i = 1; i < numberOfVillages + 1; i++) {
      CustomerInfo villageInfo =
        new CustomerInfo("VillageType1 Village " + i + " Base",
                         villagePopulation)
                .withPowerType(PowerType.CONSUMPTION);
      CustomerInfo villageInfo2 =
        new CustomerInfo("VillageType1 Village " + i + " Controllable",
                         villagePopulation)
                .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
      Village village = new Village("VillageType1 Village " + i);
      village.addCustomerInfo(villageInfo);
      village.addCustomerInfo(villageInfo2);
      village.initialize(configuration, rs1);
      villageList.add(village);
      village.subscribeDefault();
    }

    // =======SECOND VILLAGE TYPE=========//

    cfgFile = null;
    // cfgFile = new FileInputStream(configFile);
    cfgFile =
      Thread.currentThread().getContextClassLoader()
              .getResourceAsStream(configFile2);
    try {
      configuration.load(cfgFile);
      cfgFile.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    numberOfVillages =
      Integer.parseInt(configuration.getProperty("NumberOfVillages"));

    nshouses =
      Integer.parseInt(configuration.getProperty("NotShiftingCustomers"));
    rashouses =
      Integer.parseInt(configuration.getProperty("RandomlyShiftingCustomers"));
    reshouses =
      Integer.parseInt(configuration.getProperty("RegularlyShiftingCustomers"));
    sshouses =
      Integer.parseInt(configuration.getProperty("SmartShiftingCustomers"));

    villagePopulation = nshouses + rashouses + reshouses + sshouses;

    for (int i = 1; i < numberOfVillages + 1; i++) {
      CustomerInfo villageInfo =
        new CustomerInfo("VillageType2 Village " + i + " Base",
                         villagePopulation)
                .withPowerType(PowerType.CONSUMPTION);
      CustomerInfo villageInfo2 =
        new CustomerInfo("VillageType2 Village " + i + " Controllable",
                         villagePopulation)
                .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
      Village village = new Village("VillageType2 Village " + i);
      village.addCustomerInfo(villageInfo);
      village.addCustomerInfo(villageInfo2);
      village.initialize(configuration, rs1);
      villageList.add(village);
      village.subscribeDefault();
    }

    // =======THIRD VILLAGE TYPE=========//

    cfgFile = null;
    // cfgFile = new FileInputStream(configFile);
    cfgFile =
      Thread.currentThread().getContextClassLoader()
              .getResourceAsStream(configFile3);
    try {
      configuration.load(cfgFile);
      cfgFile.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    numberOfVillages =
      Integer.parseInt(configuration.getProperty("NumberOfVillages"));

    nshouses =
      Integer.parseInt(configuration.getProperty("NotShiftingCustomers"));
    rashouses =
      Integer.parseInt(configuration.getProperty("RandomlyShiftingCustomers"));
    reshouses =
      Integer.parseInt(configuration.getProperty("RegularlyShiftingCustomers"));
    sshouses =
      Integer.parseInt(configuration.getProperty("SmartShiftingCustomers"));

    villagePopulation = nshouses + rashouses + reshouses + sshouses;

    for (int i = 1; i < numberOfVillages + 1; i++) {
      CustomerInfo villageInfo =
        new CustomerInfo("VillageType3 Village " + i + " Base",
                         villagePopulation)
                .withPowerType(PowerType.CONSUMPTION);
      CustomerInfo villageInfo2 =
        new CustomerInfo("VillageType3 Village " + i + " Controllable",
                         villagePopulation)
                .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
      Village village = new Village("VillageType3 Village " + i);
      village.addCustomerInfo(villageInfo);
      village.addCustomerInfo(villageInfo2);
      village.initialize(configuration, rs1);
      villageList.add(village);
      village.subscribeDefault();
    }

    // =======FOURTH VILLAGE TYPE=========//

    cfgFile = null;
    // cfgFile = new FileInputStream(configFile);
    cfgFile =
      Thread.currentThread().getContextClassLoader()
              .getResourceAsStream(configFile4);
    try {
      configuration.load(cfgFile);
      cfgFile.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    numberOfVillages =
      Integer.parseInt(configuration.getProperty("NumberOfVillages"));

    nshouses =
      Integer.parseInt(configuration.getProperty("NotShiftingCustomers"));
    rashouses =
      Integer.parseInt(configuration.getProperty("RandomlyShiftingCustomers"));
    reshouses =
      Integer.parseInt(configuration.getProperty("RegularlyShiftingCustomers"));
    sshouses =
      Integer.parseInt(configuration.getProperty("SmartShiftingCustomers"));

    villagePopulation = nshouses + rashouses + reshouses + sshouses;

    for (int i = 1; i < numberOfVillages + 1; i++) {
      CustomerInfo villageInfo =
        new CustomerInfo("VillageType4 Village " + i + " Base",
                         villagePopulation)
                .withPowerType(PowerType.CONSUMPTION);
      CustomerInfo villageInfo2 =
        new CustomerInfo("VillageType4 Village " + i + " Controllable",
                         villagePopulation)
                .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
      Village village = new Village("VillageType4 Village " + i);
      village.addCustomerInfo(villageInfo);
      village.addCustomerInfo(villageInfo2);
      village.initialize(configuration, rs1);
      villageList.add(village);
      village.subscribeDefault();
    }

    return "HouseholdCustomer";
  }

  @Override
  public void publishNewTariffs (List<Tariff> tariffs)
  {
    publishingPeriods++;
    publishedTariffs =
      tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);

    List<Tariff> temp =
      tariffMarketService
              .getActiveTariffList(PowerType.INTERRUPTIBLE_CONSUMPTION);

    publishedTariffs.addAll(temp);

    // For each village of the server //
    for (Village village: villageList) {

      // For each type of houses of the villages //
      for (String type: village.getSubscriptionMap().keySet()) {

        // if the publishingPeriod is divided exactly with the periodicity of
        // the evaluation of each type. //
        if (publishingPeriods % village.getPeriodMap().get(type) == 0) {

          // System.out.println("Evaluation for " + type + " of village " +
          // village.toString());
          log.debug("Evaluation for " + type + " of village "
                    + village.toString());
          double rand = rs1.nextDouble();
          // System.out.println(rand);
          // If the percentage is smaller that inertia then evaluate the new
          // tariffs then evaluate //
          if (rand < village.getInertiaMap().get(type)) {
            // System.out.println("Inertia Passed for " + type + " of village "
            // + village.toString());
            log.debug("Inertia Passed for " + type + " of village "
                      + village.toString());
            village.possibilityEvaluationNewTariffs(publishedTariffs, type);
          }
        }
      }

    }

  }

  // ----------------- Data access -------------------------

  /** Getter method for the days of competition */
  public int getDaysOfCompetition ()
  {
    return daysOfCompetition;
  }

  @ConfigurableValue(valueType = "Integer", description = "The competition duration in days")
  public
    void setDaysOfCompetition (int days)
  {
    daysOfCompetition = days;
  }

  /** Getter method for the first configuration file */
  public String getConfigFile1 ()
  {
    return configFile1;
  }

  @ConfigurableValue(valueType = "String", description = "first configuration file of the household customers")
  public
    void setConfigFile1 (String config)
  {
    configFile1 = config;
  }

  /** Getter method for the second configuration file */
  public String getConfigFile2 ()
  {
    return configFile2;
  }

  @ConfigurableValue(valueType = "String", description = "second configuration file of the household customers")
  public
    void setConfigFile2 (String config)
  {
    configFile2 = config;
  }

  /** Getter method for the third configuration file */
  public String getConfigFile3 ()
  {
    return configFile3;
  }

  @ConfigurableValue(valueType = "String", description = "third configuration file of the household customers")
  public
    void setConfigFile3 (String config)
  {
    configFile3 = config;
  }

  /** Getter method for the forth configuration file */
  public String getConfigFile4 ()
  {
    return configFile4;
  }

  @ConfigurableValue(valueType = "String", description = "forth configuration file of the household customers")
  public
    void setConfigFile4 (String config)
  {
    configFile4 = config;
  }

  /**
   * This function returns the list of the villages created at the beginning of
   * the game by the service
   */
  public List<Village> getVillageList ()
  {
    return villageList;
  }

  /**
   * This function cleans the configuration files in case they have not been
   * cleaned at the beginning of the game
   */
  public void clearConfiguration ()
  {
    configFile1 = null;
    configFile2 = null;
    configFile3 = null;
    configFile4 = null;
  }

  /**
   * This function finds all the available Household Consumers in the
   * competition and creates a list of their customerInfo.
   * 
   * @return List<CustomerInfo>
   */
  public List<CustomerInfo> generateCustomerInfoList ()
  {
    ArrayList<CustomerInfo> result = new ArrayList<CustomerInfo>();
    for (Village village: villageList) {
      for (CustomerInfo customer: village.getCustomerInfo())
        result.add(customer);
    }
    return result;
  }

  @Override
  public void activate (Instant time, int phaseNumber)
  {
    log.info("Activate");
    if (villageList.size() > 0) {
      for (Village village: villageList) {
        village.step();
      }
    }
  }

  // @Override
  // public void receiveMessage (Object msg)
  // {
  // TODO Implement per-message behavior. Note that incoming messages
  // from brokers arrive in a JMS thread, so you need to synchronize
  // access to shared data structures. See AuctionService for an example.

  // If you need to handle a number of different message types, it may make
  // make sense to use a reflection-based dispatcher. Both
  // TariffMarketService and AccountingService work this way.
  // }

  @Override
  public void setDefaults ()
  {
  }

}
