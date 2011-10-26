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
package org.powertac.householdcustomer.customers;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.configurations.HouseholdConstants;
import org.powertac.common.enumerations.Status;
import org.powertac.householdcustomer.appliances.Appliance;
import org.powertac.householdcustomer.appliances.CirculationPump;
import org.powertac.householdcustomer.appliances.ConsumerElectronics;
import org.powertac.householdcustomer.appliances.Dishwasher;
import org.powertac.householdcustomer.appliances.Dryer;
import org.powertac.householdcustomer.appliances.Freezer;
import org.powertac.householdcustomer.appliances.ICT;
import org.powertac.householdcustomer.appliances.Lights;
import org.powertac.householdcustomer.appliances.NotShiftingAppliance;
import org.powertac.householdcustomer.appliances.Others;
import org.powertac.householdcustomer.appliances.Refrigerator;
import org.powertac.householdcustomer.appliances.SpaceHeater;
import org.powertac.householdcustomer.appliances.Stove;
import org.powertac.householdcustomer.appliances.WashingMachine;
import org.powertac.householdcustomer.appliances.WaterHeater;
import org.powertac.householdcustomer.persons.MostlyPresentPerson;
import org.powertac.householdcustomer.persons.PeriodicPresentPerson;
import org.powertac.householdcustomer.persons.Person;
import org.powertac.householdcustomer.persons.RandomlyAbsentPerson;

/**
 * The household is the domain instance represents a single house with the tenants living inside it
 * and fully equipped with appliances statistically distributed. There are different kinds of
 * appliances utilized by the persons inhabiting the premises and each person living has it's own
 * schedule.
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class Household
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error() appropriately. Use
   * log.debug() for output you want to see in testing or debugging.
   */
  static protected Logger log = Logger.getLogger(Household.class.getName());

  /** the household name. It is different for each one to be able to tell them apart. */
  String name;

  /**
   * This is a vector containing each day's load from the appliances installed inside the household.
   **/
  Vector<Integer> dailyBaseLoad = new Vector<Integer>();

  /**
   * This is a vector containing each day's load from the appliances installed inside the household.
   **/
  Vector<Integer> dailyControllableLoad = new Vector<Integer>();

  /**
   * This is a vector containing the load from the appliances installed inside the household for all
   * the week days.
   **/
  Vector<Vector<Integer>> weeklyBaseLoad = new Vector<Vector<Integer>>();

  /**
   * This is a vector containing the load from the appliances installed inside the household for all
   * the week days.
   **/
  Vector<Vector<Integer>> weeklyControllableLoad = new Vector<Vector<Integer>>();

  /**
   * This is a statistical measure of the household, giving a general idea of the consumption level
   * during a year.
   */
  int yearConsumption;

  /** This is an agreggated vector containing each day's load in hours. **/
  Vector<Integer> dailyBaseLoadInHours = new Vector<Integer>();

  /** This is an agreggated vector containing each day's load in hours. **/
  Vector<Integer> dailyControllableLoadInHours = new Vector<Integer>();

  /** This is an agreggated vector containing the weekly base load in hours. **/
  Vector<Vector<Integer>> weeklyBaseLoadInHours = new Vector<Vector<Integer>>();

  /** This is an agreggated vector containing the weekly controllable load in hours. **/
  Vector<Vector<Integer>> weeklyControllableLoadInHours = new Vector<Vector<Integer>>();

  /** This variable shows the current load of the house, for the current quarter or hour. **/
  int currentLoad;

  /** Helping variable for the correct refreshing of the schedules. */
  int week = 0;

  /**
   * This is a vector containing the members of the household, the people that belong to each
   * household
   */
  Vector<Person> members = new Vector<Person>();

  /**
   * This is a vector containing the members of the household, the people that belong to each
   * household
   */
  Vector<Appliance> appliances = new Vector<Appliance>();

  /**
   * This variable is pointing to the village that this household is part of.
   */
  public Village householdOf;

  /**
   * This is the initialization function. It uses the variable values for the configuration file to
   * create the household and then fill it with persons and appliances as it seems fit.
   * @param HouseName
   * @param conf
   * @param publicVacationVector
   * @param gen
   * @return
   */
  public void initialize (String HouseName, Properties conf, Vector<Integer> publicVacationVector, Random gen)
  {
    double va = Double.parseDouble(conf.getProperty("VacationAbsence"));
    name = HouseName;
    int persons = memberRandomizer(conf, gen);
    for (int i = 0; i < persons; i++)
      addPerson(i + 1, conf, publicVacationVector, gen);

    for (Person member : members) {
      for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK; i++) {
        member.fillDailyRoutine(i, va, gen);
        member.getWeeklyRoutine().add(member.getDailyRoutine());
        member.setMemberOf(this);
      }
      // member.showInfo();
    }

    fillAppliances(conf, gen);

    /*
        for (Appliance appliance : appliances) {
          appliance.showStatus();
        }
    */

    for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK; i++) {
      dailyBaseLoad = fillDailyBaseLoad(week * HouseholdConstants.DAYS_OF_WEEK + i);
      dailyControllableLoad = fillDailyControllableLoad(week * HouseholdConstants.DAYS_OF_WEEK + i);
      weeklyBaseLoad.add(dailyBaseLoad);
      weeklyControllableLoad.add(dailyControllableLoad);

      dailyBaseLoadInHours = fillDailyBaseLoadInHours();
      dailyControllableLoadInHours = fillDailyControllableLoadInHours();
      weeklyBaseLoadInHours.add(dailyBaseLoadInHours);
      weeklyControllableLoadInHours.add(dailyControllableLoadInHours);
    }

    for (week = 1; week < HouseholdConstants.WEEKS_OF_COMPETITION; week++) {
      refresh(conf, gen);
    }

    for (Appliance appliance : appliances) {
      appliance.setOperationDays();
    }

    /* System.out.println(this.toString() + "  " + weeklyBaseLoad.size());
     * System.out.println(this.toString() + "  " + weeklyControllableLoad.size());
     * System.out.println(this.toString() + "  " + weeklyBaseLoadInHours.size());
     * System.out.println(this.toString() + "  " + weeklyControllableLoadInHours.size());
     * 
     * for (Person member : members) { System.out.println(member.getWeeklyRoutine().toString()); }
     * 
     * for (Appliance appliance : appliances) {
     * System.out.println(appliance.getWeeklyLoadVector().toString()); } */
  }

  /**
   * This function is creating a random number of person (given by the next function) and add them
   * to the current household, filling it up with life.
   * @param counter
   * @param conf
   * @param publicVacationVector
   * @param gen
   * @return
   */
  void addPerson (int counter, Properties conf, Vector<Integer> publicVacationVector, Random gen)
  {
    // Taking parameters from configuration file
    int pp = Integer.parseInt(conf.getProperty("PeriodicPresent"));
    int mp = Integer.parseInt(conf.getProperty("MostlyPresent"));

    int x = gen.nextInt(HouseholdConstants.PERCENTAGE);
    if (x < pp) {
      PeriodicPresentPerson ppp = new PeriodicPresentPerson();
      ppp.initialize("PPP" + counter, conf, publicVacationVector, gen);
      members.add(ppp);

    } else {
      if (x >= pp & x < (pp + mp)) {
        MostlyPresentPerson mpp = new MostlyPresentPerson();
        mpp.initialize("MPP" + counter, conf, publicVacationVector, gen);
        members.add(mpp);
      } else {
        RandomlyAbsentPerson rap = new RandomlyAbsentPerson();
        rap.initialize("RAP" + counter, conf, publicVacationVector, gen);
        members.add(rap);
      }
    }
  }

  /**
   * This is a function that returns the appliances of the household.
   */
  public Vector<Appliance> getAppliances ()
  {
    return appliances;
  }

  /**
   * This is a function that returns the members of the household.
   */
  public Vector<Person> getMembers ()
  {
    return members;
  }

  /**
   * This is the function that utilizes the possibilities of the number of persons in a household
   * and gives back a number randomly.
   * @param conf
   * @param gen
   * @return
   */
  int memberRandomizer (Properties conf, Random gen)
  {
    int one = Integer.parseInt(conf.getProperty("OnePerson"));
    int two = Integer.parseInt(conf.getProperty("TwoPersons"));
    int three = Integer.parseInt(conf.getProperty("ThreePersons"));
    int four = Integer.parseInt(conf.getProperty("FourPersons"));
    int returnValue;

    int x = gen.nextInt(HouseholdConstants.PERCENTAGE);
    if (x < one) {
      yearConsumption = Integer.parseInt(conf.getProperty("OnePersonConsumption"));
      returnValue = HouseholdConstants.ONE_PERSON;
    } else {
      if (x >= one & x < (one + two)) {
        yearConsumption = Integer.parseInt(conf.getProperty("TwoPersonsConsumption"));
        returnValue = HouseholdConstants.TWO_PERSONS;
      } else {
        if (x >= (one + two) & x < (one + two + three)) {
          yearConsumption = Integer.parseInt(conf.getProperty("ThreePersonsConsumption"));
          returnValue = HouseholdConstants.THREE_PERSONS;
        } else {
          if (x >= (one + two + three) & x < (one + two + three + four)) {
            yearConsumption = Integer.parseInt(conf.getProperty("FourPersonsConsumption"));
            returnValue = HouseholdConstants.FOUR_PERSONS;
          } else {
            yearConsumption = Integer.parseInt(conf.getProperty("FivePersonsConsumption"));
            returnValue = HouseholdConstants.FIVE_PERSONS;
          }
        }
      }
    }
    return returnValue;
  }

  /**
   * This function is using the appliance's saturation in order to make a possibility check and
   * install or not the appliance in the current household.
   * @param app
   * @param gen
   * @return
   */
  void checkProbability (Appliance app, Random gen)
  {
    // Creating auxiliary variables

    int x = gen.nextInt(HouseholdConstants.PERCENTAGE);
    int threshold = (int) (app.getSaturation() * HouseholdConstants.PERCENTAGE);
    if (x < threshold) {
      app.fillWeeklyFunction(gen);
      app.createWeeklyPossibilityOperationVector();
    } else
      this.appliances.remove(app);
  }

  /**
   * This function is responsible for the filling of the household with the appliances and their
   * schedule for the first week using a statistic formula and the members of the household.
   * @param conf
   * @param gen
   * @return
   */
  void fillAppliances (Properties conf, Random gen)
  {

    // NOT SHIFTING ================================

    // Consumer Electronics
    ConsumerElectronics ce = new ConsumerElectronics();
    appliances.add(ce);
    ce.setApplianceOf(this);
    ce.initialize(this.name, conf, gen);
    ce.fillWeeklyFunction(gen);
    ce.createWeeklyPossibilityOperationVector();

    // ICT
    ICT ict = new ICT();
    appliances.add(ict);
    ict.setApplianceOf(this);
    ict.initialize(this.name, conf, gen);
    ict.fillWeeklyFunction(gen);
    ict.createWeeklyPossibilityOperationVector();

    // Lights
    Lights lights = new Lights();
    appliances.add(lights);
    lights.setApplianceOf(this);
    lights.initialize(this.name, conf, gen);
    lights.fillWeeklyFunction(gen);
    lights.createWeeklyPossibilityOperationVector();

    // Others
    Others others = new Others();
    appliances.add(others);
    others.setApplianceOf(this);
    others.initialize(this.name, conf, gen);
    others.fillWeeklyFunction(gen);
    others.createWeeklyPossibilityOperationVector();

    // Circulation Pump
    CirculationPump cp = new CirculationPump();
    cp.setApplianceOf(this);
    appliances.add(cp);
    cp.initialize(this.name, conf, gen);
    checkProbability(cp, gen);

    // FULLY SHIFTING ================================

    // Refrigerator
    Refrigerator ref = new Refrigerator();
    appliances.add(ref);
    ref.setApplianceOf(this);
    ref.initialize(this.name, conf, gen);
    ref.fillWeeklyFunction(gen);
    ref.createWeeklyPossibilityOperationVector();

    // Freezer
    Freezer fr = new Freezer();
    appliances.add(fr);
    fr.setApplianceOf(this);
    fr.initialize(this.name, conf, gen);
    checkProbability(fr, gen);

    // Water Heater
    WaterHeater wh = new WaterHeater();
    appliances.add(wh);
    wh.setApplianceOf(this);
    wh.initialize(this.name, conf, gen);
    checkProbability(wh, gen);

    // Space Heater
    SpaceHeater sh = new SpaceHeater();
    appliances.add(sh);
    sh.setApplianceOf(this);
    sh.initialize(this.name, conf, gen);
    checkProbability(sh, gen);

    // SEMI SHIFTING ================================

    // Dishwasher
    Dishwasher dw = new Dishwasher();
    appliances.add(dw);
    dw.setApplianceOf(this);
    dw.initialize(this.name, conf, gen);
    checkProbability(dw, gen);

    // Stove
    Stove st = new Stove();
    appliances.add(st);
    st.setApplianceOf(this);
    st.initialize(this.name, conf, gen);
    checkProbability(st, gen);

    // Washing Machine
    WashingMachine wm = new WashingMachine();
    appliances.add(wm);
    wm.setApplianceOf(this);
    wm.initialize(this.name, conf, gen);
    wm.fillWeeklyFunction(gen);
    wm.createWeeklyPossibilityOperationVector();

    // Dryer
    Dryer dr = new Dryer();
    appliances.add(dr);
    dr.setApplianceOf(this);
    dr.initialize(this.name, conf, gen);
    checkProbability(dr, gen);

  }

  /**
   * This function checks if all the inhabitants of the household are out of the household.
   * @param weekday
   * @param quarter
   * @return
   */
  public boolean isEmpty (int weekday, int quarter)
  {
    boolean x = true;
    for (Person member : members) {
      if (member.getWeeklyRoutine().get(week * HouseholdConstants.DAYS_OF_WEEK + weekday).get(quarter) == Status.Normal
          || member.getWeeklyRoutine().get(week * HouseholdConstants.DAYS_OF_WEEK + weekday).get(quarter) == Status.Sick) {
        x = false;
      }
    }
    return x;
  }

  /**
   * This is the function utilized to show the information regarding the household in question, its
   * variables values etc.
   * @return
   */
  void showStatus ()
  {
    // Printing basic variables
    log.info("HouseHold Name : " + name);
    log.info("HouseHold Yearly Consumption : " + yearConsumption);
    log.info("Number of Persons : " + members.size());

    // Printing members' status
    Iterator<Person> iter = members.iterator();
    while (iter.hasNext())
      iter.next().showInfo();

    // Printing appliances' status
    Iterator<Appliance> itera = appliances.iterator();
    log.info(" Number Of Appliances = ");
    log.info(appliances.size());
    while (itera.hasNext())
      itera.next().showStatus();

    // Printing daily load
    log.info(" Daily Load = ");
    for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK; i++) {
      log.info("Day " + (i));
      ListIterator<Integer> iter2 = weeklyBaseLoad.get(i).listIterator();
      ListIterator<Integer> iter3 = weeklyControllableLoad.get(i).listIterator();
      for (int j = 0; j < HouseholdConstants.QUARTERS_OF_DAY; j++)
        log.info("Quarter : " + (j + 1) + " Base Load : " + iter2.next() + " Controllable Load: " + iter3.next());
    }

    // Printing daily load in hours
    log.info(" Load In Hours = ");
    for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK; i++) {
      log.info("Day " + (i));
      ListIterator<Integer> iter2 = weeklyBaseLoadInHours.get(i).listIterator();
      ListIterator<Integer> iter3 = weeklyControllableLoadInHours.get(i).listIterator();
      for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++)
        log.info("Hours : " + (j + 1) + " Base Load : " + iter2.next() + " Controllable Load: " + iter3.next());
    }
  }

  /**
   * This function is used in order to fill the daily Base Load of the household for each quarter of
   * the hour
   * @param weekday
   * @return
   */
  Vector<Integer> fillDailyBaseLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v = new Vector<Integer>(HouseholdConstants.QUARTERS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Appliance appliance : appliances) {
        if (appliance instanceof NotShiftingAppliance)
          sum = sum + appliance.getWeeklyLoadVector().get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the daily Controllable Load of the household for each
   * quarter of the hour.
   * @param weekday
   * @return
   */
  Vector<Integer> fillDailyControllableLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v = new Vector<Integer>(HouseholdConstants.QUARTERS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Appliance appliance : appliances) {
        if (!(appliance instanceof NotShiftingAppliance))
          sum = sum + appliance.getWeeklyLoadVector().get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function checks if all the inhabitants of the household are away on vacation on a certain
   * quarter
   * @param quarter
   * @return
   */
  public boolean isOnVacation (int weekday, int quarter)
  {
    boolean x = false;
    for (Person member : members) {
      if (member.getWeeklyRoutine().get(week * HouseholdConstants.DAYS_OF_WEEK + weekday).get(quarter) == Status.Vacation) {
        x = true;
      }
    }
    return x;
  }

  /**
   * This function represents the function that shows the conditions in an household each moment in
   * time.
   * @param day
   * @param quarter
   * @return
   */
  public void stepStatus (int day, int quarter)
  {
    // Printing Inhabitants Status

    log.info("House: " + name);
    log.info("Person Quarter Status");

    // For each person in the house
    for (Person member : members) {
      log.info("Name: " + member.toString() + " Status: " + member.getWeeklyRoutine().get(day).get(quarter));
    }
    // Printing Inhabitants Status
    log.info("Appliances Quarter Status");
    for (Appliance appliance : appliances) {
      log.info("Name: " + appliance.toString() + " Status: " + appliance.getWeeklyOperation().get(day).get(quarter) + " + Load: " + appliance.getWeeklyLoadVector().get(day).get(quarter));
    }
    // Printing Household Status
    setCurrentLoad(day, quarter);
    log.info("Current Load: " + currentLoad);
  }

  /**
   * This function fills out the daily Base Load in hours vector taking in consideration the load
   * per quarter of an hour.
   * @return
   */
  Vector<Integer> fillDailyBaseLoadInHours ()
  {

    // Creating Auxiliary Variables
    Vector<Integer> v = new Vector<Integer>(HouseholdConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum = dailyBaseLoad.get(i * HouseholdConstants.QUARTERS_OF_HOUR) + dailyBaseLoad.get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
          + dailyBaseLoad.get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + dailyBaseLoad.get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
      v.add(sum);
    }
    return v;
  }

  /**
   * This function fills out the daily Controllable Load in hours vector taking in consideration the
   * load per quarter of an hour.
   * @return
   */
  Vector<Integer> fillDailyControllableLoadInHours ()
  {

    // Creating Auxiliary Variables
    Vector<Integer> v = new Vector<Integer>(HouseholdConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum = dailyControllableLoad.get(i * HouseholdConstants.QUARTERS_OF_HOUR) + dailyControllableLoad.get(i * HouseholdConstants.QUARTERS_OF_HOUR + 1)
          + dailyControllableLoad.get(i * HouseholdConstants.QUARTERS_OF_HOUR + 2) + dailyControllableLoad.get(i * HouseholdConstants.QUARTERS_OF_HOUR + 3);
      v.add(sum);
    }
    return v;
  }

  /**
   * This function set the current load in accordance with the time of the competition
   * @param day
   * @param quarter
   * @return
   */
  void setCurrentLoad (int day, int quarter)
  {
    currentLoad = weeklyBaseLoad.get(day).get(quarter) + weeklyControllableLoad.get(day).get(quarter);
  }

  /**
   * At the end of each week the household models refresh their schedule. This way we have a
   * realistic and dynamic model, changing function hours, consuming power and so on.
   * @param conf
   * @param gen
   * @return
   */
  void refresh (Properties conf, Random gen)
  {

    // For each member of the household
    for (Person member : members) {
      member.refresh(conf, gen);
    }

    // For each appliance of the household
    for (Appliance appliance : appliances) {
      appliance.setOperationVector(new Vector<Vector<Boolean>>());
      if (!(appliance instanceof Dryer))
        appliance.refresh(gen);

    }

    for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK; i++) {
      dailyBaseLoad = fillDailyBaseLoad(week * HouseholdConstants.DAYS_OF_WEEK + i);
      dailyControllableLoad = fillDailyControllableLoad(week * HouseholdConstants.DAYS_OF_WEEK + i);
      weeklyBaseLoad.add(dailyBaseLoad);
      weeklyControllableLoad.add(dailyControllableLoad);
      dailyBaseLoadInHours = fillDailyBaseLoadInHours();
      dailyControllableLoadInHours = fillDailyControllableLoadInHours();
      weeklyBaseLoadInHours.add(dailyBaseLoadInHours);
      weeklyControllableLoadInHours.add(dailyControllableLoadInHours);
    }

  }

  /**
   * This is the function that takes every appliance in the household and readies the shifted
   * Controllable Consumption for the needs of the tariff evaluation.
   * @param tariff
   * @param now
   * @param day
   * @return
   */
  long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {

    long[] newControllableLoad = new long[HouseholdConstants.HOURS_OF_DAY];

    for (Appliance appliance : appliances) {
      if (!(appliance instanceof NotShiftingAppliance)) {
        long[] temp = appliance.dailyShifting(tariff, now, day, gen);
        Vector<Long> tempVector = new Vector<Long>();
        Vector<Long> controllableVector = new Vector<Long>();
        log.info("Appliance " + appliance.toString());
        log.info("Load: " + appliance.getWeeklyLoadVector().get(day).toString());

        for (int i = 0; i < HouseholdConstants.HOURS_OF_DAY; i++)
          tempVector.add(temp[i]);
        log.info("Temp: " + tempVector.toString());

        for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++) {
          newControllableLoad[j] += temp[j];
          controllableVector.add(newControllableLoad[j]);
        }
        log.info("New Load: " + controllableVector.toString());
      }
    }
    return newControllableLoad;
  }

  /**
   * This function prints to the screen the daily load of the household for the weekday at hand
   * @param weekday
   * @return
   */
  public void printDailyLoad (int day)
  {
    ListIterator<Integer> iter = weeklyBaseLoadInHours.get(day).listIterator();
    ListIterator<Integer> iter2 = weeklyControllableLoadInHours.get(day).listIterator();
    log.info("Summary of Daily Load of House " + name);
    for (int j = 0; j < HouseholdConstants.HOURS_OF_DAY; j++)
      log.info("Hour : " + j + 1 + " Base Load : " + iter.next() + " Controllable Load : " + iter2.next());
  }

  public String toString ()
  {
    return name;
  }

}
