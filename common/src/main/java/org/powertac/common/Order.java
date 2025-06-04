/*
 * Copyright 2009-2013 the original author or authors.
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
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.ChainedConstructor;
import org.powertac.common.state.Domain;
import org.powertac.common.state.XStreamStateLoggable;
import org.powertac.common.xml.BrokerConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;

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
 * <p>
 * Note that the limitPrice field is a Double, not a double. A market order
 * will have null in this field, and will be sorted first by the auctioneer
 * for both buy and sell orders.
 * <p>
 * State log fields for readResolve():<br>
 * new(long brokerId, long timeslotId, double mwh, Double limitPrice)
 *
 * @author Carsten Block, John Collins
 */
@Domain(fields = {"broker", "timeslot", "MWh", "limitPrice"})
// Note that the name MWh is required due to the second char being a cap.
// Probably the field should be renamed "mwh", but that would change
// the xml representation.
@XStreamAlias("order")
public class Order extends XStreamStateLoggable
{  
  static private Logger log = LogManager.getLogger(Order.class);

  @XStreamAsAttribute
  private long id = IdGenerator.createId();

  /** the broker who created this shout */
  @XStreamConverter(BrokerConverter.class)
  private Broker broker;

  /** the timeslot for which the product should be bought or sold */
  @XStreamAsAttribute
  private int timeslot;

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
  public Order (Broker broker, int timeslot, 
                double mWh, Double limitPrice)
  {
    super();
    this.broker = broker;
    this.timeslot = timeslot;
    this.mWh = mWh;
    if (null != limitPrice && limitPrice.isNaN()) {
      log.error("Limit price is NaN");
      limitPrice = null;
    }
    this.limitPrice = limitPrice;
    // check for minimum order qty - should we adjust?
    double min = Competition.currentCompetition().getMinimumOrderQuantity();
    if (Math.abs(mWh) < min) {
      log.warn("Order quantity " + mWh + " < minimum order quantity " + min);
               //+ ": adjusting");
      //this.mWh = min;
    }
  }

  /**
   * Creates a new order using a Timeslot rather than a timeslot index value.
   * New code should not use this method.
   */
  @Deprecated
  @ChainedConstructor
  public Order (Broker broker, Timeslot timeslot, 
                double mWh, Double limitPrice)
  {
    this(broker, timeslot.getSerialNumber(), mWh, limitPrice);
  }
  
  public long getId ()
  {
    return id;
  }

  public Broker getBroker ()
  {
    return broker;
  }

  public int getTimeslotIndex ()
  {
    return timeslot;
  }
  
  public Timeslot getTimeslot ()
  {
    return getTimeslotRepo().findBySerialNumber(timeslot);
  }

  public Double getMWh ()
  {
    return mWh;
  }
  
  /**
   * Setter for logtool access; do not use otherwise
   */
  //public void setMWh (double statelogValue)
  //{
  //  mWh = statelogValue;
  //}

  public Double getLimitPrice ()
  {
    if (null == limitPrice || limitPrice.isNaN())
      return null;
    else
      return limitPrice;
  }
  
  @Override
  public String toString()
  {
    return ("Order " + id + " from " + broker.getUsername()
            + " for " + mWh + " mwh at " + limitPrice
            + " in ts " + timeslot);
  }
  
  // protected default constructor to simplify deserialization
  protected Order ()
  {
    super();
  }
  
  // access to TimeslotRepo
  private static TimeslotRepo timeslotRepo;
  
  private static TimeslotRepo getTimeslotRepo()
  {
    if (null == timeslotRepo) {
      timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    }
    return timeslotRepo;
  }
}
