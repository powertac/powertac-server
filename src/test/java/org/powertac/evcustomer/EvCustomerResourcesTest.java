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

package org.powertac.evcustomer;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.evcustomer.beans.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * @author Govert Buijs
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class EvCustomerResourcesTest
{
  private List<Car> cars;
  private Map<Integer, SocialGroup> socialGroups;
  private Map<Integer, Activity> activities;
  private Map<Integer, Map<Integer, ActivityDetail>> allActivityDetails;
  private Map<String, SocialClassDetail> socialClassDetails;

  @After
  public void tearDown ()
  {
    cars = null;
    socialGroups = null;
    activities = null;
    allActivityDetails = null;
    socialClassDetails = null;
  }

  @Test
  public void testCarTypes ()
  {
    cars = EvCustomerService.loadCarTypes();

    // If anything fails during loading, an empty list is returned
    assertTrue(cars.size() > 0);

    for (Car car : cars) {
      assertFalse(car.getName().equals(""));
      assertTrue(car.getCurrentCapacity() >= 0);
      assertTrue(car.getRange() > 0);
      assertTrue(car.getHomeCharging() > 0);
      assertTrue(car.getAwayCharging() > 0);
    }
  }

  @Test
  public void testSocialGroups ()
  {
    socialGroups = EvCustomerService.loadSocialGroups();

    // If anything fails during loading, an empty list is returned
    assertTrue(socialGroups.size() > 0);

    for (SocialGroup socialGroup : socialGroups.values()) {
      assertFalse(socialGroup.getName().equals(""));
      assertTrue(socialGroup.getId() >= 0);
      assertTrue(socialGroup.getId() < socialGroups.size());
    }
  }

  @Test
  public void testActivities ()
  {
    activities = EvCustomerService.loadActivities();

    // If anything fails during loading, an empty list is returned
    assertTrue(activities.size() > 0);

    for (Activity activitiy : activities.values()) {
      assertFalse(activitiy.getName().equals(""));
      assertTrue(activitiy.getId() >= 0);
      assertTrue(activitiy.getId() < activities.size());
      assertTrue(activitiy.getWeekdayWeight() >= 0);
      assertTrue(activitiy.getWeekendWeight() >= 0);
    }
  }

  @Test
  public void testActivityDetails ()
  {
    socialGroups = EvCustomerService.loadSocialGroups();
    activities = EvCustomerService.loadActivities();
    allActivityDetails = EvCustomerService.loadActivityDetails();

    // If anything fails during loading, an empty list is returned
    assertTrue(allActivityDetails.size() > 0);

    for (SocialGroup socialGroup : socialGroups.values()) {
      Map<Integer, ActivityDetail> activityDetails =
          allActivityDetails.get(socialGroup.getId());

      for (Activity activity : activities.values()) {
        ActivityDetail activityDetail = activityDetails.get(activity.getId());
        assertEquals(activityDetail.getActivityId(), activity.getId());
        assertTrue(activityDetail.getMaleDailyKm() >= 0);
        assertTrue(activityDetail.getFemaleDailyKm() >= 0);
        assertTrue(activityDetail.getMaleProbability() >= 0);
        assertTrue(activityDetail.getFemaleProbability() >= 0);
      }
    }
  }

  @Test
  public void testSocialClasses ()
  {
    cars = EvCustomerService.loadCarTypes();
    socialGroups = EvCustomerService.loadSocialGroups();
    activities = EvCustomerService.loadActivities();
    allActivityDetails = EvCustomerService.loadActivityDetails();
    socialClassDetails = EvCustomerService.loadSocialClassesDetails();

    // If anything fails during loading, an empty list is returned
    assertTrue(socialClassDetails.size() > 0);

    for (SocialClassDetail socialClassDetail : socialClassDetails.values()) {
      assertFalse(socialClassDetail.getName().equals(""));
      assertTrue(socialClassDetail.getMinCount() >= 0);
      assertTrue(socialClassDetail.getMaxCount() >= 0);
      assertTrue(socialClassDetail.getMinCount() <=
          socialClassDetail.getMaxCount());

      assertEquals(socialClassDetail.getSocialGroupDetails().size(),
          socialGroups.size());

      double count = 0.0;
      for (SocialGroupDetail socialGroupDetail :
          socialClassDetail.getSocialGroupDetails().values()) {
        assertTrue(socialGroups.get(socialGroupDetail.getId()) != null);
        assertTrue(socialGroupDetail.getMaleProbability() >= 0);
        assertTrue(socialGroupDetail.getMaleProbability() <= 1);

        count += socialGroupDetail.getProbability();
      }
      assertEquals(count, 1.0, 1E-06);
    }
  }
}