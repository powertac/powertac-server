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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents the fee assessed
 * by the Distribution Utility for transport of energy over its facilities
 * during the current timeslot. The kWh is the total energy delivered,
 * which is the sum of the positive net load of the broker's customers, and 
 * the positive net export of energy through the wholesale market. Negative
 * values are ignored.
 *
 * @author John Collins
 */
@Domain
@XStreamAlias("distribution-tx")
public class DistributionTransaction extends BrokerTransaction
{
  /** The total positive amount of energy transported in kWh.
   */
  @XStreamAsAttribute
  private double kWh = 0.0;
  
  /** The total charge imposed by the DU for this transport. Since this
   * is a debit, it will always be negative. */
  @XStreamAsAttribute
  private double charge = 0.0;

  public DistributionTransaction (Broker broker, Instant when, 
                                  double kwh, double charge)
  {
    super(when, broker);
    this.kWh = kwh;
    this.charge = charge;
  }
  
  @Deprecated
  public double getQuantity ()
  {
    return kWh;
  }

  public double getKWh ()
  {
    return kWh;
  }

  public double getCharge ()
  {
    return charge;
  }

  public String toString() {
    return ("Distribution tx " + postedTimeslot + 
        "-" + kWh + "-" + charge);
  }
}
