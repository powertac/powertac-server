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

package org.powertac.officecomplexcustomer.persons;

import java.util.ListIterator;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;
import org.powertac.officecomplexcustomer.customers.Office;
import org.powertac.officecomplexcustomer.enumerations.Status;

/**
 * A person domain instance represents a single person in its real life
 * activities. The person is working in an office, is on a break, he goes back
 * home, he is sick, it goes on a vacation trip. In order to make the models as
 * realistic as possible we have them to live their lives as part of a bigger
 * community.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */

public class Person
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static protected Logger log = Logger.getLogger(Person.class.getName());

  /**
   * The person's name in the community. It includes the household he is living
   * in or its type of person.
   */
  String name;

  /**
   * The person's status at each time step. He may be sleeping, working, having
   * fun etc.
   **/
  Status status;

  /** The household that the person lives in. **/
  Office memberOf;

  /** A vector the contains the working days of the week. **/
  Vector<Integer> workingDays = new Vector<Integer>();

  /** This is a vector of the working vacation days of the year for this person. **/
  Vector<Integer> vacationVector = new Vector<Integer>();

  /**
   * Vector of the public vacation days of the person's community, such as
   * Christmas, Easter and so on.
   **/
  Vector<Integer> publicVacationVector = new Vector<Integer>();

  /**
   * This is a vector of the days that the person is sick and will stay home.
   **/
  Vector<Integer> sicknessVector = new Vector<Integer>();

  /**
   * This is a vector of the person's status in quarterly fashion.
   **/
  Vector<Status> dailyRoutine = new Vector<Status>();

  /** The weekly schedule and status of the person. **/
  Vector<Vector<Status>> weeklyRoutine = new Vector<Vector<Status>>();

  /**
   * This variable is utilized for the creation of the random numbers and is
   * taken from the service.
   */
  Random gen;

  /**
   * This is the initialization function. It uses the variable values for the
   * configuration file to create the person as it should for this type.
   * 
   * @param AgentName
   * @param conf
   * @param publicVacationVector
   * @param gen
   * @return
   */
  public void initialize (String AgentName, Properties conf, Vector<Integer> publicVacationVector, Random gen)
  {
  }

  /**
   * This function checks if the person is at home.
   * 
   * @return
   */
  boolean isAtHome ()
  {
    if (status == Status.Home)
      return true;
    else
      return false;
  }

  /**
   * This function checks if the person is at work.
   * 
   * @return
   */
  boolean isAtWork ()
  {
    if (status == Status.Working)
      return true;
    else
      return false;
  }

  /**
   * This function checks if the person is on a break.
   * 
   * @return
   */
  boolean isOnBreak ()
  {
    if (status == Status.Break)
      return true;
    else
      return false;
  }

  /**
   * This function checks if the person is on vacation.
   * 
   * @return
   */
  boolean isVacation ()
  {
    if (status == Status.Vacation)
      return true;
    else
      return false;
  }

  /**
   * This function checks if the person is sick.
   * 
   * @return
   */
  boolean isSick ()
  {
    if (status == Status.Sick)
      return true;
    else
      return false;
  }

  /** This function returns the weekly routine of a person. */
  public Vector<Vector<Status>> getWeeklyRoutine ()
  {
    return weeklyRoutine;
  }

  /** This function returns the daily routine of a person. */
  public Vector<Status> getDailyRoutine ()
  {
    return dailyRoutine;
  }

  /**
   * This function fills out the vector containing the days that the person is
   * going to be sick. When a person is sick he stays in bed.
   * 
   * @param mean
   * @param dev
   * @param gen
   * @return
   */
  Vector<Integer> createSicknessVector (double mean, double dev, Random gen)
  {
    // Create auxiliary variables

    int days = (int) (dev * gen.nextGaussian() + mean);
    Vector<Integer> v = new Vector<Integer>(days);

    for (int i = 0; i < days; i++) {
      int x = gen.nextInt(OfficeComplexConstants.DAYS_OF_COMPETITION + OfficeComplexConstants.DAYS_OF_BOOTSTRAP) + 1;
      ListIterator<Integer> iter = v.listIterator();
      while (iter.hasNext()) {
        int temp = (int) iter.next();
        if (x == temp) {
          x = x + 1;
          iter = v.listIterator();
        }
      }
      v.add(x);
    }
    java.util.Collections.sort(v);
    return v;
  }

  /** This function sets the office in which the person is working on. */
  public void setMemberOf (Office house)
  {
    memberOf = house;
  }

  /**
   * This function fills out the daily routine of the person, taking into
   * account the different variables and occupations, if he is sick or on
   * vacation etc.
   * 
   * @param day
   * @param vacationAbsence
   * @param gen
   * @return
   */
  public void fillDailyRoutine (int day, Random gen)
  {
    // Create auxiliary variable
    Status st;

    int weekday = day % OfficeComplexConstants.DAYS_OF_WEEK;
    dailyRoutine = new Vector<Status>();
    if (sicknessVector.contains(day)) {
      fillSick();
    } else {
      if (publicVacationVector.contains(day) || (vacationVector.contains(day))) {
        for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
          st = Status.Vacation;
          dailyRoutine.add(st);
        }
      } else {
        normalFill();
        int index = workingDays.indexOf(weekday);
        if (index > -1) {
          fillWork();
        }
      }
    }
  }

  /**
   * This function fills out the daily routine of the person as if he stays in
   * the house all day long.
   * 
   * @return
   */
  void normalFill ()
  {
    Status st;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      st = Status.Home;
      dailyRoutine.add(st);
    }
  }

  /**
   * This function fills out the daily routine of the person that is sick for
   * the day.
   * 
   * @return
   */
  void fillSick ()
  {
    Status st;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      st = Status.Sick;
      dailyRoutine.add(st);
    }
  }

  /**
   * This function fill the daily program of the person with the suitable
   * working activities taking in consideration the working habits, duration and
   * shifts.
   * 
   * @return
   */
  void fillWork ()
  {

  }

  /**
   * This is the function utilized to show the information regarding the person
   * in question, its variables values etc.
   * 
   * @return
   */
  public void showInfo ()
  {

  }

  /**
   * At the end of each week the person models refresh their schedule. This way
   * we have a realistic and dynamic model, changing working hours, leisure
   * activities and so on.
   * 
   * @param conf
   * @param gen
   * @return
   */
  public void refresh (Properties config, Random gen)
  {

  }

  @Override
  public String toString ()
  {
    return name;
  }
}
