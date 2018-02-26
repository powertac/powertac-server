/*
 * Copyright (c) 2012, 2018 by the original authors
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
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.samplebroker.interfaces.IpcAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * Receives incoming jms messages for the broker and deserializes them just in case they are to be consumed within the
 * current JVM process.
 *
 * @author Nguyen Nguyen, John Collins
 */
@Service
public class BrokerMessageReceiver implements MessageListener
{
  static private Logger log = LogManager.getLogger(BrokerMessageReceiver.class);

  @Autowired
  private XMLMessageConverter converter;

  @Autowired
  private MessageDispatcher messageDispatcher;

  @Autowired
  private BrokerPropertiesService propertiesService;

  private IpcAdapter adapter;

  @ConfigurableValue(valueType = "Boolean",
      description = "If true, then some messages are not converted to java")
  private Boolean rawXml = false;

  @ConfigurableValue(valueType = "List",
      description = "These xml message types are passed without conversion")
  private List<String> rawMsgTypes = new ArrayList<>();

  @ConfigurableValue(valueType = "List",
      description = "These xml message types are passed after conversion")
  private List<String> cookedMsgTypes = new ArrayList<>();

  @ConfigurableValue(valueType = "String", description = "Type of xml forwarding. Can be 'CharStreamAdapter' or 'GrpcSocketAdapter'")
  private String xmlForwardType = "CharStreamAdapter";

  // hash sets to speed lookup of xml types
  private HashSet<String> rawTypes;
  private HashSet<String> cookedTypes;
  private Pattern tagRe = Pattern.compile("<([-_\\w]+)[\\s/>]");

  public void initialize()
  {
    propertiesService.configureMe(this);
    if (rawXml) {
      log.info("rawXml={}", rawXml);
      // set up data structures
      rawTypes = new HashSet<>();
      rawMsgTypes.forEach(msg -> {
        //log.info("raw type {}", msg);
        rawTypes.add(msg);
      });
      cookedTypes = new HashSet<>();
      cookedMsgTypes.forEach(msg -> {
        //log.info("cooked type {}", msg);
        cookedTypes.add(msg);
      });
      // find the message handler if it's not already there
      setMessageAdapter();
    }
  }

  private void setMessageAdapter()
  {
    if (null == adapter) {
      adapter = (IpcAdapter) SpringApplicationContext.getBean(xmlForwardType);
    }
    if (null == adapter) {
      log.error("Raw xml specified, but no adapter available");
      rawXml = false;
    }
    log.info("Using {} for xml forwarding", xmlForwardType);
  }


  @Override
  public void onMessage(Message message)
  {
    if (message instanceof TextMessage) {
      String msg;
      try {
        log.debug("onMessage(Message) - receiving a message");
        msg = ((TextMessage) message).getText();
        log.info("received message:\n" + msg);
        //onMessage(msg);
        if (rawXml) {
          // Extract the tag, conditionally pass on the message and/or
          // unmarshal it and process it locally
          Matcher m = tagRe.matcher(msg);
          if (m.lookingAt()) {
            String tag = m.group(1);
            log.info("msg tag: {}", tag);
            if (cookedTypes.contains(tag)) {
              onMessage(msg);
            }
            if (rawTypes.contains(tag)) {
              adapter.exportMessage(msg);
            }
          }
        } else {
          onMessage(msg);
        }
      } catch (JMSException e) {
        log.error("failed to extract text from TextMessage", e);
      }
    }
  }

  private void onMessage(String xml)
  {
    //log.info("onMessage(String) - received message:\n" + xml);
    Object message = converter.fromXML(xml);
    log.debug("onMessage(String) - received message of type " + message.getClass().getSimpleName());
    messageDispatcher.routeMessage(message);
  }
}
