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
package org.powertac.common.enumerations;

/**
 * Types of power a Customer can produce or consume. A single Customer may buy
 * or sell multiple PowerTypes.
 * 
 * @author jcollins
 */
public enum PowerType {
  CONSUMPTION, PRODUCTION, INTERRUPTIBLE_CONSUMPTION, THERMAL_STORAGE_CONSUMPTION, SOLAR_PRODUCTION, WIND_PRODUCTION, RUN_OF_RIVER_PRODUCTION, PUMPED_STORAGE_PRODUCTION, CHP_PRODUCTION, FOSSIL_PRODUCTION, BATTERY_STORAGE, ELECTRIC_VEHICLE
}
