/*
 * Copyright (c) 2012 by the original author
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
import org.powertac.common.state.StateChange;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Reports total consumption and total production in kwh across all customers
 * in a given timeslot. These are absolute (positive) values.
 * 
 * @author John Collins
 */
@Domain
@XStreamAlias("distribution-report")
public class DistributionReport
{
  @XStreamAsAttribute
  private double totalConsumption;
  
  @XStreamAsAttribute
  private double totalProduction;

  /**
   * Dummy constructor.
   */
  public DistributionReport ()
  {
    super();
    totalConsumption = 0.0;
    totalProduction = 0.0;
  }

  public DistributionReport (double consumption, double production)
  {
    super();
    totalConsumption = consumption;
    totalProduction = production;
  }

  public double getTotalConsumption ()
  {
    return totalConsumption;
  }

  public double getTotalProduction ()
  {
    return totalProduction;
  }
}
