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

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

/**
 * Tests for BalancingTransaction. We use Spring, because the xml serialization
 * requires that the BrokerConverter be able to find the BrokerRepo.
 * @author jcollins
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/test-config.xml"})
public class BalancingTransactionTests
{
  Instant baseTime;
  Broker broker;
  BrokerRepo brokerRepo;

  @BeforeClass
  public static void setUpLog () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    baseTime = new DateTime().toInstant();
    brokerRepo = new BrokerRepo();
    broker = new Broker("Sally");
    brokerRepo.add(broker);
  }

  @Test
  public void testBalancingTransaction ()
  {
    BalancingTransaction bt = new BalancingTransaction(broker, baseTime, 42.1, 3.22);
    assertNotNull("not null", bt);
    assertEquals("correct time", baseTime, bt.getPostedTime());
    assertEquals("correct broker", broker, bt.getBroker());
    assertEquals("correct qty", 42.1, bt.getQuantity(), 1e-6);
    assertEquals("correct charge", 3.22, bt.getCharge(), 1e-6);
  }

  @Test
  public void testToString ()
  {
    BalancingTransaction bt = new BalancingTransaction(broker, baseTime, 42.1, 3.22);
    String sut = bt.toString();
    //System.out.println(sut);
    assertTrue("match", sut.matches("Balance tx \\d+-Sally-42.1-3.22"));
  }
  
  @Test
  public void xmlSerializationTest ()
  {
    BalancingTransaction bt = new BalancingTransaction(broker, baseTime, 42.1, 3.22);
    XStream xstream = new XStream();
    xstream.processAnnotations(BalancingTransaction.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(bt));
    //System.out.println(serialized.toString());
    BalancingTransaction xbt = (BalancingTransaction)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xbt);
    assertEquals("correct time", baseTime.getMillis(), xbt.getPostedTime().getMillis());
    assertEquals("correct qty", 42.1, xbt.getQuantity(), 1e-6);
    assertEquals("correct charge", 3.22, xbt.getCharge(), 1e-6);
  }
}
