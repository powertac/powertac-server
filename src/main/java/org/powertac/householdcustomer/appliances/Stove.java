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

import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.householdcustomer.configurations.VillageConstants;

/**
 * Stove is the kitchen utility we use for cooking. It is use at least twice a
 * day depending on the number of tenants. The tenants should be present when
 * functioning so this is a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Stove extends SemiShiftingAppliance
{

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Filling the base variables
    name = household + " Stove";
    saturation = Double.parseDouble(conf.getProperty("StoveSaturation"));
    power = (int) (VillageConstants.STOVE_POWER_VARIANCE * gen.nextGaussian() + VillageConstants.STOVE_POWER_MEAN);
    cycleDuration = VillageConstants.STOVE_DURATION_CYCLE;
    times = Integer.parseInt(conf.getProperty("StoveDailyTimes"));

  }

  @Override
  public void fillDailyOperation (int weekday, Random gen)
  {

    // Initializing Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    for (int l = 0; l < VillageConstants.QUARTERS_OF_DAY; l++) {
      loadVector.add(0);
      dailyOperation.add(false);
    }

    Vector<Integer> temp = new Vector<Integer>();

    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY - cycleDuration; i++) {
      if (applianceOf.isEmpty(weekday, i) == false && applianceOf.isEmpty(weekday, i + 1) == false) {
        int count = applianceOf.tenantsNumber(weekday, i);
        for (int j = 0; j < count; j++) {
          temp.add(i);
        }
      }
    }

    if (temp.size() > 0) {
      for (int i = 0; i < times; i++) {
        int rand = gen.nextInt(temp.size());
        int quarter = temp.get(rand);

        for (int j = 0; j < cycleDuration; j++) {
          dailyOperation.set(quarter + j, true);
          loadVector.set(quarter + j, power);
        }
        temp.remove(rand);
        if (temp.size() == 0)
          break;
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
    for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY - 1; j++) {
      if (applianceOf.isEmpty(day, j) == false && applianceOf.isEmpty(day, j + 1) == false)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }

    // For the last time, without check because it is the next day
    possibilityDailyOperation.add(false);
    return possibilityDailyOperation;
  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {

    long[] newControllableLoad = new long[VillageConstants.HOURS_OF_DAY];

    int minindex = 0;
    double minvalue = Double.NEGATIVE_INFINITY;
    boolean[] functionMatrix = createShiftingOperationMatrix(day);
    Instant hour1 = now;
    Vector<Integer> possibleHours = new Vector<Integer>();
    long sumPower = 0;

    // Gather the Load Summary of the day
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++)
      sumPower += weeklyLoadVector.get(day).get(i);

    // find the all the available functioning hours of the appliance
    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      if (functionMatrix[i]) {
        possibleHours.add(i);
      }
    }

    if (possibleHours.size() == 0) {
      return newControllableLoad;
    }

    // find the all the available functioning hours of the appliance
    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      if (possibleHours.contains(i)) {
        if ((minvalue < tariff.getUsageCharge(hour1, 1, 0)) || (minvalue == tariff.getUsageCharge(hour1, 1, 0) && gen.nextFloat() > VillageConstants.SAME)) {
          minvalue = tariff.getUsageCharge(hour1, 1, 0);
          minindex = i;
        }
      }
      hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);
    }
    newControllableLoad[minindex] = sumPower;

    return newControllableLoad;
  }

  @Override
  public void refresh (Random gen)
  {
    fillWeeklyOperation(gen);
    createWeeklyPossibilityOperationVector();
  }

}
