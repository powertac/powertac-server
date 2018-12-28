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

import org.powertac.common.IdGenerator;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;

/**
 * Represents a type of electric vehicle.
 * @author Govert Buijs, John Collins
 */
@ConfigurableInstance
public class CarType
{
  private long id;
  // configurable through setter
  private double maxCapacity;     // kwh

  // configurable through setter
  private double range;           // Maximum range with a full battery, km

  // configurable through setter
  private double homeChargeKW;    // Charging speed at home, kwh / h == kw

  // configurable through setter
  private double awayChargeKW;    // Charging speed away from home

  private double curtailmentFactor = -0.08; // expected curtailment as a function of HCK
  private double dischargeFactor = -0.05;
  private double downRegFactor = 0.10;

  private String name;

  /**
   * Creates an instance, adds it to the instance list. This is public, because
   * it needs to be used by configuration.
   */
  public CarType (String name)
  {
    super();
    this.name = name;
    id = IdGenerator.createId();
  }

  /**
   * Configures an instance, needed for testing
   */
  public void configure (String name, double maxCapacity,
                  double range, double homeChargeKW, double awayChargeKW)
  {
    this.name = name;
    setMaxCapacity(maxCapacity); // setters make setting show up in state log
    setRange(range);
    setHomeChargeKW(homeChargeKW);
    setAwayChargeKW(awayChargeKW);
  }

  public String getName ()
  {
    return name;
  }

  public long getId ()
  {
    return id;
  }

  public double getMaxCapacity ()
  {
    return maxCapacity;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Maximum battery capacity")
  public void setMaxCapacity (double capacity)
  {
    this.maxCapacity = capacity;
  }

  public double getRange ()
  {
    return range;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Maximum range in km")
  public void setRange (double range)
  {
    this.range = range;
  }

  public double getHomeChargeKW ()
  {
    return homeChargeKW;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Maximum charge rate in kw, home station")
  public void setHomeChargeKW (double kw)
  {
    this.homeChargeKW = kw;
  }

  public double getAwayChargeKW ()
  {
    return awayChargeKW;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Maximum charge rate in kw, remote station")
  public void setAwayChargeKW (double kw)
  {
    this.awayChargeKW = kw;
  }

  public double getCurtailmentFactor ()
  {
    return curtailmentFactor;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Expected curtailment per timeslot, as a ratio of homeChargeKW")
  public void setCurtailmentFactor (double ratio)
  {
    curtailmentFactor = ratio;
  }

  public double getDischargeFactor ()
  {
    return dischargeFactor;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Expected V2G per timeslot as a fn of homeChargeKW")
  public void setDischargeFactor (double ratio)
  {
    dischargeFactor = ratio;
  }

  public double getDownRegFactor ()
  {
    return downRegFactor;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "expected down regulation per timeslot as fn of homeChargeKW")
  public void setDownRegFactor (double ratio)
  {
    downRegFactor = ratio;
  }
}