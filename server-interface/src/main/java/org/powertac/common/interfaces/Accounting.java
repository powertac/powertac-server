/*
 * Copyright 2009-2016 the original author or authors. Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.common.interfaces;

import java.util.List;
import java.util.Map;

import java.time.Instant;
import org.powertac.common.BalancingTransaction;
import org.powertac.common.Broker;
import org.powertac.common.CapacityTransaction;
import org.powertac.common.CustomerInfo;
import org.powertac.common.DistributionTransaction;
import org.powertac.common.MarketTransaction;
import org.powertac.common.Tariff;
import org.powertac.common.TariffTransaction;
import org.powertac.common.Timeslot;
import org.powertac.common.msg.BalancingControlEvent;

/**
 * Common interface for the PowerTAC accounting service. The accouting service
 * module is responsible for bookeeping, with respect to cash (broker bank
 * accounts) and market positions on behalf of brokers. All activities that
 * could potentially affect cash or market positions must be recorded by making
 * calls on the Accounting API. Summaries of accounts, along with all new
 * transactions, are sent to brokers at the end of each timeslot.
 * <p>
 * Accounting is also a TimeslotPhaseProcessor, running in the last phase of the
 * simulation cycle. All processors that can generate transactions must run in
 * earlier phases.
 * </p>
 * 
 * @author John Collins
 */
public interface Accounting 
{
  /**
   * Adds a market transaction that includes both a cash component and a product
   * commitment for a specific timeslot.
   */
  public MarketTransaction addMarketTransaction (Broker broker, Timeslot timeslot,
      double price, double mWh);

  /**
   * Adds a tariff transaction to the current-hour transaction list.
   */
  public TariffTransaction
  addTariffTransaction (TariffTransaction.Type txType, Tariff tariff,
                        CustomerInfo customer, int customerCount,
                        double kWh, double charge);

  /**
   * Adds a tariff transaction representing a curtailment or balancing action
   * to the current-hour transaction list.
   */
  public TariffTransaction
  addRegulationTransaction (Tariff tariff, CustomerInfo customer,
                            int customerCount,
                            double kWh, double charge);

  /**
   * Adds a distribution transaction to represent charges for customer
   * connections and energy transport.
   */
  public DistributionTransaction
  addDistributionTransaction (Broker broker, int nSmall, int nLarge,
                              double transport,
                              double distroCharge);

  /**
   * Adds a capacity transaction to represent charges for contribution to
   * a demand peak. One is generated for each peak event.
   * @param broker
   * @param peakTimeslot when the peak occurred
   * @param threshold    assessment threshold
   * @param kWh          total net demand among broker's customers during timeslot
   * @param fee          assessed capacity fee for this peak event
   */
  public CapacityTransaction
  addCapacityTransaction (Broker broker, int peakTimeslot,
                          double threshold, double kWh, double fee);

  /**
   * Adds a balancing transaction to represent the cost of imbalance
   */
  public BalancingTransaction addBalancingTransaction (Broker broker,
                                                       double imbalance,
                                                       double charge);

  /**
   * Updates a broker's cash position resulting from the Balancing Market
   * payment for Demand Response exercise.
   */
  public void postBalancingControl (BalancingControlEvent bce);

  /**
   * Returns the current net load represented by unprocessed TariffTransactions
   * for a specific Broker. This is needed to run the balancing process.
   */
  public double getCurrentNetLoad (Broker broker);
  
  /**
   * Returns a mapping of brokers to total supply and demand among subscribed
   * customers.
   */
  public Map<Broker, Map<TariffTransaction.Type, Double>>
  getCurrentSupplyDemandByBroker ();

  /**
   * Returns the market position for the current timeslot for a given broker.
   * Needed to run the balancing process.
   */
  public double getCurrentMarketPosition (Broker broker);
  
  /**
   * Returns the list of pending tariff transactions for the current timeslot.
   * List will be non-empty only after the customer models have begun reporting
   * tariff transactions, and before the accounting service has run.
   */
  public List<TariffTransaction> getPendingTariffTransactions ();
  
  /**
   * Runs the accounting process. This needs to be here to support some tests
   */
  public void activate(Instant time, int phase);
}
