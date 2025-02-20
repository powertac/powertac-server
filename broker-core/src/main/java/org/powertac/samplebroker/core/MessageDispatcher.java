/*
 * Copyright (c) 2012, 2017 by the original author
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
package org.powertac.samplebroker.core;

import static org.powertac.util.MessageDispatcher.dispatch;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.IdGenerator;
import org.powertac.common.XMLMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

/**
 * Routes incoming messages to broker components, and outgoing messages
 * to the server. 
 * Components must register for specific message types with the broker, 
 * which passes the registrations to this router. For this to work, 
 * registered components must implement a handleMessage(msg) method that
 * takes the specified type as its single argument.
 * 
 * @author John Collins
 */
@Service
public class MessageDispatcher
{
  static private Logger log = LogManager.getLogger(MessageDispatcher.class);

  @Autowired
  private XMLMessageConverter converter;

  @Autowired
  private JmsTemplate template;
  
  @Autowired
  private JmsManagementService jmsManagementService; 

  private HashMap<Class<?>, Set<Object>> registrations;
  String key = ""; // server registration secret

  /**
   * Default constructor
   */
  public MessageDispatcher ()
  {
    super();
    registrations = new HashMap<Class<?>, Set<Object>>();
  }
  
  void setKey (String key)
  {
    this.key = key;
  }

  // ------------- incoming messages ----------------
  /**
   * Sets up handlers for incoming messages by message type.
   */
  public void registerMessageHandler (Object handler, Class<?> messageType)
  {
    Set<Object> reg = registrations.get(messageType);
    if (reg == null) {
      reg = new HashSet<Object>();
      registrations.put(messageType, reg);
    }
    reg.add(handler);
  }
  
  /**
   * Routes incoming messages from the server
   */
  public void routeMessage (Object message)
  {
    Class<?> clazz = message.getClass();
    log.debug("Route " + clazz.getName());
    Set<Object> targets = registrations.get(clazz);
    if (targets == null) {
      log.warn("no targets for message of type " + clazz.getName());
      return;
    }
    for (Object target: targets) {
      dispatch(target, "handleMessage", message);
    }
  }

  // ------------------ Outgoing messages ------------------
  /**
   * Converts outgoing message to XML, sends it to the server
   */
  public void sendMessage(Object message)
  {
    if (!validateId(message))
      return;
    final String text = key + converter.toXML(message);
    sendRawMessage(text);
  }

  /**
   * Sends XML-formatted message to the server without interpretation.
   */
  public void sendRawMessage (String message)
  {
    log.info("sending text: \n" + message);

    template.send(jmsManagementService.getServerQueueName(),
                  new MessageCreator() {
      @Override
      public Message createMessage (Session session) throws JMSException
      {
        TextMessage msg = session.createTextMessage(message);
        return msg;
      }
    });
  }

  private boolean validateId (Object thing)
  {
    try {
      Field idField = thing.getClass().getDeclaredField("id");
      idField.setAccessible(true);
      long value = idField.getLong(thing);
      if (IdGenerator.getPrefix() != IdGenerator.extractPrefix(value)) {
        log.error("Invalid id value " + value + " in message "
                  + thing.toString());
        return false;
      }
    }
    catch (NoSuchFieldException e) {
      // no id field, OK to send
      return true;
    }
    catch (SecurityException e) {
      // Should not happen
      log.error("Exception accessing id field: " + e.toString());
    }
    catch (IllegalArgumentException e) {
      // Should not happen
      log.error("Exception reading id field: " + e.toString());
    }
    catch (IllegalAccessException e) {
      // Should not happen
      log.error("Exception reading id field: " + e.toString());
    }
    return true;
  }

  // test-support
  Set<Object> getRegistrations (Class<?> messageType)
  {
    return registrations.get(messageType);
  }
}
