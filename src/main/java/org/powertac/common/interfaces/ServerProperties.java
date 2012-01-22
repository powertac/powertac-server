/*
 * Copyright (c) 2011 by the original author
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
package org.powertac.common.interfaces;

/**
 * Supports server configuration by allowing components to retrieve property 
 * values from a standard Java properties file. The property values are
 * searched from three locations; values in later locations override values
 * in earlier locations.
 * <ol>
 * <li>server.properties in the top level of the server jarfile. This contains
 * standard settings for all the standard server components.</li>
 * <li>A file named server.properties in the current directory.</li>
 * <li>A file provided on the command-line as the value of the config
 * option.</li>
 * </ol>  
 * @author John Collins
 */
public interface ServerProperties
{
  /**
   * Returns the value of the property with the given name, or null if no
   * such property is found.
   */
  public String getProperty (String name);

  /**
   * Returns the value of the named property, or the defaultValue if no
   * such property is found.
   */
  public String getProperty (String name, String defaultValue);
  
  /**
   * Returns the value of the named property as an Integer, or
   * defaultValue if no such property is found.
   */
  public Integer getIntegerProperty (String name, Integer defaultValue);
  
  /**
   * Returns the value of the named property as a Double, or
   * defaultValue if no such property is found.
   */
  public Double getDoubleProperty (String name, Double defaultValue);
}
