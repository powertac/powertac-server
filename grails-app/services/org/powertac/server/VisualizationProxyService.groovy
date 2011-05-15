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
package org.powertac.server

import org.powertac.common.interfaces.VisualizationProxy
import org.powertac.common.interfaces.VisualizationListener

class VisualizationProxyService implements VisualizationProxy {

  static transactional = true

  Set registrations = []

  void registerVisualizationListener(VisualizationListener listener) {
    registrations.add(listener)
  }

  void forwardMessage(Object message) {
    registrations.each { VisualizationListener listener ->
      listener.receiveMessage(message)
    }
    // TODO: Additionally use JMS to forward messages to external visualizers
  }
}
