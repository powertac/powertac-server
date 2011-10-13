/*
* Copyright (c) 2011 by the original author
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.powertac.common.repo;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;

import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Repository for Timeslots and their Orderbooks
 * @author John Collins
 */
@Repository
public class TimeslotRepo implements DomainRepo
{
  static private Logger log = Logger.getLogger(TimeslotRepo.class.getName());
  static private Logger stateLog = Logger.getLogger("State");

  private int timeslotIndex = 0;
  private Timeslot first;
  private Timeslot last;
  private Timeslot firstEnabled;
  private Timeslot current;
  private ArrayList<Timeslot> indexedTimeslots;

  @Autowired
  TimeService timeService;
  
  /** standard constructor */
  public TimeslotRepo ()
  {
    super();
    indexedTimeslots = new ArrayList<Timeslot>();
  }
  
  /** 
   * Creates a timeslot with the given start time. It is assumed that timeslots are
   * created in sequence; therefore  the sequence number of the new timeslot is simply the 
   * count of timeslots already created, and an error will be logged if the start time of 
   * a new timeslot is not equal to the end time of the last timeslot in the list.
   */
  public Timeslot makeTimeslot (Instant startTime, Instant endTime)
  {
    if (last != null && !last.getEndInstant().isEqual(startTime)) {
      log.error("Timeslot " + (timeslotIndex + 1) + ": start:" + startTime.toString() +
                " != previous.end:" + last.getEndInstant());
      return null;
    }
    Timeslot result = new Timeslot(timeslotIndex, startTime, endTime, last);
    if (result.getSerialNumber() == -1) // big trouble
      return null;
    if (first == null)
      first = result;
    last = result;
    indexedTimeslots.add(timeslotIndex, result);
    timeslotIndex += 1;
    return result;
  }
  
  /**
   * Note that this scheme for finding the current timeslot relies on a timeslot
   * sequence that does not have gaps between sim start and the current time.
   */
  public Timeslot currentTimeslot () 
  {
    if (first == null)
      return null;
    Instant time = timeService.getCurrentTime();
    if (current != null && current.getStartInstant().isEqual(time)) {
      return current;
    }
    Timeslot test = first;
    while (test != null && test.getStartInstant().isBefore(time)) {
      test = test.getNext();
    }
    current = test;
    return current;
  }

  /**
   * Returns the timeslot (if any) with the given serial number.
   */
  public Timeslot findBySerialNumber (int serialNumber)
  {
    if (serialNumber >= indexedTimeslots.size())
      return null;
    else
      return indexedTimeslots.get(serialNumber);
  }

  /**
   * Returns the list of enabled timeslots, starting with the first by serial number.
   * This code depends on the set of enabled timeslots being contiguous in the serial
   * number sequence, and on a disabled timeslot never being re-enabled.
   */
  public List<Timeslot> enabledTimeslots ()
  {
    if (first == null)
      return null;
    if (firstEnabled == null)
      firstEnabled = first;
    while (!firstEnabled.isEnabled())
      firstEnabled = firstEnabled.getNext();
    if (firstEnabled == null) {
      log.error("ran out of timeslots looking for first enabled");
      return null;
    }
    ArrayList<Timeslot> result = new ArrayList<Timeslot>(30);
    Timeslot ts = firstEnabled;
    while (ts != null && ts.isEnabled()) {
      result.add(ts);
      ts = ts.getNext();
    }
    return result;
  }
  
  /**
   * Returns the number of timeslots that have been successfully created.
   */
  public int count ()
  {
    return indexedTimeslots.size();
  }

  public void recycle ()
  {
    timeslotIndex = 0;
    first = null;
    last = null;
    firstEnabled = null;
    current = null;
    indexedTimeslots.clear();
  }

}
