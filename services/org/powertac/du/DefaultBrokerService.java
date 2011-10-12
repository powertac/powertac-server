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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.MarketPosition;
import org.powertac.common.PluginConfig;
import org.powertac.common.Rate;
import org.powertac.common.Shout;
import org.powertac.common.Shout.OrderType;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Default broker implementation. We do the implementation in a service, because
 * the default broker is a singleton and it's convenient. The actual Broker
 * instance is implemented in an inner class. Note that this is not a type of
 * TimeslotPhaseProcessor. It's a broker, and so it runs after the last message
 * of the timeslot goes out, the TimeslotUpdate message. As implemented, it runs
 * in the message-sending thread. If this turns out to cause problems with real
 * brokers, it could run in its own thread.
 * @author John Collins
 */
@Service
public class DefaultBrokerService 
{
  static private Logger log = Logger.getLogger(DefaultBrokerService.class.getName());
  
  @Autowired // routing of outgoing messages
  private BrokerProxy brokerProxyService;
  
  @Autowired // tariff publication
  private TariffMarket tariffMarketService;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  private LocalBroker face;
  
  /** parameters */
  private double defaultConsumptionRate = 1.0;
  private double defaultProductionRate = -0.01;
  private double initialBidKWh = 500.0;
  private double buyLimitPrice = 200.0;
  private double sellLimitPrice = 0.0;
  private int usageRecordLength = 7 * 24; // one week
  
  // local state
  private TariffSpecification defaultConsumption;
  private TariffSpecification defaultProduction;
  private HashMap<TariffSpecification, 
                  HashMap<CustomerInfo, CustomerRecord>> customerSubscriptions;
  private HashMap<Timeslot, MarketPosition> marketPositions;

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
    marketPositions = new HashMap<Timeslot, MarketPosition>();

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
    
    // Other setup parameters
    initialBidKWh = config.getDoubleValue("initialBidKWh", initialBidKWh);
    buyLimitPrice = config.getDoubleValue("buyLimitPrice", buyLimitPrice);
    sellLimitPrice = config.getDoubleValue("sellLimitPrice", sellLimitPrice);
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

  public LocalBroker getFace ()
  {
    return face;
  }

  // ----------- per-timeslot activation -------------
  /**
   * In each timeslot, we must trade in the wholesale market to satisfy the
   * predicted load of our current customer base.
   */
  public void activate ()
  {
    Timeslot current = timeslotRepo.currentTimeslot();
    
    // In the first timeslot, we buy a fixed amount because we have no
    // usage record.
    if (current.getSerialNumber() == 0) {
      for (Timeslot timeslot : timeslotRepo.enabledTimeslots()) {
        submitShout(initialBidKWh, timeslot);
      }
    }
    
    // In the second through 24th timeslot, we buy enough to meet what was
    // used in the previous timeslot. Note that this is called after the server
    // disables current+1 and enables current+24.
    else if (current.getSerialNumber() < 24) {
      // we already have usage data for the current timeslot.
      double currentKWh = collectUsage(current.getSerialNumber());
      double neededKWh = 0.0;
      for (Timeslot timeslot : timeslotRepo.enabledTimeslots()) {
        // use data already collected if we have it, otherwise use data from
        // the current timeslot.
        int index = timeslot.getSerialNumber() % 24;
        double historicalKWh = collectUsage(index);
        if (historicalKWh != 0.0)
          neededKWh = historicalKWh;
        else
          neededKWh = currentKWh;
        // subtract out the current market position, and we know what to
        // buy or sell
        submitShout(neededKWh, timeslot);
      }
    }
    
    // Once we have 24 hours of records, assume we need enough to meet 
    // what we used 24 hours earlier
    else if (current.getSerialNumber() < usageRecordLength) {
      double neededKWh = 0.0;
      for (Timeslot timeslot : timeslotRepo.enabledTimeslots()) {
        int index = timeslot.getSerialNumber() % 24;
        neededKWh = collectUsage(index);
        submitShout(neededKWh, timeslot);
      }      
    }
    
    // Finally, once we have a full week of records, we use the data for
    // the hour and day-of-week.
    else {
      double neededKWh = 0.0;
      for (Timeslot timeslot : timeslotRepo.enabledTimeslots()) {
        int index = timeslot.getSerialNumber() % usageRecordLength;
        neededKWh = collectUsage(index);
        submitShout(neededKWh, timeslot);
      }      
    }
  }

  // default visibility for testing
  double collectUsage (int index)
  {
    double result = 0.0;
    for (HashMap<CustomerInfo, CustomerRecord> customerMap : customerSubscriptions.values()) {
      for (CustomerRecord record : customerMap.values()) {
        result += record.getUsage(index);
      }
    }
    return result;
  }

  private void submitShout (double neededKWh, Timeslot timeslot)
  {
    double neededMWh = neededKWh / 1000.0;
    double limitPrice = buyLimitPrice;
    MarketPosition posn = marketPositions.get(timeslot);
    if (posn != null)
      neededMWh -= posn.getOverallBalance();
    OrderType buySell = OrderType.BUY;
    if (neededMWh < 0.0) {
      buySell = OrderType.SELL;
      neededMWh = -neededMWh;
      limitPrice = sellLimitPrice;
    }
    log.info("new " + buySell + " order for " + neededMWh +
             " in timeslot " + timeslot.getSerialNumber());
    brokerProxyService.routeMessage(new Shout(face, timeslot, buySell,
                                              neededMWh, limitPrice));
  }

  // ------------ process incoming messages -------------
  /**
   * Incoming messages for brokers include:
   * <ul>
   * <li>TariffTransaction tells us about customer subscription
   *   activity and power usage,</li>
   * <li>MarketPosition tells us how much power we have bought
   *   or sold in a given timeslot,</li>
   * <li>TimeslotUpdate that tell us it's time to send in our bids/asks</li>
   * </ul>
   * along with a number of other message types that we can safely ignore.
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
  public void handleMessage(TariffTransaction ttx)
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
   * Receives a new MarketPosition for a given timeslot and stores it
   */
  public void handleMessage (MarketPosition posn)
  {
    marketPositions.put(posn.getTimeslot(), posn);
  }

  /**
   * TimeslotUpdate is sent by the server after all the "phase processing".
   * This is normally when any broker would submit its bids, so that's when
   * the DefaultBroker will do it. Any earlier, and we will find ourselves
   * unable to trade in the furthest slot, because it will not yet have 
   * been enabled.
   */
  public void handleMessage (TimeslotUpdate tsu)
  {
    this.activate();
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
   * Keeps track of customer status and usage. Usage is stored
   * per-customer-unit, but reported as the product of the per-customer
   * quantity and the subscribed population. This allows the broker to use
   * historical usage data as the subscribed population shifts.
   */
  class CustomerRecord
  {
    CustomerInfo customer;
    int subscribedPopulation = 0;
    double[] usage = new double[usageRecordLength];
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
      double kwhPerCustomer = kwh / (double)subscribedPopulation;
      double oldUsage = usage[index];
      if (oldUsage == 0.0) {
        // assume this is the first time
        usage[index] = kwhPerCustomer;
      }
      else {
        // exponential smoothing
        usage[index] = alpha * kwhPerCustomer + (1.0 - alpha) * oldUsage;
      }
    }
    
    double getUsage (int index)
    {
      if (index < 0) {
        log.warn("usage requested for negative index " + index);
        index = 0;
      }
      else if (index > usage.length) {
        index = index % usage.length;
      }
      return (usage[index] * (double)subscribedPopulation);
    }
    
    // we assume here that timeslot index always matches the number of
    // timeslots that have passed since the beginning of the simulation.
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
  
  // test-support method
  HashMap<String, Integer> getCustomerCounts()
  {
    HashMap<String, Integer> result = new HashMap<String, Integer>();
    for (TariffSpecification spec : customerSubscriptions.keySet()) {
      HashMap<CustomerInfo, CustomerRecord> customerMap = customerSubscriptions.get(spec);
      for (CustomerRecord record : customerMap.values()) {
        result.put(record.customer.getName() + spec.getPowerType(), 
                    record.subscribedPopulation);
      }
    }
    return result;
  }
  
  // test-support method
  double getUsageForCustomer (CustomerInfo customer,
                              TariffSpecification tariffSpec,
                              int index)
  {
    CustomerRecord record = customerSubscriptions.get(tariffSpec).get(customer);
    return record.getUsage(index);
  }
}
