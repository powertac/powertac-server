/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.common.msg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Broker;

import com.thoughtworks.xstream.XStream;
import org.powertac.common.XMLMessageConverter;

/**
 * Tests for the TimeslotUpdate message type.
 * @author John Collins
 */
public class BrokerAuthenticationTests
{
  Broker broker;
  
  @Before
  public void setUp () throws Exception
  {
    broker = new Broker("Maria");
  }

  @Test
  public void testCreate1 ()
  {
    BrokerAuthentication ba = new BrokerAuthentication(broker);
    assertNotNull("message not null", ba);
    assertEquals("correct username", broker.getUsername(), ba.getUsername());
    assertNull("null password", ba.getPassword());
  }

  @Test
  public void testCreate2 ()
  {
    BrokerAuthentication ba = new BrokerAuthentication("Jerry", "runspotrun");
    assertNotNull("message not null", ba);
    assertEquals("correct username", "Jerry", ba.getUsername());
    assertEquals("correct password", "runspotrun", ba.getPassword());
  }

  @Test
  public void xmlSerializationTest ()
  {
    BrokerAuthentication ba = new BrokerAuthentication("Sally", "silly-password");
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(BrokerAuthentication.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ba));
    //System.out.println(serialized.toString());
    BrokerAuthentication xba = (BrokerAuthentication)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xba);
    assertEquals("correct username", "Sally", xba.getUsername());
    assertEquals("correct password", "silly-password", xba.getPassword());
  }
}
