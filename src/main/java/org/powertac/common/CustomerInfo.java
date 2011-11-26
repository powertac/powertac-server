/*
 * Copyright 2009-2011 the original author or authors.
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

package org.powertac.common;

import java.util.ArrayList;

import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import com.thoughtworks.xstream.annotations.*;

/**
 * A {@code CustomerInfo} instance represents a customer model (i.e. a consumer or a producer)
 * within a specific competition. The customer data stored is published to all brokers in
 * the respective competition in order to provide them with an brief overview on what type
 * of customers participate in the specific competition. The collection of CustomerInfo
 * instances are serialized and sent to brokers at the beginning of a game, allowing brokers
 * to correlate tariff subscriptions and power consumption/production with individual customers.
 *
 * @author Carsten Block, KIT; John Collins, U of Minnesota
 */
@Domain
@XStreamAlias("cust-info")
public class CustomerInfo //implements Serializable 
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();

  /** Name of the customer model */
  private String name;

  /** gives a "rough" classification what type of customer to 
   * expect based on an enumeration, i.e. a fixed set of 
   * customer types. Defaults to CustomerHousehold. */
  @XStreamAsAttribute
  private CustomerType customerType = CustomerType.CustomerHousehold;
  
  /** population represented by this model */
  @XStreamAsAttribute
  private int population;

  /** gives the available power classifications of the customer */
  private ArrayList<PowerType> powerTypes;
  
  /** describes whether or not this customer engages in multiple contracts at the same time.
   * Defaults to false. */
  @XStreamAsAttribute
  private boolean multiContracting = false;

  /** describes whether or not this customer negotiates over contracts.
   * Defaults to false. */
  @XStreamAsAttribute
  private boolean canNegotiate = false;
  
  /**
   * Creates a new CustomerInfo, with no power types set. Chain calls
   * to addPowerType() to add the correct power types.
   */
  public CustomerInfo (String name, int population)
  {
    super();
    powerTypes = new ArrayList<PowerType>();
    this.name = name;
    this.population = population;
  }
  
  public long getId ()
  {
    return id;
  }

  public String getName ()
  {
    return name;
  }

  public int getPopulation ()
  {
    return population;
  }

  @StateChange
  public void setPopulation (Integer population)
  {
    this.population = population;
  }

  public CustomerType getCustomerType ()
  {
    return customerType;
  }
  
  @StateChange
  public CustomerInfo withCustomerType (CustomerType type)
  {
    customerType = type;
    return this;
  }

  public ArrayList<PowerType> getPowerTypes ()
  {
    return powerTypes;
  }
  
  @StateChange
  public CustomerInfo addPowerType (PowerType type)
  {
    powerTypes.add(type);
    return this;
  }

  public boolean isMultiContracting ()
  {
    return multiContracting;
  }
  
  @StateChange
  public CustomerInfo withMultiContracting (boolean value)
  {
    multiContracting = value;
    return this;
  }

  public boolean isCanNegotiate ()
  {
    return canNegotiate;
  }
  
  @StateChange
  public CustomerInfo withCanNegotiate (boolean value)
  {
    canNegotiate = value;
    return this;
  }

  public String toString() {
    return "CustomerInfo(" + name + ")";
  }
}
