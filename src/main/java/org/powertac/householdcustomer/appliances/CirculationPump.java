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

import org.powertac.common.configurations.HouseholdConstants;

/**
 * Circulation Pump is the appliance that brings water to the household. It works most of the hours
 * of the day, but always when someone is at home in need of water. So it's a not shifting
 * appliance.
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class CirculationPump extends NotShiftingAppliance
{

  /**
   * Variable that presents the mean possibility to utilize the appliance each quarter of the day
   * that someone is present in the household.
   */
  double percentage;

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {

    // Filling the base variables
    name = household + " CirculationPump";
    saturation = Double.parseDouble(conf.getProperty("CirculationPumpSaturation"));
    percentage = Double.parseDouble(conf.getProperty("CirculationPumpPercentage"));

    consumptionShare = (float) (HouseholdConstants.PERCENTAGE * (HouseholdConstants.CIRCULATION_PUMP_CONSUMPTION_SHARE_VARIANCE * gen.nextGaussian() + HouseholdConstants.CIRCULATION_PUMP_CONSUMPTION_SHARE_MEAN));
    baseLoadShare = HouseholdConstants.PERCENTAGE * HouseholdConstants.CIRCULATION_PUMP_BASE_LOAD_SHARE;
    power = (int) (HouseholdConstants.CIRCULATION_PUMP_POWER_VARIANCE * gen.nextGaussian() + HouseholdConstants.CIRCULATION_PUMP_POWER_MEAN);
    cycleDuration = HouseholdConstants.CIRCULATION_PUMP_DURATION_CYCLE;
    od = false;
    inUse = false;
    probabilitySeason = fillSeason(HouseholdConstants.CIRCULATION_PUMP_POSSIBILITY_SEASON_1, HouseholdConstants.CIRCULATION_PUMP_POSSIBILITY_SEASON_2,
        HouseholdConstants.CIRCULATION_PUMP_POSSIBILITY_SEASON_3);
    probabilityWeekday = fillDay(HouseholdConstants.CIRCULATION_PUMP_POSSIBILITY_DAY_1, HouseholdConstants.CIRCULATION_PUMP_POSSIBILITY_DAY_2, HouseholdConstants.CIRCULATION_PUMP_POSSIBILITY_DAY_3);
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // The pump can work each quarter someone is in the premises
    for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++) {
      if (applianceOf.isEmpty(day, j) == false)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }

    return possibilityDailyOperation;
  }

  @Override
  public void fillDailyFunction (int weekday, Random gen)
  {

    // Initializing and Creating auxiliary variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    Vector<Boolean> v = new Vector<Boolean>();

    // For each quarter of a day
    for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
      if (applianceOf.isEmpty(weekday, i) == false && (gen.nextFloat() > percentage)) {
        loadVector.add(power);
        dailyOperation.add(true);
        v.add(true);
      } else {
        loadVector.add(0);
        dailyOperation.add(false);
        v.add(false);
      }
    }
    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
    operationVector.add(v);
  }

  @Override
  public void refresh (Random gen)
  {
    fillWeeklyFunction(gen);
    createWeeklyPossibilityOperationVector();
  }

}
