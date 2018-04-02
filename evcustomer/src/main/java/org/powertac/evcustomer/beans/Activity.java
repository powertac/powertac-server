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
 * @author Govert Buijs, John Collins
 */
public class Activity
{
  private String name;

  @ConfigurableValue(valueType = "Integer", dump = false,
          description = "Group ID")
  private int id;

  @ConfigurableValue(valueType = "Double", dump = false,
          description = "Weekday value")
  private double weekdayWeight;

  @ConfigurableValue(valueType = "Double", dump = false,
          description = "Weekend value")
  private double weekendWeight;

  /**
   * Normal constructor, usable by auto-config
   */
  public Activity (String name)
  {
    this.name = name;
  }
  
  /**
   * Test constructor
   */
  public Activity (int id, String name,
                   double weekdayWeight, double weekendWeight)
  {
    this.id = id;
    this.name = name;
    this.weekdayWeight = weekdayWeight;
    this.weekendWeight = weekendWeight;
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

  public double getHourWeight (int hour, double ra)
  {
    // TODO Clean up / make more clear
    // Get hour weigths from XML?

    double t1 = 6 + 1 * ra;
    double t2 = 6.5 + 1.5 * ra; // waking up in the interval 6-7,5
    double t3 = 16.5 + 1.5 * ra; // returning from work at 16.30-18
    double t4 = 21 + 3 * ra; // going to bed from 9-12.00

    int available1 = 1;
    int available2 = 1;
    int available3 = 1;

    if (id == 1 || id == 2 || id == 5) {
      if (hour < t1) {
        available1 = 1;
      }
      else if ((hour > t2) && (hour < t3)) {
        available1 = 1;
      }
      else {
        available1 = 0;
      }
    }
    else if (id == 4 || id == 3 || id == 7 || id == 8 || id == 9) {
      if (hour < t1) {
        available2 = 1;
      }
      else if ((hour > t2) && (hour < t3)) {
        available2 = 0;
      }
      else if (hour > t3 && hour < t4) {
        available2 = 1;
      }
      else {
        available2 = 1;
      }
    }
    else if (id == 6) {
      if (hour < t1) {
        available3 = 1;
      }
      else if (hour > t2 && hour < t3) {
        available3 = 1;
      }
      else if (hour > t3 && hour < t4) {
        available3 = 0;
      }
      else {
        available3 = 0;
      }
    }

    if (available1 * available2 * available3 == 1) {
      return 1.0;
    }

    return 0.0;
  }

  public int getId ()
  {
    return id;
  }

  public String getName ()
  {
    return name;
  }

  public double getWeekdayWeight ()
  {
    return weekdayWeight;
  }

  public double getWeekendWeight ()
  {
    return weekendWeight;
  }
}