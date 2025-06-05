/*
 * Copyright (c) 2012 by the original author
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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author John Collins
 */
public class PowerTypeTest
{

  @BeforeEach
  public void setUp () throws Exception
  {
  }

  /**
   * Test method for {@link org.powertac.common.enumerations.PowerType#isConsumption()}.
   */
  @Test
  public void testIsConsumption ()
  {
    PowerType pt = PowerType.CONSUMPTION;
    assertTrue(pt.isConsumption(), "is consumption 1");
    pt = PowerType.INTERRUPTIBLE_CONSUMPTION;
    assertTrue(pt.isConsumption(), "is consumption 2");
    pt = PowerType.THERMAL_STORAGE_CONSUMPTION;
    assertTrue(pt.isConsumption(), "is consumption 3");
    pt = PowerType.BATTERY_STORAGE;
    assertFalse(pt.isConsumption(), "not consumption 1");
    pt = PowerType.PRODUCTION;
    assertFalse(pt.isConsumption(), "not consumption 2");
   
  }

  /**
   * Test method for {@link org.powertac.common.enumerations.PowerType#isProduction()}.
   */
  @Test
  public void testIsProduction ()
  {
    PowerType pt = PowerType.PRODUCTION;
    assertTrue(pt.isProduction(), "is production 1");
    pt = PowerType.RUN_OF_RIVER_PRODUCTION;
    assertTrue(pt.isProduction(), "is production 2");
    pt = PowerType.SOLAR_PRODUCTION;
    assertTrue(pt.isProduction(), "is production 3");
    pt = PowerType.WIND_PRODUCTION;
    assertTrue(pt.isProduction(), "is production 4");
    pt = PowerType.BATTERY_STORAGE;
    assertFalse(pt.isProduction(), "not production 1");
    pt = PowerType.CONSUMPTION;
    assertFalse(pt.isProduction(), "not production 2");
  }

  /**
   * Test method for {@link org.powertac.common.enumerations.PowerType#isInterruptible()}.
   */
  @Test
  public void testIsInterruptible ()
  {
    PowerType pt = PowerType.INTERRUPTIBLE_CONSUMPTION;
    assertTrue(pt.isInterruptible(), "is interruptible 1");
    pt = PowerType.THERMAL_STORAGE_CONSUMPTION;
    assertTrue(pt.isInterruptible(), "is interruptible 2");
    pt = PowerType.CONSUMPTION;
    assertFalse(pt.isInterruptible(), "not interruptible 1");
    pt = PowerType.ELECTRIC_VEHICLE;
    assertTrue(pt.isInterruptible(), "is interruptible 3");
  }
  
  @Test
  public void testGenericType ()
  {
    assertEquals(PowerType.CONSUMPTION, PowerType.INTERRUPTIBLE_CONSUMPTION.getGenericType());
    assertEquals(PowerType.STORAGE, PowerType.THERMAL_STORAGE_CONSUMPTION.getGenericType());
    assertEquals(PowerType.CONSUMPTION, PowerType.CONSUMPTION.getGenericType());
    assertEquals(PowerType.PRODUCTION, PowerType.CHP_PRODUCTION.getGenericType());
    assertEquals(PowerType.PRODUCTION, PowerType.FOSSIL_PRODUCTION.getGenericType());
    assertEquals(PowerType.PRODUCTION, PowerType.PRODUCTION.getGenericType());
    assertEquals(PowerType.STORAGE, PowerType.BATTERY_STORAGE.getGenericType());
    //assertEquals(PowerType.STORAGE, PowerType.PUMPED_STORAGE_PRODUCTION.getGenericType());
    assertEquals(PowerType.STORAGE, PowerType.STORAGE.getGenericType());
  }
  
  /**
   * Test the "can-use" functionality
   */
  @Test
  public void testCanUse ()
  {
    assertTrue(PowerType.INTERRUPTIBLE_CONSUMPTION.canUse(PowerType.CONSUMPTION));
    assertFalse(PowerType.CONSUMPTION.canUse(PowerType.INTERRUPTIBLE_CONSUMPTION));
    assertTrue(PowerType.BATTERY_STORAGE.canUse(PowerType.STORAGE));
    assertFalse(PowerType.STORAGE.canUse(PowerType.BATTERY_STORAGE));
    assertTrue(PowerType.ELECTRIC_VEHICLE.canUse(PowerType.CONSUMPTION));
    assertTrue(PowerType.ELECTRIC_VEHICLE.canUse(PowerType.STORAGE));
    assertTrue(PowerType.ELECTRIC_VEHICLE.canUse(PowerType.INTERRUPTIBLE_CONSUMPTION));
    assertFalse(PowerType.ELECTRIC_VEHICLE.canUse(PowerType.BATTERY_STORAGE));
    assertFalse(PowerType.ELECTRIC_VEHICLE.canUse(PowerType.THERMAL_STORAGE_CONSUMPTION));
  }

  /**
   * Test method for {@link org.powertac.common.enumerations.PowerType#toString()}.
   */
  @Test
  public void testToString ()
  {
    PowerType pt = PowerType.INTERRUPTIBLE_CONSUMPTION;
    assertEquals("INTERRUPTIBLE_CONSUMPTION", pt.toString(), "Correct string");
  }

  /**
   * Test conversion from string
   */
  @Test
  public void testValueOf ()
  {
    assertEquals(PowerType.BATTERY_STORAGE,
                 PowerType.valueOf("BATTERY_STORAGE"));
    assertEquals(PowerType.INTERRUPTIBLE_CONSUMPTION,
                 PowerType.valueOf("INTERRUPTIBLE_CONSUMPTION"));
    assertEquals(PowerType.CONSUMPTION, PowerType.valueOf("CONSUMPTION"));
    assertNull(PowerType.valueOf("Blah"));
  }
}
