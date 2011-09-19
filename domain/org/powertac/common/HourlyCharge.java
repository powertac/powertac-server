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

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import com.thoughtworks.xstream.annotations.*;

/**
 * Represents the cost of power during a specific timeslot in a variable
 * Rate. The value slot represents the charge/kWh; atTime is the Instant
 * at the start of the relevant timeslot. Therefore, the charge is in effect
 * from atTime until atTime + 1 hour. These are created by brokers and sent
 * to the server to update tariff pricing.
 * 
 * @author jcollins
 */
@Domain
@XStreamAlias("charge")
public class HourlyCharge implements Comparable<HourlyCharge>
{
  static private Logger stateLog = Logger.getLogger("StateChange");

  @XStreamAsAttribute
  private long id = IdGenerator.createId();

  @XStreamAsAttribute
  private long rateId = -1;
  
  @XStreamAsAttribute
  private double value;

  private Instant atTime;

  public HourlyCharge (Instant when, double value)
  {
    super();
    this.rateId = rateId;
    this.value = value;
    this.atTime = when;
  }

  public long getId ()
  {
    return id;
  }

  /**
   * Set connection between rate and hourly charge. An instance with the
   * default rateId is not considered complete, and will not appear in the
   * state log. This method is intended to be called from Rate when the
   * instance is added to the rate.
   */
  void setRateId (long rateId)
  {
    this.rateId = rateId;
    stateLog.info("HourlyCharge:" + id + ":new:" + rateId + ":" + atTime.getMillis() + ":" + value);
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

  public int compareTo (HourlyCharge obj) {
    return atTime.compareTo(obj.atTime);
  }
}
