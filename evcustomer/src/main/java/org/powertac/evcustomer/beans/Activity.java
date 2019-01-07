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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.RandomSeed;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.evcustomer.customers.EvCustomer;

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
  private double awayChargerProbability = 0.0;
  
  // epsilon to avoid numeric problems in probability calculations
  private double epsilon = 1e-6;

  /**
   * Normal constructor, usable by auto-config
   */
  public Activity (String name)
  {
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
  
  public double getAwayChargerProbability ()
  {
    return awayChargerProbability;
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
   * Finds a time for this activity that does not conflict with previously scheduled
   * activities on the given day. If a time is found, it is added to the
   * scheduled array.
   **/
  public Activity[]
          pickTimeslot (int dow, Activity[] scheduled, RandomSeed generator)
  {
    if (dow < 1 || dow > 7) {
      log.error("bad day-of-week {} in probabilityForTimeslot", dow);
      return scheduled;
    }
    double dayProbability = 1.0;
    if (null == weeklyProfile) {
      // use weekday/weekend numbers
      dayProbability *= getDayWeight(dow);
    }
    else {
      // remember that day-of-week is [1-7]
      dayProbability *= weeklyProfile[dow - 1];
    }

    // We draw two samples here to ensure repeatability.
    // The first determines whether this activity is scheduled at all.
    double p1 = generator.nextDouble();
    double p2 = generator.nextDouble();
    if (dayProbability < p1) {
      return scheduled;
    }

    // attempt to schedule this activity
    double[] probabilities = new double[scheduled.length];
    Arrays.fill(probabilities, 1.0);
    // clear the committed timeslots and count the rest
    int open = 0;
    for (int i = 0; i < scheduled.length; i++) {
      if (null != scheduled[i]) {
        probabilities[i] = 0.0;
      }
      else {
        open += 1;
      }
    }
    
    // if there are no open slots, there's not much we can do
    if (0 == open)
      return scheduled;
    
    // if this activity has a non-zero interval, then clear out all the probabilities
    // where it cannot start
    if (getInterval() > 0) {
      open = 0;
      for (int i = 0; i < scheduled.length; i++) {
        if (i + getInterval() >= scheduled.length) {
          // can't start here
          probabilities[i] = 0.0;
          continue;
        }
        if (0.0 == probabilities[i])
          // can't start here
          continue;
        for (int j = i + 1; j < i + getInterval() + 1; j++) {
          if (0.0 == probabilities[j]) {
            // available interval ends too soon
            probabilities[i] = 0.0;
            break;
          }
        }
        if (1.0 == probabilities[i]) {
          // if probabilities[i] is still 1.0, then this is a valid place to start
          open += 1;
        }
      }
    }
    
    // populate timeslot array with raw probabilities for open timeslots,
    // track sum for normalization
    double psum = 0.0;
    for (int ts = 0; ts < scheduled.length; ts++) {
      if (1.0 == probabilities[ts]) { // possible choice
        probabilities[ts] *= getProbabilityForTimeslot(dow, ts, open);
        psum += probabilities[ts];
      }
    }
    if (psum < epsilon) {
      // no possible choices, cannot schedule this activity
      return scheduled;
    }
    // normalize to 1.0
    for (int ts = 0; ts < scheduled.length; ts++) {
      probabilities[ts] /= psum;
    }
    // Find a slot for this activity based on normalized probabilities.
    // Note that we only get here if the first draw on the generator was large enough,
    // so we use the second draw here to avoid a biased result
    for (int ts = 0; ts < scheduled.length; ts++) {
      p2 -= probabilities[ts];
      if (p2 <= epsilon) {
        scheduled[ts] = this;
        if (getInterval() > 0)
          scheduled[ts + getInterval()] = this;
        break;
      }
    }
    return scheduled;
  }

  // Returns probability for given day-of-week and timeslot. Probability is the product
  // of day probability and hour probability.
  double getProbabilityForTimeslot (int dow, int slot, long openSlots)
  {
    if (slot < 0 || slot > 23) {
      log.error("bad slot {} in probabilityForTimeslot", slot);
      return 0.0;
    }

    double result = 1.0;
    // probability independent of timeslot unless dailyProfile is given
    if (null != dailyProfile) {
      result *= dailyProfile[slot];
    }
    else {
      // spread probability evenly across open slots
      result /= (double) openSlots;
    }
    return result;
  }
}