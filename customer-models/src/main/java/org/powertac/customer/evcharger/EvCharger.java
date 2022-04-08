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

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.powertac.common.CapacityProfile;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.CustomerInfo.CustomerClass;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BootstrapDataCollector;
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
public class EvCharger
extends AbstractCustomer
implements CustomerModelAccessor, BootstrapDataCollector
{
  static private Logger log =
          LogManager.getLogger(EvCharger.class.getSimpleName());
  
  @ConfigurableValue(valueType = "Double",
          publish = true,
          bootstrapState = true, dump = true,
          description = "Population of chargers")
  private double population = 1000.0;
  
  @ConfigurableValue(valueType = "Double",
          publish = true,
          bootstrapState = true, dump = true,
          description = "Individual Charger capacity in kW")
  private double chargerCapacity = 8.0;

  private PowerType powerType = PowerType.ELECTRIC_VEHICLE;
  private RandomSeed evalSeed;
  private TariffEvaluator tariffEvaluator;

  private String storageStateName = "storage-state";

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
        .withCustomerClass(CustomerClass.LARGE);
    addCustomerInfo(info);
    ensureSeeds();

    // set up the tariff evaluator. We are wide-open to variable pricing.
    tariffEvaluator = createTariffEvaluator(this);
    tariffEvaluator.withInertia(0.7).withPreferredContractDuration(14);
    tariffEvaluator.initializeInconvenienceFactors(0.0, 0.01, 0.0, 0.0);
    // TODO - fix this, possibly some ratio of population
    tariffEvaluator.initializeRegulationFactors(-1.0, 0.0, 1.0);
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
    ensureDecoration(sub);
    // initialize the decorator
    StorageState initial = (StorageState) sub.getCustomerDecorator(storageStateName);
    if (null == initial) {
      // DEFINITELY should not happen
      log.error("null storage state on initial subscription");
      return;
    }
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

  private StorageState ensureDecoration (TariffSubscription sub)
  {
    StorageState result = (StorageState) sub.getCustomerDecorator(storageStateName);
    if (null == result) {
      result = new StorageState(sub, getChargerCapacity());
      sub.addCustomerDecorator(storageStateName, result);
    }
    return result;
  }

  private double getChargerCapacity ()
  {
    return chargerCapacity;
  }

  @Override
  public CustomerInfo getCustomerInfo ()
  {
    // TODO Auto-generated method stub
    return null;
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
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getTariffChoiceSample ()
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getInertiaSample ()
  {
    // TODO Auto-generated method stub
    return 0;
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
    StorageState oldss = (StorageState) oldsub.getCustomerDecorator(storageStateName);
    // Should not be null
    if (null == oldss) {
      log.error("Null StorageState on subscription {}", oldsub.getId());
      return;
    }
    StorageState newss = ensureDecoration(newsub);
    newss.moveSubscribers(timeslotIndex, count, oldss);
  }

  @Override
  public void step ()
  {
    // sample distribution for the current date/time
    // assume sample is list of (activationCount, horizon, kWh) structs
    DateTime currentTime = service.getTimeService().getCurrentDateTime();
    // get current demand
    List<DemandElement>newDemand = getDemandInfo(currentTime);
    
    // adjust for current weather

    // iterate over subscriptions
    int timeslotIndex = service.getTimeslotRepo().currentSerialNumber();
    for (TariffSubscription sub : service.getTariffSubscriptionRepo()
      .findActiveSubscriptionsForCustomer(getCustomerInfo())) {
      // should ss compute these values?
      double ratio = (double) sub.getCustomersCommitted() / population;
      StorageState ss = (StorageState) sub.getCustomerDecorator(storageStateName);
      // regulation must distributed before distributing future demand
      ss.distributeRegulation(timeslotIndex, sub.getRegulation());
      ss.distributeDemand(timeslotIndex, newDemand, ratio);
      sub.usePower(ss.getNominalDemand(timeslotIndex));
      sub.setRegulationCapacity(ss.getRegulationCapacity(timeslotIndex));
    }
  }

  @Override
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public List<Object> collectBootstrapData (int maxTimeslots)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Returns a vector of pairs, indexed by time horizon (or the number of timeslots in the
   * future), each representing the number of vehicles that will unplug and stop actively
   * charging in that timeslot and how much energy is needed by those unplugging vehicles. 
   */
  public List<DemandElement> getDemandInfo (DateTime time)
  {
    return null; // stub
  }
}
