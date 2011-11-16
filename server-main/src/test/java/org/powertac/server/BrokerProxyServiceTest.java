package org.powertac.server;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Broker;
import org.powertac.common.CustomerInfo;
import org.powertac.common.interfaces.BrokerProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/test/resources/development.xml"})
@DirtiesContext
public class BrokerProxyServiceTest 
{
  @Autowired 
  private BrokerProxy brokerProxy;
  
  private MessageRouter router;

  private TestBroker stdBroker; // no messages if not enabled
  private TestBroker wholesaleBroker; // can always send and receive messages
  private TestBroker localBroker; 
  private CustomerInfo message;

  private JmsTemplate template;

  @Before
  public void setUp() throws Exception 
  {
    stdBroker = new TestBroker("standard_broker", false, false);
    wholesaleBroker = new TestBroker("wholesaler", true, true);
    localBroker = new TestBroker("local", true, false);
    message = new CustomerInfo("t1", 33);
    template = mock(JmsTemplate.class);
    ReflectionTestUtils.setField(brokerProxy, "template", template);
    router = mock(MessageRouter.class);
    ReflectionTestUtils.setField(brokerProxy, "router", router);
  }

  @After
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
    assertEquals("no messages for non-enabled broker", 0,
                 localBroker.messages.size());
    localBroker.setEnabled(true);
    brokerProxy.sendMessage(localBroker, message);
    assertEquals("one message for enabled broker", 1,
                 localBroker.messages.size());
    assertEquals("correct message arrived", message,
                 localBroker.messages.get(0));
  }
  
  @Test
  public void wholesaleBrokerSingleMessage ()
  {
    brokerProxy.sendMessage(wholesaleBroker, message);
    assertEquals("one message for wholesale broker", 1,
                 wholesaleBroker.messages.size());
    assertEquals("correct message arrived", message,
                 wholesaleBroker.messages.get(0));
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
    brokerProxy.routeMessage(localBroker, message);
    verify(router, times(0)).route(message);
    localBroker.setEnabled(true);
    brokerProxy.routeMessage(localBroker, message);
    verify(router, times(1)).route(message);
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
