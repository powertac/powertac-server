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

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.annotations.*;

/**
 * A {@code CashPosition} domain instance represents the current state of
 * a broker's cash account. An updated CashPosition is sent to brokers
 * during each timeslot. This is not public information.
 *
 * @author Carsten Block, David Dauer
 * @version 1.1 - 02/27/2011
 */
@XStreamAlias("cash")
public class CashPosition //implements Serializable 
{
  static private Logger stateLog = Logger.getLogger("State");

  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  
  /** The broker who owns this cash account  */
  // JEC - back-reference not needed?
  //@XStreamConverter(BrokerConverter)
  //Broker broker;

  /** The new running total for the broker's cash account  */
  @XStreamAsAttribute
  double balance = 0.0;

  public CashPosition (double initialBalance)
  {
    balance = initialBalance;
    stateLog.info("CashPosition:" + this.id + ":new:" + balance);
  }
  
  public long getId ()
  {
    return id;
  }

  public double getBalance ()
  {
    return balance;
  }

  public String toString() {
    return "cash " + balance;
  }
  
  /**
   * Updates the balance in this account by the specified amount,
   * returns the resulting balance. A withdrawal is negative,
   * deposit is positive.
   */
  public double deposit (double amount)
  {
    balance += amount;
    return balance;
  }
}
