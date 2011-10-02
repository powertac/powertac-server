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
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.du.DefaultBrokerInitializationService;
import org.powertac.du.DefaultBrokerService;
import org.springframework.test.util.ReflectionTestUtils;

public class GenericInitializationServiceTests
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
    serviceUnderTest.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("Generic");
    assertNotNull("found config", config);
    assertEquals("correct parameter", "42",
                 config.getConfigurationValue("parameter"));
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
    Competition competition = mock(Competition.class);
    // dependency injection
    ReflectionTestUtils.setField(serviceUnderTest, "genericService", service);
    // set defaults
    serviceUnderTest.setDefaults();
    // run the initialize method, confirm it makes the correct calls
    List<String> completedInits = new ArrayList<String>();
    completedInits.add("Other");
    String result = serviceUnderTest.initialize(competition, completedInits);
    assertEquals("correct result", "Generic", result);
    PluginConfig config = pluginConfigRepo.findByRoleName("Generic");
    verify(service).init(config);
  }
  
  /**
   * Confirms correct deferred initialization behavior.
   */
  @Test
  public void testInitializeNull ()
  {
    // mock the service and the competition instance
    DefaultBrokerService service = mock(DefaultBrokerService.class);
    Competition competition = mock(Competition.class);
    // dependency injection
    ReflectionTestUtils.setField(serviceUnderTest, "genericService", service);
    // set defaults
    serviceUnderTest.setDefaults();
    // run the initialize method, make sure it makes the correct calls
    List<String> completedInits = new ArrayList<String>();
    //completedInits.add("Other");
    String result = serviceUnderTest.initialize(competition, completedInits);
    assertNull("null result", result);
    verify(service, never()).init((PluginConfig) anyObject());
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
    ReflectionTestUtils.setField(serviceUnderTest, "genericService", service);
    // Do not set defaults
    // run the initialize method, make sure it fails correctly
    List<String> completedInits = new ArrayList<String>();
    completedInits.add("Other");
    String result = serviceUnderTest.initialize(competition, completedInits);
    assertEquals("failure result", "fail", result);
    verify(service, never()).init((PluginConfig) anyObject());
  }
}
