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
package org.powertac.officecomplexcustomer;

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
import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;
import org.powertac.officecomplexcustomer.customers.OfficeComplex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implements the Office Consumer Model. It creates Office Consumers that can
 * subscribe to tariffs, evaluate them in order to choose the best one for its
 * interests, shift their load in order to minimize their costs and many others.
 * They contain different types of households with respect to the way they
 * choose the tariffs and they shift their loads.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
@Service
public class OfficeComplexCustomerService extends TimeslotPhaseProcessor
  implements NewTariffListener, InitializationService
{
  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static private Logger log = Logger
          .getLogger(OfficeComplexCustomerService.class.getName());

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
  private int daysOfCompetition = 0;

  /**
   * This is the configuration file that will be utilized to pass the parameters
   * that can be adjusted by user
   */
  Properties configuration = new Properties();

  /** List of the Office Customers in the competition */
  ArrayList<OfficeComplex> officeComplexList;

  /** The Tariffs that will receive while registered as New Tariff Listener */
  List<Tariff> publishedTariffs = new ArrayList<Tariff>();

  /**
   * Counter of the publishing periods, useful for customers that won't check
   * for tariffs each time they are published.
   */
  int publishingPeriods;

  /** This is the constructor of the Office Consumer Service. */
  public OfficeComplexCustomerService ()
  {
    super();
    publishingPeriods = 0;
    officeComplexList = new ArrayList<OfficeComplex>();
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

    officeComplexList.clear();
    tariffMarketService.registerNewTariffListener(this);
    rs1 =
      randomSeedRepo.getRandomSeed("OfficeComplexCustomerService", 1,
                                   "Office Complex Customer Models");

    if (configFile1 == null) {
      log.info("No Config File for OfficeComplexType1 Taken");
      configFile1 = "OfficeComplexDefault.properties";
    }
    if (configFile2 == null) {
      log.info("No Config File for OfficeComplexType2 Taken");
      configFile2 = "OfficeComplexDefault.properties";
    }

    super.init();
    daysOfCompetition =
      Competition.currentCompetition().getExpectedTimeslotCount()
              / OfficeComplexConstants.HOURS_OF_DAY;
    OfficeComplexConstants.setDaysOfCompetition(daysOfCompetition);
    daysOfCompetition = OfficeComplexConstants.DAYS_OF_COMPETITION;

    if (daysOfCompetition == 0) {
      log.info("No Days Of Competition Taken");
      daysOfCompetition = 63;
    }
    // =======FIRST OFFICE COMPLEX TYPE=========//

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

    int numberOfOfficeComplexes =
      Integer.parseInt(configuration.getProperty("NumberOfOfficeComplexes"));

    int nsoffices =
      Integer.parseInt(configuration.getProperty("NotShiftingCustomers"));
    int ssoffices =
      Integer.parseInt(configuration.getProperty("SmartShiftingCustomers"));

    int villagePopulation = nsoffices + ssoffices;

    for (int i = 1; i < numberOfOfficeComplexes + 1; i++) {
      CustomerInfo officeComplexInfo =
        new CustomerInfo("OfficeComplexType1 OfficeComplex " + i,
                         villagePopulation)
                .withPowerType(PowerType.CONSUMPTION);
      CustomerInfo officeComplexInfo2 =
        new CustomerInfo("OfficeComplexType1 OfficeComplex " + i,
                         villagePopulation)
                .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
      OfficeComplex officeComplex =
        new OfficeComplex("OfficeComplexType1 OfficeComplex " + i);
      officeComplex.addCustomerInfo(officeComplexInfo);
      officeComplex.addCustomerInfo(officeComplexInfo2);
      officeComplex.initialize(configuration, rs1);
      officeComplexList.add(officeComplex);
      officeComplex.subscribeDefault();
    }

    // =======SECOND OFFICE COMPLEX TYPE=========//

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

    numberOfOfficeComplexes =
      Integer.parseInt(configuration.getProperty("NumberOfOfficeComplexes"));

    nsoffices =
      Integer.parseInt(configuration.getProperty("NotShiftingCustomers"));
    ssoffices =
      Integer.parseInt(configuration.getProperty("SmartShiftingCustomers"));

    villagePopulation = nsoffices + ssoffices;

    for (int i = 1; i < numberOfOfficeComplexes + 1; i++) {
      CustomerInfo officeComplexInfo =
        new CustomerInfo("OfficeComplexType2 OfficeComplex " + i,
                         villagePopulation)
                .withPowerType(PowerType.CONSUMPTION);
      CustomerInfo officeComplexInfo2 =
        new CustomerInfo("OfficeComplexType2 OfficeComplex " + i,
                         villagePopulation)
                .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
      OfficeComplex officeComplex =
        new OfficeComplex("OfficeComplexType2 OfficeComplex " + i);
      officeComplex.addCustomerInfo(officeComplexInfo);
      officeComplex.addCustomerInfo(officeComplexInfo2);
      officeComplex.initialize(configuration, rs1);
      officeComplexList.add(officeComplex);
      officeComplex.subscribeDefault();
    }

    return "OfficeComplexCustomer";
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
    for (OfficeComplex officeComplex: officeComplexList) {

      // For each type of houses of the villages //
      for (String type: officeComplex.getSubscriptionMap().keySet()) {

        // if the publishingPeriod is divided exactly with the periodicity of
        // the evaluation of each type. //
        if (publishingPeriods % officeComplex.getPeriodMap().get(type) == 0) {

          // System.out.println("Evaluation for " + type + " of village " +
          // village.toString());
          log.debug("Evaluation for " + type + " of village "
                    + officeComplex.toString());
          double rand = rs1.nextDouble();
          // System.out.println(rand);
          // If the percentage is smaller that inertia then evaluate the new
          // tariffs then evaluate //
          if (rand < officeComplex.getInertiaMap().get(type)) {
            // System.out.println("Inertia Passed for " + type + " of village "
            // + village.toString());
            log.debug("Inertia Passed for " + type + " of village "
                      + officeComplex.toString());
            officeComplex.possibilityEvaluationNewTariffs(publishedTariffs,
                                                          type);
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

  @ConfigurableValue(valueType = "String", description = "first configuration file of the office complex customers")
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

  @ConfigurableValue(valueType = "String", description = "second configuration file of the office complex customers")
  public
    void setConfigFile2 (String config)
  {
    configFile2 = config;
  }

  /**
   * This function returns the list of the villages created at the beginning of
   * the game by the service
   */
  public List<OfficeComplex> getOfficeComplexList ()
  {
    return officeComplexList;
  }

  /**
   * This function cleans the configuration files in case they have not been
   * cleaned at the beginning of the game
   */
  public void clearConfiguration ()
  {
    configFile1 = null;
    configFile2 = null;
  }

  /**
   * This function finds all the available Office Consumers in the competition
   * and creates a list of their customerInfo.
   * 
   * @return List<CustomerInfo>
   */
  public List<CustomerInfo> generateCustomerInfoList ()
  {
    ArrayList<CustomerInfo> result = new ArrayList<CustomerInfo>();
    for (OfficeComplex officeComplex: officeComplexList) {
      for (CustomerInfo customer: officeComplex.getCustomerInfo())
        result.add(customer);
    }
    return result;
  }

  @Override
  public void activate (Instant time, int phaseNumber)
  {
    log.info("Activate");
    if (officeComplexList.size() > 0) {
      for (OfficeComplex officeComplex: officeComplexList) {
        officeComplex.step();
      }
    }
  }

  @Override
  public void setDefaults ()
  {
  }

}
