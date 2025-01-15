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

package org.powertac.officecomplexcustomer.appliances;

import java.util.ListIterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.time.Instant;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluationHelper;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;
import org.powertac.officecomplexcustomer.customers.Office;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A appliance domain instance represents a single appliance inside a household.
 * There are different kinds of appliances utilized by the persons inhabiting
 * the premises. Some of them are functioning automatically, some are only used
 * when someone is present etc.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Appliance
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static protected Logger log = LogManager.getLogger(Appliance.class.getName());

  @Autowired
  protected RandomSeedRepo randomSeedRepo;

  /**
   * The appliance name. Appliances are named after the type of appliance and
   * the household that contains it.
   */
  protected String name;

  /** The household that the appliance is installed at. **/
  protected Office applianceOf;

  /**
   * This variable shows the possibility (%) that this appliance is contained in
   * a house.
   */
  protected double saturation;

  /**
   * This variable shows the power (in Watts) that are consumed when using this
   * appliance.
   */
  protected int power;
  protected int overallPower;

  /** This variable equals the duration of the operation cycle of the appliance. */
  protected int cycleDuration;

  /**
   * This is a vector containing the quarters that the appliance can start
   * functioning.
   */
  Vector<Vector<Boolean>> possibilityOperationVector =
    new Vector<Vector<Boolean>>();

  /**
   * This is a vector that contains the operation days of each appliance for the
   * competition's duration.
   */
  Vector<Boolean> operationDaysVector = new Vector<Boolean>();

  /**
   * This is a vector containing the final daily operation of the appliance
   * (after shifting due to any cause).
   */
  Vector<Boolean> dailyOperation = new Vector<Boolean>();

  /**
   * This is a vector containing the final weekly operation of the appliance
   * (after shifting due to any cause).
   */
  Vector<Vector<Boolean>> weeklyOperation = new Vector<Vector<Boolean>>();

  /**
   * This is a vector containing the consumption load of the appliance during
   * the day.
   */
  Vector<Integer> loadVector = new Vector<Integer>();

  /**
   * This is a vector containing the final weekly load of the appliance (after
   * shifting due to any cause).
   */
  Vector<Vector<Integer>> weeklyLoadVector = new Vector<Vector<Integer>>();

  /**
   * This variable contains the amount of times the appliance may work through
   * the week or day.
   */
  int times;

  /**
   * This variable is utilized for the creation of the random numbers and is
   * taken from the service.
   */
  RandomSeed gen;

  /** This function returns the power variable of the appliance. */
  public int getPower ()
  {
    return power;
  }

  /** This function returns the power variable of the appliance. */
  public int getOverallPower ()
  {
    return overallPower;
  }

  /** This function returns the household where the appliance is installed. */
  public Office getApplianceOf ()
  {
    return applianceOf;
  }

  /** This function returns the saturation variable of the appliance. */
  public double getSaturation ()
  {
    return saturation;
  }

  /** This function returns the duration variable of the appliance. */
  public int getDuration ()
  {
    return cycleDuration;
  }

  /** This function returns the weekly operation vector of the appliance. */
  public Vector<Vector<Boolean>> getWeeklyOperation ()
  {
    return weeklyOperation;
  }

  /** This function returns the weekly load vector of the appliance. */
  public Vector<Vector<Integer>> getWeeklyLoadVector ()
  {
    return weeklyLoadVector;
  }

  /** This function returns the operation days vector of the appliance. */
  public Vector<Boolean> getOperationDaysVector ()
  {
    return operationDaysVector;
  }

  /** This function sets the household in which the appliance is installed in. */
  public void setApplianceOf (Office office)
  {
    applianceOf = office;
  }

  /**
   * This function is used to create the daily possibility operation vector of
   * each appliance for the week taking into consideration the days that this
   * appliance could be able to function.
   * 
   * @param day
   * @return
   */
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {
    return new Vector<Boolean>();
  }

  /**
   * This function is used to create the weekly possibility operation vector of
   * each appliance taking into consideration the times that this appliance
   * could be able to function.
   */
  public void createWeeklyPossibilityOperationVector ()
  {
    for (int i = 0; i < OfficeComplexConstants.DAYS_OF_WEEK; i++)
      possibilityOperationVector.add(createDailyPossibilityOperationVector(i));
  }

  /**
   * This is the initialization function. It uses the variable values for the
   * configuration file to create the appliance as it should for this type.
   * 
   * @param office
   * @param conf
   * @param seed
   */
  public void initialize (String office, Properties conf, int seed)
  {

  }

  /**
   * This is a complex function that changes the appliance's function in order
   * to have the most cost effective operation load in a day schedule.
   * 
   * @param tariff
   * @param nonDominantLoad
   * @param tariffEvalHelper
   * @param day
   * @param start 
   * @return TODO
   */
  public double[] dailyShifting (Tariff tariff, double[] nonDominantLoad,
                                 TariffEvaluationHelper tariffEvalHelper,
                                 int day, Instant start)
  {
    return new double[OfficeComplexConstants.HOURS_OF_DAY];
  }

  /**
   * This is a simple function utilized for the creation of the function Vector
   * that will be used in the shifting procedure.
   * 
   * @param day
   * @return TODO
   */
  boolean[] createShiftingOperationMatrix (int day)
  {

    boolean[] shiftingOperationMatrix =
      new boolean[OfficeComplexConstants.HOURS_OF_DAY];

    for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
      boolean function =
        possibilityOperationVector.get(day)
                .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                || possibilityOperationVector.get(day)
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                || possibilityOperationVector.get(day)
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                || possibilityOperationVector.get(day)
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
      shiftingOperationMatrix[i] = function;
    }
    return shiftingOperationMatrix;
  }

  /**
   * This function fills out all the quarters of the appliance functions for a
   * single day of the week.
   * 
   * @param times
   */
  public void fillDailyOperation (int times)
  {

  }

  /**
   * This function fills out all the days of the appliance functions for each
   * day of the week.
   */
  public void fillWeeklyOperation ()
  {
    for (int i = 0; i < OfficeComplexConstants.DAYS_OF_WEEK; i++)
      fillDailyOperation(i);
  }

  /**
   * This is the function utilized to show the information regarding the
   * appliance in question, its variables values etc.
   */
  public void showStatus ()
  {
    // Printing base variables
    log.debug("Name = " + name);
    log.debug("Member Of = " + applianceOf.toString());
    log.debug("Saturation = " + saturation);
    log.debug("Power = " + power);
    log.debug("Cycle Duration = " + cycleDuration);

    // Printing Weekly Function Vector and Load
    log.debug("Weekly Operation Vector and Load = ");
    for (int i = 0; i < OfficeComplexConstants.DAYS_OF_COMPETITION
                        + OfficeComplexConstants.DAYS_OF_BOOTSTRAP; i++) {
      log.debug("Day " + i);
      ListIterator<Boolean> iter = weeklyOperation.get(i).listIterator();
      ListIterator<Integer> iter2 = weeklyLoadVector.get(i).listIterator();
      for (int j = 0; j < OfficeComplexConstants.QUARTERS_OF_DAY; j++)
        log.debug("Quarter " + j + " = " + iter.next() + "   Load = "
                  + iter2.next());
    }
  }

  /** This function fills out the daily function of an appliance for the day. */
  public void weatherDailyFunction (int day, int hour, double temp)
  {
  }

  /**
   * At the end of each week the appliance models refresh their schedule. This
   * way we have a realistic and dynamic model, changing function hours,
   * consuming power and so on.
   */
  public void refresh ()
  {
  }

  // public void test ()
  // {
  // System.out.println(toString() + " " + gen.nextDouble());
  // }

  /**
   * This is an function to fill the maps utilized by Services in order to keep
   * the vectors of each appliance during the runtime.
   */
  public void setOperationDays ()
  {

    // Add the data values for each day of competition and each quarter of each
    // day.
    for (int i = 0; i < OfficeComplexConstants.DAYS_OF_COMPETITION
                        + OfficeComplexConstants.DAYS_OF_BOOTSTRAP; i++) {
      boolean function = false;
      for (int j = 0; j < OfficeComplexConstants.QUARTERS_OF_DAY; j++) {
        function = function || weeklyOperation.get(i).get(j);
      }
      operationDaysVector.add(function);
    }
  }

  /**
   * This is an function created to estimate the overall power consumption of a
   * certain appliance in a single operation the vectors of each appliance
   * during the runtime.
   */
  public void calculateOverallPower ()
  {
    overallPower = -1;
  }

  @Override
  public String toString ()
  {
    return name;
  }
}
