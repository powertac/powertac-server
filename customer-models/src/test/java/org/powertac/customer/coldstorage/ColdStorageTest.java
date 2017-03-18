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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;



//import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.apache.commons.configuration2.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.CapacityProfile;
import org.powertac.common.Competition;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
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
  private TimeslotRepo mockTimeslotRepo;
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
                                    anyLong(),
                                    anyString())).thenReturn(seed);

    // mock the timeslotRepo
    mockTimeslotRepo = mock(TimeslotRepo.class);

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
    }).when(serverConfig).configureMe(any());

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
    MapConfiguration mapConfig = new MapConfiguration(map);
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

  @Test
  public void testStateLog ()
  {
    // create a couple entries
    init();
    uut.setCurrentTemp(-10);

    // read the state log, keeping the last few lines in a queue
    Queue<String> items = new LinkedList<String>();
    int linesNeeded = 3;
    try {
      BufferedReader input =
          new BufferedReader(new FileReader("log/test.state"));
      String test = input.readLine();
      while (test != null) {
        // looking for the last n lines from the ColdStorage instance
        String[] segments = test.split("\\.");
        if (segments[2].equals("customer")
            && segments[3].equals("coldstorage")) {
          items.add(test);
          if (items.size() > linesNeeded)
            items.remove();
        }
        test = input.readLine();
      }
      input.close();
    }
    catch (FileNotFoundException e) {
      fail("cannot find state file: " + e.toString());
    }
    catch (IOException e) {
      fail(e.toString());
    }

    // first item is constructor, starting with msec and classname
    String item = items.remove();
    String[] segments = item.split("::");
    String[] path = segments[0].split("\\.");
    assertEquals("powertac", path[1]);
    assertEquals("customer", path[2]);
    assertEquals("coldstorage", path[3]);
    assertEquals("ColdStorage", path[4]);
    // next is id, followed by "new", "test"
    assertEquals("new", segments[2]);
    assertEquals("test", segments[3]);

    // next is the setCurrentTemp in initialize()
    item = items.remove();
    segments = item.split("::");
    // assume classname is OK, content should be setCurrentTemp with some value
    assertEquals("setCurrentTemp", segments[2]);
    assertEquals("-35.0", segments[3]);

    // last is the setCurrentTemp we called at the start of this test
    item = items.remove();
    segments = item.split("::");
    // assume classname is OK, content should be setCurrentTemp with some value
    assertEquals("setCurrentTemp", segments[2]);
    assertEquals("-10.0", segments[3]);
  }

  /**
   * Test method for {@link org.powertac.customer.model.ColdStorage#evaluateTariffs(java.util.List)}.
   */
  @Test
  public void testEvaluateTariffs ()
  {
    //fail("Not yet implemented");
  }

  // check out TOU price profiling
  @Test
  public void testDailyPriceProfile ()
  {
    init();
    DateTime now =
        new DateTime(2014, 12, 1, 10, 0, 0, DateTimeZone.UTC);
    when(mockTimeslotRepo.currentTimeslot())
        .thenReturn(new Timeslot(0, now.toInstant()));
    TariffSpecification dailySpec =
        new TariffSpecification(broker, PowerType.THERMAL_STORAGE_CONSUMPTION);
    Rate r1 =
        new Rate().withDailyBegin(7).withDailyEnd(19).withValue(.18);
    dailySpec.addRate(r1);
    Rate r2 =
        new Rate().withDailyBegin(20).withDailyEnd(6).withValue(.08);
    dailySpec.addRate(r2);
    Tariff dailyTariff = new Tariff(dailySpec);
    ReflectionTestUtils.setField(dailyTariff, "timeService", timeService);
    ReflectionTestUtils.setField(dailyTariff, "tariffRepo", tariffRepo);
    dailyTariff.init();
    assertTrue("good tariff", dailyTariff.isCovered());
    ColdStorage.TariffInfo info = uut.makeTariffInfo(dailyTariff);
    double[] result = info.getPrices();
    assertEquals("length", 168, result.length);
    assertEquals("[0]", .18, result[0], 1e-6);
    assertEquals("[9]", .18, result[9], 1e-6);
    assertEquals("[10]", .08, result[10], 1e-6);
    assertEquals("[20]", .08, result[20], 1e-6);
    assertEquals("[21]", .18, result[21], 1e-6);
  }

  // check out TOU price profiling
  @Test
  public void testWeeklyPriceProfile ()
  {
    init();
    DateTime now =
        new DateTime(2015, 2, 12, 12, 0, 0, DateTimeZone.UTC);
    when(mockTimeslotRepo.currentTimeslot())
        .thenReturn(new Timeslot(0, now.toInstant()));
    TariffSpecification weeklySpec =
        new TariffSpecification(broker, PowerType.THERMAL_STORAGE_CONSUMPTION);
    Rate r1 = new Rate()
        .withWeeklyBegin(1).withWeeklyEnd(5)
        .withDailyBegin(7).withDailyEnd(19).withValue(.18);
    weeklySpec.addRate(r1);
    Rate r2 = new Rate()
        .withWeeklyBegin(1).withWeeklyEnd(5)
        .withDailyBegin(20).withDailyEnd(6).withValue(.08);
    weeklySpec.addRate(r2);
    Rate r3 = new Rate()
        .withWeeklyBegin(6).withWeeklyEnd(7)
        .withDailyBegin(7).withDailyEnd(20).withValue(.15);
    weeklySpec.addRate(r3);
    Rate r4 = new Rate()
        .withWeeklyBegin(6).withWeeklyEnd(7)
        .withDailyBegin(21).withDailyEnd(6).withValue(.05);
    weeklySpec.addRate(r4);

    Tariff weeklyTariff = new Tariff(weeklySpec);
    ReflectionTestUtils.setField(weeklyTariff, "timeService", timeService);
    ReflectionTestUtils.setField(weeklyTariff, "tariffRepo", tariffRepo);
    weeklyTariff.init();
    assertTrue("good tariff", weeklyTariff.isCovered());

    ColdStorage.TariffInfo info = uut.makeTariffInfo(weeklyTariff);
    double[] result = info.getPrices();
    // Thursday
    assertEquals("length", 168, result.length);
    assertEquals("Th 12", .18, result[0], 1e-6);
    assertEquals("Th 19", .18, result[7], 1e-6);
    assertEquals("Th 20", .08, result[8], 1e-6);
    // Friday
    assertEquals("Fr 6", .08, result[18], 1e-6);
    assertEquals("Fr 7", .18, result[19], 1e-6);
    assertEquals("Fr 19", .18, result[31], 1e-6);
    assertEquals("Fr 20", .08, result[32], 1e-6);
    assertEquals("Fr 23", .08, result[35], 1e-6);
    // Saturday
    assertEquals("Sa 0", .05, result[36], 1e-6);
    assertEquals("Sa 6", .05, result[42], 1e-6);
    assertEquals("Sa 7", .15, result[43], 1e-6);
    assertEquals("Sa 20", .15, result[56], 1e-6);
    assertEquals("Sa 21", .05, result[57], 1e-6);
  }

  // TOU usage capacityProfile
  @Test
  public void testTouHeuristicProfile ()
  {
    init();
    DateTime now =
        new DateTime(2015, 2, 12, 12, 0, 0, DateTimeZone.UTC);
    when(mockTimeslotRepo.currentTimeslot())
        .thenReturn(new Timeslot(0, now.toInstant()));
    TariffSpecification weeklySpec =
        new TariffSpecification(broker, PowerType.THERMAL_STORAGE_CONSUMPTION);
    Rate r1 = new Rate()
        .withWeeklyBegin(1).withWeeklyEnd(5)
        .withDailyBegin(7).withDailyEnd(19).withValue(.18);
    weeklySpec.addRate(r1);
    Rate r2 = new Rate()
        .withWeeklyBegin(1).withWeeklyEnd(5)
        .withDailyBegin(20).withDailyEnd(6).withValue(.08);
    weeklySpec.addRate(r2);
    Rate r3 = new Rate()
        .withWeeklyBegin(6).withWeeklyEnd(7)
        .withDailyBegin(7).withDailyEnd(20).withValue(.15);
    weeklySpec.addRate(r3);
    Rate r4 = new Rate()
        .withWeeklyBegin(6).withWeeklyEnd(7)
        .withDailyBegin(21).withDailyEnd(6).withValue(.05);
    weeklySpec.addRate(r4);

    Tariff weeklyTariff = new Tariff(weeklySpec);
    ReflectionTestUtils.setField(weeklyTariff, "timeService", timeService);
    ReflectionTestUtils.setField(weeklyTariff, "tariffRepo", tariffRepo);
    weeklyTariff.init();
    assertTrue("good tariff", weeklyTariff.isCovered());

    ColdStorage.TariffInfo info = uut.makeTariffInfo(weeklyTariff);
    uut.heuristicTouProfile(info);
    CapacityProfile profile = info.getCapacityProfile();
    assertNotNull("capacityProfile exists", profile);
    assertEquals("capacityProfile length", 168, profile.getProfile().length);
    System.out.println(Arrays.toString(profile.getProfile()));
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
      return mockTimeslotRepo;
    }

    @Override
    public TimeService getTimeService ()
    {
      return timeService;
    }

    @Override
    public WeatherReportRepo getWeatherReportRepo ()
    {
      return mockWeatherRepo;
    }

    @Override
    public ServerConfiguration getServerConfiguration ()
    {
      // Auto-generated method stub
      return null;
    }
  }

}
