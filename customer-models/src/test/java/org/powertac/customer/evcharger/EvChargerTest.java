/**
 * 
 */
package org.powertac.customer.evcharger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;

import org.apache.commons.configuration2.MapConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Broker;
import org.powertac.common.CapacityProfile;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.RegulationRate;
import org.powertac.common.RegulationAccumulator;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.customer.AbstractCustomer;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 *
 */
class EvChargerTest
{
  static private Logger log = LogManager.getLogger(EvChargerTest.class.getName());
  
  private Competition competition;
  private CustomerServiceAccessor serviceAccessor;
  private TimeService timeService;
  private Instant start;
  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;
  private TimeslotRepo mockTimeslotRepo;
  private WeatherReportRepo mockWeatherRepo;
  private WeatherReport weather;

  // brokers and initial tariffs
  private Broker defaultBroker;
  private Tariff defaultConsumption;
  private Tariff evTariff;
  private Broker bob;
  private Broker sally;

  // configuration
  //private ServerConfiguration serverConfig;
  private Configurator config;

  // mocks
  private TariffMarket tariffMarket;
  private Accounting accountingService;

  private CustomerInfo customer;
  //private TariffSpecification mySpec;
  //private Tariff myTariff;
  //private TariffSubscription mySub;
  private EvCharger uut;

  private TariffSubscription oldSub;
  //private StorageState oldSS;

  // convenient values
  private double epsilon = 1e-5;

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  void setUp () throws Exception
  {
    competition = Competition.newInstance("storage-state-test");
    Competition.setCurrent(competition);
    timeService = new TimeService();
    timeService.setCurrentTime(competition.getSimulationBaseTime());
                               //.plusMillis(TimeService.HOUR * 7));
    start = timeService.getCurrentTime().plusMillis(TimeService.HOUR);
    tariffSubscriptionRepo = new TariffSubscriptionRepo();
    tariffRepo = new TariffRepo();
    ReflectionTestUtils.setField(tariffSubscriptionRepo,
                                 "tariffRepo", tariffRepo);
    tariffMarket = mock(TariffMarket.class);
    accountingService = mock(Accounting.class);

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
    serviceAccessor = new ServiceAccessor();

    // set up default tariffs
    defaultBroker = new Broker("default");
    TariffSpecification dcSpec =
            new TariffSpecification(defaultBroker,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.6));
    defaultConsumption = new Tariff(dcSpec);
    initTariff(defaultConsumption);
    defaultConsumption.setState(Tariff.State.OFFERED);
    when(tariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
        .thenReturn(defaultConsumption);

    // other brokers
    sally = new Broker("Sally");
    bob = new Broker("Bob");

    TariffSpecification evSpec =
            new TariffSpecification(defaultBroker,
                                    PowerType.ELECTRIC_VEHICLE).
                                    addRate(new Rate().withValue(-0.3));
    evTariff = new Tariff(evSpec);
    initTariff(evTariff);
    evTariff.setState(Tariff.State.OFFERED);
    when(tariffMarket.getDefaultTariff(PowerType.ELECTRIC_VEHICLE))
    .thenReturn(evTariff);

    // Set up serverProperties mock
    //serverConfig = mock(ServerConfiguration.class);
    config = new Configurator();
//    doAnswer(new Answer<Object>() {
//      @Override
//      public Object answer(InvocationOnMock invocation) {
//        Object[] args = invocation.getArguments();
//        config.configureSingleton(args[0]);
//        return null;
//      }
//    }).when(serverConfig).configureMe(any());
    uut = new EvCharger("test");
    uut.setServiceAccessor(serviceAccessor);
  }

  private TariffSubscription subscribeTo (EvCharger customer, Tariff tariff, double count)
  {
    return subscribeTo(customer, tariff, (int)Math.round(count));
  }

  private TariffSubscription subscribeTo (EvCharger customer, Tariff tariff, int count)
  {
    TariffSubscription subscription =
            new TariffSubscription(customer.getCustomerInfo(), tariff);
    initSubscription(subscription);
    subscription.subscribe(count);
    tariffSubscriptionRepo.add(subscription);
    return subscription;
  }

  // initializes a tariff. It needs dependencies injected
  private void initTariff (Tariff tariff)
  {
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
  }
  
  // initializes a tariff subscription.
  private void initSubscription (TariffSubscription sub)
  {
    ReflectionTestUtils.setField(sub, "timeService", timeService);
    ReflectionTestUtils.setField(sub, "tariffMarketService", tariffMarket);
    ReflectionTestUtils.setField(sub, "accountingService", accountingService);
  }

  private void setConfig()
  {
    TreeMap<String, String> map = new TreeMap<>();
    map.put("customer.evcharger.evCharger.population", "1000");
    map.put("customer.evcharger.evCharger.chargerCapacity", "8.0");
    map.put("customer.evcharger.evCharger.nominalDemandBias", "0.4");
    map.put("customer.evcharger.evCharger.defaultCapacityData",
            "1.55, 1.46, 1.36, 1.25, 1.16, 1.02, 0.80, 0.51, 0.34, 0.30, 0.32, 0.37, 0.48, 0.62, 0.78, 0.96, 1.13, 1.32, 1.49, 1.60, 1.69, 1.74, 1.73, 1.66");
    map.put("customer.evcharger.evCharger.model", "residential_ev_1.xml");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
  }

  @Test
  public void testDemandElementMeanCalculation ()
  {
    //uut = new EvCharger("residential_ev");
    //uut.setServiceAccessor(serviceAccessor);
//    TreeMap<String, String> map = new TreeMap<String, String>();
//    map.put("customer.evcharger.evCharger.model", "residential_ev_1.xml");
//    MapConfiguration mapConfig = new MapConfiguration(map);
//    config.setConfiguration(mapConfig);
    setConfig();
    config.configureSingleton(uut);

    uut.initialize();

    // test at 18:00
    ZonedDateTime currentTime = timeService.getCurrentDateTime();
    int hod = currentTime.getHour();
    assertEquals(0, hod);

    // Check that demandInfoMean is empty by default.
    assertTrue(uut.getDemandInfoMean().isEmpty());

    // Check that it equals the first demandInfo if only once requested.
    List<DemandElement> demand1 = uut.getDemandInfo(currentTime);
    List<DemandElement> demandMean = uut.getDemandInfoMean().get(hod);
    for (int i = 0; i < demand1.size(); i++) {
      assertEquals(demand1.get(i).getNVehicles(),
                   demandMean.get(i).getNVehicles() * uut.getPopulation());
      assertArrayEquals(demand1.get(i).getdistribution(),
                        demandMean.get(i).getdistribution());  
    }

    // Check that the means are correctly calculated for two demands.
    List<DemandElement> demand2 = uut.getDemandInfo(currentTime);
    demandMean = uut.getDemandInfoMean().get(hod);
    assertEquals(demandMean.size(), Math.max(demand1.size(), demand2.size()));
    double meanVehicles =
            (demand1.get(0).getNVehicles() / uut.getPopulation()
                    + demand2.get(0).getNVehicles() / uut.getPopulation()) / 2.0;
    assertEquals(meanVehicles, demandMean.get(0).getNVehicles());

    for (int i = 0; i < Math.min(demand1.size(), demand2.size()); i++) {
      double[] hist1 = demand1.get(i).getdistribution();
      double[] hist2 = demand2.get(i).getdistribution();
      double[] histMean = demandMean.get(i).getdistribution();
      assertEquals(histMean.length, hist1.length);
      assertEquals(histMean.length, hist2.length);
      assertEquals(demandMean.get(i).getHorizon(),
                   demand1.get(i).getHorizon());
      assertEquals(demandMean.get(i).getHorizon(),
                   demand2.get(i).getHorizon());
      
      // check weighted mean values
      double w1 = demand1.get(0).getNVehicles();
      double w2 = demand2.get(0).getNVehicles();
      for (int j = 0; j < hist1.length; j++) {
        assertEquals(histMean[j],
                     (hist1[j] * w1 + hist2[j] * w2) / w1 + w2);
      }
    }

    // Check that the means are correctly calculated for three demands.
    List<DemandElement> demand3 = uut.getDemandInfo(currentTime);
    List<DemandElement> demandMean3 = uut.getDemandInfoMean().get(hod);
    assertEquals(demandMean3.size(), Math
            .max(demand3.size(), Math.max(demand1.size(), demand2.size())));
    for (int i = 0; i < Math
            .min(demand3.size(),
                 Math.min(demand1.size(), demand2.size())); i++) {
      double[] hist1 = demand1.get(i).getdistribution();
      double[] hist2 = demand2.get(i).getdistribution();
      double[] hist3 = demand3.get(i).getdistribution();
      double[] histMean = demandMean3.get(i).getdistribution();
      assertEquals(histMean.length, hist1.length);
      assertEquals(histMean.length, hist2.length);
      assertEquals(histMean.length, hist3.length);
      assertEquals(demandMean3.get(i).getHorizon(),
                   demand1.get(i).getHorizon());
      assertEquals(demandMean3.get(i).getHorizon(),
                   demand2.get(i).getHorizon());
      assertEquals(demandMean3.get(i).getHorizon(),
                   demand3.get(i).getHorizon());
      // needs to be updated for weighted means
      //for (int j = 0; j < hist1.length; j++) {
      //  assertEquals((hist1[j] + hist2[j] + hist3[j]) / 3, histMean[j]);
      //}
    }

    // Check that the demandInfoMean is extended for new hours.
    List<DemandElement> demand4 = uut.getDemandInfo(currentTime.plusHours(1));
    List<DemandElement> demandMean4 = uut.getDemandInfoMean().get(hod + 1);
    //assertIterableEquals(demand4, demandMean4);
    for (int i = 0; i < demand4.size(); i++) {
      assertEquals(demand4.get(i).getNVehicles(),
                   demandMean4.get(i).getNVehicles() * uut.getPopulation());
      assertArrayEquals(demand4.get(i).getdistribution(),
                        demandMean4.get(i).getdistribution());  
    }

    // Check that the map contains 2 entries because we requested demandInfo for
    // two different hours.
    assertEquals(2, uut.getDemandInfoMean().size());
  }

  // Make sure EvCharger shows up in the list of AbstractCustomers
  @Test
  void testCM ()
  {
    ServiceLoader<AbstractCustomer> loader =
            ServiceLoader.load(AbstractCustomer.class);
    Iterator<AbstractCustomer> modelIterator = loader.iterator();
    boolean found = false;
    while (!found && modelIterator.hasNext()) {
      AbstractCustomer example = modelIterator.next();
      if (example.getClass() == EvCharger.class) {
        found = true;
      }
    }
    assertTrue(found);
  }

  // test configuration
  @Test
  public void testConfig ()
  {
    TreeMap<String, String> map = new TreeMap<>();
    map.put("customer.evcharger.evCharger.population", "1000");
    map.put("customer.evcharger.evCharger.chargerCapacity", "8.0");
    map.put("customer.evcharger.evCharger.nominalDemandBias", "0.4");
    map.put("customer.evcharger.evCharger.defaultCapacityData",
            "1.55, 1.46, 1.36, 1.25, 1.16, 1.02, 0.80, 0.51, 0.34, 0.30, 0.32, 0.37, 0.48, 0.62, 0.78, 0.96, 1.13, 1.32, 1.49, 1.60, 1.69, 1.74, 1.73, 1.66");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    config.configureSingleton(uut);

    ZonedDateTime now =
            ZonedDateTime.of(2014, 12, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    when(mockTimeslotRepo.currentTimeslot())
    .thenReturn(new Timeslot(0, now.toInstant()));

    uut.initialize();
    assertEquals(1000, uut.getPopulation(), "correct population");
    assertEquals(8.0, uut.getChargerCapacity(), 1e-6, "correct charger capacity");
    double[] profile = uut.getDefaultCapacityProfile().getProfile();
    assertEquals(24, profile.length, "full profile");
    assertEquals(1.46, profile[1], 1e-6, "profile OK");
  }

  @Test
  public void testFirstStepBoot ()
  {
    uut.initialize();
    // default subscription happens in CustomerModelService
    TariffSubscription defaultSub =
            subscribeTo (uut, evTariff, uut.getPopulation());
    tariffSubscriptionRepo.add(defaultSub);
    uut.handleInitialSubscription(tariffSubscriptionRepo
                                  .findActiveSubscriptionsForCustomer(uut.getCustomerInfo()));

    ZonedDateTime now =
            ZonedDateTime.of(2014, 12, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    timeService.setCurrentTime(now.toInstant());
    when(mockTimeslotRepo.currentTimeslot())
    .thenReturn(new Timeslot(0, now.toInstant()));
    when(mockTimeslotRepo.currentSerialNumber())
    .thenReturn(0);

    ArrayList<String> data =
            new ArrayList<>(Arrays.asList("3.0", "3.0", "3.0", "3.0", "3.0", "3.0", "3.0", "4.0",
                                          "4.0", "4.0", "3.0", "3.0", "4.0", "4.0", "4.0", "4.0",
                                          "4.0", "4.0", "5.0", "6.0", "7.0", "6.0", "5.0", "4.0"));
    uut.setDefaultCapacityData(data);

    // now we step and make sure everything is set up
    uut.step();

    StorageState ss = uut.getStorageState(defaultSub);
    assertNotNull(ss);
    assertEquals(defaultSub, ss.getSubscription());
  }

  TariffSubscription runFirstStepSim ()
  {
    TreeMap<String, Object> map = new TreeMap<>();
    map.put("customer.evcharger.evCharger.population", "1000");
    map.put("customer.evcharger.evCharger.chargerCapacity", "8.0");
    map.put("customer.evcharger.evCharger.nominalDemandBias", "0.4");
    map.put("customer.evcharger.evCharger.defaultCapacityData",
            "1.55, 1.46, 1.36, 1.25, 1.16, 1.02, 0.80, 0.51, 0.34, 0.30, 0.32, 0.37, 0.48, 0.62, 0.78, 0.96, 1.13, 1.32, 1.49, 1.60, 1.69, 1.74, 1.73, 1.66");
    map.put("customer.evcharger.evCharger.model", "residential_ev_1.xml");
    // for this test, we also need to provide a storage record
    //List<String> record = new ArrayList<>();
    String data = String.join(", ", "[SE 359 [1.0] [0.0]",
                              "SE 360 [2.0, 1.0] [10.0, 2.0]",
                              "SE 361 [1.0, 1.0, 1.0] [15.0, 8.0, 1.5]",
                              "SE 362 [1.0, 0.5, 0.5, 1.0] [22.0, 8.0, 3.5, 3.4]]");
    map.put("customer.evcharger.evCharger.storageRecord", data);
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);

    config.configureSingleton(uut);

    uut.initialize();
    ZonedDateTime now =
            ZonedDateTime.of(2014, 12, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    timeService.setCurrentTime(now.toInstant());

    // default subscription happens in CustomerModelService
    TariffSubscription defaultSub =
            subscribeTo (uut, evTariff, uut.getPopulation());
    uut.handleInitialSubscription(tariffSubscriptionRepo
                                  .findActiveSubscriptionsForCustomer(uut.getCustomerInfo()));

    when(mockTimeslotRepo.currentTimeslot())
    .thenReturn(new Timeslot(360, now.toInstant()));
    when(mockTimeslotRepo.currentSerialNumber())
    .thenReturn(360);

    // now we step and make sure everything is set up
    uut.step();
    return defaultSub;
  }

  @Test
  public void testFirstStepSim ()
  {
    log.info("testFirstStepSim()");
    // need to configure first
    TariffSubscription defaultSub = runFirstStepSim();

    // check StorageState
    StorageState ss = uut.getStorageState(defaultSub);
    assertNotNull(ss);
    assertEquals(defaultSub, ss.getSubscription());

    // check demandInfo
    List<ArrayList<DemandElement>> demandInfoMean = uut.getDemandInfoMean();
    assertNotNull(demandInfoMean);
    assertEquals(24, demandInfoMean.size());
    TariffInfo ti = uut.getTariffInfo(evTariff);
    assertEquals(uut.getDefaultCapacityProfile(), ti.getCapacityProfile());
    
    // check regulation capacity
    //RegulationAccumulator ra = defaultSub.getRemainingRegulationCapacity();
    //System.out.println("Reg: up=" + ra.getUpRegulationCapacity()
    //                   + " down=" + ra.getDownRegulationCapacity());
    System.out.println("endx");
  }

  @Test
  public void testRegulationCapacity ()
  {
    log.info("testRegCapacity over two timeslots");
    // need to configure first
    TariffSubscription defaultSub = runFirstStepSim();

    // bump the clock forward an hour
    Instant now = ZonedDateTime.of(2014, 12, 1, 11, 0, 0, 0, ZoneOffset.UTC).toInstant();
    timeService.setCurrentTime(now);
    when(mockTimeslotRepo.currentTimeslot())
    .thenReturn(new Timeslot(361, now));
    when(mockTimeslotRepo.currentSerialNumber())
    .thenReturn(361);

    // generate some activity, assuming no regulation was called for
    // in the previous timeslot
    assertEquals(0.0, defaultSub.getRegulation(), epsilon);
    uut.step();
    // retrieve actual, min, max, and data sent to subscription
    double[] regData = uut.getRegulationData();
    double v2gAvail = uut.getV2G();
    RegulationAccumulator ra = defaultSub.getRemainingRegulationCapacity();
    double upreg = ra.getUpRegulationCapacity();
    double downreg = ra.getDownRegulationCapacity();
    double pop = defaultSub.getCustomersCommitted();
    log.debug("remaining regulation: up = {}, down = {}", regData[1], regData[2]);
    assertEquals(upreg * pop, regData[0] - regData[1] - v2gAvail, epsilon);
    assertEquals(downreg * pop, regData[0] - regData[2], epsilon);
  }

  /**
   * To test this, we need two subscriptions to two different tariffs and two different subscriptions.
   * The "old" subscription is needed because its StorageState needs to retrieve the population.
   * The "new" subscription may have 0 or non-zero population.
   * Start with all of PodunkChargers subscribed to the default consumption tariff, and create a
   * StorageState for that subscription.
   * Then move half of them to a new EV tariff by calling the CMA. This will test the ability to 
   */
  @Test
  public void testSubscriptions ()
  {
    uut.initialize();
    //double chargerCapacity = 5.0; //kW
    oldSub = subscribeTo (uut, defaultConsumption, uut.getPopulation());
    ArrayList<TariffSubscription> subs = new ArrayList<>();
    subs.add(oldSub);
    uut.handleInitialSubscription(subs);
    //oldSS = new StorageState(oldSub, chargerCapacity, uut.getMaxDemandHorizon());
    TariffSpecification ts1 =
            new TariffSpecification(bob,
                                    PowerType.ELECTRIC_VEHICLE).
                                    addRate(new Rate().withValue(-0.09))
                                    .withSignupPayment(-2.0);
    Tariff tariff1 = new Tariff(ts1);
    initTariff(tariff1);
    //TODO ("Not yet implemented");
  }

  @Test
  public void testDefaultCapacityProfile ()
  {
    setConfig();
    config.configureSingleton(uut);
    uut.initialize();

    ZonedDateTime now =
            ZonedDateTime.of(2014, 12, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    when(mockTimeslotRepo.currentTimeslot())
    .thenReturn(new Timeslot(0, now.toInstant()));

    //double chargerCapacity = 5.0; //kW
    ArrayList<String> data =
            new ArrayList<>(Arrays.asList("3.0", "3.0", "3.0", "3.0", "3.0", "3.0",
                                          "3.0", "4.0", "4.0", "4.0", "3.0", "3.0",
                                          "4.0", "4.0", "4.0", "4.0", "4.0", "4.0",
                                          "5.0", "6.0", "7.0", "6.0", "5.0", "4.0"));
    uut.setDefaultCapacityData(data);
    CapacityProfile cp = uut.getDefaultCapacityProfile();
    double[] dcpn = cp.getProfile();
    assertEquals(24, dcpn.length);
    assertEquals(3.0, dcpn[0], 1e-6);
    // The getCapacityProfile() method should also return the default until
    // there are multiple tariffs around
    cp = uut.getCapacityProfile(evTariff);
    dcpn = cp.getProfile();
    assertEquals(24, dcpn.length);
    assertEquals(4.0, dcpn[7], 1e-6);    
  }

  @Test
  public void testTariffEvalSimple ()
  {
    
  }

  @Test
  public void testTariffEvalTouDaily ()
  {
    
  }

  @Test
  public void testTariffEvalWeekly ()
  {
    // need to configure first
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.evcharger.evCharger.population", "1000");
    map.put("customer.evcharger.evCharger.chargerCapacity", "8.0");
    map.put("customer.evcharger.evCharger.nominalDemandBias", "0.4");
    map.put("customer.evcharger.evCharger.defaultCapacityData",
            "1.55, 1.46, 1.36, 1.25, 1.16, 1.02, 0.80, 0.51, 0.34, 0.30, 0.32, 0.37, 0.48, 0.62, 0.78, 0.96, 1.13, 1.32, 1.49, 1.60, 1.69, 1.74, 1.73, 1.66");
    map.put("customer.evcharger.evCharger.model", "residential_ev_1.xml");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    config.configureSingleton(uut);

    uut.initialize();

    TariffEvaluator te = uut.getTariffEvaluator();
    ReflectionTestUtils.setField(te, "tariffRepo", tariffRepo);
    ReflectionTestUtils.setField(te, "tariffSubscriptionRepo", tariffSubscriptionRepo);
    ReflectionTestUtils.setField(te, "tariffMarket", tariffMarket);

    ZonedDateTime now =
            ZonedDateTime.of(2014, 12, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    when(mockTimeslotRepo.currentTimeslot())
    .thenReturn(new Timeslot(0, now.toInstant()));
    
    TariffSpecification spec =
            new TariffSpecification(bob, PowerType.ELECTRIC_VEHICLE)
            .withPeriodicPayment(-1.0);
    spec.addRate(new Rate()
                 .withWeeklyBegin(1).withWeeklyEnd(5)
                 .withDailyBegin(0).withDailyEnd(23)
                 .withValue(-0.1077));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(0).withDailyEnd(2)
                 .withValue(-0.111));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(2).withDailyEnd(4)
                 .withValue(-0.106));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(4).withDailyEnd(6)
                 .withValue(-0.110));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(6).withDailyEnd(8)
                 .withValue(-0.104));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(8).withDailyEnd(10)
                 .withValue(-0.113));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(10).withDailyEnd(22)
                 .withValue(-0.103));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(12).withDailyEnd(14)
                 .withValue(-0.108));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(14).withDailyEnd(16)
                 .withValue(-0.105));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(16).withDailyEnd(18)
                 .withValue(-0.115));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(18).withDailyEnd(20)
                 .withValue(-0.113));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(20).withDailyEnd(22)
                 .withValue(-0.107));
    spec.addRate(new Rate()
                 .withWeeklyBegin(6).withWeeklyEnd(7)
                 .withDailyBegin(22).withDailyEnd(0)
                 .withValue(-0.109));
    spec.addRate(new RegulationRate()
                 .withUpRegulationPayment(0.0735)
                 .withDownRegulationPayment(-.025));
    Tariff tariff1 = new Tariff(spec);
    ReflectionTestUtils.setField(tariff1, "timeService", timeService);
    ReflectionTestUtils.setField(tariff1, "tariffRepo", tariffRepo);
    initTariff(tariff1);
    tariff1.setState(Tariff.State.OFFERED);

    // TODO -- the list of tariffs is not used, but API needs revision
    ArrayList<Tariff> tariffs = new ArrayList<>();
    tariffs.add(tariff1);
    uut.evaluateTariffs(tariffs);
  }

  class DummyCMA implements CustomerModelAccessor
  {

    @Override
    public CustomerInfo getCustomerInfo ()
    {
      return customer;
    }

    @Override
    public CapacityProfile getCapacityProfile (Tariff tariff)
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public double getBrokerSwitchFactor (boolean isSuperseding)
    {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public double getTariffChoiceSample ()
    {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public double getInertiaSample ()
    {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public double getShiftingInconvenienceFactor (Tariff tariff)
    {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public void notifyCustomer (TariffSubscription oldsub,
                                TariffSubscription newsub, int population)
    {
      // TODO Auto-generated method stub
      
    }
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
      return tariffSubscriptionRepo;
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
      return null;
    }

    @Override
    public TariffMarket getTariffMarket ()
    {
      return null;
    }

    @Override
    public XMLMessageConverter getMessageConverter ()
    {
      return null;
    }
  }
}
