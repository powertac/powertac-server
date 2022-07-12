/**
 * Copyright (c) 2022 by John Collins.
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;
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
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.state.Domain;
import org.powertac.customer.AbstractCustomer;

/**
 * 
 * @author John Collins
 */
@Domain
@ConfigurableInstance
public class EvCharger extends AbstractCustomer implements CustomerModelAccessor
{
  static private Logger log = LogManager.getLogger(EvCharger.class.getName());

  @ConfigurableValue(valueType = "String", publish = false, bootstrapState = false,
          dump = true, description = "Name of statistical model config file")
  private String model = "dummy";

  @ConfigurableValue(valueType = "Double", publish = true, bootstrapState = true,
          dump = true, description = "Population of chargers")
  private double population = 1000.0;

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
          description = "State of active chargers at end of boot session")
  private String storageRecord;

  // Tariff terms could affect this
  @ConfigurableValue(valueType = "Double", publish = false, bootstrapState = false,
          dump = true, description = "Portion of flexibility to hold back")
  private double defaultFlexibilityMargin = 0.02; // 2% on both ends

  // Default tariff-eval profile, configurable in the setter
  // Values are hourly per-vehicle. Mean hourly value should be between 0.2 and 0.5 kWh,
  // or a bit less than 4 kWh for a vehicle that drives 20000 km/year and gets around 2.2 kWh/km
  private CapacityProfile defaultCapacityProfile = null;

  @ConfigurableValue (valueType = "String", dump = false,
          description = "default expected hourly consumption in kWh/vehicle, comma-separated values")
  private String defaultCapacityData = null;

  // for tariffs that do not have weekly TOU rates, we stick with a 24-hour profile.
  private int defaultProfileSize = 24; 
  
  // Keeps track of mean demand info lists per hour of day.
  private final Map<Integer, List<DemandElement>> demandInfoMean = new HashMap<>();
  // Counter to update the rolling mean demand info lists dynamically.
  private Map<Integer, Integer> demandInfoMeanCounter = new HashMap<>();

  private PowerType powerType = PowerType.ELECTRIC_VEHICLE;
  private RandomSeed evalSeed;
  private RandomSeed demandSeed;
  private HashMap<TariffSubscription, StorageState> subState;
  private HashMap<Tariff, TariffInfo> tariffInfo;
  private TariffEvaluator tariffEvaluator;
  private DemandSampler demandSampler;

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
      .withUpRegulationKW(-100.0)
      .withDownRegulationKW(100.0);
    addCustomerInfo(info);
    ensureSeeds();

    // set up the subscription-state mapping
    subState = new HashMap<>();
    tariffInfo = new HashMap<>();

    // handle the bootstrap state if present
    // TODO - remove if not needed
    //if (null != storageRecord) {
    //  // Should be one SS record, for the default EV tariff.
    //}

    // set up the tariff evaluator. We are wide-open to variable pricing.
    tariffEvaluator = createTariffEvaluator(this);
    tariffEvaluator.withInertia(0.5).withPreferredContractDuration(14);
    tariffEvaluator.initializeInconvenienceFactors(0.0, 0.01, 0.0, 0.0);
    tariffEvaluator.initializeRegulationFactors(-0.1*population, 0.0, 0.1*population);
    demandSampler = new DemandSampler();
    demandSampler.initialize(model, getDemandSeed());
  }

  // Called from CustomerModelService after initialization.
  // We don't need to do much here for this model, the rest
  // will be handled in step() during the first timeslot.
  @Override
  public void handleInitialSubscription (List<TariffSubscription> subscriptions)
  {
    // Should be exactly one
    if (subscriptions.size() != 1) {
      log.error("Should be only one subscription, but saw {}", subscriptions.size());
    }
    // log the initial subscription
    log.info("initial subscription for {}", getCustomerInfo().getName());
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
      String[] vals = defaultCapacityData.split("-");
      double[] dcp = new double[vals.length];
      for (int i = 0; i < vals.length; i++) {
        dcp[i] = Double.valueOf(vals[i]) * getPopulation();
      }
      defaultCapacityProfile = new CapacityProfile(dcp, lastSunday());
    }
    return defaultCapacityProfile;
  }

  // test-support method, package visibility
  void setDefaultCapacityData (String data)
  {
    defaultCapacityData = data;
  }

  // private Map<Tariff, TariffInfo> TariffProfiles = null;
  @Override
  public CapacityProfile getCapacityProfile (Tariff tariff)
  {
    // TODO make tariff-specific profiles, multiply by expected tariff population
    //DateTime currentTime = service.getTimeService().getCurrentDateTime();
    //return new CapacityProfile(profile, currentTime.toInstant());
    return getDefaultCapacityProfile();
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
    else if (1 == subs.size()) {
      // We just have the initial subscription.
      // We must decorate it with its StorageState and TariffInfo,
      // and initialize the state if we're in a sim session.
      TariffSubscription sub = subs.get(0);
      StorageState initialSS = new StorageState(sub, getChargerCapacity(), getMaxDemandHorizon())
              .withUnitCapacity(getChargerCapacity());
      setStorageState(sub, initialSS);
      if (null != storageRecord)
        // sim session
        initialSS.restoreState(timeslotIndex, storageRecord);
      TariffInfo ti = new TariffInfo(sub.getTariff());
      ti.setCapacityProfile(getDefaultCapacityProfile());
    }

    if (timeslotIndex > 0) {
      // Now we handle regulation, unless this is the first ts of a boot session
      for (TariffSubscription sub : subs) {
        StorageState ss = getStorageState(sub);
        // regulation must be distributed before distributing future demand
        log.info("{} regulation for sub {} = {}", getCustomerInfo().getName(),
                 sub.getId(), sub.getRegulation());
        ss.distributeRegulation(timeslotIndex, sub.getRegulation());
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
    //log.info("New demand {}", newDemand);

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
      double nominalDemand = computeNominalDemand(sub, limits);
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
  private double computeNominalDemand (TariffSubscription sub, double[] minMax)
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
                                  actualDemand - minDemand,
                                  actualDemand - maxDemand);
  }

  // -------------------------- Evaluate tariffs ------------------------
  @Override
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    log.info(getName() + ": evaluate tariffs");
    tariffEvaluator.evaluateTariffs();
  }

  // Returns the TariffInfo associated with this Tariff, creating it if necessary
  TariffInfo getTariffInfo (Tariff tariff)
  {
    TariffInfo result = tariffInfo.get(tariff);
    if (null == result) {
      result = new TariffInfo(tariff);
      tariffInfo.put(tariff, result);
    }
    return result;
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
  @Override
  public void saveBootstrapState ()
  {
    int timeslot = service.getTimeslotRepo().currentSerialNumber();
    List<TariffSubscription> subs =
      service.getTariffSubscriptionRepo().findActiveSubscriptionsForCustomer(getCustomerInfo());
    if (subs.size() > 1) {
      // should only be one in a bootstrap session
      log.error("{} subscriptions, should be just one", subs.size());
    }
    TariffSubscription sub = subs.get(0);
    StorageState finalState = getStorageState(sub);
    storageRecord = finalState.gatherState(timeslot);
  }

  /**
   * Returns a vector of DemandElement instances representing the number of
   * vehicles that
   * plug in now and unplug in a future timeslot, how much energy they need.
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
      demandInfo = demandSampler.sample(hod, (int) population, chargerCapacity);
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
  
  private void updateDemandInfoMean (List<DemandElement> demandInfo, int hod)
  {
    // We clone the DemandElements so that the modifications to the
    // distributions do not affect the actual demandInfo object on the heap.
    List<DemandElement> newDemandInfo = demandInfo.stream()
            .map(SerializationUtils::clone).collect(Collectors.toList());
    // Make sure demandInfo is sorted by horizon so that the horizon
    // corresponds to the List index.
    newDemandInfo.sort(Comparator.comparingInt(DemandElement::getHorizon));

    List<DemandElement> currentDemandInfoMean = demandInfoMean.get(hod);

    if (currentDemandInfoMean == null) {
      demandInfoMean.put(hod, newDemandInfo);
    }
    else {
      int count = demandInfoMeanCounter.get(hod);
      for (DemandElement de: newDemandInfo) {
        if (de.getHorizon() < currentDemandInfoMean.size()) {
          // In this case we need to update the means for the existing
          // DemandElement.
          DemandElement currentDemandElementMean =
            currentDemandInfoMean.get(de.getHorizon());
          double[] currentEnergyHistogram =
            currentDemandElementMean.getdistribution();
          double[] newEnergyHistogram =
            new double[currentEnergyHistogram.length];
          for (int i = 0; i < currentEnergyHistogram.length; i++) {
            newEnergyHistogram[i] =
              (currentEnergyHistogram[i] * count + de.getdistribution()[i])
                                    / (count + 1);
          }
          de.setDistribution(newEnergyHistogram);
          currentDemandInfoMean.set(de.getHorizon(), de);
        }
        else {
          // In this case we simply add the new DemandElement to the end of
          // the list.
          currentDemandInfoMean.add(de);
        }
      }
    }

    demandInfoMeanCounter.merge(hod, 1, Integer::sum);
  }
  
  public Map<Integer, List<DemandElement>> getDemandInfoMean ()
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

  class TariffInfo
  {
    private Tariff tariff;
    private int profileSize = defaultProfileSize;
    private CapacityProfile capacityProfile;
    
    TariffInfo (Tariff tariff)
    {
      super();
      this.tariff = tariff;
      if (tariff.isWeekly()) {
        // use a weekly profile
        profileSize = defaultProfileSize * 7; 
      }
    }

    Tariff getTariff ()
    {
      return tariff;
    }

    void setCapacityProfile (CapacityProfile profile)
    {
      capacityProfile = profile;      
    }

    CapacityProfile getCapacityProfile ()
    {
      return defaultCapacityProfile;
    }

    // true just in case the tariff is a TOU tariff. If it's not, then
    // the price array and capacityProfile will be empty (null).
    boolean isTOU ()
    {
      return tariff.isTimeOfUse();
    }

    boolean isVariableRate ()
    {
      return tariff.isVariableRate();
    }

    // Heuristic decision on demand bias, depending on current vs near-future prices
    // and on value of demand flexibility
    double getDemandBias ()
    {
      double result = getNominalDemandBias();
      // do something here
      return result;
    }

    // expected cost 00:00 Monday through 23:00 Sunday
//    double[] getCost ()
//    {
//      if (null != this.costs)
//        return costs;
//      costs = new double[profileSize];
//      double cumulativeUsage = 0.0;
//      Instant start =
//          service.getTimeslotRepo().currentTimeslot().getStartInstant();
//      for (int i = 0; i < profileSize; i++) {
//        Instant when = start.plus(i * TimeService.HOUR);
//        if (when.get(DateTimeFieldType.hourOfDay()) == 0) {
//          cumulativeUsage = 0.0;
//        }
//        costs[i] =
//            tariff.getUsageCharge(when, cumulativeUsage) / nhc;
//        cumulativeUsage += nhc;
//      }
//      return costs;
//    }
  }
}
