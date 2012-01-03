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
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.CustomerFactory.Customer;
import org.powertac.factoredcustomer.CustomerFactory.CustomerCreator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Key class that encapsulates the behavior of one customer.  Much of the functionality 
 * is delegated to contained utility managers and capapcity bundles, however.
 * 
 * @author Prashant Reddy
 */
@Domain
class FactoredCustomer implements Customer 
{
    private static Logger log = Logger.getLogger(FactoredCustomer.class.getName());

    @Autowired
    private TimeslotRepo timeslotRepo;
    
    @Autowired
    protected CustomerRepo customerRepo;

    public static class Creator implements CustomerCreator
    {
        public String getKey() 
        {
            return null;  // registered as default creator
        }
        
        public Customer createModel(CustomerProfile profile)
        {
            return new FactoredCustomer(profile);
        }
    }   
    private static Creator creator = new Creator();
    public static Creator getCreator() { return creator; }
    
    private final CustomerProfile customerProfile;    
    private final UtilityOptimizer utilityOptimizer;        
    private final List<CapacityBundle> capacityBundles = new ArrayList<CapacityBundle>();
    
    
    FactoredCustomer(CustomerProfile profile) 
    {        
        customerProfile = profile;
        timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
        customerRepo = (CustomerRepo) SpringApplicationContext.getBean("customerRepo");
        customerRepo.add(profile.customerInfo);
        
        NodeList capacityBundleNodes = customerProfile.getConfigXml().getElementsByTagName("capacityBundle");
        for (int i=0; i < capacityBundleNodes.getLength(); ++i) {
            Element capacityBundleElement = (Element) capacityBundleNodes.item(i);
            capacityBundles.add(new CapacityBundle(profile, capacityBundleElement));
        }
        utilityOptimizer = new UtilityOptimizer(profile, capacityBundles);
	log.info("Customer created for profile: " + customerProfile.name);
    }
  
    /** @Override @code{CustomerFactory.Customer} **/
    public void handleNewTariffs (List<Tariff> newTariffs)
    {
        Timeslot timeslot =  timeslotRepo.currentTimeslot();
        log.info("Customer " + getName() + " received " + newTariffs.size() + " new tariffs at timeslot " + timeslot.getSerialNumber());
        utilityOptimizer.handleNewTariffs(newTariffs);
    }
	    
    /** @Override @code{CustomerFactory.Customer} **/
    public void handleNewTimeslot()
    {
        Timeslot timeslot =  timeslotRepo.currentTimeslot();
        log.info("Customer " + getName() + " activated for timeslot " + timeslot.getSerialNumber());   
        utilityOptimizer.handleNewTimeslot(timeslot);
    }
	
    
    String getName() 
    {
        return customerProfile.name;
    }
    
    CustomerProfile getCustomerProfile()
    {
        return customerProfile;
    }

    CustomerInfo getCustomerInfo()
    {
        return customerProfile.customerInfo;
    }

    int getPopulation() 
    {
        return getCustomerInfo().getPopulation();
    }
    
    public String toString() 
    {
	return "FactoredCustomer:" + getName();
    }
    
} // end class


