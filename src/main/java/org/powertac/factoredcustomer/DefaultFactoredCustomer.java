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
import org.apache.log4j.Logger;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.powertac.factoredcustomer.CapacityProfile.CapacityType;

/**
 * @author Prashant Reddy
 */
class DefaultFactoredCustomer extends FactoredCustomer 
{
    private static Logger log = Logger.getLogger(DefaultFactoredCustomer.class.getName());

    @Autowired
    TimeslotRepo timeslotRepo;
    
    private static final long MEAN_TARIFF_DURATION = 5;  // number of days

    public static class Creator implements CustomerFactory.CustomerCreator
    {
        public CustomerCategory getCategory() 
        {
            return null;  // registered as default creator
        }
        
        public FactoredCustomer createModel(CustomerProfile profile)
        {
            return new DefaultFactoredCustomer(profile);
        }
        
    }
    protected static Creator creator = new Creator();
    public static Creator getCreator() {
        return creator;
    }
    
    boolean canConsume = false;
    boolean canProduce = false;

    List<DefaultCapacityManager> capacityManagers = new ArrayList<DefaultCapacityManager>();
    List<Tariff> ignoredTariffs = new ArrayList<Tariff>();
    List<List<Double>> tariffAllocationRules = new ArrayList<List<Double>>();

    DefaultFactoredCustomer(CustomerProfile profile) 
    {
        super(profile);
        
        timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");

	customerProfile = profile; 
	for (CapacityProfile capacityProfile: customerProfile.capacityProfiles) {
	    if (capacityProfile.capacityType == CapacityType.CONSUMPTION) canConsume = true;
	    else if (capacityProfile.capacityType == CapacityType.PRODUCTION) canProduce = true;
			
	    DefaultCapacityManager capacityManager = new DefaultCapacityManager(customerProfile, capacityProfile);
	    capacityManagers.add(capacityManager);
	}
	log.info("Customer created for profile: " + customerProfile.name);
    }
  
    ///////////////// TARIFF EVALUATION //////////////////////
    
    /** @Override @code{FactoredCustomer} **/
    void handleNewTariffs (List<Tariff> newTariffs)
    {
        log.debug("handleTariffs() begin - " + getName());
        List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(getCustomerInfo());
        if (subscriptions == null || subscriptions.size() == 0) {
            subscribeDefault();
        } else { 
            reevaluateTariffs(newTariffs); 
	}
        log.debug("handleTariffs() end - " + getName());
    }
	
    public void subscribeDefault() 
    {
        for (PowerType powerType: getCustomerInfo().getPowerTypes()) { 
            if (tariffMarketService.getDefaultTariff(powerType) == null) {
                log.info("subscribeDefault() - " + getName() + ": No default tariff for power type " + powerType + "; trying less specific type.");

                CapacityType capacityType = CapacityProfile.reportCapacityType(powerType);
                PowerType generalType = CapacityProfile.reportPowerType(capacityType, CapacityProfile.CapacitySubType.NONE);
		  
                if (tariffMarketService.getDefaultTariff(generalType) == null) {
                    log.warn("subscribeDefault() - " + getName() + ": No default tariff for general power type " + powerType + " either!");
                } else {
                    tariffMarketService.subscribeToTariff(tariffMarketService.getDefaultTariff(generalType), getCustomerInfo(), getPopulation());
                    log.info("subscribeDefault() - " + getName() + " subscribed " + getPopulation() + " customers to default " + generalType + " tariff successfully.");
		} 
            } else {
                tariffMarketService.subscribeToTariff(tariffMarketService.getDefaultTariff(powerType), getCustomerInfo(), getPopulation());
                log.info("subscribeDefault() - " + getName() + " subscribed " + getPopulation() + " customers to default " + powerType + " tariff successfully.");
            }
        }
    }
    
    public void subscribe (Tariff tariff, int customerCount)
    {
      tariffMarketService.subscribeToTariff(tariff, getCustomerInfo(), customerCount);
      log.info("subscribe() - " + getName() + " subscribed " + customerCount + " customers to tariff " + tariff.getId() + " successfully.");
    }

    public void unsubscribe (TariffSubscription subscription, int customerCount)
    {
      subscription.unsubscribe(customerCount);
      log.info("unsubscribe() - " + getName() + " unsubscribed " + customerCount + " customers from tariff " + subscription.getTariff().getId() + " successfully.");
    }

    void reevaluateTariffs(List<Tariff> newTariffs) 
    {
        log.info("handleNewTariffs() begin - " + getName() + ": Received " + newTariffs.size() + " new tariffs.");
	
        if (customerProfile.tariffSwitchingInertia != null) {
            double inertia = customerProfile.tariffSwitchingInertia.drawSample();
            if (customerProfile.random.nextDouble() < inertia) {
                log.info("handleNewTariffs() - Ignoring new tariffs for now due to tariff switching inertia.");
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

        if (canConsume) manageSubscriptions(allTariffs, CapacityType.CONSUMPTION);	
	if (canProduce) manageSubscriptions(allTariffs, CapacityType.PRODUCTION);	

        log.info("handleNewTariffs() end - " + getName());
    }
	
    void manageSubscriptions(Map<Long, Tariff> allTariffs, CapacityType capacityType)
    {
        log.info("manageSubscriptions() begin - " +  getName() + "; capacityType: " + capacityType);
		
        List<Tariff> evalTariffs = new ArrayList<Tariff>();
        for (Tariff tariff: allTariffs.values()) {
            if (CapacityProfile.reportCapacityType(tariff.getTariffSpec().getPowerType()) == capacityType) {
                evalTariffs.add(tariff);
            }
        }
	if (evalTariffs.isEmpty()) {
	    log.info("manageSubscriptions(): end early - No new tariffs to evaluate for capacity type: " + capacityType);
	    return;
	}
	log.info("manageSubscriptions(): Number of " + capacityType + " tariffs for evaluation: " + evalTariffs.size());
		
	double[] estimatedPayments = new double[evalTariffs.size()];
	for (int i=0; i < evalTariffs.size(); ++i) {
	    Tariff tariff = evalTariffs.get(i);
	    if (tariff.isExpired()) {
	        estimatedPayments[i] = Double.POSITIVE_INFINITY;  // sort it to the end
	    } else {
	        double totalVariablePayments = 0.0;
	        for (DefaultCapacityManager capacityManager: capacityManagers) {
	            if (capacityManager.capacityProfile.capacityType == capacityType) {
	                totalVariablePayments += capacityManager.computeDailyUsageCharge(tariff);
	            }
	        }
	        estimatedPayments[i] = estimateFixedTariffPayments(tariff) + totalVariablePayments;
	    } 
	}		
	List<Integer> allocations = determineAllocations(evalTariffs, estimatedPayments, capacityType);
	log.info("manageSubscriptions(): " + capacityType + " allocations: " + allocations);
		
	int overAllocations = 0;
	for (int i=0; i < evalTariffs.size(); ++i) {
	    Tariff evalTariff = evalTariffs.get(i);
	    int allocation = allocations.get(i);
	    TariffSubscription subscription = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(evalTariff, getCustomerInfo()); // could be null
	    int currentCommitted = (subscription != null) ? subscription.getCustomersCommitted() : 0;
	    int numChange = allocation - currentCommitted; 
			
	    log.debug("manageSubscriptions() - evalTariff = " + evalTariff.getId() + ", numChange = " + numChange +
	                  ", currentCommitted = " + currentCommitted + ", allocation = " + allocation);
			
	    if (numChange == 0) {
	        if (currentCommitted > 0) {
	            log.info("manageSubscriptions() - Maintaining " + currentCommitted + " " + capacityType + " customers in tariff " + evalTariff.getId());
	        } else {
                    log.info("manageSubscriptions() - Not allocating any " + capacityType + " customers to tariff " + evalTariff.getId());
	        }
	    } else if (numChange > 0) {
	        if (evalTariff.isExpired()) {
	            overAllocations += numChange;
	            if (currentCommitted > 0) {
	                log.info("manageSubscriptions() - Maintaining " + currentCommitted + " " + capacityType + " customers in expired tariff " + evalTariff.getId());
	            }
	            log.info("manageSubscriptions() - Reallocating " + numChange + " " + capacityType + " customers from expired tariff " + evalTariff.getId() + " to other tariffs");
	        } else { 
                    log.info("manageSubscriptions() - Subscribing " + numChange + " " + capacityType + " customers to tariff " + evalTariff.getId());
                    subscribe(evalTariff, numChange);
	        }
	    } else if (numChange < 0) {
	        log.info("manageSubscriptions()  - Unsubscribing " + -numChange + " " + capacityType + " customers from tariff " + evalTariff.getId());
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
	    log.info("manageSubscriptions() - Subscribing " + overAllocations + " over-allocated customers to tariff " + evalTariffs.get(minIndex).getId());
	    subscribe(evalTariffs.get(minIndex), overAllocations);
	}
	log.info("manageSubscriptions(): end - " + getName() + "; capacityType: " + capacityType);
    }
	
    double estimateFixedTariffPayments(Tariff tariff)
    {
        double lifecyclePayment = tariff.getEarlyWithdrawPayment() + tariff.getSignupPayment();
  
        double minDuration;
        // When there is not a Minimum Duration of the contract, you cannot divide with the duration because you don't know it.
        if (tariff.getMinDuration() == 0) minDuration = MEAN_TARIFF_DURATION * TimeService.DAY;
        else minDuration = tariff.getMinDuration();
  
        return ((double) tariff.getPeriodicPayment() + (lifecyclePayment / minDuration));
    }
  
    List<Integer> determineAllocations(List<Tariff> evalTariffs, double[] estimatedPayments, CapacityType capacityType) 
    {
        int numTariffs = evalTariffs.size();
        List<Double> allocationRule;
        if (customerProfile.tariffAllocationRules.isEmpty()) {
            allocationRule = new ArrayList<Double>(numTariffs);
            allocationRule.add(1.0);
            for (int i=1; i < numTariffs; ++i) {
                allocationRule.add(0.0);
            }
        } else if (numTariffs <= customerProfile.tariffAllocationRules.size()) {
            allocationRule = customerProfile.tariffAllocationRules.get(numTariffs - 1);
        } else {
            allocationRule = new ArrayList<Double>(numTariffs);
            List<Double> largestRule = customerProfile.tariffAllocationRules.get(customerProfile.tariffAllocationRules.size() - 1);
            for (int i=0; i < numTariffs; ++i) {
                if (i < largestRule.size()) {
                    allocationRule.add(largestRule.get(i));
                } else { 
                    allocationRule.add(0.0);
                }
            }
        }		
        // payments are negative for production, so sorting is still valid
        List<Double> sortedPayments = new ArrayList<Double>(estimatedPayments.length);
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
    
    ///////////////// TIMESLOT ACTIVITY //////////////////////

    /** @Override @code{FactoredCustomer} **/
    public void handleNewTimeslot()
    {
        Timeslot timeslot =  timeslotRepo.currentTimeslot();
        
        log.info("Customer " + customerProfile.name + " begin step for timeslot " + timeslot.getSerialNumber());    
        checkRevokedSubscriptions();
        if (canConsume) consumePower();
        if (canProduce) producePower();
        log.info("Customer " + customerProfile.name + " end step for timeslot " + timeslot.getSerialNumber());    
    }
	
    protected void checkRevokedSubscriptions ()
    {
      List<TariffSubscription> revoked = tariffSubscriptionRepo.getRevokedSubscriptionList(getCustomerInfo());
      for (TariffSubscription revokedSubscription : revoked) {
        revokedSubscription.handleRevokedTariff();
      }
    }

    public void consumePower() 
    {
        Timeslot timeslot =  timeslotRepo.currentTimeslot();
			
        double totalConsumption = 0.0;
        List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(getCustomerInfo());
        for (TariffSubscription subscription: subscriptions) {
            if (subscription.getCustomersCommitted() > 0 && 
                    CapacityProfile.reportCapacityType(subscription.getTariff().getTariffSpec().getPowerType()) == CapacityType.CONSUMPTION) {
                for (DefaultCapacityManager capacityManager: capacityManagers) {
                    if (capacityManager.capacityProfile.capacityType == CapacityType.CONSUMPTION) {
                        double currCapacity = capacityManager.computeCapacity(timeslot, subscription);
                        subscription.usePower(currCapacity); // positive usage is consumption
                        totalConsumption += currCapacity;
                    }
                }
            }			
        }
	log.info("Total consumption for timeslot " + timeslot.getSerialNumber() + " = " + totalConsumption);
    }
	
    public void producePower() 
    {
        if (! canProduce) return;
        
        Timeslot timeslot =  timeslotRepo.currentTimeslot();
        
        double totalProduction = 0.0;
        List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(getCustomerInfo());
        for (TariffSubscription subscription: subscriptions) {
            if (subscription.getCustomersCommitted() > 0 && 
                    CapacityProfile.reportCapacityType(subscription.getTariff().getTariffSpec().getPowerType()) == CapacityType.PRODUCTION) {
                for (DefaultCapacityManager capacityManager: capacityManagers) {
                    if (capacityManager.capacityProfile.capacityType == CapacityType.PRODUCTION) {
                        double currCapacity = -1 * capacityManager.computeCapacity(timeslot, subscription);
                        subscription.usePower(currCapacity); // negative usage is production
                        totalProduction = currCapacity;
                    }			
                }
            }
        }
        log.info("Total production for timeslot " + timeslot.getSerialNumber() + " = " + totalProduction);
    }

    public String toString() 
    {
	return "DefaultFactoredCustomer:" + getName();
    }
	
} // end class


