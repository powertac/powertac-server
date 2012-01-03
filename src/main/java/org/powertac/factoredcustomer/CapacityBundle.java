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
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.factoredcustomer.CapacityProfile.CapacitySubType;
import org.powertac.factoredcustomer.CapacityProfile.CapacityType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A collection of capacities, all with the same base capacity type; 
 * i.e., CONSUMPTION or PRODUCTION.
 * 
 * @author Prashant Reddy
 */
final class CapacityBundle
{
    @Autowired
    private TimeslotRepo timeslotRepo;
    
    private static final int NUM_HOURS_IN_DAY = 24;
    
    private final Element configXml;
    
    private final CapacityType capacityType;
    private final CapacitySubType capacitySubType;
    
    private final TariffSubscriberProfile subscriberProfile;
    
    private final List<CapacityManager> capacityManagers = new ArrayList<CapacityManager>();
    
    
    CapacityBundle(CustomerProfile profile, Element xml)
    {
        configXml = xml;
        
        timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");

        capacityType = Enum.valueOf(CapacityType.class, xml.getAttribute("type"));
        capacitySubType = Enum.valueOf(CapacitySubType.class, xml.getAttribute("subType"));
        
        Element tariffSubscriberElement = (Element) xml.getElementsByTagName("tariffSubscriber").item(0);
        subscriberProfile = new TariffSubscriberProfile(profile, this, tariffSubscriberElement);
            
        NodeList capacityNodes = xml.getElementsByTagName("capacity");
        for (int i=0; i < capacityNodes.getLength(); ++i) {
            Element capacityElement = (Element) capacityNodes.item(i);
            capacityManagers.add(new CapacityManager(profile, this, capacityElement));
        }
    }

    double computeDailyUsageCharge(Tariff tariff)
    {
        Timeslot hourlyTimeslot = timeslotRepo.currentTimeslot();
                
        double totalUsage = 0.0;
        double totalCharge = 0.0;
        for (int i=0; i < NUM_HOURS_IN_DAY; ++i) {
            double hourlyUsage = 0;
            for (CapacityManager capacityManager: capacityManagers) {
                hourlyUsage += capacityManager.getBaseCapacity(hourlyTimeslot);
            }
            totalCharge += tariff.getUsageCharge(hourlyTimeslot.getStartInstant(), hourlyUsage, totalUsage);
            totalUsage += hourlyUsage;
            hourlyTimeslot = hourlyTimeslot.getNext();
        }
        return totalCharge;
    }

    double useCapacity(Timeslot timeslot, TariffSubscription subscription)
    {
        double capacity = 0;
        for (CapacityManager capacityManager: capacityManagers) {
            capacity += capacityManager.useCapacity(timeslot, subscription);
        }
        return capacity;
    }
    
    CapacityType getCapacityType()
    {
        return capacityType;
    }
    
    CapacitySubType getCapacitySubType()
    {
        return capacitySubType;
    }
    
    List<CapacityManager> getCapacityManagers() 
    {
        return capacityManagers;
    }

    public Element getConfigXml()
    {
        return configXml;
    }

    public TariffSubscriberProfile getSubscriberProfile()
    {
        return subscriberProfile;
    }
    
} // end class
