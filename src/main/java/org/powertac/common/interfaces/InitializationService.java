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
package org.powertac.common.interfaces;

import java.util.List;

import org.powertac.common.Competition;

/**
 * Implementations of this interface are expected to initialize individual
 * plugins prior to the beginning of a game. Before initialization, the database
 * will contain only a single Competition instance, and possibly the Brokers that
 * will be participating in the game.
 * <p>
 * Because some plugins may depend on the initialization of other plugins (for example,
 * the default broker may depend on the tariff market and accounting service so that it
 * can successfully publish its tariffs), the completedInits argument contains the
 * list of plugin roles that have already been successfully initialized. If the
 * completedInits list contains the names of all the plugins that must be initialized
 * before the plugin being initialized, then the initialization can proceed, and the
 * plugin must return its role name. If a plugin returns null, then
 * it will be assumed that its dependencies are not satisfied, and it will be called again
 * after all other plugins have been initialized. As long as there are no cycles in
 * the dependency graph, this approach will terminate.</p>
 * <p>
 * When the database is cleared, all the PluginConfig instances as well as the Competition
 * instance will be deleted. To make it easy to configure individual games through a
 * web interface, they must be re-created before the configuration process begins. This
 * is accomplished by calling the setDefaults method at bootstrap time and after the end 
 * of each game. There should be no sequence dependencies for this process.
 *  
 * @author John Collins
 */
public interface InitializationService
{
  /**
   * Creates and saves default PluginConfig instances.
   */
  public void setDefaults();
  
  /**
   * Initializes a plugin prior to the beginning of a game. The completedInits
   * parameter is the list of plugin role names that have been successfully initialized.
   * If sequence dependencies are satisfied (or if there are no sequence dependencies),
   * then an implementation must complete its pre-game initialization process and return
   * its role name. If sequence dependencies are not satisfied, then an implementation
   * must return null. It will be called again after additional successful initializations
   * have been completed. If initialization is not possible, then returning the string
   * 'fail' will cause the server to log an error and shut down. This will be helpful
   * just in case the implementation also logs a detailed error message.
   */
  public String initialize (Competition competition, List<String> completedInits);
}
