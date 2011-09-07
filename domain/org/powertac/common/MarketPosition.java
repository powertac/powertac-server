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

//import org.codehaus.groovy.grails.commons.ApplicationHolder
//import org.joda.time.Instant
import org.powertac.common.enumerations.ProductType;
import org.powertac.common.xml.BrokerConverter;
import org.powertac.common.xml.TimeslotConverter;
import com.thoughtworks.xstream.annotations.*;

/**
 * A {@code MarketPosition} domain instance represents the current position of a
 * single broker for wholesale power in a given timeslot. The evolution of this
 * position over time is represented by the sequence of MarketTransaction instances
 * for this broker and timeslot. These are created by the AccountingService and
 * communicated to individual brokers after the market clears in each timeslot.
 *
 * @author Carsten Block, David Dauer, John Collins
 */
@XStreamAlias("market-posn")
public class MarketPosition //implements Serializable 
{
  @XStreamAsAttribute
  protected long id = IdGenerator.createId();
  
  /** the broker this position update belongs to */
  @XStreamConverter(BrokerConverter.class)
  private Broker broker;
  
  /** the timeslot this position belongs to */
  @XStreamAsAttribute
  @XStreamConverter(TimeslotConverter.class)
  private Timeslot timeslot;
  
  /** The running total position the broker owns (> 0) / owes (< 0) of the specified
   *  product in the specified timeslot */
  @XStreamAlias("bal")
  @XStreamAsAttribute
  double overallBalance = 0.0;

  /** the product this position update belongs to */
  @XStreamAsAttribute
  private ProductType product = ProductType.Future; // not sure what this is for -- JEC

  public MarketPosition (Broker broker, Timeslot timeslot,
                         double balance, ProductType product)
  {
    super();
    this.broker = broker;
    this.timeslot = timeslot;
    this.overallBalance = balance;
    this.product = product;
  }

  public MarketPosition (Broker broker, Timeslot timeslot,
                         double balance)
  {
    this(broker, timeslot, balance, ProductType.Future);
  }
  
  public long getId()
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

  public double getOverallBalance ()
  {
    return overallBalance;
  }

  public ProductType getProduct ()
  {
    return product;
  }

  public String toString() {
    return ("MktPosn-" + broker.getId() + "-" +
            timeslot.getSerialNumber() + "-" + overallBalance);
  }
  
  /**
   * Adds a quantity to the current balance. Positive numbers signify
   * purchased power, negative numbers signify sold power. Returns the
   * resulting total balance
   */
  public double updateBalance (double quantity)
  {
    overallBalance += quantity;
    return overallBalance;
  }
}
