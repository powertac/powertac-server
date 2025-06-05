/*
 * Copyright 2009-2015 the original author or authors.
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

/**
 * A CashPosition domain instance represents the current state of
 * a broker's cash account. An new CashPosition is sent to brokers
 * during each timeslot. This is not public information.
 *
 * @author Carsten Block, David Dauer, John Collins
 */
@Domain(fields = {"postedTimeslot", "balance"})
@XStreamAlias("cash")
public class CashPosition extends BrokerTransaction 
{
  /** The new running total for the broker's cash account  */
  @XStreamAsAttribute
  private double balance = 0.0;

  public CashPosition (Broker broker, double balance, int timeslot)
  {
    super(timeslot, broker);
    this.balance = balance;
  }

  /**
   * Returns the balance in the account at the time this CashPosition was
   * generated.
   */
  public double getBalance ()
  {
    return balance;
  }

  @Override
  public String toString() {
    return "cash " + balance;
  }
  
//  /**
//   * Updates the balance in this account by the specified amount,
//   * returns the resulting balance. A withdrawal is negative,
//   * deposit is positive.
//   */
//  @StateChange
//  public double deposit (double amount)
//  {
//    balance += amount;
//    return balance;
//  }
}
