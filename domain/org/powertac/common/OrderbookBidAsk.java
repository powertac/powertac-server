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

import com.thoughtworks.xstream.annotations.*;

/**
 * Each instance is an individual entry within an Orderbook.
 * @author Daniel Schnurr
 */
@Domain
@XStreamAlias("orderbook-bid")
public class OrderbookBidAsk implements Comparable 
{

  @XStreamAsAttribute
  private double limitPrice;

  @XStreamAsAttribute
  private double mWh;
  
  public OrderbookBidAsk (double limitPrice, double mWh)
  {
    super();
    this.limitPrice = limitPrice;
    this.mWh = mWh;
  }

  public int compareTo(Object o) {
    if (!(o instanceof OrderbookBidAsk)) 
      return 1;
    OrderbookBidAsk other = (OrderbookBidAsk) o;
    return (this.limitPrice == (other.limitPrice) ? 0 : (this.limitPrice < other.limitPrice ? 1 : -1));
  }

  public double getLimitPrice ()
  {
    return limitPrice;
  }

  /**
   * @deprecated Use {@link getMWh()} instead.
   */
  @Deprecated
  public double getQuantity ()
  {
    return mWh;
  }

  public double getMWh ()
  {
    return mWh;
  }
}
