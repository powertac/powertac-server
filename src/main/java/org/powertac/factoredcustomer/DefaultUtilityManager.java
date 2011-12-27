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
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.factoredcustomer.CapacityProfile.CapacityType;
import org.powertac.factoredcustomer.CapacityProfile.CapacitySubType;
import org.powertac.factoredcustomer.TariffSubscriberProfile.AllocationMethod;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Key class responsible for managing the tariff(s) for one customer across 
 * multiple capacity bundles if necessary.
 * 
 * @author Prashant Reddy
 */
@Domain
class DefaultUtilityManager extends UtilityManager
{
    private static Logger log = Logger.getLogger(DefaultUtilityManager.class.getName());

    @Autowired
    private RandomSeedRepo randomSeedRepo;
    
    private static final long MEAN_TARIFF_DURATION = 5;  // number of days
    
    private final CustomerProfile customerProfile;
    private final List<CapacityBundle> capacityBundles;
   
    private final List<Tariff> ignoredTariffs = new ArrayList<Tariff>();
    private final Random inertiaSampler;
    
    
    DefaultUtilityManager(CustomerProfile profile, List<CapacityBundle> bundles) 
    {        
        customerProfile = profile;
        capacityBundles = bundles;
    
        randomSeedRepo = (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");

        inertiaSampler = new Random(randomSeedRepo.getRandomSeed("factoredcustomer.DefaultUtilityManager", 
                                                                 customerProfile.profileId, "InertiaSampler").getValue());
    }
  
    CustomerInfo getCustomerInfo() 
    {
        return customerProfile.customerInfo;
    }
    
    String getName()
    {
        return customerProfile.name;
    }
    
    int getPopulation()
    {
        return getCustomerInfo().getPopulation();
    }
    
    ///////////////// TARIFF EVALUATION //////////////////////
    
    /** @Override @code{UtilityManager} **/
    void handleNewTariffs (List<Tariff> newTariffs)
    {
        List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(getCustomerInfo());
        if (subscriptions == null || subscriptions.size() == 0) {
            subscribeDefault();
        } else { 
            reevaluateTariffs(newTariffs); 
	}
    }
	
    @StateChange
    public void subscribeDefault() 
    {
        for (CapacityBundle bundle: capacityBundles) {
            PowerType powerType = CapacityProfile.reportPowerType(bundle.getCapacityType(), bundle.getCapacitySubType());
            if (tariffMarketService.getDefaultTariff(powerType) == null) {
                log.info(getName() + ": No default tariff for power type " + powerType + "; trying less specific type.");

                CapacityType capacityType = CapacityProfile.reportCapacityType(powerType);
                PowerType generalType = CapacityProfile.reportPowerType(capacityType, CapacitySubType.NONE);
		  
                if (tariffMarketService.getDefaultTariff(generalType) == null) {
                    log.warn(getName() + ": No default tariff for general power type " + powerType + " either!");
                } else {
                    tariffMarketService.subscribeToTariff(tariffMarketService.getDefaultTariff(generalType), getCustomerInfo(), getPopulation());
                    log.info(getName() + " subscribed " + getPopulation() + " customers to default " + generalType + " tariff successfully.");
		} 
            } else {
                tariffMarketService.subscribeToTariff(tariffMarketService.getDefaultTariff(powerType), getCustomerInfo(), getPopulation());
                log.info(getName() + " subscribed " + getPopulation() + " customers to default " + powerType + " tariff successfully.");
            }
        }
    }
    
    @StateChange
    protected void subscribe(Tariff tariff, int customerCount)
    {
      tariffMarketService.subscribeToTariff(tariff, getCustomerInfo(), customerCount);
      log.info(getName() + " subscribed " + customerCount + " customers to tariff " + tariff.getId() + " successfully.");
    }

    @StateChange
    protected void unsubscribe(TariffSubscription subscription, int customerCount)
    {
      subscription.unsubscribe(customerCount);
      log.info(getName() + " unsubscribed " + customerCount + " customers from tariff " + subscription.getTariff().getId() + " successfully.");
    }

    private void reevaluateTariffs(List<Tariff> newTariffs) 
    {
        for (CapacityBundle bundle: capacityBundles) {
            reevaluateTariffs(newTariffs, bundle);
        }
    }
    
    private void reevaluateTariffs(List<Tariff> newTariffs, CapacityBundle bundle) 
    {
        if (bundle.getSubscriberProfile().inertiaDistribution != null) {
            double inertia = bundle.getSubscriberProfile().inertiaDistribution.drawSample();
            if (inertiaSampler.nextDouble() < inertia) {
                log.info(getName() + ": Skipping " + bundle.getCapacityType() + " tariff reevaluation due to inertia");
                for (Tariff newTariff: newTariffs) {
                    ignoredTariffs.add(newTariff);
                }
                return;
            }
        }
        // Include previously ignored tariffs and currently subscribed tariffs in evaluation.
        Map<Long, Tariff> allTariffs = new HashMap<Long, Tariff>();
        
	for (Tariff ignoredTariff: ignoredTariffs) {
	    allTariffs.put(ignoredTariff.getId(), ignoredTariff);
	}      
	ignoredTariffs.clear();
		
	List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(getCustomerInfo());
        for (TariffSubscription subscription: subscriptions) {
            allTariffs.put(subscription.getTariff().getId(), subscription.getTariff());
        }

        for (Tariff newTariff: newTariffs) {
            allTariffs.put(newTariff.getId(), newTariff);
        }

        manageSubscriptions(allTariffs, bundle);	
    }
	
    private void manageSubscriptions(Map<Long, Tariff> allTariffs, CapacityBundle bundle)
    {
        CapacityType capacityType = bundle.getCapacityType();
        
        log.info(getName() + ": Managing " + capacityType + " subscriptions");
		
        List<Tariff> evalTariffs = new ArrayList<Tariff>();
        for (Tariff tariff: allTariffs.values()) {
            if (CapacityProfile.reportCapacityType(tariff.getTariffSpec().getPowerType()) == capacityType) {
                evalTariffs.add(tariff);
            }
        }
	if (evalTariffs.isEmpty()) {
	    log.info(getName() + ": No new or ignored " + capacityType + " tariffs to evaluate");
	    return;
	}
	log.info(getName() + ": Number of " + capacityType + " tariffs for evaluation = " + evalTariffs.size());
		
	double[] estimatedPayments = new double[evalTariffs.size()];
	for (int i=0; i < evalTariffs.size(); ++i) {
	    Tariff tariff = evalTariffs.get(i);
	    if (tariff.isExpired()) {
	        if (capacityType == CapacityProfile.CapacityType.CONSUMPTION) {
	            estimatedPayments[i] = Double.POSITIVE_INFINITY;  // assume worst case
	        } else {  // PRODUCTION
	            estimatedPayments[i] = Double.NEGATIVE_INFINITY;  // assume worst case
	        }
	    } else {
	        double totalVariablePayments = 0.0;
	        totalVariablePayments += bundle.computeDailyUsageCharge(tariff);
	        estimatedPayments[i] = estimateFixedTariffPayments(tariff) + totalVariablePayments;
	    } 
	}		
	List<Integer> allocations = determineAllocations(evalTariffs, estimatedPayments, bundle);
	log.info(getName() + ": " + capacityType + " allocations: " + allocations);
		
	int overAllocations = 0;
	for (int i=0; i < evalTariffs.size(); ++i) {
	    Tariff evalTariff = evalTariffs.get(i);
	    int allocation = allocations.get(i);
	    TariffSubscription subscription = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(evalTariff, getCustomerInfo()); // could be null
	    int currentCommitted = (subscription != null) ? subscription.getCustomersCommitted() : 0;
	    int numChange = allocation - currentCommitted; 
			
	    log.debug(getName() + ": evalTariff = " + evalTariff.getId() + ", numChange = " + numChange +
	                  ", currentCommitted = " + currentCommitted + ", allocation = " + allocation);
			
	    if (numChange == 0) {
	        if (currentCommitted > 0) {
	            log.info(getName() + ": Maintaining " + currentCommitted + " " + capacityType + " customers in tariff " + evalTariff.getId());
	        } else {
                    log.info(getName() + ": Not allocating any " + capacityType + " customers to tariff " + evalTariff.getId());
	        }
	    } else if (numChange > 0) {
	        if (evalTariff.isExpired()) {
	            overAllocations += numChange;
	            if (currentCommitted > 0) {
	                log.info(getName() + ": Maintaining " + currentCommitted + " " + capacityType + " customers in expired tariff " + evalTariff.getId());
	            }
	            log.info(getName() + ": Reallocating " + numChange + " " + capacityType + " customers from expired tariff " + evalTariff.getId() + " to other tariffs");
	        } else { 
                    log.info(getName() + ": Subscribing " + numChange + " " + capacityType + " customers to tariff " + evalTariff.getId());
                    subscribe(evalTariff, numChange);
	        }
	    } else if (numChange < 0) {
	        log.info(getName() + ": Unsubscribing " + -numChange + " " + capacityType + " customers from tariff " + evalTariff.getId());
                unsubscribe(subscription, -numChange);
	    }
	}
	if (overAllocations > 0) {
	    int minIndex = 0;
	    double minEstimate = Double.POSITIVE_INFINITY;
	    for (int i=0; i < estimatedPayments.length; ++i) {
	        if (estimatedPayments[i] < minEstimate && ! evalTariffs.get(i).isExpired()) {
	            minIndex = i;
	            minEstimate = estimatedPayments[i];
	        }
	    }
	    log.info(getName() + ": Subscribing " + overAllocations + " over-allocated customers to tariff " + evalTariffs.get(minIndex).getId());
	    subscribe(evalTariffs.get(minIndex), overAllocations);
	}
    }
	
    private double estimateFixedTariffPayments(Tariff tariff)
    {
        double lifecyclePayment = tariff.getEarlyWithdrawPayment() + tariff.getSignupPayment();
  
        double minDuration;
        // When there is not a Minimum Duration of the contract, you cannot divide with the duration because you don't know it.
        if (tariff.getMinDuration() == 0) minDuration = MEAN_TARIFF_DURATION * TimeService.DAY;
        else minDuration = tariff.getMinDuration();
  
        return ((double) tariff.getPeriodicPayment() + (lifecyclePayment / minDuration));
    }
  
    private List<Integer> determineAllocations(List<Tariff> evalTariffs, double[] estimatedPayments, CapacityBundle bundle) 
    {
        if (bundle.getSubscriberProfile().allocationMethod == AllocationMethod.TOTAL_ORDER) {
            return determineTotalOrderAllocations(evalTariffs, estimatedPayments, bundle);
        } else {
            return determineLogitChoiceAllocations(evalTariffs, estimatedPayments, bundle);
        }
    }
    
    private List<Integer> determineTotalOrderAllocations(List<Tariff> evalTariffs, double[] estimatedPayments, CapacityBundle bundle) 
    {
        int numTariffs = evalTariffs.size();
        List<Double> allocationRule;
        if (bundle.getSubscriberProfile().totalOrderRules.isEmpty()) {
            allocationRule = new ArrayList<Double>(numTariffs);
            allocationRule.add(1.0);
            for (int i=1; i < numTariffs; ++i) {
                allocationRule.add(0.0);
            }
        } else if (numTariffs <= bundle.getSubscriberProfile().totalOrderRules.size()) {
            allocationRule = bundle.getSubscriberProfile().totalOrderRules.get(numTariffs - 1);
        } else {
            allocationRule = new ArrayList<Double>(numTariffs);
            List<Double> largestRule = bundle.getSubscriberProfile().totalOrderRules.get(bundle.getSubscriberProfile().totalOrderRules.size() - 1);
            for (int i=0; i < numTariffs; ++i) {
                if (i < largestRule.size()) {
                    allocationRule.add(largestRule.get(i));
                } else { 
                    allocationRule.add(0.0);
                }
            }
        }               
        // payments are negative for production, so sorting is still valid
        List<Double> sortedPayments = new ArrayList<Double>(numTariffs);
        for (double estimatedPayment: estimatedPayments) {
            sortedPayments.add(estimatedPayment);
        }
        Collections.sort(sortedPayments);

        List<Integer> allocations = new ArrayList<Integer>(numTariffs);
        for (int i=0; i < numTariffs; ++i) {
            if (allocationRule.get(i) > 0) {
                double nextBest = sortedPayments.get(i);
                for (int j=0; j < numTariffs; ++j) {
                    if (estimatedPayments[j] == nextBest) {
                        allocations.add((int) Math.round(getCustomerInfo().getPopulation() * allocationRule.get(i)));
                    }
                }
            }	
            else allocations.add(0);
        }
        return allocations;
    }
    
    private List<Integer> determineLogitChoiceAllocations(List<Tariff> evalTariffs, double[] estimatedPayments, CapacityBundle bundle) 
    {
        // logit choice model:  p_i = e^(lambda * utility_i) / sum_i(e^(lambda * utility_i))

        double bestPayment = bundle.getCapacityType() == CapacityType.CONSUMPTION ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        
        for (int i=0; i < estimatedPayments.length; ++i) {
            if (bundle.getCapacityType() == CapacityType.CONSUMPTION) {
                if (estimatedPayments[i] < bestPayment) {
                    bestPayment = estimatedPayments[i];
                }
            } else { // PRODUCTION
                if (estimatedPayments[i] > bestPayment) {
                    bestPayment = estimatedPayments[i];
                }                
            }
        }
        int numTariffs = evalTariffs.size();
        List<Double> numerators = new ArrayList<Double>(numTariffs);
        double denominator = 0.0;
        for (int i=0; i < numTariffs; ++i) {
            double lambda = bundle.getSubscriberProfile().logitChoiceRationality;  // [0.0, 1.0] 
            double utility = Math.abs(estimatedPayments[i] - bestPayment); 
            double numerator = Math.exp(lambda * utility);
            numerators.add(numerator);
            denominator += numerator;
        }
        List<Integer> allocations = new ArrayList<Integer>(numTariffs);
        for (int i=0; i < numTariffs; ++i) {
            double probability = numerators.get(i) / denominator;
            allocations.add((int) Math.round(getCustomerInfo().getPopulation() * probability));
        }
        return allocations;
    }

    ///////////////// TIMESLOT ACTIVITY //////////////////////

    /** @Override @code{FactoredCustomer} **/
    public void handleNewTimeslot(Timeslot timeslot)
    {
        checkRevokedSubscriptions();
        List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(getCustomerInfo());
        if (haveConsumptionCapacity()) consumePower(timeslot, subscriptions);
        if (haveProductionCapacity()) producePower(timeslot, subscriptions);
    }
	
    private void checkRevokedSubscriptions()
    {
      List<TariffSubscription> revoked = tariffSubscriptionRepo.getRevokedSubscriptionList(getCustomerInfo());
      for (TariffSubscription revokedSubscription : revoked) {
        revokedSubscription.handleRevokedTariff();
      }
    }

    private boolean haveConsumptionCapacity() 
    {
        for (CapacityBundle bundle: capacityBundles) {
            if (bundle.getCapacityType() == CapacityType.CONSUMPTION) {
                return true;
            }
        }
        return false;
    }

    private boolean haveProductionCapacity() 
    {
        for (CapacityBundle bundle: capacityBundles) {
            if (bundle.getCapacityType() == CapacityType.PRODUCTION) {
                return true;
            }
        }
        return false;
    }

    private void consumePower(Timeslot timeslot, List<TariffSubscription> subscriptions) 
    {
        double totalConsumption = 0.0;
        for (TariffSubscription subscription: subscriptions) {
            if (subscription.getCustomersCommitted() > 0 && 
                    CapacityProfile.reportCapacityType(subscription.getTariff().getTariffSpec().getPowerType()) == CapacityType.CONSUMPTION) {
                for (CapacityBundle bundle: capacityBundles) {
                    if (bundle.getCapacityType() == CapacityType.CONSUMPTION) {
                        double currCapacity = bundle.computeCapacity(timeslot, subscription);
                        subscription.usePower(currCapacity); // positive usage is consumption
                        totalConsumption += currCapacity;
                    }
                }
            }			
        }
	log.info(getName() + ": Total consumption for timeslot " + timeslot.getSerialNumber() + " = " + totalConsumption);
    }
	
    private void producePower(Timeslot timeslot, List<TariffSubscription> subscriptions) 
    {
        double totalProduction = 0.0;
        for (TariffSubscription subscription: subscriptions) {
            if (subscription.getCustomersCommitted() > 0 && 
                    CapacityProfile.reportCapacityType(subscription.getTariff().getTariffSpec().getPowerType()) == CapacityType.PRODUCTION) {
                for (CapacityBundle bundle: capacityBundles) {
                    if (bundle.getCapacityType() == CapacityType.PRODUCTION) {
                        double currCapacity = -1 * bundle.computeCapacity(timeslot, subscription);
                        subscription.usePower(currCapacity); // negative usage is production
                        totalProduction = currCapacity;
                    }			
                }
            }
        }
        log.info(getName() + ": Total production for timeslot " + timeslot.getSerialNumber() + " = " + totalProduction);
    }

    public String toString() 
    {
	return "DefaultFactoredCustomer:" + getName();
    }
	
} // end class


