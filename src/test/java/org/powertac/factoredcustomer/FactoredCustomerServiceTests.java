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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;


/**
 * @author Prashant Reddy, Antonios Chrysopoulos, Govert Buijs
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml" })
@DirtiesContext
@TestExecutionListeners(listeners = {
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class
})
public class FactoredCustomerServiceTests
{
  @Autowired
  private TimeService timeService;

  @Autowired
  private Accounting mockAccounting;

  @Autowired
  private TariffMarket mockTariffMarket;

  @Autowired
  private FactoredCustomerService factoredCustomerService;

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

  private DummyConfig configSvc;

  private Instant exp;
  private Instant now;
  private Broker defaultBroker;
  private TariffSpecification defaultConsumptionTariffSpec;
  private Tariff defaultConsumptionTariff;
  private TariffSpecification defaultProductionTariffSpec;
  private Tariff defaultProductionTariff;
  private Competition comp;
  private List<Object[]> accountingArgs;

  @Before
  public void setUp ()
  {
    customerRepo.recycle();
    brokerRepo.recycle();
    tariffRepo.recycle();
    tariffSubscriptionRepo.recycle();
    randomSeedRepo.recycle();
    timeslotRepo.recycle();
    reset(mockTariffMarket);
    reset(mockAccounting);

    // create a Competition, needed for initialization
    comp = Competition.newInstance("factored-customer-test");

    defaultBroker = new Broker("DefaultBroker");

    now = new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(now);
    exp = now.plus(TimeService.WEEK * 10);

    defaultConsumptionTariffSpec = new TariffSpecification(defaultBroker,
        PowerType.CONSUMPTION).withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(0.12));
    defaultConsumptionTariff = new Tariff(defaultConsumptionTariffSpec);
    defaultConsumptionTariff.init();

    defaultProductionTariffSpec = new TariffSpecification(defaultBroker,
        PowerType.PRODUCTION).withExpiration(exp)
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(new Rate().withValue(-0.08));
    defaultProductionTariff = new Tariff(defaultProductionTariffSpec);
    defaultProductionTariff.init();

    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
        .thenReturn(defaultConsumptionTariff);
    when(mockTariffMarket.getDefaultTariff(PowerType.PRODUCTION))
        .thenReturn(defaultProductionTariff);

    accountingArgs = new ArrayList<>();

    // mock the AccountingService, capture args
    doAnswer(new Answer<Object>()
    {
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        accountingArgs.add(args);
        return null;
      }
    }).when(mockAccounting).addTariffTransaction(
        isA(TariffTransaction.Type.class), isA(Tariff.class),
        isA(CustomerInfo.class), anyInt(), anyDouble(), anyDouble());

    // Initialize Config
    configSvc = new DummyConfig();
    configSvc.initialize();
  }

  @After
  public void tearDown ()
  {
    timeService = null;
    mockAccounting = null;
    mockTariffMarket = null;
    factoredCustomerService = null;
    tariffRepo = null;
    customerRepo = null;
    tariffSubscriptionRepo = null;
    timeslotRepo = null;
    brokerRepo = null;
    randomSeedRepo = null;
    exp = null;
    now = null;
    defaultBroker = null;
    defaultConsumptionTariffSpec = null;
    defaultConsumptionTariff = null;
    defaultProductionTariffSpec = null;
    defaultProductionTariff = null;
    comp = null;
    accountingArgs = null;
  }

  public void initializeService ()
  {
    Config config = Config.getInstance();
    ReflectionTestUtils.setField(config, "serverConfiguration", configSvc);
    config.configure();

    List<String> inits = new ArrayList<>();
    inits.add("DefaultBroker");
    inits.add("TariffMarket");

    factoredCustomerService.initialize(comp, inits);
  }

  @Test
  public void testServiceInitialization ()
  {
    initializeService();
    assertEquals("Configured number of customers created",
        4, factoredCustomerService.getCustomers().size());
  }
}
