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
 * Refrigerator is in use in all the households in order to keep food preserved.
 * This appliance can automatically change the freezing cycles in order to save
 * energy, without problem without tenants manipulation. So this is a fully
 * shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class CoffeeMachine extends FullyShiftingAppliance
{

  /** this is the power load consumed when in stand by mode. */
  int standByPower;

  @Override
  public void initialize (String office, Properties conf, Random gen)
  {

    // Filling the base variables
    name = office + " CoffeeMachine";
    saturation = Double.parseDouble(conf.getProperty("RefrigeratorSaturation"));
    power = (int) (OfficeComplexConstants.COFFEE_MACHINE_POWER_VARIANCE * gen.nextGaussian() + OfficeComplexConstants.COFFEE_MACHINE_POWER_MEAN);
    cycleDuration = OfficeComplexConstants.COFFEE_MACHINE_DURATION_CYCLE;
    standByPower = (int) (power / (cycleDuration - 1));
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // Freezer can work anytime
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
    // Initializing Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    int k = gen.nextInt(cycleDuration);

    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      loadVector.add(0);
      dailyOperation.add(false);
    }

    if (applianceOf.isWorkingDayOfWeek(weekday)) {
      for (int i = OfficeComplexConstants.COFFEE_MACHINE_START_OPERATION; i < OfficeComplexConstants.COFFEE_MACHINE_STOP_OPERATION; i++) {
        if (i % cycleDuration == k) {
          loadVector.set(i, power);
          dailyOperation.set(i, true);
        } else {
          loadVector.set(i, standByPower);
          dailyOperation.set(i, true);
        }
      }
    }

    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {

    long[] newControllableLoad = new long[OfficeComplexConstants.HOURS_OF_DAY];

    if (applianceOf.isWorkingDay(day)) {
      Instant now2 = new Instant(now.getMillis() + TimeService.HOUR * (OfficeComplexConstants.COFFEE_MACHINE_START_OPERATION / OfficeComplexConstants.QUARTERS_OF_HOUR));

      // Daily operation is seperated in shifting periods
      for (int i = 0; i < OfficeComplexConstants.COFFEE_MACHINE_SHIFTING_PERIODS; i++) {
        double minvalue = Double.NEGATIVE_INFINITY;
        int minindex = 0;

        // For each shifting period we search the best value
        for (int j = 0; j < OfficeComplexConstants.COFFEE_MACHINE_SHIFTING_INTERVAL; j++) {
          if ((minvalue < tariff.getUsageCharge(now2, 1, 0)) || (minvalue == tariff.getUsageCharge(now2, 1, 0) && gen.nextFloat() > OfficeComplexConstants.SAME)) {
            minvalue = tariff.getUsageCharge(now2, 1, 0);
            minindex = j;
          }
          now2 = new Instant(now2.getMillis() + TimeService.HOUR);
        }
        newControllableLoad[(OfficeComplexConstants.COFFEE_MACHINE_START_OPERATION / OfficeComplexConstants.QUARTERS_OF_HOUR) + OfficeComplexConstants.COFFEE_MACHINE_SHIFTING_INTERVAL * i + minindex] = ((OfficeComplexConstants.COFFEE_MACHINE_SHIFTING_INTERVAL * power) + (2 * (OfficeComplexConstants.COFFEE_MACHINE_SHIFTING_INTERVAL + 1))
            * standByPower);
      }
    }
    return newControllableLoad;
  }

  @Override
  public void refresh (Random gen)
  {
    fillWeeklyOperation(gen);
    createWeeklyPossibilityOperationVector();
  }

}
