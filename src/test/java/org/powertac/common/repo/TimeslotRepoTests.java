package org.powertac.common.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Competition;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.springframework.test.util.ReflectionTestUtils;

public class TimeslotRepoTests
{
  TimeService timeService;
  Instant baseTime;
  TimeslotRepo repo;

  @Before
  public void setUp () throws Exception
  {
    Competition.setCurrent(Competition.newInstance("test"));
    timeService = new TimeService();
    baseTime = Competition.currentCompetition().getSimulationBaseTime();
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
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    assertNotNull("not null 0", ts0);
    assertEquals("correct sn", 0, ts0.getSerialNumber());
    assertEquals("correct start", baseTime, ts0.getStartInstant());
    assertEquals("correct count", 1, repo.count());
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR));
    assertNotNull("not null 1", ts1);
    assertEquals("correct sn", 1, ts1.getSerialNumber());
    assertEquals("correct start", baseTime.getMillis() + TimeService.HOUR, ts1.getStartInstant().getMillis());
    assertEquals("correct count", 2, repo.count());
  }
  
  @Test
  public void testMakeTimeslotBackward ()
  {
    Timeslot ts0 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR));
    assertEquals("correct sn 1", 1, ts0.getSerialNumber());
    Timeslot ts1 = repo.makeTimeslot(baseTime);
    assertEquals("correct sn 0", 0, ts1.getSerialNumber());
    assertEquals("correct count", 2, repo.count());
  }

  @Test
  public void testCurrentTimeslot ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR));
    Timeslot ts2 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    assertEquals("ts0 current", ts0, repo.currentTimeslot());
    timeService.setCurrentTime(new Instant(baseTime.getMillis() + TimeService.HOUR));
    assertEquals("ts1 current", ts1, repo.currentTimeslot());
    timeService.setCurrentTime(new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    assertEquals("ts2 current", ts2, repo.currentTimeslot());   
  }

  @Test
  public void testFindBySerialNumber ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR));
    Timeslot ts2 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    assertEquals("correct count", 3, repo.count());
    assertEquals("sn 0", ts0, repo.findBySerialNumber(0));
    assertEquals("sn 1", ts1, repo.findBySerialNumber(1));
    assertEquals("sn 2", ts2, repo.findBySerialNumber(2));
  }

  @Test
  public void testFindOrCreateBySerialNumber ()
  {
    repo.makeTimeslot(baseTime);
    repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR));
    Timeslot ts4 = repo.findOrCreateBySerialNumber(4);
    assertEquals("5 entries", 5, repo.count());
    assertEquals("sn 4", 4, ts4.getSerialNumber());
  }
  
  @Test
  public void testFindOrCreateAll ()
  {
    Competition comp = Competition.currentCompetition();
    comp.withSimulationBaseTime(baseTime);
    Timeslot ts4 = repo.findOrCreateBySerialNumber(4);
    assertEquals("5 entries", 5, repo.count());
    assertEquals("sn 4", 4, ts4.getSerialNumber());
  }
  
  @Test
  public void testCreateInitial ()
  {
    Competition comp = Competition.currentCompetition();
    comp.withSimulationBaseTime(baseTime);
    timeService.setCurrentTime(baseTime.plus(TimeService.HOUR * 10));
    repo.createInitialTimeslots();
    assertEquals("11 entries", 11, repo.count());
  }

  @SuppressWarnings("unused")
  @Test
  public void testEnabledTimeslots0 ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR));
    Timeslot ts2 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    assertEquals("correct count", 3, repo.count());
    List<Timeslot> enabled = repo.enabledTimeslots();
    assertEquals("24 enabled", 24, enabled.size());
    assertEquals("first is ts1", ts1, enabled.get(0));
  }

  @SuppressWarnings("unused")
  @Test
  public void testEnabledTimeslots1 ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR));
    Timeslot ts2 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    assertEquals("correct count", 3, repo.count());
    List<Timeslot> enabled = repo.enabledTimeslots();
    assertEquals("24 enabled", 24, enabled.size());
    assertEquals("first is ts1", ts1, enabled.get(0));
  }

  @SuppressWarnings("unused")
  @Test
  public void testRecycle ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR));
    Timeslot ts2 = repo.makeTimeslot(new Instant(baseTime.getMillis() + TimeService.HOUR * 2));
    assertEquals("correct count", 3, repo.count());
    List<Timeslot> enabled = repo.enabledTimeslots();
    assertEquals("ts0 current", ts0, repo.currentTimeslot());
    repo.recycle();
    assertEquals("correct count", 0, repo.count());
    ts0 = repo.makeTimeslot(baseTime);
    assertEquals("correct count", 1, repo.count());
    enabled = repo.enabledTimeslots();
    assertEquals("24 enabled", 24, enabled.size());
  }
}
