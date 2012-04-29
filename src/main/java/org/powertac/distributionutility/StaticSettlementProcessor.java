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
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
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
public class StaticSettlementProcessor implements SettlementProcessor
{
  static Logger log = Logger.getLogger(StaticSettlementProcessor.class.getName());

  TariffRepo tariffRepo;
  CapacityControl capacityControlService;
  
  /* (non-Javadoc)
   * @see org.powertac.distributionutility.SettlementProcessor#settle(java.util.Collection)
   */
  @Override
  public void settle (DistributionUtilityService service,
                      List<ChargeInfo> brokerData)
  {
    tariffRepo = service.getTariffRepo();
    capacityControlService = service.getCapacityControlService();
    // find total imbalance
    double totalImbalance = 0.0;
    for (ChargeInfo info : brokerData) {
      totalImbalance += info.getNetLoadKWh();
    }
    log.debug("totalImbalance=" + totalImbalance);
    
    // get balancing orders on correct side of imbalance, sort by price
    // include the "dummy" order for power from the regulating market.
    // Negative total imbalance means we want to curtail consumption.
    SortedSet<BOWrapper> candidates =
            filterAndSort(brokerData, totalImbalance);
    BOWrapper dummy =
            new BOWrapper(-totalImbalance,
                          (totalImbalance < 0.0) ?
                                 service.getPPlus() : service.getPMinus());
    candidates.add(dummy);

    // get curtailable usage for each order that is not superseded by the
    // regulating market.
    for (BOWrapper bo : candidates) {
      double avail =
              capacityControlService.getCurtailableUsage(bo.balancingOrder);
      bo.availableCapacity = avail;
    }
    
    // determine the set that will be exercised.
    determineExerciseSet(totalImbalance, candidates);
    BOWrapper lastExercised = null;
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

    // compute VCG prices by broker.
    SortedSet<BOWrapper> nonExercised = candidates.tailSet(lastExercised);
    computeVcgPayments(brokerData, totalImbalance, candidates, nonExercised);
    
    // determine imbalance payments, subtract prices for exercised orders,
    // update ChargeInfo instances.
    double pPlus = service.getPPlus();
    double pMinus = service.getPMinus();
    for (ChargeInfo info : brokerData) {
      double charge = 0.0;
      double net = info.getNetLoadKWh(); 
      if (net < 0.0)
        // deficit - charge pPlus
        charge = -pPlus * net;
      else
        // surplus - charge pMinus
        charge = pMinus * net;
      log.info("Broker " + info.getBrokerName() + " balance charge = " + charge);
      info.setBalanceCharge(charge + info.getBalanceCharge());
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
      for (BalancingOrder bo : info.getBalancingOrders()) {
        if (tester.apply(bo)) {
          BOWrapper bow = new BOWrapper(info, bo);
          orders.add(bow);
        }
      }
    }
    return orders;
  }

  // Finds the set of balancing orders to be exercised, fills in their
  // exercised capacity.
  private void determineExerciseSet (double totalImbalance,
                                     SortedSet<BOWrapper> candidates)
  {
    double remainingImbalance = totalImbalance;
    double sgn = Math.signum(totalImbalance);
    for (BOWrapper bo : candidates) {
      if (sgn * remainingImbalance <= 0.0)
        break;
      double exercise = Math.min(sgn * remainingImbalance,
                                 sgn * bo.availableCapacity);
      bo.exercisedCapacity = sgn * exercise;
      remainingImbalance -= sgn * exercise;
    }
  }

  // Computes VCG payments for each broker that has balancing orders to be
  // exercised, and then exercises them.
  private void computeVcgPayments (List<ChargeInfo> brokerData,
                                   double totalImbalance,
                                   SortedSet<BOWrapper> candidates,
                                   SortedSet<BOWrapper> nonExercised)
  {
    double sgn = Math.signum(totalImbalance); // neg for production curtailment
    for (ChargeInfo info : brokerData) {
      ArrayList<BOWrapper> exercised = new ArrayList<BOWrapper>();
      double exercisedQty = 0.0;
      for (BOWrapper bow : candidates) {
        if (0.0 == bow.exercisedCapacity)
          break;
        else if (bow.info == info) {
          exercised.add(bow);
          exercisedQty += bow.exercisedCapacity;
        }
      }
      // we now have the list of exercised orders for this broker.
      if (exercised.size() > 0) {
        // walk the tail of the set, integrating the marginal cost of
        // removing this broker from the exercised set.
        double marginalCost = 0.0;
        double remainingQty = exercisedQty;
        Iterator<BOWrapper> nextOrders = nonExercised.iterator();
        Iterator<BOWrapper> exercisedOrders = exercised.iterator();
        BOWrapper nextOrder = nextOrders.next();
        double avail = (nextOrder.availableCapacity
                        - nextOrder.exercisedCapacity);
        BOWrapper nextExercised = exercisedOrders.next();
        double exerciseValue = 0.0; // integrated value of nextExercised
        double nextRemainingQty = nextExercised.exercisedCapacity;
        while (Math.abs(remainingQty) > 0.0) {
          double used = sgn * Math.min(sgn * avail,
                                       sgn * nextRemainingQty);
          exerciseValue += sgn * used * nextOrder.price;
          marginalCost += sgn * used * nextOrder.price;
          remainingQty -= used;
          // two cases:
          if (used == avail) {
            // run off the end of nextOrder
            nextOrder = nextOrders.next();
            avail = (nextOrder.availableCapacity
                    - nextOrder.exercisedCapacity);
          }
          else if (exercisedOrders.hasNext()) {
            // run off the end of nextExercised - time to do the exercise
            capacityControlService.exerciseBalancingControl(nextExercised.balancingOrder,
                                                            nextExercised.exercisedCapacity,
                                                            exerciseValue / nextExercised.exercisedCapacity);
            nextExercised = exercisedOrders.next();
            nextRemainingQty = nextExercised.exercisedCapacity;
            exerciseValue = 0.0;
          }
          else if (Math.abs(remainingQty) > 0.0) {
            // trouble...
            log.warn("Out of orders to process for broker " + info.getBrokerName()
                     + " with qty " + remainingQty + " remaining");
          }
        }
        // we can now compute the payment for this broker
        double payment = marginalCost / exercisedQty;
        log.info("VCG payment for " + info.getBrokerName()
                 + ": " + payment);
        info.setBalanceCharge(payment);
      }
    }
  }

  // wrapper class for tracking order status
  class BOWrapper
  {
    ChargeInfo info= null;
    BalancingOrder balancingOrder = null;
    double availableCapacity = 0.0;
    double exercisedCapacity = 0.0;
    double price = 0.0;
    
    // construct one from a BalancingOrder
    BOWrapper (ChargeInfo info, BalancingOrder bo)
    {
      super();
      this.info = info;
      this.balancingOrder = bo;
      this.price = bo.getPrice(); 
    }
    
    // construct a dummy
    BOWrapper (double availableCapacity, double price)
    {
      super();
      this.availableCapacity = availableCapacity;
      this.price = price;
    }
  }
}
