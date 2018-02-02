/*
 * Copyright (c) 2016 by John E. Collins
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.state.Domain;

/**
 * Accumulates available regulation capacity for a given TariffSubscription.
 * This is basically a data structure that holds two numbers: an amount of
 * up-regulation capacity (non-negative), and an amount of down-regulation
 * capacity (non-positive). The subscription is also included to simplify
 * log analysis; in cases where no subscription is involved, this value
 * should be null.
 * 
 * @author John Collins
 */
@Domain(fields = {"subscription", "upRegulationCapacity", "downRegulationCapacity"})
public class RegulationCapacity
{
  protected static Logger log = LogManager.getLogger(RegulationCapacity.class.getName());

  long id = IdGenerator.createId();

  // ignore small numbers
  private static double epsilon = 1e-10;

  private TariffSubscription subscription = null;

  private double upRegulationCapacity = 0.0;

  private double downRegulationCapacity = 0.0;

  /**
   * Creates a new RegulationAccumulator instance specifying the amounts of
   * regulating capacity available for up-regulation and down-regulation.
   * Values are expressed with respect to the balancing market; a negative
   * value means power is delivered to the customer (down-regulation), and a
   * positive value means power is delivered to the balancing market
   * (up-regulation).
   */
  public RegulationCapacity (TariffSubscription subscription,
                             double upRegulationCapacity,
                             double downRegulationCapacity)
  {
    super();
    this.subscription = subscription;
    if (upRegulationCapacity < 0.0) {
      if (upRegulationCapacity < -epsilon)
        log.warn("upRegulationCapacity " + upRegulationCapacity + " < 0.0");
      upRegulationCapacity = 0.0;
    }
    if (downRegulationCapacity > 0.0) {
      if (downRegulationCapacity > epsilon)
        log.warn("downRegulationCapacity " + downRegulationCapacity + " > 0.0");
      downRegulationCapacity = 0.0;
    }
    this.upRegulationCapacity = upRegulationCapacity;
    this.downRegulationCapacity = downRegulationCapacity;
  }

  /**
   * Default constructor
   */
  public RegulationCapacity ()
  {
    super();
  }

  public long getId ()
  {
    return id;
  }

  /**
   * Returns the subscription, if any, associated with this instance.
   */
  public TariffSubscription getSubscription ()
  {
    return subscription;
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
   * Returns the available down-regulation capacity in kWh.
   * Value is non-positive.
   */
  public double getDownRegulationCapacity ()
  {
    return downRegulationCapacity;
  }
}
