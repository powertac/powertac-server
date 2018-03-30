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

package org.powertac.evcustomer.beans;

import org.powertac.common.config.ConfigurableValue;

/**
 * Join table between SocialGroup and Activity.
 * Intended to be created and populated by auto-configuration
 * @author Govert Buijs, John Collins
 */
public class GroupActivity
{
  private String name;

  @ConfigurableValue(valueType = "Integer", dump = false,
      description = "Foreign key: id of associated SocialGroup")
  private int groupId;

  @ConfigurableValue(valueType = "Integer", dump = false,
      description = "Foreign key: id of associated Activity")
  private int activityId;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Daily km for males in this group/activity")
  private double maleDailyKm;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Daily km for females in this group/activity")
  private double femaleDailyKm;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Probability for males in this group/activity")
  private double maleProbability;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Probability for females in this group/activity")
  private double femaleProbability;

  /**
   * Auto-configure constructor
   */
  public GroupActivity (String name)
  {
    super();
    this.name = name;
  }

  /**
   * Fills in fields for testing
   */
  public void initialize (int activityId,
                          double maleDailyKm, double femaleDailyKm,
                          double maleProbability, double femaleProbability)
  {
    this.activityId = activityId;
    this.maleDailyKm = maleDailyKm;
    this.femaleDailyKm = femaleDailyKm;
    this.maleProbability = maleProbability;
    this.femaleProbability = femaleProbability;
  }

  public String getName ()
  {
    return name;
  }

  public int getGroupId ()
  {
    return groupId;
  }

  public int getActivityId ()
  {
    return activityId;
  }

  public double getDailyKm (String gender)
  {
    if (gender.equals("male")) {
      return maleDailyKm;
    }
    else {
      return femaleDailyKm;
    }
  }

  public double getProbability (String gender)
  {
    if (gender.equals("male")) {
      return maleProbability;
    }
    else {
      return femaleProbability;
    }
  }

  // ===== USED FOR TESTING ===== //

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