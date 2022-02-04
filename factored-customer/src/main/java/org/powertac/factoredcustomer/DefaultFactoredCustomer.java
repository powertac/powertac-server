/*
* Copyright 2011, 2016, 2022 by Prashant Reddy and John Collins
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.TimeslotRepo;
//import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.CustomerFactory.CustomerCreator;
import org.powertac.factoredcustomer.interfaces.CapacityBundle;
import org.powertac.factoredcustomer.interfaces.FactoredCustomer;
import org.powertac.factoredcustomer.interfaces.StructureInstance;
import org.powertac.factoredcustomer.interfaces.UtilityOptimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Key class that encapsulates the behavior of one customer.
 * Much of the functionality is delegated to contained utility optimizers and
 * capacity bundles, however.
 *
 * @author Prashant Reddy
 */
class DefaultFactoredCustomer implements FactoredCustomer
{
  private static Logger log = LogManager.getLogger(DefaultFactoredCustomer.class);

  private CustomerStructure customerStructure;
  private UtilityOptimizer utilityOptimizer;
  private final List<CapacityBundle> capacityBundles = new ArrayList<>();

  protected FactoredCustomerService service;

  public DefaultFactoredCustomer (CustomerStructure customerStructure)
  {
    super();
    this.customerStructure = customerStructure;
  }

  @Override
  public void initialize (FactoredCustomerService service)
  {
    this.service = service;
    Config config = Config.getInstance();
    Map<String, StructureInstance> bundles =
        config.getStructures().get("DefaultCapacityBundle");

    log.info("Initializing customer " + customerStructure.getName());

    for (int i = 0; i < customerStructure.getBundleCount(); ++i) {
      String name = customerStructure.getName();
      if (customerStructure.getBundleCount() > 1) {
        name += "-" + (i + 1);
      }
      createCapacityBundle (bundles, customerStructure, name);
    }
    utilityOptimizer = createUtilityOptimizer(customerStructure, capacityBundles);
    utilityOptimizer.initialize(service);
    log.info("Successfully initialized customer " + customerStructure.getName());
  }

  private void createCapacityBundle (Map<String, StructureInstance> bundles,
                                     CustomerStructure customerStructure,
                                     String name)
  {
    CapacityBundle capacityBundle = (CapacityBundle) bundles.get(name);
    if (capacityBundle == null) {
      throw new Error("No CapacityBundle for " + name);
    }
    capacityBundle.initialize(service, customerStructure);
    capacityBundles.add(capacityBundle);
    getCustomerRepo().add(capacityBundle.getCustomerInfo());
  }

  // Component accessors
  private CustomerRepo getCustomerRepo ()
  {
    return service.getCustomerRepo();
  }

  private TimeslotRepo getTimeslotRepo ()
  {
    return service.getTimeslotRepo();
  }

  /**
   * @Override hook
   **/
  protected UtilityOptimizer createUtilityOptimizer (
      CustomerStructure customerStructure, List<CapacityBundle> capacityBundles)
  {
    return new DefaultUtilityOptimizer(customerStructure, capacityBundles);
  }

  @Override
  public void evaluateTariffs ()
  {
    Timeslot timeslot = getTimeslotRepo().currentTimeslot();
    log.info("Customer " + getName() + " evaluating tariffs at timeslot "
        + timeslot.getSerialNumber());
    utilityOptimizer.evaluateTariffs();
  }

  @Override
  public void updatedSubscriptionRepo ()
  {
    utilityOptimizer.updatedSubscriptionRepo();
  }

  @Override
  public void handleNewTimeslot ()
  {
    Timeslot timeslot = getTimeslotRepo().currentTimeslot();
    log.info("Customer " + getName() + " activated for timeslot "
        + timeslot.getSerialNumber());
    utilityOptimizer.step();
  }

  String getName ()
  {
    return customerStructure.getName();
  }

  @Override
  public String toString ()
  {
    return this.getClass().getCanonicalName() + ":" + getName();
  }

  public static class Creator implements CustomerCreator
  {
    @Override
    public String getKey ()
    {
      return null;  // registered as default creator
    }

    @Override
    public FactoredCustomer createModel (CustomerStructure customerStructure)
    {
      return new DefaultFactoredCustomer(customerStructure);
    }
  }

  private static Creator creator = new Creator();

  public static CustomerCreator getCreator ()
  {
    return creator;
  }

  // Test support, package visibility
  CustomerStructure getCustomerStructure ()
  {
    return customerStructure;
  }

  List<CapacityBundle> getCapacityBundles()
  {
    return capacityBundles;
  }

  UtilityOptimizer getUtilityOptimizer ()
  {
    return utilityOptimizer;
  }
}
