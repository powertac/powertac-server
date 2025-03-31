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

import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import java.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluationHelper;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.householdcustomer.configurations.VillageConstants;

/**
 * Stove is the kitchen utility we use for cooking. It is use at least twice a
 * day depending on the number of tenants. The tenants should be present when
 * functioning so this is a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Stove extends SemiShiftingAppliance
{

  @Override
  public void initialize (String household, Properties conf, int seed)
  {
    // Filling the base variables
    name = household + " Stove";
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    gen =
      randomSeedRepo.getRandomSeed(toString(), seed, "Appliance Model" + seed);
    saturation = Double.parseDouble(conf.getProperty("StoveSaturation"));
    power =
      (int) (VillageConstants.STOVE_POWER_VARIANCE * gen.nextGaussian() + VillageConstants.STOVE_POWER_MEAN);
    cycleDuration = VillageConstants.STOVE_DURATION_CYCLE;
    times = Integer.parseInt(conf.getProperty("StoveDailyTimes"));

  }

  @Override
  public void fillDailyOperation (int weekday)
  {

    // Initializing Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    for (int l = 0; l < VillageConstants.QUARTERS_OF_DAY; l++) {
      loadVector.add(0);
      dailyOperation.add(false);
    }

    Vector<Integer> temp = new Vector<Integer>();

    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY - cycleDuration; i++) {
      if (applianceOf.isEmpty(weekday, i) == false
          && applianceOf.isEmpty(weekday, i + 1) == false) {
        int count = applianceOf.tenantsNumber(weekday, i);
        for (int j = 0; j < count; j++) {
          temp.add(i);
        }
      }
    }

    if (temp.size() > 0) {
      for (int i = 0; i < times; i++) {
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

    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {
    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // In order for stove to work someone must be in the house for half hour
    for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY - 1; j++) {
      if (applianceOf.isEmpty(day, j) == false
          && applianceOf.isEmpty(day, j + 1) == false)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }

    // For the last time, without check because it is the next day
    possibilityDailyOperation.add(false);
    return possibilityDailyOperation;
  }

  @Override
  public double[] dailyShifting (Tariff tariff, double[] nonDominantUsage,
                                 TariffEvaluationHelper tariffEvalHelper,
                                 int day, Instant start)
  {

    double[] newControllableLoad = new double[VillageConstants.HOURS_OF_DAY];
    double[] newTemp = new double[VillageConstants.HOURS_OF_DAY];

    boolean[] functionMatrix = createShiftingOperationMatrix(day);

    Vector<Integer> possibleHours = new Vector<Integer>();

    // find the all the available functioning hours of the appliance
    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      if (functionMatrix[i]) {
        possibleHours.add(i);
      }
    }

    log.debug("Possible Hours: " + possibleHours.toString());

    if (possibleHours.size() == 0) {
      log.debug("Not possible to shifting due to absence.");
      return newControllableLoad;
    }

    for (int i = 0; i < times; i++) {

      int minIndex = -1;
      int counter = 1;
      double minCost = Double.POSITIVE_INFINITY;

      for (int j = 0; j < possibleHours.size(); j++) {

        newTemp = Arrays.copyOf(nonDominantUsage, nonDominantUsage.length);

        newTemp[possibleHours.get(j)] +=
          VillageConstants.QUARTERS_OF_HOUR * power;

        double cost =
            Math.abs(tariffEvalHelper.estimateCost(tariff, newTemp, start));

        // log.debug("Overall Cost for hour " + possibleHours.get(j) + " : "
        // + cost);

        if (minCost == cost)
          counter++;

        if ((minCost > cost)
            || ((minCost == cost) && gen.nextFloat() > VillageConstants.SAME)) {
          minCost = cost;
          minIndex = j;
        }

      }

      if (counter == possibleHours.size() || minIndex == -1) {
        minIndex = (int) (gen.nextDouble() * possibleHours.size());
        // System.out.println("MinIndex: " + minIndex);
        // log.debug("All the same, I choose: " + minIndex);
      }

      log.debug("Less costly hour: " + possibleHours.get(minIndex));

      newControllableLoad[possibleHours.get(minIndex)] +=
        VillageConstants.QUARTERS_OF_HOUR * power;

      newTemp = Arrays.copyOf(nonDominantUsage, nonDominantUsage.length);
      newTemp[possibleHours.get(minIndex)] +=
        VillageConstants.QUARTERS_OF_HOUR * power;

      nonDominantUsage = Arrays.copyOf(newTemp, newTemp.length);

    }

    return newControllableLoad;
  }

  @Override
  public void calculateOverallPower ()
  {

    overallPower = 0;

    for (int j = 0; j < cycleDuration; j++)
      overallPower += power;

    overallPower *= times;

    // log.debug("Overall Operation Power of " + toString() + ":" +
    // overallPower);
  }

  @Override
  public void refresh ()
  {
    fillWeeklyOperation();
    createWeeklyPossibilityOperationVector();
  }

}
