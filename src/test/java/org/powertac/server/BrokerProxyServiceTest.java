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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/test/resources/development.xml"})
public class BrokerProxyServiceTest {
  @Autowired 
  private BrokerProxy brokerProxy;
  private Broker broker = new Broker("standard_broker", false, false);
  private CustomerInfo message = new CustomerInfo("t1", 33);

  private JmsTemplate template = mock(JmsTemplate.class);

  @Before
  public void setUp() throws Exception {
    ReflectionTestUtils.setField(brokerProxy, "template", template);
    reset(template);
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
    List<CustomerInfo> messageLists = new ArrayList<CustomerInfo>();
    messageLists.add(message);
    messageLists.add(new CustomerInfo("t2", 22));
    
    brokerProxy.sendMessages(broker, messageLists);
    verify(template, times(messageLists.size())).send(any(String.class), any(MessageCreator.class));
  }
}
