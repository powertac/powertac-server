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
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.xml.FullCustomerConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * A competition instance represents a single PowerTAC competition and
 * at the same time serves as the place for all competition properties that can be
 * adjusted during competition setup (i.e. during server runtime but before competition start).
 * This is an immutable value type, and most parameters are included in the
 * PluginConfig instances, rather than in individual fields. The single instance is
 * communicated to brokers at the beginning of a game scenario.
 * @author Carsten Block, KIT; John Collins, U of Minnesota
 */
@Domain
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
  
  /** Number of timeslots in initialization data dump */
  @XStreamAsAttribute
  private int bootstrapTimeslotCount = 336; // 14 days

  /** Minimum number of timeslots, aka competition length    */
  @XStreamAsAttribute
  private int minimumTimeslotCount = 480;
  
  @XStreamAsAttribute
  private int expectedTimeslotCount = 600;

  /** concurrently open timeslots, i.e. time window in which broker actions like trading are allowed   */
  @XStreamAsAttribute
  private int timeslotsOpen = 24;

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
  
  //@XStreamImplicit(itemFieldName = "plugin-config")
  //private ArrayList<PluginConfig> pluginConfigs;
  
  @XStreamImplicit(itemFieldName = "customer")
  @XStreamConverter(FullCustomerConverter.class)
  private ArrayList<CustomerInfo> customers;

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
    brokers = new ArrayList<String>();
    //pluginConfigs = new ArrayList<PluginConfig>();
    customers = new ArrayList<CustomerInfo>();
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
  
  @StateChange
  public Competition withDescription (String description)
  {
    this.description = description;
    return this;
  }
  
  /**
   * Returns the length of a timeslot in minutes (sim time).
   */
  public int getTimeslotLength ()
  {
    return timeslotLength;
  }
  
  /**
   * Returns the duration of a timeslot in milliseconds real-time.
   */
  public long getTimeslotDuration ()
  {
    return timeslotLength * TimeService.MINUTE;
  }
  
  /**
   * Converts a time value to the number of timeslots since the beginning 
   * of the simulation.
   */
  public int computeTimeslotIndex (Instant time)
  {
    return (int) (time.getMillis() / getTimeslotDuration()); 
  }

  @StateChange
  public Competition withTimeslotLength (int timeslotLength)
  {
    this.timeslotLength = timeslotLength;
    return this;
  }

  public int getMinimumTimeslotCount ()
  {
    return minimumTimeslotCount;
  }

  @StateChange
  public Competition withMinimumTimeslotCount (int minimumTimeslotCount)
  {
    this.minimumTimeslotCount = minimumTimeslotCount;
    return this;
  }

  public int getBootstrapTimeslotCount ()
  {
    return bootstrapTimeslotCount;
  }
  
  @StateChange
  public Competition withBootstrapTimeslotCount (int bootstrapTimeslotCount)
  {
    this.bootstrapTimeslotCount = bootstrapTimeslotCount;
    return this;
  }
  
  public int getExpectedTimeslotCount ()
  {
    return expectedTimeslotCount;
  }

  @StateChange
  public Competition withExpectedTimeslotCount (int expectedTimeslotCount)
  {
    this.expectedTimeslotCount = expectedTimeslotCount;
    return this;
  }

  public int getTimeslotsOpen ()
  {
    return timeslotsOpen;
  }

  @StateChange
  public Competition withTimeslotsOpen (int timeslotsOpen)
  {
    this.timeslotsOpen = timeslotsOpen;
    return this;
  }

  public int getDeactivateTimeslotsAhead ()
  {
    return deactivateTimeslotsAhead;
  }

  @StateChange
  public Competition withDeactivateTimeslotsAhead (int deactivateTimeslotsAhead)
  {
    this.deactivateTimeslotsAhead = deactivateTimeslotsAhead;
    return this;
  }

  public Instant getSimulationBaseTime ()
  {
    return simulationBaseTime;
  }

  @StateChange
  public Competition withSimulationBaseTime (Instant simulationBaseTime)
  {
    this.simulationBaseTime = simulationBaseTime;
    return this;
  }

  public long getSimulationRate ()
  {
    return simulationRate;
  }

  @StateChange
  public Competition withSimulationRate (long simulationRate)
  {
    this.simulationRate = simulationRate;
    return this;
  }

  public long getSimulationModulo ()
  {
    return simulationModulo;
  }

  @StateChange
  public Competition withSimulationModulo (long simulationModulo)
  {
    this.simulationModulo = simulationModulo;
    return this;
  }

  public List<String> getBrokers ()
  {
    return brokers;
  }
  
  @StateChange
  public Competition addBroker (String brokerUsername)
  {
    brokers.add(brokerUsername);
    return this;
  }
  
  public List<CustomerInfo> getCustomers ()
  {
    return customers;
  }
  
  @StateChange
  public Competition addCustomer (CustomerInfo customer)
  {
    customers.add(customer);
    return this;
  }

  /**
   * Updates selected fields of this Competition from a template. This is
   * designed to be used to copy attributes from a bootstrap run into a
   * normal sim run.
   */
  public void update (Competition template)
  {
    withBootstrapTimeslotCount(template.getBootstrapTimeslotCount());
    withDeactivateTimeslotsAhead(template.getDeactivateTimeslotsAhead());
    withSimulationBaseTime(template.getSimulationBaseTime());
    withSimulationModulo(template.getSimulationModulo());
    withTimeslotLength(template.getTimeslotLength());
    withTimeslotsOpen(template.getTimeslotsOpen());
  }
  
  public String toString() 
  {
    return name;
  }
}
