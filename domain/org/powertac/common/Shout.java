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

import org.joda.time.Instant;
import org.powertac.common.enumerations.BuySellIndicator;
//import org.powertac.common.enumerations.ModReasonCode;
import org.powertac.common.enumerations.OrderType;
import org.powertac.common.enumerations.ProductType;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.xml.BrokerConverter;
import org.powertac.common.xml.TimeslotConverter;
import org.springframework.beans.factory.annotation.Autowired;

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
  @Autowired
  private TimeService timeService;

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

  /** the product quantity to buy or sell */
  @XStreamAsAttribute
  private double quantity;

  /** the limit price, i.e. the max. acceptable buy or min acceptable sell price */
  @XStreamAsAttribute
  private double limitPrice;

  /** the last executed quantity (if equal to {@code quantity} the shout is fully executed otherwise it is partially executed */
  @XStreamAsAttribute
  private double executionQuantity;

  /** the last execution price */
  @XStreamAsAttribute
  private double executionPrice;

  /** either MARKET or LIMIT order */
  // not needed - presence of limit price indicates limit order
  //OrderType orderType = OrderType.MARKET

  /** the simulation time when the original shout instance was first created.
   * Defaults to current time. */
  // JEC - do we need this?
  private Instant dateCreated = timeService.getCurrentTime();

  /** the latest modification time of the shout. Defaults to dateCreated. */
  // JEC - do we need this?
  private Instant dateMod = this.dateCreated;

  /** the reason for the latest modifcation to the shout instance */
  // JEC - removed - do we need this?
  //@XStreamAsAttribute
  //ModReasonCode modReasonCode = ModReasonCode.INSERT;

  /** A transactionId is generated during the execution of the shout 
   * and marks all domain instances in all domain classes that were 
   * created or changed during this single transaction 
   * (e.g. corresponding transactionLog, CashUpdate, or 
   * MarketPosition instances). Later on this id allows for 
   * correlation of the different domain class instances during 
   * ex post analysis*/
  private long transactionId;

  /** optional comment that can be used for example to further 
   * describe why a shout was deleted by system (e.g. during 
   * deactivaton of a timeslot) */
  private String comment;

  // JEC -- do we need this at all?
  /**
   * Updates shout instance:
   * 1) updates the modReasonCode field in the cloned instance to the value provided as method param
   * 2) keeps the 'dateCreated' property in the cloned instance unchanged
   * 3) sets 'dateMod' property in the cloned instance to *now* (in simulation time)
   * 4) (does not) sets 'transactionId' property in the cloned instance to null
   *
   * @param newModReasonCode new modReasonCode to use in the cloned shout instance
   * @return cloned shout instance where the cloned instance is changed as described above
   */
  //public Shout initModification(ModReasonCode newModReasonCode) {
  //  this.dateMod = timeService.currentTime
  //  this.modReasonCode = newModReasonCode
  //  return this
  //}
  public Shout (Broker broker, Timeslot timeslot, 
                BuySellIndicator buySellIndicator,
                double quantity, double limitPrice)
  {
    super();
    this.broker = broker;
    this.timeslot = timeslot;
    this.buySellIndicator = buySellIndicator;
    this.quantity = quantity;
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

  public double getExecutionQuantity ()
  {
    return executionQuantity;
  }

  @StateChange
  public Shout withExecutionQuantity (double executionQuantity)
  {
    this.executionQuantity = executionQuantity;
    return this;
  }

  public double getExecutionPrice ()
  {
    return executionPrice;
  }

  @StateChange
  public Shout withExecutionPrice (double executionPrice)
  {
    this.executionPrice = executionPrice;
    return this;
  }

  public Instant getDateMod ()
  {
    return dateMod;
  }

  @StateChange
  public Shout withDateMod (Instant dateMod)
  {
    this.dateMod = dateMod;
    return this;
  }

  public long getTransactionId ()
  {
    return transactionId;
  }

  @StateChange
  public Shout withTransactionId (long transactionId)
  {
    this.transactionId = transactionId;
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

  public double getQuantity ()
  {
    return quantity;
  }

  public double getLimitPrice ()
  {
    return limitPrice;
  }

  public Instant getDateCreated ()
  {
    return dateCreated;
  }
}
