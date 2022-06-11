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

import java.util.Arrays;

/**
 * Immutable data carrier, represents the energy need for some number of vehicles that
 * will disconnect from their chargers in a single timeslot in the future. If <i>t</i>
 * is the current timeslot, then the horizon <i>h</i> tells us how many timeslots we
 * have to do the charging.
 * 
 * The distribution field tells how the energy needs of the nVehicles are distributed.
 * This is a histogram of (horizon + 1) elements, such that the first element is the
 * fraction of the population that needs at least horizon charger-hours, the next is
 * the fraction of the population needing between h and (h-1) charger-hours, and so on.
 *  
 * @author John Collins
 * @author Philipp Page <github@philipp-page.de>
 */
class DemandElement // package visibility
{
  // how far in the future will this amount be needed?
  private int horizon = 0;

  // how many vehicles will stop actively charging at that time?
  // It's a double, not an int, to allow for more accurate simulation.
  private double nVehicles = 0.0;

  // how is the population distributed in terms of energy requirements before
  // they disconnect?
  // It is a requirement that there be horizon elements in this array.
  private double[] distribution = {0.0};

  // Preferred constructor
  DemandElement (int horizon, double nVehicles, double[] distribution)
  {
    super();
    this.horizon = horizon;
    this.nVehicles = nVehicles;
    double dsum = Arrays.stream(distribution).sum();
    if (distribution.length == horizon + 1
            && (Math.abs(dsum - 1.0) < 1.0e-6
                || (nVehicles == 0 && dsum == 0.0))){
      this.distribution = distribution;
    }
    else {
      this.distribution = new double[horizon + 1];
      int index = 0;
      while (index < horizon + 1 - distribution.length) {
        this.distribution[index++] = 0.0;
      }
      int offset = index;
      while (index < horizon + 1) {
        double dval =
                (0.0 == nVehicles)? 0.0 :
                  distribution[index - offset] / nVehicles;
        this.distribution[index] = dval;
        index += 1;
      }      
    }
  }

  // Original constructor -- energy parameter is not used.
  DemandElement (int horizon, double nVehicles, double energy, double[] distribution)
  {
    this(horizon, nVehicles, distribution);
  }

  int getHorizon ()
  {
    return horizon;
  }

  double getNVehicles ()
  {
    return nVehicles;
  }

  double[] getdistribution ()
  {
    return distribution;
  }

  void makeCannonical ()
  {
    if (1.0 == getNVehicles()
            && distribution.length == getHorizon() + 1) {
      // It looks OK
      return;
    }
    double[] newDist = new double[getHorizon() + 1];
    int index = 0;
    while (index < getHorizon() + 1 - distribution.length) {
      newDist[index++] = 0.0;
    }
    while (index < getHorizon() + 1) {
      newDist[index] = distribution[index++] / getNVehicles();
    }
    distribution = newDist;
  }

  public String toString ()
  {
    return String.format("(h%d,n%.3f,e%s%n)", horizon, nVehicles, Arrays.toString(distribution));
  }
}
