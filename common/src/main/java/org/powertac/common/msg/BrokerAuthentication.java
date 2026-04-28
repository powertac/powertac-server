/*
 * Copyright 2011-2013 the original author or authors.
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
 * 
 * @author John Collins
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
  private long brokerTime = 0l;

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
   * Sets the system time. Should be set immediately before sending the
   * message to the server.
   */
  public void setBrokerTime (long time)
  {
    brokerTime = time;
  }

  /**
   * Returns the system time set by the broker. Used by the server to
   * estimate clock offset.
   */
  public long getBrokerTime ()
  {
    return brokerTime;
  }
}
