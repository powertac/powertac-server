/*
 * Copyright 2009-2010 the original author or authors. Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.common.configurations;

/**
 * This class contains constant definitions used throughout the household
 * models.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.0, Date: 13.12.10
 */
public class HouseholdConstants
{

  // GENERAL VARIABLES -----------------------------//
  public static final int PERCENTAGE = 100;
  public static final int THOUSAND = 1000;
  public static final int KWH = 1000;
  public static final int MWH = 1000000;
  public static final int MILLION = 1000000;
  public static final int MEAN_TARIFF_DURATION = 5;
  public static final double HALF = 0.5;
  public static final double EPSILON = 2.7;

  // TIME VARIABLES -----------------------------//
  public static final int DAYS_OF_COMPETITION = 63;
  public static final int WEEKS_OF_COMPETITION = 9;
  public static final int DAYS_OF_BOOTSTRAP = 14;
  public static final int WEEKS_OF_BOOTSTRAP = 2;
  public static final int DAYS_OF_WEEK = 7;
  public static final int WEEKDAYS = 5;
  public static final int QUARTERS_OF_HOUR = 4;
  public static final int QUARTERS_OF_DAY = 96;
  public static final int HOURS_OF_DAY = 24;

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
  public static final int MONDAY = 1;
  public static final int TUESDAY = 2;
  public static final int WEDNESDAY = 3;
  public static final int THURSDAY = 4;
  public static final int FRIDAY = 5;
  public static final int SATURDAY = 6;
  public static final int SUNDAY = 0;

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
  public static final double CIRCULATION_PUMP_CONSUMPTION_SHARE_VARIANCE = 0.01;
  public static final double CIRCULATION_PUMP_CONSUMPTION_SHARE_MEAN = 0.06;
  public static final double CIRCULATION_PUMP_BASE_LOAD_SHARE = 0.07;
  public static final int CIRCULATION_PUMP_POWER_MEAN = 90;
  public static final int CIRCULATION_PUMP_POWER_VARIANCE = 15;
  public static final int CIRCULATION_PUMP_DURATION_CYCLE = 65;
  public static final int CIRCULATION_PUMP_POSSIBILITY_SEASON_1 = 35;
  public static final int CIRCULATION_PUMP_POSSIBILITY_SEASON_2 = 30;
  public static final int CIRCULATION_PUMP_POSSIBILITY_SEASON_3 = 35;
  public static final int CIRCULATION_PUMP_POSSIBILITY_DAY_1 = 14;
  public static final int CIRCULATION_PUMP_POSSIBILITY_DAY_2 = 14;
  public static final int CIRCULATION_PUMP_POSSIBILITY_DAY_3 = 16;

  // CONSUMER ELECTRONICS VARIABLES -----------------------------//
  public static final double CONSUMER_ELECTRONICS_CONSUMPTION_SHARE_VARIANCE = 0.0022;
  public static final double CONSUMER_ELECTRONICS_CONSUMPTION_SHARE_MEAN = 0.13;
  public static final double CONSUMER_ELECTRONICS_BASE_LOAD_SHARE = 0.35;
  public static final int CONSUMER_ELECTRONICS_POWER_MEAN = 100;
  public static final int CONSUMER_ELECTRONICS_POWER_VARIANCE = 17;
  public static final int CONSUMER_ELECTRONICS_DURATION_CYCLE = 1;
  public static final int CONSUMER_ELECTRONICS_POSSIBILITY_SEASON_1 = 22;
  public static final int CONSUMER_ELECTRONICS_POSSIBILITY_SEASON_2 = 39;
  public static final int CONSUMER_ELECTRONICS_POSSIBILITY_SEASON_3 = 39;
  public static final int CONSUMER_ELECTRONICS_POSSIBILITY_DAY_1 = 14;
  public static final int CONSUMER_ELECTRONICS_POSSIBILITY_DAY_2 = 14;
  public static final int CONSUMER_ELECTRONICS_POSSIBILITY_DAY_3 = 16;

  // ICT VARIABLES -----------------------------//
  public static final double ICT_CONSUMPTION_SHARE_VARIANCE = 0.0008;
  public static final double ICT_CONSUMPTION_SHARE_MEAN = 0.005;
  public static final double ICT_BASE_LOAD_SHARE = 0.6;
  public static final int ICT_POWER_MEAN = 150;
  public static final int ICT_POWER_VARIANCE = 25;
  public static final int ICT_DURATION_CYCLE = 1;
  public static final int ICT_POSSIBILITY_SEASON_1 = 25;
  public static final int ICT_POSSIBILITY_SEASON_2 = 38;
  public static final int ICT_POSSIBILITY_SEASON_3 = 37;
  public static final int ICT_POSSIBILITY_DAY_1 = 14;
  public static final int ICT_POSSIBILITY_DAY_2 = 14;
  public static final int ICT_POSSIBILITY_DAY_3 = 16;

  // LIGHTS VARIABLES -----------------------------//
  public static final double LIGHTS_CONSUMPTION_SHARE_VARIANCE = 0.0013;
  public static final double LIGHTS_CONSUMPTION_SHARE_MEAN = 0.008;
  public static final double LIGHTS_BASE_LOAD_SHARE = 0;
  public static final int LIGHTS_POWER_MEAN = 350;
  public static final int LIGHTS_POWER_VARIANCE = 58;
  public static final int LIGHTS_DURATION_CYCLE = 1;
  public static final int LIGHTS_POSSIBILITY_SEASON_1 = 23;
  public static final int LIGHTS_POSSIBILITY_SEASON_2 = 39;
  public static final int LIGHTS_POSSIBILITY_SEASON_3 = 38;
  public static final int LIGHTS_POSSIBILITY_DAY_1 = 12;
  public static final int LIGHTS_POSSIBILITY_DAY_2 = 15;
  public static final int LIGHTS_POSSIBILITY_DAY_3 = 13;

  // OTHERS VARIABLES -----------------------------//
  public static final double OTHERS_CONSUMPTION_SHARE_VARIANCE = 0.0007;
  public static final double OTHERS_CONSUMPTION_SHARE_MEAN = 0.0041;
  public static final double OTHERS_BASE_LOAD_SHARE = 0.02;
  public static final int OTHERS_POWER_MEAN = 500;
  public static final int OTHERS_POWER_VARIANCE = 83;
  public static final int OTHERS_DURATION_CYCLE = 1;
  public static final int OTHERS_POSSIBILITY_SEASON_1 = 41;
  public static final int OTHERS_POSSIBILITY_SEASON_2 = 24;
  public static final int OTHERS_POSSIBILITY_SEASON_3 = 35;
  public static final int OTHERS_POSSIBILITY_DAY_1 = 14;
  public static final int OTHERS_POSSIBILITY_DAY_2 = 14;
  public static final int OTHERS_POSSIBILITY_DAY_3 = 16;

  // DISHWASHER VARIABLES -----------------------------//
  public static final double DISHWASHER_CONSUMPTION_SHARE_VARIANCE = 0.0006;
  public static final double DISHWASHER_CONSUMPTION_SHARE_MEAN = 0.0037;
  public static final double DISHWASHER_BASE_LOAD_SHARE = 0;
  public static final int DISHWASHER_POWER_MEAN = 530;
  public static final int DISHWASHER_POWER_VARIANCE = 88;
  public static final int DISHWASHER_DURATION_CYCLE = 8;
  public static final int DISHWASHER_POSSIBILITY_SEASON_1 = 29;
  public static final int DISHWASHER_POSSIBILITY_SEASON_2 = 36;
  public static final int DISHWASHER_POSSIBILITY_SEASON_3 = 35;
  public static final int DISHWASHER_POSSIBILITY_DAY_1 = 15;
  public static final int DISHWASHER_POSSIBILITY_DAY_2 = 14;
  public static final int DISHWASHER_POSSIBILITY_DAY_3 = 15;

  // DRYER VARIABLES -----------------------------//
  public static final double DRYER_CONSUMPTION_SHARE_VARIANCE = 0.0004;
  public static final double DRYER_CONSUMPTION_SHARE_MEAN = 0.00215;
  public static final double DRYER_BASE_LOAD_SHARE = 0.015;
  public static final int DRYER_POWER_MEAN = 1410;
  public static final int DRYER_POWER_VARIANCE = 235;
  public static final int DRYER_DURATION_CYCLE = 7;
  public static final int DRYER_POSSIBILITY_SEASON_1 = 25;
  public static final int DRYER_POSSIBILITY_SEASON_2 = 38;
  public static final int DRYER_POSSIBILITY_SEASON_3 = 37;
  public static final int DRYER_POSSIBILITY_DAY_1 = 14;
  public static final int DRYER_POSSIBILITY_DAY_2 = 14;
  public static final int DRYER_POSSIBILITY_DAY_3 = 16;

  public static final int DRYER_SECOND_PHASE = 3;
  public static final int DRYER_THIRD_PHASE = 6;
  public static final int DRYER_THIRD_PHASE_LOAD = 250;

  // AIR CONDITION VARIABLES -----------------------------//
  public static final double AIR_CONDITION_CONSUMPTION_SHARE_VARIANCE = 0.0025;
  public static final double AIR_CONDITION_CONSUMPTION_SHARE_MEAN = 0.0012;
  public static final double AIR_CONDITION_BASE_LOAD_SHARE = 0;
  public static final int AIR_CONDITION_POWER_MEAN_SMALL_NORMAL = 9000;
  public static final int AIR_CONDITION_POWER_MEAN_MEDIUM_NORMAL = 12000;
  public static final int AIR_CONDITION_POWER_MEAN_LARGE_NORMAL = 18000;

  public static final double AIR_CONDITION_POWER_OVER_START_INVERTER = 0.09;
  public static final double AIR_CONDITION_POWER_MEAN_INVERTER = 0.5;

  public static final double AIR_CONDITION_CLASS_A_EER = 3.5;
  public static final double AIR_CONDITION_CLASS_A_COP = 4;
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

  public static final int AIR_CONDITION_POSSIBILITY_SEASON_1 = 24;
  public static final int AIR_CONDITION_POSSIBILITY_SEASON_2 = 38;
  public static final int AIR_CONDITION_POSSIBILITY_SEASON_3 = 38;
  public static final int AIR_CONDITION_POSSIBILITY_DAY_1 = 16;
  public static final int AIR_CONDITION_POSSIBILITY_DAY_2 = 14;
  public static final int AIR_CONDITION_POSSIBILITY_DAY_3 = 14;

  public static final int AIR_CONDITION_LOW_LIMIT_MEAN = 16;
  public static final int AIR_CONDITION_LOW_LIMIT_VARIANCE = 2;
  public static final int AIR_CONDITION_UPPER_LIMIT_MEAN = 28;
  public static final int AIR_CONDITION_UPPER_LIMIT_VARIANCE = 2;

  // STORAGE WATER HEATER VARIABLES -----------------------------//
  public static final double STORAGE_HEATER_CONSUMPTION_SHARE_VARIANCE = 0.0022;
  public static final double STORAGE_HEATER_CONSUMPTION_SHARE_MEAN = 0.0013;
  public static final double STORAGE_HEATER_BASE_LOAD_SHARE = 0;
  public static final int STORAGE_HEATER_POWER_MEAN = 3000;
  public static final int STORAGE_HEATER_POWER_VARIANCE = 500;
  public static final int STORAGE_HEATER_DURATION_CYCLE = 8;
  public static final int STORAGE_HEATER_POSSIBILITY_SEASON_1 = 24;
  public static final int STORAGE_HEATER_POSSIBILITY_SEASON_2 = 38;
  public static final int STORAGE_HEATER_POSSIBILITY_SEASON_3 = 38;
  public static final int STORAGE_HEATER_POSSIBILITY_DAY_1 = 16;
  public static final int STORAGE_HEATER_POSSIBILITY_DAY_2 = 14;
  public static final int STORAGE_HEATER_POSSIBILITY_DAY_3 = 14;

  public static final int STORAGE_HEATER_START = 20;
  public static final int STORAGE_HEATER_PHASES = 4;
  public static final int STORAGE_HEATER_PHASE_LOAD = 20;
  public static final int STORAGE_HEATER_SHIFTING_END = 17;
  public static final double STORAGE_HEATER_POSSIBILITY = 0.8;

  // INSTANT WATER HEATER VARIABLES -----------------------------//
  public static final double INSTANT_HEATER_CONSUMPTION_SHARE_VARIANCE = 0.0022;
  public static final double INSTANT_HEATER_CONSUMPTION_SHARE_MEAN = 0.013;
  public static final double INSTANT_HEATER_BASE_LOAD_SHARE = 0;
  public static final int INSTANT_HEATER_POWER_MEAN = 12000;
  public static final int INSTANT_HEATER_POWER_VARIANCE = 2000;
  public static final int INSTANT_HEATER_DURATION_CYCLE = 1;
  public static final int INSTANT_HEATER_POSSIBILITY_SEASON_1 = 24;
  public static final int INSTANT_HEATER_POSSIBILITY_SEASON_2 = 38;
  public static final int INSTANT_HEATER_POSSIBILITY_SEASON_3 = 38;
  public static final int INSTANT_HEATER_POSSIBILITY_DAY_1 = 16;
  public static final int INSTANT_HEATER_POSSIBILITY_DAY_2 = 14;
  public static final int INSTANT_HEATER_POSSIBILITY_DAY_3 = 14;

  // WASHING MACHINE VARIABLES -----------------------------//
  public static final double WASHING_MACHINE_CONSUMPTION_SHARE_VARIANCE = 0.0006;
  public static final double WASHING_MACHINE_CONSUMPTION_SHARE_MEAN = 0.0035;
  public static final double WASHING_MACHINE_BASE_LOAD_SHARE = 0.002;
  public static final int WASHING_MACHINE_POWER_MEAN = 100;
  public static final int WASHING_MACHINE_POWER_VARIANCE = 600;
  public static final int WASHING_MACHINE_DURATION_CYCLE = 8;
  public static final int WASHING_MACHINE_POSSIBILITY_SEASON_1 = 30;
  public static final int WASHING_MACHINE_POSSIBILITY_SEASON_2 = 35;
  public static final int WASHING_MACHINE_POSSIBILITY_SEASON_3 = 35;
  public static final int WASHING_MACHINE_POSSIBILITY_DAY_1 = 14;
  public static final int WASHING_MACHINE_POSSIBILITY_DAY_2 = 14;
  public static final int WASHING_MACHINE_POSSIBILITY_DAY_3 = 16;

  // STOVE VARIABLES -----------------------------//
  public static final double STOVE_CONSUMPTION_SHARE_VARIANCE = 0.0014;
  public static final double STOVE_CONSUMPTION_SHARE_MEAN = 0.0081;
  public static final double STOVE_BASE_LOAD_SHARE = 0;
  public static final int STOVE_POWER_MEAN = 1840;
  public static final int STOVE_POWER_VARIANCE = 307;
  public static final int STOVE_DURATION_CYCLE = 2;
  public static final int STOVE_POSSIBILITY_SEASON_1 = 29;
  public static final int STOVE_POSSIBILITY_SEASON_2 = 36;
  public static final int STOVE_POSSIBILITY_SEASON_3 = 35;
  public static final int STOVE_POSSIBILITY_DAY_1 = 17;
  public static final int STOVE_POSSIBILITY_DAY_2 = 13;
  public static final int STOVE_POSSIBILITY_DAY_3 = 18;

  // SPACE HEATER VARIABLES -----------------------------//
  public static final double SPACE_HEATER_CONSUMPTION_SHARE_VARIANCE = 0.0028;
  public static final double SPACE_HEATER_CONSUMPTION_SHARE_MEAN = 0.017;
  public static final double SPACE_HEATER_BASE_LOAD_SHARE = 0;
  public static final int SPACE_HEATER_POWER_MEAN = 7000;
  public static final int SPACE_HEATER_POWER_VARIANCE = 300;
  public static final int SPACE_HEATER_DURATION_CYCLE = 14;
  public static final int SPACE_HEATER_POSSIBILITY_SEASON_1 = 21;
  public static final int SPACE_HEATER_POSSIBILITY_SEASON_2 = 42;
  public static final int SPACE_HEATER_POSSIBILITY_SEASON_3 = 37;
  public static final int SPACE_HEATER_POSSIBILITY_DAY_1 = 15;
  public static final int SPACE_HEATER_POSSIBILITY_DAY_2 = 14;
  public static final int SPACE_HEATER_POSSIBILITY_DAY_3 = 15;

  public static final int SPACE_HEATER_PHASE_1 = 9;
  public static final int SPACE_HEATER_PHASE_2 = 16;
  public static final int SPACE_HEATER_PHASE_3 = 86;
  public static final int SPACE_HEATER_PHASE_4 = 90;
  public static final int SPACE_HEATER_PHASE_LOAD = 750;

  // REFRIGERATOR VARIABLES -----------------------------//
  public static final double REFRIGERATOR_CONSUMPTION_SHARE_VARIANCE = 0.00015;
  public static final double REFRIGERATOR_CONSUMPTION_SHARE_MEAN = 0.009;
  public static final double REFRIGERATOR_BASE_LOAD_SHARE = 0;
  public static final int REFRIGERATOR_POWER_MEAN = 140;
  public static final int REFRIGERATOR_POWER_VARIANCE = 23;
  public static final int REFRIGERATOR_DURATION_CYCLE = 2;
  public static final int REFRIGERATOR_POSSIBILITY_SEASON_1 = 35;
  public static final int REFRIGERATOR_POSSIBILITY_SEASON_2 = 30;
  public static final int REFRIGERATOR_POSSIBILITY_SEASON_3 = 35;
  public static final int REFRIGERATOR_POSSIBILITY_DAY_1 = 14;
  public static final int REFRIGERATOR_POSSIBILITY_DAY_2 = 14;
  public static final int REFRIGERATOR_POSSIBILITY_DAY_3 = 16;
  public static final int REFRIGERATOR_SHIFTING_INTERVAL = 2;
  public static final int REFRIGERATOR_SHIFTING_PERIODS = 12;

  // FREEZER VARIABLES -----------------------------//
  public static final double FREEZER_CONSUMPTION_SHARE_VARIANCE = 0.00012;
  public static final double FREEZER_CONSUMPTION_SHARE_MEAN = 0.0071;
  public static final double FREEZER_BASE_LOAD_SHARE = 0;
  public static final int FREEZER_POWER_MEAN = 106;
  public static final int FREEZER_POWER_VARIANCE = 18;
  public static final int FREEZER_DURATION_CYCLE = 2;
  public static final int FREEZER_POSSIBILITY_SEASON_1 = 35;
  public static final int FREEZER_POSSIBILITY_SEASON_2 = 30;
  public static final int FREEZER_POSSIBILITY_SEASON_3 = 35;
  public static final int FREEZER_POSSIBILITY_DAY_1 = 14;
  public static final int FREEZER_POSSIBILITY_DAY_2 = 14;
  public static final int FREEZER_POSSIBILITY_DAY_3 = 16;
  public static final int FREEZER_SHIFTING_INTERVAL = 2;
  public static final int FREEZER_SHIFTING_PERIODS = 12;

  // COST ESTIMATION VARIABLES ------------------------//
  public static final int RANDOM_DAYS_NUMBER = 3;
}
