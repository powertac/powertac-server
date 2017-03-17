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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.thoughtworks.xstream.XStream;

/**
 * Tests for DistributionTransaction. We use Spring, because the xml serialization
 * requires that the BrokerConverter be able to find the BrokerRepo.
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
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

  @Before
  public void setUp () throws Exception
  {
    Competition.setCurrent(Competition.newInstance("distribution transaction test"));
    baseTime = Competition.currentCompetition().getSimulationBaseTime().plus(TimeService.DAY);
    brokerRepo = BrokerRepo.getInstance();
    broker = new Broker("Sally");
    brokerRepo.add(broker);
  }

  @Test
  public void testDistributionTransactionA ()
  {
    DistributionTransaction dt = new DistributionTransaction(broker, 24, 42.1, 3.22);
    assertNotNull("not null", dt);
    assertEquals("correct time", 24, dt.getPostedTimeslotIndex());
    assertEquals("correct broker", broker, dt.getBroker());
    assertEquals("correct qty", 42.1, dt.getKWh(), 1e-6);
    assertEquals("correct charge", 3.22, dt.getCharge(), 1e-6);
    assertEquals("no small", 0, dt.getNSmall());
    assertEquals("no large", 0, dt.getNLarge());
  }

  @Test
  public void testDistributionTransactionB ()
  {
    DistributionTransaction dt = new DistributionTransaction(broker, 24, 123, 45, 42.1, 3.22);
    assertNotNull("not null", dt);
    assertEquals("correct time", 24, dt.getPostedTimeslotIndex());
    assertEquals("correct broker", broker, dt.getBroker());
    assertEquals("correct qty", 42.1, dt.getKWh(), 1e-6);
    assertEquals("correct charge", 3.22, dt.getCharge(), 1e-6);
    assertEquals("correct small", 123, dt.getNSmall());
    assertEquals("correct large", 45, dt.getNLarge());
  }

  @Test
  public void testToString ()
  {
    DistributionTransaction dt = new DistributionTransaction(broker, 24, 123, 45, 42.1, 3.22);
    String sut = dt.toString();
    //System.out.println(sut);
    assertTrue("match", sut.matches("Distribution tx 24-Sally-123-45-42.100-3.220"));
  }

  @Test
  public void xmlSerializationTest ()
  {
    DistributionTransaction dt = new DistributionTransaction(broker, 24, 123, 45, 42.1, 3.22);
    XStream xstream = new XStream();
    xstream.processAnnotations(DistributionTransaction.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(dt));
    //System.out.println(serialized.toString());
    DistributionTransaction xdt = (DistributionTransaction)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xdt);
    assertEquals("correct broker", broker, xdt.getBroker());
    assertEquals("correct time", 24, xdt.getPostedTimeslotIndex());
    assertEquals("correct small", 123, xdt.getNSmall());
    assertEquals("correct large", 45, xdt.getNLarge());
    assertEquals("correct qty", 42.1, xdt.getKWh(), 1e-6);
    assertEquals("correct charge", 3.22, xdt.getCharge(), 1e-6);
  }
}
