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
 * Washing Machine is used to wash clothes easily. There are several programs
 * that help you automate the procedure in order to start at a less costly time,
 * without problem. The only restriction is that must be emptied by the tenants
 * after finishing and not work at night due to noise. So this is a
 * semi-shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class WashingMachine extends SemiShiftingAppliance
{

  /**
   * This variable is utilized to show if there's a dryer in the household or
   * not.
   */
  protected boolean dryerFlag = false;

  /** This variable is utilized to show the dryer's power load. */
  protected int dryerPower = 0;

  /** This variable is utilized to find dryer in the household. */
  protected int dryerIndex = -1;

  /**
   * The function mode of the washing machine. For more info, read the details
   * in the enumerations.Mode java file
   **/
  // Mode mode = Mode.One

  /**
   * The function reaction of the washing machine. For more info, read the
   * details in the enumerations.Reaction java file
   **/
  // Reaction reaction = Reaction.Strong

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Filling the base variables
    name = household + " Washing Machine";
    saturation =
      Double.parseDouble(conf.getProperty("WashingMachineSaturation"));
    power =
      (int) (VillageConstants.DISHWASHER_POWER_VARIANCE * gen.nextGaussian() + VillageConstants.DISHWASHER_POWER_MEAN);
    cycleDuration = VillageConstants.DISHWASHER_DURATION_CYCLE;
    times =
      Integer.parseInt(conf.getProperty("WashingMachineWeeklyTimes"))
              + (int) (applianceOf.getMembers().size() / 2);
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

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // In order to function the washing machine needs someone to be there in the
    // end of its
    // operation
    for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++) {
      if (checkHouse(day, j) == true)
        possibilityDailyOperation.add(false);
      else
        possibilityDailyOperation.add(true);
    }
    return possibilityDailyOperation;
  }

  /**
   * This function checks for the household to see when it is empty or not empty
   * for the duration of the operation
   * 
   * @param hour
   * @return
   */
  boolean checkHouse (int weekday, int quarter)
  {
    if (quarter + VillageConstants.WASHING_MACHINE_DURATION_CYCLE >= VillageConstants.QUARTERS_OF_DAY)
      return true;
    else
      return applianceOf
              .isEmpty(weekday,
                       quarter
                               + VillageConstants.WASHING_MACHINE_DURATION_CYCLE);
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
      Instant hour3 = new Instant(now.getMillis() + 2 * TimeService.HOUR);
      Instant hour4 = new Instant(now.getMillis() + 3 * TimeService.HOUR);

      Vector<Integer> possibleHours = new Vector<Integer>();
      double newValue = 0;

      // find the all the available functioning hours of the appliance
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        if (functionMatrix[i] && functionMatrix[i + 1]) {
          possibleHours.add(i);
        }
      }

      log.debug(possibleHours.toString());
      // System.out.println(possibleHours.toString());

      if (possibleHours.size() == 0) {
        return newControllableLoad;
      }

      // find the all the available functioning hours of the appliance
      for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
        if (possibleHours.contains(i)) {

          if (dryerFlag)
            newValue =
              tariff.getUsageCharge(hour1, 1, 0)
                      + tariff.getUsageCharge(hour2, 1, 0)
                      + tariff.getUsageCharge(hour3, 1, 0)
                      + tariff.getUsageCharge(hour4, 1, 0);
          else
            newValue =
              tariff.getUsageCharge(hour1, 1, 0)
                      + tariff.getUsageCharge(hour2, 1, 0);

          // System.out.println("Hour " + i + " Value " + newValue +
          // " Previous Best " + minvalue[0] + " Second Best " + minvalue[1]);

          if ((minvalue[0] < newValue)
              || (minvalue[0] == newValue && gen.nextFloat() > VillageConstants.SAME)) {
            minvalue[1] = minvalue[0];
            minvalue[0] = newValue;
            minindex[1] = minindex[0];
            minindex[0] = i;
          }
          else if ((minvalue[1] < newValue)
                   || (minvalue[1] == newValue && gen.nextFloat() > VillageConstants.SAME)) {
            minvalue[1] = newValue;
            minindex[1] = i;
          }

          hour1 = new Instant(hour1.getMillis() + TimeService.HOUR);
          hour2 = new Instant(hour2.getMillis() + TimeService.HOUR);
          if (dryerFlag) {
            hour3 = new Instant(hour3.getMillis() + TimeService.HOUR);
            hour4 = new Instant(hour4.getMillis() + TimeService.HOUR);
          }
        }
      }

      // System.out.println("minindexes" + minindex[0] + " , " + minindex[1]);

      if (days.get(day) == VillageConstants.OPERATION_DAILY_TIMES_LIMIT) {

        newControllableLoad[minindex[0]] =
          VillageConstants.QUARTERS_OF_HOUR * power;
        newControllableLoad[minindex[0] + 1] =
          VillageConstants.QUARTERS_OF_HOUR * power;
        newControllableLoad[minindex[1]] =
          VillageConstants.QUARTERS_OF_HOUR * power;
        newControllableLoad[minindex[1] + 1] =
          VillageConstants.QUARTERS_OF_HOUR * power;

        if (dryerFlag) {
          newControllableLoad[minindex[0] + 2] =
            VillageConstants.QUARTERS_OF_HOUR * dryerPower
                    - VillageConstants.DRYER_THIRD_PHASE_LOAD;
          newControllableLoad[minindex[0] + 3] =
            (VillageConstants.QUARTERS_OF_HOUR / 2) * dryerPower
                    - (VillageConstants.QUARTERS_OF_HOUR + 1)
                    * VillageConstants.DRYER_THIRD_PHASE_LOAD;
          newControllableLoad[minindex[1] + 2] =
            VillageConstants.QUARTERS_OF_HOUR * dryerPower
                    - VillageConstants.DRYER_THIRD_PHASE_LOAD;
          newControllableLoad[minindex[1] + 3] =
            (VillageConstants.QUARTERS_OF_HOUR / 2) * dryerPower
                    - (VillageConstants.QUARTERS_OF_HOUR + 1)
                    * VillageConstants.DRYER_THIRD_PHASE_LOAD;

        }

      }
      else {
        newControllableLoad[minindex[0]] =
          VillageConstants.QUARTERS_OF_HOUR * power;
        newControllableLoad[minindex[0] + 1] =
          VillageConstants.QUARTERS_OF_HOUR * power;

        if (dryerFlag) {
          newControllableLoad[minindex[0] + 2] =
            VillageConstants.QUARTERS_OF_HOUR * dryerPower
                    - VillageConstants.DRYER_THIRD_PHASE_LOAD;
          newControllableLoad[minindex[0] + 3] =
            (VillageConstants.QUARTERS_OF_HOUR / 2) * dryerPower
                    - (VillageConstants.QUARTERS_OF_HOUR + 1)
                    * VillageConstants.DRYER_THIRD_PHASE_LOAD;
        }
      }

    }

    return newControllableLoad;

  }

  public boolean getDryerFlag ()
  {

    return dryerFlag;

  }

  public int getDryerIndex ()
  {

    return dryerIndex;

  }

  @Override
  public void showStatus ()
  {
    // Printing basic variables
    log.debug("Name = " + name);
    log.debug("Saturation = " + saturation);
    log.debug("Power = " + power);
    log.debug("Cycle Duration = " + cycleDuration);

    // Printing Function Day Vector
    ListIterator<Integer> iter = days.listIterator();
    log.debug("Days Vector = ");
    while (iter.hasNext())
      log.debug("Day  " + iter.next());

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

  public void calculateOverallPower ()
  {
    overallPower = 0;

    for (int j = 0; j < cycleDuration; j++)
      overallPower += power;

    if (dryerFlag) {
      overallPower += getDryerOverallPower();
    }

    // log.debug("Overall Operation Power of " + toString() + ":" +
    // overallPower);
  }

  public int getDryerOverallPower ()
  {

    int power = 0;

    Vector<Appliance> applianceList = applianceOf.getAppliances();
    for (Appliance appliance: applianceList) {
      if (appliance instanceof Dryer) {
        power = appliance.getOverallPower();
      }
    }

    return power;

  }

  @Override
  public void refresh (Random gen)
  {

    fillWeeklyOperation(gen);
    createWeeklyPossibilityOperationVector();

    // if we have dryer in the household we refresh it too
    if (dryerFlag == true) {
      Vector<Appliance> applianceList = applianceOf.getAppliances();
      for (int i = 0; i < applianceList.size(); i++) {
        if (applianceList.get(i) instanceof Dryer) {
          dryerIndex = i;
          applianceList.get(i).refresh(gen);
          break;
        }
      }
    }
  }
}
