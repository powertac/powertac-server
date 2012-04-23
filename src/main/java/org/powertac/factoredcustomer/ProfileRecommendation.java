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
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

/**
 * Contains maps of opinions, scores, utilities, and choice probabilities for each CapacityProfile.
 * 
 * @author Prashant Reddy
 */
@Domain
public class ProfileRecommendation
{
    enum ScoringFactor { USAGE_CHARGE, PROFILE_CHANGE, BUNDLE_VALUE }
    
    private static final double UTILITY_RANGE_MAX_VALUE = 3.0;  // range = [-3.0, +3.0]
    
    private final Map<CapacityProfile, Opinion> opinions;
    private final Map<CapacityProfile, Double> scores = new HashMap<CapacityProfile, Double>();
    private final Map<CapacityProfile, Double> utilities = new HashMap<CapacityProfile, Double>();
    private final Map<CapacityProfile, Double> probabilities = new HashMap<CapacityProfile, Double>();


    ProfileRecommendation()
    {
        opinions = new HashMap<CapacityProfile, Opinion>();
    }
    
    ProfileRecommendation(Map<CapacityProfile, Opinion> map)
    {
        opinions = map;
    }

    @StateChange
    public void setOpinion(CapacityProfile profile, Opinion opinion)
    {
        opinions.put(profile, opinion);
    }
    
    public Map<CapacityProfile, Opinion> getOpinions()
    {
        return opinions;
    }
 
    @StateChange
    public void setScore(CapacityProfile profile, Double score)
    {
        scores.put(profile, score);
    }
    
    public Map<CapacityProfile, Double> getScores()
    {
        return scores;
    }
    
    public Map<CapacityProfile, Double> getUtilities()
    {
        return utilities;
    }

    public Map<CapacityProfile, Double> getProbabilities()
    {
        return probabilities;
    }
    
    public boolean isEmpty() 
    {
        return opinions.size() == 0;
    }

    @StateChange
    public void normalizeOpinions()
    {
        double sumUsageCharge = 0.0;
        double sumProfileChange = 0.0;
        double sumBundleValue = 0.0;
        
        for (Opinion opinion: opinions.values()) {
            sumUsageCharge += opinion.usageCharge;
            sumProfileChange += opinion.profileChange;
            sumBundleValue += opinion.bundleValue;
        }
        for (Opinion opinion: opinions.values()) {
            opinion.normUsageCharge = sumUsageCharge == 0.0 ? 0.0 : opinion.usageCharge / sumUsageCharge;
            opinion.normProfileChange = sumProfileChange == 0.0 ? 0.0 : opinion.profileChange / sumProfileChange;
            opinion.normBundleValue = sumBundleValue == 0.0 ? 0.0 : opinion.bundleValue / sumBundleValue;
        }        
    }
    
    public void computeScores(Map<ScoringFactor, Double> weights)
    {
        computeScores(weights.get(ScoringFactor.PROFILE_CHANGE), weights.get(ScoringFactor.BUNDLE_VALUE));
    }
    
    @StateChange
    public void computeScores(double profileChangeWeight, double bundleValueWeight)
    {
        for (CapacityProfile profile: opinions.keySet()) {  
            Opinion opinion = opinions.get(profile);
            double usageChargeScoringSign = opinion.normUsageCharge > 0 ? +1.0 : -1.0;
            Double score = usageChargeScoringSign * opinion.normUsageCharge
                           + profileChangeWeight * opinion.normProfileChange
                           + bundleValueWeight * opinion.normBundleValue;
            scores.put(profile, score);
        }
    }
    
    @StateChange
    public void computeUtilities()
    {
        if (scores.size() == 1) {
            utilities.put(scores.keySet().iterator().next(), UTILITY_RANGE_MAX_VALUE);
            return;
        } 
        double best = Collections.max(scores.values());
        double worst = Collections.max(scores.values());
        double sum = 0.0;
        for (Double score: scores.values()) {
            sum += score;
        }
        double mean = sum / scores.size();
        double basis = Math.max((best - mean), (mean - worst));
        if (Math.abs(basis - 0.0) < 0.0001) {
            for (AbstractMap.Entry<CapacityProfile, Double> entry: scores.entrySet()) {  
                utilities.put(entry.getKey(), UTILITY_RANGE_MAX_VALUE);
            }
        } else {        
            for (AbstractMap.Entry<CapacityProfile, Double> entry: scores.entrySet()) {  
                double utility = ((entry.getValue() - mean) / basis) * UTILITY_RANGE_MAX_VALUE;  
                utilities.put(entry.getKey(), utility);
            }
        }
    }

    @StateChange
    public void computeProbabilities(double rationality)
    {
        // multinomical logit choice model; utilities expected to be in [-3.0, +3.0]
        
        double denominator = 0.0;
        for (AbstractMap.Entry<CapacityProfile, Double> entry: utilities.entrySet()) {  
            double numerator = Math.exp(rationality * utilities.get(entry.getKey()));
            probabilities.put(entry.getKey(), numerator);
            denominator += numerator;
        }
        for (AbstractMap.Entry<CapacityProfile, Double> entry: probabilities.entrySet()) {  
            double numerator = entry.getValue(); 
            double probability = numerator / denominator;  // normalize 
            if (Double.isNaN(probability)) {
                System.err.println(this.getClass().getCanonicalName() + ": Computed probability is NaN!");
                System.err.println("  *** opinions: " + opinions.keySet() + ": " + opinions.values());
                System.err.println("  *** scores: " + scores.keySet() + ": " + scores.values());
                System.err.println("  *** utilities: " + utilities.keySet() + ": " + utilities.values());
                System.err.println("  *** probabilities: " + probabilities.keySet() + ": " + probabilities.values());
                throw new Error("Computed probability is NaN!");
            }
            entry.setValue(probability);
        }           
    }

    // PUBLIC INNER CLASSES
    
    public class Opinion
    {
        // raw computed metrics
        double usageCharge; // (-inf, +inf) under current tariff subscriptions
        double profileChange;  // [0, +inf)
        double bundleValue;  // [0, +inf)
        
        // normalized metrics
        double normUsageCharge;
        double normProfileChange;
        double normBundleValue;        
        
        public String toString() 
        {
            return "Opinion:[" + usageCharge + ", " + profileChange + ", " + bundleValue + ", "
                    + normUsageCharge + ", " + normProfileChange + ", " + normBundleValue + "]";
        }
    }
     
    public interface Listener
    {
        void handleProfileRecommendation(ProfileRecommendation rec);
    }
    
} // end class

