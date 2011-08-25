/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.common.enumerations;

@Deprecated
public enum TariffState {
  /**
   * Tariff is publicly visible to brokers & consumers
   */
  Published(1),

  /**
   * Previously published tariff is revoked by broker -> no longer visible, no longer subscribable / negotiatiable to new customers
   */
  Revoked(2),

  /**
   * Specific tariff instance is currently (and privately) in negotiation between one customer and one broker
   */
  InNegotiation(3),

  /**
   * Specific tariff instance which one broker and one customer agreed upon (might be running already startDate <= now)
   * or in future (startDate > now)
   */
  Subscribed(4),

  /**
   * Tariff endDate < now; tariff is no longer valid and only kept in history for reference
   */
  Finished(5),

  /**
   * Tariff was exited ahead of agreed contract end by customer
   */
  EarlyCustomerExit(6),

  /**
   * Broker and Customer started negotiating on an individual tariff agreement but eventually aborted the negotiation
   */
  NegotiationAborted(7);

  private final int idVal;

  TariffState(int id) {
    this.idVal = id;
  }

  public int getId() {
    return idVal;
  }
}
