/*
 * Copyright 2011-2013 the original author or authors.
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
package org.powertac.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * Abstract customer implementation
 * 
 * @author Antonios Chrysopoulos
 */
public class AbstractCustomer
{
  static protected Logger log = Logger.getLogger(AbstractCustomer.class
          .getName());

  protected TariffMarket tariffMarketService;

  protected TariffSubscriptionRepo tariffSubscriptionRepo;

  protected CustomerRepo customerRepo;

  protected RandomSeedRepo randomSeedRepo;

  /** The id of the Abstract Customer */
  protected long custId;

  /** The name of the Abstract Customer */
  protected String name;

  /** The Customer specifications */
  protected ArrayList<CustomerInfo> customerInfos;

  /**
   * >0: max power consumption (think consumer with fuse limit); <0: min power
   * production (think nuclear power plant with min output)
   */
  protected double upperPowerCap = 100.0;

  /**
   * >0: min power consumption (think refrigerator); <0: max power production
   * (think power plant with max capacity)
   */
  protected double lowerPowerCap = 0.0;

  /** >=0 - gram CO2 per kW/h */
  protected double carbonEmissionRate = 0.0;

  /**
   * measures how wind changes translate into load / generation changes of the
   * customer
   */
  protected double windToPowerConversion = 0.0;

  /**
   * measures how temperature changes translate into load / generation changes
   * of the customer
   */
  protected double tempToPowerConversion = 0.0;

  /**
   * measures how sun intensity changes translate into load /generation changes
   * of the customer
   */
  protected double sunToPowerConversion = 0.0;

  /** Random Number Generator */
  protected RandomSeed rs1;

  /**
   * Abstract Customer constructor. It takes the customerInfo as an input. It
   * creates the autowiring required using the Spring Application Context, it
   * creates the new Abstract Customer based on the customerInfo given, creates
   * a new random number generator and adds the newly created customer in the
   * CustomerRepo.
   */
  public AbstractCustomer (String name)
  {
    super();
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    customerRepo =
      (CustomerRepo) SpringApplicationContext.getBean("customerRepo");
    tariffMarketService =
      (TariffMarket) SpringApplicationContext.getBean("tariffMarketService");
    tariffSubscriptionRepo =
      (TariffSubscriptionRepo) SpringApplicationContext
              .getBean("tariffSubscriptionRepo");

    custId = IdGenerator.createId();
    this.name = name;
    customerInfos = new ArrayList<CustomerInfo>();
    rs1 =
      randomSeedRepo.getRandomSeed(name, 0,
                                   "TariffChooser");
  }

  /**
   * Overload of the above constructor
   */
  public AbstractCustomer (String name, ArrayList<CustomerInfo> customers)
  {
    super();
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    customerRepo =
      (CustomerRepo) SpringApplicationContext.getBean("customerRepo");
    tariffMarketService =
      (TariffMarket) SpringApplicationContext.getBean("tariffMarketService");
    tariffSubscriptionRepo =
      (TariffSubscriptionRepo) SpringApplicationContext
              .getBean("tariffSubscriptionRepo");

    custId = IdGenerator.createId();
    this.name = name;
    customerInfos = customers;
    rs1 =
      randomSeedRepo.getRandomSeed(name, 0,
                                   "TariffChooser");
    for (CustomerInfo customer: customerInfos)
      customerRepo.add(customer);
  }

  public String toString ()
  {
    return Long.toString(getId()) + " " + getName();
  }

  public int getPopulation (CustomerInfo customer)
  {
    return customerInfos.get(customerInfos.indexOf(customer)).getPopulation();
  }

  public long getCustId ()
  {
    return custId;
  }

  /** Synonym for getCustId() */
  public long getId ()
  {
    return custId;
  }

  public String getName ()
  {
    return name;
  }

  public ArrayList<CustomerInfo> getCustomerInfo ()
  {
    return customerInfos;
  }

  public double getUpperPowerCap ()
  {
    return upperPowerCap;
  }

  public double getLowerPowerCap ()
  {
    return lowerPowerCap;
  }

  public double getCarbonEmissionRate ()
  {
    return carbonEmissionRate;
  }

  public double getWindToPowerConversion ()
  {
    return windToPowerConversion;
  }

  public double getTempToPowerConversion ()
  {
    return tempToPowerConversion;
  }

  public double getSunToPowerConversion ()
  {
    return sunToPowerConversion;
  }

  public void addCustomerInfo (CustomerInfo customer)
  {
    boolean error = false;

    for (CustomerInfo customerInfo: customerInfos) {
      if (customerInfo.getPowerType() == customer.getPowerType())
        error = true;
    }
    if (error) {
      log.error("Same Power Type exists");
    }
    else {
      customerInfos.add(customer);
      customerRepo.add(customer);
    }
  }

  // =============================SUBSCRIPTION=================================================

  /**
   * Function utilized at the beginning in order to subscribe to the default
   * tariff
   */
  public void subscribeDefault ()
  {
    for (CustomerInfo customer: customerInfos) {

      PowerType type = customer.getPowerType();
      if (tariffMarketService.getDefaultTariff(type) == null) {
        log.info("No default Subscription for type " + type.toString() + " of "
                 + this.toString() + " to subscribe to.");
      }
      else {
        tariffMarketService.subscribeToTariff(tariffMarketService
                .getDefaultTariff(type), customer, customer.getPopulation());
        log.info("CustomerInfo of type " + type.toString() + " of "
                 + this.toString()
                 + " was subscribed to the default broker successfully.");
      }
    }
  }

  // =============================CONSUMPTION-PRODUCTION=================================================

  /**
   * The first implementation of the power consumption function. I utilized the
   * mean consumption of a neighborhood of households with a random variable
   */
  public void consumePower ()
  {
  }

  /**
   * The first implementation of the power consumption function. I utilized the
   * mean consumption of a neighborhood of households with a random variable
   */
  public void producePower ()
  {
  }

  // =============================TARIFF_SELECTION_PROCESS=================================================

//  /**
//   * The first implementation of the changing subscription function. Here we
//   * just put the tariff we want to change and the whole population is moved to
//   * another random tariff.
//   * 
//   * @param tariff
//   */
//  public void changeSubscription (Tariff tariff, CustomerInfo customer)
//  {
//
//    TariffSubscription ts =
//      tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff,
//                                                                  customer);
//
//    int populationCount = ts.getCustomersCommitted();
//    unsubscribe(ts, populationCount);
//
//    Tariff newTariff = selectTariff(tariff.getTariffSpec().getPowerType());
//    subscribe(newTariff, populationCount, customer);
//
//  }

//  /**
//   * In this overloaded implementation of the changing subscription function,
//   * Here we just put the tariff we want to change and amount of the population
//   * we want to move to the new tariff.
//   * 
//   * @param tariff
//   */
//  public void changeSubscription (Tariff tariff, Tariff newTariff,
//                                  int populationCount, CustomerInfo customer)
//  {
//    TariffSubscription ts =
//      tariffSubscriptionRepo.getSubscription(customer, tariff);
//    unsubscribe(ts, populationCount);
//    subscribe(newTariff, populationCount, customer);
//  }
//
//  /**
//   * The first implementation of the tariff selection function. This is a random
//   * chooser of the available tariffs, totally insensitive.
//   */
//  public Tariff selectTariff (PowerType powerType)
//  {
//    Tariff result;
//    List<Tariff> available = new ArrayList<Tariff>();
//    int ran, index;
//    available = tariffMarketService.getActiveTariffList(powerType);
//    // log.info("Available Tariffs for " + powerType + ": "
//    // available.toString()");
//    index = available.indexOf(tariffMarketService.getDefaultTariff(powerType));
//    log.info("Index of Default Tariff: " + index);
//    ran = index;
//    while (ran == index) {
//      ran = (int) (available.size() * rs1.nextDouble());
//    }
//    result = available.get(ran);
//    return result;
//  }
//
//  /**
//   * The first implementation of the checking for revoked subscriptions
//   * function.
//   */
//  public void checkRevokedSubscriptions ()
//  {
//    for (CustomerInfo customer: customerInfos) {
//      List<TariffSubscription> revoked =
//        tariffSubscriptionRepo.getRevokedSubscriptionList(customer);
//      for (TariffSubscription revokedSubscription: revoked) {
//        revokedSubscription.handleRevokedTariff();
//      }
//    }
//  }

  public void step ()
  {
  }

  // --------------------------------------------
  //   Test support only
  // --------------------------------------------
  /**
   * In this overloaded implementation of the changing subscription function,
   * Here we just put the tariff we want to change and the whole population is
   * moved to another random tariff. NOTE: Used only for testing...
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
  }

  /** Subscribing a certain population amount to a certain subscription */
  public void
    subscribe (Tariff tariff, int customerCount, CustomerInfo customer)
  {

    tariffMarketService.subscribeToTariff(tariff, customer, customerCount);
    log.info(this.toString() + " " + tariff.getPowerType().toString() + ": "
             + customerCount + " were subscribed to tariff " + tariff.getId());

  }

  /** Unsubscribing a certain population amount from a certain subscription */
  public void unsubscribe (TariffSubscription subscription, int customerCount)
  {

    subscription.unsubscribe(customerCount);
    log.info(this.toString() + " "
             + subscription.getTariff().getPowerType().toString() + ": "
             + customerCount + " were unsubscribed from tariff "
             + subscription.getTariff().getId());

  }
}
