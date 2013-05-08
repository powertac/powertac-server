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

import java.util.Map;
import java.util.Random;


/**
 * TODO
 *
 * @author Konstantina Valogianni, Govert Buijs
 * @version 0.2, Date: 2013.05.08
 */
public class EvCustomer {
  static protected Logger log = Logger.getLogger(EvCustomer.class.getName());

  private String gender;
  private int riskAttitude; // 1 : risk averse; always charges to full
                            // 2 : risk neutral; charges when below 50%
                            // 3 : risk eager; charges when below 20%

  private Car car;
  private SocialGroup socialGroup;
  private Map<Integer, Activity> activities;
  private Map<Integer, ActivityDetail> activityDetails;

  private double[] nomalizingFactors;

  private Random generator;

  // We are driving this timeslot, so we can't charge
  private boolean driving;

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

    // For now all rask attitudes have same probability
    riskAttitude = generator.nextInt(3);

    nomalizingFactors = calculatedNormalizingFactors(socialGroup.getId());
  }

  /*
   * Depending on the given probabilities (per activity, weighted by day-type),
   * try to perform said activity. If doing activities, we can't charge.
   */
  public void doActivities (int day, int hour)
  {
    driving = false;

    for (int activityId = 1; activityId <= activities.size(); activityId++) {
      Activity activity = activities.get(activityId);
      ActivityDetail activityDetail = activityDetails.get(activityId);
      double normalizingFactor = nomalizingFactors[activityId - 1];

      // Get probability (based on gender) and distance for activity
      double dailyDistance;
      double probability;
      if (gender.equals("male")) {
        probability = activityDetail.getMaleProbability();
        dailyDistance = activityDetails.get(activityId).getMaleDailyKm();
      }
      else {
        probability = activityDetail.getFemaleProbability();
        dailyDistance = activityDetails.get(activityId).getFemaleDailyKm();
      }

      // Adjust by day weight
      probability *= activity.getDayWeight(day);

      // Adjust by hour weight
      probability *= activity.getHourWeight(hour, generator.nextDouble());

      if (100 * probability > generator.nextInt(100)) {
        driveIfPossible(dailyDistance * normalizingFactor);
      }
    }
  }

  private void driveIfPossible (double distance)
  {
    // Check if we have enough capacity for this activity
    double neededCapacity = car.getNeededCapacity(distance);
    if (neededCapacity > car.getCurrentCapacity()) {
      return;
    }

    // We can make it! Drain the battery with needed capacity
    try {
      car.discharge(neededCapacity);
    }
    catch (Car.ChargeException ce) {
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

    // Only charge what we need or can
    double charge = Math.min(maxCharging, needed);

    try {
      car.charge(charge);
    }
    catch (Car.ChargeException ce) {
      log.error(ce);
      ce.printStackTrace();
      return 0.0;
    }
    return charge;
  }

  public void calculateNomalizingFactors ()
  {
    nomalizingFactors = new double[activities.size()];

    for (int activityId = 1; activityId <= activities.size(); activityId++) {
      Activity activity = activities.get(activityId);
      ActivityDetail activityDetail = activityDetails.get(activityId);

      double nomalizingFactor = 0.0;
      int itns = 1000000;
      for (int i = 0; i < itns; i++) {
        for (int day =0; day < 7; day++) {
          for (int hour = 0; hour < 24; hour++) {
            double probability;
            if (gender.equals("male")) {
              probability = activityDetail.getMaleProbability();
            }
            else {
              probability = activityDetail.getFemaleProbability();
            }
            probability *= activity.getDayWeight(day);
            probability *= activity.getHourWeight(hour, generator.nextDouble());

            nomalizingFactor += probability;
          }
        }
      }

      if (Math.abs(nomalizingFactor) > 0.000001) {
        nomalizingFactor = 7 * itns / nomalizingFactor;
      }
      else {
        nomalizingFactor = 1;
      }

      nomalizingFactors[activityId-1] = nomalizingFactor;
    }
  }

  public static double[] calculatedNormalizingFactors (int groupId)
  {
    // TODO For now equal between male and female

    double[] nomalizingFactors;
    switch (groupId) {
      case 1:
        nomalizingFactors = new double[]{
            0.6013745697576719, 0.6013745697576719, 0.08771463620789377,
            0.19047553189488367, 0.06272401431339633, 0.27027840108063333,
            0.621140149089667, 0.483081972886419, 0.6944550879790746};
        break;
      case 2:
        nomalizingFactors = new double[]{
            0.06013745721562055, 0.06013745721562055, 0.08771572061554128,
            0.09523756497742943, 0.06969334942192819, 0.38611408144508347,
            0.48314644161753045, 0.48314255353064334, 1.250051480084142};
        break;
      case 3:
        nomalizingFactors = new double[]{
            0.06013745721562055, 0.06013745721562055, 0.087721418537831,
            0.09524343278247911, 0.06969334942192819, 0.38610554656996915,
            0.4830392427830233, 0.4830814628107712, 1.2499358171974833};
        break;
      case 4:
        nomalizingFactors = new double[]{
            1.0, 1.0, 0.08772270286001668,
            0.09523614065438413, 0.06969334942192819, 0.3002881927598783,
            0.483028995282524, 0.4830832840858321, 1.2500221256774735};
        break;
      case 5:
        nomalizingFactors = new double[]{
            0.30068728487883595, 1.0, 0.08771531887818548,
            0.09523470756342392, 0.06969334942192819, 0.27025937174400355,
            0.4830639318652277, 0.48312299099548967, 1.250050966650248};
        break;
      case 6:
        nomalizingFactors = new double[]{
            1.0, 1.0, 0.08772548083145358,
            0.09524097529803068, 0.06969334942192819, 0.38610749353185775,
            0.48314608596980746, 0.48302124544904895, 0.6250686845880942};
        break;
      case 7:
        nomalizingFactors = new double[]{
            1.0, 1.0, 0.08771916416678519,
            0.09523617648059308, 0.06969334942192819, 0.38609083020078405,
            0.4831131765625636, 0.483093113807499, 1.2500517088981693};
        break;
      default:
        nomalizingFactors = new double[]{
            1.0, 1.0, 0.08771837492246468,
            0.09524162198875058, 0.06969334942192819, 0.38609647690834564,
            0.4830488907101153, 0.4830690398007624, 1.2500035036442108};
    }
    return nomalizingFactors;
  }

  /*
   * This gives an estimation of the daily load.
   */
  public double getDominantLoad ()
  {
    // Aggregate daily kms
    double dailyKm = 0.0;
    for (Map.Entry<Integer, ActivityDetail> entry : activityDetails.entrySet())
    {
      if (gender.equals("male")) {
        dailyKm += entry.getValue().getMaleDailyKm();
      }
      else {
        dailyKm += entry.getValue().getFemaleDailyKm();
      }
    }

    return car.getNeededCapacity(dailyKm);
  }

  /*
   * Used for testing
   */
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

  public void setGenerator (Random generator)
  {
    this.generator = generator;
  }

  public int getRiskAttitude ()
  {
    return riskAttitude;
  }

  public void setDriving (boolean driving)
  {
    this.driving = driving;
  }

  public boolean isDriving ()
  {
    return driving;
  }

  public double[] getNomalizingFactors ()
  {
    return nomalizingFactors;
  }
  public void setNomalizingFactors (double[] nomalizingFactors)
  {
    this.nomalizingFactors = nomalizingFactors;
  }
}
