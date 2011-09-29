/*
 * Copyright 2009-2010 the original author or authors.
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

import org.powertac.common.enumerations.BuySellIndicator;
import org.powertac.common.enumerations.ProductType;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.xml.BrokerConverter;
import org.powertac.common.xml.TimeslotConverter;

import com.thoughtworks.xstream.annotations.*;

/**
 * A shout domain instance represents a market (no price specified) or a limit (min/max
 * price specified) order in the PowerTAC wholesale
 * market. More precisely it represents a single state of this specific order. Each time a
 * change occurs, the shout object is cloned, the {@code latest} property is set to false
 * for the original object (which remains unchanged otherwise) and all necessary changes
 * are put into the newly cloned shout instance. Like this, each update (e.g. a partial
 * execution, a deletion by system or by user can be tracked by selecting all
 * shout instances from the database that carry the same {@code shoutId}, which is the
 * common identifier for all instances that belong to the same original shout (note that
 * this is not the {@code id} field, which is different (and unique) for each shout
 * instance and servers as primary key for the database.
 *
 * Note: The word "shout" was chosen to avoid db level incompatibilities due to the word
 * "order" being a reserved word in most SQL dialects.
 *
 * @author Carsten Block
 */
@Domain
@XStreamAlias("shout")
public class Shout //implements Serializable 
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

  /** flag that indicates if this shout is a buy or sell order */
  @XStreamAsAttribute
  private BuySellIndicator buySellIndicator;

  /** the product quantity in mWh to buy or sell */
  @XStreamAsAttribute
  private double mWh;

  /** the limit price, i.e. the max. acceptable buy or min acceptable sell price */
  @XStreamAsAttribute
  private Double limitPrice;

  /** optional comment that can be used for example to further 
   * describe why a shout was deleted by system (e.g. during 
   * deactivaton of a timeslot) */
  private String comment;

  public Shout (Broker broker, Timeslot timeslot, 
                BuySellIndicator buySellIndicator,
                double mWh, Double limitPrice)
  {
    super();
    this.broker = broker;
    this.timeslot = timeslot;
    this.buySellIndicator = buySellIndicator;
    this.mWh = mWh;
    this.limitPrice = limitPrice;
  }

  public ProductType getProduct ()
  {
    return product;
  }

  /** Fluent-style setter */
  @StateChange
  public Shout withProduct (ProductType product)
  {
    this.product = product;
    return this;
  }

  public String getComment ()
  {
    return comment;
  }

  @StateChange
  public Shout withComment (String comment)
  {
    this.comment = comment;
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

  public BuySellIndicator getBuySellIndicator ()
  {
    return buySellIndicator;
  }

  @Deprecated
  public double getQuantity ()
  {
    return mWh;
  }

  public double getMWh ()
  {
    return mWh;
  }

  public double getLimitPrice ()
  {
    return limitPrice;
  }
}
