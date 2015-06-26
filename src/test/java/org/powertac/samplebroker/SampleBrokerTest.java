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
package org.powertac.samplebroker;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.IdGenerator;
import org.powertac.common.msg.BrokerAccept;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.samplebroker.core.MessageDispatcher;
import org.powertac.samplebroker.core.PowerTacBroker;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test cases for the sample broker implementation.
 * 
 * @author John Collins
 */
public class SampleBrokerTest
{
  private Instant baseTime;

  private PowerTacBroker broker;
  private CustomerRepo customerRepo;
  private BrokerRepo brokerRepo;

  @Before
  public void setUp () throws Exception
  {
    // set the time
    baseTime = new DateTime(2011, 2, 1, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();

    // initialize the broker under test
    broker = new PowerTacBroker();

    // set up the autowired dependencies
    ApplicationContext ctx = mock(ApplicationContext.class);
    SpringApplicationContext sac = new SpringApplicationContext();
    sac.setApplicationContext(ctx);
    MessageDispatcher messageDispatcher = new MessageDispatcher();
    ReflectionTestUtils.setField(broker, "router", messageDispatcher);
    customerRepo = new CustomerRepo();
    ReflectionTestUtils.setField(broker, "customerRepo", customerRepo);
    brokerRepo = new BrokerRepo();
    ReflectionTestUtils.setField(broker, "brokerRepo", brokerRepo);
    ReflectionTestUtils.setField(broker, "username", "Sample");

    broker.init();
  }
  
  /**
   * Test method for {@link org.powertac.samplebroker.core.PowerTacBroker#SampleBroker(java.lang.String, org.powertac.samplebroker.SampleBrokerService)}.
   */
  @Test
  public void testSampleBroker ()
  {
    assertFalse(broker.getBroker().isEnabled());
  }

  /**
   * Test method for {@link org.powertac.samplebroker.core.PowerTacBroker#isEnabled()}.
   */
  @Test
  public void testIsEnabled ()
  {
    assertFalse(broker.getBroker().isEnabled());
    broker.getBroker().receiveMessage(new BrokerAccept(3));
    assertTrue(broker.getBroker().isEnabled());
    assertEquals("correct prefix", 3, IdGenerator.getPrefix());
  }

  /**
   * Test method for {@link org.powertac.samplebroker.core.PowerTacBroker#receiveMessage(java.lang.Object)}.
   */
  @Test
  public void testReceiveCompetition ()
  {
    assertEquals("initially, no competing brokers", 1, broker.getBrokerList().size());
    // set up a competition
    Competition comp = Competition.newInstance("Test")
        .withSimulationBaseTime(baseTime)
        .addBroker("Sam")
        .addBroker("Sally")
        .addCustomer(new CustomerInfo("Podunk", 3))
        .addCustomer(new CustomerInfo("Midvale", 1000))
        .addCustomer(new CustomerInfo("Metro", 100000));
    // send without first enabling
    broker.getBroker().receiveMessage(comp);
    assertEquals("still no competing brokers", 1, broker.getBrokerList().size());
    // enable the broker
    broker.getBroker().receiveMessage(new BrokerAccept(3));
    // send to broker and check
    broker.getBroker().receiveMessage(comp);
    assertEquals("2 competing brokers", 3, broker.getBrokerList().size());
    assertEquals("3 customers", 3, customerRepo.size());
  }
}
