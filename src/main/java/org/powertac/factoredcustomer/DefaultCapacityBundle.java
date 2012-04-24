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

import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.*;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.factoredcustomer.interfaces.*;
import org.powertac.common.state.Domain;

/**
 * A simple collection of capacity originators, all with the same base capacity type; 
 * i.e., CONSUMPTION or PRODUCTION.
 * 
 * @author Prashant Reddy
 */
@Domain
class DefaultCapacityBundle implements CapacityBundle
{
    private final CustomerStructure customerStructure;
    
    private final String name;
    private final CustomerInfo customerInfo;
    
    private final TariffSubscriberStructure subscriberStructure;
    private final ProfileOptimizerStructure optimizerStructure;
    
    protected final List<CapacityOriginator> capacityOriginators = new ArrayList<CapacityOriginator>();
    
    
    DefaultCapacityBundle(CustomerStructure structure, Element xml)
    {
        customerStructure = structure;

        String bundleId = xml.getAttribute("id");
        name = (bundleId == null || bundleId.isEmpty()) ? 
                customerStructure.name : customerStructure.name + "@" + bundleId;
        
        customerInfo = new CustomerInfo(name, Integer.parseInt(xml.getAttribute("population")))
            .withPowerType(PowerType.valueOf(xml.getAttribute("powerType")))
            .withMultiContracting(Boolean.parseBoolean(xml.getAttribute("multiContracting")))
            .withCanNegotiate(Boolean.parseBoolean(xml.getAttribute("canNegotiate")));

        Element tariffSubscriberElement = (Element) xml.getElementsByTagName("tariffSubscriber").item(0);
        subscriberStructure = new TariffSubscriberStructure(structure, this, tariffSubscriberElement);        
        
        Element profileOptimizerElement = (Element) xml.getElementsByTagName("profileOptimizer").item(0);
        optimizerStructure = new ProfileOptimizerStructure(structure, this, profileOptimizerElement);        
    }
    
    @Override
    public void initialize(CustomerStructure structure, Element xml)
    {
        NodeList capacityNodes = xml.getElementsByTagName("capacity");
        for (int i=0; i < capacityNodes.getLength(); ++i) {
            Element capacityElement = (Element) capacityNodes.item(i);
            String name = capacityElement.getAttribute("name");
            String countString = capacityElement.getAttribute("count");
            if (countString == null || Integer.parseInt(countString) == 1) {
                CapacityStructure capacityStructure = new CapacityStructure(name, capacityElement, this);
                capacityOriginators.add(createCapacityOriginator(capacityStructure));            
            } else {
                if (name == null) name = "";
                for (int j=1; j < (1 + Integer.parseInt(countString)); ++j) {
                    CapacityStructure capacityStructure = new CapacityStructure(name + j, capacityElement, this);
                    capacityOriginators.add(createCapacityOriginator(capacityStructure));                            
                }
            }
        }
    }

    /** @Override hook **/
    protected CapacityOriginator createCapacityOriginator(CapacityStructure capacityStructure)
    {
        return new DefaultCapacityOriginator(capacityStructure, this);            
    }

    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public int getPopulation()
    {
        return customerInfo.getPopulation();
    }
    
    @Override 
    public PowerType getPowerType()
    {
        return customerInfo.getPowerType();
    }

    @Override
    public CustomerInfo getCustomerInfo()
    {
        return customerInfo;
    }
    
    @Override
    public TariffSubscriberStructure getSubscriberStructure()
    {
        return subscriberStructure;
    }
    
    @Override
    public ProfileOptimizerStructure getOptimizerStructure()
    {
        return optimizerStructure;
    }
    
    @Override
    public List<CapacityOriginator> getCapacityOriginators() 
    {
        return capacityOriginators;
    }

    @Override
    public String toString()
    {
        return this.getClass().getCanonicalName() + ":" + customerStructure.name + ":" + customerInfo.getPowerType();
    }

} // end class
