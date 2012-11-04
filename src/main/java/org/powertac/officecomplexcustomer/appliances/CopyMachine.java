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

import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;

/**
 * Lights are utilized when the persons inhabiting the house have need for them
 * to light the rooms they are in. So it's a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class CopyMachine extends SemiShiftingAppliance
{

  /**
   * This variable shows the power consumed by the servers when they are in
   * sleep mode, most of the day
   */
  private int standbyPower;

  @Override
  public void initialize (String office, Properties conf, Random gen)
  {
    // Filling the base variables
    name = office + " CopyMachine";
    saturation = Double.parseDouble(conf.getProperty("CopyMachineSaturation"));
    power =
      (int) (OfficeComplexConstants.COPY_MACHINE_POWER_VARIANCE
             * gen.nextGaussian() + OfficeComplexConstants.COPY_MACHINE_POWER_MEAN);
    standbyPower =
      (int) (OfficeComplexConstants.COPY_MACHINE_STANDBY_POWER_VARIANCE
             * gen.nextGaussian() + OfficeComplexConstants.COPY_MACHINE_STANDBY_POWER_MEAN);
    cycleDuration = OfficeComplexConstants.COPY_MACHINE_DURATION_CYCLE;
    times =
      Integer.parseInt(conf.getProperty("CopyMachineDailyTimes"))
              + (int) (applianceOf.getMembers().size() / OfficeComplexConstants.PERSONS);
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

      if ((i > OfficeComplexConstants.START_OF_FUNCTION && i < OfficeComplexConstants.END_OF_FUNCTION)
          && !(applianceOf.isOnVacation(weekday))) {
        loadVector.add(standbyPower);
        dailyOperation.add(true);

        int count = applianceOf.employeeNumber(weekday, i);
        for (int j = 0; j < count; j++) {
          temp.add(i);
        }

      }
      else {
        loadVector.add(0);
        dailyOperation.add(false);
      }
    }

    if (temp.size() > 0) {
      for (int i = 0; i < times; i++) {
        int rand = gen.nextInt(temp.size());
        int quarter = temp.get(rand);

        loadVector.set(quarter, power);
        temp.remove(rand);
      }
    }

    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {
    long[] newControllableLoad = new long[OfficeComplexConstants.HOURS_OF_DAY];

    int[] minindex = new int[2];
    double[] minvalue = { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
    Instant hour1 =
      new Instant(now.getMillis() + TimeService.HOUR
                  * OfficeComplexConstants.START_OF_FUNCTION_HOURS);

    for (int i = OfficeComplexConstants.START_OF_FUNCTION_HOURS; i < OfficeComplexConstants.END_OF_FUNCTION_HOUR; i++) {

      newControllableLoad[i] =
        OfficeComplexConstants.QUARTERS_OF_HOUR * standbyPower;

      if ((minvalue[0] < tariff.getUsageCharge(hour1, 1, 0))
          || (minvalue[0] == tariff.getUsageCharge(hour1, 1, 0) && gen
                  .nextFloat() > OfficeComplexConstants.SAME)) {
        minvalue[1] = minvalue[0];
        minvalue[0] = tariff.getUsageCharge(hour1, 1, 0);
        minindex[1] = minindex[0];
        minindex[0] = i;
      }
      else if ((minvalue[1] < tariff.getUsageCharge(hour1, 1, 0))
               || (minvalue[1] == tariff.getUsageCharge(hour1, 1, 0) && gen
                       .nextFloat() > OfficeComplexConstants.SAME)) {
        minvalue[1] = tariff.getUsageCharge(hour1, 1, 0);
        minindex[1] = i;
      }

      hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);

    }

    if (times > 4) {

      newControllableLoad[minindex[0]] =
        OfficeComplexConstants.QUARTERS_OF_HOUR * power;
      newControllableLoad[minindex[1]] =
        (times - OfficeComplexConstants.QUARTERS_OF_HOUR) * power;

    }
    else {
      newControllableLoad[minindex[0]] = times * power;
    }

    return newControllableLoad;
  }

  public void calculateOverallPower ()
  {
    boolean flag = true;
    int day = -1;
    while (flag) {
      day = (int) (Math.random() * operationDaysVector.size());
      // System.out.println("CP Day " + day);
      if (operationDaysVector.get(day))
        flag = false;

      Vector<Integer> consumption = weeklyLoadVector.get(day);

      for (int i = 0; i < consumption.size(); i++)
        overallPower += consumption.get(i);

    }

    // log.debug("Overall Operation Power of " + toString() + ":" +
    // overallPower);
  }

  @Override
  public void refresh (Random gen)
  {
    fillWeeklyOperation(gen);
    createWeeklyPossibilityOperationVector();
  }

}
