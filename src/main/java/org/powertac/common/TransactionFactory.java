/*
 * Copyright (c) 2013 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.common;

import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Constructs transaction objects in a way that guarantees that the correct
 * data goes into the state log, without requiring unnecessary couplings
 * on the part of transaction sources.
 * 
 * Each method constructs a new transaction for the given broker and
 * arguments and returns it. 
 * There is no attempt to cache them or look them up.
 * 
 * @author John Collins
 */
@Scope("singleton")
@Service
public class TransactionFactory
{
  @Autowired
  private TimeslotRepo timeslotRepo;

  private int getTimeslotIndex ()
  {
    return timeslotRepo.currentSerialNumber();
  }

  public BankTransaction makeBankTransaction (Broker broker, double amount)
  {
    return new BankTransaction(broker, amount, getTimeslotIndex());
  }

  public BalancingTransaction
  makeBalancingTransaction (Broker broker, double kWh, double charge)
  {
    return new BalancingTransaction(broker, getTimeslotIndex(),
                                    kWh, charge);
  }

  public CashPosition makeCashPosition (Broker broker, double balance)
  {
    return new CashPosition(broker, balance, getTimeslotIndex());
  }

  public DistributionTransaction
  makeDistributionTransaction (Broker broker, int nSmall, int nLarge,
                                 double transport, double distroCharge)
  {
    return new DistributionTransaction(broker, getTimeslotIndex(),
                                       nSmall, nLarge, transport,
                                       distroCharge);
  }

  public MarketTransaction
  makeMarketTransaction (Broker broker, Timeslot timeslot,
                         double mWh, double price)
  {
    return new MarketTransaction(broker, getTimeslotIndex(),
                                 timeslot, mWh, price);
  }

  /**
   * Creates a tariff transaction that is not a regulation transaction.
   */
  public TariffTransaction
  makeTariffTransaction(Broker broker, TariffTransaction.Type txType,
                        TariffSpecification spec,
                        CustomerInfo customer,
                        int customerCount,
                        double kWh, double charge)
  {
    return new TariffTransaction (broker, getTimeslotIndex(),
                                  txType, spec, customer,
                                  customerCount, kWh, charge, false);
  }

  /**
   * Creates a tariff transaction that could be a regulation transaction,
   * depending on the value of the isRegulation parameter.
   */
  public TariffTransaction
  makeTariffTransaction(Broker broker, TariffTransaction.Type txType,
                        TariffSpecification spec,
                        CustomerInfo customer,
                        int customerCount,
                        double kWh, double charge,
                        boolean isRegulation)
  {
    return new TariffTransaction (broker, getTimeslotIndex(),
                                  txType, spec, customer,
                                  customerCount, kWh, charge, isRegulation);
  }

  public CapacityTransaction
  makeCapacityTransaction (Broker broker,
                           int peakTimeslot, double threshold,
                           double kWh, double fee)
  {
    return new CapacityTransaction(broker, getTimeslotIndex(),
                                   peakTimeslot, threshold, kWh, fee);
  }
}
