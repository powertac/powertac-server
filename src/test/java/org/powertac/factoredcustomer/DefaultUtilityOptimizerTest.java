/*
 * Copyright (c) 2013 by John Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.factoredcustomer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.factoredcustomer.interfaces.FactoredCustomer;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Test cases for DefaultUtilityOptimizer
 * 
 * @author John Collins
 */
public class DefaultUtilityOptimizerTest
{
  // foundation components
  private Competition competition;
  private TimeService timeService;
  private CustomerInfo customerInfo;
  
  // mocks
  private TariffMarket tariffMarket;
  private TariffRepo tariffRepo;
  private CustomerRepo customerRepo;
  private RandomSeedRepo randomSeedRepo;
  private RandomSeed randomSeed;
  private FactoredCustomerService service;

  private Broker bob;

  private Broker jim;

  private Broker defaultBroker;
  private Tariff defaultConsumption;
  private Tariff defaultProduction;
  private long seedValue = 42l;
  private double randomValue = 1.0;
  
  private DefaultUtilityOptimizer duo;
  
  @Before
  public void setUp () throws Exception
  {
    competition = Competition.newInstance("duo-test");
    timeService = new TimeService();
    timeService.setCurrentTime(competition.getSimulationBaseTime());

    // set up mocks
    service = mock(FactoredCustomerService.class);
    tariffMarket = mock(TariffMarket.class);
    when(service.getTariffMarket()).thenReturn(tariffMarket);
    when (tariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
        .thenReturn(defaultConsumption);

    randomSeedRepo = mock(RandomSeedRepo.class);
    when(service.getRandomSeedRepo()).thenReturn(randomSeedRepo);
    randomSeed = mock(RandomSeed.class);
    when(randomSeed.getValue()).thenReturn(seedValue);
    when(randomSeedRepo.getRandomSeed(anyString(),
                                      anyLong(),
                                      anyString()))
        .thenReturn(randomSeed);

    tariffRepo = mock(TariffRepo.class);
    when (service.getTariffRepo()).thenReturn(tariffRepo);

    customerRepo = mock(CustomerRepo.class);
    when (service.getCustomerRepo()).thenReturn(customerRepo);
    doAnswer(new Answer<Object>() {
        @Override
        public Object answer (InvocationOnMock invocation) throws Throwable
        {
          Object[] args = invocation.getArguments();
          customerInfo = (CustomerInfo)args[0];
          return null;
        }
      }).when(customerRepo).add((CustomerInfo)anyObject());
    

    // set up default tariffs
    defaultBroker = new Broker("default");
    TariffSpecification dcSpec =
            new TariffSpecification(defaultBroker,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.6));
    defaultConsumption = new Tariff(dcSpec);
    initTariff(defaultConsumption);
    when(tariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
        .thenReturn(defaultConsumption);

    TariffSpecification dpSpec =
            new TariffSpecification(defaultBroker,
                                    PowerType.PRODUCTION).
                                    addRate(new Rate().withValue(0.1));
    defaultProduction = new Tariff(dpSpec);
    initTariff(defaultProduction);
    when(tariffMarket.getDefaultTariff(PowerType.PRODUCTION))
        .thenReturn(defaultProduction);
  }
  
  // initialize a tariff. It needs dependencies injected
  private void initTariff (Tariff tariff)
  {
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
  }

  // copied from FactoredCustomerService
  private FactoredCustomer loadCustomer (String customerName)
  {
    CustomerStructure structure = null;
    try {
      InputStream configStream =
        this.getClass().getResourceAsStream("/customers/" + customerName + ".xml");

      DocumentBuilderFactory docBuilderFactory =
        DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(configStream);

      NodeList customerNodes = doc.getElementsByTagName("customer");
      Element customerElement = (Element) customerNodes.item(0);
      String name = customerElement.getAttribute("name");
      String countString = customerElement.getAttribute("count");
      int count;
      if (countString == null || countString.trim().isEmpty()) {
        count = 1;
      }
      else {
        count = Integer.parseInt(countString);
      }
      if (count == 0) {
        // ignore structure
      }
      else if (count == 1) {
        structure = new CustomerStructure(name, customerElement);
      }
      else {
        fail("customer structure with count = " + count);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      fail("Error loading factored customer structures: " + e.toString());
    }
    if (null != structure) {
      CustomerFactory factory = new CustomerFactory();
      factory.registerDefaultCreator(DefaultFactoredCustomer.getCreator());
      FactoredCustomer customer = factory.processStructure(structure);
      customer.initialize(service, structure);
      return customer;
    }
    return null;
  }

  /**
   * make sure we can load customers
   */
  @Test
  public void loadCustomerTest ()
  {
    FactoredCustomer brookside = loadCustomer("Brookside");
    assertNotNull(brookside);
    assertEquals("Correct class",
                 DefaultFactoredCustomer.class,
                 brookside.getClass());
    //System.out.println(customerInfo.getName() + ": " + customerInfo.getPopulation());
    verify(tariffMarket).subscribeToTariff(defaultConsumption,
                                           customerInfo,
                                           30000);
  }
  
  /**
   * Test for no new tariffs case.
   */
  @Test
  public void noTariffTest ()
  {
    FactoredCustomer brookside = loadCustomer("Brookside");
  }

  /**
   * Test method for {@link org.powertac.factoredcustomer.DefaultUtilityOptimizer#evaluateTariffs()}.
   */
  @Test
  public void evaluateTariffsConsumption ()
  {
    fail("Not yet implemented");
  }

  @Test
  public void evaluateTariffsProduction ()
  {
    fail("Not yet implemented");
  }

  @Test
  public void evaluateTariffsTOU ()
  {
    fail("Not yet implemented");
  }

}
