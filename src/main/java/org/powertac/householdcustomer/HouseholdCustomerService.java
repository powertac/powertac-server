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
package org.powertac.householdcustomer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.CustomerInfo;
import org.powertac.common.PluginConfig;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.householdcustomer.customers.Village;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implements the Generic Consumer abstraction. It creates an Consumer that can subscribe to tariffs, evaluate them in order to choose the best one for its interests, shift its load in order to
 * minimize its costs and many others.
 * 
 * @author Antonios Chrysopoulos
 */
@Service
// allows this service to be autowired into other services
public class HouseholdCustomerService extends TimeslotPhaseProcessor implements BrokerMessageListener, NewTariffListener
{
  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error() appropriately. Use log.debug() for output you want to see in testing or debugging.
   */
  static private Logger log = Logger.getLogger(HouseholdCustomerService.class.getName());

  @Autowired
  private TariffMarket tariffMarketService;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  /** Random Number Generator */
  private RandomSeed rs1;

  // read this normally from plugin config
  // private String configFile = "../household-customer/src/main/resources/Household.properties";
  private String configFile = "Household.properties";

  /**
   * This is the configuration file that will be utilized to pass the parameters that can be adjusted by user
   */
  Properties configuration = new Properties();

  /** List of the Generic Customers in the competition */
  ArrayList<Village> villageList;

  /** The Tariffs that will receive the New Tariff Listener */
  List<Tariff> publishedTariffs = new ArrayList<Tariff>();

  /** This is the constructor of the Household Consumer Service. */
  public HouseholdCustomerService ()
  {
    super();
    villageList = new ArrayList<Village>();
  }

  /**
   * This is called once at the beginning of each game by the initialization service. Here is where you do per-game setup. This will create a listener for our service, in order to get the new tariff
   * as well as create the generic Consumers that will be running in the game.
   * 
   * @throws IOException
   */
  void init (PluginConfig config) throws IOException
  {
    villageList.clear();
    tariffMarketService.registerNewTariffListener(this);
    rs1 = randomSeedRepo.getRandomSeed("HouseholdCustomerService", 1, "Household Customer Models");

    configFile = config.getConfigurationValue("configFile");
    super.init();

    InputStream cfgFile = null;
    // cfgFile = new FileInputStream(configFile);
    cfgFile = ClassLoader.getSystemResourceAsStream(configFile);
    configuration.load(cfgFile);
    cfgFile.close();

    int numberOfVillages = Integer.parseInt(configuration.getProperty("NumberOfVillages"));

    int nshouses = Integer.parseInt(configuration.getProperty("NotShiftingCustomers"));
    int rashouses = Integer.parseInt(configuration.getProperty("RandomlyShiftingCustomers"));
    int reshouses = Integer.parseInt(configuration.getProperty("RegularlyShiftingCustomers"));
    int sshouses = Integer.parseInt(configuration.getProperty("SmartShiftingCustomers"));

    int villagePopulation = nshouses + rashouses + reshouses + sshouses;

    for (int i = 1; i < numberOfVillages + 1; i++) {
      CustomerInfo villageInfo = new CustomerInfo("Household " + i, villagePopulation).withCustomerType(CustomerType.CustomerHousehold).addPowerType(PowerType.CONSUMPTION);
      Village village = new Village(villageInfo);
      village.initialize(configuration, rs1);
      villageList.add(village);
      village.subscribeDefault();
    }

  }

  @Override
  public void publishNewTariffs (List<Tariff> tariffs)
  {
    publishedTariffs = tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION);

    for (Village village : villageList) {
      for (String type : village.getSubscriptionMap().keySet()) {
        log.info("Evaluation for " + type + " of village " + village.toString());
        double rand = rs1.nextDouble();

        if (rand < village.getInertiaMap().get(type)) {
          log.info("Inertia Passed for " + type + " of village " + village.toString());
          village.possibilityEvaluationNewTariffs(publishedTariffs, type);
        }
      }

    }
  }

  // ----------------- Data access -------------------------

  public String getConfigFile ()
  {
    return configFile;
  }

  /**
   * Allows Spring to set the configuration file for the household models length
   */
  public void setConfigFile (String file)
  {
    configFile = file;
  }

  public List<Village> getVillageList ()
  {
    return villageList;
  }

  /**
   * This function finds all the available Generic Consumers in the competition and creates a list of their customerInfo.
   * 
   * @return List<CustomerInfo>
   */
  public List<CustomerInfo> generateCustomerInfoList ()
  {
    ArrayList<CustomerInfo> result = new ArrayList<CustomerInfo>();
    for (Village village : villageList) {
      result.add(village.getCustomerInfo());
    }
    return result;
  }

  @Override
  public void activate (Instant time, int phaseNumber)
  {
    log.info("Activate");
    if (villageList.size() > 0) {
      for (Village village : villageList) {
        village.step();
      }
    }
  }

  public void receiveMessage (Object msg)
  {
    // TODO Implement per-message behavior. Note that incoming messages
    // from brokers arrive in a JMS thread, so you need to synchronize
    // access to shared data structures. See AuctionService for an example.

    // If you need to handle a number of different message types, it may make
    // make sense to use a reflection-based dispatcher. Both
    // TariffMarketService and AccountingService work this way.
  }

}
