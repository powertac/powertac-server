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
import java.util.List;

/**
 * Support for annotation-driven configuration. Configurable services, including
 * services that create configurable instances (such as gencos or customer models),
 * configure themselves by calling configure(). The result is that properties
 * annotated as {@link org.powertac.common.config.ConfigurableValue} will be
 * matched with configuration data, and classes annotated as
 * {@link org.powertac.common.config.ConfigurableInstance} will have
 * instances created and configured as specified in the server configuration.
 * @author John Collins
 */
public interface ServerConfiguration
{
  /**
   * Configures a target object by matching configuration clauses with 
   * {@link org.powertac.common.config.ConfigurableValue} annotations on the
   * target object. This is typically called in the initialize() method.
   */
  public void configureMe (Object target);

  /**
   * Creates and configures potentially multiple instances of a target class
   * annotated as {@link org.powertac.common.config.ConfigurableInstance}.
   * Returns the created instances in a list in no particular order.
   */
  public Collection<?> configureInstances (Class<?> target);

  /**
   * Configures a set of named instances that have already been created.
   * This is useful for restoring instances from a bootstrap record when
   * those instances have already been created by, for example, a dynamically
   * configured customer model.
   */
  public Collection<?> configureNamedInstances (List<?> instances);

  /**
   * Gathers public configuration data for publication to brokers. Data is
   * gathered from {@link org.powertac.common.config.ConfigurableValue}
   * properties with publish=true. Note that such properties must either be
   * fields, or have a "standard" getter, or must specify a getter that produces
   * the value as a String. This is typically called at the end of the
   * initialize() method.
   */
  public void publishConfiguration (Object target);

  /**
   * Gathers state information at the end of a boot session to be restored in a
   * subsequent sim session. Data is gathered from
   * {@link org.powertac.common.config.ConfigurableValue} properties with
   * bootstrapState=true. Such properties can be fields, or may have "standard"
   * getters and setters. This method is called at the end of a bootstrap session
   * session by a component that wishes to have its state saved in the boot record.
   */
  public void saveBootstrapState (Object thing);
}
