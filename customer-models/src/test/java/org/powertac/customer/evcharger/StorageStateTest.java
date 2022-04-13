/**
 * 
 */
package org.powertac.customer.evcharger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Broker;
import org.powertac.common.CapacityProfile;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 *
 */
class StorageStateTest
{
  private Competition competition;
  private TimeService timeService;
  private Instant start;
  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  // brokers and initial tariffs
  private Broker defaultBroker;
  private Tariff defaultConsumption;
  private Tariff evTariff;
  private Broker bob;
  private Broker sally;

  // mocks
  private TariffMarket tariffMarket;
  private Accounting accountingService;

  private CustomerInfo customer;
  private TariffSpecification mySpec;
  private Tariff myTariff;
  private TariffSubscription mySub;
  private StorageState uut;

  private TariffSubscription oldSub;
  private StorageState oldSS;

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  void setUp () throws Exception
  {
    competition = Competition.newInstance("storage-state-test");
    timeService = new TimeService();
    timeService.setCurrentTime(competition.getSimulationBaseTime()
                               .plus(TimeService.HOUR * 7));
    start = timeService.getCurrentTime().plus(TimeService.HOUR);
    tariffSubscriptionRepo = new TariffSubscriptionRepo();
    tariffRepo = mock(TariffRepo.class);
    ReflectionTestUtils.setField(tariffSubscriptionRepo,
                                 "tariffRepo", tariffRepo);
    tariffMarket = mock(TariffMarket.class);
    accountingService = mock(Accounting.class);

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

    customer = new CustomerInfo("PodunkChargers", 1000)
            .withPowerType(PowerType.ELECTRIC_VEHICLE);
  }

  private TariffSubscription subscribeTo (CustomerInfo customer, Tariff tariff, int count)
  {
    TariffSubscription subscription =
            new TariffSubscription(customer, tariff);
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
  public void testInitial ()
  {
    double chargerCapacity = 5.0; //kW
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         customer.getPopulation());
    StorageState s1 = new StorageState(dc, chargerCapacity);
    assertEquals(dc, s1.getSubscription());
    assertEquals(customer.getPopulation(), s1.getPopulation());
    assertEquals(5.0, s1.getUnitCapacity(), 1e-6);
  }

  // check out a single demand distribution
  @Test
  public void testDemand1 ()
  {
    double chargerCapacity = 6.0; //kW
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         customer.getPopulation() / 2);
    StorageState s1 = new StorageState(dc, chargerCapacity);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(1, 4.0, 12.0));
    demand.add(new DemandElement(3, 6.0, 60.0));
    s1.distributeDemand(42, demand, 0.5);
    // StorageState should now be ts:(active, commitment)
    //   (42:(10*.5, 0), 43:(6*.5, 6), 44:(6*.5, 0), 45:(0, 60*.5))
    assertEquals(500, s1.getPopulation());
    assertNull(s1.getElement(41));
    assertNotNull(s1.getElement(42));
    // start charging here
    assertEquals(5.0, s1.getElement(42).getActiveChargers(), 1e-6);
    assertEquals(0.0, s1.getElement(42).getRemainingCommitment(), 1e-6);
    // 2 vehicles unplug at start of 43
    assertNotNull(s1.getElement(43));
    assertEquals(3.0, s1.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(6.0, s1.getElement(43).getRemainingCommitment(), 1e-6);
    // keep charging in 44
    assertNotNull(s1.getElement(44));
    assertEquals(3.0, s1.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, s1.getElement(44).getRemainingCommitment(), 1e-6);
    // done in 45
    assertNotNull(s1.getElement(45));
    assertEquals(0.0, s1.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(30.0, s1.getElement(45).getRemainingCommitment(), 1e-6);
    // check horizon
    assertEquals(4, s1.getHorizon(42));
  }

  // two demand distributions in subsequent timeslots
  @Test
  public void testDemand2 ()
  {
    double chargerCapacity = 6.0; //kW
    TariffSubscription dc =
            subscribeTo (customer, defaultConsumption,
                         (int) Math.round(customer.getPopulation() * 0.6));
    StorageState ss = new StorageState(dc, chargerCapacity);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(1, 4.0, 12.0));
    demand.add(new DemandElement(3, 6.0, 60.0));

    ss.distributeDemand(42, demand, 0.6);
    // StorageState should now be ts:(active, commitment)
    //   (42:(6, 0), 43:(3.6, 7.2), 44:(3.6, 0), 45:(0, 36))
    assertEquals(3.6, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(7.2, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(3.6, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(0.0, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(36.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
    assertEquals(4, ss.getHorizon(42));

    demand.clear();
    demand.add(new DemandElement(2, 4.0, 12.0));
    demand.add(new DemandElement(4, 6.0, 60.0));
    
    ss.distributeDemand(43, demand, 0.6);
    // StorageState should now be ts:(active, commitment)
    //   (43:(9.6, 12*.6), 44:(16*.6, 0), 45:(10*.6, 72*.6), 46:(6*.6, 0), 47:(0, 60*.6))
    assertEquals(600, ss.getPopulation());
    assertNull(ss.getElement(41));
    assertNull(ss.getElement(42)); // #42 is now gone
    assertNotNull(ss.getElement(43));
    assertEquals(16.0*.6, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(12*.6, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertNotNull(ss.getElement(44));
    assertEquals(16*.6, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    // keep charging in 44
    assertNotNull(ss.getElement(45));
    assertEquals(6*.6, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(72*.6, ss.getElement(45).getRemainingCommitment(), 1e-6);
    // no demand in 46
    assertNotNull(ss.getElement(46));
    assertEquals(6*.6, ss.getElement(46).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(46).getRemainingCommitment(), 1e-6);
    // last element is 47
    assertNotNull(ss.getElement(47));
    assertEquals(0.0, ss.getElement(47).getActiveChargers(), 1e-6);
    assertEquals(60*.6, ss.getElement(47).getRemainingCommitment(), 1e-6);
    // this is the end
    assertNull(ss.getElement(48));
    assertEquals(5, ss.getHorizon(43));
  }

  @Test
  void testDistributeRegulationUp1 ()
  {
    double chargerCapacity = 8.0; //kW
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) Math.round(customer.getPopulation() * 0.2));
    StorageState ss = new StorageState(dc, chargerCapacity);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(1, 11.0, 42.0));
    demand.add(new DemandElement(3, 15.0, 80.0));
    demand.add(new DemandElement(4, 12.0, 70.0));
    ss.distributeDemand(42, demand, 0.2);
    // StorageState should now be ts:(active, commitment)
    //   (42:(7.6, 0), 43:(5.4, 8.4), 44:(5.4, 0), 45:(2.4, 16), 46:(0, 14))
    assertEquals(7.6, ss.getElement(42).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(42).getRemainingCommitment(), 1e-6);
    assertEquals(5.4, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(8.4, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(5.4, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(2.4, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(16.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
    assertEquals(0.0, ss.getElement(46).getActiveChargers(), 1e-6);
    assertEquals(14.0, ss.getElement(46).getRemainingCommitment(), 1e-6);

    // Now assume we are now in ts 43, after we provided regulation that can 
    // be fully absorbed in ts 43. We have 16 kWh available in ts 45
    ss.distributeRegulation(43, 7.0);
    assertEquals(5.4, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(8.4, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(5.4, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(7.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(2.4, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(16.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
    assertEquals(0.0, ss.getElement(46).getActiveChargers(), 1e-6);
    assertEquals(14.0, ss.getElement(46).getRemainingCommitment(), 1e-6);
  }

  @Test
  void testDistributeRegulationUp2 ()
  {
    double chargerCapacity = 8.0; //kW
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) Math.round(customer.getPopulation() * 0.2));
    StorageState ss = new StorageState(dc, chargerCapacity);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(1, 11.0, 42.0));
    demand.add(new DemandElement(3, 15.0, 80.0));
    ss.distributeDemand(42, demand, 0.2);
    // StorageState should now be ts:(active, commitment)
    //   (42:(5.2, 0), 43:(3, 8.4), 44:(3, 0), 45:(0, 16))
    assertEquals(5.2, ss.getElement(42).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(42).getRemainingCommitment(), 1e-6);
    assertEquals(3.0, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(8.4, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(3.0, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(0.0, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(16.0, ss.getElement(45).getRemainingCommitment(), 1e-6);

    // Now assume we are now in ts 43, after we provided regulation that can 
    // be fully absorbed in ts 43. We have 8.4 kW available in the first ts
    ss.distributeRegulation(43, 7.0);
    assertEquals(3.0, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(15.4, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(3.0, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(0.0, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(16.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
  }

  /**
   * Here we need two subscriptions to two different tariffs and two different subscriptions.
   * The "old" subscription is needed because its StorageState needs to retrieve the population.
   * The "new" subscription may have 0 or non-zero population.
   * Start with all of PodunkChargers subscribed to the default consumption tariff, and create a
   * StorageState for that subscription.
   * Then move half of them to a new EV tariff by calling the CMA. This will test the ability to 
   */
  //@Test
  void testNewSubscription ()
  {
    double chargerCapacity = 5.0; //kW
    oldSub = subscribeTo (customer, defaultConsumption, customer.getPopulation());
    oldSS = new StorageState(oldSub, chargerCapacity);
    TariffSpecification ts1 =
            new TariffSpecification(bob,
                                    PowerType.ELECTRIC_VEHICLE).
                                    addRate(new Rate().withValue(-0.09))
                                    .withSignupPayment(-2.0);
        Tariff tariff1 = new Tariff(ts1);
        initTariff(tariff1);
    //fail("Not yet implemented");
  }

  // Test both nominal demand and regulation capacity
  @Test
  void testNominalDemand ()
  {
    
  }

  @Test
  void testClear ()
  {
    
  }

  @Test
  void testClean ()
  {
    
  }

  @Test
  void testGatherState ()
  {
    double chargerCapacity = 6.0; //kW
    TariffSubscription dc =
            subscribeTo (customer, defaultConsumption,
                         (int) Math.round(customer.getPopulation() * 0.6));
    StorageState ss = new StorageState(dc, chargerCapacity);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(1, 4.0, 12.0)); //43:24.6,7.2
    demand.add(new DemandElement(3, 6.0, 60.0)); //45:21,36
    demand.add(new DemandElement(4, 20.0, 200.0)); //46:9,120
    demand.add(new DemandElement(5, 15.0, 180.0)); //47:0,108
    ss.distributeDemand(42, demand, 0.6);
    // StorageState should now be ts:(active, commitment)
    //   (42:(27, 0), 43:(24.6, 7.2), 44:(24.6, 0), 45:(21, 36), 46:(9, 120), 47:(0, 108))
    assertEquals(27.0, ss.getElement(42).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(42).getRemainingCommitment(), 1e-6);
    assertEquals(24.6, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(7.2, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(24.6, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(21.0, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(36.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
    assertEquals(9.0, ss.getElement(46).getActiveChargers(), 1e-6);
    assertEquals(120.0, ss.getElement(46).getRemainingCommitment(), 1e-6);
    assertEquals(0.0, ss.getElement(47).getActiveChargers(), 1e-6);
    assertEquals(108.0, ss.getElement(47).getRemainingCommitment(), 1e-6);

    List<Object> result = ss.gatherState(42);
    assertEquals(6, result.size());
    assertEquals(42, ((List)result.get(0)).get(0));
  }

  class DummyCMA implements CustomerModelAccessor
  {

    @Override
    public CustomerInfo getCustomerInfo ()
    {
      // TODO Auto-generated method stub
      return null;
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
}
