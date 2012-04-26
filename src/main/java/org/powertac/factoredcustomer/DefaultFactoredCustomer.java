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
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.interfaces.*;
import org.powertac.factoredcustomer.CustomerFactory.CustomerCreator;

/**
 * Key class that encapsulates the behavior of one customer.  Much of the functionality 
 * is delegated to contained utility optimizers and capacity bundles, however.
 * 
 * @author Prashant Reddy
 */
@Domain
class DefaultFactoredCustomer implements FactoredCustomer 
{
    protected Logger log = Logger.getLogger(DefaultFactoredCustomer.class.getName());

    protected final TimeslotRepo timeslotRepo;
    protected final CustomerRepo customerRepo;

    protected CustomerStructure customerStructure;    
    protected UtilityOptimizer utilityOptimizer;        
    protected final List<CapacityBundle> capacityBundles = new ArrayList<CapacityBundle>();
    
    
    DefaultFactoredCustomer(CustomerStructure structure) 
    {        
        customerStructure = structure;
        
        timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
        customerRepo = (CustomerRepo) SpringApplicationContext.getBean("customerRepo");
    }
     
    @Override
    public void initialize(CustomerStructure structure)
    {
        log.info("Initializing customer " + customerStructure.name);
        NodeList capacityBundleNodes = customerStructure.getConfigXml().getElementsByTagName("capacityBundle");
        for (int i=0; i < capacityBundleNodes.getLength(); ++i) {
            Element capacityBundleElement = (Element) capacityBundleNodes.item(i);
            CapacityBundle capacityBundle = createCapacityBundle(structure, capacityBundleElement);
            capacityBundle.initialize(structure, capacityBundleElement);
            capacityBundles.add(capacityBundle);
            customerRepo.add(capacityBundle.getCustomerInfo());
        }
        utilityOptimizer = createUtilityOptimizer(structure, capacityBundles);                
        utilityOptimizer.initialize();
	log.info("Successfully initialized customer " + customerStructure.name);
    }

    /** @Override hook **/
    protected CapacityBundle createCapacityBundle(CustomerStructure structure, Element capacityBundleElement)
    {
        return new DefaultCapacityBundle(structure, capacityBundleElement);
    }
    
    /** @Override hook **/
    protected UtilityOptimizer createUtilityOptimizer(CustomerStructure structure, 
                                                      List<CapacityBundle> capacityBundles)
    {
        return new DefaultUtilityOptimizer(structure, capacityBundles);        
    }
    
    @Override 
    public void handleNewTariffs(List<Tariff> newTariffs)
    {
        Timeslot timeslot =  timeslotRepo.currentTimeslot();
        log.info("Customer " + getName() + " received " + newTariffs.size() + " new tariffs at timeslot " + timeslot.getSerialNumber());
        utilityOptimizer.handleNewTariffs(newTariffs);
    }
	    
    @Override 
    public void handleNewTimeslot()
    {
        Timeslot timeslot =  timeslotRepo.currentTimeslot();
        log.info("Customer " + getName() + " activated for timeslot " + timeslot.getSerialNumber());   
        utilityOptimizer.handleNewTimeslot(timeslot);
    }
	
    String getName() 
    {
        return customerStructure.name;
    }
    
    CustomerStructure getCustomerStructure()
    {
        return customerStructure;
    }

    @Override
    public String toString() 
    {
	return this.getClass().getCanonicalName() + ":" + getName();
    }
    
    // STATIC INNER CLASS
    
    public static class Creator implements CustomerCreator
    {
        public String getKey() 
        {
            return null;  // registered as default creator
        }
        
        @Override
        public FactoredCustomer createModel(CustomerStructure structure)
        {
            return new DefaultFactoredCustomer(structure);
        }
    }   
    private static Creator creator = new Creator();
    public static CustomerCreator getCreator() { return creator; }
    
} // end class


