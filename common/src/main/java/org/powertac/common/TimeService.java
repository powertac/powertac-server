/*
* Copyright (c) 2011 - 2014 by John Collins
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

package org.powertac.common;

import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.configuration2.MapConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import org.powertac.common.state.StateChange;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

 /**
 * This is the simulation time-keeper and event queue. Here's how it works:
 * <ul>
 * <li>You create it with four parameters: (base, start, rate, modulo), defined as
 *   <ul>
 *   <li><strong>base</strong> is the start time of the simulation scenario. So if we
 *     are simulating a period in the summer of 2007, base might be 2007-06-21-12:00.</li>
 *   <li><strong>start</strong> is the start time of the simulation run.</li>
 *   <li><strong>rate</strong> is the time-compression ratio for the simulation. So if we are
 *     running one-hour timeslots every 5 seconds, the rate would be 720.</li>
 *   <li><strong>modulo</strong> controls the values of simulation time values reported. If
 *     we are running one-hour timeslots, then the modulo should be one hour, expressed
 *     in milliseconds. If we are running one-hour timeslots but want to update time every
 *     30 minutes of simulated time, then the modulo would be 30*60*1000. Note that
 *     this will not work correctly unless the calls to updateTime() are made at
 *     modulo/rate intervals. Also note that the reported time is computed as
 *     rawTime - rawTime % modulo, which means it will never be ahead of the raw 
 *     simulation time.</li>
 *   </ul></li>
 * <li>Some process periodically calls updateTime(), at least once every simulated hour. This
 *   sets the currentTime property to the correct scenario time as<br/>
 *   <code>currentTime = base + (systemTime - start) * rate</code><br/>
 *   and runs any simulation actions that are due.</li>
 * <li>If you want something to happen sometime in the future, you add an action by
 *   calling addAction(time, action). Assuming time is not in the past, then the action
 *   (a closure with a single argument, the current time) is added to a time-ordered queue, 
 *   and will be run when its time arrives.</li>
 * </ul>
 * Note that all times are absolute times represented as UTC; timezone offset is 0, and there
 * is no "daylight-saving" time. If we want to represent the effects of DST, we'll have to have
 * our customers wake up earlier in the summertime.
 * @author John Collins
 */
@Scope("singleton")
@Service
public class TimeService 
{
  static private Logger log = LogManager.getLogger(TimeService.class.getName());

  public static final long SECOND = 1000l;
  public static final long MINUTE = SECOND * 60;
  public static final long HOUR = MINUTE * 60;
  public static final long DAY = HOUR * 24;
  public static final long WEEK = DAY * 7;
  
  // simulation clock parameters
  private long base;
  private long start = ZonedDateTime.of(2036, 12, 31, 23, 59, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
  private long rate = 720l;
  private long modulo = HOUR;
  
  // busy flag, to prevent overlap
  private boolean busy = false;

  // simulation action queue
  private TreeSet<SimulationAction> actions;

  // the current time
  private Instant currentTime;
  private ZonedDateTime currentDateTime;
  
  // debug code -- keep track of TimeService instances
  private static TimeService instance;
  private static int index = 0;
  private int id;
  
  /**
   * Default constructor. You need to set base, rate, start, and modulo before using it.
   */
  public TimeService ()
  {
    super();
    id = index++;
    instance = this;
  }
  
  /**
   * Returns the most-recently created instance of TimeService. Intended
   * to be used only for testing in Spring-free environment. Note that this
   * is not a Singleton-type instance getter - it will not create an instance.
   */
  public static TimeService getInstance ()
  {
    return instance;
  }
  
  /**
   * Handy constructor for testing
   */
  public TimeService (long base, long start, long rate, long modulo)
  {
    super();
    this.base = base;
    this.start = start;
    this.rate = rate;
    this.modulo = modulo;
    id = index++;
    instance = this;
  }

  /**
   * Sets current time to modulo before the desired start time. 
   * This allows scheduling
   * of actions to take place on the first tick, which should be at the
   * desired start time. Call this after setting clock parameters.
   */
  public void init (Instant start)
  {
    currentTime = Instant.ofEpochMilli(start.toEpochMilli() - modulo);
    currentDateTime = ZonedDateTime.ofInstant(currentTime, ZoneOffset.UTC);
  }
  
  /**
   * Updates simulation time when called as specified by clock
   * parameters, then runs any actions that may be due.
   */
  public void updateTime () 
  {
    if (busy) {
      //log.info "Timer busy";
      start += HOUR / rate;
      return;
    }
    busy = true;
    setCurrentTime();
    runActions();
    busy = false;
  }
  
  /**
   * Returns the most recent Instant at which time % modulo was zero 
   */
  public Instant truncateInstant (Instant time, long mod)
  {
    long ms = time.toEpochMilli();
    return Instant.ofEpochMilli(ms - ms % mod);
  }

  /**
   * Sets base, rate, and modulo clock parameters with a single call. We
   * do not set start at this point, because it is changed whenever the clock
   * is paused and therefore needs to be treated separately.
   */
  public void setClockParameters (long base, long rate, long modulo)
  {
    this.base = base;
    this.rate = rate;
    this.modulo = modulo;
  }
  
  /**
   * Sets base, rate, and modulo parameters from a map, which is wrapped
   * in a Configuration to allow for conversions and default values. The 
   * map must contain entries named "base", "rate", and "modulo". We also init
   * the clock here, because we have all the parameters.
   */
  public void setClockParameters (Map<String, Long> params)
  {
    MapConfiguration config = new MapConfiguration(params);
    this.base = config.getLong("base", base);
    this.rate = config.getLong("rate", rate);
    this.modulo = config.getLong("modulo", modulo);
  }
  
  /**
   * Sets base, rate, and modulo from a Competition instance
   */
  public void setClockParameters (Competition comp)
  {
    setClockParameters(comp.getClockParameters());
  }
  
  public long getBase ()
  {
    return base;
  }
  
  public Instant getBaseInstant ()
  {
    return Instant.ofEpochMilli(base);
  }

  /**
   * @deprecated  use {@link #setClockParameters} instead
   */
  @Deprecated
  public void setBase (long value)
  {
    base = value;
  }

  public long getStart ()
  {
    return start;
  }

  public void setStart (long start)
  {
    this.start = start;
    //setCurrentTime();
  }

  public long getRate ()
  {
    return rate;
  }
  
  /**
   * @deprecated  use {@link #setClockParameters} instead
   */
  @Deprecated
  public void setRate (long value)
  {
    rate = value;
  }

  public long getModulo ()
  {
    return modulo;
  }
  
  /**
   * @deprecated  use {@link #setClockParameters} instead
   */
  @Deprecated
  public void setModulo (long value)
  {
    modulo = value;
  }

  /**
   * Returns the current time as an Instant
   */
  public Instant getCurrentTime() 
  {
    return currentTime;
  }
  
  public ZonedDateTime getCurrentDateTime()
  {
    return currentDateTime;
  }
  
  /**
   * Returns the current hour-of-day
   */
  public int getHourOfDay()
  {
    return currentDateTime.getHour();
  }

  /**
   * Sets current time to a specific value. Intended for testing purposes only.
   */
  @StateChange
  public void setCurrentTime (Instant time)
  {
    log.debug("ts" + id + " setCurrentTime to " + time.toString());
    currentTime = time;
    currentDateTime = ZonedDateTime.ofInstant(time, ZoneOffset.UTC);
  }
  
  /**
   * Computes and returns the offset in msec between the current system time
   * and the current clock time.
   */
  public long getOffset ()
  {
    long systemTime = Instant.now().toEpochMilli();
    long simTime = (currentTime.toEpochMilli() - base) / rate + start;
    return systemTime - simTime;
  }
  
  /**
   * Sets current time to a specific value. Intended for testing purposes only.
   */
  @StateChange
  protected void setCurrentTime (ZonedDateTime time)
  {
    log.debug("ts" + id + " setCurrentTime to " + time.toString());
    setCurrentTime(time.toInstant());
    //currentTime = new Instant(time);
    //currentDateTime = new DateTime(time, DateTimeZone.UTC);
  }

  public void setCurrentTime() 
  {
    Instant now = Instant.now();
    long systemTime = now.toEpochMilli();

    if (systemTime >= start) {
      long raw = base + (systemTime - start) * rate;
      long adjustedRaw = raw - raw % modulo;
      log.debug("ts" + id + " updateTime: sys=" + systemTime +
          ", simTime=" + currentTime);

      setCurrentTime(Instant.ofEpochMilli(adjustedRaw));
    }
  }

  /**
   * Adds an action to the simulation queue, to be triggered at the specified time.
   */
  public void addAction (Instant time, TimedAction act)
  {
    if (actions == null)
      actions = new TreeSet<SimulationAction>();
    actions.add(new SimulationAction(time, act));
  }
  
  /**
   * Runs any actions that are due at the current simulated time.
   */
  void runActions ()
  {
    if (actions == null)
      return;
    while (!actions.isEmpty() && !actions.first().atTime.isAfter(currentTime)) {
      SimulationAction act = actions.first();
      act.action.perform(currentTime);
      actions.remove(act);
    }
  }

  class SimulationAction implements Comparable<SimulationAction>
  {
    public Instant atTime;
    TimedAction action;

    public SimulationAction (Instant time, TimedAction action)
    {
      this.atTime = time;
      this.action = action;
    }
    
    @Override
    public int compareTo (SimulationAction obj)
    {
      return atTime.compareTo(((SimulationAction)obj).atTime);
    }
  }
}
