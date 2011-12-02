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
    Competition.newInstance("test");
    baseTime = new DateTime().toInstant();
  }

  @Test
  public void testTimeslot ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime, null);
    assertNotNull("not null", ts1);
    assertNull("no previous", ts1.getPrevious());
  }
  
  @Test
  public void testTimeslotPrev ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime, null);
    Timeslot ts2 = new Timeslot(2, 
                                new Instant(baseTime.getMillis() + TimeService.HOUR), 
                                ts1);
    assertNotNull("not null", ts2);
    assertEquals("correct previous", ts1, ts2.getPrevious());
    assertEquals("correct next", ts2, ts1.getNext());
  }

  @Test
  public void testGetSerialNumber ()
  {
    Timeslot ts1 = new Timeslot(42, baseTime, null);
    assertEquals("correct serial", 42, ts1.getSerialNumber());
  }

  @Test
  public void testIsEnabled ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime, null);
    assertTrue("enabled", ts1.isEnabled());
  }

  @Test
  public void testEnable ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime, null);
    ts1.disable();
    assertFalse("disabled", ts1.isEnabled());
  }

  @Test
  public void testDisable ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime, null);
    ts1.disable();
    assertFalse("disabled", ts1.isEnabled());
    ts1.enable();
    assertTrue("enabled", ts1.isEnabled());
  }

  @Test
  public void testGetStartInstant ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime, null);
    assertEquals("correct start", baseTime, ts1.getStartInstant());
  }

//  @Test
//  public void testGetEndInstant ()
//  {
//    Timeslot ts1 = new Timeslot(1, baseTime, null);
//    assertEquals("correct end", new Instant(baseTime.getMillis() + TimeService.HOUR), ts1.getEndInstant());
//  }

  @Test
  public void testGetNextPrev ()
  {
    Timeslot ts1 = new Timeslot(1, baseTime, null);
    assertNull("no prev", ts1.getPrevious());
    assertNull("no next", ts1.getNext());
    Timeslot ts2 = new Timeslot(2, 
                                new Instant(baseTime.getMillis() + TimeService.HOUR), 
                                ts1);
    assertEquals("correct previous", ts1, ts2.getPrevious());
    assertEquals("correct next", ts2, ts1.getNext());
    assertNull("no prev", ts1.getPrevious());
    assertNull("no next", ts2.getNext());    
  }

  @Test
  public void xmlSerializationTest ()
  {
    Timeslot ts1 = new Timeslot(1, 
                                baseTime, 
                                null);
    XStream xstream = new XStream();
    xstream.processAnnotations(Timeslot.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ts1));
    //System.out.println(serialized.toString());
    Timeslot xts1 = (Timeslot)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xts1);
    assertEquals("correct serial", 1, xts1.getSerialNumber());
    assertEquals("correct start", baseTime.getMillis(), xts1.getStartInstant().getMillis());
  }
}
