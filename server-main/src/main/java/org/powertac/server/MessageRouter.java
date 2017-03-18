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

import static org.powertac.util.MessageDispatcher.dispatch;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.msg.BrokerAuthentication;
import org.springframework.stereotype.Service;

@Service
public class MessageRouter implements InitializationService
{
  static private Logger log = LogManager.getLogger(MessageRouter.class);

  // Routing data
  private HashMap<Class<?>, Set<Object>> registrations =
      new HashMap<Class<?>, Set<Object>>();

  /**
   * returns the registrations for the given message
   */
  public Set<Object> getRegistrations(Object message)
  {
    return registrations.get(message.getClass());
  }
  
  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.BrokerProxy#registerBrokerMarketListener(org.powertac.common.interfaces.BrokerMessageListener)
   */
  public void registerBrokerMessageListener(Object listener, Class<?> clazz) {
    Set<Object> targetSet= registrations.get(clazz);
    if (null == targetSet) {
      targetSet = new HashSet<Object>();
      registrations.put(clazz, targetSet);
    }
    targetSet.add(listener);
  }

  /**
   * Initializes the message listener registrations at the pre-game phase,
   * once per game.
   */

  public void recycle ()
  {
    // initialize the registrations
    registrations = new HashMap<Class<?>, Set<Object>>();
  }

  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    // nothing to see here folks, please move on.
    return "Router";
  }

  public boolean route(Object message) {
    boolean routed = false;
    
    boolean byPassed = (message instanceof BrokerAuthentication);
    
    String username = "unknown";
    Broker broker = null;
    if (!byPassed) {
      try {
        broker = (Broker)PropertyUtils.getSimpleProperty(message, "broker");
        username = broker.getUsername();
      }
      catch (IllegalAccessException e) {
        log.error("Failed to extract broker", e);
      }
      catch (InvocationTargetException e) {
        log.error("Failed to extract broker", e);
      }
      catch (NoSuchMethodException e) {
        log.error("Failed to extract broker", e);
      }
    }
    if (byPassed || (broker != null && broker.isEnabled())) {     
      log.debug("route(Object) - routing " + message.getClass().getSimpleName() + " from " + username);
      Set<Object> targets = registrations.get(message.getClass());
      if (targets == null) {
        log.warn("no targets for message of type " + message.getClass().getSimpleName());
      }
      else {
        for (Object target: targets) {
          dispatch(target, "handleMessage", message);
        }
        routed = true;
      }
    }
    log.debug("route(Object) - routed:" + routed);
    return routed;
  }
}
