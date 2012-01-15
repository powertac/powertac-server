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
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.CustomerInfo;
import org.powertac.factoredcustomer.CapacityProfile.CapacitySubType;
import org.powertac.factoredcustomer.CapacityProfile.CapacityType;

/**
 * Data-holder class for parsed configuration elements of one customer.
 * All members are declared final in the package scope.
 * 
 * @author Prashant Reddy
 */
final class CustomerProfile
{
    enum EntityType { RESIDENTIAL, COMMERCIAL, INDUSTRIAL };
    	
    private final Element configXml;
    
    private static long profileCounter = 0;
    
    final long profileId = ++profileCounter;
    final String name;
    final String creatorKey;
    final EntityType entityType;
    final CustomerInfo customerInfo;
    
    CustomerProfile(Element xml)
    {
        configXml = xml;
        
        name = xml.getAttribute("name");
        creatorKey = xml.getAttribute("creatorKey");
        	
        Element infoElement = (Element) xml.getElementsByTagName("info").item(0);
	int population = Integer.parseInt(infoElement.getAttribute("population"));
        customerInfo = new CustomerInfo(name, population)
            .withMultiContracting(Boolean.parseBoolean(infoElement.getAttribute("multiContracting")))
            .withCanNegotiate(Boolean.parseBoolean(infoElement.getAttribute("canNegotiate")));
        entityType = Enum.valueOf(EntityType.class, infoElement.getAttribute("entityType"));
        
        NodeList capacityBundles = xml.getElementsByTagName("capacityBundle");
        for (int i=0; i < capacityBundles.getLength(); ++i) {
            Element capacityBundle = (Element) capacityBundles.item(i);
            CapacityType capacityType = Enum.valueOf(CapacityType.class, capacityBundle.getAttribute("type"));
            CapacitySubType capacitySubType = Enum.valueOf(CapacitySubType.class, capacityBundle.getAttribute("subType"));                
            PowerType powerType = CapacityProfile.reportPowerType(capacityType, capacitySubType);
            if (! customerInfo.getPowerTypes().contains(powerType)) {
                customerInfo.addPowerType(powerType);
            }
        }
    }

    public Element getConfigXml()
    {
        return configXml;
    }
        
} // end class

