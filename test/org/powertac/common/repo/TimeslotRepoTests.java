package org.powertac.common.repo;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.springframework.test.util.ReflectionTestUtils;

public class TimeslotRepoTests
{
  TimeService timeService;
  Instant baseTime;
  TimeslotRepo repo;
  
  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    timeService = new TimeService();
    baseTime = new Instant();
    timeService.setCurrentTime(baseTime);
    repo = new TimeslotRepo();
    ReflectionTestUtils.setField(repo, "timeService", timeService);
  }

  @Test
  public void testTimeslotRepo ()
  {
    assertNotNull("not null", repo);
    assertEquals("empty", 0, repo.count());
  }

  @Test
  public void testMakeTimeslotNormal ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime, new Instant(baseTime.getMillis() + TimeService.HOUR));
    assertNotNull("not null 0", ts0);
    assertEquals("correct sn", 0, ts0.getSerialNumber());
    assertEquals("correct start", baseTime, ts0.getStartInstant());
    assertEquals("correct count", 1, repo.count());
    assertNull("prev null", ts0.getPrevious());
    assertNull("next null", ts0.getNext());
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    assertNotNull("not null 1", ts1);
    assertEquals("correct sn", 1, ts1.getSerialNumber());
    assertEquals("correct start", baseTime.getMillis() + TimeService.HOUR, ts1.getStartInstant().getMillis());
    assertEquals("correct count", 2, repo.count());
    assertNull("ts0 prev null", ts0.getPrevious());
    assertEquals("prev ts1", ts0, ts1.getPrevious());
    assertEquals("next ts0", ts1, ts0.getNext());
    assertNull("ts1 next null", ts1.getNext());
  }
  
  @Test
  public void testMakeTimeslotBackward ()
  {
    Timeslot ts0 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    assertEquals("correct sn", 0, ts0.getSerialNumber());
    Timeslot ts1 = repo.makeTimeslot(baseTime, new Instant(baseTime.getMillis() + TimeService.HOUR));
    assertNull("null ts1", ts1);
    assertEquals("correct count", 1, repo.count());
  }

  @Test
  public void testCurrentTimeslot ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime, new Instant(baseTime.getMillis() + TimeService.HOUR));
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    Timeslot ts2 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR * 2), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 3));
    assertEquals("ts0 current", ts0, repo.currentTimeslot());
    timeService.setCurrentTime(new Instant(baseTime.getMillis() + TimeService.HOUR));
    assertEquals("ts1 current", ts1, repo.currentTimeslot());
    timeService.setCurrentTime(new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    assertEquals("ts2 current", ts2, repo.currentTimeslot());
   
  }

  @Test
  public void testFindBySerialNumber ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime, new Instant(baseTime.getMillis() + TimeService.HOUR));
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    Timeslot ts2 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR * 2), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 3));
    assertEquals("correct count", 3, repo.count());
    assertEquals("sn 0", ts0, repo.findBySerialNumber(0));
    assertEquals("sn 1", ts1, repo.findBySerialNumber(1));
    assertEquals("sn 2", ts2, repo.findBySerialNumber(2));
  }

  @Test
  public void testEnabledTimeslots0 ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime, new Instant(baseTime.getMillis() + TimeService.HOUR));
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    Timeslot ts2 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR * 2), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 3));
    assertEquals("correct count", 3, repo.count());
    List enabled = repo.enabledTimeslots();
    assertEquals("3 enabled", 3, enabled.size());
    assertEquals("first is ts0", ts0, enabled.get(0));
  }

  @Test
  public void testEnabledTimeslots1 ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime, new Instant(baseTime.getMillis() + TimeService.HOUR));
    ts0.disable();
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    Timeslot ts2 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR * 2), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 3));
    assertEquals("correct count", 3, repo.count());
    List<Timeslot> enabled = repo.enabledTimeslots();
    assertEquals("2 enabled", 2, enabled.size());
    assertEquals("first is ts1", ts1, enabled.get(0));
  }

  @Test
  public void testRecycle ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime, new Instant(baseTime.getMillis() + TimeService.HOUR));
    ts0.disable();
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    Timeslot ts2 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR * 2), 
                                     new Instant(baseTime.getMillis() + TimeService.HOUR * 3));
    assertEquals("correct count", 3, repo.count());
    List<Timeslot> enabled = repo.enabledTimeslots();
    assertEquals("ts0 current", ts0, repo.currentTimeslot());
    repo.recycle();
    assertEquals("correct count", 0, repo.count());
    enabled = repo.enabledTimeslots();
    assertNull("no enabled", enabled);
    ts0 = repo.makeTimeslot(baseTime, new Instant(baseTime.getMillis() + TimeService.HOUR));
    assertEquals("correct count", 1, repo.count());
    enabled = repo.enabledTimeslots();
    assertEquals("no enabled", 1, enabled.size());
  }

}
