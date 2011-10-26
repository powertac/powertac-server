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
import org.powertac.common.configurations.HouseholdConstants;

/**
 * Dryer appliances are utilized by the inhabitants to order to dry the freshly washed clothes. That
 * means that the household should contain an washiung machine in order to have a dryer. Also, the
 * clothes should be placed in the washing machine shortly after the washing is finished. So this is
 * a semi-shifting appliance.
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class Dryer extends SemiShiftingAppliance
{

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Filling the base variables
    name = household + " Dryer";
    saturation = Double.parseDouble(conf.getProperty("DryerSaturation"));
    consumptionShare = (float) (HouseholdConstants.PERCENTAGE * (HouseholdConstants.DRYER_CONSUMPTION_SHARE_VARIANCE * gen.nextGaussian() + HouseholdConstants.DRYER_CONSUMPTION_SHARE_MEAN));
    baseLoadShare = HouseholdConstants.PERCENTAGE * HouseholdConstants.DRYER_BASE_LOAD_SHARE;
    power = (int) (HouseholdConstants.DRYER_POWER_VARIANCE * gen.nextGaussian() + HouseholdConstants.DRYER_POWER_MEAN);
    cycleDuration = HouseholdConstants.DRYER_DURATION_CYCLE;
    od = false;
    inUse = false;
    probabilitySeason = fillSeason(HouseholdConstants.DRYER_POSSIBILITY_SEASON_1, HouseholdConstants.DRYER_POSSIBILITY_SEASON_2, HouseholdConstants.DRYER_POSSIBILITY_SEASON_3);
    probabilityWeekday = fillDay(HouseholdConstants.DRYER_POSSIBILITY_DAY_1, HouseholdConstants.DRYER_POSSIBILITY_DAY_2, HouseholdConstants.DRYER_POSSIBILITY_DAY_3);
    times = Integer.parseInt(conf.getProperty("DryerWeeklyTimes")) + (int) (applianceOf.getMembers().size() / 2);

    // Inform the washing machine for the existence of the dryer
    for (Appliance appliance : applianceOf.getAppliances()) {
      if (appliance instanceof WashingMachine) {
        WashingMachine wm = (WashingMachine) appliance;
        wm.dryerFlag = true;
        wm.dryerPower = power;
      }
    }

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
    int start = washingEnds(weekday);
    if (start > 0) {
      for (int i = start; i < HouseholdConstants.QUARTERS_OF_DAY - 1; i++) {
        if (applianceOf.isEmpty(weekday, i) == false) {
          operation.set(i, true);
          for (int j = i; j < i + HouseholdConstants.DRYER_SECOND_PHASE; j++) {
            loadVector.set(j, power);
            dailyOperation.set(j, true);
          }
          for (int k = i + HouseholdConstants.DRYER_SECOND_PHASE; k < i + HouseholdConstants.DRYER_THIRD_PHASE; k++) {
            loadVector.set(k, loadVector.get(k - 1) - HouseholdConstants.DRYER_THIRD_PHASE_LOAD);
            dailyOperation.set(k, true);
            if (k == HouseholdConstants.QUARTERS_OF_DAY - 1)
              break;
          }
          i = HouseholdConstants.QUARTERS_OF_DAY;
        }
      }
      weeklyLoadVector.add(loadVector);
      weeklyOperation.add(dailyOperation);
      operationVector.set(weekday, operation);
    } else {
      weeklyLoadVector.add(loadVector);
      weeklyOperation.add(dailyOperation);
      operationVector.set(weekday, operation);
    }
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++) {
      // The dishwasher needs for someone to be in the house at the beginning of its function
      if (applianceOf.isEmpty(day, j) == false)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }

    return possibilityDailyOperation;
  }

  /**
   * This function is utilized in order to find when the washing machine ends its function in order
   * to put the dryer in use soon afterwards.
   * @param weekday
   * @return
   */
  int washingEnds (int weekday)
  {

    // Creating auxiliary variables
    Vector<Boolean> v = new Vector<Boolean>();
    int start = 0;

    // Search for the washing machine to take its schedule in consideration
    for (Appliance appliance : applianceOf.getAppliances())
      if (appliance instanceof WashingMachine)
        v = appliance.getWeeklyOperation().get(weekday);

    for (int i = (HouseholdConstants.QUARTERS_OF_DAY - 1); i > 0; i--) {
      if (v.get(i) == true) {
        start = i + 1;
        i = 0;
      }
    }
    return start;
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

  /**
   * In this function we take the days of function of the washing machine in order to make dryer
   * work the same days.
   * @param times
   * @return
   */
  void fillDays (int times)
  {
    // Creating auxiliary variable
    boolean flag = true;

    // Check the appliances one by one to find the washing machine
    for (Appliance appliance : applianceOf.getAppliances()) {
      if (appliance instanceof WashingMachine || flag == false) {
        WashingMachine wm = (WashingMachine) appliance;
        days = wm.getDays();
        flag = false;
      }
    }
  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {
    // Dryer's daily shifting is done by the washing machine for safety
    long[] newControllableLoad = new long[HouseholdConstants.HOURS_OF_DAY];

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
