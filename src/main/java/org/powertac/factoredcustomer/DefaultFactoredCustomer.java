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
import org.apache.log4j.Logger;
import org.powertac.common.Tariff;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.Domain;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Key class that encapsulates the behavior of one customer.  Much of the functionality 
 * is delegated to contained utility managers and capapcity bundles, however.
 * 
 * @author Prashant Reddy
 */
@Domain
class DefaultFactoredCustomer extends FactoredCustomer 
{
    private static Logger log = Logger.getLogger(DefaultFactoredCustomer.class.getName());

    @Autowired
    private TimeslotRepo timeslotRepo;
    
    public static class Creator implements CustomerFactory.CustomerCreator
    {
        public String getKey() 
        {
            return null;  // registered as default creator
        }
        
        public FactoredCustomer createModel(CustomerProfile profile)
        {
            return new DefaultFactoredCustomer(profile);
        }
    }
    private static Creator creator = new Creator();
    public static Creator getCreator() {
        return creator;
    }
    
    private final UtilityManager utilityManager;        
    private final List<CapacityBundle> capacityBundles = new ArrayList<CapacityBundle>();
    
    
    DefaultFactoredCustomer(CustomerProfile profile) 
    {
        super(profile);
        
        timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");

        NodeList capacityBundleNodes = customerProfile.getConfigXml().getElementsByTagName("capacityBundle");
        for (int i=0; i < capacityBundleNodes.getLength(); ++i) {
            Element capacityBundleElement = (Element) capacityBundleNodes.item(i);
            capacityBundles.add(new CapacityBundle(profile, capacityBundleElement));
        }
        
        utilityManager = new DefaultUtilityManager(profile, capacityBundles);
        
	log.info("Customer created for profile: " + customerProfile.name);
    }
  
    /** @Override @code{FactoredCustomer} **/
    void handleNewTariffs (List<Tariff> newTariffs)
    {
        log.info("handleNewTariffs() begin - " + getName() + ": Received " + newTariffs.size() + " new tariffs.");
        utilityManager.handleNewTariffs(newTariffs);
        log.info("handleNewTariffs() end - " + getName());
    }
	    
    /** @Override @code{FactoredCustomer} **/
    public void handleNewTimeslot()
    {
        Timeslot timeslot =  timeslotRepo.currentTimeslot();
        log.info("Customer " + customerProfile.name + " begin step for timeslot " + timeslot.getSerialNumber());   
        utilityManager.handleNewTimeslot(timeslot);
        log.info("Customer " + customerProfile.name + " end step for timeslot " + timeslot.getSerialNumber());    
    }
	
    public String toString() 
    {
	return "DefaultFactoredCustomer:" + getName();
    }
    
} // end class


