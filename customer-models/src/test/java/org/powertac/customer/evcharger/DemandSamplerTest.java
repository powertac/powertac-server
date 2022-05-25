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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Philipp Page <github@philipp-page.de>
 */
class DemandSamplerTest
{
  private static DemandSampler demandSampler;
  private static final int POP_SIZE = 1000;
  private static final double CHARGER_CAPACITY = 7.2;

  @BeforeAll
  public static void beforeAll ()
  {
    demandSampler = new DemandSampler();
    demandSampler.initialize();
  }

  @BeforeEach
  public void beforeEach ()
  {
    demandSampler.setSeed(42);
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
    demandSampler.setSeed(1337);
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
    double[][] horizonEnergyTuples = demandSampler.sampleHorizonEnergyTuples(n, 16);
    assertEquals(horizonEnergyTuples.length, 100);

    for (double[] tuple: horizonEnergyTuples) {
      assertEquals(tuple.length, 2);
    }
  }

  @Test
  public void testSampleHorizonEnergyTuplesAreNonNegative ()
  {
    final int n = 100;
    double[][] horizonEnergyTuples = demandSampler.sampleHorizonEnergyTuples(n, 16);
    assertEquals(horizonEnergyTuples.length, 100);

    for (double[] tuple: horizonEnergyTuples) {
      assertTrue(tuple[0] >= 0);
      assertTrue(tuple[1] >= 0);
    }
  }

  @Test
  public void testSampleHorizonEnergyTuplesThrowsIllegalArgumentExceptionIfHodIsInvalid ()
  {
    final int n = 100;

    // hod must be in [0, 23]
    assertThrows(IllegalArgumentException.class, () -> demandSampler.sampleHorizonEnergyTuples(n, 24));
    assertThrows(IllegalArgumentException.class, () -> demandSampler.sampleHorizonEnergyTuples(n, -1));
    assertThrows(IllegalArgumentException.class, () -> demandSampler.sampleHorizonEnergyTuples(n, 300));
    assertThrows(IllegalArgumentException.class, () -> demandSampler.sampleHorizonEnergyTuples(n, -300));
  }

  // For robustness we run this for different random seeds
  @ParameterizedTest
  @ValueSource(ints = { 42, 43, 1337 })
  public void testSampleDemandElement (int seedValue)
  {
    final int hod = 16;
    demandSampler.setSeed(seedValue);
    final int expectedNumberOfPlugins = (int) demandSampler.sampleNewPlugins(hod, POP_SIZE);
    final double[][] expectedHorizonEnergyTuples =
      demandSampler.sampleHorizonEnergyTuples(expectedNumberOfPlugins, hod);

    final int expectedMaxHorizon = Arrays.stream(expectedHorizonEnergyTuples)
            .mapToInt(horizonEnergyTuple -> (int) horizonEnergyTuple[0]).max().getAsInt();
    final double expectedMaxEnergy = Arrays.stream(expectedHorizonEnergyTuples)
            .mapToDouble(horizonEnergyTuple -> horizonEnergyTuple[1]).max().getAsDouble();
    final int expectedMaxChargerHours = (int) (expectedMaxEnergy / CHARGER_CAPACITY);
    final double expectedTotalEnergy =
      Arrays.stream(expectedHorizonEnergyTuples).mapToDouble(horizonEnergyTuple -> horizonEnergyTuple[1]).sum();

    List<DemandElement> demandElements = demandSampler.sample(hod, POP_SIZE, CHARGER_CAPACITY);

    assertEquals(expectedNumberOfPlugins,
                 demandElements.stream().mapToInt(demandElement -> (int) demandElement.getNVehicles()).sum());
    // There should be maxHorizon + 1 DemandElements each timeslot
    assertEquals(expectedMaxHorizon + 1, demandElements.size());
    // Each of these elements must have a distribution which is a double[] where
    // the index is the amount of charger hours needed and the value the number
    // of vehicles in that group. These should be maxChargerHours + 1 elements.
    for (DemandElement demandElement: demandElements) {
      assertEquals(expectedMaxChargerHours + 1, demandElement.getDistribution().length);
    }
    // The total energy of all DemandElement instances should be the sum of
    // energy of horizonEnergyTuple samples. We set double precision to 1e-4.
    assertEquals(expectedTotalEnergy,
                 demandElements.stream().mapToDouble(demandElement -> demandElement.getEnergy()).sum(), 1e-4);
  }

  @Test
  public void testReturnsDefaultValuesIfSamplerIsDisabled ()
  {
    DemandSampler disabledDemandSampler = new DemandSampler();
    ReflectionTestUtils.setField(disabledDemandSampler, "enabled", false);
    disabledDemandSampler.initialize();
    assertArrayEquals(new double[][] {}, disabledDemandSampler.sampleHorizonEnergyTuples(100, 16));
    assertEquals(0.0, disabledDemandSampler.sampleNewPlugins(16, POP_SIZE));
    assertEquals(new ArrayList<DemandElement>(), disabledDemandSampler.sample(16, POP_SIZE, CHARGER_CAPACITY));
  }
}
