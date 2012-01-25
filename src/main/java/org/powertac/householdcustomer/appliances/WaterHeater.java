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

import java.util.ListIterator;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.common.configurations.HouseholdConstants;
import org.powertac.common.enumerations.HeaterType;

/**
 * Circulation pump is the appliance that brings water to the household. It
 * works most of the hours of the day, but always when someone is at home in
 * need of water. So it's a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class WaterHeater extends FullyShiftingAppliance
{

  /**
   * The type of the water heater. For more info, read the details in the
   * enumerations.HeaterType java file
   **/
  HeaterType type;

  @Override
  public void fillDailyFunction (int weekday, Random gen)
  {
    // Initializing And Creating Auxiliary Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    Vector<Boolean> operation = new Vector<Boolean>();

    if (type == HeaterType.InstantHeater) {
      operation = operationVector.get(weekday);
      for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
        if (operation.get(i) == true) {
          boolean flag = true;
          int counter = 0;
          while ((flag) && (i < HouseholdConstants.QUARTERS_OF_DAY) && (counter >= 0)) {
            if (applianceOf.isEmpty(weekday, i) == false) {
              loadVector.add(power);
              dailyOperation.add(true);
              counter--;
              if (counter < 0)
                flag = false;
            } else {
              loadVector.add(0);
              dailyOperation.add(false);
              i++;
              if (i < HouseholdConstants.QUARTERS_OF_DAY && operation.get(i) == true)
                counter++;
            }
          }
        } else {
          loadVector.add(0);
          dailyOperation.add(false);
        }
      }
      weeklyLoadVector.add(loadVector);
      weeklyOperation.add(dailyOperation);

    } else {

      int start = 0;
      int temp = 0;

      for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
        operation.add(false);
        dailyOperation.add(false);
        loadVector.add(0);
      }

      if (gen.nextFloat() > HouseholdConstants.STORAGE_HEATER_POSSIBILITY)
        start = (HouseholdConstants.STORAGE_HEATER_START + 1) + gen.nextInt(HouseholdConstants.STORAGE_HEATER_START - 1);
      else
        start = 1 + gen.nextInt(HouseholdConstants.STORAGE_HEATER_START);

      for (int i = start; i < start + HouseholdConstants.STORAGE_HEATER_PHASE_LOAD; i++) {
        operation.set(i, true);
        dailyOperation.set(i, true);
        loadVector.set(i, power);
      }

      temp = start + HouseholdConstants.STORAGE_HEATER_PHASE_LOAD;

      for (int j = 0; j < HouseholdConstants.STORAGE_HEATER_PHASES - 1; j++) {
        operation.set((temp + HouseholdConstants.STORAGE_HEATER_PHASES * j), true);
        dailyOperation.set((temp + HouseholdConstants.STORAGE_HEATER_PHASES * j), true);
        loadVector.set((temp + HouseholdConstants.STORAGE_HEATER_PHASES * j), power);
      }
      weeklyLoadVector.add(loadVector);
      weeklyOperation.add(dailyOperation);
      operationVector.add(operation);
    }
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // If the heater is instant Heater
    if (type == HeaterType.InstantHeater) {
      // It can operate each quarter someone is at home to turn it on
      for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++) {
        if (applianceOf.isEmpty(day, j) == false)
          possibilityDailyOperation.add(true);
        else
          possibilityDailyOperation.add(false);
      }
    }
    // If heater is storage
    else {
      // It can operate all quarters of day
      for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++) {
        possibilityDailyOperation.add(true);
      }
    }
    return possibilityDailyOperation;
  }

  @Override
  public void showStatus ()
  {
    // Printing basic variables
    log.info("Name = " + name);
    log.info("Saturation = " + saturation);
    log.info("Power = " + power);
    log.info("Heater Type = " + type);
    log.info("Cycle Duration = " + cycleDuration);
    log.info("Occupancy Dependence = " + od);

    // Printing Weekly Operation Vector and Load Vector
    log.info("Weekly Operation Vector and Load = ");

    for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION; i++) {
      log.info("Day " + i);
      ListIterator<Boolean> iter3 = weeklyOperation.get(i).listIterator();
      ListIterator<Integer> iter4 = weeklyLoadVector.get(i).listIterator();
      for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++)
        log.info("Quarter " + j + " = " + iter3.next() + "   Load = " + iter4.next());
    }
  }

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Creating Auxiliary Variables
    int x = 1 + gen.nextInt(HouseholdConstants.PERCENTAGE);
    int limit = Integer.parseInt(conf.getProperty("InstantHeater"));
    // Filling the base variables
    name = household + " WaterHeater";
    saturation = Double.parseDouble(conf.getProperty("WaterHeaterSaturation"));
    // If the heater is instant Heater
    if (x < limit) {
      power = (int) (HouseholdConstants.INSTANT_HEATER_POWER_VARIANCE * gen.nextGaussian() + HouseholdConstants.INSTANT_HEATER_POWER_MEAN);
      cycleDuration = HouseholdConstants.INSTANT_HEATER_DURATION_CYCLE;
      od = false;
      type = HeaterType.InstantHeater;
      times = Integer.parseInt(conf.getProperty("InstantHeaterDailyTimes")) + (int) (applianceOf.getMembers().size() / 2);
      createWeeklyOperationVector(times, gen);
    }
    // If heater is storage
    else {
      power = (int) (HouseholdConstants.STORAGE_HEATER_POWER_VARIANCE * gen.nextGaussian() + HouseholdConstants.STORAGE_HEATER_POWER_MEAN);
      cycleDuration = HouseholdConstants.STORAGE_HEATER_DURATION_CYCLE;
      od = false;
      type = HeaterType.StorageHeater;
    }
  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {

    long[] newControllableLoad = new long[HouseholdConstants.HOURS_OF_DAY];

    // If the heater is instant Heater
    if (type == HeaterType.InstantHeater) {

      // If the heater is working the day of the shifting
      if (operationDaysVector.get(day)) {

        int minindex = 0;
        boolean[] functionMatrix = createShiftingOperationMatrix(day);

        // case of fixed tariff rate
        if ((tariff.getTariffSpec().getRates().size() == 1) && (tariff.getTariffSpec().getRates().get(0).isFixed())) {
          Vector<Integer> possibleHours = new Vector<Integer>();

          // find the all the available functioning hours of the appliance
          for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
            if (functionMatrix[i]) {
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

          // find the all the available functioning hours of the appliance
          for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
            if (functionMatrix[i]) {
              if (minvalue >= tariff.getUsageCharge(hour1, 1, 0)) {
                minvalue = tariff.getUsageCharge(hour1, 1, 0);
                minindex = i;
              }
            }
            hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);
          }
        }
        newControllableLoad[minindex] = times * power;
      }
    }
    // If heater is storage
    else {

      // If the heater is working the day of the shifting
      if (operationDaysVector.get(day)) {
        int minindex = 0;
        boolean[] functionMatrix = createShiftingOperationMatrix(day);

        // case of fixed tariff rate
        if ((tariff.getTariffSpec().getRates().size() == 1) && (tariff.getTariffSpec().getRates().get(0).isFixed())) {
          Vector<Integer> possibleHours = new Vector<Integer>();
          // find the all the available functioning hours of the appliance
          for (int i = 0; i < HouseholdConstants.STORAGE_HEATER_SHIFTING_END; i++) {
            if (functionMatrix[i]) {
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

          // find the all the available functioning hours of the appliance
          for (int i = 0; i < HouseholdConstants.STORAGE_HEATER_SHIFTING_END; i++) {
            if (functionMatrix[i]) {
              if (minvalue >= tariff.getUsageCharge(hour1, 1, 0) + tariff.getUsageCharge(new Instant(hour1.getMillis() + TimeService.HOUR), 1, 0)
                  + tariff.getUsageCharge(new Instant(hour1.getMillis() + 2 * TimeService.HOUR), 1, 0) + tariff.getUsageCharge(new Instant(hour1.getMillis() + 3 * TimeService.HOUR), 1, 0)
                  + tariff.getUsageCharge(new Instant(hour1.getMillis() + 4 * TimeService.HOUR), 1, 0)) {
                minvalue = tariff.getUsageCharge(hour1, 1, 0) + tariff.getUsageCharge(new Instant(hour1.getMillis() + TimeService.HOUR), 1, 0)
                    + tariff.getUsageCharge(new Instant(hour1.getMillis() + 2 * TimeService.HOUR), 1, 0) + tariff.getUsageCharge(new Instant(hour1.getMillis() + 3 * TimeService.HOUR), 1, 0)
                    + tariff.getUsageCharge(new Instant(hour1.getMillis() + 4 * TimeService.HOUR), 1, 0);
                minindex = i;
              }
            }
            hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);
          }
        }
        for (int i = 0; i <= HouseholdConstants.STORAGE_HEATER_PHASES; i++) {
          newControllableLoad[minindex + i] = HouseholdConstants.QUARTERS_OF_HOUR * power;
        }

        for (int i = 1; i < HouseholdConstants.STORAGE_HEATER_PHASES; i++) {
          newControllableLoad[HouseholdConstants.STORAGE_HEATER_PHASES + minindex + i] = power;
        }
      }
    }
    return newControllableLoad;
  }

  @Override
  public void refresh (Random gen)
  {
    // case the Water Heater is Instant
    if (type == HeaterType.InstantHeater)
      createWeeklyOperationVector(times, gen);
    fillWeeklyFunction(gen);
    createWeeklyPossibilityOperationVector();
  }

}
