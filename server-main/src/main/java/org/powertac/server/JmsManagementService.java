package org.powertac.server;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.stereotype.Service;

@Service
public class JmsManagementService {
  static private Logger log = Logger.getLogger(JmsManagementService.class.getName());

  @Autowired 
  private ConnectionFactory connectionFactory;
  
  private Map<MessageListener,AbstractMessageListenerContainer> listenerContainerMap = 
      new HashMap<MessageListener,AbstractMessageListenerContainer>();
  
  public void initializeServerQueue(String serverQueueName) {
    // create server queue
    createQueue(serverQueueName);
  }
  
  public void initializeBrokersQueues(String[] queueNames) {
    // create broker queues
    for (String queueName : queueNames) {
      createQueue(queueName);
    }
  }
  
  public Queue createQueue(String queueName) {
    Queue queue = null;
    try {
      Connection connection = connectionFactory.createConnection();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      queue = session.createQueue(queueName);
    } catch (JMSException e) {
      log.error("Failed to create queue " + queueName, e);
    }

    return queue;
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
