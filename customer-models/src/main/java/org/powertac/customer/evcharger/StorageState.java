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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.TariffSubscription;
import org.powertac.util.Pair;
import org.powertac.util.RingArray;

/**
 * Records the current state of a population of EV chargers subscribed to a
 * particular tariff. The code here depends strongly on being called
 * before subscriptions are updated in a given timeslot.
 * 
 * In each timeslot over some arbitrary horizon (limited by <code>ringArraySize</code>),
 * we keep track of the number of vehicles plugged in (the number of "active" chargers)
 * and the energy committed and not yet delivered in each future timeslot. We assume
 * that vehicles arrive and leave at the beginning of the arrival/departure timeslots.
 * That means they consume energy in the arrival timeslot, but not in the departure
 * timeslot.
 * 
 * Energy values are population values, not individual values. Individual values (if
 * needed) are given by the ratio of energy to the number of active chargers at any
 * given time.
 * 
 * @author John Collins
 */
public class StorageState
{
  private static Logger log =
          LogManager.getLogger(StorageState.class.getName());

  // The TariffSubscription associated with this instance
  private TariffSubscription mySub;

  // Capacity vector for the four days
  // This is a hard limit for the capacity lookahead
  private int ringArraySize = 96;
  private RingArray<StorageElement> capacityVector;

  // Capacity in kW of individual population units
  private double unitCapacity = 0.0;

  // Cached values for current timeslot
  private int cacheTimeslot = -1;
  private double minDemand = 0.0;
  private double maxDemand = 0.0;
  //private double nominalDemand = 0.0;
  
  // default constructor, used to create a dummy instance
  public StorageState (double unitCapacity)
  {
    super();
    this.unitCapacity = unitCapacity;
  }

  // normal constructor records the subscription it decorates
  public StorageState (TariffSubscription sub, double unitCapacity)
  {
    this(unitCapacity);
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
   * 
   * Maximum demand in each timeslot is the most that can be used. Unless the total
   * remaining commitments are less than what the population of chargers can use in a
   * single timeslot, then it's just the rated demand of the current active chargers.
   * We don't care about maximum demand in future timeslots, just the current one.
   * 
   * Minimum demand is the least that can be used in the current timeslot without
   * Jeopardizing future commitments.
   * 
   * Minim
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
   * Distributes exercised regulation over the horizon starting at timeslot - 1.
   * Note that a positive number means up-regulation, in which case we need to replace
   * that much energy.
   * 
   * NOTE: We must do this before distributing demand because the regulation only applies
   * to the vehicles that were plugged in during the last timeslot when we reported the
   * regulation capacity. We can ignore any commitment in the current timeslot because
   * we assume that's already been met and those vehicles will already be unplugged. 
   */
  public void distributeRegulation (int timeslot, double regulation)
  {
    // TODO -- this approach may be overly conservative
    if (regulation > 0.0) {
      // up-regulation, need to add this much to commitments
      double availableCapacity = 0.0;
      double remaining = regulation;
      int index = timeslot + 1; // current timeslot should have been cleared already
      while (remaining > 0.0) {
        StorageElement se = getElement(index);
        if (se.remainingCommitment == 0.0) {
          // can't do much here if there is no remaining commitment in this timeslot,
          // as long as this population does not support V2G 
          availableCapacity += se.getActiveChargers() * unitCapacity
                  - se.getRemainingCommitment();
          if (availableCapacity >= remaining) {
            // we can put the rest here
            se.addCommitment(remaining);
            remaining = 0.0;
          }
          else {
            // use what we can here and continue
            se.addCommitment(availableCapacity);
            remaining -= availableCapacity;
          }
        }
        index += 1;
      }
    }
    else if (regulation < 0.0) {
      // down-regulation, we got some extra energy to reduce commitments
      int index = timeslot;
      double remaining = -regulation;
      while (remaining > 0.0) {
        StorageElement se = getElement(index);
        // no need to worry here about non-zero remainingCommitment
        if (remaining <= se.remainingCommitment) {
          // dump it here and we're done
          se.reduceCommitment(remaining);
          remaining = 0.0;
        }
        else {
          remaining -= se.getRemainingCommitment();
          se.reduceCommitment(se.getRemainingCommitment());
        }
        index += 1;
      }
    }
  }

  /**
   * Distributes new demand over time. Demand for this subscription is the new demand
   * times the ratio of subscribers to the total population, as given by <code>ratio</code>.
   * NOTE that this code assumes the newDemand list is sorted by increasing timeslot.
   */
  public void distributeDemand (int timeslot, List<DemandElement> newDemand, Double ratio)
  {
    // what if the newDemand list is empty?
    if (null == newDemand || 0 == newDemand.size()) {
      return;
    }

    // We first must clear out the portion of the ring beyond the current max index
    // that we might want to use.
    capacityVector.clean(timeslot);

    // All the vehicles in newDemand start charging now, so we first have to find the total
    // activations for the current timeslot
    double activations = 0.0;
    int maxTimeslot = timeslot + getHorizon(timeslot);
    for (DemandElement de : newDemand) {
      activations += de.getNVehicles() * ratio;
      maxTimeslot = (int) Math.max(maxTimeslot, de.getHorizon() + timeslot);
    }

    // Now we walk through the timeslots between now and maxHorizon filling in the
    // activation counts and demand requirements
    Iterator<DemandElement> elements = newDemand.iterator();
    DemandElement nextDe = elements.next();
    for (int i = timeslot; i <= maxTimeslot && null != nextDe; i++) {
      StorageElement se = getElement(i);
      if (null == se) {
        // empty spot
        se = new StorageElement(0.0, 0.0);
        putElement(i, se);
      }
      if (i == nextDe.getHorizon() + timeslot) {
        activations -= nextDe.getNVehicles() * ratio;
        // fill in commitment
        se.addCommitment(nextDe.getRequiredEnergy() * ratio);
        StorageElement prev = getElement(i - 1);
        if (elements.hasNext()) {
          // go again if we haven't finished the list
          nextDe = elements.next();
        }
        else {
          nextDe = null;
        }
      }
      se.addChargers(activations); // add remaining activations
    }
  }

  /**
   * Computes the nominal demand for the current timeslot
   */
  public Pair<Double, Double> getMinMax (int timeslot)
  {
    computeFlexibility(timeslot);
    return new Pair<Double, Double>(minDemand, maxDemand);
  }

  /**
   * Retrieves the available regulation capacity for the current timeslot
   * Note that we connect the RC to its subscription, because that's the functionality
   * offered by RegulationCapacity. But we don't modify the subscription. That needs
   * to be done by the caller.
   */
//  public RegulationCapacity getRegulationCapacity (int timeslot, double nominalDemand)
//  {
//    RegulationCapacity result = new RegulationCapacity(mySub,
//                                                       nominalDemand - minDemand,
//                                                       maxDemand - nominalDemand);
//    return result;
//  }

  /**
   * Gathers and returns a list that represents the current state
   */
  public List<Object> gatherState (int timeslot)
  {
    ArrayList<Object> result = new ArrayList<>();
    System.out.println("horizon=" + getHorizon(timeslot));
    for (int i = timeslot; i < timeslot + getHorizon(timeslot); i++) {
      StorageElement se = getElement(i);
      List<Object> row = new ArrayList<>();
      row.add(i);
      row.add(se.activeChargers);
      row.add(se.remainingCommitment);
      result.add((Object) row);
    }
    return result;
  }

  // Flexibility needs to be re-computed in each timeslot. 
  private void computeFlexibility (int timeslot)
  {
    if (cacheTimeslot != timeslot) {
      // first, find the minimum demand in the current timeslot that leaves enough
      // to satisfy all future unfilled commitments
      double available = 0.0;
      // Walk through the capacityVector from the front and find the minDemand, which
      // is the 
      double totalCapacity = 0.0;
      double totalDemand = 0.0;
      maxDemand = unitCapacity * getElement(timeslot).getActiveChargers();
      for (int i = timeslot; i < timeslot + getHorizon(timeslot); i++) {
        StorageElement se = getElement(i);
        totalCapacity += se.getActiveChargers() * unitCapacity - se.getRemainingCommitment();
        // this is supposed to capture the biggest shortage
        minDemand = Math.max(minDemand, -totalCapacity);
        totalDemand += se.getRemainingCommitment();
      }
      // maximum demand is less than current capacity only if there's not enough future
      // demand to absorb it all.
      maxDemand = Math.min(maxDemand, totalDemand);
      cacheTimeslot = timeslot;
    }
  }

  // Returns the subscription attached to this SS
  TariffSubscription getSubscription ()
  {
    return mySub;
  }

  /**
   * Returns the population from the subscription, before any transfer actually takes place
   */
  int getPopulation ()
  {
    return mySub.getCustomersCommitted();
  }

  /**
   * Returns the capacity of individual chargers
   */
  double getUnitCapacity ()
  {
    return unitCapacity;
  }

  /**
   * Returns the time horizon for this state past the given timeslot.
   * This is the number of timeslots in the future for which we have active
   * charging commitments.
   */
  int getHorizon (int timeslot)
  {
    return capacityVector.getActiveLength(timeslot);
  }

  // Retrieves a specific StorageElement
  StorageElement getElement (int index)
  {
    return capacityVector.get(index);
  }

  // Stores an element at the specified location
  private void putElement (int index, StorageElement se)
  {
    capacityVector.set(index, se);
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

    // Unsatisfied demand remaining in vehicles that will disconnect in this timeslot
    private double remainingCommitment = 0.0;

    // default constructor
    StorageElement ()
    {
      super();
    }

    // populated constructor
    StorageElement (double activeChargers, double remainingCommitment)
    {
      super();
      this.activeChargers = activeChargers;
      this.remainingCommitment = remainingCommitment;
    }

    double getActiveChargers ()
    {
      return activeChargers;
    }

    void addChargers (double n)
    {
      activeChargers += n;
    }

    double getRemainingCommitment ()
    {
      return remainingCommitment;
    }

    void addCommitment (double newCommitment)
    {
      remainingCommitment += newCommitment;
    }

    void reduceCommitment (double reduction)
    {
      remainingCommitment -= reduction;
    }

    // returns a new StorageElement with the same contents as an old one
    StorageElement copy ()
    {
      return new StorageElement(getActiveChargers(), getRemainingCommitment());
    }

    // returns a new StorageElement containing a portion of an old element
    StorageElement copyScaled (double scale)
    {
      return new StorageElement(getActiveChargers() * scale,
                                getRemainingCommitment() * scale);
    }

    // adds a portion of an existing element to the contents of this one
    void addScaled (StorageElement element, double scale)
    {
      activeChargers += element.getActiveChargers() * scale;
      remainingCommitment += element.getRemainingCommitment() * scale;
    }

    // Scale this element by a constant fraction
    void scale (double fraction)
    {
      activeChargers *= fraction;
      remainingCommitment *= fraction;
    }

    
  }
}
