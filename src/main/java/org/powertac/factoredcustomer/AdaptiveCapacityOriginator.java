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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import org.apache.log4j.Logger;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.ProfileOptimizerStructure.ProfileSelectionMethod;
import org.powertac.factoredcustomer.ProfileRecommendation.ScoringFactor;
import org.powertac.factoredcustomer.utils.SeedIdGenerator;

/**
 * Extends @code{DefaultCapacityOriginator} to adapt to the learning behavior 
 * of @code{LearningUtilityOptimizer}.
 * 
 * @author Prashant Reddy
 */
@Domain
final class AdaptiveCapacityOriginator extends DefaultCapacityOriginator
     implements ProfileRecommendation.Listener
{    
    //private RandomSeedRepo randomSeedRepo;
    
    private final ProfileOptimizerStructure optimizerStructure;

    private final Random recommendationHandler;

    AdaptiveCapacityOriginator(FactoredCustomerService service,
                               CapacityStructure capacityStructure,
                               DefaultCapacityBundle bundle) 
    {
        super(service, capacityStructure, bundle);
        log = Logger.getLogger(AdaptiveCapacityOriginator.class.getName());
        
        //randomSeedRepo = (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");

        optimizerStructure = getParentBundle().getOptimizerStructure();

        recommendationHandler =
                new Random(service.getRandomSeedRepo()
                           .getRandomSeed("factoredcustomer.AdaptiveCapacityOriginator", 
                                          SeedIdGenerator.getId(),
                                          "RecommendationHandler")
                                          .getValue());
    }
    
    @Override /** @code{ProfileRecommendation.Listener} **/
    public void handleProfileRecommendation(ProfileRecommendation globalRec)
    {        
        double draw1 = recommendationHandler.nextFloat();
        if (draw1 > optimizerStructure.reactivityFactor) {
            log.info(logIdentifier + ": Ignoring received profile recommendation");
            return;
        }
        
        ProfileRecommendation localRec;
        double draw2 = recommendationHandler.nextFloat();
        if (draw2 < optimizerStructure.receptivityFactor) {
            log.info(logIdentifier + ": Adopting profile recommendation as received");
            localRec = globalRec;
        }
        else {
            localRec = new ProfileRecommendation(globalRec.getOpinions());
            
            Map<ScoringFactor, Double> weights = new HashMap<ScoringFactor, Double>();
            weights.put(ScoringFactor.PROFILE_CHANGE, optimizerStructure.profileChangeWeight);
            weights.put(ScoringFactor.BUNDLE_VALUE, optimizerStructure.bundleValueWeight);
            localRec.computeScores(weights);
            
            localRec.computeUtilities();
            localRec.computeProbabilities(optimizerStructure.rationalityFactor);
        }
        CapacityProfile chosenProfile;
        if (optimizerStructure.profileSelectionMethod == ProfileSelectionMethod.BEST_UTILITY) {
            chosenProfile = selectBestProfileInRecommendation(localRec);        
        } else { // LOGIT_CHOICE 
            chosenProfile = drawProfileFromRecommendation(localRec);        
        }
        overwriteForecastCapacities(service.getTimeslotRepo().currentTimeslot(),
                                    chosenProfile);
    }

    private CapacityProfile selectBestProfileInRecommendation(ProfileRecommendation rec) 
    {
        double bestUtility = Double.MIN_VALUE;
        CapacityProfile bestProfile = null;
        for (AbstractMap.Entry<CapacityProfile, Double> entry: rec.getUtilities().entrySet()) {
            if (entry.getValue() > bestUtility) {
                bestUtility = entry.getValue();
                bestProfile = entry.getKey();
            }
        }
        if (bestProfile == null)
          throw new Error("Best profile in recommendation is null!");
        return bestProfile;
    }
    
    private CapacityProfile drawProfileFromRecommendation(ProfileRecommendation rec) 
    {
        double draw = recommendationHandler.nextFloat();
        // sort map entries, for reproducability
        ArrayList<Map.Entry<CapacityProfile, Double>> l =
            new ArrayList<Entry<CapacityProfile, Double>>(rec.getProbabilities().entrySet());
        Collections.sort(l, new Comparator<Map.Entry<CapacityProfile, Double>>(){
          @Override
          public int compare(Map.Entry<CapacityProfile, Double> o1, Map.Entry<CapacityProfile, Double> o2) {
             return o1.getValue().compareTo(o2.getValue());
         }});
        // use the sorted map and the draw to sample an entry 
        double sumProb = 0.0;
        for (AbstractMap.Entry<CapacityProfile, Double> entry: l) {
            sumProb += entry.getValue();
            if (draw < sumProb) {
                return entry.getKey();
            }
        }        
        throw new Error("Drawing from recommendation resulted in a null profile!");
    }
    
    private void overwriteForecastCapacities(Timeslot timeslot, CapacityProfile profile)
    {
        Timeslot slider = timeslot;
        for (int i=0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
            forecastCapacities.put(slider.getSerialNumber(), profile.getCapacity(i));
            slider = service.getTimeslotRepo().getNext(slider);
        }
    }
    
    @Override
    public double useCapacity(TariffSubscription subscription)
    {
        int timeslot = service.getTimeslotRepo().currentSerialNumber();
        
        // we don't re-adjust for current weather here; would not be accurate for wind/solar production
        double forecastCapacity = getForecastCapacity(timeslot);
        logCapacityDetails(logIdentifier + ": Forecast capacity being used for timeslot " 
                           + timeslot + " = " + forecastCapacity);        

        double adjustedCapacity = forecastCapacity;       
        adjustedCapacity = adjustCapacityForSubscription(timeslot, adjustedCapacity, subscription);
        if (Double.isNaN(adjustedCapacity)) {
            throw new Error("Adjusted capacity is NaN for forecast capacity = " + forecastCapacity);
        }
        
        adjustedCapacity = truncateTo2Decimals(adjustedCapacity);
        actualCapacities.put(timeslot, adjustedCapacity);        
        log.info(logIdentifier + ": Adjusted capacity for tariff " + subscription.getTariff().getId() + " = " + adjustedCapacity);        
        return adjustedCapacity;
    }
} // end class


