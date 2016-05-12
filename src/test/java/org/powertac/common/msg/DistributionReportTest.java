/*
 * Copyright (c) 2016 by the original author
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
package org.powertac.common.msg;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.thoughtworks.xstream.XStream;

/**
 * @author jcollins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class DistributionReportTest
{
  TestContextManager f;

  /**
   * Test method for {@link org.powertac.common.msg.DistributionReport#DistributionReport(int, double, double)}.
   */
  @Test
  public void testDistributionReportIntDoubleDouble ()
  {
    DistributionReport dr = new DistributionReport(1, 2.0, 3.0);
    assertNotNull("created it", dr);
    assertEquals("timeslotIndex", 1, dr.getTimeslotIndex());
    assertEquals("cons", 2.0, dr.getTotalConsumption(), 1e-6);
    assertEquals("prod", 3.0, dr.getTotalProduction(), 1e-6);
  }

  @Test
  public void xmlSerializationTest ()
  {
    DistributionReport dr1 = new DistributionReport(2, 3.3, 2.2);
    XStream xstream = new XStream();
    xstream.processAnnotations(DistributionReport.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(dr1));
    //System.out.println(serialized.toString());
    
    DistributionReport xdr1 =
        (DistributionReport)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xdr1);
    assertEquals("correct timeslot", 2, xdr1.getTimeslotIndex());
    assertEquals("correct consumption", 3.3, xdr1.getTotalConsumption(), 1e-6);
    assertEquals("correct production", 2.2, xdr1.getTotalProduction(), 1e-6);
  }
}
