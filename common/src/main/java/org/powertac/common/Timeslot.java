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

package org.powertac.common;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

 /**
 * A timeslot instance describes an interval of time (slot) for which power may be
 * traded in the wholesale market. The duration of a timeslot is given by
 * <code>currentCompetition.getTimeslotLength()</code>. 
 * Timeslots are uniquely identified
 * by their serial numbers, which is the number of timeslots since
 * <code>currentCompetition.getSimulationBaseTime()</code>.
 * <p>
 * Timeslots are used (i) to
 * correlate tradeable products (energy futures) and trades in the market with a future time
 * interval where settlement (i.e. delivery / consumption) has to take place, (ii) to
 * correlate meter readings with a duration in time, (iii) to  allow tariffs to define
 * different consumption / production prices for different times of a day. Timeslots are
 * represented in server-broker communications by serial number.
 * <p>
 * This is an immutable type, so no state logging is needed. Creation events are logged
 * by the repository.</p>
 *
 * @author Carsten Block, John Collins
 */
//@Domain
@XStreamAlias("slot")
public class Timeslot implements Comparable<Object>
{
  /** Timeslot does not have ID; it is logged by serial number **/
  //@XStreamAsAttribute
  //private long id = IdGenerator.createId();

  /**
   * Index of this timeslot from beginning of sim.
   */
  @XStreamAsAttribute
  private int serialNumber;

  /** start date and time of the timeslot */
  @XStreamOmitField
  private Instant startInstant;
  
  /** ZonedDateTime equivalent - lazy eval **/
  @XStreamOmitField
  private ZonedDateTime startTime = null;

  /** 
   * Constructor is intended to be called by repository.
   * Note that Timeslots are initially enabled. If you
   * want to create a disabled timeslot, you have to call disable() after creating it.
   */
  public Timeslot (int serial, Instant start)
  {
    super();
    serialNumber = serial;
    startInstant = start;
  }

  public int getSerialNumber ()
  {
    return serialNumber;
  }
  
  public long getId()
  {
    return (long)serialNumber;
  }

  public Instant getStartInstant ()
  {
    return startInstant;
  }

  public Instant getEndInstant ()
  {
    return startInstant.plusMillis(Competition.currentCompetition().getTimeslotDuration());
  }
  
  /**
   * Returns the ZonedDateTime representation of the start time for this timeslot
   */
  public ZonedDateTime getStartTime ()
  {
    if (null == startTime)
      startTime = ZonedDateTime.ofInstant(startInstant, ZoneOffset.UTC);
    return startTime;
  }
  
  /**
   * Returns the timeslot index since the most recent midnight, starting at
   * zero. Note that this is hourOfDay if timeslots are one hour. Assumes tz = 0.
   */
  public int slotInDay ()
  {
    long millis = getStartTime().toLocalTime().toNanoOfDay() / 1_000_000;
    return (int) (millis/Competition.currentCompetition().getTimeslotDuration());
  }
  
  /**
   * Returns the day of week for the start of this timeslot, starting at 
   * Monday = 1.
   */
  public int dayOfWeek ()
  {
    return getStartTime().getDayOfWeek().getValue();
  }

  @Override
  public String toString() {
    return ("timeslot " + serialNumber + ":" + startInstant.toString());
  }

  @Override
  public int compareTo (Object arg)
  {
    return serialNumber - ((Timeslot)arg).getSerialNumber();
  }
}
