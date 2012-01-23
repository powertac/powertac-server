/*
 * Copyright 2009-2011 the original author or authors.
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

import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.common.configurations.HouseholdConstants;

/**
 * Refrigerator is the fridge we all use in our households. This appliance can
 * automatically change the freezing cyles in order to save energy, without
 * problem without tenants manipulation. So this is a fully shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class Refrigerator extends FullyShiftingAppliance
{

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {

    // Filling the base variables
    name = household + " Refrigerator";
    saturation = Double.parseDouble(conf.getProperty("RefrigeratorSaturation"));
    power = (int) (HouseholdConstants.REFRIGERATOR_POWER_VARIANCE * gen.nextGaussian() + HouseholdConstants.REFRIGERATOR_POWER_MEAN);
    cycleDuration = HouseholdConstants.REFRIGERATOR_DURATION_CYCLE;
    od = false;
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // Freezer can work anytime
    for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++) {
      possibilityDailyOperation.add(true);
    }

    return possibilityDailyOperation;
  }

  @Override
  public void fillDailyFunction (int weekday, Random gen)
  {
    // Initializing Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
      if (i % cycleDuration == 0) {
        loadVector.add(power);
        dailyOperation.add(true);
      } else {
        loadVector.add(0);
        dailyOperation.add(false);
      }
    }
    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
    operationVector.add(dailyOperation);
  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {

    long[] newControllableLoad = new long[HouseholdConstants.HOURS_OF_DAY];

    Instant now2 = now;

    // Daily operation is seperated in shifting periods
    for (int i = 0; i < HouseholdConstants.REFRIGERATOR_SHIFTING_PERIODS; i++) {
      double minvalue = Double.POSITIVE_INFINITY;
      int minindex = 0;

      // For each shifting period we search the best value
      for (int j = 0; j < HouseholdConstants.REFRIGERATOR_SHIFTING_INTERVAL; j++) {
        if ((minvalue > tariff.getUsageCharge(now2, 1, 0)) || (minvalue == tariff.getUsageCharge(now2, 1, 0) && gen.nextFloat() > HouseholdConstants.HALF)) {
          minvalue = tariff.getUsageCharge(now2, 1, 0);
          minindex = j;
        }
        now2 = new Instant(now2.getMillis() + TimeService.HOUR);
      }
      newControllableLoad[HouseholdConstants.REFRIGERATOR_SHIFTING_INTERVAL * i + minindex] = HouseholdConstants.QUARTERS_OF_HOUR * power;
    }
    return newControllableLoad;
  }

  @Override
  public void refresh (Random gen)
  {
    fillWeeklyFunction(gen);
    createWeeklyPossibilityOperationVector();
  }

}
