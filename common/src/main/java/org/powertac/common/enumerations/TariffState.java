package org.powertac.common.enumerations;

public enum TariffState {
    Published(1), //Tariff is publicly visible to brokers & consumers
    Revoked(2),                          //Previously published tariff is revoked by broker -> no longer visible, no longer subscribable / negotiatiable to new customers
    InNegotiation(3),                    //Specific tariff instance is currently (and privately) in negotiation between one customer and one broker
    Subscribed(4),                       //Specific tariff instance which one broker and one customer agreed upon (might be running already startDate <= now) or in future (startDate > now)
    Finished(5),                         //Tariff endDate < now; tariff is no longer valid and only kept in history
    NegotiationAborted(6);

  private final int idVal;

  TariffState(int id) {
    this.idVal = id;
  }

  public int getId() {
    return idVal;
  }
}
