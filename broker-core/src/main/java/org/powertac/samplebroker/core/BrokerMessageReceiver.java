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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.samplebroker.interfaces.IpcAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Receives incoming jms messages for the broker and deserializes them just in case they are to be consumed within the
 * current JVM process.
 *
 * @author Nguyen Nguyen, John Collins, Pascal Brokmeier
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

  private IpcAdapter adapter = null;


  @ConfigurableValue(valueType = "List",
      description = "These xml message types are passed without conversion")
  private List<String> rawMsgTypes = new ArrayList<>();

  @ConfigurableValue(valueType = "List",
      description = "These xml message types are passed after conversion")
  private List<String> cookedMsgTypes = new ArrayList<>();

  @ConfigurableValue(valueType = "Boolean", description = "If true, do xml forwarding")
  private boolean ipcAdapter = false;

  // hash sets to speed lookup of xml types
  private HashSet<String> rawTypes;
  private HashSet<String> cookedTypes;
  private Pattern tagRe = Pattern.compile("<([-_\\w]+)[\\s/>]");

  public void initialize ()
  {
    propertiesService.configureMe(this);
    if (ipcAdapter) {
      // find the message handler if it's not already there
      if (!setMessageAdapter())
        return;

      //log.info("raw type {}", msg);
      rawTypes = new HashSet<>();
      rawTypes.addAll(rawMsgTypes);
      //log.info("cooked type {}", msg);
      cookedTypes = new HashSet<>();
      cookedTypes.addAll(cookedMsgTypes);
    }
  }

  private boolean setMessageAdapter ()
  {
    if (null != adapter)
      // test support
      return true;
    List<IpcAdapter> adapters =
            SpringApplicationContext.listBeansOfType(IpcAdapter.class);
    if (0 == adapters.size()) {
      log.error("Raw xml specified, but no adapter available");
      return false;
    }
    adapter = adapters.get(0);
    log.info("Using {} for xml forwarding", adapter.getClass().getName());
    return true;
  }


  @Override
  public void onMessage (Message message)
  {
    if (message instanceof TextMessage textMessage) {
      String msg;
      try {
        log.debug("onMessage(Message) - receiving a message");
        msg = textMessage.getText();
        log.info("received message:\n" + msg);
        //onMessage(msg);
        if (adapter != null) {
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
        }
        else {
          onMessage(msg);
        }
      }
      catch (JMSException e) {
        log.error("failed to extract text from TextMessage", e);
      }
    }
  }

  private void onMessage (String xml)
  {
    //log.info("onMessage(String) - received message:\n" + xml);
    Object message = converter.fromXML(xml);
    log.debug("onMessage(String) - received message of type " + message.getClass().getSimpleName());
    messageDispatcher.routeMessage(message);
  }
}
