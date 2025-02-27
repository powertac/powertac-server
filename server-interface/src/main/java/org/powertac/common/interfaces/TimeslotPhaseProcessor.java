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

import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Plugins must extend this class in order to be invoked during timeslot
 * processing by the CompetitionControl. Each subclass must implement the
 * activate() method to do its thing during a timeslot, and must call its
 * superclass init() method during per-game initialization.
 * 
 * Per-timeslot processing takes place in 
 * phases. See https://github.com/powertac/powertac-server/wiki/Competition-controller-timeslot-process
 * for a summary of this process.
 * 
 * @author John Collins
 */
public abstract class TimeslotPhaseProcessor
{
  @Autowired
  protected CompetitionControl competitionControlService;
  
  private int timeslotPhase = 0;
  
  public TimeslotPhaseProcessor ()
  {
    super();
  }
  
  /** This method must be called in the per-game initialization code in each 
   * implementing class. This is where the timeslot phase registration gets
   * done.
   */
  protected void init ()
  {
    competitionControlService.registerTimeslotPhase(this, timeslotPhase);
  }
  
  /**
   * This is the Spring-accessible setter for the phase number
   */
  public void setTimeslotPhase (int newValue)
  {
    timeslotPhase = newValue;
  }
  
  /**
   * This method gets called once during each timeslot. To get called, the
   * module must first call the register(phaseNumber) method on CompetitionControl.
   * The call will give the current simulation time and phase number in the
   * arguments.
   */
  public abstract void activate (Instant time, int phaseNumber);
}
