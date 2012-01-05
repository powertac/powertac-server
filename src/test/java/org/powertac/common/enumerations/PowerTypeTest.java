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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * @author John Collins
 */
public class PowerTypeTest
{

  @Before
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
    assertTrue("is consumption 1", pt.isConsumption());
    pt = PowerType.INTERRUPTIBLE_CONSUMPTION;
    assertTrue("is consumption 2", pt.isConsumption());
    pt = PowerType.THERMAL_STORAGE_CONSUMPTION;
    assertTrue("is consumption 3", pt.isConsumption());
    pt = PowerType.BATTERY_STORAGE;
    assertFalse("not consumption 1", pt.isConsumption());
    pt = PowerType.PRODUCTION;
    assertFalse("not consumption 2", pt.isConsumption());
   
  }

  /**
   * Test method for {@link org.powertac.common.enumerations.PowerType#isProduction()}.
   */
  @Test
  public void testIsProduction ()
  {
    PowerType pt = PowerType.PRODUCTION;
    assertTrue("is production 1", pt.isProduction());
    pt = PowerType.RUN_OF_RIVER_PRODUCTION;
    assertTrue("is production 2", pt.isProduction());
    pt = PowerType.SOLAR_PRODUCTION;
    assertTrue("is production 3", pt.isProduction());
    pt = PowerType.WIND_PRODUCTION;
    assertTrue("is production 4", pt.isProduction());
    pt = PowerType.BATTERY_STORAGE;
    assertFalse("not production 1", pt.isProduction());
    pt = PowerType.CONSUMPTION;
    assertFalse("not production 2", pt.isProduction());
  }

  /**
   * Test method for {@link org.powertac.common.enumerations.PowerType#isInterruptible()}.
   */
  @Test
  public void testIsInterruptible ()
  {
    PowerType pt = PowerType.INTERRUPTIBLE_CONSUMPTION;
    assertTrue("is interruptible 1", pt.isInterruptible());
    pt = PowerType.THERMAL_STORAGE_CONSUMPTION;
    assertTrue("is interruptible 2", pt.isInterruptible());
    pt = PowerType.CONSUMPTION;
    assertFalse("not interruptible 1", pt.isInterruptible());
  }
  
  @Test
  public void testGenericType ()
  {
    assertEquals(PowerType.CONSUMPTION, PowerType.INTERRUPTIBLE_CONSUMPTION.getGenericType());
    assertEquals(PowerType.CONSUMPTION, PowerType.THERMAL_STORAGE_CONSUMPTION.getGenericType());
    assertEquals(PowerType.CONSUMPTION, PowerType.CONSUMPTION.getGenericType());
    assertEquals(PowerType.PRODUCTION, PowerType.CHP_PRODUCTION.getGenericType());
    assertEquals(PowerType.PRODUCTION, PowerType.FOSSIL_PRODUCTION.getGenericType());
    assertEquals(PowerType.PRODUCTION, PowerType.PRODUCTION.getGenericType());
    assertEquals(PowerType.STORAGE, PowerType.BATTERY_STORAGE.getGenericType());
    assertEquals(PowerType.STORAGE, PowerType.PUMPED_STORAGE_PRODUCTION.getGenericType());
    assertEquals(PowerType.STORAGE, PowerType.STORAGE.getGenericType());
  }

  /**
   * Test method for {@link org.powertac.common.enumerations.PowerType#toString()}.
   */
  @Test
  public void testToString ()
  {
    PowerType pt = PowerType.INTERRUPTIBLE_CONSUMPTION;
    assertEquals("Correct string", "INTERRUPTIBLE_CONSUMPTION", pt.toString());
  }

}
