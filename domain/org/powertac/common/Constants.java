/*
 * Copyright 2009-2010 the original author or authors.
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

package org.powertac.common;

import java.math.RoundingMode;

/**
 * This class contains constant definitions used throughout the project.
 * 
 * @author Carsten Block
 * @version 1.0, Date: 13.12.10
 */
public class Constants
{
  /**
   * DECIMALS constant is used to set the common decimal scaling in BigDecimal
   * numbers; for powertac this is set to 2 decimal digits
   */
  //public static final int DECIMALS = 4;
  public static final int HOURS_OF_DAY = 24;
  public static final int DAYS_OF_BOOTSTRAP = 14;
  public static final int PERCENTAGE = 100;
  public static final int MILLION = 1000000;
  //public static final double EPSILON = 2.7;
  //public static final int MEAN_TARIFF_DURATION = 5;
  public static final int MORNING_START_HOUR = 7;
  public static final int EVENING_START_HOUR = 18;
  //public static final int MEAN_NIGHT_CONSUMPTION = 0;
  //public static final int MEAN_MORNING_CONSUMPTION = 2;
  //public static final int MEAN_EVENING_CONSUMPTION = 3;

  public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  // JEC - what are these for?
  public static final String COMPETITION_INACTIVE = "competition.inactive";
  public static final String COMPETITION_NOT_FOUND = "competition.not.found";

  public static final String NOT_FOUND = "not.found";
  //public static final String TARIFF_HAS_PARENT = "tariff.has.parent";
  public static final String TARIFF_OUTDATED = "tariff.outdated";
  public static final String TARIFF_INVALID_STATE = "tariff.tariffState.invalid";
  public static final String TARIFF_NON_DYNAMIC = "tariff.not.dynamic";
  public static final String TARIFF_WRONG_CUSTOMER = "tariff.customer.wrong";
  public static final String TARIFF_WRONG_BROKER = "tariff.broker.wrong";

  public static final String TARIFF_SUBSCRIPTION_START_AFTER_END = "tariffsubscription.start.after.end";

  public static final String TIMESLOT_INACTIVE = "timeslot.inactive";

  public static final String SHOUT_DELETED = "shout.deleted";
  public static final String SHOUT_OUTDATED = "shout.outdated";
  public static final String SHOUT_EXECUTED = "shout.executed";
  public static final String SHOUT_WRONG_BROKER = "tariff.broker.wrong";
  public static final String SHOUT_LIMITORDER_NULL_LIMIT = "shout.limitorder.limit.null";
  public static final String SHOUT_MARKETORDER_WITH_LIMIT = "shout.marketorder.limit.not.null";
  public static final String SHOUT_UPDATE_WITHOUT_LIMIT_AND_QUANTITY = "shout.update.limit.and.quantity.null";
}
