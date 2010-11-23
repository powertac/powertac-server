package org.powertac.integration.gateway;


import org.springframework.integration.annotation.Gateway;

import javax.jms.Message;

public interface JmsGateway {

  @Gateway
  public void receiveJmsMessage(Message message);
}
