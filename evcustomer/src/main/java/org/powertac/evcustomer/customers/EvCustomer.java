/*
 * Copyright 2013, 2014, 2015, 2018, 2019 the original author or authors.
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

package org.powertac.evcustomer.customers;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.powertac.common.CapacityProfile;
import org.powertac.common.CustomerInfo;
import org.powertac.common.IdGenerator;
import org.powertac.common.RandomSeed;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.customer.AbstractCustomer;
import org.powertac.evcustomer.Config;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.CarType;
import org.powertac.evcustomer.beans.GroupActivity;
import org.powertac.evcustomer.beans.SocialGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;


/**
 * @author Konstantina Valogianni, Govert Buijs, John Collins
 * @version 0.5, Date: 2013.11.25
 */
@Domain
@ConfigurableInstance
public class EvCustomer
{
  static private Logger log = LogManager.getLogger(EvCustomer.class.getName());

  private String name;
  private long id;

  public enum RiskAttitude
  {
    averse (0.4, 0.8),
    neutral(0.2, 0.6),
    eager  (0.1, 0.4);

    private double distanceFactor;
    private double preferredMinimumCapacity;

    RiskAttitude (double distanceFactor, double preferredMinimumCapacity) {
      this.distanceFactor = distanceFactor;
      this.preferredMinimumCapacity = preferredMinimumCapacity;
    }
  }

  private CustomerInfo customerInfo;
  private TariffEvaluator evaluator;

  private Config config;
  private String gender;
  private RiskAttitude riskAttitude;

  private CarType car;

  private SocialGroup socialGroup;
  private Map<Integer, Activity> activities;
  private List<GroupActivity> groupActivities;

  // No activity while asleep
  // Note that we don't go past midnight, avoiding cross-day data
  private int earlyWake = 5;
  private int wakeRange = 3;
  private int earlySleep = 20;
  private int sleepRange = 4;
  private Activity sleepActivity;

  private CustomerServiceAccessor service;
  private RandomSeed generator;

  // ignore quantities less than epsilon
  private double capacityEpsilon = 0.01; // 10 watt-hours
  private double distanceEpsilon = 0.1; // 100 meters -- they should walk!
  // epsilon to avoid numeric problems in probability calculations
  private double epsilon = 1e-6;

  // Vehicle state
  // We are driving this timeslot, so we can't charge
  @ConfigurableValue(valueType = "Boolean", bootstrapState = true,
      description = "True if the customer is driving and cannot charge")
  private boolean driving = false;
  @ConfigurableValue(valueType = "Double", bootstrapState = true,
      description = "current charge in vehicle battery")
  private double currentCapacity; // kwh
  @ConfigurableValue(valueType = "Boolean", bootstrapState = true,
      description = "True if vehicle is currently plugged in")
  private boolean connected = true;

  // We plan 48 hours out
  private final int dataMapSize = 24;
  private TimeslotData[] todayMap;
  private TimeslotData[] tomorrowMap;

  // ability to print readable date/time
  private DateTimeFormatter dtf = DateTimeFormat.forPattern("E.h");

  public EvCustomer (String name)
  {
    super();
    this.name = name;
    id = IdGenerator.createId();
  }

  public String getName ()
  {
    return name;
  }

  public long getId ()
  {
    return id;
  }

  // ========== initialization =============
  public CustomerInfo initialize (SocialGroup socialGroup,
                                  String gender,
                                  Map<Integer, Activity> activities,
                                  List<GroupActivity> groupActivities,
                                  CarType car,
                                  EvSocialClass esc,
                                  CustomerServiceAccessor service,
                                  Config config)
  {
    this.socialGroup = socialGroup;
    this.activities = activities;
    this.groupActivities = new ArrayList<GroupActivity> (groupActivities);
    this.groupActivities.sort((g1, g2) ->
    (activities.get(g2.getActivityId()).getInterval()
            - activities.get(g1.getActivityId()).getInterval()));
    this.gender = gender;
    this.car = car;
    this.service = service;
    this.config = config;
    this.generator =
        service.getRandomSeedRepo().getRandomSeed(name, 1, "model");
    setCurrentCapacity(0.5 * car.getMaxCapacity());

    // For now all risk attitudes have same probability
    riskAttitude = RiskAttitude.values()[generator.nextInt(3)];

    // The up-regulation and down-regulation values are equal magnitude, which
    // means this is NOT a vehicle-to-grid setup. It just means charging can
    // be curtailed.
    customerInfo = new CustomerInfo(name, 1).
        withPowerType(PowerType.ELECTRIC_VEHICLE).
        withControllableKW(-car.getHomeChargeKW()).
        //withUpRegulationKW(-car.getHomeChargeKW()).
        //withDownRegulationKW(car.getHomeChargeKW()).
        withStorageCapacity(car.getMaxCapacity());

    // set up tariff evaluation
    configTariffEvaluator(esc);
    sleepActivity = new Activity("sleep");
    return customerInfo;
  }

  // ================ Tariff evaluation ===============
  private void configTariffEvaluator (AbstractCustomer ac)
  {
    TariffEvaluationWrapper wrapper =
        createTariffEvaluationWrapper();
    evaluator = ac.createTariffEvaluator(wrapper);
    evaluator.initializeInconvenienceFactors(config.getTouFactor(),
        config.getTieredRateFactor(),
        config.getVariablePricingFactor(),
        config.getInterruptibilityFactor());

    double weight = generator.nextDouble() * config.getWeightInconvenience();
    double expDuration = config.getMinDefaultDuration() +
        generator.nextInt(config.getMaxDefaultDuration() -
                          config.getMinDefaultDuration());

    evaluator.withInconvenienceWeight(weight)
        .withInertia(config.getNsInertia())
        .withPreferredContractDuration(expDuration)
        .withRationality(config.getRationalityFactor())
        .withTariffEvalDepth(config.getTariffCount())
        .withTariffSwitchFactor(config.getBrokerSwitchFactor());
    evaluator.initializeRegulationFactors(car.getHomeChargeKW() * car.getCurtailmentFactor(),
                                   car.getHomeChargeKW() * car.getDischargeFactor(),
                                   car.getHomeChargeKW() * car.getDownRegFactor());
  }

  public void evaluateTariffs (List<Tariff> tariffs)
  {
    evaluator.evaluateTariffs();
  }

  // ================ model operation ================
  /**
   * Runs the model forward one step
   */
  public void step (Timeslot timeslot)
  {
    int day = timeslot.getStartTime().getDayOfWeek();
    int hour = timeslot.getStartTime().getHourOfDay();

    // find the current active subscription
    TariffSubscription sub = null;
    List<TariffSubscription> subs =
        service.getTariffSubscriptionRepo().
        findActiveSubscriptionsForCustomer(customerInfo);
    if (null == subs || subs.size() == 0) {
      log.error("No subscriptions found for " + name);
      return;
    }
    else {
      // This assumes a population of 1
      sub = subs.get(0);
    }

    // Strong assumption that each instance of driving is completed within a
    // single timeslot!
    driving = false;

    // Always do handleRegulations first, setRegulation last
    // This is because the regulation actually happened in the previous timeslot.
    handleRegulation(day, hour, sub);
    makeDayPlanning(hour, day);
    doActivities(day, hour);
    double[] loads = getLoads(day, hour);
    log.info("Customer {}, ts {}, loads = {}", getName(), timeslot, loads);
    consumePower(loads, sub);
    // consumePower() should have updated the regulation values in loads
    setRegulation(loads[2], loads[3], sub);
  }

  /*
   * When getting the load for consumePower, the batteries are charged according
   * to the desired capacity. But in reality the capacity might be regulated.
   */
  private void handleRegulation (int day, int hour, TariffSubscription sub)
  {
    if (null == sub)
      return;
    // check for non-zero regulation request
    double actualRegulation =
        sub.getRegulation() * customerInfo.getPopulation(); // population is always 1
    if (Math.abs(actualRegulation) < capacityEpsilon) {
      return;
    }

    // Regulation value is positive for up-regulation
    // compute the regulation factor and do the regulation
    log.info("{} regulate: {}, currentCapacity={}",
             name, actualRegulation, currentCapacity);

    double startCapacity = currentCapacity;
    try {
      if (actualRegulation > capacityEpsilon) {
        // positive, up-regulation
        discharge(actualRegulation);
      }
      else if (actualRegulation < -capacityEpsilon) {
        // negative, down-regulation
        charge(-actualRegulation);
      }
    }
    catch (ChargeException ce) {
      log.error(name +" : "+ ce);
    }

    if (Math.abs(startCapacity - currentCapacity) > capacityEpsilon) {
      log.info(String.format("%s regulated from %.1f to %.1f",
          name, startCapacity, currentCapacity));
    }
  }

  private void setRegulation (double up, double down, TariffSubscription sub)
  {
    if (null == sub) 
      return;
    sub.setRegulationCapacity(new RegulationCapacity(sub, up, down));
    log.info(name + " setting regulation, up: " + up + "; down: " + down);
  }

  /**
   * We always have data for at least 24h in advance.
   */
  void makeDayPlanning (int hour, int day)
  {
    if (hour != 0)
      return; // only runs once/day

    // First time
    if (null == todayMap) {
      tomorrowMap = new TimeslotData[dataMapSize];
      for (int i = 0; i < dataMapSize; i++) {
        tomorrowMap[i] = new TimeslotData();
      }
      planTomorrow(day);
    }

    // Tomorrow is now today, move data
    // TODO - Could re-use structure to reduce GC load
    todayMap = tomorrowMap;
    tomorrowMap = new TimeslotData[dataMapSize];
    for (int i = 0; i < dataMapSize; i++) {
      tomorrowMap[i] = new TimeslotData();
    }

    // Let's see what we want to do tomorrow
    int tomorrow = (day - 1) % 7 + 1;
    planTomorrow(tomorrow);

    // Update driving info for today
    updateChargingHours();
  }

  private void planTomorrow (int nextDay)
  {
    // Only do activities between waking up and going to bed
    int wakeupSlot = earlyWake + generator.nextInt(wakeRange);
    for (int i = 0; i < wakeupSlot; i++) {
      tomorrowMap[i].setActivity(sleepActivity);
    }
    int sleepSlot = earlySleep + generator.nextInt(sleepRange);
    for (int i = sleepSlot; i < dataMapSize; i++) {
      tomorrowMap[i].setActivity(sleepActivity);
    }
    

    // Randomly pick activities we're going to do today
    // For now : if we do activity, we do all in picked time slot
    // Otherwise we get too many slots without charging
    //double[] intended = new double[tomorrowMap.length];
    for (GroupActivity groupActivity : groupActivities) {
      Activity act = activities.get(groupActivity.getActivityId());
      if (nextDay < 6 && act.getName().equals("commuting")) {
        log.debug("commuting");
      }

      // We draw all samples here to ensure repeatability.
      // The first two determine whether this activity is scheduled at all.
      double p1 = generator.nextDouble();
      double p2 = generator.nextDouble();
      double p3 = generator.nextDouble();
      if (groupActivity.getProbability(gender) >= p1
              && act.getDayProbability(nextDay) >= p2) {

        // attempt to schedule current activity
        double[] probabilities = new double[tomorrowMap.length];
        Arrays.fill(probabilities, 1.0);
        // clear the committed timeslots and count the rest
        int open = 0;
        for (int i = 0; i < tomorrowMap.length; i++) {
          if (tomorrowMap[i].getActivity().isPresent()) {
            probabilities[i] = 0.0;
          }
          else {
            open += 1;
          }
        }

        // if there are no open slots, there's not much we can do
        if (0 == open)
          continue;

        // if this activity has a non-zero interval, then clear out all the probabilities
        // where it cannot start
        if (act.getInterval() > 0) {
          //open = 0;
          for (int i = 0; i < tomorrowMap.length; i++) {
            if (i + act.getInterval() >= tomorrowMap.length) {
              // can't start here
              probabilities[i] = 0.0;
              continue;
            }
            if (0.0 == probabilities[i])
              // can't start here
              continue;
            for (int j = i + 1; j < i + act.getInterval() + 1; j++) {
              if (0.0 == probabilities[j]) {
                // available interval ends too soon
                probabilities[i] = 0.0;
                break;
              }
            }
            if (1.0 == probabilities[i]) {
              // if probabilities[i] is still 1.0, then this is a valid place to start
              //open += 1;
              probabilities[i] = act.getProbabilityForTimeslot(i);
            }
          }
        }
        
        // populate probabilities array with raw probabilities for open timeslots,
        // track sum for normalization
        double psum = 0.0;
        for (int ts = 0; ts < tomorrowMap.length; ts++) {
          if (probabilities[ts] > 0.0) { // possible choice
            probabilities[ts] = act.getProbabilityForTimeslot(ts);
            psum += probabilities[ts];
          }
        }
        if (psum > epsilon) {
          // schedule this activity
          // normalize to 1.0
          for (int ts = 0; ts < tomorrowMap.length; ts++) {
            probabilities[ts] /= psum;
          }
          // Find a slot for this activity based on normalized probabilities.
          // Note that we only get here if earlier draw on the generator was large enough,
          // so we use the final draw here to avoid a biased result
          for (int ts = 0; ts < tomorrowMap.length; ts++) {
            p3 -= probabilities[ts];
            if (p3 <= epsilon) {
              tomorrowMap[ts].setActivity(act);
              tomorrowMap[ts].setGroupActivity(groupActivity);
              if (act.getInterval() > 0) {
                tomorrowMap[ts + act.getInterval()].setActivity(act);
                tomorrowMap[ts + act.getInterval()].setGroupActivity(groupActivity);
              }
              break;
            }
          }
        }
      }
    }

    for (int i = 0; i < tomorrowMap.length; i++) {
      double intendedDistance = 0.0;
      if (tomorrowMap[i].getGroupActivity().isPresent()) {
        intendedDistance =
                tomorrowMap[i].getGroupActivity().get().getDailyKm(getGender());
      }
      tomorrowMap[i].setIntendedDistance(intendedDistance);
    }
  }

  // runs through daily activities to record available charging capacity and hours.
  private void updateChargingHours ()
  {
    double currentCapacity = 0.0;
    Stack<Activity> activityStack = new Stack<>();

    // Start by assuming car is at home all day
    for (TimeslotData entry: todayMap) {
      entry.setChargingCapacity(car.getHomeChargeKW());
    }

    // Assume car is at home at midnight, then walk forward recording available
    // charging capacity
    for (int i = 0; i < todayMap.length; i++) {
      if (todayMap[i].getActivity().isPresent()) {
        Activity current = todayMap[i].getActivity().get();
        if (current.getInterval() > 0) {
          if (!activityStack.isEmpty() && activityStack.peek().equals(current)) {
            // current activity is second instance of an out-and-back
            activityStack.pop();
          }
          else {
            // current activity is first instance of an out-and-back
            activityStack.push(current);
            double sample = generator.nextDouble();
            if (todayMap[i].getActivity().get().getChargerProbability() >= sample) {
              currentCapacity = car.getAwayChargeKW();
            }
            else {
              currentCapacity = 0.0;
            }
            for (int j = i + 1; j < todayMap.length; j++) {
              if (todayMap[j].getActivity().isPresent()
                      && todayMap[j].getActivity().get() == current) {
                // return trip
                break;
              }
              todayMap[j].setChargingCapacity(currentCapacity);
            }
          }
        }
      }
    }
    if (!activityStack.isEmpty()) {
      log.error("Activity stack should be empty: car {}, activity {}",
                car.getName(), activityStack.peek().getName());
    }

    // record time available for charging
    int chargingHours = 0;
    for (int i = todayMap.length - 1; i >= 0; i--) {
      TimeslotData data = todayMap[i];
      if (data.getIntendedDistance() > epsilon) {
        chargingHours = 0;
      }
      else {
        chargingHours += 1;
        data.setHoursTillNextDrive(chargingHours);
      }
    }
  }

  public void doActivities (int day, int hour)
  {
    //TODO - keep track of plugged-in state and capacity of charger
    TimeslotData timeslotData = todayMap[hour];
    double intendedDistance = timeslotData.getIntendedDistance();
    double neededCapacity = getNeededCapacity(intendedDistance);

    if (intendedDistance < distanceEpsilon) {
      return;
    }
    if (neededCapacity > currentCapacity) {
      log.warn("Customer {} out of juice!", getName());
      return;
    }

    try {
      double before = currentCapacity;
      discharge(neededCapacity);
      log.info("{} {} at {}, {} kms {} kWh from {} to {}",
          name, timeslotData.getActivity().get().getName(),
          dtf.print(service.getTimeService().getCurrentDateTime()),
          intendedDistance, neededCapacity, before, currentCapacity);
      driving = true;
    }
    catch (ChargeException ce) {
      log.error(ce);
    }
  }

  // consumes power
  private void consumePower (double[] loads, TariffSubscription sub)
  {
    sub.usePower(loads[0] + loads[1]);

    try {
      double energy = loads[0] + loads[1]; 
      charge(energy);
      // Update down-regulation value if necessary
      // Note that loads[3] should be a negative value
      loads[3] = Math.max(loads[3], (currentCapacity - car.getMaxCapacity()));
    }
    catch (ChargeException ce) {
      log.error(ce.getMessage());
    }
  }

  /*
   * This gives an estimation of the daily load.
   */
  public double getDominantLoad ()
  {
    // TODO This needs day-weights?

    // Aggregate daily kms
    double dailyKm = 0.0;
    for (GroupActivity entry : groupActivities) {
      dailyKm += entry.getDailyKm(gender);
    }

    return getNeededCapacity(dailyKm);
  }

  /*
   * loads[0] = consumptionLoad
   * loads[1] = evLoad
   * loads[2] = upRegulation
   * loads[3] = downRegulation
   * TODO More documentation
   */
  public double[] getLoads (int day, int hour)
  {
    double[] loads = new double[4];

    if (driving) {
      return loads;
    }

    double currentCapacity = getCurrentCapacity();

    // This the amount we need to have at the next TS
    double minCapacity = getLongTermNeeded(hour + 1);
    // This is the amount we would like to have at the end of this TS
    int hoursOfCharge = todayMap[hour].getHoursTillNextDrive();
    double nomCapacity = Math.max(getShortTermNeeded(hour + hoursOfCharge),
        car.getMaxCapacity() * riskAttitude.preferredMinimumCapacity);

    // This is the amount we need to charge, CONSUMPTION can't be regulated
    loads[0] = Math.max(0, minCapacity - currentCapacity);
    loads[0] = Math.min(loads[0], todayMap[hour].getChargingCapacity());

    // This is the amount we would like to charge, minus CONSUMPTION
    loads[1] = Math.max(0, (nomCapacity - currentCapacity) - loads[0]);
    loads[1] = Math.min(loads[1], todayMap[hour].getChargingCapacity());

    // This is the amount we could discharge (up regulate)
    loads[2] = Math.max(0, currentCapacity - minCapacity);
    loads[2] = Math.min(loads[2], getDischargingCapacity());
    if (loads[2] < capacityEpsilon)
      loads[2] = 0;

    // This is the amount we could charge extra (down regulate)
    loads[3] = -1 * (todayMap[hour].getChargingCapacity() - (loads[0] + loads[1]));
    loads[3] = Math.max(loads[3], (getCurrentCapacity() - car.getMaxCapacity()));
    if (loads[3] > -capacityEpsilon)
      loads[3] = 0;

    // We need the available regulations in the next timeslot
    todayMap[hour].setUpRegulationCharge(loads[1]);
    todayMap[hour].setUpRegulation(loads[2]);
    todayMap[hour].setDownRegulation(loads[3]);

    return loads;
  }

  /*
   * Calculate how much capacity we need for the next block of driving
   */
  private double getShortTermNeeded (int pointer)
  {
    double neededCapacity = 0.0;
    while (pointer < dataMapSize) {
      double tsDistance = todayMap[pointer++].getIntendedDistance();
      if (tsDistance < distanceEpsilon) {
        break;
      }
      else {
        neededCapacity += getNeededCapacity(tsDistance);
      }
    }

    return neededCapacity * riskAttitude.distanceFactor;
  }

  /*
   * Calculate how much capacity we need for driving until the end ot planning
   * This is the amount we absolutly need, hence CONSUMPTION
   */
  private double getLongTermNeeded (int hour)
  {
    double neededCapacity = 0.0;
    int pointer = dataMapSize;
    while (--pointer >= hour) {
      double tsDistance = todayMap[pointer].getIntendedDistance();

      if (tsDistance < distanceEpsilon) {
        // Not driving, charge as much as needed and possible
        // TODO Add home / away detection
        neededCapacity -= Math.min(neededCapacity, car.getHomeChargeKW());
      }
      else {
        // Driving in this TS, increase needed capacity
        neededCapacity += getNeededCapacity(tsDistance);
        // But not more than possible
        neededCapacity = Math.min(neededCapacity, car.getMaxCapacity());
      }
    }

    return neededCapacity;
  }

  // ============ Vehicle state ===============

  public double getCurrentCapacity ()
  {
    return currentCapacity;
  }

  @StateChange
  public void setCurrentCapacity (double currentCapacity)
  {
    this.currentCapacity = currentCapacity;
  }

  // TODO Set min charge to 20%?
  public void discharge (double kwh) throws ChargeException
  {
    if (currentCapacity >= (kwh - capacityEpsilon)) {
      setCurrentCapacity(currentCapacity - kwh);
      log.info("{} discharge {} to {}", name, kwh, getCurrentCapacity());
    }
    else {
      throw new ChargeException("Not possible to discharge " + name + " : "
          + kwh + " from " + currentCapacity);
    }
  }

  // Handle energy consumption and down-regulation
  public void charge (double kwh) throws ChargeException
  {
    // TODO Check if partially charging would suffice
    if ((currentCapacity + kwh) <= car.getMaxCapacity()) {
      double startCapacity = currentCapacity;
      setCurrentCapacity(currentCapacity + kwh);

      if (Math.abs(startCapacity - currentCapacity) > capacityEpsilon) {
        log.info("{} charge {} to {}", name, kwh, currentCapacity);
      }
    }
    else {
      throw new ChargeException("Not possible to charge " + name + " : "
          + kwh + " at " + currentCapacity
          + " (maxCap " + car.getMaxCapacity() +")");
    }
  }

  public double getNeededCapacity (double distance)
  {
    // For now assume a linear relation
    double fuelEconomy = car.getRange() / car.getMaxCapacity();
    return distance / fuelEconomy;
  }

  // Get this from the current TimeslotData instance
//  public double getChargingCapacity ()
//  {
//    // TODO Get home / away detection
//    return Math.min(car.getHomeChargeKW(), car.getMaxCapacity() - currentCapacity);
//  }

  public double getDischargingCapacity ()
  {
    // TODO Get home / away detection
    return Math.min(car.getHomeChargeKW(), currentCapacity);
  }

  // ===== USED FOR TESTING ===== //
  // hence the package visibility

  CustomerInfo getCustomerInfo ()
  {
    return customerInfo;
  }

  CarType getCar ()
  {
    return car;
  }

  SocialGroup getSocialGroup ()
  {
    return socialGroup;
  }

  Map<Integer, Activity> getActivities ()
  {
    return activities;
  }

  List<GroupActivity> getGroupActivities ()
  {
    return groupActivities;
  }

  String getGender ()
  {
    return gender;
  }

  String getRiskAttitude ()
  {
    return riskAttitude.toString();
  }

  void setRiskAttitude (int riskNr)
  {
    try {
      riskAttitude = RiskAttitude.values()[riskNr];
    }
    catch (Exception ignored) {
    }
  }

  TariffEvaluationWrapper createTariffEvaluationWrapper ()
  {
    return new TariffEvaluationWrapper();
  }

  void setGenerator (RandomSeed generator)
  {
    this.generator = generator;
  }

  void setDriving (boolean driving)
  {
    this.driving = driving;
  }

  boolean isDriving ()
  {
    return driving;
  }

  TimeslotData[] getTodayMap ()
  {
    return todayMap;
  }
  
  // =========== helper classes ==================  
  class TimeslotData
  {
    // include GroupActivity and Activity to support scheduling and status
    private GroupActivity groupActivity;
    private Activity activity;
    private Activity previousActivity;
    
    private double chargingCapacity = 0.0;
    
    private double intendedDistance = 0.0;
    private double upRegulation = 0.0;
    private double upRegulationCharge = 0.0;
    private double downRegulation = 0.0;
    private int hoursTillNextDrive = 0;

    TimeslotData ()
    {
      super();
    }

    Optional<GroupActivity> getGroupActivity ()
    {
      return Optional.ofNullable(groupActivity);
    }

    void setGroupActivity (GroupActivity ga)
    {
      groupActivity = ga;
    }

    Optional<Activity> getActivity ()
    {
      return Optional.ofNullable(activity);
    }

    void setActivity (Activity act)
    {
      activity = act;
    }

    Optional<Activity> getPreviousActivity ()
    {
      return Optional.ofNullable(previousActivity);
    }

    void setPreviousActivity (Activity act)
    {
      previousActivity = act;
    }

    double getChargingCapacity ()
    {
      return chargingCapacity;
    }

    void setChargingCapacity (double capacity)
    {
      chargingCapacity = capacity;
    }

    double getIntendedDistance ()
    {
      return intendedDistance;
    }

    void setIntendedDistance (double intendedDistance)
    {
      this.intendedDistance = intendedDistance;
    }

    double getUpRegulation ()
    {
      return upRegulation;
    }

    void setUpRegulation (double upRegulation)
    {
      this.upRegulation = upRegulation;
    }

    double getUpRegulationCharge ()
    {
      return upRegulationCharge;
    }

    void setUpRegulationCharge (double upRegulationCharge)
    {
      this.upRegulationCharge = upRegulationCharge;
    }

    double getDownRegulation ()
    {
      return downRegulation;
    }

    void setDownRegulation (double downRegulation)
    {
      this.downRegulation = downRegulation;
    }

    int getHoursTillNextDrive ()
    {
      return hoursTillNextDrive;
    }

    void setHoursTillNextDrive (int hoursTillNextDrive)
    {
      this.hoursTillNextDrive = hoursTillNextDrive;
    }
  }

  class TariffEvaluationWrapper implements CustomerModelAccessor
  {
    private final static int hrsPerDay = 24;

    public TariffEvaluationWrapper ()
    {
      super();
    }

    @Override
    public CustomerInfo getCustomerInfo ()
    {
      return customerInfo;
    }

    /**
     * TODO: this does not appear to be a reasonable profile
     */
    @Override
    public CapacityProfile getCapacityProfile (Tariff tariff)
    {
      double[] result = new double[config.getProfileLength()];

      for (int i = 0; i < result.length; i++) {
        result[i] = getDominantLoad() / hrsPerDay;
      }
      // Assume profile starts at midnight
      Instant start =
          service.getTimeslotRepo().currentTimeslot().getStartInstant();
      return new CapacityProfile(result,
                                 start.toDateTime(DateTimeZone.UTC)
                                 .withHourOfDay(0).toInstant()
                                 .plus(TimeService.DAY));
    }

    @Override
    public double getBrokerSwitchFactor (boolean isSuperseding)
    {
      double result = config.getBrokerSwitchFactor();
      if (isSuperseding) {
        return result * 5.0;
      }
      return result;
    }

    @Override
    public double getTariffChoiceSample ()
    {
      return generator.nextDouble();
    }

    @Override
    public double getInertiaSample ()
    {
      return generator.nextDouble();
    }

    @Override
    public double getShiftingInconvenienceFactor(Tariff tariff) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public void notifyCustomer (TariffSubscription oldsub,
                                TariffSubscription newsub, int population)
    {
      // method stub
    }
  }

  public class ChargeException extends Exception
  {
    private static final long serialVersionUID = 1L;

    public ChargeException (String message)
    {
      super(message);
    }
  }
}
