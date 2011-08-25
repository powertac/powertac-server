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

package org.powertac.common.interfaces;

import org.powertac.common.Broker;

import java.util.List;

/**
 * BrokerProxy is responsible for handling in- and outgoing communication with brokers
 *
 * @author David Dauer
 */
public interface BrokerProxy {

  /**
   * Send a message to a specific broker
   */
  void sendMessage(Broker broker, Object messageObject);

  /**
   * Sends a list of messages to a specific broker
   */
  void sendMessages(Broker broker, List<?> messageObjects);

  /**
   * Send a message to all brokers
   */
  void broadcastMessage(Object messageObject);

  /**
   * Sends a list of messages to all brokers
   */
  void broadcastMessages(List<?> messageObjects);
  
  /**
   * Sets up a dispatch listener for market messages
   */
  void registerBrokerMarketListener (BrokerMessageListener listener);

  /**
   * Sets up a dispatch listener for tariff messages
   */
  void registerBrokerTariffListener (BrokerMessageListener listener);

  /**
   * Sets up a dispatch listener for simulator messages
   */
  public void registerSimListener (CompetitionControl listener);
}
