/*
 * Copyright 2013, 2014 the original author or authors.
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

import org.apache.log4j.Logger;
import org.powertac.common.CustomerInfo;
import org.powertac.common.IdGenerator;
import org.powertac.common.RandomSeed;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.evcustomer.Config;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.CarType;
import org.powertac.evcustomer.beans.GroupActivity;
import org.powertac.evcustomer.beans.SocialGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Konstantina Valogianni, Govert Buijs, John Collins
 * @version 0.5, Date: 2013.11.25
 */
@Domain
@ConfigurableInstance
public class EvCustomer
{
  static private Logger log = Logger.getLogger(EvCustomer.class.getName());

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
  private double currentCapacity; // kwh

  private SocialGroup socialGroup;
  private Map<Integer, Activity> activities;
  private Map<Integer, GroupActivity> groupActivities;

  private CustomerServiceAccessor service;
  private RandomSeed generator;

  // ignore quantities less than epsilon
  private double epsilon = 1e-6;

  // We are driving this timeslot, so we can't charge
  @ConfigurableValue(valueType = "Boolean",
      bootstrapState = true,
      description = "True if the customer is driving and cannot charge")
  private boolean driving;

  // state
  //private double evLoad = 0.0;
  private double upRegulation = 0.0;
  private double downRegulation = 0.0;

  private final int dataMapSize = 48;
  private Map<Integer, TimeslotData> timeslotDataMap =
      new HashMap<Integer, TimeslotData>(dataMapSize);

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
                                  Map<Integer, GroupActivity> groupActivities,
                                  CarType car,
                                  CustomerServiceAccessor service)
  {
    config = Config.getInstance();
    this.socialGroup = socialGroup;
    this.activities = activities;
    this.groupActivities = groupActivities;
    this.gender = gender;
    this.car = car;
    this.service = service;
    this.generator =
        service.getRandomSeedRepo().getRandomSeed(name, 1, "model");
    setCurrentCapacity(0.5 * car.getMaxCapacity());

    // For now all risk attitudes have same probability
    riskAttitude = RiskAttitude.values()[generator.nextInt(3)];

    customerInfo = new CustomerInfo(name, 1).
        withPowerType(PowerType.ELECTRIC_VEHICLE).
        withControllableKW(-car.getHomeChargeKW()).
        withUpRegulationKW(-car.getHomeChargeKW()).
        withDownRegulationKW(car.getHomeChargeKW()).
        withStorageCapacity(car.getMaxCapacity());

    // set up tariff evaluation
    evaluator = createTariffEvaluator();
    return customerInfo;
  }

  // ================ Tariff evaluation ===============
  private TariffEvaluator createTariffEvaluator ()
  {
    TariffEvaluationWrapper wrapper =
        new TariffEvaluationWrapper();
    TariffEvaluator te = new TariffEvaluator(wrapper);
    te.initializeInconvenienceFactors(config.getTouFactor(),
        config.getTieredRateFactor(),
        config.getVariablePricingFactor(),
        config.getInterruptibilityFactor());

    double weight = generator.nextDouble() * config.getWeightInconvenience();
    double expDuration = config.getMinDefaultDuration() +
        generator.nextInt(config.getMaxDefaultDuration() -
                          config.getMinDefaultDuration());

    te.withInconvenienceWeight(weight)
        .withInertia(config.getNsInertia())
        .withPreferredContractDuration(expDuration)
        .withRationality(config.getRationalityFactor())
        .withTariffEvalDepth(config.getTariffCount())
        .withTariffSwitchFactor(config.getBrokerSwitchFactor());
    return te;
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
      sub = subs.get(0);
    }

    driving = false;

    // Always do handleRegulations first, setRegulation last
    handleRegulation(day, hour, sub);
    makeDayPlanning(hour, day);
    doActivities(day, hour);
    double[] loads = getLoads(day, hour);
    consumePower(loads, sub);
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
        sub.getRegulation() * customerInfo.getPopulation();
    if (Math.abs(actualRegulation) < epsilon) {
      return;
    }

    // compute the regulation factor and do the regulation
    log.info(name + " regulate : " + actualRegulation);
    double regulationFactor;
    if (actualRegulation > epsilon && upRegulation > epsilon) {
      regulationFactor = actualRegulation / upRegulation;
    }
    else if (actualRegulation < -epsilon && downRegulation < -epsilon) {
      regulationFactor = -1 * actualRegulation / downRegulation;
    }
    else {
      return;
    }
    // do the regulation
    regulate(hour, regulationFactor);
  }

  private void setRegulation (double up, double down, TariffSubscription sub)
  {
    if (null == sub) 
      return;
    RegulationCapacity regulationCapacity =
        new RegulationCapacity(up, down);
    sub.setRegulationCapacity(regulationCapacity);
    log.info(getName() + " setting regulation, up: "
             + up + "; down: " + down);
  }


  /**
   * We always have data for at least 24h in advance.
   */
  void makeDayPlanning (int hour, int day)
  {
    if (hour != 0)
      return; // only runs once/day

    // First time
    if (timeslotDataMap.size() != dataMapSize) {
      timeslotDataMap = new HashMap<Integer, TimeslotData>(dataMapSize);
      for (int i = 0; i < dataMapSize; i++) {
        timeslotDataMap.put(i, new TimeslotData(0.0));
      }
      planTomorrow(day);
    }

    // Tomorrow is now today, move data
    for (int i = 0; i < 24; i++) {
      timeslotDataMap.put(i, timeslotDataMap.get(i + 24));
    }

    // Let's see what we want to do tomorrow
    planTomorrow(day + 1);

    // Update driving info
    updateChargingHours();
  }

  private void planTomorrow (int nextDay)
  {
    // TODO Need hour- and day-weights
    // TODO We need to check (+ unit testing) that intended is always >= 0

    // Only do activities between waking up and going to bed
    int wakeupSlot = 6 + generator.nextInt(3);
    int sleepSlot = 21 + generator.nextInt(4);

    // Randomly pick activities we're going to do today
    // For now : if we do activity, we do all kms in picked time slot
    // Otherwise we get too many slots without charging
    Map<Integer, Double> intended = new HashMap<Integer, Double>();
    for (int activityId = 0; activityId < activities.size(); activityId++) {
      GroupActivity groupActivity = groupActivities.get(activityId);
      double probability = groupActivity.getProbability(gender);
      double dailyDistance = groupActivity.getDailyKm(gender);

      // Probs > 1.0 will always happen (why),
      // hence the distance factoring == 1
      if (probability >= generator.nextDouble()) {
        int pickedTS = pickSlotForEvent(wakeupSlot, sleepSlot, intended);
        double distance = dailyDistance * 2.0 * generator.nextDouble();
        intended.put(pickedTS, distance);
      }
    }

    for (int i = 0; i < 24; i++) {
      Double intendedDistance = intended.get(i) != null ? intended.get(i) : 0.0;
      timeslotDataMap.put(i + 24, new TimeslotData(intendedDistance));
    }
  }

  private int pickSlotForEvent (int wakeupSlot, int sleepSlot,
                                Map<Integer, Double> intended)
  {
    // Make a list of all avalable spots
    List<Integer> available = new ArrayList<Integer>();
    for (int i = wakeupSlot; i < sleepSlot; i++) {
      if (intended.get(i) == null) {
        available.add(i);
      }
    }
    // No spots available, just ignore
    if (available.size() == 0) {
      return -1;
    }
    // Spots available, just pick one
    return available.get(generator.nextInt(available.size()));
  }

  // runs through datamap backwards to find charging intervals.
  private void updateChargingHours ()
  {
    int chargingHours = 0;
    for (int i = dataMapSize - 1; i >= 0; i--) {
      TimeslotData pointer = timeslotDataMap.get(i);
      if (pointer.getIntendedDistance() > epsilon) {
        chargingHours = 0;
      }
      else {
        chargingHours += 1;
        pointer.setHoursTillNextDrive(chargingHours);
      }
    }
  }

  public void doActivities (int day, int hour)
  {
    TimeslotData timeslotData = timeslotDataMap.get(hour);
    double intendedDistance = timeslotData.getIntendedDistance();
    double neededCapacity = getNeededCapacity(intendedDistance);

    if (intendedDistance < epsilon || neededCapacity > currentCapacity) {
      return;
    }

    try {
      discharge(neededCapacity);
      log.info("driving " + intendedDistance +
               ", using " + neededCapacity + " kWh");
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
  }

  /*
   * This gives an estimation of the daily load.
   */
  public double getDominantLoad ()
  {
    // TODO This needs day-weights?

    // Aggregate daily kms
    double dailyKm = 0.0;
    for (Map.Entry<Integer, GroupActivity> entry : groupActivities.entrySet()) {
      dailyKm += entry.getValue().getDailyKm(gender);
    }

    return getNeededCapacity(dailyKm);
  }

  /*
   * loads[0] = consumptionLoad
   * loads[1] = evLoad
   * loads[2] = upRegulation
   * loads[0] = downRegulation
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
    int hoursOfCharge = timeslotDataMap.get(hour).getHoursTillNextDrive();
    double nomCapacity = Math.max(getShortTermNeeded(hour + hoursOfCharge),
        car.getMaxCapacity() * riskAttitude.preferredMinimumCapacity);

    // This is the amount we need to charge, CONSUMPTION can't be regulated
    loads[0] = Math.max(0, minCapacity - currentCapacity);
    loads[0] = Math.min(loads[0], getChargingCapacity());

    // This is the amount we would like to charge, minus CONSUMPTION
    loads[1] = Math.max(0, (nomCapacity - currentCapacity) - loads[0]);
    loads[1] = Math.min(loads[1], getChargingCapacity());

    // This is the amount we could discharge (up regulate)
    loads[2] = Math.max(0, currentCapacity - minCapacity);
    loads[2] = Math.min(loads[2], getDischargingCapacity());

    // This is the amount we could charge extra (down regulate)
    loads[3] = -1 * (getChargingCapacity() - (loads[0] + loads[1]));

    try {
      charge(loads[0] + loads[1]);
    }
    catch (ChargeException ce) {
      log.error(ce.getMessage());
    }

    // We need the available regulations in the next timeslot
    timeslotDataMap.get(hour).setUpRegulationCharge(loads[1]);
    timeslotDataMap.get(hour).setUpRegulation(loads[2]);
    timeslotDataMap.get(hour).setDownRegulation(loads[3]);

    return loads;
  }

  /*
   * Calculate how much capacity we need for the next block of driving
   */
  private double getShortTermNeeded (int pointer)
  {
    double neededCapacity = 0.0;
    while (pointer < dataMapSize) {
      double tsDistance = timeslotDataMap.get(pointer++).getIntendedDistance();
      if (tsDistance < epsilon) {
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
      double tsDistance = timeslotDataMap.get(pointer).getIntendedDistance();

      if (tsDistance < epsilon) {
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

  /*
   * We divide the amount we need to regulate evenly over the ev customers that
   * allowed regulation. But we need to
   * TODO Need more doc
   */
  public void regulate (int hour, double regulationFactor)
  {
    TimeslotData tsData = timeslotDataMap.get(hour - 1);

    // At the beginning no regulation set
    if (tsData == null) {
      return;
    }

    try {
      if (regulationFactor < -epsilon && tsData.getDownRegulation() < -epsilon){
        double regulation = regulationFactor * tsData.getDownRegulation();
        charge(regulation);
      }
      else if (regulationFactor > epsilon) {
        if (tsData.getUpRegulationCharge() > epsilon) {
          // This is the part we thought we we're charging, but we didn't get
          // due to regulation. Just subtract from the current capacity
          double cap = -1 * regulationFactor * tsData.getUpRegulationCharge();
          setCurrentCapacity(getCurrentCapacity() - cap);
        }

        if (tsData.getUpRegulation() > epsilon) {
          // This is the part that's regulated via actual discharge
          double discharge = -1 * regulationFactor * tsData.getUpRegulation();
          discharge(-1 * discharge);
        }
      }
    }
    catch (ChargeException ce) {
      log.error(ce);
    }
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
    if (currentCapacity >= kwh) {
      setCurrentCapacity(currentCapacity - kwh);
    }
    else {
      throw new ChargeException("Not possible to discharge " + name + " : "
          + kwh + " from " + currentCapacity);
    }
  }

  public void charge (double kwh) throws ChargeException
  {
    // TODO Check if partially charging would suffice

    if ((currentCapacity + kwh) <= car.getMaxCapacity()) {
      setCurrentCapacity(currentCapacity + kwh);
    }
    else {
      throw new ChargeException("Not possible to charge " + name + " : "
          + kwh + " at " + currentCapacity + " (maxCap " + car.getMaxCapacity() +")");
    }
  }

  public double getNeededCapacity (double distance)
  {
    // For now assume a linear relation
    double fuelEconomy = car.getRange() / car.getMaxCapacity();
    return distance / fuelEconomy;
  }

  public double getChargingCapacity ()
  {
    // TODO Get home / away detection
    return Math.min(car.getHomeChargeKW(), car.getMaxCapacity() - currentCapacity);
  }

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

  Map<Integer, GroupActivity> getActivityDetails ()
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

  Map<Integer, TimeslotData> getTimeslotDataMap ()
  {
    return timeslotDataMap;
  }
  
  // =========== helper classes ==================
  class TimeslotData
  {
    private double intendedDistance = 0.0;
    private double upRegulation = 0.0;
    private double upRegulationCharge = 0.0;
    private double downRegulation = 0.0;
    private int hoursTillNextDrive = 0;

    public TimeslotData (double distance)
    {
      intendedDistance = distance;
    }

    public double getIntendedDistance ()
    {
      return intendedDistance;
    }

    public void setIntendedDistance (double intendedDistance)
    {
      this.intendedDistance = intendedDistance;
    }

    public double getUpRegulation ()
    {
      return upRegulation;
    }

    public void setUpRegulation (double upRegulation)
    {
      this.upRegulation = upRegulation;
    }

    public double getUpRegulationCharge ()
    {
      return upRegulationCharge;
    }

    public void setUpRegulationCharge (double upRegulationCharge)
    {
      this.upRegulationCharge = upRegulationCharge;
    }

    public double getDownRegulation ()
    {
      return downRegulation;
    }

    public void setDownRegulation (double downRegulation)
    {
      this.downRegulation = downRegulation;
    }

    public int getHoursTillNextDrive ()
    {
      return hoursTillNextDrive;
    }

    public void setHoursTillNextDrive (int hoursTillNextDrive)
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
    public double[] getCapacityProfile (Tariff tariff)
    {
      double[] result = new double[config.getProfileLength()];

      for (int i = 0; i < result.length; i++) {
        result[i] = getDominantLoad() / hrsPerDay;
      }
      return result;
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
