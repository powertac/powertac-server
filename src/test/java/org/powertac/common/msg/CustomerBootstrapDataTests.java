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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringWriter;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.CustomerInfo;
import org.powertac.common.repo.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

/**
 * Tests for CustomerBootstrapData. Note that this needs Spring dependency
 * injection, because the xml deserialization process requires autowiring.
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/test/resources/test-config.xml"})
@DirtiesContext
public class CustomerBootstrapDataTests
{
  @Autowired
  private CustomerRepo customerRepo;
  
  private CustomerInfo customer;

  private double[] data;
  
  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("src/test/resources/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    customer = customerRepo.createCustomerInfo("Population", 42);
    data = new double[] {1.3,1.4,1.5,1.6,1.7,1.8,1.9,2.0,2.1,2.2,2.3,2.4,
                         2.5,2.6,2.7,2.8,2.9,3.0,3.1,3.2,3.3,3.4,3.5,3.6};
  }

  @Test
  public void testCustomerBootstrapData ()
  {
    CustomerBootstrapData cbd = new CustomerBootstrapData(customer, data);
    assertNotNull("object created", cbd);
    assertEquals("correct customer name", customer.getName(), cbd.getCustomerName());
    assertEquals("correct array size", 24, cbd.getNetUsage().length);
    assertEquals("correct second element", 1.4, cbd.getNetUsage()[1], 1e-6);
  }

  @Test
  public void xmlSerializationTest ()
  {
    CustomerBootstrapData cbd = new CustomerBootstrapData(customer, data);
    XStream xstream = new XStream();
    xstream.processAnnotations(CustomerBootstrapData.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(cbd));
    //System.out.println(serialized.toString());
    CustomerBootstrapData xcbd = 
      (CustomerBootstrapData)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xcbd);
    assertEquals("correct id", cbd.getId(), xcbd.getId());
    assertEquals("correct 5th element", 1.7, xcbd.getNetUsage()[4], 1e-6);
  }

}
