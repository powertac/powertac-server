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
import org.powertac.common.xml.BrokerConverter;
import com.thoughtworks.xstream.annotations.*;

/**
 * A {@code DistributionTransaction} instance represents the fee assessed
 * by the Distribution Utility for transport of energy over its facilities
 * during the current timeslot. The quantity is the total energy delivered,
 * which is the sum of the positive net load of the broker's customers, and 
 * the positive net export of energy through the wholesale market. Negative
 * values are ignored.
 *
 * @author John Collins
 */
@XStreamAlias("distribution-tx")
public class DistributionTransaction
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  
  /** Whose transaction is this? */
  @XStreamConverter(BrokerConverter.class)
  private Broker broker;

  /** The timeslot for which this meter reading is generated */
  private Instant postedTime;

  /** The total positive amount of energy transported by the in kWH.
   */
  @XStreamAsAttribute
  private double quantity = 0.0;
  
  /** The total charge imposed by the DU for this transport. Since this
   * is a debit, it will always be negative. */
  @XStreamAsAttribute
  private double charge = 0.0;

  public DistributionTransaction (Instant when, Broker broker,
                                  double quantity, double charge)
  {
    super();
    this.postedTime = when;
    this.broker = broker;
    this.quantity = quantity;
    this.charge = charge;
  }
  
  public long getId ()
  {
    return id;
  }

  public Broker getBroker ()
  {
    return broker;
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
    return ("Distribution tx " + postedTime.getMillis()/TimeService.HOUR + 
        "-" + quantity + "-" + charge);
  }
}
