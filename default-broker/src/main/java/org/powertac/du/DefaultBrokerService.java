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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.time.Instant;
import org.powertac.common.*;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.*;
import org.powertac.common.msg.BrokerAccept;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.msg.MarketBootstrapData;
import org.powertac.common.msg.TimeslotComplete;
import org.powertac.common.repo.BootstrapDataRepo;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static org.powertac.util.MessageDispatcher.dispatch;

/**
 * Default broker implementation. We do the implementation in a service, because
 * the default broker is a singleton and it's convenient. The actual Broker
 * instance is implemented in an inner class. Note that this is not a type of
 * TimeslotPhaseProcessor. It's a broker, and so it runs after the last message
 * of the timeslot goes out, the TimeslotComplete message. As implemented, it runs
 * in the message-sending thread. If this turns out to cause problems with real
 * brokers, it could run in its own thread.
 * @author John Collins
 */
@Service
public class DefaultBrokerService
  implements BootstrapDataCollector, InitializationService
{
  static private Logger log = LogManager.getLogger(DefaultBrokerService.class.getName());
  
  @Autowired // routing of outgoing messages
  private BrokerProxy brokerProxyService;
  
  @Autowired // needed to discover sim mode
  private CompetitionControl competitionControlService;
  
  @Autowired // tariff publication
  private TariffMarket tariffMarketService;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private BrokerRepo brokerRepo;
  
  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private BootstrapDataRepo bootstrapDataRepo;
  
  @Autowired
  private ServerConfiguration serverPropertiesService;

  private DefaultBroker face;
  private Competition competition;
  
  /** parameters */
  // keep in mind that brokers need to deal with two viewpoints with regard
  // to signs. Tariffs
  // types take the viewpoint of the customer, while market-related types
  // take the viewpoint of the broker.
  private double defaultConsumptionRate = -1.0; // customer pays
  private double defaultProductionRate = 0.01;  // broker pays
  private double initialBidKWh = 500.0;
  
  // max and min offer prices. Max means "sure to trade"
  private double buyLimitPriceMax = -1.0;  // broker pays
  private double buyLimitPriceMin = -100.0;  // broker pays
  private double sellLimitPriceMax = 100.0;    // other broker pays
  private double sellLimitPriceMin = 0.2;    // other broker pays
  private int usageRecordLength = 7 * 24; // one week
  private double storageTariffValue = 0.04; // per kWh
  private double storageTariffMargin = 0.2; // buy-sell margin
  
  // bootstrap-mode data - uninitialized for normal sim mode
  private boolean bootstrapMode = false;
  private HashMap<Timeslot, ArrayList<MarketTransaction>> marketTxMap;
  private ArrayList<Double> marketMWh;
  private ArrayList<Double> marketPrice;
  private ArrayList<WeatherReport> weather;

  private HashMap<TariffSpecification,
                  HashMap<CustomerInfo, CustomerRecord>> customerSubscriptions;
  private HashMap<CustomerInfo, CustomerRecord> bootProfiles;
  private RandomSeed randomSeed;
  private HashMap<Timeslot, Order> lastOrder;

  private double minMWh = 0.0; // Get this from the Competition when we first look at is

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
   * Configures parameters, sets up and publishes default tariffs.
   */
  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    // defer for TariffMarket initialization
    int index = completedInits.indexOf("TariffMarket");
    if (index == -1) {
      return null;
    }

    // keep track of competition
    this.competition = competition;

    // create the default broker instance, register it with the repo
    brokerRepo.add(createBroker("default broker"));

    // log in to ccs
    competitionControlService.loginBroker(face.getUsername());
    
    // set up local state
    bootstrapMode = competitionControlService.isBootstrapMode();
    log.info("init, bootstrapMode=" + bootstrapMode);
    customerSubscriptions = new LinkedHashMap<>();
    bootProfiles = new LinkedHashMap<>();
    lastOrder = new HashMap<>();
    randomSeed = randomSeedRepo.getRandomSeed(this.getClass().getName(),
                                              0, "pricing");
    
    // if we are in bootstrap mode, we need to set up the dataset
    if (bootstrapMode)
    {
      marketTxMap = new HashMap<>();
      marketMWh = new ArrayList<>();
      marketPrice = new ArrayList<>();
      weather = new ArrayList<>();
    }

    // pull down configuration
    serverPropertiesService.configureMe(this);
    
    // Compute the value of storage tariffs
    computeStorageTariffValue();

    // create and publish default tariffs
    TariffSpecification defaultConsumption = new TariffSpecification(face, PowerType.CONSUMPTION)
        .addRate(new Rate().withValue(defaultConsumptionRate));
    tariffMarketService.setDefaultTariff(defaultConsumption);
    customerSubscriptions.put(defaultConsumption, new LinkedHashMap<>());

    TariffSpecification defaultProduction = new TariffSpecification(face, PowerType.PRODUCTION)
        .addRate(new Rate().withValue(defaultProductionRate));
    tariffMarketService.setDefaultTariff(defaultProduction);
    customerSubscriptions.put(defaultProduction, new LinkedHashMap<>());

    TariffSpecification defaultStorage = new TariffSpecification(face, PowerType.STORAGE)
        .addRate(new Rate().withValue(defaultConsumptionRate));
//        .addRate(new RegulationRate()
//           .withUpRegulationPayment(getStorageTariffValue() *
//                                    (1.0 + getStorageTariffMargin()))
//           .withDownRegulationPayment(- getStorageTariffValue() *
//                                      (1.0 - getStorageTariffMargin())));
    tariffMarketService.setDefaultTariff(defaultStorage);
    customerSubscriptions.put(defaultStorage, new LinkedHashMap<>());

    return "DefaultBroker";
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
    face = new DefaultBroker(username);
    //face.setEnabled(true); // log in -- do not set this directly
    face.setService(this);
    return face;
  }

  public DefaultBroker getFace ()
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
    log.info("activate: timeslot " + current.getSerialNumber());

    // In the first through 23rd timeslot, we buy enough to meet what was
    // used in the previous timeslot. Note that this is called after the
    // customer model has run in the current timeslot, for a market clearing
    // at the beginning of the following timeslot.
    if (current.getSerialNumber() < 24) {
      // we already have usage data for the current timeslot.
      double currentKWh = collectUsage(current.getSerialNumber());
      double neededKWh = 0.0;
      for (Timeslot timeslot : timeslotRepo.enabledTimeslots()) {
        // use data already collected if we have it, otherwise use data from
        // the current timeslot.
        int index = (timeslot.getSerialNumber()) % 24;
        double historicalKWh = collectUsage(index);
        if (historicalKWh != 0.0)
          neededKWh = historicalKWh;
        else
          neededKWh = currentKWh;
        // subtract out the current market position, and we know what to
        // buy or sell
        submitOrder(neededKWh, timeslot);
      }
    }
    
    // Once we have 24 hours of records, assume we need enough to meet 
    // the mean of what we have used at this time of day so far
    else if (current.getSerialNumber() <= usageRecordLength) {
      for (Timeslot timeslot : timeslotRepo.enabledTimeslots()) {
        double neededKWh = 0.0;
        int index = (timeslot.getSerialNumber()) % 24;
        int count = 0;
        while (index <= current.getSerialNumber()) {
          neededKWh += collectUsage(index);
          index += 24;
          count += 1;
        }
        submitOrder((neededKWh / count), timeslot);
      }
    }

    // Finally, once we have a full week of records, we use the data for
    // the hour and day-of-week.
    else {
      double neededKWh = 0.0;
      for (Timeslot timeslot : timeslotRepo.enabledTimeslots()) {
        int index = (timeslot.getSerialNumber()) % usageRecordLength;
        neededKWh = collectUsage(index);
        submitOrder(neededKWh, timeslot);
      }      
    }
  }

  private void ensureBootProfiles ()
  {
    if (0 == bootProfiles.size()) {
      for (CustomerInfo customer : customerRepo.list()) {
        bootProfiles.put(customer,
                         new CustomerRecord(customer,
                                            customer.getPopulation()));
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
    log.debug("Usage(" + index + ")=" + result);
    return -result; // convert to needed energy account balance
  }

  // lazy accessor for Competition.minMwh
  private double getMinMWh ()
  {
    if (minMWh == 0.0)
      minMWh = Competition.currentCompetition().getMinimumOrderQuantity();
    return minMWh;
  }

  private void submitOrder (double neededKWh, Timeslot timeslot)
  {
    double neededMWh = neededKWh / 1000.0;
    if (Math.abs(neededMWh) < getMinMWh()) {
      // don't bother
      return;
    }

    Double limitPrice;
    MarketPosition posn = face.findMarketPositionByTimeslot(timeslot.getSerialNumber());
    if (posn != null)
      neededMWh -= posn.getOverallBalance();
    log.debug("needed mWh=" + neededMWh);
    if (Math.abs(neededMWh) < getMinMWh()) {
      log.info("no power required in timeslot " + timeslot.getSerialNumber());
      return;
    }
    else {
      limitPrice = computeLimitPrice(timeslot, neededMWh);
    }
    log.info("new order for " + neededMWh + " at " + limitPrice +
             " in timeslot " + timeslot.getSerialNumber());
    Order result = new Order(face, timeslot.getSerialNumber(), neededMWh, limitPrice);
    lastOrder.put(timeslot, result);
    brokerProxyService.routeMessage(result);
  }

  /**
   * Computes a limit price with a random element. 
   */
  private Double computeLimitPrice (Timeslot timeslot,
                                    double amountNeeded)
  {
    // start with default limits
    Double oldLimitPrice;
    double minPrice;
    if (amountNeeded > 0.0) {
      // buying
      oldLimitPrice = buyLimitPriceMax;
      minPrice = buyLimitPriceMin;
    }
    else {
      // selling
      oldLimitPrice = sellLimitPriceMax;
      minPrice = sellLimitPriceMin;
    }
    // check for escalation
    Order lastTry = lastOrder.get(timeslot);
    if (lastTry != null
        && Math.signum(amountNeeded) == Math.signum(lastTry.getMWh()))
      oldLimitPrice = lastTry.getLimitPrice();

    // set price between oldLimitPrice and maxPrice, according to number of
    // remaining chances we have to get what we need.
    double newLimitPrice = minPrice; // default value
    Timeslot current = timeslotRepo.currentTimeslot();
    int remainingTries = (timeslot.getSerialNumber()
                          - current.getSerialNumber()
                          - Competition.currentCompetition().getDeactivateTimeslotsAhead());
    if (remainingTries > 0) {
      double range = (minPrice - oldLimitPrice) * 2.0 / (double)remainingTries;
      log.debug("oldLimitPrice=" + oldLimitPrice + ", range=" + range);
      double computedPrice = oldLimitPrice + randomSeed.nextDouble() * range; 
      return Math.max(newLimitPrice, computedPrice);
    }
    else
      return null; // market order
  }

  // ------------ process incoming messages -------------
  /**
   * Incoming messages for brokers include:
   * <ul>
   * <li>TariffTransaction tells us about customer subscription
   *   activity and power usage,</li>
   * <li>MarketPosition tells us how much power we have bought
   *   or sold in a given timeslot,</li>
   * <li>TimeslotComplete that tell us it's time to send in our bids/asks</li>
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
   * Handles the BrokerAccept message for completeness
   */
  public void handleMessage (BrokerAccept ba)
  {
    log.info("Broker login accepted");
  }
  
  /**
   * Handles a TariffTransaction. We only care about certain types: PRODUCE,
   * CONSUME, SIGNUP, and WITHDRAW.
   */
  public void handleMessage (TariffTransaction ttx)
  {
    TariffTransaction.Type txType = ttx.getTxType();
    CustomerInfo customer = ttx.getCustomerInfo();
    HashMap<CustomerInfo, CustomerRecord> customerMap = 
      customerSubscriptions.get(ttx.getTariffSpec());
    CustomerRecord record = customerMap.get(customer);

    if (TariffTransaction.Type.SIGNUP == txType) {
      // keep track of customer counts
      if (record == null) {
        record = new CustomerRecord(customer, ttx.getCustomerCount());
        customerMap.put(customer, record);
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
        log.info("production by subset " + ttx.getCustomerCount() +
                 " of subscribed population " + record.subscribedPopulation);
      }
      record.produceConsume(ttx.getKWh(), ttx.getPostedTime());
    }
    else if (TariffTransaction.Type.CONSUME == txType) {
      if (ttx.getCustomerCount() != record.subscribedPopulation) {
        log.info("consumption by subset " + ttx.getCustomerCount() +
                 " of subscribed population " + record.subscribedPopulation);
      }
      record.produceConsume(ttx.getKWh(), ttx.getPostedTime());
    }

    // fill out bootstrap record if needed
    if (bootstrapMode &&
        (TariffTransaction.Type.PRODUCE == txType ||
        TariffTransaction.Type.CONSUME == txType)) {
      ensureBootProfiles();
      record = bootProfiles.get(ttx.getCustomerInfo());
      record.produceConsume(ttx.getKWh(), ttx.getPostedTime());
    }
  }

  /**
   * Receives a new WeatherReport. We only care about this if in bootstrap
   * mode, in which case we simply store it in the bootstrap dataset.
   */
  public void handleMessage (WeatherReport report)
  {
    // only in bootstrap mode
    if (bootstrapMode) {
      weather.add(report);
    }
  }
  
  /**
   * Receives a new MarketTransaction. In bootstrapMode, we need to record
   * these as they arrive in order to be able to compute delivered price of
   * power purchased in the wholesale market. Note that this computation will
   * ignore balancing cost. This is intentional.
   */
  public void handleMessage (MarketTransaction tx)
  {
    // Save all transactions in bootstrapMode
    if (bootstrapMode) {
      ArrayList<MarketTransaction> txs = marketTxMap.get(tx.getTimeslot());
      if (txs == null) {
        txs = new ArrayList<MarketTransaction>();
        marketTxMap.put(tx.getTimeslot(), txs);
      }
      txs.add(tx);
    }
    // reset price escalation when a trade fully clears.
    Order lastTry = lastOrder.get(tx.getTimeslot());
    if (lastTry == null) // should not happen
      log.error("order corresponding to market tx " + tx + " is null");
    else if (tx.getMWh() == lastTry.getMWh()) // fully cleared
      lastOrder.put(tx.getTimeslot(), null);
  }
  
  /**
   * Handles CustomerBootstrapData by populating the customer model 
   * corresponding to the given customer and power type. This gives the
   * broker a running start.
   */
  public void handleMessage (CustomerBootstrapData cbd)
  {
    TariffSpecification tariff = null;
    for (TariffSpecification spec : customerSubscriptions.keySet()) {
      if (cbd.getPowerType().canUse(spec.getPowerType())) {
        tariff = spec;
        break;
      }
    }
    if (tariff == null) {
      log.error("Failed to find tariff for powerType " + cbd.getPowerType());
    }

    CustomerInfo customer =
        customerRepo.findByNameAndPowerType(cbd.getCustomerName(),
                                            cbd.getPowerType());
    if (null == customer) {
      log.error("Failed to find customer " + cbd.getCustomerName());
    }
    else {
      HashMap<CustomerInfo, CustomerRecord> customerMap = 
          customerSubscriptions.get(tariff);
      CustomerRecord record = customerMap.get(customer); // subscription exists
      if (record == null) {
        record = new CustomerRecord(customer, customer.getPopulation());
        customerMap.put(customer, record);
      }
      if (cbd.getNetUsage().length <
          Competition.currentCompetition().getBootstrapTimeslotCount()) {
        log.error("Short boot record {} for customer {} -- filling with zeros",
                  cbd.getNetUsage().length,
                  customer.getName());
      }
      //int offset = (timeslotRepo.currentTimeslot().getSerialNumber()
      //    - cbd.getNetUsage().length);
      //log.info("sn=" + timeslotRepo.currentTimeslot().getSerialNumber()
      //         + ", offset=" + offset);
      for (int i = 0; i < cbd.getNetUsage().length; i++) {
        record.produceConsume(cbd.getNetUsage()[i], i);// + offset);
      }
      for (int i = cbd.getNetUsage().length;
          i < Competition.currentCompetition().getBootstrapTimeslotCount();
          i++) {
        record.produceConsume(0.0, i);//) + offset);
      }
    }
  }

  /**
   * CashPosition is the last message sent by Accounting.
   * In bootstrapMode, this is when we collect customer
   * usage data.
   */
  public void handleMessage (CashPosition cp)
  {
    // collect usage and price data
    if (bootstrapMode) {
      // the wholesale market transactions can be mined for the net cost of
      // purchased power in the current timeslot.
      recordDeliveredPrice();
    }
  }
  
  /**
   * TimeslotComplete is the last message sent in each timeslot.
   * This is normally when any broker would submit its bids, so that's when
   * the DefaultBroker will do it. Any earlier, and we will find ourselves
   * unable to trade in the furthest slot, because it will not yet have 
   * been enabled. 
   */
  public void handleMessage (TimeslotComplete tc)
  {
    this.activate();
    // In bootstrap mode,
    // fill out customer records for which no transactions have arrived
    if (bootstrapMode) {
      for (CustomerRecord record : bootProfiles.values()) {
        record.fillBootstrapUsage(0.0, timeslotRepo.currentSerialNumber());
      }
    }
  }

  /**
   * Records the delivered price of purchased power in the current timeslot.
   * If the broker has purchased more than it has sold, this will be a negative
   * number.
   */
  private void recordDeliveredPrice ()
  {
    Timeslot current = timeslotRepo.currentTimeslot();
    ArrayList<MarketTransaction> txList = marketTxMap.get(current);
    if (txList == null) {
      txList = new ArrayList<MarketTransaction>();
      marketTxMap.put(current, txList);
    }
    double totalMWh = 0.0;
    double totalCost = 0.0;
    for (MarketTransaction tx : txList) {
      // only include buy orders
      if (tx.getMWh() > 0.0) {
        log.info("record price: mwh=" + tx.getMWh() + ", price=" + tx.getPrice());
        totalMWh += tx.getMWh();
        totalCost += tx.getPrice() * tx.getMWh();
      }
    }
    log.info("market totals: mwh=" + totalMWh
             + ", price=" + totalCost / totalMWh);
    marketMWh.add(totalMWh);
    if (totalMWh == 0.0) {
      marketPrice.add(0.0);
    }
    else {
      marketPrice.add(totalCost / totalMWh);
    }
  }
  
  // -------------------- Bootstrap data queries --------------------------

  /**
   * Collects and returns a list of messages representing collected customer
   * demand, market price, and weather records for the bootstrap period. Note
   * that the customer and weather info is flattened.
   */
  @Override
  public List<Object> collectBootstrapData (int maxTimeslots)
  {
    ArrayList<Object> result = new ArrayList<Object>();
    for (Object item : getCustomerBootstrapData(maxTimeslots)) {
      result.add(item);
    }
    result.add(getMarketBootstrapData(maxTimeslots));
    for (Object item : getWeatherReports(maxTimeslots)) {
      result.add(item);
    }
    return result;
  }

  /**
   * Returns a list of CustomerBootstrapData instances, one for each
   * (tariff, customer) pair. Note that this only
   * makes sense at the end of a bootstrap sim run.
   */
  List<CustomerBootstrapData> getCustomerBootstrapData (int maxTimeslots)
  {
    ArrayList<CustomerBootstrapData> result = new ArrayList<>();
    // iterate through the customers
    for (CustomerInfo customer : bootProfiles.keySet()) {
      CustomerRecord record = bootProfiles.get(customer);
      ArrayList<Double>usageList = record.bootstrapUsage;
      int startIndex = Math.max(0, usageList.size() - maxTimeslots);
      double[] usage = new double[usageList.size() - startIndex];
      for (int i = 0; i < usage.length; i++)
        usage[i] = usageList.get(i + startIndex);
      result.add(new CustomerBootstrapData(customer,
                                           customer.getPowerType(),
                                           usage));
    }
    return result;
  }
  
  /**
   * Returns a single MarketBootstrapData instance representing the quantities
   * and prices paid by the default broker for the power it purchased over 
   * the bootstrap period. Intended to be called at the end of a bootstrap
   * session.
   */
  MarketBootstrapData getMarketBootstrapData (int maxTimeslots)
  {
    if (marketMWh.size() != marketPrice.size()) {
      // should not happen
      log.error("marketMWh.size()=" + marketMWh.size() + " != " +
                "marketPrice.size()=" + marketPrice.size());
    }
    int startOffset = Math.max(0, marketMWh.size() - maxTimeslots);
    int size = marketMWh.size() - startOffset;

    // ARRRGH - autoboxing does not work for arrays...
    double[] mwh = new double[size];
    double[] price = new double[size];
    for (int i = 0; i < size; i++) {
      mwh[i] = marketMWh.get(i + startOffset);
      price[i] = marketPrice.get(i + startOffset);
    }
    return new MarketBootstrapData(mwh, price);
  }
  
  /**
   * Returns the accumulated list of WeatherReport instances
   */
  List<WeatherReport> getWeatherReports (int maxTimeslotCount)
  {
    int discardCount = weather.size() - maxTimeslotCount;
    if (discardCount > 0) {
      for (int i = 0; i < discardCount; i++) {
        weather.remove(0);
      }
    }
    return weather;
  }
  
  // ------------------ Property access for configuration ------------------

  public double getConsumptionRate ()
  {
    return defaultConsumptionRate;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Fixed price/kwh for default consumption tariff")
  public void setConsumptionRate (double defaultConsumptionRate)
  {
    this.defaultConsumptionRate = defaultConsumptionRate;
  }

  public double getProductionRate ()
  {
    return defaultProductionRate;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Fixed price/kwh for default production tariff")
  public void setProductionRate (double defaultProductionRate)
  {
    this.defaultProductionRate = defaultProductionRate;
  }

  public double getInitialBidKWh ()
  {
    return initialBidKWh;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Quantity to buy in day-ahead market before seeing actual customer data")
  public void setInitialBidKWh (double initialBidKWh)
  {
    this.initialBidKWh = initialBidKWh;
  }

  public double getBuyLimitPriceMax ()
  {
    return buyLimitPriceMax;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Initial limit price/mwh for bids in day-ahead market")
  public void setBuyLimitPriceMax (double buyLimitPriceMax)
  {
    this.buyLimitPriceMax = buyLimitPriceMax;
  }

  public double getBuyLimitPriceMin ()
  {
    return buyLimitPriceMin;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Final limit price/mwh for bids in day-ahead market")
  public void setBuyLimitPriceMin (double buyLimitPriceMin)
  {
    this.buyLimitPriceMin = buyLimitPriceMin;
  }

  public double getSellLimitPriceMax ()
  {
    return sellLimitPriceMax;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Initial limit price/mwh for asks in day-ahead market")
  public void setSellLimitPriceMax (double sellLimitPriceMax)
  {
    this.sellLimitPriceMax = sellLimitPriceMax;
  }

  public double getSellLimitPriceMin ()
  {
    return sellLimitPriceMin;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Final limit price/mwh for asks in day-ahead market")
  public void setSellLimitPriceMin (double sellLimitPriceMin)
  {
    this.sellLimitPriceMin = sellLimitPriceMin;
  }

  // storage tariff value
  public double getStorageTariffValue ()
  {
    return storageTariffValue;
  }

  @ConfigurableValue(valueType = "Double",
          description = "Bootstrap value for regulation in storage tariff")
  public void setStorageTariffValue (double storageTariffValue)
  {
    this.storageTariffValue = storageTariffValue;
  }

  // Computes value of storage tariff from bootstrap market data
  void computeStorageTariffValue ()
  {
    if (!isBootstrapMode()) {
      MarketBootstrapData data =
              (MarketBootstrapData)(bootstrapDataRepo
                      .getData(MarketBootstrapData.class).get(0));
      if (null == data) {
        log.error("Market bootstrap data not available");
        return;
      }
      storageTariffValue = -1.0 * data.getMeanMarketPrice() / 1000.0;
      log.info("Storage tariff value = {}", storageTariffValue);
    }
  }

  // storage tariff margin
  public double getStorageTariffMargin ()
  {
    return storageTariffMargin;
  }

  @ConfigurableValue(valueType = "Double",
          description = "margin for regulation in storage tariff")
  public void setStorageTariffMargin (double storageTariffMargin)
  {
    this.storageTariffMargin = storageTariffMargin;
  }
  
  //-------------------- Customer-model recording ---------------------
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
    int usageIndexOffset = -1; // timeslot offset for usage indexing
    ArrayList<Double> bootstrapUsage = new ArrayList<Double>();
    Instant base = null;
    double alpha = 0.3;
    
    CustomerRecord (CustomerInfo customer, int population)
    {
      super();
      this.customer = customer;
      this.subscribedPopulation = population;
      base = timeslotRepo.findBySerialNumber(0).getStartInstant();
    }
    
    // Returns the CustomerInfo for this record
    CustomerInfo getCustomerInfo ()
    {
      return customer;
    }
    
    // Adds new individuals to the count
    void signup (int population)
    {
      subscribedPopulation = Math.min(customer.getPopulation(),
                                      subscribedPopulation + population);
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
      produceConsume(kwh, index);
    }
    
    // store profile data at the given index
    void produceConsume (double kwh, int rawIndex)
    {
      fillBootstrapUsage(kwh, rawIndex);
      int index = getIndex(rawIndex);
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
      log.debug("consume " + kwh + " at " + index +
                ", customer " + customer.getName());
    }

    void fillBootstrapUsage (double kwh, int rawIndex)
    {
      // in bootstrap mode, we also record everything raw
      if (bootstrapMode) {
        if (bootstrapUsage.size() <= rawIndex) {
          while (bootstrapUsage.size() < rawIndex)
            bootstrapUsage.add(0.0);
          bootstrapUsage.add(kwh);
        }
        else {
          bootstrapUsage.set(rawIndex, bootstrapUsage.get(rawIndex) + kwh);
        }
        //log.info("bootstrapUsage customer " + customer.getName()
        //         + "[" + rawIndex + "]=" + bootstrapUsage.get(rawIndex));
      }
    }

    double getUsage (int index)
    {
      if (index < 0) {
        log.warn("usage requested for negative index " + index);
        index = 0;
      }
      return (usage[getIndex(index)] * (double)subscribedPopulation);
    }
    
    // we assume here that timeslot index always matches the number of
    // timeslots that have passed since the beginning of the simulation,
    // offset by the number of bootstrap timeslots.
    int getIndex (Instant when)
    {
      int offset = getUsageIndexOffset();
      int result = (int)((when.toEpochMilli() - base.toEpochMilli()) /
                         (Competition.currentCompetition().getTimeslotDuration()))
                   - offset;
      log.debug("offset=" + offset + ", index=" + result);
      return result;
    }
    
    private int getIndex (int rawIndex)
    {
      return rawIndex % usage.length;
    }
    
    private int getUsageIndexOffset ()
    {
      if (usageIndexOffset < 0) {
        // not yet initialized
        usageIndexOffset = 0; // offset for bootstrap mode
        if (!isBootstrapMode()) {
          usageIndexOffset =
                  Competition.currentCompetition().getBootstrapTimeslotCount()
                  + Competition.currentCompetition().getBootstrapDiscardedTimeslots();
        }
      }
      return usageIndexOffset;
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
  
  // test-support methods
  double getUsageForCustomer (CustomerInfo customer,
                              TariffSpecification tariffSpec,
                              int index)
  {
    CustomerRecord record = customerSubscriptions.get(tariffSpec).get(customer);
    return record.getUsage(index);
  }
  
  boolean isBootstrapMode ()
  {
    return bootstrapMode;
  }
}
