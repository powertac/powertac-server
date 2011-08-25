/*
 * Copyright 2009-2010 the original author or authors.
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

import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;

import java.util.List;

/**
 * Common Interface for the Physical Environment module.
 *
 * @author David Dauer, Carsten Block
 * @version 0.1, January 2nd, 2011
 */
public interface PhysicalEnvironment {
  /**
   * Generates and returns weather forecasts for every enabled timeslot
   * The physical environment module is responsible for computing weather forecasts for each entry in {@code targetTimeslots} from the perspective of the {@code currentTimeslot}.
   *
   * Note: For the specific resulting {@link Weather} instance for which {@code weatherInstance.targetTimeslot == weatherInstance.currentTimeslot} (i.e. the "now" timeslot) {@code forecast} attribute must be set to false as this is the real (i.e. metered) weather data and not a forecast anymore
   *
   * @param currentTimeslot the current timeslot
   * @param targetTimeslots timeslots to generate weather forecasts for
   * @return a list of weather forecast objects
   */
  List<WeatherReport> generateWeatherData(Timeslot currentTimeslot, List<Timeslot> targetTimeslots);
}
