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
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.factoredcustomer.interfaces.*;

/**
 * Register CustomerCategory-specific creators.  Creators implement the nested 
 * public interface @code{CustomerCreator}.
 * 
 * @author Prashant Reddy
 */
@Domain
final class CustomerFactory
{
    public interface CustomerCreator {
        public String getKey();
        public FactoredCustomer createModel(CustomerStructure structure);
    }
    
    CustomerCreator defaultCreator;
    Map<String, CustomerCreator> customerCreators = new HashMap<String, CustomerCreator>();
    
    @StateChange
    void registerDefaultCreator(CustomerCreator creator) 
    {
        defaultCreator = creator;
    }
	
    @StateChange
    void registerCreator(CustomerCreator creator)
    {
        registerCreator(creator.getKey(), creator);
    }
        
    @StateChange
    void registerCreator(String key, CustomerCreator creator) 
    {
        customerCreators.put(key, creator);
    }

    FactoredCustomer processStructure(CustomerStructure structure) 
    {
        CustomerCreator creator;
        if (structure.creatorKey == null || structure.creatorKey.trim().isEmpty()) {
            creator = defaultCreator;
        } else {
            creator = customerCreators.get(structure.creatorKey);
            if (creator == null) {
                throw new Error("CustomerFactory does not have a registered creator for key: " + structure.creatorKey);
            }
        }
        return creator.createModel(structure);            
    }

} // end class

