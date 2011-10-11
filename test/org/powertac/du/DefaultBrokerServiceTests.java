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
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.du.DefaultBrokerInitializationService;
import org.powertac.du.DefaultBrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for a DefaultBrokerService. See AuctionServiceTests for a detailed
 * example. We use the Spring test runner for integration testing. Mocks
 * may be created in the test code, or may be instantiated by Spring and
 * autowired into the test. Test component configuration is in test-config.xml.
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/test-config.xml"})
public class DefaultBrokerServiceTests
{
  @Autowired
  private TimeService timeService;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private BrokerProxy mockProxy;
  
  private TariffMarket mockMarket; // not autowired
  
  private DefaultBrokerInitializationService initializer;
  private DefaultBrokerService service;
  private CustomerInfo customer1;
  private CustomerInfo customer2;
  private Competition competition;
  private Instant start;

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
    reset(mockProxy);
    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(start);
    customer1 = new CustomerInfo("town", 1000);
    customer2 = new CustomerInfo("village", 200);
    
    service = new DefaultBrokerService();
    initializer = new DefaultBrokerInitializationService();
    ReflectionTestUtils.setField(initializer,
                                 "defaultBrokerService",
                                 service);
    ReflectionTestUtils.setField(initializer,
                                 "brokerRepo",
                                 brokerRepo);
    ReflectionTestUtils.setField(initializer,
                                 "pluginConfigRepo",
                                 pluginConfigRepo);
    mockMarket = mock(TariffMarket.class);
    ReflectionTestUtils.setField(service, "tariffMarketService", mockMarket);

    
    competition = Competition.newInstance("broker-test");
  }

  private Broker init ()
  {
    initializer.setDefaults();
    initializer.initialize(competition, new ArrayList<String>());
    Broker face = brokerRepo.findByUsername("default broker");
    return face;
  }

  @Test
  public void testService ()
  {
    Broker face = init();
    assertNotNull("found face", face);
    assertEquals("correct face", face, service.getFace());
  }
  
  @SuppressWarnings("rawtypes")
  @Test
  public void testDefaultTariffPublication ()
  {
    final ArrayList<TariffSpecification> specs = new ArrayList<TariffSpecification>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        specs.add((TariffSpecification)args[0]);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));
    
    Broker face = init();

    // should now have two tariff specs, one for production and
    // one for consumption
    boolean foundProduction = false;
    boolean foundConsumption = false;
    
    assertEquals("two default tariffs", 2, specs.size());
    for (TariffSpecification spec : specs) {
      if (spec.getPowerType() == PowerType.CONSUMPTION) {
        foundConsumption = true;
        assertEquals("correct issuer", face, spec.getBroker());
        List<Rate> rates = spec.getRates();
        assertEquals("just one rate", 1, rates.size());
        assertTrue("fixed rate", rates.get(0).isFixed());
        assertEquals("correct rate", 0.5, rates.get(0).getValue(), 1e-6);
      }
      else if (spec.getPowerType() == PowerType.PRODUCTION) {
        foundProduction = true;
        assertEquals("correct issuer", face, spec.getBroker());
        List<Rate> rates = spec.getRates();
        assertEquals("just one rate", 1, rates.size());
        assertTrue("fixed rate", rates.get(0).isFixed());
        assertEquals("correct rate", -0.02, rates.get(0).getValue(), 1e-6);
      }
    }
    assertTrue("found a consumption tariff", foundConsumption);
    assertTrue("found a production tariff", foundProduction);
  }

  // incoming messages drive all activity. These include TariffTransaction,
  // MarketPosition, and TimeslotUpdate.
  @SuppressWarnings("rawtypes")
  @Test
  public void testReceiveSignupMessage ()
  {
    final HashMap<PowerType, TariffSpecification> specs = 
      new HashMap<PowerType, TariffSpecification>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        TariffSpecification spec =(TariffSpecification)args[0];
        specs.put(spec.getPowerType(), spec);
        return null;
      }
    }).when(mockMarket).setDefaultTariff(isA(TariffSpecification.class));

    Broker face = init();
    HashMap<String, Integer> customerCounts = service.getCustomerCounts();
    assertEquals("no customers yet", 0, customerCounts.size());
    
    face.receiveMessage(new TariffTransaction(face,
                                              timeService.getCurrentTime(),
                                              TariffTransaction.Type.SIGNUP, 
                                              specs.get(PowerType.CONSUMPTION),
                                              customer1, 
                                              customer1.getPopulation(),
                                              0.0, 4.2));
    // now one customer, population=1000
    customerCounts = service.getCustomerCounts();
    assertEquals("one customer", 1, customerCounts.size());
    int count = customerCounts.get(customer1.getName() + PowerType.CONSUMPTION);
    assertEquals("1000 individuals", 1000, count);
  }

}
