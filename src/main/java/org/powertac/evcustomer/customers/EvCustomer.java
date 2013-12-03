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
 * @author Konstantina Valogianni, Govert Buijs
 * @version 0.5, Date: 2013.11.25
 */
public class EvCustomer
{
  static protected Logger log = Logger.getLogger(EvCustomer.class.getName());

  // TODO Just use a percentage? Make dynamic?
  private static enum RiskAttitude
  {
    risk_averse,  // charge when below 100%
    risk_neutral, // charge when below 50%
    risk_eager    // charge when below 20%
  }

  private String gender;
  private RiskAttitude riskAttitude;
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

    // For now all risk attitudes have same probability
    riskAttitude = RiskAttitude.values()[generator.nextInt(3)];

    calculateNomalizingFactors();
  }

  /*
   * Depending on the given probabilities (per activity, weighted by day-type),
   * try to perform said activity. If doing activities, we can't charge.
   */
  public void doActivities (int day, int hour)
  {
    driving = false;

    for (int activityId = 0; activityId < activities.size(); activityId++) {
      Activity activity = activities.get(activityId);
      ActivityDetail activityDetail = activityDetails.get(activityId);
      double normalizingFactor = nomalizingFactors[activityId];

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

    if (riskAttitude == RiskAttitude.risk_averse) {
      // Always charge when not full
      if (car.getCurrentCapacity() >= car.getMaxCapacity()) {
        return 0.0;
      }
    }
    else if (riskAttitude == RiskAttitude.risk_neutral) {
      // Only charge when below 50%
      if (car.getCurrentCapacity() >= 0.5 * car.getMaxCapacity()) {
        return 0.0;
      }
    }
    else if (riskAttitude == RiskAttitude.risk_eager) {
      // Only charge when below 20%
      if (car.getCurrentCapacity() >= 0.2 * car.getMaxCapacity()) {
        return 0.0;
      }
    }

    // TODO Weight with hour probalities?
    // Get charge type depending on time (and on day?)
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

    for (int activityId = 0; activityId < activities.size(); activityId++) {
      Activity activity = activities.get(activityId);
      ActivityDetail activityDetail = activityDetails.get(activityId);

      double factor = 0.0;

      for (int day = 1; day <= 7; day++) {  // Simulating Joda dayOfWeek
        for (int hour = 0; hour < 24; hour++) {
          double probability;
          if (gender.equals("male")) {
            probability = activityDetail.getMaleProbability();
          }
          else {
            probability = activityDetail.getFemaleProbability();
          }
          probability *= activity.getDayWeight(day);
          probability *= activity.getHourWeight(hour, 0);
          factor += probability;
        }
      }

      if (Math.abs(factor) > 1E-06) {
        factor = 7 / factor;
      }
      else {
        factor = 1;
      }

      nomalizingFactors[activityId] = factor;
    }
  }

  /*
   * This gives an estimation of the daily load.
   * TODO This should be hour (and day?) specific?
   */
  public double getDominantLoad ()
  {
    // Aggregate daily kms
    double dailyKm = 0.0;
    for (Map.Entry<Integer, ActivityDetail> entry : activityDetails.entrySet()) {
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

  public void setNomalizingFactors (double[] nomalizingFactors)
  {
    this.nomalizingFactors = nomalizingFactors;
  }
}
