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
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements minimal functionality for a factored customer which includes 
 * adding the customer to the global customer repo.
 * 
 * @author Prashant Reddy
 */
abstract class FactoredCustomer 
{
    @Autowired
    protected CustomerRepo customerRepo;

    protected final CustomerProfile customerProfile;
    
    FactoredCustomer(CustomerProfile profile) 
    {
        customerProfile = profile;
        
        customerRepo = (CustomerRepo) SpringApplicationContext.getBean("customerRepo");
        
        customerRepo.add(profile.customerInfo);
    }
    
    /** Tariff publication callback **/
    abstract void handleNewTariffs(List<Tariff> newTariffs);
    
    /** Timeslot activation callback **/
    abstract void handleNewTimeslot();
    
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


