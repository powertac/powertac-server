package org.powertac.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;

import java.io.StringWriter;

import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Broker;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.msg.BrokerAuthentication;
import org.powertac.common.msg.PauseRequest;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.test.util.ReflectionTestUtils;

import com.thoughtworks.xstream.XStream;

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
  public void testOnMessageAuth() throws Exception
  {
    Broker broker = new Broker("abc");
    BrokerAuthentication ba = new BrokerAuthentication(broker);
    String xml = baToXml(ba);
    TextMessage message = mock(TextMessage.class);
    when(message.getText()).thenReturn(xml);
    when(converter.fromXML(any(String.class))).thenReturn(ba);
    
    receiver.onMessage(xml);
    verify(brokerProxy).routeMessage(ba);
  }
  
  // this test requires a Spring context, because the BrokerConverter needs
  // to see the BrokerRepo.
//  @Test
//  public void testOnMessagePause() throws Exception
//  {
//    Broker broker = new Broker("Anne");
//    broker.setKey("mykey");
//    BrokerRepo repo = new BrokerRepo();
//    repo.add(broker);
//    ReflectionTestUtils.setField(receiver, "brokerRepo", repo);
//    PauseRequest rq = new PauseRequest(broker);
//    String xml = "mykey" + prToXml(rq);
//    TextMessage message = mock(TextMessage.class);
//    when(message.getText()).thenReturn(xml);
//    when(converter.fromXML(any(String.class))).thenReturn(rq);
//    
//    receiver.onMessage(xml);
//    verify(brokerProxy).routeMessage(rq);
//  }
  
  private String baToXml (BrokerAuthentication ba)
  {
    XStream xstream = new XStream();
    xstream.processAnnotations(BrokerAuthentication.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ba));
    return serialized.toString();
  }
  
  private String prToXml (PauseRequest pr)
  {
    XStream xstream = new XStream();
    xstream.processAnnotations(PauseRequest.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(pr));
    return serialized.toString();
  }
}
