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

package org.powertac.factoredcustomer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Prashant Reddy, Antonios Chrysolpoulos
 */
public class FactoredCustomerInitializationServiceTests
{
    PluginConfigRepo pluginConfigRepo;
    FactoredCustomerInitializationService serviceUnderTest;

    /** sets up before each test */
    @Before
    public void setUp() throws Exception
    {
        // manual dependency injection
        pluginConfigRepo = new PluginConfigRepo();
        serviceUnderTest = new FactoredCustomerInitializationService();
        ReflectionTestUtils.setField(serviceUnderTest, "pluginConfigRepo", pluginConfigRepo);
    }

    /**
     * Makes sure the pluginConfig instance gets created correctly.
     */
    @Test
    public void testSetDefaults()
    {
        // mock and inject the service and the brokerRepo
        FactoredCustomerService service = mock(FactoredCustomerService.class);
        ReflectionTestUtils.setField(serviceUnderTest,"factoredCustomerService", service);

        serviceUnderTest.setDefaults();

        PluginConfig config = pluginConfigRepo.findByRoleName("FactoredCustomer");
        assertNotNull("Found config", config);
        assertEquals("Correct config resource", "FactoredCustomers.xml", config.getConfigurationValue("configResource"));
    }

    /**
     * Confirms that the initialization service correctly initializes its
     * service.
     * 
     * @throws IOException
     */
    @Test
    public void testInitialize() throws IOException
    {
        // mock the service and the competition instance
        FactoredCustomerService service = mock(FactoredCustomerService.class);
        Competition competition = mock(Competition.class);

        // dependency injection
        ReflectionTestUtils.setField(serviceUnderTest, "factoredCustomerService", service);

        // set defaults
        serviceUnderTest.setDefaults();
        // run the initialize method, confirm it makes the correct calls
        List<String> completedInits = new ArrayList<String>();
        completedInits.add("DefaultBroker");
        String result = serviceUnderTest.initialize(competition, completedInits);
        assertEquals("correct result", "FactoredCustomer", result);
        PluginConfig config = pluginConfigRepo.findByRoleName("FactoredCustomer");
        verify(service).init(config);
    }

    /**
     * Confirms correct failed initialization behavior.
     * 
     * @throws IOException
     */
    @Test
    public void testInitializeFail() throws IOException
    {
        // mock the service and the competition instance
        FactoredCustomerService service = mock(FactoredCustomerService.class);
        Competition competition = mock(Competition.class);
        // dependency injection
        ReflectionTestUtils.setField(serviceUnderTest,"factoredCustomerService", service);
        // Do not set defaults
        // run the initialize method, make sure it fails correctly
        List<String> completedInits = new ArrayList<String>();
        completedInits.add("DefaultBroker");
        String result = serviceUnderTest.initialize(competition, completedInits);
        assertEquals("failure result", "fail", result);
        verify(service, never()).init((PluginConfig) anyObject());
    }
}
