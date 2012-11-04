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
import org.powertac.householdcustomer.enumerations.HeaterType;

/**
 * Water Heater is an appliance utilized to provide with hot water when we need
 * to bathe. There are two different types each having each own way of working
 * so we have to see to them seperately.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class WaterHeater extends FullyShiftingAppliance
{

  /**
   * The type of the water heater. For more info, read the details in the
   * enumerations.HeaterType java file
   **/
  HeaterType type;

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Creating Auxiliary Variables
    int x = 1 + gen.nextInt(VillageConstants.PERCENTAGE);
    int limit = Integer.parseInt(conf.getProperty("InstantHeater"));
    // Filling the base variables
    name = household + " WaterHeater";
    saturation = Double.parseDouble(conf.getProperty("WaterHeaterSaturation"));

    // If the heater is instant Heater
    if (x < limit) {
      power =
        (int) (VillageConstants.INSTANT_HEATER_POWER_VARIANCE
               * gen.nextGaussian() + VillageConstants.INSTANT_HEATER_POWER_MEAN);
      cycleDuration = VillageConstants.INSTANT_HEATER_DURATION_CYCLE;
      type = HeaterType.InstantHeater;
      times =
        Integer.parseInt(conf.getProperty("InstantHeaterDailyTimes"))
                + (int) (applianceOf.getMembers().size() / 2);

      if (times == 0)
        times = 1;

    }
    // If heater is storage
    else {
      power =
        (int) (VillageConstants.STORAGE_HEATER_POWER_VARIANCE
               * gen.nextGaussian() + VillageConstants.STORAGE_HEATER_POWER_MEAN);
      cycleDuration = VillageConstants.STORAGE_HEATER_DURATION_CYCLE;
      type = HeaterType.StorageHeater;
    }
  }

  @Override
  public void fillDailyOperation (int weekday, Random gen)
  {
    // Initializing And Creating Auxiliary Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    if (type == HeaterType.InstantHeater) {

      for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
        dailyOperation.add(false);
        loadVector.add(0);
      }

      Vector<Integer> temp = new Vector<Integer>();

      for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
        int count = applianceOf.tenantsNumber(weekday, i);
        for (int j = 0; j < count; j++) {
          temp.add(i);
        }

      }

      if (temp.size() > 0) {
        for (int i = 0; i < times; i++) {
          int rand = gen.nextInt(temp.size());
          int quarter = temp.get(rand);

          dailyOperation.set(quarter, true);
          loadVector.set(quarter, (loadVector.get(quarter) + power));
          temp.remove(rand);
          if (temp.size() == 0)
            break;
        }
      }

      weeklyLoadVector.add(loadVector);
      weeklyOperation.add(dailyOperation);

    }
    else {

      int start = 0;
      int temp = 0;

      for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
        dailyOperation.add(false);
        loadVector.add(0);
      }

      if (gen.nextFloat() > VillageConstants.STORAGE_HEATER_POSSIBILITY)
        start =
          (VillageConstants.STORAGE_HEATER_START + 1)
                  + gen.nextInt(VillageConstants.STORAGE_HEATER_START - 1);
      else
        start = 1 + gen.nextInt(VillageConstants.STORAGE_HEATER_START);

      for (int i = start; i < start
                              + VillageConstants.STORAGE_HEATER_PHASE_LOAD; i++) {
        dailyOperation.set(i, true);
        loadVector.set(i, power);
      }

      temp = start + VillageConstants.STORAGE_HEATER_PHASE_LOAD;

      for (int j = 1; j < VillageConstants.STORAGE_HEATER_PHASES; j++) {
        dailyOperation.set((temp + VillageConstants.STORAGE_HEATER_PHASES * j),
                           true);
        loadVector.set((temp + VillageConstants.STORAGE_HEATER_PHASES * j),
                       power);
      }

      weeklyLoadVector.add(loadVector);
      weeklyOperation.add(dailyOperation);

    }
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // If the heater is instant Heater
    if (type == HeaterType.InstantHeater) {
      // It can operate each quarter someone is at home to turn it on
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++) {
        if (applianceOf.isEmpty(day, j) == false)
          possibilityDailyOperation.add(true);
        else
          possibilityDailyOperation.add(false);
      }
    }
    // If heater is storage
    else {
      // It can operate all quarters of day
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++) {
        possibilityDailyOperation.add(true);
      }
    }
    return possibilityDailyOperation;
  }

  @Override
  public void showStatus ()
  {
    // Printing basic variables
    log.debug("Name = " + name);
    log.debug("Saturation = " + saturation);
    log.debug("Power = " + power);
    log.debug("Heater Type = " + type);
    log.debug("Cycle Duration = " + cycleDuration);

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
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {

    long[] newControllableLoad = new long[VillageConstants.HOURS_OF_DAY];

    // If the heater is working the day of the shifting
    if (operationDaysVector.get(day)) {

      int minindex = 0;
      boolean[] functionMatrix = createShiftingOperationMatrix(day);
      double minvalue = Double.NEGATIVE_INFINITY;
      Instant hour1 = now;

      // If the heater is instant Heater
      if (type == HeaterType.InstantHeater) {

        // find the all the available functioning hours of the appliance
        for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
          if (functionMatrix[i]) {
            if ((minvalue < tariff.getUsageCharge(hour1, 1, 0))
                || (minvalue == tariff.getUsageCharge(hour1, 1, 0) && gen
                        .nextFloat() > VillageConstants.SAME)) {
              minvalue = tariff.getUsageCharge(hour1, 1, 0);
              minindex = i;
            }
          }
          hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);
        }

        newControllableLoad[minindex] = times * power;

      }
      // If heater is storage
      else {

        double newValue = 0;

        // find the all the available functioning hours of the appliance
        for (int i = 0; i < VillageConstants.STORAGE_HEATER_SHIFTING_END; i++) {

          newValue =
            tariff.getUsageCharge(hour1, 1, 0)
                    + tariff.getUsageCharge(new Instant(hour1.getMillis()
                                                        + TimeService.HOUR), 1,
                                            0)
                    + tariff.getUsageCharge(new Instant(hour1.getMillis() + 2
                                                        * TimeService.HOUR), 1,
                                            0)
                    + tariff.getUsageCharge(new Instant(hour1.getMillis() + 3
                                                        * TimeService.HOUR), 1,
                                            0)
                    + tariff.getUsageCharge(new Instant(hour1.getMillis() + 4
                                                        * TimeService.HOUR), 1,
                                            0);

          if ((minvalue < newValue)
              || (minvalue == newValue && gen.nextFloat() > VillageConstants.SAME)) {
            minvalue = newValue;
            minindex = i;
          }
          hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);
        }

        for (int i = 0; i <= VillageConstants.STORAGE_HEATER_PHASES; i++) {
          newControllableLoad[minindex + i] =
            VillageConstants.QUARTERS_OF_HOUR * power;
        }

        for (int i = 1; i < VillageConstants.STORAGE_HEATER_PHASES; i++) {
          newControllableLoad[VillageConstants.STORAGE_HEATER_PHASES + minindex
                              + i] = power;
        }
      }
    }
    return newControllableLoad;
  }

  public void calculateOverallPower ()
  {
    boolean flag = true;
    int day = -1;

    while (flag) {
      day = (int) (Math.random() * operationDaysVector.size());
      // log.debug("WH Choosen Day: " + day);

      flag = false;
      Vector<Integer> consumption = weeklyLoadVector.get(day);

      for (int i = 0; i < consumption.size(); i++)
        overallPower += consumption.get(i);

    }

  }

  @Override
  public void refresh (Random gen)
  {
    fillWeeklyOperation(gen);
    createWeeklyPossibilityOperationVector();
  }

}
