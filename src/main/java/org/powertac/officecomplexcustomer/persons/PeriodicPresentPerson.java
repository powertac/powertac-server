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

import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;
import org.powertac.officecomplexcustomer.enumerations.Status;

/**
 * This is the instance of the person type that works in a regular basis for a
 * number of days in the week. The standard program gives space for some leisure
 * activities.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 **/
public class PeriodicPresentPerson extends WorkingPerson
{

  @Override
  public void initialize (String AgentName, Properties conf, Vector<Integer> publicVacationVector, Random generator)
  {
    // Variables Taken from the configuration file
    double sicknessMean = Double.parseDouble(conf.getProperty("SicknessMean"));
    double sicknessDev = Double.parseDouble(conf.getProperty("SicknessDev"));
    double workingDurationMean = Double.parseDouble(conf.getProperty("WorkingDurationMean"));
    double workingDurationDev = Double.parseDouble(conf.getProperty("WorkingDurationDev"));
    double breakDurationMean = Double.parseDouble(conf.getProperty("BreakDurationMean"));
    double breakDurationDev = Double.parseDouble(conf.getProperty("BreakDurationDev"));
    double vacationDurationMean = Double.parseDouble(conf.getProperty("VacationDurationMean"));
    double vacationDurationDev = Double.parseDouble(conf.getProperty("VacationDurationDev"));

    // Filling the main variables
    name = AgentName;
    status = Status.Home;
    gen = generator;

    // Filling the sickness and public Vacation Vectors
    sicknessVector = createSicknessVector(sicknessMean, sicknessDev, gen);
    this.publicVacationVector = publicVacationVector;

    // Filling Working variables
    workingStartHour = (int) (OfficeComplexConstants.START_OF_WORK_VARIANCE * gen.nextGaussian() + OfficeComplexConstants.START_OF_WORK_MEAN);
    int work = workingDaysRandomizer(conf, gen);
    workingDays = createWorkingDaysVector(work, gen);
    workingDuration = (int) (workingDurationDev * gen.nextGaussian() + workingDurationMean);
    breakDuration = (int) (breakDurationDev * gen.nextGaussian() + breakDurationMean);

    // Filling Vacation Variables
    vacationDuration = (int) (vacationDurationDev * gen.nextGaussian() + vacationDurationMean);
    vacationVector = createVacationVector(Math.max(0, vacationDuration), gen);
  }

  @Override
  void fillWork ()
  {
    // Create auxiliary variables
    Status st;
    for (int i = workingStartHour; i < workingStartHour + workingDuration; i++) {
      st = Status.Working;
      dailyRoutine.set(i, st);
    }

    int breakStartHour = (int) (OfficeComplexConstants.START_OF_BREAK_VARIANCE * gen.nextGaussian() + OfficeComplexConstants.START_OF_BREAK_MEAN);
    breakStartHour = Math.max(breakStartHour, workingStartHour);
    breakStartHour = Math.min(breakStartHour, workingStartHour + workingDuration);

    for (int i = breakStartHour; i < breakStartHour + breakDuration; i++) {
      st = Status.Break;
      dailyRoutine.set(i, st);
    }

  }

  @Override
  public void refresh (Properties conf, Random gen)
  {
    for (int i = 0; i < OfficeComplexConstants.DAYS_OF_WEEK; i++) {
      fillDailyRoutine(i, gen);
      weeklyRoutine.add(dailyRoutine);
    }
  }

}
