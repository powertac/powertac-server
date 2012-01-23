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

import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.configurations.HouseholdConstants;

/**
 * Spaceheater is a electric appliance utilized to keep the rooms of a household warm when needed. These devices can work automatically in order to save as much energy as possible, knowing when the
 * room must be warm. So this is a fully shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class SpaceHeater extends WeatherSensitiveAppliance
{

  /**
   * Variable that presents the mean possibility to utilize the appliance each hour of the day that someone is present in the housesold.
   */
  double percentage;

  /**
   * Variable that presents the temperature that is inconvenient enough for the inhabitants of the house in the start of the day in order to open the appliance.
   */
  int temperatureThreshold;

  /**
   * Variable utilized as a random number generator
   */
  Random generator;

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Filling the base variables
    name = household + " SpaceHeater";
    saturation = Double.parseDouble(conf.getProperty("SpaceHeaterSaturation"));
    percentage = Double.parseDouble(conf.getProperty("SpaceHeaterPercentage"));
    temperatureThreshold = (int) (HouseholdConstants.SPACE_HEATER_TEMPERATURE_VARIANCE * gen.nextGaussian() + HouseholdConstants.SPACE_HEATER_TEMPERATURE_MEAN);
    power = (int) (HouseholdConstants.SPACE_HEATER_POWER_VARIANCE * gen.nextGaussian() + HouseholdConstants.SPACE_HEATER_POWER_MEAN);
    cycleDuration = HouseholdConstants.SPACE_HEATER_DURATION_CYCLE;
    od = false;
    generator = gen;
  }

  @Override
  public void fillDailyFunction (int weekday, Random gen)
  {
    // Initializing Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
      loadVector.add(0);
      dailyOperation.add(false);
    }
    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
    operationVector.add(dailyOperation);

  }

  @Override
  public void weatherDailyFunction (int day, int hour, double temp)
  {

    double perc = generator.nextDouble();

    // System.out.println(this.toString() + " " + (applianceOf.isOnVacation(day)) + " " + (temp > temperatureThreshold) + " " + (perc > percentage));

    if ((applianceOf.isOnVacation(day)) || (temp > temperatureThreshold) || (perc > percentage)) {

    } else {
      for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
        loadVector.add(0);
        dailyOperation.add(true);
      }
      for (int i = 0; i < HouseholdConstants.SPACE_HEATER_PHASE_1; i++)
        loadVector.set(i, power);
      for (int i = HouseholdConstants.SPACE_HEATER_PHASE_1; i < HouseholdConstants.SPACE_HEATER_PHASE_2; i++)
        loadVector.set(i, loadVector.get(i - 1) - HouseholdConstants.SPACE_HEATER_PHASE_LOAD);
      for (int i = HouseholdConstants.SPACE_HEATER_PHASE_2; i < HouseholdConstants.SPACE_HEATER_PHASE_3; i++)
        loadVector.set(i, loadVector.get(i - 1));
      for (int i = HouseholdConstants.SPACE_HEATER_PHASE_3; i < HouseholdConstants.SPACE_HEATER_PHASE_4; i++)
        loadVector.set(i, loadVector.get(i - 1) + 2 * HouseholdConstants.SPACE_HEATER_PHASE_LOAD);
      for (int i = HouseholdConstants.SPACE_HEATER_PHASE_4; i < HouseholdConstants.QUARTERS_OF_DAY; i++)
        loadVector.set(i, power);
      weeklyLoadVector.set(day, loadVector);
      weeklyOperation.set(day, dailyOperation);
      operationVector.set(day, dailyOperation);
      log.debug("Changed");
    }
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // In case the attenants are not in vacation, the spaceheater works all day
    if (applianceOf.isOnVacation(day)) {
      for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++) {
        possibilityDailyOperation.add(false);
      }
    } else {
      for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++) {
        possibilityDailyOperation.add(true);
      }
    }
    return possibilityDailyOperation;
  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {
    long[] newControllableLoad = new long[HouseholdConstants.HOURS_OF_DAY];

    // In this case the daily shifting is useless because it works all day
    for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
      for (int j = 0; j < HouseholdConstants.QUARTERS_OF_HOUR; j++)
        newControllableLoad[i] += weeklyLoadVector.get(day).get(i * HouseholdConstants.QUARTERS_OF_HOUR + j);
    }
    return newControllableLoad;
  }

  @Override
  public void showStatus ()
  {

    super.showStatus();
    log.info("Percentage: " + percentage);
    log.info("Temperature Threshold: " + temperatureThreshold);
  }

  @Override
  public void refresh (Random gen)
  {
    fillWeeklyFunction(gen);
    createWeeklyPossibilityOperationVector();
  }

}
