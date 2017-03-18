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

package org.powertac.householdcustomer.persons;

import java.util.ListIterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.RandomSeed;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.householdcustomer.configurations.VillageConstants;
import org.powertac.householdcustomer.customers.Household;
import org.powertac.householdcustomer.enumerations.Status;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A person domain instance represents a single person in its real life
 * activities The person is living in a house, it may work, it goes to the
 * movies, it is sick, it goes on a vacation trip. In order to make the models
 * as realistic as possible we have them to live their lives as part of a bigger
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
  static protected Logger log = LogManager.getLogger(Person.class.getName());

  @Autowired
  protected RandomSeedRepo randomSeedRepo;

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
  Household memberOf;

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

  /** The duration each of the person's leisure activity takes **/
  int leisureDuration = 0;

  /**
   * This is a vector containing the days of the week that the person has
   * leisure time.
   **/
  Vector<Integer> leisureVector = new Vector<Integer>();

  /**
   * This is a vector of the person's status in quarterly fashion.
   **/
  Vector<Status> dailyRoutine = new Vector<Status>();

  /** The weekly schedule and status of the person. **/
  Vector<Vector<Status>> weeklyRoutine = new Vector<Vector<Status>>();

  /**
   * This variable is utilized for the creation of the RandomSeed numbers and is
   * taken from the service.
   */
  RandomSeed gen;

  /**
   * This is the initialization function. It uses the variable values for the
   * configuration file to create the person as it should for this type.
   * 
   * @param AgentName
   * @param conf
   * @param publicVacationVector
   * @param seed
   */
  public void initialize (String AgentName, Properties conf,
                          Vector<Integer> publicVacationVector, int seed)
  {

  }

  /**
   * This function checks if the person is sleeping.
   * 
   * @return True if the person is sleeping
   */
  boolean isSleeping ()
  {
    if (status == Status.Sleeping)
      return true;
    else
      return false;
  }

  /**
   * This function checks if the person is at work.
   * 
   * @return True if the person is at work
   */
  boolean isAtWork ()
  {
    if (status == Status.Working)
      return true;
    else
      return false;
  }

  /**
   * This function checks if the person is doing a leisure activity.
   * 
   * @return True if the person is doing a leisure activity
   */
  boolean isLeisure ()
  {
    if (status == Status.Leisure)
      return true;
    else
      return false;
  }

  /**
   * This function checks if the person is on vacation.
   * 
   * @return true if the person is on vacation
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
   * @return true if the person is sick
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
   * This function fills out the leisure days' vector of the person by choosing
   * randomly days of the week, while the amount of days is different for each
   * person type.
   * 
   * @param counter
   * @param gen
   * @return leisure days' vector
   */
  Vector<Integer> createLeisureVector (int counter)
  {
    // Create auxiliary variable
    Vector<Integer> v = new Vector<Integer>();

    // Loop for the amount of days
    for (int i = 0; i < counter; i++) {
      int day = gen.nextInt(VillageConstants.DAYS_OF_WEEK);
      v.add(day);
    }
    java.util.Collections.sort(v);
    return v;
  }

  /**
   * This function fills out the vector containing the days that the person is
   * going to be sick. When a person is sick he stays in the household.
   * 
   * @param mean
   * @param dev
   * @param gen
   * @return vector with sick days
   */
  Vector<Integer> createSicknessVector (double mean, double dev)
  {
    // Create auxiliary variables

    int days = (int) (dev * gen.nextGaussian() + mean);
    Vector<Integer> v = new Vector<Integer>(days);

    for (int i = 0; i < days; i++) {
      int x =
        gen.nextInt(VillageConstants.DAYS_OF_COMPETITION
                    + VillageConstants.DAYS_OF_BOOTSTRAP) + 1;
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

  /** This function sets the household in which the person is living in. */
  public void setMemberOf (Household house)
  {
    memberOf = house;
  }

  /**
   * This function fills out the daily routine of the person, taking into
   * account the different variables and occupations, if he is sick or working
   * etc.
   * 
   * @param day
   * @param vacationAbsence
   */
  public void fillDailyRoutine (int day, double vacationAbsence)
  {
    // Create auxiliary variable
    Status st;

    int weekday = day % VillageConstants.DAYS_OF_WEEK;
    dailyRoutine = new Vector<Status>();
    if (sicknessVector.contains(day)) {
      fillSick();
    }
    else {
      if (publicVacationVector.contains(day)
          || (this instanceof WorkingPerson && vacationVector.contains(day))) {
        if (gen.nextDouble() < vacationAbsence) {
          for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
            st = Status.Vacation;
            dailyRoutine.add(st);
          }
        }
        else {
          normalFill();
          addLeisure(weekday);
        }
      }
      else {
        normalFill();
        if (this instanceof WorkingPerson) {
          int index = workingDays.indexOf(weekday);
          if (index > -1) {
            fillWork();
            addLeisureWorking(weekday);
          }
          else {
            addLeisure(weekday);
          }
        }
        else {
          addLeisure(weekday);
        }
      }
    }
  }

  /**
   * This function fills out the daily routine with the leisure activity of the
   * day, if there is one for the person in question for the certain weekday.
   * 
   * @param weekday
   * @param gen
   */
  void addLeisure (int weekday)
  {
    // Create auxiliary variables
    ListIterator<Integer> iter = leisureVector.listIterator();
    Status st;

    while (iter.hasNext()) {
      if (iter.next() == weekday) {
        int start =
          VillageConstants.START_OF_LEISURE
                  + gen.nextInt(VillageConstants.LEISURE_WINDOW);
        for (int i = start; i < start + leisureDuration; i++) {
          st = Status.Leisure;
          dailyRoutine.set(i, st);
          if (i == VillageConstants.QUARTERS_OF_DAY - 1)
            break;
        }
      }
    }
  }

  /**
   * This function fills out the leisure activities in the daily schedule of the
   * person in question.
   * 
   * @param weekday
   * @param gen
   */
  void addLeisureWorking (int weekday)
  {

  }

  /**
   * This function fills out the daily routine of the person as if he stays in
   * the house all day long.
   */
  void normalFill ()
  {
    Status st;
    for (int i = VillageConstants.START_OF_SLEEPING_1; i < VillageConstants.END_OF_SLEEPING_1; i++) {
      st = Status.Sleeping;
      dailyRoutine.add(st);
    }
    for (int i = VillageConstants.END_OF_SLEEPING_1; i < VillageConstants.START_OF_SLEEPING_2; i++) {
      st = Status.Normal;
      dailyRoutine.add(st);
    }
    for (int i = VillageConstants.START_OF_SLEEPING_2; i < VillageConstants.END_OF_SLEEPING_2; i++) {
      st = Status.Sleeping;
      dailyRoutine.add(st);
    }
  }

  /**
   * This function fills out the daily routine of the person that is sick for
   * the day.
   */
  void fillSick ()
  {
    Status st;
    for (int i = VillageConstants.START_OF_SLEEPING_1; i < VillageConstants.END_OF_SLEEPING_1; i++) {
      st = Status.Sleeping;
      dailyRoutine.add(st);
    }
    for (int i = VillageConstants.END_OF_SLEEPING_1; i < VillageConstants.START_OF_SLEEPING_2; i++) {
      st = Status.Sick;
      dailyRoutine.add(st);
    }
    for (int i = VillageConstants.START_OF_SLEEPING_2; i < VillageConstants.END_OF_SLEEPING_2; i++) {
      st = Status.Sleeping;
      dailyRoutine.add(st);
    }
  }

  /**
   * This function fill the daily program of the person with the suitable
   * working activities taking in consideration the working habits, duration and
   * shifts.
   */
  void fillWork ()
  {

  }

  /**
   * This is the function utilized to show the information regarding the person
   * in question, its variables values etc.
   */
  public void showInfo ()
  {

  }

  /**
   * At the end of each week the person models refresh their schedule. This way
   * we have a realistic and dynamic model, changing working hours, leisure
   * activities and so on.
   * 
   * @param config
   */
  public void refresh (Properties config)
  {

  }

  public void test ()
  {

    System.out.println(toString() + " " + gen.nextDouble());

  }

  @Override
  public String toString ()
  {
    return name;
  }
}
