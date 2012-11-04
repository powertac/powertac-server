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

package org.powertac.officecomplexcustomer.appliances;

import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;

/**
 * MicrowaveOven is the kitchen utility we use for cooking. It is use at least
 * twice a day depending on the number of tenants. The tenants should be present
 * when functioning so this is a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class MicrowaveOven extends SemiShiftingAppliance
{

  /**
   * This variable shows the possibility (%) that this appliance will be used
   * when workers are on break.
   */
  private double operationPercentage;

  @Override
  public void initialize (String office, Properties conf, Random gen)
  {
    // Filling the base variables
    name = office + " MicrowaveOven";
    saturation =
      Double.parseDouble(conf.getProperty("MicrowaveOvenSaturation"));
    power =
      (int) (OfficeComplexConstants.MICROWAVE_OVEN_POWER_VARIANCE
             * gen.nextGaussian() + OfficeComplexConstants.MICROWAVE_OVEN_POWER_MEAN);
    cycleDuration = OfficeComplexConstants.MICROWAVE_OVEN_DURATION_CYCLE;
    operationPercentage =
      OfficeComplexConstants.MICROWAVE_OVEN_OPERATION_PERCENTAGE;
    times = Integer.parseInt(conf.getProperty("MicrowaveOvenDailyTimes"));

  }

  @Override
  public void fillDailyOperation (int weekday, Random gen)
  {

    // Initializing and Creating auxiliary variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {

      dailyOperation.add(false);
      loadVector.add(0);

      if (applianceOf.isOnBreak(weekday, i)) {

        double tempPercentage =
          operationPercentage
                  + (OfficeComplexConstants.OPERATION_PARTITION * (applianceOf
                          .employeeOnBreakNumber(weekday, i)));
        if (tempPercentage > gen.nextDouble()
            && i > OfficeComplexConstants.START_OF_LAUNCH_BREAK
            && i < OfficeComplexConstants.END_OF_LAUNCH_BREAK) {
          dailyOperation.set(i, true);
          loadVector.set(i, power);
        }
      }

    }
    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {
    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // In order for stove to work someone must be in the house for half hour
    for (int j = 0; j < OfficeComplexConstants.QUARTERS_OF_DAY; j++) {
      if (applianceOf.isOnBreak(day, j) == true)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }

    return possibilityDailyOperation;
  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {
    long[] newControllableLoad = new long[OfficeComplexConstants.HOURS_OF_DAY];

    int minindex = 0;
    double minvalue = Double.NEGATIVE_INFINITY;
    Instant hour1 =
      new Instant(now.getMillis() + TimeService.HOUR
                  * (OfficeComplexConstants.START_OF_LAUNCH_BREAK_HOUR - 1));
    long sumPower = 0;

    // Gather the Load Summary of the day
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++)
      sumPower += weeklyLoadVector.get(day).get(i);

    for (int i = OfficeComplexConstants.START_OF_LAUNCH_BREAK_HOUR - 1; i < OfficeComplexConstants.END_OF_LAUNCH_BREAK_HOUR + 2; i++) {

      if ((minvalue < tariff.getUsageCharge(hour1, 1, 0))
          || (minvalue == tariff.getUsageCharge(hour1, 1, 0) && gen.nextFloat() > OfficeComplexConstants.SAME)) {
        minvalue = tariff.getUsageCharge(hour1, 1, 0);
        minindex = i;
      }

      hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);

    }

    newControllableLoad[minindex] = sumPower;
    return newControllableLoad;
  }

  public void calculateOverallPower ()
  {
    boolean flag = true;
    int day = -1;
    // log.debug("MO operation: " + operationDaysVector.toString());
    // log.debug("MO members: " + applianceOf.getMembers().size());

    while (flag) {
      day = (int) (Math.random() * operationDaysVector.size());
      // System.out.println("MO Day " + day);
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
