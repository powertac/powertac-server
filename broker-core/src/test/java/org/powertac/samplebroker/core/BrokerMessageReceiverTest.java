/*
 * Copyright (c) 2016 by the original author
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
package org.powertac.samplebroker.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.XMLMessageConverter;
import org.powertac.samplebroker.interfaces.IpcAdapter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

/**
 * @author jcollins
 */
public class BrokerMessageReceiverTest
{
  BrokerMessageReceiver uut;
  MessageDispatcher md;
  XMLMessageConverter xmc;

  List<String> rawTypes = Arrays.asList("cash", "market-tx");
  List<String> cookedTypes = Arrays.asList("cash", "sim-pause");

  ArrayList<String> exportedMessages;
  ArrayList<Object> dispatchedMessages;

  @Before
  public void setUp () throws Exception
  {
    exportedMessages = new ArrayList<>();
    dispatchedMessages = new ArrayList<>();
    uut = new BrokerMessageReceiver();
    ReflectionTestUtils.setField(uut, "adapter", new Adapter());

    md = mock(MessageDispatcher.class);
    ReflectionTestUtils.setField(uut, "messageDispatcher", md);

    xmc = mock(XMLMessageConverter.class);
    ReflectionTestUtils.setField(uut, "converter", xmc);
    when(xmc.fromXML(anyString())).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        return("converted-" + (String)args[0]);
      }
    });

    BrokerPropertiesService bps = mock(BrokerPropertiesService.class);
    ReflectionTestUtils.setField(uut, "propertiesService", bps);
    ReflectionTestUtils.setField(uut, "rawMsgTypes", rawTypes);
    ReflectionTestUtils.setField(uut, "cookedMsgTypes", cookedTypes);
  }

  private TextMessage createTextMessage (String contents)
  {
    TextMessage result = mock(TextMessage.class);
    try {
      doAnswer(new Answer<Object>() {
        @Override
        public Object answer(InvocationOnMock invocation) {
          return contents;
        }
      }).when(result).getText();
    }
    catch (JMSException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return result;
  }

  @Test
  public void testRawTxtMsg ()
  {
    ReflectionTestUtils.setField(uut, "rawXml", true);
    uut.initialize();
    String msg = "<market-tx value=\"42\"/>";
    TextMessage tmsg = createTextMessage(msg);
    assertEquals("no exported messages", 0, exportedMessages.size());
    uut.onMessage(tmsg);
    assertEquals("one exported msg", 1, exportedMessages.size());
    assertEquals("correct msg", msg, exportedMessages.get(0));
    //verifyZeroInteractions(xmc);
  }

  @Test
  public void testCookedMsg ()
  {
    ReflectionTestUtils.setField(uut, "rawXml", true);
    uut.initialize();
    String msg = "<sim-pause value=\"42\"/>";
    TextMessage tmsg = createTextMessage(msg);
    assertEquals("no exported messages", 0, exportedMessages.size());
    uut.onMessage(tmsg);
    assertEquals("no exported messages", 0, exportedMessages.size());
    //verify(md).routeMessage("converted-<sim-pause value=\"42\"/>");
  }

  @Test
  public void testRawCookedMsg ()
  {
    ReflectionTestUtils.setField(uut, "rawXml", true);
    uut.initialize();
    String msg = "<cash value=\"42\"/>";
    TextMessage tmsg = createTextMessage(msg);
    assertEquals("no exported messages", 0, exportedMessages.size());
    uut.onMessage(tmsg);
    assertEquals("one exported msg", 1, exportedMessages.size());
    assertEquals("correct msg", msg, exportedMessages.get(0));
    //verify(md).routeMessage("converted-<cash value=\"42\"/>");
  }

  // Try without rawXml
  @Test
  public void testNormalMsg ()
  {
    ReflectionTestUtils.setField(uut, "rawXml", false);
    uut.initialize();
    String msg = "<market-tx value=\"42\"/>";
    TextMessage tmsg = createTextMessage(msg);
    assertEquals("no exported messages", 0, exportedMessages.size());
    uut.onMessage(tmsg);
    assertEquals("no exported messages", 0, exportedMessages.size());
    verify(md).routeMessage("converted-<market-tx value=\"42\"/>");
    
  }

  class Adapter implements IpcAdapter
  {

    @Override
    public void exportMessage (String message)
    {
      exportedMessages.add(message);
    }

    @Override
    public void startMessageImport ()
    {
      // Nothing needed here
    }
  }
}
