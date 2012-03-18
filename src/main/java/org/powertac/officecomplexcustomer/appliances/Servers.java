/*
 * Copyright 2009-2012 the original author or authors.
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

package org.powertac.officecomplexcustomer.appliances;

import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.powertac.common.configurations.OfficeComplexConstants;

/**
 * Lights are utilized when the persons inhabiting the house have need for them
 * to light the rooms they are in. So it's a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Servers extends NotShiftingAppliance
{

  /**
   * This variable shows the power consumed by the servers when they are in
   * sleep mode, most of the day
   */
  private int sleepPower;

  @Override
  public void initialize (String office, Properties conf, Random gen)
  {
    // Filling the base variables
    name = office + " Servers";
    saturation = Double.parseDouble(conf.getProperty("ServersSaturation"));
    power = (int) (OfficeComplexConstants.SERVERS_POWER_VARIANCE * gen.nextGaussian() + OfficeComplexConstants.SERVERS_POWER_MEAN);
    sleepPower = (int) (OfficeComplexConstants.SERVERS_SLEEP_POWER_VARIANCE * gen.nextGaussian() + OfficeComplexConstants.SERVERS_SLEEP_POWER_MEAN);
    cycleDuration = OfficeComplexConstants.SERVERS_DURATION_CYCLE;
    times = Integer.parseInt(conf.getProperty("ServersDailyTimes")) + (int) (applianceOf.getMembers().size() / OfficeComplexConstants.PERSONS);

  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {
    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // Lights need to operate only when someone is in the house
    for (int j = 0; j < OfficeComplexConstants.QUARTERS_OF_DAY; j++) {
      if (applianceOf.isWorking(day, j) == true)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }

    return possibilityDailyOperation;
  }

  @Override
  public void fillDailyOperation (int weekday, Random gen)
  {
    // Initializing and Creating auxiliary variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    Vector<Integer> temp = new Vector<Integer>();

    // For each quarter of a day
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      loadVector.add(sleepPower);
      dailyOperation.add(true);

      int count = applianceOf.employeeNumber(weekday, i);
      for (int j = 0; j < count; j++) {
        temp.add(i);
      }
    }

    if (temp.size() > 0) {
      for (int i = 0; i < times; i++) {
        int rand = gen.nextInt(temp.size());
        int quarter = temp.get(rand);

        loadVector.set(quarter, (loadVector.get(quarter) + power));
        temp.remove(rand);
      }
    }

    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  @Override
  public void refresh (Random gen)
  {
    fillWeeklyOperation(gen);
    createWeeklyPossibilityOperationVector();
  }

}
