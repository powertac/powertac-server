/*
 * Copyright 2013, 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.evcustomer.beans;

import org.powertac.common.config.ConfigurableValue;

/**
 * Represents a type of electric vehicle.
 * @author Govert Buijs, John Collins
 */
public class CarType
{
  @ConfigurableValue(valueType = "Double",
      description = "Maximum battery capacity")
  private Double maxCapacity;     // kwh
  private double currentCapacity; // kwh

  @ConfigurableValue(valueType = "Double",
      description = "Maximum range in km")
  private Double range;           // Maximum range with a full battery, km

  @ConfigurableValue(valueType = "Double",
      description = "Maximum charge rate in kw, home station")
  private Double homeCharging;    // Charging speed at home, kwh / h == kw

  @ConfigurableValue(valueType = "Double",
      description = "Maximum charge rate in kw, remote station")
  private Double awayCharging;    // Charging speed away from home

  private String name;

  /**
   * Creates an instance, adds it to the instance list. This is public, because
   * it needs to be used by configuration.
   */
  public CarType (String name)
  {
    super();
    this.name = name;
  }

  /**
   * Configures an instance, needed for testing
   */
  public void configure (String name, double maxCapacity,
                  double range, double homeCharging, double awayCharging)
  {
    this.name = name;
    this.maxCapacity = maxCapacity;
    this.currentCapacity = 0.5 * this.maxCapacity;
    this.range = range;
    this.homeCharging = homeCharging;
    this.awayCharging = awayCharging;
  }

  // TODO Set min charge to 20%?
  public void discharge (double kwh) throws ChargeException
  {
    if (currentCapacity >= kwh) {
      currentCapacity -= kwh;
    }
    else {
      throw new ChargeException("Not possible to discharge " + name + " : "
          + kwh + " from " + currentCapacity);
    }
  }

  public void charge (double kwh) throws ChargeException
  {
    // TODO Check if partially charging would suffice

    if ((currentCapacity + kwh) <= maxCapacity) {
      currentCapacity += kwh;
    }
    else {
      throw new ChargeException("Not possible to charge " + name + " : "
          + kwh + " at " + currentCapacity + " (maxCap " + maxCapacity +")");
    }
  }

  public double getNeededCapacity (double distance)
  {
    // For now assume a linear relation
    double fuelEconomy = range / maxCapacity;
    return distance / fuelEconomy;
  }

  public double getChargingCapacity ()
  {
    // TODO Get home / away detection
    return Math.min(homeCharging, maxCapacity - currentCapacity);
  }

  public double getDischargingCapacity ()
  {
    // TODO Get home / away detection
    return Math.min(homeCharging, currentCapacity);
  }

  public String getName ()
  {
    return name;
  }

  public double getMaxCapacity ()
  {
    return maxCapacity;
  }

  public double getCurrentCapacity ()
  {
    return currentCapacity;
  }

  public void setCurrentCapacity (double currentCapacity)
  {
    this.currentCapacity = currentCapacity;
  }

  public double getRange ()
  {
    return range;
  }

  public double getHomeCharging ()
  {
    return homeCharging;
  }

  public double getAwayCharging ()
  {
    return awayCharging;
  }

  public class ChargeException extends Exception
  {
    private static final long serialVersionUID = 1L;

    public ChargeException (String message)
    {
      super(message);
    }
  }
}