package org.powertac.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerRegistry;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.log4j.Logger;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.ServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.stereotype.Service;

@Service
public class JmsManagementService
{
  static private Logger log = Logger.getLogger(JmsManagementService.class);

  @Resource(name = "jmsFactory")
  private ConnectionFactory connectionFactory;

  @Autowired
  private Executor taskExecutor;

  @Autowired
  private ServerConfiguration serverPropertiesService;

  private Map<MessageListener, AbstractMessageListenerContainer> listenerContainerMap = new HashMap<MessageListener, AbstractMessageListenerContainer>();

  // configurable parameters
  private boolean servingJms = true;
  private String jmsBrokerUrl = "tcp://localhost:61616";
  private String jmsBrokerName = "simJmsProvider";

  public void initializeServerQueue (String serverQueueName)
  {
    // create server queue
    createQueue(serverQueueName);
  }

  public void start ()
  {
    // pull down configuration - this will later change when configuration
    // available from web-app
    serverPropertiesService.configureMe(this);

    if (isServingJms()) {
      startProvider();
    }

    initializeClientInterface();
  }

  public void startProvider ()
  {
    BrokerService brokerService = BrokerRegistry.getInstance()
            .lookup(getJmsBrokerName());
    if (brokerService != null) {
      log.info("JMS Server is already started.");
      return;
    }

    brokerService = new BrokerService();
    try {
      brokerService.setBrokerName(getJmsBrokerName());
      brokerService.addConnector(getJmsBrokerUrl());
      brokerService.start();
      brokerService.waitUntilStarted();
    }
    catch (Exception e) {
      log.error("Failed to start JMS Server", e);
    }
  }

  public void stop ()
  {
    unregisterAllMessageListeners();
    if (isServingJms()) {
      try {
        // let's wait a few seconds before shutting down
        Thread.sleep(3000);
      }
      catch (InterruptedException e) {
        log.info("Hey, why did you bother me??", e);
      }
      stopProvider();
    }
  }

  public void initializeClientInterface ()
  {
    if (connectionFactory instanceof PooledConnectionFactory) {
      PooledConnectionFactory pooledConnectionFactory = (PooledConnectionFactory) connectionFactory;
      if (pooledConnectionFactory.getConnectionFactory() instanceof ActiveMQConnectionFactory) {
        ActiveMQConnectionFactory amqConnectionFactory = (ActiveMQConnectionFactory) pooledConnectionFactory
                .getConnectionFactory();
        amqConnectionFactory.setBrokerURL(getJmsBrokerUrl());
      }
    }
  }

  public void stopProvider ()
  {
    BrokerService brokerService = BrokerRegistry.getInstance()
            .lookup(getJmsBrokerName());
    try {
      if (brokerService != null) {
        brokerService.stop();
        brokerService.waitUntilStopped();
      }
      else {
        log.info("Could not stop ActiveMQ broker.  It was never started");
      }
    }
    catch (Exception e) {
      log.error("Failed to stop JMS Server", e);
    }
  }

  public void initializeBrokersQueues (String[] queueNames)
  {
    // create broker queues
    for (String queueName: queueNames) {
      createQueue(queueName);
    }
  }

  public Queue createQueue (String queueName)
  {
    Queue queue = null;
    try {
      Connection connection = connectionFactory.createConnection();
      Session session = connection.createSession(false,
                                                 Session.AUTO_ACKNOWLEDGE);
      queue = session.createQueue(queueName);
    }
    catch (JMSException e) {
      log.error("Failed to create queue " + queueName, e);
    }

    return queue;
  }

  public void registerMessageListener (String destinationName,
                                       MessageListener listener)
  {
    log.info("registerMessageListener(" + destinationName + ", " + listener
             + ")");
    DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setDestinationName(destinationName);
    container.setMessageListener(listener);
    container.setTaskExecutor(taskExecutor);
    container.afterPropertiesSet();
    container.start();

    listenerContainerMap.put(listener, container);
  }

  public void unregisterMessageListener (MessageListener listener)
  {
    DefaultMessageListenerContainer container = (DefaultMessageListenerContainer) listenerContainerMap
            .get(listener);
    if (container != null) {
      container.shutdown();
    }
    listenerContainerMap.remove(listener);
  }

  public void unregisterAllMessageListeners ()
  {
    for (Map.Entry<MessageListener, AbstractMessageListenerContainer> entry: listenerContainerMap
            .entrySet()) {
      unregisterMessageListener(entry.getKey());
    }
  }

  /**
   * @return the servingJms
   */
  public boolean isServingJms ()
  {
    return servingJms;
  }

  /**
   * @return the servingJms
   */
  public boolean getServingJms ()
  {
    return servingJms;
  }

  /**
   * @param servingJms
   *          the servingJms to set
   */
  @ConfigurableValue(valueType = "Boolean", description = "Flag to indicate if this sim server is also the JMS provider")
  public void setServingJms (boolean servingJms)
  {
    this.servingJms = servingJms;
  }

  /**
   * @return the jmsBrokerUrl
   */
  public String getJmsBrokerUrl ()
  {
    return jmsBrokerUrl;
  }

  /**
   * @param jmsBrokerUrl
   *          the jmsBrokerUrl to set
   */
  @ConfigurableValue(valueType = "String", description = "JMS broker URL to serve and/or use by sim server")
  public void setJmsBrokerUrl (String jmsBrokerUrl)
  {
    this.jmsBrokerUrl = jmsBrokerUrl;
  }

  /**
   * @return the jmsBrokerName
   */
  public String getJmsBrokerName ()
  {
    return jmsBrokerName;
  }

  /**
   * @param jmsBrokerName
   *          the jmsBrokerName to set
   */
  @ConfigurableValue(valueType = "String", description = "JMS broker name for looking up JMS provider")
  public void setJmsBrokerName (String jmsBrokerName)
  {
    this.jmsBrokerName = jmsBrokerName;
  }
}
