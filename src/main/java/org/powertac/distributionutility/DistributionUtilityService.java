/*
 * Copyright 2009-2011 the original author or authors.
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

package org.powertac.distributionutility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.Orderbook;
import org.powertac.common.RandomSeed;
import org.powertac.common.Timeslot;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DistributionUtilityService
extends TimeslotPhaseProcessor
implements InitializationService
{
  Logger log = Logger.getLogger(this.getClass().getName());

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private OrderbookRepo orderbookRepo;

  @Autowired
  private Accounting accountingService;
  
  @Autowired
  private ServerConfiguration serverProps;

  @Autowired
  private RandomSeedRepo randomSeedService;
  private RandomSeed randomGen;

  // fees and prices should be negative, because they are debits against brokers
  @ConfigurableValue(valueType = "Double",
      description = "Low end of distribution fee range")
  private double distributionFeeMin = -0.005;

  @ConfigurableValue(valueType = "Double",
      description = "High end of distribution fee range")
  private double distributionFeeMax = -0.15;

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "Distribution fee: overrides random value selection")
  private Double distributionFee = null;

  @ConfigurableValue(valueType = "Double",
      description = "Low end of balancing cost range")
  private double balancingCostMin = -0.01;

  @ConfigurableValue(valueType = "Double",
      description = "High end of balancing cost range")
  private double balancingCostMax = -0.02;

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "Balancing cost: overrides random value selection")
  private Double balancingCost = null;

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "Spot price/mwh used if unavailable from wholesale market")
  private double defaultSpotPrice = 30.0; // per mwh
  
  @ConfigurableValue(valueType = "String",
          publish = true,
          description = "Balancing settlement processing: blank for no controllable capacity, "
          + "\"static\" for per-timeslot processing of balancing orders")
  private String settlementProcess = "";
  
  // map settlement process to strategy instances
  @SuppressWarnings("serial")
  private Map<String, SettlementProcessor> settlementMap =
          new HashMap<String, SettlementProcessor> () {{
            put("simple", new SimpleSettlementProcessor());
            put("static", new StaticSettlementProcessor());
            put ("dynamic", new DynamicSettlementProcessor());
          }};

  private SettlementProcessor processor = null;
  
  @Override
  public void setDefaults ()
  {
  }

  /**
   * Computes actual distribution and balancing costs by random selection
   */
  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    super.init();
    distributionFee = null;
    balancingCost = null;

    serverProps.configureMe(this);
    
    // compute randomly-generated values if not overridden
    randomGen = randomSeedService.getRandomSeed("DistributionUtilityService",
                                                0, "model");
    if (null == distributionFee)
      distributionFee = (distributionFeeMin + randomGen.nextDouble()
                         * (distributionFeeMax - distributionFeeMin));
    if (null == balancingCost)
      balancingCost = (balancingCostMin + randomGen.nextDouble()
                       * (balancingCostMax - balancingCostMin));
    log.info("Configured DU: distro fee = " + distributionFee
             + ", balancing cost = " + balancingCost);
    
    // determine and record settlement process
    if (settlementProcess.equals(""))
      settlementProcess = "simple";
    processor = settlementMap.get(settlementProcess);
    if (null == processor) {
      log.error("Null settlement processor for " + settlementProcess);
      processor = settlementMap.get("simple");
    }
    
    serverProps.publishConfiguration(this);
    return "DistributionUtility";
  }

  @Override
  public void activate (Instant time, int phaseNumber)
  {
    log.info("Activate");
    List<Broker> brokerList = brokerRepo.findRetailBrokers();
    if (brokerList == null) {
      log.error("Failed to retrieve retail broker list");
      return;
    }

    // Run the balancing market
    // Transactions are posted to the Accounting Service and Brokers are
    // notified of balancing transactions
    balanceTimeslot(timeslotRepo.currentTimeslot(), brokerList);

    // Add distribution transactions
    for (Broker broker : brokerList) {
      double netLoad = -accountingService.getCurrentNetLoad(broker);
      accountingService.addDistributionTransaction(broker, netLoad,
                                                   netLoad * distributionFee);
    }
  }

  /**
   * Generates a list of Transactions that balance the overall market.
   * Transactions are generated on a per-broker basis depending on the broker's
   * balance within its own market.
   * 
   * @return List of MarketTransactions
   */
  public List<ChargeInfo> balanceTimeslot (Timeslot currentTimeslot,
                                                 List<Broker> brokerList)
  {
    //HashMap<Broker, ChargeInfo> chargeInfoMap = new HashMap<Broker, ChargeInfo>();
    //List<Double> brokerBalances = new ArrayList<Double>();
    List<ChargeInfo> brokerData = new ArrayList<ChargeInfo>();
    for (Broker broker : brokerList) {
      brokerData.add(new ChargeInfo(broker,
                                    -getMarketBalance(broker)));
    }
    processor.settle(this, brokerData);
    
    // add balancing transactions
    for (ChargeInfo info : brokerData) {
      double balanceCharge = info.getBalanceCharge();
      if (balanceCharge != 0.0) {
        accountingService.addBalancingTransaction(info.getBroker(),
                                                  info.getNetLoadKWh(),
                                                  balanceCharge);
      }
    }
    return brokerData;
  }

  /**
   * Returns the difference between a broker's current market position and its
   * net load. Note: market position is computed in MWh and net load is computed
   * in kWh, conversion is needed to compute the difference.
   * 
   * @return a broker's current energy balance within its market. Pos for
   *         over-production, neg for under-production
   */
  public double getMarketBalance (Broker broker)
  {
    double result = accountingService.getCurrentMarketPosition(broker) * 1000.0
                    + accountingService.getCurrentNetLoad(broker);
    log.info("market balance for " + broker.getUsername() + ": " + result);
    return result;
  }

  /**
   * Returns the spot market price - the clearing price for the current timeslot
   * in the most recent trading period.
   */
  double getSpotPrice ()
  {
    Double result = defaultSpotPrice;
    // most recent trade is determined by Competition parameters
    // orderbooks have timeslot and execution time
    Orderbook ob =
        orderbookRepo.findSpotByTimeslot(timeslotRepo.currentTimeslot());
    if (ob != null) {
      result = ob.getClearingPrice();
    }
    else {
      log.info("null Orderbook");
    }
    return result / 1000.0; // convert to kwh
  }
  
  /**
   * Returns the maximum price for energy in the current timeslot
   */
  double getPPlus ()
  {
    double result = defaultSpotPrice;
    List<Orderbook> obs = 
        orderbookRepo.findAllByTimeslot(timeslotRepo.currentTimeslot());
    if (obs != null && obs.size() > 0) {
      Double max = null;
      for (Orderbook ob : obs) {
        Double price = ob.getClearingPrice();
        if (price != null && (max == null || price > max))
          max = price;
      }
      result = max;
    }
    return result / 1000.0;
  }
  
  /**
   * Returns the minimum price for energy in the current timeslot
   */
  double getPMinus ()
  {
    double result = defaultSpotPrice;
    List<Orderbook> obs = 
        orderbookRepo.findAllByTimeslot(timeslotRepo.currentTimeslot());
    if (obs != null && obs.size() > 0) {
      Double min = null;
      for (Orderbook ob : obs) {
        Double price = ob.getClearingPrice();
        if (price != null && (min == null || price < min))
          min = price;
      }
      result = min;
    }
    return result / 1000.0;
  }

  // ---------- Getters and setters for test support ---------
  double getDistributionFeeMin ()
  {
    return distributionFeeMin;
  }

  double getDistributionFeeMax ()
  {
    return distributionFeeMax;
  }

  Double getDistributionFee ()
  {
    return distributionFee;
  }

  double getBalancingCostMin ()
  {
    return balancingCostMin;
  }

  double getBalancingCostMax ()
  {
    return balancingCostMax;
  }

  Double getBalancingCost ()
  {
    return balancingCost;
  }

  double getDefaultSpotPrice ()
  {
    return defaultSpotPrice;
  }
}
