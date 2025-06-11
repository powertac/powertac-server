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

import java.io.StringWriter;

import org.apache.commons.beanutils.PropertyUtils;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.thoughtworks.xstream.XStream;

/**
 * Tests for BalancingTransaction. We use Spring, because the xml serialization
 * requires that the BrokerConverter be able to find the BrokerRepo.
 * @author John Collins
 *
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class BalancingTransactionTests
{
  Instant baseTime;
  Broker broker;
  BrokerRepo brokerRepo;

  @BeforeEach
  public void setUp () throws Exception
  {
    Competition.setCurrent(Competition.newInstance("market order test"));
    baseTime = Competition.currentCompetition().getSimulationBaseTime().plusMillis(TimeService.DAY);
    brokerRepo = BrokerRepo.getInstance();
    broker = new Broker("Sally");
    brokerRepo.add(broker);
  }

  @Test
  public void testBalancingTransaction ()
  {
    BalancingTransaction bt = new BalancingTransaction(broker, 24, 42.1, 3.22);
    assertNotNull(bt, "not null");
    assertEquals(24, bt.getPostedTimeslotIndex(), "correct time");
    assertEquals(broker, bt.getBroker(), "correct broker");
    assertEquals(42.1, bt.getKWh(), 1e-6, "correct qty");
    assertEquals(3.22, bt.getCharge(), 1e-6, "correct charge");
  }

  @Test
  public void testToString ()
  {
    BalancingTransaction bt = new BalancingTransaction(broker, 24, 42.1, 3.22);
    String sut = bt.toString();
    //System.out.println(sut);
    assertTrue(sut.matches("Balance tx \\d+-Sally-42.1-3.22"), "match");
  }

  @Test
  public void testPropertyNames ()
  {
    BalancingTransaction bt = new BalancingTransaction(broker, 24, 42.1, 3.22);
    try {
      Object obj = PropertyUtils.getSimpleProperty(bt, "charge");
      assertEquals(Double.class, obj.getClass(), "it's a Double");
      assertEquals(3.22, ((Double)obj).doubleValue(), "correct value");
      obj = PropertyUtils.getSimpleProperty(bt, "KWh");
      assertEquals(42.1, ((Double)obj).doubleValue(), "correct kWh");
    }
    catch (Exception ex) {
      fail("Exception " + ex.toString());
    }
  }
  
  @Test
  public void xmlSerializationTest ()
  {
    BalancingTransaction bt = new BalancingTransaction(broker, 24, 42.1, 3.22);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(BalancingTransaction.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(bt));
    //System.out.println(serialized.toString());
    BalancingTransaction xbt = (BalancingTransaction)xstream.fromXML(serialized.toString());
    assertNotNull(xbt, "deserialized something");
    assertEquals(broker, xbt.getBroker(), "correct broker");
    assertEquals(24, xbt.getPostedTimeslotIndex(), "correct time");
    assertEquals(42.1, xbt.getKWh(), 1e-6, "correct qty");
    assertEquals(3.22, xbt.getCharge(), 1e-6, "correct charge");
  }
}
