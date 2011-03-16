package org.powertac.server

//import org.springframework.integration.Message

class BrokerManagementService {

  static transactional = true

  public void sendMessage(message) {
    //TODO: implement jms logic that sends this particular message to once single broker
  }

  public void broadcastMessage(message) {
    //TODO: implement jms logic that sends this particular message to all brokers
  }
}
