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
 * Circulation pump is the appliance that brings water to the household. It
 * works most of the hours of the day, but always when someone is at home in
 * need of water. So it's a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class AirCondition extends WeatherSensitiveAppliance
{

  /**
   * The type of the water heater. For more info, read the details in the
   * enumerations.HeaterType java file
   **/
  AirConditionType type;
  AirConditionClass acClass;
  double acClassEER;
  double acClassCOP;
  AirConditionPowerClass acPowerClass;
  AirConditionOperation acOperation;
  int cycleOn;
  int cycleOff;
  int BTU;
  int powerHeating;
  int powerCooling;
  int powerOffHeating;
  int powerOffCooling;
  int powerStartHeating;
  int powerStartCooling;
  int lowerLimit;
  int upperLimit;

  int cycleCounter;

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
    cycleCounter = 0;

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
    double classA = HouseholdConstants.PERCENTAGE * Double.parseDouble(conf.getProperty("AirConditionClassA"));
    double classB = HouseholdConstants.PERCENTAGE * Double.parseDouble(conf.getProperty("AirConditionClassB"));
    double classC = HouseholdConstants.PERCENTAGE * Double.parseDouble(conf.getProperty("AirConditionClassC"));
    double classD = HouseholdConstants.PERCENTAGE * Double.parseDouble(conf.getProperty("AirConditionClassD"));
    double classE = HouseholdConstants.PERCENTAGE * Double.parseDouble(conf.getProperty("AirConditionClassE"));
    double classF = HouseholdConstants.PERCENTAGE * Double.parseDouble(conf.getProperty("AirConditionClassF"));

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
    double powerA = HouseholdConstants.PERCENTAGE * Double.parseDouble(conf.getProperty("AirConditionPowerTypeSmall"));
    double powerB = HouseholdConstants.PERCENTAGE * Double.parseDouble(conf.getProperty("AirConditionPowerTypeMedium"));

    powerOffHeating = 0;
    powerOffCooling = 0;
    powerStartHeating = 0;
    powerStartCooling = 0;

    if (x < powerA) {
      acPowerClass = AirConditionPowerClass.Small;
      BTU = HouseholdConstants.AIR_CONDITION_BTU_SMALL;
      powerCooling = (int) (BTU / (acClassEER * HouseholdConstants.QUARTERS_OF_HOUR));
      powerHeating = (int) (BTU / (acClassCOP * HouseholdConstants.QUARTERS_OF_HOUR));

    } else if ((x >= powerA) & (x < powerA + powerB)) {

      acPowerClass = AirConditionPowerClass.Medium;
      BTU = HouseholdConstants.AIR_CONDITION_BTU_MEDIUM;
      powerCooling = (int) (BTU / acClassEER);
      powerHeating = (int) (BTU / acClassCOP);

    } else {

      BTU = HouseholdConstants.AIR_CONDITION_BTU_LARGE;
      acPowerClass = AirConditionPowerClass.Large;
      powerCooling = (int) (BTU / acClassEER);
      powerHeating = (int) (BTU / acClassCOP);

    }

    if (type == AirConditionType.Inverter) {

      powerStartHeating = (int) (powerHeating * (1 + HouseholdConstants.AIR_CONDITION_POWER_OVER_START_INVERTER));
      powerStartCooling = (int) (powerCooling * (1 + HouseholdConstants.AIR_CONDITION_POWER_OVER_START_INVERTER));
      powerOffHeating = (int) (powerHeating * (0.5 - HouseholdConstants.AIR_CONDITION_POWER_OVER_START_INVERTER));
      powerOffCooling = (int) (powerCooling * (0.5 - HouseholdConstants.AIR_CONDITION_POWER_OVER_START_INVERTER));
      powerHeating = (int) (powerHeating * (0.5 + HouseholdConstants.AIR_CONDITION_POWER_OVER_START_INVERTER));
      powerCooling = (int) (powerCooling * (0.5 + HouseholdConstants.AIR_CONDITION_POWER_OVER_START_INVERTER));

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
  public void weatherDailyFunction (int day, int hour, double temperature)
  {
    int trueCounter = 0;
    boolean open = true; // this is a variable to help separate cases of
                         // operation
    int start = 0, end = 0; // this variables are used to show the beginning or
                            // the end of the operation cycle
    boolean starting = true; // this shows if the air condition is starting or
                             // ending

    // See what mode will the Air Condition will be on
    if (temperature > upperLimit) {
      acOperation = AirConditionOperation.Cooling;
    } else if (temperature < lowerLimit) {
      acOperation = AirConditionOperation.Heating;
    } else
      acOperation = AirConditionOperation.Off;

    // If it is going to work because the temperature is out of limits
    if (acOperation != AirConditionOperation.Off) {

      boolean[] hourPresence = new boolean[HouseholdConstants.QUARTERS_OF_HOUR];

      for (int i = 0; i < HouseholdConstants.QUARTERS_OF_HOUR; i++) {
        hourPresence[i] = possibilityOperationVector.get(day).get(hour * HouseholdConstants.QUARTERS_OF_HOUR + i);
        if (hourPresence[i] == true)
          trueCounter++;
        // log.debug("Day:" + day + " Hour: " + hour + " Quarter: " + (hour *
        // HouseholdConstants.QUARTERS_OF_HOUR + i) + " Presence: " +
        // hourPresence[i] + " " + trueCounter);
      }

      if (trueCounter == HouseholdConstants.QUARTERS_OF_HOUR) {
        start = 0;
      } else if (trueCounter == HouseholdConstants.QUARTERS_OF_HOUR - 1) {
        if (hourPresence[0] == false) {
          start = 1;
        } else if (hourPresence[3] == false) {
          end = 3;
          starting = false;
        } else {
          start = 0;
        }
      } else if (trueCounter == HouseholdConstants.QUARTERS_OF_HOUR - 2) {
        if ((hourPresence[0] == false) && (hourPresence[1] == false)) {
          start = 2;
        } else if ((hourPresence[2] == false) && (hourPresence[3] == false)) {
          end = 2;
          starting = false;
        } else
          open = false;
      } else
        open = false;

      /*
            log.debug("Open:" + open + "  ");
            if (starting)
              log.debug("Start time:" + start);
            else
              log.debug("End time:" + end);
      */

      if (open) {
        loadVector = weeklyLoadVector.get(day);
        dailyOperation = weeklyOperation.get(day);

        if (type == AirConditionType.Normal) { // Normal type of air condition

          // If this is the start of the operation of air condition
          if (starting) {

            for (int i = start; i < HouseholdConstants.QUARTERS_OF_HOUR; i++) {
              int time = cycleCounter % cycleDuration;
              // log.debug("CycleCounter: " + cycleCounter);
              // log.debug("CycleDuration: " + cycleDuration);
              // log.debug("Time: " + time);

              if (time < cycleOn) {
                if (acOperation == AirConditionOperation.Cooling)
                  loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerCooling);
                else
                  loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerHeating);

                dailyOperation.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, true);
              }
              cycleCounter++;
            }

            // log.debug("Changed");
          } else {
            for (int i = 0; i < end; i++) {
              int time = cycleCounter % cycleDuration;
              // log.debug("CycleCounter: " + cycleCounter);
              // log.debug("CycleDuration: " + cycleDuration);
              // log.debug("Time: " + time);

              if (time < cycleOn) {
                if (acOperation == AirConditionOperation.Cooling)
                  loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerCooling);
                else
                  loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerHeating);

                dailyOperation.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, true);
              }
              cycleCounter++;
            }
            cycleCounter = 0;

            // log.debug("Changed");
          }
        } else { // Inverter Case of Air Conditioning

          // If this is the start of the operation of air condition
          if (starting) {

            for (int i = start; i < HouseholdConstants.QUARTERS_OF_HOUR; i++) {
              int time = cycleCounter % cycleDuration;
              // log.debug("CycleCounter: " + cycleCounter);
              // log.debug("CycleDuration: " + cycleDuration);
              // log.debug("Time: " + time);

              if (time < cycleOn) {
                if (acOperation == AirConditionOperation.Cooling) {
                  if (cycleCounter == 0) {
                    loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerStartCooling);
                  } else {
                    loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerCooling);
                  }
                } else {
                  if (cycleCounter == 0) {
                    loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerStartHeating);
                  } else {
                    loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerHeating);
                  }
                }

              } else {
                if (acOperation == AirConditionOperation.Cooling)
                  loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerOffCooling);
                else
                  loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerOffHeating);
              }

              dailyOperation.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, true);
              cycleCounter++;
            }

            // log.debug("Changed");
          } else {
            for (int i = 0; i < end; i++) {
              int time = cycleCounter % cycleDuration;
              // log.debug("CycleCounter: " + cycleCounter);
              // log.debug("CycleDuration: " + cycleDuration);
              // log.debug("Time: " + time);

              if (time < cycleOn) {
                if (acOperation == AirConditionOperation.Cooling)
                  loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerCooling);
                else
                  loadVector.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, powerHeating);

                dailyOperation.set(hour * HouseholdConstants.QUARTERS_OF_HOUR + i, true);
              }
              cycleCounter++;
            }
            cycleCounter = 0;

            // log.debug("Changed");
          }

        }

        weeklyLoadVector.set(day, loadVector);
        weeklyOperation.set(day, dailyOperation);
        // log.debug("Changed");

      } else
        cycleCounter = 0;

    } else {
      cycleCounter = 0;
    }

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
    log.debug("Name = " + name);
    log.debug("Saturation = " + saturation);
    log.debug("Occupancy Dependence = " + od);
    log.debug("Air Condition Type = " + type);
    log.debug("Air Condition Power Class = " + acPowerClass);
    log.debug("Air Condition BTU = " + BTU);
    log.debug("Air Condition Class = " + acClass);
    log.debug("Air Condition Class EER = " + acClassEER);
    log.debug("Air Condition Class COP = " + acClassCOP);
    log.debug("Air Condition Operation = " + acOperation);

    log.debug("Power Start Heating = " + powerStartHeating);
    log.debug("Power Start Cooling = " + powerStartCooling);
    log.debug("Power Heating = " + powerHeating);
    log.debug("Power Cooling = " + powerCooling);
    log.debug("Power Off Heating = " + powerOffHeating);
    log.debug("Power Off Cooling = " + powerOffCooling);

    log.debug("Cycle Duration On = " + cycleOn);
    log.debug("Cycle Duration Off = " + cycleOff);
    log.debug("Cycle Duration = " + cycleDuration);

    log.debug("Lower Limit = " + lowerLimit);
    log.debug("Upper Limit = " + upperLimit);

    // Printing Weekly Operation Vector and Load Vector
    log.debug("Weekly Operation Vector and Load = ");

    for (int i = 0; i < HouseholdConstants.DAYS_OF_COMPETITION; i++) {
      log.debug("Day " + i);
      ListIterator<Boolean> iter3 = weeklyOperation.get(i).listIterator();
      ListIterator<Integer> iter4 = weeklyLoadVector.get(i).listIterator();
      for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++)
        log.debug("Quarter " + j + " = " + iter3.next() + "   Load = " + iter4.next());
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
