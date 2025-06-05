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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class XMLMessageConverterTests 
{
  private static final Log log = LogFactory.getLog(XMLMessageConverterTests.class);
  
  private XMLMessageConverter converter;
  
  @Autowired
  private CustomerRepo customerRepo;
  
  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  public void setUp() throws Exception {
    converter = new XMLMessageConverter();
    converter.afterPropertiesSet();
  }

  @Test
  public void testToXMLAndFromXML() {
    CustomerInfo info = new CustomerInfo("t1", 33);
    assertNotNull(info, "not null");

    String xml = converter.toXML(info);
    
    log.error("xml:\n" + xml);
    
    CustomerInfo convertedInfo = (CustomerInfo)converter.fromXML(xml);

    assertEquals("t1", convertedInfo.getName(), "name");
    assertEquals(33, convertedInfo.getPopulation(), "population");
    assertEquals(PowerType.CONSUMPTION, convertedInfo.getPowerType());
    assertFalse(convertedInfo.isMultiContracting(), "no multicontracting");
    assertFalse(convertedInfo.isCanNegotiate(), "can't negotiate");
  }

  @Test
  public void testFullCustomerConverter() {
    Competition competition = Competition.newInstance("test");
    competition.addBroker("broker 1");
    
    CustomerInfo c1 = new CustomerInfo("c1", 10);
    competition.addCustomer(c1);
    
    assertNull(customerRepo.findById(c1.getId()));
    
    String xml = converter.toXML(competition);
    //System.out.println(xml);
    Competition convertedCompetition = (Competition)converter.fromXML(xml);
    assertNotNull(convertedCompetition);
    assertNotNull(convertedCompetition.getCustomers(), "has customers");
    assertEquals(1, convertedCompetition.getCustomers().size(), "one customer");
    CustomerInfo xc1 = convertedCompetition.getCustomers().get(0);
    assertEquals(c1.getName(), xc1.getName(), "correct name");
    // See issue #449
    //assertEquals(xc1, customerRepo.findById(c1.getId()), "customer in repo");
  }
}
