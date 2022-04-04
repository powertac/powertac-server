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
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.RegulationCapacity;
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
  //private double minimumCapacity = 0.0;
  //private double maximumCapacity = 0.0;

  // Ratio of the number of units currently in service (for EVs, this is the number of
  // vehicles plugged in and not fully charged) to the total population (for EVs, this
  // is the number of chargers). Note that this value will always be 1.0 for models that
  // don't allow capacity to be added and removed.
  private double inServiceRatio = 1.0;

  // Mean usage per member in kW to achieve charging goal with maximum flexibility
  //private double nominalChargeRate = 0.0;

  // Capacity vector for the next week
  // This is a hard limit for the capacity lookahead
  private int ringArraySize = 168;
  private RingArray<StorageElement> capacityVector;
  
  // default constructor, used to create a dummy instance
  public StorageState ()
  {
    super();
    capacityVector = new RingArray<>(ringArraySize);
  }

  // normal constructor records the subscription it decorates
  public StorageState (TariffSubscription sub)
  {
    super();
    mySub = sub;
    capacityVector = new RingArray<> (ringArraySize);
  }

  /**
   * Transfers subscribers from another subscription having the specified StorageState.
   * Updates capacity and inServiceRatio values accordingly. Note that when this is called, the
   * subscriptions themselves have not yet been updated. For now we assume the "old" subscription does
   * not need to be updated because all the values are per-inservice-customer. 
   */
  public void moveSubscribers (int timeslotIndex, int count, StorageState oldState)
  {
    double fraction = (double) count / oldState.getPopulation(); 

    // if there were no existing subscribers, then we bring over a portion of the oldState
    // equal to the count being moved over.
    if (0 == getPopulation()) {
      capacityVector.clear();
      copyScaled(timeslotIndex, oldState, fraction);
      scaleState(timeslotIndex, oldState, 1.0 - fraction);
    }

    // Do we need to do anything with the subscription that's losing customers?
    else if (count > 0) {
      // Since we are taking weighted means, and since all the chargers have the same maximum
      // capacity, it's not possible here to violate capacity constraints.
      // First, we get the number of in-service customers for both populations
      double xfrActive = oldState.getInServiceRatio() * count;
      double originalActive = getInServicePopulation();

      
      
      double xfrIsr = oldState.getInServiceRatio() * xfrActive;
      double originalIsr = getInServiceRatio() * originalActive;
      inServiceRatio = (xfrIsr + originalIsr) / (xfrActive + originalActive);
      //nominalChargeRate = (minimumCapacity + maximumCapacity) / 2;
    }
  }

  // called to copy over a portion of the state from another subscription
  private void copyScaled (int timeslot, StorageState old, double fraction)
  {
    
  }

  // called to scale back values in a state that's losing customers
  private void scaleState(int timeslot, StorageState old, double fraction)
  {
    if (fraction > 1.0) {
      // Should not happen
      log.error("updateState called with fraction > 1");
      return;
    }
    else if (fraction < 0.0) {
      log.error("updateState called with negative fraction");
      return;
    }

    // All we need to do is scale back the capacityVector
    Consumer<StorageElement> operator = new Consumer<>() {
      public void accept (StorageElement item) {
        item.scale(fraction);
      }
    };
    capacityVector.operate(operator, timeslot);
  }

  /**
   * Distributes new demand over time
   */
  public void distributeDemand (int timeslot, List<DemandElement> newDemand,
                                Double ratio, Double regulation)
  {
    
  }

  /**
   * Computes the nominal demand for the current timeslot
   */
  public double getNominalDemand (int timeslot)
  {
    return 0.0;
  }

  /**
   * Retrieves the available regulation capacity for the current timeslot
   */
  public RegulationCapacity getRegulationCapacity (int timeslot)
  {
    return null;
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

  /** ---------------------------------------------------------------------------------------
   * Mutable element of the StorageState forward capacity vector for the EV Charger model.
   * Each represents the max and min capacity and the number of active chargers in a timeslot.
   * 
   * Max demand is simply the sum of individual capacities of the chargers, constrained by to
   * remaining unfilled capacity in the batteries of attached vehicles.
   * 
   * Min demand is the minimum amount that must be consumed in a given timeslot to meet the
   * charging requirements of attached vehicles, constrained by max demand.
   * 
   * @author John Collins
   *
   */
  class StorageElement // package visibility
  {
    // Number of active chargers
    private double activeChargers = 0.0;

    // Maximum total demand in a given timeslot, constrained by number of active chargers and
    // remaining unfilled capacity in attached vehicles
    private double maxDemand = 0.0;

    // Minimum demand is the lowest amount that can be consumed in a given timeslot, constrained by
    // unsatisfied charging demand in the current and future timeslots
    private double minDemand = 0.0;

    // Unsatisfied demand remaining in vehicles that will disconnect in this timeslot
    private double remainingCommitment = 0.0;

    StorageElement ()
    {
      super();
    }

    // Scale this element by a constant fraction
    void scale (double fraction)
    {
      activeChargers *= fraction;
      maxDemand *= fraction;
      minDemand *= fraction;
      remainingCommitment *= fraction;
    }
  }
}
