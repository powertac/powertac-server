/*
 * Copyright (c) 2012 by the original author
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
package org.powertac.distributionutility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.powertac.common.Tariff;
import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.repo.TariffRepo;
import org.powertac.util.Predicate;

/**
 * DU settlement processor for Scenario 2 - controllable capacities,
 * one-shot static solution
 * @author 
 */
public class StaticSettlementProcessor extends SettlementProcessor
{
  private double epsilon = 1e-6;  // 1 milliwatt-hour
  //private SettlementContext service;
  double pPlus, pMinus;
  double pPlusPrime, pMinusPrime;
  
  StaticSettlementProcessor (TariffRepo tariffRepo, CapacityControl capacityControl)
  {
    super(tariffRepo, capacityControl);
  }
  
  /* (non-Javadoc)
   * @see org.powertac.distributionutility.SettlementProcessor#settle(java.util.Collection)
   */
  @Override
  public void settle (SettlementContext service,
                      List<ChargeInfo> brokerData)
  {
    //this.service = service;
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
    log.debug("totalImbalance=" + totalImbalance);
    // fudge to prevent divide-by-zero errors
    if (Math.abs(totalImbalance) < epsilon) {
      if (totalQty < epsilon)
        // nothing to settle; just return
        return;
      totalImbalance = epsilon * totalQty;
    }
    
    // get balancing orders on correct side of imbalance, sort by price.
    // Negative total imbalance means we want to curtail consumption.
    SortedSet<BOWrapper> candidates =
            filterAndSort(brokerData, totalImbalance);

    // get curtailable usage for each order.
    for (BOWrapper bo : candidates) {
      bo.availableCapacity =
              capacityControlService.getCurtailableUsage(bo.balancingOrder);
    }

    // insert dummy orders to represent available balancing power through
    // the wholesale regulating market.
    insertDummyOrders(candidates, totalImbalance * 2);
    
    // determine the set that will be exercised.
    double satisfied = determineExerciseSet(totalImbalance, candidates);
    BOWrapper lastExercised = candidates.first();
    for (BOWrapper bow : candidates) {
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
      return;
    }

    // compute VCG charges (p_2) by broker.
    SortedSet<BOWrapper> nonExercised = candidates.tailSet(lastExercised);
    HashSet<ChargeInfo> nonParticipants = new HashSet<ChargeInfo>(); 
    for (ChargeInfo info : brokerData) {
      nonParticipants.add(info);
      info.setBalanceChargeP2
        (computeVcgCharges(totalImbalance,
                           candidates, nonExercised,
                           nonParticipants));
      nonParticipants.remove(info);
    }
    
    // Determine imbalance payments (p_1) for each broker.
    computeImbalanceCharges(brokerData, totalImbalance, candidates, nonExercised);
    
    // Exercise balancing controls
    for (ChargeInfo info : brokerData) {
      exerciseControls(info, candidates, info.getBalanceChargeP2());
    }
  }

  // Produces the sorted list of balancing orders that are candidates
  // to be exercised.
  private SortedSet<BOWrapper> filterAndSort (List<ChargeInfo> brokerData,
                                              double totalImbalance)
  {
    TreeSet<BOWrapper> orders =
            new TreeSet<BOWrapper> (new Comparator<BOWrapper> () {
              @Override
              public int compare (BOWrapper b0,
                                  BOWrapper b1) {
                if (b0 == b1)
                  return 0;
                if (b0.price < b1.price)
                  return -1;
                return 1;
              }
            });
    
    Predicate<BalancingOrder> tester;
    if (totalImbalance < 0.0) {
      tester = new Predicate<BalancingOrder>() {
        @Override
        public boolean apply (BalancingOrder bo)
        {
          Tariff tariff = tariffRepo.findTariffById(bo.getTariffId());
          return (tariff.getPowerType().isConsumption());
        }
      };
    }
    else {
      tester = new Predicate<BalancingOrder>() {
        @Override
        public boolean apply (BalancingOrder bo)
        {
          Tariff tariff = tariffRepo.findTariffById(bo.getTariffId());
          return (tariff.getPowerType().isProduction());
        }
      };
    }
    for (ChargeInfo info : brokerData) {
      List<BalancingOrder> balancingOrders = info.getBalancingOrders(); 
      if (null != balancingOrders && balancingOrders.size() > 0) {
        for (BalancingOrder bo : info.getBalancingOrders()) {
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
            new BOWrapper(-totalImbalance, price, slope);
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
                                         nextBO.price + epsilon,
                                         dummy.slope);
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
                + " for " + bo.exercisedCapacity);
      remainingImbalance -= sgn * exercise;
    }
    return totalImbalance - remainingImbalance;
  }

  // Computes VCG charge (p_2) for a broker represented by brokerData,
  // by integrating the area under the non-exercised offers.
  // Offers from the nonParticipants set are excluded
  private double computeVcgCharges (double totalImbalance,
                                    SortedSet<BOWrapper> candidates,
                                    SortedSet<BOWrapper> nonExercised,
                                    Set<ChargeInfo> nonParticipants)
  {
    //HashSet<ChargeInfo> nonParticipants = new HashSet<ChargeInfo>();
    //for (ChargeInfo info : brokerData) {
    //  nonParticipants.add(info);
    //  double newMC = computeMarginalPrice(totalImbalance, 
    //                                      candidates, 
    //                                      nonParticipants);
    //  nonParticipants.remove(info);
    //  info.setBalanceChargeP2(exerciseControls(info, candidates, newMC));
    //}
    // compute capacity (remainingQty) offered by nonParticipants
    double remainingQty = 0;
    double sgn = Math.signum(totalImbalance);
    for (BOWrapper bow : candidates) {
      if (0.0 == bow.exercisedCapacity)
        break;
      if (nonParticipants.contains(bow.info))
        remainingQty += bow.exercisedCapacity;
      if (Math.abs(bow.availableCapacity - bow.exercisedCapacity) > 0.0)
        // stop on the last one
        break;
    }
    // compute price if this capacity is retrieved from other brokers
    double price = 0;
    for (BOWrapper nextNonExercised : nonExercised) {
      if (Math.abs(remainingQty) < epsilon) 
        break;
      else if (!(nonParticipants.contains(nextNonExercised.info))) {
        double avail = (nextNonExercised.availableCapacity - nextNonExercised.exercisedCapacity);
        double used = sgn * Math.max(sgn * avail,
                                     sgn * remainingQty);
        price += sgn * nextNonExercised.getTotalPrice(used);
        remainingQty -= used;
        log.debug("  VCG cost part of " + nextNonExercised.getTotalPrice(used) + " for " + used + " kWh" );
      }
    }
    if(Math.abs(remainingQty) > epsilon)
      log.error("Not enough orders to compute VCG price.");
    log.debug("VCG price" + " is " + price );
    return price;
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
                                        SortedSet<BOWrapper> candidates,
                                        SortedSet<BOWrapper> nonExercised)
  {
    HashSet<ChargeInfo> contributors = new HashSet<ChargeInfo>();
    HashSet<ChargeInfo> nonContributors = new HashSet<ChargeInfo>();
    double sgn = Math.signum(totalImbalance); 
    for (ChargeInfo info : brokerData) {
      if (sgn != Math.signum(info.getNetLoadKWh())) {
        // broker is on the other side of the balance
        nonContributors.add(info);
      }
      else
        contributors.add(info);
    }
    
    // get the cost of regulating power
    double rpCost = 0.0;
    for (BOWrapper bid : candidates) {
      if (bid.isDummy())
        rpCost -= (bid.exercisedCapacity
                   * bid.getMarginalPrice(bid.exercisedCapacity));
    }
    
    // Do the contributors - the brokers on the imbalance side
    for (ChargeInfo info : contributors) {
      nonContributors.add(info);
      // imbalanceCost is the cost of regulating power plus the sum of
      // vcg payments for each of the other brokers. For contributors, we
      // do not include offers from non-contributors
      double imbalanceCost = rpCost;
      for (ChargeInfo exclude : contributors) {
        if (exclude != info) {
          nonContributors.add(exclude);
          imbalanceCost -= computeVcgCharges(totalImbalance,
                                             candidates, nonExercised,
                                             nonContributors);
          nonContributors.remove(exclude);
        }
      }
      nonContributors.remove(info);
      info.setBalanceChargeP1(imbalanceCost * info.getNetLoadKWh()
                              / totalImbalance);
    }

    // handle the no-imbalance case
//    if (Math.abs(totalImbalance) < epsilon) {
//      double penalty = pPlus;
//      if (totalImbalance < 0.0)
//        penalty = pMinus;
//      for (ChargeInfo info : nonContributors) {
//        info.setBalanceChargeP1(penalty * info.getNetLoadKWh());
//      }
//    }
//    else {
    // do the non-contributors
    HashSet<ChargeInfo> excludes = new HashSet<ChargeInfo>();
    for (ChargeInfo info : nonContributors) {
      excludes.add(info);
      double imbalanceCost = rpCost;
      for (ChargeInfo exclude : contributors) {
        if (exclude != info) {
          excludes.add(exclude);
          imbalanceCost += computeVcgCharges(totalImbalance,
                                             candidates, nonExercised,
                                             excludes);
          excludes.remove(exclude);
        }
      }
      excludes.remove(info);
      info.setBalanceChargeP1(imbalanceCost * info.getNetLoadKWh()
                              / totalImbalance);
    }
//  }
  }
  
//  private double computeMarginalPrice (double imbalance,
//                                       SortedSet<BOWrapper> candidates,
//                                       Set<ChargeInfo> exclude)
//  {
//    double result = 0.0;
//    double sgn = Math.signum(imbalance); // neg for deficit
//    double remaining = -imbalance;
//    for (BOWrapper bow : candidates) {
//      if ((null == exclude) || !(exclude.contains(bow.info))) {
//        if (sgn * bow.getCapacity() < (sgn * remaining + epsilon)) {
//          // this is the last one
//          result = bow.getMarginalPrice(remaining);
//          break;
//        }
//        else {
//          remaining -= bow.getCapacity();
//        }
//      }
//    }
//    return result;
//  }
  
  private void exerciseControls (ChargeInfo broker,
                                 SortedSet<BOWrapper> candidates,
                                 double settlementPrice)
  {
    for (BOWrapper candidate: candidates) {
      if (candidate.info == broker && 0.0 != candidate.exercisedCapacity) {
        capacityControlService.exerciseBalancingControl(candidate.balancingOrder,
                                                        candidate.exercisedCapacity,
                                                        candidate.exercisedCapacity * settlementPrice);
      }
    }
  }
  
//  private double getExercisedCapacity (ChargeInfo broker,
//                                       SortedSet<BOWrapper> candidates)
//  {
//    double result = 0.0;
//    for (BOWrapper candidate: candidates) {
//      if (candidate.info == broker) {
//        result += candidate.exercisedCapacity;
//      }
//    }
//    return result;
//  }

  // wrapper class for tracking order status
  class BOWrapper
  {
    ChargeInfo info= null;
    BalancingOrder balancingOrder = null;
    double availableCapacity = 0.0;
    double exercisedCapacity = 0.0;
    double price = 0.0;
    double slope = 0.0;
    
    // construct one from a BalancingOrder
    BOWrapper (ChargeInfo info, BalancingOrder bo)
    {
      super();
      this.info = info;
      this.balancingOrder = bo;
      this.price = bo.getPrice(); 
    }
    
    // construct an intermediate dummy
    BOWrapper (double availableCapacity, double price)
    {
      super();
      this.availableCapacity = availableCapacity;
      this.price = price;
    }
    
    // construct a final dummy, with a non-zero slope
    BOWrapper (double availableCapacity, double price, double slope)
    {
      super();
      this.availableCapacity = availableCapacity;
      this.price = price;
      this.slope = slope;
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
    
    // Returns the total price (integral) for using qty from the order
    double getTotalPrice (double qty)
    {
      return qty * 0.5 * (price + price + slope * qty);
    }
    
    @Override
    public String toString ()
    {
      if (null == balancingOrder)
        return "Dummy";
      else
        return (balancingOrder.getBroker().getUsername()
                + ":" + balancingOrder.getTariffId());
    }
  }
}
