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

package org.powertac.evcustomer.customers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.evcustomer.PredictableRandom;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.ActivityDetail;
import org.powertac.evcustomer.beans.CarType;
import org.powertac.evcustomer.beans.SocialGroup;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


/**
 * @author Govert Buijs
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class EvCustomerTest
{
  private EvCustomer evCustomer;
  private SocialGroup socialGroup;
  private Map<Integer, Activity> activities;
  private Activity activity;
  private Map<Integer, ActivityDetail> details;
  private ActivityDetail detail;
  private CarType carType;

  private double maleDailyKm = 10;
  private double femaleDailyKm = 10;
  private int groupId = 2;

  private PredictableRandom gen;

  @Before
  public void setUp ()
  {
    evCustomer = new EvCustomer();
    socialGroup = new SocialGroup(groupId, "Group " + groupId);
    activity = new Activity(0, "TestActivity", 1, 1);
    detail = new ActivityDetail(0, maleDailyKm, femaleDailyKm, 1, 1);
    carType = new CarType();
    carType.configure("TestCar", 100.0, 200.0, 10.0, 10.0);

    gen = new PredictableRandom(new double[]{0.4},
        new int[]{0, 51, 52, 53, 54, 55});
  }

  @After
  public void tearDown ()
  {
    evCustomer = null;
    gen = null;
  }

  public void initialize (String gender)
  {
    if (activities == null) {
      activities = new HashMap<Integer, Activity>();
      activities.put(activity.getId(), activity);
    }
    if (details == null) {
      details = new HashMap<Integer, ActivityDetail>();
      details.put(detail.getActivityId(), detail);
    }
    evCustomer.initialize(socialGroup, gender, activities, details, carType, gen);
  }

  @Test
  public void checkCarInitialization ()
  {
    // CarType model isn't dependent on gender
    initialize("male");

    CarType car2 = evCustomer.getCar();

    assertEquals(car2.getName(),            carType.getName());
    assertEquals(car2.getMaxCapacity(),     carType.getMaxCapacity(),     1E-06);
    assertEquals(car2.getCurrentCapacity(), carType.getCurrentCapacity(), 1E-06);
    assertEquals(car2.getRange(),           carType.getRange(),           1E-06);
    assertEquals(car2.getHomeCharging(),    carType.getHomeCharging(),    1E-06);
    assertEquals(car2.getAwayCharging(), carType.getAwayCharging(), 1E-06);
  }

  @Test
  public void checkSocialGroupInitialization ()
  {
    initialize("male");

    SocialGroup socialGroup2 = evCustomer.getSocialGroup();

    assertEquals(socialGroup2.getId(),    socialGroup.getId());
    assertEquals(socialGroup2.getName(),  socialGroup.getName());
  }

  @Test
  public void checkActivityInitialization ()
  {
    // Activities aren't dependent on gender
    initialize("male");

    Map<Integer, Activity> activities2 = evCustomer.getActivities();
    Activity activity2 = activities2.get(activity.getId());

    assertEquals(activity2.getId(),             activity.getId());
    assertEquals(activity2.getName(),           activity.getName());
    assertEquals(activity2.getWeekdayWeight(),  activity.getWeekdayWeight(), 1E-06);
    assertEquals(activity2.getWeekendWeight(),  activity.getWeekendWeight(), 1E-06);
  }

  @Test
  public void checkDetailsInitialization ()
  {
    // ActivityDetails aren't dependent on gender
    initialize("male");

    Map<Integer, ActivityDetail> activityDetails = evCustomer.getActivityDetails();
    ActivityDetail activityDetail2 = activityDetails.get(activity.getId());

    assertEquals(activityDetail2.getActivityId(),         detail.getActivityId());
    assertEquals(activityDetail2.getMaleDailyKm(),        detail.getMaleDailyKm(),        1E-06);
    assertEquals(activityDetail2.getFemaleDailyKm(),      detail.getFemaleDailyKm(),      1E-06);
    assertEquals(activityDetail2.getMaleProbability(),    detail.getMaleProbability(),    1E-06);
    assertEquals(activityDetail2.getFemaleProbability(),  detail.getFemaleProbability(),  1E-06);
  }

  @Test
  public void testGender ()
  {
    gen.setIntSeed(new int[]{0});

    initialize("male");
    assertEquals("male",    evCustomer.getGender());

    initialize("female");
    assertEquals("female",  evCustomer.getGender());
  }

  @Test
  public void testRiskAttitude ()
  {
    // Risk attitude isn't dependent on gender
    initialize("male");
    assertEquals("averse",   evCustomer.getRiskAttitude());

    gen.setIntSeed(new int[]{1});
    gen.resetCounters();
    initialize("male");
    assertEquals("neutral", evCustomer.getRiskAttitude());

    gen.setIntSeed(new int[]{2});
    gen.resetCounters();
    initialize("male");
    assertEquals("eager",  evCustomer.getRiskAttitude());
  }

  @Test
  public void testConsumePowerRiskAverse ()
  {
    carType = new CarType();
    carType.configure("TestCar", 100.0, 200.0, 20.0, 10.0);

    gen.setIntSeed(new int[]{0, 100});
    gen.setDoubleSeed(new double[]{0});
    initialize("male");
    evCustomer.makeDayPlanning(0);

    CarType car2 = evCustomer.getCar();

    assertEquals("averse", evCustomer.getRiskAttitude());

    // Risk averse always charges when under 80%
    assertEquals(50, car2.getCurrentCapacity(), 1E-06);
    double[] loads = evCustomer.getLoads(0, 0);
    assertEquals(20.0, loads[1], 1E-06);

    car2.setCurrentCapacity(70);
    assertEquals(70, car2.getCurrentCapacity(), 1E-06);
    loads = evCustomer.getLoads(0, 0);
    assertEquals(10.0, loads[1], 1E-06);

    car2.setCurrentCapacity(90);
    assertEquals(90, car2.getCurrentCapacity(), 1E-06);
    loads = evCustomer.getLoads(0, 0);
    assertEquals(0.0, loads[1], 1E-06);
  }

  @Test
  public void testConsumePowerRiskNeutral ()
  {
    carType = new CarType();
    carType.configure("TestCar", 100.0, 200.0, 20.0, 10.0);

    gen.setIntSeed(new int[]{1, 100});
    gen.setDoubleSeed(new double[]{0});
    initialize("male");
    evCustomer.makeDayPlanning(0);

    CarType car2 = evCustomer.getCar();

    assertEquals("neutral", evCustomer.getRiskAttitude());

    // Risk neutral always charges when under 60 %
    assertEquals(50, car2.getCurrentCapacity(), 1E-06);
    double[] loads = evCustomer.getLoads(0, 0);
    assertEquals(10.0, loads[1], 1E-06);

    car2.setCurrentCapacity(70);
    assertEquals(70, car2.getCurrentCapacity(), 1E-06);
    loads = evCustomer.getLoads(0, 0);
    assertEquals(0.0, loads[1], 1E-06);
  }

  @Test
  public void testConsumePowerRiskEager ()
  {
    carType = new CarType();
    carType.configure("TestCar", 100.0, 200.0, 20.0, 10.0);

    gen.setIntSeed(new int[]{2, 100});
    gen.setDoubleSeed(new double[]{0});
    initialize("male");
    evCustomer.makeDayPlanning(0);

    CarType car2 = evCustomer.getCar();

    assertEquals("eager", evCustomer.getRiskAttitude());

    // Risk eager always charges when under 40 %
    assertEquals(50, car2.getCurrentCapacity(), 1E-06);
    double[] loads = evCustomer.getLoads(0, 0);
    assertEquals(0.0, loads[1], 1E-06);

    car2.setCurrentCapacity(30);
    assertEquals(30, car2.getCurrentCapacity(), 1E-06);
    loads = evCustomer.getLoads(0, 0);
    assertEquals(10.0, loads[1], 1E-06);
  }

  @Test
  public void testDominantLoad ()
  {
    initialize("male");

    double totalKms = 0.0;
    for (Map.Entry<Integer, ActivityDetail> entry :
        evCustomer.getActivityDetails().entrySet()) {
      totalKms += entry.getValue().getMaleDailyKm();
    }
    double totalKwh = evCustomer.getCar().getNeededCapacity(totalKms);

    assertEquals(totalKwh, evCustomer.getDominantLoad(), 1E-06);

    setUp();
    initialize("female");

    totalKms = 0.0;
    for (Map.Entry<Integer, ActivityDetail> entry :
        evCustomer.getActivityDetails().entrySet()) {
      totalKms += entry.getValue().getMaleDailyKm();
    }
    totalKwh = evCustomer.getCar().getNeededCapacity(totalKms);

    assertEquals(totalKwh, evCustomer.getDominantLoad(), 1E-06);
  }

  @Test
  public void testDoActivities ()
  {
    gen.setIntSeed(new int[]{0, 0, 100, 0});
    initialize("male");
    evCustomer.makeDayPlanning(0);

    CarType car2 = evCustomer.getCar();

    assertEquals(50, car2.getCurrentCapacity(), 1E-06);

    evCustomer.doActivities(0, 6);
    assertEquals(evCustomer.isDriving(), true);
    assertEquals(25, car2.getCurrentCapacity(), 1E-06);
  }
}