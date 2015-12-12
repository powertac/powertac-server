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
public class CapacityTransactionTests
{
  TestContextManager f;
  Instant baseTime;
  Broker broker;
  BrokerRepo brokerRepo;

  @Before
  public void setUp () throws Exception
  {
    Competition.setCurrent(Competition.newInstance("Capacity transaction test"));
    baseTime = Competition.currentCompetition().getSimulationBaseTime().plus(TimeService.DAY);
    brokerRepo = BrokerRepo.getInstance();
    broker = new Broker("Sally");
    brokerRepo.add(broker);
  }

  @Test
  public void testCapacityTransaction ()
  {
    CapacityTransaction ct = new CapacityTransaction(broker, 24, 120.0, 42.1, 3.22);
    assertNotNull("not null", ct);
    assertEquals("correct time", 24, ct.getPostedTimeslotIndex());
    assertEquals("correct broker", broker, ct.getBroker());
    assertEquals("correct qty", 42.1, ct.getKWh(), 1e-6);
    assertEquals("correct charge", 3.22, ct.getCharge(), 1e-6);
  }

  @Test
  public void testToString ()
  {
    CapacityTransaction ct = new CapacityTransaction(broker, 24, 110.0, 42.1, 3.22);
    String sut = ct.toString();
    assertEquals("match", "Capacity tx 24-Sally-(110.0,42.1)-3.22", sut);
  }

  @Test
  public void xmlSerializationTest ()
  {
    CapacityTransaction ct = new CapacityTransaction(broker, 24, 130.0, 42.1, 3.22);
    XStream xstream = new XStream();
    xstream.processAnnotations(CapacityTransaction.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ct));
    //System.out.println(serialized.toString());
    CapacityTransaction xct = (CapacityTransaction)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xct);
    assertEquals("correct broker", broker, xct.getBroker());
    assertEquals("correct time", 24, xct.getPostedTimeslotIndex());
    assertEquals("correct threshold", 130.0, xct.getThreshold(), 1e-6);
    assertEquals("correct qty", 42.1, xct.getKWh(), 1e-6);
    assertEquals("correct charge", 3.22, xct.getCharge(), 1e-6);
  }
}
