/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.du;

import static org.powertac.util.MessageDispatcher.dispatch;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.MarketPosition;
import org.powertac.common.PluginConfig;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.state.StateChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Default broker implementation. We do the implementation in a service, because
 * the default broker is a singleton and it's convenient. The actual Broker
 * instance is implemented in an inner class.
 * @author John Collins
 */
@Service
public class DefaultBrokerService 
implements TimeslotPhaseProcessor
{
  static private Logger log = Logger.getLogger(DefaultBrokerService.class.getName());

  @Autowired
  private CompetitionControl competitionControlService;
  
  @Autowired
  private TariffMarket tariffMarketService;
  
  @Autowired
  private TimeService timeService;
  
  private LocalBroker face;
  
  /** parameters */
  private double defaultConsumptionRate = 1.0;
  private double defaultProductionRate = -0.01;
  
  /** when to invoke this service in per-timeslot processing -- we use
   * phase 1, which is after both the market and accounting run in the
   * previous timeslot, and bids/asks are cleared in the current timeslot. */
  private int simulationPhase = 3;
  
  // local state
  private TariffSpecification defaultConsumption;
  private TariffSpecification defaultProduction;
  private HashMap<TariffSpecification, 
                  HashMap<CustomerInfo, CustomerRecord>> customerSubscriptions;
    
  /**
   * Default constructor, called once when the server starts, before
   * any application-specific initialization has been done.
   */
  public DefaultBrokerService ()
  {
    super();
  }
  
  /**
   * Called by initialization service once at the beginning of each game.
   * Sets up and publishes default tariffs.
   * Question: when do customers subscribe to default tariff?
   */
  void init (PluginConfig config)
  {
    // set up local state
    customerSubscriptions = new HashMap<TariffSpecification,
                                        HashMap<CustomerInfo, CustomerRecord>>();
    
    // create and publish default tariffs
    double consumption = (config.getDoubleValue("consumptionRate",
                                                defaultConsumptionRate));
    defaultConsumption = new TariffSpecification(face, PowerType.CONSUMPTION)
        .addRate(new Rate().withValue(consumption));
    tariffMarketService.setDefaultTariff(defaultConsumption);
    customerSubscriptions.put(defaultConsumption,
                              new HashMap<CustomerInfo, CustomerRecord>());

    double production = (config.getDoubleValue("productionRate",
                                                defaultProductionRate));
    defaultProduction = new TariffSpecification(face, PowerType.PRODUCTION)
        .addRate(new Rate().withValue(production));
    tariffMarketService.setDefaultTariff(defaultProduction);
    customerSubscriptions.put(defaultProduction,
                              new HashMap<CustomerInfo, CustomerRecord>());
    
    // register for activation
    competitionControlService.registerTimeslotPhase(this, simulationPhase);
  }
  
  /**
   * Creates the internal Broker instance that can receive messages intended
   * for local Brokers. It would be a Really Bad Idea to call this at any time
   * other than during the pre-game phase of a competition, because this method
   * does not register the broker in the BrokerRepo, which is a requirement to
   * see the messages.
   */
  public Broker createBroker (String username)
  {
    face = new LocalBroker(username);
    return face;
  }

  // ----------- per-timeslot activation -------------
  /**
   * In each timeslot, we must trade in the wholesale market to satisfy the
   * predicted load of our current customer base.
   */
  public void activate (Instant time, int phaseNumber)
  {
    // TODO Implement per-timeslot behavior
  }

  // ------------ process incoming messages -------------
  /**
   * Incoming messages for brokers include:
   * <ul>
   * <li>Competition, containing a list of Customers and their attributes</li>
   * <li>CustomerBootstrapData that tells us how much power the various
   *   customer groups can be expected to use</li>
   * <li>Orderbooks and ClearedTrades that tell us the state of the
   *   wholesale market</li>
   * <li>TariffTransactions that tell us about customer subscription
   *   activity and power usage</li>
   * <li>MarketTransactions that tell us how much power we have bought
   *   or sold</li>
   * </ul>
   */
  public void receiveBrokerMessage (Object msg)
  {
    if (msg != null)
    {
      dispatch(this, "handleMessage", msg);
    }
  }
  
  /**
   * Handles a TariffTransaction. We only care about certain types: PRODUCE,
   * CONSUME, SIGNUP, and WITHDRAW.
   */
  @SuppressWarnings("unused")
  private void handleMessage(TariffTransaction ttx)
  {
    TariffTransaction.Type txType = ttx.getTxType();
    CustomerInfo customer = ttx.getCustomerInfo();
    HashMap<CustomerInfo, CustomerRecord> customerMap = 
      customerSubscriptions.get(ttx.getTariffSpec());
    CustomerRecord record = customerMap.get(customer);
    
    if (TariffTransaction.Type.SIGNUP == txType) {
      // keep track of customer counts
      if (record == null) {
        customerMap.put(customer, 
                        new CustomerRecord(customer, ttx.getCustomerCount()));
      }
      else {
        record.signup(ttx.getCustomerCount());
      }
    }
    else if (TariffTransaction.Type.WITHDRAW == txType) {
      // customers presumably found a better deal
      if (customerMap.get(customer) == null) {
        // should not happen
        log.warn("unknown customer withdraws subscription");
      }
      else {
        record.withdraw(ttx.getCustomerCount());
      }
    }
    else if (TariffTransaction.Type.PRODUCE == txType) {
      // if ttx count and subscribe population don't match, it will be hard
      // to estimate per-individual production
      if (ttx.getCustomerCount() != record.subscribedPopulation) {
        log.warn("production by subset " + ttx.getCustomerCount() +
                 " of subscribed population " + record.subscribedPopulation);
      }
      record.produceConsume(ttx.getKWh(), ttx.getPostedTime());
    }
    else if (TariffTransaction.Type.CONSUME == txType) {
      if (ttx.getCustomerCount() != record.subscribedPopulation) {
        log.warn("consumption by subset " + ttx.getCustomerCount() +
                 " of subscribed population " + record.subscribedPopulation);
      }
      record.produceConsume(ttx.getKWh(), ttx.getPostedTime());      
    }
  }

  /**
   * Here's the actual "default broker". This is needed to intercept messages
   * sent to the broker.
   */
  class LocalBroker extends Broker
  {
    public LocalBroker (String username)
    {
      super(username);
      setLocal(true);
    }

    /**
     * Receives a message intended for the broker, forwards it to the
     * message handler in the enclosing service.
     */
    public void receiveMessage(Object object) 
    {
      receiveBrokerMessage(object);
    }
  }
  
  /**
   * Keeps track of customer status
   */
  class CustomerRecord
  {
    CustomerInfo customer;
    int subscribedPopulation = 0;
    double[] usage = new double[7*24];
    Instant base = null;
    double alpha = 0.3;
    
    CustomerRecord (CustomerInfo customer, int population)
    {
      super();
      this.customer = customer;
      this.subscribedPopulation = population;
    }
    
    // Adds new individuals to the count
    void signup (int population)
    {
      subscribedPopulation += population;
    }
    
    // Removes individuals from the count
    void withdraw (int population)
    {
      subscribedPopulation -= population;
    }
    
    // Customer produces or consumes power. We assume the kwh value is negative
    // for production, positive for consumption
    void produceConsume (double kwh, Instant when)
    {
      int index = getIndex(when);
      double oldUsage = usage[index];
      if (oldUsage == 0.0) {
        // assume this is the first time
        usage[index] = kwh;
      }
      else {
        // exponential smoothing
        usage[index] = alpha * kwh / (double)subscribedPopulation
                       + (1.0 - alpha) * oldUsage;
      }
    }
    
    private int getIndex (Instant when)
    {
      if (base == null)
        base = when;
      int result = (int)((when.getMillis() - base.getMillis()) /
                         (Competition.currentCompetition().getTimeslotLength() * 
                          TimeService.MINUTE));
      result = result % usage.length;
      log.debug("index=" + result + " at time " + when.toString());
      return result;
    }
  }
}
