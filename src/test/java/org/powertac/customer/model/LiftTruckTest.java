/*
 * Copyright (c) 2014 by the original author
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
package org.powertac.customer.model;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.RandomSeed;
import org.powertac.common.Timeslot;
import org.powertac.common.config.Configurator;
import org.powertac.customer.StepInfo;
import org.powertac.customer.model.LiftTruck.Shift;
import org.powertac.customer.model.LiftTruck.ShiftEnergy;

/**
 * @author John Collins
 */
public class LiftTruckTest
{
  Configuration config;

  /**
   *
   */
  @Before
  public void setUp () throws Exception
  {
  }

  // map names to instances
  private Map<String, LiftTruck> mapNames (Collection<?> objects)
  {
    Map<String, LiftTruck> result = new HashMap<String, LiftTruck>();
    for (Object thing : objects) {
      LiftTruck truck = (LiftTruck)thing;
      result.put(truck.getName(), truck);
    }
    return result;
  }

  /**
   * Test method for {@link org.powertac.customer.model.LiftTruck#LiftTruck(java.lang.String)}.
   */
  @Test
  public void testLiftTruck ()
  {
    LiftTruck truck = new LiftTruck("Test");
    assertNotNull("constructed", truck);
    assertEquals("1 trucks 2nd shift",
                 8, truck.getNChargers());
  }

  @Test
  public void basicConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "twoShift,threeShift");
    map.put("customer.model.liftTruck.twoShift.shiftData",
            "block,1,2,3,4,5, shift,8,10,8, shift,18,10,6,"
            + "block,6, shift,8,10,3");
    map.put("customer.model.liftTruck.threeShift.shiftData",
            "block,1,2,3,4,5, shift,6,8,8, shift,14,8,6, shift,22,8,4,"
            + "block,6,7, shift,6,8,3, shift,14,8,2");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("two instances", 2, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck twoshift = trucks.get("twoShift");
    assertNotNull("found twoShift", twoshift);
    twoshift.ensureShifts();
    Shift[] schedule = twoshift.getShiftSchedule();
    assertNotNull("schedule exists", schedule);
    assertEquals("correct size", 168, schedule.length);
    assertNull("idle Mon 0:00", schedule[0]);
    Shift s1 = schedule[8];
    assertNotNull("entry for 8:00 Monday", s1);
    assertEquals("8:00 Monday start", 8, s1.getStart());
    assertEquals("8:00 trucks", 8, s1.getTrucks());
    assertEquals("s1 runs to 17:00", s1, schedule[17]);
    Shift s2 = schedule[18];
    assertNotNull("second shift exists", s2);
    assertEquals("18:00 start", 18, s2.getStart());
    assertEquals("Tue 3:00", s2, schedule[27]);
    assertEquals("Tue 3:00 trucks", 6, s2.getTrucks());
    assertNull("Idle Tue 4:00", schedule[28]);
    assertEquals("Tue 8:00 is s1", s1, schedule[32]);
    assertEquals("Tue 18:00 is s2", s2, schedule[42]);
    assertEquals("Fri 18:00 is s2", s2, schedule[18 + LiftTruck.HOURS_DAY * 4]);
    assertEquals("Sat 3:00 is s2", s2, schedule[3 + LiftTruck.HOURS_DAY * 5]);
    assertNull("Sat 4:00 is null", schedule[4 + LiftTruck.HOURS_DAY * 5]);
    Shift s3 = schedule[8 + LiftTruck.HOURS_DAY * 5];
    assertNotNull("Sat day shift", s3);
    assertEquals("Sat trucks", 3, s3.getTrucks());
    assertNull("idle Sun", schedule[8 + LiftTruck.HOURS_DAY * 6]);

    LiftTruck threeshift = trucks.get("threeShift");
    assertNotNull("found threeshift", threeshift);
    threeshift.ensureShifts();
    schedule = threeshift.getShiftSchedule();
    assertNotNull("exists", schedule);
    assertEquals("size", 168, schedule.length);
    assertNull("idle Mon midnight", schedule[0]);
    assertNull("idle 5:00 Mon", schedule[5]);
    s1 = schedule[6];
    assertNotNull("not idle 6:00 Mon", s1);
    assertEquals("s1 start", 6, s1.getStart());
    assertEquals("s1 dur", 8, s1.getDuration());
    assertEquals("s1 trucks", 8, s1.getTrucks());
    assertEquals("s1 Mon 13:00", s1, schedule[13]);
    s2 = schedule[14];
    assertNotNull("not idle Mon 14:00", s2);
    assertNotSame("different from s1", s1, s2);
    assertEquals("s2 start", 14, s2.getStart());
    assertEquals("s2 dur", 8, s2.getDuration());
    assertEquals("s2 trucks", 6, s2.getTrucks());
    assertEquals("s2 Mon 21:00", s2, schedule[21]);
    s3 = schedule[22];
    assertNotNull("not idle Mon 22:00", s3);
    assertNotSame("different from s1", s1, s3);
    assertNotSame("different from s2", s2, s3);
    assertEquals("s3 start", 22, s3.getStart());
    assertEquals("s3 dur", 8, s3.getDuration());
    assertEquals("s3 trucks", 4, s3.getTrucks());
    assertEquals("s3 Tue 5:00", s3, schedule[29]);
    // check Friday - Sat AM
    assertEquals("Fri 0:00", s3, schedule[LiftTruck.HOURS_DAY * 4]);
    assertEquals("Fri 5:00", s3, schedule[5 + LiftTruck.HOURS_DAY * 4]);
    assertEquals("Fri 6:00", s1, schedule[6 + LiftTruck.HOURS_DAY * 4]);
    assertEquals("Fri 13:00", s1, schedule[13 + LiftTruck.HOURS_DAY * 4]);
    assertEquals("Fri 14:00", s2, schedule[14 + LiftTruck.HOURS_DAY * 4]);
    assertEquals("Fri 21:00", s2, schedule[21 + LiftTruck.HOURS_DAY * 4]);
    assertEquals("Fri 22:00", s3, schedule[22+ LiftTruck.HOURS_DAY * 4]);
    assertEquals("Sat 5:00", s3, schedule[5 + LiftTruck.HOURS_DAY * 5]);
    Shift s4 = schedule[6 + LiftTruck.HOURS_DAY * 5];
    assertNotNull("not idle Sat 6:00", s4);
    assertNotSame("different from s1", s1, s4);
    assertNotSame("different from s2", s2, s4);
    assertNotSame("different from s3", s3, s4);
    assertEquals("s4 start", 6, s4.getStart());
    assertEquals("s4 dur", 8, s4.getDuration());
    assertEquals("s4 trucks", 3, s4.getTrucks());
    assertEquals("Sat 6:00", s4, schedule[6 + LiftTruck.HOURS_DAY * 5]);
    assertEquals("Sat 13:00", s4, schedule[13 + LiftTruck.HOURS_DAY * 5]);
    Shift s5 = schedule[14 + LiftTruck.HOURS_DAY * 5];
    assertNotNull("not idle Sat 14:00", s5);
    assertNotSame("different from s1", s1, s5);
    assertNotSame("different from s2", s2, s5);
    assertNotSame("different from s3", s3, s5);
    assertNotSame("different from s4", s4, s5);
    assertEquals("s5 start", 14, s5.getStart());
    assertEquals("s5 dur", 8, s5.getDuration());
    assertEquals("s5 trucks", 2, s5.getTrucks());
    assertEquals("Sat 14:00", s5, schedule[14 + LiftTruck.HOURS_DAY * 5]);
    assertEquals("Sat 21:00", s5, schedule[21 + LiftTruck.HOURS_DAY * 5]);
    assertNull("idle Sat 22:00", schedule[22 + LiftTruck.HOURS_DAY * 5]);
    assertNull("idle Sun 0:00", schedule[LiftTruck.HOURS_DAY * 6]);
    assertNull("idle Sun 5:00", schedule[5 + LiftTruck.HOURS_DAY * 6]);
    assertEquals("Sun 6:00", s4, schedule[6 + LiftTruck.HOURS_DAY * 6]);
    assertEquals("Sun 13:00", s4, schedule[13 + LiftTruck.HOURS_DAY * 6]);
    assertEquals("Sun 14:00", s5, schedule[14 + LiftTruck.HOURS_DAY * 6]);
    assertEquals("Sun 21:00", s5, schedule[21 + LiftTruck.HOURS_DAY * 6]);
    assertNull("idle Sun 22:00", schedule[22 + LiftTruck.HOURS_DAY * 6]);
    
  }

  @Test
  public void BogusShiftConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "test1, test2, test3");
    // no block
    map.put("customer.model.liftTruck.test1.shiftData", "shift,8,10,8");
    // no shift
    map.put("customer.model.liftTruck.test3.shiftData", "block,1,2,3,4,5");
    // missing shift info
    map.put("customer.model.liftTruck.test3.shiftData",
            "block,1,2,3,4,5, shift,6,8");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("three instances", 3, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck test1 = trucks.get("test1");
    assertNotNull("found test1", test1);
    Shift[] schedule = test1.getShiftSchedule();
    for (Shift shift: schedule) {
      if (null != shift)
        fail("test1 non-null entry");
    }

    LiftTruck test2 = trucks.get("test2");
    assertNotNull("found test2", test2);
    schedule = test2.getShiftSchedule();
    for (Shift shift: schedule) {
      if (null != shift)
        fail("test1 non-null entry");
    }

    LiftTruck test3 = trucks.get("test3");
    assertNotNull("found test3", test3);
    schedule = test3.getShiftSchedule();
    for (Shift shift: schedule) {
      if (null != shift)
        fail("test1 non-null entry");
    }
  }

  @Test
  public void rolloverShiftConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "test");
    map.put("customer.model.liftTruck.test.shiftData",
            "block,1,2,3,4,5, shift,8,10,8, shift,18,10,6,"
            + "block,6, shift,8,10,3, block,7, shift,18,10,2");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("one instance", 1, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck test = trucks.get("test");
    assertNotNull("found uut", test);
    test.ensureShifts();
    Shift[] schedule = test.getShiftSchedule();
    assertNotNull("schedule exists", schedule);
    assertEquals("correct size", 168, schedule.length);
    Shift s4 = schedule[0];
    assertEquals("Mon 0:00 start", 18, s4.getStart());
    assertEquals("Mon 0:00 dur", 10, s4.getDuration());
    assertEquals("Mon 0:00 trucks", 2, s4.getTrucks());
    assertEquals("Mon 3:00", s4, schedule[3]);
    assertNull("idle Mon 4:00", schedule[4]);
  }

  // battery validation
  @Test
  public void testValidateBatteries ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "short,long");
    map.put("customer.model.liftTruck.short.shiftData",
            "block,1,2,3,4,5, shift,6,8,8, shift,14,8,6, shift,22,8,4,"
            + "block,6,7, shift,6,8,3, shift,14,8,2");
    map.put("customer.model.liftTruck.short.nBatteries", "6");
    map.put("customer.model.liftTruck.long.batteryCapacity", "24.0");
    map.put("customer.model.liftTruck.long.shiftData",
            "block,1,2,3,4,5, shift,6,8,5, shift,14,8,3, shift,22,8,7,"
            + "block,6,7, shift,6,8,3, shift,14,8,2");
    map.put("customer.model.liftTruck.long.nBatteries", "10");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("two instances", 2, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck shortTruck = trucks.get("short");
    assertEquals("short before validation", 6, shortTruck.getnBatteries());
    shortTruck.validateBatteries();
    assertEquals("short after validation", 14, shortTruck.getnBatteries());

    LiftTruck longTruck = trucks.get("long");
    assertEquals("long before validation", 10, longTruck.getnBatteries());
    longTruck.validateBatteries();
    assertEquals("long after validation", 16, longTruck.getnBatteries());
  }

  // charger validation
  @Test
  public void testValidateChargers1 ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "truck_kw, charge_kw, ncharge");
    map.put("customer.model.liftTruck.truck_kw.truckKW", "10.0");
    map.put("customer.model.liftTruck.charge_kw.maxChargeKW", "2.0");
    map.put("customer.model.liftTruck.ncharge.nChargers", "3");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("three instances", 3, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck tkw = trucks.get("truck_kw");
    tkw.ensureShifts();
    assertEquals("8 chargers tkw", 8, tkw.getNChargers());
    tkw.validateChargers();
    assertEquals("10 after tkw validation", 10, tkw.getNChargers());

    LiftTruck ckw = trucks.get("charge_kw");
    ckw.ensureShifts();
    assertEquals("8 chargers", 8, ckw.getNChargers());
    ckw.validateChargers();
    assertEquals("12 after validation", 12, ckw.getNChargers());

    LiftTruck nc = trucks.get("ncharge");
    nc.ensureShifts();
    assertEquals("3 chargers", 3, nc.getNChargers());
    nc.validateChargers();
    assertEquals("4 after validation", 4, nc.getNChargers());
  }

  // charger validation
  @Test
  public void testValidateChargers2 ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances", "c5");
    map.put("customer.model.liftTruck.c5.nChargers", "5");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck c5 = trucks.get("c5");
    c5.ensureShifts();
    assertEquals("5 chargers c5", 5, c5.getNChargers());
    c5.validateChargers();
    assertEquals("5 after c5 validation", 5, c5.getNChargers());
  }

  // initialize fills in unconfigured fields
  @Test
  public void testInitialize ()
  {
    LiftTruck truck = new LiftTruck("Test");
    // initially, shift schedule is empty
    for (Shift s : truck.getShiftSchedule()) {
      if (null != s)
        fail("shift schedule should be empty");
    }
    truck.initialize(null, null,  new RandomSeed("test", 0, "1"));
    // now we should see default data
    Shift[] shifts = truck.getShiftSchedule();
    for (Shift s : truck.getShiftSchedule()) {
      if (null != s)
        return;
    }
    fail("shift schedule should be non-empty");
  }

  @Test
  public void testFutureEnergyNeedsDefault ()
  {
    LiftTruck truck = new LiftTruck("Test");
    truck.initialize(null, null, new RandomSeed("test", 0, "1"));
    DateTime now =
        new DateTime(2014, 12, 1, 10, 0, 0, DateTimeZone.UTC);
    Timeslot ts = new Timeslot(2, now.toInstant());
    StepInfo info = new StepInfo(ts, null);
    ShiftEnergy[] needs = truck.ensureFutureEnergyNeeds(info);
    // ix  dur  end  req  chg  max  sur
    //  0    6   16  192    7  252   60
    //  1    8    0   96    8  384  288
    //  2    8    8  256    8  384  128
    //  3    8   16  192    7  336  144
    //  4    8    0   96    8  384  288
    //  5    8    8  256    8  384  128
    //  6    8   16  192    7  336  144
    //  7    0    0   96    8  384  288
    assertNotNull("needs not null", needs);
    assertEquals("8 items", 8, needs.length);
    assertEquals("duration[0] is 6", 6, needs[0].getDuration());
    assertEquals("[0] ends at 16", 16, needs[0].getNextShift().getStart());
    assertEquals("[0] requires", 192.0,
                 needs[0].getEnergyNeeded(), 1e-6);
    assertEquals("[0] max surplus", 60.0,
                 needs[0].getMaxSurplus(), 1e-6);
    assertEquals("[1] ends at 0", 0, needs[1].getNextShift().getStart());
    assertEquals("[1] requires", 96.0,
                 needs[1].getEnergyNeeded(), 1e-6);
    assertEquals("[1] surplus", 288.0,
                 needs[1].getMaxSurplus(), 1e-6);
    assertEquals("[2] ends at 8", 8, needs[2].getNextShift().getStart());
    assertEquals("[2] requires", 256.0,
                 needs[2].getEnergyNeeded(), 1e-6);
    assertEquals("[2] surplus", 128.0,
                 needs[2].getMaxSurplus(), 1e-6);
    assertEquals("[7] ends at 0", 0, needs[7].getNextShift().getStart());
    assertEquals("[7] requires", 96.0,
                 needs[7].getEnergyNeeded(), 1e-6);
    assertEquals("[7] surplus", 288.0,
                 needs[7].getMaxSurplus(), 1e-6);
  }

  @Test
  public void testFutureEnergyNeedsShort ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances", "short");
    map.put("customer.model.liftTruck.short.nChargers", "5");
    map.put("customer.model.liftTruck.short.energyCharging", "14.0");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("one instance", 1, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck truck = trucks.get("short");
    assertNotNull("found short", truck);

    truck.initialize(null, null, new RandomSeed("test", 0, "1"));
    DateTime now =
        new DateTime(2014, 12, 1, 10, 0, 0, DateTimeZone.UTC);
    Timeslot ts = new Timeslot(2, now.toInstant());
    StepInfo info = new StepInfo(ts, null);
    ShiftEnergy[] needs = truck.ensureFutureEnergyNeeds(info);
    // ix  dur  end  req  chg  max  sur
    //  0    6   16  192    5  180  -12+14
    //  1    8    0   96    5  240  128
    //  2    8    8  256    5  240  -16
    //  3    8   16  192    5  240   48
    //  4    8    0   96    5  240  128
    //  5    8    8  256    5  240  -16
    //  6    8   16  192    5  240   48
    //  7    0    0   96    5  240  144
    assertNotNull("needs not null", needs);
    assertEquals("8 items", 8, needs.length);
    assertEquals("duration[0] is 6", 6, needs[0].getDuration());
    assertEquals("[0] requires", 192.0,
                 needs[0].getEnergyNeeded(), 1e-6);
    assertEquals("[0] max surplus", 2.0,
                 needs[0].getMaxSurplus(), 1e-6);
    assertEquals("[1] requires", 96.0,
                 needs[1].getEnergyNeeded(), 1e-6);
    assertEquals("[1] surplus", 128.0,
                 needs[1].getMaxSurplus(), 1e-6);
    assertEquals("[2] requires", 256.0,
                 needs[2].getEnergyNeeded(), 1e-6);
    assertEquals("[2] surplus", 0.0,
                 needs[2].getMaxSurplus(), 1e-6);
    assertEquals("[7] requires", 96.0,
                 needs[7].getEnergyNeeded(), 1e-6);
    assertEquals("[7] surplus", 144.0,
                 needs[7].getMaxSurplus(), 1e-6);
  }

  @Test
  public void testFutureEnergyNeedsIdle ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "idle");
    map.put("customer.model.liftTruck.idle.shiftData",
            "block,1,2,3,4,5, shift,8,8,8, shift,0,8,6,"
            + "block,6,7, shift,8,8,4");
    map.put("customer.model.liftTruck.idle.nChargers", "5");
    map.put("customer.model.liftTruck.idle.energyCharging", "4.0");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("one instance", 1, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck tk = trucks.get("idle");
    assertNotNull("got configured", tk);
    tk.initialize(null, null, new RandomSeed("test", 0, "2"));
    DateTime now =
        new DateTime(2014, 12, 1, 10, 0, 0, DateTimeZone.UTC);
    Timeslot ts = new Timeslot(2, now.toInstant());
    StepInfo info = new StepInfo(ts, null);
    ShiftEnergy[] needs = tk.ensureFutureEnergyNeeds(info);
    // ix  dur  end  req  chg  max  sur
    //  0    6   16    0    5  180  180+4
    //  1    8    0  192    5  240   32
    //  2    8    8  256    5  240  -16
    //  3    8   16    0    5  240  240
    //  4    8    0  192    5  240   32
    //  5    8    8  256    5  240  -16
    //  6    8   16    0    5  240  240
    //  7    0    0  192    5  240   48
    assertNotNull("needs not null", needs);
    assertEquals("8 items", 8, needs.length);
    assertEquals("duration[0] is 6", 6, needs[0].getDuration());
    assertNull("[0] ends at idle", needs[0].getNextShift());
    assertEquals("[0] req", 0.0, needs[0].getEnergyNeeded(), 1e-6);
    assertEquals("[0] sur", 184.0, needs[0].getMaxSurplus(), 1e-6);
    assertNotNull("[1] not null", needs[1].getNextShift());
    assertEquals("[1] ends 00:00", 0, needs[1].getNextShift().getStart());
    assertEquals("[1] req", 192.0, needs[1].getEnergyNeeded(), 1e-6);
    assertEquals("[1] sur", 32.0, needs[1].getMaxSurplus(), 1e-6);
    assertEquals("[2] req", 256.0, needs[2].getEnergyNeeded(), 1e-6);
    assertEquals("[2] sur", 0.0, needs[2].getMaxSurplus(), 1e-6);
    assertEquals("[7] req", 192.0, needs[7].getEnergyNeeded(), 1e-6);
    assertEquals("[7] sur", 48.0, needs[7].getMaxSurplus(), 1e-6);
  }

  @Test
  public void testFutureEnergyNeedsWeekend ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "idle");
    map.put("customer.model.liftTruck.idle.shiftData",
            "block,1,2,3,4,5, shift,8,8,8, shift,0,8,6,"
            + "block,6,7, shift,8,8,4");
    map.put("customer.model.liftTruck.idle.nChargers", "5");
    map.put("customer.model.liftTruck.idle.energyCharging", "75.0");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("one instance", 1, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck tk = trucks.get("idle");
    assertNotNull("got configured", tk);
    tk.initialize(null, null, new RandomSeed("test", 0, "2"));
    // start on Sunday
    DateTime now =
        new DateTime(2014, 12, 7, 6, 0, 0, DateTimeZone.UTC);
    Timeslot ts = new Timeslot(2, now.toInstant());
    StepInfo info = new StepInfo(ts, null);
    ShiftEnergy[] needs = tk.ensureFutureEnergyNeeds(info);
    // ix  dur  end  req  chg  max  sur
    //  0    2    8  128    5   60  -68+75 - Sun 06:00-08:00
    //  1    8   16    0    5  240  240   -- Sun 08:00-16:00
    //  2    8    0  192    5  240   32   -- Sun 16:00-00:00
    //  3    8    8  256    5  240  -16   -- Mon 00:00-08:00
    //  4    8   16    0    5  240  240
    //  5    8    0  192    5  240   32
    //  6    8    8  256    5  240  -16
    //  7    8   16    0    5  240  240
    //  8    8    0  192    5  240   48
    assertNotNull("needs not null", needs);
    assertEquals("8 items", 8, needs.length);
    assertEquals("duration[0] is 2", 2, needs[0].getDuration());
    assertEquals("[0] req", 128.0, needs[0].getEnergyNeeded(), 1e-6);
    assertEquals("[0] sur", 7.0, needs[0].getMaxSurplus(), 1e-6);
    assertNull("[1] ends at idle", needs[1].getNextShift());
    assertEquals("[1] req", 0.0, needs[1].getEnergyNeeded(), 1e-6);
    assertEquals("[1] sur", 240.0, needs[1].getMaxSurplus(), 1e-6);
    assertNotNull("[2] not null", needs[2].getNextShift());
    assertEquals("[2] ends 00:00", 0, needs[2].getNextShift().getStart());
    assertEquals("[2] req", 192.0, needs[2].getEnergyNeeded(), 1e-6);
    assertEquals("[2] sur", 32.0, needs[2].getMaxSurplus(), 1e-6);
    assertEquals("[3] req", 256.0, needs[3].getEnergyNeeded(), 1e-6);
    assertEquals("[3] sur", 0.0, needs[3].getMaxSurplus(), 1e-6);
    assertEquals("[7] req", 0.0, needs[7].getEnergyNeeded(), 1e-6);
    assertEquals("[7] sur", 240.0, needs[7].getMaxSurplus(), 1e-6);
  }

  /**
   * Test method for {@link org.powertac.customer.model.LiftTruck#step(org.powertac.common.Timeslot)}.
   */
  @Test
  public void testStep ()
  {
//    Instant now = new Instant();
//    System.out.println("hour: " + now.get(DateTimeFieldType.hourOfDay()));
//    System.out.println("day: " + now.get(DateTimeFieldType.dayOfWeek()));
  }
}
