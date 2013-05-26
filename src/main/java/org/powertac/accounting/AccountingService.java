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
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.*;
import org.powertac.common.TariffTransaction.Type;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.*;
import org.powertac.common.msg.DistributionReport;
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
  private TransactionFactory txFactory;
  
  @Autowired
  private ServerConfiguration serverProps;

  private ArrayList<BrokerTransaction> pendingTransactions;
  private DistributionReport distributionReport;
  private HashMap<Timeslot, ArrayList<MarketTransaction>>
      pendingMarketTransactions;

  // read this from configuration
  
  @ConfigurableValue(valueType = "Double",
          description = "low end of bank interest rate range")
  private double minInterest = 0.04;

  @ConfigurableValue(valueType = "Double",
      description = "high end of bank interest rate range")
  private double maxInterest = 0.12;
  
  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "override random setting of bank interest rate")
  private Double bankInterest = null;

  public AccountingService ()
  {
    super();
    pendingTransactions = new ArrayList<BrokerTransaction>();
    pendingMarketTransactions =
            new HashMap<Timeslot, ArrayList<MarketTransaction>>();
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
    bankInterest = null;
    serverProps.configureMe(this);

    RandomSeed random =
        randomSeedService.getRandomSeed("AccountingService",
                                        0l, "interest");
    if (bankInterest == null) {
      // interest will be non-null in case it was overridden in the config
      bankInterest = (minInterest +
                           (random.nextDouble() *
                               (maxInterest - minInterest)));
      log.info("bank interest: " + bankInterest);
    }
    serverProps.publishConfiguration(this);
    return "AccountingService";
  }

  @Override
  public synchronized MarketTransaction 
  addMarketTransaction(Broker broker,
                       Timeslot timeslot,
                       double mWh,
                       double price) 
  {
    MarketTransaction mtx = 
            txFactory.makeMarketTransaction(broker, timeslot, mWh, price);

    // post pending tx so it gets sent to broker
    pendingTransactions.add(mtx);
    updateBrokerMarketPosition(mtx);

    // defer posting to delivery timeslot
    ArrayList<MarketTransaction> theList =
            pendingMarketTransactions.get(timeslot);
    if (null == theList) {
      theList = new ArrayList<MarketTransaction>();
      pendingMarketTransactions.put(timeslot, theList);
    }
    theList.add(mtx);
    return mtx;
  }

  @Override
  public synchronized TariffTransaction 
  addTariffTransaction(TariffTransaction.Type txType,
                       Tariff tariff,
                       CustomerInfo customer,
                       int customerCount,
                       double kWh,
                       double charge) 
  {
    TariffTransaction ttx =
            txFactory.makeTariffTransaction(tariff.getBroker(), txType, 
                                            tariffRepo.findSpecificationById(tariff.getSpecId()),
                                            customer, customerCount,
                                            kWh, charge);
    if (null == ttx.getTariffSpec())
      log.error("Null tariff spec in addTariffTx()");
    pendingTransactions.add(ttx);
    return ttx;
  }

  @Override
  public synchronized DistributionTransaction 
  addDistributionTransaction(Broker broker,
                             double kWh,
                             double charge) 
  {
    DistributionTransaction dtx =
            txFactory.makeDistributionTransaction(broker, kWh, charge);
    pendingTransactions.add(dtx);
    return dtx;
  }

  @Override
  public synchronized BalancingTransaction 
  addBalancingTransaction(Broker broker, double kWh, double charge)
  {
    BalancingTransaction btx =
            txFactory.makeBalancingTransaction(broker, kWh, charge);
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
  @Override
  public synchronized double getCurrentNetLoad (Broker broker) 
  {
    double netLoad = 0.0;
    for (BrokerTransaction btx : pendingTransactions) {
      if (btx instanceof TariffTransaction) {
        TariffTransaction ttx = (TariffTransaction)btx;
        if (ttx.getBroker().getUsername().equals(broker.getUsername())) {
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
   * Returns a mapping of brokers to total supply and demand among subscribed
   * customers.
   */
  @Override
  public Map<Broker, Map<Type, Double>> getCurrentSupplyDemandByBroker ()
  {
    HashMap<Broker, Map<Type, Double>> result =
            new HashMap<Broker, Map<Type, Double>>();
    for (BrokerTransaction btx : pendingTransactions) {
      if (btx instanceof TariffTransaction) {
        TariffTransaction ttx = (TariffTransaction)btx;
        Broker broker = ttx.getBroker();
        Map<Type, Double> record = result.get(broker);
        if (null == record) {
          record = new HashMap<Type, Double>();
          result.put(broker, record);
          record.put(Type.CONSUME, 0.0);
          record.put(Type.PRODUCE, 0.0);
        }
        if (ttx.getTxType() == Type.CONSUME)
          record.put(Type.CONSUME, record.get(Type.CONSUME) + ttx.getKWh());
        else if (ttx.getTxType() == Type.PRODUCE)
          record.put(Type.PRODUCE, record.get(Type.PRODUCE) + ttx.getKWh());
      }
    }
    return result;
  }

  /**
   * Gets the net market position for the current timeslot. This only works on
   * processed transactions, but it can be used before activation in case there
   * can be no new market transactions for the current timeslot. This is the
   * normal case. The value will be positive if the broker is importing power
   * during the current timeslot.
   */

  @Override
  public synchronized double getCurrentMarketPosition(Broker broker) 
  {
    Timeslot current = timeslotRepo.currentTimeslot();
    log.debug("current timeslot: " + current.getSerialNumber());
    MarketPosition position =
        broker.findMarketPositionByTimeslot(current.getSerialNumber());
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
  @Override
  public void activate(Instant time, int phaseNumber) 
  {
    log.info("Activate: " + pendingTransactions.size() + " messages");
    HashMap<Broker, List<Object>> brokerMsg = new HashMap<Broker, List<Object>>();
    for (Broker broker : brokerRepo.list()) {
      brokerMsg.put(broker, new ArrayList<Object>());
    }
    // initialize the distribution report
    distributionReport = new DistributionReport();
    
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
    // handle the backed-up mkt transactions for this timeslot
    handleMarketTransactionsForTimeslot(timeslotRepo.currentTimeslot());
    // for each broker, compute interest and send messages
    double rate = bankInterest / 365.0;
    for (Broker broker : brokerRepo.list()) {
      // run interest payments at midnight
      if (timeService.getHourOfDay() == 0) {
        double brokerRate = rate;
        double cash = broker.getCashBalance();
        if (cash >= 0.0) {
          // rate on positive balance is 1/2 of negative
          brokerRate /= 2.0;
        }
        double interest = cash * brokerRate;
        brokerMsg.get(broker).add(txFactory.makeBankTransaction(broker, interest));
        broker.updateCash(interest);
      }
      // add the cash position to the list and send messages
      brokerMsg.get(broker).add(txFactory.makeCashPosition(broker, broker.getCashBalance()));
      log.info("Sending " + brokerMsg.get(broker).size() + " messages to " + broker.getUsername());
      brokerProxyService.sendMessages(broker, brokerMsg.get(broker));
    }
    // send the distribution report
    brokerProxyService.broadcastMessage(distributionReport);
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
    // update the distribution report
    if (TariffTransaction.Type.CONSUME == tx.getTxType())
      distributionReport.addConsumption(-tx.getKWh());
    else if (TariffTransaction.Type.PRODUCE == tx.getTxType())
      distributionReport.addProduction(tx.getKWh());
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
  
  // process market transaction by sending update market position.
  // actual transaction posting is deferred to delivery time
  public void processTransaction(MarketTransaction tx,
                                 ArrayList<Object> messages) {
    MarketPosition mkt =
        tx.getBroker().findMarketPositionByTimeslot(tx.getTimeslotIndex());
    if (!messages.contains(mkt))
      messages.add(mkt);
  }
  
  // process deferred market transactions for the current timeslot
  public void handleMarketTransactionsForTimeslot(Timeslot ts) 
  {
    ArrayList<MarketTransaction> pending = pendingMarketTransactions.get(ts);
    if (null == pending)
      return;
    for (MarketTransaction tx : pending) {
      Broker broker = tx.getBroker();
      updateCash(broker, tx.getPrice() * Math.abs(tx.getMWh()));
    }
  }

  // pre-process a market transaction
  public void updateBrokerMarketPosition(MarketTransaction tx) 
  {
    Broker broker = tx.getBroker();
    MarketPosition mkt =
        broker.findMarketPositionByTimeslot(tx.getTimeslotIndex());
    if (mkt == null) {
      mkt = new MarketPosition(broker, tx.getTimeslot(), tx.getMWh());
      log.debug("New MarketPosition(" + broker.getUsername() + 
                ", " + tx.getTimeslot().getSerialNumber() + "): " + 
                mkt.getId());
      broker.addMarketPosition(mkt, tx.getTimeslotIndex());
    }
    else {
      mkt.updateBalance(tx.getMWh());
    }
  }

  private void updateCash(Broker broker, double amount) 
  {
    broker.updateCash(amount);
  }

  public void processTransaction (BankTransaction tx,
                                  ArrayList<Object> messages)
  {
    log.error("tx " + tx.toString() + " calls processTransaction - should not happen");   
  }
  
  /**
   * Returns the current list of pending tariff transactions. This will be
   * non-empty only after the customer model has run and before accounting
   * has run in the current timeslot.
   */
  @Override
  public synchronized List<TariffTransaction> getPendingTariffTransactions ()
  {
    List<TariffTransaction> result = new ArrayList<TariffTransaction>();
    for (BrokerTransaction tx : pendingTransactions) {
      if (tx instanceof TariffTransaction)
        result.add((TariffTransaction)tx);
    }
    return result;
  }

  // test support
  List<BrokerTransaction> getPendingTransactions ()
  {
    return pendingTransactions;
  }

  public double getMinInterest ()
  {
    return minInterest;
  }

  public double getMaxInterest ()
  {
    return maxInterest;
  }

  public Double getBankInterest ()
  {
    return bankInterest;
  }
  
  // test support
  void setBankInterest (Double interest)
  {
    bankInterest = interest;
  }
}
