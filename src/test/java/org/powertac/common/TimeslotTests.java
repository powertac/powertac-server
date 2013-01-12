package org.powertac.common;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class TimeslotTests
{
  Instant baseTime;
  
  @Before
  public void setUp () throws Exception
  {
    Competition.setCurrent(Competition.newInstance("test"));
    baseTime = Competition.currentCompetition().getSimulationBaseTime();
  }

  @Test
  public void testTimeslot ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime);
    assertNotNull("not null", ts1);
  }
  
  @Test
  public void testTimeslotPrev ()
  {
    Timeslot ts2 = new Timeslot(2, 
                                new Instant(baseTime.getMillis() + TimeService.HOUR));
    assertNotNull("not null", ts2);
  }

  @Test
  public void testGetSerialNumber ()
  {
    Timeslot ts1 = new Timeslot(42, baseTime);
    assertEquals("correct serial", 42, ts1.getSerialNumber());
  }

  @Test
  public void testGetStartInstant ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime);
    assertEquals("correct start", baseTime, ts1.getStartInstant());
  }
}
