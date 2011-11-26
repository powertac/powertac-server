/* Copyright 2011 the original author or authors.
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
import java.util.Random;

/**
 * @author Prashant Reddy
 */
class BehaviorsProfile extends CapacityProfile
{
    ProbabilityDistribution baseTotalCapacity = null;
    ProbabilityDistribution baseCapacityPerCustomer = null;
    double[][] elasticityOfCapacity = null;
		
    BehaviorsProfile(Element xml, Random random) 
    {
        super(xml, random);

        Element baseTotalCapacityElement = (Element) xml.getElementsByTagName("baseTotalCapacity").item(0);
        if (baseTotalCapacityElement != null) {
            baseTotalCapacity = new ProbabilityDistribution(baseTotalCapacityElement, random.nextLong());
            // Note: Ignore base capacity per customer here
        } else {
            // Note: Leave baseTotalCapacity as null to indicate per customer simulation
            Element baseCapacityPerCustomerElement = (Element) xml.getElementsByTagName("baseCapacityPerCustomer").item(0);
            baseCapacityPerCustomer = new ProbabilityDistribution(baseCapacityPerCustomerElement, random.nextLong());
        }
        Element elasticityElement = (Element) xml.getElementsByTagName("elasticityOfCapacity").item(0);
        if (elasticityElement == null) throw new Error("Undefined element: elasticityOfCapacity");
        String elasticityFunction = elasticityElement.getAttribute("function");
	if (elasticityFunction != null) {
	    elasticityOfCapacity = pairsAsDoubleArray(elasticityFunction);
	}
    }
	
} // end class

