/*
 * Copyright (c) 2014 by John Collins
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
 * Implementers of this interface will be asked to record their state at the
 * end of a bootstrap session, for restoration at the beginning of the
 * corresponding sim session.
 * 
 * @author John Collins
 */
public interface BootstrapState
{
  /**
   * Saves state at the conclusion of a bootstrap session as configuration
   * items. State can then be restored at the beginning of
   * the sim session through the normal configuration process. The usual
   * way to save state is to call
   * ServerPropertiesService.saveBootstrapState(arg), where arg is either a
   * single configurable object (configured with configureMe() or
   * configureSingleton()), or a list of objects annotated with
   * {@link org.powertac.common.config.ConfigurableInstance}.
   */
  public void saveBootstrapState();
}
