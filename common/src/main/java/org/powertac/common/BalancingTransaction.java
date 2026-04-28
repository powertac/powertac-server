/*
 * Copyright 2011-2015 the original author or authors.
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
 * Represents the broker's final supply/demand
 * imbalance in the current timeslot, and the Distribution Utility's charge
 * for this imbalance.
 *
 * @author John Collins
 */
@Domain(fields = {"postedTimeslot", "KWh", "charge"})
@XStreamAlias("balance-tx")
public class BalancingTransaction extends BrokerTransaction
{
  @XStreamAsAttribute
  private double kWh = 0.0;
  
  @XStreamAsAttribute
  private double charge = 0.0;
  
  public BalancingTransaction (Broker broker, int when, 
                               double kWh, double charge)
  {
    super(when, broker);
    this.kWh = kWh;
    this.charge = charge;
  }

//  @Deprecated
//  public double getQuantity ()
//  {
//    return kWh;
//  }
  
  /**
   * Returns the total size of the imbalance in kWH, positive for surplus and
   * negative for deficit
   */
  public double getKWh ()
  {
    return kWh;
  }

  /**
   * Returns the total charge imposed by the DU for this imbalance --
   * positive for credit to broker, negative for debit from broker
   */
  public double getCharge ()
  {
    return charge;
  }

  @Override
  public String toString() {
    return ("Balance tx " + postedTimeslot +
            "-" + broker.getUsername() + "-" + kWh + "-" + charge);
  }
}
