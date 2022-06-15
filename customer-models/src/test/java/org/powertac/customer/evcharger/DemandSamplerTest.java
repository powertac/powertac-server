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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.powertac.common.RandomSeed;
import org.powertac.common.repo.RandomSeedRepo;
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
    demandSampler.initialize("residential_ev_1.xml");
  }

  @BeforeEach
  public void beforeEach ()
  {
    demandSampler.setCurrentSeed(42);
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
    demandSampler.setCurrentSeed(1337);
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
  @ValueSource(ints = { 1, 2, 3 })
  public void testSampleDemandElement (int seedValue)
  {
    final int hod = 16;
    demandSampler.setCurrentSeed(seedValue);
    final int expectedNumberOfPlugins = (int) demandSampler.sampleNewPlugins(hod, POP_SIZE);
    final double[][] expectedHorizonEnergyTuples =
      demandSampler.sampleHorizonEnergyTuples(expectedNumberOfPlugins, hod);

    final int expectedMaxHorizon = Arrays.stream(expectedHorizonEnergyTuples)
            .mapToInt(horizonEnergyTuple -> (int) horizonEnergyTuple[0]).max().getAsInt();

    List<DemandElement> demandElements = demandSampler.sample(hod, POP_SIZE, CHARGER_CAPACITY);

    assertEquals(expectedNumberOfPlugins,
                 demandElements.stream().mapToInt(demandElement -> (int) demandElement.getNVehicles()).sum());
    // There should be maxHorizon + 1 DemandElements each timeslot
    assertEquals(expectedMaxHorizon + 1, demandElements.size());
    // Each of these elements must have a distribution which is a double[] where
    // the index is the amount of charger hours needed and the value the number
    // of vehicles in that group. These should be maxChargerHours + 1 elements.
    for (DemandElement demandElement: demandElements) {
      assertEquals(demandElement.getHorizon() + 1, demandElement.getdistribution().length);
      // The distribution is not allowed to contain vehicles who need more charger hours 
      // than the max horizon allows.
      for (int i = demandElement.getHorizon() + 1; i < demandElement.getdistribution().length; i++) {
        assertEquals(0.0, demandElement.getdistribution()[i]);
      }
    }
  }

  @Test
  public void testReturnsDefaultValuesIfSamplerIsDisabled ()
  {
    DemandSampler disabledDemandSampler = new DemandSampler();
    ReflectionTestUtils.setField(disabledDemandSampler, "enabled", false);
    disabledDemandSampler.initialize("residential_ev_1.xml");
    // Even though we try to sample 100 tuples for 16 o'clock we expect the
    // default value to be returned because the model is disabled
    assertArrayEquals(new double[][] {}, disabledDemandSampler.sampleHorizonEnergyTuples(100, 16));
    assertEquals(0.0, disabledDemandSampler.sampleNewPlugins(16, POP_SIZE));
    assertEquals(new ArrayList<DemandElement>(), disabledDemandSampler.sample(16, POP_SIZE, CHARGER_CAPACITY));
  }

  @Test
  public void testSamplerGetsDisabledIfConfigPathIsWrong ()
  {
    DemandSampler wrongConfigPathDemandSampler = new DemandSampler();
    wrongConfigPathDemandSampler.initialize("Some wrong config path.xml");
    assertFalse(wrongConfigPathDemandSampler.isEnabled());
  }

  // This test makes sure that the plug-in probability has actual density mass
  // within the interval [0, 23]
  @ParameterizedTest
  @ValueSource(ints = { 1, 2, 3 })
  public void testPluginDensitySumsUpCloseToOne (int seedValue)
  {
    demandSampler.setCurrentSeed(seedValue);
    double probabilitySum = 0.0;
    for (int i = 0; i < 24; i++) {
      // We divide by POP_SIZE to reduce the absolute number to a probability
      // again.
      probabilitySum += demandSampler.sampleNewPlugins(i, POP_SIZE) / POP_SIZE;
    }
    // We allow for a tolerance of alpha = 0.05 which means that 5% of the
    // density are allowed to lie outside of the interval [0, 23] on each side.
    assertTrue(probabilitySum >= 0.9);
  }

  @Test
  public void testDemandSamplerReturnsReproducibleSequenceIfRandomSeedIsGiven ()
  {
    String model = "residential_ev_1.xml";
    RandomSeedRepo randomSeedRepo = new RandomSeedRepo();

    RandomSeed demandSeed1 =
      randomSeedRepo.getRandomSeed(EvCharger.class.getName(), POP_SIZE, model);
    // Sample a sequence of DemandElement lists.
    DemandSampler randomSeedDemandSampler1 = new DemandSampler();
    randomSeedDemandSampler1.initialize(model, demandSeed1);
    assertTrue(randomSeedDemandSampler1.isEnabled());
    List<DemandElement> demandElements11 =
      randomSeedDemandSampler1.sample(16, POP_SIZE, CHARGER_CAPACITY);
    List<DemandElement> demandElements12 =
      randomSeedDemandSampler1.sample(17, POP_SIZE, CHARGER_CAPACITY);
    // Same hod as demandElements11
    List<DemandElement> demandElements13 =
      randomSeedDemandSampler1.sample(16, POP_SIZE, CHARGER_CAPACITY);

    RandomSeed demandSeed2 =
      randomSeedRepo.getRandomSeed(EvCharger.class.getName(), POP_SIZE, model);
    // Sample a sequence of DemandElement lists with a different
    // DemandSampler but the same demandSeed from the repo.
    DemandSampler randomSeedDemandSampler2 = new DemandSampler();
    randomSeedDemandSampler2.initialize(model, demandSeed2);
    assertTrue(randomSeedDemandSampler2.isEnabled());
    List<DemandElement> demandElements21 =
      randomSeedDemandSampler2.sample(16, POP_SIZE, CHARGER_CAPACITY);
    List<DemandElement> demandElements22 =
      randomSeedDemandSampler2.sample(17, POP_SIZE, CHARGER_CAPACITY);
    // Same hod as demandElements21
    List<DemandElement> demandElements23 =
      randomSeedDemandSampler2.sample(16, POP_SIZE, CHARGER_CAPACITY);

    // Both independent sequences must be equal
    assertEquals(demandElements11.size(), demandElements21.size());
    for (int i = 0; i < demandElements11.size(); i++) {
      assertEquals(demandElements11.get(i), demandElements21.get(i));
    }
    assertEquals(demandElements12.size(), demandElements22.size());
    for (int i = 0; i < demandElements12.size(); i++) {
      assertEquals(demandElements12.get(i), demandElements22.get(i));
    }
    assertEquals(demandElements13.size(), demandElements23.size());
    for (int i = 0; i < demandElements13.size(); i++) {
      assertEquals(demandElements13.get(i), demandElements23.get(i));
    }

    // But the same DemandSampler should not repeat samples for the same
    // parameters.
    assertNotEquals(demandElements11, demandElements13);
    assertNotEquals(demandElements21, demandElements23);
  }
}
