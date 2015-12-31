package org.powertac.distributionutility;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.CustomerInfo.CustomerClass;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction.Type;
import org.powertac.common.TimeService;
import org.powertac.common.repo.BootstrapDataRepo;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
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
  private OrderbookRepo orderbookRepo;
  
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
  private DateTime start;

  @Before
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
    start = new DateTime(start, DateTimeZone.UTC);
    timeService.setCurrentTime(base.plus(TimeService.HOUR * 4));
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
    }).when(serverPropertiesService).configureMe(anyObject());
  }

  @After
  public void tearDown ()
  {
    // clear all repos
    timeslotRepo.recycle();
    brokerRepo.recycle();
    tariffRepo.recycle();
    orderbookRepo.recycle();

    // clear member lists
    tariffSpecList.clear();
    tariffList.clear();
    cfgMap.clear();
    reset(bootRepo);
  }

  private void initializeService ()
  {
    Configuration mapConfig = new MapConfiguration(cfgMap);
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
    assertTrue("using capacity fee",
                 distributionUtilityService.usingCapacityFee());
    assertEquals("correct assessmentInterval", 24,
                 distributionUtilityService.getAssessmentInterval(), 24);
    assertEquals("correct std coefficient", 1.1,
                 distributionUtilityService.getStdCoefficient(), 1e-6);
    assertEquals("correct fee-per-point", 1001.0,
                 distributionUtilityService.getFeePerPoint(), 1e-6);
    assertEquals("correct initial timeslot", 4, timeslotRepo.currentSerialNumber());
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
    }).when(bootRepo).getData(anyObject());
  }

  @Test
  public void testBootInit ()
  {
    setBootRecord();
    cfgMap.put("distributionutility.distributionUtilityService.useCapacityFee", "true");
    initializeService();
    assertEquals("correct mean", 9.5, distributionUtilityService.getRunningMean(), 1e-6);
    assertEquals("correct sigma", 3.8729833, distributionUtilityService.getRunningSigma(), 1e-5);
    assertEquals("correct count", 4, distributionUtilityService.getRunningCount());
  }

  int accCalls = 0;
  @Test
  public void testCapAssessment ()
  {
    cfgMap.put("distributionutility.distributionUtilityService.useCapacityFee", "true");
    cfgMap.put("distributionutility.distributionUtilityService.useTransportFee", "false");
    cfgMap.put("distributionutility.distributionUtilityService.assessmentInterval", "2");
    cfgMap.put("distributionutility.distributionUtilityService.stdCoefficient", "1.1");
    cfgMap.put("distributionutility.distributionUtilityService.feePerPoint", "1001.0");
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
    when(accountingService.addCapacityTransaction(anyObject(), anyInt(),
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
    verify(accountingService, never()).addCapacityTransaction(anyObject(), anyInt(),
                                                              anyDouble(), anyDouble(),
                                                              anyDouble());
    bumpTime(TimeService.HOUR);

    // ts 5: customers use and produce energy, create peak, DU gets activated, no tx.
    broker1Map.put(Type.CONSUME, -7.0);
    broker1Map.put(Type.PRODUCE, 2.0); //+5
    broker2Map.put(Type.CONSUME, -5.0);
    broker2Map.put(Type.PRODUCE, 1.0); //+4
    broker3Map.put(Type.CONSUME, -7.5);
    broker3Map.put(Type.PRODUCE, 2.0); //+4.5
    distributionUtilityService.activate(timeService.getCurrentTime(), 4);
    verify(accountingService, never()).addCapacityTransaction(anyObject(), anyInt(),
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
    verify(accountingService, times(3)).addCapacityTransaction(anyObject(), anyInt(),
                                                              anyDouble(), anyDouble(),
                                                              anyDouble());
    bumpTime(TimeService.HOUR);
  }

  private void bumpTime (long incr)
  {
    timeService.setCurrentTime(timeService.getCurrentTime().plus(incr));
  }
}
