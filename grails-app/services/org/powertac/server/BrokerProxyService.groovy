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

import grails.plugin.jms.Queue
import org.powertac.common.Broker
import grails.converters.XML
import javax.jms.JMSException
import org.powertac.common.Competition
import org.powertac.common.interfaces.BrokerProxy
import org.powertac.common.interfaces.BrokerTariffListener
import org.powertac.common.interfaces.BrokerMarketListener

/**
 * BrokerProxyService is responsible for handling in- and outgoing communication with brokers
 * @author David Dauer
 */
class BrokerProxyService implements BrokerProxy {

  static transactional = true
  static expose = ['jms']

  def jmsService

  def tariffRegistrations = []
  def marketRegistrations = []

  /**
   * Send a message to a specific broker
   */
  void sendMessage(Broker broker, Object messageObject) {
    if (broker.local) {
      broker.receiveMessage(messageObject)
      return
    }
    def queueName = broker.toQueueName()
    def xml = messageObject as XML
    try {
      jmsService.send(queueName, xml.toString())
    } catch (Exception e) {
      throw new JMSException("Failed to send message to queue '$queueName' ($xml)")
    }
  }

  /**
   * Send a list of messages to a specific broker
   */
  void sendMessages(Broker broker, List<?> messageObjects) {
    messageObjects?.each { message ->
      sendMessage(broker, message)
    }
  }

  /**
   * Send a message to all brokers
   */
  void broadcastMessage(Object messageObject) {
    def xml = messageObject as XML
    broadcastMessage(xml.toString())
  }

  void broadcastMessage(String text) {
    def brokerQueueNames = Competition.currentCompetition()?.brokers?.collect { it.toQueueName() }
    def queueName = brokerQueueNames.join(",")
    log.info("Broadcast queue name is ${queueName}")
    try {
      jmsService.send(queueName, text)
    } catch (Exception e) {
      throw new JMSException("Failed to send message to queue '$queueName' ($xml)")
    }
  }

  /**
   * Sends a list of messages to all brokers
   */
  void broadcastMessages(List<?> messageObjects) {
    messageObjects?.each { message ->
      broadcastMessage(message)
    }
  }

  /**
   * Receives and routes all incoming messages
   */
  @Queue(name = "server.inputQueue")
  def receiveMessage(String xmlMessage) {
    //  def xml = new XmlSlurper().parseText(xmlMessage)
    log.debug "received ${xmlMessage}"
    broadcastMessage("test")
  }

  /**
   * Should be called if tariff-related incoming broker messages should be sent to listener
   */
  void registerBrokerTariffListener(BrokerTariffListener listener) {
    tariffRegistrations.add(listener)
  }

  /**
   * Should be called if market-related incoming broker messages should be sent to listener
   */
  void registerBrokerMarketListener(BrokerMarketListener listener) {
    marketRegistrations.add(listener)
  }
}
