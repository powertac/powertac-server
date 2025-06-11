/*
 * Copyright (c) 2011 by the original author
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

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.CustomerInfo;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.thoughtworks.xstream.XStream;

/**
 * Tests for CustomerBootstrapData. Note that this needs Spring dependency
 * injection, because the xml deserialization process requires autowiring.
 * @author John Collins
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class CustomerBootstrapDataTests
{
  @Autowired
  private CustomerRepo customerRepo;
  
  private CustomerInfo customer;

  private double[] data;

  @BeforeEach
  public void setUp () throws Exception
  {
    customer = customerRepo.createCustomerInfo("Population", 42);
    data = new double[] {1.3,1.4,1.5,1.6,1.7,1.8,1.9,2.0,2.1,2.2,2.3,2.4,
                         2.5,2.6,2.7,2.8,2.9,3.0,3.1,3.2,3.3,3.4,3.5,3.6};
  }

  @Test
  public void testCustomerBootstrapData ()
  {
    CustomerBootstrapData cbd = new CustomerBootstrapData(customer, PowerType.CONSUMPTION, data);
    assertNotNull(cbd, "object created");
    assertEquals(customer.getName(), cbd.getCustomerName(), "correct customer name");
    assertEquals(24, cbd.getNetUsage().length, "correct array size");
    assertEquals(1.4, cbd.getNetUsage()[1], 1e-6, "correct second element");
  }

  @Test
  public void xmlSerializationTest ()
  {
    CustomerBootstrapData cbd = new CustomerBootstrapData(customer, PowerType.CONSUMPTION, data);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(CustomerBootstrapData.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(cbd));
    //System.out.println(serialized.toString());
    CustomerBootstrapData xcbd = (CustomerBootstrapData)xstream.fromXML(serialized.toString());
    assertNotNull(xcbd, "deserialized something");
    assertEquals(cbd.getId(), xcbd.getId(), "correct id");
    assertEquals(1.7, xcbd.getNetUsage()[4], 1e-6, "correct 5th element");
  }
}
