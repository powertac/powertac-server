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
 * This is the instance of the person type that spents most of its time inside
 * the house. Such types are children or elderly people. These persons don't
 * work at all, so they have more time for leisure activities.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class MostlyPresentPerson extends Person
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
    double MPLeisure = Double.parseDouble(conf.getProperty("MPLeisure"));

    // Filling the main variables
    name = AgentName;
    status = Status.Normal;
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    gen = randomSeedRepo.getRandomSeed(toString(), seed, "Person Model" + seed);
    sicknessVector = createSicknessVector(sicknessMean, sicknessDev);
    this.publicVacationVector = publicVacationVector;
    int x = (int) (gen.nextGaussian() + MPLeisure);
    leisureVector = createLeisureVector(x);
    leisureDuration =
      (int) (leisureDurationDev * gen.nextGaussian() + leisureDurationMean);
  }

  @Override
  public void showInfo ()
  {
    // Printing base variables
    log.debug("Name = " + name);
    log.debug("Member Of = " + memberOf.toString());

    // Printing Sickness variables
    log.debug("Sickness Days = ");
    ListIterator<Integer> iter = sicknessVector.listIterator();
    while (iter.hasNext())
      log.debug(iter.next());

    // Printing Leisure variables
    log.info("Leisure Days of Week = ");
    iter = leisureVector.listIterator();
    while (iter.hasNext())
      log.debug(iter.next());
    log.info("Leisure Duration = " + leisureDuration);

    // Printing Public Vacation Variables
    log.debug("Public Vacation of Year = ");
    iter = publicVacationVector.listIterator();
    while (iter.hasNext())
      log.debug(iter.next());

    // Printing Weekly Schedule
    log.debug("Weekly Routine Length : " + weeklyRoutine.size());
    log.debug("Weekly Routine : ");

    for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION
                        + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
      log.debug("Day " + (i));
      ListIterator<Status> iter2 = weeklyRoutine.get(i).listIterator();
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++)
        log.debug("Quarter : " + (j + 1) + " Status : " + iter2.next());
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
    double MPLeisure = Double.parseDouble(conf.getProperty("MPLeisure"));
    double vacationAbsence =
      Double.parseDouble(conf.getProperty("VacationAbsence"));

    int x = (int) (gen.nextGaussian() + MPLeisure);
    leisureDuration =
      (int) (leisureDurationDev * gen.nextGaussian() + leisureDurationMean);
    leisureVector = createLeisureVector(x);
    for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++) {
      fillDailyRoutine(i, vacationAbsence);
      weeklyRoutine.add(dailyRoutine);
    }
  }
}
