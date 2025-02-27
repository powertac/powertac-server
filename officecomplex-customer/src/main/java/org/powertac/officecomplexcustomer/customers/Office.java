/*
 * Copyright 2009-2012 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12 the original author or authors.
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
package org.powertac.officecomplexcustomer.customers;

import java.util.Arrays;
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
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.officecomplexcustomer.appliances.AirCondition;
import org.powertac.officecomplexcustomer.appliances.Appliance;
import org.powertac.officecomplexcustomer.appliances.CoffeeMachine;
import org.powertac.officecomplexcustomer.appliances.Computers;
import org.powertac.officecomplexcustomer.appliances.ConsumerElectronics;
import org.powertac.officecomplexcustomer.appliances.CopyMachine;
import org.powertac.officecomplexcustomer.appliances.ICT;
import org.powertac.officecomplexcustomer.appliances.Lights;
import org.powertac.officecomplexcustomer.appliances.MicrowaveOven;
import org.powertac.officecomplexcustomer.appliances.NotShiftingAppliance;
import org.powertac.officecomplexcustomer.appliances.Refrigerator;
import org.powertac.officecomplexcustomer.appliances.Servers;
import org.powertac.officecomplexcustomer.appliances.VendingMachine;
import org.powertac.officecomplexcustomer.appliances.WeatherSensitiveAppliance;
import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;
import org.powertac.officecomplexcustomer.enumerations.Status;
import org.powertac.officecomplexcustomer.persons.PeriodicPresentPerson;
import org.powertac.officecomplexcustomer.persons.Person;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The office is the domain instance represents a single working facility with
 * the workers acting inside it and fully equipped with appliances statistically
 * distributed. There are different kinds of appliances utilized by the persons
 * working in the premises and each person has it's own working and break
 * schedule.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Office
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static protected Logger log = LogManager.getLogger(Office.class.getName());

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  int seedId = 1;

  /**
   * The office name. It is different for each one to be able to tell them
   * apart.
   */
  String name;

  /**
   * This is a vector containing each day's base load from the appliances
   * installed inside the office.
   **/
  Vector<Integer> dailyBaseLoad = new Vector<Integer>();
  Vector<Integer> dailyControllableLoad = new Vector<Integer>();
  Vector<Integer> dailyWeatherSensitiveLoad = new Vector<Integer>();
  Vector<Integer> dailyNonDominantLoad = new Vector<Integer>();
  Vector<Integer> dailyDominantLoad = new Vector<Integer>();

  /**
   * This is a vector containing the base load from the appliances installed
   * inside the office for all the week days.
   **/
  Vector<Vector<Integer>> weeklyBaseLoad = new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyControllableLoad =
    new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyWeatherSensitiveLoad =
    new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyNonDominantLoad = new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyDominantLoad = new Vector<Vector<Integer>>();

  /** This is an aggregated vector containing each day's base load in hours. **/
  Vector<Integer> dailyBaseLoadInHours = new Vector<Integer>();
  Vector<Integer> dailyControllableLoadInHours = new Vector<Integer>();
  Vector<Integer> dailyWeatherSensitiveLoadInHours = new Vector<Integer>();
  Vector<Integer> dailyNonDominantLoadInHours = new Vector<Integer>();
  Vector<Integer> dailyDominantLoadInHours = new Vector<Integer>();

  /** This is an aggregated vector containing the weekly base load in hours. **/
  Vector<Vector<Integer>> weeklyBaseLoadInHours = new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyControllableLoadInHours =
    new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyWeatherSensitiveLoadInHours =
    new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyNonDominantLoadInHours =
    new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyDominantLoadInHours =
    new Vector<Vector<Integer>>();

  /**
   * Helping variable showing the current week of competition for the correct
   * refreshing of the schedules.
   */
  int week = 0;

  /**
   * This is a vector containing the members of the office, the persons that
   * belong to each office.
   */
  Vector<Person> members = new Vector<Person>();

  /**
   * This is a vector containing the appliances installed in the office.
   */
  Vector<Appliance> appliances = new Vector<Appliance>();

  /**
   * This is the index of most power consuming appliance of the office.
   */
  int dominantAppliance;

  /**
   * The number of days that the dominant appliance operates and not.
   */
  int daysDominant = 0;
  int daysNonDominant = 0;

  /**
   * These are the mean consumptions when utilizing the dominant appliance and
   * when not.
   */
  double[] dominantConsumption =
    new double[OfficeComplexConstants.HOURS_OF_DAY];
  double[] nonDominantConsumption =
    new double[OfficeComplexConstants.HOURS_OF_DAY];

  /**
   * This variable is pointing to the OfficeComplex that this office is part of.
   */
  public OfficeComplex officeOf;

  /**
   * This variable is utilized for the creation of the random numbers and is
   * taken from the service.
   */
  RandomSeed gen;

  /**
   * This is the initialization function. It uses the variable values for the
   * configuration file to create the office and then fill it with persons and
   * appliances as it seems fit.
   * 
   * @param OfficeName
   * @param conf
   * @param publicVacationVector
   * @param seed
   */
  public void initialize (String OfficeName, Properties conf,
                          Vector<Integer> publicVacationVector, int seed)
  {

    name = OfficeName;
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    gen = randomSeedRepo.getRandomSeed(toString(), seed, "Office Model" + seed);

    int persons = memberRandomizer(conf);
    for (int i = 0; i < persons; i++)
      addPerson(i + 1, conf, publicVacationVector);

    for (Person member: members) {
      for (int i = 0; i < OfficeComplexConstants.DAYS_OF_WEEK; i++) {
        member.fillDailyRoutine(i);
        member.getWeeklyRoutine().add(member.getDailyRoutine());
        member.setMemberOf(this);
      }
      // member.showInfo();
    }

    fillAppliances(conf);

    for (int i = 0; i < OfficeComplexConstants.DAYS_OF_WEEK; i++) {
      dailyBaseLoad =
        fillDailyBaseLoad(week * OfficeComplexConstants.DAYS_OF_WEEK + i);
      dailyControllableLoad =
        fillDailyControllableLoad(week * OfficeComplexConstants.DAYS_OF_WEEK
                                  + i);
      dailyWeatherSensitiveLoad =
        fillDailyWeatherSensitiveLoad(week
                                      * OfficeComplexConstants.DAYS_OF_WEEK + i);
      weeklyBaseLoad.add(dailyBaseLoad);
      weeklyControllableLoad.add(dailyControllableLoad);
      weeklyWeatherSensitiveLoad.add(dailyWeatherSensitiveLoad);

      dailyBaseLoadInHours = fillDailyBaseLoadInHours();
      dailyControllableLoadInHours = fillDailyControllableLoadInHours();
      dailyWeatherSensitiveLoadInHours = fillDailyWeatherSensitiveLoadInHours();
      weeklyBaseLoadInHours.add(dailyBaseLoadInHours);
      weeklyControllableLoadInHours.add(dailyControllableLoadInHours);
      weeklyWeatherSensitiveLoadInHours.add(dailyWeatherSensitiveLoadInHours);
    }

    for (week = 1; week < OfficeComplexConstants.WEEKS_OF_COMPETITION
                          + OfficeComplexConstants.WEEKS_OF_BOOTSTRAP; week++) {
      refresh(conf);
    }

    for (Appliance appliance: appliances) {
      appliance.setOperationDays();
      appliance.calculateOverallPower();
    }

    findDominantAppliance();

    if (getDominantAppliance().getOverallPower() != 1)
      createDominantOperationVectors();

    int overallDays =
      (OfficeComplexConstants.WEEKS_OF_COMPETITION + OfficeComplexConstants.WEEKS_OF_BOOTSTRAP)
              * OfficeComplexConstants.DAYS_OF_WEEK;

    for (int i = 0; i < overallDays; i++) {
      dailyNonDominantLoad = fillDailyNonDominantLoad(i);
      weeklyNonDominantLoad.add(dailyNonDominantLoad);
      dailyNonDominantLoadInHours = fillDailyNonDominantLoadInHours();
      weeklyNonDominantLoadInHours.add(dailyNonDominantLoadInHours);
      dailyDominantLoad = fillDailyDominantLoad(i);
      weeklyDominantLoad.add(dailyDominantLoad);
      dailyDominantLoadInHours = fillDailyDominantLoadInHours();
      weeklyDominantLoadInHours.add(dailyDominantLoadInHours);
    }

    /*
    for (Appliance appliance : appliances) {
      appliance.showStatus();
    }
    
    
    
        System.out.println(this.toString() + "  " + weeklyBaseLoad.size());
        System.out.println(this.toString() + "  " + weeklyControllableLoad.size());
        System.out.println(this.toString() + "  " + weeklyBaseLoadInHours.size());
        System.out.println(this.toString() + "  " + weeklyControllableLoadInHours.size());
    
    System.out.println(this.toString() + " Dominant Appliance: "
                       + getDominantAppliance()
                       + " Overall Power Consumption: "
                       + getDominantAppliance().getOverallPower());
    System.out.println(this.toString() + "  "
                       + weeklyDominantLoad.get(0).toString());
    System.out.println(this.toString() + "  "
                       + weeklyNonDominantLoad.get(0).toString());
                       */
  }

  /**
   * This function is creating a random number of person (given by the next
   * function) and add them to the current office, filling it up with life.
   * 
   * @param counter
   * @param conf
   * @param publicVacationVector
   */
  void addPerson (int counter, Properties conf,
                  Vector<Integer> publicVacationVector)
  {
    // Taking parameters from configuration file
    // int pp = Integer.parseInt(conf.getProperty("PeriodicPresent"));

    PeriodicPresentPerson ppp = new PeriodicPresentPerson();
    ppp.initialize("PPP" + counter, conf, publicVacationVector, seedId++);
    members.add(ppp);

  }

  private void createDominantOperationVectors ()
  {

    Appliance app = appliances.get(dominantAppliance);
    Vector<Boolean> op = app.getOperationDaysVector();

    for (int i = 0; i < op.size(); i++) {
      if (op.get(i))
        daysDominant++;
      else
        daysNonDominant++;

      for (int j = 0; j < OfficeComplexConstants.HOURS_OF_DAY; j++) {
        if (op.get(i))
          dominantConsumption[j] +=
            weeklyBaseLoadInHours.get(i).get(j)
                    + weeklyControllableLoadInHours.get(i).get(j)
                    + weeklyWeatherSensitiveLoadInHours.get(i).get(j);
        else
          nonDominantConsumption[j] +=
            weeklyBaseLoadInHours.get(i).get(j)
                    + weeklyControllableLoadInHours.get(i).get(j)
                    + weeklyWeatherSensitiveLoadInHours.get(i).get(j);
      }
    }

    for (int j = 0; j < OfficeComplexConstants.HOURS_OF_DAY; j++) {
      if (daysDominant != 0)
        dominantConsumption[j] /= daysDominant;
      if (daysNonDominant != 0)
        nonDominantConsumption[j] /= daysNonDominant;
    }
    /*
        System.out.println("Household:" + toString());
        System.out.println("Dominant Consumption:"
                           + Arrays.toString(dominantConsumption));
        System.out.println("Non Dominant Consumption:"
                           + Arrays.toString(nonDominantConsumption));
    */
  }

  /**
   * This is a function that returns the appliances of the office.
   */
  public Vector<Appliance> getAppliances ()
  {
    return appliances;
  }

  /**
   * This is a function that returns the members of the office.
   */
  public Vector<Person> getMembers ()
  {
    return members;
  }

  /**
   * This is a function returning the dominant Consumption Load for a certain
   * hour.
   */
  public double getDominantConsumption (int hour)
  {
    return dominantConsumption[hour];
  }

  /**
   * This is a function returning the non dominant Consumption Load for a
   * certain hour.
   */
  public double getNonDominantConsumption (int hour)
  {
    return nonDominantConsumption[hour];
  }

  /** This function returns the dominant appliance of the household. */
  public void findDominantAppliance ()
  {
    double maxConsumption = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < appliances.size(); i++) {
      if (maxConsumption < appliances.get(i).getOverallPower()) {
        maxConsumption = appliances.get(i).getOverallPower();
        dominantAppliance = i;
      }
    }

  }

  /** This function returns the dominant appliance of the household. */
  public Appliance getDominantAppliance ()
  {
    return appliances.get(dominantAppliance);
  }

  /**
   * This is the function that utilizes the possibilities of the number of
   * persons in an office and gives back a number randomly.
   * 
   * @param conf
   * @return TODO
   */
  int memberRandomizer (Properties conf)
  {
    int one = Integer.parseInt(conf.getProperty("OneToFivePersons"));
    int two = Integer.parseInt(conf.getProperty("SixToTenPersons"));
    int three = Integer.parseInt(conf.getProperty("ElevenToFifteenPersons"));
    int returnValue;

    int x = gen.nextInt(OfficeComplexConstants.PERCENTAGE);
    if (x < one) {
      returnValue = 1 + gen.nextInt(OfficeComplexConstants.PERSONS);
    }
    else {
      if (x >= one & x < (one + two)) {
        returnValue =
          1 + OfficeComplexConstants.PERSONS
                  + gen.nextInt(OfficeComplexConstants.PERSONS);
      }
      else {
        if (x >= (one + two) & x < (one + two + three)) {
          returnValue =
            1 + 2 * OfficeComplexConstants.PERSONS
                    + gen.nextInt(OfficeComplexConstants.PERSONS);
        }
        else {
          returnValue =
            1 + 3 * OfficeComplexConstants.PERSONS
                    + gen.nextInt(OfficeComplexConstants.PERSONS);
        }
      }
    }
    return returnValue;
  }

  /**
   * This function is using the appliance's saturation in order to make a
   * possibility check and install or not the appliance in the current office.
   * 
   * @param app
   */
  void checkProbability (Appliance app)
  {
    // Creating auxiliary variables

    int x = gen.nextInt(OfficeComplexConstants.PERCENTAGE);
    int threshold =
      (int) (app.getSaturation() * OfficeComplexConstants.PERCENTAGE);
    if (x < threshold) {
      app.fillWeeklyOperation();
      app.createWeeklyPossibilityOperationVector();
    }
    else
      this.appliances.remove(app);
  }

  /**
   * This function is responsible for the filling of the office with the
   * appliances and their schedule for the first week using a statistic formula
   * and the members of the office.
   * 
   * @param conf
   */
  void fillAppliances (Properties conf)
  {

    // NOT SHIFTING ================================

    // Air Condition
    AirCondition ac = new AirCondition();
    appliances.add(ac);
    ac.setApplianceOf(this);
    ac.initialize(this.name, conf, seedId++);
    checkProbability(ac);

    // Consumer Electronics
    ConsumerElectronics ce = new ConsumerElectronics();
    appliances.add(ce);
    ce.setApplianceOf(this);
    ce.initialize(this.name, conf, seedId++);
    ce.fillWeeklyOperation();
    ce.createWeeklyPossibilityOperationVector();

    // ICT
    ICT ict = new ICT();
    appliances.add(ict);
    ict.setApplianceOf(this);
    ict.initialize(this.name, conf, seedId++);
    ict.fillWeeklyOperation();
    ict.createWeeklyPossibilityOperationVector();

    // Lights
    Lights lights = new Lights();
    appliances.add(lights);
    lights.setApplianceOf(this);
    lights.initialize(this.name, conf, seedId++);
    lights.fillWeeklyOperation();
    lights.createWeeklyPossibilityOperationVector();

    // Computers
    Computers com = new Computers();
    appliances.add(com);
    com.setApplianceOf(this);
    com.initialize(this.name, conf, seedId++);
    com.fillWeeklyOperation();
    com.createWeeklyPossibilityOperationVector();

    // Servers
    Servers servers = new Servers();
    appliances.add(servers);
    servers.setApplianceOf(this);
    servers.initialize(this.name, conf, seedId++);
    checkProbability(servers);

    // FULLY SHIFTING ================================

    // Refrigerator
    Refrigerator ref = new Refrigerator();
    appliances.add(ref);
    ref.setApplianceOf(this);
    ref.initialize(this.name, conf, seedId++);
    ref.fillWeeklyOperation();
    ref.createWeeklyPossibilityOperationVector();

    // CoffeeMachine
    CoffeeMachine coffee = new CoffeeMachine();
    appliances.add(coffee);
    coffee.setApplianceOf(this);
    coffee.initialize(this.name, conf, seedId++);
    checkProbability(coffee);

    // Vending Machine
    VendingMachine vm = new VendingMachine();
    appliances.add(vm);
    vm.setApplianceOf(this);
    vm.initialize(this.name, conf, seedId++);
    checkProbability(vm);

    // SEMI SHIFTING ================================

    // MicrowaveOven
    MicrowaveOven mo = new MicrowaveOven();
    appliances.add(mo);
    mo.setApplianceOf(this);
    mo.initialize(this.name, conf, seedId++);
    checkProbability(mo);

    // Copy Machine
    CopyMachine cm = new CopyMachine();
    appliances.add(cm);
    cm.setApplianceOf(this);
    cm.initialize(this.name, conf, seedId++);
    checkProbability(cm);

  }

  /**
   * This function checks if any of the workers in the office are working
   * 
   * @param weekday
   * @param quarter
   * @return true if any of the workers in the office are working
   */
  public boolean isWorking (int weekday, int quarter)
  {
    boolean x = false;
    for (Person member: members) {
      if (member.getWeeklyRoutine()
              .get(week * OfficeComplexConstants.DAYS_OF_WEEK + weekday)
              .get(quarter) == Status.Working)
        x = true;
    }
    return x;
  }

  /**
   * This function checks if any of the workers of the office are on break.
   * 
   * @param weekday
   * @param quarter
   * @return true if any of the workers of the office are on break
   */
  public boolean isOnBreak (int weekday, int quarter)
  {
    boolean x = false;
    for (Person member: members) {
      if (member.getWeeklyRoutine()
              .get(week * OfficeComplexConstants.DAYS_OF_WEEK + weekday)
              .get(quarter) == Status.Break)
        x = true;
    }
    return x;
  }

  /**
   * This function checks if all the workers of the office are away on vacation
   * or sick on a certain day
   * 
   * @param weekday
   * @return true all the workers of the office are away on vacation or sick
   */
  public boolean isOnVacation (int weekday)
  {
    boolean x = true;
    for (Person member: members) {
      if (member.getWeeklyRoutine()
              .get(week * OfficeComplexConstants.DAYS_OF_WEEK + weekday).get(0) != Status.Vacation
          || member.getWeeklyRoutine()
                  .get(week * OfficeComplexConstants.DAYS_OF_WEEK + weekday)
                  .get(0) != Status.Sick)
        x = false;
    }
    return x;
  }

  /**
   * This function checks if all the workers of the office are away on vacation
   * or sick on a certain day
   * 
   * @param weekday
   * @return true if all the workers of the office are away on vacation
   * or sick
   */
  public boolean isWorkingDayOfWeek (int weekday)
  {
    boolean x = false;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      for (Person member: members) {
        if (member.getWeeklyRoutine()
                .get(week * OfficeComplexConstants.DAYS_OF_WEEK + weekday)
                .get(i) == Status.Working
            || member.getWeeklyRoutine()
                    .get(week * OfficeComplexConstants.DAYS_OF_WEEK + weekday)
                    .get(i) == Status.Break) {
          x = true;
          i = OfficeComplexConstants.QUARTERS_OF_DAY;
          break;
        }
      }
    }
    return x;
  }

  /**
   * This function checks if all the workers of the office are away on vacation
   * or sick on a certain day
   * 
   * @param day
   * @return boolean if all the workers of the office are away on vacation
   * or sick
   */
  public boolean isWorkingDay (int day)
  {
    boolean x = false;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      for (Person member: members) {
        if (member.getWeeklyRoutine().get(day).get(i) == Status.Working
            || member.getWeeklyRoutine().get(day).get(i) == Status.Break) {
          x = true;
          i = OfficeComplexConstants.QUARTERS_OF_DAY;
          break;
        }
      }
    }
    return x;
  }

  /**
   * This function checks if any of the workers is in the office.
   * 
   * @param weekday
   * @param quarter
   * @return true if none of the workers are in the office.
   */
  public boolean isEmpty (int weekday, int quarter)
  {
    boolean x = true;

    if (isWorking(weekday, quarter) || isOnBreak(weekday, quarter))
      x = false;

    return x;
  }

  /**
   * This function checks the number of working employees in the office in a
   * specific quarter.
   * 
   * @param weekday
   * @param quarter
   * @return the number of working employees in the office in a
   * specific quarter
   */
  public int employeeWorkingNumber (int weekday, int quarter)
  {
    int counter = 0;
    for (Person member: members) {
      if (member.getWeeklyRoutine()
              .get(week * OfficeComplexConstants.DAYS_OF_WEEK + weekday)
              .get(quarter) == Status.Working)
        counter++;
    }
    return counter;
  }

  /**
   * This function checks the number of employees on a break in the office in a
   * specific quarter.
   * 
   * @param weekday
   * @param quarter
   * @return the number of employees on a break in the office in a
   * specific quarter
   */
  public int employeeOnBreakNumber (int weekday, int quarter)
  {
    int counter = 0;
    for (Person member: members) {
      if (member.getWeeklyRoutine()
              .get(week * OfficeComplexConstants.DAYS_OF_WEEK + weekday)
              .get(quarter) == Status.Break)
        counter++;
    }
    return counter;
  }

  /**
   * This function checks the number of employees in the office in a specific
   * quarter, either working either on break.
   * 
   * @param weekday
   * @param quarter
   * @return the number of employees in the office in a specific quarter,
   *         either working either on break
   */
  public int employeeNumber (int weekday, int quarter)
  {
    int counter =
      employeeWorkingNumber(weekday, quarter)
              + employeeOnBreakNumber(weekday, quarter);

    return counter;
  }

  /**
   * This is the function utilized to show the information regarding the office
   * in question, its variables values etc.
   */
  void showStatus ()
  {

    // Printing basic variables
    log.info("Office Name : " + name);
    log.info("Number of Persons : " + members.size());
    /*
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
    */
    // Printing daily load
    log.info(" Daily Load = ");
    for (int i = 0; i < OfficeComplexConstants.DAYS_OF_COMPETITION
                        + OfficeComplexConstants.DAYS_OF_BOOTSTRAP; i++) {
      log.info("Day " + i);
      ListIterator<Integer> iter2 = weeklyBaseLoad.get(i).listIterator();
      ListIterator<Integer> iter3 =
        weeklyControllableLoad.get(i).listIterator();
      ListIterator<Integer> iter4 =
        weeklyWeatherSensitiveLoad.get(i).listIterator();
      for (int j = 0; j < OfficeComplexConstants.QUARTERS_OF_DAY; j++)
        log.info("Quarter : " + j + " Base Load : " + iter2.next()
                 + " Controllable Load: " + iter3.next()
                 + " WeatherSensitive Load: " + iter4.next());
    }

    // Printing daily load in hours
    log.info(" Load In Hours = ");
    for (int i = 0; i < OfficeComplexConstants.DAYS_OF_COMPETITION
                        + OfficeComplexConstants.DAYS_OF_BOOTSTRAP; i++) {
      log.info("Day " + i);
      ListIterator<Integer> iter2 = weeklyBaseLoadInHours.get(i).listIterator();
      ListIterator<Integer> iter3 =
        weeklyControllableLoadInHours.get(i).listIterator();
      ListIterator<Integer> iter4 =
        weeklyWeatherSensitiveLoadInHours.get(i).listIterator();
      for (int j = 0; j < OfficeComplexConstants.HOURS_OF_DAY; j++)
        log.info("Hours : " + j + " Base Load : " + iter2.next()
                 + " Controllable Load: " + iter3.next()
                 + " WeatherSensitive Load: " + iter4.next());
    }
  }

  /**
   * This function is used in order to fill the daily Base Load of the office
   * for each quarter of the hour.
   * 
   * @param day
   * @return daily base load
   */
  Vector<Integer> fillDailyBaseLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v =
      new Vector<Integer>(OfficeComplexConstants.QUARTERS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Appliance appliance: appliances) {
        if (appliance instanceof NotShiftingAppliance)
          sum = sum + appliance.getWeeklyLoadVector().get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the daily Controllable Load of the
   * office for each quarter of the hour.
   * 
   * @param day
   * @return daily controllable load
   */
  Vector<Integer> fillDailyControllableLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v =
      new Vector<Integer>(OfficeComplexConstants.QUARTERS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Appliance appliance: appliances) {
        if (!(appliance instanceof NotShiftingAppliance))
          sum = sum + appliance.getWeeklyLoadVector().get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the daily weather sensitive load of
   * the office for each quarter of the hour.
   * 
   * @param day
   * @return daily weather sensitive load
   */
  Vector<Integer> fillDailyWeatherSensitiveLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v =
      new Vector<Integer>(OfficeComplexConstants.QUARTERS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (Appliance appliance: appliances) {
        if (appliance instanceof WeatherSensitiveAppliance)
          sum = sum + appliance.getWeeklyLoadVector().get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function is used in order to fill the daily dominant load of
   * the household for each quarter of the hour.
   * 
   * @param day
   * @return daily dominant load
   */
  Vector<Integer> fillDailyDominantLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v =
      new Vector<Integer>(OfficeComplexConstants.QUARTERS_OF_DAY);
    int sum = 0;

    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      if (appliances.get(dominantAppliance).getOverallPower() != -1)
        sum =
          appliances.get(dominantAppliance).getWeeklyLoadVector().get(day)
                  .get(i);

      v.add(sum);
    }

    return v;
  }

  /**
   * This function is used in order to fill the daily non dominant load of
   * the household for each quarter of the hour.
   * 
   * @param day
   * @return daily non-dominant load
   */
  Vector<Integer> fillDailyNonDominantLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v =
      new Vector<Integer>(OfficeComplexConstants.QUARTERS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      sum = 0;
      for (int j = 0; j < appliances.size(); j++) {
        if (j != dominantAppliance)
          sum = sum + appliances.get(j).getWeeklyLoadVector().get(day).get(i);
      }
      v.add(sum);
    }
    return v;
  }

  /**
   * This function fills out the daily Base Load in hours vector taking in
   * consideration the load per quarter of an hour.
   * 
   * @return daily base load in hours
   */
  Vector<Integer> fillDailyBaseLoadInHours ()
  {

    // Creating Auxiliary Variables
    Vector<Integer> v =
      new Vector<Integer>(OfficeComplexConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum =
        dailyBaseLoad.get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                + dailyBaseLoad.get(i * OfficeComplexConstants.QUARTERS_OF_HOUR
                                    + 1)
                + dailyBaseLoad.get(i * OfficeComplexConstants.QUARTERS_OF_HOUR
                                    + 2)
                + dailyBaseLoad.get(i * OfficeComplexConstants.QUARTERS_OF_HOUR
                                    + 3);
      v.add(sum);
    }
    return v;
  }

  /**
   * This function fills out the daily Controllable Load in hours vector taking
   * in consideration the load per quarter of an hour.
   * 
   * @return daily controllable load in hours
   */
  Vector<Integer> fillDailyControllableLoadInHours ()
  {

    // Creating Auxiliary Variables
    Vector<Integer> v =
      new Vector<Integer>(OfficeComplexConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum =
        dailyControllableLoad.get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                + dailyControllableLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                + dailyControllableLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                + dailyControllableLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
      v.add(sum);
    }
    return v;
  }

  /**
   * This function fills out the daily weather sensitive Load in hours vector
   * taking in consideration the load per quarter of an hour.
   * 
   * @return daily weather sensitive load in hours
   */
  Vector<Integer> fillDailyWeatherSensitiveLoadInHours ()
  {

    // Creating Auxiliary Variables
    Vector<Integer> v =
      new Vector<Integer>(OfficeComplexConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum =
        dailyWeatherSensitiveLoad
                .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                + dailyWeatherSensitiveLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                + dailyWeatherSensitiveLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                + dailyWeatherSensitiveLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
      v.add(sum);
    }
    return v;
  }

  /**
   * This function fills out the daily dominant Load in hours vector
   * taking in consideration the load per quarter of an hour.
   * 
   * @return daily dominant load in hours
   */
  Vector<Integer> fillDailyDominantLoadInHours ()
  {

    // Creating Auxiliary Variables
    Vector<Integer> v =
      new Vector<Integer>(OfficeComplexConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum =
        dailyDominantLoad.get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                + dailyDominantLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                + dailyDominantLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                + dailyDominantLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
      v.add(sum);
    }
    return v;
  }

  /**
   * This function fills out the daily non dominant Load in hours vector
   * taking in consideration the load per quarter of an hour.
   * 
   * @return daily non-dominant load in hours
   */
  Vector<Integer> fillDailyNonDominantLoadInHours ()
  {

    // Creating Auxiliary Variables
    Vector<Integer> v =
      new Vector<Integer>(OfficeComplexConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < OfficeComplexConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum =
        dailyNonDominantLoad.get(i * OfficeComplexConstants.QUARTERS_OF_HOUR)
                + dailyNonDominantLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 1)
                + dailyNonDominantLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 2)
                + dailyNonDominantLoad
                        .get(i * OfficeComplexConstants.QUARTERS_OF_HOUR + 3);
      v.add(sum);
    }
    return v;
  }

  /**
   * At the end of each week the office models refresh their schedule. This way
   * we have a realistic and dynamic model, changing function hours, consuming
   * power and so on.
   * 
   * @param conf
   */
  void refresh (Properties conf)
  {

    // For each member of the office
    for (Person member: members) {
      member.refresh(conf);
    }

    // For each appliance of the office
    for (Appliance appliance: appliances) {
      appliance.refresh();
    }

    for (int i = 0; i < OfficeComplexConstants.DAYS_OF_WEEK; i++) {
      dailyBaseLoad =
        fillDailyBaseLoad(week * OfficeComplexConstants.DAYS_OF_WEEK + i);
      dailyControllableLoad =
        fillDailyControllableLoad(week * OfficeComplexConstants.DAYS_OF_WEEK
                                  + i);
      dailyWeatherSensitiveLoad =
        fillDailyWeatherSensitiveLoad(week
                                      * OfficeComplexConstants.DAYS_OF_WEEK + i);
      weeklyBaseLoad.add(dailyBaseLoad);
      weeklyControllableLoad.add(dailyControllableLoad);
      weeklyWeatherSensitiveLoad.add(dailyWeatherSensitiveLoad);

      dailyBaseLoadInHours = fillDailyBaseLoadInHours();
      dailyControllableLoadInHours = fillDailyControllableLoadInHours();
      dailyWeatherSensitiveLoadInHours = fillDailyWeatherSensitiveLoadInHours();
      weeklyBaseLoadInHours.add(dailyBaseLoadInHours);
      weeklyControllableLoadInHours.add(dailyControllableLoadInHours);
      weeklyWeatherSensitiveLoadInHours.add(dailyWeatherSensitiveLoadInHours);
    }

  }

  /**
   * This function is checking the current weather conditions and the existence
   * of weather sensitive appliances and if the temperature is over/under a
   * certain threshold, the appliances begin or stop their operation.
   */
  public void weatherCheck (int day, int hour, Instant now, double temperature)
  {
    boolean flag = false;

    for (Appliance appliance: appliances) {

      if ((appliance instanceof AirCondition) && (flag == false)) {

        appliance.weatherDailyFunction(day, hour, temperature);

        if ((appliance.getWeeklyLoadVector().get(day)
                .get(hour * OfficeComplexConstants.QUARTERS_OF_HOUR) > 0)
            || (appliance.getWeeklyLoadVector().get(day)
                    .get(hour * OfficeComplexConstants.QUARTERS_OF_HOUR + 1) > 0)
            || (appliance.getWeeklyLoadVector().get(day)
                    .get(hour * OfficeComplexConstants.QUARTERS_OF_HOUR + 2) > 0)
            || (appliance.getWeeklyLoadVector().get(day)
                    .get(hour * OfficeComplexConstants.QUARTERS_OF_HOUR + 3) > 0)) {

          // log.debug("Changed Air Condition indeed");
          dailyWeatherSensitiveLoad = fillDailyWeatherSensitiveLoad(day);
          weeklyWeatherSensitiveLoad.set(day, dailyWeatherSensitiveLoad);
          dailyWeatherSensitiveLoadInHours =
            fillDailyWeatherSensitiveLoadInHours();
          weeklyWeatherSensitiveLoadInHours
                  .set(day, dailyWeatherSensitiveLoadInHours);

        }
      }
    }

  }

  /**
   * This is the function that takes every appliance in the office and reads
   * the shifted Controllable Consumption for the needs of the tariff
   * evaluation.
   * 
   * @param tariff
   * @param nonDominantLoad
   * @param tariffEvalHelper
   * @param day
   * @param start
   * @return TODO
   */
  double[] dailyShifting (Tariff tariff, double[] nonDominantLoad,
                          TariffEvaluationHelper tariffEvalHelper, int day,
                          Instant start)
  {

    double[] dominantLoad = new double[OfficeComplexConstants.HOURS_OF_DAY];

    Appliance appliance = appliances.get(dominantAppliance);

    if (appliance.getOverallPower() != -1)
      dominantLoad =
        appliance.dailyShifting(tariff, nonDominantLoad,
                                tariffEvalHelper, day, start);

    log.debug("Dominant Appliance " + appliance.toString() + " Overall Power: "
              + appliance.getOverallPower());
    log.debug("New Dominant Load: " + Arrays.toString(dominantLoad));

    return dominantLoad;
  }

  // public void test ()
  // {
  // for (Appliance appliance: appliances)
  // appliance.test();
  //
  // }

  /**
   * This function prints to the screen the daily load of the office for the
   * weekday at hand.
   * 
   * @param day
   */
  public void printDailyLoad (int day)
  {
    ListIterator<Integer> iter = weeklyBaseLoadInHours.get(day).listIterator();
    ListIterator<Integer> iter2 =
      weeklyControllableLoadInHours.get(day).listIterator();
    ListIterator<Integer> iter3 =
      weeklyWeatherSensitiveLoadInHours.get(day).listIterator();
    log.info("Summary of Daily Load of House " + name);
    for (int j = 0; j < OfficeComplexConstants.HOURS_OF_DAY; j++)
      log.info("Hour : " + j + 1 + " Base Load : " + iter.next()
               + " Controllable Load : " + iter2.next()
               + " Weather Sensitive Load : " + iter3.next());
  }

  @Override
  public String toString ()
  {
    return name;
  }

}
