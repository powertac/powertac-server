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
  static private Logger log = Logger.getLogger(PluginConfig.class.getName());

  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  
  /** Role name for this plugin. */
  @XStreamAsAttribute
  private String roleName;
  
  /** Instance name for this plugin, in case there are (or could be)
   *  multiple plugins in the same role. */
  @XStreamAsAttribute
  private String name = "";
  
  @XStreamOmitField
  private boolean privileged = false;
  
  
  /** Attribute-value pairs representing the configuration settings. */
  private TreeMap<String, String> configuration;
  
  /**
   * Creates a new PluginConfig. Attributes can be added with the fluent-style
   * addConfiguration() method. The role should be the classname of the type
   * that is being configured (not the name of the type creating the
   * PluginConfig, unless it's the same type). The name can be left blank
   * as long as the role is a singleton; otherwise it should distinguish the
   * instances of the role from each other.
   */
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
  
  public boolean isPrivileged ()
  {
    return privileged;
  }
  
  @StateChange
  public PluginConfig asPrivileged ()
  {
    privileged = true;
    return this;
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
   * Updates the configuration from a matching PluginConfig (presumably this
   * was read from a file or received as a message). This only works if the
   * roleName and name match. If successful, returns true; otherwise false.
   */
  public boolean update (PluginConfig replacement)
  {
    if (!roleName.equals(replacement.getRoleName()) ||
        !name.equals(replacement.getName())) {
      log.error("replacement match failed: ours=[" + roleName + "," + name +
                "], theirs=[" + replacement.getRoleName() + "," +
                replacement.getName() + "]");
      return false;
    }
    // we could just replace the configuration, but that would not generate
    // state-change entries
    Map<String, String> rmap = replacement.getConfiguration();
    for (String key : rmap.keySet()) {
      addConfiguration(key, rmap.get(key));
    }
    return true;
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
    StringBuffer sb = new StringBuffer();
    sb.append("PluginConfig:").append(roleName).append(":");
    sb.append(name).append("[");
    String delimiter = "";
    for (String key : configuration.keySet()) {
      sb.append(delimiter).append(key).append(":").append(configuration.get(key));
      delimiter = ", ";
    }
    sb.append("]");
    return sb.toString();
  }

  public Integer getIntegerValue (String name, Integer defaultValue)
  {
    String value = getConfigurationValue(name);
    // error check
    Integer number = null;
    if (value != null) {
      number = Integer.parseInt(value);
    }
    if (value == null || number == null) {
      log.warn("parameter " + name + " not given in config");
      return defaultValue;
    }
    return number;
  }

  public Double getDoubleValue (String name, Double defaultValue)
  {
    String value = getConfigurationValue(name);
    // error check
    Double number = null;
    if (value != null) {
      number = Double.parseDouble(value);
    }
    if (value == null || number == null) {
      log.warn("parameter " + name + " not given in config");
      return defaultValue;
    }
    return number;
  }
}
