/**
 * Copyright (c) 2022 by John Collins.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.customer.evcharger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Philipp Page <github@philipp-page.de>
 */
class DemandSamplerTest
{
  private static DemandSampler demandSampler;
  private static final int POP_SIZE = 1000;

  @BeforeAll
  static void beforeAll ()
  {
    demandSampler = new DemandSampler();
    demandSampler.initialize();
  }

  @Test
  public void testSampleNewPlugins ()
  {
    final double newPlugins = demandSampler.sampleNewPlugins(16, POP_SIZE);
    assertTrue(newPlugins <= POP_SIZE);
    assertTrue(newPlugins >= 0);
  }

  @Test
  public void testSampleNewPluginsAreRandom ()
  {
    final double newPlugins1 = demandSampler.sampleNewPlugins(16, POP_SIZE);
    final double newPlugins2 = demandSampler.sampleNewPlugins(16, POP_SIZE);
    assertNotEquals(newPlugins1, newPlugins2);
  }

  @Test
  public void testSampleNewPluginsNightIsLessThanDay ()
  {
    final double newPluginsDay = demandSampler.sampleNewPlugins(16, POP_SIZE);
    final double newPluginsNight = demandSampler.sampleNewPlugins(2, POP_SIZE);
    assertTrue(newPluginsNight < newPluginsDay);
  }

  @Test
  public void testSampleHorizonEnergyTuplesHaveCorrectDimension ()
  {
    final int n = 100;
    double[][] horizonEnergyTuples =
      demandSampler.sampleHorizonEnergyTuples(n, 16);
    assertEquals(horizonEnergyTuples.length, 100);

    for (double[] tuple: horizonEnergyTuples) {
      assertEquals(tuple.length, 2);
    }
  }

  @Test
  public void testSampleHorizonEnergyTuplesAreNonNegative ()
  {
    final int n = 100;
    double[][] horizonEnergyTuples =
      demandSampler.sampleHorizonEnergyTuples(n, 16);
    assertEquals(horizonEnergyTuples.length, 100);

    for (double[] tuple: horizonEnergyTuples) {
      assertTrue(tuple[0] >= 0);
      assertTrue(tuple[1] >= 0);
    }
  }

  @Test
  public void
    testSampleHorizonEnergyTuplesThrowsIllegalArgumentExceptionIfHodIsInvalid ()
  {
    final int n = 100;

    // hod must be in [0, 23]
    assertThrows(IllegalArgumentException.class,
                 () -> demandSampler.sampleHorizonEnergyTuples(n, 24));
    assertThrows(IllegalArgumentException.class,
                 () -> demandSampler.sampleHorizonEnergyTuples(n, -1));
    assertThrows(IllegalArgumentException.class,
                 () -> demandSampler.sampleHorizonEnergyTuples(n, 300));
    assertThrows(IllegalArgumentException.class,
                 () -> demandSampler.sampleHorizonEnergyTuples(n, -300));
  }
}
