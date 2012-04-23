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

import org.w3c.dom.*;
import org.apache.log4j.Logger;
import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.interfaces.CapacityBundle;
import org.powertac.factoredcustomer.interfaces.UtilityOptimizer;

/**
 * Extends @code{DefaultFactoredCustomer} to create @code{LearningUtilityOptimizer}
 * and @code{AdaptiveCapacityBundle} instances.
 * 
 * @author Prashant Reddy
 */
@Domain
class LearningFactoredCustomer extends DefaultFactoredCustomer
{
    LearningFactoredCustomer(CustomerStructure structure) 
    {        
        super(structure);
        log = Logger.getLogger(LearningFactoredCustomer.class.getName());
    }

    @Override
    protected CapacityBundle createCapacityBundle(CustomerStructure structure, Element capacityBundleElement)
    {
        return new AdaptiveCapacityBundle(structure, capacityBundleElement);
    }
    
    @Override
    protected UtilityOptimizer createUtilityOptimizer(CustomerStructure structure, 
                                                      List<CapacityBundle> capacityBundles)
    {
        return new LearningUtilityOptimizer(structure, capacityBundles);        
    }


} // end class


