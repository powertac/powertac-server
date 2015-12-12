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
 * Represents the fee assessed by the Distribution Utility for peak
 * capacity events. These are issued to each broker once for each
 * capacity assessment interval.
 *
 * @author John Collins
 */
@Domain(fields = {"postedTimeslot", "threshold", "KWh", "charge"})
@XStreamAlias("distribution-tx")
public class CapacityTransaction extends BrokerTransaction
{
  /**
   * The peak-demand threshold for this assessment.
   */
  @XStreamAsAttribute
  private double threshold = 0.0;

  /** The amount by which this broker's total net consumption
   *  exceeded the threshold, in kWh.
   */
  @XStreamAsAttribute
  private double kWh = 0.0;
  
  /** The total charge imposed by the DU for this assessment. Since this
   * is a debit, it will always be negative. */
  @XStreamAsAttribute
  private double charge = 0.0;

  public CapacityTransaction (Broker broker, int when, double threshold,
                              double kwh, double charge)
  {
    super(when, broker);
    this.threshold = threshold;
    this.kWh = kwh;
    this.charge = charge;
  }

  public double getThreshold ()
  {
    return threshold;
  }

  public double getKWh ()
  {
    return kWh;
  }

  public double getCharge ()
  {
    return charge;
  }

  @Override
  public String toString() {
    return ("Capacity tx " + postedTimeslot +
        "-" + broker.getUsername() + "-(" + threshold + "," + kWh + ")-" + charge);
  }
}
