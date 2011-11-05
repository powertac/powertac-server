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

import org.powertac.common.enumerations.ProductType;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.xml.BrokerConverter;
import org.powertac.common.xml.TimeslotConverter;

import com.thoughtworks.xstream.annotations.*;

/**
 * A Order instance represents a market (no price specified) or a limit
 * (min/max price specified) order in the PowerTAC wholesale market. Each Order
 * specifies an amount of energy in MWh, and a price in units. The quantities
 * represent the broker's view of the proposed transaction in terms of the
 * broker's energy and money accounts: positive quantities
 * of energy represent a proposal to buy power from another party and transfer
 * it to the broker. A positive quantity of energy is almost always accompanied 
 * by a negative price, which in turn represents money to be transfered out
 * of the broker's account to the other party in the transaction. So a buy order
 * is indicated by a positive energy quantity, and a sell order is indicated by
 * a negative energy quantity.
 *
 * @author Carsten Block, John Collins
 */
@Domain
@XStreamAlias("order")
public class Order
{  
  @XStreamAsAttribute
  private long id = IdGenerator.createId();

  /** the broker who created this shout */
  @XStreamConverter(BrokerConverter.class)
  private Broker broker;

  /** the product that should be bought or sold. Defaults to Future */
  @XStreamAsAttribute
  private ProductType product = ProductType.Future;

  /** the timeslot for which the product should be bought or sold */
  @XStreamAsAttribute
  @XStreamConverter(TimeslotConverter.class)
  private Timeslot timeslot;

  /** product quantity in mWh - positive to buy, negative to sell */
  @XStreamAsAttribute
  private double mWh;

  /**
   * Limit price/mWh -- max. acceptable buy or sell price. A positive value
   * indicates payment to the broker; a negative value indicates payment to
   * the other party. Null value indicates a market order.
   */
  @XStreamAsAttribute
  private Double limitPrice = null;

  /**
   * Creates a new Order for Broker to buy or sell a quantity of energy
   * in Timeslot. A positive value for mWh indicates a buy order (because the
   * broker's energy account will increase), and a negative value for mWh
   * indicates an offer to sell. Similarly, a negative value for limitPrice
   * indicates a willingness to pay the given amount, while a positive value
   * indicates a demand to be paid at least that amount. A null value for
   * limitPrice indicates an unlimited price. In the clearing process, null
   * values will be considered last for both buy and sell orders, and
   * the price-setting algorithm may not be advantageous for the broker in this
   * case.
   */
  public Order (Broker broker, Timeslot timeslot, 
                double mWh, Double limitPrice)
  {
    super();
    this.broker = broker;
    this.timeslot = timeslot;
    this.mWh = mWh;
    this.limitPrice = limitPrice;
  }

  public ProductType getProduct ()
  {
    return product;
  }

  /** Fluent-style setter */
  @StateChange
  public Order withProduct (ProductType product)
  {
    this.product = product;
    return this;
  }

  public long getId ()
  {
    return id;
  }

  public Broker getBroker ()
  {
    return broker;
  }

  public Timeslot getTimeslot ()
  {
    return timeslot;
  }

  public double getMWh ()
  {
    return mWh;
  }

  public Double getLimitPrice ()
  {
    return limitPrice;
  }
}
