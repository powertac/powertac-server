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
@XStreamAlias("cust-info")
public class CustomerInfo //implements Serializable 
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();

  /** Name of the customer model */
  private String name;

  /** gives a "rough" classification what type of customer to expect based on an enumeration, i.e. a fixed set of customer types */
  @XStreamAsAttribute
  private CustomerType customerType;
  
  /** population represented by this model */
  @XStreamAsAttribute
  private Integer population = 1;

  /** gives the available power classifications of the customer */
  private ArrayList<PowerType> powerTypes;
  
  /** describes whether or not this customer engages in multiple contracts at the same time */
  @XStreamAsAttribute
  private Boolean multiContracting = false;

  /** describes whether or not this customer negotiates over contracts */
  @XStreamAsAttribute
  private Boolean canNegotiate = false;
  
  public CustomerInfo ()
  {
    super();
    powerTypes = new ArrayList<PowerType>();
    powerTypes.add(PowerType.CONSUMPTION);
    powerTypes.add(PowerType.PRODUCTION);
  }

  public Integer getPopulation ()
  {
    return population;
  }

  public void setPopulation (Integer population)
  {
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

  public CustomerType getCustomerType ()
  {
    return customerType;
  }

  public ArrayList<PowerType> getPowerTypes ()
  {
    return powerTypes;
  }

  public Boolean getMultiContracting ()
  {
    return multiContracting;
  }

  public Boolean getCanNegotiate ()
  {
    return canNegotiate;
  }

  public String toString() {
    return name;
  }
}
