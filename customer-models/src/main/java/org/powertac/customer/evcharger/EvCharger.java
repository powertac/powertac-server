/**
 * Copyright (c) 2022 by John Collins.
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
package org.powertac.customer.evcharger;

import java.util.List;

import org.powertac.common.CapacityProfile;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.customer.AbstractCustomer;

/**
 * 
 * @author John Collins
 */
public class EvCharger extends AbstractCustomer implements CustomerModelAccessor
{

  @Override
  public CustomerInfo getCustomerInfo ()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CapacityProfile getCapacityProfile (Tariff tariff)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getBrokerSwitchFactor (boolean isSuperseding)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getTariffChoiceSample ()
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getInertiaSample ()
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getShiftingInconvenienceFactor (Tariff tariff)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void notifyCustomer (TariffSubscription oldsub,
                              TariffSubscription newsub, int population)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void step ()
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    // TODO Auto-generated method stub

  }

}
