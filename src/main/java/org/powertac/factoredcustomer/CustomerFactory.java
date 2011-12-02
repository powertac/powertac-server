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

import java.util.Map;
import java.util.HashMap;

/**
 * @author Prashant Reddy
 * Register CustomerCategory-specific creators.  Creators can be classes or closures 
 * that implement a createModel() method.
 */
class CustomerFactory
{
    public interface CustomerCreator {
        public CustomerCategory getCategory();
        public FactoredCustomer createModel(CustomerProfile profile);
    }
    
    CustomerCreator defaultCreator;
    Map<CustomerCategory, CustomerCreator> customerCreators = new HashMap<CustomerCategory, CustomerCreator>();
    
    void registerDefaultCreator(CustomerCreator creator) 
    {
        defaultCreator = creator;
    }
	
    void registerCreator(CustomerCreator creator)
    {
        registerCreator(creator.getCategory(), creator);
    }
        
    void registerCreator(CustomerProfile.EntityType entityType, CustomerProfile.CustomerRole customerRole, 
                         CustomerProfile.ModelType modelType, CustomerCreator creator) 
    {
        registerCreator(new CustomerCategory(entityType, customerRole, modelType), creator);
    }

    void registerCreator(CustomerCategory category, CustomerCreator creator) 
    {
        customerCreators.put(category, creator);
    }

    FactoredCustomer processProfile(CustomerProfile profile) 
    {
	CustomerCreator creator = customerCreators.get(profile.category);
	if (creator == null) creator = defaultCreator;
	return creator.createModel(profile);
    }

} // end class

