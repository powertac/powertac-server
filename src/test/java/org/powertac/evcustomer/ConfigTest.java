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
 * @version 0.5, Date: 2013.11.28
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class ConfigTest
{
  @Test
  public void testStatic ()
  {
    assertEquals(Config.EPSILON, 2.7, 1E-06);
    assertEquals(Config.LAMDA, 20, 1E-06);
    assertEquals(Config.PERCENTAGE, 100, 1E-06);
    assertEquals(Config.MEAN_TARIFF_DURATION, 7);
    assertEquals(Config.HOURS_OF_DAY, 24);
    assertEquals(Config.DAYS_OF_WEEK, 7);
    assertEquals(Config.DAYS_OF_BOOTSTRAP, 14);
    assertEquals(Config.TOU_FACTOR, 0.05, 1E-06);
    assertEquals(Config.INTERRUPTIBILITY_FACTOR, 0.5, 1E-06);
    assertEquals(Config.VARIABLE_PRICING_FACTOR, 0.7, 1E-06);
    assertEquals(Config.TIERED_RATE_FACTOR, 0.1, 1E-06);
    assertEquals(Config.MIN_DEFAULT_DURATION, 1);
    assertEquals(Config.MAX_DEFAULT_DURATION, 3);
    assertEquals(Config.DEFAULT_DURATION_WINDOW,
        Config.MAX_DEFAULT_DURATION - Config.MIN_DEFAULT_DURATION);
    assertEquals(Config.RATIONALITY_FACTOR, 0.9, 1E-06);
    assertEquals(Config.TARIFF_COUNT, 5);
    assertEquals(Config.BROKER_SWITCH_FACTOR, 0.02, 1E-06);
    assertEquals(Config.WEIGHT_INCONVENIENCE, 1, 1E-06);
    assertEquals(Config.NSInertia, 0.9, 1E-06);
  }
}
