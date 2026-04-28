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

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;

import java.time.ZonedDateTime;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thoughtworks.xstream.XStream;

public class HourlyChargeTests
{
  Instant now;
  HourlyCharge hc;

  @BeforeEach
  public void setUp () throws Exception
  {
    now = ZonedDateTime.now().toInstant();
    hc = new HourlyCharge(now, 0.21);
  }

  @Test
  public void testHourlyCharge ()
  {
    assertNotNull(hc, "something created");
    assertEquals(now.toEpochMilli(), hc.getAtTime().toEpochMilli(), "correct time");
    assertEquals(0.21, hc.getValue(), 1e-6, "correct amount");
    assertEquals(-1l, hc.getRateId(), "default rateId");
  }

  @Test
  public void testRateId ()
  {
    assertEquals(-1l, hc.getRateId(), "default rateId");
    hc.setRateId(42l);
    assertEquals(42l, hc.getRateId(), "correct rateId");
  }
  
  @Test
  public void testCompareTo ()
  {
    HourlyCharge hcLt = new HourlyCharge(now.minusMillis(10000l), 0.33);
    HourlyCharge hcGt = new HourlyCharge(now.plusMillis(10000l), 0.13);
    assertTrue(hc.compareTo(hcLt) > 0, "lt sorts first");
    assertTrue(hc.compareTo(hcGt) < 0, "gt sorts last");
  }

  @Test
  public void xmlSerializationTest ()
  {
    hc.setRateId(37l);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(HourlyCharge.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(hc));
    //System.out.println(serialized.toString());
    HourlyCharge xhc= (HourlyCharge)xstream.fromXML(serialized.toString());
    assertNotNull(xhc, "deserialized something");
    assertEquals(now.toEpochMilli(), xhc.getAtTime().toEpochMilli(), "correct time");
    assertEquals(0.21, xhc.getValue(), 1e-6, "correct amount");
    assertEquals(37l, xhc.getRateId(), "correct rate ID");
  }
}
