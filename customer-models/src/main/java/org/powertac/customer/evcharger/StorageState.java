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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.TariffSubscription;
import org.powertac.util.RingArray;

/**
 * Records the current state of a population of EV chargers subscribed to a
 * particular tariff. Intended to be used as a <code>customerDecorator</code> on a
 * <code>TariffSubscription</code>. The code here depends strongly on being called
 * before the subscriptions themselves are updated.
 * 
 * In each timeslot over some arbitrary horizon (limited by <code>ringArraySize</code>),
 * we keep track of the number of vehicles plugged in (the number of "active" chargers)
 * the current aggregate state-of-charge of the attached vehicles, and the energy committed
 * and not yet delivered by that time. We assume that vehicles arrive and leave at the
 * beginning of the arrival/departure timeslots. That means they consume energy in the
 * arrival timeslot, but not in the departure timeslot.
 * 
 * Energy values are population values, not individual values. Individual values (if
 * needed) are given by the ratio of energy to the subscribed population.
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
   * Updates the capacityVector accordingly.
   * 
   * NOTE that when this is called, the subscriptions themselves have not yet been updated.
   * In other words, it needs to be called in the <code>notifyCustomer()</code> method,
   * which in <code>TariffEvaluator</code> happens before <code>updateSubscriptions()</code>
   * is called. Specifically, this means that the population numbers in the subscriptions
   * themselves have NOT yet been updated when <code>moveSubscribers()</code> is called.
   */
  public void moveSubscribers (int timeslotIndex, int count, StorageState oldState)
  {
    double fraction = (double) count / oldState.getPopulation(); 

    // if there were no existing subscribers, then we bring over a portion of the oldState
    // equal to the count being moved over.
    if (0 == getPopulation()) {
      capacityVector.clear();
      copyScaled(timeslotIndex, oldState, fraction);
    }

    // Do we need to do anything with the subscription that's losing customers?
    else if (count > 0) {
      // Since we are using weighted means, and since all the chargers have the same maximum
      // capacity, it's not possible here to violate capacity constraints.
      addScaled(timeslotIndex, oldState, fraction);

      // First, we get the number of in-service customers for both populations
      //double xfrActive = oldState.getInServiceRatio() * count;
      //double originalActive = getInServicePopulation();

      //double xfrIsr = oldState.getInServiceRatio() * xfrActive;
      //double originalIsr = getInServiceRatio() * originalActive;
      //inServiceRatio = (xfrIsr + originalIsr) / (xfrActive + originalActive);
      //nominalChargeRate = (minimumCapacity + maximumCapacity) / 2;
    }
    // in either case, we have to scale back the old state
    scaleState(timeslotIndex, oldState, 1.0 - fraction);
  }

  // Copies over a portion of the state from another subscription.
  // Since we assume the current state is empty, we start by clearing it.
  private void copyScaled (int timeslot, StorageState from, double fraction)
  {
    capacityVector.clear();
    for (int i = timeslot; i < timeslot + from.getHorizon(timeslot); i++) {
      capacityVector.set(i, from.getElement(i).copyScaled(fraction));
    }
  }

  // Add a portion of the state from another subscription to this state.
  private void addScaled (int timeslot, StorageState from, double fraction)
  {
    for (int i = timeslot; i < timeslot + from.getHorizon(timeslot); i++) {
      capacityVector.set(i, from.getElement(i).copyScaled(fraction));
    }
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
    
    // walk through the active array and scale it
    for (int i = 0; i < old.getHorizon(timeslot); i++) {
      StorageElement element = old.getElement(i + timeslot);
      element.scale(fraction);
    }
  }

  /**
   * Distributes exercised regulation over the horizon starting at timeslot
   */
  public void distributeRegulation (int timeslot, double regulation)
  {
    
  }

  /**
   * Distributes new demand over time
   */
  public void distributeDemand (int timeslot, List<DemandElement> newDemand, Double ratio)
  {
    
  }

  /**
   * Computes the nominal demand for the current timeslot
   */
  public double getNominalDemand (int timeslot)
  {
    StorageElement current = capacityVector.get(timeslot);
    return (current.getMinDemand() + current.getMaxDemand()) / 2.0;
  }

  /**
   * Retrieves the available regulation capacity for the current timeslot
   * Note that we connect the RC to its subscription, because that's the functionality
   * offered by RegulationCapacity. But we don't modify the subscription. That needs
   * to be done by the caller.
   */
  public RegulationCapacity getRegulationCapacity (int timeslot)
  {
    StorageElement current = getElement(timeslot);
    double nominal = (current.getMinDemand() + current.getMaxDemand()) / 2.0;
    RegulationCapacity result = new RegulationCapacity(mySub,
                                                       nominal - current.getMinDemand(),
                                                       current.getMaxDemand() - nominal);
    return result;
  }

  /**
   * Returns the proportion of total subscriber population that are active (plugged in)
   * in the given timeslot. Presumably this number is identical for all subscriptions.
   * Note that 
   */
  public double getInServiceRatio (int timeslot)
  {
    return getElement(timeslot).getActiveChargers() / getPopulation();
  }

  /**
   * Returns the population from the subscription, before any transfer actually takes place
   */
  public int getPopulation ()
  {
    return mySub.getCustomersCommitted();
  }

  public double getInServicePopulation (int timeslot)
  {
    return capacityVector.get(timeslot).getActiveChargers();
  }

  /**
   * Returns the time horizon for this state past the given timeslot.
   * This is the number of timeslots in the future for which we have active
   * charging commitments.
   */
  public int getHorizon (int timeslot)
  {
    return capacityVector.getActiveLength(timeslot);
  }

  // Retrieves a specific StorageElement
  private StorageElement getElement (int index)
  {
    return capacityVector.get(index);
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

    // default constructor
    StorageElement ()
    {
      super();
    }

    // populated constructor
    StorageElement (double activeChargers, double maxDemand,
                    double minDemand, double remainingCommitment)
    {
      super();
      this.activeChargers = activeChargers;
      this.maxDemand = maxDemand;
      this.minDemand = minDemand;
      this.remainingCommitment = remainingCommitment;
    }

    double getActiveChargers ()
    {
      return activeChargers;
    }

    double getMaxDemand ()
    {
      return maxDemand;
    }

    double getMinDemand ()
    {
      return minDemand;
    }

    double getRemainingCommitment ()
    {
      return remainingCommitment;
    }

    // returns a new StorageElement with the same contents as an old one
    StorageElement copy ()
    {
      return new StorageElement(getActiveChargers(),
                                getMaxDemand(),
                                getMinDemand(),
                                getRemainingCommitment());
    }

    // returns a new StorageElement containing a portion of an old element
    StorageElement copyScaled (double scale)
    {
      return new StorageElement(getActiveChargers() * scale,
                                getMaxDemand() * scale,
                                getMinDemand() * scale,
                                getRemainingCommitment() * scale);
    }

    // adds a portion of an existing element to the contents of this one
    void addScaled (StorageElement element, double scale)
    {
      activeChargers += element.getActiveChargers() * scale;
      maxDemand += element.getMaxDemand() * scale;
      minDemand += element.getMinDemand() * scale;
      remainingCommitment += element.getRemainingCommitment() * scale;
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
