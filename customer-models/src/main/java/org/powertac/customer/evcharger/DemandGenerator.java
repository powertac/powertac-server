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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.Timeslot;

/**
 * Generates demand data from a set of statistical distributions. 
 * In each timeslot, we need to know how many vehicles are plugged in, how much energy they need, and
 * by when. Demand will presumably vary by time-of-day and day-of-week, and will also increase when
 * weather is (predicted to be) hot or cold. Heat requires some energy to be devoted to the AC, while
 * cold reduces battery capacity and requires heating the cabin, presumably with a resistance heater.
 * 
 * @author John Collins
 */
public class DemandGenerator
{
  static private Logger log = LogManager.getLogger(DemandGenerator.class.getName());

  public DemandGenerator ()
  {
    super();
  }

  /**
   * Returns the number of vehicles that are newly plugged and actively charging starting in
   * the given Timeslot.
   */
  public double getActivations (Timeslot ts)
  {
    double result = 0.0;
    // do something...
    return result;
  }

  /**
   * Returns a vector of pairs, indexed by time horizon (or the number of timeslots in the future), 
   * each representing the number of vehicles that will unplug and stop actively charging in that
   * timeslot and how much energy is needed by those unplugging vehicles. 
   */
  public List<DemandElement> getDemandVector (Timeslot ts)
  {
    return null; // stub
  }
}
