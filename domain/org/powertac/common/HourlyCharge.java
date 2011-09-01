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
@XStreamAlias("charge")
public class HourlyCharge implements Comparable<HourlyCharge>
{
  static private Logger stateLog = Logger.getLogger("State");

  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  
  @XStreamAsAttribute
  private double value;

  private Instant atTime;

  public HourlyCharge (Instant when, double value)
  {
    super();
    this.value = value;
    this.atTime = when;
    stateLog.info("HourlyCharge:" + id + ":new:" + when.getMillis() + ":" + value);
  }

  public long getId ()
  {
    return id;
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
