/*
 * Copyright (c) 2014 by John E. Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.common.msg;

import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Reports total net imbalance for a given timeslot, in kWh. The value is
 * positive for surplus (needing down-regulation), negative for deficit
 * (needing up-regulation).
 * 
 * @author John Collins
 */
@XStreamAlias("balance-report")
@Domain
public class BalanceReport
{
  @XStreamAsAttribute
  private double netImbalance = 0.0;

  @XStreamAsAttribute
  private int timeslotIndex;
  
  /**
   * Constructed as balanced. Call addImbalance to fill it in.
   */
  public BalanceReport (int timeslotIndex)
  {
    super();
    this.timeslotIndex = timeslotIndex;
  }

  /**
   * Adds to the netImbalance. Value must be positive for surplus, negative for
   * shortage.
   */
  public void addImbalance (double kwh)
  {
    netImbalance += kwh;
  }

  public double getNetImbalance ()
  {
    return netImbalance;
  }

  public int getTimeslotIndex()
  {
    return timeslotIndex;
  }
}
