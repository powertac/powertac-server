/*
 * Copyright (c) 2014 by Konstantina Valogiani, Govert Buijs, and John Collins
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.factoredcustomer.interfaces.StructureInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Singleton configuration class shared by all the factored-customer types
 *
 * @author John Collins, Govert Buijs
 */
public final class Config
{
  static private Logger log = LogManager.getLogger(Config.class.getName());

  // Singleton instance
  private static Config instance = null;

  // System configurator
  private ServerConfiguration serverConfiguration;

  @ConfigurableValue(valueType = "Boolean", dump = false,
      description = "Toggle logging of tariff allocation details")
  private boolean allocationDetailsLogging = true;

  @ConfigurableValue(valueType = "Boolean", dump = false,
      description = "Toogle logging of capacity adjustment details")
  private boolean capacityDetailsLogging = false;

  @ConfigurableValue(valueType = "Boolean", dump = false,
      description = "Toggle logging of expected usage charges")
  private boolean usageChargesLogging = false;

  @ConfigurableValue(valueType = "List", dump = false,
      description = "classnames of bean types to be configured")
  private List<String> structureTypes = new ArrayList<>();

  private Map<String, Map<String, StructureInstance>> structures;

  // Singleton constructor
  private Config (ServerConfiguration source)
  {
    super();
    serverConfiguration = source;
  }

  // =================== Accessors ====================
  public boolean isAllocationDetailsLogging ()
  {
    return allocationDetailsLogging;
  }

  public boolean isCapacityDetailsLogging ()
  {
    return capacityDetailsLogging;
  }

  public boolean isUsageChargesLogging ()
  {
    return usageChargesLogging;
  }

  // Just for testing
  public List<String> getStructureTypes ()
  {
    return structureTypes;
  }

  // =================== Configuration ================

  /**
   * Configures this instance from the given configuration service.
   */
  public void configure ()
  {
    if (null == serverConfiguration) {
      log.error("Config not initialized");
      return;
    }
    serverConfiguration.configureMe(this);
    structures = new HashMap<>();

    for (String classname : structureTypes) {
      Class<?> clazz;
      try {
        clazz = Class.forName("org.powertac.factoredcustomer." + classname);
        Collection<?> list = serverConfiguration.configureInstances(clazz);
        if (list == null) {
          structures.put(classname, new HashMap<>());
          log.info("No instances of " + classname);
          continue;
        }

        Map<String, StructureInstance> instanceMap = new LinkedHashMap<>();
        for (Object thing : list) {
          StructureInstance instance = (StructureInstance) thing;
          instanceMap.put(instance.getName(), instance);
        }
        structures.put(classname, instanceMap);
        log.info("Loaded " + list.size() + " instances of " + classname);
      }
      catch (ClassNotFoundException e) {
        e.printStackTrace();
        log.error("Cannot find class " + classname);
      }
    }
  }

  /**
   * Retrieves the list of configured beans
   */
  public Map<String, Map<String, StructureInstance>> getStructures ()
  {
    // already configured
    if (null == structures) {
      log.error("Call to getStructures() before initialization");
      return null;
    }
    return structures;
  }

  /**
   * Singleton accessor
   */
  public synchronized static Config getInstance ()
  {
    if (null == instance) {
      log.error("Unconfigured instance requested");
    }
    return instance;
  }

  public synchronized static void
  initializeInstance (ServerConfiguration configSource)
  {
    instance = new Config(configSource);
  }
}