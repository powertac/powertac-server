/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.common.enumerations;

import java.util.HashMap;

import org.powertac.common.xml.PowerTypeConverter;

import com.thoughtworks.xstream.annotations.XStreamConverter;

/**
 * Types of power a Customer can produce or consume. A single Customer may buy
 * or sell multiple PowerTypes.
 * 
 * This class is intended to behave like an enum for existing code.
 * 
 * @author John Collins
 */
@XStreamConverter(PowerTypeConverter.class)
public class PowerType
{
  // The order of static elements, including the enum, must be preserved, 
  // because they are processed in order of occurrence.
  private enum TypeLabel {
  CONSUMPTION,
  PRODUCTION,
  STORAGE,
  INTERRUPTIBLE_CONSUMPTION,
  THERMAL_STORAGE_CONSUMPTION,
  SOLAR_PRODUCTION, WIND_PRODUCTION,
  RUN_OF_RIVER_PRODUCTION,
  PUMPED_STORAGE_PRODUCTION,
  CHP_PRODUCTION,
  FOSSIL_PRODUCTION,
  BATTERY_STORAGE,
  ELECTRIC_VEHICLE
  }

  // index to convert strings to PowerType instances
  private static HashMap<String, PowerType> index = 
      new HashMap<String, PowerType>();

  // These are the possible values. You cannot create others, because the
  // constructor is private.
  public static final PowerType CONSUMPTION =
      new PowerType(TypeLabel.CONSUMPTION);
  public static final PowerType PRODUCTION =
      new PowerType(TypeLabel.PRODUCTION);
  public static final PowerType STORAGE =
      new PowerType(TypeLabel.STORAGE);
  public static final PowerType INTERRUPTIBLE_CONSUMPTION =
      new PowerType(TypeLabel.INTERRUPTIBLE_CONSUMPTION);
  public static final PowerType THERMAL_STORAGE_CONSUMPTION =
      new PowerType(TypeLabel.THERMAL_STORAGE_CONSUMPTION);
  public static final PowerType SOLAR_PRODUCTION =
      new PowerType(TypeLabel.SOLAR_PRODUCTION);
  public static final PowerType WIND_PRODUCTION =
      new PowerType(TypeLabel.WIND_PRODUCTION);
  public static final PowerType RUN_OF_RIVER_PRODUCTION =
      new PowerType(TypeLabel.RUN_OF_RIVER_PRODUCTION);
  public static final PowerType PUMPED_STORAGE_PRODUCTION =
      new PowerType(TypeLabel.PUMPED_STORAGE_PRODUCTION);
  public static final PowerType CHP_PRODUCTION =
      new PowerType(TypeLabel.CHP_PRODUCTION);
  public static final PowerType FOSSIL_PRODUCTION =
      new PowerType(TypeLabel.FOSSIL_PRODUCTION);
  public static final PowerType BATTERY_STORAGE =
      new PowerType(TypeLabel.BATTERY_STORAGE);
  public static final PowerType ELECTRIC_VEHICLE =
      new PowerType(TypeLabel.ELECTRIC_VEHICLE);

  // This is the instance data field.
  private TypeLabel label;
  
  /**
   * Private constructor, used only to create enumeration instances.
   */
  private PowerType (TypeLabel label)
  {
    super();
    this.label = label;
    index.put(label.toString(), this);
  }

  /**
   * Returns true just in case this type would apply to a consumption tariff.
   */
  public boolean isConsumption ()
  {
    return (label == TypeLabel.CONSUMPTION
            || label == TypeLabel.ELECTRIC_VEHICLE
            || label == TypeLabel.INTERRUPTIBLE_CONSUMPTION
            || label == TypeLabel.THERMAL_STORAGE_CONSUMPTION);
  }

  /**
   * Returns true just in case this type would apply to a production tariff.
   */
  public boolean isProduction ()
  {
    return (label == TypeLabel.PRODUCTION
            || label == TypeLabel.CHP_PRODUCTION
            || label == TypeLabel.FOSSIL_PRODUCTION
            || label == TypeLabel.RUN_OF_RIVER_PRODUCTION
            || label == TypeLabel.SOLAR_PRODUCTION
            || label == TypeLabel.WIND_PRODUCTION);
  }
  
  /**
   * Returns true just in case this powerType is a supertype (can use) 
   * the tariffType.
   */
  public boolean canUse (PowerType tariffType)
  {
    return (this.equals(tariffType)
            || (isConsumption() && tariffType.label == TypeLabel.CONSUMPTION)
            || (isProduction() && tariffType.label == TypeLabel.PRODUCTION)
            || (isStorage() && tariffType.label == TypeLabel.STORAGE)
            || (isInterruptible() && tariffType.label == TypeLabel.INTERRUPTIBLE_CONSUMPTION));
  }
  
  /**
   * Returns true just in case this type is interruptible.
   */
  public boolean isInterruptible ()
  {
    return (label == TypeLabel.INTERRUPTIBLE_CONSUMPTION
            || label == TypeLabel.THERMAL_STORAGE_CONSUMPTION
            || label == TypeLabel.BATTERY_STORAGE
            || label == TypeLabel.ELECTRIC_VEHICLE);
  }
  
  /**
   * Returns true just in case this type is a storage type.
   */
  public boolean isStorage ()
  {
    return (label == TypeLabel.STORAGE
            || label == TypeLabel.THERMAL_STORAGE_CONSUMPTION
            || label == TypeLabel.BATTERY_STORAGE
            || label == TypeLabel.ELECTRIC_VEHICLE
            || label == TypeLabel.PUMPED_STORAGE_PRODUCTION);
  }
  
  /**
   * Returns the most-specific capacity type for a given PowerType.
   * Note that this assumes the Default Broker only issues defaults
   * for specific types.
   */
  public PowerType getGenericType ()
  {
    if (isStorage())
      return STORAGE;
    else if (isConsumption())
      return CONSUMPTION;
    else if (isProduction())
      return PRODUCTION;
    else
      return null;
  }

  @Override
  public String toString ()
  {
    return label.toString();
  }
  
  public static PowerType valueOf (String name)
  {
    return index.get(name);
  }
}
