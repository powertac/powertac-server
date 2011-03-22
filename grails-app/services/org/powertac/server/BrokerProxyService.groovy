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

/**
 * BrokerProxyService is responsible for handling in- and outgoing communication with brokers
 * @author David Dauer
 */
class BrokerProxyService implements BrokerProxy {

  static transactional = true

  /**
   * Send a message to a specific broker
   */
  void sendMessage(Broker broker, Object messageObject) {
    def queueName = broker.toQueueName()
    def xmlString = messageObject as XML
    // TODO: "as XML" syntax soo simple here
    try {
      sendQueueJMSMessage(queueName, xmlString)
    } catch (Exception e) {
      throw new JMSException("Failed to send message to queue '$queueName' ($xmlString)")
    }
  }

  /**
   * Send a message to all brokers
   */
  void broadcastMessage(Object messageObject) {
    def brokerQueueNames = Competition.currentCompetition()?.brokers?.collect { it.toQueueName() }
    def queueName = brokerQueueNames.join(",")
    def xmlString = messageObject as XML
    // TODO: "as XML" syntax soo simple here
    try {
      sendQueueJMSMessage(queueName, xmlString)
    } catch (Exception e) {
      throw new JMSException("Failed to send message to queue '$queueName' ($xmlString)")
    }
  }

  /**
   * Receives and routes all incoming messages
   */
  @Queue(name = "server.inputQueue")
  def receiveMessage(String xmlMessage) {
    def xml = new XmlSlurper().parseText(xmlMessage)
    log.debug "received ${xmlMessage}"
  }
}
