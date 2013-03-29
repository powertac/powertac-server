/*
 * Copyright 2009-2012 the original author or authors.
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

package org.powertac.householdcustomer.appliances;

import java.util.Vector;

import org.powertac.householdcustomer.configurations.VillageConstants;

/**
 * This is the class for the appliance domain instances that can change / shift
 * their load but need to be programmed by the tenants or require them to be
 * there when start / end functioning. That's the reason it has restricted
 * shifting capabilities.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
class SemiShiftingAppliance extends Appliance
{

  /** This vector contains the weekdays that the appliance will be functioning. */
  Vector<Integer> days = new Vector<Integer>();

  /** This array contains the times of operation for last weeks days. */
  int[] lastWeek = new int[VillageConstants.DAYS_OF_WEEK];

  /** This function returns days vector of the appliance */
  public Vector<Integer> getDays ()
  {
    return days;
  }

  /**
   * This function returns the times of operation for a certain day of the
   * appliance
   */
  public int getTimesForDay (int day)
  {
    return days.get(day);
  }

  /**
   * This function fills out the vector that contains the days that the
   * appliance is functioning.
   * 
   * @param times
   * @return
   */
  void fillDays ()
  {
    lastWeek = new int[VillageConstants.DAYS_OF_WEEK];

    for (int i = 0; i < times; i++) {
      int temp = gen.nextInt(VillageConstants.DAYS_OF_WEEK);
      if (lastWeek[temp] < VillageConstants.OPERATION_DAILY_TIMES_LIMIT)
        lastWeek[temp]++;
      else
        i--;
    }

    for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++) {
      days.add(lastWeek[i]);
    }

  }

  @Override
  public void fillWeeklyOperation ()
  {
    if ((this instanceof Stove) == false)
      fillDays();
    super.fillWeeklyOperation();
  }

}
