/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.repo.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class XMLMessageConverterTests {
  private static final Log log = LogFactory.getLog(XMLMessageConverterTests.class);
  
  private XMLMessageConverter converter;
  
  @Autowired
  private CustomerRepo customerRepo;
  
  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    converter = new XMLMessageConverter();
    converter.afterPropertiesSet();
  }

  @Test
  public void testToXMLAndFromXML() {
    CustomerInfo info = new CustomerInfo("t1", 33);
    assertNotNull("not null", info);

    String xml = converter.toXML(info);
    
    log.error("xml:\n" + xml);
    
    CustomerInfo convertedInfo = (CustomerInfo)converter.fromXML(xml);

    assertEquals("name", "t1", convertedInfo.getName());
    assertEquals("population", 33, convertedInfo.getPopulation());
    assertEquals("customerType", CustomerType.CustomerHousehold, convertedInfo.getCustomerType());
    assertEquals("no power types", 0, convertedInfo.getPowerTypes().size());
    assertFalse("no multicontracting", convertedInfo.isMultiContracting());
    assertFalse("can't negotiate", convertedInfo.isCanNegotiate());
  }

  @Test
  public void testFullCustomerConverter() {
    Competition competition = Competition.newInstance("test");
    competition.addBroker("broker 1");
    
    CustomerInfo c1 = new CustomerInfo("c1", 10);
    competition.addCustomer(c1);
    
    assertNull(customerRepo.findById(c1.getId()));
    
    String xml = converter.toXML(competition);
        
    Competition convertedCompetition = (Competition)converter.fromXML(xml);
    assertNotNull(convertedCompetition);
    
    assertEquals(c1, customerRepo.findById(c1.getId()));
  }
}
