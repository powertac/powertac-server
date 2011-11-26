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

import org.apache.log4j.Logger;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Prashant Reddy
 */
class DefaultCapacityManager
{
    private static Logger log = Logger.getLogger(DefaultCapacityManager.class.getName());

    @Autowired
    TimeService timeService;
    
    @Autowired
    TimeslotRepo timeslotRepo;

    protected static final int BASE_CAPACITY_TIMESLOTS = 24;
    protected static final int NUM_HOURS_IN_DAY = 24;
    
    CustomerProfile customerProfile;
    CapacityProfile capacityProfile;
    
    DefaultCapacityManager(CustomerProfile customer, CapacityProfile capacity) 
    {
        customerProfile = customer;
        capacityProfile = capacity;
        
        timeService = (TimeService) SpringApplicationContext.getBean("timeService");
        timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    }
    
    double computeDailyUsageCharge(Tariff tariff)
    {
        Timeslot hourlyTimeslot = timeslotRepo.currentTimeslot();
		
	double totalUsage = 0.0;
	double totalCharge = 0.0;
	for (int i=0; i < NUM_HOURS_IN_DAY; ++i) {
	    double baseCapacity = 0;
	    if (capacityProfile.specType == CapacityProfile.SpecType.BEHAVIORS) {
	        BehaviorsProfile behaviorsProfile = (BehaviorsProfile) capacityProfile;
	        if (behaviorsProfile.baseTotalCapacity != null) {
	            baseCapacity = ((BehaviorsProfile) capacityProfile).baseTotalCapacity.drawSample();
	        } else {
	            double draw = behaviorsProfile.baseCapacityPerCustomer.drawSample();
	            baseCapacity += draw * customerProfile.customerInfo.getPopulation();
	        }
	    }
	    double hourlyUsage = customerProfile.customerInfo.getPopulation() * (baseCapacity / BASE_CAPACITY_TIMESLOTS);
	    totalCharge += tariff.getUsageCharge(hourlyTimeslot.getStartInstant(), hourlyUsage, totalUsage);
	    totalUsage += hourlyUsage;
	    hourlyTimeslot = hourlyTimeslot.getNext();
	}
	return totalCharge;
    }
	
    double drawBaseCapacitySample(TariffSubscription subscription) 
    {
        double baseCapacity = 0.0;
        BehaviorsProfile behaviorsProfile = (BehaviorsProfile) capacityProfile;
        if (behaviorsProfile.baseTotalCapacity != null) {
            double popRatio = subscription.getCustomersCommitted() / customerProfile.customerInfo.getPopulation();
            baseCapacity = popRatio * behaviorsProfile.baseTotalCapacity.drawSample() / BASE_CAPACITY_TIMESLOTS;
        } else {
            for (int i=0; i < subscription.getCustomersCommitted(); ++i) {
                double draw = behaviorsProfile.baseCapacityPerCustomer.drawSample();
                baseCapacity += draw / BASE_CAPACITY_TIMESLOTS;
            }
        }
        return truncateTo2Decimals(baseCapacity);
    }
	
    double computeCapacity(Timeslot timeslot, TariffSubscription subscription)
    {
        double computedCapacity;
        if (capacityProfile.specType == CapacityProfile.SpecType.BEHAVIORS) {
            computedCapacity = computeCapacityFromBehaviors(timeslot, subscription);
        } else {  // CapacityProfile.SpecType.FACTORED
            computedCapacity = computeCapacityFromFactors(timeslot, subscription);
        }
        return truncateTo2Decimals(computedCapacity);
    }

    double computeCapacityFromBehaviors(Timeslot timeslot, TariffSubscription subscription)
    {
        double baseCapacity = drawBaseCapacitySample(subscription);
        log.info("Base capacity from behaviors = " + baseCapacity);

        double adjustedCapacity = baseCapacity;
        BehaviorsProfile behaviorsProfile = (BehaviorsProfile) capacityProfile;
        if (behaviorsProfile.elasticityOfCapacity != null) {
            adjustedCapacity = computeElasticCapacity(timeslot, subscription, baseCapacity);
        }
        adjustedCapacity = truncateTo2Decimals(adjustedCapacity);
        log.info("Adjusted capacity from behaviors = " + adjustedCapacity);
	return adjustedCapacity;
    }

    double computeCapacityFromFactors(Timeslot timeslot, TariffSubscription subscription)
    {
        double baseCapacity = drawBaseCapacitySample(subscription);
        log.info("Base capacity from factors = " + baseCapacity);

        double adjustedCapacity = baseCapacity;
        BehaviorsProfile behaviorsProfile = (BehaviorsProfile) capacityProfile;
        if (behaviorsProfile.elasticityOfCapacity != null) {
            adjustedCapacity = computeElasticCapacity(timeslot, subscription, baseCapacity);
        }
        
        // TODO: Adjust for the other specified factors 

        adjustedCapacity = truncateTo2Decimals(adjustedCapacity);
        log.info("Adjusted capacity from factors = " + adjustedCapacity);        
        return adjustedCapacity;
    }

    double computeElasticCapacity(Timeslot timeslot, TariffSubscription subscription, double baseCapacity)
    {
        double chargeForBase = subscription.getTariff().getUsageCharge(timeslot.getStartInstant(), 
                                                                       baseCapacity, subscription.getTotalUsage());
        double rateForBase = chargeForBase / baseCapacity;
        double rateRatio = rateForBase / capacityProfile.baseBenchmarkRate;
        BehaviorsProfile behaviorsProfile = (BehaviorsProfile) capacityProfile;
        double[][] elasticity = behaviorsProfile.elasticityOfCapacity;
        double elasticityFactor = lookupElasticityFactor(rateRatio, elasticity);
        log.debug("Compute elastic capacity - elasticityFactor = " + elasticityFactor);
        double elasticCapacity = baseCapacity * elasticityFactor;
	return elasticCapacity;
    }
	
    double lookupElasticityFactor(double rateRatio, double[][] elasticity)
    {
        if (rateRatio == 1 || elasticity.length == 0) return 1.0;	
        final int RATE_RATIO_INDEX = 0;
        final int CAPACITY_FACTOR_INDEX = 1;
        double rateLowerBound = Double.NEGATIVE_INFINITY;
        double rateUpperBound = Double.POSITIVE_INFINITY;
        double lowerBoundCapacityFactor = 1.0;
        double upperBoundCapacityFactor = 1.0;
        for (int i=0; i < elasticity.length; ++i) {
            double r = elasticity[i][RATE_RATIO_INDEX];
            if (r <= rateRatio && r > rateLowerBound) {
                rateLowerBound = r;
                lowerBoundCapacityFactor = elasticity[i][CAPACITY_FACTOR_INDEX];
            }
            if (r >= rateRatio && r < rateUpperBound) {
                rateUpperBound = r;
                upperBoundCapacityFactor = elasticity[i][CAPACITY_FACTOR_INDEX];
            }
        }	
        return (rateRatio < 1) ? upperBoundCapacityFactor : lowerBoundCapacityFactor;
    }
    
    private static double truncateTo2Decimals(double x)
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
		
}
