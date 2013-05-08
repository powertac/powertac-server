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

package org.powertac.evcustomer.beans;


/**
 * @author Govert Buijs
 * @version 0.2, Date: 2013.05.08
 */
public class ActivityDetail {
  private int activityId;
  private double maleDailyKm;
  private double femaleDailyKm;
  private double maleProbability;
  private double femaleProbability;

  public ActivityDetail (int activityId,
                         double maleDailyKm, double femaleDailyKm,
                         double maleProbability, double femaleProbability)
  {
    this.activityId = activityId;
    this.maleDailyKm = maleDailyKm;
    this.femaleDailyKm = femaleDailyKm;
    this.maleProbability = maleProbability;
    this.femaleProbability = femaleProbability;
  }

  public int getActivityId ()
  {
    return activityId;
  }

  public double getMaleDailyKm ()
  {
    return maleDailyKm;
  }

  public double getFemaleDailyKm ()
  {
    return femaleDailyKm;
  }

  public double getMaleProbability ()
  {
    return maleProbability;
  }

  public double getFemaleProbability ()
  {
    return femaleProbability;
  }
}