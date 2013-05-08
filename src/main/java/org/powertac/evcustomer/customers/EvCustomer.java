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
import org.powertac.common.enumerations.PowerType;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.ActivityDetail;
import org.powertac.evcustomer.beans.CarType;
import org.powertac.evcustomer.beans.SocialGroup;

import java.util.List;
import java.util.Random;


/**
 * TODO
 *
 * @author Konstantina Valogianni, Govert Buijs
 * @version 0.1, Date: 2013.03.21
 */
public class EvCustomer {
  static protected Logger log = Logger.getLogger(EvCustomer.class.getName());

  private String gender;
  private int riskAttitude; // 1 : risk averse; always charges to full
                            // 2 : risk neutral; charges when below 50%
                            // 3 : risk eager; charges when below 20%

  private CarType car;
  private SocialGroup socialGroup;
  private List<Activity> activities;
  private List<ActivityDetail> activityDetails;

  private Random gen;

  // We are driving this timeslot, so we can't charge
  private boolean driving;

  // TODO Replace in code
  public static PowerType powerType = PowerType.ELECTRIC_VEHICLE;

  public void initialize (List<SocialGroup> socialGroups,
                          List<Activity> activities,
                          List<List<ActivityDetail>> allActivityDetails,
                          List<CarType> carTypes, Random gen)
  {
    this.gen = gen;

    int randomGroup =  gen.nextInt(socialGroups.size());
    socialGroup = socialGroups.get(randomGroup);
    this.activityDetails = allActivityDetails.get(randomGroup);

    if (gen.nextDouble() <=  socialGroup.getMaleProbability()) {
      gender = "male";
    } else {
      gender = "female";
    }

    this.activities = activities;

    car = carTypes.get(gen.nextInt(carTypes.size()));

    // TODO Get probabilities from a config file ??
    riskAttitude = gen.nextInt(3);
  }

  /*
   * Depending on the given probabilities (per activity, weighted by day-type),
   * try to perform said activity. If doing activities, we can't charge.
   */
  public void doActivities (int day, int hour)
  {
    driving = false;

    // Load the probabilities
    double[] probabilities = new double[activities.size()];
    for (int activityId = 0; activityId < activities.size(); activityId++) {
      ActivityDetail activityDetail = activityDetails.get(activityId);
      if (gender.equals("male")) {
        probabilities[activityId] = activityDetail.getMaleProbability();
      }
      else {
        probabilities[activityId] = activityDetail.getFemaleProbability();
      }
    }

    // Adjust for weekday / weekend
    for (int i = 0; i < probabilities.length; i++) {
      double weight;
      if (day < 6) { // mon = 1 .. fri = 5, sat = 6, sun = 7
        weight = activities.get(i).getWeekdayWeight();
      }
      else {
        weight = activities.get(i).getWeekendWeight();
      }
      probabilities[i] = probabilities[i] * weight;
    }

    // TODO Adjust for hour of day

    // TODO Normalize ??

    // Depending on probability try the activity
    for (int i=0; i < probabilities.length; i++) {
      boolean driveQM = gen.nextInt(100) <= (100 * probabilities[i]);
      if (driveQM) {
        driveIfPossible(i);
      }
    }
  }

  private void driveIfPossible (int activityId)
  {
    // Calculate kms for this activity
    double activityDistance;
    if (gender.equals("male")) {
      activityDistance = activityDetails.get(activityId).getMaleDailyKm();
    }
    else {
      activityDistance = activityDetails.get(activityId).getFemaleDailyKm();
    }

    // Check if we have enough capacity for this activity
    double neededCapacity = activityDistance / car.getFuelEconomy();
    if (neededCapacity > car.getCurrentCapacity()) {
      return;
    }

    // We can make it! Drain the battery with needed capacity
    try {
      car.discharge(neededCapacity);
    }
    catch (CarType.ChargeException ce) {
      log.error(ce);
      return;
    }

    // We're driving, so we can't charge
    driving = true;
  }

  public double charge (int day, int hour)
  {
    if (driving) {
      return 0.0;
    }

    if (riskAttitude==0) {
      // Always charge when not full
      if (car.getCurrentCapacity() >= car.getMaxCapacity()) {
        return 0.0;
      }
    }
    else if (riskAttitude==1) {
      // Always charge when below 50%
      if (car.getCurrentCapacity() >= 0.5 * car.getMaxCapacity()) {
        return 0.0;
      }
    }
    else if (riskAttitude==2) {
      // Always charge when below 20%
      if (car.getCurrentCapacity() >= 0.2 * car.getMaxCapacity()) {
        return 0.0;
      }
    }

    // TODO Weigh with hour probalities?

    // TODO Get charge type depending on time (and on day?)
    double maxCharging = car.getAwayCharging();
    double needed = car.getMaxCapacity() - car.getCurrentCapacity();

    // Only charge what we need
    double charge = Math.min(maxCharging, needed);

    try {
      car.charge(charge);
    }
    catch (CarType.ChargeException ce) {
      log.error(ce);
      return 0.0;
    }
    return charge;
  }

  /*
   * This gives an estimation of the daily load.
   */
  // TODO Check if this is correct
  public double getDominantLoad ()
  {
    // Aggregate daily kms, divide by km/kwh
    double dailyKm = 0.0;
    for (ActivityDetail activityDetail: activityDetails) {
      if (gender.equals("male")) {
        dailyKm += activityDetail.getMaleDailyKm();
      }
      else {
        dailyKm += activityDetail.getFemaleDailyKm();
      }
    }

    return dailyKm / car.getFuelEconomy();
  }

  /* Used for unit tests */
  public CarType getCar () {
    return car;
  }

  public SocialGroup getSocialGroup () {
    return socialGroup;
  }

  public List<Activity> getActivities () {
    return activities;
  }

  public List<ActivityDetail> getActivityDetails () {
    return activityDetails;
  }

  public String getGender () {
    return gender;
  }

  public int getRiskAttitude () {
    return riskAttitude;
  }

  public boolean isDriving () {
    return driving;
  }
}
