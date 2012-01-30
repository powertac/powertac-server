/*
 * Copyright 2010-2011 the original author or authors.
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

package org.powertac.accounting;

import static org.powertac.util.MessageDispatcher.dispatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.*;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link org.powertac.common.interfaces.Accounting}
 * @author John Collins
 */
@Service
public class AccountingService
  extends TimeslotPhaseProcessor 
  implements Accounting, InitializationService
{
  static private Logger log = Logger.getLogger(AccountingService.class.getName());

  @Autowired
  private TimeService timeService;
  
  @Autowired
  private TariffRepo tariffRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private BrokerProxy brokerProxyService;

  @Autowired
  private RandomSeedRepo randomSeedService;
  
  @Autowired
  private ServerConfiguration serverProps;

  private ArrayList<BrokerTransaction> pendingTransactions;

  // read this from configuration
  
  private double minInterest = 0.04;
  private double maxInterest = 0.12;
  private Double bankInterest = null;

  private int simulationPhase = 3;

  public AccountingService ()
  {
    super();
    pendingTransactions = new ArrayList<BrokerTransaction>();
  }

  @Override
  public void setDefaults ()
  {    
  }

  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    pendingTransactions.clear();
    super.init();
    serverProps.configureMe(this);

    RandomSeed random =
        randomSeedService.getRandomSeed("AccountingService",
                                        0l, "interest");
    if (bankInterest == null) {
      // interest will be non-null in case it was overridden in the config
      this.setBankInterest(minInterest +
                           (random.nextDouble() *
                               (maxInterest - minInterest)));
      log.info("bank interest: " + bankInterest);
    }
    return "AccountingService";
  }
  
  /**
   * Sets parameters, registers for timeslot phase activation.
   */
  public void init(PluginConfig config) 
  {
  }

  public synchronized MarketTransaction 
  addMarketTransaction(Broker broker,
                       Timeslot timeslot,
                       double mWh,
                       double price) 
  {
    MarketTransaction mtx = new MarketTransaction(broker, timeService.getCurrentTime(),
                                                  timeslot, mWh, price);
    pendingTransactions.add(mtx);
    return mtx;
  }

  public synchronized TariffTransaction 
  addTariffTransaction(TariffTransaction.Type txType,
                       Tariff tariff,
                       CustomerInfo customer,
                       int customerCount,
                       double kWh,
                       double charge) 
  {
    TariffTransaction ttx = new TariffTransaction(tariff.getBroker(),
                                                  timeService.getCurrentTime(), txType, 
                                                  tariffRepo.findSpecificationById(tariff.getSpecId()),
                                                  customer, customerCount,
                                                  kWh, charge);
    pendingTransactions.add(ttx);
    return ttx;
  }

  public synchronized DistributionTransaction 
  addDistributionTransaction(Broker broker,
                             double kWh,
                             double charge) 
  {
    DistributionTransaction dtx = new DistributionTransaction(broker, 
                                                              timeService.getCurrentTime(), 
                                                              kWh, charge);
    pendingTransactions.add(dtx);
    return dtx;
  }

  public synchronized BalancingTransaction 
  addBalancingTransaction(Broker broker,
                          double kWh,
                          double charge) 
  {
    BalancingTransaction btx = new BalancingTransaction(broker,
                                                        timeService.getCurrentTime(),
                                                        kWh, charge);
    pendingTransactions.add(btx);
    return btx;
  }

  /**
   * Returns the net load for the given broker in the current timeslot.
   * Note that this only works AFTER the customer models have run, and
   * BEFORE the day's transactions have been processed. The value will be
   * negative if the broker's customers are consuming more than they produce
   * in the current timeslot.
   */
  public synchronized double getCurrentNetLoad (Broker broker) 
  {
    double netLoad = 0.0;
    for (BrokerTransaction btx : pendingTransactions) {
      if (btx instanceof TariffTransaction) {
        TariffTransaction ttx = (TariffTransaction)btx;
        if (ttx.getBroker().getUsername() == broker.getUsername()) {
          if (ttx.getTxType() == TariffTransaction.Type.CONSUME ||
              ttx.getTxType() == TariffTransaction.Type.PRODUCE) {
            netLoad += ttx.getKWh();
          }
        }
      }
    }
    log.info("net load for " + broker.getUsername() + ": " + netLoad);
    return netLoad;
  }

  /**
   * Gets the net market position for the current timeslot. This only works on
   * processed transactions, but it can be used before activation in case there
   * can be no new market transactions for the current timeslot. This is the
   * normal case. The value will be positive if the broker is importing power
   * during the current timeslot.
   */

  public synchronized double getCurrentMarketPosition(Broker broker) 
  {
    Timeslot current = timeslotRepo.currentTimeslot();
    log.debug("current timeslot: " + current.getSerialNumber());
    MarketPosition position =
        broker.findMarketPositionByTimeslot(current);
    if (position == null) {
      log.debug("null position for ts " + current.getSerialNumber());
      return 0.0;
    }
    log.info("market position for " + broker.getUsername()
             + ": " + position.getOverallBalance());
    return position.getOverallBalance();
  }

  /**
   * Processes the pending transaction list, computes interest, sends 
   * updates to brokers
   */
  public void activate(Instant time, int phaseNumber) 
  {
    log.info("Activate: " + pendingTransactions.size() + " messages");
    HashMap<Broker, List<Object>> brokerMsg = new HashMap<Broker, List<Object>>();
    for (Broker broker : brokerRepo.list()) {
      brokerMsg.put(broker, new ArrayList<Object>());
    }
    // walk through the pending transactions and run the updates
    for (BrokerTransaction tx : getPendingTransactionList()) {
      // need to refresh the transaction first
      if (tx.getBroker() == null) {
        log.error("tx " + tx.getClass().getName() + ":" + tx.getId() + 
                  " has null broker");
      }
      if (brokerMsg.get(tx.getBroker()) == null) {
        log.error("tx " + tx.getClass().getName() + ":" + tx.getId() + 
                  " has unknown broker " + tx.getBroker().getUsername());
      }
      brokerMsg.get(tx.getBroker()).add(tx);
      // process transactions by method lookup
      dispatch(this, "processTransaction", 
               tx, brokerMsg.get(tx.getBroker()));
    }
    // for each broker, compute interest and send messages
    double rate = bankInterest / 365.0;
    for (Broker broker : brokerRepo.list()) {
      // run interest payments at midnight
      if (timeService.getHourOfDay() == 0) {
        double brokerRate = rate;
        CashPosition cash = broker.getCash();
        if (cash.getBalance() >= 0.0) {
          // rate on positive balance is 1/2 of negative
          brokerRate /= 2.0;
        }
        double interest = cash.getBalance() * brokerRate;
        brokerMsg.get(broker).add(new BankTransaction(broker, interest,
                                                      timeService.getCurrentTime()));
        cash.deposit(interest);
      }
      // add the cash position to the list and send messages
      brokerMsg.get(broker).add(broker.getCash());
      log.info("Sending " + brokerMsg.get(broker).size() + " messages to " + broker.getUsername());
      brokerProxyService.sendMessages(broker, brokerMsg.get(broker));
    }
  }
  
  /**
   * Copies out the pending transaction list with concurrency protection,
   * clears the pending transaction list, and returns the copy.
   */
  private synchronized List<BrokerTransaction> getPendingTransactionList ()
  {
    ArrayList<BrokerTransaction> result = 
      new ArrayList<BrokerTransaction>(pendingTransactions);
    pendingTransactions.clear();
    return result;
  }

  // process a tariff transaction
  public void processTransaction(TariffTransaction tx,
                                 ArrayList<Object> messages) {
    //log.info("processing tariff tx " + tx.toString());
    updateCash(tx.getBroker(), tx.getCharge());
  }

  // process a balance transaction
  public void processTransaction(BalancingTransaction tx,
                                 ArrayList<Object> messages) {
    updateCash(tx.getBroker(), tx.getCharge());
  }

  // process a DU fee transaction
  public void processTransaction(DistributionTransaction tx,
                                 ArrayList<Object> messages) {
    updateCash(tx.getBroker(), tx.getCharge());
  }

  // process a market transaction
  public void processTransaction(MarketTransaction tx,
                                 ArrayList<Object> messages) 
  {
    Broker broker = tx.getBroker();
    updateCash(broker, tx.getPrice() * Math.abs(tx.getMWh()));
    MarketPosition mkt =
        broker.findMarketPositionByTimeslot(tx.getTimeslot());
    if (mkt == null) {
      mkt = new MarketPosition(broker, tx.getTimeslot(), tx.getMWh());
      log.debug("New MarketPosition(" + broker.getUsername() + 
                ", " + tx.getTimeslot().getSerialNumber() + "): " + 
                mkt.getId());
      broker.addMarketPosition(mkt, tx.getTimeslot());
      messages.add(mkt);
    }
    else {
      mkt.updateBalance(tx.getMWh());
    }
  }

  private void updateCash(Broker broker, double amount) 
  {
    CashPosition cash = broker.getCash();
    cash.deposit(amount);
  }

  public void processTransaction (BankTransaction tx,
                                  ArrayList<Object> messages)
  {
    log.error("tx " + tx.toString() + " calls processTransaction - should not happen");   
  }
  
  // test-support code
  List<BrokerTransaction> getPendingTransactions ()
  {
    return pendingTransactions;
  }
  
  int getSimulationPhase ()
  {
    return simulationPhase;
  }

  public double getMinInterest ()
  {
    return minInterest;
  }

  @ConfigurableValue(valueType = "Double",
      description = "low end of bank interest rate range")
  public void setMinInterest (double minInterest)
  {
    this.minInterest = minInterest;
  }

  public double getMaxInterest ()
  {
    return maxInterest;
  }

  @ConfigurableValue(valueType = "Double",
      description = "high end of bank interest rate range")
  public void setMaxInterest (double maxInterest)
  {
    this.maxInterest = maxInterest;
  }

  public Double getBankInterest ()
  {
    return bankInterest;
  }
  
  @ConfigurableValue(valueType = "Double",
      description = "override random setting of bank interest rate")
  public void setBankInterest (Double value)
  {
    bankInterest = value;
  }
}
