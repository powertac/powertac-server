/*
 * Copyright (c) 2014 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author John Collins
 */
public class RegulationCapacityTest
{

  /**
   *
   */
  @BeforeEach
  public void setUp () throws Exception
  {
  }

  @Test
  public void testRegulationCapacity ()
  {
    // normal creation
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    assertEquals(1.0, rc.getUpRegulationCapacity(),1e-6, "correct up-regulation");
    assertEquals(-2.0, rc.getDownRegulationCapacity(), 1e-6, "correct down-regulation");
  }

  @Test
  public void bogusRegulationCapacity ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, 2.0);
    assertEquals(1.0, rc.getUpRegulationCapacity(), 1e-6, "correct up-regulation");
    assertEquals(0.0, rc.getDownRegulationCapacity(), 1e-6, "default down-regulation");
    RegulationAccumulator rc1 = new RegulationAccumulator(-1.0, -2.0);
    assertEquals(0.0, rc1.getUpRegulationCapacity(), 1e-6, "default up-regulation");
    assertEquals(-2.0, rc1.getDownRegulationCapacity(), 1e-6, "correct down-regulation");
  }

  @Test
  public void testSetUp ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.setUpRegulationCapacity(2.5);
    assertEquals(2.5, rc.getUpRegulationCapacity(), 1e-6, "successful set");
    rc.setUpRegulationCapacity(-3.0);
    assertEquals(2.5, rc.getUpRegulationCapacity(), 1e-6, "no change");
  }

  @Test
  public void testSetDown ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.setDownRegulationCapacity(-2.5);
    assertEquals(-2.5, rc.getDownRegulationCapacity(), 1e-6, "successful set");
    rc.setDownRegulationCapacity(3.0);
    assertEquals(-2.5, rc.getDownRegulationCapacity(), 1e-6, "no change");
  }

  @Test
  public void testAdd ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    RegulationAccumulator rc1 = new RegulationAccumulator(2.0, -3.0);
    rc.add(rc1);
    assertEquals(3.0, rc.getUpRegulationCapacity(), 1e-6, "correct up-regulation");
    assertEquals(-5.0, rc.getDownRegulationCapacity(), 1e-6, "correct down-regulation");
  }

  @Test
  public void testAddUpRegulation ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.addUpRegulation(3.0);
    assertEquals(4.0, rc.getUpRegulationCapacity(), 1e-6, "correct up-regulation");
    assertEquals(-2.0, rc.getDownRegulationCapacity(), 1e-6, "correct down-regulation");
  }

  @Test
  public void bugusAddUpRegulation ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.addUpRegulation(-3.0);
    assertEquals(1.0, rc.getUpRegulationCapacity(), 1e-6, "correct up-regulation");
    assertEquals(-2.0, rc.getDownRegulationCapacity(), 1e-6, "correct down-regulation");
  }

  @Test
  public void testAddDownRegulation ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.addDownRegulation(-3.0);
    assertEquals(1.0, rc.getUpRegulationCapacity(), 1e-6, "correct up-regulation");
    assertEquals(-5.0, rc.getDownRegulationCapacity(), 1e-6, "correct down-regulation");
  }

  @Test
  public void bogusAddDownRegulation ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.addDownRegulation(3.0);
    assertEquals(1.0, rc.getUpRegulationCapacity(), 1e-6, "correct up-regulation");
    assertEquals(-2.0, rc.getDownRegulationCapacity(), 1e-6, "correct down-regulation");
  }
}
