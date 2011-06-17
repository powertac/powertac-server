/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.server;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

import org.powertac.common.TimeService;

/**
 * Timer-based clock management for the Power TAC simulator. This is a
 * singleton class, but it must be initialized with a reference to the 
 * timeService in order to do its job. Therefore, it is an error to 
 * attempt to retrieve the instance before initialization.
 * <p>
 * The basic design for this scheme is given at 
 * https://github.com/powertac/powertac-server/wiki/Time-management.
 * The goal is to produce a tick by calling timeService.updateTime() 
 * every n seconds, where n is 3600/timeService.rate. So if 
 * timeService.rate is 360, this would be a tick every 10 seconds.
 * Current simulation time is given by the formula
 *   time = rate * (systemTime - start) + base
 * where start, base, and rate are simulation parameters.</p>
 * <p>
 * We assume that the simulator is a single thread, although it may
 * control other threads. That thread waits for the next tick, then
 * does some work. Ideally, that work will be completed in fewer than
 * n seconds, but occasionally it may take longer. Since the broker 
 * needs to keep track of time on its own, it needs to know when this
 * happens, and must be informed of the updated start time so that it
 * can compute a simulation time that agrees with the server. In
 * addition, a broker might want to pause the clock in order to allow
 * a user to fill out a dialog or otherwise interact with a user. Given
 * an appropriate set of messages, this event could be handled in much
 * the same way as a server timeslot overrun.</p>
 * 
 * @author John Collins
 */
public class SimulationClockControl
{
  public enum Status { CLEAR, COMPLETE, PAUSED }
  
  private final long postPauseDelay = 500l; // 500 msec
  
  private TimeService timeService;
  private Status state = Status.CLEAR;
  
  private long base;
  private long start;
  private long rate;
  private long modulo;
  
  private Timer theTimer;
  private TickAction tickAction;
  private WatchdogAction watchdogAction;

  private SimulationClockControl (TimeService timeService)
  {
    super();
    this.timeService = timeService;
    theTimer = new Timer();
    tickAction = new TickAction();
    watchdogAction = new WatchdogAction();
  }
  
  /**
   * Sets the sim clock start time, which in turn gets propagated to the
   * timeService.
   */
  public void setStart (long start)
  {
    this.start = start;
    timeService.setStart(start);
  }

  /**
   * Schedules the next tick. The interval between now and the next tick is
   * determined by comparing the current system time with what the time should
   * be on the next tick.
   */
  public void scheduleTick ()
  {
    theTimer.schedule(tickAction, 
                      computeNextTickTime() - new Date().getTime());
  }
  
  /**
   * Indicates that the simulator has completed its work on the current
   * timeslot. If the sim was paused, then un-pause it.
   */
  public synchronized void complete ()
  {
    if (state == Status.PAUSED) {
      // compute new start time, communicate it to brokers, and re-start
      // the clock.
      long originalNextTick = computeNextTickTime();
      long actualNextTick = new Date().getTime() + postPauseDelay;
      start += actualNextTick - originalNextTick;
      timeService.setStart(start);
      // TODO: communicate to brokers somehow
      scheduleTick();
   }
    setState(Status.COMPLETE);
  }
  
  /**
   * Pauses the clock and notifies brokers just in case the current state
   * is not COMPLETE, otherwise schedules the next tick. This method and 
   * the complete() method are synchronized to protect against complete()
   * being called before state is set to paused.
   */
  private synchronized void pauseMaybe ()
  {
    if (state == Status.CLEAR) {
      // sim thread is not finished
      setState(Status.PAUSED);
      // TODO: communicate pause to brokers
    }
    else if (state == Status.COMPLETE) {
      // sim finished - schedule the next tick
      scheduleTick();
    }
  }

  private synchronized void setState (Status newState)
  {
    state = newState;
  }
  
  private synchronized Status getState ()
  {
    return state;
  }

  private long computeNextTickTime ()
  {
    long simTime = timeService.getCurrentTime().getMillis();
    long nextSimTime = simTime + modulo;
    return start + (nextSimTime - base) / rate;
  }
  
  // ---- Singleton methods ----
  private static SimulationClockControl instance;

  /**
   * Creates the instance and sets the reference to the timeService.
   */
  public static void initialize (TimeService timeService)
  {
    instance = new SimulationClockControl(timeService);
  }
  
  /**
   * Returns the instance, which of course will be null if the singleton
   * is not yet initialized.
   * @return
   */
  public static SimulationClockControl getInstance ()
  {
    return instance;
  }
  
  private class TickAction extends TimerTask
  {

    @Override
    public void run ()
    {
      // TODO Auto-generated method stub
      
    }
  }
  
  private class WatchdogAction extends TimerTask
  {

    @Override
    public void run ()
    {
      // TODO Auto-generated method stub
      
    }
  }
}
