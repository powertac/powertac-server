/*
 * Copyright 2009-2012 the original author or authors.
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
import org.powertac.common.xml.PowerTypeConverter;

import com.thoughtworks.xstream.annotations.*;

/**
 * A CustomerInfo instance represents a customer model (i.e. a consumer or a producer)
 * within a specific competition. The customer data stored is published to all brokers in
 * the Competition in order to provide them with an brief overview on what type
 * of customers participate in the specific competition. The collection of CustomerInfo
 * instances are serialized and sent to brokers at the beginning of a game, allowing brokers
 * to correlate tariff subscriptions and power consumption/production with individual customers.
 *
 * @author Carsten Block, John Collins
 */
@Domain
@XStreamAlias("cust-info")
public class CustomerInfo //implements Serializable 
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();

  /** Name of the customer model */
  @XStreamAsAttribute
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
  @XStreamImplicit(itemFieldName = "power-type")
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

  /**
   * Display name for this CustomerInfo instance.
   */
  public String getName ()
  {
    return name;
  }

  /**
   * Population of the model represented by the CustomerInfo. This is not
   * necessarily the number of people represented, but the number of potential
   * tariff subscribers.
   */
  public int getPopulation ()
  {
    return population;
  }

  /**
   * Updates the population for the underlying model. Depending on when this
   * method is called, it may or may not have any impact. There is also no
   * guarantee that the underlying model refers to this value in any useful
   * way.
   */
  @StateChange
  public void setPopulation (Integer population)
  {
    this.population = population;
  }

  /** 
   * Gives a "rough" classification what type of customer to 
   * expect based on the CustomerType enumeration, i.e. a fixed set of 
   * customer types. Defaults to CustomerHousehold. */
  public CustomerType getCustomerType ()
  {
    return customerType;
  }
  
  /**
   * Fluent setter for customer classification.
   */
  @StateChange
  public CustomerInfo withCustomerType (CustomerType type)
  {
    customerType = type;
    return this;
  }

  /**
   * The types of power consumption and/or production modalities available in
   * the customer model.
   */
  public ArrayList<PowerType> getPowerTypes ()
  {
    if (powerTypes == null) {
      // deserialization can leave this null
      powerTypes = new ArrayList<PowerType>();
    }
    return powerTypes;
  }
  
  /**
   * Fluent setter to add PowerType flags to this CustomerInfo.
   */
  @StateChange
  public CustomerInfo addPowerType (PowerType type)
  {
    powerTypes.add(type);
    return this;
  }

  /**
   * True if this customer can subscribe to multiple contracts. This is normally
   * true for population models, false for models of individual entities.
   */
  public boolean isMultiContracting ()
  {
    return multiContracting;
  }

  /**
   * Fluent setter for the multiContracting property.
   */
  @StateChange
  public CustomerInfo withMultiContracting (boolean value)
  {
    multiContracting = value;
    return this;
  }

  /**
   * True just in case the underlying Customer model can negotiate individual
   * contracts.
   */
  public boolean isCanNegotiate ()
  {
    return canNegotiate;
  }
  
  /**
   * Fluent setter for the canNegotiate flag. Should be called only by the
   * model itself.
   */
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
