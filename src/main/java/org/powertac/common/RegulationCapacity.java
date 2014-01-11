/*
 * Copyright (c) 2014 by the original author
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
package org.powertac.common;

/**
 * Represents available regulation capacity for a given TariffSubscription.
 * 
 * @author John Collins
 */
public class RegulationCapacity
{
  private double upRegulationCapacity = 0.0;
  
  private double downRegulationCapacity = 0.0;
  
  public RegulationCapacity (double upRegulationCapacity,
                             double downRegulationCapacity)
  {
    super();
    this.upRegulationCapacity = upRegulationCapacity;
    this.downRegulationCapacity = downRegulationCapacity;
  }
  
  /**
   * Returns the up-regulationCapacity available in kWh
   */
  public double getUpRegulationCapacity ()
  {
    return upRegulationCapacity;
  }

  public double getDownRegulationCapacity ()
  {
    return downRegulationCapacity;
  }
}
