/**
 * 
 */
package org.powertac.customer.evcharger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

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
  private int maxHorizon = 48;

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
                                    addRate(new Rate().withValue(-0.4));
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
    StorageState s1 = new StorageState(dc, chargerCapacity, maxHorizon);
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
    // so the ratio is 0.5
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(1, 4.0, 12.0));
    demand.add(new DemandElement(3, 6.0, 60.0));
    ss.distributeDemand(42, demand, 0.5);
    // StorageState should now be ts:(active, commitment)
    //   (42:(5, 0), 43:(10*.5, 6), 44:(6*.5, 0), 45:(6*.5, 60*.5))
    assertEquals(500, ss.getPopulation());
    assertNull(ss.getElement(41));
    assertNotNull(ss.getElement(42));
    // start charging here
    assertEquals(5.0, ss.getElement(42).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(42).getRemainingCommitment(), 1e-6);
    // 2 vehicles unplug at end of 43
    assertNotNull(ss.getElement(43));
    assertEquals(5.0, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(6.0, ss.getElement(43).getRemainingCommitment(), 1e-6);
    // keep charging in 44
    assertNotNull(ss.getElement(44));
    assertEquals(3.0, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertNotNull(ss.getElement(45));
    assertEquals(3.0, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(30.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
    // done in 46
    assertNull(ss.getElement(46));
    // check horizon
    assertEquals(4, ss.getHorizon(42));
  }

  // two demand distributions in subsequent timeslots
  @Test
  public void testDemand2 ()
  {
    double chargerCapacity = 6.0; //kW
    TariffSubscription dc =
            subscribeTo (customer, defaultConsumption,
                         (int) Math.round(customer.getPopulation() * 0.6));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(1, 4.0, 12.0));
    demand.add(new DemandElement(3, 6.0, 60.0));

    ss.distributeDemand(42, demand, 0.6);
    // StorageState should now be ts:(active, commitment)
    //   (42:(6, 0), 43:(6, 7.2), 44:(3.6, 0.0), 45:(3.6, 36))
    assertEquals(6.0, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(7.2, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(3.6, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(3.6, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(36.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
    assertEquals(4, ss.getHorizon(42));

    demand.clear();
    demand.add(new DemandElement(2, 4.0, 12.0)); //45
    demand.add(new DemandElement(4, 6.0, 60.0)); //47
    
    ss.distributeDemand(43, demand, 0.6);
    // StorageState should now be ts:(active, commitment)
    //   (43:(9.6, 7.2), 44:(16*.6, 0), 45:(10*.6, 36+7.2), 46:(6*.6, 0), 47,(6*.6, 60*.6))
    assertEquals(600, ss.getPopulation());
    assertNull(ss.getElement(42)); // #42 is now gone
    assertNotNull(ss.getElement(43));
    assertEquals(12.0, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(7.2, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertNotNull(ss.getElement(44));
    assertEquals(9.6, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    // keep charging in 44
    assertNotNull(ss.getElement(45));
    assertEquals(9.6, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(43.2, ss.getElement(45).getRemainingCommitment(), 1e-6);
    // no demand in 46
    assertNotNull(ss.getElement(46));
    assertEquals(6*.6, ss.getElement(46).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(46).getRemainingCommitment(), 1e-6);
    // final is 47
    assertNotNull(ss.getElement(47));
    assertEquals(6*.6, ss.getElement(47).getActiveChargers(), 1e-6);
    assertEquals(36.0, ss.getElement(47).getRemainingCommitment(), 1e-6);
    assertNull(ss.getElement(48));
    assertEquals(5, ss.getHorizon(43));
  }

  @Test
  void testTranche ()
  {
    double chargerCapacity = 4.0; //kW
    TariffSubscription dc =
            subscribeTo (customer, defaultConsumption,
                         (int) Math.round(customer.getPopulation() * 1.0));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(0, 4.0, 12.0));
    demand.add(new DemandElement(2, 7.0, 12.0));
    demand.add(new DemandElement(4, 14.0, 25.0));
    demand.add(new DemandElement(5, 6.0, 191.0));
    ss.distributeDemand(22, demand, 1.0);
    // nchargers should be (0:31, 1:27, 2:27, 3:20, 4:20, 5:6) 
    assertEquals(4, ss.getTranche(22));
    assertEquals(0, ss.getTranche(23));
    assertEquals(7, ss.getTranche(24));
    assertEquals(0, ss.getTranche(25));
    assertEquals(14, ss.getTranche(26));
    assertEquals(6, ss.getTranche(27));
  }

  @Test
  void testMinEnergyReq ()
  {
    double chargerCapacity = 2.0; //kW
    TariffSubscription dc =
            subscribeTo (customer, defaultConsumption,
                         (int) Math.round(customer.getPopulation() * 0.6));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(0, 4.0, 12.0)); // needs 3 kW
    demand.add(new DemandElement(1, 4.0, 12.0)); // needs 1.5 kW
    demand.add(new DemandElement(2, 4.0, 25.0)); // needs just over 2 kW
    demand.add(new DemandElement(3, 6.0, 38.3));// just under 2 kW
    ss.distributeDemand(22, demand, 0.8);

    TreeMap<Double, Integer> map = ss.getMinEnergyRequirements(22);
    assertEquals(2, map.size());
    Iterator<Double> result = map.descendingKeySet().iterator();
    Double y = result.next();
    assertEquals(0, map.get(y));
  }

  @Test
  void testDistributeRegulationUp1 ()
  {
    double chargerCapacity = 8.0; //kW
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) Math.round(customer.getPopulation() * 0.2));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(1, 11.0, 42.0));
    demand.add(new DemandElement(3, 15.0, 80.0));
    demand.add(new DemandElement(4, 12.0, 70.0));
    ss.distributeDemand(42, demand, 0.2);
    // StorageState should now be ts:(active, commitment)
    //   (42:(7.6, 0), 43:(5.4, 8.4), 44:(5.4, 0), 45:(2.4, 16), 46:(0, 14))
    assertEquals(7.6, ss.getElement(42).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(42).getRemainingCommitment(), 1e-6);
    assertEquals(7.6, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(8.4, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(5.4, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(5.4, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(16.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
    assertEquals(2.4, ss.getElement(46).getActiveChargers(), 1e-6);
    assertEquals(14.0, ss.getElement(46).getRemainingCommitment(), 1e-6);

    // Now assume we are in ts 43, and up-regulate by 7 kWh
    // We don't take anything from the current ts
    // Power/charger is 7 kWh / 5.4 chargers
    ss.distributeRegulation(43, 7.0);
    assertEquals(8.4, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    // ts 45 has 3 chargers, gets 3.8889 kWh
    assertEquals(16.0 + 3.8888889, ss.getElement(45).getRemainingCommitment(), 1e-6);
    // ts 46 has 2.4 chargers, gets 3.1111 kWn
    assertEquals(14.0 + 3.111111, ss.getElement(46).getRemainingCommitment(), 1e-6);
  }

  @Test
  void testDistributeRegulationUp2 ()
  {
    double chargerCapacity = 8.0; //kW
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) Math.round(customer.getPopulation() * 0.2));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(1, 11.0, 42.0));
    demand.add(new DemandElement(3, 15.0, 80.0));
    ss.distributeDemand(42, demand, 0.2);
    // StorageState should now be ts:(active, commitment)
    //   (42:(5.2, 0), 43:(3, 8.4), 44:(3, 0), 45:(0, 16))
    assertEquals(5.2, ss.getElement(42).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(42).getRemainingCommitment(), 1e-6);
    assertEquals(5.2, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(8.4, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(3.0, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(3.0, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(16.0, ss.getElement(45).getRemainingCommitment(), 1e-6);

    // Now assume we are in ts 43, get up-regulation of 7 kWh
    // ts 43 is not affected, 44 has no commitment, so it's all on 45
    ss.distributeRegulation(43, 7.0);
    
    assertEquals(8.4, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(23.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
  }

  @Test
  void testDistributeUsage ()
  {
    double chargerCapacity = 6.0; //kW
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) Math.round(customer.getPopulation() * 0.4));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    
    demand.add(new DemandElement(2, 11.0, 42.0));
    demand.add(new DemandElement(3, 15.0, 80.0));
    demand.add(new DemandElement(5, 12.0, 60.0));
    demand.add(new DemandElement(7, 25.0, 130.0));
    ss.distributeDemand(42, demand, 0.4);
    // StorageState should now be ts:(active, commitment)
    //   (42:(25.2, 0), 43:(25.2, 0), 44:(25.2, 16.8), 45:(14.8, 32),
    //    46:(14.8, 0), 47:(37.0 * 0.4, 24), 48:(10.0, 0), 49:(10.0, 52))
    assertEquals(25.2, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(16.8, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(20.8, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(32.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
    assertEquals(10.0, ss.getElement(49).getActiveChargers(), 1e-6);
    assertEquals(52.0, ss.getElement(49).getRemainingCommitment(), 1e-6);

    // no timeslots need more than their share
    assertEquals(0, ss.getMinEnergyRequirements(42).size());

    ss.distributeUsage(42, 100.0);
    // charge rate should be 100/25.2 = 3.97 kW
    assertEquals(0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    // charge rate is now 4 kW
    assertEquals(8.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
    assertEquals(4.8, ss.getElement(47).getRemainingCommitment(), 1e-6);
    assertEquals(12.0, ss.getElement(49).getRemainingCommitment(), 1e-6);
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
    oldSS = new StorageState(oldSub, chargerCapacity, maxHorizon);
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
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

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
    assertEquals(27.0, ss.getElement(43).getActiveChargers(), 1e-6);
    assertEquals(7.2, ss.getElement(43).getRemainingCommitment(), 1e-6);
    assertEquals(24.6, ss.getElement(44).getActiveChargers(), 1e-6);
    assertEquals(0.0, ss.getElement(44).getRemainingCommitment(), 1e-6);
    assertEquals(24.6, ss.getElement(45).getActiveChargers(), 1e-6);
    assertEquals(36.0, ss.getElement(45).getRemainingCommitment(), 1e-6);
    assertEquals(21.0, ss.getElement(46).getActiveChargers(), 1e-6);
    assertEquals(120.0, ss.getElement(46).getRemainingCommitment(), 1e-6);
    assertEquals(9.0, ss.getElement(47).getActiveChargers(), 1e-6);
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
