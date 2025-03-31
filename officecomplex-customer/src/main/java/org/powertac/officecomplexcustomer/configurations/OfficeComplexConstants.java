/*
 * Copyright 2009-2012 the original author or authors. Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.officecomplexcustomer.configurations;

import java.time.Instant;
import java.time.ZoneOffset;

import org.powertac.common.Competition;

/**
 * This class contains constant definitions used throughout the office complex
 * models.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class OfficeComplexConstants
{

  // GENERAL VARIABLES -----------------------------//
  public static final int PERCENTAGE = 100;
  public static final int THOUSAND = 1000;
  public static final double SAME = 0.60;
  public static final double OPERATION_PARTITION = 0.02;
  public static final double DISTRUST_FACTOR = 2;
  public static final double TOU_FACTOR = 0.05;
  public static final double INTERRUPTIBILITY_FACTOR = 0.5;
  public static final double VARIABLE_PRICING_FACTOR = 0.7;
  public static final double TIERED_RATE_FACTOR = 0.1;
  public static final double TARIFF_SWITCH_FACTOR = 0.1;
  public static final double BROKER_SWITCH_FACTOR = 0.02;
  public static final double RATIONALITY_FACTOR = 0.9;
  public static final int TARIFF_COUNT = 5;
  public static final double WEIGHT_INCONVENIENCE = 1;
  public static final int MIN_DEFAULT_DURATION = 1;
  public static final int MAX_DEFAULT_DURATION = 3;

  // TIME VARIABLES -----------------------------//
  public static int DAYS_OF_COMPETITION;
  public static int WEEKS_OF_COMPETITION;
  public static final int DAYS_OF_BOOTSTRAP = 14;
  public static final int WEEKS_OF_BOOTSTRAP = 2;
  public static final int DAYS_OF_WEEK = 7;
  public static final int WEEKDAYS = 5;
  public static final int QUARTERS_OF_HOUR = 4;
  public static final int QUARTERS_OF_DAY = 96;
  public static final int HOURS_OF_DAY = 24;
  public static final int MID_DAY_QUARTER = 48;
  public static final int LUMINANCE_VARIANCE = 8;

  // OFFICE COMPLEX TYPES VARIABLES ---------------------//
  public static final int NOT_SHIFTING_TYPE = 0;
  public static final int SMART_SHIFTING_TYPE = 1;

  // WORK VARIABLES -----------------------------//
  public static final int PERSONS = 5;
  public static final int START_OF_WORK_MEAN = 32;
  public static final int START_OF_WORK_VARIANCE = 2;

  public static final int START_OF_BREAK_MEAN = 52;
  public static final int START_OF_BREAK_VARIANCE = 4;

  public static final int START_OF_LAUNCH_BREAK = 52;
  public static final int START_OF_LAUNCH_BREAK_HOUR = 13;
  public static final int END_OF_LAUNCH_BREAK = 60;
  public static final int END_OF_LAUNCH_BREAK_HOUR = 15;

  public static int MONDAY = 1;
  public static int TUESDAY = 2;
  public static int WEDNESDAY = 3;
  public static int THURSDAY = 4;
  public static int FRIDAY = 5;
  public static int SATURDAY = 6;
  public static int SUNDAY = 0;

  public static final int ONE_WORKING_DAY = 1;
  public static final int TWO_WORKING_DAYS = 2;
  public static final int THREE_WORKING_DAYS = 3;
  public static final int FOUR_WORKING_DAYS = 4;
  public static final int FIVE_WORKING_DAYS = 5;
  public static final int SIX_WORKING_DAYS = 6;
  public static final int SEVEN_WORKING_DAYS = 7;

  // GENERAL APPLIANCES VARIABLES -----------------------------//
  public static final int START_OF_FUNCTION = 32;
  public static final int START_OF_FUNCTION_HOURS = 8;
  public static final int END_OF_FUNCTION = 84;
  public static final int END_OF_FUNCTION_HOUR = 21;

  // CONSUMER ELECTRONICS VARIABLES -----------------------------//
  public static final int CONSUMER_ELECTRONICS_POWER_MEAN = 100;
  public static final int CONSUMER_ELECTRONICS_POWER_VARIANCE = 17;
  public static final int CONSUMER_ELECTRONICS_DURATION_CYCLE = 1;

  // ICT VARIABLES -----------------------------//
  public static final int ICT_POWER_MEAN = 150;
  public static final int ICT_POWER_VARIANCE = 25;
  public static final int ICT_DURATION_CYCLE = 1;

  // LIGHTS VARIABLES -----------------------------//
  public static final int LIGHTS_POWER_MEAN = 50;
  public static final int LIGHTS_POWER_VARIANCE = 5;
  public static final int LIGHTS_DURATION_CYCLE = 1;

  // AIR CONDITION VARIABLES -----------------------------//
  public static final int AIR_CONDITION_BTU_SMALL = 9000;
  public static final int AIR_CONDITION_BTU_MEDIUM = 18000;
  public static final int AIR_CONDITION_BTU_LARGE = 25000;
  public static final double AIR_CONDITION_COP_EER_RATIO = 3.412;

  public static final double AIR_CONDITION_POWER_OVER_START_INVERTER = 0.09;
  public static final double AIR_CONDITION_POWER_MEAN_INVERTER = 0.5;

  public static final double AIR_CONDITION_CLASS_A_EER = 4;
  public static final double AIR_CONDITION_CLASS_A_COP = 6;
  public static final double AIR_CONDITION_CLASS_B_EER = 3.1;
  public static final double AIR_CONDITION_CLASS_B_COP = 3.5;
  public static final double AIR_CONDITION_CLASS_C_EER = 2.9;
  public static final double AIR_CONDITION_CLASS_C_COP = 3.3;
  public static final double AIR_CONDITION_CLASS_D_EER = 2.7;
  public static final double AIR_CONDITION_CLASS_D_COP = 3.0;
  public static final double AIR_CONDITION_CLASS_E_EER = 2.5;
  public static final double AIR_CONDITION_CLASS_E_COP = 2.7;
  public static final double AIR_CONDITION_CLASS_F_EER = 2.3;
  public static final double AIR_CONDITION_CLASS_F_COP = 2.5;
  public static final double AIR_CONDITION_CLASS_G_EER = 2;
  public static final double AIR_CONDITION_CLASS_G_COP = 2.2;

  public static final int AIR_CONDITION_DURATION_CYCLE_ON_NORMAL = 3;
  public static final int AIR_CONDITION_DURATION_CYCLE_OFF_NORMAL = 2;
  public static final int AIR_CONDITION_DURATION_CYCLE_INVERTER = 1;

  public static final int AIR_CONDITION_LOW_LIMIT_MEAN = 14;
  public static final int AIR_CONDITION_LOW_LIMIT_VARIANCE = 2;
  public static final int AIR_CONDITION_UPPER_LIMIT_MEAN = 28;
  public static final int AIR_CONDITION_UPPER_LIMIT_VARIANCE = 2;

  // STOVE VARIABLES -----------------------------//
  public static final int MICROWAVE_OVEN_POWER_MEAN = 800;
  public static final int MICROWAVE_OVEN_POWER_VARIANCE = 307;
  public static final int MICROWAVE_OVEN_DURATION_CYCLE = 1;
  public static final int MICROWAVE_OVEN_OPERATION_PERCENTAGE = 1;

  // REFRIGERATOR VARIABLES -----------------------------//
  public static final int REFRIGERATOR_POWER_MEAN = 140;
  public static final int REFRIGERATOR_POWER_VARIANCE = 23;
  public static final int REFRIGERATOR_DURATION_CYCLE = 2;
  public static final int REFRIGERATOR_SHIFTING_INTERVAL = 2;
  public static final int REFRIGERATOR_SHIFTING_PERIODS = 12;

  // COFFEE MACHINE VARIABLES -----------------------------//
  public static final int COFFEE_MACHINE_POWER_MEAN = 100;
  public static final int COFFEE_MACHINE_POWER_VARIANCE = 23;
  public static final int COFFEE_MACHINE_DURATION_CYCLE = 4;
  public static final int COFFEE_MACHINE_START_OPERATION = 32;
  public static final int COFFEE_MACHINE_STOP_OPERATION = 72;
  public static final int COFFEE_MACHINE_SHIFTING_INTERVAL = 2;
  public static final int COFFEE_MACHINE_SHIFTING_PERIODS = 5;

  // VENDING MACHINE VARIABLES -----------------------------//
  public static final int VENDING_MACHINE_POWER_MEAN = 240;
  public static final int VENDING_MACHINE_POWER_VARIANCE = 40;
  public static final int VENDING_MACHINE_DURATION_CYCLE = 4;
  public static final int VENDING_MACHINE_SHIFTING_INTERVAL = 2;
  public static final int VENDING_MACHINE_SHIFTING_PERIODS = 12;

  // COPY MACHINE VARIABLES -----------------------------//
  public static final int COPY_MACHINE_POWER_MEAN = 350;
  public static final int COPY_MACHINE_POWER_VARIANCE = 54;
  public static final int COPY_MACHINE_STANDBY_POWER_MEAN = 74;
  public static final int COPY_MACHINE_STANDBY_POWER_VARIANCE = 14;
  public static final int COPY_MACHINE_DURATION_CYCLE = 1;

  // SERVERS VARIABLES -----------------------------//
  public static final int SERVERS_POWER_MEAN = 450;
  public static final int SERVERS_POWER_VARIANCE = 50;
  public static final int SERVERS_SLEEP_POWER_MEAN = 100;
  public static final int SERVERS_SLEEP_POWER_VARIANCE = 15;
  public static final int SERVERS_DURATION_CYCLE = 1;

  // COMPUTERS VARIABLES -----------------------------//
  public static final int COMPUTERS_POWER_MEAN = 150;
  public static final int COMPUTERS_POWER_VARIANCE = 30;
  public static final int COMPUTERS_DURATION_CYCLE = 1;

  // COST ESTIMATION VARIABLES ------------------------//
  public static final int RANDOM_DAYS_NUMBER = 3;

  /**
   * This is the function utilized in order to estimate the days of Competition
   * for precomputing the model's behavior
   **/
  public static void setDaysOfCompetition (int days)
  {
    WEEKS_OF_COMPETITION =
      (int) (Math.ceil(((float) days) / ((float) DAYS_OF_WEEK)));
    DAYS_OF_COMPETITION = WEEKS_OF_COMPETITION * DAYS_OF_WEEK;

    // System.out.println("Days:" + DAYS_OF_COMPETITION + " Weeks:" +
    // WEEKS_OF_COMPETITION);
  }

  @SuppressWarnings("deprecation")
  public static void setDaysOfWeek ()
  {
    Instant base = Competition.currentCompetition().getSimulationBaseTime();

    int bias = Math.abs(base.atZone(ZoneOffset.UTC).getDayOfWeek().getValue() - DAYS_OF_WEEK) % DAYS_OF_WEEK;

    MONDAY = (MONDAY + bias) % DAYS_OF_WEEK;
    TUESDAY = (TUESDAY + bias) % DAYS_OF_WEEK;
    WEDNESDAY = (WEDNESDAY + bias) % DAYS_OF_WEEK;
    THURSDAY = (THURSDAY + bias) % DAYS_OF_WEEK;
    FRIDAY = (FRIDAY + bias) % DAYS_OF_WEEK;
    SATURDAY = (SATURDAY + bias) % DAYS_OF_WEEK;
    SUNDAY = (SUNDAY + bias) % DAYS_OF_WEEK;

  }
}
