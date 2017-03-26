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

package org.powertac.householdcustomer.appliances;

import java.util.Properties;
import java.util.Vector;

import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.householdcustomer.configurations.VillageConstants;

/**
 * Consumer Electronics are the appliances that are utilized mainly for work or
 * entertainment in the household such as TV, DVD Players, Stereos and so on.
 * They works only when someone is at home. So it's a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class ConsumerElectronics extends NotShiftingAppliance
{

  @Override
  public void initialize (String household, Properties conf, int seed)
  {

    // Filling the base variables
    name = household + " ConsumerElectronics";
    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    gen =
      randomSeedRepo.getRandomSeed(toString(), seed, "Appliance Model" + seed);
    saturation = 1;
    power =
      (int) (VillageConstants.CONSUMER_ELECTRONICS_POWER_VARIANCE
             * gen.nextGaussian() + VillageConstants.CONSUMER_ELECTRONICS_POWER_MEAN);
    cycleDuration = VillageConstants.CONSUMER_ELECTRONICS_DURATION_CYCLE;
    times =
      Integer.parseInt(conf.getProperty("ConsumerElectronicsDailyTimes"))
              + applianceOf.getMembers().size();

  }

  @Override
  public void fillDailyOperation (int weekday)
  {
    // Initializing and Creating auxiliary variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {

      dailyOperation.add(false);
      loadVector.add(0);

    }

    Vector<Integer> temp = new Vector<Integer>();

    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      int count = applianceOf.tenantsNumber(weekday, i);
      for (int j = 0; j < count; j++) {
        temp.add(i);
      }

    }

    if (temp.size() > 0) {
      for (int i = 0; i < times; i++) {
        int rand = gen.nextInt(temp.size());
        int quarter = temp.get(rand);

        dailyOperation.set(quarter, true);
        loadVector.set(quarter, (loadVector.get(quarter) + power));
        temp.remove(rand);
        if (temp.size() == 0)
          break;
      }
    }

    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  @Override
  public void refresh ()
  {
    fillWeeklyOperation();
    createWeeklyPossibilityOperationVector();
  }

}
