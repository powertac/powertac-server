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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.math.stat.descriptive.moment.Variance;
import org.apache.log4j.Logger;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.CapacityProfile.PermutationRule;
import org.powertac.factoredcustomer.ProfileRecommendation.Opinion;
import org.powertac.factoredcustomer.interfaces.*;

/**
 * Key class responsible for managing the tariff(s) for one customer across 
 * multiple capacity bundles if necessary.
 * 
 * @author Prashant Reddy
 */
@Domain
class LearningUtilityOptimizer extends DefaultUtilityOptimizer
{    
    private static final double NUM_SAMPLING_ITERATIONS = 30; 
    
    private Random recommendationMaker;
    
    
    LearningUtilityOptimizer(CustomerStructure structure, List<CapacityBundle> bundles) 
    {
        super(structure, bundles);
        log = Logger.getLogger(LearningUtilityOptimizer.class.getName());
    }
  
    @Override
    public void initialize()
    {
        inertiaSampler = new Random(randomSeedRepo.getRandomSeed("factoredcustomer.LearningUtilityOptimizer", 
                customerStructure.structureId, "InertiaSampler").getValue());
        tariffSelector = new Random(randomSeedRepo.getRandomSeed("factoredcustomer.LearningUtilityOptimizer", 
                customerStructure.structureId, "TariffSelector").getValue());
        recommendationMaker = new Random(randomSeedRepo.getRandomSeed("factoredcustomer.LearningUtilityOptimizer", 
                customerStructure.structureId, "RecommendationMaker").getValue());
        
        subscribeDefault();
    }
    
    @Override
    public void handleNewTariffs (List<Tariff> newTariffs)
    {        
        super.handleNewTariffs(newTariffs);
        
        recommendProfilesToBundles();
    }
        
    private void recommendProfilesToBundles() 
    {
        // Ignore cross-bundle optimization for now.  For example, we could 
        // produce more locally when we have higher local demand, but we don't 
        // currently support net-metering in PowerTAC, so it won't do anything.
        
        for (CapacityBundle bundle: capacityBundles)
        {
            if (bundle.getOptimizerStructure().receiveRecommendations == true) {
                recommendProfilesToBundle(bundle);
            }
        }
    }
    
    private void recommendProfilesToBundle(CapacityBundle bundle)
    {
        /**
          find all subscriptions
          for each subscription get forecast from each capacity originator in bundle
          for each forecast, get permutations
            + use learned history to determine which permutations are more likely
            + learn value for receptivityIndex and weights for recommendationFactors
            + weight permutations by learned history 
          compute weight of each permutation as inverse of sum of normalized usage charge and normalized distance from forecast
          assign weights to each permutation for each CM to make a recommendation
            + optimize weights of permutations over all capacity originators (anti-herding)
          make weighted recommendations to each capacity originator
            + monitor deviation of actual from forecast and also from recommended
        **/
        
        List<TariffSubscription> subscriptions = getBundleSubscriptions(bundle);
        
        Map<CapacityOriginator, ForecastRecord> forecasts = new HashMap<CapacityOriginator, ForecastRecord>();
        Map<CapacityOriginator, List<CapacityProfile>> perms = new HashMap<CapacityOriginator, List<CapacityProfile>>();
        Map<CapacityOriginator, ProfileRecommendation> recs = new HashMap<CapacityOriginator, ProfileRecommendation>();
        
        for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
            CapacityProfile forecast = capacityOriginator.getCurrentForecast();   
            double charge = computeProfileUsageCharge(forecast, subscriptions, capacityOriginator);
            ForecastRecord forecastRecord = new ForecastRecord(forecast, charge);
            forecasts.put(capacityOriginator, forecastRecord);
            PermutationRule permutationRule = bundle.getOptimizerStructure().permutationRule;
            if (permutationRule == null) permutationRule = PermutationRule.ALL_SHIFTS;
            perms.put(capacityOriginator, forecast.getPermutations(permutationRule));
            log.info(bundle.getName() + ": Evaluating " + perms.get(capacityOriginator).size() + " profile permutations for " 
                    + bundle.getCustomerInfo().getPowerType() + " capacity originator: " + capacityOriginator.getCapacityName());
            recs.put(capacityOriginator, getProfileRecommendation(capacityOriginator, bundle, forecastRecord, perms, subscriptions));
        }
        if (bundle.getOptimizerStructure().raconcileRecommendations == true) {
            reconcileRecommendations(subscriptions, forecasts, perms, recs);
        }
        for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
            if (capacityOriginator instanceof ProfileRecommendation.Listener) {
                ProfileRecommendation rec = recs.get(capacityOriginator);
                if (! rec.isEmpty()) {
                    log.info(bundle.getName() + ": Submitting " + rec.getOpinions().size() + " profile suggestions to " 
                             + bundle.getCustomerInfo().getPowerType() + " capacity originator: " + capacityOriginator.getCapacityName());
                    ((ProfileRecommendation.Listener) capacityOriginator).handleProfileRecommendation(rec);
                }
                else {
                    log.info(bundle.getName() + ": No beneficial profile permutations for " 
                             + bundle.getCustomerInfo().getPowerType() + " capacity originator: " + capacityOriginator.getCapacityName());
                }
            }
        }
    }
    
    private List<TariffSubscription> getBundleSubscriptions(CapacityBundle bundle) 
    {
        return tariffSubscriptionRepo.findSubscriptionsForCustomer(bundle.getCustomerInfo());
    }
    
    private ProfileRecommendation getProfileRecommendation(CapacityOriginator capacityOriginator, CapacityBundle bundle, 
                                                           ForecastRecord forecastRecord, 
                                                           Map<CapacityOriginator, List<CapacityProfile>> perms, 
                                                           List<TariffSubscription> subscriptions)
    {
        logRecommendationDetails("Forecast " + forecastRecord.capacityProfile + " usage charge = " + forecastRecord.usageCharge);

        ProfileRecommendation rec = new ProfileRecommendation();
        for (CapacityProfile perm: perms.get(capacityOriginator)) {
            double usageCharge = computeProfileUsageCharge(perm, subscriptions, capacityOriginator);
            logRecommendationDetails("Permutation " + perm + " usage charge = " + usageCharge);
            if (isPermutationAcceptable(capacityOriginator, bundle.getOptimizerStructure(), usageCharge, forecastRecord.usageCharge)) {
                Opinion opinion = rec.new Opinion();                
                opinion.usageCharge = computeProfileUsageCharge(perm, subscriptions, capacityOriginator);
                opinion.profileChange = forecastRecord.capacityProfile.distanceTo(perm);
                rec.setOpinion(perm, opinion);
            } 
        }
        if (! rec.isEmpty()) computeDerivedValues(rec, bundle.getOptimizerStructure());
        return rec;
    }
    
    private void computeDerivedValues(ProfileRecommendation rec, ProfileOptimizerStructure optimizerStructure)
    {
        rec.normalizeOpinions();
        rec.computeScores(optimizerStructure.profileChangeWeight, optimizerStructure.bundleValueWeight);
        rec.computeUtilities();
        rec.computeProbabilities(optimizerStructure.rationalityFactor);        
    }
  
    private double computeProfileUsageCharge(CapacityProfile profile, List<TariffSubscription> subscriptions, 
                                             CapacityOriginator capacityOriginator)
    {
        Timeslot timeslot = timeslotRepo.currentTimeslot();
        double totalCharge = 0.0;
        for (int i=0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
            double totalTimeslotUsage = profile.getCapacity(i);
            //System.out.println("timeslot usage total = " + totalTimeslotUsage);
            double timeslotCharge = 0.0;            
            for (TariffSubscription subscription: subscriptions) {
                double subTimeslotUsage = capacityOriginator.adjustCapacityForSubscription(timeslot, totalTimeslotUsage, subscription);
                //System.out.println("timeslot usage for subscription = " + totalTimeslotUsage);
                timeslotCharge += subscription.getTariff().getUsageCharge(timeslot.getStartInstant(), subTimeslotUsage, 0.0);                
                //System.out.println("timeslot charge = " + timeslotCharge);
            }
            totalCharge += timeslotCharge;
            timeslot = timeslot.getNext();
        }
        return totalCharge;    
    }
    
    private boolean isPermutationAcceptable(CapacityOriginator capacityOriginator, ProfileOptimizerStructure optimizerStructure, 
                                            double permCharge, double forecastCharge)
    {        
        Double threshold = null;
        switch (optimizerStructure.usageChargeStance) {
        case NEUTRAL: 
            return true;
        case BENEFIT: 
            if (capacityOriginator.getParentBundle().getCustomerInfo().getPowerType().isConsumption()) {  
                // less negative is better
                threshold = (1.0 - optimizerStructure.usageChargePercentBenefit) * forecastCharge;
            }
            else { 
                // PRODUCTION or STORAGE -- more positive is better
                threshold = (1.0 + optimizerStructure.usageChargePercentBenefit) * forecastCharge;
            }
            // fall through
        case THRESHOLD:
            if (threshold == null) {
                threshold = optimizerStructure.usageChargeThreshold;
            }
            return permCharge > threshold;
        default:
            throw new Error("Unexpected case in usage charge stance: " + optimizerStructure.usageChargeStance);
        }
    }
    
    private void reconcileRecommendations(List<TariffSubscription> subscriptions, 
                                          Map<CapacityOriginator, ForecastRecord> forecasts, 
                                          Map<CapacityOriginator, List<CapacityProfile>> perms, 
                                          Map<CapacityOriginator, ProfileRecommendation> recs)
    {        
        // TODO: adjust for accumulation towards tiered rates across capacity originators

        for (AbstractMap.Entry<CapacityOriginator, ProfileRecommendation> targetEntry: recs.entrySet()) {
            CapacityOriginator targetOriginator = targetEntry.getKey();
            ProfileRecommendation targetRec = targetEntry.getValue();
            if (targetRec.isEmpty() || 
                targetOriginator.getParentBundle().getCapacityOriginators().size() == 1) {
                continue;
            }
            double[] othersCapacities = new double[CapacityProfile.NUM_TIMESLOTS];
            for (int s=0; s < NUM_SAMPLING_ITERATIONS; ++s) {            
                for (AbstractMap.Entry<CapacityOriginator, ProfileRecommendation> otherEntry: recs.entrySet()) {
                    ProfileRecommendation otherRec = otherEntry.getValue();
                    CapacityProfile otherProfile; 
                    if (otherRec.isEmpty()) {
                        otherProfile = forecasts.get(otherEntry.getKey()).capacityProfile;
                    } else {
                        otherProfile = drawProfileFromRecommendation(otherRec);
                    }
                    for (int i=0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
                        othersCapacities[i] += otherProfile.getCapacity(i);
                        if (s == NUM_SAMPLING_ITERATIONS) {
                            othersCapacities[i] = othersCapacities[i] / (double) NUM_SAMPLING_ITERATIONS;
                        }
                    }
                }
            }
            CapacityProfile forecastProfile = forecasts.get(targetOriginator).capacityProfile;
            double forecastVariance = computeAggregateVariance(forecastProfile, othersCapacities);
            for (AbstractMap.Entry<CapacityProfile, Opinion> opinionEntry: targetRec.getOpinions().entrySet()) {
                CapacityProfile targetProfile = opinionEntry.getKey();
                double targetVariance = computeAggregateVariance(targetProfile, othersCapacities);
                double bundleValue = forecastVariance / targetVariance;
                opinionEntry.getValue().bundleValue = bundleValue;
            }
            computeDerivedValues(targetRec, targetOriginator.getParentBundle().getOptimizerStructure()); // TODO use local opt-structure
        }
    }
    
    private double computeAggregateVariance(CapacityProfile profile, double[] otherCapacities)
    {
        double[] aggCapacities = new double[CapacityProfile.NUM_TIMESLOTS];
        for (int i=0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
            aggCapacities[i] = profile.getCapacity(i) + otherCapacities[i];
        }
        return new Variance().evaluate(aggCapacities);
    }
    
    private CapacityProfile drawProfileFromRecommendation(ProfileRecommendation rec) 
    {
        double draw = recommendationMaker.nextFloat();        
        double sumProb = 0.0;
        for (AbstractMap.Entry<CapacityProfile, Double> entry: rec.getProbabilities().entrySet()) {
            sumProb += entry.getValue();
            if (draw < sumProb) {
                return entry.getKey();
            }
        }        
        throw new Error("Drawing from recommendation resulted in a null profile!");
    }
    
    private void logRecommendationDetails(String msg)
    {
        //log.info(msg);
        log.debug(msg);
    }
    
    // INNER CLASS
    
    private class ForecastRecord
    {
        CapacityProfile capacityProfile;
        double usageCharge;
        
        ForecastRecord(CapacityProfile p, double c)
        {
            capacityProfile = p;
            usageCharge = c;
        }
    }
    
} // end class


