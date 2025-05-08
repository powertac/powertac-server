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
import java.util.Vector;

import java.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluationHelper;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.householdcustomer.configurations.VillageConstants;

/**
 * Dryer appliances are utilized by the inhabitants to order to dry the freshly
 * washed clothes. That means that the household should contain an washing
 * machine in order to have a dryer. Also, the clothes should be placed in the
 * washing machine shortly after the washing is finished. So this is a
 * semi-shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Dryer extends SemiShiftingAppliance
{

  @Override
  public void initialize (String household, Properties conf, int seed)
  {

    // Filling the base variables
    name = household + " Dryer";
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    gen =
      randomSeedRepo.getRandomSeed(toString(), seed, "Appliance Model" + seed);
    saturation = Double.parseDouble(conf.getProperty("DryerSaturation"));
    power =
      (int) (VillageConstants.DRYER_POWER_VARIANCE * gen.nextGaussian() + VillageConstants.DRYER_POWER_MEAN);
    cycleDuration = VillageConstants.DRYER_DURATION_CYCLE;

  }

  @Override
  public void fillDailyOperation (int weekday)
  {

    // Inform the washing machine for the existence of the dryer
    for (Appliance appliance: applianceOf.getAppliances()) {
      if (appliance instanceof WashingMachine wm) {
        times = wm.getTimes();
        days = wm.getDays();
        wm.dryerFlag = true;
        wm.dryerPower = power;
        break;
      }
    }

    // Initializing Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    for (int l = 0; l < VillageConstants.QUARTERS_OF_DAY; l++) {
      loadVector.add(0);
      dailyOperation.add(false);
    }

    int start = washingEnds(weekday);

    if (start > 0) {
      for (int i = start; i < VillageConstants.QUARTERS_OF_DAY - 1; i++) {
        if (applianceOf.isEmpty(weekday, i) == false) {
          for (int j = i; j < i + VillageConstants.DRYER_SECOND_PHASE; j++) {
            loadVector.set(j, power);
            dailyOperation.set(j, true);
            if (j == VillageConstants.QUARTERS_OF_DAY - 1)
              break;
          }
          for (int k = i + VillageConstants.DRYER_SECOND_PHASE; k < i
                                                                    + VillageConstants.DRYER_THIRD_PHASE; k++) {
            if (k >= VillageConstants.QUARTERS_OF_DAY) {
              // System.out.println("K out of bounds " + k);
              break;
            }
            loadVector.set(k, loadVector.get(k - 1)
                              - VillageConstants.DRYER_THIRD_PHASE_LOAD);
            dailyOperation.set(k, true);

          }
          i = VillageConstants.QUARTERS_OF_DAY;
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

    for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++) {
      // The dishwasher needs for someone to be in the house at the beginning of
      // its function
      if (applianceOf.isEmpty(day, j) == false)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }

    return possibilityDailyOperation;
  }

  /**
   * This function is utilized in order to find when the washing machine ends
   * its function to fill the dryer and start its operation soon afterwards.
   * 
   * @param weekday
   * @return
   */
  int washingEnds (int weekday)
  {

    // Creating auxiliary variables
    Vector<Boolean> v = new Vector<Boolean>();
    int start = 0;

    // Search for the washing machine to take its schedule in consideration
    for (Appliance appliance: applianceOf.getAppliances())
      if (appliance instanceof WashingMachine)
        v =
          appliance.getWeeklyOperation()
                  .get(applianceOf.getWeek() * VillageConstants.DAYS_OF_WEEK
                               + weekday);

    for (int i = (VillageConstants.QUARTERS_OF_DAY - 1); i > 0; i--) {
      if (v.get(i) == true) {
        start = i + 1;
        i = 0;
      }
    }
    return start;
  }

  // dead code
//  @Override
//  public void showStatus ()
//  {
//    // Printing basic variables
//    log.debug("Name = " + name);
//    log.debug("Saturation = " + saturation);
//    log.debug("Power = " + power);
//    log.debug("Cycle Duration = " + cycleDuration);
//
//    // Printing Function Day Vector
//    ListIterator<Integer> iter = days.listIterator();
//    log.debug("Days Vector = ");
//    while (iter.hasNext())
//      log.debug("Day  " + iter.next());
//
//    // Printing Weekly Operation Vector and Load Vector
//    log.debug("Weekly Operation Vector and Load = ");
//
//    for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION
//                        + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
//      log.debug("Day " + i);
//      ListIterator<Boolean> iter3 = weeklyOperation.get(i).listIterator();
//      ListIterator<Integer> iter4 = weeklyLoadVector.get(i).listIterator();
//      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++)
//        log.debug("Quarter " + j + " = " + iter3.next() + "  Load = "
//                  + iter4.next());
//    }
//  }

  @Override
  public double[] dailyShifting (Tariff tariff, double[] nonDominantUsage,
                                 TariffEvaluationHelper tariffEvalHelper,
                                 int day, Instant start)
  {

    double[] newControllableLoad = new double[VillageConstants.HOURS_OF_DAY];

    return newControllableLoad;
  }

  @Override
  public void calculateOverallPower ()
  {
    overallPower = 0;

    for (int j = 0; j < VillageConstants.DRYER_SECOND_PHASE; j++)
      overallPower += power;

    for (int k = 0; k < VillageConstants.DRYER_THIRD_PHASE
                        - VillageConstants.DRYER_SECOND_PHASE; k++)
      overallPower += power - (k + 1) * VillageConstants.DRYER_THIRD_PHASE_LOAD;

    // log.debug("Overall Operation Power of " + toString() + ":" +
    // overallPower);
  }

  @Override
  public void refresh ()
  {
    for (Appliance appliance: applianceOf.getAppliances()) {
      if (appliance instanceof WashingMachine wm) {
        days = wm.getDays();
        break;
      }
    }
    fillWeeklyOperation();
    createWeeklyPossibilityOperationVector();
  }

}
