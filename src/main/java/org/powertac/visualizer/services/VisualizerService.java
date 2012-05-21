/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.visualizer.services;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.log4j.Logger;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.VisualizerMessageListener;
import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.VisualizerApplicationContext;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.interfaces.Initializable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.stereotype.Service;

/**
 * Main Visualizer service. Its main purpose is to register with Visualizer
 * proxy and to receive messages from simulator.
 * 
 * @author Jurica Babic
 * 
 */

@Service
public class VisualizerService
  implements VisualizerMessageListener, ApplicationContextAware, MessageListener
{
  static private Logger log = Logger.getLogger(VisualizerService.class
          .getName());

  @Resource(name="jmsFactory")
  private ConnectionFactory connectionFactory;
  
  @Autowired
  private Executor taskExecutor;
  
  @Autowired
  XMLMessageConverter converter;

  @Autowired
  private VisualizerBean visualizerBean;

  private boolean alreadyRegistered = false;
  private String serverUrl = "tcp://localhost:61616";
  private String queueName = "remote-visualizer";

  @Autowired
  private MessageDispatcher dispatcher;

  public VisualizerService ()
  {
    super();
  }

  /**
   * Should be called before simulator run in order to prepare/reset
   * Visualizer beans and register with the new simulator instance.
   */
  public void init (VisualizerProxy visualizerProxy)
  {

    visualizerBean.newRun();

    // Register Visualizer with VisualizerProxy service
    if (!alreadyRegistered) {
      visualizerProxy.registerVisualizerMessageListener(this);
      alreadyRegistered = true;
    }

    // visualizerLogService.startLog(visualizerBean.getVisualizerRunCount());

    // registrations for message listeners:
    List<Initializable> initializers =
      VisualizerApplicationContext.listBeansOfType(Initializable.class);
    for (Initializable init: initializers) {
      log.debug("initializing..." + init.getClass().getName());
      init.initialize();
    }
  }

  public void receiveMessage (Object msg)
  {

    visualizerBean.incrementMessageCounter();

    if (msg != null) {
      log.debug("Counter: " + visualizerBean.getMessageCount()
                + ", Got message: " + msg.getClass().getName());
      dispatcher.routeMessage(msg);
    }
    else {

      log.warn("Counter:" + visualizerBean.getMessageCount()
               + " Received message is NULL!");
    }

  }

  // JMS message input processing
  public void onMessage (Message message)
  {
    if (message instanceof TextMessage) {
      try {
        log.debug("onMessage(Message) - receiving a message");
        onMessage(((TextMessage) message).getText());
      } catch (JMSException e) {
        log.error("failed to extract text from TextMessage", e);
      }
    }
  }

  private void onMessage (String xml) {
    //log.info("onMessage(String) - received message:\n" + xml);
    Object message = converter.fromXML(xml);
    log.debug("onMessage(String) - received message of type " + message.getClass().getSimpleName());
    receiveMessage(message);
  }
  
  // URL and queue name methods
  public String getQueueName ()
  {
    return queueName;
  }
  
  public void setQueueName (String newName)
  {
    queueName = newName;
  }
  
  public String getServerUrl ()
  {
    return serverUrl;
  }
  
  public void setServerUrl (String newUrl)
  {
    serverUrl = newUrl;
  }
  
  public void setApplicationContext (ApplicationContext applicationContext)
    throws BeansException
  {
    
  }

  class LocalVisualizerProxy implements VisualizerProxy
  {
    TreeSet<VisualizerMessageListener> listeners =
      new TreeSet<VisualizerMessageListener>();
    
    VisualizerService host;

    LocalVisualizerProxy ()
    {
      super();
    }

    // set up the jms queue
    void init (VisualizerService host)
    {
      this.host = host;
      
      if (connectionFactory instanceof PooledConnectionFactory) {
        PooledConnectionFactory pooledConnectionFactory = (PooledConnectionFactory) connectionFactory;
        if (pooledConnectionFactory.getConnectionFactory() instanceof ActiveMQConnectionFactory) {
          ActiveMQConnectionFactory amqConnectionFactory = (ActiveMQConnectionFactory) pooledConnectionFactory
                  .getConnectionFactory();
          amqConnectionFactory.setBrokerURL(getServerUrl());
        }
      }

      // create the queue first
      boolean success = false;
      while (!success) {
        try {
          createQueue(getQueueName());
          success = true;
        }
        catch (JMSException e) {
          log.info("JMS message broker not ready - delay and retry");
          try {
            Thread.sleep(20000);
          }
          catch (InterruptedException e1) {
            // ignore exception
          }
        }
      }

      // register host as listener
      DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
      container.setConnectionFactory(connectionFactory);
      container.setDestinationName(getQueueName());
      container.setMessageListener(host);
      container.setTaskExecutor(taskExecutor);
      container.afterPropertiesSet();
      container.start();
    }

    public void createQueue (String queueName) throws JMSException
    {
      // now we can create the queue
      Connection connection = connectionFactory.createConnection();
      Session session = connection.createSession(false,
                                                 Session.AUTO_ACKNOWLEDGE);
      session.createQueue(queueName);
      log.info("JMS Queue " + queueName + " created");
    }

    // dummy method
    public void
      registerVisualizerMessageListener (VisualizerMessageListener listener)
    {
      //listeners.add(listener);
    }

    public void forwardMessage (Object message)
    {
      // dummy method
    }

  }
}
