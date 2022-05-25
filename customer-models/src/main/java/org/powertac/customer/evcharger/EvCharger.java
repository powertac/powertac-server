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
    // First, we must distribute regulation from the previous timeslot, and then re-balance
    // storage state histograms for each subscription.
    DateTime currentTime = service.getTimeService().getCurrentDateTime();
    int timeslotIndex = service.getTimeslotRepo().currentSerialNumber();

    List<TariffSubscription> subs = service.getTariffSubscriptionRepo()
            .findActiveSubscriptionsForCustomer(getCustomerInfo());
    for (TariffSubscription sub : subs) {
      StorageState ss = subState.get(sub);
      // regulation must distributed before distributing future demand
      log.info("{} regulation for sub {} = {}", getCustomerInfo().getName(),
               sub.getId(), sub.getRegulation());
      ss.distributeRegulation(timeslotIndex, sub.getRegulation());
      // after regulation, we must collapse arrays and rebalance
      ss.collapseElements(timeslotIndex);
      ss.rebalance(timeslotIndex);
    }

    // Next, we sample demand distributions for the current date/time
    // assume sample is list of (horizon, activation count, [distribution]) structs
    // in which the lengths of the distribution arrays are equal to horizon + 1
    // get current demand
    List<DemandElement>newDemand = getDemandInfo(currentTime);
    log.info("New demand {}", newDemand);
    
    // adjust for current weather?

    // distribute new demand over subscriptions, use power, set regulation capacity
    for (TariffSubscription sub : subs) {
      // should ss compute these values? No.
      double ratio = (double) sub.getCustomersCommitted() / population;
      StorageState ss = subState.get(sub);
      ss.distributeDemand(timeslotIndex, newDemand, ratio);
      double[] limits = ss.getMinMax(timeslotIndex);
      
      double nominalDemand = computeNominalDemand(sub, limits);
      
      ss.distributeUsage(timeslotIndex, nominalDemand);
      sub.usePower(nominalDemand);
      
      RegulationCapacity rc = computeRegulationCapacity(sub, nominalDemand,
                                                        limits[0], limits[1]);
      if (null != rc) {
        // if this subscription will compensate us for regulation, we'll report our
        // available capacity
        sub.setRegulationCapacity(rc);
      }
    }
  }

  // Computes nominal demand for the current timeslot based on tariff terms
  private double computeNominalDemand (TariffSubscription sub, double[] minMax)
  {
    double result = 0.0;
    Tariff tariff = sub.getTariff();
    if (!tariff.isTimeOfUse()
            && !tariff.isVariableRate()
            && !tariff.hasRegulationRate()) {
      // for flat-rate consumption tariffs, we charge as quickly as we can
      result = minMax[1];
    }
    // handle other types here
    else {
      // default case is the midpoint
      result = minMax[2];
    }
    return result;
  }

  // Computes the regulation capacity to be reported on a subscription
  private RegulationCapacity computeRegulationCapacity (TariffSubscription sub,
                                                        double nominalDemand,
                                                        double minDemand, double maxDemand)
  {
    return new RegulationCapacity(sub, nominalDemand - minDemand, maxDemand - nominalDemand);
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
