/**
 * Copyright (c) 2022 by John Collins and Philipp Page.
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
package org.powertac.customer.evcharger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.powertac.common.CapacityProfile;
import org.powertac.common.CustomerInfo;
import org.powertac.common.CustomerInfo.CustomerClass;
import org.powertac.common.RandomSeed;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.state.Domain;
import org.powertac.customer.AbstractCustomer;

/**
 * This is a population model of Electric Vehicle chargers. 
 * 
 * @author John Collins, Philipp Page
 */
@Domain
@ConfigurableInstance
public class EvCharger extends AbstractCustomer implements CustomerModelAccessor
{
  static private Logger log = LogManager.getLogger(EvCharger.class.getName());

  @ConfigurableValue(valueType = "String", publish = false, bootstrapState = false,
          dump = true, description = "Name of statistical model config file")
  private String model = "dummy.xml";

  @ConfigurableValue(valueType = "Double", publish = true, bootstrapState = true,
          dump = true, description = "Population of chargers")
  private double population = 1000.0;

  @ConfigurableValue(valueType = "Integer", publish = true, bootstrapState = true,
          dump = true, description = "Minimum population subscribing to a specific tariff")
  private int minimumChunkSize = 20;

  @ConfigurableValue(valueType = "Double", publish = true, bootstrapState = true,
          dump = true, description = "Individual Charger capacity in kW")
  private double chargerCapacity = 8.0;

  @ConfigurableValue(valueType = "Double", publish = false, bootstrapState = true,
          dump = true, description = "Where in the min-max range we compute nominal demand")
  private double nominalDemandBias = 0.5;

  @ConfigurableValue(valueType = "Integer", publish = false, bootstrapState = true,
          dump = true, description = "Maximum horizon for individual charging demand elements")
  private int maxDemandHorizon = 96; // 4 days?

  @ConfigurableValue(valueType = "String",
          publish = false, dump = false, bootstrapState = true,
          description = "Collected demand statistics at end of boot session")
  private String storageRecord;

  // Tariff terms could affect this
  @ConfigurableValue(valueType = "Double", publish = false, bootstrapState = false,
          dump = true, description = "Portion of flexibility to hold back")
  private double defaultFlexibilityMargin = 0.02; // 2% on both ends

  // Default tariff-eval profile, configurable through defaultCapacityData
  // Values are hourly per-charger.
  private CapacityProfile defaultCapacityProfile = null;

  @ConfigurableValue (valueType = "Double", publish = false,
          description = "Probability customer will ignore tariff publications")
  private double evalInertia = 0.9;

  @ConfigurableValue (valueType = "Double", publish = false,
          description = "Expected up-regulation ratio")
  private double evalUpreg = -0.3;

  @ConfigurableValue (valueType = "Double", publish = false,
          description = "Expected down-regulation ratio")
  private double evalDownreg = 0.3;

  @ConfigurableValue (valueType = "Double", publish = false,
          description = "Influence of utility on tariff choice")
  private double evalRationality = 0.8;

  @ConfigurableValue (valueType = "Boolean", publish = false,
          description = "If true, then re-evaluate all tariffs in each cycle")
  private boolean evaluateAll = true;

  @ConfigurableValue (valueType = "List", dump = false,
          description = "default expected hourly consumption in kWh/charger, comma-separated values")
  private List<String> defaultCapacityData = null;

  // for tariffs that do not have weekly TOU rates, we stick with a 24-hour profile.
  @ConfigurableValue (valueType = "Integer", publish = false, bootstrapState = true,
          description = "periodicity of the customer data model")
  private int defaultProfileSize = 24;
  
  private ArrayList<ArrayList<DemandElement>> demandInfoMean;
  private int[] demandInfoMeanCounter;

  // The count is the product of the number of timeslots in the default profile,
  // times the number of days that have been averaged by demandInfoMean
  //@ConfigurableValue(valueType = "Integer", publish = false, bootstrapState = true,
  //        description = "number of samples represented by demandInfoMean")
  //private int demandInfoMeanCount;

  private PowerType powerType = PowerType.ELECTRIC_VEHICLE;
  private RandomSeed evalSeed;
  private RandomSeed demandSeed;
  private HashMap<TariffSubscription, StorageState> subState;
  private HashMap<Tariff, TariffInfo> tariffInfo;
  private TariffEvaluator tariffEvaluator;
  private DemandSampler demandSampler;
  private double epsilon = 1e-9; // small number for numeric accuracy testing

  /**
   * Default constructor, requires manual setting of name
   */
  public EvCharger ()
  {
    super();
  }

  /**
   * Constructor with name
   */
  public EvCharger (String name)
  {
    super(name);
  }

  @Override
  public void initialize ()
  {
    super.initialize();
    log.info("Initialize " + name);
    // fill out CustomerInfo
    CustomerInfo info = new CustomerInfo(name, (int) Math.round(population));
    info.withPowerType(powerType)
      .withCustomerClass(CustomerClass.SMALL)
      //.withUpRegulationKW(-100.0)
      //.withDownRegulationKW(100.0)
      .withMultiContracting(true);
    addCustomerInfo(info);
    ensureSeeds();

    // Initialize demandInfoMean
    if (null == demandInfoMean) {
      demandInfoMean = new ArrayList<ArrayList<DemandElement>>();
      //demandInfoMeanCount = 0;
      demandInfoMeanCounter = new int[defaultProfileSize];
      //demandInfoMeanWeight = new double[defaultProfileSize];
    }

    // set up the tariff information map
    tariffInfo = new HashMap<>();

    // set up the tariff evaluator. We are wide-open to variable pricing.
    tariffEvaluator = createTariffEvaluator(this);
    tariffEvaluator.withPreferredContractDuration(14)
    .withChunkSize(minimumChunkSize)
    .withInertia(evalInertia)
    .withRationality(evalRationality)
    .withEvaluateAllTariffs(evaluateAll);
    // no problem with tou or interruptible rates, not ready for variable rates
    tariffEvaluator.initializeInconvenienceFactors(0.0, 0.4, 0.0);
    tariffEvaluator.initializeRegulationFactors(evalUpreg, 0.0, evalDownreg);
    demandSampler = new DemandSampler();
    demandSampler.initialize(model, getDemandSeed());
  }

  // Initialize the demandInfoMean structure -- package visibility for testing
  void initDemandInfoMean ()
  {
    int repeat = 168;
    DateTime currentTime = service.getTimeService().getCurrentDateTime();
    for (int i = 0; i < repeat; i++) {
      getDemandInfo(currentTime.plus(i * TimeService.HOUR));
    }
  }

  // Called from CustomerModelService after initialization.
  // We don't need to do much here for this model, the rest
  // will be handled in step() during the first timeslot.
  @Override
  public void handleInitialSubscription (List<TariffSubscription> subscriptions)
  {
    // Should be exactly one
    //if (subscriptions.size() != 1) {
    //  log.error("Should be only one subscription, but saw {}", subscriptions.size());
    //}
    // log the initial subscription
    for (TariffSubscription sub : subscriptions) {
      log.info("initial subscription for {}", getCustomerInfo().getName());      
    }
    // we assume the new StorageState and TariffInfo instances will be
    // initialized on the first step. No subscription changes should
    // occur before that.
  }

  // Gets a new random-number opSeed just in case we don't already have one.
  // Useful for mock-based testing.
  private void ensureSeeds ()
  {
    if (null == evalSeed) {
      RandomSeedRepo repo = service.getRandomSeedRepo();
      evalSeed = repo.getRandomSeed(EvCharger.class.getName() + "-" + name, 0, "eval");
      demandSeed = repo.getRandomSeed(EvCharger.class.getName(), maxDemandHorizon, model);
    }
  }

  @Override
  public CustomerInfo getCustomerInfo ()
  {
    return getCustomerInfo(powerType);
  }

  /**
   * Creates, if necessary, and returns the configured
   * default capacity profile, presumably suitable for
   * a flat-rate tariff.
   */
  public CapacityProfile getDefaultCapacityProfile ()
  {
    if (null == defaultCapacityProfile) {
      if (null == defaultCapacityData) {
        // we need to always return a non-null value here
        log.error("Empty default capacity data");
        return null;
      }
      double[] dcp = new double[defaultCapacityData.size()];
      int index = 0;
      for (String item : defaultCapacityData) {
        dcp[index++] = Double.valueOf(item); //* getPopulation();
      }
      defaultCapacityProfile = new CapacityProfile(dcp, lastSunday());
    }
    return defaultCapacityProfile;
  }

  /**
   * Returns the mean hourly capacity per-charger, or the mean value of
   * the defaultCapacityProfile
   */
  public double getMeanDefaultCapacity ()
  {
    double sum = 0.0;
    double[] profile = getDefaultCapacityProfile().getProfile();
    for (double val : profile) {
      sum += val;
    }
    return sum / profile.length;
  }

  // test-support method, package visibility
  void setDefaultCapacityData (List<String> data)
  {
    defaultCapacityData = data;
  }

  @Override
  public CapacityProfile getCapacityProfile (Tariff tariff)
  {
    // return existing capacityProfile if it exists
    return getTariffInfo(tariff).getCapacityProfile();
  }

  // Returns the TariffInfo for this tariff, creating it if necessary
  TariffInfo getTariffInfo (Tariff tariff)
  {
    TariffInfo result = tariffInfo.get(tariff);
    if (null == result) {
      result = new TariffInfo (this, tariff);
      tariffInfo.put(tariff, result);
    }
    return result;
  }

  /**
   *  Returns the random seed for generating demand
   */
  public RandomSeed getDemandSeed ()
  {
    return demandSeed;
  }

  @Override
  public double getBrokerSwitchFactor (boolean isSuperseding)
  {
    if (isSuperseding)
      return 0;
    else
      return 0.02;

  }

  @Override
  public double getTariffChoiceSample ()
  {
    return evalSeed.nextDouble();
  }

  @Override
  public double getInertiaSample ()
  {
    return evalSeed.nextDouble();
  }

  @Override
  public double getShiftingInconvenienceFactor (Tariff tariff)
  {
    // TODO Auto-generated method stub
    return 0.0;
  }

  /**
   * Called when some portion of the population switches from one tariff to another. This
   * is a customer model, so after the initial subscription (see handleInitialSubscription)
   * every member of the population is always subscribed to some tariff or another.
   */
  @Override
  public void notifyCustomer (TariffSubscription oldsub, TariffSubscription newsub, int count)
  {
    // We assume count is positive
    if (count < 0) {
      log.error("Negative population in notifyCustomer({}, {} {})", oldsub.getId(), newsub.getId(), count);
      return;
    }
    else if (count == 0) {
      // unexpected, but not exactly an error
      log.warn("Zero population in notifyCustomer({}, {}, 0)", oldsub.getId(), newsub.getId());
      return;
    }

    // Now we do the transfer
    int timeslotIndex = service.getTimeslotRepo().currentSerialNumber();
    StorageState oldss = getStorageState(oldsub);
    // Should not be null
    if (null == oldss) {
      log.error("Null StorageState on subscription {}", oldsub.getId());
      return;
    }
    StorageState newss = getStorageState(newsub);
    if (null == newss) {
      // Need to set up a new SS
      newss = new StorageState(newsub, getChargerCapacity(), getMaxDemandHorizon())
              .withUnitCapacity(getChargerCapacity());
      setStorageState(newsub, newss);
    }
    newss.moveSubscribers(timeslotIndex, count, oldss);
  }

  void setStorageState (TariffSubscription sub, StorageState ss)
  {
    if (null == subState) {
      // lazy initialization
      subState = new HashMap<>();
    }
    subState.put(sub, ss);
  }

  StorageState getStorageState (TariffSubscription sub)
  {
    if (null == subState) {
      return null;
    }
    return subState.get(sub);
  }

  @Override
  public void step ()
  {
    // In each timeslot, we first distribute regulation from the previous timeslot,
    // and then collapse and re-balance storage state histograms for each subscription.
    DateTime currentTime = service.getTimeService().getCurrentDateTime();
    int timeslotIndex = service.getTimeslotRepo().currentSerialNumber();
    log.info("Step at ts {}", timeslotIndex);

    // if needed, set up StorageState for the initial subscription to the default
    // consumption tariff. If this is a sim session, then the saved storage record needs
    // to be restored.
    List<TariffSubscription> subs = service.getTariffSubscriptionRepo()
            .findActiveSubscriptionsForCustomer(getCustomerInfo());
    if (0 == subs.size()) {
      log.error("No subscriptions at step {}", timeslotIndex);
    }
    else if (null == subState) {
      // First step, could be boot or sim session.
      // We decorate initial subscription with StorageState and TariffInfo,
      // and initialize the state if we're in a sim session.
      TariffSubscription sub = subs.get(0);
      TariffInfo ti = getTariffInfo(sub.getTariff());
      ti.setCapacityProfile(getDefaultCapacityProfile());
      if (null != storageRecord) {
        // sim session;
        StorageState initialSS =
          StorageState.restoreState(getChargerCapacity(), sub,
                                    getMaxDemandHorizon(), storageRecord);
        setStorageState(sub, initialSS);
      }
      else {
        // boot session, need to set up ss
        StorageState initialSS = new StorageState(sub, getChargerCapacity(),
                                                  getMaxDemandHorizon());
        setStorageState(sub, initialSS);
      }
      if (0 == demandInfoMean.size()) {
        log.info("Initializing demandInfoMean");
        //List<Object> data = (List<Object>) demandRecord;
        initDemandInfoMean();
      }
    }

    if (timeslotIndex > 0) {
      // Now we handle regulation, unless this is the first ts of a boot session
      for (TariffSubscription sub : subs) {
        StorageState ss = getStorageState(sub);
        // regulation must be distributed before distributing future demand
        log.info("{} regulation for tariff {} = {}", getCustomerInfo().getName(),
                 sub.getTariff().getId(), sub.getRegulation());
        double residue = ss.distributeRegulation(timeslotIndex, sub.getRegulation());
        if (Math.abs(residue) > epsilon) {
          log.error("Attempt to distribute regulation {} in ts {} left residue of {}",
                    sub.getRegulation(), timeslotIndex, residue);
        }
        // after regulation, we must collapse StorageState arrays and rebalance
        ss.collapseElements(timeslotIndex);
        ss.rebalance(timeslotIndex);
      }
    }

    // Next, we sample demand distributions for the current date/time
    // assume sample is list of (horizon, activation count, [distribution]) structs
    // in which the lengths of the distribution arrays are equal to horizon + 1
    // get current demand
    List<DemandElement> newDemand = getDemandInfo(currentTime);
    if (newDemand.size() < 4) {
      log.info("short demand sample {}", newDemand.size());
    }
    else {
      for (int i = 0; i < 4; i++) {
        DemandElement de = newDemand.get(i);
        log.info("New demand h={} nv={} d={}",
                 de.getHorizon(), de.getNVehicles(),
                 Arrays.toString(de.getdistribution()));
      }
    }

    // adjust for current weather?

    // iterate over subscriptions
    for (TariffSubscription sub: service.getTariffSubscriptionRepo()
            .findActiveSubscriptionsForCustomer(getCustomerInfo())) {
      // should ss compute these values? No.
      double ratio = (double) sub.getCustomersCommitted() / population;
      StorageState ss = getStorageState(sub);
      ss.distributeDemand(timeslotIndex, newDemand, ratio);
      double[] limits = ss.getMinMax(timeslotIndex);
      log.info("nominalDemandBias = {}", nominalDemandBias);
      double nominalDemand = computeNominalDemand(currentTime, sub, limits);
      log.info("Sub {}: Use power min={}, max={}, nominal={}",
               sub.getId(), limits[0], limits[1], nominalDemand);
      ss.distributeUsage(timeslotIndex, nominalDemand);
      sub.usePower(nominalDemand);
      
      RegulationCapacity rc = computeRegulationCapacity(sub, nominalDemand,
                                                        limits[0], limits[1]);
      if (null != rc) {
        // if this subscription will compensate us for regulation, we'll report
        // our available capacity
        sub.setRegulationCapacity(rc);
      }
    }
  }

  // Computes nominal demand for the current timeslot based on tariff terms
  private double computeNominalDemand (DateTime time,
                                       TariffSubscription sub,
                                       double[] minMax)
  {
    double result = 0.0;
    TariffInfo info = getTariffInfo(sub);
    if (!info.isTOU()
            && !info.isVariableRate()) {
      // For flat-rate consumption tariffs, we go for a relatively flat profile
      // which we get by applying the default nominalDemandBias.
      // This also maximizes flexibility.
      result = minMax[0] + nominalDemandBias * (minMax[1] - minMax[0]);
    }
    // handle other types here
    else {
      // if price will drop in the near future, go closer to min,
      // if it will rise in the near future, go closer to max. How close is a
      // function of price variation magnitude and value pf flexibility
      result = minMax[0] + info.getDemandBias() * (minMax[1] - minMax[0]);
    }
    return result;
  }

  // Computes the regulation capacity to be reported on a subscription
  private RegulationCapacity
  computeRegulationCapacity (TariffSubscription sub,
                             double actualDemand, double minDemand, double maxDemand)
  {
    return new RegulationCapacity(sub,
                                  (actualDemand - minDemand)
                                  / sub.getCustomersCommitted(),
                                  (actualDemand - maxDemand)
                                  / sub.getCustomersCommitted());
  }

  // -------------------------- Evaluate tariffs ------------------------
  @Override
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    log.info(getName() + ": evaluate tariffs");
    tariffEvaluator.evaluateTariffs();
  }

  // Returns the TariffInfo associated with this Subscription
  TariffInfo getTariffInfo (TariffSubscription sub)
  {
    return getTariffInfo(sub.getTariff());
  }

  // ------------------- AbstractCustomer methods ------------------------
  /**
   * Bootstrap data is just the content of the StorageState.
   * For this we need to know the timeslot index of the first timeslot in the
   * sim session.
   */
  //@SuppressWarnings("unchecked")
  @Override
  public void saveBootstrapState ()
  {
    log.info("saveBootstrapState");
    int timeslot = service.getTimeslotRepo().currentSerialNumber();
    List<TariffSubscription> subs =
      service.getTariffSubscriptionRepo().findActiveSubscriptionsForCustomer(getCustomerInfo());
    if (subs.size() > 1) {
      // If there's more than one in a bootstrap session, we 
      log.warn("{} subscriptions, should be just one", subs.size());
    }
    TariffSubscription sub = subs.get(0);
    StorageState finalState = getStorageState(sub);
    if (null == finalState) {
      log.error("saveBootstrapState() null finalState");
    }
    else {
      storageRecord =
            finalState.gatherState(timeslot);
    }
  }

  /**
   * Returns a vector of DemandElement instances representing the number of
   * vehicles that plug in now and unplug in a future timeslot,
   * and how needed energy is distributed.
   */
  public List<DemandElement> getDemandInfo (DateTime time)
  {
    List<DemandElement> demandInfo = new ArrayList<DemandElement>();
    if (!demandSampler.isEnabled()) {
      log.error("Demand sampler is currently disabled due to an internal error. Returning empty demand info.");
      return demandInfo;
    }
    try {
      int hod = time.getHourOfDay();
      demandInfo = demandSampler.sample(hod, (int) getPopulation(), chargerCapacity);
      updateDemandInfoMean(demandInfo, hod);
    }
    catch (IllegalArgumentException e) {
      log.error("Cannot sample new demandInfo due to an invalid argument. Returning empty demand info: " + e);
    }
    catch (Exception e) {
      log.error("Cannot sample new demand info. Returning emtpy demand info: " + e);
    }

    return demandInfo;
  }
  
  // package visibility to support testing
  void updateDemandInfoMean (List<DemandElement> demandInfo, int hod)
  {
    if (hod > demandInfoMean.size()) {
      // should not get here
      log.error("demandInfoMean not initialized at tod = {}", hod);
      return;
    }
    int count = demandInfoMeanCounter[hod];
    
    // We clone the DemandElements so that the modifications to the
    // distributions do not affect the actual demandInfo object on the heap.
    // We also scale mean values to population = 1.0 for correct tariff eval.
    double scale = 1.0 / getPopulation();
    if (0 == count) {
      // First entry, clone the demandInfo
      demandInfoMean.add((ArrayList<DemandElement>) demandInfo.stream()
                         .map(de -> new DemandElement(de, scale))
                         .collect(Collectors.toList()));      
    }
    else {
      // retrieve the current mean value and proceed
      ArrayList<DemandElement> currentDemandInfoMean = demandInfoMean.get(hod);
      for (DemandElement de: demandInfo) {
        if (de.getHorizon() < currentDemandInfoMean.size()) {
          // In this case we need to update the means for the existing
          // DemandElement.
          DemandElement currentDemandElementMean =
                  currentDemandInfoMean.get(de.getHorizon());
          double[] currentEnergyHistogram =
                  currentDemandElementMean.getdistribution();
          double[] newEnergyHistogram =
                  new double[currentEnergyHistogram.length];
          // preserve invariant
          double histogramSum = 0.0;
          double currentWeight = count * currentDemandElementMean.getNVehicles();
          if (0.0 != (currentWeight + de.getNVehicles())) {
            for (int i = 0; i < currentEnergyHistogram.length; i++) {
              newEnergyHistogram[i] =
                      (currentEnergyHistogram[i] * currentWeight
                              + de.getdistribution()[i] * de.getNVehicles())
                      / (currentWeight + de.getNVehicles());
              histogramSum += newEnergyHistogram[i];
            }
          }
          if (histogramSum > epsilon) {
            double invariantError = histogramSum - 1.0;
            if (Math.abs(invariantError) > epsilon) {
              // restore invariant
              double ratio = 1.0 / histogramSum;
              log.info("DemandInfoMean restoring invariant at hod={} by {}",
                       hod, ratio);
              for (int i = 0; i < newEnergyHistogram.length; i++) {
                newEnergyHistogram[i] *= ratio;
              }
            }
          }
          currentDemandElementMean.setDistribution(newEnergyHistogram);
          double meanVehicles =
                  ((currentDemandElementMean.getNVehicles() * count)
                          + (de.getNVehicles() * scale))
                  / (count + 1);
          currentDemandElementMean.setNVehicles(meanVehicles);
        }
        else {
          // otherwise the horizon is longer than the mean horizon,
          // so we add this one to the end. Probably we should make sure
          // de,horizon is currentDemandInfoMean.size() + 1.
          currentDemandInfoMean.add(new DemandElement(de, scale));
        }
      }
    }
    // Finally, we update the per-hour and overall counts
    demandInfoMeanCounter[hod] += 1;
    //demandInfoMeanCount += 1;
  }
  
  public List<ArrayList<DemandElement>> getDemandInfoMean ()
  {
    return demandInfoMean;
  }

  // getters and setters, package visibility
  double getPopulation ()
  {
    return population;
  }

  EvCharger withPopulation (double population)
  {
    this.population = population;
    return this;
  }

  double getChargerCapacity ()
  {
    return chargerCapacity;
  }

  int getMaxDemandHorizon ()
  {
    return maxDemandHorizon;
  }

  EvCharger withChargerCapacity (double capacity)
  {
    chargerCapacity = capacity;
    return this;
  }

  double getNominalDemandBias ()
  {
    return nominalDemandBias;
  }

  EvCharger withNominalDemandBias (double bias)
  {
    nominalDemandBias = bias;
    return this;
  }

  TariffEvaluator getTariffEvaluator ()
  {
    return tariffEvaluator;
  }
}
