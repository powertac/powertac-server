package org.powertac.server;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
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

  private Broker broker;
  private CustomerInfo message;

  private JmsTemplate template;

  @Before
  public void setUp() throws Exception {
    broker = new Broker("standard_broker", false, false);
    message = new CustomerInfo("t1", 33);
    template = mock(JmsTemplate.class);
    ReflectionTestUtils.setField(brokerProxy, "template", template);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testSendMessage_single() {
    brokerProxy.sendMessage(broker, message);
    
    verify(template, times(1)).send(any(String.class), any(MessageCreator.class));
  }

  @Test
  public void testSendMessage_multiple() {
    List<CustomerInfo> messageList = new ArrayList<CustomerInfo>();
    messageList.add(message);
    messageList.add(new CustomerInfo("t2", 22));
    
    brokerProxy.sendMessages(broker, messageList);
    verify(template, times(messageList.size())).send(any(String.class), any(MessageCreator.class));
  }
}
