/*
 * Copyright 2011-2014 the original author or authors.
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
package org.powertac.customer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.CustomerInfo;
import org.powertac.common.IdGenerator;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * Abstract customer implementation
 * 
 * @author Antonios Chrysopoulos
 */
public abstract class AbstractCustomer
{
  static protected Logger log = Logger.getLogger(AbstractCustomer.class
          .getName());

  protected String name = "dummy";
  protected HashMap<PowerType, List<CustomerInfo>> customerInfos;
  protected List<CustomerInfo> allCustomerInfos;

  // Service accessor
  protected CustomerServiceAccessor service;

  /** The id of the Abstract Customer */
  protected long custId;

  /** Random Number Generator */
  protected RandomSeed rs1;

  /**
   * Default constructor, requires explicit setting of name
   */
  public AbstractCustomer ()
  {
    super();
    custId = IdGenerator.createId();
    customerInfos = new HashMap<PowerType, List<CustomerInfo>>();
    allCustomerInfos = new ArrayList<CustomerInfo>();
  }

  /**
   * Abstract Customer constructor with explicit name.
   */
  public AbstractCustomer (String name)
  {
    this();
    this.name = name;
  }

  /**
   * Provides a reference to the service accessor, through which we can get
   * at sim services
   */
  public void setServiceAccessor (CustomerServiceAccessor csa)
  {
    this.service = csa;
  }

  /**
   * Initializes the instance. Called after configuration, and after
   * a call to setServices().
   * TODO -- do we really want this here?
   */
  public void initialize ()
  {
    rs1 = service.getRandomSeedRepo().getRandomSeed(name, 0, "TariffChooser");
  }

  /**
   * Adds an additional CustomerInfo to the list
   */
  public void addCustomerInfo (CustomerInfo info)
  {
    if (null == customerInfos.get(info.getPowerType())) {
      customerInfos.put(info.getPowerType(), new ArrayList<CustomerInfo>());
    }
    customerInfos.get(info.getPowerType()).add(info);
    allCustomerInfos.add(info);
  }

  /**
   * Returns the first CustomerInfo associated with this instance and PowerType. 
   * It is up to individual models to fill out the fields.
   */
  public CustomerInfo getCustomerInfo (PowerType pt)
  {
    return getCustomerInfoList(pt).get(0);
  }

  /**
   * Returns the list of CustomerInfos associated with this instance and
   * PowerType.
   */
  public List<CustomerInfo> getCustomerInfoList (PowerType pt)
  {
    return customerInfos.get(pt);
  }

  /**
   * Returns the list of CustomerInfo records associated with this customer
   * model.
   */
  public List<CustomerInfo> getCustomerInfos ()
  {
    return new ArrayList<CustomerInfo>(allCustomerInfos);
  }

  /**
   * Returns the current tariff subscriptions for the first CustomerInfo.
   * Useful for customer models with a single CustomerInfo.
   */
  public List<TariffSubscription> getCurrentSubscriptions ()
  {
    return service.getTariffSubscriptionRepo().
        findActiveSubscriptionsForCustomer(allCustomerInfos.get(0));
  }

  /**
   * Returns the current tariff subscriptions for the first CustomerInfo
   * with the given PowerType. Useful for customer models with a single
   * CustomerInfo per PowerType.
   */
  public List<TariffSubscription> getCurrentSubscriptions (PowerType type)
  {
    return service.getTariffSubscriptionRepo().
        findActiveSubscriptionsForCustomer(customerInfos.get(type).get(0));
  }

  @Override
  public String toString ()
  {
    return Long.toString(getId()) + " " + getName();
  }

  public int getPopulation (CustomerInfo customer)
  {
    return customer.getPopulation();
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

  /** Sets the name for this model **/
  public void setName (String name)
  {
    this.name = name;
  }

  /** Returns the name of this model **/
  public String getName ()
  {
    return name;
  }

  /**
   * Called to run the model forward one step.
   */
  public abstract void step ();

  /**
   * Called to evaluate tariffs.
   */
  public abstract void evaluateTariffs (List<Tariff> tariffs);

  // --------------------------------------------
  //   Test support only
  // --------------------------------------------

  private TariffMarket tariffMarketService;

  public void setTariffMarket (TariffMarket service)
  {
    tariffMarketService = service;
  }
  
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
      service.getTariffSubscriptionRepo().getSubscription(customer, tariff);
    int populationCount = ts.getCustomersCommitted();
    unsubscribe(ts, populationCount);
    subscribe(newTariff, populationCount, customer);
  }

  /** Subscribing a certain population amount to a certain subscription */
  void subscribe (Tariff tariff,
                         int customerCount,
                         CustomerInfo customer)
  {
    tariffMarketService.subscribeToTariff(tariff, customer, customerCount);
    log.info(this.toString() + " " + tariff.getPowerType().toString() + ": "
             + customerCount + " were subscribed to tariff " + tariff.getId());

  }

  /** Unsubscribing a certain population amount from a certain subscription */
  void unsubscribe (TariffSubscription subscription, int customerCount)
  {

    subscription.unsubscribe(customerCount);
    log.info(this.toString() + " "
             + subscription.getTariff().getPowerType().toString() + ": "
             + customerCount + " were unsubscribed from tariff "
             + subscription.getTariff().getId());

  }
}
