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
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
//import org.mockito.ArgumentCaptor;
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
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.ServerConfiguration;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Prashant Reddy, Antonios Chrysopoulos
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
@DirtiesContext
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
    
    @Autowired
    private ServerConfiguration mockServerProperties;

    private Instant exp;
    private Instant now;
    private Broker defaultBroker;
    private TariffSpecification defaultConsumptionTariffSpec;
    private Tariff defaultConsumptionTariff;
    private TariffSpecification defaultProductionTariffSpec;
    private Tariff defaultProductionTariff;
    private Competition comp;
    private List<Object[]> accountingArgs;
    private Configurator config;

    @Before
    public void setUp()
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

        defaultConsumptionTariffSpec = new TariffSpecification(defaultBroker, PowerType.CONSUMPTION).withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(0.12));
        defaultConsumptionTariff = new Tariff(defaultConsumptionTariffSpec);
        defaultConsumptionTariff.init();

        defaultProductionTariffSpec = new TariffSpecification(defaultBroker, PowerType.PRODUCTION).withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(-0.08));
        defaultProductionTariff = new Tariff(defaultProductionTariffSpec);
        defaultProductionTariff.init();

        when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION)).thenReturn(defaultConsumptionTariff);
        when(mockTariffMarket.getDefaultTariff(PowerType.PRODUCTION)).thenReturn(defaultProductionTariff);

        accountingArgs = new ArrayList<Object[]>();

        // mock the AccountingService, capture args
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation)
            {
                Object[] args = invocation.getArguments();
                accountingArgs.add(args);
                return null;
            }
        }).when(mockAccounting).addTariffTransaction(
                isA(TariffTransaction.Type.class), isA(Tariff.class),
                isA(CustomerInfo.class), anyInt(), anyDouble(), anyDouble());


        // Set up serverProperties mock
        config = new Configurator();
        doAnswer(new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) {
            Object[] args = invocation.getArguments();
            config.configureSingleton(args[0]);
            return null;
          }
        }).when(mockServerProperties).configureMe(anyObject());
    }

    public void initializeService()
    {
      TreeMap<String, String> map = new TreeMap<String, String>();
      map.put("factoredcustomer.factoredCustomerService.configResource",
              "FactoredCustomers.xml");
      Configuration mapConfig = new MapConfiguration(map);
      config.setConfiguration(mapConfig);
        List<String> inits = new ArrayList<String>();
        inits.add("DefaultBroker");
        inits.add("TariffMarket");
        factoredCustomerService.initialize(comp, inits);
    }

    @Test
    public void testServiceInitialization()
    {
        initializeService();
        assertEquals("Configured number of customers created", 7, factoredCustomerService.getCustomers().size());
    }

    /******  IGNORE FOR NOW
    public void subscribeDefault()
    {
        for (FactoredCustomer customer : factoredCustomerService.getCustomers()) {
            // capture subscription method args
            ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
            ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
            ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
            when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture()))
                .thenReturn(tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariffArg.getValue()));
        }
        for (FactoredCustomer customer : factoredCustomerService.getCustomers()) {
            customer.subscribeDefault();
        }
    }
    
    @Test
    public void testTimeslotActivation()
    {
        initializeService();
        //subscribeDefault();
        
        timeService.setCurrentTime(now.plus(TimeService.HOUR));
        factoredCustomerService.activate(timeService.getCurrentTime(), 1);
        for (FactoredCustomer customer : factoredCustomerService.getCustomers()) {
            assertFalse("Factored customer capacity",
                        tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()) == null
                        || tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTotalUsage() == 0);
        }
        assertEquals("Tariff transactions created", factoredCustomerService.getCustomers().size() + 1, accountingArgs.size());
    }


    @Test
    public void testTariffEvaluation()
    {
        initializeService();
        subscribeDefault();
        
        Broker secondBroker = new Broker("SecondBroker");

        TariffSpecification secondConsumptionTariffSpec = new TariffSpecification(secondBroker, PowerType.CONSUMPTION).withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(0.10));
        Tariff secondConsumptionTariff = new Tariff(secondConsumptionTariffSpec);
        secondConsumptionTariff.init();

        TariffSpecification secondProductionTariffSpec = new TariffSpecification(secondBroker, PowerType.PRODUCTION).withExpiration(exp)
            .withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(-0.09));
        Tariff secondProductionTariff = new Tariff(secondProductionTariffSpec);
        secondProductionTariff.init();

        List<Tariff> tariffs = tariffRepo.findAllTariffs();
        assertEquals("Four tariffs found", 4, tariffRepo.findAllTariffs().size());

        factoredCustomerService.publishNewTariffs(tariffs);
    }
     END IGNORE FOR NOW ******/
}
