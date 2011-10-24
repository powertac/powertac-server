package org.powertac.server;

import java.util.HashMap;
import java.util.Map;

import javax.jms.MessageListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.stereotype.Service;

@Service
public class JmsManagementService {
  @Autowired 
  private SingleConnectionFactory connectionFactory;
  
  private Map<MessageListener,AbstractMessageListenerContainer> listenerContainerMap = 
      new HashMap<MessageListener,AbstractMessageListenerContainer>();
  
  public void initialize() {
    // TODO
  }
  
  public void createQueue(String queueName) {
    // TODO
  }
  
  public void registerMessageListener(String destinationName, MessageListener listener) {
    DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setDestinationName(destinationName);
    container.setMessageListener(listener);
    container.afterPropertiesSet();
    container.start();
    
    listenerContainerMap.put(listener, container);
  }
}
