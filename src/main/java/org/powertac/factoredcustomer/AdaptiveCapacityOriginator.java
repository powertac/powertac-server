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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.ProfileOptimizerStructure.ProfileSelectionMethod;
import org.powertac.factoredcustomer.ProfileRecommendation.Opinion;
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

    private Map<TariffSubscription, Map<Integer, Double>> forecastCapacitiesPerSub;
    
    
    
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
        
        forecastCapacitiesPerSub = new HashMap<TariffSubscription, Map<Integer,Double>>();
    }
    
    @Override /** @code{ProfileRecommendation.Listener} **/
    public void handleProfileRecommendation(ProfileRecommendation globalRec)
    {        
        //log.info("handleProfileRecommendation()");
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
            //log.info("Daniel getting opinions");
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
        //log.info("chosen: ALL-AVG  " + chosenProfile.toString());
        overwriteForecastCapacities(service.getTimeslotRepo().currentTimeslot(),
                                    chosenProfile);
    }

    @Override /** @code{ProfileRecommendation.Listener} **/
    public void handleProfileRecommendationPerSub(ProfileRecommendation globalRec, TariffSubscription sub, CapacityProfile capacityProfile)
    {        
        //log.info("handleProfileRecommendationPerSub()");
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
            //log.info("Daniel getting opinions");
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
        if (!chosenProfile.toString().equals(capacityProfile.toString())) {
          //log.info("handleProfileRecommendationPerSub(" + sub.getCustomer().getName() + ", " + sub.getTariff().getId() + ") DIFFERENT:");
          //log.info("forecast: " + capacityProfile.toString());
        }
        else {
          //log.info("handleProfileRecommendationPerSub(" + sub.getCustomer().getName() + ", " + sub.getTariff().getId() + ") SAME:");
        }
        //log.info("srv chosen: " + sub.getCustomer().getName() + " " +  sub.getTariff().getId() + " " + chosenProfile.toString());
        overwriteForecastCapacitiesPerSub(service.getTimeslotRepo().currentTimeslot(),
                                    chosenProfile, sub);
    }
    private CapacityProfile selectBestProfileInRecommendation(ProfileRecommendation rec) 
    {
        //log.info("selectBestProfileInRecommendation()");
        double bestUtility = Double.MIN_VALUE;
        CapacityProfile bestProfile = null;
        for (AbstractMap.Entry<CapacityProfile, Double> entry: rec.getUtilities().entrySet()) {
            if (entry.getValue() > bestUtility) {
                bestUtility = entry.getValue();
                bestProfile = entry.getKey();
            }
        }        
        if (bestProfile == null) throw new Error("Best profile in recommendation is null!");
        //log.info("selectBestProfileInRecommendation() " + Arrays.toString(bestProfile.values.toArray()));
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
        //log.info("Daniel overwriteForecastCapacities()");
        Timeslot slider = timeslot;
        for (int i=0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
            //log.info("Daniel forecastCapacities.put(" + slider.getSerialNumber() + "," + profile.getCapacity(i) + ")");
            forecastCapacities.put(slider.getSerialNumber(), profile.getCapacity(i));
            slider = service.getTimeslotRepo().getNext(slider);
        }
    }
    
    private void overwriteForecastCapacitiesPerSub(Timeslot timeslot, CapacityProfile profile, TariffSubscription sub)
    {
        //log.info("Daniel overwriteForecastCapacitiesPerSub()");
        Timeslot slider = timeslot;
        for (int i=0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
            //log.info("Daniel forecastCapacitiesPerSub.put(" + sub.getCustomer().getName() + " " + sub.getTariff().getId() + " " + slider.getSerialNumber() + "," + profile.getCapacity(i) + ")");
            int futureTimeslot = slider.getSerialNumber();
            double futureCapacity = profile.getCapacity(i);
            insertIntoForecastCapacitiesPerSub(sub, futureTimeslot, futureCapacity);
            slider = service.getTimeslotRepo().getNext(slider);
        }
    }

    private void insertIntoForecastCapacitiesPerSub(TariffSubscription sub,
        int futureTimeslot, double futureCapacity) {
      Map<Integer, Double> ts2capacity = forecastCapacitiesPerSub.get(sub);
      if (null == ts2capacity) {
        ts2capacity = new HashMap<Integer, Double>();
        forecastCapacitiesPerSub.put(sub, ts2capacity);        
      }
      //log.info("forecastCapacitiesPerSub[" + sub.getTariff().getId() + "," + futureTimeslot + "]=" + futureCapacity);
      ts2capacity.put(futureTimeslot, futureCapacity);
    }
    
    @Override
    public double useCapacity(TariffSubscription subscription)
    {
        //log.info("useCapacity()");
        int timeslot = service.getTimeslotRepo().currentSerialNumber();
        
        // we don't re-adjust for current weather here; would not be accurate for wind/solar production
        // 
        // Daniel: try to get per sub first, if doesn't work get the 
        // old, averaged one
        double forecastCapacity = getForecastCapacityPerSub(timeslot, subscription);
        //if (null == forecastCapacity) {
        //  forecastCapacity = getForecastCapacity(timeslot);
        //  //log.info("Daniel: failed to get sub capacity!");
        //}
        //else {
        //  // TODO: remove, just for print
        //  double perAllCapacity = getForecastCapacity(timeslot);
        //  //log.info("Daniel: succeeded to get sub capacity!, " + ((forecastCapacity != perAllCapacity) ? "DIFFERENT" : "") + " normalized: " + forecastCapacity/subscription.getCustomersCommitted() + " instead of " + perAllCapacity/subscription.getCustomersCommitted() + " " + subscription.getTariff().getBroker().getUsername() + " time=" + service.getTimeslotRepo().getTimeForIndex(timeslot).toDateTime().getHourOfDay());
        //}
//        logCapacityDetails(logIdentifier + ": Forecast capacity being used for timeslot " 
//                           + timeslot + " = " + forecastCapacity);        
        //log.info(logIdentifier + ": srv Daniel " + subscription.getCustomer().getName() + " " + subscription.getTariff().getId() + " Forecast capacity being used for timeslot " 
        //                   + timeslot + " = " + forecastCapacity + " instead of forecastCapacities(" + timeslot + ")=" + getForecastCapacity(timeslot));

        double adjustedCapacity = forecastCapacity;       
        adjustedCapacity = adjustCapacityForSubscription(timeslot, adjustedCapacity, subscription);
        //log.info("Daniel Adjusted capacity 1: " + adjustedCapacity);
        if (Double.isNaN(adjustedCapacity)) {
            throw new Error("Adjusted capacity is NaN for forecast capacity = " + forecastCapacity);
        }
        
        adjustedCapacity = truncateTo2Decimals(adjustedCapacity);
        actualCapacities.put(timeslot, adjustedCapacity);        
        log.info(logIdentifier + ": Adjusted capacity for tariff " + subscription.getTariff().getId() + " = " + adjustedCapacity);        
        return adjustedCapacity;
    }







    @Override
    public CapacityProfile getCurrentForecastPerSub(TariffSubscription sub) {
      int timeslot = service.getTimeslotRepo().currentSerialNumber();
      return getForecastPerSubStartingAt(timeslot, sub);
    }

    private CapacityProfile getForecastPerSubStartingAt(int startingTimeslot,
        TariffSubscription subscription) {
      int timeslot = startingTimeslot;
      List<Double> values = new ArrayList<Double>();
      for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
        values.add(getForecastCapacityPerSub(timeslot, subscription));
        timeslot += 1;
      }
      return new CapacityProfile(values);
    }

    private Double getForecastCapacityPerSub(int timeslot,
        TariffSubscription subscription) {

      Map<Integer, Double> ts2capacity = forecastCapacitiesPerSub.get(subscription);
      
      if (null == ts2capacity || null == ts2capacity.get(timeslot)) {
        //log.info("Daniel: failed to get sub capacity! falling back to default...");
        return getForecastCapacity(timeslot);
      } else {
        // TODO: remove, just for print
        double perAllCapacity = getForecastCapacity(timeslot);
        double perSubCapacity = ts2capacity.get(timeslot);
        //log.info("Daniel: succeeded to get sub capacity!, " + ((perSubCapacity != perAllCapacity) ? "DIFFERENT " : " ") + "nonShifted=" + perAllCapacity + " shifted=" + perSubCapacity + " " + subscription.getTariff().getBroker().getUsername() + " time=" + service.getTimeslotRepo().getTimeForIndex(timeslot).toDateTime(DateTimeZone.UTC).getHourOfDay());
        //log.info("sub capacity for " + subscription.getCustomer().getName() + " " + subscription.getTariff().getId() + ": " + perSubCapacity);
        return perSubCapacity; 
      }
    } 
    
} // end class


