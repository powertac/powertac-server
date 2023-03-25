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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
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
  implements NewTariffListener, InitializationService, CustomerServiceAccessor
{
  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static private Logger log = LogManager
          .getLogger(OfficeComplexCustomerService.class.getName());

  @Autowired
  private TariffMarket tariffMarketService;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private ServerConfiguration serverPropertiesService;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private TimeService timeService;

  @Autowired
  private WeatherReportRepo weatherReportRepo;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  /** Random Number Generator */
//  private RandomSeed rs1;

  int seedId = 1;

  // read this from configurator
  private String configFile1 = null;
  //private int daysOfCompetition = 0;

  /**
   * This is the configuration file that will be utilized to pass the parameters
   * that can be adjusted by user
   */
  Properties configuration;

  /** List of the Office Customers in the competition */
  ArrayList<OfficeComplex> officeComplexList;

  /** This is the constructor of the Office Consumer Service. */
  public OfficeComplexCustomerService ()
  {
    super();
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
  public String initialize (Competition competition,
                            List<String> completedInits)
  {
    if (!completedInits.contains("DefaultBroker")
        || !completedInits.contains("TariffMarket"))
      return null;
    super.init();
    configuration = new Properties();
    officeComplexList = new ArrayList<OfficeComplex>();
    tariffMarketService.registerNewTariffListener(this);

    serverPropertiesService.configureMe(this);

    //tariffMarketService.registerNewTariffListener(this);

    if (configFile1 == null) {
      log.info("No Config File for OfficeComplexType1 Taken");
      configFile1 = "OfficeComplexDefault.properties";
    }

    addOfficeComplexes(configFile1, "1");

    return "OfficeComplexCustomer";
  }

  private void addOfficeComplexes (String configFile, String type)
  {

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
      log.error("failed to open configuration file");
      e.printStackTrace();
      return;
    }

    String[] types = { "NS", "SS" };
    String[] shifts = { "Base", "Controllable" };
    Map<String, Integer> offices = new TreeMap<String, Integer>();

    int numberOfOfficeComplexes =
      Integer.parseInt(configuration.getProperty("NumberOfOfficeComplexes"));
    int nsoffices =
      Integer.parseInt(configuration.getProperty("NotShiftingCustomers"));
    offices.put("NS", nsoffices);
    int ssoffices =
      Integer.parseInt(configuration.getProperty("SmartShiftingCustomers"));
    offices.put("SS", ssoffices);

    Comparator<CustomerInfo> comp = new Comparator<CustomerInfo>() {
      @Override
      public int compare (CustomerInfo customer1, CustomerInfo customer2)
      {
        return customer1.getName().compareToIgnoreCase(customer2.getName());
      }
    };

    for (int i = 1; i < numberOfOfficeComplexes + 1; i++) {
      OfficeComplex officeComplex = new OfficeComplex("OfficeComplex " + i);
      Map<CustomerInfo, String> map = new TreeMap<CustomerInfo, String>(comp);

      for (String officeType: types) {
        for (String shifting: shifts) {

          CustomerInfo officeComplexInfo =
            new CustomerInfo("OfficeComplex " + i + " " + officeType + " "
                             + shifting, offices.get(officeType));
          if (shifting.equalsIgnoreCase("Base"))
            officeComplexInfo.withPowerType(PowerType.CONSUMPTION);
          else
            officeComplexInfo
                    .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);

          map.put(officeComplexInfo, officeType + " " + shifting);
          officeComplex.addCustomerInfo(officeComplexInfo);
          customerRepo.add(officeComplexInfo);
        }

      }

      officeComplex.setServiceAccessor(this);
      officeComplex.initialize(configuration, seedId++, map);
      officeComplexList.add(officeComplex);
      officeComplex.subscribeDefault(tariffMarketService);

    }
  }

  @Override
  public void publishNewTariffs (List<Tariff> tariffs)
  {

    // For each village of the server //
    for (OfficeComplex officeComplex: officeComplexList)
      officeComplex.evaluateTariffs(tariffs);

  }

  // ----------------- Data access -------------------------

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
      for (CustomerInfo customer: officeComplex.getCustomerInfos())
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

  // ============== CustomerServiceAccessor API
  @Override
  public CustomerRepo getCustomerRepo ()
  {
    return customerRepo;
  }

  @Override
  public RandomSeedRepo getRandomSeedRepo ()
  {
    return randomSeedRepo;
  }

  @Override
  public TariffRepo getTariffRepo ()
  {
    return tariffRepo;
  }

  @Override
  public TariffSubscriptionRepo getTariffSubscriptionRepo ()
  {
    return tariffSubscriptionRepo;
  }

  @Override
  public TimeslotRepo getTimeslotRepo ()
  {
    return timeslotRepo;
  }

  @Override
  public TimeService getTimeService ()
  {
    return timeService;
  }

  @Override
  public WeatherReportRepo getWeatherReportRepo ()
  {
    return weatherReportRepo;
  }

  @Override
  public ServerConfiguration getServerConfiguration ()
  {
    // Auto-generated method stub
    return serverPropertiesService;
  }

  @Override
  public TariffMarket getTariffMarket ()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public XMLMessageConverter getMessageConverter ()
  {
    // TODO Auto-generated method stub
    return null;
  }
}
