/*
* Copyright 2011-2016 the original author or authors.
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

//import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.interfaces.CapacityBundle;
import org.powertac.factoredcustomer.interfaces.UtilityOptimizer;

import java.util.List;


/**
 * Extends @code{DefaultFactoredCustomer} to create
 * @code{LearningUtilityOptimizer} instances.
 *
 * @author Prashant Reddy
 */
//@Domain
class LearningFactoredCustomer extends DefaultFactoredCustomer
{
  public LearningFactoredCustomer (CustomerStructure customerStructure)
  {
    super(customerStructure);
  }

  @Override
  protected UtilityOptimizer createUtilityOptimizer (
      CustomerStructure customerStructure, List<CapacityBundle> capacityBundles)
  {
    return new LearningUtilityOptimizer(customerStructure, capacityBundles);
  }
}


