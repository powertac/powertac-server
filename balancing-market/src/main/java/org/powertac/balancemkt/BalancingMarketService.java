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

package org.powertac.balancemkt;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.time.Instant;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BalancingMarket;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.Orderbook;
import org.powertac.common.TariffEvaluationHelper;
import org.powertac.common.Timeslot;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.BalanceReport;
import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BalancingMarketService
extends TimeslotPhaseProcessor
implements BalancingMarket, SettlementContext, InitializationService
{
  static Logger log = LogManager.getLogger(BalancingMarketService.class.getSimpleName());

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private OrderbookRepo orderbookRepo;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private BrokerProxy brokerProxyService;

  @Autowired
  private Accounting accountingService;

  @Autowired
  private CapacityControl capacityControlService;

  @Autowired
  private ServerConfiguration serverProps;

  private Double balancingCost = 0D;

  @ConfigurableValue(valueType = "Double",
          publish = true,
          description = "Slope of up-regulation cost /kwh")
  private Double pPlusPrime = 0.0; // .00002/kwh

  @ConfigurableValue(valueType = "Double",
          publish = true,
          description = "Slope of down-regulation cost /kwh")
  private Double pMinusPrime = 0.0; // -.00002/kwh

  @ConfigurableValue(valueType = "Double",
          publish = true,
          description = "Ratio of regulating-market price to spot price")
  private Double rmPremium = 1.1; // 10% premium

  @ConfigurableValue(valueType = "Double",
          publish = true,
          description = "Fixed cost/kWh for regulation energy")
  private Double rmFee = 0.035;

  @ConfigurableValue(valueType = "Double",
          publish = true,
          description = "Spot price/mwh used if unavailable from wholesale market")
  private Double defaultSpotPrice = 30.0; // per mwh

  @ConfigurableValue(valueType = "String",
          publish = true,
          description = "Balancing settlement processing: \"static\" for per-timeslot processing (default),"
          + " \"dynamic\" for multiple-timeslot processing, of balancing orders")
  private String settlementProcess = "";

  // map settlement process to strategy instances
  @SuppressWarnings("serial")
  private Map<String, Class<?>> settlementMap =
          new HashMap<String, Class<?>> () {{
            put("static", StaticSettlementProcessor.class);
            put("dynamic", DynamicSettlementProcessor.class);
          }};

  private Map<Broker, ChargeInfo> balancingResults = null;

  /**
   * Computes actual distribution and balancing costs by random selection
   */
  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    super.init();
    balancingCost = null;

    serverProps.configureMe(this);

    if (null == balancingCost)
        balancingCost = 0D;
    log.info("Configured BM: balancing cost = " + balancingCost
             + ", (pPlus',pMinus') = (" + pPlusPrime + "," + pMinusPrime + ")");

    // publish configuration for market and for TariffEvaluationHelper
    serverProps.publishConfiguration(this);

    TariffEvaluationHelper teh = new TariffEvaluationHelper();
    serverProps.configureMe(teh);
    serverProps.publishConfiguration(teh);
    
    return "BalancingMarket";
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

    // create the BalanceReport to carry the total imbalance
    Timeslot current = timeslotRepo.currentTimeslot();
    TotalImbalance sum = makeTotalImbalance();

    // Run the balancing market
    // Transactions are posted to the Accounting Service and Brokers are
    // notified of balancing transactions
    balancingResults = balanceTimeslot(brokerList, sum);

    // Send the balance report
    BalanceReport report = new BalanceReport(current.getSerialNumber(),
                                             sum.getValue());
    brokerProxyService.broadcastMessage(report);
  }

  /**
   * Generates a list of Transactions that balance the overall market.
   * Transactions are generated on a per-broker basis depending on the broker's
   * balance within its own market.
   *
   * @return List of ChargeInfo instances
   */
  public Map<Broker, ChargeInfo> balanceTimeslot (List<Broker> brokerList,
                                                  TotalImbalance report)
  {
    Map<Broker, ChargeInfo> chargeInfoMap = new LinkedHashMap<>();

    // create the ChargeInfo instances for each broker
    for (Broker broker : brokerList) {
      double imbalance = getMarketBalance(broker);
      ChargeInfo info = new ChargeInfo(broker, imbalance);
      report.add(imbalance);
      chargeInfoMap.put(broker, info);
    }

    // retrieve and allocate the balancing orders
    Collection<BalancingOrder> boc = tariffRepo.getBalancingOrders();
    for (BalancingOrder order : boc) {
      ChargeInfo info = chargeInfoMap.get(order.getBroker());
      info.addBalancingOrder(order);
    }

    // gather up the list of ChargeInfo instances and settle
    log.info("balancing prices: pPlus=" + getPPlus()
             + ", pMinus=" + getPMinus());
    List<ChargeInfo> brokerData = new ArrayList<>(chargeInfoMap.values());
    getSettlementProcessor().settle(this, brokerData);

    // add balancing transactions - note that debits/credits for balancing
    // orders (p2 values) will already have been posted in the process of
    // exercising orders.
    for (ChargeInfo info : brokerData) {
      double balanceCharge = info.getBalanceChargeP1();
      if (balanceCharge != 0.0) {
        accountingService.addBalancingTransaction(info.getBroker(),
                                                  info.getNetLoadKWh(),
                                                  balanceCharge);
      }
    }
    return chargeInfoMap;
  }

  /**
   * Returns the difference between a broker's current market position and its
   * net load. Note: market position is computed in MWh and net load is computed
   * in kWh, conversion is needed to compute the difference in kWh.
   *
   * @return a broker's current energy balance within its market. Pos for
   *         over-production, neg for under-production
   */
  @Override
  public double getMarketBalance (Broker broker)
  {
    double result = accountingService.getCurrentMarketPosition(broker) * 1000.0
                    + accountingService.getCurrentNetLoad(broker);
    log.info("market balance for " + broker.getUsername() + ": " + result);
    return result;
  }

  /**
   * Returns the net balancing result for a given broker. Valid only after
   * service activation within a given timeslot.
   */
  @Override
  public double getRegulation (Broker broker)
  {
    ChargeInfo ci = balancingResults.get(broker);
    if (null == ci) {
      log.error("Null balancing result for broker " + broker.getUsername());
      return 0.0;
    }
    return ci.getCurtailment();
  }

  /**
   * Returns the spot market price per kWh -
   * This is the most recent valid clearing price for the current timeslot.
   */
  double getSpotPrice ()
  {
    Double result = defaultSpotPrice / 1000.0; // convert to kwh
    // most recent trade is determined by Competition parameters
    // orderbooks have timeslot and execution time
    Orderbook ob =
        orderbookRepo.findSpotByTimeslot(timeslotRepo.currentTimeslot());
    if (ob != null) {
      result = ob.getClearingPrice() / 1000.0;
    }
    else {
      log.info("null Orderbook");
    }
    log.info("ts {}, spot price {}",
             timeslotRepo.currentTimeslot().getSerialNumber(),
             result);
    return result;
  }

  /**
   * Returns the zero-quantity price for up-regulation energy
   * in the current timeslot. Value is positive per kWh
   */
  @Override
  public Double getPPlus ()
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
      if (max != null)
        result = max;
    }
    result = result * rmPremium / 1000.0 + rmFee;
    //log.info("PPlus = {}", result);
    return result;
  }

  /**
   * Returns the zero-quantity price for down-regulation energy
   * in the current timeslot. Price is per kWh.
   */
  @Override
  public Double getPMinus ()
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
      if (min != null)
        result = min;
    }
    result = -result / rmPremium / 1000.0 - rmFee;
    //log.info("PMinus = {}", result);
    return result;
  }

  /**
   * Returns the slope of up-regulation energy purchased through the
   * wholesale ancillary services market.
   */
  @Override
  public Double getPPlusPrime ()
  {
    return pPlusPrime;
  }

  /**
   * Returns the slope of down-regulation energy sold through the
   * wholesale ancillary services market.
   */
  @Override
  public Double getPMinusPrime ()
  {
    return pMinusPrime;
  }

  // ---------- Getters and setters for settlement processsors ---------

  @Override
  public Double getBalancingCost() {
      return balancingCost;
  }

  @Override
  public Double getDefaultSpotPrice () {
      return defaultSpotPrice;
  }

  private SettlementProcessor getSettlementProcessor ()
  {
    // determine and record settlement process
    if (settlementProcess.equals(""))
      settlementProcess = "static";
    Class<?> processor = settlementMap.get(settlementProcess);
    if (null == processor) {
      log.error("Null settlement processor for " + settlementProcess);
      processor = settlementMap.get("simple");
    }
    SettlementProcessor result = null;
    try {
      Constructor<?> constructor =
              processor.getDeclaredConstructor(TariffRepo.class,
                                               CapacityControl.class);
      result = (SettlementProcessor) constructor.newInstance(tariffRepo, capacityControlService);
    }
    catch (Exception e) {
      log.error("cannot create settlement processor: " + e.toString());
    }
    return result;
  }

  // test support
  Double getRmPremium ()
  {
    return rmPremium;
  }

  void setRmPremium (double value)
  {
    rmPremium = value;
  }

  Double getRmFee ()
  {
    return rmFee;
  }

  void setRmFee (double value)
  {
    rmFee = value;
  }

  /**
   * Mutable double to support computation of total imbalance
   * @author jcollins
   */
  class TotalImbalance
  {
    double value = 0.0;

    TotalImbalance()
    {
      super();
    }

    double add (double addend)
    {
      value += addend;
      return value;
    }

    double getValue ()
    {
      return value;
    }
  }

  // needed for testing
  TotalImbalance makeTotalImbalance()
  {
    return new TotalImbalance();
  }

}
