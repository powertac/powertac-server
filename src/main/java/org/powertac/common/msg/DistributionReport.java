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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Reports total consumption and total production in kwh across all customers
 * in a given timeslot. These are absolute (positive) values.
 * 
 * @author John Collins
 */
@XStreamAlias("distribution-report")
public class DistributionReport
{
  @XStreamAsAttribute
  private double totalConsumption;
  
  @XStreamAsAttribute
  private double totalProduction;

  /**
   * Constructed empty. Call addProduction and addConsumption to fill it in.
   */
  public DistributionReport ()
  {
    super();
    totalConsumption = 0.0;
    totalProduction = 0.0;
  }

  /**
   * Adds to the total consumption. If kwh is coming from a source that
   * uses negative values for consumption, then the caller must negate
   * the value.
   */
  public void addConsumption (double kwh)
  {
    totalConsumption += kwh;
  }
  
  public double getTotalConsumption ()
  {
    return totalConsumption;
  }
  
  /**
   * Adds to production
   */
  public void addProduction (double kwh)
  {
    totalProduction += kwh;
  }
  
  public double getTotalProduction ()
  {
    return totalProduction;
  }
}
