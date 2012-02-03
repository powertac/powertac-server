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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.powertac.common.TimeService;
import org.springframework.beans.factory.annotation.Autowired;

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
  static private Logger log = Logger.getLogger(SimulationClockControl.class);

  public enum Status { CLEAR, COMPLETE, DELAYED, PAUSED, STOPPED }
  
  private static final long postPauseDelay = 500l; // 500 msec
  private static final long watchdogSlack = 200l; // 200 msec
  
  @Autowired
  private TimeService timeService;
  
  @Autowired
  private CompetitionControlService competitionControl;

  private long base;
  private long start;
  private long rate;
  private long modulo;

  private Status state = Status.CLEAR; // package visibility for testing
  private int nextTick = -1;
  private boolean pauseRequested = false;
  
  private Timer theTimer;
  private WatchdogAction currentWatchdog;
  
  private Set<Semaphore> waitUntilStopSemaphores;
  
  // ------------- Singleton methods -------------
  private static SimulationClockControl instance;

  /**
   * Creates the instance and sets the reference to the timeService.
   */
  public static void initialize (CompetitionControlService competitionControl,
                                 TimeService timeService)
  {
    instance = new SimulationClockControl(competitionControl, timeService);
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

  private SimulationClockControl (CompetitionControlService competitionControl,
                                  TimeService timeService)
  {
    super();
    this.competitionControl = competitionControl;
    this.timeService = timeService;
    this.base = timeService.getBase();
    this.rate = timeService.getRate();
    this.modulo = timeService.getModulo();
    theTimer = new Timer();
    this.waitUntilStopSemaphores = Collections.synchronizedSet(new HashSet<Semaphore>());

  }
  
  // --------------- external api ----------------
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
    //System.out.println("scheduleTick() " + new Date().getTime());
    long nextTick = computeNextTickTime();
    boolean success = false;
    while (!success) {
      try {
        theTimer.schedule(new TickAction(this), new Date(nextTick));
        success = true;
      }
      catch (IllegalStateException ise) {
        //System.out.println("Timer failure: " + ise.toString());
        // at this point there should be no outstanding timer tasks
        theTimer = new Timer();
      }
    }
  }
  
  /**
   * Indicates that the simulator has completed its work on the current
   * timeslot. If the sim was delayed, then resume it. On the last tick,
   * client must call stop() rather than complete().
   */
  public synchronized void complete ()
  {
    //System.out.println("complete() " + new Date().getTime());
    if (state == Status.DELAYED) {
      if (pauseRequested) {
        // already paused, just change the state
        state = Status.PAUSED;
        pauseRequested = false;
        return;
      }
      else
        resume();
    }
    // let watchdog start the next tick
    state = Status.COMPLETE;
  }
  
  /**
   * Stops the clock. Call this method when processing on the last tick is
   * finished.
   */
  public synchronized void stop ()
  {
    //System.out.println("Stop at " + new Date().getTime());
    state = Status.STOPPED;
    if (currentWatchdog != null) {
      currentWatchdog.cancel();
      currentWatchdog = null;
    }
    for (Semaphore sem : waitUntilStopSemaphores) {
      sem.release();
    }
    waitUntilStopSemaphores.clear();
  }
  
  public void waitUntilStop() {
    Status state = getState();
    if (state != Status.STOPPED) {
      Semaphore sem = new Semaphore(0);
      waitUntilStopSemaphores.add(sem);
      try {
        sem.acquire();
      }
      catch (InterruptedException e) {
        log.info("Who dares wake me up??", e);
      }
    }
  }
  
  /**
   * Blocks the caller until the next tick.
   */
  public synchronized void waitForTick (int n)
  {
    // Can we guarantee this is called BEFORE the corresponding notifyTick()?
    //System.out.println("waitForTick() nextTick=" + nextTick + ", n=" + n);
    while (nextTick < n) {
      try {
        wait();
      }
      catch (InterruptedException ie) { }
    }
  }
  
  /**
   * Serves an external pause request. 
   */
  public synchronized void requestPause ()
  {
    // just set the flag. We'll pay attention to it the next
    // time complete() is called.
    pauseRequested = true;
  }
  
  /**
   * Releases an externally-requested pause.
   */
  public synchronized void releasePause ()
  {
    if (state != Status.PAUSED) {
      // not paused yet, just clear the request
      pauseRequested = false;
    }
    else {
      // already paused, just proceed
      state = Status.COMPLETE;
      resume();
    }
  }

  // ------------------------- internal methods ------------------
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
  private synchronized void delayMaybe ()
  {
    //System.out.println("delayMaybe() " + new Date().getTime());
    if (state == Status.CLEAR) {
      // sim thread is not finished
      //System.out.println("delaying");
      state = Status.DELAYED; // clock resumed by calling complete()
      competitionControl.pause();
    }
    else if (pauseRequested) {
      // don't schedule the next tick here
      state = Status.PAUSED;
      competitionControl.pause();
      pauseRequested = false;
    }
    else if (state == Status.COMPLETE) {
      // sim finished - schedule the next tick
      scheduleTick();
    }
  }

  // compute new start time, communicate it to brokers, and re-start
  // the clock.
  private void resume ()
  {
    //System.out.println("resume()");
    long originalNextTick = computeNextTickTime();
    long actualNextTick = new Date().getTime() + postPauseDelay;
    start += actualNextTick - originalNextTick;
    timeService.setStart(start);
    competitionControl.resume(start);
    scheduleTick();
  }

  synchronized Status getState () // package visibility for test support
  {
    return state;
  }

  /**
   * Sets state in synchronized block. Needed for cases (such as
   * TickAction.run()) where state needs to be set from outside a
   * synchronized block.
   */
  synchronized void setState (Status newState)
  {
    state = newState;
  }

  private long computeNextTickTime ()
  {
    long current = new Date().getTime();
    // not a valid test?
    if (current < start) {
      // first tick is special
      //System.out.println("first tick at " + start + "; current is " + current);
      return start;
    }
    else {
      // second and subsequent ticks
      long simTime = timeService.getCurrentTime().getMillis();
      long nextSimTime = simTime + modulo;
      long nextTick = start + (nextSimTime - base) / rate; 
      //System.out.println("next tick: current " + current + "; next tick at " + nextTick);
      return nextTick;
    }
  }
  
  private class TickAction extends TimerTask
  {
    SimulationClockControl scc;
    
    TickAction (SimulationClockControl scc)
    {
      super();
      this.scc = scc;
    }
    
    /**
     * Runs a tick - updates the timeService, clears the state,
     * schedules the watchdog, and notifies the simulator. The
     * monitor object is the singleton.
     */
    @Override
    public void run ()
    {
      //System.out.println("TickAction.run() " + new Date().getTime());
      timeService.updateTime();
      scc.setState(Status.CLEAR);
      long wdTime = computeNextTickTime() - watchdogSlack;
      //System.out.println("watchdog set for " + wdTime);
      currentWatchdog = new WatchdogAction(scc);
      theTimer.schedule(currentWatchdog, new Date(wdTime));
      scc.notifyTick();
    }
  }
  
  private class WatchdogAction extends TimerTask
  {
    SimulationClockControl scc;
    
    WatchdogAction (SimulationClockControl scc)
    {
      super();
      this.scc = scc;
    }
    
    /**
     * Checks for sim task completion by calling pauseMaybe on the
     * instance.
     */
    @Override
    public void run ()
    {
      //System.out.println("WatchdogAction.run() " + new Date().getTime());
      scc.delayMaybe();
      currentWatchdog = null;
    }
  }
}
