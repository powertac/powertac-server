/*
 * Copyright 2009-2010 the original author or authors. Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.common.interfaces;

import java.util.List;
import java.util.Map;

import org.powertac.common.CustomerInfo;

/**
 * Interface that specifies common methods a Customer module needs to implement.
 * 
 * @author Carsten Block
 * @version 0.1 - January 2nd, 2011
 */
public interface Customer {

  /**
   * Called to make the customer model produce its "real consumption" / real
   * production based on the given subscription (which includes current and
   * future prices), and on "real weather data" (which might only be relevant to
   * customer models that react to weather impact, such as PV or wind turbine
   * customers.
   * 
   * @param weather
   *          real measured weather data for a particular timeslot
   * @return real consumption / production of the customer for the timeslot
   *         specified in the given {@link Weather}
   */
  // public TariffTransaction generateMeterReading(TariffSubscription
  // subscription, Weather weather);

  /**
   * As soon as this method is called the customer model is required to store /
   * update its own {@link org.powertac.common.Customer} instance in the
   * database, which is used to publicly report some common properties
   * describing the customer model
   * 
   * @see org.powertac.common.Customer
   * @return a list of the Customers contains customer master data (i.e. a
   *         generic description of the customer)
   */
  public List<CustomerInfo> generateCustomerInfoList();

  /**
   * This method is called in order for the customer models to broadcast the
   * bootstrap data of the 14 pilot days of the customer's consumption
   * 
   * @return Map that contains the bootstrap data
   */
  public Map generateBootstrapDataMap();
}
