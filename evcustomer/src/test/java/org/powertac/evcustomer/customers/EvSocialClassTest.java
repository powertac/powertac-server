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
import static org.mockito.Mockito.*;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.RandomSeed;
import org.powertac.common.TimeService;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.ConfigurationRecorder;
import org.powertac.common.config.Configurator;
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
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.CarType;
import org.powertac.evcustomer.beans.ClassCar;
import org.powertac.evcustomer.beans.ClassGroup;
import org.powertac.evcustomer.beans.SocialGroup;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * @author Govert Buijs, John Collins
 */
public class EvSocialClassTest
{
  private TimeslotRepo timeslotRepo;

  private CustomerRepo mockCustomerRepo;

  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed mockSeed;

  private DummyConfig serverConfiguration;
  private ServiceAccessor service;

  private String className = "HighIncome_2";

  private EvSocialClass evSocialClass;

  @BeforeEach
  public void setUp ()
  {
    evSocialClass = new EvSocialClass(className);
    mockSeedRepo = mock(RandomSeedRepo.class);
    mockSeed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(anyString(),
                                    anyLong(),
                                    anyString())).thenReturn(mockSeed);
    mockCustomerRepo = mock(CustomerRepo.class);

    serverConfiguration = new DummyConfig();
    serverConfiguration.initialize();
    Config config = Config.getInstance();
    ReflectionTestUtils.setField(config, "serverConfiguration",
                                 serverConfiguration);

    service = new ServiceAccessor();
  }

  @AfterEach
  public void shutDown ()
  {
    Config.recycle();
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
    assertEquals(className, evSocialClass.getName(), "Correct name");
    assertEquals(2, evSocialClass.getMinCount(), "correct min count");
  }

  @Test
  public void testBeans ()
  {
    initializeClass();
    Map<Integer, SocialGroup> groups = evSocialClass.getGroups();
    assertEquals(3, groups.size(), "3 groups");
    assertEquals(groups.get(1).getName(), "parttime", "includes parttime");

    Map<String, CarType> carTypes = evSocialClass.getCarTypes();
    assertEquals(2, carTypes.size(), "2 cartypes");
    assertTrue(carTypes.keySet().contains("Tesla_40_kWh"), "includes Tesla_40_kWh");
    assertTrue(carTypes.keySet().contains("Nissan_Leaf_24_kWh"), "includes Nissan_Leaf_24_kWh");

    Map<Integer, Activity> activities = evSocialClass.getActivities();
    assertEquals(2, activities.size(), "2 activities");
    assertEquals("commuting", activities.get(0).getName(), "commuting activity");
    assertEquals("business_trip", activities.get(1).getName(), "business_trip activity");

    Map<String, ClassCar> classCars = evSocialClass.getClassCars();
    assertEquals(2, classCars.size(), "2 cartypes");
    assertTrue(classCars.keySet().contains("Tesla_40_kWh"), "includes Tesla_40_kWh");
    assertTrue(classCars.keySet().contains("Nissan_Leaf_24_kWh"), "includes Nissan_Leaf_24_kWh");

    Map<Integer, ClassGroup> classGroups = evSocialClass.getClassGroups();
    assertEquals(3, classGroups.size(), "twelve class-groups");
    ClassGroup hi2_1 = classGroups.get(1);
    assertEquals("HighIncome_2", hi2_1.getSocialClassName(), "correct socialClassName");
    assertEquals(0.125, hi2_1.getProbability(), 1e-6, "correct probability");
  }

  @Test
  public void testEvCustomers ()
  {
    initializeClass();
    assertEquals(2, evSocialClass.getMinCount(), "correct min count");
    assertEquals(4, evSocialClass.getMaxCount(), "correct max count");
    ArrayList<EvCustomer> customers = evSocialClass.getEvCustomers();
    assertEquals(2, evSocialClass.getPopulation(), "correct population");
    assertEquals(2, customers.size(), "correct number of customers");
    assertEquals(2, evSocialClass.getCustomerInfos().size(), "correct number of infos");
    assertEquals("HighIncome_2_0", customers.get(0).getName(), "correct name");
    assertEquals("0.male.Tesla_40_kWh.x", evSocialClass.getCustomerAttributeList().get(0), "correct boot-config list 0");
    assertEquals("0.male.Tesla_40_kWh.x", evSocialClass.getCustomerAttributeList().get(1), "correct boot-config list 1");
  }

  @Test
  public void testBootConfig ()
  {
    initializeClass();
    Configurator testConfig = new Configurator();
    ConfigurationPublisher pub = new ConfigurationPublisher();
    testConfig.gatherBootstrapState(evSocialClass, pub);
    assertEquals(1, pub.getConfig().size(), "one property");
    Object pop =
        pub.getConfig().get("evcustomer.customers.evSocialClass.customerAttributeList");
    @SuppressWarnings("unchecked")
    List<String> popList = (List<String>)pop;
    assertEquals(2, popList.size(), "2 items");
    assertEquals("0.male.Tesla_40_kWh.x", popList.get(0), "correct customer instance 0");
    assertEquals("0.male.Tesla_40_kWh.x", popList.get(1), "correct customer instance 1");
  }

  // Boot-restore is triggered and creates the correct objects
  @Test
  public void testBootRestoreTrigger ()
  {
    ArrayList<String> gcList = new ArrayList<String>();
    gcList.add("0.male.Tesla_40_kWh.0");
    gcList.add("2.female.Nissan_Leaf_24_kWh.1");
    gcList.add("1.female.Tesla_40_kWh.2");
    ReflectionTestUtils.setField(evSocialClass, "customerAttributeList",
                                 gcList);
    initializeClass();
    assertEquals(3, evSocialClass.getPopulation(), "3 instances");
    List<EvCustomer> customers = evSocialClass.getEvCustomers();
    assertEquals(3, customers.size(), "3 in list");
    EvCustomer cust = customers.get(0);
    assertEquals(cust.getName(), className + "_0", "correct name");
    assertEquals(0, cust.getSocialGroup().getId(), "correct group");
    assertEquals(cust.getGender(), "male", "correct gender");
    assertEquals(cust.getCar().getName(), "Tesla_40_kWh", "correct car");
    cust = customers.get(2);
    assertEquals(cust.getName(), className + "_2", "correct name");
    assertEquals(1, cust.getSocialGroup().getId(), "correct group");
    assertEquals(cust.getGender(), "female", "correct gender");
    assertEquals(cust.getCar().getName(), "Tesla_40_kWh", "correct car");
  }

  // Boot-restore works from a Configuration instance
  @Test
  public void testBootRestoreConfig ()
  {
    // need to configure manually for test
    serverConfiguration.addXmlConfiguration("config/test-properties.xml");
    Collection<?> escs =
        serverConfiguration.configureInstances(EvSocialClass.class);

    assertEquals(4, escs.size(), "four classes");
    EvSocialClass target = null;
    Iterator<?> targets = escs.iterator();
    while (targets.hasNext() && null == target) {
      EvSocialClass candidate = (EvSocialClass)targets.next();
      if (candidate.getName().equals("HighIncome_2")) {
        target = candidate;
      }
    }

    assertNotNull(target, "found target");
    assertNull(target.getCustomerAttributeList(), "customer attribute list created");

    target.setServiceAccessor(service);
    target.initialize();

    // make sure initialization does not mess it up
    assertNotNull(target.getCustomerAttributeList(), "customer attribute list created");
    assertEquals(15, target.getCustomerAttributeList().size(), "15 elements of customer attribute list");

    List<EvCustomer> customers = target.getEvCustomers();
    assertEquals(15, customers.size(), "15 customers");
    assertFalse(customers.get(0).isDriving(), "first customer is not driving");
  }

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

      try {
        config.addConfiguration(Configurator.readXML("config/test-properties.xml"));
        configurator.setConfiguration(config);
      }
      catch (Exception e) {
        e.printStackTrace();
        fail(e.toString());
      }
    }

    void addXmlConfiguration (String path)
    {
      try {
        config.addConfiguration(Configurator.readXML(path));
      }
      catch (Exception e) {
        e.printStackTrace();
        fail(e.toString());
      }
    }

    void addPropertiesConfiguration (String path)
    {
      try {
        config.addConfiguration(Configurator.readProperties(path));
      }
      catch (Exception e) {
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

    @Override
    public Collection<?> configureNamedInstances (List<?> instances)
    {
      return configurator.configureNamedInstances(instances);
    }

    void printKeys ()
    {
      Iterator<String> keys = config.getKeys();
      while (keys.hasNext()) {
        String key = keys.next();
        System.out.println(key);
      }
    }
  }

  /**
   * Configuration recorder for publishing config info to brokers
   */
  class ConfigurationPublisher implements ConfigurationRecorder
  {
    Properties publishedConfig;
    
    ConfigurationPublisher ()
    {
      publishedConfig = new Properties();
    }

    @Override
    public void recordItem (String key, Object value)
    {
      publishedConfig.put(key, value);      
    }
    
    Properties getConfig ()
    {
      return publishedConfig;
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
    public TimeService getTimeService ()
    {
      return null;
    }

    @Override
    public WeatherReportRepo getWeatherReportRepo ()
    {
      return null;
    }

    @Override
    public ServerConfiguration getServerConfiguration ()
    {
      return serverConfiguration;
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
