/*
 * Copyright (c) 2016 by the original author
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
package org.powertac.customer.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author jcollins
 */
public class BatteryTest
{
  private Competition competition;
  private TimeService timeService;
  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo mockSubscriptionRepo;
  private Broker broker;
  private TariffSpecification spec;
  private Tariff tariff;
  private TariffSubscription subscription;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;
  private ServerConfiguration serverConfig;
  private Configurator configurator;
  private MapConfiguration config;
  private TimeslotRepo tsRepo;
  private ServiceAccessor serviceAccessor;

  @BeforeEach
  public void setUp () throws Exception
  {
    tsRepo = mock(TimeslotRepo.class);
    competition =
        Competition.newInstance("ColdStorage test").withTimeslotsOpen(4);
    Competition.setCurrent(competition);
    timeService = new TimeService();
    Instant now =
        ZonedDateTime.of(2011, 1, 10, 0, 0, 0, 0, TimeService.UTC).toInstant();
    timeService.setCurrentTime(now);

    // tariff setup
    tariffRepo = new TariffRepo();
    broker = new Broker("Sam");
    spec =
        new TariffSpecification(broker, PowerType.THERMAL_STORAGE_CONSUMPTION)
    .addRate(new Rate().withValue(-0.11));
    tariff = new Tariff(spec);
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();

    // need a subscription
    subscription = mock(TariffSubscription.class);
    List<TariffSubscription> subs = new ArrayList<TariffSubscription>();
    subs.add(subscription);
    mockSubscriptionRepo = mock(TariffSubscriptionRepo.class);
    when(mockSubscriptionRepo.findActiveSubscriptionsForCustomer(any()))
        .thenReturn(subs);

    // set up randomSeed mock
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(anyString(),
                                    anyLong(),
                                    anyString())).thenReturn(seed);

    // Set up serverProperties mock
    serverConfig = mock(ServerConfiguration.class);
    configurator = new Configurator();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        configurator.configureSingleton(args[0]);
        return null;
      }
    }).when(serverConfig).configureMe(any());

    serviceAccessor = new ServiceAccessor();
  }

  @Test
  public void testCreate ()
  {
    Battery battery = new Battery("test");
    assertNotNull(battery, "constructed");
    assertEquals(50.0, battery.getCapacityKWh(), 1e-6, "correct capacity");
  }

  @Test
  public void testConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.battery.instances",
            "b1, b2");
    map.put("customer.model.battery.b1.capacityKWh", "30.0");
    map.put("customer.model.battery.b2.capacityKWh", "60.0");
    map.put("customer.model.battery.b1.maxChargeKW", "10.0");
    map.put("customer.model.battery.b2.maxChargeKW", "4.0");
    map.put("customer.model.battery.b1.maxDischargeKW", "-12.0");
    map.put("customer.model.battery.b2.maxDischargeKW", "-6.0");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(Battery.class);
    assertEquals(2, instances.size(), "two instances");
    Map<String, Battery> batteries = mapNames(instances);
    Battery b1 = batteries.get("b1");
    assertNotNull(b1, "Found b1");
    assertEquals(30.0, b1.getCapacityKWh(), 1e-6, "correct capacity");
    assertEquals(10.0, b1.getMaxChargeKW(), 1e-6, "correct charge");
    assertEquals(-12.0, b1.getMaxDischargeKW(), 1e-6, "correct discharge");
    Battery b2 = batteries.get("b2");
    assertNotNull(b2, "Found b2");
    assertEquals(60.0, b2.getCapacityKWh(), 1e-6, "correct capacity");
    assertEquals(4.0, b2.getMaxChargeKW(), 1e-6, "correct charge");
    assertEquals(-6.0, b2.getMaxDischargeKW(), 1e-6, "correct discharge");
  }

  @Test
  public void testBogusConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.model.battery.instances",
            "b1, b2");
    map.put("customer.model.battery.b1.capacityKWh", "0.0");
    map.put("customer.model.battery.b2.capacityKWh", "-6.0");
    map.put("customer.model.battery.b1.maxChargeKW", "0.0");
    map.put("customer.model.battery.b2.maxChargeKW", "-4.0");
    map.put("customer.model.battery.b1.maxDischargeKW", "-0.0");
    map.put("customer.model.battery.b2.maxDischargeKW", "6.0");
    map.put("customer.model.battery.b1.selfDischargeRate", "-0.2");
    map.put("customer.model.battery.b2.selfDischargeRate", "1.2");
    map.put("customer.model.battery.b1.chargeEfficiency", "-0.2");
    map.put("customer.model.battery.b2.chargeEfficiency", "1.1");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(Battery.class);
    assertEquals(2, instances.size(), "two instances");
    Map<String, Battery> batteries = mapNames(instances);
    Battery b1 = batteries.get("b1");
    b1.setServiceAccessor(serviceAccessor);
    b1.initialize();
    Battery b2 = batteries.get("b2");
    b2.setServiceAccessor(serviceAccessor);
    b2.initialize();
    assertEquals(1.0, b1.getCapacityKWh(), 1e-6, "fixed capacity");
    assertEquals(1.0, b2.getCapacityKWh(), 1e-6, "fixed capacity");
    assertEquals(1.0, b1.getMaxChargeKW(), 1e-6, "fixed charge rate");
    assertEquals(1.0, b2.getMaxChargeKW(), 1e-6, "fixed charge rate");
    assertEquals(-1.0, b1.getMaxDischargeKW(), 1e-6, "fixed discharge rate");
    assertEquals(-1.0, b2.getMaxDischargeKW(), 1e-6, "fixed discharge rate");
    assertEquals(0.0, b1.getSelfDischargeRate(), 1e-6, "fixed sdr");
    assertEquals(0.0, b2.getSelfDischargeRate(), 1e-6, "fixed sdr");
    assertEquals(1.0, b1.getChargeEfficiency(), 1e-6, "fixed ce");
    assertEquals(1.0, b2.getChargeEfficiency(), 1e-6, "fixed ce");
  }

  @Test
  public void testRC1 ()
  {
    Battery battery = new Battery("rc1");
    battery.setServiceAccessor(serviceAccessor);
    battery.initialize();
    when(subscription.getRegulation()).thenReturn(-5.0);
    battery.step();
    assertEquals(4.725, battery.getStateOfCharge(), 1e-6, "correct soc");
    ArgumentCaptor<RegulationCapacity> arg =
        ArgumentCaptor.forClass(RegulationCapacity.class);
    verify(subscription).setRegulationCapacity(arg.capture());
    RegulationCapacity rc = arg.getValue();
    assertNotNull(rc);
    assertEquals(4.725, rc.getUpRegulationCapacity(), 1e-6, "correct up");
    assertEquals(-20.0, rc.getDownRegulationCapacity(), 1e-6, "correct down");
  }

  @Test
  public void testRC2 ()
  {
    Battery battery = new Battery("rc1");
    battery.setServiceAccessor(serviceAccessor);
    battery.initialize();
    when(subscription.getRegulation()).thenReturn(-25.0);
    battery.step();
    assertEquals(23.725, battery.getStateOfCharge(), 1e-6, "correct soc");
    ArgumentCaptor<RegulationCapacity> arg =
        ArgumentCaptor.forClass(RegulationCapacity.class);
    verify(subscription).setRegulationCapacity(arg.capture());
    RegulationCapacity rc = arg.getValue();
    assertNotNull(rc);
    assertEquals(20.0, rc.getUpRegulationCapacity(), 1e-6, "correct up");
    assertEquals(-20.0, rc.getDownRegulationCapacity(), 1e-6, "correct down");
  }

  // map names to instances
  private Map<String, Battery> mapNames (Collection<?> objects)
  {
    Map<String, Battery> result = new HashMap<String, Battery>();
    for (Object thing : objects) {
      Battery bt = (Battery)thing;
      result.put(bt.getName(), bt);
    }
    return result;
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
      return tsRepo;
    }

    @Override
    public TimeService getTimeService ()
    {
      return timeService;
    }

    @Override
    public WeatherReportRepo getWeatherReportRepo ()
    {
      return null;
    }

    @Override
    public ServerConfiguration getServerConfiguration ()
    {
      // Auto-generated method stub
      return null;
    }

    @Override
    public TariffMarket getTariffMarket ()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public XMLMessageConverter getMessageConverter ()
    {
      // TODO Auto-generated method stub
      return null;
    }
  }
}
