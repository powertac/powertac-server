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

import org.joda.time.Instant;

/**
 * Plugins must implement this interface to be invoked during timeslot
 * processing by the CompetitionControl. Processing will take place in 
 * phases. See https://github.com/powertac/powertac-server/wiki/Competition-controller-timeslot-process
 * for a summary of this process.
 * 
 * @author John Collins
 */
public interface TimeslotPhaseProcessor
{
  
  /**
   * This method gets called once during each timeslot. To get called, the
   * module must first call the register(phaseNumber) method on CompetitionControl.
   * The call will give the current simulation time and phase number in the
   * arguments.
   */
  public void activate (Instant time, int phaseNumber);
}
