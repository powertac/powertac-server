/*
 * Copyright 2009-2011 the original author or authors.
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

package org.powertac.common;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import com.thoughtworks.xstream.annotations.*;

/**
 * A competition instance represents a single PowerTAC competition and
 * at the same time serves as the place for all competition properties that can be
 * adjusted during competition setup (i.e. during server runtime but before competition start).
 * This is an immutable value type, and most parameters are included in the
 * PluginConfig instances, rather than in individual fields. The single instance is
 * communicated to brokers at the beginning of a game scenario.
 * @author Carsten Block, KIT; John Collins, U of Minnesota
 */
@XStreamAlias("competition")
public class Competition //implements Serializable 
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();

  /** The competition's name */
  @XStreamAsAttribute
  private String name = "default";

  /** Optional text that further describes the competition    */
  private String description = "";

  /** length of a timeslot in simulation minutes    */
  @XStreamAsAttribute
  private int timeslotLength = 60;

  /** Minimum number of timeslots, aka competition length    */
  @XStreamAsAttribute
  private int minimumTimeslotCount = 240;
  
  @XStreamAsAttribute
  private int expectedTimeslotCount = 288;

  /** concurrently open timeslots, i.e. time window in which broker actions like trading are allowed   */
  @XStreamAsAttribute
  private int timeslotsOpen = 23;

  /** # timeslots a timeslot gets deactivated ahead of the now timeslot (default: 1 timeslot, which (given default length of 60 min) means that e.g. trading is disabled 60 minutes ahead of time    */
  @XStreamAsAttribute
  private int deactivateTimeslotsAhead = 1;

  /** the start time of the simulation scenario, in sim time. */
  @XStreamAsAttribute
  private Instant simulationBaseTime = new DateTime(2010, 6, 21, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();

  /** the time-compression ratio for the simulation. So if we are running one-hour timeslots every 5 seconds, the rate would be 720 (=default).    */
  @XStreamAsAttribute
  private long simulationRate = 720l;

  /** controls the values of simulation time values reported. If
   *  we are running one-hour timeslots, then the modulo should be one hour, expressed
   *  in milliseconds. If we are running one-hour timeslots but want to update time every
   *  30 minutes of simulated time, then the modulo would be 30*60*1000. Note that
   *  this will not work correctly unless the calls to updateTime() are made at
   *  modulo/rate intervals. Also note that the reported time is computed as
   *  rawTime - rawTime % modulo, which means it will never be ahead of the raw
   *  simulation time. Note that values other than the length of a timeslot have
   *  not been tested. */
  @XStreamAsAttribute
  private long simulationModulo = 60*60*1000;
  
  // include the list of broker usernames
  @XStreamImplicit(itemFieldName = "broker")
  private ArrayList<String> brokers;
  
  @XStreamImplicit(itemFieldName = "plugin-config")
  private ArrayList<PluginConfig> pluginConfigs;

  // singleton instance
  private static Competition theCompetition;
  
  public static Competition newInstance (String name)
  {
    Competition result = new Competition(name);
    theCompetition = result;
    return result;
  }

  /**
   * Returns the current Competition instance. There should always be either
   * zero or one of these.
   */
  public static Competition currentCompetition() {
    return theCompetition;
  }
  
  /**
   * Constructor replaces current competition instance. It is up to the
   * caller to ensure that this is done at the correct time.
   */
  private Competition (String name)
  {
    super();
    this.name = name;
  }
  
  public long getId ()
  {
    return id;
  }

  public String getName ()
  {
    return name;
  }

  public String getDescription ()
  {
    return description;
  }

  public int getTimeslotLength ()
  {
    return timeslotLength;
  }

  public int getMinimumTimeslotCount ()
  {
    return minimumTimeslotCount;
  }

  public int getExpectedTimeslotCount ()
  {
    return expectedTimeslotCount;
  }

  public int getTimeslotsOpen ()
  {
    return timeslotsOpen;
  }

  public int getDeactivateTimeslotsAhead ()
  {
    return deactivateTimeslotsAhead;
  }

  public Instant getSimulationBaseTime ()
  {
    return simulationBaseTime;
  }

  public long getSimulationRate ()
  {
    return simulationRate;
  }

  public long getSimulationModulo ()
  {
    return simulationModulo;
  }

  public List<String> getBrokers ()
  {
    return brokers;
  }
  
  public void addBroker (String brokerUsername)
  {
    brokers.add(brokerUsername);
  }
  
  public List<PluginConfig> getPluginConfigs ()
  {
    return pluginConfigs;
  }
  
  public void addToPlugins (PluginConfig config)
  {
    pluginConfigs.add(config);
  }

  public String toString() 
  {
    return name;
  }
}
