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
import java.util.Map;
import java.util.TreeMap;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.msg.MarketBootstrapData;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.xml.FullCustomerConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;


/**
 * A competition instance represents a single PowerTAC competition and
 * at the same time serves as the place for all competition properties that can be
 * adjusted during competition setup (i.e. during server runtime but before competition start).
 * This is an immutable value type.
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

  /** POM ID from server.properties */
  @XStreamAsAttribute
  private String pomId = "unknown";

  /** length of a timeslot in simulation minutes. Note that if this is not one hour,
   *  there will likely be problems with other elements of the simulation, such as
   *  weather reporting. */
  @XStreamAsAttribute
  private int timeslotLength = 60;
  
  /** Number of timeslots in initialization data dump */
  @XStreamAsAttribute
  private int bootstrapTimeslotCount = 336; // 14 days

  /** Number of extra timeslots at start of bootstrap before data collection starts */
  @XStreamAsAttribute
  private int bootstrapDiscardedTimeslots = 24;

  /** Minimum number of timeslots, aka competition length    */
  @XStreamOmitField
  private int minimumTimeslotCount = 480;

  @XStreamOmitField
  private int expectedTimeslotCount = 600;
  
  @XStreamOmitField
  private Integer fixedTimeslotCount = null;

  /** concurrently open timeslots, i.e. time window in which broker actions like trading are allowed   */
  @XStreamAsAttribute
  private int timeslotsOpen = 24;

  /** # timeslots a timeslot gets deactivated ahead of the now timeslot (default: 1 timeslot, which (given default length of 60 min) means that e.g. trading is disabled 60 minutes ahead of time    */
  @XStreamAsAttribute
  private int deactivateTimeslotsAhead = 1;

  /** Minimum order quantity */
  @XStreamAsAttribute
  private double minimumOrderQuantity = 0.01; // MWh

  // Tariff evaluation parameters
  /** Above this ratio, regulation is discounted. */
  @XStreamAsAttribute
  private double maxUpRegulationPaymentRatio = -4.0;

  @XStreamAsAttribute
  private double upRegulationDiscount = 0.5;

  /** Above this ratio, customer will discount down-regulation,
      either during evaluation nor at runtime. */
  @XStreamAsAttribute
  private double maxDownRegulationPaymentRatio = 1.5;

  @XStreamAsAttribute
  private double downRegulationDiscount = 0.4;

  /** Brokers typically pay less for production than they charge for
      consumption. This ratio is an estimate of that margin that is used
      to modify the constraints on up- and down- regulation behavior. */
  @XStreamAsAttribute
  private double estimatedConsumptionPremium = 2.0;

  /** the start time of the simulation scenario, in sim time. */
  @XStreamAsAttribute
  private Instant simulationBaseTime = ZonedDateTime.of(2010, 6, 21, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();

  /** timezone offset for scenario locale */
  @XStreamAsAttribute
  private int timezoneOffset = 0;
  
  /** approximate latitude in degrees north for scenario locale */
  @XStreamAsAttribute
  private int latitude = 45;
  
  /** the time-compression ratio for the simulation. So if we are running
   *  one-hour timeslots every 5 seconds, the rate would be 720 (=default). * */
  @XStreamAsAttribute
  private long simulationRate = 720l;

  /** controls the granularity of simulation time. If
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
  
  @XStreamImplicit(itemFieldName = "customer")
  @XStreamConverter(FullCustomerConverter.class)
  private ArrayList<CustomerInfo> customers;

  // Market data needed for tariff evaluation, set at start of sim session
  // Must not be serialized, since it gets serialized elsewhere.
  @XStreamOmitField
  private MarketBootstrapData marketBootstrapData;

  // singleton instance
  private static Competition theCompetition;
  
  public static Competition newInstance (String name)
  {
    return newInstance(name, true);
  }
  
  public static Competition newInstance (String name, boolean singleton)
  {
    Competition result = new Competition(name, singleton);
    //theCompetition = result;
    return result;
  }

  /**
   * Returns the current Competition instance. There should always be either
   * zero or one of these.
   */
  public static Competition currentCompetition()
  {
    return theCompetition;
  }
  
  /**
   * Makes a Competition instance be the "current" competition - this is 
   * needed in a broker when the Competition instance arrives from the
   * server.
   */
  public static void setCurrent (Competition newCurrent)
  {
    theCompetition = newCurrent;
  }
  
  /**
   * private constructor optionally replaces current competition instance. It is up to the
   * caller to ensure that this is done at the correct time.
   */
  private Competition (String name, boolean singleton)
  {
    super();
    this.name = name;
    brokers = new ArrayList<String>();
    customers = new ArrayList<CustomerInfo>();
    if (singleton)
      theCompetition = this;
  }

  // Original constructor
  private Competition (String name)
  {
    this(name, true);
  }

  public long getId ()
  {
    return id;
  }

  /** Returns the competition name */
  public String getName ()
  {
    return name;
  }

  /** Uninterpreted text that further describes the competition. */
  public String getDescription ()
  {
    return description;
  }

  /**
   * Fluent setter for competition description.
   */
  @ConfigurableValue(
      description = "user-readable description of the Competition",
      valueType = "String")
  @StateChange
  public Competition withDescription (String description)
  {
    this.description = description;
    return this;
  }

  /**
   * Returns the pom version id from the server on which this Competition
   * was created.
   */
  public String getPomId ()
  {
    return pomId;
  }

  /** Fluent setter for Pom ID.
   */
  @ConfigurableValue(
      description = "maven version identifier from server",
      valueType = "String")
  @StateChange
  public Competition withPomId (String id)
  {
    this.pomId = id;
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
   * Returns the duration of a timeslot in milliseconds sim-time.
   */
  public long getTimeslotDuration ()
  {
    return timeslotLength * TimeService.MINUTE;
  }

  /**
   * Fluent setter for timeslot length, interpreted as minutes in sim time.
   */
  @ConfigurableValue(name = "timeslotLength",
                     description = "length of timeslot in minutes sim time",
                     valueType = "Integer")
  @StateChange
  public Competition withTimeslotLength (int timeslotLength)
  {
    this.timeslotLength = timeslotLength;
    return this;
  }

  /**
   * Minimum number of timeslots for this competition. The actual number is
   * randomized by CompetitionControl at sim start time.
   */
  public int getMinimumTimeslotCount ()
  {
    return minimumTimeslotCount;
  }

  /**
   * Fluent setter for minimumTimeslotCount.
   */
  @ConfigurableValue(valueType = "Integer",
      description = "minimum number of timeslots in simulation run")
  @StateChange
  public Competition withMinimumTimeslotCount (int minimumTimeslotCount)
  {
    this.minimumTimeslotCount = minimumTimeslotCount;
    return this;
  }
  
  /**
   * Expected value of timeslot count for a normal sim session.
   */
  public int getExpectedTimeslotCount ()
  {
    return expectedTimeslotCount;
  }

  /**
   * Fluent setter for the expected length of a normal sim session.
   */
  @ConfigurableValue(valueType = "Integer",
      description = "expected number of timeslots in simulation run")
  @StateChange
  public Competition withExpectedTimeslotCount (int expectedTimeslotCount)
  {
    this.expectedTimeslotCount = expectedTimeslotCount;
    return this;
  }
  
  /**
   * Fixed value for timeslot count, allows external tools such as tournament scheduler to
   * compute game lengths externally.
   */
  public Integer getFixedTimeslotCount ()
  {
    return fixedTimeslotCount;
  }

  /**
   * Fluent setter for the expected length of a normal sim session.
   */
  @ConfigurableValue(valueType = "Integer",
      description = "If given, overrides min and expected timeslot count values")
  @StateChange
  public Competition withFixedTimeslotCount (Integer fixedTimeslotCount)
  {
    this.fixedTimeslotCount = fixedTimeslotCount;
    return this;
  }

  /**
   * Number of timeslots simultaneously open for trading.
   */
  public int getTimeslotsOpen ()
  {
    return timeslotsOpen;
  }

  /**
   * Fluent setter for the open timeslot count. Default value is 24.
   */
  @ConfigurableValue(valueType = "Integer",
      description = "expected number of timeslots in simulation run")
  @StateChange
  public Competition withTimeslotsOpen (int timeslotsOpen)
  {
    this.timeslotsOpen = timeslotsOpen;
    return this;
  }

  /**
   * Number of timeslots, starting with the current timeslot, that are closed
   * for trading. 
   */
  public int getDeactivateTimeslotsAhead ()
  {
    return deactivateTimeslotsAhead;
  }

  /**
   * Fluent setter for number of timeslots, starting with the current timeslot,
   * that are closed for trading. Default value is 1. 
   */
  @ConfigurableValue(valueType = "Integer",
      description = "expected number of timeslots in simulation run")
  @StateChange
  public Competition withDeactivateTimeslotsAhead (int deactivateTimeslotsAhead)
  {
    this.deactivateTimeslotsAhead = deactivateTimeslotsAhead;
    return this;
  }

  /**
   * Minimum order quantity in MWh.
   */
  public double getMinimumOrderQuantity ()
  {
    return minimumOrderQuantity;
  }

  /**
   * Customers assume up-regulation will never clear if the regulation price
   * is higher than the consumption price times this ratio, and so up-regulation
   * will be ignored during tariff evaluation.
   */
  public double getMaxUpRegulationPaymentRatio ()
  {
    return maxUpRegulationPaymentRatio;
  }

  /**
   * Fluent setter for the maximum ratio between consumption price and up-regulation
   * price for which customers will include up-regulation in tariff evaluation.
   */
  @ConfigurableValue(valueType = "Double",
          description = "Limit on up-regulation payment ratio")
  public Competition withMaxUpRegulationPaymentRatio (double value)
  {
    maxUpRegulationPaymentRatio = value;
    return this;
  }

  /**
   * Discount rate for overpriced up-regulation.
   */
  public double getUpRegulationDiscount ()
  {
    return upRegulationDiscount;
  }

  /**
   * Fluent setter for overpriced up-regulation discount rate.
   */
  @ConfigurableValue(valueType = "Double",
          description = "Discount rate on overpriced up-regulation")
  public Competition withUpRegulationDiscount (double value)
  {
    upRegulationDiscount = value;
    return this;
  }

  /**
   * If a tariff offers a down-regulation price larger (more negative) than the
   * consumption price times this ratio, customers will not offer down-regulation,
   * and will ignore down-regulation during tariff evaluation.
   */
  public double getMaxDownRegulationPaymentRatio ()
  {
    return maxDownRegulationPaymentRatio;
  }

  /**
   * Fluent setter for the maximum down-regulation payment ratio.
   */
  @ConfigurableValue(valueType = "Double",
          description = "Limit on down-regulation payment")
  public Competition withMaxDownRegulationPaymentRatio (double value)
  {
    maxDownRegulationPaymentRatio = value;
    return this;
  }

  /**
   * Discount rate for overpriced down-regulation.
   */
  public double getDownRegulationDiscount ()
  {
    return downRegulationDiscount;
  }

  /**
   * Fluent setter for overpriced down-regulation discount rate.
   */
  @ConfigurableValue(valueType = "Double",
          description = "Discount rate on overpriced down-regulation")
  public Competition withDownRegulationDiscount (double value)
  {
    downRegulationDiscount = value;
    return this;
  }
  
  /**
   * Getter and setter for market bootstrap data
   */
  public MarketBootstrapData getMarketBootstrapData ()
  {
    return marketBootstrapData;
  }

  public void setMarketBootstrapData (MarketBootstrapData mbd)
  {
    marketBootstrapData = mbd;
  }

  /**
   * Brokers typically pay less for production than they charge for consumption.
   * This ratio is an estimate of that margin that is used to modify the
   * constraints on up- and down-regulation behavior.
   */
  public double getEstimatedConsumptionPremium ()
  {
    return estimatedConsumptionPremium;
  }

  /**
   * Fluent setter for the estimated consumption price premium.
   */
  @ConfigurableValue(valueType = "Double",
          description = "Estimated ratio of consumption prices over production prices")
  public Competition withEstimatedConsumptionPremium (double value)
  {
    estimatedConsumptionPremium = value;
    return this;
  }

  /**
   * Fluent setter for minimum order quantity. Default is 0.01 MWh.
   */
  @ConfigurableValue(valueType = "Double",
      description = "Minimum order quantity in MWh")
  @StateChange
  public Competition withMinimumOrderQuantity (double minOrderQty)
  {
    this.minimumOrderQuantity = minOrderQty;
    return this;
  }

  /**
   * Start time of a sim session in the sim world. This is actually the start
   * of the bootstrap session, which is typically 15 days before the start of
   * a normal sim session.
   */
  public Instant getSimulationBaseTime ()
  {
    return simulationBaseTime;
  }

  /**
   * Fluent setter for simulation base time that takes a String, interpreted
   * as a standard DateTimeFormat as yyyy-MM-dd. If that fails, try to parse
   * the string as a regular (long) timestamp.
   */
  @ConfigurableValue(valueType = "String",
    description = "Scenario start time of the bootstrap portion of a simulation")
  public Competition withSimulationBaseTime (String baseTime)
  {
    Instant instant;
    try {
      DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

      instant = LocalDate.parse(baseTime, formatter)
              .atStartOfDay(ZoneOffset.UTC).toInstant();
    }
    catch (IllegalArgumentException | DateTimeParseException e) {
      // Try to interpret the string as a long timestamp instead
      instant = Instant.ofEpochMilli(Long.parseLong(baseTime));
    }
    return withSimulationBaseTime(instant);
  }

  /**
   * Fluent setter for simulation base time. This is the start of a simulation
   * scenario, in the sim world, at the beginning of a bootstrap session. So if
   * the bootstrap session collects data for 14 days, with an addional day of 
   * discarded data at the beginning, it is 15 days before the start of a
   * normal sim. 
   */
  public Competition withSimulationBaseTime (Instant simulationBaseTime)
  {
    return withSimulationBaseTime(simulationBaseTime.toEpochMilli());
  }

  /**
   * Fluent setter for simulation base time that takes a long.
   */
//  @ConfigurableValue(valueType = "Long",
//    description = "Scenario start time of the bootstrap portion of a simulation")
  @StateChange
  public Competition withSimulationBaseTime (long baseTime)
  {
    this.simulationBaseTime = Instant.ofEpochMilli(baseTime);
    return this;
  }

  /**
   * Returns timezone offset for sim locale.
   */
  public int getTimezoneOffset ()
  {
    return timezoneOffset;
  }

  /**
   * Fluent setter for timezone offset
   */
  @ConfigurableValue(valueType = "Integer",
          description = "Timezone offset from UTC for sim locale")
  @StateChange
  public Competition withTimezoneOffset (int offset)
  {
    this.timezoneOffset = offset;
    return this;
  }

  /**
   * Returns approximate latitude in degrees for sim locale.
   */
  public int getLatitude ()
  {
    return latitude;
  }

  /**
   * Fluent setter for latitude value
   */
  @ConfigurableValue(valueType = "Integer",
          description = "Approximate latitude of sim locale")
  @StateChange
  public Competition withLatitude (int latitude)
  {
    this.latitude = latitude;
    return this;
  }

  /**
   * Number of timeslots in the bootstrap data report for a normal sim.
   */
  public int getBootstrapTimeslotCount ()
  {
    return bootstrapTimeslotCount;
  }
  
  /**
   * Fluent setter for the bootstrap timeslot count. It only makes sense to
   * change this before running a bootstrap session.
   */
  @ConfigurableValue(valueType = "Integer",
    description = "Number of timeslots in bootstrap session during which data is collected")
  @StateChange
  public Competition withBootstrapTimeslotCount (int bootstrapTimeslotCount)
  {
    this.bootstrapTimeslotCount = bootstrapTimeslotCount;
    return this;
  }
  
  /**
   * Length of bootstrap interval in msec. Add this to the simulation base
   * time, and you get the start time for a normal sim session.
   */
  public int getBootstrapDiscardedTimeslots()
  {
    return bootstrapDiscardedTimeslots;
  }
  
  /**
   * Fluent setter for bootstrap interval.
   */
  @ConfigurableValue(valueType = "Integer",
      description = "Number of timeslots in bootstrap session that are discarded" +
                    "before data collection begins")
  @StateChange
  public Competition withBootstrapDiscardedTimeslots (int count)
  {
    this.bootstrapDiscardedTimeslots = count;
    return this;
  }

  /**
   * The time-compression factor for the simulation.
   */
  public long getSimulationRate ()
  {
    return simulationRate;
  }

  /**
   * Fluent setter for time compression ratio. Default value is 720, which
   * runs 1-hour timeslots in 5 real-time seconds.
   * Value may be adjusted to make real-world timeslot length an integer
   * number of milliseconds.
   */
  @StateChange
  public Competition withSimulationRate (long simulationRate)
  {
    this.simulationRate = simulationRate;
    adjustRate();
    return this;
  }

  // find int rate that makes timeslot clock time be an integer number of msec by finding r'
  // as the largest integer < rate that gives integer timeslot length. We do this by factoring.
  private void adjustRate()
  {
    long sr = 0;
    for (long r = simulationRate; r > 0; r--) {
      if (getTimeslotDuration() % r == 0) {
        sr = r;
        break;
      }
    }
    simulationRate = sr;
  }
  
  /**
   * Returns the number of seconds in wall-clock time per timeslot.
   */
  public double getSimulationTimeslotSeconds ()
  {
    return timeslotLength * 60.0 / simulationRate;
  }
  
  /**
   * Fluent setter for controlling simulation rate by setting the number of
   * wall-clock seconds per timeslot.
   * Results may be strange if timeslotLength is changed after this is set.
   */
  @ConfigurableValue(valueType = "Double",
      description = "Time compression ratio for simulation clock")
  public Competition withSimulationTimeslotSeconds (double seconds)
  {
    return withSimulationRate(Math.round(timeslotLength * 60.0 / seconds));
  }

  /**
   * Minimum value in milliseconds by which time advances in a simulation,
   * or in other words, the size of a clock tick. Normally it's
   * one timeslot. In the sim world, time is always at the beginning of
   * a clock tick.
   */
  public long getSimulationModulo ()
  {
    return simulationModulo;
  }

  /**
   * Fluent setter for simulation modulo. Most likely, most server components
   * will not respond properly for values that are different from a timeslot
   * length. Default value is 3600000 msec.
   */
  @ConfigurableValue(valueType = "Long",
      description = "Size, in milliseconds, of a simulation clock tick." +
                    "Normally, this is the same as a timeslot.")
  @StateChange
  public Competition withSimulationModulo (long simulationModulo)
  {
    this.simulationModulo = simulationModulo;
    return this;
  }
  
  /**
   * Returns the clock parameters for the start of a normal sim session
   * as a simple Map, to simplify code that
   * must mediate between Competition and TimeService instances. The computed
   * base time will be the base time of the bootstrap period plus the length
   * of the bootstrap period.
   */
  public Map<String, Long> getClockParameters ()
  {
    Map<String, Long> result = new TreeMap<String, Long>();
    //long bootstrapOffset = getTimeslotDuration() *
    //                       (getBootstrapDiscardedTimeslots() +
    //                        getBootstrapTimeslotCount());
    //Instant simBase = 
    //    getSimulationBaseTime().plus(bootstrapOffset);
    result.put("base", getSimulationBaseTime().toEpochMilli());
    result.put("rate", getSimulationRate());
    result.put("modulo", getSimulationModulo());
    return result;
  }

  /**
   * The Brokers who are participating in this Competion.
   */
  public List<String> getBrokers ()
  {
    return brokers;
  }

  /**
   * Adds a broker to the Competition. This only makes sense in the server
   * environment.
   */
  @StateChange
  public Competition addBroker (String brokerUsername)
  {
    brokers.add(brokerUsername);
    return this;
  }

  /**
   * The list of customers (or more precisely, customer models) in the 
   * simulation environment.
   */
  public List<CustomerInfo> getCustomers ()
  {
    return customers;
  }
  
  /**
   * Adds a customer to the Competition. This only makes sense in the server
   * environment.
   */
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
    withBootstrapDiscardedTimeslots(template.getBootstrapDiscardedTimeslots());
    withSimulationModulo(template.getSimulationModulo());
    withTimeslotLength(template.getTimeslotLength());
    withTimeslotsOpen(template.getTimeslotsOpen());
  }
  
  @Override
  public String toString() 
  {
    return name;
  }
}
