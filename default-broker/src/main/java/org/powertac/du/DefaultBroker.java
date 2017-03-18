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
package org.powertac.du;

import org.powertac.common.Broker;
import org.powertac.common.state.Domain;

/**
 * @author jcollins
 */
@Domain
public class DefaultBroker
extends Broker
{
  private DefaultBrokerService messageHandler = null;
  
  public DefaultBroker (String username)
  {
    super(username);
    setLocal(true);
  }
  
  public void setService (DefaultBrokerService service)
  {
    messageHandler = service;
  }

  /**
   * Receives a message intended for the broker, forwards it to the
   * message handler in the enclosing service.
   */
  @Override
  public void receiveMessage(Object object) 
  {
    messageHandler.receiveBrokerMessage(object);
  }

}
