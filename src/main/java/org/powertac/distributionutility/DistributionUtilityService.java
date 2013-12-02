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

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BalancingMarket;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.RandomSeed;
import org.powertac.common.TariffTransaction;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DistributionUtilityService
extends TimeslotPhaseProcessor
implements InitializationService
{
  Logger log = Logger.getLogger(this.getClass().getSimpleName());

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private OrderbookRepo orderbookRepo;
  
  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private Accounting accounting;

  @Autowired
  private BalancingMarket balancingMarket;
  
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

    serverProps.configureMe(this);
    
    // compute randomly-generated values if not overridden
    randomGen = randomSeedService.getRandomSeed("DistributionUtilityService",
                                                0, "model");
    if (null == distributionFee)
      distributionFee = (distributionFeeMin + randomGen.nextDouble()
                         * (distributionFeeMax - distributionFeeMin));
    log.info("Configured DU: distro fee = " + distributionFee);
    
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

    // Add distribution transactions
    // should be total production + total consumption
    //    + final imbalance - balancing transactions
    Map<Broker, Map<TariffTransaction.Type, Double>> totals =
            accounting.getCurrentSupplyDemandByBroker();
    for (Broker broker : brokerList) {
      Map<TariffTransaction.Type, Double> brokerTotals = totals.get(broker);
      if (null == brokerTotals)
        continue;
      double consumption = brokerTotals.get(TariffTransaction.Type.CONSUME);
      double production = brokerTotals.get(TariffTransaction.Type.PRODUCE);
      double imports = accounting.getCurrentMarketPosition(broker) * 1000.0;
      // balancing adjusts imports
      double imbalance = balancingMarket.getMarketBalance(broker); // >0 if oversupply
      double balanceAdj = balancingMarket.getRegulation(broker);
      log.info("Distribution tx for "
               + broker.getUsername() + "(c,p,m,i,b) = ("
               + consumption + ","
               + production + ","
               + imports + ","
               + imbalance + ","
               + balanceAdj + ")");
      double transport = (production - consumption - balanceAdj
                          + Math.abs(imports - imbalance)) / 2.0;
      accounting.addDistributionTransaction(broker, transport,
                                                   transport * distributionFee);
    }
  }

  // ---------- Getters and setters for settlement processsors ---------
//  @Override
//  public CapacityControl getCapacityControlService ()
//  {
//    return capacityControlService;
//  }
//  
//  @Override
//  public TariffRepo getTariffRepo ()
//  {
//    return tariffRepo;
//  }
  
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
}
