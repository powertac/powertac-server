package org.powertac.server.core;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;

public class Main {

  public static void main(String[] args) {
    String [] contextPaths = {"/META-INF/spring/applicationContext.xml","/META-INF/spring/applicationContext-integration.xml", "/META-INF/spring/applicationContext-integration-tariff-revoke.xml"};
    ApplicationContext context = new ClassPathXmlApplicationContext(contextPaths);
    MessageChannel channel = context.getBean("incomingMessageChannel", MessageChannel.class);
    Message<String> msg = MessageBuilder.withPayload("<?xml version=\"1.0\" encoding=\"UTF-8\"?><revokeTariffCommand><authToken>MyBrokerAuthToken</authToken><tariffId>2</tariffId></revokeTariffCommand>").build();
    channel.send(msg);
  }

}
