/*
 * Copyright (c) 2016 by John E. Collins
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
package org.powertac.customer.model;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.time.Instant;
import org.powertac.common.CapacityProfile;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
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
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.customer.AbstractCustomer;

/**
 * Models a single grid-connected storage battery with configurable capacity,
 * max charge rate, max discharge rate, and efficiency. Batteries do not charge
 * unless used for regulation, so repeated use for up-regulation will leave
 * them discharged, and repeated use for down-regulation will leave them
 * fully charged. If not used at all, they will eventually end up completely
 * discharged just in case their selfDischargeRate values are non-zero.
 * The initial state-of-charge is always 0.0;
 * 
 * @author John Collins
 */
@Domain
@ConfigurableInstance
public class Battery
extends AbstractCustomer
implements CustomerModelAccessor
{
  static private Logger log = LogManager.getLogger(Battery.class.getName());

  // configured through setters...
  private double capacityKWh = 50.0;
  private double maxChargeKW = 20.0;
  private double maxDischargeKW = -20.0;
  private double chargeEfficiency = 0.95;
  private double selfDischargeRate = 0.0005; // Proportion of capacity lost/hour

  // current State of Charge
  @ConfigurableValue(valueType = "Double", dump = false,
      bootstrapState = true,
      description = "State of charge")
  private double stateOfCharge = 0.0; // current energy content

  private PowerType powerType = PowerType.BATTERY_STORAGE;

  // random seeds
  private RandomSeed evalSeed = null;

  // context references
  private TariffEvaluator tariffEvaluator;

  /**
   * Default constructor, requires manual setting of name
   */
  public Battery ()
  {
    super();
  }

  /**
   * Standard constructor for named configurable type
   */
  public Battery (String name)
  {
    super(name);
  }

  /**
   * Initialization must provide accessor to Customer instance and time.
   * We assume configuration has already happened. We start with the battery
   * at its targetSOC. 
   */
  @Override
  public void initialize ()
  {
    super.initialize();
    log.info("Initialize " + name);
    // fill out CustomerInfo.
    CustomerInfo info = new CustomerInfo(name, 1);
    // conservative interruptible capacity
    double interruptible = maxChargeKW;
    info.withPowerType(powerType)
        .withControllableKW(-interruptible)
        .withStorageCapacity(capacityKWh)
        .withUpRegulationKW(maxDischargeKW)
        .withDownRegulationKW(maxChargeKW);
    addCustomerInfo(info);
    ensureSeeds();

    // validate parameters
    if (capacityKWh <= 0.0) {
      log.error("{}: bad capacity value {}",
                name, capacityKWh);
      capacityKWh = 1.0;
    }
    if (maxChargeKW <= 0.0) {
      log.error("{}: bad value {} for maxChargeKW",
                name, maxChargeKW);
      maxChargeKW = 1.0;
    }
    if (maxDischargeKW >= 0.0) {
      log.error("{}: bad value {} for maxDischargeKW",
                name, maxDischargeKW);
      maxDischargeKW = -1.0;
    }
    if (selfDischargeRate < 0.0 || selfDischargeRate > 1.0) {
      log.error ("{}: selfDischargeRate {} invalid",
                 name, selfDischargeRate);
      selfDischargeRate = 0.0;
    }
    if (chargeEfficiency < 0.0 || chargeEfficiency > 1.0) {
      log.error ("{}: chargeEfficiency {} invalid",
                 name, chargeEfficiency);
      chargeEfficiency = 1.0;
    }

    // set up the tariff evaluator. We are wide-open to variable pricing.
    tariffEvaluator = createTariffEvaluator(this);
    tariffEvaluator.withInertia(0.7).withRationality(0.99)
        .withPreferredContractDuration(14);
    tariffEvaluator.initializeInconvenienceFactors(0.0, 0.01, 0.0, 0.0);
    tariffEvaluator.initializeRegulationFactors(0.0,
                                                maxDischargeKW * 0.2 * chargeEfficiency,
                                                maxChargeKW * 0.2);
  }

  // Gets a new random-number seeds just in case we don't already have them.
  // Useful for mock-based testing.
  private void ensureSeeds ()
  {
    if (null == evalSeed) {
      evalSeed = service.getRandomSeedRepo()
          .getRandomSeed(Battery.class.getName() + "-" + name,
                         0, "eval");
    }
  }

  @Override
  public CustomerInfo getCustomerInfo ()
  {
    return getCustomerInfo(powerType);
  }

  // ======== per-timeslot activities ========
  @Override
  public void step ()
  {
    TariffSubscription subscription = getSubscription();

    // use energy on chargers, accounting for regulation
    // up-regulation is positive
    double regulation = subscription.getRegulation();
    log.info(getName() + ": regulation " + regulation);
    if (regulation > 0) {
      // up-regulation reduces state-of-charge
      stateOfCharge -= regulation;
    }
    else {
      // down-regulation increases state-of-charge
      stateOfCharge -= regulation * chargeEfficiency;
    }

    // self-discharge
    double timeslotLength = // in hours
        (Competition.currentCompetition().getTimeslotDuration()
                 / (double) TimeService.HOUR);
    stateOfCharge = Math.max(0.0,
                             stateOfCharge
                             - (selfDischargeRate * capacityKWh
                                 * timeslotLength));

    // set regulation capacity for next timeslot
    double up = Math.min(stateOfCharge, -maxDischargeKW * timeslotLength);
    double down = -((Math.min((capacityKWh - stateOfCharge) / chargeEfficiency,
                              maxChargeKW * timeslotLength)));
    subscription.setRegulationCapacity(new RegulationCapacity(subscription,
                                                              up, down));
    subscription.usePower(0.0);
  }

  // ================ getters and setters =====================
  // Note that list values must arrive and depart as List<String>,
  // while internally many of them are lists or arrays of numeric values.
  // Therefore we provide @ConfigurableValue setters that do the translation.
  @Override
  public String getName ()
  {
    return name;
  }

  @Override
  public void setName (String name)
  {
    this.name = name;
  }

  //private double capacityKWh = 50.0;
  @ConfigurableValue(valueType = "Double", dump = false,
      description = "size of battery in kWh")
  @StateChange
  public void setCapacityKWh (double value)
  {
    capacityKWh = value;
  }

  public double getCapacityKWh ()
  {
    return capacityKWh;
  }

  //private double maxChargeKW = 5.0;
  @ConfigurableValue(valueType = "Double", dump = false,
      description = "maximum charge rate")
  @StateChange
  public void setMaxChargeKW (double value)
  {
    maxChargeKW = value;
  }

  public double getMaxChargeKW ()
  {
    return maxChargeKW;
  }

  //private double maxDischargeKW = 5.0;
  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Maximum discharge rate")
  public void setMaxDischargeKW (double value)
  {
    maxDischargeKW = value;
  }

  public double getMaxDischargeKW ()
  {
    return maxDischargeKW;
  }

//private double chargeEfficiency = 0.9;
  @ConfigurableValue(valueType = "Double", dump = false,
      description = "ratio of charge energy to battery energy")
  @StateChange
  public void setChargeEfficiency (double value)
  {
    chargeEfficiency = value;
  }

  public double getChargeEfficiency ()
  {
    return chargeEfficiency;
  }

  //private double selfDischargeRate = 0.001;
  @ConfigurableValue(valueType = "Double", dump = false,
      description = "hourly charge lost as proportion of capacity")
  public void setSelfDischargeRate (double value)
  {
    selfDischargeRate = value;
  }

  public double getSelfDischargeRate ()
  {
    return selfDischargeRate;
  }

  // =========== Test support ============
  double getStateOfCharge ()
  {
    return stateOfCharge;
  }

  void setStateOfCharge (double value)
  {
    stateOfCharge = value;
  }

  // ======== CustomerModelAccessor API ===========

  // digs out the current subscription for this thing. Since the population is
  // always one, there should only ever be one of them
  private TariffSubscription getSubscription ()
  {
    List<TariffSubscription> subs = getCurrentSubscriptions(powerType);
    if (subs.size() > 1) {
      log.warn("Multiple subscriptions " + subs.size() + " for " + getName());
    }
    return subs.get(0);
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
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    log.info(getName() + ": evaluate tariffs");
    tariffEvaluator.evaluateTariffs();
  }

  @Override
  public double getShiftingInconvenienceFactor(Tariff tariff) {
    return 0;
  }

  @Override
  public CapacityProfile getCapacityProfile (Tariff tariff)
  {
    Instant start =
        service.getTimeslotRepo().currentTimeslot().getStartInstant();
    double drain = capacityKWh * (1.0 - chargeEfficiency) / 2.0;
    double[] profile = new double[24];
    Arrays.fill(profile, drain);
    return new CapacityProfile (profile,
                                start);
  }

  @Override
  public void notifyCustomer (TariffSubscription olds, TariffSubscription news, int pop)
  {
    // method stub
  }
}
