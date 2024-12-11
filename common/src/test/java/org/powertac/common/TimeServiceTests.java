/*
 * Copyright 2011-2013 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;

/**
 * Test cases for TimeService
 * @author John Collins
 */
public class TimeServiceTests
{
  ZonedDateTime theBase;
  ZonedDateTime theStart;
  int theRate;
  int theMod;
  TimeService ts;
  
  public TimeServiceTests ()
  {
    super();
  }

  @BeforeEach
  public void setUp() throws Exception
  {
    theBase = ZonedDateTime.of(2008, 6, 21, 12, 0, 0, 0, ZoneOffset.UTC);
    theStart = ZonedDateTime.now(ZoneOffset.UTC);
    theRate = 360;       // 6 min/sec -- 10 sec/hr
    theMod = 15*60*1000; // 15 min (2.5 sec) timeslots
    ts = new TimeService(theBase.toInstant().toEpochMilli(),
                         theStart.toInstant().toEpochMilli(),
                         theRate,
                         theMod);
    ts.updateTime();
  }

  @Test
  public void testCreate ()
  {
    assertEquals(theBase.toInstant().toEpochMilli(), ts.getBase(), "correct base");
    assertEquals(theRate, ts.getRate(), "correct rate");
    assertEquals(theMod, ts.getModulo(), "correct modulo");
  }
  
  // set base, start, rate and test, check initial time
  @Test
  public void testTimeConversion () 
  {
    long offset = ts.getCurrentTime().toEpochMilli() - theBase.toInstant().toEpochMilli();
    assertEquals(0, offset, "offset zero");
    //assertTrue("$offset close to base time", offset < 60*1000) // less than one minute has elapsed
  }
  
  @Test
  public void testSetClockParamsGood ()
  {
    Map<String, Long> params = new TreeMap<String, Long>();
    long newBase = theBase.toInstant().plusMillis(TimeService.DAY).toEpochMilli();
    long newMod = TimeService.HOUR;
    params.put("base", newBase);
    params.put("rate", 560l);
    params.put("modulo", newMod);
    params.put("modulo1", 0l);
    ts.setClockParameters(params);
    assertEquals(newBase, ts.getBase(), "correct base");
    assertEquals(560l, ts.getRate(), "correct rate");
    assertEquals(newMod, ts.getModulo(), "correct modulo");
    //Instant newTime = new Instant(newBase).minus(newMod);
    //assertEquals(newTime, ts.getCurrentTime(), "correct time");
  }
  
  @Test
  public void testSetClockParamsBogus ()
  {
    Map<String, Long> params = new TreeMap<String, Long>();
    long newBase = theBase.toInstant().plusMillis(TimeService.DAY).toEpochMilli();
    params.put("base", newBase);
    params.put("1rate", 560l);
    params.put("modulo", TimeService.HOUR);
    ts.setClockParameters(params);
    assertEquals(newBase, ts.getBase(), "correct base");
    assertEquals(theRate, ts.getRate(), "correct rate");
    assertEquals(TimeService.HOUR, ts.getModulo(), "correct modulo");
  }

  // set base, start, rate and test, check time after delay
  @Test
  public void testTimePass() 
  {
    try {
      Thread.sleep(5000); // 5 seconds / 30 min
      ts.updateTime();
      long delay = ts.getCurrentTime().toEpochMilli() - theBase.toInstant().toEpochMilli();
      assertEquals(30 * TimeService.MINUTE, delay, "delay is 30 min");
    }
    catch (InterruptedException ie) {
      fail("unexpected exception " + ie.toString());
    }
  }

  // Test offset detection
  @Test
  public void testOffset ()
  {
    try {
      Thread.sleep(2500); // 2.5 sec, 15 min
      ts.updateTime();
      long delay = ts.getCurrentTime().toEpochMilli() - theBase.toInstant().toEpochMilli();
      assertEquals(15 * TimeService.MINUTE, delay, "delay is 15 min");

      Thread.sleep(1001);
      long offset = ts.getOffset();
      assertTrue((offset > 1000l), "offset > 1000 msec");
      if (offset > 2000l)
        System.out.println("Slow response - offset should be 1000, was " + offset);
    }
    catch (InterruptedException ie) {
      fail("unexpected exception " + ie.toString());
    }
  }

  //@Test
  //public void testSetStart()
  //{
  //  assertEquals(theBase, ts.getCurrentTime(), "base time");
  //  ts.setStart(ts.getStart() - (TimeService.HOUR / theRate));
  //  assertEquals(theBase.plusMillis(TimeService.HOUR), ts.getCurrentTime(), "one hour");
  //}
  
  // single action, already due
  @Test
  public void testSingleActionDue()
  {
    final IntHolder var = new IntHolder(0);
    ts.addAction(theBase.toInstant(),
                 new TimedAction() {
      @Override
      public void perform(Instant time) {
        var.setValue(1); 
        }
    });
    ts.updateTime();
    assertEquals(1, var.getValue(), "var got set to 1");
  }
  
  // single action, in the future
  @Test
  public void testSingleActionFuture()
  {
    final IntHolder var = new IntHolder(0);
    ts.addAction(theBase.toInstant().plusMillis(15*60*1000),
                 new TimedAction(){
      @Override
      public void perform(Instant time) {
        var.setValue(2);
      }
    });
    ts.updateTime(); // not yet
    assertEquals(0, var.getValue(), "var unchanged");
    try {
      Thread.sleep(3000); // 3 seconds -> 18 min sim time
      ts.updateTime();
      assertEquals(2, var.getValue(), "var changed");
      long offset = ts.getCurrentTime().toEpochMilli() - theBase.toInstant().toEpochMilli();
      assertEquals(15*60*1000, offset, "offset is 15 min");
    }
    catch (InterruptedException ie) {
      fail("unexpected " + ie.toString());
    }
  }

  // simple repeated action
  @Test
  public void testRepeatedActionFuture()
  {
    final IntHolder var = new IntHolder(0);
    final IntHolder actionCount = new IntHolder(0);
    final int interval = 15 * 60 * 1000; // one 15-minute tick
    final TimedAction action = new TimedAction() {
      @Override
      public void perform(Instant time) {
        actionCount.setValue(actionCount.getValue() + 1);
        var.setValue(3 * actionCount.getValue());
      }
    };
    final RecursiveTimedAction add = new RecursiveTimedAction(interval);
    TimedAction recurse = new TimedAction(){
      @Override
      public void perform(Instant time) {
        action.perform(time);
        add.perform(time);
      }
    };
    add.setCore(recurse);
    add.perform(ts.getCurrentTime());
    ts.updateTime(); // not yet
    assertEquals(0, var.getValue(), "var unchanged");
    try {
      Thread.sleep(2500); // 2.5 seconds -> 15 min sim time
      ts.updateTime();
      assertEquals(3, var.getValue(), "var changed");
      assertEquals(1, actionCount.getValue(), "actionCount=1");
      Thread.sleep(1000); // 1 second -> 6 min sim time
      assertEquals(3, var.getValue(), "var not changed");
      assertEquals(1, actionCount.getValue(), "actionCount=1");
      Thread.sleep(1500); // 1.5 seconds -> 9 min sim time
      ts.updateTime();
      assertEquals(6, var.getValue(), "var changed");
      assertEquals(2, actionCount.getValue(), "actionCount=2");
      Thread.sleep(2500); // 2.5 seconds -> 15 min sim time
      ts.updateTime();
      assertEquals(9, var.getValue(), "var changed");
      assertEquals(3, actionCount.getValue(), "actionCount=3");
    }
    catch (InterruptedException ie) {
      fail ("unexpected " + ie.toString());
    }
  }

  // simple repeated action
  @Test
  public void testRepeatingAction()
  {
    final IntHolder var = new IntHolder(0);
    final IntHolder actionCount = new IntHolder(0);
    final long interval = 15 * 60 * 1000; // one 15-minute tick
    final TimedAction action = new TimedAction() {
      @Override
      public void perform(Instant time) {
        actionCount.setValue(actionCount.getValue() + 1);
        var.setValue(3 * actionCount.getValue());
      }
    };
    RepeatingTimedAction rta =
            new RepeatingTimedAction(action, interval);
    ts.addAction(ts.getCurrentTime().plusMillis(interval), rta);
    ts.updateTime(); // not yet
    assertEquals(0, var.getValue(), "no action yet");
    try {
      Thread.sleep(2500); // 2.5 seconds -> 15 min sim time
      ts.updateTime();
      assertEquals(3, var.getValue(), "var changed");
      assertEquals(1, actionCount.getValue(), "actionCount=1");
      Thread.sleep(1000); // 1 second -> 6 min sim time
      assertEquals(3, var.getValue(), "var not changed");
      assertEquals(1, actionCount.getValue(), "actionCount=1");
      Thread.sleep(1500); // 1.5 seconds -> 9 min sim time
      ts.updateTime();
      assertEquals(6, var.getValue(), "var changed");
      assertEquals(2, actionCount.getValue(), "actionCount=2");
      Thread.sleep(2500); // 2.5 seconds -> 15 min sim time
      ts.updateTime();
      assertEquals(9, var.getValue(), "var changed");
      assertEquals(3, actionCount.getValue(), "actionCount=3");
    }
    catch (InterruptedException ie) {
      fail ("unexpected " + ie.toString());
    }
  }
  
  class IntHolder
  {
    private int value = 0;

    IntHolder(int val)
    {
      value = val;
    }
    
    int getValue ()
    {
      return value;
    }
    
    void setValue (int val)
    {
      value = val;
    }
  }
  
  // need to break out the recursive action to get around Java
  // rules about initialized final variables.
  class RecursiveTimedAction implements TimedAction
  {
    int interval = 0;
    TimedAction core = null;
    
    RecursiveTimedAction (int interval)
    {
      this.interval = interval;
    }
    
    void setCore (TimedAction act)
    {
      core = act;
    }
    
    @Override
    public void perform (Instant time) {
      ts.addAction(ts.getCurrentTime().plusMillis(interval),
                   core);
    }
  }
}
