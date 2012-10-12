/*
* Copyright 2011 the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import org.apache.log4j.Logger;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffEvaluationHelper;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.factoredcustomer.interfaces.*;
import org.powertac.factoredcustomer.TariffSubscriberStructure.AllocationMethod;

/**
 * Key class responsible for managing the tariff(s) for one customer across 
 * multiple capacity bundles if necessary.
 * 
 * @author Prashant Reddy
 */
@Domain
class DefaultUtilityOptimizer implements UtilityOptimizer
{
    protected Logger log = Logger.getLogger(DefaultUtilityOptimizer.class.getName());

    protected final FactoredCustomerService factoredCustomerService;
    protected final TariffMarket tariffMarketService;
    protected final TariffSubscriptionRepo tariffSubscriptionRepo;
    protected final TimeslotRepo timeslotRepo;
    protected final RandomSeedRepo randomSeedRepo;
    
    protected static final int NUM_HOURS_IN_DAY = 24;
    protected static final long MEAN_TARIFF_DURATION = 5;  // number of days
    
    protected final CustomerStructure customerStructure;
    protected final List<CapacityBundle> capacityBundles;
   
    protected final List<Tariff> ignoredTariffs = new ArrayList<Tariff>();
    protected Random inertiaSampler;
    protected Random tariffSelector;
    
    protected final TariffEvaluationHelper tariffEvalHelper = new TariffEvaluationHelper();
    
    protected final List<Tariff> allTariffs = new ArrayList<Tariff>();
    protected int tariffEvaluationCounter = 0;
    
    protected List<CapacityBundle> bundlesWithRevokedTariffs = new ArrayList<CapacityBundle>();
    
    
    DefaultUtilityOptimizer(CustomerStructure structure, List<CapacityBundle> bundles) 
    {        
        customerStructure = structure;
        capacityBundles = bundles;
    
        factoredCustomerService = (FactoredCustomerService) SpringApplicationContext.getBean("factoredCustomerService");
        tariffMarketService = (TariffMarket) SpringApplicationContext.getBean("tariffMarketService");
        tariffSubscriptionRepo = (TariffSubscriptionRepo) SpringApplicationContext.getBean("tariffSubscriptionRepo");
        timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
        randomSeedRepo = (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    }
    
    @Override
    public void initialize()
    {
        inertiaSampler = new Random(randomSeedRepo.getRandomSeed("factoredcustomer.DefaultUtilityOptimizer", 
                customerStructure.structureId, "InertiaSampler").getValue());
        tariffSelector = new Random(randomSeedRepo.getRandomSeed("factoredcustomer.DefaultUtilityOptimizer", 
                customerStructure.structureId, "TariffSelector").getValue());
        
        subscribeDefault();
    }
  
    ///////////////// TARIFF EVALUATION //////////////////////
    
    @StateChange
    protected void subscribe(Tariff tariff, CapacityBundle bundle, int customerCount, boolean verbose)
    {
      tariffMarketService.subscribeToTariff(tariff, bundle.getCustomerInfo(), customerCount);
      if (verbose) log.info(bundle.getName() + ": Subscribed " + customerCount + " customers to tariff " + tariff.getId() + " successfully");
    }

    @StateChange
    protected void unsubscribe(TariffSubscription subscription, CapacityBundle bundle, int customerCount, boolean verbose)
    {
      subscription.unsubscribe(customerCount);
      if (verbose) log.info(bundle.getName() + ": Unsubscribed " + customerCount + " customers from tariff " + subscription.getTariff().getId() + " successfully");
    }

    /** @Override hook **/
    protected void subscribeDefault() 
    {
        for (CapacityBundle bundle: capacityBundles) {
            PowerType powerType = bundle.getPowerType();
            if (tariffMarketService.getDefaultTariff(powerType) != null) {
                log.info(bundle.getName() + ": Subscribing " + bundle.getPopulation() + " customers to default " + powerType + " tariff");
                subscribe(tariffMarketService.getDefaultTariff(powerType), bundle, bundle.getPopulation(), false);
            } else {
                log.info(bundle.getName() + ": No default tariff for power type " + powerType + "; trying generic type");
                PowerType genericType = powerType.getGenericType();
                if (tariffMarketService.getDefaultTariff(genericType) == null) {
                    log.error(bundle.getName() + ": No default tariff for generic power type " + genericType + " either!");
                } else {
                    log.info(bundle.getName() + ": Subscribing " + bundle.getPopulation() + " customers to default " + genericType + " tariff");
                    subscribe(tariffMarketService.getDefaultTariff(genericType), bundle, bundle.getPopulation(), false);
                } 
            }
        }
    }
    
    @Override
    public void handleNewTariffs (List<Tariff> newTariffs)
    {
        ++tariffEvaluationCounter;
        for (Tariff tariff: newTariffs) {
            allTariffs.add(tariff);
        }               
        for (CapacityBundle bundle: capacityBundles) {
            evaluateTariffs(bundle, newTariffs); 
        }
        bundlesWithRevokedTariffs.clear();
    }
	
    private boolean isTariffApplicable(Tariff tariff, CapacityBundle bundle)
    {
        PowerType bundlePowerType = bundle.getCustomerInfo().getPowerType();
        if (tariff.getPowerType() == bundlePowerType ||
            tariff.getPowerType() == bundlePowerType.getGenericType()) {
            return true;
        }
        return false;
    }
    
    private void evaluateTariffs(CapacityBundle bundle, List<Tariff> newTariffs) 
    {
        if ((tariffEvaluationCounter % bundle.getSubscriberStructure().reconsiderationPeriod) == 0 
             || bundlesWithRevokedTariffs.contains(bundle)) { 
            reevaluateAllTariffs(bundle);
        } 
        else if (! ignoredTariffs.isEmpty()) {
            evaluateCurrentTariffs(bundle, newTariffs);                
        }   
        else if (! newTariffs.isEmpty()) {
            boolean ignoringNewTariffs = true;
            for (Tariff tariff: newTariffs) {
                if (isTariffApplicable(tariff, bundle)) {
                    ignoringNewTariffs = false;
                    evaluateCurrentTariffs(bundle, newTariffs);
                    break;
                }
            }
            if (ignoringNewTariffs) log.info(bundle.getName() + ": New tariffs are not applicable; skipping evaluation");
        }
    }
    
    private void reevaluateAllTariffs(CapacityBundle bundle) 
    {
        log.info(bundle.getName() + ": Reevaluating all tariffs for " + bundle.getPowerType() + " subscriptions");
        
        List<Tariff> evalTariffs = new ArrayList<Tariff>();
        for (Tariff tariff: allTariffs) {
            if (! tariff.isRevoked() && ! tariff.isExpired() && isTariffApplicable(tariff, bundle)) {
                evalTariffs.add(tariff);
            }
        }
        assertNotEmpty(bundle, evalTariffs);
        ignoredTariffs.clear();         
        manageSubscriptions(bundle, evalTariffs);
    }
    
    private void evaluateCurrentTariffs(CapacityBundle bundle, List<Tariff> newTariffs) 
    {
        if (bundle.getSubscriberStructure().inertiaDistribution != null) {
            double inertia = bundle.getSubscriberStructure().inertiaDistribution.drawSample();
            if (inertiaSampler.nextDouble() < inertia) {
                log.info(bundle.getName() + ": Skipping " + bundle.getCustomerInfo().getPowerType() + " tariff reevaluation due to inertia");
                for (Tariff newTariff: newTariffs) {
                    ignoredTariffs.add(newTariff);
                }
                return;
            }
        }
        // Include previously ignored tariffs, currently subscribed tariffs, and default 
        // tariff in evaluation. Use map instead of list to eliminate duplicate tariffs.
        Map<Long, Tariff> currTariffs = new HashMap<Long, Tariff>();
	for (Tariff ignoredTariff: ignoredTariffs) {
	    currTariffs.put(ignoredTariff.getId(), ignoredTariff);
	}      
	ignoredTariffs.clear();		
	List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(bundle.getCustomerInfo());
        for (TariffSubscription subscription: subscriptions) {
            Tariff subTariff = subscription.getTariff();
            if (!(subTariff.isExpired() || subTariff.isExpired())) {
                currTariffs.put(subTariff.getId(), subTariff);
            }
        }
        for (Tariff newTariff: newTariffs) {
            currTariffs.put(newTariff.getId(), newTariff);
        }
        Tariff defaultTariff = getDefaultTariff(bundle);
        if (! currTariffs.containsKey(defaultTariff.getId())) {
            currTariffs.put(defaultTariff.getId(), defaultTariff);
        }

        List<Tariff> evalTariffs = new ArrayList<Tariff>();
        for (Tariff tariff: currTariffs.values()) {
            if (isTariffApplicable(tariff, bundle)) {
                evalTariffs.add(tariff);
            }
        }
        assertNotEmpty(bundle, evalTariffs);
        manageSubscriptions(bundle, evalTariffs);	
    }

    private void assertNotEmpty(CapacityBundle bundle, List<Tariff> evalTariffs) 
    {
        if (evalTariffs.isEmpty()) {
            throw new Error(bundle.getName() + ": The evaluation tariffs list is unexpectedly empty!");
        }
    }
    
    private void manageSubscriptions(CapacityBundle bundle, List<Tariff> evalTariffs)
    {
        Collections.shuffle(evalTariffs);
        
        PowerType powerType = bundle.getCustomerInfo().getPowerType();        
        List<Long> tariffIds = new ArrayList<Long>(evalTariffs.size());
        for (Tariff tariff: evalTariffs) tariffIds.add(tariff.getId());
        logAllocationDetails(bundle.getName() + ": " + powerType + " tariffs for evaluation: " + tariffIds);

		List<Double> estimatedPayments = estimatePayments(bundle, evalTariffs);
		logAllocationDetails(bundle.getName() + ": Estimated payments for tariffs: " + estimatedPayments);
	        
		List<Integer> allocations = determineAllocations(bundle, evalTariffs, estimatedPayments);
		logAllocationDetails(bundle.getName() + ": Allocations for tariffs: " + allocations);
			
		int overAllocations = 0;
		for (int i=0; i < evalTariffs.size(); ++i) {
		    Tariff evalTariff = evalTariffs.get(i);
		    int allocation = allocations.get(i);
		    TariffSubscription subscription = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(evalTariff, bundle.getCustomerInfo()); // could be null
		    int currentCommitted = (subscription != null) ? subscription.getCustomersCommitted() : 0;
		    int numChange = allocation - currentCommitted; 
				
		    log.debug(bundle.getName() + ": evalTariff = " + evalTariff.getId() + ", numChange = " + numChange +
		                  ", currentCommitted = " + currentCommitted + ", allocation = " + allocation);
				
		    if (numChange == 0) {
		        if (currentCommitted > 0) {
		            log.info(bundle.getName() + ": Maintaining " + currentCommitted + " " + powerType + " customers in tariff " + evalTariff.getId());
		        } else {
	                    log.info(bundle.getName() + ": Not allocating any " + powerType + " customers to tariff " + evalTariff.getId());
		        }
		    } else if (numChange > 0) {
		        if (evalTariff.isExpired()) {
		            overAllocations += numChange;
		            if (currentCommitted > 0) {
		                log.info(bundle.getName() + ": Maintaining " + currentCommitted + " " + powerType + " customers in expired tariff " + evalTariff.getId());
		            }
		            log.info(bundle.getName() + ": Reallocating " + numChange + " " + powerType + " customers from expired tariff " + evalTariff.getId() + " to other tariffs");
		        } else { 
	                    log.info(bundle.getName() + ": Subscribing " + numChange + " " + powerType + " customers to tariff " + evalTariff.getId());
	                    subscribe(evalTariff, bundle, numChange, false);
		        }
		    } else if (numChange < 0) {
		        log.info(bundle.getName() + ": Unsubscribing " + -numChange + " " + powerType + " customers from tariff " + evalTariff.getId());
	                unsubscribe(subscription, bundle, -numChange, false);
		    }
		}
		if (overAllocations > 0) {
		    int minIndex = 0;
		    double minEstimate = Double.POSITIVE_INFINITY;
		    for (int i=0; i < estimatedPayments.size(); ++i) {
		        if (estimatedPayments.get(i) < minEstimate && ! evalTariffs.get(i).isExpired()) {
		            minIndex = i;
		            minEstimate = estimatedPayments.get(i);
		        }
		    }
		    log.info(bundle.getName() + ": Subscribing " + overAllocations + " over-allocated customers to tariff " + evalTariffs.get(minIndex).getId());
		    subscribe(evalTariffs.get(minIndex), bundle, overAllocations, false);
		}
    }
	
    private List<Double> estimatePayments(CapacityBundle bundle, List<Tariff> evalTariffs) 
    {
        List<Double> estimatedPayments = new ArrayList<Double>(evalTariffs.size());
        for (int i=0; i < evalTariffs.size(); ++i) {
            Tariff tariff = evalTariffs.get(i);
            double fixedPayments = estimateFixedTariffPayments(bundle, tariff);
            double variablePayment = forecastDailyUsageCharge(bundle, tariff);
            double totalPayment = truncateTo2Decimals(fixedPayments + variablePayment);    
            double adjustedPayment = adjustForInterruptibility(bundle, tariff, totalPayment);
            estimatedPayments.add(adjustedPayment); 
        }      
        return estimatedPayments;
    }
    
    private double estimateFixedTariffPayments(CapacityBundle bundle, Tariff tariff)
    {
        double lifecyclePayment = tariff.getEarlyWithdrawPayment() + tariff.getSignupPayment();
        double minDuration;
        if (tariff.getMinDuration() == 0) minDuration = MEAN_TARIFF_DURATION;
        else minDuration = tariff.getMinDuration() / TimeService.DAY;
        double dailyLifecyclePayment = lifecyclePayment / minDuration;  
        //double dailyPeriodicPayment = tariff.getPeriodicPayment() * NUM_HOURS_IN_DAY;
        //return bundle.getPopulation() * (dailyLifecyclePayment + dailyPeriodicPayment);
        return bundle.getPopulation() * (dailyLifecyclePayment);
           }
  
    private double forecastDailyUsageCharge(CapacityBundle bundle, Tariff tariff)
    {
        double usageSign = bundle.getPowerType().isConsumption() ? +1 : -1;  
        double[] usageForecast = new double[CapacityProfile.NUM_TIMESLOTS];
        for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
            CapacityProfile forecast = capacityOriginator.getCurrentForecast();            
            for (int i=0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
                double hourlyUsage = usageSign * forecast.getCapacity(i);
                usageForecast[i] += hourlyUsage;
            }
        }        
        TariffSubscriberStructure subStructure = bundle.getSubscriberStructure();
        tariffEvalHelper.init(subStructure.expMeanPriceWeight, subStructure.maxValuePriceWeight, 
        					  subStructure.realizedPriceWeight, tariffEvalHelper.getSoldThreshold());
        return tariffEvalHelper.estimateCost(tariff, usageForecast);
    }
    
    private double adjustForInterruptibility(CapacityBundle bundle, Tariff tariff, double totalPayment) 
    {
        double interruptibilityDiscount = bundle.getSubscriberStructure().interruptibilityDiscount;
        if (interruptibilityDiscount > 0.0 && tariff.getPowerType().isInterruptible()) {
            double effectSign = tariff.getPowerType().isConsumption() ? -1 : +1;
            double adjustedPayment = (1.0 + effectSign * interruptibilityDiscount) * totalPayment;
            return adjustedPayment;
        } else {
            return totalPayment;
        }
    }

    private List<Integer> determineAllocations(CapacityBundle bundle, List<Tariff> evalTariffs, 
                                               List<Double> estimatedPayments) 
    {
        List<Integer> allocations = new ArrayList<Integer>();
        if (evalTariffs.size() == 1) {
            allocations.add(bundle.getPopulation());
            return allocations;
        }
        List<Boolean> validTariffsIndex = enforceTariffConstraints(bundle, evalTariffs, estimatedPayments);
        List<Tariff> checkedTariffs = new ArrayList<Tariff>();
        List<Double> checkedPayments = new ArrayList<Double>();
        for (int i=0; i < evalTariffs.size(); ++i) {
            if (validTariffsIndex.get(i)) {
                checkedTariffs.add(evalTariffs.get(i));
                checkedPayments.add(estimatedPayments.get(i));
            }
        }
        List<Integer> checkedAllocs = bundle.getSubscriberStructure().allocationMethod == AllocationMethod.TOTAL_ORDER ?
            determineTotalOrderAllocations(bundle, checkedTariffs, checkedPayments) :
            determineLogitChoiceAllocations(bundle, checkedTariffs, checkedPayments);
        int checkedCounter = 0;
        for (int i=0; i < evalTariffs.size(); ++i) {
            allocations.add(validTariffsIndex.get(i) ? checkedAllocs.get(checkedCounter++) : 0);
        }
        return allocations;
    }
    
    private List<Boolean> enforceTariffConstraints(CapacityBundle bundle, List<Tariff> evalTariffs, 
                                                   List<Double> estimatedPayments)
   {
        List<Boolean> validityIndex = new ArrayList<Boolean>();
        
        Tariff defaultTariff = getDefaultTariff(bundle);
        if (defaultTariff == null) throw new Error("Default tariff not found amongst eval tariffs!");
        double defaultPayment = estimatedPayments.get(evalTariffs.indexOf(defaultTariff));
        
        boolean benchmarkRiskEnabled = bundle.getSubscriberStructure().benchmarkRiskEnabled;
        boolean tariffThrottlingEnabled = bundle.getSubscriberStructure().tariffThrottlingEnabled;
        
        Map<Long, Integer> brokerBests = new HashMap<Long, Integer>();  // brokerId -> tariffIndex
        
        for (int i=0; i < evalTariffs.size(); ++i) {
            Tariff evalTariff = evalTariffs.get(i);           
            // #1: Default tariff is always valid.
            if (evalTariff == defaultTariff) {
                validityIndex.add(true); continue;
            }
            // #2: If tariff is expired, don't consider it.
            if (evalTariff.isExpired() || evalTariff.isRevoked()) {
                log.info("Tariff " + evalTariff.getId() + " has expired or been revoked; being ignored.");
                validityIndex.add(false); continue;
            }
            // #3: Tariff payments cannot be too much worse than those of default tariff.
            if (benchmarkRiskEnabled) {
                double evalRatio = estimatedPayments.get(i) / defaultPayment;
                if ((bundle.getPowerType().isConsumption() && evalRatio > bundle.getSubscriberStructure().benchmarkRiskRatio) ||
                    (bundle.getPowerType().isProduction() && evalRatio < bundle.getSubscriberStructure().benchmarkRiskRatio)) {
                    logAllocationDetails(bundle.getName() + ": Tariff " + evalTariff.getId() + " has a worse than constrained benchmark risk at: " + evalRatio);
                    validityIndex.add(false); continue;
                }
            }
            // #4: Only include best N tariffs per broker.
            if (tariffThrottlingEnabled) {
                Long brokerId = evalTariff.getBroker().getId();
                if (! brokerBests.containsKey(brokerId)) {
                    brokerBests.put(brokerId, i);
                } else {
                    TariffSubscription evalSub = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(evalTariff, bundle.getCustomerInfo());
                    if (evalSub == null || evalSub.getCustomersCommitted() == 0) {
                        double evalPayment = estimatedPayments.get(i);
                        double bestPayment = estimatedPayments.get(brokerBests.get(brokerId));
                        if ((bundle.getPowerType().isConsumption() && evalPayment <= bestPayment) ||
                            (bundle.getPowerType().isProduction() && evalPayment >= bestPayment)) {                           
                            logAllocationDetails(bundle.getName() + ": Tariff " + evalTariff.getId() + " is no better than " 
                                    + evalTariffs.get(brokerBests.get(brokerId)).getId());
                            validityIndex.add(false); continue;
                        } else {
                            // reevaluate previous brokerBest
                            TariffSubscription sub = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(evalTariff, bundle.getCustomerInfo());
                            if (sub == null || sub.getCustomersCommitted() == 0) {
                                validityIndex.set(brokerBests.get(brokerId), false);
                                brokerBests.put(brokerId, i);
                            }
                        }
                    }
                }
            }
            validityIndex.add(true);
        }
        logAllocationDetails(bundle.getName() + ": Tariff constraint validation index: " + validityIndex);
        return validityIndex;
   }
    
    private List<Integer> determineTotalOrderAllocations(CapacityBundle bundle, List<Tariff> checkedTariffs, 
                                                         List<Double> estimatedPayments) 
    {
        int numTariffs = checkedTariffs.size();
        List<Double> allocationRule;
        if (bundle.getSubscriberStructure().totalOrderRules.isEmpty()) {
            allocationRule = new ArrayList<Double>(numTariffs);
            allocationRule.add(1.0);
            for (int i=1; i < numTariffs; ++i) {
                allocationRule.add(0.0);
            }
        } else if (numTariffs <= bundle.getSubscriberStructure().totalOrderRules.size()) {
            allocationRule = bundle.getSubscriberStructure().totalOrderRules.get(numTariffs - 1);
        } else {
            allocationRule = new ArrayList<Double>(numTariffs);
            List<Double> largestRule = bundle.getSubscriberStructure().totalOrderRules.get(bundle.getSubscriberStructure().totalOrderRules.size() - 1);
            for (int i=0; i < numTariffs; ++i) {
                if (i < largestRule.size()) {
                    allocationRule.add(largestRule.get(i));
                } else { 
                    allocationRule.add(0.0);
                }
            }
        }               
        // payments are positive for production, so sorting is still valid
        List<Double> sortedPayments = new ArrayList<Double>(numTariffs);
        for (double estimatedPayment: estimatedPayments) {
            sortedPayments.add(estimatedPayment);
        }
        Collections.sort(sortedPayments, Collections.reverseOrder()); // we want descending order

        List<Integer> allocations = new ArrayList<Integer>(numTariffs);
        for (int i=0; i < numTariffs; ++i) {
            if (allocationRule.get(i) > 0) {
                double nextBest = sortedPayments.get(i);
                for (int j=0; j < numTariffs; ++j) {
                    if (estimatedPayments.get(j) == nextBest) {
                        allocations.add((int) Math.round(bundle.getCustomerInfo().getPopulation() * allocationRule.get(i)));
                    }
                }
            }	
            else allocations.add(0);
        }
        return allocations;
    }
    
    private List<Integer> determineLogitChoiceAllocations(CapacityBundle bundle, List<Tariff> checkedTariffs, 
                                                          List<Double> estimatedPayments) 
    {
        // logit choice model:  p_i = e^(lambda * utility_i) / sum_i(e^(lambda * utility_i))

        int numTariffs = checkedTariffs.size();
        double bestPayment = Collections.max(estimatedPayments);
        double worstPayment = Collections.min(estimatedPayments);
        
        List<Double> probabilities = new ArrayList<Double>(numTariffs);
        if (bestPayment - worstPayment < 0.01) { // i.e., approximately zero
            for (int i=0; i < numTariffs; ++i) {
                probabilities.add(1.0 / numTariffs);
            }               
        } else {
            double midPayment = (worstPayment + bestPayment) / 2.0;           
            double basis = Math.max((bestPayment - midPayment), (midPayment - worstPayment));

            double absMin = Double.MAX_VALUE;
            double absMax = Double.MIN_VALUE;
            for (int i=0; i < numTariffs; ++i) {
                double absPayment = Math.abs(estimatedPayments.get(i));
                absMin = Math.min(absPayment, absMin);
                absMax = Math.max(absPayment, absMax);
            }   
            double kappa = Math.min(10.0, Math.max(1.0, absMax / absMin));  // utility curve shape factor
            double lambda = bundle.getSubscriberStructure().logitChoiceRationality;  // [0.0 = irrational, 1.0 = perfectly rational] 

            List<Double> numerators = new ArrayList<Double>(numTariffs);
            double denominator = 0.0;
            for (int i=0; i < numTariffs; ++i) {  
                double utility = ((estimatedPayments.get(i) - midPayment) / basis) * kappa;  // [-kappa, +kappa] 
                //System.out.println("***** utility for tariff[" + i + "] = " + utility);
                double numerator = Math.exp(lambda * utility);
                if (Double.isNaN(numerator)) numerator = 0.0;
                numerators.add(numerator);
                denominator += numerator;
            }
            for (int i=0; i < numTariffs; ++i) {
                probabilities.add(numerators.get(i) / denominator);
            }   
            //System.out.println("***** allocation probabilities: " + probabilities);
        }
        // Now determine allocations based on above probabilities
        List<Integer> allocations = new ArrayList<Integer>(numTariffs);
        int population = bundle.getPopulation();
        if (bundle.getCustomerInfo().isMultiContracting())
        {
            int sumAllocations = 0;
            for (int i=0; i < numTariffs; ++i) {
                int allocation;
                if (sumAllocations == population) {
                    allocation = 0;
                } else if (i < (numTariffs - 1)) {
                    allocation = (int) Math.round(population * probabilities.get(i));
                    if ((sumAllocations + allocation) > population) {
                        allocation = population - sumAllocations;
                    }
                    sumAllocations += allocation;
                } else {
                    allocation = population - sumAllocations;
                }
                allocations.add(allocation);
            }
        } else {
            double r = ((double) tariffSelector.nextInt(100) / 100.0); // [0.0, 1.0]
            double cumProbability = 0.0;
            for (int i=0; i < numTariffs; ++i) {
                cumProbability += probabilities.get(i); 
                if (r <= cumProbability) {
                    allocations.add(population);
                    for (int j=i+1; j < numTariffs; ++j) {
                        allocations.add(0);
                    }
                    break;
                } else {
                    allocations.add(0);
                }
            }
        }
        return allocations;
    }

    private Tariff getDefaultTariff(CapacityBundle bundle)
    {
        Tariff defaultTariff;
        defaultTariff = tariffMarketService.getDefaultTariff(bundle.getPowerType());
        if (defaultTariff == null) {
            defaultTariff = tariffMarketService.getDefaultTariff(bundle.getPowerType().getGenericType()); 
        }
        if (defaultTariff == null) {
            throw new Error(bundle.getName() + ": There is no default tariff for bundle type or it's generic type!");
        }
        return defaultTariff;
    }
    

    ///////////////// TIMESLOT ACTIVITY //////////////////////

    @Override
    public void handleNewTimeslot(Timeslot timeslot)
    {
        checkRevokedSubscriptions();
        usePower(timeslot);
    }
	
    private void checkRevokedSubscriptions()
    {
        for (CapacityBundle bundle: capacityBundles) {
            List<TariffSubscription> revoked = tariffSubscriptionRepo.getRevokedSubscriptionList(bundle.getCustomerInfo());
            for (TariffSubscription revokedSubscription : revoked) {
                revokedSubscription.handleRevokedTariff();
                bundlesWithRevokedTariffs.add(bundle);
            }
        }
    }

    private void usePower(Timeslot timeslot) 
    {        
        for (CapacityBundle bundle: capacityBundles) {
            List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(bundle.getCustomerInfo());
            double totalCapacity = 0.0; 
            double totalUsageCharge = 0.0;
            for (TariffSubscription subscription: subscriptions) {
                double usageSign = bundle.getPowerType().isConsumption() ? +1 : -1;  
                double currCapacity = usageSign * useCapacity(subscription, bundle); 
                if (factoredCustomerService.getUsageChargesLogging() == true) {
                    double charge = subscription.getTariff().getUsageCharge(currCapacity, subscription.getTotalUsage(), false);
                    totalUsageCharge += charge;
                }
                subscription.usePower(currCapacity);
                totalCapacity += currCapacity;
            }
            log.info(bundle.getName() + ": Total " + bundle.getPowerType() + " capacity for timeslot " + timeslot.getSerialNumber() + " = " + totalCapacity);
            logUsageCharges(bundle.getName() + ": Total " + bundle.getPowerType() + " usage charge for timeslot " + timeslot.getSerialNumber() + " = " + totalUsageCharge);     
        }
    }

    public double useCapacity(TariffSubscription subscription, CapacityBundle bundle)
    {
        double capacity = 0;
        for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
            capacity += capacityOriginator.useCapacity(subscription);
        }
        return capacity;
    }
    
    protected String getCustomerName()
    {
        return customerStructure.name;
    }
    
    protected double truncateTo2Decimals(double x)
    {
        double fract, whole;
        if (x > 0) {
            whole = Math.floor(x);
            fract = Math.floor((x - whole) * 100) / 100;
        } else {
            whole = Math.ceil(x);
            fract = Math.ceil((x - whole) * 100) / 100;
        }
        return whole + fract;
    }

    private void logAllocationDetails(String msg) 
    {
        if (factoredCustomerService.getAllocationDetailsLogging() == true) {
            log.info(msg);
        }
    }
    
    private void logUsageCharges(String msg) 
    {
        if (factoredCustomerService.getUsageChargesLogging() == true) {
            log.info(msg);
        }
    }
    
    @Override
    public String toString() 
    {
	return this.getClass().getCanonicalName() + ":" + getCustomerName();
    }
	
} // end class


