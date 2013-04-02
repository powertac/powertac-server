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

import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.householdcustomer.configurations.VillageConstants;
import org.powertac.householdcustomer.enumerations.Status;

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
  public void initialize (String AgentName, Properties conf,
                          Vector<Integer> publicVacationVector, int seed)
  {
    // Variables Taken from the configuration file
    double sicknessMean = Double.parseDouble(conf.getProperty("SicknessMean"));
    double sicknessDev = Double.parseDouble(conf.getProperty("SicknessDev"));
    double leisureDurationMean =
      Double.parseDouble(conf.getProperty("LeisureDurationMean"));
    double leisureDurationDev =
      Double.parseDouble(conf.getProperty("LeisureDurationDev"));
    double PPLeisure = Double.parseDouble(conf.getProperty("PPLeisure"));
    double workingDurationMean =
      Double.parseDouble(conf.getProperty("WorkingDurationMean"));
    double workingDurationDev =
      Double.parseDouble(conf.getProperty("WorkingDurationDev"));
    double vacationDurationMean =
      Double.parseDouble(conf.getProperty("VacationDurationMean"));
    double vacationDurationDev =
      Double.parseDouble(conf.getProperty("VacationDurationDev"));

    // Filling the main variables

    name = AgentName;
    status = Status.Normal;
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    gen = randomSeedRepo.getRandomSeed(toString(), seed, "Person Model" + seed);
    // Filling the sickness and public Vacation Vectors
    sicknessVector = createSicknessVector(sicknessMean, sicknessDev);
    this.publicVacationVector = publicVacationVector;
    // Filling the leisure variables
    int x = (int) (gen.nextGaussian() + PPLeisure);
    leisureVector = createLeisureVector(x);
    leisureDuration =
      (int) (leisureDurationDev * gen.nextGaussian() + leisureDurationMean);
    // Filling Working variables
    workingStartHour = VillageConstants.START_OF_WORK;
    int work = workingDaysRandomizer(conf);
    workingDays = createWorkingDaysVector(work);
    workingDuration =
      (int) (workingDurationDev * gen.nextGaussian() + workingDurationMean);
    // Filling Vacation Variables
    vacationDuration =
      (int) (vacationDurationDev * gen.nextGaussian() + vacationDurationMean);
    vacationVector = createVacationVector(Math.max(0, vacationDuration));
  }

  @Override
  void addLeisureWorking (int weekday)
  {
    // Create auxiliary variables
    ListIterator<Integer> iter = leisureVector.listIterator();
    Status st;

    // Check each day on leisure vector
    while (iter.hasNext()) {
      if (iter.next() == weekday) {
        int start = workingStartHour + workingDuration;
        int startq =
          gen.nextInt(Math.max(1, VillageConstants.LEISURE_END_WINDOW - start))
                  + start;
        for (int i = startq; i < startq + leisureDuration; i++) {
          st = Status.Leisure;
          dailyRoutine.set(i, st);
          if (i == VillageConstants.QUARTERS_OF_DAY - 1)
            break;
        }
      }
    }
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
  }

  @Override
  public void refresh (Properties conf)
  {
    // Renew Variables
    double leisureDurationMean =
      Double.parseDouble(conf.getProperty("LeisureDurationMean"));
    double leisureDurationDev =
      Double.parseDouble(conf.getProperty("LeisureDurationDev"));
    double PPLeisure = Double.parseDouble(conf.getProperty("PPLeisure"));
    double vacationAbsence =
      Double.parseDouble(conf.getProperty("VacationAbsence"));

    int x = (int) (gen.nextGaussian() + PPLeisure);
    leisureDuration =
      (int) (leisureDurationDev * gen.nextGaussian() + leisureDurationMean);
    leisureVector = createLeisureVector(x);
    for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++) {
      fillDailyRoutine(i, vacationAbsence);
      weeklyRoutine.add(dailyRoutine);
    }
  }

}
