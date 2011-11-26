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

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import org.powertac.common.enumerations.*;
import org.powertac.common.RandomSeed;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.CustomerInfo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Prashant Reddy
 */
class CustomerProfile
{
    @Autowired
    RandomSeedRepo randomSeedRepo;

    enum EntityType { RESIDENTIAL, COMMERCIAL, INDUSTRIAL }
    enum CustomerRole { CONSUMPTION, PRODUCTION, COMBINATION }
    enum ModelType { INDIVIDUAL, POPULATION }
	
    enum TariffUtilityCriteria { BEST_VALUE, PREFER_GREEN }
	
    private static long profileCounter = 0;
    
    long profileId = ++profileCounter;
    Random random;
    String name;
    CustomerCategory category;
    CustomerInfo customerInfo;
	
    // Customer behaviors
    TariffUtilityCriteria tariffUtilityCriteria = null;
    List<List<Double>> tariffAllocationRules = new ArrayList<List<Double>>();
    ProbabilityDistribution tariffSwitchingInertia = null;
	
    // Customer factors
    double[] temperatureByMonth = null;
    ProbabilityDistribution customerWealth = null;
    ProbabilityDistribution newTariffsExposure = null;
    ProbabilityDistribution tariffSwitchDelay = null;
    ProbabilityDistribution waitAfterTariffSwitch = null;
    boolean preferFewerTariffs = false;
	
    List<CapacityProfile> capacityProfiles = new ArrayList<CapacityProfile>();
		
    CustomerProfile(Element xml)
    {
        randomSeedRepo = (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
        
        RandomSeed rs = randomSeedRepo.getRandomSeed(CustomerProfile.class.getName(), profileId, "Initializer");
        random = new Random(rs.getValue());
		
        name = xml.getAttribute("name");
		
        Element categoryElement = (Element) xml.getElementsByTagName("category").item(0);
        EntityType entityType = Enum.valueOf(EntityType.class, categoryElement.getAttribute("entityType"));
        CustomerRole customerRole = Enum.valueOf(CustomerRole.class, categoryElement.getAttribute("customerRole"));
        ModelType modelType = Enum.valueOf(ModelType.class, categoryElement.getAttribute("modelType"));
        category = new CustomerCategory(entityType, customerRole, modelType);
        		
	int population = Integer.parseInt(((Element) xml.getElementsByTagName("population").item(0)).getAttribute("value"));
        customerInfo = new CustomerInfo(name, population);
	if (category.entityType == EntityType.RESIDENTIAL) {
	    customerInfo.withCustomerType(CustomerType.CustomerHousehold);
	} 
	else if (category.entityType == EntityType.COMMERCIAL) {
	    customerInfo.withCustomerType(CustomerType.CustomerOffice);
	} 
	else if (category.entityType == EntityType.INDUSTRIAL) {
	    customerInfo.withCustomerType(CustomerType.CustomerFactory);
	} 
	else {  // EntityType.AGRICULTURAL
	    customerInfo.withCustomerType(CustomerType.CustomerOther);
	}
	Element multiContractingElement = (Element) xml.getElementsByTagName("multiContracting").item(0);
	customerInfo.withMultiContracting(Boolean.parseBoolean(multiContractingElement.getAttribute("value")));
	
        Element canNegotiateElement = (Element) xml.getElementsByTagName("canNegotiate").item(0);
        customerInfo.withCanNegotiate(Boolean.parseBoolean(canNegotiateElement.getAttribute("value")));
		
        boolean customerBehaviorsInitialized = false;
        boolean customerFactorsInitialized = false;
        
        NodeList capacities = xml.getElementsByTagName("capacity");
        for (int i=0; i < capacities.getLength(); ++i) {
            Element capacitySpec = (Element) capacities.item(i); 
            CapacityProfile.SpecType specType = CapacityProfile.reportSpecType(capacitySpec);
            CapacityProfile capacityProfile;
            if (specType == CapacityProfile.SpecType.BEHAVIORS) {
                if (! customerBehaviorsInitialized) {
                    Element tariffUtilityElement = (Element) xml.getElementsByTagName("tariffUtility").item(0);
                    tariffUtilityCriteria = Enum.valueOf(TariffUtilityCriteria.class, tariffUtilityElement.getAttribute("criteria"));
                    String tariffAllocationRulesConfig = tariffUtilityElement.getAttribute("allocationRules");  
                    populateTariffAllocationRules(tariffAllocationRulesConfig);
                    
                    Element tariffSwitchingInertiaElement = (Element) xml.getElementsByTagName("tariffSwitchingInertia").item(0);
                    tariffSwitchingInertia = new ProbabilityDistribution(tariffSwitchingInertiaElement, random.nextLong());
                    
                    customerBehaviorsInitialized = true;
                }
                capacityProfile = new BehaviorsProfile(capacitySpec, random);
            } 
            else { // CapacityProfile.SpecType.FACTORED
                if (! customerFactorsInitialized) {
                    Element temperatureByMonthElement = (Element) xml.getElementsByTagName("temperatureByMonth").item(0); 
                    String temperatureByMonthString = temperatureByMonthElement.getAttribute("array");
                    temperatureByMonth = parseDoubleArray(temperatureByMonthString);
                    
                    Element customerWealthElement = (Element) xml.getElementsByTagName("customerWealth").item(0);
                    customerWealth = new ProbabilityDistribution(customerWealthElement, random.nextLong());
                    
                    Element newTariffsExposureElement = (Element) xml.getElementsByTagName("newTariffsExposure").item(0);
                    newTariffsExposure = new ProbabilityDistribution(newTariffsExposureElement, random.nextLong());
                    
                    Element tariffSwitchDelayElement = (Element) xml.getElementsByTagName("tariffSwitchDelay").item(0);
                    tariffSwitchDelay = new ProbabilityDistribution(tariffSwitchDelayElement, random.nextLong());
                    
                    Element waitAfterTariffSwitchElement = (Element) xml.getElementsByTagName("waitAfterTariffSwitch").item(0);
                    waitAfterTariffSwitch = new ProbabilityDistribution(waitAfterTariffSwitchElement, random.nextLong());
                    
                    Element preferFewerTariffsElement = (Element) xml.getElementsByTagName("preferFewerTariffs").item(0);
                    preferFewerTariffs = Boolean.parseBoolean(preferFewerTariffsElement.getAttribute("value"));
                    
                    customerFactorsInitialized = true;
                }
                capacityProfile = new FactoredProfile(capacitySpec, random);
            }
            capacityProfiles.add(capacityProfile);
            PowerType powerType = capacityProfile.determinePowerType();
            if (! customerInfo.getPowerTypes().contains(powerType)) {
		customerInfo.addPowerType(powerType);
            }
        }
    }
	
    void populateTariffAllocationRules(String config) 
    {
        // example config: "0.7:0.3, 0.5:0.3:0.2, 0.4:0.3:0.2:0.1, 0.4:0.3:0.2:0.05:0.05"
        // which yields the following rules:
        // 		size = 2, rule = [0.7, 0.3]
        // 		size = 3, rule = [0.5, 0.3, 0.2]
        // 		size = 4, rule = [0.4, 0.3, 0.2, 0.1]
        // 		size = 5, rule = [0.4, 0.3, 0.2, 0.05, 0.05]

        String[] rules = config.split(",");
	
	List<Double> degenerateRule = new ArrayList<Double>(1);
	degenerateRule.add(1.0);
	tariffAllocationRules.add(degenerateRule);

	for (int i=0; i < rules.length; ++i) {	
	    if (rules[i].length() > 0) {
	        String[] vals = rules[i].split(":");
	        List<Double> rule = new ArrayList<Double>(vals.length);
	        for (int j=0; j < vals.length; ++j) {
	            rule.add(Double.parseDouble(vals[j]));
	        }
	        tariffAllocationRules.add(rule);
	    }
	}
    }	

    protected double[] parseDoubleArray(String input) {
        String[] items = input.split(",");
        double[] ret = new double[items.length];
        for (int i=0; i < items.length; ++i) {
            ret[i] = Double.parseDouble(items[i]);
        }
        return ret;
    }

} // end class

