/*
 * Copyright 2011 the original author or authors.
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
import org.powertac.common.state.Domain;
import org.powertac.common.xml.TimeslotConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;

/**
 * A ClearedTrade instance reports public information about a specific
 * market clearing -- clearing price and total quantity traded. Each time 
 * the market is cleared, a ClearedTrade is generated at least for each 
 * timeslot in which a non-zero quantity was traded. 
 * @author Daniel Schnurr, John Collins
 */
@Domain
@XStreamAlias("trade")
public class ClearedTrade
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();

  /** underlying timeslot for the trade (e.g. for a future the timeslot when real-world exchanges happen)*/
  @XStreamAsAttribute
  @XStreamConverter(TimeslotConverter.class)
  private Timeslot timeslot;

  /** the transactionId is generated during the execution of a trade in market and
   * relates corresponding domain instances that were created or changed during
   * this transaction. Like this the clearedTradeInstance with transactionId=1
   * can be correlated to shout instances with transactionId=1 in ex-post analysis  */
  //@XStreamAsAttribute
  //private long transactionId;

  /** clearing price of the trade */
  @XStreamAsAttribute
  private double executionPrice;

  /** traded quantity in mWh of the specified product */
  @XStreamAsAttribute
  private double executionMWh;

  /** point in time when cleared Trade object was created */
  @XStreamAsAttribute
  private Instant dateExecuted;
  
  public ClearedTrade (Timeslot timeslot, double executionMWh,
                       double executionPrice, Instant dateExecuted)
  {
    super();
    this.timeslot = timeslot;
    this.executionPrice = executionPrice;
    this.executionMWh = executionMWh;
    this.dateExecuted = dateExecuted;
  }

  public long getId ()
  {
    return id;
  }

  public Timeslot getTimeslot ()
  {
    return timeslot;
  }

  public double getExecutionPrice ()
  {
    return executionPrice;
  }

  public double getExecutionMWh ()
  {
    return executionMWh;
  }

  public Instant getDateExecuted ()
  {
    return dateExecuted;
  }
  
  @Override
  public String toString()
  {
    return "ClearedTrade " + executionMWh + "@" + executionPrice;
  }
}
