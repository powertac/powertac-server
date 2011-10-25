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
  private int prefix;

  /**
   * @return the prefix
   */
  public int getPrefix ()
  {
    return prefix;
  }

  /**
   * @param prefix the prefix to set
   */
  public void setPrefix (int prefix)
  {
    this.prefix = prefix;
  }
}
