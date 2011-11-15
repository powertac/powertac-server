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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.powertac.common.HourlyCharge;
import org.powertac.common.Rate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.msg.BrokerAuthentication;
import org.powertac.common.msg.PauseRelease;
import org.powertac.common.msg.PauseRequest;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffUpdate;
import org.powertac.common.msg.VariableRateUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageRouter
{
  @Autowired
  private MessageListenerRegistrar registrar;
  
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  Set<?> tariffMessageTypes = new HashSet<Class>(Arrays.asList(TariffSpecification.class,
      Rate.class, HourlyCharge.class, TariffUpdate.class, TariffExpire.class,
      TariffRevoke.class, VariableRateUpdate.class));
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  Set<?> simMessageTypes = new HashSet<Class>(Arrays.asList(PauseRequest.class, 
      PauseRelease.class, BrokerAuthentication.class));

  public void route(Object message) {
    if (tariffMessageTypes.contains(message.getClass())) {
      for (BrokerMessageListener tariffMessageListener : registrar.getTariffRegistrations()) {
        tariffMessageListener.receiveMessage(message);
      }
    } else if (simMessageTypes.contains(message.getClass())) {
      for (BrokerMessageListener simMessageListener : registrar.getSimRegistrations()) {
        simMessageListener.receiveMessage(message);
      }
    } else {
      for (BrokerMessageListener marketMessageListener : registrar.getMarketRegistrations()) {
        marketMessageListener.receiveMessage(message);
      }
    }
  }
}
