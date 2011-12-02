/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.common;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class HourlyChargeTests
{
  Instant now;
  HourlyCharge hc;

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
