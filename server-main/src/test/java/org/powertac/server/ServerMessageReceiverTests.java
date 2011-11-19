package org.powertac.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;

import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Broker;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.msg.BrokerAuthentication;
import org.springframework.test.util.ReflectionTestUtils;

public class ServerMessageReceiverTests
{
  ServerMessageReceiver receiver;
  BrokerProxy brokerProxy;
  XMLMessageConverter converter;
  
  @Before
  public void before() {
    receiver = new ServerMessageReceiver();
    
    brokerProxy = mock(BrokerProxy.class);
    converter = mock(XMLMessageConverter.class);
    
    ReflectionTestUtils.setField(receiver, "brokerProxy", brokerProxy);
    ReflectionTestUtils.setField(receiver, "converter", converter);
  }

  @Test
  public void testOnMessage() throws Exception
  {
    BrokerAuthentication ba = new BrokerAuthentication();
    Broker broker = new Broker("abc");
    ba.setBroker(broker);
    String xml = converter.toXML(ba);
    TextMessage message = mock(TextMessage.class);
    
    when(message.getText()).thenReturn(xml);
    when(converter.fromXML(any(String.class))).thenReturn(ba);
    
    receiver.onMessage(message);
    verify(brokerProxy).routeMessage(broker, ba);
  }

}
