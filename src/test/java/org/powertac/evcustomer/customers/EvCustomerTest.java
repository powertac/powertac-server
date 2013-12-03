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
import org.powertac.evcustomer.EvCustomerService;
import org.powertac.evcustomer.PredictableRandom;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.ActivityDetail;
import org.powertac.evcustomer.beans.Car;
import org.powertac.evcustomer.beans.SocialGroup;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


/**
 * @author Govert Buijs
 * @version 0.5, Date: 2013.11.28
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
  private Map<Integer, ActivityDetail> activityDetails;
  private ActivityDetail activityDetail;
  private Car car;

  private double maleProbability = 0.5;
  private double maleDailyKm = 50.0;
  private double femaleDailyKm = 50.0;
  private int groupId = 2;

  private PredictableRandom gen;

  @Before
  public void setUp ()
  {
    evCustomer = new EvCustomer();
    socialGroup = new SocialGroup(groupId, "Group " + groupId);
    activity = new Activity(0, "TestActivity", 1, 1);
    activityDetail = new ActivityDetail(0, maleDailyKm, femaleDailyKm, 0.5, 0.5);
    car = new Car("TestCar", 100.0, 200.0, 10.0, 10.0);

    gen = new PredictableRandom(new double[]{0.4},
        new int[]{0, 51, 52, 53, 54, 55});
  }

  @After
  public void tearDown ()
  {
    evCustomer = null;
    gen = null;
  }

  public void initialize ()
  {
    if (activities == null) {
      activities = new HashMap<Integer, Activity>();
      activities.put(activity.getId(), activity);
    }
    if (activityDetails == null) {
      activityDetails = new HashMap<Integer, ActivityDetail>();
      activityDetails.put(activityDetail.getActivityId(), activityDetail);
    }
    String gender = "female";
    if (gen.nextDouble() < maleProbability) {
      gender = "male";
    }
    evCustomer.initialize(socialGroup, gender, activities, activityDetails, car, gen);
  }

  @Test
  public void checkCarInitialization ()
  {
    initialize();

    Car car2 = evCustomer.getCar();

    assertEquals(car.getName(), car2.getName());
    assertEquals(car.getMaxCapacity(), car2.getMaxCapacity(), 1E-06);
    assertEquals(car.getCurrentCapacity(), car2.getCurrentCapacity(), 1E-06);
    assertEquals(car.getRange(), car2.getRange(), 1E-06);
    assertEquals(car.getHomeCharging(), car2.getHomeCharging(), 1E-06);
    assertEquals(car.getAwayCharging(), car2.getAwayCharging(), 1E-06);
  }

  @Test
  public void checkSocialGroupInitialization ()
  {
    initialize();

    SocialGroup socialGroup2 = evCustomer.getSocialGroup();

    assertEquals(socialGroup.getId(), socialGroup2.getId());
    assertEquals(socialGroup.getName(), socialGroup2.getName());
  }

  @Test
  public void checkActivityInitialization ()
  {
    initialize();

    Map<Integer, Activity> activities2 = evCustomer.getActivities();
    Activity activity2 = activities2.get(activity.getId());

    assertEquals(activity.getId(), activity2.getId());
    assertEquals(activity.getName(), activity2.getName());
    assertEquals(activity.getWeekdayWeight(), activity2.getWeekdayWeight(), 1E-06);
    assertEquals(activity.getWeekendWeight(), activity2.getWeekendWeight(), 1E-06);
  }

  @Test
  public void checkActivityDetailsInitialization ()
  {
    initialize();

    Map<Integer, ActivityDetail> activityDetails = evCustomer.getActivityDetails();
    ActivityDetail activityDetail2 = activityDetails.get(activity.getId());

    assertEquals(activityDetail.getActivityId(), activityDetail2.getActivityId());
    assertEquals(activityDetail.getMaleDailyKm(), activityDetail2.getMaleDailyKm(), 1E-06);
    assertEquals(activityDetail.getFemaleDailyKm(), activityDetail2.getFemaleDailyKm(), 1E-06);
    assertEquals(activityDetail.getMaleProbability(), activityDetail2.getMaleProbability(), 1E-06);
    assertEquals(activityDetail.getFemaleProbability(), activityDetail2.getFemaleProbability(), 1E-06);
  }

  @Test
  public void testGender ()
  {
    gen.setDoubleSeed(new double[]{0.6});
    initialize();
    assertEquals(evCustomer.getGender(), "female");

    gen.setDoubleSeed(new double[]{0.4});
    gen.resetCounters();
    initialize();
    assertEquals(evCustomer.getGender(), "male");
  }

  @Test
  public void testRiskAttitude ()
  {
    initialize();
    assertEquals(evCustomer.getRiskAttitude(), "risk_averse");

    gen.setIntSeed(new int[]{1});
    gen.resetCounters();
    initialize();
    assertEquals(evCustomer.getRiskAttitude(), "risk_neutral");

    gen.setIntSeed(new int[]{2});
    gen.resetCounters();
    initialize();
    assertEquals(evCustomer.getRiskAttitude(), "risk_eager");
  }

  @Test
  public void testConsumePowerRiskAverse ()
  {
    car = new Car("TestCar", 100.0, 200.0, 20.0, 10.0);

    gen.setIntSeed(new int[]{0, 0, 100});
    gen.setDoubleSeed(new double[]{0});
    initialize();
    evCustomer.setNomalizingFactors(new double[]{1, 1, 1, 1, 1, 1, 1, 1, 1});

    Car car2 = evCustomer.getCar();

    assertEquals(evCustomer.getRiskAttitude(), "risk_averse");

    // Deplete the battery to 75%
    assertEquals(car2.getCurrentCapacity(), 100, 1E-06);
    evCustomer.doActivities(0, 0);
    assertEquals(evCustomer.isDriving(), true);
    assertEquals(car2.getCurrentCapacity(), 75, 1E-06);
    // Make sure we aren't driving
    evCustomer.doActivities(6, 0);
    assertEquals(evCustomer.isDriving(), false);
    assertEquals(car2.getCurrentCapacity(), 75, 1E-06);

    // Risk averse always charges when under 100%
    double charge = evCustomer.charge(0, 0);
    assertEquals(car.getAwayCharging(), charge, 1E-06);
    assertEquals(car2.getCurrentCapacity(), 75 + car.getAwayCharging(), 1E-06);
  }

  @Test
  public void testConsumePowerRiskNeutral ()
  {
    car = new Car("TestCar", 100.0, 200.0, 20.0, 10.0);

    gen.setIntSeed(new int[]{1, 0, 0, 100, 0, 100});
    gen.setDoubleSeed(new double[]{0});
    initialize();
    evCustomer.setNomalizingFactors(new double[]{1, 1, 1, 1, 1, 1, 1, 1, 1});

    Car car2 = evCustomer.getCar();

    assertEquals(evCustomer.getRiskAttitude(), "risk_neutral");

    // Deplete the battery to 50%
    assertEquals(car2.getCurrentCapacity(), 100, 1E-06);
    evCustomer.doActivities(0, 0);
    evCustomer.doActivities(0, 0);
    assertEquals(evCustomer.isDriving(), true);
    assertEquals(car2.getCurrentCapacity(), 50, 1E-06);
    // Make sure we aren't driving
    evCustomer.doActivities(6, 0);
    assertEquals(evCustomer.isDriving(), false);
    assertEquals(car2.getCurrentCapacity(), 50, 1E-06);

    // Risk Neutral doesn't charge when 50% and up
    double charge = evCustomer.charge(0, 0);
    assertEquals(charge, 0.0, 1E-06);
    assertEquals(car2.getCurrentCapacity(), 50, 1E-06);

    // Deplete battery to 25%
    evCustomer.doActivities(0, 0);
    assertEquals(evCustomer.isDriving(), true);
    assertEquals(car2.getCurrentCapacity(), 25, 1E-06);
    // Make sure we aren't driving
    evCustomer.doActivities(6, 0);
    assertEquals(evCustomer.isDriving(), false);
    assertEquals(car2.getCurrentCapacity(), 25, 1E-06);

    // Risk neutral starts charging when below 50%
    charge = evCustomer.charge(0, 0);
    assertEquals(charge, 10.0, 1E-06);
    assertEquals(car2.getCurrentCapacity(), 35, 1E-06);
  }

  @Test
  public void testConsumePowerRiskEager ()
  {
    car = new Car("TestCar", 100.0, 200.0, 20.0, 10.0);

    gen.setIntSeed(new int[]{2, 0, 0, 0, 100, 0, 100});
    gen.setDoubleSeed(new double[]{0});
    initialize();
    evCustomer.setNomalizingFactors(new double[]{1, 1, 1, 1, 1, 1, 1, 1, 1});

    Car car2 = evCustomer.getCar();

    assertEquals(evCustomer.getRiskAttitude(), "risk_eager");

    // Deplete the battery to 25%
    assertEquals(car2.getCurrentCapacity(), 100, 1E-06);
    evCustomer.doActivities(0, 0);
    evCustomer.doActivities(0, 0);
    evCustomer.doActivities(0, 0);
    assertEquals(evCustomer.isDriving(), true);
    assertEquals(car2.getCurrentCapacity(), 25, 1E-06);
    // Make sure we aren't driving
    evCustomer.doActivities(6, 0);
    assertEquals(evCustomer.isDriving(), false);
    assertEquals(car2.getCurrentCapacity(), 25, 1E-06);

    // Risk eager doesn't charge when not below 20%
    double charge = evCustomer.charge(0, 0);
    assertEquals(charge, 0.0, 1E-06);
    assertEquals(car2.getCurrentCapacity(), 25, 1E-06);

    // Deplete the battery to 0%
    evCustomer.doActivities(0, 0);
    assertEquals(evCustomer.isDriving(), true);
    assertEquals(car2.getCurrentCapacity(), 0, 1E-06);
    // Make sure we aren't driving
    evCustomer.doActivities(6, 0);
    assertEquals(evCustomer.isDriving(), false);
    assertEquals(car2.getCurrentCapacity(), 0, 1E-06);

    // Risk eager finally starts charging
    charge = evCustomer.charge(0, 0);
    assertEquals(charge, 10.0, 1E-06);
    assertEquals(car2.getCurrentCapacity(), 10, 1E-06);
  }

  @Test
  public void testDominantLoad ()
  {
    initialize();

    double totalKms = 0.0;
    for (Map.Entry<Integer, ActivityDetail> entry :
        evCustomer.getActivityDetails().entrySet()) {
      totalKms += entry.getValue().getMaleDailyKm();
    }
    double totalKwh = evCustomer.getCar().getNeededCapacity(totalKms);

    assertEquals(totalKwh, evCustomer.getDominantLoad(), 1E-06);
  }

  @Test
  public void testDoActivities1 ()
  {
    gen.setIntSeed(new int[]{0, 0, 100, 0});
    initialize();
    evCustomer.setNomalizingFactors(new double[]{1, 1, 1, 1, 1, 1, 1, 1, 1});

    Car car2 = evCustomer.getCar();

    assertEquals(car2.getCurrentCapacity(), 100, 1E-06);
    evCustomer.doActivities(0, 0);
    assertEquals(evCustomer.isDriving(), true);
    assertEquals(car2.getCurrentCapacity(), 75, 1E-06);
    evCustomer.doActivities(6, 0);
    assertEquals(evCustomer.isDriving(), false);
    assertEquals(car2.getCurrentCapacity(), 75, 1E-06);
    evCustomer.doActivities(0, 0);
    assertEquals(evCustomer.isDriving(), true);
    assertEquals(car2.getCurrentCapacity(), 50, 1E-06);
  }

  @Test
  public void testDoActivities2 ()
  {
    // Make sure we have a male, risk averse customer
    gen.setDoubleSeed(new double[]{0});
    gen.setIntSeed(new int[]{0});

    // Get the activities from the config file
    activities = EvCustomerService.loadActivities();
    activityDetails = EvCustomerService
        .loadActivityDetails().get(socialGroup.getId());
    initialize();

    double sum0 = 0.0;
    gen.setDoubleSeed(new double[]{0});
    gen.setIntSeed(new int[]{0});
    for (int day = 0; day < 7; day++) {
      for (int hour = 0; hour < 24; hour++) {
        evCustomer.doActivities(day, hour);

        evCustomer.setDriving(false);
        sum0 += evCustomer.charge(day, hour);
      }
    }
    double sum1 = 0.0;
    gen.setDoubleSeed(new double[]{0});
    gen.setIntSeed(new int[]{50});
    for (int day = 0; day < 7; day++) {
      for (int hour = 0; hour < 24; hour++) {
        evCustomer.doActivities(day, hour);

        evCustomer.setDriving(false);
        sum1 += evCustomer.charge(day, hour);
      }
    }
    double sum2 = 0.0;
    gen.setDoubleSeed(new double[]{0});
    gen.setIntSeed(new int[]{100});
    for (int day = 0; day < 7; day++) {
      for (int hour = 0; hour < 24; hour++) {
        evCustomer.doActivities(day, hour);

        evCustomer.setDriving(false);
        sum2 += evCustomer.charge(day, hour);
      }
    }

    assertEquals(sum0, 500.2565991804522, 1E-06);
    assertEquals(sum1, 187.8662879225693, 1E-06);
    assertEquals(sum2, 0, 1e-14);
  }
}