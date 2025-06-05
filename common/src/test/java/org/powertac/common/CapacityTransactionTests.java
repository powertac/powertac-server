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
public class CapacityTransactionTests
{
  TestContextManager f;
  Instant baseTime;
  Broker broker;
  BrokerRepo brokerRepo;

  @BeforeEach
  public void setUp () throws Exception
  {
    Competition.setCurrent(Competition.newInstance("Capacity transaction test"));
    baseTime = Competition.currentCompetition().getSimulationBaseTime().plusMillis(TimeService.DAY);
    brokerRepo = BrokerRepo.getInstance();
    broker = new Broker("Sally");
    brokerRepo.add(broker);
  }

  @Test
  public void testCapacityTransaction ()
  {
    CapacityTransaction ct =
        new CapacityTransaction(broker, 24, 22, 120.0, 42.1, 3.22);
    assertNotNull(ct, "not null");
    assertEquals(24, ct.getPostedTimeslotIndex(), "correct time");
    assertEquals(22, ct.getPeakTimeslot(), "correct ts");
    assertEquals(broker, ct.getBroker(), "correct broker");
    assertEquals(42.1, ct.getKWh(), 1e-6, "correct qty");
    assertEquals(3.22, ct.getCharge(), 1e-6, "correct charge");
  }

  @Test
  public void testToString ()
  {
    CapacityTransaction ct =
        new CapacityTransaction(broker, 24, 22, 110.0, 42.1, 3.22);
    String sut = ct.toString();
    assertEquals(String.format("Capacity tx %d-%s-(%d,%.2f,%.2f)-%.2f", 24,"Sally",22, 110.0, 42.1, 3.22), sut, "match");
  }

  @Test
  public void xmlSerializationTest ()
  {
    CapacityTransaction ct =
        new CapacityTransaction(broker, 24, 22, 130.0, 42.1, 3.22);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(CapacityTransaction.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ct));
    //System.out.println(serialized.toString());
    CapacityTransaction xct = (CapacityTransaction)xstream.fromXML(serialized.toString());
    assertNotNull(xct, "deserialized something");
    assertEquals(broker, xct.getBroker(), "correct broker");
    assertEquals(24, xct.getPostedTimeslotIndex(), "correct time");
    assertEquals(22, xct.getPeakTimeslot(), "correct peak ts");
    assertEquals(130.0, xct.getThreshold(), 1e-6, "correct threshold");
    assertEquals(42.1, xct.getKWh(), 1e-6, "correct qty");
    assertEquals(3.22, xct.getCharge(), 1e-6, "correct charge");
  }
}
