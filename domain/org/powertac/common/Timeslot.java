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

//import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.joda.time.Instant;
import com.thoughtworks.xstream.annotations.*;

 /**
 * A timeslot instance describes a duration in time (slot). Time slots are used (i) to
 * correlate tradeable products (energy futures) and trades in the market with a future time
 * interval where settlement (i.e. delivery / consumption) has to take place, (ii) to
 * correlate meter readings with a duration in time, (iii) to  allow tariffs to define
 * different consumption / production prices for different times of a day. Timeslots are
 * represented in server-broker communications by serial number and start time.
 *
 * @author Carsten Block
 * @version 1.0 - Feb,6,2011
 */
@XStreamAlias("slot")
public class Timeslot //implements Serializable 
{  
  @XStreamOmitField
  private String id = IdGenerator.createId();

  /**
   * used to find succeeding / preceding timeslot instances
   * @see {@code Timeslot.next()} {@code Timeslot.previous()}
   */
  @XStreamAsAttribute
  private int serialNumber;

  /** flag that determines enabled state of the slot. 
   * E.g. in the market only orders for enabled timeslots 
   * are accepted. */
  @XStreamOmitField
  private boolean enabled = false;

  /** start date and time of the timeslot */
  private Instant startInstant;

  /** end date and time of the timeslot */
  @XStreamOmitField
  private Instant endInstant;
  
  //@XStreamOmitField
  //List<Orderbook> orderbooks
  
  public Timeslot (int serial, Instant start, Instant end)
  {
    super();
    serialNumber = serial;
    startInstant = start;
    endInstant = end;
  }

  public String getId ()
  {
    return id;
  }

  public int getSerialNumber ()
  {
    return serialNumber;
  }

  public boolean isEnabled ()
  {
    return enabled;
  }

  public Instant getStartInstant ()
  {
    return startInstant;
  }

  public Instant getEndInstant ()
  {
    return endInstant;
  }

  public String toString() {
    return "${serialNumber}: ${startInstant} - ${endInstant} (${enabled})";
  }

  // TODO - all of this goes to the repository
  /**
   * Note that this scheme for finding the current timeslot relies on the
   * time granularity reported by the timeService being the same as the length
   * of a timeslot.
   */
//  public static Timeslot currentTimeslot () 
//  {
//    return Timeslot.findByStartInstant(timeService.currentTime)
//  }
//  
//  public static List<Timeslot> enabledTimeslots ()
//  {
//    return Timeslot.findAllByEnabled(true)
//  }
//
//  public Timeslot next() {
//    return Timeslot.findBySerialNumber(this.serialNumber + 1)
//  }
//
//  public Timeslot previous() {
//    return Timeslot.findBySerialNumber(this.serialNumber - 1)
//  }
}
