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

import java.util.List;

import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.WeatherReportRepo;

/**
 * Customer models must implement this interface.
 * @author John Collins
 */
@ConfigurableInstance
public abstract class AbstractCustomer
{
  protected String name = "dummy";
  protected CustomerInfo customerInfo;

  // Services available to subclasses, populated by setServices()
  protected WeatherReportRepo weatherReportRepo;
  protected RandomSeedRepo randomSeedRepo;
  protected TariffRepo tariffRepo;
  protected TariffSubscriptionRepo tariffSubscriptionRepo;

  // current tariff subscription
  protected TariffSubscription currentSubscription;

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
    this.customerInfo = new CustomerInfo(name, 1);
  }

  /**
   * Populates the instance with service pointers to avoid Spring dependency.
   * @param tariffSubscriptionRepo 
   * @param tariffRepo 
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
   * Subclasses must call this method to get the CustomerInfo registered.
   * @param randomSeedRepo 
   */
  public void initialize ()
  {
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

  /**
   * Returns the CustomerInfo associated with this instance. It is up to
   * individual models to fill out the fields.
   */
  public CustomerInfo getCustomerInfo ()
  {
    return customerInfo;
  }

  @ConfigurableValue(valueType = "String",
      description = "instance name - required")
  public void setName (String name)
  {
    this.name = name;
  }

  /**
   * Sets the current subscription for this model.
   * Note that this won't work for multi-contracting models.
   */
  public void setCurrentSubscription (TariffSubscription subscription)
  {
    currentSubscription = subscription;
  }

  /**
   * Returns the current tariff subscription for this model
   */
  public TariffSubscription getCurrentSubscription ()
  {
    return currentSubscription;
  }
}

