/*
 * Copyright 2011-13 the original author or authors.
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

package org.powertac.factoredcustomer.interfaces;

import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.factoredcustomer.CustomerStructure;
import org.powertac.factoredcustomer.FactoredCustomerService;
import org.powertac.factoredcustomer.ProfileOptimizerStructure;
import org.powertac.factoredcustomer.TariffSubscriberStructure;

import java.util.List;


/**
 * @author Prashant Reddy
 */
public interface CapacityBundle
{
  void initialize (FactoredCustomerService service,
                   CustomerStructure customerStructure);

  String getName ();
  
  int getCount();

  int getPopulation ();

  PowerType getPowerType ();

  CustomerInfo getCustomerInfo ();

  TariffSubscriberStructure getSubscriberStructure ();

  ProfileOptimizerStructure getOptimizerStructure ();

  List<CapacityOriginator> getCapacityOriginators ();

  /**
   * True just in case all CapacityOriginators in this bundle are INDIVIDUAL
   */
  boolean isAllIndividual();
}
