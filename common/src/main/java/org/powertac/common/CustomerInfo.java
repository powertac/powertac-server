/*
 * Copyright 2009-2015 the original author or authors.
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

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
  static private Logger log = LogManager.getLogger(CustomerInfo.class.getName());

  public enum CustomerClass { SMALL, LARGE }
  @XStreamAsAttribute
  private long id = IdGenerator.createId();

  /** Name of the customer model */
  @XStreamAsAttribute
  private String name;

  /** population represented by this model */
  @XStreamAsAttribute
  private int population;

  @XStreamAsAttribute
  private PowerType powerType;

  @XStreamAsAttribute
  private CustomerClass customerClass = CustomerClass.SMALL;

  @XStreamAsAttribute
  private double controllableKW = 0.0;

  /** Negative value for up-regulation */
  @XStreamAsAttribute
  private double upRegulationKW = 0.0;

  /** Positive value for down-regulation */
  @XStreamAsAttribute
  private double downRegulationKW = 0.0;

  @XStreamAsAttribute
  private double storageCapacity = 0.0;

  /** True just in case the population of this customer model can be
   * divided to engage in multiple contracts at the same time.
   * Defaults to false. */
  @XStreamAsAttribute
  private boolean multiContracting = false;

  /** True just in case this customer negotiates over contracts.
   * Defaults to false, currently ignored. */
  @XStreamAsAttribute
  private boolean canNegotiate = false;
  
  /**
   * Creates a new CustomerInfo, with default power type set to
   * CONSUMPTION. Chain a call
   * to withPowerType() to set the correct power types.
   */
  public CustomerInfo (String name, int population)
  {
    super();
    powerType = PowerType.CONSUMPTION;
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
   * Returns the population of the model represented by this CustomerInfo.
   * This is not necessarily the number of people represented,
   * but the number of potential tariff subscribers.
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
   * Returns the type of power consumption and/or production available in
   * this customer model.
   */
  public PowerType getPowerType ()
  {
    return powerType;
  }
  
  /**
   * Fluent setter to set PowerType for this CustomerInfo.
   * Default value is PowerType.CONSUMPTION.
   */
  @StateChange
  public CustomerInfo withPowerType (PowerType type)
  {
    powerType = type;
    return this;
  }

  /**
   * Returns the customer class of this customer. This value determines the
   * amount of the periodic per-customer "meter charge" if this feature is
   * used.
   */
  public CustomerClass getCustomerClass ()
  {
    return customerClass;
  }

  /**
   * Fluent setter for customerClass attribute.
   */
  @StateChange
  public CustomerInfo withCustomerClass (CustomerClass cClass)
  {
    customerClass = cClass;
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
   * Default is false - all entities in the model will subscribe to the
   * same tariff.
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

  /**
   * Returns total controllable capacity in kW per member. This is the maximum
   * possible response to curtailment request. For a consumption power type,
   * this results in up-regulation and the value is <= 0,
   * because it represents less energy being delivered to the customer. 
   * Brokers should assume that curtailment will result in shifting of load
   * (or supply, for a production power type) to a later timeslot.
   */
  public double getControllableKW ()
  {
    return controllableKW;
  }

  /**
   * Deprecated synonym for getControllableKW().
   */
  @Deprecated
  public double getControllableKWh ()
  {
    return controllableKW;
  }

  /**
   * Fluent setter for controllable kWh. Intended to be called only by the
   * customer model itself. Must be non-positive.
   */
  @StateChange
  public CustomerInfo withControllableKW (double value)
  {
    if (value > 0.0)
      log.error(getName() + ": controllableKW " + value
                + " must be non-positive");
    else
      controllableKW = value;
    return this;
  }

  /**
   * Returns the maximum up-regulation rate in kW per member,
   * beyond the curtailment of its load.
   * Value is zero for thermal storage or no storage,
   * and negative for a battery
   * (because energy is flowing away from the customer).
   */
  public double getUpRegulationKW ()
  {
    return upRegulationKW;
  }

  /**
   * Fluent setter for up-regulation kW. Intended to be called only by the
   * customer model itself. Must be non-positive.
   */
  @StateChange
  public CustomerInfo withUpRegulationKW (double value)
  {
    if (value > 0.0)
      log.error(getName() + ": upRegulationKW " + value
                + " must be non-positive");
    else
      upRegulationKW = value;
    return this;
  }

  /**
   * Returns the maximum down-regulation rate in kW per member.
   * This is energy the
   * customer can absorb in a timeslot, beyond its normal usage.
   * A positive value represents charging a battery or dumping heat
   * (or cold) into some type of thermal storage system.
   */
  public double getDownRegulationKW ()
  {
    return downRegulationKW;
  }

  /**
   * Fluent setter for down-regulation kW. Intended to be called only by the
   * customer model itself. Must be non-negative.
   */
  @StateChange
  public CustomerInfo withDownRegulationKW (double value)
  {
    if (value < 0.0)
      log.error(getName() + ": downRegulationKW " + value
                + " must be non-negative");
    else
      downRegulationKW = value;
    return this;
  }

  /**
   * Returns the maximum energy storage capacity in kWh per member.
   * If a 2 kWh battery can be charged at 1 kW, it's maximum down-regulation
   * would be 1 kW, and its storage capacity would be 2 kWh.
   */
  public double getStorageCapacity ()
  {
    return storageCapacity;
  }

  /**
   * Fluent setter for storage capacity. Intended to be called only by the
   * customer model itself. Value must be non-negative.
   */
  @StateChange
  public CustomerInfo withStorageCapacity (double value)
  {
    if (value < 0.0)
      log.error(getName() + ": storageCapacity " + value
                + " must be non-negative");
    else
      storageCapacity = value;
    return this;
  }

  @Override
  public String toString()
  {
    return "CustomerInfo(" + name + ")";
  }
}
