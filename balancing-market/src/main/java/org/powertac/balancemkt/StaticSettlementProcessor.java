/*
 * Copyright (c) 2012-2014 by the original author
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
package org.powertac.balancemkt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.powertac.common.RegulationAccumulator;
import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.repo.TariffRepo;
import org.powertac.util.Predicate;

/**
 * DU settlement processor for Scenario 2 - controllable capacities,
 * one-shot static solution
 * 
 * Naming convention: Price is per-unit, Cost is price * qty
 * 
 * @author John Collins, Mathijs de Weerdt
 */
public class StaticSettlementProcessor extends SettlementProcessor
{
  private double epsilon = 1e-6;  // 1 milliwatt-hour

  double pPlus, pMinus;
  double pPlusPrime, pMinusPrime;

  public StaticSettlementProcessor (TariffRepo tariffRepo,
                                    CapacityControl capacityControl)
  {
    super(tariffRepo, capacityControl);
  }

  /* (non-Javadoc)
   * @see org.powertac.balancemkt.SettlementProcessor#settle(java.util.Collection)
   */
  @Override
  public void settle (SettlementContext service,
                      List<ChargeInfo> brokerData)
  {
    pPlus = service.getPPlus();
    pPlusPrime = service.getPPlusPrime();
    pMinus = service.getPMinus();
    pMinusPrime = service.getPMinusPrime();

    // find total imbalance
    double totalImbalance = 0.0;
    double totalQty = 0.0;
    for (ChargeInfo info : brokerData) {
      totalImbalance += info.getNetLoadKWh();
      totalQty += Math.abs(info.getNetLoadKWh());
    }
    log.info("totalImbalance=" + totalImbalance);

    // fudge to prevent divide-by-zero errors
    if (Math.abs(totalImbalance) < epsilon) {
      if (totalQty < epsilon)
        // nothing to settle; just return
        return;
      totalImbalance = epsilon / 2.0; // make it slightly positive
    }
    double sgn = Math.signum(totalImbalance);

    // get balancing orders on correct side of imbalance, sort by price.
    // Negative total imbalance means we want to curtail consumption.
    SortedSet<BOWrapper> candidates =
      findCandidateOrders(brokerData, totalImbalance);

    // get curtailable usage for each order.
    ArrayList<BOWrapper> possibles = new ArrayList<BOWrapper> ();
    possibles.addAll(candidates);
    for (BOWrapper bo: possibles) {
      RegulationAccumulator cap =
        capacityControlService.getRegulationCapacity(bo.balancingOrder);
      log.info("tariff " + bo.balancingOrder.getTariffId()
               + ": up=" + cap.getUpRegulationCapacity()
               + ", down=" + cap.getDownRegulationCapacity());
      if (sgn < 0.0) {
        // up-regulation
        bo.availableCapacity = cap.getUpRegulationCapacity();
      }
      else {
        // down-regulation
        bo.availableCapacity = cap.getDownRegulationCapacity();
      }
      // drop the bo if it has insufficient capacity
      if (Math.abs(bo.availableCapacity) < epsilon)
        candidates.remove(bo);
    }

    // insert dummy orders to represent available balancing power through
    // the wholesale regulating market.
    insertDummyOrders(candidates, totalImbalance * 2);
    
    // determine the set that will be exercised.
    determineExerciseSet(totalImbalance, candidates);
    //log.info("satisfied " + satisfied + " through balancing orders");
    SortedSet<BOWrapper> nonExercised = determineNonExercisedSet(candidates);

    // compute VCG charges (p_2) by broker.
    HashSet<ChargeInfo> nonParticipants = new HashSet<ChargeInfo>(); 
    for (ChargeInfo info: brokerData) {
      info.setBalanceChargeP2(computeVcgCharges(info, totalImbalance,
                                                candidates, nonExercised,
                                                nonParticipants));
    }

    // Determine imbalance payments (p_1) for each broker.
    computeImbalanceCharges(brokerData, totalImbalance, candidates);
    
    // Exercise balancing controls
    for (ChargeInfo info : brokerData) {
      exerciseControls(info, candidates, info.getBalanceChargeP2());
    }
    if (log.isInfoEnabled()) {
      // log payments
      StringBuffer sb = new StringBuffer();
      sb.append("DU static settlement <broker(p2,p1)>:");
      for (ChargeInfo info : brokerData) {
        sb.append(" ").append(info.getBrokerName()).append("(");
        sb.append(info.getBalanceChargeP2()).append(",");
        sb.append(info.getBalanceChargeP1()).append(")");
      }
      log.info(sb.toString());

      // compute actual DU costs
      double rmCost = 0.0;
      for (BOWrapper bo: candidates) {
        if (bo.isDummy()) {
          rmCost =
            bo.exercisedCapacity * bo.getMarginalPrice(bo.exercisedCapacity);
        }
      }
      double brokerCost = 0.0;
      for (ChargeInfo info : brokerData) {
        brokerCost += info.getBalanceChargeP1() + info.getBalanceChargeP2();
      }

      // log budget balance
      log.info("DU budget: rm cost = " + rmCost + ", broker cost = "
               + brokerCost);
    }
  }

  // Produces the sorted list of balancing orders that are candidates
  // to be exercised.
  private SortedSet<BOWrapper>
    findCandidateOrders (List<ChargeInfo> brokerData, double totalImbalance)
  {
    TreeSet<BOWrapper> orders =
            new TreeSet<BOWrapper> (new BOComparator());
    
    Predicate<BalancingOrder> tester;
    if (totalImbalance < 0.0) {
      tester = new Predicate<BalancingOrder>() {
        @Override
        public boolean apply (BalancingOrder bo)
        {
          return (bo.getExerciseRatio() > 0.0);
        }
      };
    }
    else {
      tester = new Predicate<BalancingOrder>() {
        @Override
        public boolean apply (BalancingOrder bo)
        {
          return (bo.getExerciseRatio() < 0.0);
        }
      };
    }
    for (ChargeInfo info : brokerData) {
      List<BalancingOrder> balancingOrders = info.getBalancingOrders(); 
      if (null != balancingOrders && balancingOrders.size() > 0) {
        for (BalancingOrder bo : balancingOrders) {
          if (tester.apply(bo)) {
            BOWrapper bow = new BOWrapper(info, bo);
            orders.add(bow);
          }
        }
      }
    }
    return orders;
  }

  // Inserts orders into the candidate list derived from the regulating
  // market. This requires orders for both shortage and surplus
  private void insertDummyOrders (SortedSet<BOWrapper> orders,
                          double totalImbalance)
  {
    // first, make a single dummy order and insert it
    double price = pPlus;
    double slope = pPlusPrime;
    if (totalImbalance >= 0.0) {
      // we are in surplus; exercise production curtailments
      price = pMinus;
      slope = pMinusPrime;
    }
    BOWrapper dummy =
            new BOWrapper(-totalImbalance, price, slope, 0.0);
    orders.add(dummy);
    
    // split the dummy order around the following order, if there is one
    // and if the slope is non-zero
    if (dummy.slope != 0.0) {
      splitDummyOrder(orders, orders.tailSet(dummy));
    }
  }

  // Splits the dummy order around a higher-priced following order
  private void splitDummyOrder (SortedSet<BOWrapper> orders,
                                SortedSet<BOWrapper> tail)
  {
    if (tail.size() <= 1)
      // we're done -- dummy order is last
      return;
    
    // at this point, we have a dummy order at some price, followed by
    // at least one "real" order. The dummy order must be split in two at
    // the point where its price matches the price of the following order.
    Iterator<BOWrapper> bos = tail.iterator();
    BOWrapper dummy = bos.next();
    BOWrapper nextBO = bos.next();
    double capacity = (nextBO.price - dummy.price) / dummy.slope;

    // there are now three possibilities:
    // - capacity has the opposite sign from remaining capacity, which is
    //   an error; or
    if (Math.signum(capacity) != Math.signum(dummy.availableCapacity)) {
      log.error("Sign of needed capacity " + capacity +
                " != sign of dummy avail capacity " + dummy.availableCapacity);
    }
    // - capacity is at least as large as remaining capacity of dummy order,
    //   in which case we are finished; or
    else if (Math.abs(capacity) >= Math.abs(dummy.availableCapacity)) {
      return;
    }
    // - capacity is smaller than remaining capacity of the dummy order,
    //   in which case we split the dummy around the following order.
    else {
      BOWrapper newDummy = new BOWrapper(dummy.availableCapacity - capacity,
                                         nextBO.price + epsilon/1000.0,
                                         dummy.slope,
                                         capacity + dummy.startX);
      dummy.availableCapacity = capacity;
      orders.add(newDummy);
      splitDummyOrder(orders, orders.tailSet(newDummy));
    }
  }

  // Finds the set of balancing orders to be exercised, fills in their
  // exercised capacity, returns the total imbalance that is satisfied by
  // the balancing orders.
  private double determineExerciseSet (double totalImbalance,
                                       SortedSet<BOWrapper> candidates)
  {
    double remainingImbalance = totalImbalance;
    double sgn = Math.signum(totalImbalance);
    for (BOWrapper bo : candidates) {
      if (sgn * remainingImbalance <= 0.0)
        break;
      double exercise = Math.min(sgn * remainingImbalance,
                                 -sgn * bo.availableCapacity);
      bo.exercisedCapacity = -sgn * exercise;
      log.debug("exercising order " + bo.toString()
                + " for " + bo.exercisedCapacity + " at " + bo.price);
      remainingImbalance -= sgn * exercise;
    }
    return totalImbalance - remainingImbalance;
  }

  // returns the tail of the candidate list that is non-exercised
  SortedSet<BOWrapper>
    determineNonExercisedSet (SortedSet<BOWrapper> candidates)
  {
    BOWrapper lastExercised = candidates.first();
    for (BOWrapper bow: candidates) {
      if (0.0 == bow.exercisedCapacity)
        break;
      lastExercised = bow;
      if (Math.abs(bow.availableCapacity - bow.exercisedCapacity) > 0.0)
        // this one is partially exercised
        break;
    }
    // lastExercised should not be null
    if (null == lastExercised) {
      log.warn("unable to settle: lastExercised is null");
      return null;
    }
    return candidates.tailSet(lastExercised);
  }

  // Computes VCG charge (p_2) for a broker represented by target,
  // by integrating the area under the non-exercised offers up to the
  // exercised quantity of balancing orders from target.
  // Offers from the nonParticipants set are excluded
  private double computeVcgCharges (ChargeInfo target,
                                    double totalImbalance,
                                    SortedSet<BOWrapper> candidates,
                                    SortedSet<BOWrapper> nonExercised,
                                    Set<ChargeInfo> nonParticipants)
  {
    // compute capacity (remainingQty) offered by nonParticipants
    double targetRemainingQty = 0;
    double sgn = Math.signum(totalImbalance);
    for (BOWrapper bow : candidates) {
      if (bow.availableCapacity != 0.0 && 0.0 == bow.exercisedCapacity)
        break;
      if (target == bow.info)
        targetRemainingQty += bow.exercisedCapacity;
      if (Math.abs(bow.availableCapacity - bow.exercisedCapacity) > 0.0)
        // stop on the last one
        break;
    }
    // compute price if this capacity is retrieved from other brokers
    double price = 0;
    nonParticipants.add(target);
    //double rmQty = 0.0;
    //double rmMarginalPrice = 0.0;

    for (BOWrapper nextNonExercised: nonExercised) {
      if (Math.abs(targetRemainingQty) < epsilon)
        break;
      else if (!(nonParticipants.contains(nextNonExercised.info))) {
        double avail =
          (nextNonExercised.availableCapacity
              - nextNonExercised.exercisedCapacity);
        double used = sgn * Math.max(sgn * avail, sgn * targetRemainingQty);
        price += sgn * nextNonExercised.getTotalNECost(used);
        targetRemainingQty -= used;
        log.debug("  VCG cost part of "
                  + nextNonExercised.getTotalNECost(used) + " for " + used
                  + " kWh at " + nextNonExercised.price);
      }
    }
    //price -= rmQty * rmMarginalPrice;
    nonParticipants.remove(target);
    if(Math.abs(targetRemainingQty) > epsilon)
      log.error("Not enough orders to compute VCG price.");
    log.debug("VCG price" + " is " + price );
    return -price; // result is positive for credit to the broker
  }

  // Computes imbalance costs for each broker. This is
  //    VCG(C,X)/X * x
  // where
  //    X is the total imbalance, 
  //    VCG(C,X) is the sum of VCG payments to other brokers
  //       (except that for brokers whose imbalance has the same sign as the 
  //       total imbalance it excludes balancing orders from brokers whose
  //       imbalance has the opposite sign) plus the additional cost of
  //       external regulating power.
  //    x is the broker's individual imbalance.
  private void computeImbalanceCharges (List<ChargeInfo> brokerData,
                                        double totalImbalance,
                                        SortedSet<BOWrapper> candidates)
  {
    HashSet<ChargeInfo> contributors = new HashSet<ChargeInfo>();
    HashSet<ChargeInfo> nonContributors = new HashSet<ChargeInfo>();
    double sgn = Math.signum(totalImbalance);

    // handle the no-imbalance case
    if (Math.abs(totalImbalance) < epsilon) {
      // This is supposed to be the cheapest, but the condition is too
      // rare to waste time on. We would have to find the cheapest controllable
      // capacities on both sides, so it's not enough to use the candidates
      // list.
      double penaltyPlus = pPlus;
      double penaltyMinus = pMinus;
      for (ChargeInfo info : brokerData) {
        double sign = Math.signum(info.getNetLoadKWh());
        if (sign < 0.0)
          info.setBalanceChargeP1(penaltyPlus * info.getNetLoadKWh());
        else
          info.setBalanceChargeP1(-penaltyMinus * info.getNetLoadKWh());
      }
      return;
    }

    // First, separate the brokers into contributors and non-contributors
    for (ChargeInfo info: brokerData) {
      if (info.getNetLoadKWh() != 0
          && sgn != Math.signum(info.getNetLoadKWh())) {
        // broker is on the other side of the balance
        nonContributors.add(info);
      }
      else
        contributors.add(info);
    }
    
    // Do the contributors - the brokers on the imbalance side
    for (ChargeInfo broker : contributors) {
      
      // find a new sequence of non-exercised orders, excluding the
      // non-contributors and broker
      nonContributors.add(broker);
      SortedSet<BOWrapper> remains = filterOrders(candidates, nonContributors);
      determineExerciseSet(totalImbalance, remains);
      SortedSet<BOWrapper> nonExercised = determineNonExercisedSet(remains);
      
      // get the cost of regulating power
      double rpCost = findRpCost(remains);

      // imbalanceCost is the cost of regulating power plus the sum of
      // vcg payments for each of the other brokers. For contributors, we
      // do not include offers from non-contributors
      double imbalanceCost = rpCost;
      // include only the contributors
      for (ChargeInfo target : contributors) {
        if (target != broker) {
          imbalanceCost -= computeVcgCharges(target, totalImbalance,
                                             remains, nonExercised,
                                             nonContributors);
        }
      }
      nonContributors.remove(broker);
      broker.setBalanceChargeP1(-sgn * imbalanceCost * broker.getNetLoadKWh()
                                / totalImbalance);
    }

    // do the non-contributors
    HashSet<ChargeInfo> excludes = new HashSet<ChargeInfo>();
    for (ChargeInfo info : nonContributors) {
      excludes.add(info);
      SortedSet<BOWrapper> remains = filterOrders(candidates, excludes);
      determineExerciseSet(totalImbalance, remains);
      SortedSet<BOWrapper> nonExercised = determineNonExercisedSet(remains);
      
      // get the cost of regulating power
      double imbalanceCost = findRpCost(remains);

      // include all other brokers
      for (ChargeInfo target : brokerData) {
        if (target != info) {
          excludes.add(target);
          imbalanceCost -= computeVcgCharges(target, totalImbalance,
                                             remains, nonExercised,
                                             excludes);
          excludes.remove(target);
        }
      }
      excludes.remove(info);
      info.setBalanceChargeP1(-sgn * imbalanceCost * info.getNetLoadKWh()
                              / totalImbalance);
    }
  }

  // gets the regulating cost across the dummy orders in remains
  private double findRpCost (SortedSet<BOWrapper> remains)
  {
    double rpCost = 0.0;
    //double rpQty = 0.0;
    for (BOWrapper bid : remains) {
      // this code depends on encountering the dummy orders in order
      if (bid.isDummy() && bid.exercisedCapacity != 0.0)
        // cost is total dummy qty times final marginal price
        //rpQty += bid.exercisedCapacity;
        //rpCost = -rpQty * bid.getMarginalPrice(bid.exercisedCapacity);
        rpCost = -bid.getTotalECost();
    }
    return rpCost;
  }

  // filter a list of candidated orders to exclude 
  private SortedSet<BOWrapper> filterOrders (SortedSet<BOWrapper> candidates,
                                             HashSet<ChargeInfo> exclude)
  {
    TreeSet<BOWrapper> remains =
            new TreeSet<BOWrapper> (new BOComparator());
    for (BOWrapper bow : candidates) {
      if (!(exclude.contains(bow.info))) {
        // create a new wrapper for this one so we can recompute exercised qty
        remains.add(bow.duplicate());
      }
    }
    return remains;
  }

  private void exerciseControls (ChargeInfo broker,
                                 SortedSet<BOWrapper> candidates,
                                 double settlementValue)
  {
    for (BOWrapper candidate: candidates) {
      if (candidate.info == broker && 0.0 != candidate.exercisedCapacity) {
        // add up the exercised capacity first
        broker.addCurtailment(candidate.exercisedCapacity);
      }
    }        
        
    for (BOWrapper candidate: candidates) {
      if (candidate.info == broker && 0.0 != candidate.exercisedCapacity) {
        // pro-rate settlement value over balancing orders
        double boValue =
            settlementValue
            * candidate.exercisedCapacity
            / broker.getCurtailment();
        capacityControlService
            .exerciseBalancingControl(candidate.balancingOrder,
                                      candidate.exercisedCapacity,
                                      boValue);
      }
    }
  }

  // wrapper class for tracking order status
  class BOWrapper implements Cloneable
  {
    ChargeInfo info= null;
    BalancingOrder balancingOrder = null;
    double availableCapacity = 0.0;
    double exercisedCapacity = 0.0;
    double price = 0.0;
    double slope = 0.0;
    double startX = 0.0;

    // constructs one from a BalancingOrder
    BOWrapper (ChargeInfo info, BalancingOrder bo)
    {
      super();
      this.info = info;
      this.balancingOrder = bo;
      this.price = bo.getPrice();
    }

    // constructs an intermediate dummy
//    BOWrapper (double availableCapacity, double price)
//    {
//      super();
//      this.availableCapacity = availableCapacity;
//      this.price = price;
//    }

    // constructs a dummy with a non-zero slope
    BOWrapper (double availableCapacity, double price,
               double slope, double startX)
    {
      super();
      this.availableCapacity = availableCapacity;
      this.price = price;
      this.slope = slope;
      this.startX = startX; // keeps track of x-distance to first dummy
    }

    // constructs a clone
    BOWrapper duplicate ()
    {
      try {
        return (BOWrapper) super.clone();
      }
      catch (CloneNotSupportedException e) {
        e.printStackTrace();
        return null;
      }
    }

    // Dummy orders don't wrap balancing orders.
    boolean isDummy ()
    {
      return (null == balancingOrder);
    }

    // Returns the total capacity
    double getCapacity ()
    {
      return availableCapacity;
    }

    // Returns the marginal price for using qty from the order
    double getMarginalPrice (double qty)
    {
      return price + slope * qty;
    }

    // Returns the total cost (integral) for using qty from the 
    // non-exercised portion of order
    double getTotalNECost (double qty)
    {
      //double nePrice = getMarginalPrice(exercisedCapacity);
      //return qty * 0.5 * (nePrice + nePrice + slope * qty);
      double oldMPrice = getMarginalPrice(exercisedCapacity);
      double newMPrice = getMarginalPrice(exercisedCapacity + qty);
      return newMPrice * qty +        // cost of the additional qty 
             (newMPrice - oldMPrice) * exercisedCapacity + // extra cost for the already exercisedCapacity
             startX * (newMPrice - price);                 // extra cost for any earlier dummy orders
    }
    
    // Returns total cost of this order, including its effect on earlier dummy orders
    double getTotalECost ()
    {
      double mp1 = 0.0;
      if (startX != 0.0)
        mp1 = getMarginalPrice(startX);
      double mp2 = getMarginalPrice(startX + exercisedCapacity);
      double cost = mp2 * exercisedCapacity + // for exercisedCapacity
            startX * (mp2 - mp1);    // extra cost for any earlier dummy orders
      //return Math.signum(exercisedCapacity) * cost;
      return cost;
    }

    @Override
    public String toString ()
    {
      if (null == balancingOrder)
        return "Dummy";
      else
        return (balancingOrder.getBroker().getUsername()
                + ":" + balancingOrder.getTariffId()
                + ":" + price
                + ":" + availableCapacity
                + ":" + exercisedCapacity);
    }
  }

  class BOComparator implements Comparator<BOWrapper>
  {
    @Override
    public int compare (BOWrapper b0,
                        BOWrapper b1) {
      if (b0 == b1)
        return 0;
      if (b0.price < b1.price)
        return -1;
      return 1;
    }
  }
}
