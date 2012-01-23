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

import java.util.ListIterator;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.powertac.common.configurations.HouseholdConstants;
import org.powertac.common.enumerations.AirConditionClass;
import org.powertac.common.enumerations.AirConditionOperation;
import org.powertac.common.enumerations.AirConditionPowerClass;
import org.powertac.common.enumerations.AirConditionType;

/**
 * Circulation pump is the appliance that brings water to the household. It works most of the hours of the day, but always when someone is at home in need of water. So it's a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class AirCondition extends NotShiftingAppliance
{

  /**
   * The type of the water heater. For more info, read the details in the enumerations.HeaterType java file
   **/
  AirConditionType type;
  AirConditionClass acClass;
  double acClassEER;
  double acClassCOP;
  AirConditionPowerClass acPowerClass;
  AirConditionOperation acOperation;
  int cycleOn;
  int cycleOff;
  int powerOff;
  int powerStart;
  int lowerLimit;
  int upperLimit;

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Creating Auxiliary Variables
    int x = gen.nextInt(HouseholdConstants.PERCENTAGE + 1);
    double limit = HouseholdConstants.PERCENTAGE * Double.parseDouble(conf.getProperty("AirConditionTypeNormal"));
    // Filling the base variables
    name = household + " AirCondition";
    saturation = Double.parseDouble(conf.getProperty("AirConditionSaturation"));
    od = true;

    // Air Condition Specific Variables
    acOperation = AirConditionOperation.Off;

    if (x < limit) {
      type = AirConditionType.Normal;
      cycleOn = HouseholdConstants.AIR_CONDITION_DURATION_CYCLE_ON_NORMAL;
      cycleOff = HouseholdConstants.AIR_CONDITION_DURATION_CYCLE_OFF_NORMAL;
      cycleDuration = cycleOn + cycleOff;

    } else {
      type = AirConditionType.Inverter;
      cycleOn = HouseholdConstants.AIR_CONDITION_DURATION_CYCLE_INVERTER;
      cycleOff = HouseholdConstants.AIR_CONDITION_DURATION_CYCLE_INVERTER;
      cycleDuration = cycleOn + cycleOff;
    }

    x = gen.nextInt(HouseholdConstants.PERCENTAGE + 1);
    double classA = Double.parseDouble(conf.getProperty("AirConditionClassA"));
    double classB = Double.parseDouble(conf.getProperty("AirConditionClassB"));
    double classC = Double.parseDouble(conf.getProperty("AirConditionClassC"));
    double classD = Double.parseDouble(conf.getProperty("AirConditionClassD"));
    double classE = Double.parseDouble(conf.getProperty("AirConditionClassE"));
    double classF = Double.parseDouble(conf.getProperty("AirConditionClassF"));

    if (x < classA) {
      acClass = AirConditionClass.A;
      acClassEER = HouseholdConstants.AIR_CONDITION_CLASS_A_EER;
      acClassCOP = HouseholdConstants.AIR_CONDITION_CLASS_A_COP;
    } else {
      if (x >= classA & x < (classA + classB)) {
        acClass = AirConditionClass.B;
        acClassEER = HouseholdConstants.AIR_CONDITION_CLASS_B_EER;
        acClassCOP = HouseholdConstants.AIR_CONDITION_CLASS_B_COP;
      } else {
        if (x >= (classA + classB) & x < (classA + classB + classC)) {
          acClass = AirConditionClass.C;
          acClassEER = HouseholdConstants.AIR_CONDITION_CLASS_C_EER;
          acClassCOP = HouseholdConstants.AIR_CONDITION_CLASS_C_COP;
        } else {
          if (x >= (classA + classB + classC) & x < (classA + classB + classC + classD)) {
            acClass = AirConditionClass.D;
            acClassEER = HouseholdConstants.AIR_CONDITION_CLASS_D_EER;
            acClassCOP = HouseholdConstants.AIR_CONDITION_CLASS_D_COP;
          } else {
            if (x >= (classA + classB + classC + classD) & x < (classA + classB + classC + classD + classE)) {
              acClass = AirConditionClass.E;
              acClassEER = HouseholdConstants.AIR_CONDITION_CLASS_E_EER;
              acClassCOP = HouseholdConstants.AIR_CONDITION_CLASS_E_COP;
            } else {
              if (x >= (classA + classB + classC + classD + classE) & x < (classA + classB + classC + classD + classE + classF)) {
                acClass = AirConditionClass.F;
                acClassEER = HouseholdConstants.AIR_CONDITION_CLASS_F_EER;
                acClassCOP = HouseholdConstants.AIR_CONDITION_CLASS_F_COP;
              } else {
                acClass = AirConditionClass.G;
                acClassEER = HouseholdConstants.AIR_CONDITION_CLASS_G_EER;
                acClassCOP = HouseholdConstants.AIR_CONDITION_CLASS_G_COP;
              }
            }
          }
        }
      }
    }

    x = gen.nextInt(HouseholdConstants.PERCENTAGE + 1);
    double powerA = Double.parseDouble(conf.getProperty("AirConditionPowerTypeSmall"));
    double powerB = Double.parseDouble(conf.getProperty("AirConditionPowerTypeMedium"));

    powerOff = 0;
    powerStart = 0;

    if (x < powerA) {
      acPowerClass = AirConditionPowerClass.Small;
      power = HouseholdConstants.AIR_CONDITION_POWER_MEAN_SMALL_NORMAL;

    } else if ((x >= powerA) & (x < powerA + powerB)) {

      acPowerClass = AirConditionPowerClass.Medium;
      power = HouseholdConstants.AIR_CONDITION_POWER_MEAN_MEDIUM_NORMAL;

    } else {

      power = HouseholdConstants.AIR_CONDITION_POWER_MEAN_LARGE_NORMAL;
      acPowerClass = AirConditionPowerClass.Large;

    }

    if (type == AirConditionType.Inverter) {

      powerStart = (int) (power * (1 + HouseholdConstants.AIR_CONDITION_POWER_OVER_START_INVERTER));
      power = (int) (power * (0.5 + HouseholdConstants.AIR_CONDITION_POWER_OVER_START_INVERTER));
      powerOff = (int) (power * (0.5 - HouseholdConstants.AIR_CONDITION_POWER_OVER_START_INVERTER));

    }

    lowerLimit = (int) (HouseholdConstants.AIR_CONDITION_LOW_LIMIT_VARIANCE * gen.nextGaussian() + HouseholdConstants.AIR_CONDITION_LOW_LIMIT_MEAN);
    upperLimit = (int) (HouseholdConstants.AIR_CONDITION_UPPER_LIMIT_VARIANCE * gen.nextGaussian() + HouseholdConstants.AIR_CONDITION_UPPER_LIMIT_MEAN);

  }

  @Override
  public void fillDailyFunction (int weekday, Random gen)
  {
    // Initializing And Creating Auxiliary Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    Vector<Boolean> operation = new Vector<Boolean>();

    for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
      operation.add(false);
      dailyOperation.add(false);
      loadVector.add(0);
    }

    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
    operationVector.add(operation);

  }

  @Override
  public void weatherDailyFunction (int day, int hour, double temp)
  {
    /*
        System.out.println(this.toString() + " " + (applianceOf.isOnVacation(day)) + " " + (temp > temperatureThreshold) + " " + (perc > percentage));

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
    */
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // It can operate each quarter someone is at home to turn it on
    for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++) {
      if (applianceOf.isEmpty(day, j) == false)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }

    return possibilityDailyOperation;
  }

  @Override
  public void showStatus ()
  {
    // Printing basic variables
    log.info("Name = " + name);
    log.info("Saturation = " + saturation);
    log.info("Occupancy Dependence = " + od);
    log.info("Air Condition Type = " + type);
    log.info("Air Condition Power Class = " + acPowerClass);
    log.info("Air Condition Class = " + acClass);
    log.info("Air Condition Class EER = " + acClassEER);
    log.info("Air Condition Class COP = " + acClassCOP);
    log.info("Air Condition Operation = " + acOperation);

    log.info("Power Start = " + powerStart);
    log.info("Power On = " + power);
    log.info("Power Off = " + powerOff);

    log.info("Cycle Duration On = " + cycleOn);
    log.info("Cycle Duration Off = " + cycleOff);
    log.info("Cycle Duration = " + cycleDuration);

    log.info("Lower Limit = " + lowerLimit);
    log.info("Upper Limit = " + upperLimit);

    // Printing Weekly Operation Vector and Load Vector
    log.info("Weekly Operation Vector and Load = ");

    for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION; i++) {
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
    // case the Water Heater is Instant
    fillWeeklyFunction(gen);
    createWeeklyPossibilityOperationVector();
  }

}
