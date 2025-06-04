package org.powertac.common.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Competition;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.springframework.test.util.ReflectionTestUtils;

public class TimeslotRepoTests
{
  TimeService timeService;
  Instant baseTime;
  TimeslotRepo repo;

  @BeforeEach
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
    assertNotNull(repo, "not null");
    assertEquals(0, repo.count(), "empty");
  }

  @Test
  public void testMakeTimeslotNormal ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    assertNotNull(ts0, "not null 0");
    assertEquals(0, ts0.getSerialNumber(), "correct sn");
    assertEquals(baseTime, ts0.getStartInstant(), "correct start");
    assertEquals(1, repo.count(), "correct count");
    Timeslot ts1 = repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR));
    assertNotNull(ts1, "not null 1");
    assertEquals(1, ts1.getSerialNumber(), "correct sn");
    assertEquals(baseTime.toEpochMilli() + TimeService.HOUR, ts1.getStartInstant().toEpochMilli(), "correct start");
    assertEquals(2, repo.count(), "correct count");
  }
  
  @Test
  public void testMakeTimeslotBackward ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime.plusMillis( TimeService.HOUR));
    assertEquals(1, ts0.getSerialNumber(), "correct sn 1");
    Timeslot ts1 = repo.makeTimeslot(baseTime);
    assertEquals(0, ts1.getSerialNumber(), "correct sn 0");
    assertEquals(2, repo.count(), "correct count");
  }

  @Test
  public void testCurrentTimeslot ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(baseTime.plusMillis( TimeService.HOUR));
    Timeslot ts2 = repo.makeTimeslot(baseTime.plusMillis( TimeService.HOUR * 2));
    assertEquals(ts0, repo.currentTimeslot(), "ts0 current");
    timeService.setCurrentTime(baseTime.plusMillis( TimeService.HOUR));
    assertEquals(ts1, repo.currentTimeslot(), "ts1 current");
    timeService.setCurrentTime(baseTime.plusMillis(TimeService.HOUR * 2));
    assertEquals(ts2, repo.currentTimeslot(), "ts2 current");   
  }

  @Test
  public void testFindBySerialNumber ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR));
    Timeslot ts2 = repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR * 2));
    assertEquals(3, repo.count(), "correct count");
    assertEquals(ts0, repo.findBySerialNumber(0), "sn 0");
    assertEquals(ts1, repo.findBySerialNumber(1), "sn 1");
    assertEquals(ts2, repo.findBySerialNumber(2), "sn 2");
  }

  @Test
  public void testFindOrCreateBySerialNumber ()
  {
    repo.makeTimeslot(baseTime);
    repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR));
    Timeslot ts4 = repo.findOrCreateBySerialNumber(4);
    assertEquals(5, repo.count(), "5 entries");
    assertEquals(4, ts4.getSerialNumber(), "sn 4");
  }
  
  @Test
  public void testFindOrCreateAll ()
  {
    Competition comp = Competition.currentCompetition();
    comp.withSimulationBaseTime(baseTime);
    Timeslot ts4 = repo.findOrCreateBySerialNumber(4);
    assertEquals(5, repo.count(), "5 entries");
    assertEquals(4, ts4.getSerialNumber(), "sn 4");
  }
  
  @Test
  public void testCreateInitial ()
  {
    Competition comp = Competition.currentCompetition();
    comp.withSimulationBaseTime(baseTime);
    timeService.setCurrentTime(baseTime.plusMillis(TimeService.HOUR * 10));
    repo.createInitialTimeslots();
    assertEquals(11, repo.count(), "11 entries");
  }

  @SuppressWarnings("unused")
  @Test
  public void testEnabledTimeslots0 ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR));
    Timeslot ts2 = repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR * 2));
    assertEquals(3, repo.count(), "correct count");
    List<Timeslot> enabled = repo.enabledTimeslots();
    assertEquals(24, enabled.size(), "24 enabled");
    assertEquals(ts1, enabled.get(0), "first is ts1");
  }

  @SuppressWarnings("unused")
  @Test
  public void testEnabledTimeslots1 ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR));
    Timeslot ts2 = repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR * 2));
    assertEquals(3, repo.count(), "correct count");
    List<Timeslot> enabled = repo.enabledTimeslots();
    assertEquals(24, enabled.size(), "24 enabled");
    assertEquals(ts1, enabled.get(0), "first is ts1");
  }

  @SuppressWarnings("unused")
  @Test
  public void testTimeForIndex ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR));
    long tsDuration = Competition.currentCompetition().getTimeslotDuration();
    assertEquals(0, ts0.getSerialNumber(), "correct base index");
    assertEquals(baseTime.plusMillis(tsDuration * 3), repo.getTimeForIndex(3), "correct offset");
  }

  @SuppressWarnings("unused")
  @Test
  public void testRecycle ()
  {
    Timeslot ts0 = repo.makeTimeslot(baseTime);
    Timeslot ts1 = repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR));
    Timeslot ts2 = repo.makeTimeslot(baseTime.plusMillis(TimeService.HOUR * 2));
    assertEquals(3, repo.count(), "correct count");
    List<Timeslot> enabled = repo.enabledTimeslots();
    assertEquals(ts0, repo.currentTimeslot(), "ts0 current");
    repo.recycle();
    assertEquals(0, repo.count(), "correct count");
    ts0 = repo.makeTimeslot(baseTime);
    assertEquals(1, repo.count(), "correct count");
    enabled = repo.enabledTimeslots();
    assertEquals(24, enabled.size(), "24 enabled");
  }
}
