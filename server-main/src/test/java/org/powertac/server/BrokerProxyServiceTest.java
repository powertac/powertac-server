package org.powertac.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Broker;
import org.powertac.common.CustomerInfo;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.VisualizerProxy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.test.util.ReflectionTestUtils;

public class BrokerProxyServiceTest 
{
  private BrokerProxy brokerProxy;
  
  private MessageRouter router;

  private TestBroker stdBroker; // no messages if not enabled
  private TestBroker wholesaleBroker; // can always send and receive messages
  private TestBroker localBroker; 
  private CustomerInfo message;
  
  private VisualizerProxy visualizer;
  private JmsTemplate template;
  private XMLMessageConverter converter;

  @BeforeEach
  public void setUp() throws Exception 
  {
    brokerProxy = new BrokerProxyService();
        
    stdBroker = new TestBroker("standard_broker", false, false);
    wholesaleBroker = new TestBroker("wholesaler", true, true);
    localBroker = new TestBroker("local", true, false);
    message = new CustomerInfo("t1", 33);
    
    template = mock(JmsTemplate.class);
    ReflectionTestUtils.setField(brokerProxy, "template", template);
    router = mock(MessageRouter.class);
    ReflectionTestUtils.setField(brokerProxy, "router", router);
    visualizer = mock(VisualizerProxy.class);
    ReflectionTestUtils.setField(brokerProxy, "visualizerProxyService", visualizer);    
    converter = mock(XMLMessageConverter.class);
    ReflectionTestUtils.setField(brokerProxy, "converter", converter);     
  }

  @AfterEach
  public void tearDown() throws Exception 
  {
  }

  @Test
  public void testSendMessage_single() {
    brokerProxy.sendMessage(stdBroker, message);
    verify(template, times(0)).send(any(String.class),
                                    any(MessageCreator.class));
    stdBroker.setEnabled(true);
    brokerProxy.sendMessage(stdBroker, message);
    verify(template, times(1)).send(any(String.class),
                                    any(MessageCreator.class));
  }
  
  @Test
  public void localBrokerSingleMessage ()
  {
    brokerProxy.sendMessage(localBroker, message);
    assertEquals(0, localBroker.messages.size(), "no messages for non-enabled broker");
    localBroker.setEnabled(true);
    brokerProxy.sendMessage(localBroker, message);
    assertEquals(1, localBroker.messages.size(), "one message for enabled broker");
    assertEquals(message, localBroker.messages.get(0), "correct message arrived");
  }
  
  @Test
  public void wholesaleBrokerSingleMessage ()
  {
    brokerProxy.sendMessage(wholesaleBroker, message);
    assertEquals(1, wholesaleBroker.messages.size(), "one message for wholesale broker");
    assertEquals(message, wholesaleBroker.messages.get(0), "correct message arrived");
  }

  @Test
  public void testSendMessage_multiple() 
  {
    List<Object> messageList = new ArrayList<Object>();
    messageList.add(message);
    messageList.add(new CustomerInfo("t2", 22));
    messageList.add(new CustomerInfo("t3", 23));
    brokerProxy.sendMessages(stdBroker, messageList);
    verify(template, times(0)).send(any(String.class),
                                    any(MessageCreator.class));
    stdBroker.setEnabled(true);
    brokerProxy.sendMessages(stdBroker, messageList);
    verify(template, times(messageList.size())).send(any(String.class),
                                                     any(MessageCreator.class));
  }
  
  @Test
  public void routeMessageTest()
  {
    when(router.route(message)).thenReturn(false);
    brokerProxy.routeMessage(message);
    verify(router, times(1)).route(message);
    verify(visualizer, times(0)).forwardMessage(message);
    
    reset(router);
    reset(visualizer);
    when(router.route(message)).thenReturn(true);    
    
    brokerProxy.routeMessage(message);
    verify(router, times(1)).route(message);
    verify(visualizer, times(1)).forwardMessage(message);
  }
  
  // Broker that collects the messages it receives
  class TestBroker extends Broker
  {
    ArrayList<Object> messages = new ArrayList<Object>();
    
    public TestBroker (String username, boolean local, boolean wholesale)
    {
      super(username, local, wholesale);
    }
    
    public void receiveMessage(Object message)
    {
      messages.add(message);
    }
  }
}
