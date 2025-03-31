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
 * MicrowaveOven is the kitchen utility we use for cooking. It is use at least
 * twice a day depending on the number of tenants. The tenants should be present
 * when functioning so this is a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class MicrowaveOven extends SemiShiftingAppliance
{

  /**
   * This variable shows the possibility (%) that this appliance will be used
   * when workers are on break.
   */
  private double operationPercentage;

  @Override
  public void initialize (String office, Properties conf, int seed)
  {
    // Filling the base variables
    name = office + " MicrowaveOven";
    saturation =
      Double.parseDouble(conf.getProperty("MicrowaveOvenSaturation"));

    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    gen =
      randomSeedRepo.getRandomSeed(toString(), seed, "Appliance Model" + seed);
    power =
      (int) (OfficeComplexConstants.MICROWAVE_OVEN_POWER_VARIANCE
             * gen.nextGaussian() + OfficeComplexConstants.MICROWAVE_OVEN_POWER_MEAN);
    cycleDuration = OfficeComplexConstants.MICROWAVE_OVEN_DURATION_CYCLE;
    operationPercentage =
      OfficeComplexConstants.MICROWAVE_OVEN_OPERATION_PERCENTAGE;
    times = Integer.parseInt(conf.getProperty("MicrowaveOvenDailyTimes"));

  }

  @Override
  public void fillDailyOperation (int weekday)
  {

    // Initializing and Creating auxiliary variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {

      dailyOperation.add(false);
      loadVector.add(0);

      if (applianceOf.isOnBreak(weekday, i)) {

        double tempPercentage =
          operationPercentage
                  + (OfficeComplexConstants.OPERATION_PARTITION * (applianceOf
                          .employeeOnBreakNumber(weekday, i)));
        if (tempPercentage > gen.nextDouble()
            && i > OfficeComplexConstants.START_OF_LAUNCH_BREAK
            && i < OfficeComplexConstants.END_OF_LAUNCH_BREAK) {
          dailyOperation.set(i, true);
          loadVector.set(i, power);
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

    // In order for stove to work someone must be in the house for half hour
    for (int j = 0; j < OfficeComplexConstants.QUARTERS_OF_DAY; j++) {
      if (applianceOf.isOnBreak(day, j) == true)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }

    return possibilityDailyOperation;
  }

  @Override
  public double[] dailyShifting (Tariff tariff, double[] nonDominantUsage,
                                 TariffEvaluationHelper tariffEvalHelper,
                                 int day, Instant start)
  {
    double[] newControllableLoad =
      new double[OfficeComplexConstants.HOURS_OF_DAY];
    double[] newTemp = new double[OfficeComplexConstants.HOURS_OF_DAY];

    for (int i = 0; i < times; i++) {

      int minIndex = -1;
      int counter = 1;
      double minCost = Double.POSITIVE_INFINITY;

      for (int j = OfficeComplexConstants.START_OF_LAUNCH_BREAK_HOUR - 1; j < OfficeComplexConstants.END_OF_LAUNCH_BREAK_HOUR + 2; j++) {

        newTemp = Arrays.copyOf(nonDominantUsage, nonDominantUsage.length);

        newTemp[j] += power;

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

      if (counter == OfficeComplexConstants.END_OF_LAUNCH_BREAK_HOUR + 2
                     - (OfficeComplexConstants.START_OF_LAUNCH_BREAK_HOUR - 1)
          || minIndex == -1) {
        minIndex =
          (int) (gen.nextDouble() * counter)
                  + OfficeComplexConstants.START_OF_LAUNCH_BREAK_HOUR - 1;
        // log.debug("All the same, I choose: " + minIndex);
      }

      log.debug("Less costly hour: " + minIndex);

      newControllableLoad[minIndex] += power;

      newTemp = Arrays.copyOf(nonDominantUsage, nonDominantUsage.length);
      newTemp[minIndex] += OfficeComplexConstants.QUARTERS_OF_HOUR * power;

      nonDominantUsage = Arrays.copyOf(newTemp, newTemp.length);

    }

    return newControllableLoad;
  }

  @Override
  public void calculateOverallPower ()
  {
    overallPower = power * times;
  }

  @Override
  public void refresh ()
  {
    fillWeeklyOperation();
    createWeeklyPossibilityOperationVector();
  }

}
