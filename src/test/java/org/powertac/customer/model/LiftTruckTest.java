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
import org.powertac.common.config.Configurator;
import org.powertac.customer.model.LiftTruck.BatteryState;

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
    //assertEquals("10 trucks", 10, truck.getnTrucks());
    assertEquals("1 trucks 2nd shift",
                 5.0, truck.getTrucksInUseWeekday().get(1), 1e-6);
  }

  @Test
  public void basicConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "twoShift,threeShift");
    map.put("customer.model.liftTruck.twoShift.shifts",
            "16, 10, 6, 10");
    map.put("customer.model.liftTruck.twoShift.trucksInUseWeekday",
            "8.0, 6.0");
    map.put("customer.model.liftTruck.twoShift.trucksInUseWeekend",
            "2.0, 1.0");
    map.put("customer.model.liftTruck.threeShift.shifts",
            "6, 8, 14, 8, 22, 8");
    map.put("customer.model.liftTruck.threeShift.trucksInUseWeekday",
            "8.0, 6.0, 4.0");
    map.put("customer.model.liftTruck.threeShift.trucksInUseWeekend",
            "3.0, 2.0, 1.0");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("two instances", 2, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck twoshift = trucks.get("twoShift");
    assertNotNull("found twoShift", twoshift);
    assertEquals("twoshift spec size",
                 2, twoshift.getShifts().size());
    assertEquals("twoshift s1 start",
                 6, twoshift.getShifts().get(0).getStart());
    assertEquals("twoshift s2 duration",
                 10, twoshift.getShifts().get(1).getDuration());
    List<Double> trucksInUse = twoshift.getTrucksInUseWeekday();
    assertEquals("two entries", 2, trucksInUse.size());
    assertEquals("first entry", 8.0, trucksInUse.get(0), 1e-6);
    assertEquals("second entry", 6.0, trucksInUse.get(1), 1e-6);
    trucksInUse = twoshift.getTrucksInUseWeekend();
    assertEquals("two entries", 2, trucksInUse.size());
    assertEquals("first entry", 2.0, trucksInUse.get(0), 1e-6);
    assertEquals("second entry", 1.0, trucksInUse.get(1), 1e-6);

    LiftTruck threeshift = trucks.get("threeShift");
    assertNotNull("found threeshift", threeshift);
    assertEquals("threeshift spec size",
                 3, threeshift.getShifts().size());
    assertEquals("threeshift s1 start",
                 6, threeshift.getShifts().get(0).getStart());
    assertEquals("threeshift s2 duration",
                 8, threeshift.getShifts().get(1).getDuration());
    trucksInUse = threeshift.getTrucksInUseWeekday();
    assertEquals("three entries", 3, trucksInUse.size());
    assertEquals("first entry", 8.0, trucksInUse.get(0), 1e-6);
    assertEquals("third entry", 4.0, trucksInUse.get(2), 1e-6);
    trucksInUse = threeshift.getTrucksInUseWeekend();
    assertEquals("three entries", 3, trucksInUse.size());
    assertEquals("first entry", 3.0, trucksInUse.get(0), 1e-6);
    assertEquals("second entry", 2.0, trucksInUse.get(1), 1e-6);
  }

  @Test
  public void bogusShiftConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "short3Shift,long3Shift");
    map.put("customer.model.liftTruck.short3Shift.shifts",
            "16, 10, 6, 10, 4");
    map.put("customer.model.liftTruck.short3Shift.trucksInUseWeekday",
            "8.0, 6.0");
    map.put("customer.model.liftTruck.short3Shift.trucksInUseWeekend",
            "2.0, 1.0");
    map.put("customer.model.liftTruck.long3Shift.shifts",
            "14, 8, 22, 8, 6, 8, 7");
    map.put("customer.model.liftTruck.long3Shift.trucksInUseWeekday",
            "8.0, 6.0, 4.0");
    map.put("customer.model.liftTruck.long3Shift.trucksInUseWeekend",
            "3.0, 2.0, 1.0");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("two instances", 2, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck short3Shift = trucks.get("short3Shift");
    assertNotNull("found short3Shift", short3Shift);
    assertEquals("short3Shift spec size",
                 2, short3Shift.getShifts().size());
    assertEquals("short3Shift s1 start",
                 6, short3Shift.getShifts().get(0).getStart());
    assertEquals("short3Shift s2 duration",
                 10, short3Shift.getShifts().get(1).getDuration());
    List<Double> trucksInUse = short3Shift.getTrucksInUseWeekday();
    assertEquals("two entries", 2, trucksInUse.size());
    assertEquals("first entry", 8.0, trucksInUse.get(0), 1e-6);
    assertEquals("second entry", 6.0, trucksInUse.get(1), 1e-6);
    trucksInUse = short3Shift.getTrucksInUseWeekend();
    assertEquals("two entries", 2, trucksInUse.size());
    assertEquals("first entry", 2.0, trucksInUse.get(0), 1e-6);
    assertEquals("second entry", 1.0, trucksInUse.get(1), 1e-6);

    LiftTruck long3Shift = trucks.get("long3Shift");
    assertNotNull("found long3Shift", long3Shift);
    assertEquals("long3Shift spec size",
                 3, long3Shift.getShifts().size());
    assertEquals("long3Shift s1 start",
                 6, long3Shift.getShifts().get(0).getStart());
    assertEquals("long3Shift s2 duration",
                 8, long3Shift.getShifts().get(1).getDuration());
    trucksInUse = long3Shift.getTrucksInUseWeekday();
    assertEquals("three entries", 3, trucksInUse.size());
    assertEquals("first entry", 8.0, trucksInUse.get(0), 1e-6);
    assertEquals("third entry", 4.0, trucksInUse.get(2), 1e-6);
    trucksInUse = long3Shift.getTrucksInUseWeekend();
    assertEquals("three entries", 3, trucksInUse.size());
    assertEquals("first entry", 3.0, trucksInUse.get(0), 1e-6);
    assertEquals("second entry", 2.0, trucksInUse.get(1), 1e-6);
  }

  /**
   * Test method for {@link org.powertac.customer.model.LiftTruck#validateShifts()}.
   */
  @Test
  public void testValidateShifts ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "shortShift,longShift");
    map.put("customer.model.liftTruck.shortShift.shifts",
            "16, 8, 0, 8, 8, 8");
    map.put("customer.model.liftTruck.shortShift.trucksInUseWeekday",
            "8.0, 6.0");
    map.put("customer.model.liftTruck.shortShift.trucksInUseWeekend",
            "2.0, 3.0, 4.0");
    map.put("customer.model.liftTruck.longShift.shifts",
            "6, 8, 14, 8, 22, 8");
    map.put("customer.model.liftTruck.longShift.trucksInUseWeekday",
            "8.0, 6.0, 4.0");
    map.put("customer.model.liftTruck.longShift.trucksInUseWeekend",
            "3.0, 2.0, 1.0, 5.0");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("two instances", 2, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck truck = trucks.get("shortShift");
    assertEquals("3 shifts",
                 3, truck.getShifts().size());
    assertEquals("2 weekday usage specs",
                 2, truck.getTrucksInUseWeekday().size());
    assertEquals("3 weekend usage specs",
                 3, truck.getTrucksInUseWeekend().size());
    truck.validateShifts();
    assertEquals("3 weekday usage specs",
                 3, truck.getTrucksInUseWeekday().size());
    assertEquals("3 weekend usage specs",
                 3, truck.getTrucksInUseWeekend().size());

    truck = trucks.get("longShift");
    assertEquals("3 shifts",
                 3, truck.getShifts().size());
    assertEquals("3 weekday usage specs",
                 3, truck.getTrucksInUseWeekday().size());
    assertEquals("4 weekend usage specs",
                 4, truck.getTrucksInUseWeekend().size());
    truck.validateShifts();
    assertEquals("3 weekday usage specs",
                 3, truck.getTrucksInUseWeekday().size());
    assertEquals("3 weekend usage specs",
                 3, truck.getTrucksInUseWeekend().size());
  }

  // battery validation
  @Test
  public void testValidateBatteries ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances",
            "short,long");
    map.put("customer.model.liftTruck.short.shifts",
            "16, 8, 0, 8, 8, 8");
    map.put("customer.model.liftTruck.short.trucksInUseWeekday",
            "4.0, 4.0, 3.0");
    map.put("customer.model.liftTruck.short.trucksInUseWeekend",
            "2.0, 3.0, 4.0");
    map.put("customer.model.liftTruck.short.stateOfCharge",
        "40.0, 35.0, 30.0, 24.0, 31.0, 12.0");
    map.put("customer.model.liftTruck.long.batteryCapacity", "24.0");
    map.put("customer.model.liftTruck.long.shifts",
            "6, 8, 14, 8, 22, 8");
    map.put("customer.model.liftTruck.long.trucksInUseWeekday",
            "5.0, 3.0, 7.0");
    map.put("customer.model.liftTruck.long.trucksInUseWeekend",
            "3.0, 2.0, 1.0");
    map.put("customer.model.liftTruck.long.stateOfCharge",
        "20.0, 15.0, 10.0, 24.0, 11.0, 12.0, 15.0, 16.0, 21.0, 22.0");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    assertEquals("two instances", 2, instances.size());
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck shortTruck = trucks.get("short");
    shortTruck.validateShifts();
    shortTruck.populateShifts();
    assertEquals("short before validation", 6, shortTruck.getBatteryState().length);
    shortTruck.validateBatteries();
    assertEquals("short after validation", 8, shortTruck.getBatteryState().length);

    LiftTruck longTruck = trucks.get("long");
    longTruck.validateShifts();
    longTruck.populateShifts();
    assertEquals("long before validation", 10, longTruck.getBatteryState().length);
    longTruck.validateBatteries();
    assertEquals("long after validation", 16, longTruck.getBatteryState().length);
  }

  // battery validation
  @Test
  public void testValidateChargers ()
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
    tkw.validateShifts();
    tkw.populateShifts();
    assertEquals("8 chargers tkw", 8, tkw.getNChargers());
    tkw.validateChargers();
    assertEquals("9 after tkw validation", 9, tkw.getNChargers());

    LiftTruck ckw = trucks.get("charge_kw");
    ckw.ensureShifts();
    ckw.validateShifts();
    ckw.populateShifts();
    assertEquals("8 chargers", 8, ckw.getNChargers());
    ckw.validateChargers();
    assertEquals("11 after validation", 11, ckw.getNChargers());

    LiftTruck nc = trucks.get("ncharge");
    nc.ensureShifts();
    nc.validateShifts();
    nc.populateShifts();
    assertEquals("8 chargers", 3, nc.getNChargers());
    nc.validateChargers();
    assertEquals("11 after validation", 4, nc.getNChargers());
  }

  // initialize fills in unconfigured fields
  @Test
  public void testInitialize ()
  {
    LiftTruck truck = new LiftTruck("Test");
    // initially, shift and battery state fields are empty
    assertNull("no shifts", truck.getShifts());
    assertNull("no battery state", truck.getDoubleStateOfCharge());
    truck.initialize(null, null, null);
    // now we should see default data
    assertEquals("3 shifts", 3, truck.getShifts().size());
    assertEquals("first shift at midnight",
                 0, truck.getShifts().get(0).getStart());
    assertEquals("second shift duration",
                 8, truck.getShifts().get(1).getDuration());
    assertEquals("third shift start",
                 16, truck.getShifts().get(2).getStart());
    List<Double> soc = truck.getDoubleStateOfCharge();
    assertEquals("16 batteries", 16, soc.size());
    assertEquals("first soc is 50.0", 50.0, soc.get(0), 1e-6);
    assertEquals("fifth soc is 20.0", 20.0, soc.get(4), 1e-6);
  }

  // test sorting of battery state
  @Test
  public void testSortBatteryState ()
  {
    LiftTruck truck = new LiftTruck("Test");
    truck.initialize(null, null, null);
    BatteryState[] initialState = truck.getBatteryState();
    assertEquals("first element",
                 50.0, initialState[0].getStateOfCharge(), 1e-6);
    initialState[13].setInUse(0);
    BatteryState[] sorted = truck.getSortedBatteryState();
    assertEquals("first element 1.0",
                 1.0, sorted[0].getStateOfCharge(), 1e-6);
    assertEquals("2nd element",
                 1.5, sorted[1].getStateOfCharge(), 1e-6);
    assertEquals("3rd element skips inUse battery",
                 4.0, sorted[2].getStateOfCharge(), 1e-6);
    assertFalse("last unused", sorted[14].isInUse());
    assertTrue("last element is inUse", sorted[15].isInUse());
    assertEquals("last element",
                 2.0, sorted[15].getStateOfCharge(), 1e-6);
  }

  // check shift indexing, weekdays & weekends
//  @Test
//  public void testShiftIndex ()
//  {
//    LiftTruck truck = new LiftTruck("Test");
//    truck.initialize(null, null, null);
//    DateTime dt = new DateTime(2000, 3, 21, 0, 0, DateTimeZone.UTC);
//    Instant time = new Instant(dt);
//    assertEquals("0 is start of 1st shift",
//                 new Integer(0), truck.indexStartOfShift(time));
//    assertNull("1 is start of no shift",
//               truck.indexStartOfShift(time.plus(LiftTruck.HOUR)));
//    assertNull("7 is start of no shift",
//               truck.indexStartOfShift(time.plus(LiftTruck.HOUR * 7)));
//    assertEquals("8 is start of 2nd shift",
//                 new Integer(1),
//                 truck.indexStartOfShift(time.plus(LiftTruck.HOUR * 8)));
//  }

  /**
   * Test method for {@link org.powertac.customer.model.LiftTruck#step(org.powertac.common.Timeslot)}.
   */
  @Test
  public void testStep ()
  {
    Instant now = new Instant();
    System.out.println("hour: " + now.get(DateTimeFieldType.hourOfDay()));
    System.out.println("day: " + now.get(DateTimeFieldType.dayOfWeek()));
  }
}
