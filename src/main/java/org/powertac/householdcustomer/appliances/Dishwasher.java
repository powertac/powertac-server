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
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.common.configurations.VillageConstants;

/**
 * Dishwasher are used in order to wash easily the dishes after dinner. There
 * are several programs that help you automate the procedure in order to start
 * at a less costly time, without problem, because it doesn't need emptying
 * after utilization. So this is a semi-shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Dishwasher extends SemiShiftingAppliance
{

  /**
   * The function mode of the dishwasher. For more info, read the details in the
   * enumerations.Mode java file
   **/
  // Mode mode = Mode.One

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Filling the base variables
    name = household + " Dishwasher";
    saturation = Double.parseDouble(conf.getProperty("DishwasherSaturation"));
    power = (int) (VillageConstants.DISHWASHER_POWER_VARIANCE * gen.nextGaussian() + VillageConstants.DISHWASHER_POWER_MEAN);
    cycleDuration = VillageConstants.DISHWASHER_DURATION_CYCLE;
    od = false;
    times = Integer.parseInt(conf.getProperty("DishwasherWeeklyTimes")) + applianceOf.getMembers().size();
    createWeeklyOperationVector(times, gen);
  }

  @Override
  public void showStatus ()
  {
    // Printing basic variables
    log.debug("Name = " + name);
    log.debug("Saturation = " + saturation);
    log.debug("Power = " + power);
    log.debug("Cycle Duration = " + cycleDuration);
    log.debug("Occupancy Dependence = " + od);

    // Printing Function Day Vector
    ListIterator<Integer> iter = days.listIterator();
    log.debug("Days Vector = ");
    while (iter.hasNext())
      log.debug("Day  " + iter.next());

    // Printing Weekly Operation Vector and Load Vector
    log.debug("Weekly Operation Vector and Load = ");

    for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION; i++) {
      log.debug("Day " + i);
      ListIterator<Boolean> iter3 = weeklyOperation.get(i).listIterator();
      ListIterator<Integer> iter4 = weeklyLoadVector.get(i).listIterator();
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++)
        log.debug("Quarter " + j + " = " + iter3.next() + "   Load = " + iter4.next());
    }
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // The dishwasher needs for someone to be in the house at the beginning and
    // the end of its
    // function
    for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++) {
      if (checkHouse(day, j) == true)
        possibilityDailyOperation.add(false);
      else
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
    Vector<Boolean> operation = operationVector.get(weekday);
    for (int l = 0; l < VillageConstants.QUARTERS_OF_DAY; l++) {
      loadVector.add(0);
      dailyOperation.add(false);
    }

    // Check all quarters of the day
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      if (operation.get(i) == true) {
        boolean flag = true;
        while (flag && i < (VillageConstants.QUARTERS_OF_DAY - VillageConstants.DISHWASHER_DURATION_CYCLE + 1)) {
          boolean empty = checkHouse(weekday, i);
          if (empty == false) {
            for (int k = i; k < i + VillageConstants.DISHWASHER_DURATION_CYCLE; k++) {
              loadVector.set(k, power);
              dailyOperation.set(k, true);
              if (k == VillageConstants.QUARTERS_OF_DAY - 1)
                break;
            }
            i = VillageConstants.QUARTERS_OF_DAY;
            flag = false;
          } else {
            i++;
          }
        }
      }
    }
    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  /**
   * This function checks for the household to see when it is empty or not empty
   * for the duration of the operation
   * 
   * @param weekday
   * @param quarter
   * @return
   */
  boolean checkHouse (int weekday, int quarter)
  {

    if (quarter + VillageConstants.DISHWASHER_DURATION_CYCLE >= VillageConstants.QUARTERS_OF_DAY)
      return true;
    else
      return applianceOf.isEmpty(weekday, quarter + VillageConstants.DISHWASHER_DURATION_CYCLE);

  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {

    long[] newControllableLoad = new long[VillageConstants.HOURS_OF_DAY];

    if (operationDaysVector.get(day)) {
      int minindex = 0;
      boolean[] functionMatrix = createShiftingOperationMatrix(day);

      // If we have a fixed tariff rate
      if ((tariff.getTariffSpec().getRates().size() == 1) && (tariff.getTariffSpec().getRates().get(0).isFixed())) {
        Vector<Integer> possibleHours = new Vector<Integer>();

        // find the all the available functioning hours of the appliance
        for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
          if (functionMatrix[i] && functionMatrix[i + 1]) {
            possibleHours.add(i);
          }
        }

        if (possibleHours.size() == 0) {
          return newControllableLoad;
        }

        minindex = possibleHours.get(gen.nextInt(possibleHours.size()));
      }
      // case of variable tariff rate
      else {

        double minvalue = Double.POSITIVE_INFINITY;
        Instant hour1 = now;
        Instant hour2 = new Instant(now.getMillis() + TimeService.HOUR);

        // find the all the available functioning hours of the appliance
        for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
          if (functionMatrix[i] && functionMatrix[i + 1]) {
            if (minvalue >= tariff.getUsageCharge(hour1, 1, 0) + tariff.getUsageCharge(hour2, 1, 0)) {
              minvalue = tariff.getUsageCharge(hour1, 1, 0) + tariff.getUsageCharge(hour2, 1, 0);
              minindex = i;
            }
          }
          hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);
          hour2 = new Instant(hour2.getMillis() + TimeService.HOUR);
        }
      }
      newControllableLoad[minindex] = VillageConstants.QUARTERS_OF_HOUR * power;
      newControllableLoad[minindex + 1] = VillageConstants.QUARTERS_OF_HOUR * power;
    }
    return newControllableLoad;
  }

  @Override
  public void refresh (Random gen)
  {
    createWeeklyOperationVector(times, gen);
    fillWeeklyFunction(gen);
    createWeeklyPossibilityOperationVector();
  }

}
