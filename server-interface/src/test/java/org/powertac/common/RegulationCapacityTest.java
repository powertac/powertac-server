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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * @author John Collins
 */
public class RegulationCapacityTest
{

  /**
   *
   */
  @Before
  public void setUp () throws Exception
  {
  }

  @Test
  public void testRegulationCapacity ()
  {
    // normal creation
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    assertEquals("correct up-regulation", 1.0, rc.getUpRegulationCapacity(),
                 1e-6);
    assertEquals("correct down-regulation", -2.0,
                 rc.getDownRegulationCapacity(), 1e-6);
  }

  @Test
  public void bogusRegulationCapacity ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, 2.0);
    assertEquals("correct up-regulation", 1.0, rc.getUpRegulationCapacity(),
                 1e-6);
    assertEquals("default down-regulation", 0.0,
                 rc.getDownRegulationCapacity(), 1e-6);
    RegulationAccumulator rc1 = new RegulationAccumulator(-1.0, -2.0);
    assertEquals("default up-regulation", 0.0, rc1.getUpRegulationCapacity(),
                 1e-6);
    assertEquals("correct down-regulation", -2.0,
                 rc1.getDownRegulationCapacity(), 1e-6);
  }

  @Test
  public void testSetUp ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.setUpRegulationCapacity(2.5);
    assertEquals("successful set", 2.5, rc.getUpRegulationCapacity(), 1e-6);
    rc.setUpRegulationCapacity(-3.0);
    assertEquals("no change", 2.5, rc.getUpRegulationCapacity(), 1e-6);
  }

  @Test
  public void testSetDown ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.setDownRegulationCapacity(-2.5);
    assertEquals("successful set", -2.5, rc.getDownRegulationCapacity(), 1e-6);
    rc.setDownRegulationCapacity(3.0);
    assertEquals("no change", -2.5, rc.getDownRegulationCapacity(), 1e-6);
  }

  @Test
  public void testAdd ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    RegulationAccumulator rc1 = new RegulationAccumulator(2.0, -3.0);
    rc.add(rc1);
    assertEquals("correct up-regulation", 3.0, rc.getUpRegulationCapacity(),
                 1e-6);
    assertEquals("correct down-regulation", -5.0,
                 rc.getDownRegulationCapacity(), 1e-6);
  }

  @Test
  public void testAddUpRegulation ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.addUpRegulation(3.0);
    assertEquals("correct up-regulation", 4.0, rc.getUpRegulationCapacity(),
                 1e-6);
    assertEquals("correct down-regulation", -2.0,
                 rc.getDownRegulationCapacity(), 1e-6);
  }

  @Test
  public void bugusAddUpRegulation ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.addUpRegulation(-3.0);
    assertEquals("correct up-regulation", 1.0, rc.getUpRegulationCapacity(),
                 1e-6);
    assertEquals("correct down-regulation", -2.0,
                 rc.getDownRegulationCapacity(), 1e-6);
  }

  @Test
  public void testAddDownRegulation ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.addDownRegulation(-3.0);
    assertEquals("correct up-regulation", 1.0, rc.getUpRegulationCapacity(),
                 1e-6);
    assertEquals("correct down-regulation", -5.0,
                 rc.getDownRegulationCapacity(), 1e-6);
  }

  @Test
  public void bogusAddDownRegulation ()
  {
    RegulationAccumulator rc = new RegulationAccumulator(1.0, -2.0);
    rc.addDownRegulation(3.0);
    assertEquals("correct up-regulation", 1.0, rc.getUpRegulationCapacity(),
                 1e-6);
    assertEquals("correct down-regulation", -2.0,
                 rc.getDownRegulationCapacity(), 1e-6);
  }
}
