/**
 * 
 */
package org.powertac.customer.evcharger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.CapacityProfile;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
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
  private ServerConfiguration serverConfig;
  private Configurator config;

  // mocks
  private TariffMarket tariffMarket;
  private Accounting accountingService;

  private CustomerInfo customer;
  private TariffSpecification mySpec;
  private Tariff myTariff;
  private TariffSubscription mySub;
  private EvCharger uut;

  private TariffSubscription oldSub;
  private StorageState oldSS;

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
                               //.plus(TimeService.HOUR * 7));
    start = timeService.getCurrentTime().plus(TimeService.HOUR);
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
    //when(tariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
    //    .thenReturn(defaultConsumption);

    // other brokers
    sally = new Broker("Sally");
    bob = new Broker("Bob");

    TariffSpecification evSpec =
            new TariffSpecification(defaultBroker,
                                    PowerType.ELECTRIC_VEHICLE).
                                    addRate(new Rate().withValue(0.1));
    evTariff = new Tariff(evSpec);
    initTariff(evTariff);

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

  @Test
  public void testDemandElementMeanCalculation ()
  {
    uut = new EvCharger("residential_ev");
    uut.setServiceAccessor(serviceAccessor);
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.evcharger.evCharger.model", "residential_ev_1.xml");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    serverConfig.configureMe(uut);

    uut.initialize();

    DateTime currentTime = timeService.getCurrentDateTime();
    int hod = currentTime.getHourOfDay();
    // Check that demandInfoMean is empty by default.
    assertTrue(uut.getDemandInfoMean().isEmpty());

    // Check that it equals the first demandInfo if only once requested.
    List<DemandElement> demand1 = uut.getDemandInfo(currentTime);
    List<DemandElement> demandMean1 = uut.getDemandInfoMean().get(hod);
    assertIterableEquals(demand1, demandMean1);

    // Check that the means are correctly calculated for two demands.
    List<DemandElement> demand2 = uut.getDemandInfo(currentTime);
    List<DemandElement> demandMean2 = uut.getDemandInfoMean().get(hod);
    assertEquals(demandMean2.size(), Math.max(demand1.size(), demand2.size()));
    for (int i = 0; i < Math.min(demand1.size(), demand2.size()); i++) {
      double[] hist1 = demand1.get(i).getdistribution();
      double[] hist2 = demand2.get(i).getdistribution();
      double[] histMean = demandMean2.get(i).getdistribution();
      assertEquals(histMean.length, hist1.length);
      assertEquals(histMean.length, hist2.length);
      assertEquals(demandMean2.get(i).getHorizon(),
                   demand1.get(i).getHorizon());
      assertEquals(demandMean2.get(i).getHorizon(),
                   demand2.get(i).getHorizon());
      for (int j = 0; j < hist1.length; j++) {
        assertEquals(histMean[j], (hist1[j] + hist2[j]) / 2);
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
      for (int j = 0; j < hist1.length; j++) {
        assertEquals((hist1[j] + hist2[j] + hist3[j]) / 3, histMean[j]);
      }
    }

    // Check that the demandInfoMean is extended for new hours.
    List<DemandElement> demand4 = uut.getDemandInfo(currentTime.plusHours(1));
    List<DemandElement> demandMean4 = uut.getDemandInfoMean().get(hod + 1);
    assertIterableEquals(demand4, demandMean4);

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
    uut.initialize();

    DateTime now =
            new DateTime(2014, 12, 1, 10, 0, 0, DateTimeZone.UTC);
    when(mockTimeslotRepo.currentTimeslot())
    .thenReturn(new Timeslot(0, now.toInstant()));

    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.evcharger.evCharger.population", "1000");
    map.put("customer.evcharger.evCharger.chargerCapacity", "8.0");
    map.put("customer.evcharger.evCharger.nominalDemandBias", "0.4");
    map.put("customer.evcharger.evCharger.defaultCapacityData",
            "1.55-1.46-1.36-1.25-1.16-1.02-0.80-0.51-0.34-0.30-0.32-0.37-0.48-0.62-0.78-0.96-1.13-1.32-1.49-1.60-1.69-1.74-1.73-1.66");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    serverConfig.configureMe(uut);
    assertEquals(1000, uut.getPopulation(), "correct population");
    assertEquals(8.0, uut.getChargerCapacity(), 1e-6, "correct charger capacity");
    double[] profile = uut.getDefaultCapacityProfile().getProfile();
    assertEquals(24, profile.length, "full profile");
    assertEquals(1460.0, profile[1], 1e-6, "profile OK");
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

    DateTime now =
            new DateTime(2014, 12, 1, 10, 0, 0, DateTimeZone.UTC);
    when(mockTimeslotRepo.currentTimeslot())
    .thenReturn(new Timeslot(0, now.toInstant()));

    //double chargerCapacity = 5.0; //kW
    uut.setDefaultCapacityData("3.0-3.0-3.0-3.0-3.0-3.0-3.0-4.0-4.0-4.0-3.0-3.0-"
                                  + "4.0-4.0-4.0-4.0-4.0-4.0-5.0-6.0-7.0-6.0-5.0-4.0");

  }

  @Test
  public void testFirstStepSim ()
  {
    // need to configure first

    uut = new EvCharger("residential_ev");
    uut.setServiceAccessor(serviceAccessor);
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("customer.evcharger.evCharger.population", "1000");
    map.put("customer.evcharger.evCharger.chargerCapacity", "8.0");
    map.put("customer.evcharger.evCharger.nominalDemandBias", "0.4");
    map.put("customer.evcharger.evCharger.defaultCapacityData",
            "1.55-1.46-1.36-1.25-1.16-1.02-0.80-0.51-0.34-0.30-0.32-0.37-0.48-0.62-0.78-0.96-1.13-1.32-1.49-1.60-1.69-1.74-1.73-1.66");
    map.put("customer.evcharger.evCharger.model", "residential_ev_1");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    serverConfig.configureMe(uut);

    uut.initialize();
    DateTime now =
            new DateTime(2014, 12, 1, 10, 0, 0, DateTimeZone.UTC);
    timeService.setCurrentTime(now.toInstant());
    uut.setDefaultCapacityData("3.0-3.0-3.0-3.0-3.0-3.0-3.0-4.0-4.0-4.0-3.0-3.0-"
            + "4.0-4.0-4.0-4.0-4.0-4.0-5.0-6.0-7.0-6.0-5.0-4.0");
    uut.initialize();
    // default subscription happens in CustomerModelService
    TariffSubscription defaultSub =
            subscribeTo (uut, evTariff, uut.getPopulation());
    //tariffSubscriptionRepo.add(defaultSub);
    uut.handleInitialSubscription(tariffSubscriptionRepo
                                  .findActiveSubscriptionsForCustomer(uut.getCustomerInfo()));

    when(mockTimeslotRepo.currentTimeslot())
    .thenReturn(new Timeslot(360, now.toInstant()));
    when(mockTimeslotRepo.currentSerialNumber())
    .thenReturn(360);

    // now we step and make sure everything is set up
    uut.step();

    StorageState ss = uut.getStorageState(defaultSub);
    assertNotNull(ss);
    assertEquals(defaultSub, ss.getSubscription());

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
  public void test ()
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
    //fail("Not yet implemented");
  }

  @Test
  public void testDefaultCapacityProfile ()
  {
    uut.initialize();

    DateTime now =
            new DateTime(2014, 12, 1, 10, 0, 0, DateTimeZone.UTC);
    when(mockTimeslotRepo.currentTimeslot())
    .thenReturn(new Timeslot(0, now.toInstant()));

    //double chargerCapacity = 5.0; //kW
    uut.setDefaultCapacityData("3.0-3.0-3.0-3.0-3.0-3.0-3.0-4.0-4.0-4.0-3.0-3.0-"
                                  + "4.0-4.0-4.0-4.0-4.0-4.0-5.0-6.0-7.0-6.0-5.0-4.0");
    CapacityProfile cp = uut.getDefaultCapacityProfile();
    double[] dcpn = cp.getProfile();
    assertEquals(24, dcpn.length);
    assertEquals(3000.0, dcpn[0], 1e-6);
    // The getCapacityProfile() method should also return the default until
    // there are multiple tariffs around
    cp = uut.getCapacityProfile(evTariff);
    dcpn = cp.getProfile();
    assertEquals(24, dcpn.length);
    assertEquals(4000.0, dcpn[7], 1e-6);    
  }

  @Test
  public void testRegulationCapacity ()
  {
    //TODO - test this feature
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
