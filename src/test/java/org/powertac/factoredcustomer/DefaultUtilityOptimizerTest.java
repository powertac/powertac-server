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
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
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
  private Accounting accountingService;
  private TariffMarket tariffMarket;
  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  private CustomerRepo customerRepo;
  private RandomSeedRepo randomSeedRepo;
  private RandomSeed randomSeed;
  private TimeslotRepo timeslotRepo;
  private WeatherReportRepo weatherReportRepo;
  private WeatherForecastRepo weatherForecastRepo;
  private FactoredCustomerService service;

  private Broker bob;

  private Broker jim;

  private Broker defaultBroker;
  private Tariff defaultConsumption;
  private Tariff defaultProduction;
  private TariffSubscription defaultConsumptionSubscription;
  private long seedValue = 42l;
  private double randomValue = 1.0;
  
  private DefaultUtilityOptimizer duo;
  
  @Before
  public void setUp () throws Exception
  {
    competition = Competition.newInstance("duo-test");
    timeService = new TimeService();
    timeService.setCurrentTime(competition.getSimulationBaseTime()
                               .plus(TimeService.HOUR * 7));

    // set up mocks
    makeMocks();

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

  private FactoredCustomer setupConsumer ()
  {
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer (InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        customerInfo = (CustomerInfo)args[1];
        defaultConsumptionSubscription =
                new TariffSubscription(customerInfo, (Tariff)args[0]);
        populateSubscription(defaultConsumptionSubscription);
        defaultConsumptionSubscription.subscribe((Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff((Tariff)anyObject(),
                                            (CustomerInfo)anyObject(),
                                            anyInt());
    FactoredCustomer brookside = loadCustomer("Brookside");
    assertNotNull("Subscription created", defaultConsumptionSubscription);
    assertEquals("Correct population",
                 30000,
                 defaultConsumptionSubscription.getCustomersCommitted());
    verify(tariffMarket, times(1))
        .subscribeToTariff(defaultConsumption, customerInfo, 30000);

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer (InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        ArrayList<TariffSubscription> result =
                new ArrayList<TariffSubscription>();
        result.add(defaultConsumptionSubscription);
        return result;
      }
    }).when(tariffSubscriptionRepo).findSubscriptionsForCustomer(customerInfo);
    return brookside;
  }
  
  /**
   * Test for no new tariffs case.
   */
  @Test
  public void noTariffTest ()
  {
    FactoredCustomer brookside = setupConsumer();
    
    when(tariffRepo.findRecentActiveTariffs(anyInt(), (PowerType)anyObject()))
        .thenReturn(new ArrayList<Tariff>());

    brookside.evaluateTariffs();
    assertEquals("Correct customer",
               "BrooksideHomes", customerInfo.getName());
    verify(tariffMarket, times(1))
        .subscribeToTariff(defaultConsumption, customerInfo, 30000);
  }

  /**
   * Test method for {@link org.powertac.factoredcustomer.DefaultUtilityOptimizer#evaluateTariffs()}.
   */
  @Test
  public void evaluateSingleTariffConsumption ()
  {
    FactoredCustomer brookside = setupConsumer();

    TariffSpecification newTS =
            new TariffSpecification(defaultBroker,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.59));
    Tariff newTariff = new Tariff(newTS);
    initTariff(newTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(newTariff);

    when(tariffRepo.findRecentActiveTariffs(anyInt(), (PowerType)anyObject()))
        .thenReturn(tariffs);
    when(randomSeed.nextDouble()).thenReturn(0.5);
    brookside.evaluateTariffs();
    verify(tariffMarket, times(1))
        .subscribeToTariff(newTariff, customerInfo, 30000);
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

  private void makeMocks ()
  {
    service = mock(FactoredCustomerService.class);
    timeslotRepo = new TimeslotRepo();
    ReflectionTestUtils.setField(timeslotRepo, "timeService", timeService);
    when (service.getTimeslotRepo()).thenReturn(timeslotRepo);

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

    tariffSubscriptionRepo = mock(TariffSubscriptionRepo.class);
    when (service.getTariffSubscriptionRepo()).thenReturn(tariffSubscriptionRepo);

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
    
    weatherReportRepo = new WeatherReportRepo();
    ReflectionTestUtils.setField(weatherReportRepo,
                                 "timeslotRepo",
                                 timeslotRepo);
    when (service.getWeatherReportRepo()).thenReturn(weatherReportRepo);
    weatherReportRepo.add(getWeatherReport(7));

    weatherForecastRepo = new WeatherForecastRepo();
    ReflectionTestUtils.setField(weatherForecastRepo,
                                 "timeslotRepo",
                                 timeslotRepo);
    when (service.getWeatherForecastRepo()).thenReturn(weatherForecastRepo);
    weatherForecastRepo.add(getWeatherForecast(7));
    
    accountingService = mock(Accounting.class);
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
  
  double[] weatherData = {11.4, 2.0, 100.0, 1.0};
  double[][] forecastData = {{9.8, 1.43, 160.0, 1.0}, {9.5, 1.03, 162.0, 1.0},
                         {8.4, 2.8, 156.0, 1.0}, {9.2, 3.78, 173.0, 1.0},
                         {8.5, 2.7, 174.0, 1.0}, {10.1, 4.74, 281.0, 0.999},
                         {11.7, 3.22, 272.0, 0.992}, {12.2, 4.02, 270.0, 0.98},
                         {12.0, 3.32, 286.0, 1.0}, {11.6, 5.6, 297.0, 1.0},
                         {11.7, 6.0, 308.0, 0.652}, {11.0, 8.14, 321.0, 0.875},
                         {10.6, 8.0, 314.0, 1.0}, {9.3, 6.62, 300.0, 1.0},
                         {8.2, 5.8, 299.0, 1.0}, {8.7, 5.01, 290.0, 1.0},
                         {8.6, 4.9, 308.0, 1.0}, {8.7, 4.11, 328.0, 1.0},
                         {8.8, 4.21, 326.0, 1.0}, {9.1, 3.09, 296.0, 1.0},
                         {7.4, 3.84, 267.0, 1.0}, {6.9, 2.82, 269.0, 1.0},
                         {5.8, 2.75, 278.0, 0.163}, {5.0, 3.97, 266.0, 0.528}};

  private WeatherReport getWeatherReport (int ts)
  {
    return new WeatherReport(ts,
                             weatherData[0], weatherData[1],
                             weatherData[2], weatherData[3]);    
  }
  
  private WeatherForecast getWeatherForecast (int ts)
  {
    ArrayList<WeatherForecastPrediction> preds =
            new ArrayList<WeatherForecastPrediction>();
    int index = ts + 1;
    for (double[] data : forecastData) {
      preds.add(new WeatherForecastPrediction(index++,
                                              data[0], data[1],
                                              data[2], data[3]));
    }
    return new WeatherForecast(ts, preds);
  }

  private void populateSubscription (TariffSubscription subscription)
  {
    ReflectionTestUtils.setField(subscription,
                                 "accountingService",
                                 accountingService);
    ReflectionTestUtils.setField(subscription,
                                 "tariffMarketService",
                                 tariffMarket);
    ReflectionTestUtils.setField(subscription, "timeService", timeService);
  }
}
