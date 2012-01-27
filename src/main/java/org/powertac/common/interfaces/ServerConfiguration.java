/*
 * Copyright (c) 2012 by the original author
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

import java.util.Collection;

/**
 * Support for annotation-driven configuration. Configurable services, including
 * services that create configurable instances (such as gencos or customer models),
 * configure themselves by calling configure(). The result is that properties
 * annotated as @ConfigurableValue will be matched with configuration
 * data, and classes annotated as @ConfigurableInstance will have instances
 * created and configured as specified in the server configuration.
 * @author John Collins
 */
public interface ServerConfiguration
{
  /**
   * Configures a target object by matching configuration clauses with 
   * @ConfigurableValue annotations on the target object.
   */
  public void configureMe (Object target);
  
  /**
   * Creates and configures potentially multiple instances of a target class
   * annotated as @ConfigurableInstance. Returns the created instances in 
   * a list in no particular order.
   */
  public Collection<?> configureInstances (Class<?> target);
}
