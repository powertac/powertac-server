package org.powertac.distributionutility;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.CustomerInfo.CustomerClass;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TimeService;
import org.powertac.common.repo.BootstrapDataRepo;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
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
            Competition.currentCompetition().getSimulationBaseTime().plus(TimeService.DAY);
    start = new DateTime(start, DateTimeZone.UTC);
    timeService.setCurrentTime(base);
    timeslotRepo.makeTimeslot(base);
    //timeslotRepo.currentTimeslot().disable();// enabled: false);
    reset(accountingService);

    // Create 3 test brokers
    Broker broker1 = new Broker("testBroker1");
    brokerRepo.add(broker1);
    brokerList.add(broker1);

    Broker broker2 = new Broker("testBroker2");
    brokerRepo.add(broker2);
    brokerList.add(broker2);

    Broker broker3 = new Broker("testBroker3");
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
    brokerList.clear();
    tariffSpecList.clear();
    tariffList.clear();
  }

  private void initializeService ()
  {
    Configuration mapConfig = new MapConfiguration(cfgMap);
    config.setConfiguration(mapConfig);
    distributionUtilityService.initialize(comp, Arrays.asList("BalancingMarket"));
  }

  @Test
  public void testTransportInit ()
  {
    cfgMap.put("distributionutility.distributionUtilityService.distributionFeeMin", "-0.01");
    cfgMap.put("distributionutility.distributionUtilityService.distributionFeeMax", "-0.12");
    initializeService();
    assertEquals("correct min dist fee", -0.01,
                 distributionUtilityService.getDistributionFeeMin(), 1e-6);
    assertEquals("correct max dist fee", -0.12,
                 distributionUtilityService.getDistributionFeeMax(), 1e-6);
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
  }

  @Test
  public void testBootInit ()
  {
    
    cfgMap.put("distributionutility.distributionUtilityService.useCapacityFee", "true");
    initializeService();
    
  }
}
