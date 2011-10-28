/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.common.repo;

import java.util.ArrayList;
import java.util.List;

import org.powertac.common.PluginConfig;
import org.springframework.stereotype.Repository;

/**
 * Stores PluginConfig instances, allows access to the list.
 * @author John Collins
 */
@Repository
public class PluginConfigRepo implements DomainRepo
{
  // no need for ids here, just a list
  private ArrayList<PluginConfig> storage;
  
  public PluginConfigRepo ()
  {
    super();
    storage = new ArrayList<PluginConfig>();
  }

  public PluginConfig makePluginConfig (String role, String name)
  {
    PluginConfig result = new PluginConfig(role, name);
    storage.add(result);
    return result;
  }
  
  /**
   * Returns the list of instances in the repo.
   */
  public List<PluginConfig> list ()
  {
    return storage;
  }
  
  /**
   * Returns the first config with a matching role name, if any
   */
  public PluginConfig findByRoleName (String role)
  {
    for (PluginConfig config : storage) {
      if (config.getRoleName().equals(role)) {
        return config;
      }
    }
    return null;
  }
  
  /**
   * Returns the first config with a matching role name, if any
   */
  public List<PluginConfig> findAllByRoleName (String role)
  {
    ArrayList<PluginConfig> result = new ArrayList<PluginConfig>(); 
    for (PluginConfig config : storage) {
      if (config.getRoleName().equals(role)) {
        result.add(config);
      }
    }
    return result;
  }
  
  /**
   * Clears out the repository in preparation for a new sim run.
   */
  public void recycle ()
  {
    storage.clear();
  }
}
