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
  public enum Status { CLEAR, COMPLETE, PAUSED, STOPPED }
  
  private static final long postPauseDelay = 500l; // 500 msec
  private static final long watchdogSlack = 200l; // 200 msec
  
  private TimeService timeService;
  private long base;
  private long start;
  private long rate;
  private long modulo;

  private Status state = Status.CLEAR; // package visibility for testing
  private int nextTick = -1;
  
  private Timer theTimer;
  //private TickAction tickAction;
  private WatchdogAction currentWatchdog;

  private SimulationClockControl (TimeService timeService)
  {
    super();
    this.timeService = timeService;
    this.base = timeService.getBase();
    this.rate = timeService.getRate();
    this.modulo = timeService.getModulo();
    theTimer = new Timer();
    //tickAction = new TickAction();
    //watchdogAction = new WatchdogAction();
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
    System.out.println("scheduleTick() " + new Date().getTime());
    long nextTick = computeNextTickTime();
    theTimer.schedule(new TickAction(), new Date(nextTick));
  }
  
  /**
   * Indicates that the simulator has completed its work on the current
   * timeslot. If the sim was paused, then un-pause it. On the last tick,
   * call stop() rather than complete().
   */
  public synchronized void complete ()
  {
    System.out.println("complete() " + new Date().getTime());
    if (state == Status.PAUSED) {
      // compute new start time, communicate it to brokers, and re-start
      // the clock.
      long originalNextTick = computeNextTickTime();
      long actualNextTick = new Date().getTime() + postPauseDelay;
      start += actualNextTick - originalNextTick;
      timeService.setStart(start);
      // TODO: communicate to brokers somehow
      state = Status.PAUSED;
      scheduleTick();
   }
    state = Status.COMPLETE;
  }
  
  /**
   * Stops the clock. Call this method when processing on the last tick is
   * finished.
   */
  public synchronized void stop ()
  {
    System.out.println("Stop at " + new Date().getTime());
    state = Status.STOPPED;
    if (currentWatchdog != null) {
      currentWatchdog.cancel();
      currentWatchdog = null;
    }
  }
  
  /**
   * Blocks the caller until the next tick.
   */
  public synchronized void waitForTick (int n)
  {
    // Can we guarantee this is called BEFORE the corresponding notifyTick()?
    System.out.println("nextTick=" + nextTick + ", n=" + n);
    while (nextTick < n) {
      try {
        wait();
      }
      catch (InterruptedException ie) { }
    }
  }
  
  /**
   * notifies the waiting thread (if any).
   */
  private synchronized void notifyTick ()
  {
    nextTick += 1;
    notifyAll();
  }
  
  /**
   * Pauses the clock and notifies brokers just in case the current state
   * is CLEAR, otherwise if the state is COMPLETE, schedules the next tick. 
   * No action is taken if the current state is PAUSED or STOPPED, thereby
   * effectively stopping the clock. 
   * This method and the complete() method are synchronized to protect 
   * against complete() being called before state is set to paused.
   */
  private synchronized void pauseMaybe ()
  {
    System.out.println("pauseMaybe() " + new Date().getTime());
    if (state == Status.CLEAR) {
      // sim thread is not finished
      System.out.println("pausing");
      state = Status.PAUSED; // clock resumed by calling complete()
      // TODO: communicate pause to brokers
    }
    else if (state == Status.COMPLETE) {
      // sim finished - schedule the next tick
      scheduleTick();
    }
  }

  synchronized Status getState () // package visibility for test support
  {
    return state;
  }

  private long computeNextTickTime ()
  {
    long current = new Date().getTime();
    if (current < start) {
      // first tick is special
      System.out.println("first tick at " + start + "; current is " + current);
      return start;
    }
    else {
      // second and subsequent ticks
      long simTime = timeService.getCurrentTime().getMillis();
      long nextSimTime = simTime + modulo;
      long nextTick = start + (nextSimTime - base) / rate; 
      System.out.println("next tick: time " + current + "; next tick at " + nextTick);
      return nextTick;
    }
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
    /**
     * Runs a tick - updates the timeService, clears the state,
     * schedules the watchdog, and notifies the simulator. The
     * monitor object is the singleton.
     */
    @Override
    public void run ()
    {
      System.out.println("TickAction.run() " + new Date().getTime());
      timeService.updateTime();
      state = Status.CLEAR;
      long wdTime = computeNextTickTime() - watchdogSlack;
      System.out.println("watchdog set for " + wdTime);
      currentWatchdog = new WatchdogAction();
      theTimer.schedule(currentWatchdog, new Date(wdTime));
      notifyTick();
    }
  }
  
  private class WatchdogAction extends TimerTask
  {
    /**
     * Checks for sim task completion by calling pauseMaybe on the
     * instance.
     */
    @Override
    public void run ()
    {
      System.out.println("WatchdogAction.run() " + new Date().getTime());
      instance.pauseMaybe();
      currentWatchdog = null;
    }
  }
}
