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
package org.powertac.server;

import org.powertac.common.Broker;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.msg.BrokerAccept;
import org.powertac.common.msg.BrokerAuthentication;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.util.PropertiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This class manages broker login process.
 */
@Service
public class BrokerManagementService
{
  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private BrokerProxy brokerProxy;

  @Autowired
  private PropertiesUtil propertiesUtil;

  public synchronized void processBrokerAuthentication (BrokerAuthentication authentication)
  {
    Broker broker = brokerRepo.findByUsername(authentication.getUsername());
    boolean authenticated = false;
    if (propertiesUtil.getProperty("server.mode", "research").equals("tournament")) { 
      if (broker != null && authentication.getGameToken().equals(broker.getApiKey())) {
        authenticated = true;         
      }
    } else {
      if (broker == null) {
        brokerRepo.add(new Broker(authentication.getUsername(), false, false));
      }
      authenticated = true;               
    }
 
  
    if (authenticated) {
      broker.setEnabled(true);
      brokerProxy.sendMessage(broker, new BrokerAccept((int)broker.getId()));
    }
  }
}
