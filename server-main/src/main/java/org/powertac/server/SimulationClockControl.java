/*
 * Copyright (c) 2011, 2025 by the original author or authors.
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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.TimeService;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.spring.SpringApplicationContext;

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
  static private Logger log = LogManager.getLogger(SimulationClockControl.class);

  // Clock state values
  public enum Status { CLEAR, COMPLETE, DELAYED, PAUSED, STOPPED }
  Status state = Status.CLEAR; // package visibility for testing
  
  private TimeService timeService;
  
  private CompetitionControlService competitionControl;
    
  //@ConfigurableValue(valueType = "Integer",
  //    publish = true,
  //    description = "Minimum agent time per timeslot in msec")
  @ConfigurableValue(valueType = "Double",
          publish = true,
          description = "portion of timeslot allocated to agents")
  private double agentShare = 0.6;
  
  private Integer minAgentWindow = 1000;
  private int minWindow = 10;
  private int minPauseInterval = 100; // min time before pause
  private double maxTickOffsetRatio = 0.2; // max offset as proportion of tickInterval
  private long maxSyncWindow = 3000;

  private long base;
  private long start;
  private long rate;
  private long modulo;
  private long tickInterval;
  private long scheduledTickTime;

  private int nextTick = -1;
  private boolean pauseRequested = false;
  
  private Timer theTimer;
  private WatchdogAction currentWatchdog;
  
  private boolean brokerSync = false;
  
  private Set<Semaphore> waitUntilStopSemaphores;
  
  // ------------- Singleton methods -------------
  private static SimulationClockControl instance;

  /**
   * Creates the instance and sets the reference to the timeService.
   */
  private static CompetitionControlService ccs;
  public static void initialize (CompetitionControlService competitionControl,
                                 TimeService timeService)
  {
    instance = new SimulationClockControl(competitionControl, timeService);
    ServerConfiguration serverConfig =
        (ServerConfiguration) SpringApplicationContext.getBean("serverPropertiesService");
    serverConfig.configureMe(instance);
    ccs = competitionControl;
  }
  
  /**
   * Returns the instance, which of course will be null if the singleton
   * is not yet initialized.
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
    this.tickInterval = this.modulo / this.rate;
    theTimer = new Timer();
    this.waitUntilStopSemaphores = Collections.synchronizedSet(new HashSet<Semaphore>());

  }
  
  // --------------- external api ----------------
  /**
   * Ensure that the minAgentWindow in msec is the correct portion
   * of the total timeslot time in seconds.
   * @param simulationTimeslotSeconds The actual complete timeslot window
   */
  public void adjustAgentWindow (double simulationTimeslotSeconds)
  {
    minAgentWindow = (int) Math.round(Math.floor(simulationTimeslotSeconds * 1000 * agentShare));
    log.info("minAgentWindow = {}", minAgentWindow);
  }

  private int getMinAgentWindow ()
  {
    if (minAgentWindow == 0) {
      log.error("minAgentWindow not yet initialized");
      return 3000;
    }
    else {
      return minAgentWindow;
    }
  }
  
  public void setBrokerSync (boolean sync, long window)
  {
    brokerSync = sync;
    maxSyncWindow = window;
  }

  /**
   * Sets the sim clock start time, which in turn gets propagated to the
   * timeService.
   */
  public void setStart (long start)
  {
    this.start = start;
    timeService.setStart(start);
    if (!competitionControl.isBootstrapMode()) {
      minWindow = getMinAgentWindow();
    }
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
        log.info("tick {} activated", n);
      }
      catch (InterruptedException ie) { }
    }

    // find the delay offset for this tick
    // The absolute value is used to allow this to handle short intervals
    // arising in brokerSync mode
    long offset = new Date().getTime() - scheduledTickTime;
    log.info("Clock offset {}, tick interval {}", offset, tickInterval);
    if (Math.abs(offset) > (long)(tickInterval / maxTickOffsetRatio)) {
      log.warn("clock delay: " + offset + " msec");
      updateStart(offset);
    }

    // update the time, set the watchdog, and schedule the next tick.
    timeService.updateTime();
    setState(Status.CLEAR);
    if (currentWatchdog != null) {
      currentWatchdog.cancel();
    }
    currentWatchdog = new WatchdogAction(this);
    if (!ccs.getBrokerSync()) {
      // watchdog controls pause mechanism
      long earliestPause = new Date().getTime() + minPauseInterval;
      long wdTime = computeNextTickTime() - minWindow;
      if (wdTime < earliestPause)
        wdTime = earliestPause;
      //System.out.println("watchdog set for " + wdTime);
      theTimer.schedule(currentWatchdog, new Date(wdTime));
    }
    else {
      // Otherwise watchdog aborts the sim if a broker has not responded
      theTimer.schedule(currentWatchdog,
                        computeNextTickTime() + maxSyncWindow);
    }
  }
  
  /**
   * Compares sim time to sys time, updates start if it's off too much
   */
  public void checkClockDrift() {
    long offset = timeService.getOffset();
    if (offset > (long)(tickInterval / maxTickOffsetRatio)) {
      log.warn("clock drift " + offset);
      updateStart(offset);
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
  synchronized void notifyTick ()
  {
    nextTick += 1;
    log.info("notify tick {}", nextTick);
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
    long originalNextTick = computeNextTickTime();
    long actualNextTick = new Date().getTime() + minWindow;
    updateStart(actualNextTick - originalNextTick);
    scheduleTick();
  }

  // push the clock forward by offset msec
  private void updateStart (long offset)
  {
    start += offset;
    timeService.setStart(start);
    competitionControl.resume(start);
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
    // not a valid test in sim mode...
    if (current < start) {
      // first tick is special
      //System.out.println("first tick at " + start + "; current is " + current);
      return start;
    }
    else {
      // second and subsequent ticks
      long simTime = timeService.getCurrentTime().toEpochMilli();
      long nextSimTime = simTime + modulo;
      long nextTick = start + (nextSimTime - base) / rate; 
      log.info("next tick: current {} ; next tick at {}", current, nextTick);
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
      scc.scheduledTickTime = scheduledExecutionTime();
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
     * In normal operation, checks for sim task completion by
     * calling pauseMaybe on the instance.
     * In broker-sync mode, shuts down the simulation with
     * an error.
     */
    @Override
    public void run ()
    {
      //System.out.println("WatchdogAction.run() " + new Date().getTime());
      if (!brokerSync) {
        scc.delayMaybe();
        currentWatchdog = null;
      }
      else {
        // If this goes off in brokerSync mode, it's a fatal error
        log.error("Broker failure, shutting down");
        competitionControl.stop();
        scc.stop();
      }
    }
  }
}
