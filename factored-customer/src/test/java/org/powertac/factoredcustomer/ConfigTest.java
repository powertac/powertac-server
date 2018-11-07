/*
 * Copyright (c) 2014 by the original author
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
package org.powertac.factoredcustomer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.factoredcustomer.interfaces.StructureInstance;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * @author Govert Buijs
 */
public class ConfigTest
{
  private DummyConfig configSvc;

  @BeforeEach
  public void setUp ()
  {
    configSvc = new DummyConfig();
    configSvc.initialize();
    Config.initializeInstance(configSvc);
  }

  @Test
  public void testGetInstance ()
  {
    Config item = Config.getInstance();
    assertNotNull(item, "Config created");
  }

  /**
   * Test property config
   */
  @Test
  public void testPropertyConfig ()
  {
    Config config = Config.getInstance();
    config.configure();

    assertEquals(config.isAllocationDetailsLogging(), true, "ConfiguredValue allocationDetailsLogging");
    assertEquals(config.isCapacityDetailsLogging(), false, "ConfiguredValue capacityDetailsLogging");
    assertEquals(config.isUsageChargesLogging(), false, "ConfiguredValue usageChargesLogging");
  }

  /**
   * Test retrieval by key
   */
  @Test
  public void testStructureKeys ()
  {
    Config config = Config.getInstance();
    config.configure();

    Set<String> names = config.getStructures().keySet();

    assertEquals(names.size(), 7, "Structure size");
    assertEquals(names.contains("CapacityStructure"), true, "Structure CapacityStructure");
    assertEquals(names.contains("CustomerStructure"), true, "Structure CustomerStructure");
    assertEquals(names.contains("DefaultCapacityBundle"), true, "Structure DefaultCapacityBundle");
    assertEquals(names.contains("ProbabilityDistribution"), true, "Structure ProbabilityDistribution");
    assertEquals(names.contains("TariffSubscriberStructure"), true, "Structure TariffSubscriberStructure");
    assertEquals(names.contains("TimeseriesGenerator"), true, "Structure TimeseriesGenerator");
    // Empty
    assertEquals(names.contains("ProfileOptimizerStructure"), true, "Structure ProfileOptimizerStructure");
  }

  @Test
  public void testStructureConfig ()
  {
    Config config = Config.getInstance();
    config.configure();
    Map<String, Map<String, StructureInstance>> structures = config.getStructures();

    // Customer structures
    SortedSet<String> customerNames = new TreeSet<>();
    customerNames.addAll(structures.get("CustomerStructure").keySet());
    assertEquals(4, customerNames.size(), "4 Customer Structures");
    assertEquals(customerNames.first(), "BrooksideHomes", "BrooksideHomes Customer Structures");
    assertEquals(customerNames.last(), "WindmillCoOp", "WindmillCoOp Customer Structures");

    // TariffSubscriber structures
    SortedSet<String> subscriberNames = new TreeSet<>();
    subscriberNames.addAll(structures.get("TariffSubscriberStructure").keySet());
    assertEquals(6, subscriberNames.size(), "6 Subscriber Structures");
    assertEquals(subscriberNames.first(), "BrooksideHomes", "BrooksideHomes Subscriber Structures");
    assertEquals(subscriberNames.last(), "WindmillCoOp-2", "WindmillCoOp Subscriber Structures");

    // Capacity structures
    SortedSet<String> capacityNames = new TreeSet<>();
    capacityNames.addAll(structures.get("CapacityStructure").keySet());
    assertEquals(8, capacityNames.size(), "8 Capacity Structures");
    assertEquals(capacityNames.first(), "BrooksideHomes", "BrooksideHomes Capacity Structures");
    assertEquals(capacityNames.last(), "WindmillCoOp-2", "WindmillCoOp Capacity Structures");

    // DefaultCapacityBundle structures
    SortedSet<String> bundleNames = new TreeSet<>();
    bundleNames.addAll(structures.get("DefaultCapacityBundle").keySet());
    assertEquals(6, bundleNames.size(), "6 Bundle Structures");
    assertEquals(bundleNames.first(), "BrooksideHomes", "BrooksideHomes Bundle Structures");
    assertEquals(bundleNames.last(), "WindmillCoOp-2", "WindmillCoOp Bundle Structures");

    // ProbabilityDistribution structures
    SortedSet<String> distNames = new TreeSet<>();
    distNames.addAll(structures.get("ProbabilityDistribution").keySet());
    assertEquals(13, distNames.size(), "13 Distribution Structures");
    assertEquals(distNames.first(), "BrooksideHomesInertia", "BrooksideHomesInertia Bundle Structures");
    assertEquals(distNames.last(), "WindmillCoOp-2Population", "WindmillCoOp-2Population Bundle Structures");

    // TimeseriesGenerator structures
    SortedSet<String> seriesNames = new TreeSet<>();
    seriesNames.addAll(structures.get("TimeseriesGenerator").keySet());
    assertEquals(1, seriesNames.size(), "1 Timeseries Structures");
    assertEquals(seriesNames.first(), "BrooksideHomesPopulation", "BrooksideHomesPopulation Timeseries Structures");


    // ProfileOptimizerStructure structures
    SortedSet<String> optimizerNames = new TreeSet<>();
    optimizerNames.addAll(structures.get("ProfileOptimizerStructure").keySet());
    assertEquals(0, optimizerNames.size(), "0 Timeseries Structures");
  }
}
