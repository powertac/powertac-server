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
package org.powertac.server;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.springframework.stereotype.Service;

@Service
public class MessageListenerRegistrar
{
  static private Logger log = Logger.getLogger(MessageListenerRegistrar.class);

  // Routing data
  private Set<BrokerMessageListener> tariffRegistrations = new HashSet<BrokerMessageListener>();
  private Set<BrokerMessageListener> marketRegistrations = new HashSet<BrokerMessageListener>();
  private Set<BrokerMessageListener> simRegistrations = new HashSet<BrokerMessageListener>();

  /**
   * @return the tariffRegistrations
   */
  public Set<BrokerMessageListener> getTariffRegistrations()
  {
    return tariffRegistrations;
  }

  /**
   * @return the marketRegistrations
   */
  public Set<BrokerMessageListener> getMarketRegistrations()
  {
    return marketRegistrations;
  }

  /**
   * @return the simRegistrations
   */
  public Set<BrokerMessageListener> getSimRegistrations()
  {
    return simRegistrations;
  }  
  
  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.BrokerProxy#registerBrokerMarketListener(org.powertac.common.interfaces.BrokerMessageListener)
   */
  public void registerBrokerMarketListener(BrokerMessageListener listener) {
    marketRegistrations.add(listener);
  }
  
  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.BrokerProxy#registerBrokerTariffListener(org.powertac.common.interfaces.BrokerMessageListener)
   */
  public void registerBrokerTariffListener(BrokerMessageListener listener) {
    tariffRegistrations.add(listener);
  }
  
  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.BrokerProxy#registerSimListener(org.powertac.common.interfaces.BrokerMessageListener)
   */
  public void registerSimListener(BrokerMessageListener listener) {
    log.info("registerSimListener(BrokerMessageListener) - start");
    simRegistrations.add(listener);
    log.info("registerSimListener(BrokerMessageListener) - end");
  }
}
