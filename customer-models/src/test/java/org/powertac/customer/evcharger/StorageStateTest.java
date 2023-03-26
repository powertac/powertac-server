/**
 * 
 */
package org.powertac.customer.evcharger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Instant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

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
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.util.Pair;
import org.springframework.test.util.ReflectionTestUtils;

import cern.colt.Arrays;

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
  private int population = 1000;

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
  private TariffSubscription mySub;
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
                               .plusMillis(TimeService.HOUR * 7));
    start = timeService.getCurrentTime().plusMillis(TimeService.HOUR);
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

    customer = new CustomerInfo("PodunkChargers", population)
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

  // check out a single demand distribution, current ts
  @Test
  public void testDemand1 ()
  {
    double chargerCapacity = 6.0; //kW
    double ratio = 0.8;
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) (customer.getPopulation() * ratio));
    // so the ratio is 0.8
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(0, 4.0, 0.0, // 4 chargers, 3 kWh each
                                 new double[]{1.0}));
    ss.distributeDemand(36, demand, 0.8);
    assertEquals(800, ss.getPopulation());
    assertNotNull(ss.getElement(36));
    //assertEquals(3.2, ss.getElement(36).getActiveChargers());
    // should be 3.2 chargers, each half-power
    assertArrayEquals(new double[] {3.2},
                      ss.getElement(36).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {9.6},
                    ss.getElement(36).getEnergy(), 1e-6);
  }

  // distribute demand over two timeslots
  @Test
  public void testDemand2 ()
  {
    double chargerCapacity = 6.0; //kW
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         customer.getPopulation() / 2);
    // so the ratio is 0.5
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(0, 4.0, 0.0,
                                 new double[]{1.0}));
    demand.add(new DemandElement(1, 6.0, 0.0,
                                 new double[]{0.4, 0.6}));
    ss.distributeDemand(42, demand, 0.5);
    // StorageState should now be ts:(active, commitment)
    //   42:5,{6.0}, 43:3,{10.8,5.4}
    assertEquals(500, ss.getPopulation());
    assertNull(ss.getElement(41));
    assertNotNull(ss.getElement(42));
    // start charging here
    //assertEquals(5.0, ss.getElement(42).getActiveChargers(), 1e-6);
    assertArrayEquals(new double[] {6.0},
                      ss.getElement(42).getEnergy(), 1e-6);

    assertNotNull(ss.getElement(43));
    //assertEquals(3.0, ss.getElement(43).getActiveChargers(), 1e-6);
    assertArrayEquals(new double[] {10.8,5.4},
                      ss.getElement(43).getEnergy(), 1e-6);
  }

  // Test actual demand that's causing strange results
  @Test
  void testDemandX ()
  {
    double chargerCapacity = 7.0; //kW
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         customer.getPopulation());
    // so the ratio is 1.0
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    // Actual demand from early trial
    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(0, 2.0,
                                 new double[] {2.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(1, 1.0,
                                 new double[] {0.0, 1.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(2, 1.0,
                                 new double[] {0.0, 0.0, 1.0, 0.0, 0.0}));
    demand.add(new DemandElement(3, 0.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(4, 0.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(5, 0.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(6, 0.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(7, 0.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(8, 0.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(9, 2.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(10, 3.0,
                                 new double[] {1.0, 0.0, 0.0, 1.0, 1.0}));
    demand.add(new DemandElement(11, 2.0,
                                 new double[] {0.0, 1.0, 1.0, 0.0, 0.0}));
    demand.add(new DemandElement(12, 1.0,
                                 new double[] {0.0, 0.0, 0.0, 1.0, 0.0}));
    demand.add(new DemandElement(13, 1.0,
                                 new double[] {0.0, 1.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(14, 1.0,
                                 new double[] {0.0, 0.0, 1.0, 0.0, 0.0}));
    demand.add(new DemandElement(15, 0.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(16, 2.0,
                                 new double[] {2.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(17, 0.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(18, 0.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    demand.add(new DemandElement(19, 0.0,
                                 new double[] {0.0, 0.0, 0.0, 0.0, 0.0}));
    ss.distributeDemand(0, demand, 1.0);
    assertEquals(1000, ss.getPopulation());
    assertNotNull(ss.getElement(0));
    //assertEquals(16.0, ss.getElement(0).getActiveChargers(), 1e-6);
    assertArrayEquals(new double[] {7.0},
                      ss.getElement(0).getEnergy(), 1e-6);
  }
  
  // Test computation of min and max capacity
  @Test
  void testMinMax ()
  {
    double chargerCapacity = 4.0; //kW
    double ratio = 0.8;
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) Math.round(customer.getPopulation() * ratio));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    // min: 8ch * 4kW * .5 = 16
    demand.add(new DemandElement(0, 10.0, new double[]{1.0}));
    // min: 6ch * 4kW = 24, max: 6 ch * 4kW * .5 = 12
    demand.add(new DemandElement(1, 15.0, new double[] {0.5,0.5}));
    // min: 2.4ch * 4 kW = 9.6, max: 14.4ch * 4 kW + 7.2ch * 4kw * .5 = 72
    demand.add(new DemandElement(2, 30.0, new double[] {0.1,0.6,0.3}));
    ss.distributeDemand(22, demand, ratio);

    double[] minMax = ss.getMinMax(22);
    assertEquals(49.6, minMax[0], 1e-6);
    assertEquals(133.6, minMax[1], 1e-6);
    assertEquals(49.6 + (133.6 - 49.6) / 2.0, minMax[2], 1e-6);
  }

  // Create some demand, distribute maximum usage.
  @Test
  void testDistributeUsageMax ()
  {
    double chargerCapacity = 6.0; //kW
    double ratio = 0.7;
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) Math.round(customer.getPopulation() * ratio));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    // min = max = 7ch * 6kW * .5 = 21
    demand.add(new DemandElement(0, 10.0, new double[]{1.0}));
    // min: 5.25ch * 6kW = 31.5, max: + 5.25ch * 6kW * .5 = 42
    demand.add(new DemandElement(1, 15.0, new double[] {0.5, 0.5}));
    // min: 2.1ch * 6 kW = 12.6, max: + 21ch*.6*6kW+21ch*.3*6kw*.5 = 107.1
    demand.add(new DemandElement(2, 30.0, new double[] {0.1, 0.6, 0.3}));
    ss.distributeDemand(22, demand, ratio);
    double[] minMax = ss.getMinMax(22);
    assertEquals(65.1, minMax[0], 1e-6); // 21.0 + 31.5 + 12.6
    assertEquals(175.35, minMax[1], 1e-6); //21.0 + 42.0 + 107.1
    assertEquals(120.225, minMax[2], 1e-6); // 65.1 + (170.1 - 65.1) / 2

    // nominal is min + (max - min) / 2
    ss.distributeUsage(22, 175.35);
    assertEquals(0.0, ss.getElement(22).getEnergy()[0], 1e-6);
    assertEquals(2, ss.getElement(23).getEnergy().length);
    assertArrayEquals(new double[] {2.1, 12.6, 6.3},
                      ss.getElement(24).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {18.9, 37.8, 0.0},
                      ss.getElement(24).getEnergy(), 1e-6);
  }

  // Create some demand, distribute minimum usage.
  @Test
  void testDistributeUsageMin ()
  {
    
  }

  // Create some demand, distribute nominal usage.
  @Test
  void testDistributeUsageNominal ()
  {
    double chargerCapacity = 6.0; //kW
    double ratio = 0.7;
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) Math.round(customer.getPopulation() * ratio));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    // min = max = 7ch * 6kW * .5 = 21
    demand.add(new DemandElement(0, 10.0, new double[]{1.0}));
    // min: 5.25ch * 6kW = 31.5, max: + 5.25ch * 6kW * .5 = 42
    demand.add(new DemandElement(1, 15.0, new double[] {0.5, 0.5}));
    // min: 2.1ch * 6 kW = 12.6, max: + 21ch*.6*6kW+21ch*.3*6kw*.5 = 107.1
    demand.add(new DemandElement(2, 30.0, new double[] {0.1, 0.6, 0.3}));
    ss.distributeDemand(22, demand, ratio);
    double[] minMax = ss.getMinMax(22);
    assertEquals(65.1, minMax[0], 1e-6); // 21.0 + 31.5 + 12.6
    assertEquals(175.35, minMax[1], 1e-6); //21.0 + 42.0 + 107.1
    assertEquals(120.225, minMax[2], 1e-6); // 65.1 + (170.1 - 65.1) / 2

    // nominal is min + (max - min) / 2
    ss.distributeUsage(22, 120.225);
    assertEquals(0.0, ss.getElement(22).getEnergy()[0], 1e-6);
    assertEquals(2, ss.getElement(23).getEnergy().length);
    assertArrayEquals(new double[] {2.1, 12.6, 6.3},
                      ss.getElement(24).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {18.9, 75.6, 9.45},
                      ss.getElement(24).getEnergy(), 1e-6);
  }

  // Create some demand, use power, clean up, add more demand 
  @Test
  void testDemandTwice ()
  {
    double chargerCapacity = 8.0; //kW
    double ratio = 0.8;
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) Math.round(customer.getPopulation() * ratio));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    // 8ch = {32}
    demand.add(new DemandElement(0, 10.0, new double[]{1.0}));
    // 6ch, 6 ch = {72, 24}
    demand.add(new DemandElement(1, 15.0, new double[] {0.5,0.5}));
    // 2.4ch, 14.4ch, 7.2ch = {48, 172.8, 28.8}
    demand.add(new DemandElement(2, 30.0, new double[] {0.1,0.6,0.3}));

    // start in ts 22
    ss.distributeDemand(22, demand, ratio);
    assertArrayEquals(new double[] {32.0},
                      ss.getElement(22).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {72.0, 24.0},
                      ss.getElement(23).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {48.0, 172.8, 28.8},
                      ss.getElement(24).getEnergy(), 1e-6);

    double[] minMax = ss.getMinMax(22);
    assertArrayEquals(new double[] {99.2, 267.2, 183.2}, minMax, 1e-6);
    ss.distributeUsage(22, minMax[2]);

    // now we move to ts 23
    ss.collapseElements(23);
    ss.rebalance(23);
    assertArrayEquals(new double[] {36.0}, ss.getElement(23).getEnergy(), 1e-6);
    //System.out.println("24: " + ss.getElement(24).toString());
    assertArrayEquals(new double[] {93.6, 64.8}, ss.getElement(24).getEnergy(), 1e-6);

    demand.clear();
    demand.add(new DemandElement(1, 18, new double[] {0.6, 0.4}));
    demand.add(new DemandElement(2, 24, new double[] {0.4, 0.4, 0.2}));
    ss.distributeDemand(23, demand, ratio);
    //System.out.println("23: " + ss.getElement(23).toString());
    //System.out.println("24: " + ss.getElement(24).toString());
    //System.out.println("25: " + ss.getElement(25).toString());
    assertArrayEquals(new double[] {197.28, 87.84},
                      ss.getElement(24).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {153.6, 92.16, 15.36},
                      ss.getElement(25).getEnergy(), 1e-6);
  }

  @Test
  void testCopyState ()
  {
    double chargerCapacity = 8.0; //kW
    double ratio = 1.0;
    TariffSubscription dc = subscribeTo (customer, defaultConsumption, 100);
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(0, 8.0, // 8 chg, 32 kWh
                                 new double[] {1.0}));
    demand.add(new DemandElement(1, 22.0, // 22 chg, p={6.6,15.4}, e={79.2,61.6}
                                 new double[] {0.3,0.7}));
    demand.add(new DemandElement(2, 32.0, // 32 chg, p={6.4,19.2,6.4}, e={25.6,230.4,128.0}
                                 new double[] {0.2,0.6,0.2}));
    ss.distributeDemand(36, demand, ratio);

    // confirm demand data
    assertArrayEquals(new double[] {8.0},
                      ss.getElement(36).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {32.0},
                      ss.getElement(36).getEnergy(), 1e-6);

    assertArrayEquals(new double[] {6.6, 15.4},
                      ss.getElement(37).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {79.2, 61.6},
                      ss.getElement(37).getEnergy(), 1e-6);

    assertArrayEquals(new double[] {6.4, 19.2, 6.4},
                      ss.getElement(38).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {128.0, 230.4, 25.6},
                      ss.getElement(38).getEnergy(), 1e-6);

    StorageState copy = ss.copy();

    // confirm demand data in copy
    assertArrayEquals(new double[] {8.0},
                      copy.getElement(36).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {32.0},
                      copy.getElement(36).getEnergy(), 1e-6);

    assertArrayEquals(new double[] {6.6, 15.4},
                      copy.getElement(37).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {79.2, 61.6},
                      copy.getElement(37).getEnergy(), 1e-6);

    assertArrayEquals(new double[] {6.4, 19.2, 6.4},
                      copy.getElement(38).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {128.0, 230.4, 25.6},
                      copy.getElement(38).getEnergy(), 1e-6);
    
    // modify the new state, ensure the old one was not changed
    copy.distributeUsage(36, 136.0);

    // confirm demand data again
    assertArrayEquals(new double[] {8.0},
                      ss.getElement(36).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {32.0},
                      ss.getElement(36).getEnergy(), 1e-6);

    assertArrayEquals(new double[] {6.6, 15.4},
                      ss.getElement(37).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {79.2, 61.6},
                      ss.getElement(37).getEnergy(), 1e-6);

    assertArrayEquals(new double[] {6.4, 19.2, 6.4},
                      ss.getElement(38).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {128.0, 230.4, 25.6},
                      ss.getElement(38).getEnergy(), 1e-6);

  }

  @Test
  void testRebalance ()
  {
    double chargerCapacity = 8.0; //kW
    double ratio = 0.6;
    TariffSubscription dc = subscribeTo (customer, defaultConsumption,
                                         (int) Math.round(customer.getPopulation() * ratio));
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(0, 8.0, // 4.8 chg, 19.2 kWh
                                 new double[] {1.0}));
    demand.add(new DemandElement(1, 22.0, // 13.2 chg, p={3.96, 9.24}, e={47.52,36.96}
                                 new double[] {0.3,0.7}));
    demand.add(new DemandElement(2, 32.0, // 19.2 chg, p={3.84,11.52,3.84}, e={76.8,138.24,15.36}
                                 new double[] {0.2,0.6,0.2}));
    ss.distributeDemand(36, demand, ratio);
    assertArrayEquals(new double[] {4.8},
                      ss.getElement(36).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {19.2},
                      ss.getElement(36).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {3.96, 9.24},
                      ss.getElement(37).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {47.52,36.96}, // [1.5, 0.5]
                      ss.getElement(37).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {3.84,11.52,3.84},
                      ss.getElement(38).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {76.8,138.24,15.36}, // [2.5, 1.5, 0.5]
                      ss.getElement(38).getEnergy(), 1e-6);

    StorageState ss1 = ss.copy();

    // ----------------------------
    // Now we apply minimal usage
    double[] minMax = ss1.getMinMax(36);
    assertArrayEquals(new double[] {81.6, 226.08, 153.84}, minMax, 1e-8);

    ss1.distributeUsage(36, minMax[0]); // minimal usage
    assertArrayEquals(new double[] {0.0},
                      ss1.getElement(36).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {15.84, 36.96}, // [0.5, 0.5]
                      ss1.getElement(37).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {46.08,138.24,15.36}, // [1.5, 1.5, 0.5]
                      ss1.getElement(38).getEnergy(), 1e-6);
    
    // collapse and rebalance
    ss1.collapseElements(37);
    assertArrayEquals(new double[] {3.96 + 9.24},
                      ss1.getElement(37).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {15.84 + 36.96},
                      ss1.getElement(37).getEnergy(), 1e-6);

    assertArrayEquals(new double[] {3.84, 15.36},
                      ss1.getElement(38).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {46.08, 138.24 + 15.36},
                      ss1.getElement(38).getEnergy(), 1e-6);

    StorageElement se = ss1.getElement(38);
    double totalE = se.getEnergy()[1] + se.getEnergy()[0];
    //System.out.println("38 before: " + se.toString());
    ss1.rebalance(37);
    assertArrayEquals(new double[] {3.96 + 9.24},
                      ss1.getElement(37).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {15.84 + 36.96},
                      ss1.getElement(37).getEnergy(), 1e-6);

    //System.out.println("38 after: " + se.toString());
    assertEquals(totalE, se.getEnergy()[1] + se.getEnergy()[0], 1e-6);
    assertEquals(0.5, se.getEnergy()[1]
            / (se.getPopulation()[1] * chargerCapacity), 1e-6);
    assertArrayEquals(new double[] {15.36, 3.84},
                      ss1.getElement(38).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {184.32, 15.36},
                      ss1.getElement(38).getEnergy(), 1e-6);

    // -------------------------------------------------
    // next we apply max usage, collapse and rebalance
    ss1 = ss.copy();
    ss1.distributeUsage(36, minMax[1]); // max usage
    assertArrayEquals(new double[] {0.0},
                      ss1.getElement(36).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {15.84, 0.0}, // [0.5, 0.0]
                      ss1.getElement(37).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {46.08,46.08,0.0}, // [1.5, 0.5, 0.0]
                      ss1.getElement(38).getEnergy(), 1e-6);
    
    ss1.collapseElements(37);
    ss1.rebalance(37);
    assertArrayEquals(new double[] {3.96},
                      ss1.getElement(37).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {15.84},
                      ss1.getElement(37).getEnergy(), 1e-6);

    assertArrayEquals(new double[] {3.84, 11.52},
                      ss1.getElement(38).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {46.08, 46.08},
                      ss1.getElement(38).getEnergy(), 1e-6);

    // -------------------------------------
    // finally we apply nominal usage, collapse and rebalance
    ss1 = ss.copy();
    ss1.distributeUsage(36, minMax[2]); // nominal usage
    assertArrayEquals(new double[] {0.0},
                      ss1.getElement(36).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {15.84, 18.48}, // [0.5, 0.25]
                      ss1.getElement(37).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {46.08,92.16,7.68}, // [1.5, 1.0, 0.25]
                      ss1.getElement(38).getEnergy(), 1e-6);

    ss1.collapseElements(37);

    se = ss1.getElement(38);
    totalE = se.getEnergy()[1] + se.getEnergy()[0];
    //System.out.println("38 before: " + se.toString());
    ss1.rebalance(37);
    assertArrayEquals(new double[] {3.96 + 9.24},
                      ss1.getElement(37).getPopulation(), 1e-6);
    assertArrayEquals(new double[] {15.84+18.48},
                      ss1.getElement(37).getEnergy(), 1e-6);

    //System.out.println("38 after: " + se.toString());
    assertEquals(totalE, se.getEnergy()[1] + se.getEnergy()[0], 1e-6);
    assertArrayEquals(new double[] {8.64, 10.56},
                      ss1.getElement(38).getPopulation(), 1e-5);
    assertArrayEquals(new double[] {103.68, 42.24},
                      ss1.getElement(38).getEnergy(), 1e-6);
  }

  @Test
  void testDistributeRegulation ()
  {
    double chargerCapacity = 8.0; //kW
    double ratio = 1.0;
    TariffSubscription dc = subscribeTo (customer, defaultConsumption, 100);
    StorageState ss = new StorageState(dc, chargerCapacity, maxHorizon);

    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(0, 8.0, // 8 chg, 32 kWh
                                 new double[] {1.0}));
    demand.add(new DemandElement(1, 22.0, // 22 chg, p={6.6,15.4}, e={79.2,61.6}
                                 new double[] {0.3,0.7}));
    demand.add(new DemandElement(2, 32.0, // 32 chg, p={6.4,19.2,6.4}, e={25.6,230.4,128.0}
                                 new double[] {0.2,0.6,0.2}));
    ss.distributeDemand(36, demand, ratio);

    // energy is now [32.0], [79.2, 61.6], [128.0, 230.4, 25.6]
    StorageState ssc = ss.copy();
    double[] minMax = ssc.getMinMax(36);
    assertArrayEquals(new double[] {136.0, 376.8, 256.4}, minMax, 1e-6);
    ssc.distributeUsage(36,  minMax[1]);
    // upRegulationCapacity = 376.8 - 136.0; 
    double result = ssc.distributeRegulation(37, 100.0); // next timeslot
    //System.out.println("difference = " + result);
    assertEquals(0.0, result, 1e-6);

    //distribute nominal usage, check state, then distribute regulation in following timeslot
    ssc = ss.copy();
    ssc.distributeUsage(36, minMax[2]);
    assertArrayEquals(new double[] {0.0},
                      ssc.getElement(36).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {26.4, 30.8}, // (6.6*8*.5), (15.4*8*.25)
                      ssc.getElement(37).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {76.8, 153.6, 12.8}, // 6.4*8*1.5, 19.2*8*1.0, 6.4*.25
                      ssc.getElement(38).getEnergy(), 1e-6);
    // down-regulation capacity = 136 - 256.4
    result = ssc.distributeRegulation(37, -100);
    assertEquals(0.0, result, 1e-6);

    // distribute usage that is close to max and try regulating it
    ssc = ss.copy();
    minMax = ssc.getMinMax(36);
    ssc.distributeUsage(36,  minMax[1] - 50);
    // upRegulationCapacity = 326.8 - 136.0; 
    result = ssc.distributeRegulation(37, 100.0); // next timeslot
    assertEquals(0.0, result, 1e-6);    
  }

  /**
   * Here we need two subscriptions to two different tariffs and two different subscriptions.
   * The "old" subscription is needed because its StorageState needs to retrieve the population.
   * The "new" subscription may have 0 or non-zero population, in this case zero because it's new.
   * Start with all of PodunkChargers subscribed to the default consumption tariff, and create a
   * StorageState for that subscription.
   * Then move 40% of them to a new EV tariff. This will test the ability to correctly populate
   * a new subscription.
   */
  @Test
  void testNewSubscription ()
  {
    double chargerCapacity = 7.0; //kW
    oldSub = subscribeTo (customer, defaultConsumption, customer.getPopulation()); // 100%
    oldSS = new StorageState(oldSub, chargerCapacity, maxHorizon);
    // add some demand
    ArrayList<DemandElement> demand = new ArrayList<>();
    demand.add(new DemandElement(0, 11.0, new double[] {1.0}));
    demand.add(new DemandElement(1, 15.0, new double[] {0.4, 0.6}));
    demand.add(new DemandElement(3, 12.0, new double[] {0.2, 0.3, 0.3, 0.2}));
    demand.add(new DemandElement(4, 25.0, new double[] {0.1, 0.3, 0.3, 0.2, 0.1}));
    oldSS.distributeDemand(40, demand, 1.0);
    assertArrayEquals(new double[] {38.5},
                      oldSS.getElement(40).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {63.0, 31.5},
                      oldSS.getElement(41).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {0.0, 0.0, 0.0},
                      oldSS.getElement(42).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {58.8, 63.0, 37.8, 8.4},
                      oldSS.getElement(43).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {78.75, 183.75, 131.25, 52.5, 8.75},
                      oldSS.getElement(44).getEnergy(), 1e-6);

    // introduce a new tariff and shift 40% of the population to it
    TariffSpecification ts1 =
            new TariffSpecification(bob, PowerType.ELECTRIC_VEHICLE)
            .addRate(new Rate().withValue(-0.09))
            .withSignupPayment(-2.0);
    Tariff tariff1 = new Tariff(ts1);
    initTariff(tariff1);
    // move 40% of the population to the new tariff
    TariffSubscription newSub =
            subscribeTo(customer, tariff1, (int) Math.round(customer.getPopulation() * 0.4));
    StorageState newSS = new StorageState(newSub, chargerCapacity, maxHorizon);
    newSS.moveSubscribers(40, newSub.getCustomersCommitted(), oldSS);
    // energy numbers in the old ss should be 60% of original
    assertArrayEquals(new double[] {23.1},
                      oldSS.getElement(40).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {37.8, 18.9},
                      oldSS.getElement(41).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {0.0, 0.0, 0.0},
                      oldSS.getElement(42).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {35.28, 37.8, 22.68, 5.04},
                      oldSS.getElement(43).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {47.25, 110.25, 78.75, 31.5, 5.25},
                      oldSS.getElement(44).getEnergy(), 1e-6);
    // and energy numbers in the new ss should be 40% of the original
    assertArrayEquals(new double[] {15.4},
                      newSS.getElement(40).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {25.2, 12.6},
                      newSS.getElement(41).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {0.0, 0.0, 0.0},
                      newSS.getElement(42).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {23.52, 25.2, 15.12, 3.36},
                      newSS.getElement(43).getEnergy(), 1e-6);
    assertArrayEquals(new double[] {31.5, 73.5, 52.5, 21.0, 3.5},
                      newSS.getElement(44).getEnergy(), 1e-6);
  }

  @Test
  void testSubscriptionShift ()
  {
    
  }

  //Bogus XStream version
  @Test
  void testRestore ()
  {
    String data = String.join(", ", "[SE 301 [22.0] [0.0]",
            "SE 302 [21.0, 10.0] [105.2, 32.5]",
            "SE 303 [12.0, 0.0, 16.5] [150.6, 0.0, 7.2]",
            "SE 304 [0.0, 8.4, 12.6, 3.2] [0.0, 89.6, 62.3, 7.1]]");

    StorageState newState = StorageState.restoreState(6.0,  mySub, maxHorizon, data);
    assertNotNull(newState);
    assertEquals(4, newState.getHorizon(301));

    StorageElement se301 = newState.getElement(301);
    assertNotNull(se301);
    assertEquals(1, se301.getPopulation().length);
  }

  @Test
  void testClear ()
  {
    
  }

  @Test
  void testClean ()
  {
    
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
