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

import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.householdcustomer.configurations.VillageConstants;

/**
 * Space Heater is a electric appliance utilized to keep the rooms of a
 * household warm when needed. These devices can work automatically in order to
 * save as much energy as possible, knowing when the room must be warm. So this
 * is a fully shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class SpaceHeater extends WeatherSensitiveAppliance
{

  /**
   * Variable that presents the mean possibility to utilize the appliance each
   * hour of the day that someone is present in the household.
   */
  double operationPercentage;

  /**
   * Variable that presents the temperature that is inconvenient enough for the
   * inhabitants of the house in the start of the day in order to open the
   * appliance.
   */
  int temperatureThreshold;

  @Override
  public void initialize (String household, Properties conf, int seed)
  {
    // Filling the base variables
    name = household + " SpaceHeater";
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    gen =
      randomSeedRepo.getRandomSeed(toString(), seed, "Appliance Model" + seed);
    saturation = Double.parseDouble(conf.getProperty("SpaceHeaterSaturation"));
    operationPercentage =
      Double.parseDouble(conf.getProperty("SpaceHeaterPercentage"));
    temperatureThreshold =
      (int) (VillageConstants.SPACE_HEATER_TEMPERATURE_VARIANCE
             * gen.nextGaussian() + VillageConstants.SPACE_HEATER_TEMPERATURE_MEAN);
    power =
      (int) (VillageConstants.SPACE_HEATER_POWER_VARIANCE * gen.nextGaussian() + VillageConstants.SPACE_HEATER_POWER_MEAN);
    cycleDuration = VillageConstants.SPACE_HEATER_DURATION_CYCLE;

  }

  @Override
  public void fillDailyOperation (int weekday)
  {
    // Initializing Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      loadVector.add(0);
      dailyOperation.add(false);
    }
    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);

  }

  @Override
  public void weatherDailyOperation (int day, int hour, double temp)
  {

    double perc = gen.nextDouble();

    // System.out.println(this.toString() + " " +
    // (applianceOf.isOnVacation(day)) + " " + (temp > temperatureThreshold) +
    // " " + (perc > percentage));

    if ((applianceOf.isOnVacation(day)) || (temp > temperatureThreshold)
        || (perc > operationPercentage)) {

    }
    else {
      for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
        loadVector.add(0);
        dailyOperation.add(true);
      }
      for (int i = 0; i < VillageConstants.SPACE_HEATER_PHASE_1; i++)
        loadVector.set(i, power);
      for (int i = VillageConstants.SPACE_HEATER_PHASE_1; i < VillageConstants.SPACE_HEATER_PHASE_2; i++)
        loadVector.set(i, loadVector.get(i - 1)
                          - VillageConstants.SPACE_HEATER_PHASE_LOAD);
      for (int i = VillageConstants.SPACE_HEATER_PHASE_2; i < VillageConstants.SPACE_HEATER_PHASE_3; i++)
        loadVector.set(i, loadVector.get(i - 1));
      for (int i = VillageConstants.SPACE_HEATER_PHASE_3; i < VillageConstants.SPACE_HEATER_PHASE_4; i++)
        loadVector.set(i, loadVector.get(i - 1) + 2
                          * VillageConstants.SPACE_HEATER_PHASE_LOAD);
      for (int i = VillageConstants.SPACE_HEATER_PHASE_4; i < VillageConstants.QUARTERS_OF_DAY; i++)
        loadVector.set(i, power);
      weeklyLoadVector.set(day, loadVector);
      weeklyOperation.set(day, dailyOperation);
      log.debug("Changed");
    }
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // In case the attenants are not in vacation, the spaceheater works all day
    if (applianceOf.isOnVacation(day)) {
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++) {
        possibilityDailyOperation.add(false);
      }
    }
    else {
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++) {
        possibilityDailyOperation.add(true);
      }
    }
    return possibilityDailyOperation;
  }

  // dead code
//  @Override
//  public void showStatus ()
//  {
//    super.showStatus();
//    log.debug("Percentage: " + operationPercentage);
//    log.debug("Temperature Threshold: " + temperatureThreshold);
//  }

  @Override
  public void refresh ()
  {
    fillWeeklyOperation();
    createWeeklyPossibilityOperationVector();
  }

}
