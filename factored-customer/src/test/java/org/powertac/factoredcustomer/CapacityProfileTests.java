/*
 * Copyright 2011 the original author or authors.
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

package org.powertac.factoredcustomer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.factoredcustomer.CapacityProfile.PermutationRule;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Prashant Reddy
 */
public class CapacityProfileTests
{
  @BeforeEach
  public void setUp () throws Exception
  {
  }

  @Test
  public void testTimeShiftPermutations ()
  {
    List<Double> list = new ArrayList<>();
    for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
      list.add(1.0 * i);
    }
    CapacityProfile profile = new CapacityProfile(list);
    List<CapacityProfile> perms =
        profile.getPermutations(PermutationRule.TEMPORAL_SHIFTS);
    assertNotNull(perms, "Non null permutations");
    assertEquals(perms.size(), CapacityProfile.NUM_TIMESLOTS, "Correct number of permutations");
  }

  @Test
  public void testPeakShiftPermutations ()
  {
    double[] array = {0.5, 0.4, 0.4, 0.5, 0.5, 0.6, 0.6, 0.6, 0.7, 0.6, 0.6, 0.6,
        0.6, 0.6, 0.6, 0.6, 0.7, 0.8, 0.9, 1.0, 1.0, 0.9, 0.7, 0.5};

    CapacityProfile profile = new CapacityProfile(array);
    List<CapacityProfile> perms =
        profile.getPermutations(PermutationRule.BALANCING_SHIFTS);
    // assuming above array in not uniform
    assertNotNull(perms, "Non null permutations");
  }
}

