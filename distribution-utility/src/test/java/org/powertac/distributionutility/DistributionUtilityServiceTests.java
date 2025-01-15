package org.powertac.distributionutility;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.Broker;
import org.powertac.common.CapacityTransaction;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.CustomerInfo.CustomerClass;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction.Type;
import org.powertac.common.TimeService;
import org.powertac.common.repo.BootstrapDataRepo;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class
})
public class DistributionUtilityServiceTests
{

  @Autowired
  private TimeService timeService;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private BootstrapDataRepo bootRepo;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  
  @Autowired
  private Accounting accountingService;
  
  @Autowired
  private ServerConfiguration serverPropertiesService;

  @Autowired
  private DistributionUtilityService distributionUtilityService;

  private Competition comp;
  private Configurator config;
  private TreeMap<String, String> cfgMap;
  private List<Broker> brokerList = new ArrayList<Broker>();
  private List<TariffSpecification> tariffSpecList = new ArrayList<TariffSpecification>();
  private List<Tariff> tariffList = new ArrayList<Tariff>();
  private Broker broker1;
  private Broker broker2;
  private Broker broker3;
  private CustomerInfo cust1;
  private CustomerInfo cust2;
  private ZonedDateTime start;

  @BeforeEach
  public void setUp ()
  {
    // create a Competition, needed for initialization
    comp = Competition.newInstance("du-test");
    Competition.setCurrent(comp);

    // set up some customers
    cust1 =
        new CustomerInfo("Podunk", 10).withCustomerClass(CustomerClass.SMALL);
    cust2 =
        new CustomerInfo("Acme", 1).withCustomerClass(CustomerClass.LARGE);

    Instant base =
            Competition.currentCompetition().getSimulationBaseTime();
    if (null == start) {
      start = ZonedDateTime.now(ZoneOffset.UTC);
    }
    timeService.setCurrentTime(base.plusMillis(TimeService.HOUR * 4));
    timeslotRepo.makeTimeslot(base);
    //timeslotRepo.currentTimeslot().disable();// enabled: false);
    reset(accountingService);

    // Create 3 test brokers
    broker1 = new Broker("testBroker1");
    brokerRepo.add(broker1);
    brokerList.add(broker1);

    broker2 = new Broker("testBroker2");
    brokerRepo.add(broker2);
    brokerList.add(broker2);

    broker3 = new Broker("testBroker3");
    brokerRepo.add(broker3);
    brokerList.add(broker3);

    // Set up serverProperties mock
    cfgMap = new TreeMap<String, String>();
    config = new Configurator();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(serverPropertiesService).configureMe(any());
  }

  @AfterEach
  public void tearDown ()
  {
    // clear all repos
    timeslotRepo.recycle();
    brokerRepo.recycle();
    tariffRepo.recycle();

    // clear member lists
    tariffSpecList.clear();
    tariffList.clear();
    cfgMap.clear();
    reset(bootRepo);
  }

  private void initializeService ()
  {
    MapConfiguration mapConfig = new MapConfiguration(cfgMap);
    config.setConfiguration(mapConfig);
    distributionUtilityService.initialize(comp, Arrays.asList("BalancingMarket"));
  }

  @Test
  public void testCapacityInit ()
  {
    cfgMap.put("distributionutility.distributionUtilityService.useCapacityFee", "true");
    cfgMap.put("distributionutility.distributionUtilityService.assessmentInterval", "24");
    cfgMap.put("distributionutility.distributionUtilityService.stdCoefficient", "1.1");
    cfgMap.put("distributionutility.distributionUtilityService.feePerPoint", "1001.0");
    initializeService();
    assertTrue(distributionUtilityService.usingCapacityFee(), "using capacity fee");
    assertEquals(24, distributionUtilityService.getAssessmentInterval(), 24, "correct assessmentInterval");
    assertEquals(1.1, distributionUtilityService.getStdCoefficient(), 1e-6, "correct std coefficient");
    assertEquals(1001.0, distributionUtilityService.getFeePerPoint(), 1e-6, "correct fee-per-point");
    assertEquals(4, timeslotRepo.currentSerialNumber(), "correct initial timeslot");
  }

  private void setBootRecord ()
  {
    CustomerBootstrapData cbd1 =
            new CustomerBootstrapData(cust1, PowerType.CONSUMPTION,
                                      new double[] {-3.0,-4.0,-5.0,-6.0});
    CustomerBootstrapData cbd2 =
            new CustomerBootstrapData(cust2, PowerType.CONSUMPTION,
                                      new double[] {-2.0,-4.0,-6.0,-8.0});
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        List<CustomerBootstrapData> result =
                new ArrayList<CustomerBootstrapData>();
        result.add(cbd1);
        result.add(cbd2);
        return result;
      }
    }).when(bootRepo).getData(any());
  }

  @Test
  public void testBootInit ()
  {
    setBootRecord();
    cfgMap.put("distributionutility.distributionUtilityService.useCapacityFee", "true");
    initializeService();
    assertEquals(9.5 * 1.02, distributionUtilityService.getRunningMean(), 1e-6, "correct mean");
    assertEquals(3.95044, distributionUtilityService.getRunningSigma(), 1e-5, "correct sigma");
    assertEquals(4, distributionUtilityService.getRunningCount(), "correct count");
  }

  //int accCalls = 0;
  @Test
  public void testCapAssessment ()
  {
    cfgMap.put("distributionutility.distributionUtilityService.useCapacityFee", "true");
    cfgMap.put("distributionutility.distributionUtilityService.useTransportFee", "false");
    cfgMap.put("distributionutility.distributionUtilityService.useMeterFee", "false");
    cfgMap.put("distributionutility.distributionUtilityService.assessmentInterval", "2");
    cfgMap.put("distributionutility.distributionUtilityService.stdCoefficient", "1.1");
    cfgMap.put("distributionutility.distributionUtilityService.feePerPoint", "10.0");
    setBootRecord();
    initializeService();

    // set up Accounting responses
    Map<Broker, Map<Type, Double>> response =
            new HashMap<Broker, Map<Type, Double>> ();
    Map<Type, Double> broker1Map = new HashMap<Type, Double>();
    response.put(broker1, broker1Map);
    Map<Type, Double> broker2Map = new HashMap<Type, Double>();
    response.put(broker2, broker2Map);
    Map<Type, Double> broker3Map = new HashMap<Type, Double>();
    response.put(broker3, broker3Map);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        return response;
      }
    }).when(accountingService).getCurrentSupplyDemandByBroker();
    when(accountingService.addCapacityTransaction(any(), anyInt(),
                                                  anyDouble(), anyDouble(),
                                                  anyDouble())).thenReturn(null);

    // ts 4: customers use and produce energy, DU gets activated, no tx.
    broker1Map.put(Type.CONSUME, -5.0);
    broker1Map.put(Type.PRODUCE, 2.0); //+3
    broker2Map.put(Type.CONSUME, -5.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+4
    broker3Map.put(Type.CONSUME, -4.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+2
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    verify(accountingService, never()).addCapacityTransaction(any(), anyInt(),
                                                              anyDouble(), anyDouble(),
                                                              anyDouble());
    bumpTime(TimeService.HOUR);

    // ts 5: customers use and produce energy, create peak, DU gets activated, no tx.
    broker1Map.put(Type.CONSUME, -7.0);
    broker1Map.put(Type.PRODUCE, 2.0); //+5
    broker2Map.put(Type.CONSUME, -5.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+4
    broker3Map.put(Type.CONSUME, -7.5);
    broker3Map.put(Type.PRODUCE, 2.0); //+5.5
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    verify(accountingService, never()).addCapacityTransaction(any(), anyInt(),
                                                              anyDouble(), anyDouble(),
                                                              anyDouble());
    bumpTime(TimeService.HOUR);

    // ts 6: customers use and produce energy, DU gets activated, 3 tx for ts 5.
    broker1Map.put(Type.CONSUME, -5.0);
    broker1Map.put(Type.PRODUCE, 2.0); //+3
    broker2Map.put(Type.CONSUME, -5.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+4
    broker3Map.put(Type.CONSUME, -4.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+2
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    verify(accountingService, times(3)).addCapacityTransaction(any(), anyInt(),
                                                              anyDouble(), anyDouble(),
                                                              anyDouble());
    bumpTime(TimeService.HOUR);
  }

  @Test
  public void testCapAssessment2 ()
  {
    cfgMap.put("distributionutility.distributionUtilityService.useCapacityFee", "true");
    cfgMap.put("distributionutility.distributionUtilityService.useTransportFee", "false");
    cfgMap.put("distributionutility.distributionUtilityService.useMeterFee", "false");
    cfgMap.put("distributionutility.distributionUtilityService.assessmentInterval", "2");
    cfgMap.put("distributionutility.distributionUtilityService.stdCoefficient", "1.1");
    cfgMap.put("distributionutility.distributionUtilityService.feePerPoint", "10.0");
    cfgMap.put("distributionutility.distributionUtilityService.bootstrapConsumptionMultiplier", "1.0");
    setBootRecord();
    initializeService();

    // set up Accounting responses
    Map<Broker, Map<Type, Double>> response =
            new HashMap<Broker, Map<Type, Double>> ();
    Map<Type, Double> broker1Map = new HashMap<Type, Double>();
    response.put(broker1, broker1Map);
    Map<Type, Double> broker2Map = new HashMap<Type, Double>();
    response.put(broker2, broker2Map);
    Map<Type, Double> broker3Map = new HashMap<Type, Double>();
    response.put(broker3, broker3Map);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        return response;
      }
    }).when(accountingService).getCurrentSupplyDemandByBroker();

    Map<Broker, List<CapacityTransaction>> ctxMap;
    ctxMap = new HashMap<> ();
    ctxMap.put(broker1, new ArrayList<CapacityTransaction>());
    ctxMap.put(broker2, new ArrayList<CapacityTransaction>());
    ctxMap.put(broker3, new ArrayList<CapacityTransaction>());
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer (InvocationOnMock invocation)
      {
        // broker, ts, threshold, kwh, fee
        Object[] args = invocation.getArguments();
        Broker broker = (Broker)args[0];
        CapacityTransaction ctx =
            new CapacityTransaction(broker, 0, (Integer)args[1],
                                    (Double)args[2], (Double)args[3],
                                    (Double)args[4]);
        ctxMap.get(broker).add(ctx);
        return ctx;
      }
    }).when(accountingService).addCapacityTransaction(any(), anyInt(),
                                                  anyDouble(), anyDouble(),
                                                  anyDouble());

    // ts 4: customers use and produce energy, DU gets activated, no tx.
    broker1Map.put(Type.CONSUME, -5.0);
    broker1Map.put(Type.PRODUCE, 2.0); //+3
    broker2Map.put(Type.CONSUME, -5.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+4
    broker3Map.put(Type.CONSUME, -4.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+2
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(0, ctxMap.get(broker1).size(), "no ctx broker1");
    assertEquals(0, ctxMap.get(broker2).size(), "no ctx broker2");
    assertEquals(0, ctxMap.get(broker3).size(), "no ctx broker3");
    bumpTime(TimeService.HOUR);

    // ts 5: customers use and produce energy, create peak, DU gets activated, no tx.
    broker1Map.put(Type.CONSUME, -7.0);
    broker1Map.put(Type.PRODUCE, 2.0); //+5
    broker2Map.put(Type.CONSUME, -5.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+4
    broker3Map.put(Type.CONSUME, -7.5);
    broker3Map.put(Type.PRODUCE, 2.0); //+5.5
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(0, ctxMap.get(broker1).size(), "no ctx broker1");
    assertEquals(0, ctxMap.get(broker2).size(), "no ctx broker2");
    assertEquals(0, ctxMap.get(broker3).size(), "no ctx broker3");
    bumpTime(TimeService.HOUR);

    // ts 6: customers use and produce energy, DU gets activated, 3 tx for ts 5.
    //broker1Map.put(Type.CONSUME, -5.0);
    //broker1Map.put(Type.PRODUCE, 2.0); //+3
    response.put(broker1, null);
    broker2Map.put(Type.CONSUME, -5.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+4
    broker3Map.put(Type.CONSUME, -4.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+2
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(1, ctxMap.get(broker1).size(), "one ctx broker1");
    assertEquals(1, ctxMap.get(broker2).size(), "one ctx broker2");
    assertEquals(1, ctxMap.get(broker3).size(), "one ctx broker3");
    CapacityTransaction ctx = ctxMap.get(broker1).get(0);
    assertEquals(14.272903, ctx.getThreshold(), 1e-6, "threshold 1");
    assertEquals(0.078309248, ctx.getKWh(), 1e-6, "kwh 1");
    ctxMap.put(broker1, new ArrayList<CapacityTransaction>());
    ctxMap.put(broker2, new ArrayList<CapacityTransaction>());
    ctxMap.put(broker3, new ArrayList<CapacityTransaction>());
    bumpTime(TimeService.HOUR);

    // ts 7: customers use and produce energy, create peak, DU gets activated, no tx.
    //broker1Map.put(Type.CONSUME, -7.0);
    //broker1Map.put(Type.PRODUCE, 2.0); //+5
    response.put(broker1, null);
    broker2Map.put(Type.CONSUME, -7.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+6
    broker3Map.put(Type.CONSUME, -10.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+8
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(0, ctxMap.get(broker1).size(), "no ctx broker1");
    assertEquals(0, ctxMap.get(broker2).size(), "no ctx broker2");
    assertEquals(0, ctxMap.get(broker3).size(), "no ctx broker3");
    bumpTime(TimeService.HOUR);

    // ts 8: customers use and produce energy, DU gets activated, 2 tx for ts 5.
    //broker1Map.put(Type.CONSUME, -5.0);
    //broker1Map.put(Type.PRODUCE, 2.0); //+3
    response.put(broker1, null);
    broker2Map.put(Type.CONSUME, -7.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+6
    broker3Map.put(Type.CONSUME, -6.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+4
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(1, ctxMap.get(broker1).size(), "one ctx broker1");
    assertEquals(1, ctxMap.get(broker2).size(), "one ctx broker2");
    assertEquals(1, ctxMap.get(broker3).size(), "one ctx broker3");
    ctx = ctxMap.get(broker1).get(0);
    //assertEquals(14.272903, ctx.getThreshold(), 1e-6, "threshold 1");
    assertEquals(0.0, ctx.getKWh(), 1e-6, "kwh 1");
    assertEquals(1, ctxMap.get(broker1).size(), "one ctx broker1");
    assertEquals(1, ctxMap.get(broker2).size(), "one ctx broker2");
    assertEquals(1, ctxMap.get(broker3).size(), "one ctx broker3");
    //System.out.println("ctx ts=" + ctx.getPeakTimeslot() +
    //                   ", threshold=" + ctx.getThreshold() +
    //                   ", kwh=" + ctx.getKWh() +
    //                   ", fee=" + ctx.getCharge());
    bumpTime(TimeService.HOUR);
  }

  @Test
  public void testCapAssessment3 ()
  {
    cfgMap.put("distributionutility.distributionUtilityService.useCapacityFee", "true");
    cfgMap.put("distributionutility.distributionUtilityService.useTransportFee", "false");
    cfgMap.put("distributionutility.distributionUtilityService.useMeterFee", "false");
    cfgMap.put("distributionutility.distributionUtilityService.assessmentInterval", "5");
    cfgMap.put("distributionutility.distributionUtilityService.assessmentCount", "2");
    cfgMap.put("distributionutility.distributionUtilityService.stdCoefficient", "1.1");
    cfgMap.put("distributionutility.distributionUtilityService.feePerPoint", "10.0");
    cfgMap.put("distributionutility.distributionUtilityService.bootstrapConsumptionMultiplier", "1.0");
    setBootRecord();
    initializeService();

    // set up Accounting responses
    Map<Broker, Map<Type, Double>> response =
            new HashMap<Broker, Map<Type, Double>> ();
    Map<Type, Double> broker1Map = new HashMap<Type, Double>();
    response.put(broker1, broker1Map);
    Map<Type, Double> broker2Map = new HashMap<Type, Double>();
    response.put(broker2, broker2Map);
    Map<Type, Double> broker3Map = new HashMap<Type, Double>();
    response.put(broker3, broker3Map);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        return response;
      }
    }).when(accountingService).getCurrentSupplyDemandByBroker();

    Map<Broker, List<CapacityTransaction>> ctxMap;
    ctxMap = new HashMap<> ();
    ctxMap.put(broker1, new ArrayList<CapacityTransaction>());
    ctxMap.put(broker2, new ArrayList<CapacityTransaction>());
    ctxMap.put(broker3, new ArrayList<CapacityTransaction>());
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer (InvocationOnMock invocation)
      {
        // broker, ts, threshold, kwh, fee
        Object[] args = invocation.getArguments();
        Broker broker = (Broker)args[0];
        CapacityTransaction ctx =
            new CapacityTransaction(broker, 0, (Integer)args[1],
                                    (Double)args[2], (Double)args[3],
                                    (Double)args[4]);
        ctxMap.get(broker).add(ctx);
        return ctx;
      }
    }).when(accountingService).addCapacityTransaction(any(), anyInt(),
                                                  anyDouble(), anyDouble(),
                                                  anyDouble());

    // ts 4: customers use and produce energy, DU gets activated, no tx.
    broker1Map.put(Type.CONSUME, -7.0);
    broker1Map.put(Type.PRODUCE, 2.0); //+5.0
    broker2Map.put(Type.CONSUME, -11.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+10
    broker3Map.put(Type.CONSUME, -9.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+2 -> 22
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(0, ctxMap.get(broker1).size(), "no ctx broker1");
    assertEquals(0, ctxMap.get(broker2).size(), "no ctx broker2");
    assertEquals(0, ctxMap.get(broker3).size(), "no ctx broker3");
    bumpTime(TimeService.HOUR);

    // ts 5: customers use and produce energy, create peak, DU gets activated, no tx.
    broker1Map.put(Type.CONSUME, -6.9);
    broker1Map.put(Type.PRODUCE, 2.0); //+4.9
    broker2Map.put(Type.CONSUME, -5.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+4
    broker3Map.put(Type.CONSUME, -15.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+13.0 -> 21.9
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(0, ctxMap.get(broker1).size(), "no ctx broker1");
    assertEquals(0, ctxMap.get(broker2).size(), "no ctx broker2");
    assertEquals(0, ctxMap.get(broker3).size(), "no ctx broker3");
    bumpTime(TimeService.HOUR);

    // ts 6: customers use and produce energy, DU gets activated, no tx.
    //broker1Map.put(Type.CONSUME, -5.0);
    //broker1Map.put(Type.PRODUCE, 2.0); //+3
    response.put(broker1, null);
    broker2Map.put(Type.CONSUME, -0.0);
    broker2Map.put(Type.PRODUCE, 2.0); //-2
    broker3Map.put(Type.CONSUME, -4.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+2 -> 1.0
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(0, ctxMap.get(broker1).size(), "no ctx broker1");
    assertEquals(0, ctxMap.get(broker2).size(), "no ctx broker2");
    assertEquals(0, ctxMap.get(broker3).size(), "no ctx broker3");
    bumpTime(TimeService.HOUR);

    // ts 7: customers use and produce energy, create peak, DU gets activated, no tx.
    response.put(broker1, broker1Map);
    broker1Map.put(Type.CONSUME, -9.01);
    broker1Map.put(Type.PRODUCE, 2.0); //+7.1
    broker2Map.put(Type.CONSUME, -7.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+6
    broker3Map.put(Type.CONSUME, -11.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+9 -> 22.1
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(0, ctxMap.get(broker1).size(), "no ctx broker1");
    assertEquals(0, ctxMap.get(broker2).size(), "no ctx broker2");
    assertEquals(0, ctxMap.get(broker3).size(), "no ctx broker3");
    bumpTime(TimeService.HOUR);

    // ts 8: customers use and produce energy, create peak, DU gets activated, no tx.
    response.put(broker1, broker1Map);
    broker1Map.put(Type.CONSUME, -2.0);
    broker1Map.put(Type.PRODUCE, 1.9); //+.1
    broker2Map.put(Type.CONSUME, -2.0);
    broker2Map.put(Type.PRODUCE, 2.0); //-0
    broker3Map.put(Type.CONSUME, -3.0);
    broker3Map.put(Type.PRODUCE, 3.0); //+0 -> .1
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(0, ctxMap.get(broker1).size(), "no ctx broker1");
    assertEquals(0, ctxMap.get(broker2).size(), "no ctx broker2");
    assertEquals(0, ctxMap.get(broker3).size(), "no ctx broker3");
    bumpTime(TimeService.HOUR);

    // ts 9: customers use and produce energy, DU gets activated, 2 tx for ts 5.
    broker1Map.put(Type.CONSUME, -5.0);
    broker1Map.put(Type.PRODUCE, 2.0); //+3
    broker2Map.put(Type.CONSUME, -7.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+6
    broker3Map.put(Type.CONSUME, -6.0);
    broker3Map.put(Type.PRODUCE, 2.0); //+4 -> 13
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(2, ctxMap.get(broker1).size(), "one ctx broker1");
    assertEquals(2, ctxMap.get(broker2).size(), "one ctx broker2");
    assertEquals(2, ctxMap.get(broker3).size(), "one ctx broker3");
    CapacityTransaction ctx = ctxMap.get(broker1).get(0);
    assertEquals(21.4876, ctx.getThreshold(), 1e-4, "threshold 1");
    assertEquals(7, ctx.getPeakTimeslot(), "ts 1a");
    assertEquals(0.16639, ctx.getKWh(), 1e-5, "kwh 1a");
    assertEquals(1.66391, ctx.getCharge(), 1e-5, "fee 1a");
    //System.out.println("ctx ts=" + ctx.getPeakTimeslot() +
    //                   ", threshold=" + ctx.getThreshold() +
    //                   ", kwh=" + ctx.getKWh() +
    //                   ", fee=" + ctx.getCharge());
    ctx = ctxMap.get(broker1).get(1);
    assertEquals(4, ctx.getPeakTimeslot(), "ts 1b");
    assertEquals(0.11646, ctx.getKWh(), 1e-5, "kwh 1b");
    assertEquals(1.16462, ctx.getCharge(), 1e-5, "fee 1b");
    //System.out.println("ctx ts=" + ctx.getPeakTimeslot() +
    //                   ", threshold=" + ctx.getThreshold() +
    //                   ", kwh=" + ctx.getKWh() +
    //                   ", fee=" + ctx.getCharge());
    ctx = ctxMap.get(broker2).get(0);
    assertEquals(7, ctx.getPeakTimeslot(), "ts 2a");
    assertEquals(0.14242, ctx.getKWh(), 1e-5, "kwh 2a");
    assertEquals(1.42418, ctx.getCharge(), 1e-5, "fee 2a");
    //System.out.println("ctx ts=" + ctx.getPeakTimeslot() +
    //                   ", threshold=" + ctx.getThreshold() +
    //                   ", kwh=" + ctx.getKWh() +
    //                   ", fee=" + ctx.getCharge());
    ctx = ctxMap.get(broker2).get(1);
    assertEquals(4, ctx.getPeakTimeslot(), "ts 2b");
    assertEquals(0.23292, ctx.getKWh(), 1e-5, "kwh 2b");
    assertEquals(2.32925, ctx.getCharge(), 1e-5, "fee 2b");
    //System.out.println("ctx ts=" + ctx.getPeakTimeslot() +
    //                   ", threshold=" + ctx.getThreshold() +
    //                   ", kwh=" + ctx.getKWh() +
    //                   ", fee=" + ctx.getCharge());
    ctx = ctxMap.get(broker3).get(0);
    assertEquals(7, ctx.getPeakTimeslot(), "ts 3a");
    assertEquals(0.21363, ctx.getKWh(), 1e-5, "kwh 3a");
    assertEquals(2.13626, ctx.getCharge(), 1e-5, "fee 3a");
    //System.out.println("ctx ts=" + ctx.getPeakTimeslot() +
    //                   ", threshold=" + ctx.getThreshold() +
    //                   ", kwh=" + ctx.getKWh() +
    //                   ", fee=" + ctx.getCharge());
    ctx = ctxMap.get(broker3).get(1);
    assertEquals(4, ctx.getPeakTimeslot(), "ts 2b");
    assertEquals(0.16305, ctx.getKWh(), 1e-5, "kwh 2b");
    assertEquals(1.63047, ctx.getCharge(), 1e-5, "fee 2b");
    //System.out.println("ctx ts=" + ctx.getPeakTimeslot() +
    //                   ", threshold=" + ctx.getThreshold() +
    //                   ", kwh=" + ctx.getKWh() +
    //                   ", fee=" + ctx.getCharge());
    bumpTime(TimeService.HOUR);
  }

  private void bumpTime (long incr)
  {
    timeService.setCurrentTime(timeService.getCurrentTime().plusMillis(incr));
  }

  @Test
  public void testMeterFee ()
  {
    cfgMap.put("distributionutility.distributionUtilityService.useCapacityFee", "false");
    cfgMap.put("distributionutility.distributionUtilityService.useTransportFee", "false");
    cfgMap.put("distributionutility.distributionUtilityService.useMeterFee", "true");
    cfgMap.put("distributionutility.distributionUtilityService.mSmall", "0.12");
    cfgMap.put("distributionutility.distributionUtilityService.mLarge", "0.18");
    setBootRecord();
    initializeService();

    // broker 1 - two tariffs, four subscriptions, 30 small meters, 8 large.
    TariffSpecification spec1a =
            new TariffSpecification(broker1, PowerType.CONSUMPTION)
            .addRate(new Rate().withValue(-0.11));
    Tariff tariff1a = new Tariff(spec1a);
    tariff1a.init();
    TariffSubscription sub1a1 = new TariffSubscription(cust1, tariff1a);
    sub1a1.setCustomersCommitted(10);
    TariffSubscription sub1a2 = new TariffSubscription(cust2, tariff1a);
    sub1a2.setCustomersCommitted(5);
    TariffSpecification spec1b =
            new TariffSpecification(broker1, PowerType.CONSUMPTION)
            .addRate(new Rate().withValue(-0.11));
    Tariff tariff1b = new Tariff(spec1b);
    tariff1b.init();
    TariffSubscription sub1b1 = new TariffSubscription(cust1, tariff1b);
    sub1b1.setCustomersCommitted(20);
    TariffSubscription sub1b2 = new TariffSubscription(cust2, tariff1b);
    sub1b2.setCustomersCommitted(3);
    List<TariffSubscription> subs1 =
            Arrays.asList(sub1a1, sub1a2, sub1b1, sub1b2);

    // broker 2, one tariff, 2 subs, 18 small, 7 large
    TariffSpecification spec2a =
            new TariffSpecification(broker2, PowerType.CONSUMPTION)
            .addRate(new Rate().withValue(-0.11));
    Tariff tariff2a = new Tariff(spec2a);
    tariff2a.init();
    TariffSubscription sub2a1 = new TariffSubscription(cust1, tariff2a);
    sub2a1.setCustomersCommitted(18);
    TariffSubscription sub2a2 = new TariffSubscription(cust2, tariff2a);
    sub2a2.setCustomersCommitted(7);
    List<TariffSubscription> subs2 = Arrays.asList(sub2a1, sub2a2);

    // broker 3, one tariff, 15 large customers
    TariffSpecification spec3a =
            new TariffSpecification(broker2, PowerType.CONSUMPTION)
            .addRate(new Rate().withValue(-0.11));
    Tariff tariff3a = new Tariff(spec3a);
    tariff3a.init();
    TariffSubscription sub3a2 = new TariffSubscription(cust2, tariff3a);
    sub3a2.setCustomersCommitted(15);
    List<TariffSubscription> subs3 = Arrays.asList(sub3a2);

    // return the subscriptions when asked
    when(tariffSubscriptionRepo.findActiveSubscriptionsForBroker(broker1))
    .thenReturn(subs1);
    when(tariffSubscriptionRepo.findActiveSubscriptionsForBroker(broker2))
    .thenReturn(subs2);
    when(tariffSubscriptionRepo.findActiveSubscriptionsForBroker(broker3))
    .thenReturn(subs3);

    // capture transactions
    Map<Broker, Object[]> answers =
            new HashMap<Broker, Object[]>();
    when(accountingService.addDistributionTransaction(any(Broker.class),
                                                      anyInt(), anyInt(),
                                                      anyDouble(),
                                                      anyDouble()))
        .thenAnswer(new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) {
            Object[] args = invocation.getArguments();
            Broker b = (Broker)args[0];
            answers.put(b, args);
            return null;
          }
        });

    // Activate, check calls
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    assertEquals(3, answers.size(), "one entry per broker");
    Object[] answer = answers.get(broker1);
    assertEquals(30, answer[1], "correct small");
    assertEquals(8, answer[2], "correct large");
    assertEquals(0.0, (double)answer[3], 1e-6, "no kwh");
    assertEquals(30*.12 + 8*.18, (double)answer[4], 1e-6, "correct fee");
  }
}
