/*
 * Copyright (c) 2012-2016 by the original author
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

import org.powertac.common.IdGenerator;
import org.powertac.common.state.Domain;
import org.powertac.common.state.XStreamStateLoggable;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Reports total consumption and total production in kwh across all customers
 * in a given timeslot. These are absolute (positive) values.
 * 
 * @author John Collins
 */
@Domain(fields = {"timeslot", "totalConsumption", "totalProduction"})
@XStreamAlias("distribution-report")
public class DistributionReport extends XStreamStateLoggable
{
  @XStreamAsAttribute
  protected long id = IdGenerator.createId();

  @XStreamAsAttribute
  private int timeslot;

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
    timeslot = 0;
  }

  public DistributionReport (int timeslot,
                             double consumption, double production)
  {
    super();
    this.timeslot = timeslot;
    totalConsumption = consumption;
    totalProduction = production;
  }

  public long getId()
  {
    return id;
  }

  public int getTimeslot()
  {
    return timeslot;
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
