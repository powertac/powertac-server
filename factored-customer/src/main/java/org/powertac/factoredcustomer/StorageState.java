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
package org.powertac.factoredcustomer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.TariffSubscription;

/**
 * Records the current state of a storage-capable customer population subscribed to a particular tariff.
 * Intended to be used as a customerDecorator on a TariffSubscription.
 * 
 * @author John Collins
 */
public class StorageState
{
  private static Logger log =
          LogManager.getLogger(StorageState.class.getName());

  // The TariffSubscription decorated by this instance
  private TariffSubscription mySub;

  // Current SoC for the population subscribed to the associated tariff.
  // Value should be in [0.0, 1.0]
  private double stateOfCharge = 0.0;

  // Mean usage per member in kW to achieve charging goal with maximum flexibility
  private double nominalChargeRate = 0.0;
  
  // default constructor, used to create a dummy instance
  public StorageState ()
  {
    super();
  }

  // normal constructor records the subscription it decorates
  public StorageState (TariffSubscription sub)
  {
    super();
    mySub = sub;
  }

  /**
   * Transfers subscribers from another subscription having the specified StorageState.
   * Updates SoC and NCR accordingly
   */
  public void addSubscribers (int count, StorageState oldState)
  {
    // if there were no existing subscribers, then just transfer the numbers
    if (count == getPopulation()) {
      stateOfCharge = oldState.getSoC();
      nominalChargeRate = oldState.getNominalChargeRate();
    }
    // Otherwise compute and store the weighted means of the old and new values
    else {
      double xfrSoC = oldState.getSoC() * count;
      double xfrNCR = oldState.getNominalChargeRate() * count;
      double originalSoC = stateOfCharge * (getPopulation() - count); // original population
      double originalNCR = nominalChargeRate * (getPopulation() - count); // original population
      stateOfCharge = (xfrSoC + originalSoC) / getPopulation();
      nominalChargeRate = (xfrNCR + originalNCR) / getPopulation();
    }
  }

  // Note that this getter retrieves the population from the subscription, not the local value
  public int getPopulation ()
  {
    return mySub.getCustomersCommitted();
  }

  public double getSoC ()
  {
    return stateOfCharge;
  }

  public double getNominalChargeRate ()
  {
    return nominalChargeRate;
  }
}
