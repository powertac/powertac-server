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
 * This is the instance of the person type that works in shifts that may vary
 * form week to week or from month to month. The consequence is that he has
 * little time for leisure activities.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 **/
public class RandomlyAbsentPerson extends WorkingPerson
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
    double RALeisure = Double.parseDouble(conf.getProperty("RALeisure"));
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
    int x = (int) (gen.nextGaussian() + RALeisure);
    leisureVector = createLeisureVector(x);
    leisureDuration =
      (int) (leisureDurationDev * gen.nextGaussian() + leisureDurationMean);
    // Filling Working variables
    int work = workingDaysRandomizer(conf);
    workingDays = createWorkingDaysVector(work);
    workingStartHour = createWorkingStartHour();
    workingDuration =
      (int) (workingDurationDev * gen.nextGaussian() + workingDurationMean);
    // Filling Vacation Variables
    vacationDuration =
      (int) (vacationDurationDev * gen.nextGaussian() + vacationDurationMean);
    vacationVector = createVacationVector(vacationDuration);
  }

  /**
   * This function selects the shift of the worker. There three different
   * shifts: 00:00 - 08:00 08:00 - 16:00 and 16:00 - 24:00.
   * 
   * @param gen
   * @return
   */
  int createWorkingStartHour ()
  {
    int x = gen.nextInt(VillageConstants.NUMBER_OF_SHIFTS);
    return (x * VillageConstants.HOURS_OF_SHIFT_WORK * VillageConstants.QUARTERS_OF_HOUR);
  }

  /**
   * This function fills out the leisure activities in the daily schedule of the
   * person in question.
   * 
   * @param weekday
   * @param gen
   * @return
   */
  void addLeisureWorking (int weekday)
  {
    // Create auxiliary variables
    ListIterator<Integer> iter = leisureVector.listIterator();
    Status st;
    while (iter.hasNext()) {
      if (iter.next() == weekday) {
        int start = workingStartHour + workingDuration;
        if (workingStartHour == VillageConstants.SHIFT_START_1
            && ((VillageConstants.LEISURE_WINDOW + 1) - start > 0)) {
          int startq =
            gen.nextInt((VillageConstants.LEISURE_WINDOW + 1) - start)
                    + (start + VillageConstants.SHIFT_START_2);
          for (int i = startq; i < startq + leisureDuration; i++) {
            st = Status.Leisure;
            dailyRoutine.set(i, st);
            if (i == VillageConstants.QUARTERS_OF_DAY - 1)
              break;
          }
        }
        else {
          if (workingStartHour == VillageConstants.SHIFT_START_2) {
            int startq =
              start
                      + gen.nextInt(VillageConstants.LEISURE_WINDOW_SHIFT
                                    - start);
            for (int i = startq; i < startq + leisureDuration; i++) {
              st = Status.Leisure;
              dailyRoutine.set(i, st);
              if (i == VillageConstants.QUARTERS_OF_DAY - 1)
                break;
            }
          }
          else {
            int startq =
              VillageConstants.SHIFT_START_2
                      + gen.nextInt(VillageConstants.SHIFT_START_3
                                    - (VillageConstants.LEISURE_WINDOW - 1));
            for (int i = startq; i < startq + leisureDuration; i++) {
              st = Status.Leisure;
              dailyRoutine.set(i, st);
              if (i == VillageConstants.QUARTERS_OF_DAY - 1)
                break;
            }
          }
        }
      }
    }
  }

  @Override
  void fillWork ()
  {
    // Create auxiliary variables
    Status st;
    if (workingStartHour == VillageConstants.SHIFT_START_1) {
      for (int i = VillageConstants.SHIFT_START_1; i < workingDuration; i++) {
        st = Status.Working;
        dailyRoutine.set(i, st);
      }
      for (int i = workingDuration; i < workingDuration
                                        + VillageConstants.SHIFT_START_2; i++) {
        st = Status.Sleeping;
        dailyRoutine.set(i, st);
      }
      for (int i = workingDuration + VillageConstants.SHIFT_START_2; i < VillageConstants.QUARTERS_OF_DAY; i++) {
        st = Status.Normal;
        dailyRoutine.set(i, st);
      }
    }
    else {
      if (workingStartHour == VillageConstants.SHIFT_START_2) {
        for (int i = VillageConstants.START_OF_SLEEPING_1; i < VillageConstants.END_OF_SLEEPING_1; i++) {
          st = Status.Sleeping;
          dailyRoutine.set(i, st);
        }
        for (int i = VillageConstants.END_OF_SLEEPING_1; i < VillageConstants.SHIFT_START_2; i++) {
          st = Status.Normal;
          dailyRoutine.set(i, st);
        }
        for (int i = VillageConstants.SHIFT_START_2; i < workingDuration
                                                         + VillageConstants.SHIFT_START_2; i++) {
          st = Status.Working;
          dailyRoutine.set(i, st);
        }
        for (int i = workingDuration + VillageConstants.SHIFT_START_2; i < VillageConstants.START_OF_SLEEPING_1; i++) {
          st = Status.Normal;
          dailyRoutine.set(i, st);
        }
        for (int i = VillageConstants.START_OF_SLEEPING_1; i < VillageConstants.QUARTERS_OF_DAY; i++) {
          st = Status.Sleeping;
          dailyRoutine.set(i, st);
        }
      }
      else {
        for (int i = VillageConstants.START_OF_SLEEPING_1; i < VillageConstants.END_OF_SLEEPING_1; i++) {
          st = Status.Sleeping;
          dailyRoutine.set(i, st);
        }
        for (int i = VillageConstants.END_OF_SLEEPING_1; i < VillageConstants.SHIFT_START_3; i++) {
          st = Status.Normal;
          dailyRoutine.set(i, st);
        }
        if (workingDuration > VillageConstants.HOURS_OF_SHIFT_WORK
                              * VillageConstants.QUARTERS_OF_HOUR) {
          for (int i = VillageConstants.SHIFT_START_3; i < VillageConstants.QUARTERS_OF_DAY; i++) {
            st = Status.Working;
            dailyRoutine.set(i, st);
          }
        }
        else {
          for (int i = VillageConstants.SHIFT_START_3; i < VillageConstants.SHIFT_START_3
                                                           + workingDuration; i++) {
            if (i >= VillageConstants.QUARTERS_OF_DAY)
              break;
            st = Status.Working;
            dailyRoutine.set(i, st);
          }
          for (int i = VillageConstants.SHIFT_START_3 + workingDuration; i < VillageConstants.QUARTERS_OF_DAY; i++) {
            st = Status.Sleeping;
            dailyRoutine.set(i, st);
          }
        }
      }
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
    double RALeisure = Double.parseDouble(conf.getProperty("RALeisure"));
    double vacationAbsence =
      Double.parseDouble(conf.getProperty("VacationAbsence"));

    int work = workingDaysRandomizer(conf);
    workingDays = createWorkingDaysVector(work);
    workingStartHour = createWorkingStartHour();

    int x = (int) (gen.nextGaussian() + RALeisure);
    leisureDuration =
      (int) (leisureDurationDev * gen.nextGaussian() + leisureDurationMean);
    leisureVector = createLeisureVector(x);

    for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++) {
      fillDailyRoutine(i, vacationAbsence);
      weeklyRoutine.add(dailyRoutine);
    }
  }

}
