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

import org.apache.log4j.Logger;

/**
 * Represents available regulation capacity for a given TariffSubscription.
 * This is basically a data structure that holds two numbers: an amount of
 * up-regulation capacity (non-negative), and an amount of down-regulation
 * capacity (non-positive).
 * 
 * @author John Collins
 */
public class RegulationCapacity
{
  protected static Logger log = Logger.getLogger(RegulationCapacity.class.getName());

  // ignore small numbers
  private static double epsilon = 1e-4;

  private double upRegulationCapacity = 0.0;

  private double downRegulationCapacity = 0.0;

  /**
   * Creates a new RegulationCapacity instance specifying the amounts of
   * regulating capacity available for up-regulation and down-regulation.
   * Values are expressed with respect to the balancing market; a negative
   * value means power is delivered to the customer (down-regulation), and a
   * positive value means power is delivered to the balancing market
   * (up-regulation).
   */
  public RegulationCapacity (double upRegulationCapacity,
                             double downRegulationCapacity)
  {
    super();
    if (upRegulationCapacity < 0.0) {
      log.warn("upRegulationCapacity " + upRegulationCapacity + " < 0.0");
      upRegulationCapacity = 0.0;
    }
    if (downRegulationCapacity > 0.0) {
      log.warn("downRegulationCapacity " + downRegulationCapacity + " > 0.0");
      downRegulationCapacity = 0.0;
    }
    this.upRegulationCapacity = upRegulationCapacity;
    this.downRegulationCapacity = downRegulationCapacity;
  }

  /**
   * Returns the available up-regulation capacity in kWh.
   * Value is non-negative.
   */
  public double getUpRegulationCapacity ()
  {
    return upRegulationCapacity;
  }

  /**
   * Sets the up-regulation value.
   * Argument must be non-negative.
   */
  public void setUpRegulationCapacity (double value)
  {
    double filteredValue = filterValue(value);
    if (filteredValue < 0.0) {
      log.warn("Attempt to set negative up-regulation capacity "
               + filteredValue);
      return;
    }
    upRegulationCapacity = filteredValue;
  }

  /**
   * Returns the available down-regulation capacity in kWh.
   * Value is non-positive.
   */
  public double getDownRegulationCapacity ()
  {
    return downRegulationCapacity;
  }

  /**
   * Sets the down-regulation value.
   * Argument must be non-negative.
   */
  public void setDownRegulationCapacity (double value)
  {
    double filteredValue = filterValue(value);
    if (filteredValue > 0.0) {
      log.warn("Attempt to set positive down-regulation capacity " + filteredValue);
      return;
    }
    downRegulationCapacity = filteredValue;
  }

  /**
   * Adds the capacities in the given RegulationCapacity instance to this
   * instance. 
   */
  public void add (RegulationCapacity rc)
  {
    upRegulationCapacity += rc.upRegulationCapacity;
    downRegulationCapacity += rc.downRegulationCapacity;
  }

  /**
   * Adds the given amount of up-regulation capacity.
   * Amount must be non-negative.
   */
  public void addUpRegulation (double amount)
  {
    if (amount < 0.0) {
      log.warn("Attempt to add negative up-regulation capacity " + amount);
      return;
    }
    upRegulationCapacity += amount;
  }

  /**
   * Adds the given amount of down-regulation capacity.
   * Amount must be non-positive.
   */
  public void addDownRegulation (double amount)
  {
    if (amount > 0.0) {
      log.warn("Attempt to add positive down-regulation capacity " + amount);
      return;
    }
    downRegulationCapacity += amount;
  }

  // filter out small values
  private double filterValue (double original)
  {
    if (Math.abs(original) < epsilon)
      return 0.0;
    return original;

  }
}
