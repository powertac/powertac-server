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
import org.powertac.util.RingArray;

/**
 * Records the current state of a storage-capable customer population subscribed to a particular tariff.
 * Intended to be used as a <code>customerDecorator</code> on a <code>TariffSubscription</code>. The
 * code here depends strongly on being called before the subscriptions themselves are updated. In other
 * words, it needs to be called in the <code>notifyCustomer()</code> method, which in
 * <code>TariffEvaluator</code> happens before <code>updateSubscriptions()</code> is
 * called. Specifically, this means that the population numbers in the subscriptions themselves have
 * NOT yet been updated when <code>addSubscribers()</code> is called.
 * 
 * 
 * @author John Collins
 */
public class StorageState
{
  private static Logger log =
          LogManager.getLogger(StorageState.class.getName());

  // The TariffSubscription decorated by this instance
  private TariffSubscription mySub;

  // Current minimum and maximum capacity for the population subscribed to the associated tariff.
  private double minimumCapacity = 0.0;
  private double maximumCapacity = 0.0;

  // Ratio of the number of units currently in service (for EVs, this is the number of
  // vehicles plugged in) to the total population (for EVs, this is the number of chargers).
  // Note that this value will always be 1.0 for models that don't allow capacity to be added
  // and removed.
  private double inServiceRatio = 1.0;

  // Mean usage per member in kW to achieve charging goal with maximum flexibility
  private double nominalChargeRate = 0.0;

  // Capacity vector for the next 60 timeslots
  private int ringArraySize = 60;
  private RingArray capacityVector;
  
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
    capacityVector = new RingArray (ringArraySize);
  }

  /**
   * Transfers subscribers from another subscription having the specified StorageState.
   * Updates capacity and inServiceRatio values accordingly. Note that when this is called, the
   * subscriptions themselves have not yet been updated. For now we assume the "old" subscription does
   * not need to be updated because all the values are per-inservice-customer. 
   */
  public void addSubscribers (int count, StorageState oldState)
  {
    // if there were no existing subscribers, then just transfer the numbers
    // this works because all the numbers are per-inservice-instance.
    if (count == getPopulation()) {
      minimumCapacity = oldState.getMinCapacity();
      minimumCapacity = oldState.getMaxCapacity();
      inServiceRatio = oldState.getInServiceRatio();
      nominalChargeRate = oldState.getNominalChargeRate();
    }
    // We don't do anything with the subscription that's losing customers
    else if (count > 0) {
      // Since we are taking weighted means, and since all the chargers have the same maximum
      // capacity, it's not possible here to violate capacity constraints.
      // First, we get the number of in-service customers for both populations
      double xfrActive = oldState.getInServiceRatio() * count;
      double originalActive = getInServicePopulation();

      double xfrMin = oldState.getMinCapacity() * xfrActive;
      double originalMin = getMinCapacity() * originalActive; // original population
      minimumCapacity = (xfrMin + originalMin) / (xfrActive + originalActive);

      double xfrMax = oldState.getMaxCapacity() * xfrActive;
      double originalMax = getMaxCapacity() * originalActive; // original population
      maximumCapacity = (xfrMax + originalMax) / (xfrActive + originalActive);
      
      double xfrIsr = oldState.getInServiceRatio() * xfrActive;
      double originalIsr = getInServiceRatio() * originalActive;
      inServiceRatio = (xfrIsr + originalIsr) / (xfrActive + originalActive);
      nominalChargeRate = (minimumCapacity + maximumCapacity) / 2;
    }
  }

  // Min and max capacity
  public void setMinCapacity (double cap)
  {
    minimumCapacity = cap;
  }

  public double getMinCapacity ()
  {
    return minimumCapacity;
  }

  public void setMaxCapacity (double cap)
  {
    maximumCapacity = cap;
  }

  public double getMaxCapacity ()
  {
    return maximumCapacity;
  }

  // Proportion of total population that are active at the moment (plugged in for EVs)
  public void setInServiceRatio (double ratio)
  {
    inServiceRatio = ratio;
  }

  public double getInServiceRatio ()
  {
    return inServiceRatio;
  }

  // This is the population from the subscription, before the transfer actually takes place
  public int getPopulation ()
  {
    return mySub.getCustomersCommitted();
  }

  public double getInServicePopulation ()
  {
    return getPopulation() * getInServiceRatio();
  }

  public double getNominalChargeRate ()
  {
    return nominalChargeRate;
  }

  /**
   * Element of the capacity vector
   */
  class CapacityElement
  {
    
  }
}
