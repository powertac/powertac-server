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
import org.powertac.householdcustomer.configurations.VillageConstants;

/**
 * Dishwashers are used in order to wash easily the dishes after eating. There
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
    power =
      (int) (VillageConstants.DISHWASHER_POWER_VARIANCE * gen.nextGaussian() + VillageConstants.DISHWASHER_POWER_MEAN);
    cycleDuration = VillageConstants.DISHWASHER_DURATION_CYCLE;
    times =
      Integer.parseInt(conf.getProperty("DishwasherWeeklyTimes"))
              + applianceOf.getMembers().size();
  }

  @Override
  public void showStatus ()
  {
    // Printing basic variables
    log.debug("Name = " + name);
    log.debug("Saturation = " + saturation);
    log.debug("Power = " + power);
    log.debug("Cycle Duration = " + cycleDuration);
    log.debug("Weekly Times = " + times);
    // Printing Function Day Vector

    log.debug("Days Vector = ");
    for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION
                        + VillageConstants.DAYS_OF_BOOTSTRAP; i++)
      log.debug("Day: " + i + " Times: " + days.get(i));

    // Printing Weekly Operation Vector and Load Vector
    log.debug("Weekly Operation Vector and Load = ");

    for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION
                        + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
      log.debug("Day " + i);
      ListIterator<Boolean> iter3 = weeklyOperation.get(i).listIterator();
      ListIterator<Integer> iter4 = weeklyLoadVector.get(i).listIterator();
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++)
        log.debug("Quarter " + j + " = " + iter3.next() + "   Load = "
                  + iter4.next());
    }
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // The dishwasher needs for someone to be in the house at the beginning and
    // the end of its function.
    for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++) {
      if (checkHouse(day, j) == true)
        possibilityDailyOperation.add(false);
      else
        possibilityDailyOperation.add(true);
    }
    return possibilityDailyOperation;
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

    if (lastWeek[weekday] > 0) {
      Vector<Integer> temp = new Vector<Integer>();

      for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY - cycleDuration; i++) {
        if (checkHouse(weekday, i) == false) {
          int count = applianceOf.tenantsNumber(weekday, i + cycleDuration);
          for (int j = 0; j < count; j++) {
            temp.add(i);
          }
        }
      }

      if (temp.size() > 0) {
        for (int i = 0; i < lastWeek[weekday]; i++) {
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

    }
    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  /**
   * This function checks for the household to see when it is empty or not for
   * in order to choose the time of operation.
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
      return applianceOf.isEmpty(weekday, quarter + cycleDuration);

  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {

    long[] newControllableLoad = new long[VillageConstants.HOURS_OF_DAY];

    if (days.get(day) > 0) {

      int[] minindex = new int[2];
      double[] minvalue =
        { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
      boolean[] functionMatrix = createShiftingOperationMatrix(day);
      Instant hour1 = now;
      Instant hour2 = new Instant(now.getMillis() + TimeService.HOUR);
      Vector<Integer> possibleHours = new Vector<Integer>();
      double newValue = 0;

      // find the all the available functioning hours of the appliance
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        if (functionMatrix[i] && functionMatrix[i + 1]) {
          possibleHours.add(i);
        }
      }

      if (possibleHours.size() == 0) {
        return newControllableLoad;
      }

      // find the all the available functioning hours of the appliance
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        if (possibleHours.contains(i)) {

          newValue =
            tariff.getUsageCharge(hour1, 1, 0)
                    + tariff.getUsageCharge(hour2, 1, 0);

          if ((minvalue[0] < newValue)
              || (minvalue[0] == newValue && gen.nextFloat() > VillageConstants.SAME)) {
            minvalue[1] = minvalue[0];
            minvalue[0] = tariff.getUsageCharge(hour1, 1, 0);
            minindex[1] = minindex[0];
            minindex[0] = i;
          }
          else if ((minvalue[1] < newValue)
                   || (minvalue[1] == newValue && gen.nextFloat() > VillageConstants.SAME)) {
            minvalue[1] = tariff.getUsageCharge(hour1, 1, 0);
            minindex[1] = i;
          }

          hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);
          hour2 = new Instant(hour2.getMillis() + TimeService.HOUR);

        }
      }

      if (days.get(day) == VillageConstants.OPERATION_DAILY_TIMES_LIMIT) {

        newControllableLoad[minindex[0]] =
          VillageConstants.QUARTERS_OF_HOUR * power;
        newControllableLoad[minindex[0] + 1] =
          VillageConstants.QUARTERS_OF_HOUR * power;
        newControllableLoad[minindex[1]] =
          VillageConstants.QUARTERS_OF_HOUR * power;
        newControllableLoad[minindex[1] + 1] =
          VillageConstants.QUARTERS_OF_HOUR * power;

      }
      else {
        newControllableLoad[minindex[0]] =
          VillageConstants.QUARTERS_OF_HOUR * power;
        newControllableLoad[minindex[0] + 1] =
          VillageConstants.QUARTERS_OF_HOUR * power;
      }

    }
    return newControllableLoad;
  }

  public void calculateOverallPower ()
  {
    boolean flag = true;
    int day = -1;
    Vector<Integer> consumption = new Vector<Integer>();

    while (flag) {
      overallPower = 0;
      day = (int) (Math.random() * operationDaysVector.size());

      // log.debug("Choosen Day: " + day);
      // log.debug("Times for that day: " + getTimesForDay(day));

      if (getTimesForDay(day) == 1)
        flag = false;

      consumption = weeklyLoadVector.get(day);

      for (int i = 0; i < consumption.size(); i++)
        overallPower += consumption.get(i);

      if (overallPower == 0)
        flag = true;

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
