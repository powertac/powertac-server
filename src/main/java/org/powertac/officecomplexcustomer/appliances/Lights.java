/*
 * Copyright 2009-2012 the original author or authors.
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
 * Lights are utilized when the persons inhabiting the house have need for them
 * to light the rooms they are in. So it's a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Lights extends NotShiftingAppliance
{

  @Override
  public void initialize (String office, Properties conf, Random gen)
  {
    // Filling the base variables
    name = office + " Lights";
    saturation = 1;
    power = (int) (OfficeComplexConstants.LIGHTS_POWER_VARIANCE * gen.nextGaussian() + OfficeComplexConstants.LIGHTS_POWER_MEAN) * applianceOf.getMembers().size();
    cycleDuration = OfficeComplexConstants.LIGHTS_DURATION_CYCLE;

  }

  @Override
  public void fillDailyOperation (int weekday, Random gen)
  {
    // Initializing and Creating auxiliary variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    // For each quarter of a day
    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {
      loadVector.add(0);
      dailyOperation.add(false);

      if (applianceOf.isWorking(weekday, i) || applianceOf.isOnBreak(weekday, i)) {
        loadVector.set(i, power);
        dailyOperation.set(i, true);
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

}
