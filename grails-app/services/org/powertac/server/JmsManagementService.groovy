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
package org.powertac.server

import javax.management.remote.JMXServiceURL
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.MBeanServerConnection
import javax.management.ObjectName
import org.apache.activemq.broker.jmx.BrokerViewMBean
import javax.management.MBeanServerInvocationHandler
import org.apache.activemq.broker.jmx.QueueViewMBean
import javax.jms.JMSException
import org.powertac.common.Competition
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * This is the JMS management service. It can create and delete queues for JMS communication with brokers and also
 * provides a way to retrieve statistics of the queues
 */
class JmsManagementService {

  static transactional = true
  def config = ConfigurationHolder.config

  /**
   * Finds and returns the JMS bean
   */
  private BrokerViewMBean getMBean() throws JMSException {
    try {
      JMXServiceURL url = new JMXServiceURL(config.powertac.jmx.broker.url)
      JMXConnector connector = JMXConnectorFactory.connect(url, null)
      connector.connect()
      MBeanServerConnection connection = connector.getMBeanServerConnection()
      ObjectName name = new ObjectName("org.apache.activemq:BrokerName=localhost,Type=Broker")
      BrokerViewMBean mbean = (BrokerViewMBean) MBeanServerInvocationHandler.newProxyInstance(connection, name, BrokerViewMBean.class, true)
      return mbean
    } catch (Exception e) {
      throw new JMSException("Failed to create JMX M queues: ${e.getMessage()}, ${e.getCause()}")
    }
  }

  /**
   * Generates statistics to be displayed for debug purposes
   */
  def getStats() {
    def stats = [:]
    JMXServiceURL url = new JMXServiceURL(config.powertac.jmx.broker.url)
    JMXConnector connector = JMXConnectorFactory.connect(url, null)
    connector.connect()
    MBeanServerConnection connection = connector.getMBeanServerConnection()
    ObjectName name = new ObjectName("org.apache.activemq:BrokerName=localhost,Type=Broker")
    BrokerViewMBean mbean = (BrokerViewMBean) MBeanServerInvocationHandler.newProxyInstance(connection, name, BrokerViewMBean.class, true)
    stats.brokerId = "${mbean.getBrokerId()} - ${mbean.getBrokerName()}"
    stats.totalMessageCount = "Total message count: ${mbean.getTotalMessageCount()}"
    stats.totalConsumerCount = "Total number of consumers: ${mbean.getTotalConsumerCount()}"
    stats.totalNumberOfQueues = "Total number of Queues: ${mbean.getQueues().length} "

    for (ObjectName queueName: mbean.getQueues()) {
      QueueViewMBean queueMbean = (QueueViewMBean) MBeanServerInvocationHandler.newProxyInstance(connection, queueName, QueueViewMBean.class, true)
      stats["Queue: ${queueMbean.name}".toString()] = "Size: ${queueMbean.queueSize}, # consumers: ${queueMbean.consumerCount}, # enqueued msg: ${queueMbean.enqueueCount}, # dequeued msg: ${queueMbean.dequeueCount}"
    }

    return stats
  }

  /**
   * Creates queues for every broker in the competition as well as global queues
   */
  def createQueues(Competition competition) throws JMSException {
    try {
      BrokerViewMBean mbean = getMBean()
      mbean.addQueue('server.inputQueue')

      def brokers = competition?.brokers
      brokers.each { broker ->
        mbean.addQueue(broker?.toQueueName())
      }

    } catch (Exception e) {
      throw new JMSException("Failed to create queues: ${e.getMessage()}, ${e.getCause()}")
    }
  }

  /**
   * Deletes all broker and global queues
   */
  def deleteQueues() throws JMSException {
    try {
      BrokerViewMBean mbean = getMBean()

      for (ObjectName queueName: mbean.getQueues()) {
        log.debug "Trying to remove queue: ${queueName.getKeyProperty('Destination')}"
        mbean.removeQueue(queueName.getKeyProperty('Destination'))
      }
    } catch (Exception e) {
      log.error("Failed to delete topics and queues: ${e.getMessage()}", e)
      throw new JMSException("Failed to delete queues: ${e.getMessage()}, ${e.getCause()}")
    }
  }


}
