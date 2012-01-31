package org.powertac.distributionutility;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
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
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.distributionutility.DistributionUtilityService.ChargeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
public class DistributionUtilityServiceTests
{

  @Autowired
  private TimeService timeService;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private OrderbookRepo orderbookRepo;
  
  @Autowired
  private Accounting accountingService;
  
  @Autowired
  private ServerConfiguration serverPropertiesService;

  @Autowired
  private DistributionUtilityService distributionUtilityService;

  private TariffRepo tariffRepo;
  private Competition comp;
  private Configurator config;
  private List<Broker> brokerList = new ArrayList<Broker>();
  private List<TariffSpecification> tariffSpecList = new ArrayList<TariffSpecification>();
  private List<Tariff> tariffList = new ArrayList<Tariff>();
  private DateTime start;

  @Before
  public void setUp ()
  {
    // create a Competition, needed for initialization
    comp = Competition.newInstance("du-test");

    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC);
    timeService.setCurrentTime(start.toInstant());
    timeslotRepo.makeTimeslot(start.toInstant());
    timeslotRepo.currentTimeslot().disable();// enabled: false);
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
    config = new Configurator();
    doAnswer(new Answer() {
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
    tariffRepo = (TariffRepo) SpringApplicationContext.getBean("tariffRepo");
    tariffRepo.recycle();
    orderbookRepo.recycle();

    // clear member lists
    brokerList.clear();
    tariffSpecList.clear();
    tariffList.clear();
  }

  private void initializeService ()
  {
    distributionUtilityService.setDefaults();
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("distributionutility.distributionUtilityService.balancingCostMin", "-0.06");
    map.put("distributionutility.distributionUtilityService.balancingCostMax", "-0.06");
    Configuration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    distributionUtilityService.initialize(comp, new ArrayList<String>());
    assertEquals("correct setting", -0.06,
                 distributionUtilityService.getBalancingCostMin(), 1e-6);
    assertEquals("correct setting", -0.06,
                 distributionUtilityService.getBalancingCostMax(), 1e-6);
    assertEquals("correct setting", -0.06,
                 distributionUtilityService.getBalancingCost(), 1e-6);
  }

  @Test
  public void testGetMarketBalance ()
  {
    initializeService();
    when(accountingService.getCurrentMarketPosition((Broker) anyObject())).thenReturn(1.0);
    when(accountingService.getCurrentNetLoad((Broker) anyObject())).thenReturn(-500.0);    
    assertEquals("correct balance",
                 500.0,
                 distributionUtilityService.getMarketBalance(brokerList.get(0)),
                 1e-6);
    when(accountingService.getCurrentNetLoad((Broker) anyObject())).thenReturn(-1000.0);    
    assertEquals("correct balance",
                 0.0,
                 distributionUtilityService.getMarketBalance(brokerList.get(0)),
                 1e-6);
    when(accountingService.getCurrentNetLoad((Broker) anyObject())).thenReturn(-1500.0);    
    assertEquals("correct balance",
                 -500.0,
                 distributionUtilityService.getMarketBalance(brokerList.get(0)),
                 1e-6);
  }

  @Test
  public void testNegImbalancedMarket ()
  {
    initializeService();
    when(accountingService.getCurrentMarketPosition((Broker) anyObject())).thenReturn(0.0);
    when(accountingService.getCurrentNetLoad((Broker) anyObject())).thenReturn(-50.0);    
    double marketBalance = -150.0; // Compute market balance
    List<ChargeInfo> theChargeInfoList =
        distributionUtilityService.balanceTimeslot(timeslotRepo.currentTimeslot(),
                                                   brokerList);

    assertEquals("correct number of balance tx", 3, theChargeInfoList.size());
    for (ChargeInfo ci : theChargeInfoList) {
      marketBalance += ci.itsNetLoadKWh;
    }
    assertEquals("correct balancing transactions", 0.0, marketBalance, 1e-6);
  }

  @Test
  public void TestPosImbalancedMarket ()
  {
    initializeService();
    when(accountingService.getCurrentMarketPosition((Broker) anyObject())).thenReturn(0.0);
    when(accountingService.getCurrentNetLoad((Broker) anyObject())).thenReturn(50.0);    
    double marketBalance = 150.0; // Compute market balance

    List<ChargeInfo> theChargeInfoList = distributionUtilityService.balanceTimeslot(timeslotRepo.currentTimeslot(),
                                                                                    brokerList);

    assertEquals("correct number of balance tx", 3, theChargeInfoList.size());
    for (ChargeInfo ci : theChargeInfoList) {
      marketBalance += ci.itsNetLoadKWh;
    }
    assertEquals("correct balancing transactions", 0.0, marketBalance, 1e-6);
  }

  @Test
  public void testIndividualBrokerBalancing ()
  {
    initializeService();
    double balance = 0.0;

    when(accountingService.getCurrentMarketPosition((Broker) anyObject())).thenReturn(0.0);
    when(accountingService.getCurrentNetLoad(brokerList.get(0))).thenReturn(-19599990.0);    
    when(accountingService.getCurrentNetLoad(brokerList.get(1))).thenReturn(0.0);
    when(accountingService.getCurrentNetLoad(brokerList.get(2))).thenReturn(8791119.0);    

    // Compute market balance
    for (Broker b : brokerList) {
      balance += distributionUtilityService.getMarketBalance(b);
    }

    List<ChargeInfo> theChargeInfoList = distributionUtilityService.balanceTimeslot(timeslotRepo.currentTimeslot(),
                                                                                    brokerList);

    // ensure each broker was balanced correctly
    for (int i = 0; i < brokerList.size(); i++) {
      ChargeInfo ci = theChargeInfoList.get(i);

      if (ci.itsBrokerName != brokerList.get(i).getUsername()) {
        fail("theChargeInfoList does not match brokerList for index " + i);

      }
      if (i < brokerList.size()) {
        assertEquals("broker correctly balanced",
                     0.0,
                     (distributionUtilityService.getMarketBalance(brokerList.get(i)) + ci.itsNetLoadKWh),
                     1e-6);
        balance += ci.itsNetLoadKWh;
      }
    }
    assertEquals("market fully balanced", 0.0, balance, 1e-6);
  }

  @Test
  public void testScenario1BalancingCharges ()
  {
    initializeService();

    when(accountingService.getCurrentMarketPosition((Broker) anyObject())).thenReturn(0.0);
    when(accountingService.getCurrentNetLoad(brokerList.get(0))).thenReturn(200.0);    
    when(accountingService.getCurrentNetLoad(brokerList.get(1))).thenReturn(-400.0);
    when(accountingService.getCurrentNetLoad(brokerList.get(2))).thenReturn(0.0);    

    // List solution =
    // distributionUtilityService.computeNonControllableBalancingCharges(brokerList)
    List<ChargeInfo> theChargeInfoList =
        distributionUtilityService.balanceTimeslot(timeslotRepo.currentTimeslot(),
                                                   brokerList);

    // Correct solution list is [-4, 14, 2] (but negated)
    ChargeInfo ci = theChargeInfoList.get(0); // BalancingTransaction.findByBroker(brokerList.get(0));
    assertNotNull("non-null btx, broker 1", ci);
    assertEquals("correct balancing charge broker1", 4.0, ci.itsBalanceCharge,
                 1e-6);
    ci = theChargeInfoList.get(1); // BalancingTransaction.findByBroker(brokerList.get(1));
    assertNotNull("non-null btx, broker 2", ci);
    assertEquals("correct balancing charge broker2", -14.0, ci.itsBalanceCharge,
                 1e-6);
    ci = theChargeInfoList.get(2); // BalancingTransaction.findByBroker(brokerList.get(2));
    assertNotNull("non-null btx, broker 3", ci);
    assertEquals("correct balancing charge broker3", -2.0, ci.itsBalanceCharge,
                 1e-6);
  }

  @Test
  public void testSpotPrice ()
  {
    initializeService();
    // add some new timeslots
    Timeslot ts0 = timeslotRepo.currentTimeslot();
    long start = timeService.getCurrentTime().getMillis();
    Timeslot ts1 = new Timeslot(1, new Instant(start - TimeService.HOUR * 3), null);
    ts1.disable(); // enabled: false
    Timeslot ts2 = new Timeslot(2, new Instant(start - TimeService.HOUR * 2), null);
    ts2.disable(); // enabled: false
    Timeslot ts3 = new Timeslot(3, new Instant(start - TimeService.HOUR), null);
    ts3.disable(); // enabled: false

    // add some orderbooks
    orderbookRepo.makeOrderbook(ts3, 33.0);
    orderbookRepo.makeOrderbook(ts3, 32.0);
    orderbookRepo.makeOrderbook(ts0, 20.2);
    // this should be the spot price
    orderbookRepo.makeOrderbook(ts0, 20.1);

    // make sure we can retrieve current spot price
    assertEquals("correct spot price", -0.0201,
                 distributionUtilityService.getSpotPrice(), 1e-6);
  }
}
