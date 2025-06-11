/*
 * Copyright (c) 2012 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.samplebroker.interfaces;

import java.util.List;

import java.time.Instant;
import org.powertac.common.Broker;

/**
 * Provides message handler registration, along with
 * access to competition context information, including
 * <ul>
 * <li>The message router for outgoing messages</li>
 * <li>The underlying org.powertac.common.Broker instance</li>
 * <li>The name of this broker</li>
 * <li>The base time for the current simulation</li>
 * <li>The length of common data arrays</li>
 * </ul>
 * @author John Collins
 */
public interface BrokerContext
{
  /**
   * Delegates registrations to the router
   */
  public void registerMessageHandler (Object handler, Class<?> messageType);
  
  /**
   * Sends an outgoing message. May need to be reimplemented in a remote broker.
   */
  public void sendMessage (Object message);

//  /**
//   * Returns the router for outgoing messages.
//   */
//  public MessageDispatcher getRouter ();
  
  /**
   * Returns the org.powerac.common.Broker instance
   */
  public Broker getBroker ();
  
  /**
   * Returns the broker name (username of the underlying Broker)
   */
  public String getBrokerUsername ();
  
  /**
   * Returns the simulation base time
   */
  public Instant getBaseTime ();
  
  /**
   * Returns length of data array used 
   * for tracking customer consumption/production
   */
  public int getUsageRecordLength ();
  
  /**
   * Returns the broker's list of competing brokers - non-public
   */
  public List<String> getBrokerList ();
}
