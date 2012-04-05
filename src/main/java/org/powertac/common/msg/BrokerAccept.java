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
package org.powertac.common.msg;

import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * This message is used to signify that the broker authentication is accepted.
 * Server sends this message to broker after successful validation of 
 * BrokerAuthentication message from the broker.  Broker must use the prefix
 * to initialize its IdGenerator. 
 */

@Domain
@XStreamAlias("broker-accept")
public class BrokerAccept
{
  @XStreamAsAttribute
  private int prefix;

  @XStreamAsAttribute
  private String key;
  
  public BrokerAccept (int prefix)
  {
    this.prefix = prefix;
  }
  
  public BrokerAccept (int prefix, String key)
  {
    this(prefix);
    this.key = key;
  }

  /**
   * Returns the ID prefix to be used by the broker. On receiving this message,
   * a remote broker is responsible for calling IdGenerator.setPrefix(prefix).
   */
  public int getPrefix ()
  {
    return prefix;
  }

  /**
   * Returns the jms key used to validate broker communications. On receiving
   * this message, a remote broker is responsible for calling
   * broker.setKey(key) before sending messages to the server.
   */
  public String getKey ()
  {
    return key;
  }
}
