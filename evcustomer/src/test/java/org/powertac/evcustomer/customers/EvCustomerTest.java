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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.RandomSeed;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TimeService;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.evcustomer.Config;
import org.powertac.evcustomer.PredictableRandom;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.GroupActivity;
import org.powertac.evcustomer.beans.CarType;
import org.powertac.evcustomer.beans.SocialGroup;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Govert Buijs, John Collins
 */
public class EvCustomerTest
{
  private TariffSubscriptionRepo mockSubscriptionRepo;
  private RandomSeedRepo mockSeedRepo;
  private MockRandomSeed mockSeed;
  private TimeService mockTimeService;
  private EvSocialClass mockEsc;
  private ServiceAccessor service;

  private EvCustomer evCustomer;
  private String cName = "Joe";
  private SocialGroup socialGroup;
  private HashMap<Integer, Activity> activities;
  private Activity activity;
  private List<GroupActivity> gas;
  private GroupActivity detail;
  private CarType carType;

  private double maleDailyKm = 10;
  private double femaleDailyKm = 10;
  private int groupId = 2;

  @BeforeEach
  public void setUp ()
  {
    evCustomer = new EvCustomer(cName);
    mockEsc = mock(EvSocialClass.class);
    when (mockEsc.createTariffEvaluator(any()))
    .thenReturn(new TariffEvaluator(evCustomer.createTariffEvaluationWrapper()));
    mockTimeService = mock(TimeService.class);
    when (mockTimeService.getCurrentDateTime())
    .thenReturn(ZonedDateTime.now(TimeService.UTC));
    mockSeedRepo = mock(RandomSeedRepo.class);
    mockSeed = new MockRandomSeed("Test", groupId, cName);
    when(mockSeedRepo.getRandomSeed(anyString(),
                                    anyLong(),
                                    anyString())).thenReturn(mockSeed);

    service = new ServiceAccessor();

    socialGroup = new SocialGroup(groupId, "Group " + groupId);
    activities = new HashMap<>();
    gas = new ArrayList<>();
    carType = new CarType("TestCar");
    carType.configure("TestCar", 100.0, 200.0, 10.0, 5.0);
    mockSeed.setDoubleSeed(new double[]{0.4,0.4,0.1});
    mockSeed.setIntSeed(new int[]{0, 51, 52, 53, 54, 55});
  }

  // default activity setup 
  public void initialize (String gender)
  {
    activity = addActivity("TestActivity", 0, 0);
    detail = addGroupActivity("test-ga", activity,
                              maleDailyKm, femaleDailyKm, 1.0, 1.0);
    config(gender);
  }

  // final config independent of activity setup
  public void config (String gender)
  {
    // consumes 1 double, 2 int random values
    Config config = Config.getInstance();
    evCustomer.initialize(socialGroup, gender, activities, gas,
        carType, mockEsc, service, config);
  }

  private Activity addActivity (String name, int id, int interval)
  {
    Activity act = new Activity(name);
    act.setInterval(interval);
    act.setId(id);
    activities.put(id, act);
    return act;
  }

  private Activity addActivity (String name, int id, int interval,
                                double chargerProbability)
  {
    Activity result = addActivity(name, id, interval);
    result.setChargerProbability(chargerProbability);
    return result;
  }

  private GroupActivity
    addGroupActivity (String name, Activity activity,
                      double maleKm, double femaleKm,
                      double maleProbability, double femaleProbability)
  {
    GroupActivity result = new GroupActivity (name);
    result.initialize(activity.getId(), maleKm, femaleKm,
                      maleProbability, femaleProbability);
    gas.add(result);
    return result;
  }

  @Test
  public void checkCarInitialization ()
  {
    // CarType model isn't dependent on gender
    initialize("male");

    CarType car2 = evCustomer.getCar();

    assertEquals(car2.getName(), carType.getName());
    assertEquals(car2.getMaxCapacity(), carType.getMaxCapacity(), 1E-06);
    assertEquals(car2.getRange(), carType.getRange(), 1E-06);
    assertEquals(car2.getHomeChargeKW(), carType.getHomeChargeKW(), 1E-06);
    assertEquals(car2.getAwayChargeKW(), carType.getAwayChargeKW(), 1E-06);
  }

  @Test
  public void testCurrentCapacity ()
  {
    initialize("male");
    assertEquals(0.5 * evCustomer.getCar().getMaxCapacity(), evCustomer.getCurrentCapacity(), 1E-06, "current capacity initialized");
  }

  @Test
  public void testDischargeValid () throws EvCustomer.ChargeException
  {
    carType.setMaxCapacity(100.0);
    initialize("female");
    assertEquals(50.0, evCustomer.getCurrentCapacity(), 1E-06, "initial capacity");
    evCustomer.discharge(25.0);
    assertEquals(25.0, evCustomer.getCurrentCapacity(), 1E-06, "reduced capacity");
  }

//  @Test
//  public void testDischargeInvalid () //throws EvCustomer.ChargeException
//  {
//    assertThrows(EvCustomer.ChargeException.class, () -> {
//      carType.setMaxCapacity(100.0);
//      initialize("female");
//      evCustomer.discharge(51.0);
//      assertEquals(0.0, evCustomer.getCurrentCapacity(), 1E-06, "should not get here");
//    });
//  }

  @Test
  public void testChargeValid () throws EvCustomer.ChargeException
  {
    carType.setMaxCapacity(100.0);
    initialize("female");
    assertEquals(50.0, evCustomer.getCurrentCapacity(), 1E-06, "initial capacity");
    evCustomer.discharge(50);
    evCustomer.charge(25);
    assertEquals(25.0, evCustomer.getCurrentCapacity(), 1E-06, "25 remains");
  }

//  @Test
//  public void testChargeInvalid () //throws EvCustomer.ChargeException
//  {
//    assertThrows(EvCustomer.ChargeException.class, () -> {
//      carType.setMaxCapacity(100.0);
//      initialize("female");
//      assertEquals(50.0, evCustomer.getCurrentCapacity(), 1E-06, "initial capacity");
//      evCustomer.charge(51.0);
//      assertEquals(0.0, evCustomer.getCurrentCapacity(), 1E-06, "should not get here");
//    });
//  }

  @Test
  public void testNeededCapacity ()
  {
    carType.setMaxCapacity(100.0);
    initialize("female");
    assertEquals(100.0, evCustomer.getNeededCapacity(200.0), 1E-06, "correct range calc");
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

    List<GroupActivity> groupActivities = evCustomer.getGroupActivities();
    GroupActivity activityDetail2 = groupActivities.get(activity.getId());

    assertEquals(activityDetail2.getActivityId(),         detail.getActivityId());
    assertEquals(activityDetail2.getMaleDailyKm(),        detail.getMaleDailyKm(),        1E-06);
    assertEquals(activityDetail2.getFemaleDailyKm(),      detail.getFemaleDailyKm(),      1E-06);
    assertEquals(activityDetail2.getMaleProbability(),    detail.getMaleProbability(),    1E-06);
    assertEquals(activityDetail2.getFemaleProbability(),  detail.getFemaleProbability(),  1E-06);
  }

  @Test
  public void testGender ()
  {
    mockSeed.setIntSeed(new int[]{0});

    initialize("male");
    assertEquals(   evCustomer.getGender(), "male");

    initialize("female");
    assertEquals( evCustomer.getGender(), "female");
  }

  @Test
  public void testRiskAttitude ()
  {
    // Risk attitude isn't dependent on gender
    initialize("male");
    assertEquals(  evCustomer.getRiskAttitude(), "averse");

    mockSeed.setIntSeed(new int[]{1});
    mockSeed.resetCounters();
    initialize("male");
    assertEquals(evCustomer.getRiskAttitude(), "neutral");

    mockSeed.setIntSeed(new int[]{2});
    mockSeed.resetCounters();
    initialize("male");
    assertEquals( evCustomer.getRiskAttitude(), "eager");
  }

  @Test
  public void testDayPlanning ()
  {
    initialize("female");
    mockSeed.setIntSeed(new int[]{1, 2});
    mockSeed.setDoubleSeed(new double[]{0.5,0.5,0.3});
    mockSeed.resetCounters();
    evCustomer.makeDayPlanning(0, 0);
    EvCustomer.TimeslotData[] data =
        evCustomer.getTodayMap();
    assertEquals(24, data.length, "correct map size");
    // first trip should be 10 km in hour 9
    assertEquals(10, data[0].getHoursTillNextDrive(), "10 hours away");
    assertEquals(0.0, data[0].getIntendedDistance(), 1e-6, "no driving at 0");
    assertEquals(1, data[9].getHoursTillNextDrive(), "1 hr from 9");
    assertEquals(0.0, data[9].getIntendedDistance(), 1e-6, "no driving at 9");
    assertEquals(10.0, data[10].getIntendedDistance(), 1e-6, "10 km at 10");
  }

  // Single out-and-back activity
  @Test
  public void testDayPlanningInterval1 ()
  {
    addGroupActivity("i1", addActivity("interval6", 0, 6, 1.0),
                     10.0, 10.0, 1.0, 1.0);
    mockSeed.setIntSeed(new int[]{1,1,2});
    // one seed for init, four 1st pass through makeDayPlanning, three subsequent passes
    mockSeed.setDoubleSeed(new double[]{0.5,1.0,1.0,0.5,0.8});
    mockSeed.resetCounters();
    config("female");

    evCustomer.makeDayPlanning(0, 0);
    EvCustomer.TimeslotData[] data =
        evCustomer.getTodayMap();
    assertEquals(24, data.length, "correct map size");
    // first trip should be 10 km in hour 9
    assertEquals(11, data[0].getHoursTillNextDrive(), "11 hours away");
    assertEquals(10.0, data[0].getChargingCapacity(), 1e-6);
    assertEquals(0.0, data[0].getIntendedDistance(), 1e-6, "no driving in 0 hour");
    assertEquals(1, data[10].getHoursTillNextDrive(), "1 hr from 11");
    assertEquals(0.0, data[10].getIntendedDistance(), 1e-6, "no driving at 10");
    assertEquals(10.0, data[10].getChargingCapacity(), 1e-6);
    assertEquals(10.0, data[11].getIntendedDistance(), 1e-6, "10 km in hr 11");
    assertEquals(0.0, data[12].getIntendedDistance(), 1e-6, "no driving at 12");
    assertEquals(5.0, data[12].getChargingCapacity(), 1e-6);
    
    assertEquals(0.0, data[16].getIntendedDistance(), 1e-6, "no driving 16");
    assertEquals(5.0, data[16].getChargingCapacity(), 1e-6);
    assertEquals(10.0, data[17].getIntendedDistance(), 1e-6, "return 10 km hr 17");
    assertEquals(0.0, data[18].getIntendedDistance(), 1e-6, "no driving 18");
    assertEquals(10.0, data[18].getChargingCapacity(), 1e-6);
  }

  // single out-and-back, with simple activity partway through
  @Test
  public void testDayPlanningInterval1a ()
  {
    addGroupActivity("i1a", addActivity("interval8", 0, 8, 1.0),
                     20.0, 20.0, 1.0, 1.0);
    addGroupActivity("i2", addActivity("side trip", 1, 0),
                     8.0, 8.0, 1.0, 1.0);
    mockSeed.setIntSeed(new int[]{1,1,2,1,1,2});
    // one seed for init, four 1st pass through makeDayPlanning, three subsequent passes
    mockSeed.setDoubleSeed(new double[]{0.5,1.0,1.0,0.5,0.5,0.5,0.5,1.0,1.0,0.5,0.5,0.5,0.5,0.4});
    mockSeed.resetCounters();
    config("female");

    evCustomer.makeDayPlanning(0, 0);
    EvCustomer.TimeslotData[] data =
        evCustomer.getTodayMap();
    assertEquals(24, data.length, "correct map size");
    // first trip should be 10 km in hour 9
    assertEquals(9, data[0].getHoursTillNextDrive(), "9 hours away");
    assertEquals(0.0, data[0].getIntendedDistance(), 1e-6, "no driving in 0 hour");
    assertEquals(1, data[8].getHoursTillNextDrive(), "1 hr from 8");
    assertEquals(0.0, data[8].getIntendedDistance(), 1e-6, "no driving 9");
    assertEquals(20.0, data[9].getIntendedDistance(), 1e-6, "20 km in hr 10");
    assertEquals(0.0, data[10].getIntendedDistance(), 1e-6, "no driving in 11 hour");

    assertEquals(0.0, data[12].getIntendedDistance(), 1e-6, "no driving 13");
    assertEquals(8.0, data[13].getIntendedDistance(), 1e-6, "return 8 km hr 14");
    assertEquals(0.0, data[14].getIntendedDistance(), 1e-6, "no driving 15");

    assertEquals(0.0, data[16].getIntendedDistance(), 1e-6, "no driving 17");
    assertEquals(20.0, data[17].getIntendedDistance(), 1e-6, "return 20 km hr 18");
    assertEquals(0.0, data[18].getIntendedDistance(), 1e-6, "no driving 19");
  }

  @Test
  public void testDayPlanningInterval2 ()
  {
    
  }

  @Test
  public void testDayPlanningInterval2a ()
  {
    
  }

  @Test
  public void testConsumePowerRiskAverse ()
  {
    carType = new CarType("Test1");
    carType.configure("TestCar", 100.0, 200.0, 20.0, 10.0);

    mockSeed.setIntSeed(new int[]{0, 1});
    mockSeed.setDoubleSeed(new double[]{0});
    initialize("male");
    evCustomer.makeDayPlanning(0, 0);

    assertEquals(evCustomer.getRiskAttitude(), "averse");

    // Risk averse always charges when under 80%
    assertEquals(50, evCustomer.getCurrentCapacity(), 1E-06);
    double[] loads = evCustomer.getLoads(0, 0);
    assertEquals(20.0, loads[1], 1E-06);

    evCustomer.setCurrentCapacity(70);
    assertEquals(70, evCustomer.getCurrentCapacity(), 1E-06);
    loads = evCustomer.getLoads(0, 0);
    assertEquals(10.0, loads[1], 1E-06);

    evCustomer.setCurrentCapacity(90);
    assertEquals(90, evCustomer.getCurrentCapacity(), 1E-06);
    loads = evCustomer.getLoads(0, 0);
    assertEquals(0.0, loads[1], 1E-06);
  }

  @Test
  public void testConsumePowerRiskNeutral ()
  {
    carType = new CarType("Test2");
    carType.configure("TestCar", 100.0, 200.0, 20.0, 10.0);

    mockSeed.setIntSeed(new int[]{1, 1});
    mockSeed.setDoubleSeed(new double[]{0});
    initialize("male");
    evCustomer.makeDayPlanning(0, 0);

    assertEquals(evCustomer.getRiskAttitude(), "neutral");

    // Risk neutral always charges when under 60 %
    assertEquals(50, evCustomer.getCurrentCapacity(), 1E-06);
    double[] loads = evCustomer.getLoads(0, 0);
    assertEquals(10.0, loads[1], 1E-06);

    evCustomer.setCurrentCapacity(70);
    assertEquals(70, evCustomer.getCurrentCapacity(), 1E-06);
    loads = evCustomer.getLoads(0, 0);
    assertEquals(0.0, loads[1], 1E-06);
    assertEquals(20.0, loads[2], 1e-6);
    assertEquals(-20.0, loads[3], 1e-6);
    //System.out.format("loads=[%.3f,%.3f,%.3f,%.3f]\n",
    //                  loads[0], loads[1], loads[2], loads[3]);
  }

  @Test
  public void testConsumePowerRiskEager ()
  {
    carType = new CarType("Test3");
    carType.configure("TestCar", 100.0, 200.0, 20.0, 10.0);

    mockSeed.setIntSeed(new int[]{2, 1});
    mockSeed.setDoubleSeed(new double[]{0.0});
    initialize("male");
    evCustomer.makeDayPlanning(0, 0);

    assertEquals(evCustomer.getRiskAttitude(), "eager");

    // Risk eager always charges when under 40 %
    assertEquals(50, evCustomer.getCurrentCapacity(), 1E-06);
    double[] loads = evCustomer.getLoads(0, 0);
    assertEquals(0.0, loads[1], 1E-06);

    evCustomer.setCurrentCapacity(30);
    assertEquals(30, evCustomer.getCurrentCapacity(), 1E-06);
    loads = evCustomer.getLoads(0, 0);
    assertEquals(10.0, loads[1], 1E-06);
  }

  @Test
  public void testDominantLoad ()
  {
    initialize("male");

    double totalKms = 0.0;
    for (GroupActivity entry : evCustomer.getGroupActivities()) {
      totalKms += entry.getMaleDailyKm();
    }
    double totalKwh = evCustomer.getNeededCapacity(totalKms);

    assertEquals(totalKwh, evCustomer.getDominantLoad(), 1E-06);

    setUp();
    initialize("female");

    totalKms = 0.0;
    for (GroupActivity entry : evCustomer.getGroupActivities()) {
      totalKms += entry.getMaleDailyKm();
    }
    totalKwh = evCustomer.getNeededCapacity(totalKms);

    assertEquals(totalKwh, evCustomer.getDominantLoad(), 1E-06);
  }

  @Test
  public void testDoActivities ()
  {
    mockSeed.setIntSeed(new int[]{0, 0});
    initialize("male");
    evCustomer.makeDayPlanning(0, 1);

    assertEquals(50, evCustomer.getCurrentCapacity(), 1E-06);

    evCustomer.doActivities(0, 5);
    assertFalse(evCustomer.isDriving());
    evCustomer.doActivities(0, 6);
    assertTrue(evCustomer.isDriving());
    assertEquals(45.0, evCustomer.getCurrentCapacity(), 1E-06);
  }

  @Test
  public void testRegulationCapacityUpperLimit ()
  {
    carType = new CarType("Test2");
    carType.configure("TestCar", 100.0, 200.0, 20.0, 10.0);

    mockSeed.setIntSeed(new int[]{1, 1});
    mockSeed.setDoubleSeed(new double[]{0});
    initialize("male");
    evCustomer.makeDayPlanning(0, 0);

    assertEquals(evCustomer.getRiskAttitude(), "neutral");

    evCustomer.setCurrentCapacity(90.0);
    assertEquals(90.0, evCustomer.getCurrentCapacity(), 1E-06);
    double[] loads = evCustomer.getLoads(0, 0);
    assertEquals(0.0, loads[1], 1E-06);
    assertEquals(20.0, loads[2], 1e-6);
    assertEquals(-10.0, loads[3], 1e-6);
    //System.out.format("loads=[%.3f,%.3f,%.3f,%.3f]\n",
    //                  loads[0], loads[1], loads[2], loads[3]);
  }

  @Test
  public void testRegulationCapacityLowerLimit ()
  {
    carType = new CarType("Test2");
    carType.configure("TestCar", 100.0, 200.0, 20.0, 10.0);

    mockSeed.setIntSeed(new int[]{1, 1});
    mockSeed.setDoubleSeed(new double[]{0});
    initialize("male");
    evCustomer.makeDayPlanning(0, 0);

    assertEquals(evCustomer.getRiskAttitude(), "neutral");

    evCustomer.setCurrentCapacity(10.0);
    assertEquals(10.0, evCustomer.getCurrentCapacity(), 1E-06);
    double[] loads = evCustomer.getLoads(0, 0);
    assertEquals(20.0, loads[1], 1E-06);
    assertEquals(10.0, loads[2], 1e-6);
    assertEquals(0.0, loads[3], 1e-6);
    System.out.format("loads=[%.3f,%.3f,%.3f,%.3f]\n",
                      loads[0], loads[1], loads[2], loads[3]);
  }

  // =============== helper classes =================
  @SuppressWarnings("serial")
  class MockRandomSeed extends RandomSeed
  {
    private PredictableRandom delegate;

    public MockRandomSeed (String classname, long requesterId, String purpose)
    {
      super(classname, requesterId, purpose);
      delegate = new PredictableRandom();
    }

    // ======= passthrough methods =======
    @Override
    public double nextDouble ()
    {
      double result = delegate.nextDouble();
      //System.out.println("nextDouble " + result);
      return result;
    }

    void setDoubleSeed (double[] seed)
    {
      delegate.setDoubleSeed(seed);
    }

    @Override
    public int nextInt ()
    {
      int result = delegate.nextInt(1);
      //System.out.println("nextInt " + result);
      return result;
    }

    @Override
    public int nextInt (int n)
    {
      return delegate.nextInt(n);
    }

    void setIntSeed (int[] seed)
    {
      delegate.setIntSeed(seed);
    }

    void resetCounters ()
    {
      delegate.resetCounters();
    }
  }

  class ServiceAccessor implements CustomerServiceAccessor
  {
    @Override
    public CustomerRepo getCustomerRepo ()
    {
      return null;
    }

    @Override
    public RandomSeedRepo getRandomSeedRepo ()
    {
      return mockSeedRepo;
    }

    @Override
    public TariffRepo getTariffRepo ()
    {
      return null;
    }

    @Override
    public TariffSubscriptionRepo getTariffSubscriptionRepo ()
    {
      return mockSubscriptionRepo;
    }

    @Override
    public TimeslotRepo getTimeslotRepo ()
    {
      return null;
    }

    @Override
    public TimeService getTimeService ()
    {
      return mockTimeService;
    }

    @Override
    public WeatherReportRepo getWeatherReportRepo ()
    {
      return null;
    }

    @Override
    public ServerConfiguration getServerConfiguration ()
    {
      return null;
    }

    @Override
    public TariffMarket getTariffMarket ()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public XMLMessageConverter getMessageConverter ()
    {
      // TODO Auto-generated method stub
      return null;
    }
  }
}