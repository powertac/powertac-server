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
package org.powertac.common;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import com.thoughtworks.xstream.annotations.*;

/**
 * Instances of this class can be used to configure plugins, and to
 * communicate configuration information to brokers at the beginning
 * of a simulation. In order to use this correctly, there should be
 * exactly once instance created for each configurable plugin. This
 * should be created in plugin's initialization service.
 * @author John Collins
 */
@Domain
@XStreamAlias("plugin-config")
public class PluginConfig 
{
  static private Logger log = Logger.getLogger(Rate.class.getName());

  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  
  /** Role name for this plugin. */
  @XStreamAsAttribute
  private String roleName;
  
  /** Instance name for this plugin, in case there are (or could be)
   *  multiple plugins in the same role. */
  @XStreamAsAttribute
  private String name = "";
  
  /** Attribute-value pairs representing the configuration settings. */
  private TreeMap<String, String> configuration;
  
  public PluginConfig (String role, String name)
  {
    super();
    this.roleName = role;
    this.name = name;
    configuration = new TreeMap<String, String>();
  }
  
  public long getId ()
  {
    return id;
  }
  
  public String getRoleName ()
  {
    return roleName;
  }

  public String getName ()
  {
    return name;
  }

  public Map<String, String> getConfiguration ()
  {
    return configuration;
  }
  
  public String getConfigurationValue (String key)
  {
    return configuration.get(key);
  }
  
  /**
   * Adds a config item to this PluginConfig. Returns the PluginConfig
   * instance for convenience in stringing together config calls.
   */
  @StateChange
  public PluginConfig addConfiguration (String name, String value)
  {
    configuration.put(name, value);
    return this;
  }

  public String toString() {
    return "PluginConfig:" + roleName + "." + name;
  }
}
