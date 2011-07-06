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
import javax.jms.JMSException
import org.powertac.common.*
import org.powertac.common.msg.*
import org.powertac.common.command.*
import org.powertac.common.interfaces.BrokerProxy
import org.powertac.common.interfaces.BrokerMessageListener
import org.powertac.common.interfaces.CompetitionControl

/**
 * BrokerProxyService is responsible for handling in- and outgoing communication with brokers
 * @author David Dauer
 */
class BrokerProxyService 
    implements BrokerProxy
{

  static transactional = false
  static expose = ['jms']

  def jmsService
  MessageConverter messageConverter = new MessageConverter()

  def visualizationProxyService // autowire
  //def competitionControlService - circular dependency

  Set tariffRegistrations = []
  Set marketRegistrations = []
  Set simRegistrations = []
  Set tariffMessageTypes = 
    [TariffSpecification.class, Rate.class, HourlyCharge.class, TariffUpdate.class,
     TariffExpire.class, TariffRevoke.class, VariableRateUpdate.class] as Set
  Set simMessageTypes = [PauseRequest.class, PauseRelease.class] as Set
  
/**
 * Send a message to a specific broker
 */
  void sendMessage(Broker broker, Object messageObject) 
  {
    if (messageObject == null) {
      log.info "null message for $broker"
      return
    }
    // dispatch to visualizers
    visualizationProxyService.forwardMessage(messageObject)

    if (broker.local) {
      broker.receiveMessage(messageObject)
      return
    }
    def queueName = broker.toQueueName()
    String xml = messageConverter.toXML(messageObject)
    log.debug "send to ${broker.username} ${xml}"
    try {
      jmsService.send(queueName, xml)
    } catch (Exception e) {
      log.error "Failed to send message to queue '$queueName' ($xml)"
      //throw new JMSException("Failed to send message to queue '$queueName' ($xml)")
    }
  }

/**
 * Send a list of messages to a specific broker
 */
  void sendMessages(Broker broker, List<?> messageObjects) 
  {
    if (messageObjects.size() == 0) {
      log.info "empty list for $broker"
      return
    }
    sendMessage(broker, messageObjects)
  }

/**
 * Send a message to all brokers
 */
  void broadcastMessage(Object messageObject) 
  {
    if (messageObject == null) {
      log.info "broadcast null object"
      return
    }
    // dispatch to visualizers
    visualizationProxyService.forwardMessage(messageObject)

    String xml = messageConverter.toXML(messageObject)
    log.debug "broadcast ${xml}"
    broadcastMessage(xml)

    // Include local brokers
    def localBrokers = Broker.list().findAll { (it.local) }
    localBrokers*.receiveMessage(messageObject)
  }

  void broadcastMessage(String text) 
  {
    def brokerQueueNames = Broker.list().findAll { !(it.local) }?.collect { it.toQueueName() }
    def queueName = brokerQueueNames.join(",")
    log.info("Broadcast queue name is ${queueName}")
    try {
      jmsService.send(queueName, text)
    } catch (Exception e) {
      log.error "Failed to send message to queue '$queueName' ($text)"
      //throw new JMSException("Failed to send message to queue '$queueName' ($text)")
    }
  }

/**
 * Sends a list of messages to all brokers
 */
  void broadcastMessages(List<?> messageObjects) 
  {
    if (messageObjects.size() == 0) {
      log.info "broadcast empty list"
      return
    }
    broadcastMessage(messageObjects)
  }

/**
 * Receives and routes all incoming messages
 */
  @Queue(name = "server.inputQueue")
  void receiveMessage(String xmlMessage) 
  {
    def thing = messageConverter.fromXML(xmlMessage)
    log.debug "received ${xmlMessage}"
    
    // persist incoming messages (#169)
    if (!thing.validate()) {
      log.warn("validation error on ${xmlMessage}: ${thing.errors.allErrors.collect {it.toString()}}")
    }
    else {
      //thing.save()
      routeMessage(thing)
    }
  }
  
  void routeMessage(Object thing)
  {
    // dispatch to visualizers
    visualizationProxyService?.forwardMessage(thing)

    // dispatch to listeners
    if (tariffMessageTypes.contains(thing.class)) {
      tariffRegistrations.each { listener ->
        listener.receiveMessage(thing)
      }
    }
    else if (simMessageTypes.contains(thing.class)) {
	  simRegistrations.each { listener ->
        listener.receiveMessage(thing)
	  }
    }
    else {
      marketRegistrations.each { listener ->
        listener.receiveMessage(thing)
      }
    }
    //broadcastMessage("I got your message")
    //log.debug "receiveMessage - end"
  }

/**
 * Should be called if tariff-related incoming broker messages should be sent to listener
 */
  void registerBrokerTariffListener(BrokerMessageListener listener) 
  {
    tariffRegistrations.add(listener)
  }

/**
 * Should be called if market-related incoming broker messages should be sent to listener
 */
  void registerBrokerMarketListener(BrokerMessageListener listener) 
  {
    marketRegistrations.add(listener)
  }
  
  void registerSimListener(CompetitionControl listener)
  {
    simRegistrations.add(listener)
  }
}
