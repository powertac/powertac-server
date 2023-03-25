/*
 * Copyright (c) 2013, 2022 by John Collins
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
package org.powertac.common.interfaces;

import org.powertac.common.CapacityProfile;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;

/**
 * Defines an interface for access to customer model details
 * that support tariff evaluation. This includes generation of per-tariff
 * customer usage/production profiles. 
 * Required by {@link org.powertac.common.TariffEvaluationHelper}.
 * Profiles can be for a full day, a full week,
 * or whatever time period makes sense for the customer. Usage may be for
 * a single individual customer or for the entire population.
 * Results are normalized by the tariff evaluation process, so the 
 * only requirement is that all profiles for a given customer use the
 * same time period (and the same weather), and the same population.
 *
 * @author John Collins
 */
public interface CustomerModelAccessor
{
  /**
   * Returns the CustomerInfo instance for this customer model.
   */
  public CustomerInfo getCustomerInfo ();

  /**
   * Returns a capacity profile for the given tariff. This must represent
   * the usage of a single individual in a population model over some
   * model-specific time period.
   */
  public CapacityProfile getCapacityProfile (Tariff tariff);

  /**
   * Returns a [0,1] value representing the inconvenience of switching brokers.
   * The value may depend
   * on whether the current subscription is being switched to a superseding
   * tariff as a result of revocation.
   */
  public double getBrokerSwitchFactor (boolean isSuperseding);

  /**
   * Returns a [0,1] random value used to make choices using the logit choice
   * model.
   */
  public double getTariffChoiceSample ();

  /**
   * Returns a [0,1] random value used to choose whether individual customers
   * evaluate tariffs or not.
   */
  public double getInertiaSample ();

  /**
   * Returns a [0,1] value representing the inconvenience of dealing with
   * curtailment in exchange for a lower price.
   */
  public double getShiftingInconvenienceFactor (Tariff tariff);

  /**
   * Notifies customer of subscription changes.
   */
  public void notifyCustomer (TariffSubscription oldsub, TariffSubscription newsub, int population);
}
