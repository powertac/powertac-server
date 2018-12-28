/*
 * Copyright 2013 the original author or authors.
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

package org.powertac.evcustomer.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author Govert Buijs
 */
public class ClassGroupTest
{
  private ClassGroup classGroup;
  private int id = 1;
  private double probability = 0.5;
  private double maleProbability = 0.5;

  @BeforeEach
  public void setUp ()
  {
    initialize();
  }

  @AfterEach
  public void tearDown ()
  {
    classGroup = null;
  }

  private void initialize ()
  {
    classGroup = new ClassGroup("name");
    classGroup.initialize(id, probability, maleProbability);
  }

  @Test
  public void testInitialization ()
  {
    assertEquals(id,              classGroup.getGroupId(), id);
    assertEquals(probability,     classGroup.getProbability(), 1E-06);
    assertEquals(maleProbability, classGroup.getMaleProbability(), 1E-06);
  }
}