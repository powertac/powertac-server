/*
 * Copyright 2009-2012 the original author or authors.
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

package org.powertac.officecomplexcustomer.appliances;

import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import java.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluationHelper;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;

/**
 * Lights are utilized when the persons inhabiting the house have need for them
 * to light the rooms they are in. So it's a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class CopyMachine extends SemiShiftingAppliance
{

  /**
   * This variable shows the power consumed by the servers when they are in
   * sleep mode, most of the day
   */
  private int standbyPower;

  @Override
  public void initialize (String office, Properties conf, int seed)
  {
    // Filling the base variables
    name = office + " CopyMachine";
    saturation = Double.parseDouble(conf.getProperty("CopyMachineSaturation"));
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    gen =
      randomSeedRepo.getRandomSeed(toString(), seed, "Appliance Model" + seed);

    power =
      (int) (OfficeComplexConstants.COPY_MACHINE_POWER_VARIANCE
             * gen.nextGaussian() + OfficeComplexConstants.COPY_MACHINE_POWER_MEAN);
    standbyPower =
      (int) (OfficeComplexConstants.COPY_MACHINE_STANDBY_POWER_VARIANCE
             * gen.nextGaussian() + OfficeComplexConstants.COPY_MACHINE_STANDBY_POWER_MEAN);
    cycleDuration = OfficeComplexConstants.COPY_MACHINE_DURATION_CYCLE;
    times =
      Integer.parseInt(conf.getProperty("CopyMachineDailyTimes"))
              + (int) (applianceOf.getMembers().size() / OfficeComplexConstants.PERSONS);
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {
    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // Lights need to operate only when someone is in the house
    for (int j = 0; j < OfficeComplexConstants.QUARTERS_OF_DAY; j++) {
      if (applianceOf.isWorking(day, j) == true)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }

    return possibilityDailyOperation;
  }

  @Override
  public void fillDailyOperation (int weekday)
  {
    // Initializing and Creating auxiliary variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    Vector<Integer> temp = new Vector<Integer>();

    // For each quarter of a day
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {

      if ((i > OfficeComplexConstants.START_OF_FUNCTION && i < OfficeComplexConstants.END_OF_FUNCTION)
          && !(applianceOf.isOnVacation(weekday))) {
        loadVector.add(standbyPower);
        dailyOperation.add(true);

        int count = applianceOf.employeeNumber(weekday, i);
        for (int j = 0; j < count; j++) {
          temp.add(i);
        }

      }
      else {
        loadVector.add(0);
        dailyOperation.add(false);
      }
    }

    if (temp.size() > 0) {
      for (int i = 0; i < times; i++) {
        int rand = gen.nextInt(temp.size());
        int quarter = temp.get(rand);

        loadVector.set(quarter, power);
        temp.remove(rand);
      }
    }

    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  @Override
  public double[] dailyShifting (Tariff tariff, double[] nonDominantUsage,
                                 TariffEvaluationHelper tariffEvalHelper,
                                 int day, Instant start)
  {

    double[] newControllableLoad =
      new double[OfficeComplexConstants.HOURS_OF_DAY];

    double[] newTemp = new double[OfficeComplexConstants.HOURS_OF_DAY];

    for (int j = OfficeComplexConstants.START_OF_FUNCTION_HOURS; j < OfficeComplexConstants.END_OF_FUNCTION_HOUR; j++)
      newControllableLoad[j] =
        OfficeComplexConstants.QUARTERS_OF_HOUR * standbyPower;

    for (int i = 0; i < times; i++) {

      int minIndex = -1;
      int counter = 1;
      double minCost = Double.POSITIVE_INFINITY;

      for (int j = OfficeComplexConstants.START_OF_FUNCTION_HOURS; j < OfficeComplexConstants.END_OF_FUNCTION_HOUR; j++) {

        newTemp = Arrays.copyOf(nonDominantUsage, nonDominantUsage.length);

        newTemp[j] += OfficeComplexConstants.QUARTERS_OF_HOUR * power;

        double cost =
            Math.abs(tariffEvalHelper.estimateCost(tariff, newTemp, start));

        // log.debug("Overall Cost for hour " + j + " : " + cost);

        if (minCost == cost)
          counter++;

        if ((minCost > cost)
            || ((minCost == cost) && gen.nextFloat() > OfficeComplexConstants.SAME)) {
          minCost = cost;
          minIndex = j;
        }

      }

      if (counter == OfficeComplexConstants.END_OF_FUNCTION_HOUR
                     - OfficeComplexConstants.START_OF_FUNCTION_HOURS
          || minIndex == -1) {
        minIndex =
          (int) (gen.nextDouble() * counter)
                  + OfficeComplexConstants.START_OF_FUNCTION_HOURS;
        // log.debug("All the same, I choose: " + minIndex);
      }

      log.debug("Less costly hour: " + minIndex);

      newControllableLoad[minIndex] += power;

      newTemp = Arrays.copyOf(nonDominantUsage, nonDominantUsage.length);
      newTemp[minIndex] += power;

      nonDominantUsage = Arrays.copyOf(newTemp, newTemp.length);

    }

    return newControllableLoad;
  }

  /*
    @Override
    public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
    {
      long[] newControllableLoad = new long[OfficeComplexConstants.HOURS_OF_DAY];

      int[] minindex = new int[2];
      double[] minvalue = { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
      Instant hour1 =
        Instant.ofEpochMilli(now.toInstant().toEpochMilli() + TimeService.HOUR
                    * OfficeComplexConstants.START_OF_FUNCTION_HOURS);

      for (int i = OfficeComplexConstants.START_OF_FUNCTION_HOURS; i < OfficeComplexConstants.END_OF_FUNCTION_HOUR; i++) {

        newControllableLoad[i] =
          OfficeComplexConstants.QUARTERS_OF_HOUR * standbyPower;

        if ((minvalue[0] < tariff.getUsageCharge(hour1, 1, 0))
            || (minvalue[0] == tariff.getUsageCharge(hour1, 1, 0) && gen
                    .nextFloat() > OfficeComplexConstants.SAME)) {
          minvalue[1] = minvalue[0];
          minvalue[0] = tariff.getUsageCharge(hour1, 1, 0);
          minindex[1] = minindex[0];
          minindex[0] = i;
        }
        else if ((minvalue[1] < tariff.getUsageCharge(hour1, 1, 0))
                 || (minvalue[1] == tariff.getUsageCharge(hour1, 1, 0) && gen
                         .nextFloat() > OfficeComplexConstants.SAME)) {
          minvalue[1] = tariff.getUsageCharge(hour1, 1, 0);
          minindex[1] = i;
        }

        hour1 = Instant.ofEpochMilli(hour1.toInstant().toEpochMilli() + TimeService.HOUR);

      }

      if (times > 4) {

        newControllableLoad[minindex[0]] =
          OfficeComplexConstants.QUARTERS_OF_HOUR * power;
        newControllableLoad[minindex[1]] =
          (times - OfficeComplexConstants.QUARTERS_OF_HOUR) * power;

      }
      else {
        newControllableLoad[minindex[0]] = times * power;
      }

      return newControllableLoad;
    }
  */
  @Override
  public void calculateOverallPower ()
  {
    overallPower = 0;

    for (int i = OfficeComplexConstants.START_OF_FUNCTION_HOURS; i < OfficeComplexConstants.END_OF_FUNCTION_HOUR; i++)
      overallPower += OfficeComplexConstants.QUARTERS_OF_HOUR * standbyPower;

    for (int i = 0; i < times; i++)
      overallPower += power - standbyPower;

  }

  @Override
  public void refresh ()
  {
    fillWeeklyOperation();
    createWeeklyPossibilityOperationVector();
  }

}
