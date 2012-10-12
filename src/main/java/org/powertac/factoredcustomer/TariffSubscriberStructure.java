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

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.*;
import org.powertac.factoredcustomer.ProbabilityDistribution;
import org.powertac.factoredcustomer.interfaces.CapacityBundle;

/**
 * Data-holder class for parsed configuration elements of one tariff subscriber, 
 * which typically corresponds to one capapcity bundle. Relevant members are 
 * declared final in the package scope.
 *
 * @author Prashant Reddy
 */
public final class TariffSubscriberStructure
{
    enum AllocationMethod { TOTAL_ORDER, LOGIT_CHOICE }
    
    private final CustomerStructure customerStructure;
    private final CapacityBundle capacityBundle;
    
    final boolean benchmarkRiskEnabled;
    final double benchmarkRiskRatio;
    final boolean tariffThrottlingEnabled;
    final double interruptibilityDiscount;
    final Double expMeanPriceWeight;
    final Double maxValuePriceWeight;
    final Double realizedPriceWeight;
    final AllocationMethod allocationMethod;
    final List<List<Double>> totalOrderRules = new ArrayList<List<Double>>();
    final double logitChoiceRationality; 
    final int reconsiderationPeriod;
    final ProbabilityDistribution inertiaDistribution;
    final ProbabilityDistribution customerWealthDistribution;
    final double customerWealthReferenceMedian;
    final ProbabilityDistribution newTariffsExposure;
    final ProbabilityDistribution switchingDelay;
    final ProbabilityDistribution waitAfterSwitch;
    
    
    TariffSubscriberStructure(CustomerStructure structure, CapacityBundle bundle, Element xml)
    {
        customerStructure = structure;
        capacityBundle = bundle;

        Element constraintsElement = (Element) xml.getElementsByTagName("constraints").item(0);
        if (constraintsElement != null) {
            Element benchmarkRiskElement = (Element) constraintsElement.getElementsByTagName("benchmarkRisk").item(0);
            if (benchmarkRiskElement != null) {
                benchmarkRiskEnabled = Boolean.parseBoolean(benchmarkRiskElement.getAttribute("enable"));
                double[][] ratio = ParserFunctions.parseMapToDoubleArray(benchmarkRiskElement.getAttribute("ratio"));
                benchmarkRiskRatio = ratio[0][0] / ratio[0][1];
            } else {
                benchmarkRiskEnabled = false;
                benchmarkRiskRatio = Double.NaN;
            }          
            Element tariffThrottlingElement = (Element) constraintsElement.getElementsByTagName("tariffThrottling").item(0);
            if (tariffThrottlingElement != null) {
                tariffThrottlingEnabled = Boolean.parseBoolean(tariffThrottlingElement.getAttribute("enable"));
            } else tariffThrottlingEnabled = false;
        } else throw new Error("Tariff subscriber constraints element must be included, even if empty.");

        Element influenceFactorsElement = (Element) xml.getElementsByTagName("influenceFactors").item(0);
        if (influenceFactorsElement != null) {
            Element interruptibilityElement = (Element) influenceFactorsElement.getElementsByTagName("interruptibility").item(0);
            if (interruptibilityElement != null) {
                interruptibilityDiscount = Double.parseDouble(interruptibilityElement.getAttribute("discount"));
            } else interruptibilityDiscount = 0.0;
            Element priceWeightsElement = (Element) influenceFactorsElement.getElementsByTagName("priceWeights").item(0);
            if (priceWeightsElement != null) {
                expMeanPriceWeight = Double.parseDouble(priceWeightsElement.getAttribute("expMean"));
                maxValuePriceWeight = Double.parseDouble(priceWeightsElement.getAttribute("maxValue"));
                realizedPriceWeight = Double.parseDouble(priceWeightsElement.getAttribute("realized"));
            } else {
            	expMeanPriceWeight = null;
            	maxValuePriceWeight = null;
                realizedPriceWeight = null;
            }
        } else throw new Error("Tariff subscriber influence factors element must be included, even if empty.");

        Element allocationElement = (Element) xml.getElementsByTagName("allocation").item(0);
        allocationMethod = Enum.valueOf(AllocationMethod.class, allocationElement.getAttribute("method"));
        if (allocationMethod == AllocationMethod.TOTAL_ORDER) {
            Element totalOrderElement = (Element) allocationElement.getElementsByTagName("totalOrder").item(0);
            populateTotalOrderRules(totalOrderElement.getAttribute("rules"));
            logitChoiceRationality = 1.0;
        } else {
            Element logitChoiceElement = (Element) allocationElement.getElementsByTagName("logitChoice").item(0);
            logitChoiceRationality = Double.parseDouble(logitChoiceElement.getAttribute("rationality"));
        }
        
        Element reconsiderationElement = (Element) xml.getElementsByTagName("reconsideration").item(0);
        reconsiderationPeriod = Integer.parseInt(reconsiderationElement.getAttribute("period"));
        Element inertiaElement = (Element) xml.getElementsByTagName("switchingInertia").item(0);
        Node inertiaDistributionNode = inertiaElement.getElementsByTagName("inertiaDistribution").item(0);
        if (inertiaDistributionNode != null) {  
            Element inertiaDistributionElement = (Element) inertiaDistributionNode;
            inertiaDistribution = new ProbabilityDistribution(inertiaDistributionElement);
            
            customerWealthDistribution = null;
            customerWealthReferenceMedian = 0.0;
            newTariffsExposure = null;
            switchingDelay = null;
            waitAfterSwitch = null;
        } else {
            inertiaDistribution = null;
            
            Node inertiaFactorsNode = inertiaElement.getElementsByTagName("inertiaFactors").item(0);
            if (inertiaFactorsNode == null) {
                throw new Error("TariffSubscriberStructure(): Inertia distribution and factors are both undefined!");
            }
            Element inertiaFactorsElement = (Element) inertiaFactorsNode;
            Element customerWealthElement = (Element) inertiaFactorsElement.getElementsByTagName("customerWealth").item(0);
            customerWealthDistribution = new ProbabilityDistribution(customerWealthElement);
            customerWealthReferenceMedian = Double.parseDouble(customerWealthElement.getAttribute("referenceMedian"));          
            Element newTariffsExposureElement = (Element) inertiaFactorsElement.getElementsByTagName("newTariffsExposure").item(0);
            newTariffsExposure = new ProbabilityDistribution(newTariffsExposureElement);
            Element switchingDelayElement = (Element) inertiaFactorsElement.getElementsByTagName("switchingDelay").item(0);
            switchingDelay = new ProbabilityDistribution(switchingDelayElement);
            Element waitAfterSwitchElement = (Element) inertiaFactorsElement.getElementsByTagName("waitAfterSwitch").item(0);
            waitAfterSwitch = new ProbabilityDistribution(waitAfterSwitchElement);
        }
    }
 
    private void populateTotalOrderRules(String config) 
    {
        // example config: "0.7:0.3, 0.5:0.3:0.2, 0.4:0.3:0.2:0.1, 0.4:0.3:0.2:0.05:0.05"
        // which yields the following rules:
        //              size = 2, rule = [0.7, 0.3]
        //              size = 3, rule = [0.5, 0.3, 0.2]
        //              size = 4, rule = [0.4, 0.3, 0.2, 0.1]
        //              size = 5, rule = [0.4, 0.3, 0.2, 0.05, 0.05]

        String[] rules = config.split(",");
        
        List<Double> degenerateRule = new ArrayList<Double>(1);
        degenerateRule.add(1.0);
        totalOrderRules.add(degenerateRule);

        for (int i=0; i < rules.length; ++i) {  
            if (rules[i].length() > 0) {
                String[] vals = rules[i].split(":");
                List<Double> rule = new ArrayList<Double>(vals.length);
                for (int j=0; j < vals.length; ++j) {
                    rule.add(Double.parseDouble(vals[j]));
                }
                totalOrderRules.add(rule);
            }
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
