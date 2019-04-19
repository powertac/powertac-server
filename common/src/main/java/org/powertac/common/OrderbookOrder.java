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

import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Each instance is an individual un-cleared entry (a Bid or an Ask) within 
 * an Orderbook.
 * @author Daniel Schnurr
 */
@Domain(fields = {"mWh", "limitPrice"})
@XStreamAlias("orderbook-bid")
public class OrderbookOrder implements Comparable<Object> 
{
  @XStreamOmitField
  private long id = IdGenerator.createId();

  @XStreamAsAttribute
  private Double limitPrice;

  @XStreamAsAttribute
  private double mWh;
  
  
  public OrderbookOrder (double mWh, Double limitPrice)
  {
    super();
    this.limitPrice = limitPrice;
    this.mWh = mWh;
  }
  
  public long getId ()
  {
    return id;
  }

  @Override
  public int compareTo(Object o) {
    if (!(o instanceof OrderbookOrder)) 
      return 1;
    OrderbookOrder other = (OrderbookOrder) o;
    if (this.limitPrice == null)
      if (other.limitPrice == null)
        return 0;
      else
        return -1;
    else if (other.limitPrice == null)
      return 1;
    else
      return (this.limitPrice == (other.limitPrice) ? 0 : (this.limitPrice < other.limitPrice ? -1 : 1));
  }
  
  /** 
   * Returns the limit price for this unsatisfied order. Normally this will 
   * have a sign opposite to the mWh energy quantity.
   */
  public Double getLimitPrice ()
  {
    return limitPrice;
  }

  /**
   * Returns the quantity of energy unsatisfied for an order. This will positive
   * for a bid, negative for an ask.
   */
  public double getMWh ()
  {
    return mWh;
  }
  
  @Override
  public String toString()
  {
    return ("OrderbookOrder: " + mWh + "@" + limitPrice);
  }
}
