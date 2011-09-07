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

import org.joda.time.Instant;
import org.powertac.common.enumerations.BuySellIndicator;
import org.powertac.common.enumerations.MarketTransactionType;
import org.powertac.common.enumerations.ProductType;
import org.powertac.common.xml.BrokerConverter;
import org.powertac.common.xml.TimeslotConverter;
import com.thoughtworks.xstream.annotations.*;

/**
 * A MarketTransaction instance represents data commonly
 * referred to as trade and quote data (TAQ) in financial markets (stock exchanges).
 * One domain instance can (i) represent a trade that happened on the market
 * (price, quantity tuple and - in case of CDA markets - buyer and seller) or (ii)
 * a quote (which occurs if an order was entered into the system that changed the best
 * bid and/or best ask price / quantity but did not causing a clearing / trade). These
 * are created by the market, used by Accounting to update broker accounts, and then
 * forwarded to brokers.
 *<p>
 * Note: this domain class / table is closely modeled after the Thompson Reuter's TAQ data
 * file format in order to allow ex-post data analysis using the econometrics tools of the
 * Karlsruhe financial markets research group. The denormalization (trade and quote in one
 * domain class) is on purpose as econometrics analysis of market efficiency usually rely
 * on the combined data stream of both information types sorted by time precedence</p>
 * <p>
 * This an immutable value type, and therefore is not auditable.</p>
 *
 * @author Carsten Block, John Collins
 */
@XStreamAlias("market-tx")
public class MarketTransaction extends BrokerTransaction
{
  /** the product for which this information is created */
  @XStreamAsAttribute
  private ProductType product; // not clear what this means -- JEC

  /** price/mWh of a trade, positive for a buy, negative for a sell */
  @XStreamAsAttribute
  private double price;

  /** quantity of trade in mWh, positive for buy, negative for sell */
  @XStreamAsAttribute
  private double quantity;
  
  /** the timeslot for which this trade or quote information is created */
  @XStreamAsAttribute
  @XStreamConverter(TimeslotConverter.class)
  private Timeslot timeslot;
  
  public MarketTransaction (Broker broker, Instant when, 
                            Timeslot timeslot, double price, double quantity)
  {
    super(when, broker);
    this.timeslot = timeslot;
    this.price = price;
    this.quantity = quantity;
  }

  public ProductType getProduct ()
  {
    return product;
  }

  public double getPrice ()
  {
    return price;
  }

  public double getQuantity ()
  {
    return quantity;
  }

  public Timeslot getTimeslot ()
  {
    return timeslot;
  }

  public String toString() {
    return ("MktTx-" + timeslot.getSerialNumber() + "-" +
            quantity + "-" + price);
  }
}
