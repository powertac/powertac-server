/*
 * Copyright 2011-2018 the original author or authors.
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
 * @author Prashant Reddy, John Collins
 */
//@Domain
public class DefaultCapacityBundle implements CapacityBundle, StructureInstance
{
  protected FactoredCustomerService service;
  private CustomerStructure customerStructure;

  protected String name;
  @ConfigurableValue(description = "Number of capacity structures expected",
          valueType = "Integer", dump = false)
  protected int count;

  @ConfigurableValue(valueType = "Integer")
  protected int population;

  @ConfigurableValue(description = "PowerType for this bundle",
          valueType = "String", dump = false)
  protected String type;

  @ConfigurableValue(valueType = "String", dump = false)
  protected String customerSize = "SMALL";

  @ConfigurableValue(description = "If true, then this bundle can divide itself among multiple tariffs",
          valueType = "Boolean", dump = false)
  protected boolean multiContracting = false;

  @ConfigurableValue(description = "Unsupported, value ignored",
          valueType = "Boolean", dump = false)
  protected boolean canNegotiate = false;

  @ConfigurableValue(description = "Maximum curtailment per timeslot",
          valueType = "Double", dump = false)
  protected double controllableKW = 0.0;

  @ConfigurableValue(description = "Maximum storage discharge per timeslot",
          valueType = "Double", dump = false)
  protected double upRegulationKW = 0.0;

  @ConfigurableValue(description = "Maximum down-regulation (energy absorbed) per timeslot",
          valueType = "Double", dump = false)
  protected double downRegulationKW = 0.0;

  @ConfigurableValue(description = "",
          valueType = "Double", dump = false)
  protected double storageCapacity = 0.0;

  @ConfigurableValue(valueType = "Boolean", dump = false)
  protected boolean isAdaptive;

  private CustomerInfo customerInfo;

  private TariffSubscriberStructure subscriberStructure;
  private ProfileOptimizerStructure optimizerStructure;

  protected List<CapacityOriginator> capacityOriginators = new ArrayList<>();

  // keep track of INDIVIDUAL status
  private boolean allIndividual;

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
    this.allIndividual = true;

    // look up enum values and check validity
    PowerType pt = PowerType.valueOf(this.type);
    if (null == pt)
      throw new Error("Invalid PowerType for " + name);
    CustomerInfo.CustomerClass cc =
            CustomerInfo.CustomerClass.valueOf(customerSize);
    if (null == cc)
      throw new Error("Invalid CustomerClass for " + name);

    customerInfo = new CustomerInfo(name, this.population)
        .withPowerType(pt)
        .withCustomerClass(cc)
        .withMultiContracting(this.multiContracting)
        .withCanNegotiate(this.canNegotiate)
        .withControllableKW(controllableKW)
        .withUpRegulationKW(upRegulationKW)
        .withDownRegulationKW(downRegulationKW)
        .withStorageCapacity(storageCapacity);

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
        // Bad smell - strong, undocumented assumption about naming in config file.
        CapacityStructure capacityStructure =
            (CapacityStructure) capacities.get(name + (j + 1));
        if (capacityStructure == null) {
          throw new Error("No CapacityStructure for " + name + (j + 1));
        }
        capacityStructure.initialize(service);
        capacityOriginators.add(createCapacityOriginator(capacityStructure));
        if (!capacityStructure.isIndividual())
          allIndividual = false;
      }
    }
    else {
      allIndividual = false; // makes no sense for single instance
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

  @Override
  public boolean isAllIndividual ()
  {
    return allIndividual;
  }
}
