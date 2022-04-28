/**
 * Copyright (c) 2022 by John Collins.
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
package org.powertac.customer.evcharger;

import java.util.List;

/**
 * Immutable data carrier, represents the energy need for some number of vehicles that will
 * depart from their chargers in a single timeslot in the future.
 * 
 * @author John Collins
 */
class DemandElement // package visibility
{
  // how far in the future will this amount be needed?
  private int horizon = 0;

  // how many vehicles will stop actively charging at that time?
  // It's a double, not an int, to allow for more accurate simulation.
  private double nVehicles = 0.0;

  // how much energy in units of charger-hours is needed by those vehicles at the time they
  // disconnect? This is a histogram of (horizon + 1) elements, such that the first element
  // is the charger-hours needed by vehicles that need at least horizon charger-hours,
  // the next is the charger-hours needed by vehicles needing between h and (h-1) charger-
  // hours, and so on.
  private double[] requiredEnergy = {0.0};
  
  DemandElement (int horizon, double nVehicles, double[] requiredEnergy)
  {
    super();
    this.horizon = horizon;
    this.nVehicles = nVehicles;
    this.requiredEnergy = requiredEnergy;
  }

  int getHorizon ()
  {
    return horizon;
  }

  double getNVehicles ()
  {
    return nVehicles;
  }

  double[] getRequiredEnergy ()
  {
    return requiredEnergy;
  }

  void adjustRequiredEnergy (double[] increment)
  {
    if (increment.length > requiredEnergy.length) {
      double[] newre = new double[increment.length];
      for (int i = 0; i < requiredEnergy.length; i++) {
        newre[i] = requiredEnergy[i];
      }
      requiredEnergy = newre;
    }
    for (int i = 0; i < increment.length; i++ ) {
      requiredEnergy[i] += increment[i];
    }
  }

  public String toString ()
  {
    return String.format("(h%d,n%.3f,e%s%n)", horizon, nVehicles, requiredEnergy.toString());
  }
}
