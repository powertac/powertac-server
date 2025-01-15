/*
 * Copyright 2009-2015 the original author or authors.
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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.time.Instant;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BalancingMarket;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo.CustomerClass;
import org.powertac.common.RandomSeed;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TariffTransaction.Type;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.repo.BootstrapDataRepo;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of the Distribution Utility function. Levies a per-timeslot
 * charge based on the total energy transferred to and from customers in each
 * broker's portfolio.
 * 
 * @author John Collins
 */
@Service
public class DistributionUtilityService
extends TimeslotPhaseProcessor
implements InitializationService
{
  static Logger log = LogManager.getLogger(DistributionUtilityService.class.getSimpleName());

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private BootstrapDataRepo bootstrapDataRepo;
  
  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

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

  // Parameters for original transport-based distribution fee
  @ConfigurableValue(valueType = "Boolean",
      publish = true,
      description = "If true, DU should charge for energy transport")
  private boolean useTransportFee = true;

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

  // ------------------
  // Parameters for per-customer meter charges
  @ConfigurableValue(valueType = "Boolean",
      publish = true,
      description = "If true, DU should assess fixed per-customer meter charges")
  private boolean useMeterFee = false;

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "Per-customer meter fee for small customers")
  private double mSmall = -0.015; // per timeslot

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "Per-customer meter fee for large customers")
  private double mLarge = -0.05;

  // ------------------
  @ConfigurableValue(valueType = "Boolean",
      publish = true,
      description = "If true, DU should assess transmission capacity fees")
  private boolean useCapacityFee = false;

  @ConfigurableValue(valueType = "Integer",
      publish = true,
      description = "Assessment interval in hours")
  private int assessmentInterval = 168;
  private Integer timeslotOffset = null;

  @ConfigurableValue(valueType = "Integer",
      publish = true,
      description = "Number of peaks assessed")
  private int assessmentCount = 3;


  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "Std deviation coefficient (nu)")
  private double stdCoefficient = 1.2;

  @ConfigurableValue(valueType = "Double",
      description = "multiplier for bootstrap consumption data")
  private double bootstrapConsumptionMultiplier = 1.02;

  @ConfigurableValue (valueType = "Double",
      publish = true,
      description = "Per-point fee (lambda)")
  private double feePerPoint = -18.0;

  // peak-demand dataset
  private double[] netDemand;
  private HashMap<Broker, double[]> brokerNetDemand = null;
  private double runningMean = 0.0;
  private double runningVar = 0.0;
  private double runningSigma = 0.0;
  private int runningCount = 0;
  private int lastAssessmentTimeslot = 0;

  /**
   * Computes actual distribution and balancing costs by random selection
   */
  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    int index = completedInits.indexOf("BalancingMarket");
    if (index == -1) {
      return null;
    }

    super.init();
    distributionFee = null;

    serverProps.configureMe(this);

    // init local data
    netDemand = null;
    brokerNetDemand = null;
    timeslotOffset = null;
    runningMean = 0.0;
    runningVar = 0.0;
    runningSigma = 0.0;
    runningCount = 0;
    lastAssessmentTimeslot = 0;

    // initialize peak-demand data
    if (useCapacityFee) {
      netDemand = new double[assessmentInterval];
      processBootstrapRecord();
    }

    // compute randomly-generated values if not overridden
    randomGen = randomSeedService.getRandomSeed("DistributionUtilityService",
                                                0, "model");
    if (null == distributionFee)
      distributionFee = (distributionFeeMin + randomGen.nextDouble()
                         * (distributionFeeMax - distributionFeeMin));
    if (!useTransportFee)
      distributionFee = 0.0;
    log.info("Configured DU: distroFee={}, capFee={}, feePerPoint={}, mSmall={}, mLarge={}",
             distributionFee, useCapacityFee, feePerPoint, mSmall, mLarge);
    
    serverProps.publishConfiguration(this);
    return "DistributionUtility";
  }

  private void processBootstrapRecord ()
  {
    List<Object> usage = bootstrapDataRepo.getData(CustomerBootstrapData.class);
    if (null == usage || 0 == usage.size()) {
      // boot session, ignore
      return;
    }
    // data contains usage array for each customer. Should be 14 days, 336 hrs
    double[] first = ((CustomerBootstrapData) usage.get(0)).getNetUsage();
    if (336 != first.length) {
      // note error but use it
      log.warn("First item in customer bootstrap data is {} hrs long",
               first.length);
    }
    // aggregate the usage numbers across all customers
    double[] result = new double[first.length];
    for (Object item: usage) {
      double[] data = ((CustomerBootstrapData) item).getNetUsage();
      if (data.length != first.length) {
        log.warn("Length inconsistency for record {}, length = {}",
                 ((CustomerBootstrapData) item).getCustomerName(),
                 data.length);
      }
      for (int i = 0; i < Math.min(first.length, data.length); i += 1) {
        result[i] -= data[i] * bootstrapConsumptionMultiplier;
      }
    }
    // Initialize running mean, sigma
    for (int i = 0; i < result.length; i++) {
      updateStats(result[i]);
    }
    log.info("Bootstrap data: n = {}, mean = {}, sigma = {}",
             runningCount, runningMean, runningSigma);
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
    if (useCapacityFee && null == brokerNetDemand) {
      // first time through
      brokerNetDemand = new HashMap<Broker, double[]>();
      for (Broker b: brokerList) {
        brokerNetDemand.put(b, new double[assessmentInterval]);
      }
    }

    // retrieve timeslot index and supply/demand data
    int timeslot = timeslotRepo.getTimeslotIndex(time);
    Map<Broker, Map<TariffTransaction.Type, Double>> totals =
        accounting.getCurrentSupplyDemandByBroker();

    // optionally do peak-demand assessment
    if (useCapacityFee) {
      assessCapacityFees(brokerList, timeslot, totals);
    }

    // Add distribution transactions
    for (Broker broker : brokerList) {
      double transport = 0.0;
      double distroCharge = 0.0;
      int nSmall = 0;
      int nLarge = 0;
      // meter charge is small-customers * mSmall + large customers * mLarge
      if (useMeterFee) {
        // count up individual customers
        List<TariffSubscription> subs =
                tariffSubscriptionRepo.findActiveSubscriptionsForBroker(broker);
        for (TariffSubscription sub: subs) {
          if (CustomerClass.LARGE == sub.getCustomer().getCustomerClass()) {
            nLarge += sub.getCustomersCommitted();
          }
          else {
            nSmall += sub.getCustomersCommitted();
          }
        }
        distroCharge += nLarge * mLarge;
        distroCharge += nSmall * mSmall;
        log.info("Meter charges for {}: small={}, large={}, charge={}",
                 broker.getUsername(), nSmall, nLarge, distroCharge);
      }

      if (useTransportFee) {
        // transport fee is total production + total consumption
        //    + final imbalance - balancing transactions
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
        transport =
                (production - consumption - balanceAdj
                        + Math.abs(imports - imbalance)) / 2.0;
        distroCharge += transport * distributionFee;
      }
      accounting.addDistributionTransaction(broker, nSmall, nLarge, transport, distroCharge);
    }
  }

  void
  assessCapacityFees (List<Broker> brokerList, int timeslot,
                      Map<Broker, Map<TariffTransaction.Type, Double>> totals)
  {
    // make sure we know the timeslot offset
    if (null == timeslotOffset) {
      timeslotOffset = timeslot;
      lastAssessmentTimeslot = timeslot;
      log.info("Start timeslot {}, timeslotOffset = {}",
               timeslot, timeslotOffset);
    }
    else if (0 == (timeslot - timeslotOffset) % assessmentInterval) {
      // do the assessment
      log.info("Peak-demand assessment at timeslot {}", timeslot);
      // discover the over-threshold peaks in the netDemand array
      double threshold = runningMean + stdCoefficient * runningSigma;
      List<PeakEvent> peaks = new ArrayList<PeakEvent>();
      for (int i = 0; i < netDemand.length; i++) {
        if (netDemand[i] >= threshold) {
          // gather peak
          peaks.add(new PeakEvent(netDemand[i], i));
        }
      }
      log.info("{} peaks found above threshold {}", peaks.size(), threshold);
      if (peaks.size() > 0) {
        // sort the peak events and assess charges
        peaks.sort(null);
        Map<Broker, Double> brokerCharge = new HashMap<Broker, Double>();
        for (PeakEvent peak: peaks.subList(0, Math.min(assessmentCount,
                                                       peaks.size()))) {
          double excess = peak.value - threshold;
          double charge = excess * feePerPoint;
          for (Broker broker: brokerList) {
            // charge for broker comes from broker_usage/peak.value
            double[] brokerDemand = brokerNetDemand.get(broker);
            double cost = charge * brokerDemand[peak.index]
                / netDemand[peak.index];
            brokerCharge.put(broker, cost);
            double brokerExcess = 
                excess * brokerDemand[peak.index]/ netDemand[peak.index];
            accounting.addCapacityTransaction(broker,
                                              lastAssessmentTimeslot + peak.index,
                                              threshold, brokerExcess, cost);
          }
          if (log.isInfoEnabled()) {
            double pts = peak.value - threshold;
            StringBuilder sb =
                new StringBuilder(String.format("Peak at ts %d, pts=%.3f, charge=%.3f (",
                                                peak.index + timeslot - assessmentInterval,
                                                pts, charge));
            for (Broker broker: brokerCharge.keySet()) {
              sb.append(String.format("%s:%.3f, ",
                                      broker.getUsername(),
                                      brokerCharge.get(broker)));
            }
            sb.append(")");
            log.info(sb.toString());
          }
        }
      }
      else {
        // no peaks this time. Send dummy capacity tx to convey theshold info
        for (Broker broker: brokerList) {
          accounting.addCapacityTransaction(broker, timeslot, threshold,
                                            0.0, 0.0);
        }
      }
      // record time of last assessment
      lastAssessmentTimeslot = timeslot;
    }
    // keep track of demand peaks for next assessment
    recordNetDemand(timeslot, brokerList, totals);
  }

  // Records hourly net demand, updates running stats
  private void recordNetDemand (int timeslot, List<Broker> brokerList,
                                Map<Broker, Map<Type, Double>> totals)
  {
    int index = (timeslot - timeslotOffset) % assessmentInterval;
    double totalConsumption = 0.0;
    double totalProduction = 0.0;
    for (Broker broker: brokerList) {
      // pull up the netDemand array for this broker
      double[] brokerDemand = brokerNetDemand.get(broker);
      if (null == brokerDemand) {
        log.warn("Broker {} not in brokerNetDemand map", broker.getUsername());
        brokerDemand = new double[assessmentInterval];
        brokerNetDemand.put(broker, brokerDemand);
      }
      // update net demand for this ts
      Map<TariffTransaction.Type, Double> data = totals.get(broker);
      if (null == data) {
        // zero out this broker
        brokerDemand[index] = 0.0;
      }
      else {
        double consumption = data.get(Type.CONSUME);
        double production = data.get(Type.PRODUCE);
        //double netConsumption = 
        //-(data.get(Type.PRODUCE) + data.get(Type.CONSUME));
        
        brokerDemand[index] = -(consumption + production);
        totalConsumption += consumption;
        totalProduction += production;
      }
    }
    double netConsumption = -(totalConsumption + totalProduction);
    log.info("ts {}: consumption = {}, production = {}, net = {}",
             timeslot, totalConsumption, totalProduction,
             netConsumption);
    netDemand[index] = netConsumption;
    // Update running mean and var
    if (runningCount == 0) {
      // first time through, assume this is a boot session
      runningMean = netConsumption;
      runningVar = 0.0;
      runningCount = 1;
    }
    else {
      // use recurrence formula to update mean, sigma
      updateStats(netConsumption);
      log.info("Net demand k = {}, mean = {}, sigma = {}",
               runningCount, runningMean, runningSigma);
    }
  }

  // Runs the recurrence formula for computing mean, sigma
  private void updateStats (double netConsumption)
  {
    double lastM = runningMean;
    runningCount += 1;
    runningMean = lastM + (netConsumption - lastM) / runningCount;
    runningVar = runningVar +
        (netConsumption - lastM) * (netConsumption - runningMean);
    runningSigma = Math.sqrt(runningVar / (runningCount - 1.0));
  }

  // ---------- parameter getters -- test support ---------

  /**
   * True just in case the transport fee is being assessed
   */
  boolean usingTransportFee ()
  {
    return useTransportFee;
  }

  /**
   * Returns the minimum value for the per-kWh distribution fee.
   */
  double getDistributionFeeMin ()
  {
    return distributionFeeMin;
  }

  /**
   * Returns the maximum value for the per-kWh distribution fee.
   */
  double getDistributionFeeMax ()
  {
    return distributionFeeMax;
  }

  /**
   * Returns the game value for the per-kWh distribution fee.
   * 
   */
  Double getDistributionFee ()
  {
    if (null == distributionFee)
      return 0.0;
    else
      return distributionFee;
  }

  /**
   * True just in case per-meter charges are being assessed.
   */
  boolean usingMeterFee ()
  {
    return useMeterFee;
  }

  /**
   * Returns the per-timeslot meter charge for small customers.
   */
  double getMSmall ()
  {
    return mSmall;
  }

  /**
   * Returns the per-timeslot meter charge for large customers.
   */
  double getMLarge ()
  {
    return mLarge;
  }

  /**
   * True just in case capacity (peak-demand) fees are being assessed.
   */
  boolean usingCapacityFee ()
  {
    return useCapacityFee;
  }

  /**
   * Returns the assessment interval for peak-demand measurement.
   */
  int getAssessmentInterval ()
  {
    return assessmentInterval;
  }

  /**
   * Returns the coefficient nu used in peak-demand assessment, in the formula
   * points = mean + coeff * sigma.
   */
  double getStdCoefficient ()
  {
    return stdCoefficient;
  }

  /**
   * Returns the fee assessed per point for peak demand at each assessment
   * interval.
   */
  double getFeePerPoint ()
  {
    return feePerPoint;
  }

  double getRunningMean ()
  {
    return runningMean;
  }

  double getRunningVar ()
  {
    return runningVar;
  }

  double getRunningSigma ()
  {
    return runningSigma;
  }

  int getRunningCount ()
  {
    return runningCount;
  }

  int getLastAssessmentTimeslot ()
  {
    return lastAssessmentTimeslot;
  }

  // -------- Delegation methods for backward compatibility -------
  // The only purpose of these methods is to produce configuration data for
  // older brokers. Should not be called by server code.

  /**
   * @deprecated
   * For backward-compatibility only - should not be called.
   */
  @ConfigurableValue(valueType = "Double", dump=false,
      name = "balancingCost",
      publish = true,
      description = "Low end of distribution fee range")
  public double getBalancingCost ()
  {
    return balancingMarket.getBalancingCost();
  }


  /**
   * @deprecated
   * For backward-compatibility only - should not be called.
   */
  @ConfigurableValue(valueType = "Double",
      name = "pPlusPrime",
      publish = true, dump=false,
      description = "Slope of up-regulation cost function")
  public double getPPlusPrime ()
  {
    return balancingMarket.getPPlusPrime();
  }


  /**
   * @deprecated
   * For backward-compatibility only - should not be called.
   */
  @ConfigurableValue(valueType = "Double",
      name = "pMinusPrime",
      publish = true, dump=false,
      description = "slope of down-regulation cost function")
  public double getPMinusPrime()
  {
    return balancingMarket.getPMinusPrime();
  }


  /**
   * @deprecated
   * For backward-compatibility only - should not be called.
   */
  @ConfigurableValue(valueType = "Double",
      name = "defaultSpotPrice",
      publish = true, dump=false,
      description = "value used for spot price/MWh if unavailable from market")
  public double getDefaultSpotPrice()
  {
    return balancingMarket.getDefaultSpotPrice();
  }

  // Sortable data structure for tracking peak-demand events
  class PeakEvent implements Comparable<PeakEvent>
  {
    double value = 0.0;
    int index = 0;

    PeakEvent (double val, int idx)
    {
      super();
      value = val;
      index = idx;
    }

    @Override
    public int compareTo (PeakEvent o)
    {
      if (this.value < o.value)
        return 1;
      else if (this.value > o.value)
        return -1;
      else
        // make comparison consistent with equals
        return this.index - o.index;
    }
  }
}
