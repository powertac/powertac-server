/*
 * Copyright (c) 2011 by the original author or authors.
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

import java.util.List;

/**
 * Object that collects bootstrap data for initializing brokers.
 * @author John Collins
 */
public interface BootstrapDataCollector
{
  /**
   * Returns a list of bootstrap messages that can be bundled up and sent
   * to brokers at the beginning of a game. The argument is the maximum
   * number of timeslots to be included. This allows data from the beginning 
   * of the bootstrap period to be excluded
   */
  public List<Object> collectBootstrapData (int maxTimeslots);
}
