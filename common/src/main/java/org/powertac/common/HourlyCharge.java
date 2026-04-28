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

import java.time.Instant;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.state.XStreamStateLoggable;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents the cost of power during a specific timeslot in a variable
 * Rate. The value slot represents the charge/kWh; atTime is the Instant
 * at the start of the relevant timeslot. Therefore, the charge is in effect
 * from atTime until atTime + 1 hour. These are created by brokers and sent
 * to the server to update tariff pricing.
 * 
 * State log fields for readResolve():<br>
 * new(long rateId, double value, Instant atTime)
 * 
 * @author jcollins
 */
@Domain (fields = {"rateId", "value", "atTime"})
@XStreamAlias("charge")
public class HourlyCharge
extends XStreamStateLoggable
implements Comparable<HourlyCharge>
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();

  @XStreamAsAttribute
  private long rateId = -1;
  
  @XStreamAsAttribute
  private double value;

  protected Instant atTime;

  /**
   * Creates a new HourlyCharge to communicate rate information to customers.
   * The {@code when} parameter specifies when this charge takes effect.
   * The specified charge/kWh applies until the next HourlyCharge takes effect
   * on the same Rate. Note that
   * the numbers are interpreted from the viewpoint of the Customer, so if
   * the customer is expected to pay the broker, the value should be negative
   * (a debit). 
   */
  public HourlyCharge (Instant when, double charge)
  {
    super();
    this.value = charge;
    this.atTime = when;
  }

  public long getId ()
  {
    return id;
  }

  /**
   * Sets connection between rate and hourly charge. An instance with the
   * default rateId is not considered complete. This method is intended 
   * to be called from Rate when the instance is added to the rate.
   */
  @StateChange
  public void setRateId (long rateId)
  {
    this.rateId = rateId;
  }
  
  public long getRateId ()
  {
    return rateId;
  }
  
  public double getValue ()
  {
    return value;
  }

  public Instant getAtTime ()
  {
    return atTime;
  }

  @Override
  public int compareTo (HourlyCharge obj) {
    return atTime.compareTo(obj.atTime);
  }
  
  // protected default constructor for reconstruction
  protected HourlyCharge () {
    super();
  }
}
