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

import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * This message is used for authenticating a broker with the server.
 * Broker sends this message to the server with its username and game-token which 
 * it receives from the web-app.  The game-token is ignored in research mode.
 * 
 */

@Domain
@XStreamAlias("broker-authentication")
public class BrokerAuthentication
{
  @XStreamAsAttribute
  private String username;

  @XStreamAsAttribute
  private String gameToken;

  /**
   * @return the username
   */
  public String getUsername ()
  {
    return username;
  }

  /**
   * @param username
   *          the username to set
   */
  public void setUsername (String username)
  {
    this.username = username;
  }

  /**
   * @return the gameToken
   */
  public String getGameToken ()
  {
    return gameToken;
  }

  /**
   * @param gameToken
   *          the gameToken to set
   */
  public void setGameToken (String gameToken)
  {
    this.gameToken = gameToken;
  }
}
