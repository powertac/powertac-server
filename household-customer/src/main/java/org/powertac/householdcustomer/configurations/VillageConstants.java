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

package org.powertac.householdcustomer.configurations;

import java.time.Instant;
import java.time.ZoneOffset;
import org.powertac.common.Competition;

/**
 * This class contains constant definitions used throughout the household
 * models.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class VillageConstants
{

  // GENERAL VARIABLES -----------------------------//
  public static final int PERCENTAGE = 100;
  public static final int THOUSAND = 1000;
  public static final double SAME = 0.60;
  public static final double OPERATION_PARTITION = 0.05;
  public static final double OPERATION_DAILY_TIMES_LIMIT = 2;
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
  public static final int LUMINANCE_VARIANCE = 16;
  public static final int LUMINANCE_FACTOR = 40;

  // VILLAGE TYPES VARIABLES ---------------------//
  public static final int NOT_SHIFTING_TYPE = 0;
  public static final int RANDOM_SHIFTING_TYPE = 1;
  public static final int REGULAR_SHIFTING_TYPE = 2;
  public static final int SMART_SHIFTING_TYPE = 3;

  // PERSON VARIABLES ----------------------------//
  public static final int ONE_PERSON = 1;
  public static final int TWO_PERSONS = 2;
  public static final int THREE_PERSONS = 3;
  public static final int FOUR_PERSONS = 4;
  public static final int FIVE_PERSONS = 5;

  // SLEEP VARIABLES -----------------------------//
  public static final int START_OF_SLEEPING_1 = 0;
  public static final int START_OF_SLEEPING_2 = 91;
  public static final int END_OF_SLEEPING_1 = 25;
  public static final int END_OF_SLEEPING_2 = 96;

  // LEISURE VARIABLES -----------------------------//
  public static final int START_OF_LEISURE = 28;
  public static final int LEISURE_WINDOW = 46;
  public static final int LEISURE_END_WINDOW = 75;
  public static final int LEISURE_WINDOW_SHIFT = 80;

  // WORK VARIABLES -----------------------------//
  public static final int START_OF_WORK = 28;
  public static final int NUMBER_OF_SHIFTS = 3;
  public static final int HOURS_OF_SHIFT_WORK = 8;
  public static final int SHIFT_START_1 = 0;
  public static final int SHIFT_START_2 = 33;
  public static final int SHIFT_START_3 = 65;
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
  public static final int END_OF_FUNCTION = 84;
  public static final int END_OF_FUNCTION_HOUR = 21;

  // CIRCULATION PUMP VARIABLES -----------------------------//
  public static final int CIRCULATION_PUMP_POWER_MEAN = 90;
  public static final int CIRCULATION_PUMP_POWER_VARIANCE = 15;
  public static final int CIRCULATION_PUMP_DURATION_CYCLE = 1;

  // CONSUMER ELECTRONICS VARIABLES -----------------------------//
  public static final int CONSUMER_ELECTRONICS_POWER_MEAN = 100;
  public static final int CONSUMER_ELECTRONICS_POWER_VARIANCE = 17;
  public static final int CONSUMER_ELECTRONICS_DURATION_CYCLE = 1;

  // ICT VARIABLES -----------------------------//
  public static final int ICT_POWER_MEAN = 150;
  public static final int ICT_POWER_VARIANCE = 25;
  public static final int ICT_DURATION_CYCLE = 1;

  // LIGHTS VARIABLES -----------------------------//
  public static final int LIGHTS_POWER_MEAN = 350;
  public static final int LIGHTS_POWER_VARIANCE = 58;
  public static final int LIGHTS_DURATION_CYCLE = 1;

  // OTHERS VARIABLES -----------------------------//
  public static final int OTHERS_POWER_MEAN = 500;
  public static final int OTHERS_POWER_VARIANCE = 83;
  public static final int OTHERS_DURATION_CYCLE = 1;

  // DISHWASHER VARIABLES -----------------------------//
  public static final int DISHWASHER_POWER_MEAN = 530;
  public static final int DISHWASHER_POWER_VARIANCE = 88;
  public static final int DISHWASHER_DURATION_CYCLE = 8;

  // DRYER VARIABLES -----------------------------//
  public static final int DRYER_POWER_MEAN = 1410;
  public static final int DRYER_POWER_VARIANCE = 235;
  public static final int DRYER_DURATION_CYCLE = 7;

  public static final int DRYER_SECOND_PHASE = 3;
  public static final int DRYER_THIRD_PHASE = 6;
  public static final int DRYER_THIRD_PHASE_LOAD = 250;

  // AIR CONDITION VARIABLES -----------------------------//
  public static final int AIR_CONDITION_BTU_SMALL = 9000;
  public static final int AIR_CONDITION_BTU_MEDIUM = 12000;
  public static final int AIR_CONDITION_BTU_LARGE = 18000;
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

  // STORAGE WATER HEATER VARIABLES -----------------------------//
  public static final int STORAGE_HEATER_POWER_MEAN = 3000;
  public static final int STORAGE_HEATER_POWER_VARIANCE = 500;
  public static final int STORAGE_HEATER_DURATION_CYCLE = 8;
  public static final int STORAGE_HEATER_START = 20;
  public static final int STORAGE_HEATER_PHASES = 4;
  public static final int STORAGE_HEATER_PHASE_LOAD = 20;
  public static final int STORAGE_HEATER_SHIFTING_END = 17;
  public static final double STORAGE_HEATER_POSSIBILITY = 0.8;

  // INSTANT WATER HEATER VARIABLES -----------------------------//
  public static final int INSTANT_HEATER_POWER_MEAN = 12000;
  public static final int INSTANT_HEATER_POWER_VARIANCE = 2000;
  public static final int INSTANT_HEATER_DURATION_CYCLE = 1;

  // WASHING MACHINE VARIABLES -----------------------------//
  public static final int WASHING_MACHINE_POWER_MEAN = 100;
  public static final int WASHING_MACHINE_POWER_VARIANCE = 600;
  public static final int WASHING_MACHINE_DURATION_CYCLE = 8;

  // STOVE VARIABLES -----------------------------//
  public static final int STOVE_POWER_MEAN = 1840;
  public static final int STOVE_POWER_VARIANCE = 307;
  public static final int STOVE_DURATION_CYCLE = 2;

  // SPACE HEATER VARIABLES -----------------------------//
  public static final int SPACE_HEATER_POWER_MEAN = 7000;
  public static final int SPACE_HEATER_POWER_VARIANCE = 300;
  public static final int SPACE_HEATER_DURATION_CYCLE = 14;

  public static final int SPACE_HEATER_TEMPERATURE_MEAN = 13;
  public static final int SPACE_HEATER_TEMPERATURE_VARIANCE = 3;

  public static final int SPACE_HEATER_PHASE_1 = 9;
  public static final int SPACE_HEATER_PHASE_2 = 16;
  public static final int SPACE_HEATER_PHASE_3 = 86;
  public static final int SPACE_HEATER_PHASE_4 = 90;
  public static final int SPACE_HEATER_PHASE_LOAD = 750;

  // REFRIGERATOR VARIABLES -----------------------------//
  public static final int REFRIGERATOR_POWER_MEAN = 140;
  public static final int REFRIGERATOR_POWER_VARIANCE = 23;
  public static final int REFRIGERATOR_DURATION_CYCLE = 2;
  public static final int REFRIGERATOR_SHIFTING_INTERVAL = 2;
  public static final int REFRIGERATOR_SHIFTING_PERIODS = 12;

  // FREEZER VARIABLES -----------------------------//
  public static final int FREEZER_POWER_MEAN = 106;
  public static final int FREEZER_POWER_VARIANCE = 18;
  public static final int FREEZER_DURATION_CYCLE = 2;
  public static final int FREEZER_SHIFTING_INTERVAL = 2;
  public static final int FREEZER_SHIFTING_PERIODS = 12;

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
