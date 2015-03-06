/*
 * Copyright (c) 2015 by the original author
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
package org.powertac.customer;

import org.powertac.common.RegulationCapacity;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;

/**
 * Data-holder class, instances are used to communicate dynamic information
 * to and transport information back from the step() method of a
 * model component.
 * @author John Collins
 */
public class StepInfo
{
  // Energy used (positive) or produced (negative) during the step
  private double kWh = 0.0;

  // regulation capacity available at end of step
  private RegulationCapacity regulationCapacity;

  // current timeslot: immutable
  private Timeslot timeslot;

  void setTimeslot (Timeslot timeslot)
  {
    this.timeslot = timeslot;
  }

  // current tariff subscription: immutable
  private TariffSubscription subscription;

  public StepInfo (Timeslot slot, TariffSubscription sub)
  {
    super();
    timeslot = slot;
    subscription = sub;
    regulationCapacity = new RegulationCapacity(sub, 0.0, 0.0);
  }

  public double getKWh ()
  {
    return kWh;
  }

  public void setkWh (double kWh)
  {
    this.kWh = kWh;
  }

  public void addkWh (double kWh)
  {
    this.kWh += kWh;
  }

  public RegulationCapacity getRegulationCapacity ()
  {
    return regulationCapacity;
  }

  public void setRegulationCapacity (RegulationCapacity capacity)
  {
    this.regulationCapacity = capacity;
  }

  public void addRegulationCapacity (RegulationCapacity capacity)
  {
    regulationCapacity.add(capacity);
  }

  public Timeslot getTimeslot ()
  {
    return timeslot;
  }

  public TariffSubscription getSubscription ()
  {
    return subscription;
  }
}
