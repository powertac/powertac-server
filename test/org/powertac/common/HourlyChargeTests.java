package org.powertac.common;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class HourlyChargeTests
{
  Instant now;
  HourlyCharge hc;
  
  @BeforeClass
  public static void setUpLog () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    now = new DateTime().toInstant();
    hc = new HourlyCharge(now, 0.21);
  }

  @Test
  public void testHourlyCharge ()
  {
    assertNotNull("something created", hc);
    assertEquals("correct time", now.getMillis(), hc.getAtTime().getMillis());
    assertEquals("correct amount", 0.21, hc.getValue(), 1e-6);
    assertEquals("default rateId", -1l, hc.getRateId());
  }

  @Test
  public void testRateId ()
  {
    assertEquals("default rateId", -1l, hc.getRateId());
    hc.setRateId(42l);
    assertEquals("correct rateId", 42l, hc.getRateId());
  }
  
  @Test
  public void testCompareTo ()
  {
    HourlyCharge hcLt = new HourlyCharge(new Instant(now.minus(10000l)), 0.33);
    HourlyCharge hcGt = new HourlyCharge(new Instant(now.plus(10000l)), 0.13);
    assertTrue("lt sorts first", hc.compareTo(hcLt) > 0);
    assertTrue("gt sorts last", hc.compareTo(hcGt) < 0);
  }

  @Test
  public void xmlSerializationTest ()
  {
    hc.setRateId(37l);
    XStream xstream = new XStream();
    xstream.processAnnotations(HourlyCharge.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(hc));
    //System.out.println(serialized.toString());
    HourlyCharge xhc= (HourlyCharge)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xhc);
    assertEquals("correct time", now.getMillis(), xhc.getAtTime().getMillis());
    assertEquals("correct amount", 0.21, xhc.getValue(), 1e-6);
    assertEquals("correct rate ID", 37l, xhc.getRateId());
  }
}
