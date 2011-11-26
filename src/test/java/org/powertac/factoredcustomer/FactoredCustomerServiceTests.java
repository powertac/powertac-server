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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
//import org.powertac.accounting.AccountingInitializationService;
//import org.powertac.accounting.AccountingService;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.PauseRelease;
import org.powertac.common.msg.PauseRequest;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
//import org.powertac.tariffmarket.TariffMarketInitializationService;
//import org.powertac.tariffmarket.TariffMarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Prashant Reddy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:src/test/resources/test-config.xml" })
@DirtiesContext
public class FactoredCustomerServiceTests
{

  @Autowired
  private TimeService timeService;

//  @Autowired
//  private AccountingService accountingService;

//  @Autowired
//  private AccountingInitializationService accountingInitializationService;

//  @Autowired
//  private TariffMarketService tariffMarketService;

//  @Autowired
//  private TariffMarketInitializationService tariffMarketInitializationService;

  @Autowired
  private FactoredCustomerService factoredCustomerService;

  @Autowired
  private FactoredCustomerInitializationService factoredCustomerInitializationService;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private PluginConfigRepo pluginConfigRepo;

  private Instant exp;
  private Broker broker1;
  private Broker broker2;
  private Instant now;
  private TariffSpecification defaultTariffSpec;
  private Competition comp;

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("src/test/resources/log.config");
  }

  @Before
  public void setUp ()
  {
    customerRepo.recycle();
    brokerRepo.recycle();
    tariffRepo.recycle();
    tariffSubscriptionRepo.recycle();
    pluginConfigRepo.recycle();
    randomSeedRepo.recycle();
    timeslotRepo.recycle();

    // create a Competition, needed for initialization
    comp = Competition.newInstance("factored-customer-test");

    broker1 = new Broker("Joe");
    broker2 = new Broker("Anna");

    now = new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(now);
    timeService.setBase(now.getMillis());
    //timeService.setStart(now.getMillis());
    exp = now.plus(TimeService.WEEK * 10);

    List<String> inits = new ArrayList<String>();
/**
    accountingInitializationService.setDefaults();
    accountingInitializationService.initialize(comp, inits);

    inits.add("AccountingService");

    tariffMarketInitializationService.setDefaults();
    tariffMarketInitializationService.initialize(comp, inits);

    defaultTariffSpec = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(exp).withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(0.222));
    tariffMarketService.setDefaultTariff(defaultTariffSpec);
**/
  }

  @After
  public void shutDown ()
  {
    // VillageService.shutDown();
  }

  public void initializeService ()
  {
    factoredCustomerInitializationService.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("FactoredCustomer");
    config.getConfiguration().put("configResource", "FactoredCustomers.xml");
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    factoredCustomerInitializationService.initialize(comp, inits);
  }

  @Test
  public void testNormalInitialization ()
  {
    factoredCustomerInitializationService.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("FactoredCustomer");
    config.getConfiguration().put("configResource", "FactoredCustomers.xml");
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    String result = factoredCustomerInitializationService.initialize(comp, inits);
    assertEquals("correct return value", "FactoredCustomer", result);
    assertEquals("correct configuration file", "FactoredCustomers.xml", factoredCustomerService.getConfigResource());
  }

  @Test
  public void testBogusInitialization ()
  {
    PluginConfig config = pluginConfigRepo.findByRoleName("FactoredCustomer");
    assertNull("config not created", config);
    List<String> inits = new ArrayList<String>();
    String result = factoredCustomerInitializationService.initialize(comp, inits);
    assertNull("needs DefaultBrokerService in the list", result);
    inits.add("DefaultBroker");
    result = factoredCustomerInitializationService.initialize(comp, inits);
    assertEquals("failure return value", "fail", result);
    factoredCustomerInitializationService.setDefaults();
  }

  class MockCC implements CompetitionControl
  {

    public MockCC (TimeslotPhaseProcessor thing, int phase)
    {
      processor = thing;
      timeslotPhase = phase;
    }

    TimeslotPhaseProcessor processor;
    int timeslotPhase;

    public void registerTimeslotPhase (TimeslotPhaseProcessor thing, int phase)
    {
      processor = thing;
      timeslotPhase = phase;
    }

    public boolean isBootstrapMode ()
    {
      return false;
    }

    public void receiveMessage (PauseRequest msg)
    {
    }

    public void receiveMessage (PauseRelease msg)
    {
    }

    @Override
    public void preGame ()
    {
      // TODO Auto-generated method stub
    }

    @Override
    public void setAuthorizedBrokerList (ArrayList<String> brokerList)
    {
      // TODO Auto-generated method stub
    }
    
    @Override
    public boolean loginBroker (String name)
    {
      // TODO Auto-generated method stub 
      return false;
    }

    @Override
    public void runOnce ()
    {
      // TODO Auto-generated method stub 
    }
  }

}
