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

import org.powertac.common.CustomerInfo;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
//import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.interfaces.CapacityBundle;
import org.powertac.factoredcustomer.interfaces.CapacityOriginator;
import org.powertac.factoredcustomer.interfaces.StructureInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * A simple collection of capacity originators, all with the same base capacity
 * type;
 * i.e., CONSUMPTION or PRODUCTION.
 *
 * @author Prashant Reddy
 */
//@Domain
public class DefaultCapacityBundle implements CapacityBundle, StructureInstance
{
  protected FactoredCustomerService service;
  private CustomerStructure customerStructure;

  protected String name;
  @ConfigurableValue(valueType = "Integer")
  protected int count;
  @ConfigurableValue(valueType = "Integer")
  protected int population;
  @ConfigurableValue(valueType = "String")
  protected String type;
  @ConfigurableValue(valueType = "String")
  protected String customerSize = "SMALL";
  @ConfigurableValue(valueType = "Boolean")
  protected boolean multiContracting;
  @ConfigurableValue(valueType = "Boolean")
  protected boolean canNegotiate;
  

  @ConfigurableValue(valueType = "Boolean")
  protected boolean isAdaptive;

  private CustomerInfo customerInfo;

  private TariffSubscriberStructure subscriberStructure;
  private ProfileOptimizerStructure optimizerStructure;

  protected List<CapacityOriginator> capacityOriginators = new ArrayList<>();

  public DefaultCapacityBundle (String name)
  {
    this.name = name;
  }

  @Override
  public void initialize (FactoredCustomerService service,
                          CustomerStructure customerStructure)
  {
    this.service = service;
    this.customerStructure = customerStructure;

    customerInfo = new CustomerInfo(name, this.population)
        .withPowerType(PowerType.valueOf(this.type))
        .withCustomerClass(CustomerInfo.CustomerClass.valueOf(customerSize))
        .withMultiContracting(this.multiContracting)
        .withCanNegotiate(this.canNegotiate);

    Config config = Config.getInstance();
    Map<String, StructureInstance> subscribers =
        config.getStructures().get("TariffSubscriberStructure");
    Map<String, StructureInstance> optimizers =
        config.getStructures().get("ProfileOptimizerStructure");
    Map<String, StructureInstance> capacities =
        config.getStructures().get("CapacityStructure");

    subscriberStructure =
        (TariffSubscriberStructure) subscribers.get(name);
    if (subscriberStructure != null) {
      subscriberStructure.initialize(service);
    }
    else {
      throw new Error("No TariffSubscriberStructure for : " + name);
    }

    optimizerStructure =
        (ProfileOptimizerStructure) optimizers.get(name);
    if (optimizerStructure == null) {
      optimizerStructure = new ProfileOptimizerStructure(name);
    }

    if (this.count > 1) {
      for (int j = 0; j < this.count; j++) {
        CapacityStructure capacityStructure =
            (CapacityStructure) capacities.get(name + (j + 1));
        if (capacityStructure == null) {
          throw new Error("No CapacityStructure for " + name + (j + 1));
        }
        capacityStructure.initialize(service);
        capacityOriginators.add(createCapacityOriginator(capacityStructure));
      }
    }
    else {
      CapacityStructure capacityStructure =
          (CapacityStructure) capacities.get(name);
      if (capacityStructure == null) {
        throw new Error("No CapacityStructure for " + name);
      }
      capacityStructure.initialize(service);
      capacityOriginators.add(createCapacityOriginator(capacityStructure));
    }
  }

  protected CapacityOriginator createCapacityOriginator (
      CapacityStructure capacityStructure)
  {
    if (isAdaptive) {
      return new AdaptiveCapacityOriginator(service, capacityStructure, this);
    }
    else {
      return new DefaultCapacityOriginator(service, capacityStructure, this);
    }
  }

  @Override
  public String getName ()
  {
    return name;
  }

  @Override
  public int getPopulation ()
  {
    return customerInfo.getPopulation();
  }

  @Override
  public PowerType getPowerType ()
  {
    return customerInfo.getPowerType();
  }

  @Override
  public CustomerInfo getCustomerInfo ()
  {
    return customerInfo;
  }

  @Override
  public TariffSubscriberStructure getSubscriberStructure ()
  {
    return subscriberStructure;
  }

  @Override
  public ProfileOptimizerStructure getOptimizerStructure ()
  {
    return optimizerStructure;
  }

  @Override
  public List<CapacityOriginator> getCapacityOriginators ()
  {
    return capacityOriginators;
  }
}
