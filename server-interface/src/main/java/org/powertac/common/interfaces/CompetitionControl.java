/*
 * Copyright 2009-2012 the original author or authors.
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
   * Attempts to log in a broker by username. Returns true just in case the
   * login is successful. The intent is that login will be successful if the
   * username is on the authorizedBrokerList, or if it is one of the
   * pre-configured brokers (usually only the default broker).
   */
  public boolean loginBroker (String username);

  /**
   * Sets the list of brokers authorized to log in to the next game. Must
   * be called after completion of a simulation and before calling runOnce(). 
   * This is normally done immediately after calling preGame().
   */
  public void setAuthorizedBrokerList (List<String> brokerList);

  /**
   * Sets the broker-synchronization flag, which if true allows
   * sim to proceed as soon as all brokers have sent their
   * BrokerComplete messages
   */
  public void setBrokerSync (boolean sync);

  /**
   * Waits for broker login, then starts and runs a simulation.
   * The second form allows for configuration dumps without actually running
   * the simulation.
   */
  public void runOnce (boolean bootstrapMode);
  public void runOnce (boolean bootstrapMode, boolean dumpConfigOnly);

  /**
   * True if a simulation (boot or sim) session is currently running.
   */
  public boolean isRunning();
  
  /**
   * Stops a running simulation, and sends out the SimEnd message
   * to brokers.
   */
  public void shutDown ();
}
