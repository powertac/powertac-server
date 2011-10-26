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
package org.powertac.genericcustomer;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.CustomerInfo;
import org.powertac.common.PluginConfig;
import org.powertac.common.Tariff;
import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implements the Generic Producer abstraction. It creates an Producer that can subscribe to
 * tariffs, evaluate them in order to choose the best one for its interests, shift its load in order
 * to minimize its costs and many others.
 * @author Antonios Chrysopoulos
 */
@Service
// allows this service to be autowired into other services
public class GenericProducerService extends TimeslotPhaseProcessor implements BrokerMessageListener, NewTariffListener
{
  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error() appropriately. Use
   * log.debug() for output you want to see in testing or debugging.
   */
  static private Logger log = Logger.getLogger(GenericProducerService.class.getName());

  @Autowired
  private TariffMarket tariffMarketService;

  @Autowired
  private CompetitionControl competitionControlService;

  // read this from plugin config
  private int population = 100;
  private int numberOfProducers = 0;

  /** List of the Generic Customers in the competition */
  ArrayList<GenericProducer> genericProducersList;

  /** The Tariffs that will receive the New Tariff Listener */
  List<Tariff> publishedTariffs = new ArrayList<Tariff>();

  /** This is the constructor of the Generic Producer Service. */
  public GenericProducerService ()
  {
    super();
    genericProducersList = new ArrayList<GenericProducer>();

  }

  /**
   * This is called once at the beginning of each game by the initialization service. Here is where
   * you do per-game setup. This will create a listener for our service, in order to get the new
   * tariff as well as create the generic Producers that will be running in the game.
   */
  void init (PluginConfig config)
  {
    genericProducersList.clear();
    tariffMarketService.registerNewTariffListener(this);

    numberOfProducers = config.getIntegerValue("numberOfProducers", numberOfProducers);
    population = config.getIntegerValue("population", population);

    super.init();

    for (int i = 1; i < numberOfProducers + 1; i++) {
      CustomerInfo genericProducerInfo = new CustomerInfo("GenericProducer " + i, population).withCustomerType(CustomerType.CustomerHousehold).addPowerType(PowerType.PRODUCTION);
      GenericProducer genericProducer = new GenericProducer(genericProducerInfo);
      genericProducersList.add(genericProducer);
      genericProducer.subscribeDefault();
    }

  }

  @Override
  public void publishNewTariffs (List<Tariff> tariffs)
  {
    publishedTariffs = tariffs;
    for (GenericProducer producer : genericProducersList) {
      producer.possibilityEvaluationNewTariffs(publishedTariffs);
    }
  }

  // ----------------- Data access -------------------------

  public int getPopulation ()
  {
    return population;
  }

  public int getNumberOfProducers ()
  {
    return numberOfProducers;
  }

  public List<GenericProducer> getGenericProducersList ()
  {
    return genericProducersList;
  }

  /**
   * This function finds all the available Generic Producers in the competition and creates a list
   * of their customerInfo.
   * @return List<CustomerInfo>
   */
  public List<CustomerInfo> generateProducerInfoList ()
  {
    ArrayList<CustomerInfo> result = new ArrayList<CustomerInfo>();
    for (GenericProducer producer : genericProducersList) {
      result.add(producer.getCustomerInfo());
    }
    return result;
  }

  @Override
  public void activate (Instant time, int phaseNumber)
  {
    log.info("Activate");
    if (genericProducersList.size() > 0) {
      if (phaseNumber == 1) {
        log.info("Phase 1");
        for (GenericProducer producer : genericProducersList) {
          producer.step();
        }
      } else {
        // should never get here
        log.info("Phase 2");
        for (GenericProducer producer : genericProducersList) {
          producer.toString();
        }
      }
    }

  }

  @Override
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
