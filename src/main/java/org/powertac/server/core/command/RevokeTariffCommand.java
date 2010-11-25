package org.powertac.server.core.command;

import org.powertac.server.core.domain.Broker;
import org.powertac.server.core.domain.Tariff;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="revokeTariffCommand")
public class RevokeTariffCommand {

  @XmlElement
  private String authToken;
  @XmlElement
  private Long tariffId;




  private RevokeTariffCommand() {
  }

  public String getAuthToken() {
    return authToken;
  }

  public Long getTariffId() {
    return tariffId;
  }

  public RevokeTariffCommand(String authToken, Long tariffId) {
    this.authToken = authToken;
    this.tariffId = tariffId;
  }
}
