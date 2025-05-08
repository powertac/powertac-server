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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerServiceAccessor;
//import org.powertac.common.interfaces.Accounting;
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

  @BeforeEach
  public void setUp () throws Exception
  {
    competition =
      Competition.newInstance("ColdStorage test").withTimeslotsOpen(4);
    Competition.setCurrent(competition);
    timeService = new TimeService();
    Instant now =
      ZonedDateTime.of(2011, 1, 10, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();;
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
    assertEquals(uut.getName(), "test", "correct name");
    when(seed.nextDouble()).thenReturn(0.5);
    init();
    assertEquals(-35.0, uut.getMinTemp(), 1e-6, "correct min temp");
    assertEquals(-10.0, uut.getMaxTemp(), 1e-6, "correct max temp");
    assertEquals(-22.5, uut.getCurrentTemp(), 1e-6, "correct current temp");
    assertEquals(15.0, uut.getCurrentNcUsage(), 1e-6, "correct nc usage");
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
    assertEquals(-30.0, uut.getMinTemp(), 1e-6, "correct min temp");
    assertEquals(-5.0, uut.getMaxTemp(), 1e-6, "correct max temp");
  }

  // loss rate
  @Test
  public void testCoolingLoss ()
  {
    when(seed.nextDouble()).thenReturn(15.0/25.0);
    weather = new WeatherReport(0, 30, 0, 0, 0);
    init();
    assertEquals(-20.0, uut.getCurrentTemp(), 1e-6, "correct current temp");
    assertEquals(0.416179, uut.getCoolingLossPerK(), 1e-5, "correct loss per K");
    double coolingLoss = uut.computeCoolingLoss(30.0);
    assertEquals(40.16216, coolingLoss, 1e-5, "correct total loss");
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
    assertEquals(52.6998, pwr.getValue(), 1e-4, "correct usage");
    ArgumentCaptor<RegulationCapacity> rcap =
      ArgumentCaptor.forClass(RegulationCapacity.class);
    verify(subscription).setRegulationCapacity(rcap.capture());
    RegulationCapacity rc = rcap.getValue();
    assertEquals(37.0998, rc.getUpRegulationCapacity(), 1e-4, "correct up-regulation");
    assertEquals(-56.3402, rc.getDownRegulationCapacity(), 1e-4, "correct down-regulationCapacity");
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
    assertEquals(110.24, pwr.getValue(), 1e-4, "correct usage");
    ArgumentCaptor<RegulationCapacity> rcap =
      ArgumentCaptor.forClass(RegulationCapacity.class);
    verify(subscription).setRegulationCapacity(rcap.capture());
    RegulationCapacity rc = rcap.getValue();
    assertEquals(93.44, rc.getUpRegulationCapacity(), 1e-4, "correct up-regulation");
    assertEquals(0.0, rc.getDownRegulationCapacity(), 1e-4, "correct down-regulationCapacity");
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
    assertEquals(14.4, pwr.getValue(), 1e-4, "correct usage");
    ArgumentCaptor<RegulationCapacity> rcap =
      ArgumentCaptor.forClass(RegulationCapacity.class);
    verify(subscription).setRegulationCapacity(rcap.capture());
    RegulationCapacity rc = rcap.getValue();
    assertEquals(0.0, rc.getUpRegulationCapacity(), 1e-4, "correct up-regulation");
    assertEquals(-93.44, rc.getDownRegulationCapacity(), 1e-4, "correct down-regulationCapacity");
  }

//  @Test
//  public void testStateLog ()
//  {
//    // create a couple entries
//    init();
//    uut.setCurrentTemp(-10);
//
//    // read the state log, keeping the last few lines in a queue
//    Queue<String> items = new LinkedList<String>();
//    int linesNeeded = 3;
//    try {
//      BufferedReader input =
//          new BufferedReader(new FileReader("log/test.state"));
//      String test = input.readLine();
//      System.out.println(test);
//      while (test != null) {
//        // looking for the last n lines from the ColdStorage instance
//        String[] segments = test.split("\\.");
//        if (segments[2].equals("customer")
//            && segments[3].equals("coldstorage")) {
//          items.add(test);
//          if (items.size() > linesNeeded)
//            items.remove();
//        }
//        test = input.readLine();
//      }
//      input.close();
//    }
//    catch (FileNotFoundException e) {
//      fail("cannot find state file: " + e.toString());
//    }
//    catch (IOException e) {
//      fail(e.toString());
//    }
//
//    // first item is constructor, starting with msec and classname
//    String item = items.remove();
//    String[] segments = item.split("::");
//    String[] path = segments[0].split("\\.");
//    assertEquals(path[1], "powertac");
//    assertEquals(path[2], "customer");
//    assertEquals(path[3], "coldstorage");
//    assertEquals(path[4], "ColdStorage");
//    // next is id, followed by "new", "test"
//    assertEquals(segments[2], "new");
//    assertEquals(segments[3], "test");
//
//    // next is the setCurrentTemp in initialize()
//    item = items.remove();
//    segments = item.split("::");
//    // assume classname is OK, content should be setCurrentTemp with some value
//    assertEquals(segments[2], "setCurrentTemp");
//    assertEquals(segments[3], "-35.0");
//
//    // last is the setCurrentTemp we called at the start of this test
//    item = items.remove();
//    segments = item.split("::");
//    // assume classname is OK, content should be setCurrentTemp with some value
//    assertEquals(segments[2], "setCurrentTemp");
//    assertEquals(segments[3], "-10.0");
//  }

  /**
   * Test method for {@link org.powertac.customer.coldstorage.ColdStorage#evaluateTariffs(java.util.List)}.
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
    ZonedDateTime now =
        ZonedDateTime.of(2014, 12, 1, 10, 0, 0, 0, ZoneOffset.UTC);
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
    assertTrue(dailyTariff.isCovered(), "good tariff");
    ColdStorage.TariffInfo info = uut.makeTariffInfo(dailyTariff);
    double[] result = info.getPrices();
    assertEquals(168, result.length, "length");
    assertEquals(.18, result[0], 1e-6, "[0]");
    assertEquals(.18, result[9], 1e-6, "[9]");
    assertEquals(.08, result[10], 1e-6, "[10]");
    assertEquals(.08, result[20], 1e-6, "[20]");
    assertEquals(.18, result[21], 1e-6, "[21]");
  }

  // check out TOU price profiling
  @Test
  public void testWeeklyPriceProfile ()
  {
    init();
    ZonedDateTime now =
        ZonedDateTime.of(2015, 2, 12, 12, 0, 0, 0, ZoneOffset.UTC);
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
    assertTrue(weeklyTariff.isCovered(), "good tariff");

    ColdStorage.TariffInfo info = uut.makeTariffInfo(weeklyTariff);
    double[] result = info.getPrices();
    // Thursday
    assertEquals(168, result.length, "length");
    assertEquals(.18, result[0], 1e-6, "Th 12");
    assertEquals(.18, result[7], 1e-6, "Th 19");
    assertEquals(.08, result[8], 1e-6, "Th 20");
    // Friday
    assertEquals(.08, result[18], 1e-6, "Fr 6");
    assertEquals(.18, result[19], 1e-6, "Fr 7");
    assertEquals(.18, result[31], 1e-6, "Fr 19");
    assertEquals(.08, result[32], 1e-6, "Fr 20");
    assertEquals(.08, result[35], 1e-6, "Fr 23");
    // Saturday
    assertEquals(.05, result[36], 1e-6, "Sa 0");
    assertEquals(.05, result[42], 1e-6, "Sa 6");
    assertEquals(.15, result[43], 1e-6, "Sa 7");
    assertEquals(.15, result[56], 1e-6, "Sa 20");
    assertEquals(.05, result[57], 1e-6, "Sa 21");
  }

  // TOU usage capacityProfile
  @Test
  public void testTouHeuristicProfile ()
  {
    init();
    ZonedDateTime now =
        ZonedDateTime.of(2015, 2, 12, 12, 0, 0, 0, ZoneOffset.UTC);
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
    assertTrue(weeklyTariff.isCovered(), "good tariff");

    ColdStorage.TariffInfo info = uut.makeTariffInfo(weeklyTariff);
    uut.heuristicTouProfile(info);
    CapacityProfile profile = info.getCapacityProfile();
    assertNotNull(profile, "capacityProfile exists");
    assertEquals(168, profile.getProfile().length, "capacityProfile length");
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
