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

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.common.configurations.HouseholdConstants;

/**
 * Washing Machine is used to wash clothes easily. There are several programs that help you automate
 * the procedure in order to start at a less costly time, without problem. The only restriction is
 * that must be emptied by the tenants after finishing and not work at night due to noise. So this
 * is a semi-shifting appliance.
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class WashingMachine extends SemiShiftingAppliance
{

  /** This variable is utilized to show if there's a dryer in the household or not. */
  public boolean dryerFlag = false;

  /** This variable is utilized to show the dryer's power load. */
  public int dryerPower = 0;

  /**
   * The function mode of the washing machine. For more info, read the details in the
   * enumerations.Mode java file
   **/
  // Mode mode = Mode.One

  /**
   * The function reaction of the washing machine. For more info, read the details in the
   * enumerations.Reaction java file
   **/
  // Reaction reaction = Reaction.Strong

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Filling the base variables
    name = household + " Washing Machine";
    saturation = Double.parseDouble(conf.getProperty("WashingMachineSaturation"));
    consumptionShare = (float) (HouseholdConstants.PERCENTAGE * (HouseholdConstants.DISHWASHER_CONSUMPTION_SHARE_VARIANCE * gen.nextGaussian() + HouseholdConstants.DISHWASHER_CONSUMPTION_SHARE_MEAN));
    baseLoadShare = HouseholdConstants.PERCENTAGE * HouseholdConstants.DISHWASHER_BASE_LOAD_SHARE;
    power = (int) (HouseholdConstants.DISHWASHER_POWER_VARIANCE * gen.nextGaussian() + HouseholdConstants.DISHWASHER_POWER_MEAN);
    cycleDuration = HouseholdConstants.DISHWASHER_DURATION_CYCLE;
    od = false;
    inUse = false;
    probabilitySeason = fillSeason(HouseholdConstants.DISHWASHER_POSSIBILITY_SEASON_1, HouseholdConstants.DISHWASHER_POSSIBILITY_SEASON_2, HouseholdConstants.DISHWASHER_POSSIBILITY_SEASON_3);
    probabilityWeekday = fillDay(HouseholdConstants.DISHWASHER_POSSIBILITY_DAY_1, HouseholdConstants.DISHWASHER_POSSIBILITY_DAY_2, HouseholdConstants.DISHWASHER_POSSIBILITY_DAY_3);
    times = Integer.parseInt(conf.getProperty("WashingMachineWeeklyTimes")) + (int) (applianceOf.getMembers().size() / 2);
    createWeeklyOperationVector(times, gen);
  }

  @Override
  public void fillDailyFunction (int weekday, Random gen)
  {
    // Initializing Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    Vector<Boolean> operation = operationVector.get(weekday);
    for (int l = 0; l < HouseholdConstants.QUARTERS_OF_DAY; l++) {
      loadVector.add(0);
      dailyOperation.add(false);
    }
    for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
      if (operation.get(i) == true) {
        boolean flag = true;
        while (flag && i < HouseholdConstants.QUARTERS_OF_DAY) {
          boolean empty = checkHouse(weekday, i);
          if (empty == false) {
            for (int k = i; k < i + HouseholdConstants.WASHING_MACHINE_DURATION_CYCLE; k++) {
              loadVector.set(k, power);
              dailyOperation.set(k, true);
              if (k == HouseholdConstants.QUARTERS_OF_DAY - 1)
                break;
            }
            i = HouseholdConstants.QUARTERS_OF_DAY;
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

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // In order to function the washing machine needs someone to be there in the end of its
    // operation
    for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++) {
      if (checkHouse(day, j) == true)
        possibilityDailyOperation.add(false);
      else
        possibilityDailyOperation.add(true);
    }
    return possibilityDailyOperation;
  }

  /**
   * This function checks for the household to see when it is empty or not empty for the duration of
   * the operation
   * @param hour
   * @return
   */
  boolean checkHouse (int weekday, int quarter)
  {
    if (quarter + HouseholdConstants.WASHING_MACHINE_DURATION_CYCLE >= HouseholdConstants.QUARTERS_OF_DAY)
      return true;
    else
      return applianceOf.isEmpty(weekday, quarter + HouseholdConstants.WASHING_MACHINE_DURATION_CYCLE);
  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {

    long[] newControllableLoad = new long[HouseholdConstants.HOURS_OF_DAY];

    // If it supposed to operate at the day of shifting
    if (operationDaysVector.get(day)) {

      int minindex = 0;
      boolean[] functionMatrix = createShiftingOperationMatrix(day);

      // case of fixed tariff rate
      if ((tariff.getTariffSpec().getRates().size() == 1) && (tariff.getTariffSpec().getRates().get(0).isFixed())) {
        Vector<Integer> possibleHours = new Vector<Integer>();

        // find the all the available functioning hours of the appliance
        for (int i = 0; i < HouseholdConstants.END_OF_FUNCTION_HOUR; i++) {
          if (functionMatrix[i])
            possibleHours.add(i);
        }
        minindex = possibleHours.get(gen.nextInt(possibleHours.size()));

        newControllableLoad[minindex] = HouseholdConstants.QUARTERS_OF_HOUR * power;
        newControllableLoad[minindex + 1] = HouseholdConstants.QUARTERS_OF_HOUR * power;

        // if we have dryer in the household
        if (dryerFlag) {
          newControllableLoad[minindex + 2] = HouseholdConstants.QUARTERS_OF_HOUR * dryerPower - HouseholdConstants.DRYER_THIRD_PHASE_LOAD;
          newControllableLoad[minindex + 3] = (HouseholdConstants.QUARTERS_OF_HOUR / 2) * dryerPower - (HouseholdConstants.QUARTERS_OF_HOUR + 1) * HouseholdConstants.DRYER_THIRD_PHASE_LOAD;
        }
      }
      // case of variable tariff rate
      else {

        double minvalue = Double.POSITIVE_INFINITY;
        Instant hour1 = now;

        // if we have dryer in the household
        if (dryerFlag) {

          // find the all the available functioning hours of the appliance
          for (int i = 0; i < HouseholdConstants.END_OF_FUNCTION_HOUR; i++) {
            if (functionMatrix[i]) {
              if (minvalue >= tariff.getUsageCharge(hour1, 1, 0) + tariff.getUsageCharge(new Instant(hour1.getMillis() + TimeService.HOUR), 1, 0)
                  + tariff.getUsageCharge(new Instant(hour1.getMillis() + 2 * TimeService.HOUR), 1, 0) + tariff.getUsageCharge(new Instant(hour1.getMillis() + 3 * TimeService.HOUR), 1, 0)) {
                minvalue = tariff.getUsageCharge(hour1, 1, 0) + tariff.getUsageCharge(new Instant(hour1.getMillis() + TimeService.HOUR), 1, 0)
                    + tariff.getUsageCharge(new Instant(hour1.getMillis() + 2 * TimeService.HOUR), 1, 0) + tariff.getUsageCharge(new Instant(hour1.getMillis() + 3 * TimeService.HOUR), 1, 0);
                minindex = i;
              }
            }
            hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);
          }
          newControllableLoad[minindex] = HouseholdConstants.QUARTERS_OF_HOUR * power;
          newControllableLoad[minindex + 1] = HouseholdConstants.QUARTERS_OF_HOUR * power;
          newControllableLoad[minindex + 2] = HouseholdConstants.QUARTERS_OF_HOUR * dryerPower - HouseholdConstants.DRYER_THIRD_PHASE_LOAD;
          newControllableLoad[minindex + 3] = (HouseholdConstants.QUARTERS_OF_HOUR / 2) * dryerPower - (HouseholdConstants.QUARTERS_OF_HOUR + 1) * HouseholdConstants.DRYER_THIRD_PHASE_LOAD;
        }
        // if we don't have dryer in the household
        else {
          // find the all the available functioning hours of the appliance
          if (operationDaysVector.get(day)) {
            for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
              if (functionMatrix[i]) {
                if (minvalue >= tariff.getUsageCharge(hour1, 1, 0) + tariff.getUsageCharge(new Instant(hour1.getMillis() + TimeService.HOUR), 1, 0)) {
                  minvalue = tariff.getUsageCharge(hour1, 1, 0) + tariff.getUsageCharge(new Instant(hour1.getMillis() + TimeService.HOUR), 1, 0);
                  minindex = i;
                }
              }
              hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);
            }
            newControllableLoad[minindex] = HouseholdConstants.QUARTERS_OF_HOUR * power;
            newControllableLoad[minindex + 1] = HouseholdConstants.QUARTERS_OF_HOUR * power;
          }
        }
      }
    }
    return newControllableLoad;
  }

  @Override
  public void showStatus ()
  {
    // Printing basic variables
    log.info("Name = " + name);
    log.info("Saturation = " + saturation);
    log.info("Consumption Share = " + consumptionShare);
    log.info("Base Load Share = " + baseLoadShare);
    log.info("Power = " + power);
    log.info("Cycle Duration = " + cycleDuration);
    log.info("Occupancy Dependence = " + od);
    log.info("In Use = " + inUse);

    // Printing probability variables variables
    Set<Entry<String, Double>> set = probabilitySeason.entrySet();
    Iterator<Entry<String, Double>> it = set.iterator();
    log.info("Probability Season = ");
    while (it.hasNext()) {
      Map.Entry<String, Double> me = (Map.Entry<String, Double>) it.next();
      log.info(me.getKey() + " : " + me.getValue());
    }

    set = probabilityWeekday.entrySet();
    it = set.iterator();
    log.info("Probability Weekday = ");
    while (it.hasNext()) {
      Map.Entry<String, Double> me = (Map.Entry<String, Double>) it.next();
      log.info(me.getKey() + " : " + me.getValue());
    }

    // Printing Function Day Vector
    ListIterator<Integer> iter = days.listIterator();
    log.info("Days Vector = ");
    while (iter.hasNext())
      log.info("Day  " + iter.next());

    // Printing Operation Vector
    log.info("Operation Vector = ");
    for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK; i++) {
      log.info("Day " + (i + 1));
      ListIterator<Boolean> iter3 = operationVector.get(i).listIterator();
      for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++)
        log.info("Quarter : " + (j + 1) + "  " + iter3.next());
    }

    // Printing Weekly Operation Vector and Load Vector
    log.info("Weekly Operation Vector and Load = ");

    for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK; i++) {
      log.info("Day " + (i + 1));
      ListIterator<Boolean> iter3 = weeklyOperation.get(i).listIterator();
      ListIterator<Integer> iter4 = weeklyLoadVector.get(i).listIterator();
      for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++)
        log.info("Quarter " + (j + 1) + " = " + iter3.next() + "   Load = " + iter4.next());
    }
  }

  @Override
  public void refresh (Random gen)
  {
    createWeeklyOperationVector(times, gen);
    fillWeeklyFunction(gen);
    createWeeklyPossibilityOperationVector();

    // if we have dryer in the household we refresh it too
    if (dryerFlag == true) {
      Vector<Appliance> applianceList = applianceOf.getAppliances();
      for (Appliance appliance : applianceList) {
        if (appliance instanceof Dryer) {
          operationVector = new Vector<Vector<Boolean>>();
          appliance.refresh(gen);
        }
      }
    }
  }
}
