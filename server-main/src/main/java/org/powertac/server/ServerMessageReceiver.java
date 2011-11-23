package org.powertac.server;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.BrokerProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServerMessageReceiver implements MessageListener
{
  static private Logger log = Logger.getLogger(ServerMessageReceiver.class);
  
  @Autowired
  XMLMessageConverter converter;
  
  @Autowired 
  BrokerProxy brokerProxy;

  @Override
  public void onMessage (Message message)
  {
    if (message instanceof TextMessage) {
      try {
        log.info("onMessage(Message) - receiving a message");
        onMessage(((TextMessage) message).getText());
      } catch (JMSException e) {
        log.error("failed to extract text from TextMessage", e);
      }
    }
  }

  private void onMessage (String xml) {
    Object message = converter.fromXML(xml);
    log.info("onMessage(String) - received message of type " + message.getClass().getSimpleName());
    brokerProxy.routeMessage(message);
  }
}
