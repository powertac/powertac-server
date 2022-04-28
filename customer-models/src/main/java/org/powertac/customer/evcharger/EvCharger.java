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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.powertac.common.CapacityProfile;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.CustomerInfo.CustomerClass;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BootstrapDataCollector;
import org.powertac.common.interfaces.BootstrapState;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.state.Domain;
import org.powertac.customer.AbstractCustomer;
import org.powertac.customer.evcharger.StorageState.StorageElement;
import org.powertac.util.Pair;

/**
 * 
 * @author John Collins
 */
@Domain
@ConfigurableInstance
public class EvCharger
extends AbstractCustomer
implements CustomerModelAccessor
{
  static private Logger log =
          LogManager.getLogger(EvCharger.class.getSimpleName());
  
  @ConfigurableValue(valueType = "Double",
          publish = true, bootstrapState = true, dump = true,
          description = "Population of chargers")
  private double population = 1000.0;
  
  @ConfigurableValue(valueType = "Double",
          publish = true, bootstrapState = true, dump = true,
          description = "Individual Charger capacity in kW")
  private double chargerCapacity = 8.0;

  @ConfigurableValue(valueType = "Double",
          publish = false, bootstrapState = true, dump = true,
          description = "Where in the min-max range we compute nominal demand")
  private double nominalDemandBias = 0.5;

  @ConfigurableValue(valueType = "Integer",
          publish = false, bootstrapState = true, dump = true,
          description = "Maximum horizon for individual charging demand elements")
  private int maxDemandHorizon = 96; // 4 days?

  // Tariff terms could affect this
  @ConfigurableValue(valueType = "Double",
          publish = false, bootstrapState = false, dump = true,
          description = "Portion of flexibility to hold back")
  private double defaultFlexibilityMargin = 0.02; // 2% on both ends

  @ConfigurableValue(valueType = "List",
          publish = false, dump = false, bootstrapState = true,
          description = "State of active chargers at end of boot session")
  private List<List> storageRecord;

  // Local definition of negligible energy quantity = 1 Wh
  private double epsilon = 0.001;

  private PowerType powerType = PowerType.ELECTRIC_VEHICLE;
  private RandomSeed evalSeed;
  private HashMap<TariffSubscription, StorageState> subState;
  private TariffEvaluator tariffEvaluator;

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
        .withCustomerClass(CustomerClass.SMALL);
    addCustomerInfo(info);
    ensureSeeds();

    // set up the subscription-state mapping
    subState = new HashMap<>();

    // set up the tariff evaluator. We are wide-open to variable pricing.
    tariffEvaluator = createTariffEvaluator(this);
    tariffEvaluator.withInertia(0.6).withPreferredContractDuration(14);
    tariffEvaluator.initializeInconvenienceFactors(0.0, 0.01, 0.0, 0.0);
    // TODO - fix this, possibly some ratio of population
    tariffEvaluator.initializeRegulationFactors(-100.0, 0.0, 100.0);
  }

  // called from CustomerModelService after initialization
  @Override
  public void handleInitialSubscription (List<TariffSubscription> subscriptions)
  {
    // Should be exactly one
    if (subscriptions.size() != 1) {
      log.error("Should be only one subscription, but saw {}", subscriptions.size());
    }
    // decorate the initial subscription
    TariffSubscription sub = subscriptions.get(0);
    subState.put(sub, new StorageState(sub, getChargerCapacity(), getMaxDemandHorizon())
                 .withUnitCapacity(getChargerCapacity()));
    // we assume the new StorageState will be initialized on the first step. No subscription
    // changes should occur before than.
  }

  // Gets a new random-number opSeed just in case we don't already have one.
  // Useful for mock-based testing.
  private void ensureSeeds ()
  {
    if (null == evalSeed) {
      RandomSeedRepo repo = service.getRandomSeedRepo();
      evalSeed = repo.getRandomSeed(
                         EvCharger.class.getName() + "-" + name, 0, "eval");
    }
  }

  @Override
  public CustomerInfo getCustomerInfo ()
  {
    return getCustomerInfo(powerType);
  }

  //private Map<Tariff, TariffInfo> TariffProfiles = null;
  @Override
  public CapacityProfile getCapacityProfile (Tariff tariff)
  {
    // TODO Auto-generated method stub
    return null;
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
    return 0;
  }

  /**
   * Called when some portion of the population switches from one tariff to another. This
   * is a customer model, so after the initial subscription (see handleInitialSubscription)
   * every member of the population is always subscribed to some tariff or another.
   * tariff.
   */
  @Override
  public void notifyCustomer (TariffSubscription oldsub,
                              TariffSubscription newsub,
                              int count)
  {
    // We assume count is positive
    if (count < 0) {
      log.error("Negative population in notifyCustomer({}, {} {})",
                oldsub.getId(), newsub.getId(), count);
      return;
    }
    else if (count == 0) {
      // unexpected, but not exactly an error
      log.warn("Zero population in notifyCustomer({}, {}, 0)",
               oldsub.getId(), newsub.getId());
      return;
    }

    // Now we do the transfer
    int timeslotIndex = service.getTimeslotRepo().currentSerialNumber();
    StorageState oldss = subState.get(oldsub);
    // Should not be null
    if (null == oldss) {
      log.error("Null StorageState on subscription {}", oldsub.getId());
      return;
    }
    // TODO - might want to set flexibility margin according to flexibility value
    StorageState newss = new StorageState(newsub, getChargerCapacity(), getMaxDemandHorizon())
            .withUnitCapacity(getChargerCapacity());
    subState.put(newsub, newss);
    newss.moveSubscribers(timeslotIndex, count, oldss);
  }

  @Override
  public void step ()
  {
    // sample distribution for the current date/time
    // assume sample is list of (activationCount, horizon, kWh) structs
    DateTime currentTime = service.getTimeService().getCurrentDateTime();
    int timeslotIndex = service.getTimeslotRepo().currentSerialNumber();
    // get current demand
    List<DemandElement>newDemand = getDemandInfo(currentTime);
    log.info("New demand {}", newDemand);
    
    // adjust for current weather?

    // check horizon and capacity constraints - Each DemandElement must specify enough
    // charger capacity to meet the associated demand
    for (DemandElement de : newDemand) {
      if (de.getHorizon() >= getMaxDemandHorizon()) {
        log.info("Reducing demand horizon from {} to {}", de.getHorizon(), getMaxDemandHorizon());
      }
      double margin = de.getNVehicles() * de.getHorizon() * getChargerCapacity()
              - de.getRequiredEnergy();
      if (margin < 0.0) {
        // constraint violation
        log.warn("Capacity constraint violation: ts {}, horizon {}, excess demand {}",
                 timeslotIndex, de.getHorizon(), -margin);
        de.adjustRequiredEnergy(margin);
      }
    }

    // iterate over subscriptions
    for (TariffSubscription sub : service.getTariffSubscriptionRepo()
      .findActiveSubscriptionsForCustomer(getCustomerInfo())) {
      // should ss compute these values? No.
      double ratio = (double) sub.getCustomersCommitted() / population;
      StorageState ss = subState.get(sub);
      // regulation must distributed before distributing future demand
      ss.distributeRegulation(timeslotIndex, sub.getRegulation());
      ss.distributeDemand(timeslotIndex, newDemand, ratio);
      Pair<Double, Double> limits = ss.getMinMax(timeslotIndex);
      double nominalDemand = computeNominalDemand(sub, limits);
      nominalDemand = topUpNext(timeslotIndex, ss, nominalDemand);
      ss.distributeUsage(timeslotIndex, nominalDemand);
      sub.usePower(nominalDemand);
      
      RegulationCapacity rc = computeRegulationCapacity(sub, nominalDemand,
                                                        limits.cdr(), limits.car());
      if (null != rc) {
        // if this subscription will compensate us for regulation, we'll report our
        // available capacity
        sub.setRegulationCapacity(rc);
      }
    }
  }

  // Computes nominal demand for the current timeslot based on tariff terms
  private double computeNominalDemand (TariffSubscription sub, Pair<Double, Double> minMax)
  {
    double result = 0.0;
    Tariff tariff = sub.getTariff();
    double halfRange = (minMax.cdr() - minMax.car()) / 2.0;
    if (!tariff.isTimeOfUse()
            && !tariff.isVariableRate()
            && !tariff.hasRegulationRate()) {
      // for flat-rate consumption tariffs, we charge as quickly as we can
      result = Math.max(minMax.cdr() - halfRange * defaultFlexibilityMargin,
                        minMax.car() + halfRange);
    }
    // handle other types here
    else {
      // default case
      result = minMax.car() + halfRange;
    }
    return result;
  }

  // Checks current-timeslot constraint, removes its demand from usage.
  // Returns remaining usage to be distributed over future timeslots
  private double topUpNext (int timeslot, StorageState ss, double usage)
  {
    //double shortage = 0.0;
    // First, peel off the commitment for the current timeslot
    // Note that we can only use the chargers that came with this demand
    StorageElement next = ss.getElement(timeslot);
    double commitment = next.getRemainingCommitment();
    double result = usage - commitment;
    double dedicatedChargers = next.getTranche();
    if (commitment > usage) {
      // big problem
      log.error("usage {} insufficient for current-timeslot commitment {}",
                usage, commitment);
      next.reduceCommitment(usage);
      result = 0.0;
    }
    else if (commitment > dedicatedChargers * getChargerCapacity()) {
      // charger capacity problem
      log.error("{} {} chargers insufficient to supply {} in one timeslot",
                dedicatedChargers, getChargerCapacity(), commitment);
      next.reduceCommitment(commitment);
    }
    else {
      next.reduceCommitment(commitment);
    }
    return result;
  }

  // Computes the regulation capacity to be reported on a subscription
  private RegulationCapacity computeRegulationCapacity (TariffSubscription sub,
                                                        double nominalDemand,
                                                        double minDemand, double maxDemand)
  {
    return new RegulationCapacity(sub, maxDemand - nominalDemand, nominalDemand - minDemand);
  }

  @Override
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    // TODO Auto-generated method stub

  }

  /**
   * Bootstrap data is just the content of the StorageState.
   * For this we need to know the timeslot index of the first timeslot in the
   * sim session.
   */
  @Override
  public void saveBootstrapState ()
  {
    int timeslot = service.getTimeslotRepo().currentSerialNumber();
    List<TariffSubscription> subs = service.getTariffSubscriptionRepo().
            findActiveSubscriptionsForCustomer(getCustomerInfo());
    if (subs.size() > 1) {
      // should only be one in a bootstrap session
      log.error("{} subscriptions, should be just one", subs.size());
    }
    TariffSubscription sub = subs.get(0);
    StorageState finalState = subState.get(sub);
    storageRecord = finalState.gatherState(timeslot);
  }

  /**
   * Returns a vector of DemandElement instances representing the number of vehicles that
   * plug in now and unplug in a future timeslot, how much energy they need. 
   */
  public List<DemandElement> getDemandInfo (DateTime time)
  {
    return null; // stub
  }

  //getters and setters, package visibility
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
}
