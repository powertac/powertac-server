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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.thoughtworks.xstream.XStream;

public class RegulationRateTests
{

  @BeforeEach
  public void setUp() 
  {
  }

  @Test
  public void construction1() 
  {
    RegulationRate rr = new RegulationRate();
    assertEquals(RegulationRate.ResponseTime.MINUTES, rr.getResponse(), "default response");
    assertEquals(0.0, rr.getUpRegulationPayment(), 1e-6, "default up-reg pmt");
    assertEquals(0.0, rr.getDownRegulationPayment(), 1e-6, "default down-reg pmt");
  }

  @Test
  public void construction2() 
  {
    RegulationRate rr = new RegulationRate()
      .withUpRegulationPayment(0.05)
      .withDownRegulationPayment(-0.05)
      .withResponse(RegulationRate.ResponseTime.SECONDS);
    assertEquals(RegulationRate.ResponseTime.SECONDS, rr.getResponse(), "response");
    assertEquals(0.05, rr.getUpRegulationPayment(), 1e-6, "default up-reg pmt");
    assertEquals(-0.05, rr.getDownRegulationPayment(), 1e-6, "default down-reg pmt");
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
    assertNotNull(xr, "deserialized something");
    assertEquals(RegulationRate.ResponseTime.SECONDS, xr.getResponse(), "default response");
    assertEquals(0.05, xr.getUpRegulationPayment(), 1e-6, "correct value");
    assertEquals(-0.05, xr.getDownRegulationPayment(), 1e-6, "correct value");
  }
}
