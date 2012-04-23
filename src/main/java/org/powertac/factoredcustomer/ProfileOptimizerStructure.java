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

import org.w3c.dom.*;
import org.powertac.factoredcustomer.CapacityProfile.PermutationRule;
import org.powertac.factoredcustomer.interfaces.CapacityBundle;

/**
 * Data-holder class for parsed configuration elements that control the 
 * capacity profile optimization of a capacity bundle. Relevant members 
 * are declared final in the package scope.
 *
 * @author Prashant Reddy
 */
public final class ProfileOptimizerStructure
{
    enum UsageChargeStance { NEUTRAL, BENEFIT, THRESHOLD }
    enum ProfileSelectionMethod { BEST_UTILITY, LOGIT_CHOICE }
    
    private static final double DEFAULT_REACTIVITY_FACTOR = 1.0; 
    private static final double DEFAULT_RECEPTIVITY_FACTOR = 1.0; 
    private static final double DEFAULT_RATIONALITY_FACTOR = 1.0; 
    
    private static final UsageChargeStance DEFAULT_USAGE_CHARGE_STANCE = UsageChargeStance.BENEFIT;   
    private static final double DEFAULT_USAGE_CHARGE_PERCENT_BENEFIT = 0.01;  // 1% improvement 
    
    private static final double DEFAULT_PROFILE_CHANGE_WEIGHT = -1.0;
    private static final double DEFAULT_BUNDLE_VALUE_WEIGHT = +10.0;
    
    private final CustomerStructure customerStructure;
    private final CapacityBundle capacityBundle;
    
    final boolean receiveRecommendations;
    final PermutationRule permutationRule;
    final boolean raconcileRecommendations;
    
    final ProfileSelectionMethod profileSelectionMethod = ProfileSelectionMethod.LOGIT_CHOICE;

    // factors controlling responsiveness to recommendation
    final double reactivityFactor;  // [0.0, 1.0]
    final double receptivityFactor;  // [0.0, 1.0]
    final double rationalityFactor;  // [0.0, 1.0]
    
    // required percent benefit in usage charge vs. forecast profile
    final UsageChargeStance usageChargeStance;
    final double usageChargePercentBenefit;
    final double usageChargeThreshold;  // [0.0, +inf)

    // scoring weights of other factors relative to fixed usage charge weight of +/-1.
    final double profileChangeWeight;  // (-inf, 0.0]
    final double bundleValueWeight;  //  [0.0, inf]

    
    ProfileOptimizerStructure(CustomerStructure structure, CapacityBundle bundle, Element xml)
    {
        customerStructure = structure;
        capacityBundle = bundle;        
        
        if (xml == null) {
            receiveRecommendations = false;
            raconcileRecommendations = false;
            permutationRule = null;
            reactivityFactor = DEFAULT_REACTIVITY_FACTOR; 
            receptivityFactor = DEFAULT_RECEPTIVITY_FACTOR; 
            rationalityFactor = DEFAULT_RATIONALITY_FACTOR; 
            usageChargeStance = DEFAULT_USAGE_CHARGE_STANCE;   
            usageChargePercentBenefit = DEFAULT_USAGE_CHARGE_PERCENT_BENEFIT;   
            usageChargeThreshold = Double.NaN;
            profileChangeWeight = DEFAULT_PROFILE_CHANGE_WEIGHT;
            bundleValueWeight = DEFAULT_BUNDLE_VALUE_WEIGHT;
        } else {
            receiveRecommendations = Boolean.parseBoolean(xml.getAttribute("recommendation"));
            raconcileRecommendations = Boolean.parseBoolean(xml.getAttribute("reconcile"));
            permutationRule = Enum.valueOf(PermutationRule.class, xml.getAttribute("permutationRule"));
            
            Element responseFactorsElement = (Element) xml.getElementsByTagName("responseFactors").item(0);
            reactivityFactor = Double.parseDouble(responseFactorsElement.getAttribute("reactivity"));
            receptivityFactor = Double.parseDouble(responseFactorsElement.getAttribute("receptivity"));
            rationalityFactor = Double.parseDouble(responseFactorsElement.getAttribute("rationality"));
            
            Element constraintsElement = (Element) xml.getElementsByTagName("constraints").item(0);
            usageChargeStance = Enum.valueOf(UsageChargeStance.class, constraintsElement.getAttribute("usageChargeStance"));
            String percentBenefitString = constraintsElement.getAttribute("percentBenefit");
            usageChargePercentBenefit = percentBenefitString.isEmpty() ? Double.NaN : Double.parseDouble(percentBenefitString);
            String thresholdString = constraintsElement.getAttribute("threshold");
            usageChargeThreshold = thresholdString.isEmpty() ? Double.NaN : Double.parseDouble(thresholdString);
            
            Element scoringWeightsElement = (Element) xml.getElementsByTagName("scoringWeights").item(0);
            profileChangeWeight = Double.parseDouble(scoringWeightsElement.getAttribute("profileChange"));
            bundleValueWeight = Double.parseDouble(scoringWeightsElement.getAttribute("bundleValue"));
        }
    }
    
    CustomerStructure getCustomerStructure()
    {
        return customerStructure;
    }
    
    CapacityBundle getCapacityBundle()
    {
        return capacityBundle;
    }
    
} // end class

