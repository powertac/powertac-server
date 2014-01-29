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

import static org.junit.Assert.*;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

/**
 * Test cases for TimeService
 * @author John Collins
 */
public class TimeServiceTests
{
  DateTime theBase;
  DateTime theStart;
  int theRate;
  int theMod;
  TimeService ts;
  
  public TimeServiceTests ()
  {
    super();
  }

  @Before
  public void setUp() throws Exception
  {
    theBase = new DateTime(2008, 6, 21, 12, 0, 0, 0, DateTimeZone.UTC);
    theStart = new DateTime(DateTimeZone.UTC);
    theRate = 360;       // 6 min/sec -- 10 sec/hr
    theMod = 15*60*1000; // 15 min (2.5 sec) timeslots
    ts = new TimeService(theBase.getMillis(),
                         theStart.getMillis(),
                         theRate,
                         theMod);
    ts.updateTime();
  }

  @Test
  public void testCreate ()
  {
    assertEquals("correct base", theBase.getMillis(), ts.getBase());
    assertEquals("correct rate", theRate, ts.getRate());
    assertEquals("correct modulo", theMod, ts.getModulo());
  }
  
  // set base, start, rate and test, check initial time
  @Test
  public void testTimeConversion () 
  {
    long offset = ts.getCurrentTime().getMillis() - theBase.getMillis();
    assertEquals("offset zero", 0, offset);
    //assertTrue("$offset close to base time", offset < 60*1000) // less than one minute has elapsed
  }
  
  @Test
  public void testSetClockParamsGood ()
  {
    Map<String, Long> params = new TreeMap<String, Long>();
    long newBase = theBase.plus(TimeService.DAY).getMillis();
    long newMod = TimeService.HOUR;
    params.put("base", newBase);
    params.put("rate", 560l);
    params.put("modulo", newMod);
    params.put("modulo1", 0l);
    ts.setClockParameters(params);
    assertEquals("correct base", newBase, ts.getBase());
    assertEquals("correct rate", 560l, ts.getRate());
    assertEquals("correct modulo", newMod, ts.getModulo());
    //Instant newTime = new Instant(newBase).minus(newMod);
    //assertEquals("correct time", newTime, ts.getCurrentTime());
  }
  
  @Test
  public void testSetClockParamsBogus ()
  {
    Map<String, Long> params = new TreeMap<String, Long>();
    long newBase = theBase.plus(TimeService.DAY).getMillis();
    params.put("base", newBase);
    params.put("1rate", 560l);
    params.put("modulo", TimeService.HOUR);
    ts.setClockParameters(params);
    assertEquals("correct base", newBase, ts.getBase());
    assertEquals("correct rate", theRate, ts.getRate());
    assertEquals("correct modulo", TimeService.HOUR, ts.getModulo());
  }

  // set base, start, rate and test, check time after delay
  @Test
  public void testTimePass() 
  {
    try {
      Thread.sleep(5000); // 5 seconds / 30 min
      ts.updateTime();
      long delay = ts.getCurrentTime().getMillis() - theBase.getMillis();
      assertEquals("delay is 30 min", 30 * TimeService.MINUTE, delay);
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
      long delay = ts.getCurrentTime().getMillis() - theBase.getMillis();
      assertEquals("delay is 15 min", 15 * TimeService.MINUTE, delay);

      Thread.sleep(1001);
      long offset = ts.getOffset();
      assertTrue("offset > 1000 msec", (offset > 1000l));
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
  //  assertEquals("base time", theBase, ts.getCurrentTime());
  //  ts.setStart(ts.getStart() - (TimeService.HOUR / theRate));
  //  assertEquals("one hour", theBase.plus(TimeService.HOUR), ts.getCurrentTime());
  //}
  
  // single action, already due
  @Test
  public void testSingleActionDue()
  {
    final IntHolder var = new IntHolder(0);
    ts.addAction(theBase.toInstant(),
                 new TimedAction() {
      public void perform(Instant time) {
        var.setValue(1); 
        }
    });
    ts.updateTime();
    assertEquals("var got set to 1", 1, var.getValue());
  }
  
  // single action, in the future
  @Test
  public void testSingleActionFuture()
  {
    final IntHolder var = new IntHolder(0);
    ts.addAction(theBase.toInstant().plus(15*60*1000),
                 new TimedAction(){
      public void perform(Instant time) {
        var.setValue(2);
      }
    });
    ts.updateTime(); // not yet
    assertEquals("var unchanged", 0, var.getValue());
    try {
      Thread.sleep(3000); // 3 seconds -> 18 min sim time
      ts.updateTime();
      assertEquals("var changed", 2, var.getValue());
      long offset = ts.getCurrentTime().getMillis() - theBase.getMillis();
      assertEquals("offset is 15 min", 15*60*1000, offset);
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
      public void perform(Instant time) {
        actionCount.setValue(actionCount.getValue() + 1);
        var.setValue(3 * actionCount.getValue());
      }
    };
    final RecursiveTimedAction add = new RecursiveTimedAction(interval);
    TimedAction recurse = new TimedAction(){
      public void perform(Instant time) {
        action.perform(time);
        add.perform(time);
      }
    };
    add.setCore(recurse);
    add.perform(ts.getCurrentTime());
    ts.updateTime(); // not yet
    assertEquals("var unchanged", 0, var.getValue());
    try {
      Thread.sleep(2500); // 2.5 seconds -> 15 min sim time
      ts.updateTime();
      assertEquals("var changed", 3, var.getValue());
      assertEquals("actionCount=1", 1, actionCount.getValue());
      Thread.sleep(1000); // 1 second -> 6 min sim time
      assertEquals("var not changed", 3, var.getValue());
      assertEquals("actionCount=1", 1, actionCount.getValue());
      Thread.sleep(1500); // 1.5 seconds -> 9 min sim time
      ts.updateTime();
      assertEquals("var changed", 6, var.getValue());
      assertEquals("actionCount=2", 2, actionCount.getValue());
      Thread.sleep(2500); // 2.5 seconds -> 15 min sim time
      ts.updateTime();
      assertEquals("var changed", 9, var.getValue());
      assertEquals("actionCount=3", 3, actionCount.getValue());
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
    
    public void perform (Instant time) {
      ts.addAction(ts.getCurrentTime().plus(interval),
                   core);
    }
  }
}
