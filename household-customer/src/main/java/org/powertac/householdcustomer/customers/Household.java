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
package org.powertac.householdcustomer.customers;

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
import org.powertac.householdcustomer.appliances.AirCondition;
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
import org.powertac.householdcustomer.appliances.WeatherSensitiveAppliance;
import org.powertac.householdcustomer.configurations.VillageConstants;
import org.powertac.householdcustomer.enumerations.Status;
import org.powertac.householdcustomer.persons.MostlyPresentPerson;
import org.powertac.householdcustomer.persons.PeriodicPresentPerson;
import org.powertac.householdcustomer.persons.Person;
import org.powertac.householdcustomer.persons.RandomlyAbsentPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The household is the domain instance represents a single house with the
 * tenants living inside it and fully equipped with appliances statistically
 * distributed. There are different kinds of appliances utilized by the persons
 * inhabiting the premises and each person living has it's own schedule.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Household
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static protected Logger log = LogManager.getLogger(Household.class.getName());

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  int seedId = 1;

  /**
   * The household name. It is different for each one to be able to tell them
   * apart.
   */
  String name;

  /**
   * This is a vector containing each day's base, controllable and weather
   * sensitive load from the appliances installed inside the household.
   **/
  Vector<Integer> dailyBaseLoad = new Vector<Integer>();
  Vector<Integer> dailyControllableLoad = new Vector<Integer>();
  Vector<Integer> dailyWeatherSensitiveLoad = new Vector<Integer>();
  Vector<Integer> dailyNonDominantLoad = new Vector<Integer>();
  Vector<Integer> dailyDominantLoad = new Vector<Integer>();

  /**
   * This is a vector containing the base, controllable and weather sensitive
   * load from the appliances installed inside the household for all the week
   * days.
   **/
  Vector<Vector<Integer>> weeklyBaseLoad = new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyControllableLoad =
    new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyWeatherSensitiveLoad =
    new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyNonDominantLoad = new Vector<Vector<Integer>>();
  Vector<Vector<Integer>> weeklyDominantLoad = new Vector<Vector<Integer>>();

  /**
   * This is an aggregated vector containing each day's base, controllable and
   * weather sensitive load in hours.
   **/
  Vector<Integer> dailyBaseLoadInHours = new Vector<Integer>();
  Vector<Integer> dailyControllableLoadInHours = new Vector<Integer>();
  Vector<Integer> dailyWeatherSensitiveLoadInHours = new Vector<Integer>();
  Vector<Integer> dailyNonDominantLoadInHours = new Vector<Integer>();
  Vector<Integer> dailyDominantLoadInHours = new Vector<Integer>();

  /**
   * This is an aggregated vector containing the weekly base, controllable and
   * weather sensitive load in hours.
   **/
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
   * This is a vector containing the members of the household, the persons that
   * belong to each household.
   */
  Vector<Person> members = new Vector<Person>();

  /**
   * This is a vector containing the appliances installed in the household.
   */
  Vector<Appliance> appliances = new Vector<Appliance>();

  /**
   * This is the index of most power consuming appliance of the household.
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
  double[] dominantConsumption = new double[VillageConstants.HOURS_OF_DAY];
  double[] nonDominantConsumption = new double[VillageConstants.HOURS_OF_DAY];

  /**
   * This variable is pointing to the village that this household is part of.
   */
  public Village householdOf;

  /**
   * This variable is utilized for the creation of the RandomSeed numbers and is
   * taken from the service.
   */
  RandomSeed gen;

  /**
   * This is the initialization function. It uses the variable values for the
   * configuration file to create the household and then fill it with persons
   * and appliances as it seems fit.
   * 
   * @param HouseName
   * @param conf
   * @param publicVacationVector
   * @param seed
   */
  public void initialize (String HouseName, Properties conf,
                          Vector<Integer> publicVacationVector, int seed)
  {
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    double va = Double.parseDouble(conf.getProperty("VacationAbsence"));
    name = HouseName;

    gen =
      randomSeedRepo.getRandomSeed(toString(), seed, "Household Model" + seed);

    int persons = memberRandomizer(conf);
    for (int i = 0; i < persons; i++)
      addPerson(i + 1, conf, publicVacationVector);

    for (Person member: members) {
      for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++) {
        member.fillDailyRoutine(i, va);
        member.getWeeklyRoutine().add(member.getDailyRoutine());
        member.setMemberOf(this);
      }
      // member.showInfo();
    }

    fillAppliances(conf);

    for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++) {
      dailyBaseLoad =
        fillDailyBaseLoad(week * VillageConstants.DAYS_OF_WEEK + i);
      dailyControllableLoad =
        fillDailyControllableLoad(week * VillageConstants.DAYS_OF_WEEK + i);
      dailyWeatherSensitiveLoad =
        fillDailyWeatherSensitiveLoad(week * VillageConstants.DAYS_OF_WEEK + i);
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

    for (week = 1; week < VillageConstants.WEEKS_OF_COMPETITION
                          + VillageConstants.WEEKS_OF_BOOTSTRAP; week++) {
      refresh(conf);
    }

    for (Appliance appliance: appliances) {
      if (appliance instanceof Dryer) {
        appliance.setOperationDays();
        appliance.calculateOverallPower();
      }
    }

    for (Appliance appliance: appliances) {
      if (!(appliance instanceof Dryer)) {
        appliance.setOperationDays();
        appliance.calculateOverallPower();
      }
    }

    findDominantAppliance();
    if (getDominantAppliance().getOverallPower() != 1)
      createDominantOperationVectors();

    int overallDays =
      (VillageConstants.WEEKS_OF_COMPETITION + VillageConstants.WEEKS_OF_BOOTSTRAP)
              * VillageConstants.DAYS_OF_WEEK;

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
    
    System.out.println(this.toString() + " Dominant Appliance: "
                       + getDominantAppliance()
                       + " Overall Power Consumption: "
                       + getDominantAppliance().getOverallPower());
    
    System.out.println(this.toString() + "  " + weeklyBaseLoad.size());
    System.out.println(this.toString() + "  " + weeklyControllableLoad.size());
    System.out.println(this.toString() + "  "
                       + weeklyWeatherSensitiveLoad.size());
    System.out.println(this.toString() + "  " + weeklyNonDominantLoad.size());
    System.out.println(this.toString() + "  " + weeklyDominantLoad.size());
    System.out.println(this.toString() + "  " + weeklyBaseLoadInHours.size());
    System.out.println(this.toString() + "  "
                       + weeklyControllableLoadInHours.size());
    System.out.println(this.toString() + "  "
                       + weeklyWeatherSensitiveLoadInHours.size());
    System.out.println(this.toString() + "  "
                       + weeklyNonDominantLoadInHours.size());
    System.out.println(this.toString() + "  "
                       + weeklyDominantLoadInHours.size());
             
    System.out.println(this.toString() + "  "
                       + weeklyDominantLoad.get(0).toString());
    System.out.println(this.toString() + "  "
                       + weeklyNonDominantLoad.get(0).toString());
    */
  }

  /**
   * This function is creating a RandomSeed number of person (given by the next
   * function) and add them to the current household, filling it up with life.
   * 
   * @param counter
   * @param conf
   * @param publicVacationVector
   * @param gen
   * @return
   */
  void addPerson (int counter, Properties conf,
                  Vector<Integer> publicVacationVector)
  {
    // Taking parameters from configuration file
    int pp = Integer.parseInt(conf.getProperty("PeriodicPresent"));
    int mp = Integer.parseInt(conf.getProperty("MostlyPresent"));

    int x = gen.nextInt(VillageConstants.PERCENTAGE);
    if (x < pp) {
      PeriodicPresentPerson ppp = new PeriodicPresentPerson();
      ppp.initialize(toString() + "PPP" + counter, conf, publicVacationVector,
                     seedId++);
      members.add(ppp);

    }
    else {
      if (x >= pp & x < (pp + mp)) {
        MostlyPresentPerson mpp = new MostlyPresentPerson();
        mpp.initialize(toString() + "MPP" + counter, conf,
                       publicVacationVector, seedId++);
        members.add(mpp);
      }
      else {
        RandomlyAbsentPerson rap = new RandomlyAbsentPerson();
        rap.initialize(toString() + "RAP" + counter, conf,
                       publicVacationVector, seedId++);
        members.add(rap);
      }
    }
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

      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
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

    for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++) {
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
   * This is a function that returns the week of refresh.
   */
  public int getWeek ()
  {
    return week;
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
   * persons in a household and gives back a number randomly.
   * 
   * @param conf
   * @param gen
   * @return
   */
  int memberRandomizer (Properties conf)
  {
    int one = Integer.parseInt(conf.getProperty("OnePerson"));
    int two = Integer.parseInt(conf.getProperty("TwoPersons"));
    int three = Integer.parseInt(conf.getProperty("ThreePersons"));
    int four = Integer.parseInt(conf.getProperty("FourPersons"));
    int returnValue;

    int x = gen.nextInt(VillageConstants.PERCENTAGE);
    if (x < one) {
      returnValue = VillageConstants.ONE_PERSON;
    }
    else {
      if (x >= one & x < (one + two)) {
        returnValue = VillageConstants.TWO_PERSONS;
      }
      else {
        if (x >= (one + two) & x < (one + two + three)) {
          returnValue = VillageConstants.THREE_PERSONS;
        }
        else {
          if (x >= (one + two + three) & x < (one + two + three + four)) {
            returnValue = VillageConstants.FOUR_PERSONS;
          }
          else {
            returnValue = VillageConstants.FIVE_PERSONS;
          }
        }
      }
    }
    return returnValue;
  }

  /**
   * This function is using the appliance's saturation in order to make a
   * possibility check and install or not the appliance in the current
   * household.
   * 
   * @param app
   * @param gen
   * @return
   */
  void checkProbability (Appliance app)
  {
    // Creating auxiliary variables

    int x = gen.nextInt(VillageConstants.PERCENTAGE);

    int threshold = (int) (app.getSaturation() * VillageConstants.PERCENTAGE);
    if (x < threshold) {
      app.fillWeeklyOperation();
      app.createWeeklyPossibilityOperationVector();
    }
    else
      this.appliances.remove(app);
  }

  /**
   * This function is responsible for the filling of the household with the
   * appliances and their schedule for the first week using a statistic formula
   * and the members of the household.
   * 
   * @param conf
   * @param gen
   * @return
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

    // Others
    Others others = new Others();
    appliances.add(others);
    others.setApplianceOf(this);
    others.initialize(this.name, conf, seedId++);
    others.fillWeeklyOperation();
    others.createWeeklyPossibilityOperationVector();

    // Circulation Pump
    CirculationPump cp = new CirculationPump();
    cp.setApplianceOf(this);
    appliances.add(cp);
    cp.initialize(this.name, conf, seedId++);
    checkProbability(cp);

    // FULLY SHIFTING ================================

    // Refrigerator
    Refrigerator ref = new Refrigerator();
    appliances.add(ref);
    ref.setApplianceOf(this);
    ref.initialize(this.name, conf, seedId++);
    ref.fillWeeklyOperation();
    ref.createWeeklyPossibilityOperationVector();

    // Freezer
    Freezer fr = new Freezer();
    appliances.add(fr);
    fr.setApplianceOf(this);
    fr.initialize(this.name, conf, seedId++);
    checkProbability(fr);

    // Space Heater
    SpaceHeater sh = new SpaceHeater();
    appliances.add(sh);
    sh.setApplianceOf(this);
    sh.initialize(this.name, conf, seedId++);
    checkProbability(sh);

    // Water Heater
    WaterHeater wh = new WaterHeater();
    appliances.add(wh);
    wh.setApplianceOf(this);
    wh.initialize(this.name, conf, seedId++);
    checkProbability(wh);

    // SEMI SHIFTING ================================

    // Dishwasher
    Dishwasher dw = new Dishwasher();
    appliances.add(dw);
    dw.setApplianceOf(this);
    dw.initialize(this.name, conf, seedId++);
    checkProbability(dw);

    // Stove
    Stove st = new Stove();
    appliances.add(st);
    st.setApplianceOf(this);
    st.initialize(this.name, conf, seedId++);
    checkProbability(st);

    // Washing Machine
    WashingMachine wm = new WashingMachine();
    appliances.add(wm);
    wm.setApplianceOf(this);
    wm.initialize(this.name, conf, seedId++);
    wm.fillWeeklyOperation();
    wm.createWeeklyPossibilityOperationVector();

    // Dryer
    Dryer dr = new Dryer();
    appliances.add(dr);
    dr.setApplianceOf(this);
    dr.initialize(this.name, conf, seedId++);
    checkProbability(dr);

  }

  /**
   * This function checks if all the inhabitants of the household are out of the
   * household.
   * 
   * @param weekday
   * @param quarter
   * @return true if all the inhabitants of the household are out of the
   * household
   */
  public boolean isEmpty (int weekday, int quarter)
  {
    boolean x = true;
    for (Person member: members) {
      if (member.getWeeklyRoutine()
              .get(week * VillageConstants.DAYS_OF_WEEK + weekday).get(quarter) == Status.Normal
          || member.getWeeklyRoutine()
                  .get(week * VillageConstants.DAYS_OF_WEEK + weekday)
                  .get(quarter) == Status.Sick) {
        x = false;
      }
    }
    return x;
  }

  /**
   * This function checks the number of tenants in the house in a specific
   * quarter, either sick or normal.
   * 
   * @param weekday
   * @param quarter
   * @return the number of tenants in the house in a specific quarter
   */
  public int tenantsNumber (int weekday, int quarter)
  {
    int counter = 0;
    for (Person member: members) {
      if (member.getWeeklyRoutine()
              .get(week * VillageConstants.DAYS_OF_WEEK + weekday).get(quarter) == Status.Normal
          || member.getWeeklyRoutine()
                  .get(week * VillageConstants.DAYS_OF_WEEK + weekday)
                  .get(quarter) == Status.Sick)
        counter++;
    }
    return counter;
  }

  /**
   * This is the function utilized to show the information regarding the
   * household in question, its variables values etc.
   */
  void showStatus ()
  {

    // Printing basic variables
    log.info("HouseHold Name : " + name);
    log.info("Number of Persons : " + members.size());

    // Printing daily load
    log.info(" Daily Load = ");
    for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION
                        + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
      log.info("Day " + i);
      ListIterator<Integer> iter2 = weeklyBaseLoad.get(i).listIterator();
      ListIterator<Integer> iter3 =
        weeklyControllableLoad.get(i).listIterator();
      ListIterator<Integer> iter4 =
        weeklyWeatherSensitiveLoad.get(i).listIterator();
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++)
        log.info("Quarter : " + j + " Base Load : " + iter2.next()
                 + " Controllable Load: " + iter3.next()
                 + " WeatherSensitive Load: " + iter4.next());
    }

    // Printing daily load in hours
    log.info(" Load In Hours = ");
    for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION
                        + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
      log.info("Day " + i);
      ListIterator<Integer> iter2 = weeklyBaseLoadInHours.get(i).listIterator();
      ListIterator<Integer> iter3 =
        weeklyControllableLoadInHours.get(i).listIterator();
      ListIterator<Integer> iter4 =
        weeklyWeatherSensitiveLoadInHours.get(i).listIterator();
      for (int j = 0; j < VillageConstants.HOURS_OF_DAY; j++)
        log.info("Hours : " + j + " Base Load : " + iter2.next()
                 + " Controllable Load: " + iter3.next()
                 + " WeatherSensitive Load: " + iter4.next());
    }
  }

  /**
   * This function is used in order to fill the daily Base Load of the household
   * for each quarter of the hour.
   * 
   * @param weekday
   * @return daily base load
   */
  Vector<Integer> fillDailyBaseLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v = new Vector<Integer>(VillageConstants.QUARTERS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
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
   * household for each quarter of the hour.
   * 
   * @param weekday
   * @return daily controllable load
   */
  Vector<Integer> fillDailyControllableLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v = new Vector<Integer>(VillageConstants.QUARTERS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
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
   * the household for each quarter of the hour.
   * 
   * @param weekday
   * @return daily weather sensitive load
   */
  Vector<Integer> fillDailyWeatherSensitiveLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v = new Vector<Integer>(VillageConstants.QUARTERS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
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
   * @param weekday
   * @return daily dominant load
   */
  Vector<Integer> fillDailyDominantLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v = new Vector<Integer>(VillageConstants.QUARTERS_OF_DAY);
    int sum = 0;
    int helpIndex = -1;

    // Case of Washing Machine as dominant Appliance
    Appliance app = appliances.get(dominantAppliance);
    if (app instanceof WashingMachine) {
      WashingMachine wm = (WashingMachine) app;
      // Case there is dryer
      if (wm.getDryerFlag())
        helpIndex = wm.getDryerIndex();
    }

    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      if (appliances.get(dominantAppliance).getOverallPower() != -1) {
        sum =
          appliances.get(dominantAppliance).getWeeklyLoadVector().get(day)
                  .get(i);

        if (helpIndex != -1)
          sum +=
            appliances.get(helpIndex).getWeeklyLoadVector().get(day).get(i);
      }
      v.add(sum);
    }

    return v;
  }

  /**
   * This function is used in order to fill the daily non dominant load of
   * the household for each quarter of the hour.
   * 
   * @param weekday
   * @return daily non-dominant load
   */
  Vector<Integer> fillDailyNonDominantLoad (int day)
  {
    // Creating auxiliary variables
    Vector<Integer> v = new Vector<Integer>(VillageConstants.QUARTERS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
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
   * This function checks if all the inhabitants of the household are away on
   * vacation on a certain day
   * 
   * @param day
   * @return true if all inhabitants are on vacation
   */
  public boolean isOnVacation (int day)
  {
    boolean x = true;
    for (Person member: members) {
      if (member.getWeeklyRoutine().get(day).get(0) != Status.Vacation)
        x = false;
    }
    return x;
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
    Vector<Integer> v = new Vector<Integer>(VillageConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum =
        dailyBaseLoad.get(i * VillageConstants.QUARTERS_OF_HOUR)
                + dailyBaseLoad.get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                + dailyBaseLoad.get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                + dailyBaseLoad.get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
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
    Vector<Integer> v = new Vector<Integer>(VillageConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum =
        dailyControllableLoad.get(i * VillageConstants.QUARTERS_OF_HOUR)
                + dailyControllableLoad.get(i
                                            * VillageConstants.QUARTERS_OF_HOUR
                                            + 1)
                + dailyControllableLoad.get(i
                                            * VillageConstants.QUARTERS_OF_HOUR
                                            + 2)
                + dailyControllableLoad.get(i
                                            * VillageConstants.QUARTERS_OF_HOUR
                                            + 3);
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
    Vector<Integer> v = new Vector<Integer>(VillageConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum =
        dailyWeatherSensitiveLoad.get(i * VillageConstants.QUARTERS_OF_HOUR)
                + dailyWeatherSensitiveLoad
                        .get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
                + dailyWeatherSensitiveLoad
                        .get(i * VillageConstants.QUARTERS_OF_HOUR + 2)
                + dailyWeatherSensitiveLoad
                        .get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
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
    Vector<Integer> v = new Vector<Integer>(VillageConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum =
        dailyDominantLoad.get(i * VillageConstants.QUARTERS_OF_HOUR)
                + dailyDominantLoad.get(i * VillageConstants.QUARTERS_OF_HOUR
                                        + 1)
                + dailyDominantLoad.get(i * VillageConstants.QUARTERS_OF_HOUR
                                        + 2)
                + dailyDominantLoad.get(i * VillageConstants.QUARTERS_OF_HOUR
                                        + 3);
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
    Vector<Integer> v = new Vector<Integer>(VillageConstants.HOURS_OF_DAY);
    int sum = 0;
    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      sum = 0;
      sum =
        dailyNonDominantLoad.get(i * VillageConstants.QUARTERS_OF_HOUR)
                + dailyNonDominantLoad.get(i
                                           * VillageConstants.QUARTERS_OF_HOUR
                                           + 1)
                + dailyNonDominantLoad.get(i
                                           * VillageConstants.QUARTERS_OF_HOUR
                                           + 2)
                + dailyNonDominantLoad.get(i
                                           * VillageConstants.QUARTERS_OF_HOUR
                                           + 3);
      v.add(sum);
    }
    return v;
  }

  /**
   * At the end of each week the household models refresh their schedule. This
   * way we have a realistic and dynamic model, changing function hours,
   * consuming power and so on.
   * 
   * @param conf
   */
  void refresh (Properties conf)
  {

    // For each member of the household
    for (Person member: members) {
      member.refresh(conf);
    }

    // For each appliance of the household
    for (Appliance appliance: appliances) {
      if (!(appliance instanceof Dryer))
        appliance.refresh();

    }

    for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++) {
      dailyBaseLoad =
        fillDailyBaseLoad(week * VillageConstants.DAYS_OF_WEEK + i);
      dailyControllableLoad =
        fillDailyControllableLoad(week * VillageConstants.DAYS_OF_WEEK + i);
      dailyWeatherSensitiveLoad =
        fillDailyWeatherSensitiveLoad(week * VillageConstants.DAYS_OF_WEEK + i);
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
   * TODO - JEC - this code should be in the individual appliances.
   */
  public void weatherCheck (int day, int hour, Instant now, double temperature)
  {
    boolean flag = false;

    for (Appliance appliance: appliances) {

      if (appliance instanceof SpaceHeater && hour == 23
          && (day + 1 < VillageConstants.DAYS_OF_COMPETITION)) {

        appliance.weatherDailyOperation(day + 1, 0, temperature);

        if (appliance.getWeeklyLoadVector().get(day + 1).get(0) > 0) {
          // log.debug("Changed Space Heater indeed");

          dailyWeatherSensitiveLoad = fillDailyWeatherSensitiveLoad(day + 1);
          weeklyWeatherSensitiveLoad.set(day + 1, dailyWeatherSensitiveLoad);
          dailyWeatherSensitiveLoadInHours =
            fillDailyWeatherSensitiveLoadInHours();
          weeklyWeatherSensitiveLoadInHours
                  .set(day + 1, dailyWeatherSensitiveLoadInHours);
          flag = true;
        }
      }

      if ((appliance instanceof AirCondition) && (flag == false)) {

        appliance.weatherDailyOperation(day, hour, temperature);

        if ((appliance.getWeeklyLoadVector().get(day)
                .get(hour * VillageConstants.QUARTERS_OF_HOUR) > 0)
            || (appliance.getWeeklyLoadVector().get(day)
                    .get(hour * VillageConstants.QUARTERS_OF_HOUR + 1) > 0)
            || (appliance.getWeeklyLoadVector().get(day)
                    .get(hour * VillageConstants.QUARTERS_OF_HOUR + 2) > 0)
            || (appliance.getWeeklyLoadVector().get(day)
                    .get(hour * VillageConstants.QUARTERS_OF_HOUR + 3) > 0)) {

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

  // public void test ()
  // {
  //
  // for (Appliance appliance: appliances)
  // appliance.test();
  //
  // }

  /**
   * This is the function that takes every appliance in the household and reads
   * the shifted Controllable Consumption for the needs of the tariff
   * evaluation.
   * 
   * @param tariff
   * @param nonDominantLoad
   * @param tariffEvalHelper
   * @param day
   * @param gen
   * @param start
   * @return TODO
   */
  double[] dailyShifting (Tariff tariff, double[] nonDominantLoad,
                          TariffEvaluationHelper tariffEvalHelper, int day,
                          RandomSeed gen, Instant start)
  {

    double[] dominantLoad = new double[VillageConstants.HOURS_OF_DAY];

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

  @Override
  public String toString ()
  {
    return name;
  }

}
