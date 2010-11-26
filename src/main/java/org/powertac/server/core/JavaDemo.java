package org.powertac.server.core;

import org.powertac.server.core.domain.Broker;

public class JavaDemo {
  public static void main(String[] args) {
    Broker.findBrokersByUsernameEquals("test");
  }
}
