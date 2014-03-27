/*
 * Copyright 2013 the original author or authors.
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
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.ActivityDetail;
import org.powertac.evcustomer.beans.Car;
import org.powertac.evcustomer.beans.SocialGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * @author Konstantina Valogianni, Govert Buijs
 * @version 0.5, Date: 2013.11.25
 */
public class EvCustomer
{
  static private Logger log = Logger.getLogger(EvCustomer.class.getName());

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

  private String gender;
  private RiskAttitude riskAttitude;
  private Car car;
  private SocialGroup socialGroup;
  private Map<Integer, Activity> activities;
  private Map<Integer, ActivityDetail> activityDetails;

  private Random generator;

  // ignore quantities less than epsilon
  private double epsilon = 1e-6;

  // We are driving this timeslot, so we can't charge
  private boolean driving;

  private final int dataMapSize = 48;
  private Map<Integer, TimeslotData> timeslotDataMap =
      new HashMap<Integer, TimeslotData>(dataMapSize);

  public void initialize (SocialGroup socialGroup,
                          String gender,
                          Map<Integer, Activity> activities,
                          Map<Integer, ActivityDetail> activityDetails,
                          Car car,
                          Random generator)
  {
    this.generator = generator;
    this.socialGroup = socialGroup;
    this.activities = activities;
    this.activityDetails = activityDetails;
    this.gender = gender;
    this.car = car;

    // For now all risk attitudes have same probability
    riskAttitude = RiskAttitude.values()[generator.nextInt(3)];
  }

  /*
   * We always have data for at least 24h in advance.
   */
  public void makeDayPlanning (int day)
  {
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
      ActivityDetail activityDetail = activityDetails.get(activityId);
      double probability = activityDetail.getProbability(gender);
      double dailyDistance = activityDetail.getDailyKm(gender);

      // TODO
      if (probability < 1.0) {
        probability = Math.sqrt(probability);
      }
      dailyDistance *= 5;

      // Probs > 1.0 will always happen, hence the distance factoring == 1
      if (100 * probability > generator.nextInt(100)) {
        int pickedTS = pickSlotForEvent(wakeupSlot, sleepSlot, intended);
        double distance = dailyDistance / Math.min(1.0, probability);
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
    double neededCapacity = car.getNeededCapacity(intendedDistance);

    if (intendedDistance < epsilon || neededCapacity > car.getCurrentCapacity()) {
      return;
    }

    try {
      car.discharge(neededCapacity);
      driving = true;
    }
    catch (Car.ChargeException ce) {
      log.error(ce);
      driving = false;
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
    for (Map.Entry<Integer, ActivityDetail> entry : activityDetails.entrySet()) {
      dailyKm += entry.getValue().getDailyKm(gender);
    }

    return car.getNeededCapacity(dailyKm);
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

    double currentCapacity = car.getCurrentCapacity();

    // This the amount we need to have at the next TS
    double minCapacity = getLongTermNeeded(hour + 1);
    // This is the amount we would like to have at the end of this TS
    int hoursOfCharge = timeslotDataMap.get(hour).getHoursTillNextDrive();
    double nomCapacity = Math.max(getShortTermNeeded(hour + hoursOfCharge),
        car.getMaxCapacity() * riskAttitude.preferredMinimumCapacity);

    // This is the amount we need to charge, CONSUMPTION can't be regulated
    loads[0] = Math.max(0, minCapacity - currentCapacity);
    loads[0] = Math.min(loads[0], car.getChargingCapacity());

    // This is the amount we would like to charge, minus CONSUMPTION
    loads[1] = Math.max(0, (nomCapacity - currentCapacity) - loads[0]);
    loads[1] = Math.min(loads[1], car.getChargingCapacity());

    // This is the amount we could discharge (up regulate)
    loads[2] = Math.max(0, currentCapacity - minCapacity);
    loads[2] = Math.min(car.getDischargingCapacity(), loads[2]);

    // This is the amount we could charge extra (down regulate)
    loads[3] = -1 * (car.getChargingCapacity() - (loads[0] + loads[1]));

    try {
      car.charge(loads[0] + loads[1]);
    }
    catch (Car.ChargeException ce) {
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
        neededCapacity += car.getNeededCapacity(tsDistance);
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

      if (tsDistance > epsilon) {
        // Nnot driving, charge as much as needed and possible
        // TODO Add home / away detection
        neededCapacity -= Math.min(neededCapacity, car.getHomeCharging());
      }
      else {
        // Driving in this TS, increase needed capacity
        neededCapacity += car.getNeededCapacity(tsDistance);
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
        car.charge(regulation);
      }
      else if (regulationFactor > epsilon) {
        if (tsData.getUpRegulationCharge() > epsilon) {
          // This is the part we thought we we're charging, but we didn't get
          // due to regulation. Just subtract from the current capacity
          double cap = -1 * regulationFactor * tsData.getUpRegulationCharge();
          car.setCurrentCapacity(car.getCurrentCapacity() - cap);
        }

        if (tsData.getUpRegulation() > epsilon) {
          // This is the part that's regulated via actual discharge
          double discharge = -1 * regulationFactor * tsData.getUpRegulation();
          car.discharge(-1 * discharge);
        }
      }
    }
    catch (Car.ChargeException ce) {
      log.error(ce);
    }
  }

  // ===== USED FOR TESTING ===== //

  public Car getCar ()
  {
    return car;
  }

  public SocialGroup getSocialGroup ()
  {
    return socialGroup;
  }

  public Map<Integer, Activity> getActivities ()
  {
    return activities;
  }

  public Map<Integer, ActivityDetail> getActivityDetails ()
  {
    return activityDetails;
  }

  public String getGender ()
  {
    return gender;
  }

  public String getRiskAttitude ()
  {
    return riskAttitude.toString();
  }

  public void setRiskAttitude (int riskNr)
  {
    try {
      riskAttitude = RiskAttitude.values()[riskNr];
    }
    catch (Exception ignored) {
    }
  }

  public void setGenerator (Random generator)
  {
    this.generator = generator;
  }

  public void setDriving (boolean driving)
  {
    this.driving = driving;
  }

  public boolean isDriving ()
  {
    return driving;
  }
}

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
