/*
 * Copyright 2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.powertac.common.msg;

import org.powertac.common.Broker;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * This message is used for authenticating a broker with the server.
 * Broker sends this message to the server with its username, password, and 
 * the name of the queue through which it wishes to receive messages. The
 * password is ignored by a standalone server; in a tournament situation it is
 * the game-token received from the tournament manager. The queue name may be
 * the username, but in a tournament situation using some sort of
 * difficult-to-guess hash (or possibly the game token) will make it more
 * difficult (but not impossible) for other brokers to intercept its messages.
 * If the login is accepted, a {@link BrokerAccept} message is returned.
 */

@Domain
@XStreamAlias("broker-authentication")
public class BrokerAuthentication
{
  @XStreamAsAttribute
  private String username;
  
  @XStreamAsAttribute
  private String password;
  
  @XStreamAsAttribute
  private String queueName;
  
  /**
   * Creates an instance from a broker
   */
  public BrokerAuthentication (Broker broker)
  {
    super();
    this.username = broker.getUsername();
    this.password = broker.getPassword();
  }
  
  /**
   * Creates an instance from a username, password
   */
  public BrokerAuthentication (String username, String password)
  {
    super();
    this.username = username;
    this.password = password;
  }
  
  /**
   * Creates an instance with a queue name
   */
  public BrokerAuthentication (String username,
                               String password,
                               String queueName)
  {
    this(username, password);
    this.queueName = queueName;
  }

  /**
   * @return the broker username
   */
  public String getUsername ()
  {
    return username;
  }
  
  /**
   * @return the password
   */
  public String getPassword ()
  {
    return password;
  }

  /**
   * Returns the queue name
   */
  public String getQueueName ()
  {
    return queueName;
  }
  
  /**
   * @param password new password
   */
//  @StateChange
//  public void setPassword (String password)
//  {
//    this.password = password;
//  }
}
