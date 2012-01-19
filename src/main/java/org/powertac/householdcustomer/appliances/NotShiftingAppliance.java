/*
 * Copyright 2009-2011 the original author or authors.
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

import java.util.Random;
import java.util.Vector;

import org.powertac.common.configurations.HouseholdConstants;

/**
 * This is the class for the appliance domain instances that cannot change / shift their load at
 * all. Most of them are appliance that require the tenant's presence in order to begin functioning
 * @author Antonios Chrysopoulos
 * @version 1, 13/02/2011
 */
public class NotShiftingAppliance extends Appliance
{

  @Override
  Vector<Boolean> createDailyOperationVector (int times, Random gen)
  {

    // Creating Auxiliary Variables
    Vector<Boolean> v = new Vector<Boolean>(HouseholdConstants.QUARTERS_OF_DAY);
    for (int i = 0; i < HouseholdConstants.QUARTERS_OF_DAY; i++)
      v.add(false);

    // Then for the times it work add function quarters
    for (int i = 0; i < times; i++) {
      int quarter = gen.nextInt(HouseholdConstants.QUARTERS_OF_DAY);
      v.set(quarter, true);
    }
    return v;
  }

  @Override
  void createWeeklyOperationVector (int times, Random gen)
  {
    for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK; i++)
      operationVector.add(createDailyOperationVector(times, gen));
  }

  @Override
  public void fillWeeklyFunction (Random gen)
  {
    for (int i = 0; i < HouseholdConstants.DAYS_OF_WEEK; i++)
      fillDailyFunction(i, gen);
  }

}
