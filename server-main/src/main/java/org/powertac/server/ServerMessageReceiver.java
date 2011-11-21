package org.powertac.server;

import java.lang.reflect.InvocationTargetException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.powertac.common.Broker;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.BrokerProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServerMessageReceiver implements MessageListener
{
  private static final Log log = LogFactory.getLog(BrokerProxyService.class);
  
  @Autowired
  XMLMessageConverter converter;
  
  @Autowired 
  BrokerProxy brokerProxy;

  @Override
  public void onMessage (Message message)
  {
    System.out.println("onMessage(message) - received a message");
    if (message instanceof TextMessage) {
      try {
        onMessage(((TextMessage) message).getText());
      } catch (JMSException e) {
        log.error("failed to extract text from TextMessage", e);
      }
    }
  }

  private void onMessage (String xml) {
    Object message = converter.fromXML(xml);
    Broker broker = null;
    try {
      System.out.println("onMessage(String) - received a " + message.getClass().getSimpleName() + " message");
      broker = (Broker)PropertyUtils.getSimpleProperty(message, "broker");
      brokerProxy.routeMessage(broker, message);
    }
    catch (IllegalAccessException e) {
      log.error("Failed to extract broker from message", e);
    }
    catch (InvocationTargetException e) {
      log.error("Failed to extract broker from message", e);
    }
    catch (NoSuchMethodException e) {
      log.error("Failed to extract broker from message", e);
    }    
  }
}
