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

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.thoughtworks.xstream.XStream;

/**
 * Tests for DistributionTransaction. We use Spring, because the xml serialization
 * requires that the BrokerConverter be able to find the BrokerRepo.
 * @author John Collins
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class DistributionTransactionTests
{
  TestContextManager f;
  Instant baseTime;
  Broker broker;
  BrokerRepo brokerRepo;

  @BeforeEach
  public void setUp () throws Exception
  {
    Competition.setCurrent(Competition.newInstance("distribution transaction test"));
    baseTime = Competition.currentCompetition().getSimulationBaseTime().plusMillis(TimeService.DAY);
    brokerRepo = BrokerRepo.getInstance();
    broker = new Broker("Sally");
    brokerRepo.add(broker);
  }

  @Test
  public void testDistributionTransactionA ()
  {
    DistributionTransaction dt = new DistributionTransaction(broker, 24, 42.1, 3.22);
    assertNotNull(dt, "not null");
    assertEquals(24, dt.getPostedTimeslotIndex(), "correct time");
    assertEquals(broker, dt.getBroker(), "correct broker");
    assertEquals(42.1, dt.getKWh(), 1e-6, "correct qty");
    assertEquals(3.22, dt.getCharge(), 1e-6, "correct charge");
    assertEquals(0, dt.getNSmall(), "no small");
    assertEquals(0, dt.getNLarge(), "no large");
  }

  @Test
  public void testDistributionTransactionB ()
  {
    DistributionTransaction dt = new DistributionTransaction(broker, 24, 123, 45, 42.1, 3.22);
    assertNotNull(dt, "not null");
    assertEquals(24, dt.getPostedTimeslotIndex(), "correct time");
    assertEquals(broker, dt.getBroker(), "correct broker");
    assertEquals(42.1, dt.getKWh(), 1e-6, "correct qty");
    assertEquals(3.22, dt.getCharge(), 1e-6, "correct charge");
    assertEquals(123, dt.getNSmall(), "correct small");
    assertEquals(45, dt.getNLarge(), "correct large");
  }

  @Test
  public void testToString ()
  {
    DistributionTransaction dt = new DistributionTransaction(broker, 24, 123, 45, 42.1, 3.22);
    String sut = dt.toString();
    //System.out.println(sut);
    assertTrue(sut.matches("Distribution tx 24-Sally-123-45-42.100-3.220"), "match");
  }

  @Test
  public void xmlSerializationTest ()
  {
    DistributionTransaction dt = new DistributionTransaction(broker, 24, 123, 45, 42.1, 3.22);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(DistributionTransaction.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(dt));
    //System.out.println(serialized.toString());
    DistributionTransaction xdt = (DistributionTransaction)xstream.fromXML(serialized.toString());
    assertNotNull(xdt, "deserialized something");
    assertEquals(broker, xdt.getBroker(), "correct broker");
    assertEquals(24, xdt.getPostedTimeslotIndex(), "correct time");
    assertEquals(123, xdt.getNSmall(), "correct small");
    assertEquals(45, xdt.getNLarge(), "correct large");
    assertEquals(42.1, xdt.getKWh(), 1e-6, "correct qty");
    assertEquals(3.22, xdt.getCharge(), 1e-6, "correct charge");
  }
}
