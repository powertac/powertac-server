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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.TariffSubscription;
import org.powertac.util.RingArray;

import cern.colt.Arrays;

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
  static Logger log =
          LogManager.getLogger(StorageState.class.getName());

  // The TariffSubscription associated with this instance
  private TariffSubscription mySub;

  // Capacity vector covers four days
  // This is a hard limit for the capacity lookahead
  private int ringArraySize = 96;
  private int startIndex = 0;
  private RingArray<StorageElement> capacityVector;

  // Capacity in kW of individual population units
  private double unitCapacity = 0.0;
  private double epsilon = 1e-9;

  // Cached values for current timeslot
  private double currentMin = 0.0;
  private double currentUsage = 0.0;
  
  /**
   * Default constructor, used to create a dummy instance.
   * The unitCapacity is the charger capacity in kW.
   * We ignore for now the inefficiency of the charger, which is
   * typically in the 85%-95% range.
   */
  public StorageState (double unitCapacity)
  {
    super();
    this.unitCapacity = unitCapacity;
  }

  /**
   * Normal constructor records the subscription it decorates,
   * specifies the maximum time (in timeslots) that we track. This is the
   * maximum interval a user can specify when plugging in, and should cover
   * the large majority of actual cases.
   */
  public StorageState (TariffSubscription sub, double unitCapacity, int maxHorizon)
  {
    this(unitCapacity);
    mySub = sub;
    ringArraySize = maxHorizon;
    capacityVector = new RingArray<>(ringArraySize);
  }

  /**
   * Private copy constructor, access using the copy() method
   */
  private StorageState (StorageState original)
  {
    this(original.unitCapacity);
    this.mySub = original.mySub;
    this.ringArraySize = original.ringArraySize;
    this.capacityVector = new RingArray<>(this.ringArraySize);
    this.startIndex = original.startIndex;
    for (int i = startIndex; i < startIndex
            + original.capacityVector.getActiveLength(startIndex); i++) {
      this.capacityVector.set(i, original.capacityVector.get(i).copy());
    }
  }

  /**
   * Returns a copy without sharing structure
   */
  public StorageState copy()
  {
    return new StorageState(this);
  }

  /**
   * Restores the current state at the start of a sim session.
   * The record is a list containing the initial timeslot
   * and a sequence of StorageElement instances.
   */
//  @SuppressWarnings("unchecked")
  static public StorageState restoreState
  (double unitCapacity, TariffSubscription sub,
   int maxHorizon, List<Object> bootRecord)
  {
    StorageState result =
            new StorageState(sub, unitCapacity, maxHorizon);
    int first = (int) bootRecord.get(0);
    result.setStartIndex(first);
    for (int index = 1; index < bootRecord.size(); index++) {
      StorageElement next = (StorageElement) bootRecord.get(index);
      result.putElement(first + index - 1, next);
//    List<Object> record = (List<Object>) bootRecord;
//    int ts = (int) record.get(0);
//    for (int index = 1; index < record.size(); index++) {
//      StorageElement element = (StorageElement) record.get(index);
//      putElement(ts++, element);
    }
    return result;
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
   * jeopardizing future commitments.
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
    if (fraction > (1.0 + epsilon)) {
      // Should not happen
      log.error("updateState called with fraction > 1");
      return;
    }
    else if (fraction < -epsilon) {
      log.error("updateState called with negative fraction");
      return;
    }

    // walk through the active array and scale it
    for (StorageElement element : old.getElementList(timeslot)) {
      element.scale(fraction);
    }
  }

  /**
   * Distributes new demand over time. Demand for this subscription is the new demand
   * times the ratio of subscribers to the total population, as given by <code>ratio</code>.
   * NOTE that this code assumes the newDemand list is sorted by increasing timeslot. It also
   * assumes no constraint violations in the new demand vector.
   */
  public void distributeDemand (int timeslot, List<DemandElement> newDemand,
                                Double ratio)
  {
    // what if the newDemand list is empty?
    if (null == newDemand || 0 == newDemand.size()) {
      return;
    }

    // We first must clear out the portion of the ring beyond the current max index
    // that we might want to use.
    capacityVector.clean(timeslot);
    setStartIndex(timeslot);

    // All the vehicles in newDemand start charging now, so we first have to find
    // the total activations for the current timeslot
    double activations = 0.0;
    int maxTimeslot = 0;
    for (DemandElement de : newDemand) {
      activations += de.getNVehicles() * ratio;
      maxTimeslot = (int) Math.max(maxTimeslot, de.getHorizon() + timeslot);
    }

    // Now we walk through the timeslots between now and maxHorizon filling in the
    // activation counts and demand requirements
    Iterator<DemandElement> elements = newDemand.iterator();
    DemandElement nextDe = elements.next();
    // turn nextDe.energy into an array based on nextDe.distribution
    // we assume without checking that the entries in nextDe.distribution
    // add up to 1.0.
    for (int i = timeslot; i <= maxTimeslot && null != nextDe; i++) {
      int arrayLength = i - timeslot + 1;
      StorageElement se = getElement(i);
      if (null == se) {
        // empty spot
        se = new StorageElement(i - timeslot + 1);
        putElement(i, se);
      }

      // add remaining activations regardless of whether there's demand for this ts
      se.addChargers(activations);
      if (i == nextDe.getHorizon() + timeslot) {
        activations -= nextDe.getNVehicles() * ratio;
        // distribute nextDe population and energy according to distribution
        double[] allocations = nextDe.getdistribution();
        double[] pop = new double [arrayLength];
        double[] energy = new double [arrayLength];

        int nValues = (int) Math.round(Math.min(arrayLength, allocations.length));
        for (int ix = 0; ix < nValues; ix++) {
          if (Double.isNaN(allocations[ix])) {
            log.error("NaN demand ts {}, h {}, i {}",
                      timeslot, nextDe.getHorizon(), ix);
            continue;
          }
          else if (allocations[ix] < epsilon) {
            continue;
          }
          pop[ix] = nextDe.getNVehicles() * allocations[ix] * ratio;
          energy[ix] = getUnitCapacity() * pop[ix] * (arrayLength - ix - 0.5);
        }
        if (allocations.length > se.getEnergy().length) {
          // Should never happen
          log.error("ts {} h {}, se array length {} should be {}",
                    timeslot, i - timeslot,
                    se.getEnergy().length, allocations.length);
        }
        se.addCommitments(pop, energy);
        if (elements.hasNext()) {
          // go again if we haven't finished the list
          nextDe = elements.next();
        }
        else {
          nextDe = null;
        }
      }
    }
    if (log.isInfoEnabled()) {
      StringBuffer buf = new StringBuffer();
      int horizon = maxTimeslot - timeslot;
      buf.append(String.format("StorageState [p] [e], h = %d:", horizon));
      for (int i = 0; i < (int) Math.min(6,  horizon); i++) {
        buf.append("\n   ");
        StorageElement se = getElement(timeslot + i);
        if (null == se) {
          log.error("Null StorageElement at {} of {}",
                    timeslot, maxTimeslot);
        }
        else {
          buf.append(se.toString());
          buf.append(", r").append(Arrays.toString(se.getRatios(unitCapacity)));
        }
      }
      log.info(buf);
    }
  }

  /**
   * Distributes energy usage in the current timeslot across the connected EVs. 
   * All of the demand that's needed by vehicles that will disconnect in this timeslot
   * needs to be covered first. Second is full charger capacity for the vehicles that
   * have no remaining flexibility. The remaining usage is shared evenly across the
   * remaining chargers. After distributing usage, each future timeslot needs to be
   * re-balanced.
   * 
   * Note that capacity is the amount for the current subscription, not the total
   * usage for the customer.
   */
  public void distributeUsage(int timeslot, double capacity)
  {
    // set up cached values to handle regulation
    currentUsage = capacity;
    double[] minMax = getMinMax(timeslot);
    currentMin = minMax[0];

    double remainingCapacity = capacity;
    // Start by finishing off the current timeslot
    StorageElement target = getElement(timeslot);
    if (null == target) {
      // can't do much here
      log.warn("No StorageElement at ts {}", timeslot);
      return;
    }
    // this one should have only one bundle
    double[] energy = target.getEnergy();
    if (energy.length > 1) {
      // big problem
      log.error("Unsatisfiable demand {} in current timeslot{}", energy.toString(), timeslot);
      for (int i = 0; i < energy.length - 1; i++) {
        remainingCapacity -= unitCapacity * target.getPopulation()[i];
      }
    }
    else {
      remainingCapacity -=
              energy[0];
      energy[0] = 0.0;
    }

    // Next, we have to run the critical chargers in the remaining timeslots
    for (int ts = timeslot + 1;
            ts < timeslot + capacityVector.getActiveLength(timeslot); ts++) {
      target = getElement(ts);
      double[] pop = target.getPopulation();
      energy = target.getEnergy();
      double usage = Math.min(unitCapacity * pop[0], energy[0]);
      target.energy[0] -= usage;
      remainingCapacity -= usage;
    }

    // Now we have to divide up the remaining energy among the remaining chargers
    // Remaining chargers are all but the 0 element in future timeslots.
    // Also, the last cohort in each timeslot is typically half-power.
    double remainingDemand = 0;
    for (int ts = timeslot + 1;
            ts < timeslot + capacityVector.getActiveLength(timeslot); ts++) {
      target = getElement(ts);
      for (int p = 1; p < target.getPopulation().length; p++) {
        double hrEnergy = Math.min(target.getPopulation()[p] * getUnitCapacity(),
                                   target.getEnergy()[p]);
        remainingDemand += hrEnergy;
      }
    }

    // We now know how much we could use and how much we have.
    // We spread out the actual capacity evenly across all
    // the remaining chargers
    if (0.0 == remainingDemand) {
      if (Math.abs(remainingCapacity) > epsilon) {
        log.error("Remaining capacity = {} with remainingDemand = 0.0",
                  remainingCapacity);
      }
    }
    else {
      double capacityRatio = remainingCapacity / remainingDemand;
      for (int ts = timeslot + 1;
              ts < timeslot + capacityVector.getActiveLength(timeslot); ts++) {
        target = getElement(ts);
        for (int e = 1; e < target.getEnergy().length; e++) {
          double hrEnergy = Math.min(target.getPopulation()[e] * getUnitCapacity(),
                                     target.getEnergy()[e]);
          // here's where we allocate energy
          target.getEnergy()[e] -= hrEnergy * capacityRatio;
        }
      }
    }
  }

  /**
   * Distributes exercised regulation over the horizon starting at timeslot.
   * Note that a positive number means up-regulation, in which case we need to
   * replace that much energy.
   * 
   * NOTE: We must do this before collapsing elements and rebalancing,
   * and before distributing demand because the regulation only
   * applies to the vehicles that were plugged in during the last timeslot when we
   * reported the regulation capacity. We can ignore any commitment in the previous
   * timeslot because we assume that's already been met and those vehicles will
   * already be unplugged.
   * 
   * Returns the residue in kWh that could not be accommodated.
   * Return value should always be zero.
   */
  public double distributeRegulation (int timeslot, double regulation)
  {
    // Regulation applies to all StorageElements in this and all future
    // timeslots. The first column is never regulated.
    if (Math.abs(regulation) < epsilon) {
      return 0.0;
    }

    double result = 0.0;
    // First, we know how much regulation capacity was reported
    // and how much was used
    // Reported capacity was the amount by which
    // discretionary usage could be cut in the last timeslot.
    // We want a ratio to be applied to every group in every cohort.
    if (Math.abs(currentUsage - currentMin) < epsilon) {
      // cannot do up-regulation in this case
      if (regulation > 0.0) {
        log.error("up-regulation {} requested in ts {}, no available capacity",
                  regulation, timeslot);        
        return result;
      }
      // TODO - apply down-regulation in this case
      log.warn("ts {} request for down-regulation from min usage not implemented");
      return result;
    }
    double ratio = (currentUsage + regulation - currentMin)
            / (currentUsage - currentMin);
    log.info("Regulation {}, currentUsage={}, currentMin={}, ratio={}",
             regulation, currentUsage, currentMin, ratio);
    // Usage has already been reported, but collapse/rebalance has not happened
    for (int ts = timeslot;
            ts < timeslot + capacityVector.getActiveLength(timeslot);
            ts++) {
      StorageElement target = getElement(ts);
      // we need to increase or decrease discretionary usage by ratio
      // note that usage in the first group is not discretionary
      double[] pop = target.getPopulation();
      double[] energy = target.getEnergy();
      for (int i = 1; i < pop.length; i++) {
        double minMultiplier = Math.max(pop.length -i - 0.5, 0.0);
        double minE = minMultiplier * pop[i] * getUnitCapacity(); // min remaining demand
        double du = energy[i] - minE; // discretionary usage
        double dr = du * ratio;       // regulated usage
        energy[i] = minE + dr;
        result += (du - dr); // positive for up-reg, negative for down-reg
      }
    }
    return regulation - result;
  }

  /**
   * Closes out a timeslot by reducing the length of all the population and
   * energy arrays by 1. This should work because the last index now needs at most
   * one hour to complete charging, as does the previous index. This must be done
   * in the timeslot following demand and usage distribution, and before re-balancing.
   */
  public void collapseElements (int timeslot)
  {
    for (int ts = timeslot;
            ts < timeslot + capacityVector.getActiveLength(timeslot); ts++) {
      StorageElement target = getElement(ts);
      // last index, if not already complete, must be folded into the previous index
      int lastIndex = target.getEnergy().length - 1;
      if (target.getEnergy()[lastIndex] < -epsilon) {
        // very strange
        log.error("negative demand {} timeslot {}", target.getEnergy()[lastIndex], ts);
        target.getEnergy()[lastIndex] = 0.0;
        target.getPopulation()[lastIndex] = 0.0;
      }
      else if (target.getEnergy()[lastIndex] > epsilon) {
        // Move remaining demand and population in the last index
        // up to the previous index
        target.getEnergy()[lastIndex - 1] += target.getEnergy()[lastIndex];
        target.getPopulation()[lastIndex - 1] += target.getPopulation()[lastIndex];
      }
      target.collapseArrays();
    }
  }

  // Shifts portions of the population in each future timeslot toward
  // higher-demand groups in case
  // less than the full demand was satisfied in the previous timeslot. This must
  // be done after distributing regulation and collapsing arrays in the current timeslot,
  // and before distributing demand and usage.
  public void rebalance (int timeslot)
  {            
    // At this point, demand for the first cohort should be reduced by a full
    // charger-hour, but other cohorts may not be, so we need to move a portion
    // of each of the remaining cohorts up to the next higher-demand (lower index)
    // population.
    for (int ts = timeslot + 1;
            ts < timeslot + capacityVector.getActiveLength(timeslot); ts++) {
      // we skip the first StorageElement, which should be fully satisfied
      StorageElement target = getElement(ts);
      target.rebalance(unitCapacity);
    }
  }

  /**
   * Computes the minimum, maximum, and nominal demand
   * for the current timeslot. Returns an array containing
   * [minimum, maximum, midpoint] demand values.
   */
  double[] getMinMax (int timeslot)
  {
    // Minimum demand includes enough for the current timeslot plus the
    // amounts needed for the full-power cohorts in all future timeslots
    double minDemand = 0.0;
    double maxDemand = 0.0;
    StorageElement target = getElement(timeslot);
    if (null == target || 0 == target.getEnergy().length) {
      // nothing to work with here
      return new double[] {0.0, 0.0, 0.0};
    }
    // The first one has only one cohort that must be completely satisfied
    minDemand += target.getEnergy()[0];
    for (int ts = timeslot + 1;
            ts < timeslot + getHorizon(timeslot); ts++) {
      target = getElement(ts);
      double[] pop = target.getPopulation();
      // Add must-run chargers from future timeslots to minDemand
      minDemand += Math.min(target.getEnergy()[0], pop[0] * getUnitCapacity());
      // Add a full chunk from each future timeslot to maxDemand
      for (int i = 1; i < pop.length; i++) {
        maxDemand += Math.min(target.getEnergy()[i],
                              pop[i] * getUnitCapacity());
      }
    }
    maxDemand += minDemand;
    return new double[] {minDemand, maxDemand,
                         minDemand + (maxDemand - minDemand) / 2.0};
  }

  /**
   * Gathers and returns a list that represents the current state
   */
  public List<Object> gatherState (int timeslot)
  {
    ArrayList<Object> result = new ArrayList<>();
    //System.out.println("horizon=" + getHorizon(timeslot));
    result.add(timeslot);
    for (int i = timeslot; i < timeslot + getHorizon(timeslot); i++) {
      result.add(getElement(i));
    }
    return result;
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
   * Returns or sets the capacity of individual chargers
   */
  double getUnitCapacity ()
  {
    return unitCapacity;
  }

  StorageState withUnitCapacity (double capacity)
  {
    if (unitCapacity < -epsilon) {
      log.error("Invalid unit capacity {}", capacity);
    }
    else {
      unitCapacity = capacity;
    }
    return this;
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

  // Returns the capacityVector as a list
  List<StorageElement> getElementList (int start)
  {
    return capacityVector.asList(start);
  }

  // Stores an element at the specified location
  private void putElement (int index, StorageElement se)
  {
    capacityVector.set(index, se);
  }

  // returns the current start index for the capacity array
  int getStartIndex ()
  {
    return startIndex;
  }

  void setStartIndex (int index)
  {
    startIndex = index;
  }
}
