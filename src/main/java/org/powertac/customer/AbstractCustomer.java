/*
 * Copyright (c) 2014 by John Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.customer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
//import org.powertac.common.AbstractCustomer;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
//import org.powertac.common.interfaces.CustomerModelAccessor;
//import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.WeatherReportRepo;

/**
 * Customer models must implement this interface.
 * @author John Collins
 */
@ConfigurableInstance
public abstract class AbstractCustomer //implements CustomerModelAccessor
{
  static protected Logger log = Logger.getLogger(AbstractCustomer.class
                                                 .getName());

  protected String name = "dummy";
  protected HashMap<PowerType, CustomerInfo> customerInfos;

  // Services available to subclasses, populated by setServices()
  protected WeatherReportRepo weatherReportRepo;
  protected RandomSeedRepo randomSeedRepo;
  protected TariffRepo tariffRepo;
  protected TariffSubscriptionRepo tariffSubscriptionRepo;

  // current tariff subscription
  //protected TariffSubscription currentSubscription;

  public AbstractCustomer ()
  {
    super();
  }

  /**
   * Sets up basic attributes. Population is 1 unless individual models
   * change it.
   */
  public AbstractCustomer (String name)
  {
    super();
    this.name = name;
    this.customerInfos = new HashMap<PowerType, CustomerInfo>();
  }

  /**
   * Populates the instance with service pointers to avoid Spring dependency.
   */
  public void setServices (RandomSeedRepo randomSeedRepo,
                           WeatherReportRepo weatherReportRepo,
                           TariffRepo tariffRepo,
                           TariffSubscriptionRepo tariffSubscriptionRepo)
  {
    this.randomSeedRepo = randomSeedRepo;
    this.weatherReportRepo = weatherReportRepo;
    this.tariffRepo = tariffRepo;
    this.tariffSubscriptionRepo = tariffSubscriptionRepo;
  }

  /**
   * Initializes the instance. Called after configuration, and after
   * a call to setServices().
   */
  public abstract void initialize ();

  /**
   * Adds an additional CustomerInfo to the list
   */
  public void addCustomerInfo (CustomerInfo info)
  {
    if (null != customerInfos.get(info.getPowerType())) {
      log.error("PowerType " + info.getPowerType().toString()
                + " already specified");
    }
  }

  /**
   * Returns the CustomerInfo associated with this instance. It is up to
   * individual models to fill out the fields.
   */
  public CustomerInfo getCustomerInfo (PowerType pt)
  {
    return customerInfos.get(pt);
  }

  /**
   * Returns the list of CustomerInfo records associated with this customer
   * model.
   */
  public Collection<CustomerInfo> getCustomerInfos ()
  {
    return customerInfos.values();
  }

  /**
   * Called to run the model forward one step.
   */
  public abstract void step ();

  /**
   * Called to evaluate tariffs.
   */
  public abstract void evaluateTariffs (List<Tariff> tariffs);

  // ---------------- common attributes --------------
  /**
   * Returns the name of this customer instance. Names need not be unique, but
   * unique names will be of considerable value in understanding log file
   * entries.
   */
  public String getName ()
  {
    return name;
  }

  @ConfigurableValue(valueType = "String",
      description = "instance name - required")
  public void setName (String name)
  {
    this.name = name;
  }

  /**
   * Returns the current tariff subscriptions for this model
   */
  public List<TariffSubscription> getCurrentSubscriptions (PowerType powerType)
  {
    return tariffSubscriptionRepo
        .findActiveSubscriptionsForCustomer(getCustomerInfo(powerType));
  }
}

