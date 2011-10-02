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

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.du.DefaultBrokerInitializationService;
import org.powertac.du.DefaultBrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for a DefaultBrokerService. See AuctionServiceTests for a detailed
 * example. We use the Spring test runner for integration testing. Mocks
 * may be created in the test code, or may be instantiated by Spring and
 * autowired into the test. Test component configuration is in test-config.xml.
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/test-config.xml"})
public class GenericServiceTests
{
  @Autowired
  private DefaultBrokerService service;
  
  @Autowired
  private DefaultBrokerInitializationService defaultBrokerInitializationService;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    // clean up from previous tests
    pluginConfigRepo.recycle();
    timeslotRepo.recycle();
  }

  @Test
  public void testGenericService ()
  {
    fail("Not yet implemented");
  }

  @Test
  public void testInit ()
  {
    fail("Not yet implemented");
  }

  @Test
  public void testActivate ()
  {
    fail("Not yet implemented");
  }

  @Test
  public void testReceiveMessage ()
  {
    fail("Not yet implemented");
  }

}
