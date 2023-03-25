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

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.config.ConfigurableValue;

/**
 * Represents an activity that involves driving a vehicle. Some activities involve
 * driving for some distance and returning to the departure point; these have an
 * <code>interval</code> value of 0. If the <code>interval</code> value is greater
 * than zero, then the activity involves two trips, out and back. At the destination,
 * there may or may not be a charger available, as determined by the
 * <code>awayChargerProbability</code>. The distance to be driven is determined by
 * the associated <code>GroupActivity</code>.
 * When scheduling activities, it is important to schedule the activities first that
 * have the longest intervals, so they may be nested.
 * <p>
 * The probability of an activity happening at all is controlled by the
 * "day probability" as determined either by the pair <code>weekdayWeight</code> and
 * <code>weekendWeight</code> (both of which default to 1)
 * or by the <code>weeklyProfile</code> which is expected
 * to have an entry for each of the seven days in a week. Once it is decided that
 * an activity should be scheduled, the time (one-hour granularity for now) is determined
 * by the <code>dailyProfile</code>.
 * </p> <p>
 * Activities are added to a daily schedule one at a time, starting with the activities
 * having the greatest <code>interval</code>. Calls to <code>pickTimeslot()</code>
 * must supply a 24-element array of Activities representing those already scheduled.
 * The array will then be updated by the call. If the customer sleeps from 23:00-6:00,
 * then those timeslots must be pre-filled with dummy Activity instances. When called,
 * <code>pickTimeslot</code> will first determine whether to attempt to schedule the
 * activity based on the day probability.
 * Second, if this Activity has a non-zero <code>interval</code>, all slots that do
 * not allow for the return trip will be eliminated.
 * Finally, it will normalize the remaining unscheduled slots in the daily profile
 * and pick one. If there are two trips, the same activity will be populated in both
 * the outgoing and returning position. The caller is responsible for keeping track
 * of whether charging is available between trips.
 * </p>
 * 
 * @author Govert Buijs, John Collins
 */
public class Activity
{
  static private Logger log = LogManager.getLogger(Activity.class.getName());

  private String name;

  @ConfigurableValue(valueType = "Integer", dump = false,
          description = "Group ID")
  private int id;

  @ConfigurableValue(valueType = "Double", dump = false,
          description = "Weekday value")
  private double weekdayWeight = 1.0;

  @ConfigurableValue(valueType = "Double", dump = false,
          description = "Weekend value")
  private double weekendWeight = 1.0;
  
  // Configured in setter method
  private double[] weeklyProfile;
  private List<String> weeklyProfileRaw;

  // Configured in setter method
  private double[] dailyProfile;
  private List<String> dailyProfileRaw;

  @ConfigurableValue(valueType = "Integer", dump = false,
          description = "Interval in hours between daily trip pair")
  private int interval = 0;

  @ConfigurableValue(valueType = "Double", dump = false,
          description = "Probability of charger at destination")
  private double chargerProbability = 0.0;
  

  /**
   * Default constructor, needed for wrapper classes
   */
  public Activity ()
  {
    super();
  }

  /**
   * Normal constructor, usable by auto-config
   */
  public Activity (String name)
  {
    super();
    this.name = name;
  }

  public double getDayWeight (int day)
  {
    // TODO Add some randomness (ie holidays/free days)?

    // day comes from Joda, mon = 1 .. fri = 5, sat = 6, sun = 7
    if (day < 6) {
      return weekdayWeight;
    }
    else {
      return weekendWeight;
    }
  }

  public int getId ()
  {
    return id;
  }

  public String getName ()
  {
    return name;
  }
  
  public double getChargerProbability ()
  {
    return chargerProbability;
  }
  
  public void setChargerProbability (double prob)
  {
    chargerProbability = prob;
  }
  
  /**
   * If zero, then this is an out-and-back trip within a single timeslot.
   * If one, then this is two trips in sequential timeslots, without an opportunity
   * to charge.
   */
  public int getInterval ()
  {
    return interval;
  }

  public double getWeekdayWeight ()
  {
    return weekdayWeight;
  }

  public double getWeekendWeight ()
  {
    return weekendWeight;
  }

  @ConfigurableValue(valueType = "List", dump = false,
          description = "When this activity might start")
  public void setWeeklyProfile (List<String> value)
  {
    int daysPerWeek = 7;
    weeklyProfile = new double[daysPerWeek];
    if (value.size() != daysPerWeek)
      log.warn("incorrect week token count {} activity {}", value.size(), getName());
    int index = 0;
    for (String token : value) {
      weeklyProfile[index++] = Double.parseDouble(token);
      if (index >= weeklyProfile.length)
        break;
    }
  }

  // getter needed for config dump
  public List<String> getWeeklyProfile ()
  {
    return weeklyProfileRaw;
  }
  
  /**
   * Returns an Optional containing the weekly profile if it exists.
   */
  public Optional<double[]> getWeeklyProfileOptional ()
  {
    return Optional.ofNullable(weeklyProfile);
  }

  @ConfigurableValue(valueType = "List", dump = false,
          description = "When this activity might start")
  public void setDailyProfile (List<String> value)
  {
    dailyProfileRaw = value;
    int slotsPerDay = 24;
    dailyProfile = new double[slotsPerDay];
    if (value.size() != slotsPerDay)
      log.warn("incorrect day token count {} activity {}", value.size(), getName());
    int index = 0;
    for (String token : value) {
      dailyProfile[index++] = Double.parseDouble(token);
      if (index >= dailyProfile.length)
        break;
    }
    // normalize, assuming one instance/day
    // TODO: no need to normalize, that's done later
    double psum = 0.0;
    for (double prob: dailyProfile) {
      psum += prob;
    }
    for (int h = 0; h < dailyProfile.length; h++) {
      dailyProfile[h] /= psum;
    }
  }
  
  // getter needed for config dump
  public List<String> getDailyProfile ()
  {
    return dailyProfileRaw;
  }

  /**
   * Returns an Optional containing the daily profile if it exists
   */
  public Optional<double[]> getDailyProfileOptional ()
  {
    return Optional.ofNullable(dailyProfile);
  }

  /**
   * Returns the probability of doing this activity on the given day, driven either
   * by weekend/weekday probabilities, or by the weekly profile.
   */
  public double getDayProbability (int dayOfWeek)
  {
    double result = 1.0;
    if (dayOfWeek < 1 || dayOfWeek > 7) {
      log.error("bad day-of-week {} in probabilityForTimeslot", dayOfWeek);
    }
    else if (!getWeeklyProfileOptional().isPresent()) {
      // use weekday/weekend numbers
      result *= getDayWeight(dayOfWeek);
    }
    else {
      // remember that day-of-week is [1-7]
      result *= weeklyProfile[dayOfWeek - 1];
    }
    return result;
  }

  // Returns normalized probability for given day-of-week and timeslot.
  public double getProbabilityForTimeslot (int slot)
  {
    double result = 1.0;
    if (slot < 0 || slot > 23) {
      log.error("bad slot {} in probabilityForTimeslot", slot);
      result = 0.0;
    }
    // probability independent of timeslot unless dailyProfile is given
    else if (getDailyProfileOptional().isPresent()) {
      result = getDailyProfileOptional().get()[slot];
    }
    else {
      result = 1.0;
    }
    return result;
  }

  // Test methods -------------------------------------
  public void setInterval (int value)
  {
    interval = value;
  }

  public void setId (int value)
  {
    id = value;
  }
}