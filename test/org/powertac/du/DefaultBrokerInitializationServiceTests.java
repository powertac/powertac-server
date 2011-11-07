/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.du;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyObject;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.du.DefaultBrokerInitializationService;
import org.powertac.du.DefaultBrokerService;
import org.springframework.test.util.ReflectionTestUtils;

public class DefaultBrokerInitializationServiceTests
{
  PluginConfigRepo pluginConfigRepo;
  DefaultBrokerInitializationService serviceUnderTest;

  /** Initializes log4j */
  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  /** sets up before each test */
  @Before
  public void setUp () throws Exception
  {
    // manual dependency injection
    pluginConfigRepo = new PluginConfigRepo();
    serviceUnderTest = new DefaultBrokerInitializationService();
    ReflectionTestUtils.setField(serviceUnderTest,
                                 "pluginConfigRepo",
                                 pluginConfigRepo);
  }

  /**
   * Makes sure the pluginConfig instance gets created correctly.
   */
  @Test
  public void testSetDefaults ()
  {
    // mock and inject the service and the brokerRepo
    DefaultBrokerService service = mock(DefaultBrokerService.class);
    ReflectionTestUtils.setField(serviceUnderTest, "defaultBrokerService", service);
    BrokerRepo repo = mock(BrokerRepo.class);
    ReflectionTestUtils.setField(serviceUnderTest, "brokerRepo", repo);

    serviceUnderTest.setDefaults();
    verify(service).createBroker("default broker");
    verify(repo).add(null);
    
    PluginConfig config = pluginConfigRepo.findByRoleName("defaultBroker");
    assertNotNull("found config", config);
    assertEquals("correct consumption rate", "-0.5",
                 config.getConfigurationValue("consumptionRate"));
    assertEquals("correct productionRate", "0.02",
                 config.getConfigurationValue("productionRate"));
    assertEquals("correct bid kwh", "1000.0",
                 config.getConfigurationValue("initialBidKWh"));
    assertEquals("correct buy limit price", "-100.0",
                 config.getConfigurationValue("buyLimitPrice"));
    assertEquals("correct sell limit price", "1.0",
                 config.getConfigurationValue("sellLimitPrice"));
  }

  /**
   * Confirms that the initialization service correctly initializes
   * its service.
   */
  @Test
  public void testInitialize ()
  {
    // mock the service and the competition instance
    DefaultBrokerService service = mock(DefaultBrokerService.class);
    BrokerRepo repo = mock(BrokerRepo.class);
    Competition competition = mock(Competition.class);
    // dependency injection
    ReflectionTestUtils.setField(serviceUnderTest, "defaultBrokerService", service);
    ReflectionTestUtils.setField(serviceUnderTest, "brokerRepo", repo);

    // set defaults
    serviceUnderTest.setDefaults();
    // run the initialize method, confirm it makes the correct calls
    List<String> completedInits = new ArrayList<String>();
    completedInits.add("TariffMarket");
    String result = serviceUnderTest.initialize(competition, completedInits);
    assertEquals("correct result", "DefaultBroker", result);
    PluginConfig config = pluginConfigRepo.findByRoleName("defaultBroker");
    verify(service).init(config);
  }
  
  /**
   * Confirms correct failed initialization behavior.
   */
  @Test
  public void testInitializeFail ()
  {
    // mock the service and the competition instance
    DefaultBrokerService service = mock(DefaultBrokerService.class);
    Competition competition = mock(Competition.class);
    // dependency injection
    ReflectionTestUtils.setField(serviceUnderTest, "defaultBrokerService", service);
    // Do not set defaults
    // run the initialize method, make sure it fails correctly
    List<String> completedInits = new ArrayList<String>();
    completedInits.add("TariffMarket");
    String result = serviceUnderTest.initialize(competition, completedInits);
    assertEquals("failure result", "fail", result);
    verify(service, never()).init((PluginConfig) anyObject());
  }
}
