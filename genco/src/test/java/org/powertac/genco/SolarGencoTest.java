package org.powertac.genco;

import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.powertac.common.*;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.ContextService;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SolarGencoTest
{
  private BrokerProxy mockProxy;
  private TimeslotRepo timeslotRepo;
  private WeatherReportRepo mockReportRepo;
  private WeatherForecastRepo mockForecastRepo;
  private SolarGenco genco;
  private Competition comp;
  private Instant start;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;

  @BeforeEach
  public void setUp () throws Exception
  {
    comp = Competition.newInstance("Solar Genco test").withTimeslotsOpen(4);
    Competition.setCurrent(comp);
    comp.withTimeslotsOpen(4);
    mockProxy = mock(BrokerProxy.class);
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(eq(SolarGenco.class.getName()), anyLong(),
                                    anyString())).thenReturn(seed);
    timeslotRepo = new TimeslotRepo();
    mockReportRepo = mock(WeatherReportRepo.class);
    mockForecastRepo = mock(WeatherForecastRepo.class);
    genco = new SolarGenco("SolarTest");
    start = comp.getSimulationBaseTime().plusMillis(TimeService.DAY);
    TimeService timeService = new TimeService();
    timeService.setCurrentTime(start);
    ReflectionTestUtils.setField(timeslotRepo, "timeService", timeService);
  }

  private void init ()
  {
    ContextService svc = mock(ContextService.class);
    when(svc.getBean("timeslotRepo")).thenReturn(timeslotRepo);
    when(svc.getBean("randomSeedRepo")).thenReturn(mockSeedRepo);
    when(svc.getBean("weatherReportRepo")).thenReturn(mockReportRepo);
    when(svc.getBean("weatherForecastRepo")).thenReturn(mockForecastRepo);
    when(seed.nextLong()).thenReturn(1L);
    genco.init(mockProxy, 0, svc);
  }

  /**
   * Test method for
   * {@link org.powertac.genco.SolarGenco#SolarGenco(java.lang.String)}.
   */
  @Test
  public void testSolarGenco ()
  {
    assertNotNull(genco, "created something");
    assertEquals("SolarTest", genco.getUsername(), "correct name");
    assertEquals(0.2, genco.getSolarEfficiency(), 1e-6,
                 "correct default efficiency");
    assertEquals(50.0, genco.getNominalCapacityMw(), 1e-6,
                 "correct default capacity");
    assertEquals(2.0, genco.getBaseBidPrice(), 1e-6, "correct default price");
  }

  /**
   * Test method for {@link org.powertac.genco.SolarGenco#init}.
   */
  @Test
  public void testInit ()
  {
    init();
    verify(mockSeedRepo).getRandomSeed(eq(SolarGenco.class.getName()),
                                       anyLong(), eq("bid"));
  }

  // config test
  @Test
  public void configTest ()
  {
    TreeMap<String, String> map = new TreeMap<>();
    map.put("genco.solarGenco.solarEfficiency", "0.25");
    map.put("genco.solarGenco.baseBidPrice", "3.0");
    map.put("genco.solarGenco.nominalCapacity", "75.0");
    map.put("genco.solarGenco.cloudCoverFactor", "0.6");
    init();
    MapConfiguration conf = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(conf);
    configurator.configureSingleton(genco);

    assertEquals(0.25, genco.getSolarEfficiency(), 1e-6,
                 "configured efficiency");
    assertEquals(75.0, genco.getNominalCapacityMw(), 1e-6,
                 "configured capacity");
    assertEquals(3.0, genco.getBaseBidPrice(), 1e-6, "configured price");
    assertEquals(0.6, genco.getCloudCoverSensitivity(), 1e-6,
                 "configured cloud factor");
  }

  // Default weather conditions during daylight
  private void defaultDaytimeWeather ()
  {
    WeatherReport wr = new WeatherReport(0, 20.0, 0.0, 0.0, 0.0);
    when(mockReportRepo.currentWeatherReport()).thenReturn(wr);
    List<WeatherForecastPrediction> wfs = new ArrayList<>();
    for (int i = 0; i < 24; i += 1) {
      WeatherForecastPrediction wfp =
              new WeatherForecastPrediction(i + 1, 20.0, 0.0, 0.0, 0.0);
      wfs.add(wfp);
    }
    WeatherForecast wf = new WeatherForecast(0, wfs);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);
  }

  // Nighttime weather conditions
  private void nightWeather ()
  {
    WeatherReport wr = new WeatherReport(0, 15.0, 0.0, 0.0, 0.0);
    when(mockReportRepo.currentWeatherReport()).thenReturn(wr);
    List<WeatherForecastPrediction> wfs = new ArrayList<>();
    for (int i = 0; i < comp.getTimeslotsOpen(); i += 1) {
      WeatherForecastPrediction wfp =
              new WeatherForecastPrediction(i + 1, 15.0, 0.0, 0.0, 0.0);
      wfs.add(wfp);
    }
    WeatherForecast wf = new WeatherForecast(0, wfs);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);
  }

  /**
   * Test method for {@link org.powertac.genco.SolarGenco#generateOrders}.
   */
  @Test
  public void generateOrdersDaylight ()
  {
    init();

    genco.withMaxBidHorizon(24);
    defaultDaytimeWeather();
    timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR));
    // mock the normal distribution
    NormalDistribution mockNorm = mock(NormalDistribution.class);
    ReflectionTestUtils.setField(genco, "normal01", mockNorm);
    double[] samples = new double[2];
    samples[0] = 0.0;
    samples[1] = 0.0;
    when(mockNorm.sample(2)).thenReturn(samples);
    // capture orders
    final ArrayList<Order> orderList = captureOrders();
    // set up daytime timeslots (12 PM)
    Timeslot ts1 =
            timeslotRepo.makeTimeslot(start.plusMillis(12 * TimeService.HOUR));
    // generate orders and check
    genco.generateOrders(start, Collections.singletonList(ts1));
    assertFalse(orderList.isEmpty(), "orders generated during daylight");
    Order order = orderList.getFirst();
    assertTrue(order.getMWh() < 0, "negative quantity for sell order");
    assertTrue(order.getLimitPrice() >= 0, "non-negative price");
  }

  @Test
  public void noOrdersAtNight ()
  {
    init();
    nightWeather();
    // capture orders
    final ArrayList<Order> orderList = captureOrders();
    // set up night timeslot (2 AM)
    Timeslot ts1 =
            timeslotRepo.makeTimeslot(start.plusMillis(2 * TimeService.HOUR));
    // generate orders and check
    genco.generateOrders(start, Collections.singletonList(ts1));
    assertEquals(0, orderList.size(), "no orders at night");
  }

  @Test
  public void testMaxBidHorizonFiltering ()
  {
    init();
    defaultDaytimeWeather();

    // Set a short bid horizon
    genco.withMaxBidHorizon(2);

    // mock the normal distribution
    NormalDistribution mockNorm = mock(NormalDistribution.class);
    ReflectionTestUtils.setField(genco, "normal01", mockNorm);
    double[] samples = new double[2];
    samples[0] = 0.0;
    samples[1] = 0.0;
    when(mockNorm.sample(2)).thenReturn(samples);

    // capture orders
    final ArrayList<Order> orderList = captureOrders();

    // Create timeslots at different horizons (all during daylight hours)
    Instant now = start.plusMillis(12 * TimeService.HOUR);
    Timeslot ts1 = timeslotRepo.makeTimeslot(
            now.plusMillis(TimeService.HOUR)); // 1 hour ahead at noon
    Timeslot ts2 = timeslotRepo.makeTimeslot(
            now.plusMillis(2 * TimeService.HOUR)); // 2 hours ahead at 2 PM
    Timeslot ts3 = timeslotRepo.makeTimeslot(
            now.plusMillis(3 * TimeService.HOUR)); // 3 hours ahead at 3 PM
    Timeslot ts4 = timeslotRepo.makeTimeslot(
            now.plusMillis(4 * TimeService.HOUR)); // 4 hours ahead at 4 PM

    List<Timeslot> allSlots = Arrays.asList(ts1, ts2, ts3, ts4);

    // Generate orders with maxBidHorizon = 2
    genco.generateOrders(now, allSlots);

    // Should only generate orders for the first 2 timeslots (within the horizon)
    assertEquals(2, orderList.size(),
                 "only generates orders within bid horizon");

    // Verify the orders are for the correct timeslots
    boolean hasTs1Order = orderList.stream().anyMatch(
            order -> order.getTimeslotIndex() == ts1.getSerialNumber());
    boolean hasTs2Order = orderList.stream().anyMatch(
            order -> order.getTimeslotIndex() == ts2.getSerialNumber());
    boolean hasTs3Order = orderList.stream().anyMatch(
            order -> order.getTimeslotIndex() == ts3.getSerialNumber());
    boolean hasTs4Order = orderList.stream().anyMatch(
            order -> order.getTimeslotIndex() == ts4.getSerialNumber());

    assertTrue(hasTs1Order, "has order for timeslot 1 (within horizon)");
    assertTrue(hasTs2Order, "has order for timeslot 2 (within horizon)");
    assertFalse(hasTs3Order, "no order for timeslot 3 (beyond horizon)");
    assertFalse(hasTs4Order, "no order for timeslot 4 (beyond horizon)");

    // Test with an extended horizon
    orderList.clear();
    genco.withMaxBidHorizon(4);
    genco.generateOrders(now, allSlots);

    assertEquals(4, orderList.size(),
                 "generates orders for all slots with extended horizon");
  }

  @Test
  public void cloudCoverReducesGeneration ()
  {
    init();
    // Setup cloudy weather
    List<WeatherForecastPrediction> wfs = new ArrayList<>();
    for (int i = 0; i < 24; i += 1) {
      WeatherForecastPrediction wfp =
              new WeatherForecastPrediction(i + 1, 20.0, 0.0, 0.0,
                                            0.5); // 50% cloud cover
      wfs.add(wfp);
    }
    WeatherForecast wf = new WeatherForecast(0, wfs);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);

    // mock the normal distribution for consistent results
    NormalDistribution mockNorm = mock(NormalDistribution.class);
    ReflectionTestUtils.setField(genco, "normal01", mockNorm);
    double[] samples = new double[2];
    samples[0] = 0.0;
    samples[1] = 0.0;
    when(mockNorm.sample(2)).thenReturn(samples);

    // capture orders
    final ArrayList<Order> orderList = captureOrders();

    Instant now = start.plusMillis(12 * TimeService.HOUR);

    // set up daytime timeslot
    Timeslot ts1 =
            timeslotRepo.makeTimeslot(now.plusMillis((TimeService.HOUR)));
    genco.generateOrders(now, Collections.singletonList(ts1));

    assertFalse(orderList.isEmpty(), "orders generated with clouds");
    double cloudyQuantity = Math.abs(orderList.getFirst().getMWh());

    // Now test with clear weather
    orderList.clear();
    wfs.clear();
    for (int i = 0; i < 24; i += 1) {
      WeatherForecastPrediction wfp =
              new WeatherForecastPrediction(i + 1, 20.0, 0.0, 0.0,
                                            0.0); // clear
      wfs.add(wfp);
    }
    wf = new WeatherForecast(0, wfs);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);

    genco.generateOrders(now, Collections.singletonList(ts1));
    double clearQuantity = Math.abs(orderList.getFirst().getMWh());

    assertTrue(clearQuantity > cloudyQuantity, "clear weather generates more");
  }

  @Test
  public void temperatureDerating ()
  {
    init();
    genco.withOptimalTemperature(25.0);
    genco.withTempDeratingFactor(0.004);

    // Set up high-temperature weather
    List<WeatherForecastPrediction> wfs = new ArrayList<>();
    for (int i = 0; i < 24; i += 1) {
      WeatherForecastPrediction wfp =
              new WeatherForecastPrediction(i + 1, 40.0, 0.0, 0.0, 0.0); // hot
      wfs.add(wfp);
    }
    WeatherForecast wf = new WeatherForecast(0, wfs);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);

    // mock the normal distribution
    NormalDistribution mockNorm = mock(NormalDistribution.class);
    ReflectionTestUtils.setField(genco, "normal01", mockNorm);
    double[] samples = new double[2];
    samples[0] = 0.0;
    samples[1] = 0.0;
    when(mockNorm.sample(2)).thenReturn(samples);

    // capture orders
    final ArrayList<Order> orderList = captureOrders();

    Instant now = start.plusMillis(12 * TimeService.HOUR);
    Timeslot ts1 = timeslotRepo.makeTimeslot(now.plusMillis(TimeService.HOUR));
    genco.generateOrders(now, Collections.singletonList(ts1));

    double hotQuantity = Math.abs(orderList.getFirst().getMWh());

    // Test with optimal temperature
    orderList.clear();
    wfs.clear();
    for (int i = 0; i < 24; i += 1) {
      WeatherForecastPrediction wfp =
              new WeatherForecastPrediction(i + 1, 25.0, 0.0, 0.0, 0.0);
      wfs.add(wfp);
    }
    wf = new WeatherForecast(0, wfs);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);

    genco.generateOrders(now, Collections.singletonList(ts1));
    double optimalQuantity = Math.abs(orderList.getFirst().getMWh());

    assertTrue(optimalQuantity > hotQuantity, "optimal temp generates more");
  }

  @Test
  public void noOrdersWhenFullySold ()
  {
    init();
    defaultDaytimeWeather();

    Timeslot ts1 =
            timeslotRepo.makeTimeslot(start.plusMillis(12 * TimeService.HOUR));

    // Add market position showing capacity fully sold
    MarketPosition marketPosition = new MarketPosition(genco, ts1, -100.0);
    genco.addMarketPosition(marketPosition, ts1.getSerialNumber());

    final ArrayList<Order> orderList = captureOrders();

    genco.generateOrders(start, List.of(ts1));
    assertEquals(0, orderList.size(), "no orders when fully sold");
  }

  private ArrayList<Order> captureOrders ()
  {
    final ArrayList<Order> orderList = new ArrayList<>();
    doAnswer((Answer<Object>) invocation -> {
      Object[] args = invocation.getArguments();
      orderList.add((Order) args[0]);
      return null;
    }).when(mockProxy).routeMessage(isA(Order.class));
    return orderList;
  }
}
