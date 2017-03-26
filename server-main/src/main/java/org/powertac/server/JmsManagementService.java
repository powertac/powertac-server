package org.powertac.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerRegistry;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.broker.region.DestinationStatistics;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.ServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.stereotype.Service;

@Service
public class JmsManagementService
{
  static private Logger log = LogManager.getLogger(JmsManagementService.class);

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
  private long maxQueueDepth = 1000;

  private BrokerService getProvider ()
  {
    return BrokerRegistry.getInstance().lookup(getJmsBrokerName());
  }

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
    BrokerService brokerService = getProvider();
    if (brokerService != null) {
      log.info("JMS Server is already started.");
      return;
    }

    brokerService = new BrokerService();
    try {
      brokerService.setBrokerName(getJmsBrokerName());
      brokerService.setPersistent(false);
      brokerService.setUseJmx(false);
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
    
    // reset connection factory
    if (connectionFactory instanceof CachingConnectionFactory) {
      CachingConnectionFactory cachingConnectionFactory = (CachingConnectionFactory) connectionFactory;
      cachingConnectionFactory.resetConnection();
    }
    
    try {
      // let's wait a few seconds before shutting down
      Thread.sleep(3000);
    }
    catch (InterruptedException e) {
      log.info("Hey, why did you bother me??", e);
    }
    if (isServingJms()) {
      stopProvider();
    }
  }

  public void initializeClientInterface ()
  {
    ActiveMQConnectionFactory amqConnectionFactory = null;
    if (connectionFactory instanceof PooledConnectionFactory) {
      PooledConnectionFactory pooledConnectionFactory = (PooledConnectionFactory) connectionFactory;
      if (pooledConnectionFactory.getConnectionFactory() instanceof ActiveMQConnectionFactory) {
        amqConnectionFactory = (ActiveMQConnectionFactory) pooledConnectionFactory
                .getConnectionFactory();
      }
    }
    else if (connectionFactory instanceof CachingConnectionFactory) {
      CachingConnectionFactory cachingConnectionFactory = (CachingConnectionFactory) connectionFactory;
      if (cachingConnectionFactory.getTargetConnectionFactory() instanceof ActiveMQConnectionFactory) {
        amqConnectionFactory = (ActiveMQConnectionFactory) cachingConnectionFactory
                .getTargetConnectionFactory();
      }
    }

    if (amqConnectionFactory != null) {
      amqConnectionFactory.setBrokerURL(getJmsBrokerUrl());
    }
  }

  public void stopProvider ()
  {
    BrokerService brokerService = getProvider();
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

  /**
   * @return the maxQueueDepth
   */
  public long getMaxQueueDepth ()
  {
    return maxQueueDepth;
  }

  /**
   * @param maxQueueDepth
   *          the maxQueueDepth to set
   */
  @ConfigurableValue(valueType = "Long", description = "Maximum Queue Depth")
  public void setMaxQueueDepth (long maxQueueDepth)
  {
    this.maxQueueDepth = maxQueueDepth;
  }

  private boolean destinationLimitReached (Destination dst)
  {
    DestinationStatistics stats = dst.getDestinationStatistics();
    long depth = stats.getEnqueues().getCount()
                      - stats.getDequeues().getCount();
    log.debug("destination " + dst.getName() + " - depth:" + depth);
    return depth > getMaxQueueDepth();
  }

  public Set<String> processQueues ()
  {
    BrokerService brokerService = getProvider();
    if (brokerService == null) {
      log.debug("processQueues - JMS Server has not been started");
      return null;
    }

    Set<String> badQueues = new HashSet<String>();
    try {
      Broker broker = brokerService.getBroker();
      Map<ActiveMQDestination, Destination> dstMap = broker.getDestinationMap();
      for (Map.Entry<ActiveMQDestination, Destination> entry: dstMap.entrySet()) {
        ActiveMQDestination amqDestination = entry.getKey();
        Destination destination = entry.getValue();
        if (destinationLimitReached(destination)) {
          badQueues.add(destination.getName());
          deleteDestination(broker, amqDestination, destination);
        }
      }
    }
    catch (Exception e) {
      log.error("Encounter exception while getting jms broker", e);
    }

    return badQueues;
  }

  private void deleteDestination (Broker broker,
                                  ActiveMQDestination amqDestination,
                                  Destination destination) throws Exception
  {
    List<Subscription> subscriptions = destination.getConsumers();
    for (Subscription subscription: subscriptions) {
      ConsumerInfo info = new ConsumerInfo();
      info.setDestination(amqDestination);
      info.setConsumerId(subscription.getConsumerInfo().getConsumerId());
      broker.removeConsumer(subscription.getContext(), info);
    }
    ConnectionContext context = new ConnectionContext();
    context.setBroker(broker);
    broker.removeDestination(context, amqDestination, 0);
    log.info("processQueues - successfully remove queue " + destination.getName());
  }
}

