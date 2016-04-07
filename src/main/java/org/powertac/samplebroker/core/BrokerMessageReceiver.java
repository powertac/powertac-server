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
package org.powertac.samplebroker.core;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.ConfigurableValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Receives incoming jms messages for the broker
 * @author Nguyen Nguyen, John Collins
 */
@Service
public class BrokerMessageReceiver implements MessageListener
{
  static private Logger log = LogManager.getLogger(BrokerMessageReceiver.class);
  
  @Autowired
  XMLMessageConverter converter;
  
  @Autowired 
  MessageDispatcher messageDispatcher;

  @Autowired
  private BrokerPropertiesService propertiesService;

  @ConfigurableValue(valueType = "Boolean",
      description = "If true, then some messages are not converted to java")
  private Boolean rawXML = false;

  @ConfigurableValue(valueType = "List",
      description = "These xml message types are passed without conversion")
  private List<String> rawMsgTypes = new ArrayList<>();

  @ConfigurableValue(valueType = "List",
      description = "These xml message types are passed after conversion")
  private List<String> cookedMsgTypes = new ArrayList<>();

  public void initialize ()
  {
    propertiesService.configureMe(this);
  }

  @Override
  public void onMessage (Message message)
  {
    if (message instanceof TextMessage) {
      try {
        log.debug("onMessage(Message) - receiving a message");
        onMessage(((TextMessage) message).getText());
      } catch (JMSException e) {
        log.error("failed to extract text from TextMessage", e);
      }
    }
  }

  private void onMessage (String xml) {
    log.info("onMessage(String) - received message:\n" + xml);
    Object message = converter.fromXML(xml);
    log.debug("onMessage(String) - received message of type " + message.getClass().getSimpleName());
    messageDispatcher.routeMessage(message);
  }
}
