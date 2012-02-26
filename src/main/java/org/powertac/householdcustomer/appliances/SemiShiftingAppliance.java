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

import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;

import org.powertac.common.configurations.VillageConstants;

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

  /** This function returns days vector of the appliance */
  public Vector<Integer> getDays ()
  {
    return days;
  }

  @Override
  Vector<Boolean> createDailyOperationVector (int weekday, Random gen)
  {
    // Creating Auxiliary Variables
    Vector<Boolean> v = new Vector<Boolean>(VillageConstants.QUARTERS_OF_DAY);

    // First initialize all to false
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++)
      v.add(false);
    if (days.contains(weekday) && ((this instanceof Dryer) == false)) {
      int quarter = gen.nextInt(VillageConstants.END_OF_FUNCTION);
      v.set(quarter, true);
    }
    return v;
  }

  @Override
  void createWeeklyOperationVector (int times, Random gen)
  {
    fillDays(times, gen);
    for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++)
      operationVector.add(createDailyOperationVector(i, gen));
  }

  @Override
  public void fillWeeklyFunction (Random gen)
  {
    for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++)
      fillDailyFunction(i, gen);
  }

  /**
   * This function fills out the vector that contains the days of the week that
   * the appliance is functioning.
   * 
   * @param times
   * @return
   */
  void fillDays (int times, Random gen)
  {
    for (int i = 0; i < times; i++) {
      int day = gen.nextInt(VillageConstants.DAYS_OF_WEEK - 1);
      ListIterator<Integer> iter = days.listIterator();
      while (iter.hasNext()) {
        int temp = (int) iter.next();
        if (day == temp) {
          day = day + 1;
          iter = days.listIterator();
        }
      }
      days.add(day);
      java.util.Collections.sort(days);
    }
    java.util.Collections.sort(days);
  }

}
