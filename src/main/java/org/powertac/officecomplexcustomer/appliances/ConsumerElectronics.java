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
import java.util.Random;
import java.util.Vector;

import org.powertac.officecomplexcustomer.configurations.OfficeComplexConstants;

/**
 * Consumer Electronics are the appliances that are utilized mainly for work or
 * enteratinment in the household such as TV, DVD Players, Stereos and so on.
 * They works only when someone is at home. So it's a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class ConsumerElectronics extends NotShiftingAppliance
{

  /**
   * This variable shows the possibility (%) that this appliance will be used
   * when workers are on break.
   */
  private double operationPercentage;

  @Override
  public void initialize (String office, Properties conf, Random gen)
  {
    // Filling the base variables
    name = office + " ConsumerElectronics";
    saturation = 1;
    power = (int) (OfficeComplexConstants.CONSUMER_ELECTRONICS_POWER_VARIANCE * gen.nextGaussian() + OfficeComplexConstants.CONSUMER_ELECTRONICS_POWER_MEAN);
    cycleDuration = OfficeComplexConstants.CONSUMER_ELECTRONICS_DURATION_CYCLE;
    operationPercentage = Double.parseDouble(conf.getProperty("ConsumerElectronicsWorking"));

  }

  @Override
  public void fillDailyOperation (int weekday, Random gen)
  {
    // Initializing and Creating auxiliary variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {

      dailyOperation.add(false);
      loadVector.add(0);

      if (applianceOf.isOnBreak(weekday, i)) {

        double tempPercentage = operationPercentage + (OfficeComplexConstants.OPERATION_PARTITION * (applianceOf.employeeOnBreakNumber(weekday, i)));
        if (tempPercentage > gen.nextDouble()) {
          dailyOperation.set(i, true);
          loadVector.set(i, power);
        }
      }

    }

    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  @Override
  public void refresh (Random gen)
  {
    fillWeeklyOperation(gen);
    createWeeklyPossibilityOperationVector();
  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // The consumers electronics can work each quarter someone is in the
    // premises
    for (int j = 0; j < OfficeComplexConstants.QUARTERS_OF_DAY; j++) {
      if (applianceOf.isOnBreak(day, j) == true)
        possibilityDailyOperation.add(true);
      else
        possibilityDailyOperation.add(false);
    }
    return possibilityDailyOperation;
  }
}
