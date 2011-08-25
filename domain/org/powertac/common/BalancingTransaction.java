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
import com.thoughtworks.xstream.annotations.*;

/**
 * A {@code BalancingTransaction} instance represents the final supply/demand
 * imbalance in the current timeslot, and the Distribution Utility's charge
 * for this imbalance.
 *
 * @author John Collins
 */
@XStreamAlias("balance-tx")
public class BalancingTransaction
{
  @XStreamAsAttribute
  private int id;
  
  /** Whose transaction is this? */
  @XStreamAsAttribute
  private String brokerId;

  /** The timeslot for which this meter reading is generated */
  private Instant postedTime;

  /** The total size of the imbalance in kWH, positive for surplus and
   * negative for deficit
   */
  @XStreamAsAttribute
  private double quantity = 0.0;
  
  /** The total charge imposed by the DU for this imbalance --
   *  positive for credit to broker, negative for debit from broker */
  @XStreamAsAttribute
  private double charge = 0.0;

  /**
   * Creates a BalancingTransaction in the standard way, except that the
   * broker is converted to its ID value;
   */
  public BalancingTransaction (String brokerId, Instant time,
                               double quantity, double charge)
  {
    this.id = makeId();
    this.brokerId = brokerId;
    this.postedTime = time;
    this.quantity = quantity;
    this.charge = charge;
  }
  
  public int getId ()
  {
    return id;
  }

  public String getBrokerId ()
  {
    return brokerId;
  }

  public Instant getPostedTime ()
  {
    return postedTime;
  }

  public double getQuantity ()
  {
    return quantity;
  }

  public double getCharge ()
  {
    return charge;
  }

  public String toString() {
    return ("Balancing transaction " + postedTime.getMillis()/TimeService.HOUR
            + "-" + quantity + "-" + charge);
  }
  
  // static ID management
  private static int idValue = 0;
  
  private static synchronized int makeId ()
  {
    return idValue++;
  }
}
