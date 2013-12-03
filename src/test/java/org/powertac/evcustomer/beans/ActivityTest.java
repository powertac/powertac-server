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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;


/**
 * @author Govert Buijs
 * @version 0.5, Date: 2013.11.28
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class ActivityTest
{
  private Activity activity;
  private int id = 3;
  private String activityName = "TestActivity";
  private double weekdayWeight = 1.0;
  private double weekendWeight = 0.5;

  @Before
  public void setUp ()
  {
    initialize();
  }

  @After
  public void tearDown ()
  {
    activity = null;
  }

  public void initialize ()
  {
    activity = new Activity(id, activityName, weekdayWeight, weekendWeight);
  }

  @Test
  public void testInitialization ()
  {
    assertEquals(activity.getId(), id);
    assertEquals(activity.getName(), activityName);
    assertEquals(activity.getWeekdayWeight(), weekdayWeight, 1E-06);
    assertEquals(activity.getWeekendWeight(), weekendWeight, 1E-06);
  }

  @Test
  public void testDayWeight ()
  {
    assertEquals(activity.getDayWeight(1), weekdayWeight, 1E-06);
    assertEquals(activity.getDayWeight(2), weekdayWeight, 1E-06);
    assertEquals(activity.getDayWeight(3), weekdayWeight, 1E-06);
    assertEquals(activity.getDayWeight(4), weekdayWeight, 1E-06);
    assertEquals(activity.getDayWeight(5), weekdayWeight, 1E-06);
    assertEquals(activity.getDayWeight(6), weekendWeight, 1E-06);
    assertEquals(activity.getDayWeight(7), weekendWeight, 1E-06);
  }

  @Test
  public void testHourWeight0 ()
  {
    assertEquals(activity.getHourWeight(0, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(1, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(2, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(3, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(4, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(5, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(6, 0), 1, 1E-06);

    assertEquals(activity.getHourWeight(7, 0), 0, 1E-06);
    assertEquals(activity.getHourWeight(8, 0), 0, 1E-06);
    assertEquals(activity.getHourWeight(9, 0), 0, 1E-06);
    assertEquals(activity.getHourWeight(10, 0), 0, 1E-06);
    assertEquals(activity.getHourWeight(11, 0), 0, 1E-06);
    assertEquals(activity.getHourWeight(12, 0), 0, 1E-06);
    assertEquals(activity.getHourWeight(13, 0), 0, 1E-06);
    assertEquals(activity.getHourWeight(14, 0), 0, 1E-06);
    assertEquals(activity.getHourWeight(15, 0), 0, 1E-06);
    assertEquals(activity.getHourWeight(16, 0), 0, 1E-06);

    assertEquals(activity.getHourWeight(17, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(18, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(19, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(20, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(21, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(22, 0), 1, 1E-06);
    assertEquals(activity.getHourWeight(23, 0), 1, 1E-06);
  }

  @Test
  public void testHourWeight1 ()
  {
    assertEquals(activity.getHourWeight(0, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(1, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(2, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(3, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(4, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(5, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(6, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(7, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(8, 1), 1, 1E-06);

    assertEquals(activity.getHourWeight(9, 1), 0, 1E-06);
    assertEquals(activity.getHourWeight(10, 1), 0, 1E-06);
    assertEquals(activity.getHourWeight(11, 1), 0, 1E-06);
    assertEquals(activity.getHourWeight(12, 1), 0, 1E-06);
    assertEquals(activity.getHourWeight(13, 1), 0, 1E-06);
    assertEquals(activity.getHourWeight(14, 1), 0, 1E-06);
    assertEquals(activity.getHourWeight(15, 1), 0, 1E-06);
    assertEquals(activity.getHourWeight(16, 1), 0, 1E-06);
    assertEquals(activity.getHourWeight(17, 1), 0, 1E-06);

    assertEquals(activity.getHourWeight(18, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(19, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(20, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(21, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(22, 1), 1, 1E-06);
    assertEquals(activity.getHourWeight(23, 1), 1, 1E-06);
  }
}