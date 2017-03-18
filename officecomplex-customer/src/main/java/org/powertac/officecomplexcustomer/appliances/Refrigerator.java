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

package org.powertac.officecomplexcustomer.appliances;

import java.util.Properties;
import java.util.Vector;

import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;

/**
 * Refrigerator is in use in all the households in order to keep food preserved.
 * This appliance can automatically change the freezing cycles in order to save
 * energy, without problem without tenants manipulation. So this is a fully
 * shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Refrigerator extends FullyShiftingAppliance
{

  @Override
  public void initialize (String office, Properties conf, int seed)
  {

    // Filling the base variables
    name = office + " Refrigerator";
    saturation = Double.parseDouble(conf.getProperty("RefrigeratorSaturation"));

    randomSeedRepo =
      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    gen =
      randomSeedRepo.getRandomSeed(toString(), seed, "Appliance Model" + seed);
    power =
      (int) (OfficeComplexConstants.REFRIGERATOR_POWER_VARIANCE
             * gen.nextGaussian() + OfficeComplexConstants.REFRIGERATOR_POWER_MEAN);
    cycleDuration = OfficeComplexConstants.REFRIGERATOR_DURATION_CYCLE;
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // Freezer can work anytime
    for (int j = 0; j < OfficeComplexConstants.QUARTERS_OF_DAY; j++) {
      possibilityDailyOperation.add(true);
    }

    return possibilityDailyOperation;
  }

  @Override
  public void fillDailyOperation (int weekday)
  {
    // Initializing Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    int k = gen.nextInt(cycleDuration);

    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      if (i % cycleDuration == k) {
        loadVector.add(power);
        dailyOperation.add(true);
      }
      else {
        loadVector.add(0);
        dailyOperation.add(false);
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
