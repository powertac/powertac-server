/*
 * Copyright (c) 2014 by the original author
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
package org.powertac.customer.coldstorage;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;


//import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.WeatherReport;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerServiceAccessor;
//import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 */
public class ColdStorageTest
{
  private ColdStorage uut;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;
  private WeatherReportRepo mockWeatherRepo;
  private WeatherReport weather;
  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo mockSubscriptionRepo;
  private TimeService timeService;
  private CustomerServiceAccessor serviceAccessor;

  private ServerConfiguration serverConfig;
  private Configurator config;

  private Competition competition;
  private Broker broker;
  private TariffSpecification spec;
  private Tariff tariff;
  private TariffSubscription subscription;

  @Before
  public void setUp () throws Exception
  {
    competition =
      Competition.newInstance("ColdStorage test").withTimeslotsOpen(4);
    Competition.setCurrent(competition);
    timeService = new TimeService();
    Instant now =
      new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(now);

    // tariff setup
    tariffRepo = new TariffRepo();
    mockSubscriptionRepo = mock(TariffSubscriptionRepo.class);
    broker = new Broker("Sam");
    spec =
      new TariffSpecification(broker, PowerType.THERMAL_STORAGE_CONSUMPTION)
          .addRate(new Rate().withValue(-0.11));
    tariff = new Tariff(spec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    // set up randomSeed mock
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(anyString(),
                                    anyInt(),
                                    anyString())).thenReturn(seed);

    // set up WeatherRepo mock
    mockWeatherRepo = mock(WeatherReportRepo.class);
    when(mockWeatherRepo.currentWeatherReport()).thenReturn(weather);

    // Set up serverProperties mock
    serverConfig = mock(ServerConfiguration.class);
    config = new Configurator();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(serverConfig).configureMe(anyObject());

    serviceAccessor = new ServiceAccessor();
    uut = new ColdStorage("test");
  }

  private void init ()
  {
    uut.setServiceAccessor(serviceAccessor);
    uut.initialize();
    subscription = mock(TariffSubscription.class);
    List<TariffSubscription> subs = new ArrayList<TariffSubscription>();
    subs.add(subscription);
    when(mockSubscriptionRepo.findActiveSubscriptionsForCustomer(uut
             .getCustomerInfo())).thenReturn(subs);
  }

  // initialization
  @Test
  public void testInitialize ()
  {
    assertEquals("correct name", "test", uut.getName());
    when(seed.nextDouble()).thenReturn(0.5);
    init();
    assertEquals("correct min temp", -35.0, uut.getMinTemp(), 1e-6);
    assertEquals("correct max temp", -10.0, uut.getMaxTemp(), 1e-6);
    assertEquals("correct current temp", -22.5, uut.getCurrentTemp(), 1e-6);
    assertEquals("correct nc usage", 15.0, uut.getCurrentNcUsage(), 1e-6);
  }

  // configuration
  @Test
  public void testConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.coldstorage.coldStorage.minTemp", "-30");
    map.put("customer.coldstorage.coldStorage.maxTemp", "-5");
    Configuration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    serverConfig.configureMe(uut);
    assertEquals("correct min temp", -30.0, uut.getMinTemp(), 1e-6);
    assertEquals("correct max temp", -5.0, uut.getMaxTemp(), 1e-6);
  }

  // loss rate
  @Test
  public void testCoolingLoss ()
  {
    when(seed.nextDouble()).thenReturn(15.0/25.0);
    weather = new WeatherReport(0, 30, 0, 0, 0);
    init();
    assertEquals("correct current temp", -20.0, uut.getCurrentTemp(), 1e-6);
    assertEquals("correct loss per K", 0.416179, uut.getCoolingLossPerK(), 1e-5);
    double coolingLoss = uut.computeCoolingLoss(30.0);
    assertEquals("correct total loss", 40.16216, coolingLoss, 1e-5);
  }

  // step at setpoint, no regulation
  @Test
  public void testStepSetpoint ()
  {
    when(seed.nextDouble()).thenReturn(15.0/25.0);
    weather = new WeatherReport(0, 30, 0, 0, 0);
    when(mockWeatherRepo.currentWeatherReport()).thenReturn(weather);
    init();
    when(subscription.getRegulation()).thenReturn(0.0);
    uut.step();
    ArgumentCaptor<Double> pwr = ArgumentCaptor.forClass(Double.class);
    verify(subscription).usePower(pwr.capture());
    assertEquals("correct usage", 52.6998, pwr.getValue(), 1e-4);
    ArgumentCaptor<RegulationCapacity> rcap =
      ArgumentCaptor.forClass(RegulationCapacity.class);
    verify(subscription).setRegulationCapacity(rcap.capture());
    RegulationCapacity rc = rcap.getValue();
    assertEquals("correct up-regulation", 37.0998,
                 rc.getUpRegulationCapacity(), 1e-4);
    assertEquals("correct down-regulationCapacity", -56.3402,
                 rc.getDownRegulationCapacity(), 1e-4);
  }

  // step 5K above setpoint, no regulation
  @Test
  public void testStepAbove ()
  {
    when(seed.nextDouble()).thenReturn(20.0/25.0);
    weather = new WeatherReport(0, 30, 0, 0, 0);
    when(mockWeatherRepo.currentWeatherReport()).thenReturn(weather);
    init();
    when(subscription.getRegulation()).thenReturn(0.0);
    uut.step();
    ArgumentCaptor<Double> pwr = ArgumentCaptor.forClass(Double.class);
    verify(subscription).usePower(pwr.capture());
    assertEquals("correct usage", 110.24, pwr.getValue(), 1e-4);
    ArgumentCaptor<RegulationCapacity> rcap =
      ArgumentCaptor.forClass(RegulationCapacity.class);
    verify(subscription).setRegulationCapacity(rcap.capture());
    RegulationCapacity rc = rcap.getValue();
    assertEquals("correct up-regulation", 93.44,
                 rc.getUpRegulationCapacity(), 1e-4);
    assertEquals("correct down-regulationCapacity", 0.0,
                 rc.getDownRegulationCapacity(), 1e-4);
  }

  // step 5K above setpoint, no regulation
  @Test
  public void testStepBelow ()
  {
    when(seed.nextDouble()).thenReturn(10.0/25.0);
    weather = new WeatherReport(0, 30, 0, 0, 0);
    when(mockWeatherRepo.currentWeatherReport()).thenReturn(weather);
    init();
    when(subscription.getRegulation()).thenReturn(0.0);
    uut.step();
    ArgumentCaptor<Double> pwr = ArgumentCaptor.forClass(Double.class);
    verify(subscription).usePower(pwr.capture());
    assertEquals("correct usage", 14.4, pwr.getValue(), 1e-4);
    ArgumentCaptor<RegulationCapacity> rcap =
      ArgumentCaptor.forClass(RegulationCapacity.class);
    verify(subscription).setRegulationCapacity(rcap.capture());
    RegulationCapacity rc = rcap.getValue();
    assertEquals("correct up-regulation", 0.0,
                 rc.getUpRegulationCapacity(), 1e-4);
    assertEquals("correct down-regulationCapacity", -93.44,
                 rc.getDownRegulationCapacity(), 1e-4);
  }

  /**
   * Test method for {@link org.powertac.customer.model.ColdStorage#evaluateTariffs(java.util.List)}.
   */
  @Test
  public void testEvaluateTariffs ()
  {
    //fail("Not yet implemented");
  }

  class ServiceAccessor implements CustomerServiceAccessor
  {

    @Override
    public CustomerRepo getCustomerRepo ()
    {
      return null;
    }

    @Override
    public RandomSeedRepo getRandomSeedRepo ()
    {
      return mockSeedRepo;
    }

    @Override
    public TariffRepo getTariffRepo ()
    {
      return tariffRepo;
    }

    @Override
    public TariffSubscriptionRepo getTariffSubscriptionRepo ()
    {
      return mockSubscriptionRepo;
    }

    @Override
    public TimeslotRepo getTimeslotRepo ()
    {
      return null;
    }

    @Override
    public WeatherReportRepo getWeatherReportRepo ()
    {
      return mockWeatherRepo;
    }
  }
}
