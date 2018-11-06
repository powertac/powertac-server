/*
 * Copyright (c) 2016 by John Collins.
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
package org.powertac.common;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class TariffTransactionTests
{
  TestContextManager f;
  Broker b1;
  BrokerRepo brokerRepo;
  TariffSpecification spec;
  CustomerInfo customer;

  @Before
  public void setUp() 
  {
    brokerRepo = BrokerRepo.getInstance();
    b1 = new Broker("bob");
    brokerRepo.add(b1);
    spec = new TariffSpecification(b1, PowerType.CONSUMPTION);
    customer = new CustomerInfo("Podunk", 42);
  }

  @Test
  public void construction1() 
  {
    TariffTransaction ttx =
        new TariffTransaction(b1, 1, TariffTransaction.Type.PUBLISH,
                              spec, null, 0, 0.0, 0.0, false);
    assertNotNull("created", ttx);
    assertEquals("type",TariffTransaction.Type.PUBLISH, ttx.getTxType());
    assertEquals("broker", b1, ttx.getBroker());
    assertFalse("not reg", ttx.isRegulation());
  }

  @Test
  public void xmlSerializationTest ()
  {
    TariffTransaction ttx =
        new TariffTransaction(b1, 2, TariffTransaction.Type.CONSUME,
                              spec, customer, 42, -420.0, 42.0, true);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(TariffTransaction.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ttx));
    //System.out.println(serialized.toString());
    TariffTransaction xttx =
        (TariffTransaction) xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xttx);
    assertEquals("correct type",
                 TariffTransaction.Type.CONSUME, xttx.getTxType());
    assertTrue("regulation", xttx.isRegulation());
    assertEquals("correct value", 42.0, xttx.getCharge(), 1e-6);
  }
}
