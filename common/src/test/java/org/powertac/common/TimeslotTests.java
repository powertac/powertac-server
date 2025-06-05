package org.powertac.common;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TimeslotTests
{
  Instant baseTime;
  
  @BeforeEach
  public void setUp () throws Exception
  {
    Competition.setCurrent(Competition.newInstance("test"));
    baseTime = Competition.currentCompetition().getSimulationBaseTime();
  }

  @Test
  public void testTimeslot ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime);
    assertNotNull(ts1, "not null");
  }
  
  @Test
  public void testTimeslotPrev ()
  {
    Timeslot ts2 = new Timeslot(2, baseTime.plusMillis(TimeService.HOUR));
    assertNotNull(ts2, "not null");
  }

  @Test
  public void testGetSerialNumber ()
  {
    Timeslot ts1 = new Timeslot(42, baseTime);
    assertEquals(42, ts1.getSerialNumber(), "correct serial");
  }

  @Test
  public void testGetStartInstant ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime);
    assertEquals(baseTime, ts1.getStartInstant(), "correct start");
  }
}
