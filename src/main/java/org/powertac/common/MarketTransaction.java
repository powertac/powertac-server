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

import org.powertac.common.xml.TimeslotConverter;
import org.powertac.common.state.Domain;
import com.thoughtworks.xstream.annotations.*;

/**
 * A MarketTransaction instance represents a trade in the wholesale market.
 * It is created by the market, used by Accounting to update accounts, and
 * forwarded to the broker for its records. The values represent changes in
 * the broker's energy and cash balances, from the viewpoint of the broker.
 * Therefore, a positive price means that money will be deposited in the
 * broker's bank account, and a positive amount of energy means that the broker
 * has an additional quantity of energy in its account for the given timeslot.
 *
 * @author Carsten Block, John Collins
 */
@Domain
@XStreamAlias("market-tx")
public class MarketTransaction extends BrokerTransaction
{
  /** price/mWh of a trade, positive for a buy, negative for a sell */
  @XStreamAsAttribute
  private double price;

  /** mWh of trade in mWh, positive for buy, negative for sell */
  @XStreamAsAttribute
  private double mWh;
  
  /** the timeslot for which this trade or quote information is created */
  @XStreamAsAttribute
  @XStreamConverter(TimeslotConverter.class)
  private Timeslot timeslot;
  
  public MarketTransaction (Broker broker, int when, 
                            Timeslot timeslot, double mWh, double price)
  {
    super(when, broker);
    this.timeslot = timeslot;
    this.price = price;
    this.mWh = mWh;
  }

  public double getPrice ()
  {
    return price;
  }

  public double getMWh ()
  {
    return mWh;
  }

  public Timeslot getTimeslot ()
  {
    return timeslot;
  }

  @Override
  public String toString() {
    return ("MktTx: time " + timeslot.getSerialNumber() + ", " +
            mWh + "@" + price);
  }
}
