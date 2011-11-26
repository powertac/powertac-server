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

import org.powertac.common.Competition;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Repository for Timeslots. Timeslots are created with makeTimeslot(). Several
 * query methods are supported, including currentTimeslot(), enabledTimeslots(),
 * and findBySerialNumber(). The implementation makes a strong assumption that
 * timeslots are created in sequence, and that each timeslot starts when the
 * previous timeslots ends.
 * @author John Collins
 */
@Repository
public class TimeslotRepo implements DomainRepo
{
  static private Logger log = Logger.getLogger(TimeslotRepo.class.getName());

  // local state
  private int timeslotIndex = 0;
  private Timeslot first;
  private Timeslot last;
  private Timeslot firstEnabled;
  private Timeslot current;
  private ArrayList<Timeslot> indexedTimeslots;

  @Autowired
  private TimeService timeService;
  
  /** standard constructor */
  public TimeslotRepo ()
  {
    super();
    indexedTimeslots = new ArrayList<Timeslot>();
  }

  /** 
   * Creates a timeslot with the given start time. It is assumed that timeslots are
   * created in sequence; therefore  the sequence number of the new timeslot is simply the 
   * count of timeslots already created, and an error will be logged (and null
   * returned) if the start time of 
   * a new timeslot is not equal to the end time of the last timeslot in the list.
   * Note that new timeslots are always created in the "enabled" state.
   */
  public Timeslot makeTimeslot (Instant startTime)
  {
    long duration = Competition.currentCompetition().getTimeslotDuration();
    log.debug("makeTimeslot" + startTime.toString());
    Instant lastStart = startTime.minus(duration);
    if (last != null && !last.getStartInstant().isEqual(lastStart)) {
      log.error("Timeslot " + (timeslotIndex + 1) + ": start:" + startTime.toString() +
                " != previous.end:" + lastStart.plus(duration));
      return null;
    }
    Timeslot result = new Timeslot(timeslotIndex, startTime, last);
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
    current = findByInstant(time);
    log.debug("current: " + current.toString());
    return current;
  }

  /**
   * Returns the timeslot (if any) with the given serial number.
   */
  public Timeslot findBySerialNumber (int serialNumber)
  {
    log.debug("find sn " + serialNumber);
    //Timeslot result = null;
    int index = serialNumber;
    if (index >= indexedTimeslots.size())
      return null;
    else
      return indexedTimeslots.get(index);
  }

  /**
   * Returns the timeslot (if any) with the given serial number.
   */
  public Timeslot findOrCreateBySerialNumber (int serialNumber)
  {
    log.debug("find or create sn " + serialNumber);
    Timeslot result = findBySerialNumber(serialNumber);
    if (result != null)
      return result;
    else if (serialNumber < 0) {
      log.error("FindOrCreate: serial number " + serialNumber + " < 0");
      return null;
    }
    else if (serialNumber < count()) {
      log.error("FindOrCreate: serial number "
                + serialNumber + " < count " + count());
      return null;
    }
    else if (last == null) {
      log.error("FindOrCreate: no last timeslot");
      return null;
    }
    else {
      // at this point, the serial number should be >= count 
      for (int sn = last.getSerialNumber() + 1; sn <= serialNumber; sn++) {
        last = new Timeslot(sn, last.getEndInstant(), last);
        indexedTimeslots.add(sn, last);
      }
      return last;
    }
  }
  
  /**
   * Returns the timeslot (if any) corresponding to a particular Instant.
   */
  public Timeslot findByInstant (Instant time)
  {
    log.debug("find " + time.toString());
    long offset = time.getMillis() - first.getStartInstant().getMillis();
    long duration = Competition.currentCompetition().getTimeslotDuration();
    // truncate to timeslot boundary
    offset -= offset % duration;
    int index = (int)(offset / duration);
    return findBySerialNumber(index);
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
    while (firstEnabled != null && !firstEnabled.isEnabled())
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
    log.debug("recycle");
    timeslotIndex = 0;
    first = null;
    last = null;
    firstEnabled = null;
    current = null;
    indexedTimeslots.clear();
  }

}
