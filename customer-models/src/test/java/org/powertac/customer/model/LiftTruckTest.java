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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.customer.model.LiftTruck.Shift;
import org.powertac.customer.model.LiftTruck.ShiftEnergy;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 */
public class LiftTruckTest
{
  private Competition competition;
  private TimeService timeService;
  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo mockSubscriptionRepo;
  private Broker broker;
  private TariffSpecification spec;
  private Tariff tariff;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;
  private ServerConfiguration serverConfig;
  private Configurator configurator;
  private MapConfiguration config;
  private TimeslotRepo tsRepo;
  private ServiceAccessor serviceAccessor;

  @BeforeEach
  public void setUp () throws Exception
  {
    tsRepo = mock(TimeslotRepo.class);
    competition =
        Competition.newInstance("ColdStorage test").withTimeslotsOpen(4);
    Competition.setCurrent(competition);
    timeService = new TimeService();
    Instant now =
        ZonedDateTime.of(2011, 1, 10, 0, 0, 0, 0, TimeService.UTC).toInstant();
    timeService.setCurrentTime(now);

    // tariff setup
    tariffRepo = new TariffRepo();
    mockSubscriptionRepo = mock(TariffSubscriptionRepo.class);
    broker = new Broker("Sam");
    spec =
        new TariffSpecification(broker, PowerType.THERMAL_STORAGE_CONSUMPTION)
    .addRate(new Rate().withValue(-0.11));
    tariff = new Tariff(spec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    // set up randomSeed mock
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(anyString(),
                                    anyLong(),
                                    anyString())).thenReturn(seed);

    // Set up serverProperties mock
    serverConfig = mock(ServerConfiguration.class);
    configurator = new Configurator();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        configurator.configureSingleton(args[0]);
        return null;
      }
    }).when(serverConfig).configureMe(any());

    serviceAccessor = new ServiceAccessor();
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
    assertNotNull(truck, "constructed");
    assertEquals(8, truck.getNChargers(), "1 trucks 2nd shift");
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
    assertEquals(2, instances.size(), "two instances");
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck twoshift = trucks.get("twoShift");
    assertNotNull(twoshift, "found twoShift");
    twoshift.ensureShifts();
    Shift[] schedule = twoshift.getShiftSchedule();
    assertNotNull(schedule, "schedule exists");
    assertEquals(168, schedule.length, "correct size");
    assertNull(schedule[0], "idle Mon 0:00");
    Shift s1 = schedule[8];
    assertNotNull(s1, "entry for 8:00 Monday");
    assertEquals(8, s1.getStart(), "8:00 Monday start");
    assertEquals(8, s1.getTrucks(), "8:00 trucks");
    assertEquals(s1, schedule[17], "s1 runs to 17:00");
    Shift s2 = schedule[18];
    assertNotNull(s2, "second shift exists");
    assertEquals(18, s2.getStart(), "18:00 start");
    assertEquals(s2, schedule[27], "Tue 3:00");
    assertEquals(6, s2.getTrucks(), "Tue 3:00 trucks");
    assertNull(schedule[28], "Idle Tue 4:00");
    assertEquals(s1, schedule[32], "Tue 8:00 is s1");
    assertEquals(s2, schedule[42], "Tue 18:00 is s2");
    assertEquals(s2, schedule[18 + LiftTruck.HOURS_DAY * 4], "Fri 18:00 is s2");
    assertEquals(s2, schedule[3 + LiftTruck.HOURS_DAY * 5], "Sat 3:00 is s2");
    assertNull(schedule[4 + LiftTruck.HOURS_DAY * 5], "Sat 4:00 is null");
    Shift s3 = schedule[8 + LiftTruck.HOURS_DAY * 5];
    assertNotNull(s3, "Sat day shift");
    assertEquals(3, s3.getTrucks(), "Sat trucks");
    assertNull(schedule[8 + LiftTruck.HOURS_DAY * 6], "idle Sun");

    LiftTruck threeshift = trucks.get("threeShift");
    assertNotNull(threeshift, "found threeshift");
    threeshift.ensureShifts();
    schedule = threeshift.getShiftSchedule();
    assertNotNull(schedule, "exists");
    assertEquals(168, schedule.length, "size");
    assertNull(schedule[0], "idle Mon midnight");
    assertNull(schedule[5], "idle 5:00 Mon");
    s1 = schedule[6];
    assertNotNull(s1, "not idle 6:00 Mon");
    assertEquals(6, s1.getStart(), "s1 start");
    assertEquals(8, s1.getDuration(), "s1 dur");
    assertEquals(8, s1.getTrucks(), "s1 trucks");
    assertEquals(s1, schedule[13], "s1 Mon 13:00");
    s2 = schedule[14];
    assertNotNull(s2, "not idle Mon 14:00");
    assertNotSame(s1, s2, "different from s1");
    assertEquals(14, s2.getStart(), "s2 start");
    assertEquals(8, s2.getDuration(), "s2 dur");
    assertEquals(6, s2.getTrucks(), "s2 trucks");
    assertEquals(s2, schedule[21], "s2 Mon 21:00");
    s3 = schedule[22];
    assertNotNull(s3, "not idle Mon 22:00");
    assertNotSame(s1, s3, "different from s1");
    assertNotSame(s2, s3, "different from s2");
    assertEquals(22, s3.getStart(), "s3 start");
    assertEquals(8, s3.getDuration(), "s3 dur");
    assertEquals(4, s3.getTrucks(), "s3 trucks");
    assertEquals(s3, schedule[29], "s3 Tue 5:00");
    // check Friday - Sat AM
    assertEquals(s3, schedule[LiftTruck.HOURS_DAY * 4], "Fri 0:00");
    assertEquals(s3, schedule[5 + LiftTruck.HOURS_DAY * 4], "Fri 5:00");
    assertEquals(s1, schedule[6 + LiftTruck.HOURS_DAY * 4], "Fri 6:00");
    assertEquals(s1, schedule[13 + LiftTruck.HOURS_DAY * 4], "Fri 13:00");
    assertEquals(s2, schedule[14 + LiftTruck.HOURS_DAY * 4], "Fri 14:00");
    assertEquals(s2, schedule[21 + LiftTruck.HOURS_DAY * 4], "Fri 21:00");
    assertEquals(s3, schedule[22+ LiftTruck.HOURS_DAY * 4], "Fri 22:00");
    assertEquals(s3, schedule[5 + LiftTruck.HOURS_DAY * 5], "Sat 5:00");
    Shift s4 = schedule[6 + LiftTruck.HOURS_DAY * 5];
    assertNotNull(s4, "not idle Sat 6:00");
    assertNotSame(s1, s4, "different from s1");
    assertNotSame(s2, s4, "different from s2");
    assertNotSame(s3, s4, "different from s3");
    assertEquals(6, s4.getStart(), "s4 start");
    assertEquals(8, s4.getDuration(), "s4 dur");
    assertEquals(3, s4.getTrucks(), "s4 trucks");
    assertEquals(s4, schedule[6 + LiftTruck.HOURS_DAY * 5], "Sat 6:00");
    assertEquals(s4, schedule[13 + LiftTruck.HOURS_DAY * 5], "Sat 13:00");
    Shift s5 = schedule[14 + LiftTruck.HOURS_DAY * 5];
    assertNotNull(s5, "not idle Sat 14:00");
    assertNotSame(s1, s5, "different from s1");
    assertNotSame(s2, s5, "different from s2");
    assertNotSame(s3, s5, "different from s3");
    assertNotSame(s4, s5, "different from s4");
    assertEquals(14, s5.getStart(), "s5 start");
    assertEquals(8, s5.getDuration(), "s5 dur");
    assertEquals(2, s5.getTrucks(), "s5 trucks");
    assertEquals(s5, schedule[14 + LiftTruck.HOURS_DAY * 5], "Sat 14:00");
    assertEquals(s5, schedule[21 + LiftTruck.HOURS_DAY * 5], "Sat 21:00");
    assertNull(schedule[22 + LiftTruck.HOURS_DAY * 5], "idle Sat 22:00");
    assertNull(schedule[LiftTruck.HOURS_DAY * 6], "idle Sun 0:00");
    assertNull(schedule[5 + LiftTruck.HOURS_DAY * 6], "idle Sun 5:00");
    assertEquals(s4, schedule[6 + LiftTruck.HOURS_DAY * 6], "Sun 6:00");
    assertEquals(s4, schedule[13 + LiftTruck.HOURS_DAY * 6], "Sun 13:00");
    assertEquals(s5, schedule[14 + LiftTruck.HOURS_DAY * 6], "Sun 14:00");
    assertEquals(s5, schedule[21 + LiftTruck.HOURS_DAY * 6], "Sun 21:00");
    assertNull(schedule[22 + LiftTruck.HOURS_DAY * 6], "idle Sun 22:00");
    
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
    assertEquals(3, instances.size(), "three instances");
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck test1 = trucks.get("test1");
    assertNotNull(test1, "found test1");
    Shift[] schedule = test1.getShiftSchedule();
    for (Shift shift: schedule) {
      if (null != shift)
        fail("test1 non-null entry");
    }

    LiftTruck test2 = trucks.get("test2");
    assertNotNull(test2, "found test2");
    schedule = test2.getShiftSchedule();
    for (Shift shift: schedule) {
      if (null != shift)
        fail("test1 non-null entry");
    }

    LiftTruck test3 = trucks.get("test3");
    assertNotNull(test3, "found test3");
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
    assertEquals(1, instances.size(), "one instance");
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck test = trucks.get("test");
    assertNotNull(test, "found uut");
    test.ensureShifts();
    Shift[] schedule = test.getShiftSchedule();
    assertNotNull(schedule, "schedule exists");
    assertEquals(168, schedule.length, "correct size");
    Shift s4 = schedule[0];
    assertEquals(18, s4.getStart(), "Mon 0:00 start");
    assertEquals(10, s4.getDuration(), "Mon 0:00 dur");
    assertEquals(2, s4.getTrucks(), "Mon 0:00 trucks");
    assertEquals(s4, schedule[3], "Mon 3:00");
    assertNull(schedule[4], "idle Mon 4:00");
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
    assertEquals(2, instances.size(), "two instances");
    Map<String, LiftTruck> trucks = mapNames(instances);

    LiftTruck shortTruck = trucks.get("short");
    assertEquals(6, shortTruck.getNBatteries(), "short before validation");
    shortTruck.validateBatteries();
    assertEquals(14, shortTruck.getNBatteries(), "short after validation");

    LiftTruck longTruck = trucks.get("long");
    assertEquals(10, longTruck.getNBatteries(), "long before validation");
    longTruck.validateBatteries();
    assertEquals(16, longTruck.getNBatteries(), "long after validation");
  }

  // charger validation - check limits
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
    assertEquals(3, instances.size(), "three instances");
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck tkw = trucks.get("truck_kw");
    tkw.ensureShifts();
    assertEquals(8, tkw.getNChargers(), "8 chargers tkw");
    tkw.validateChargers();
    assertEquals(10, tkw.getNChargers(), "10 after tkw validation");

    LiftTruck ckw = trucks.get("charge_kw");
    ckw.ensureShifts();
    assertEquals(8, ckw.getNChargers(), "8 chargers");
    ckw.validateChargers();
    assertEquals(12, ckw.getNChargers(), "12 after validation");

    LiftTruck nc = trucks.get("ncharge");
    nc.ensureShifts();
    assertEquals(3, nc.getNChargers(), "3 chargers");
    nc.validateChargers();
    assertEquals(4, nc.getNChargers(), "4 after validation");
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
    assertEquals(5, c5.getNChargers(), "5 chargers c5");
    c5.validateChargers();
    assertEquals(5, c5.getNChargers(), "5 after c5 validation");
  }

  // charger validation with null shift at midnight
  @Test
  public void testValidateChargers3 ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.liftTruck.instances", "c5");
    map.put("customer.model.liftTruck.c5.nChargers", "5");
    map.put("customer.model.liftTruck.c5.shiftData",
            "block,1,2,3,4,5, shift,6,8,8, shift,14,8,6");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(LiftTruck.class);
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck c5 = trucks.get("c5");
    c5.ensureShifts();
    assertEquals(5, c5.getNChargers(), "5 chargers c5");
    c5.validateChargers();
    assertEquals(5, c5.getNChargers(), "5 after c5 validation");
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
    truck.setServiceAccessor(serviceAccessor);
    truck.initialize();
    // now we should see default data
    Shift[] shifts = truck.getShiftSchedule();
    for (Shift s : shifts) {
      if (null != s)
        return;
    }
    fail("shift schedule should be non-empty");
  }

  @Test
  public void testFutureEnergyNeedsDefault ()
  {
    LiftTruck truck = new LiftTruck("Test");
    truck.setServiceAccessor(serviceAccessor);
    truck.initialize();
    ZonedDateTime now =
        ZonedDateTime.of(2014, 12, 1, 10, 0, 0, 0, TimeService.UTC);
    new Timeslot(2, now.toInstant());
    ShiftEnergy[] needs =
        truck.getFutureEnergyNeeds(now.toInstant(), 60, 0.0);
    // ix  dur  end  req  chg  max  sur
    //  0    6   16  192    7  252   60
    //  1    8    0   96    8  384  288
    //  2    8    8  256    8  384  128
    //  3    8   16  192    7  336  144
    //  4    8    0   96    8  384  288
    //  5    8    8  256    8  384  128
    //  6    8   16  192    7  336  144
    //  7    0    0   96    8  384  288
    assertNotNull(needs, "needs not null");
    assertEquals(8, needs.length, "8 items");
    assertEquals(6, needs[0].getDuration(), "duration[0] is 6");
    assertEquals(16, needs[0].getNextShift().getStart(), "[0] ends at 16");
    assertEquals(192.0 / truck.getChargeEfficiency(), needs[0].getEnergyNeeded(), 1e-6, "[0] requires");
    assertEquals(60.0 / truck.getChargeEfficiency(), needs[0].getMaxSurplus(), 1e-6, "[0] max surplus");
    assertEquals(0, needs[1].getNextShift().getStart(), "[1] ends at 0");
    assertEquals(96.0 / truck.getChargeEfficiency(), needs[1].getEnergyNeeded(), 1e-6, "[1] requires");
    assertEquals(288.0 / truck.getChargeEfficiency(), needs[1].getMaxSurplus(), 1e-6, "[1] surplus");
    assertEquals(8, needs[1].getDuration(), "[1] dur");
    assertEquals(8, needs[2].getNextShift().getStart(), "[2] ends at 8");
    assertEquals(256.0 / truck.getChargeEfficiency(), needs[2].getEnergyNeeded(), 1e-6, "[2] requires");
    assertEquals(128.0 / truck.getChargeEfficiency(), needs[2].getMaxSurplus(), 1e-6, "[2] surplus");
    assertEquals(0, needs[7].getNextShift().getStart(), "[7] ends at 0");
    assertEquals(96.0 / truck.getChargeEfficiency(), needs[7].getEnergyNeeded(), 1e-6, "[7] requires");
    assertEquals(288.0 / truck.getChargeEfficiency(), needs[7].getMaxSurplus(), 1e-6, "[7] surplus");
    assertEquals(8, needs[7].getDuration(), "[7] dur");
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
    assertEquals(1, instances.size(), "one instance");
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck truck = trucks.get("short");
    assertNotNull(truck, "found short");

    truck.setServiceAccessor(serviceAccessor);
    truck.initialize();
    ZonedDateTime now =
        ZonedDateTime.of(2014, 12, 1, 10, 0, 0, 0, TimeService.UTC);
    new Timeslot(2, now.toInstant());
    ShiftEnergy[] needs =
        truck.getFutureEnergyNeeds(now.toInstant(), 60, 14.0);
    // ix  dur  end  req  chg  max  sur
    //  0    6   16  192    5  180  -12+14
    //  1    8    0   96    5  240  128
    //  2    8    8  256    5  240  -16
    //  3    8   16  192    5  240   48
    //  4    8    0   96    5  240  128
    //  5    8    8  256    5  240  -16
    //  6    8   16  192    5  240   48
    //  7    0    0   96    5  240  144
    assertNotNull(needs, "needs not null");
    assertEquals(8, needs.length, "8 items");
    assertEquals(6, needs[0].getDuration(), "duration[0] is 6");
    assertEquals(192.0 / truck.getChargeEfficiency(), needs[0].getEnergyNeeded(), 1e-6, "[0] requires");
    assertEquals(-12.0 / truck.getChargeEfficiency() + 14, needs[0].getMaxSurplus(), 1e-6, "[0] max surplus");
    assertEquals(96.0 / truck.getChargeEfficiency(), needs[1].getEnergyNeeded(), 1e-6, "[1] requires");
    assertEquals(128.0 / truck.getChargeEfficiency(), needs[1].getMaxSurplus(), 1e-6, "[1] surplus");
    assertEquals(256.0 / truck.getChargeEfficiency(), needs[2].getEnergyNeeded(), 1e-6, "[2] requires");
    assertEquals(-16.0 / truck.getChargeEfficiency(), needs[2].getMaxSurplus(), 1e-6, "[2] surplus");
    assertEquals(96.0 / truck.getChargeEfficiency(), needs[7].getEnergyNeeded(), 1e-6, "[7] requires");
    assertEquals(144.0 / truck.getChargeEfficiency(), needs[7].getMaxSurplus(), 1e-6, "[7] surplus");
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
    assertEquals(1, instances.size(), "one instance");
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck tk = trucks.get("idle");
    assertNotNull(tk, "got configured");
    tk.setServiceAccessor(serviceAccessor);
    tk.initialize();
    ZonedDateTime now =
        ZonedDateTime.of(2014, 12, 1, 10, 0, 0, 0, TimeService.UTC);
    new Timeslot(2, now.toInstant());
    ShiftEnergy[] needs = 
        tk.getFutureEnergyNeeds(now.toInstant(), 60, 4.0);
    // ix  dur  end  req  chg  max  sur
    //  0    6   16    0    5  180  180+4
    //  1    8    0  192    5  240   32
    //  2    8    8  256    5  240  -16
    //  3    8   16    0    5  240  240
    //  4    8    0  192    5  240   32
    //  5    8    8  256    5  240  -16
    //  6    8   16    0    5  240  240
    //  7    0    0  192    5  240   48
    assertNotNull(needs, "needs not null");
    assertEquals(8, needs.length, "8 items");
    assertEquals(6, needs[0].getDuration(), "duration[0] is 6");
    assertNull(needs[0].getNextShift(), "[0] ends at idle");
    assertEquals(0.0, needs[0].getEnergyNeeded(), 1e-6, "[0] req");
    assertEquals(180.0 / tk.getChargeEfficiency() + 4.0, needs[0].getMaxSurplus(), 1e-6, "[0] sur");
    assertNotNull(needs[1].getNextShift(), "[1] not null");
    assertEquals(0, needs[1].getNextShift().getStart(), "[1] ends 00:00");
    assertEquals(192.0 / tk.getChargeEfficiency(), needs[1].getEnergyNeeded(), 1e-6, "[1] req");
    assertEquals(32.0 / tk.getChargeEfficiency(), needs[1].getMaxSurplus(), 1e-6, "[1] sur");
    assertEquals(256.0 / tk.getChargeEfficiency(), needs[2].getEnergyNeeded(), 1e-6, "[2] req");
    assertEquals(-16.0 / tk.getChargeEfficiency(), needs[2].getMaxSurplus(), 1e-6, "[2] sur");
    assertEquals(192.0 / tk.getChargeEfficiency(), needs[7].getEnergyNeeded(), 1e-6, "[7] req");
    assertEquals(48.0 / tk.getChargeEfficiency(), needs[7].getMaxSurplus(), 1e-6, "[7] sur");
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
    assertEquals(1, instances.size(), "one instance");
    Map<String, LiftTruck> trucks = mapNames(instances);
    LiftTruck tk = trucks.get("idle");
    assertNotNull(tk, "got configured");
    tk.setServiceAccessor(serviceAccessor);
    tk.initialize();
    // start on Sunday
    ZonedDateTime now =
        ZonedDateTime.of(2014, 12, 7, 6, 0, 0, 0, TimeService.UTC);
    new Timeslot(2, now.toInstant());
    ShiftEnergy[] needs =
        tk.getFutureEnergyNeeds(now.toInstant(), 60, 75.0);
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
    assertNotNull(needs, "needs not null");
    assertEquals(9, needs.length, "9 items");
    assertEquals(2, needs[0].getDuration(), "duration[0] is 2");
    assertEquals(128.0 / tk.getChargeEfficiency(), needs[0].getEnergyNeeded(), 1e-6, "[0] req");
    assertEquals(-68.0 / tk.getChargeEfficiency() + 75.0, needs[0].getMaxSurplus(), 1e-6, "[0] sur");
    assertNull(needs[1].getNextShift(), "[1] ends at idle");
    assertEquals(0.0, needs[1].getEnergyNeeded(), 1e-6, "[1] req");
    assertEquals(240.0 / tk.getChargeEfficiency(), needs[1].getMaxSurplus(), 1e-6, "[1] sur");
    assertNotNull(needs[2].getNextShift(), "[2] not null");
    assertEquals(0, needs[2].getNextShift().getStart(), "[2] ends 00:00");
    assertEquals(192.0 / tk.getChargeEfficiency(), needs[2].getEnergyNeeded(), 1e-6, "[2] req");
    assertEquals(32.0 / tk.getChargeEfficiency(), needs[2].getMaxSurplus(), 1e-6, "[2] sur");
    assertEquals(256.0 / tk.getChargeEfficiency(), needs[3].getEnergyNeeded(), 1e-6, "[3] req");
    assertEquals(-16.0 / tk.getChargeEfficiency(), needs[3].getMaxSurplus(), 1e-6, "[3] sur");
    assertEquals(0.0, needs[7].getEnergyNeeded(), 1e-6, "[7] req");
    assertEquals(240.0 / tk.getChargeEfficiency(), needs[7].getMaxSurplus(), 1e-6, "[7] sur");
    assertEquals(192.0 / tk.getChargeEfficiency(), needs[8].getEnergyNeeded(), 1e-6, "[8] req");
    assertEquals(48.0 / tk.getChargeEfficiency(), needs[8].getMaxSurplus(), 1e-6, "[8] sur");
  }

  @Test
  public void testPlanFlatDefault ()
  {
    LiftTruck truck = new LiftTruck("Test");
    truck.setServiceAccessor(serviceAccessor);
    truck.initialize();
    ZonedDateTime now =
        ZonedDateTime.of(2014, 12, 1, 10, 0, 0, 0, TimeService.UTC);
    Timeslot ts = new Timeslot(2, now.toInstant());
    when(tsRepo.currentTimeslot()).thenReturn(ts);
    Broker broker = new Broker("bob");
    TariffSpecification spec =
        new TariffSpecification(broker, PowerType.CONSUMPTION);
    Rate rate = new Rate().withValue(0.15);
    spec.addRate(rate);
    Tariff tariff = new Tariff(spec);
    TimeService tsvc = mock(TimeService.class);
    when(tsvc.getCurrentTime()).thenReturn(now.toInstant());
    ReflectionTestUtils.setField(tariff, "timeService", tsvc);
    ReflectionTestUtils.setField(tariff, "tariffRepo", mock(TariffRepo.class));
    tariff.init();

    LiftTruck.CapacityPlan plan =
        truck.getCapacityPlan(tariff, now.toInstant(), 95);
    assertNotNull(plan, "Created a plan");
    assertNull(plan.getCapacityProfile().getProfile(), "No solution yet");
    plan.createPlan(1.0);

    double[] usage = plan.getCapacityProfile().getProfile();
    assertEquals(102, usage.length, "correct length");

    ShiftEnergy[] needs = plan.updateNeeds();
    assertEquals(13, needs.length, "correct length");

  }

  /**
   * Test method for {@link org.powertac.customer.model.LiftTruck#step()}.
   */
  @Test
  public void testStep ()
  {
//    Instant now = new Instant();
//    System.out.println("hour: " + now.get(DateTimeFieldType.hourOfDay()));
//    System.out.println("day: " + now.get(DateTimeFieldType.dayOfWeek()));
  }

  class ServiceAccessor implements CustomerServiceAccessor
  {

    @Override
    public CustomerRepo getCustomerRepo ()
    {
      return null;
    }

    @Override
    public RandomSeedRepo getRandomSeedRepo ()
    {
      return mockSeedRepo;
    }

    @Override
    public TariffRepo getTariffRepo ()
    {
      return tariffRepo;
    }

    @Override
    public TariffSubscriptionRepo getTariffSubscriptionRepo ()
    {
      return mockSubscriptionRepo;
    }

    @Override
    public TimeslotRepo getTimeslotRepo ()
    {
      return tsRepo;
    }

    @Override
    public TimeService getTimeService ()
    {
      return timeService;
    }

    @Override
    public WeatherReportRepo getWeatherReportRepo ()
    {
      return null;
    }

    @Override
    public ServerConfiguration getServerConfiguration ()
    {
      // Auto-generated method stub
      return null;
    }

    @Override
    public TariffMarket getTariffMarket ()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public XMLMessageConverter getMessageConverter ()
    {
      // TODO Auto-generated method stub
      return null;
    }
  }
}
