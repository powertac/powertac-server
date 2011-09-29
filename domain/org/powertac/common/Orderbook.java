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

import java.util.SortedSet;
import java.util.TreeSet;

import org.joda.time.Instant;
import org.powertac.common.enumerations.ProductType;
import com.thoughtworks.xstream.annotations.*;
import com.thoughtworks.xstream.converters.collections.TreeSetConverter;

import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.xml.TimeslotConverter;

/**
 * An orderbook instance captures a snapshot of the PowerTAC wholesale market's orderbook
 * (the un-cleared bids and asks remaining after the market is cleared). 
 * Each OrderbookEntry contained in the orderbook contains a limit price and
 * total un-cleared buy / sell quantity im mWh at that price.
 * Each time the market clears, one orderbook is created and sent to brokers for each
 * timeslot being traded during that clearing event.
 *
 * @author Daniel Schnurr
 * @version 1.2 , 05/02/2011
 */
@Domain
@XStreamAlias("orderbook")
public class Orderbook 
{
  //@XStreamAsAttribute
  private Instant dateExecuted;

  /** the transactionId is generated during the execution of a trade in market and
   * marks all domain instances in all domain classes that were created or changed
   * during this transaction. Like this the orderbookInstance with transactionId=1
   * can be correlated to shout instances with transactionId=1 in ex-post analysis  */
  //@XStreamAsAttribute
  //private long transactionId;

  /** the product this orderbook is generated for  */
  @XStreamAsAttribute
  private ProductType product = ProductType.Future;

  /** the timeslot this orderbook is generated for  */
  @XStreamConverter(TimeslotConverter.class)
  private Timeslot timeslot;

  /** last clearing price; if there is no last clearing price the min ask (max bid) will be returned*/
  @XStreamAsAttribute
  private Double clearingPrice;


  /** sorted set of OrderbookEntries with buySellIndicator = buy (descending)*/
  private SortedSet<OrderbookOrder> bids = new TreeSet<OrderbookOrder>();

  /** sorted set of OrderbookEntries with buySellIndicator = sell (ascending)*/
  private SortedSet<OrderbookOrder> asks = new TreeSet<OrderbookOrder>();

  public Orderbook (Timeslot timeslot, ProductType product,
                    Double clearingPrice, Instant dateExecuted)
  {
    super();
    this.timeslot = timeslot;
    this.product = product;
    this.clearingPrice = clearingPrice;
    this.dateExecuted = dateExecuted;
    //this.transactionId = transactionId;
  }
  
  public Double getClearingPrice ()
  {
    return clearingPrice;
  }

  public Instant getDateExecuted ()
  {
    return dateExecuted;
  }

  //public long getTransactionId ()
  //{
  //  return transactionId;
  //}

  public ProductType getProduct ()
  {
    return product;
  }

  public Timeslot getTimeslot ()
  {
    return timeslot;
  }

  public SortedSet<OrderbookOrder> getBids ()
  {
    return bids;
  }
  
  @StateChange
  public Orderbook addBid (OrderbookOrder bid)
  {
    bids.add(bid);
    return this;
  }

  public SortedSet<OrderbookOrder> getAsks ()
  {
    return asks;
  }
  
  @StateChange
  public Orderbook addAsk (OrderbookOrder ask)
  {
    asks.add(ask);
    return this;
  }
}