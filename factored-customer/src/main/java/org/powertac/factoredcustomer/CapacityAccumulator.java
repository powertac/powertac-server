/*
 * Copyright (c) 2018 by John E. Collins
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
package org.powertac.factoredcustomer;

/**
 * Communicates and accumulates capacity information, including production,
 * consumption, up-regulation, and down-regulation. Intended to be used by
 * CapacityOriginator instances to return values to UtilityOptimizers.
 * 
 * @author John Collins
 */
public class CapacityAccumulator
{
  private double capacity = 0.0;
  private double upRegulationCapacity = 0.0;
  private double downRegulationCapacity = 0.0;

  /**
   * Creates an empty one
   */
  public CapacityAccumulator ()
  {
    super();
  }

  /**
   * Creates an instance with specific values
   */
  public CapacityAccumulator (double capacity,
                              double upRegCap,
                              double downRegCap)
  {
    super();
    this.capacity = capacity;
    this.upRegulationCapacity = upRegCap;
    this.downRegulationCapacity = downRegCap;
  }

  /**
   * Adds another CapacityAccumulator to this one.
   */
  public CapacityAccumulator add (CapacityAccumulator other)
  {
    this.capacity += other.getCapacity();
    this.upRegulationCapacity += other.getUpRegulationCapacity();
    this.downRegulationCapacity += other.getDownRegulationCapacity();
    return this;
  }

  /**
   * Scales values by ratio. Used to adjust for subscription ratios.
   */
  public void scale (double ratio)
  {
    capacity *= ratio;
    upRegulationCapacity *= ratio;
    downRegulationCapacity *= ratio;
  }

  // Field accessors
  public double getCapacity ()
  {
    return capacity;
  }

  public double getUpRegulationCapacity ()
  {
    return upRegulationCapacity;
  }

  public double getDownRegulationCapacity ()
  {
    return downRegulationCapacity;
  }
}
