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

import org.powertac.common.msg.PauseRelease;
import org.powertac.common.msg.PauseRequest;

/**
 * This is the core of the Power TAC simulation framework, responsible
 * for starting, running, and completing a competition. Plugins that are
 * designed to run in the main simulation loop can be activated at the
 * proper phase during each timeslot by registering themselves by phase
 * number. 
 * @author jcollins
 */
public interface CompetitionControl
{
  /**
   * True just in case the sim is running in bootstrap mode - generating
   * bootstrap data.
   */
  public boolean isBootstrapMode ();
  
  /**
   * Registers the caller to be activated during each timeslot in the
   * proper phase sequence.
   */
  public void registerTimeslotPhase (TimeslotPhaseProcessor thing, int phase);

  /**
   * Processes simulation pause messages
   */
  public void receiveMessage (PauseRequest msg);
  
  public void receiveMessage (PauseRelease msg);
}
