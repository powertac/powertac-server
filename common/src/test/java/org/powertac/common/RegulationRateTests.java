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

import org.junit.Before;
import org.junit.Test;
import com.thoughtworks.xstream.XStream;

public class RegulationRateTests
{

  @Before
  public void setUp() 
  {
  }

  @Test
  public void construction1() 
  {
    RegulationRate rr = new RegulationRate();
    assertEquals("default response",
                 RegulationRate.ResponseTime.MINUTES, rr.getResponse());
    assertEquals("default up-reg pmt",
                 0.0, rr.getUpRegulationPayment(), 1e-6);
    assertEquals("default down-reg pmt",
                 0.0, rr.getDownRegulationPayment(), 1e-6);
  }

  @Test
  public void construction2() 
  {
    RegulationRate rr = new RegulationRate()
      .withUpRegulationPayment(0.05)
      .withDownRegulationPayment(-0.05)
      .withResponse(RegulationRate.ResponseTime.SECONDS);
    assertEquals("response",
                 RegulationRate.ResponseTime.SECONDS, rr.getResponse());
    assertEquals("default up-reg pmt",
                 0.05, rr.getUpRegulationPayment(), 1e-6);
    assertEquals("default down-reg pmt",
                 -0.05, rr.getDownRegulationPayment(), 1e-6);
  }

  @Test
  public void xmlSerializationTest ()
  {
    RegulationRate r = new RegulationRate()
      .withUpRegulationPayment(0.05)
      .withDownRegulationPayment(-0.05)
      .withResponse(RegulationRate.ResponseTime.SECONDS);

    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(RegulationRate.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(r));
    //System.out.println(serialized.toString());
    RegulationRate xr= (RegulationRate)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xr);
    assertEquals("default response",
                 RegulationRate.ResponseTime.SECONDS, xr.getResponse());
    assertEquals("correct value", 0.05, xr.getUpRegulationPayment(), 1e-6);
    assertEquals("correct value", -0.05, xr.getDownRegulationPayment(), 1e-6);
  }
}
