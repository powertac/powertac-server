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

import org.junit.Before;
import org.junit.Test;
import org.powertac.factoredcustomer.interfaces.StructureInstance;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author Govert Buijs
 */
public class ConfigTest
{
  private DummyConfig configSvc;

  @Before
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
    assertNotNull("Config created", item);
  }

  /**
   * Test property config
   */
  @Test
  public void testPropertyConfig ()
  {
    Config config = Config.getInstance();
    config.configure();

    assertEquals("ConfiguredValue allocationDetailsLogging",
        config.isAllocationDetailsLogging(), true);
    assertEquals("ConfiguredValue capacityDetailsLogging",
        config.isCapacityDetailsLogging(), false);
    assertEquals("ConfiguredValue usageChargesLogging",
        config.isUsageChargesLogging(), false);
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

    assertEquals("Structure size", names.size(), 7);
    assertEquals("Structure CapacityStructure",
        names.contains("CapacityStructure"), true);
    assertEquals("Structure CustomerStructure",
        names.contains("CustomerStructure"), true);
    assertEquals("Structure DefaultCapacityBundle",
        names.contains("DefaultCapacityBundle"), true);
    assertEquals("Structure ProbabilityDistribution",
        names.contains("ProbabilityDistribution"), true);
    assertEquals("Structure TariffSubscriberStructure",
        names.contains("TariffSubscriberStructure"), true);
    assertEquals("Structure TimeseriesGenerator",
        names.contains("TimeseriesGenerator"), true);
    // Empty
    assertEquals("Structure ProfileOptimizerStructure",
        names.contains("ProfileOptimizerStructure"), true);
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
    assertEquals("4 Customer Structures", 4, customerNames.size());
    assertEquals("BrooksideHomes Customer Structures",
        customerNames.first(), "BrooksideHomes");
    assertEquals("WindmillCoOp Customer Structures",
        customerNames.last(), "WindmillCoOp");

    // TariffSubscriber structures
    SortedSet<String> subscriberNames = new TreeSet<>();
    subscriberNames.addAll(structures.get("TariffSubscriberStructure").keySet());
    assertEquals("6 Subscriber Structures", 6, subscriberNames.size());
    assertEquals("BrooksideHomes Subscriber Structures",
        subscriberNames.first(), "BrooksideHomes");
    assertEquals("WindmillCoOp Subscriber Structures",
        subscriberNames.last(), "WindmillCoOp-2");

    // Capacity structures
    SortedSet<String> capacityNames = new TreeSet<>();
    capacityNames.addAll(structures.get("CapacityStructure").keySet());
    assertEquals("8 Capacity Structures", 8, capacityNames.size());
    assertEquals("BrooksideHomes Capacity Structures",
        capacityNames.first(), "BrooksideHomes");
    assertEquals("WindmillCoOp Capacity Structures",
        capacityNames.last(), "WindmillCoOp-2");

    // DefaultCapacityBundle structures
    SortedSet<String> bundleNames = new TreeSet<>();
    bundleNames.addAll(structures.get("DefaultCapacityBundle").keySet());
    assertEquals("6 Bundle Structures", 6, bundleNames.size());
    assertEquals("BrooksideHomes Bundle Structures",
        bundleNames.first(), "BrooksideHomes");
    assertEquals("WindmillCoOp Bundle Structures",
        bundleNames.last(), "WindmillCoOp-2");

    // ProbabilityDistribution structures
    SortedSet<String> distNames = new TreeSet<>();
    distNames.addAll(structures.get("ProbabilityDistribution").keySet());
    assertEquals("13 Distribution Structures", 13, distNames.size());
    assertEquals("BrooksideHomesInertia Bundle Structures",
        distNames.first(), "BrooksideHomesInertia");
    assertEquals("WindmillCoOp-2Population Bundle Structures",
        distNames.last(), "WindmillCoOp-2Population");

    // TimeseriesGenerator structures
    SortedSet<String> seriesNames = new TreeSet<>();
    seriesNames.addAll(structures.get("TimeseriesGenerator").keySet());
    assertEquals("1 Timeseries Structures", 1, seriesNames.size());
    assertEquals("BrooksideHomesPopulation Timeseries Structures",
        seriesNames.first(), "BrooksideHomesPopulation");


    // ProfileOptimizerStructure structures
    SortedSet<String> optimizerNames = new TreeSet<>();
    optimizerNames.addAll(structures.get("ProfileOptimizerStructure").keySet());
    assertEquals("0 Timeseries Structures", 0, optimizerNames.size());
  }
}
