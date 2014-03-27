/*
 * Copyright 2013 the original author or authors.
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

package org.powertac.evcustomer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;


/**
 * @author Govert Buijs
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class ConfigTest
{
  @Test
  public void testStatic ()
  {
    assertEquals(2.7,   Config.EPSILON,                 1E-06);
    assertEquals(20,    Config.LAMDA,                   1E-06);
    assertEquals(100,   Config.PERCENTAGE,              1E-06);
    assertEquals(7,     Config.MEAN_TARIFF_DURATION);
    assertEquals(24,    Config.HOURS_OF_DAY);
    assertEquals(7,     Config.DAYS_OF_WEEK);
    assertEquals(14,    Config.DAYS_OF_BOOTSTRAP);
    assertEquals(0.05,  Config.TOU_FACTOR,              1E-06);
    assertEquals(0.5,   Config.INTERRUPTIBILITY_FACTOR, 1E-06);
    assertEquals(0.7,   Config.VARIABLE_PRICING_FACTOR, 1E-06);
    assertEquals(0.1,   Config.TIERED_RATE_FACTOR,      1E-06);
    assertEquals(1,     Config.MIN_DEFAULT_DURATION);
    assertEquals(3,     Config.MAX_DEFAULT_DURATION);
    assertEquals(Config.MAX_DEFAULT_DURATION - Config.MIN_DEFAULT_DURATION,
                 Config.DEFAULT_DURATION_WINDOW);
    assertEquals(0.9,   Config.RATIONALITY_FACTOR,      1E-06);
    assertEquals(5,     Config.TARIFF_COUNT);
    assertEquals(0.02,  Config.BROKER_SWITCH_FACTOR,    1E-06);
    assertEquals(1,     Config.WEIGHT_INCONVENIENCE,    1E-06);
    assertEquals(0.9,   Config.NSInertia,               1E-06);
  }
}
