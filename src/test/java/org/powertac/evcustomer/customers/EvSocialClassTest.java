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

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
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
import org.powertac.evcustomer.ConfigTest;
import org.powertac.evcustomer.PredictableRandom;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.GroupActivity;
import org.powertac.evcustomer.beans.CarType;
import org.powertac.evcustomer.beans.SocialGroup;
import org.powertac.evcustomer.beans.ClassGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * @author Govert Buijs
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:test-config.xml"})
//@DirtiesContext
public class EvSocialClassTest
{
  //private TimeService timeService;

  private TimeslotRepo timeslotRepo;

  private CustomerRepo mockCustomerRepo;

  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed mockSeed;

  private TariffMarket mockTariffMarket;

  private DummyConfig serverConfiguration;
  private ServiceAccessor service;

  private String className = "HighIncome_2";

  private Instant now;

  private int seedId = 1;

  private EvSocialClass evSocialClass;

  @Before
  public void setUp ()
  {
    evSocialClass = new EvSocialClass(className);
    mockSeedRepo = mock(RandomSeedRepo.class);
    mockSeed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(anyString(),
                                    anyInt(),
                                    anyString())).thenReturn(mockSeed);
    mockCustomerRepo = mock(CustomerRepo.class);

    serverConfiguration = new DummyConfig();
    serverConfiguration.initialize();
    Config config = Config.getInstance();
    ReflectionTestUtils.setField(config, "serverConfiguration",
                                 serverConfiguration);

    service = new ServiceAccessor();

//    socialGroup = new SocialGroup(groupId, groupName);
//    socialGroups = new HashMap<Integer, SocialGroup>();
//    activities = new HashMap<Integer, Activity>();
//    activity = new Activity(0, "Test Activity", 1.0, 1.0);
//    groupActivities = new HashMap<Integer, GroupActivity>();
//    groupActivity = new GroupActivity(0, 10, 10, 1.0, 1.0);
//    carTypes = new ArrayList<CarType>();
//
//    customerRepo.recycle();
//    tariffSubscriptionRepo.recycle();
//    tariffRepo.recycle();
//    Broker broker1 = new Broker("Joe");
//
//    now = new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
//    timeService.setCurrentTime(now.toInstant());
//    Instant exp = new Instant(now.getMillis() + TimeService.WEEK * 10);
//
//    defaultTariffSpec =
//        new TariffSpecification(broker1, PowerType.CONSUMPTION)
//            .withExpiration(exp).withMinDuration(TimeService.WEEK * 8)
//            .addRate(new Rate().withValue(-0.222));
//    defaultTariff = new Tariff(defaultTariffSpec);
//    defaultTariff.init();
//    defaultTariff.setState(Tariff.State.OFFERED);
//
//    defaultTariffSpecEV =
//        new TariffSpecification(broker1, PowerType.ELECTRIC_VEHICLE)
//            .withExpiration(exp).withMinDuration(TimeService.WEEK * 8)
//            .addRate(new Rate().withValue(-0.121).withMaxCurtailment(0.3));
//    defaultTariffEV = new Tariff(defaultTariffSpecEV);
//    defaultTariffEV.init();
//    defaultTariffEV.setState(Tariff.State.OFFERED);
//
//    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
//        .thenReturn(defaultTariff);
//    when(mockTariffMarket.getDefaultTariff(PowerType.ELECTRIC_VEHICLE))
//        .thenReturn(defaultTariffEV);
  }

  private void initializeClass ()
  {
    evSocialClass.setServiceAccessor(service);
    evSocialClass.setMinCount(2);
    evSocialClass.setMaxCount(4);
    evSocialClass.initialize();
  }

  @Test
  public void testInitialization ()
  {
    initializeClass();

    assertEquals("Correct name", className, evSocialClass.getName());

    //serverConfiguration.configureMe(evSocialClass);
    assertEquals("correct min count", 2, evSocialClass.getMinCount());
  }

  @Test
  public void testBeans ()
  {
    initializeClass();
    Map<Integer, SocialGroup> groups = evSocialClass.getGroups();
    assertEquals("3 groups", 3, groups.size());
    assertEquals("includes parttime", "parttime", groups.get(1).getName());

    Map<Integer, ClassGroup> classGroups = evSocialClass.getClassGroups();
    assertEquals("twelve class-groups", 3, classGroups.size());
    ClassGroup hi2_1 = classGroups.get(1);
    assertEquals("correct socialClassName",
                 "HighIncome_2", hi2_1.getSocialClassName());
    assertEquals("correct probability", 0.125, hi2_1.getProbability(), 1e-6);
  }

  @Test
  public void testEvCustomers ()
  {
    initializeClass();
    assertEquals("correct min count", 2, evSocialClass.getMinCount());
    assertEquals("correct max count", 4, evSocialClass.getMaxCount());
    ArrayList<EvCustomer> customers = evSocialClass.getEvCustomers();
    assertEquals("correct number of customers", 2, customers.size());
    assertEquals("correct number of infos", 2,
                 evSocialClass.getCustomerInfos().size());
  }

////  @Test
////  public void testActivities ()
////  {
////    maleProbability = 1;
////    Random predictable = new PredictableRandom(new double[]{0}, new int[]{0});
////
////    initializeClass();
////    EvCustomer evCustomer = evSocialClass.getEvCustomers().get(0);
////    evCustomer.setGenerator(predictable);
////    evCustomer.makeDayPlanning(0);
////    evCustomer.setRiskAttitude(0);
////
////    assertEquals(50, carType.getCurrentCapacity(), 1E-6);
////    evSocialClass.doActivities(0, 6);
////    assertEquals(25, carType.getCurrentCapacity(), 1E-6);
////  }
////
////  @Test
////  public void testConsumePower ()
////  {
////    // Make sure the battery is 25% full
////    testActivities();
////
////    EvCustomer evCustomer = evSocialClass.getEvCustomers().get(0);
////    CarType car2 = evCustomer.getCar();
////
////    // Normally isDriving would be set by doActivity
////    evCustomer.setDriving(false);
////
////    TariffSubscription ts = new TariffSubscription(info2, defaultTariffEV);
////    ts.subscribe(info2.getPopulation());
////    tariffSubscriptionRepo.add(ts);
////
////    assertEquals(25, car2.getCurrentCapacity(), 1E-6);
////
////    evSocialClass.getLoads(0, 0);
////    assertEquals(45, car2.getCurrentCapacity(), 1E-6);
////
////    evSocialClass.getLoads(0, 6);
////    assertEquals(65, car2.getCurrentCapacity(), 1E-6);
////
////    evSocialClass.getLoads(0, 7);
////    assertEquals(80, car2.getCurrentCapacity(), 1E-6);
////
////    evSocialClass.getLoads(0, 8);
////    assertEquals(80, car2.getCurrentCapacity(), 1E-6);
////
////    for (CustomerInfo customerInfo : evSocialClass.getCustomerInfos()) {
////      List<TariffSubscription> subs = tariffSubscriptionRepo
////          .findActiveSubscriptionsForCustomer(customerInfo);
////
////      assertTrue("EvSocialClass consumed power for each customerInfo",
////          subs.size() == 0 || subs.get(0).getTotalUsage() >= 0);
////    }
////  }
//
//  @Test
//  public void testTariffEvaluator ()
//  {
//    initializeClass();
//
//    EvCustomer evCustomer = evSocialClass.getEvCustomers().get(0);
//    Random generator = new PredictableRandom(
//        new double[]{0, 1, 0, 1}, new int[]{0});
//    double weight = 0.5;
//    double weeks = Config.maxDefaultDuration;
//
//    TariffEvaluator tariffEvaluator =
//        evSocialClass.createTariffEvaluator(info, weight, weeks);
//
//    assertEquals(tariffEvaluator.getInterruptibilityFactor(),
//        Config.INTERRUPTIBILITY_FACTOR, 1E-6);
//    assertEquals(tariffEvaluator.getTieredRateFactor(),
//        Config.TIERED_RATE_FACTOR, 1E-6);
//    assertEquals(tariffEvaluator.getTouFactor(),
//        Config.TOU_FACTOR, 1E-6);
//    assertEquals(tariffEvaluator.getVariablePricingFactor(),
//        Config.VARIABLE_PRICING_FACTOR, 1E-6);
//
//    TariffEvaluationWrapper wrapper = new TariffEvaluationWrapper(
//        info, evSocialClass.getEvCustomers(), generator);
//
//    assertEquals(info.getName(), wrapper.getCustomerInfo().getName());
//    assertEquals(wrapper.getInertiaSample(), 0, 1E-6);
//    assertEquals(wrapper.getInertiaSample(), 1, 1E-6);
//    assertEquals(wrapper.getTariffChoiceSample(), 0, 1E-6);
//    assertEquals(wrapper.getTariffChoiceSample(), 1, 1E-6);
//    assertEquals(wrapper.getBrokerSwitchFactor(false),
//        Config.BROKER_SWITCH_FACTOR, 1E-6);
//    assertEquals(wrapper.getBrokerSwitchFactor(true),
//        5 * Config.BROKER_SWITCH_FACTOR, 1E-6);
//
//    double[] profile0 = wrapper.getCapacityProfile(defaultTariff);
//    double[] profile1 = wrapper.getCapacityProfile(defaultTariffEV);
//
//    assertEquals(profile0.length, profile1.length);
//    for (int i = 0; i < profile0.length; i++) {
//      double load = evCustomer.getDominantLoad() / Config.HOURS_OF_DAY;
//      assertEquals(profile0[i], load, 1E-6);
//      assertEquals(profile1[i], load, 1E-6);
//    }
//    assertEquals(profile0.length, 24);
//  }

  class DummyConfig implements ServerConfiguration
  {
    private Configurator configurator;
    private CompositeConfiguration config;

    DummyConfig ()
    {
      super();
    }

    void initialize ()
    {
      config = new CompositeConfiguration();
      configurator = new Configurator();
      InputStream stream =
          ConfigTest.class.getResourceAsStream("/config/test-properties.xml");
      XMLConfiguration xconfig = new XMLConfiguration();
      try {
        xconfig.load(stream);
        config.addConfiguration(xconfig);
        configurator.setConfiguration(config);
      }
      catch (ConfigurationException e) {
        e.printStackTrace();
        fail(e.toString());
      }
    }

    @Override
    public void configureMe (Object target)
    {
      configurator.configureSingleton(target);
    }

    @Override
    public Collection<?> configureInstances (Class<?> target)
    {
      return configurator.configureInstances(target);
    }

    @Override
    public void publishConfiguration (Object target)
    {
      
    }

    @Override
    public void saveBootstrapState (Object thing)
    {
      
    }
  }

  class ServiceAccessor implements CustomerServiceAccessor
  {

    @Override
    public CustomerRepo getCustomerRepo ()
    {
      return mockCustomerRepo;
    }

    @Override
    public RandomSeedRepo getRandomSeedRepo ()
    {
      return mockSeedRepo;
    }

    @Override
    public TariffRepo getTariffRepo ()
    {
      return tariffRepo;
    }

    @Override
    public TariffSubscriptionRepo getTariffSubscriptionRepo ()
    {
      return tariffSubscriptionRepo;
    }

    @Override
    public TimeslotRepo getTimeslotRepo ()
    {
      return timeslotRepo;
    }

    @Override
    public WeatherReportRepo getWeatherReportRepo ()
    {
      return null;
    }
  }
}
