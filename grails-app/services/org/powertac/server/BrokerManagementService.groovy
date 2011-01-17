package org.powertac.server

import org.springframework.integration.Message

class BrokerManagementService {

  static transactional = true

  public void sendMessage(Message message) {
    //TODO: implement jms logic that sends this particular message to once single broker
  }

  public void broadcastMessage(Message message) {
    //TODO: implement jms logic that sends this particular message to all brokers
  }
}
