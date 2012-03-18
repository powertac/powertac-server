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

import org.powertac.common.configurations.OfficeComplexConstants;

/**
 * ICT are the appliances that are utilized mainly for work or to communicate
 * with others from the household such as computers, telephone devices etc. They
 * works only when someone is at home and uses them. So it's a not shifting
 * appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class ICT extends NotShiftingAppliance
{

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Filling the base variables
    name = household + " ICT";
    saturation = 1;
    power = (int) (OfficeComplexConstants.ICT_POWER_VARIANCE * gen.nextGaussian() + OfficeComplexConstants.ICT_POWER_MEAN);
    cycleDuration = OfficeComplexConstants.ICT_DURATION_CYCLE;
    times = Integer.parseInt(conf.getProperty("ICTDailyTimes")) + (int) (applianceOf.getMembers().size() / OfficeComplexConstants.PERSONS);

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

    }

    Vector<Integer> temp = new Vector<Integer>();

    for (int i = 0; i < OfficeComplexConstants.QUARTERS_OF_DAY; i++) {

      int count = applianceOf.employeeNumber(weekday, i);
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
