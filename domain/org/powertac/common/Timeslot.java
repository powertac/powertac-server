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
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import com.thoughtworks.xstream.annotations.*;

 /**
 * A timeslot instance describes an interval of time (slot) for which power may be
 * traded in the wholesale market. Time slots are used (i) to
 * correlate tradeable products (energy futures) and trades in the market with a future time
 * interval where settlement (i.e. delivery / consumption) has to take place, (ii) to
 * correlate meter readings with a duration in time, (iii) to  allow tariffs to define
 * different consumption / production prices for different times of a day. Timeslots are
 * represented in server-broker communications by serial number and start time.
 * <p>
 * This is an immutable type, so no state logging is needed. Creation events are logged
 * by the repository.</p>
 *
 * @author Carsten Block
 * @version 1.0 - Feb,6,2011
 */
@Domain
@XStreamAlias("slot")
public class Timeslot
{
  static private Logger log = Logger.getLogger(Timeslot.class.getName());

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
  
  /** previous and next timeslots */
  @XStreamOmitField
  private Timeslot previous;

  @XStreamOmitField
  private Timeslot next;

  /** 
   * Constructor is intended to be called by repository.
   * Note that Timeslots are created in sequence, and are initially enabled. If you
   * want to create a disabled timeslot, you have to call disable() after creating it.
   */
  public Timeslot (int serial, Instant start, Instant end, Timeslot previous)
  {
    super();
    serialNumber = serial;
    startInstant = start;
    endInstant = end;
    enabled = true;
    if (previous != null) {
      // special case for first timeslot - should never happen
      if (!previous.endInstant.equals(start)) {
        log.error("Timeslot " + serial + ": start:" + start.toString() +
                  " != previous.end:" + previous.endInstant);
        serialNumber = -1;
      }
      else {
        this.previous = previous;
        previous.next =  this;
      }
    }
  }

  public int getSerialNumber ()
  {
    return serialNumber;
  }

  public boolean isEnabled ()
  {
    return enabled;
  }
  
  @StateChange
  public void enable ()
  {
    enabled = true;
  }
  
  @StateChange
  public void disable ()
  {
    enabled = false;
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
    return ("timeslot " + serialNumber + ":" + startInstant.toString() +
            " - " + endInstant.toString() + "(" + 
            (enabled ? "enabled" : "disabled") + ")");
  }

  public Timeslot getNext() {
    return next;
  }

  public Timeslot getPrevious() {
    return previous;
  }
}
