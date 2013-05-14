/*
* Copyright 2011-2013 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an
* "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
* either express or implied. See the License for the specific language
* governing permissions and limitations under the License.
*/

package org.powertac.factoredcustomer;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;
import org.apache.log4j.Logger;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffEvaluationHelper;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.factoredcustomer.interfaces.*;
import org.powertac.factoredcustomer.utils.SeedIdGenerator;

/**
 * Key class responsible for managing the tariff(s) for one customer across
 * multiple capacity bundles if necessary.
 * 
 * @author Prashant Reddy, John Collins
 */
@Domain
class DefaultUtilityOptimizer implements UtilityOptimizer
{
  protected Logger log =
          Logger.getLogger(DefaultUtilityOptimizer.class.getName());

  private FactoredCustomerService service;

  protected static final int NUM_HOURS_IN_DAY = 24;
  protected static final long MEAN_TARIFF_DURATION = 5; // number of days
  protected static int tariffEvalCount = 5; // # of tariffs/powerType to eval

  // Evaluation parameters

  protected final CustomerStructure customerStructure;
  protected final List<CapacityBundle> capacityBundles;

  //protected final List<Tariff> ignoredTariffs = new ArrayList<Tariff>();
  protected Random inertiaSampler;
  protected Random tariffSelector;

  protected final TariffEvaluationHelper tariffEvalHelper =
    new TariffEvaluationHelper();

  protected int tariffEvaluationCounter = 0;
  protected TariffEvalCache cache;
  protected int allocationChunks = 50; // target number of allocation chunks
  protected HashMap<Tariff, Integer> allocations;

  protected List<CapacityBundle> bundlesWithRevokedTariffs =
    new ArrayList<CapacityBundle>();

  DefaultUtilityOptimizer (CustomerStructure structure,
                           List<CapacityBundle> bundles)
  {
    customerStructure = structure;
    capacityBundles = bundles;

    cache = new TariffEvalCache();
  }

  @Override
  public void initialize (FactoredCustomerService service)
  {
    this.service = service;
    inertiaSampler =
      new Random(getRandomSeedRepo()
              .getRandomSeed("factoredcustomer.DefaultUtilityOptimizer",
                             SeedIdGenerator.getId(), "InertiaSampler")
              .getValue());
    tariffSelector =
      new Random(getRandomSeedRepo()
              .getRandomSeed("factoredcustomer.DefaultUtilityOptimizer",
                             SeedIdGenerator.getId(), "TariffSelector")
              .getValue());

    subscribeDefault();
  }
  
  // ----- Access components through service to support mocking ------

  protected RandomSeedRepo getRandomSeedRepo ()
  {
    return service.getRandomSeedRepo();
  }

  protected TariffMarket getTariffMarket ()
  {
    return service.getTariffMarket();
  }

  protected TariffSubscriptionRepo getTariffSubscriptionRepo ()
  {
    return service.getTariffSubscriptionRepo();
  }
  
  protected TariffRepo getTariffRepo ()
  {
    return service.getTariffRepo();
  }
  
  protected TimeslotRepo getTimeslotRepo ()
  {
    return service.getTimeslotRepo();
  }  

  // /////////////// TARIFF EVALUATION //////////////////////

  @StateChange
  protected void subscribe (Tariff tariff, CapacityBundle bundle,
                            int customerCount, boolean verbose)
  {
    getTariffMarket().subscribeToTariff(tariff, bundle.getCustomerInfo(),
                                          customerCount);
    if (verbose)
      log.info(bundle.getName() + ": Subscribed " + customerCount
               + " customers to tariff " + tariff.getId() + " successfully");
  }

  @StateChange
  protected void unsubscribe (TariffSubscription subscription,
                              CapacityBundle bundle, int customerCount,
                              boolean verbose)
  {
    subscription.unsubscribe(customerCount);
    if (verbose)
      log.info(bundle.getName() + ": Unsubscribed " + customerCount
               + " customers from tariff " + subscription.getTariff().getId()
               + " successfully");
  }

  /** @Override hook **/
  protected void subscribeDefault ()
  {
    for (CapacityBundle bundle: capacityBundles) {
      PowerType powerType = bundle.getPowerType();
      if (getTariffMarket().getDefaultTariff(powerType) != null) {
        log.info(bundle.getName() + ": Subscribing " + bundle.getPopulation()
                 + " customers to default " + powerType + " tariff");
        subscribe(getTariffMarket().getDefaultTariff(powerType), bundle,
                  bundle.getPopulation(), false);
      }
      else {
        log.info(bundle.getName() + ": No default tariff for power type "
                 + powerType + "; trying generic type");
        PowerType genericType = powerType.getGenericType();
        if (getTariffMarket().getDefaultTariff(genericType) == null) {
          log.error(bundle.getName()
                    + ": No default tariff for generic power type "
                    + genericType + " either!");
        }
        else {
          log.info(bundle.getName() + ": Subscribing " + bundle.getPopulation()
                   + " customers to default " + genericType + " tariff");
          subscribe(getTariffMarket().getDefaultTariff(genericType), bundle,
                    bundle.getPopulation(), false);
        }
      }
    }
  }

  // Come here on tariff publication
  @Override
  public void evaluateTariffs ()
  {
    ++tariffEvaluationCounter;
    for (CapacityBundle bundle: capacityBundles) {
      List<Tariff> newTariffs =
              getTariffRepo().findRecentActiveTariffs(tariffEvalCount,
                                                 bundle.getPowerType());
      evaluateTariffs(bundle, newTariffs);
    }
    bundlesWithRevokedTariffs.clear();
  }

  private void evaluateTariffs (CapacityBundle bundle,
                                List<Tariff> tariffsToEval)
  {
    // inertia should affect the portion of the population affected,
    // and not whether evaluation happens at all.
    double inertia = 0.8; // default value
    if (bundle.getSubscriberStructure().inertiaDistribution != null) {
      inertia =
        bundle.getSubscriberStructure().inertiaDistribution.drawSample();
    }
    // adjust for BOG
    inertia = (1.0 - Math.pow(2, 1 - tariffEvaluationCounter)) * inertia;
    
    // Get the cost eval for the appropriate default tariff
    Tariff defaultTariff =
            getTariffMarket().getDefaultTariff(bundle.getPowerType());
    EvalData defaultEval = cache.getEvaluation(defaultTariff);
    if (null == defaultEval) {
      defaultEval =
              new EvalData(forecastDailyUsageCharge(bundle, defaultTariff),
                           0.0);
    }
    
    // Get the cost eval for each tariff
    for (Tariff tariff : tariffsToEval) {
      EvalData eval = cache.getEvaluation(tariff);
      if (null == eval) {
        // compute the projected cost for this tariff
        double cost = forecastDailyUsageCharge(bundle, tariff);
        double hassle = evaluateHassleFactor(bundle, tariff);
        eval = new EvalData(cost, hassle);
        cache.addEvaluation(tariff, eval);
      }
    }
    
    // for each current subscription
    allocations = new HashMap<Tariff, Integer>();
    for (TariffSubscription subscription
            : getTariffSubscriptionRepo().
            findSubscriptionsForCustomer(bundle.getCustomerInfo())) {
      Tariff subTariff = subscription.getTariff();
      // find out how many of these customers can withdraw without penalty
      double withdrawCost = subTariff.getEarlyWithdrawPayment(); 
      int committedCount = subscription.getCustomersCommitted();
      int expiredCount = subscription.getExpiredCustomerCount();
      if (withdrawCost == 0.0 || expiredCount == committedCount) {
        // no need to worry about expiration
        evaluateAlternativeTariffs(bundle, subscription, inertia,
                                   0.0, committedCount,
                                   defaultTariff, defaultEval, tariffsToEval);
      }
      else {
        // Evaluate expired and unexpired subsets separately
        evaluateAlternativeTariffs(bundle, subscription, inertia,
                                   0.0, expiredCount,
                                   defaultTariff, defaultEval, tariffsToEval);
        evaluateAlternativeTariffs(bundle, subscription, inertia,
                                   withdrawCost, committedCount - expiredCount,
                                   defaultTariff, defaultEval, tariffsToEval);
      }
    }
    for (Tariff newTariff : allocations.keySet()) {
      this.subscribe(newTariff, bundle, allocations.get(newTariff), true);
    }
  }

  // evaluate alternatives
  private void evaluateAlternativeTariffs (CapacityBundle bundle,
                                           TariffSubscription current,
                                           double inertia,
                                           double withdraw0,
                                           int population,
                                           Tariff defaultTariff,
                                           EvalData defaultEval,
                                           List<Tariff> tariffsToEval)
  {
    // Associate each alternate tariff with its utility value
    PriorityQueue<TariffUtility> evals = new PriorityQueue<TariffUtility>();
    TariffSubscriberStructure subStructure = bundle.getSubscriberStructure();
    
    HashSet<Tariff> tariffs = new HashSet<Tariff>(tariffsToEval);
    tariffs.add(defaultTariff);
    tariffs.add(current.getTariff());

    // for each tariff, including the current and default tariffs,
    // compute the utility
    for (Tariff tariff: tariffs) {
      EvalData eval = cache.getEvaluation(tariff);
      double inconvenience = eval.inconvenience;
      double cost = eval.costEstimate;
      if (tariff != current.getTariff()) {
        inconvenience += subStructure.tariffSwitchFactor;
        if (tariff.getBroker() != current.getTariff().getBroker())
          inconvenience += subStructure.brokerSwitchFactor;
        cost += tariff.getSignupPayment();
        cost += withdraw0;
        double withdrawFactor =
                Math.min(1.0,
                         (double)tariff.getMinDuration()
                         / (subStructure.expectedDuration * TimeService.DAY));
        cost += withdrawFactor * tariff.getEarlyWithdrawPayment();
      }
      double utility = computeNormalizedDifference(bundle, cost,
                                                   defaultEval.costEstimate);
      utility -= subStructure.inconvenienceWeight * inconvenience;
      evals.add(new TariffUtility(tariff, utility));
    }
    
    // We now have utility values for each possible tariff.
    // Time to make some choices -
    // -- first, we have to compute the sum of transformed utilities
    double logitDenominator = 0.0;
    for (TariffUtility util : evals) {
      logitDenominator +=
              Math.exp(subStructure.logitChoiceRationality * util.utility);
    }
    // then we can compute the probabilities
    for (TariffUtility util : evals) {
      util.probability =
              Math.exp(subStructure.logitChoiceRationality * util.utility)
              / logitDenominator;
    }
    if (bundle.getCustomerInfo().isMultiContracting()) {
      // Ideally, each individual customer makes a choice.
      // For large populations, we do it in chunks,
      // where "large" means n > allocationChunks.
      int chunk = 1;
      if (population > allocationChunks)
        chunk = population / allocationChunks; // note integer division
      int remainingPopulation = population;
      while (remainingPopulation > 0) {
        int count = (int)Math.min(remainingPopulation, chunk);
        // allocate a chunk
        double inertiaSample = inertiaSampler.nextDouble();
        if (inertiaSample < inertia)
          continue; // skip this one
        double tariffSample = tariffSelector.nextDouble();
        // walk down the list until we run out of probability
        for (TariffUtility tu : evals) {
          if (tariffSample <= tu.probability) {
            addAllocation(current.getTariff(), tu.tariff, count);
            remainingPopulation -= count;
            break;
          }
          else {
            tariffSample -= tu.probability;
          }
        }
      }
    }
    else {
      // not multicontracting
      double inertiaSample = inertiaSampler.nextDouble();
      if (inertiaSample >= inertia) {
        double tariffSample = tariffSelector.nextDouble();
        // walk down the list until we run out of probability
        for (TariffUtility tu : evals) {
          if (tariffSample <= tu.probability) {
            addAllocation(current.getTariff(), tu.tariff, population);
            break;
          }
          else {
            tariffSample -= tu.probability;
          }
        }
      }
    }
  }

  private void addAllocation (Tariff current, Tariff newTariff, int count)
  {
    Integer ac = allocations.get(current);
    if (null == ac)
      ac = count;
    else
      ac += count;
    allocations.put(newTariff, ac);
  }

//  private void manageSubscriptions (CapacityBundle bundle,
//                                    List<Tariff> evalTariffs)
//  {
//    //Collections.shuffle(evalTariffs);
//
//    PowerType powerType = bundle.getCustomerInfo().getPowerType();
//    List<Long> tariffIds = new ArrayList<Long>(evalTariffs.size());
//    for (Tariff tariff: evalTariffs)
//      tariffIds.add(tariff.getId());
//    logAllocationDetails(bundle.getName() + ": " + powerType
//                         + " tariffs for evaluation: " + tariffIds);
//
//    List<Double> estimatedPayments = estimatePayments(bundle, evalTariffs);
//    logAllocationDetails(bundle.getName()
//                         + ": Estimated payments for tariffs: "
//                         + estimatedPayments);
//
//    List<Integer> allocations =
//      determineAllocations(bundle, evalTariffs, estimatedPayments);
//    logAllocationDetails(bundle.getName() + ": Allocations for tariffs: "
//                         + allocations);
//
//    int overAllocations = 0;
//    for (int i = 0; i < evalTariffs.size(); ++i) {
//      Tariff evalTariff = evalTariffs.get(i);
//      int allocation = allocations.get(i);
//      TariffSubscription subscription =
//        getTariffSubscriptionRepo()
//                .findSubscriptionForTariffAndCustomer(evalTariff,
//                                                      bundle.getCustomerInfo()); // could
//                                                                                 // be
//                                                                                 // null
//      int currentCommitted =
//        (subscription != null)? subscription.getCustomersCommitted(): 0;
//      int numChange = allocation - currentCommitted;
//
//      log.debug(bundle.getName() + ": evalTariff = " + evalTariff.getId()
//                + ", numChange = " + numChange + ", currentCommitted = "
//                + currentCommitted + ", allocation = " + allocation);
//
//      if (numChange == 0) {
//        if (currentCommitted > 0) {
//          log.info(bundle.getName() + ": Maintaining " + currentCommitted + " "
//                   + powerType + " customers in tariff " + evalTariff.getId());
//        }
//        else {
//          log.info(bundle.getName() + ": Not allocating any " + powerType
//                   + " customers to tariff " + evalTariff.getId());
//        }
//      }
//      else if (numChange > 0) {
//        if (evalTariff.isExpired()) {
//          overAllocations += numChange;
//          if (currentCommitted > 0) {
//            log.info(bundle.getName() + ": Maintaining " + currentCommitted
//                     + " " + powerType + " customers in expired tariff "
//                     + evalTariff.getId());
//          }
//          log.info(bundle.getName() + ": Reallocating " + numChange + " "
//                   + powerType + " customers from expired tariff "
//                   + evalTariff.getId() + " to other tariffs");
//        }
//        else {
//          log.info(bundle.getName() + ": Subscribing " + numChange + " "
//                   + powerType + " customers to tariff " + evalTariff.getId());
//          subscribe(evalTariff, bundle, numChange, false);
//        }
//      }
//      else if (numChange < 0) {
//        log.info(bundle.getName() + ": Unsubscribing " + -numChange + " "
//                 + powerType + " customers from tariff " + evalTariff.getId());
//        unsubscribe(subscription, bundle, -numChange, false);
//      }
//    }
//    if (overAllocations > 0) {
//      int minIndex = 0;
//      double minEstimate = Double.POSITIVE_INFINITY;
//      for (int i = 0; i < estimatedPayments.size(); ++i) {
//        if (estimatedPayments.get(i) < minEstimate
//            && !evalTariffs.get(i).isExpired()) {
//          minIndex = i;
//          minEstimate = estimatedPayments.get(i);
//        }
//      }
//      log.info(bundle.getName() + ": Subscribing " + overAllocations
//               + " over-allocated customers to tariff "
//               + evalTariffs.get(minIndex).getId());
//      subscribe(evalTariffs.get(minIndex), bundle, overAllocations, false);
//    }
//  }
  
  // Computes the normalized difference between the cost of the default tariff
  // and the cost of a proposed tariff
  private double computeNormalizedDifference (CapacityBundle bundle,
                                              double cost, double defaultCost)
  {
    double ndiff = (defaultCost - cost) / defaultCost;
    if (bundle.getPowerType().isProduction())
      ndiff = -ndiff;
    return ndiff;
  }
  
  // Computes the "inconvenience" factor for a tariff
  // This computation includes only the "isolated" factors, and not
  // the broker-switching factor because that is a function of tariff pairs
  private double evaluateHassleFactor (CapacityBundle bundle, Tariff tariff)
  {
    TariffSubscriberStructure subStructure = bundle.getSubscriberStructure();
    tariffEvalHelper.initializeInconvenienceFactors(subStructure.touFactor,
                                                    subStructure.tieredRateFactor,
                                                    subStructure.variablePricingFactor,
                                                    subStructure.interruptibilityFactor);
    return tariffEvalHelper.computeInconvenience(tariff);
  }

//  private List<Double> estimatePayments (CapacityBundle bundle,
//                                         List<Tariff> evalTariffs)
//  {
//    List<Double> estimatedPayments = new ArrayList<Double>(evalTariffs.size());
//    for (int i = 0; i < evalTariffs.size(); ++i) {
//      Tariff tariff = evalTariffs.get(i);
//      //double fixedPayments = estimateFixedTariffPayments(bundle, tariff);
//      double variablePayment = forecastDailyUsageCharge(bundle, tariff);
//      //double totalPayment =
//      //  truncateTo2Decimals(fixedPayments + variablePayment);
//      //double adjustedPayment =
//      //  adjustForInterruptibility(bundle, tariff, totalPayment);
//      estimatedPayments.add(variablePayment);
//    }
//    return estimatedPayments;
//  }

//  private double estimateFixedTariffPayments (CapacityBundle bundle,
//                                              Tariff tariff)
//  {
//    double lifecyclePayment =
//      tariff.getEarlyWithdrawPayment() + tariff.getSignupPayment();
//    double minDuration;
//    if (tariff.getMinDuration() == 0l)
//      minDuration = (double) MEAN_TARIFF_DURATION;
//    else
//      minDuration = (double) tariff.getMinDuration() / (double) TimeService.DAY;
//    double dailyLifecyclePayment = lifecyclePayment / minDuration;
//    return bundle.getPopulation() * (dailyLifecyclePayment);
//  }

  private double
    forecastDailyUsageCharge (CapacityBundle bundle, Tariff tariff)
  {
    double usageSign = bundle.getPowerType().isConsumption()? +1: -1;
    double[] usageForecast = new double[CapacityProfile.NUM_TIMESLOTS];
    for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
      CapacityProfile forecast = capacityOriginator.getCurrentForecast();
      for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
        double hourlyUsage = usageSign * forecast.getCapacity(i);
        usageForecast[i] += hourlyUsage / bundle.getPopulation();
      }
    }
    TariffSubscriberStructure subStructure = bundle.getSubscriberStructure();
    tariffEvalHelper.init(subStructure.expMeanPriceWeight,
                          subStructure.maxValuePriceWeight,
                          subStructure.realizedPriceWeight,
                          tariffEvalHelper.getSoldThreshold());
    return tariffEvalHelper.estimateCost(tariff, usageForecast);
  }

//  private double adjustForInterruptibility (CapacityBundle bundle,
//                                            Tariff tariff, double totalPayment)
//  {
//    double interruptibilityDiscount =
//      bundle.getSubscriberStructure().interruptibilityDiscount;
//    if (interruptibilityDiscount > 0.0
//        && tariff.getPowerType().isInterruptible()) {
//      double effectSign = tariff.getPowerType().isConsumption()? -1: +1;
//      double adjustedPayment =
//        (1.0 + effectSign * interruptibilityDiscount) * totalPayment;
//      return adjustedPayment;
//    }
//    else {
//      return totalPayment;
//    }
//  }

//  private List<Integer> determineAllocations (CapacityBundle bundle,
//                                              List<Tariff> evalTariffs,
//                                              List<Double> estimatedPayments)
//  {
//    List<Integer> allocations = new ArrayList<Integer>();
//    if (evalTariffs.size() == 1) {
//      allocations.add(bundle.getPopulation());
//      return allocations;
//    }
//    List<Boolean> validTariffsIndex =
//      enforceTariffConstraints(bundle, evalTariffs, estimatedPayments);
//    List<Tariff> checkedTariffs = new ArrayList<Tariff>();
//    List<Double> checkedPayments = new ArrayList<Double>();
//    for (int i = 0; i < evalTariffs.size(); ++i) {
//      if (validTariffsIndex.get(i)) {
//        checkedTariffs.add(evalTariffs.get(i));
//        checkedPayments.add(estimatedPayments.get(i));
//      }
//    }
//    List<Integer> checkedAllocs =
//      bundle.getSubscriberStructure().allocationMethod == AllocationMethod.TOTAL_ORDER? determineTotalOrderAllocations(bundle,
//                                                                                                                       checkedTariffs,
//                                                                                                                       checkedPayments)
//                                                                                      : determineLogitChoiceAllocations(bundle,
//                                                                                                                        checkedTariffs,
//                                                                                                                        checkedPayments);
//    int checkedCounter = 0;
//    for (int i = 0; i < evalTariffs.size(); ++i) {
//      allocations.add(validTariffsIndex.get(i)? checkedAllocs
//              .get(checkedCounter++): 0);
//    }
//    return allocations;
//  }

//  private List<Boolean>
//    enforceTariffConstraints (CapacityBundle bundle, List<Tariff> evalTariffs,
//                              List<Double> estimatedPayments)
//  {
//    List<Boolean> validityIndex = new ArrayList<Boolean>();
//
//    Tariff defaultTariff = getDefaultTariff(bundle);
//    if (defaultTariff == null)
//      throw new Error("Default tariff not found amongst eval tariffs!");
//    double defaultPayment =
//      estimatedPayments.get(evalTariffs.indexOf(defaultTariff));
//
//    boolean benchmarkRiskEnabled =
//      bundle.getSubscriberStructure().benchmarkRiskEnabled;
//    //boolean tariffThrottlingEnabled =
//    //  bundle.getSubscriberStructure().tariffThrottlingEnabled;
//
//    Map<Long, Integer> brokerBests = new HashMap<Long, Integer>(); // brokerId
//                                                                   // ->
//                                                                   // tariffIndex
//
//    for (int i = 0; i < evalTariffs.size(); ++i) {
//      Tariff evalTariff = evalTariffs.get(i);
//      // #1: Default tariff is always valid.
//      if (evalTariff == defaultTariff) {
//        validityIndex.add(true);
//        continue;
//      }
//      // #2: If tariff is expired, don't consider it.
//      if (evalTariff.isExpired() || evalTariff.isRevoked()) {
//        log.info("Tariff " + evalTariff.getId()
//                 + " has expired or been revoked; being ignored.");
//        validityIndex.add(false);
//        continue;
//      }
//      // #3: Tariff payments cannot be too much worse than those of default
//      // tariff.
//      if (benchmarkRiskEnabled) {
//        double evalRatio = estimatedPayments.get(i) / defaultPayment;
//        if ((bundle.getPowerType().isConsumption() && evalRatio > bundle
//                .getSubscriberStructure().benchmarkRiskRatio)
//            || (bundle.getPowerType().isProduction() && evalRatio < bundle
//                    .getSubscriberStructure().benchmarkRiskRatio)) {
//          logAllocationDetails(bundle.getName()
//                               + ": Tariff "
//                               + evalTariff.getId()
//                               + " has a worse than constrained benchmark risk at: "
//                               + evalRatio);
//          validityIndex.add(false);
//          continue;
//        }
//      }
//      // #4: Only include best N tariffs per broker.
//      if (tariffThrottlingEnabled) {
//        Long brokerId = evalTariff.getBroker().getId();
//        if (!brokerBests.containsKey(brokerId)) {
//          brokerBests.put(brokerId, i);
//        }
//        else {
//          TariffSubscription evalSub =
//            getTariffSubscriptionRepo()
//                    .findSubscriptionForTariffAndCustomer(evalTariff, bundle
//                            .getCustomerInfo());
//          if (evalSub == null || evalSub.getCustomersCommitted() == 0) {
//            double evalPayment = estimatedPayments.get(i);
//            double bestPayment =
//              estimatedPayments.get(brokerBests.get(brokerId));
//            if ((bundle.getPowerType().isConsumption() && evalPayment <= bestPayment)
//                || (bundle.getPowerType().isProduction() && evalPayment >= bestPayment)) {
//              logAllocationDetails(bundle.getName()
//                                   + ": Tariff "
//                                   + evalTariff.getId()
//                                   + " is no better than "
//                                   + evalTariffs.get(brokerBests.get(brokerId))
//                                           .getId());
//              validityIndex.add(false);
//              continue;
//            }
//            else {
//              // reevaluate previous brokerBest
//              TariffSubscription sub =
//                getTariffSubscriptionRepo()
//                        .findSubscriptionForTariffAndCustomer(evalTariff,
//                                                              bundle.getCustomerInfo());
//              if (sub == null || sub.getCustomersCommitted() == 0) {
//                validityIndex.set(brokerBests.get(brokerId), false);
//                brokerBests.put(brokerId, i);
//              }
//            }
//          }
//        }
//      }
//      validityIndex.add(true);
//    }
//    logAllocationDetails(bundle.getName()
//                         + ": Tariff constraint validation index: "
//                         + validityIndex);
//    return validityIndex;
//  }

//  private List<Integer>
//    determineTotalOrderAllocations (CapacityBundle bundle,
//                                    List<Tariff> checkedTariffs,
//                                    List<Double> estimatedPayments)
//  {
//    int numTariffs = checkedTariffs.size();
//    List<Double> allocationRule;
//    if (bundle.getSubscriberStructure().totalOrderRules.isEmpty()) {
//      allocationRule = new ArrayList<Double>(numTariffs);
//      allocationRule.add(1.0);
//      for (int i = 1; i < numTariffs; ++i) {
//        allocationRule.add(0.0);
//      }
//    }
//    else if (numTariffs <= bundle.getSubscriberStructure().totalOrderRules
//            .size()) {
//      allocationRule =
//        bundle.getSubscriberStructure().totalOrderRules.get(numTariffs - 1);
//    }
//    else {
//      allocationRule = new ArrayList<Double>(numTariffs);
//      List<Double> largestRule =
//        bundle.getSubscriberStructure().totalOrderRules.get(bundle
//                .getSubscriberStructure().totalOrderRules.size() - 1);
//      for (int i = 0; i < numTariffs; ++i) {
//        if (i < largestRule.size()) {
//          allocationRule.add(largestRule.get(i));
//        }
//        else {
//          allocationRule.add(0.0);
//        }
//      }
//    }
//    // payments are positive for production, so sorting is still valid
//    List<Double> sortedPayments = new ArrayList<Double>(numTariffs);
//    for (double estimatedPayment: estimatedPayments) {
//      sortedPayments.add(estimatedPayment);
//    }
//    Collections.sort(sortedPayments, Collections.reverseOrder()); // we want
//                                                                  // descending
//                                                                  // order
//
//    List<Integer> allocations = new ArrayList<Integer>(numTariffs);
//    for (int i = 0; i < numTariffs; ++i) {
//      if (allocationRule.get(i) > 0) {
//        double nextBest = sortedPayments.get(i);
//        for (int j = 0; j < numTariffs; ++j) {
//          if (estimatedPayments.get(j) == nextBest) {
//            allocations.add((int) Math.round(bundle.getCustomerInfo()
//                    .getPopulation() * allocationRule.get(i)));
//          }
//        }
//      }
//      else
//        allocations.add(0);
//    }
//    return allocations;
//  }

//  private List<Integer>
//    determineLogitChoiceAllocations (CapacityBundle bundle,
//                                     List<Tariff> checkedTariffs,
//                                     List<Double> estimatedPayments)
//  {
//    // logit choice model: p_i = e^(lambda * utility_i) / sum_i(e^(lambda *
//    // utility_i))
//
//    int numTariffs = checkedTariffs.size();
//    double bestPayment = Collections.max(estimatedPayments);
//    double worstPayment = Collections.min(estimatedPayments);
//
//    List<Double> probabilities = new ArrayList<Double>(numTariffs);
//    if (bestPayment - worstPayment < 0.01) { // i.e., approximately zero
//      for (int i = 0; i < numTariffs; ++i) {
//        probabilities.add(1.0 / numTariffs);
//      }
//    }
//    else {
//      double midPayment = (worstPayment + bestPayment) / 2.0;
//      double basis =
//        Math.max((bestPayment - midPayment), (midPayment - worstPayment));
//
//      double absMin = Double.MAX_VALUE;
//      double absMax = Double.MIN_VALUE;
//      for (int i = 0; i < numTariffs; ++i) {
//        double absPayment = Math.abs(estimatedPayments.get(i));
//        absMin = Math.min(absPayment, absMin);
//        absMax = Math.max(absPayment, absMax);
//      }
//      double kappa =
//              Math.min(10.0, Math.max(1.0, absMax / absMin)); // utility curve
//                                                              // shape factor
//      double lambda =
//              bundle.getSubscriberStructure().logitChoiceRationality;
//          // [0.0 = irrational, 1.0 = perfectly rational]
//
//      List<Double> numerators = new ArrayList<Double>(numTariffs);
//      double denominator = 0.0;
//      for (int i = 0; i < numTariffs; ++i) {
//        double utility =
//          ((estimatedPayments.get(i) - midPayment) / basis) * kappa; // [-kappa,
//                                                                     // +kappa]
//        // System.out.println("***** utility for tariff[" + i + "] = " +
//        // utility);
//        double numerator = Math.exp(lambda * utility);
//        if (Double.isNaN(numerator))
//          numerator = 0.0;
//        numerators.add(numerator);
//        denominator += numerator;
//      }
//      for (int i = 0; i < numTariffs; ++i) {
//        probabilities.add(numerators.get(i) / denominator);
//      }
//      // System.out.println("***** allocation probabilities: " + probabilities);
//    }
//    // Now determine allocations based on above probabilities
//    List<Integer> allocations = new ArrayList<Integer>(numTariffs);
//    int population = bundle.getPopulation();
//    if (bundle.getCustomerInfo().isMultiContracting()) {
//      int sumAllocations = 0;
//      for (int i = 0; i < numTariffs; ++i) {
//        int allocation;
//        if (sumAllocations == population) {
//          allocation = 0;
//        }
//        else if (i < (numTariffs - 1)) {
//          allocation = (int) Math.round(population * probabilities.get(i));
//          if ((sumAllocations + allocation) > population) {
//            allocation = population - sumAllocations;
//          }
//          sumAllocations += allocation;
//        }
//        else {
//          allocation = population - sumAllocations;
//        }
//        allocations.add(allocation);
//      }
//    }
//    else {
//      double r = ((double) tariffSelector.nextInt(100) / 100.0); // [0.0, 1.0]
//      double cumProbability = 0.0;
//      for (int i = 0; i < numTariffs; ++i) {
//        cumProbability += probabilities.get(i);
//        if (r <= cumProbability) {
//          allocations.add(population);
//          for (int j = i + 1; j < numTariffs; ++j) {
//            allocations.add(0);
//          }
//          break;
//        }
//        else {
//          allocations.add(0);
//        }
//      }
//    }
//    return allocations;
//  }

//  private Tariff getDefaultTariff (CapacityBundle bundle)
//  {
//    Tariff defaultTariff;
//    defaultTariff = getTariffMarket().getDefaultTariff(bundle.getPowerType());
//    if (defaultTariff == null) {
//      defaultTariff =
//        getTariffMarket().getDefaultTariff(bundle.getPowerType()
//                .getGenericType());
//    }
//    if (defaultTariff == null) {
//      throw new Error(
//                      bundle.getName()
//                              + ": There is no default tariff for bundle type or it's generic type!");
//    }
//    return defaultTariff;
//  }

  // /////////////// TIMESLOT ACTIVITY //////////////////////

  @Override
  public void handleNewTimeslot (Timeslot timeslot)
  {
    checkRevokedSubscriptions();
    usePower(timeslot);
  }

  private void checkRevokedSubscriptions ()
  {
    for (CapacityBundle bundle: capacityBundles) {
      List<TariffSubscription> revoked =
        getTariffSubscriptionRepo().getRevokedSubscriptionList(bundle
                .getCustomerInfo());
      for (TariffSubscription revokedSubscription: revoked) {
        revokedSubscription.handleRevokedTariff();
        bundlesWithRevokedTariffs.add(bundle);
      }
    }
  }

  private void usePower (Timeslot timeslot)
  {
    for (CapacityBundle bundle: capacityBundles) {
      List<TariffSubscription> subscriptions =
        getTariffSubscriptionRepo().findActiveSubscriptionsForCustomer(bundle
                .getCustomerInfo());
      double totalCapacity = 0.0;
      double totalUsageCharge = 0.0;
      for (TariffSubscription subscription: subscriptions) {
        double usageSign = bundle.getPowerType().isConsumption()? +1: -1;
        double currCapacity = usageSign * useCapacity(subscription, bundle);
        if (service.getUsageChargesLogging() == true) {
          double charge =
            subscription.getTariff().getUsageCharge(currCapacity,
                                                    subscription
                                                            .getTotalUsage(),
                                                    false);
          totalUsageCharge += charge;
        }
        subscription.usePower(currCapacity);
        totalCapacity += currCapacity;
      }
      log.info(bundle.getName() + ": Total " + bundle.getPowerType()
               + " capacity for timeslot " + timeslot.getSerialNumber() + " = "
               + totalCapacity);
      logUsageCharges(bundle.getName() + ": Total " + bundle.getPowerType()
                      + " usage charge for timeslot "
                      + timeslot.getSerialNumber() + " = " + totalUsageCharge);
    }
  }

  public double useCapacity (TariffSubscription subscription,
                             CapacityBundle bundle)
  {
    double capacity = 0;
    for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
      capacity += capacityOriginator.useCapacity(subscription);
    }
    return capacity;
  }

  protected String getCustomerName ()
  {
    return customerStructure.name;
  }

//  protected double truncateTo2Decimals (double x)
//  {
//    double fract, whole;
//    if (x > 0) {
//      whole = Math.floor(x);
//      fract = Math.floor((x - whole) * 100.0) / 100.0;
//    }
//    else {
//      whole = Math.ceil(x);
//      fract = Math.ceil((x - whole) * 100.0) / 100.0;
//    }
//    return whole + fract;
//  }

//  private void logAllocationDetails (String msg)
//  {
//    if (factoredCustomerService.getAllocationDetailsLogging() == true) {
//      log.info(msg);
//    }
//  }

  private void logUsageCharges (String msg)
  {
    if (service.getUsageChargesLogging() == true) {
      log.info(msg);
    }
  }

  @Override
  public String toString ()
  {
    return this.getClass().getCanonicalName() + ":" + getCustomerName();
  }
  
  // Container for tariff-utility recording
  class TariffUtility implements Comparable<TariffUtility>
  {
    Tariff tariff;
    double utility;
    double probability = 0.0;
    
    TariffUtility (Tariff tariff, double utility)
    {
      super();
      this.tariff = tariff;
      this.utility = utility;
    }

    @Override
    // natural ordering is by decreasing utility values
    public int compareTo (TariffUtility other)
    {
      double result = other.utility - utility;
      if (result == 0.0)
        // consistent with equals...
        return (int)(other.tariff.getId() - tariff.getId());
      else if (result > 0.0)
        return 1;
      else
        return -1;
    }
  }
  
  // Container for tariff-evaluation data
  class EvalData
  {
    double costEstimate;
    double inconvenience;
    
    EvalData (double cost, double inconvenience)
    {
      super();
      this.costEstimate = cost;
      this.inconvenience = inconvenience;
    }
  }
  
  // Tariff-evaluation cache per CapacityBundle
  class TariffEvalCache
  {
    HashMap<Tariff, EvalData> evaluatedTariffs;
    
    TariffEvalCache ()
    {
      super();
      evaluatedTariffs = new HashMap<Tariff, EvalData>();
    }
    
    void addEvaluation (Tariff tariff, EvalData data)
    {
      evaluatedTariffs.put(tariff, data);
    }
    
    EvalData getEvaluation (Tariff tariff)
    {
      return evaluatedTariffs.get(tariff);
    }
  }

} // end class


