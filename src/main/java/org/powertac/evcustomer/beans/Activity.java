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
 * @version 0.1, Date: 2013.03.21
 */
public class Activity {
  private int id;
  private String name;
  private double weekdayWeight;
  private double weekendWeight;

  public Activity (int id, String name,
                   double weekdayWeight, double weekendWeight)
  {
    this.id = id;
    this.name = name;
    this.weekdayWeight = weekdayWeight;
    this.weekendWeight = weekendWeight;
  }

  public int getId () {
    return id;
  }

  public String getName () {
    return name;
  }

  public double getWeekdayWeight () {
    return weekdayWeight;
  }

  public double getWeekendWeight () {
    return weekendWeight;
  }
}