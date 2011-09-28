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

import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.*;

/**
 * A {@code BalanceTransaction} instance represents the final supply/demand
 * imbalance in the current timeslot, and the Distribution Utilities charge
 * for this imbalance.
 *
 * @author John Collins
 */
@Domain
@XStreamAlias("balance-tx")
public class BalancingTransaction extends BrokerTransaction
{
  /** The total size of the imbalance in kWH, positive for surplus and
   * negative for deficit
   */
  @XStreamAsAttribute
  private double quantity = 0.0;
  
  /** The total charge imposed by the DU for this imbalance --
   *  positive for credit to broker, negative for debit from broker */
  @XStreamAsAttribute
  private double charge = 0.0;
  
  public BalancingTransaction (Broker broker, Instant when, 
                               double quantity, double charge)
  {
    super(when, broker);
    this.quantity = quantity;
    this.charge = charge;
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
    return ("Balance tx " + postedTime.getMillis()/TimeService.HOUR +
            "-" + broker.getUsername() + "-" + quantity + "-" + charge);
  }
}
