package org.powertac.balancemkt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.powertac.util.ListTools.*;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.util.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class
})
public class BalancingMarketServiceTests
{

  @Autowired
  private TimeService timeService;

  @Autowired
  private TimeslotRepo timeslotRepo;

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
  private BalancingMarketService balancingMarketService;

  private Competition comp;
  private Configurator config;
  private List<Broker> brokerList = new ArrayList<>();
  private List<TariffSpecification> tariffSpecList = new ArrayList<>();
  private List<Tariff> tariffList = new ArrayList<>();
  //private Instant start;

  @BeforeEach
  public void setUp ()
  {
    // create a Competition, needed for initialization
    comp = Competition.newInstance("du-test");
    Competition.setCurrent(comp);

    Instant base =
            Competition.currentCompetition().getSimulationBaseTime().plusMillis(TimeService.DAY);
    //start = ZonedDateTime.of(start, TimeService.UTC);
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
    orderbookRepo.recycle();

    // clear member lists
    brokerList.clear();
    tariffSpecList.clear();
    tariffList.clear();
  }

  private void initializeService ()
  {
    TreeMap<String, String> map = new TreeMap<>();
    map.put("balancemkt.balancingMarketService.balancingCostMin", "-0.06");
    map.put("balancemkt.balancingMarketService.balancingCostMax", "-0.06");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    balancingMarketService.initialize(comp, new ArrayList<>());
  }

  @Test
  public void testGetMarketBalance ()
  {
    initializeService();
    when(accountingService.getCurrentMarketPosition((Broker) any())).thenReturn(1.0);
    when(accountingService.getCurrentNetLoad((Broker) any())).thenReturn(-500.0);
    assertEquals(500.0, balancingMarketService.getMarketBalance(brokerList.get(0)), 1e-6, "correct balance");
    when(accountingService.getCurrentNetLoad((Broker) any())).thenReturn(-1000.0);
    assertEquals(0.0, balancingMarketService.getMarketBalance(brokerList.get(0)), 1e-6, "correct balance");
    when(accountingService.getCurrentNetLoad((Broker) any())).thenReturn(-1500.0);
    assertEquals(-500.0, balancingMarketService.getMarketBalance(brokerList.get(0)), 1e-6, "correct balance");
  }

  @Test
  public void testNegImbalancedMarket ()
  {
    initializeService();
    when(accountingService.getCurrentMarketPosition((Broker) any())).thenReturn(0.0);
    when(accountingService.getCurrentNetLoad((Broker) any())).thenReturn(-50.0);
    double marketBalance = -150.0; // Compute market balance
    //Timeslot current = timeslotRepo.currentTimeslot();
    BalancingMarketService.DoubleWrapper report =
        balancingMarketService.makeDoubleWrapper();
    //BalanceReport report = new BalanceReport(current.getSerialNumber());
    Map<Broker, ChargeInfo> theChargeInfoList =
        balancingMarketService.balanceTimeslot(brokerList, report);
    assertEquals(marketBalance, report.getValue(), 1e-6, "correct balance report");

    assertEquals(3, theChargeInfoList.size(), "correct number of balance tx");
    for (ChargeInfo ci : theChargeInfoList.values()) {
      marketBalance -= ci.getNetLoadKWh();
    }
    assertEquals(0.0, marketBalance, 1e-6, "correct balancing transactions");
  }

  @Test
  public void TestPosImbalancedMarket ()
  {
    initializeService();
    when(accountingService.getCurrentMarketPosition((Broker) any())).thenReturn(0.0);
    when(accountingService.getCurrentNetLoad((Broker) any())).thenReturn(50.0);
    double marketBalance = 150.0; // Compute market balance

    //Timeslot current = timeslotRepo.currentTimeslot();
    BalancingMarketService.DoubleWrapper report =
        balancingMarketService.makeDoubleWrapper();
    //BalanceReport report = new BalanceReport(current.getSerialNumber());
    Map<Broker, ChargeInfo> theChargeInfoList =
        balancingMarketService.balanceTimeslot(brokerList, report);
    assertEquals(marketBalance, report.getValue(), 1e-6, "correct balance report");

    assertEquals(3, theChargeInfoList.size(), "correct number of balance tx");
    for (ChargeInfo ci : theChargeInfoList.values()) {
      marketBalance -= ci.getNetLoadKWh();
    }
    assertEquals(0.0, marketBalance, 1e-6, "correct balancing transactions");
  }

  @Test
  public void testIndividualBrokerBalancing ()
  {
    initializeService();
    double balance = 0.0;

    when(accountingService.getCurrentMarketPosition((Broker) any())).thenReturn(0.0);
    when(accountingService.getCurrentNetLoad(brokerList.get(0))).
      thenReturn(-19599990.0);
    when(accountingService.getCurrentNetLoad(brokerList.get(1))).
      thenReturn(0.0);
    when(accountingService.getCurrentNetLoad(brokerList.get(2))).
      thenReturn(8791119.0);

    // Compute market balance
    for (Broker b : brokerList) {
      balance += balancingMarketService.getMarketBalance(b);
    }

    //Timeslot current = timeslotRepo.currentTimeslot();
    BalancingMarketService.DoubleWrapper report =
        balancingMarketService.makeDoubleWrapper();
    //BalanceReport report = new BalanceReport(current.getSerialNumber());
    Map<Broker, ChargeInfo> chargeInfos =
        balancingMarketService.balanceTimeslot(brokerList, report);

    // ensure each broker was balanced correctly
    for (Broker broker : brokerList) {
      ChargeInfo ci = chargeInfos.get(broker);
      assertNotNull(ci, "found ChargeInfo");
      assertEquals(0.0, (balancingMarketService.getMarketBalance(broker) - ci.getNetLoadKWh()), 1e-6, "broker correctly balanced");
      balance -= ci.getNetLoadKWh();
    }
    assertEquals(0.0, balance, 1e-6, "market fully balanced");
  }

  @Test
  public void testSpotPrice ()
  {
    initializeService();
    updatePrices();
    balancingMarketService.setRmPremium(1.1);
    balancingMarketService.setRmFee(0.04);

    // make sure we can retrieve current spot price
    assertEquals(0.0201, balancingMarketService.getSpotPrice(), 1e-6, "correct spot price");
    assertEquals(-0.0198 / 1.1 - 0.04, balancingMarketService.getPMinus(), 1e-6, "correct pMinus");
    assertEquals(0.0212 * 1.1 + 0.04, balancingMarketService.getPPlus(), 1e-6, "correct pPlus");
  }

  @SuppressWarnings("unused")
  private void updatePrices ()
  {
    // add some new timeslots
    Timeslot ts0 = timeslotRepo.currentTimeslot();
    long start = timeService.getCurrentTime().toEpochMilli();
    Timeslot ts1 = timeslotRepo.findByInstant(Instant.ofEpochMilli(start - TimeService.HOUR * 3));
    Timeslot ts2 = timeslotRepo.findByInstant(Instant.ofEpochMilli(start - TimeService.HOUR * 2));
    Timeslot ts3 = timeslotRepo.findByInstant(Instant.ofEpochMilli(start - TimeService.HOUR));

    // add some orderbooks
    orderbookRepo.makeOrderbook(ts3, 33.0);
    orderbookRepo.makeOrderbook(ts3, 32.0);
    orderbookRepo.makeOrderbook(ts0, 20.2);
    orderbookRepo.makeOrderbook(ts0, 21.2);
    orderbookRepo.makeOrderbook(ts0, 19.8);
    // this should be the spot price
    orderbookRepo.makeOrderbook(ts0, 20.1);
  }

  // make sure balancing orders are correctly allocated
  @Test
  public void testBalancingOrderAllocation ()
  {
    initializeService();
    final Broker b1 = brokerRepo.findByUsername("testBroker1");
    TariffSpecification b1ts1 =
            new TariffSpecification(b1, PowerType.INTERRUPTIBLE_CONSUMPTION);
    b1ts1.addRate(new Rate().withValue(0.12).withMaxCurtailment(0.3));
    tariffRepo.addSpecification(b1ts1);
    TariffSpecification b1ts2 =
            new TariffSpecification(b1, PowerType.INTERRUPTIBLE_CONSUMPTION);
    b1ts1.addRate(new Rate().withValue(0.10).withMaxCurtailment(0.5));
    tariffRepo.addSpecification(b1ts2);

    final Broker b2 = brokerRepo.findByUsername("testBroker2");
    TariffSpecification b2ts1 =
            new TariffSpecification(b2, PowerType.INTERRUPTIBLE_CONSUMPTION);
    b1ts1.addRate(new Rate().withValue(0.13).withMaxCurtailment(0.2));
    tariffRepo.addSpecification(b2ts1);

    BalancingOrder bo1t1 = new BalancingOrder(b1, b1ts1, 0.8, 0.2);
    tariffRepo.addBalancingOrder(bo1t1);
    BalancingOrder bo1t2 = new BalancingOrder(b1, b1ts2, 0.6, 0.15);
    tariffRepo.addBalancingOrder(bo1t2);
    BalancingOrder bo2t1 = new BalancingOrder(b2, b2ts1, 0.7, 0.1);
    tariffRepo.addBalancingOrder(bo2t1);

    assertEquals(3, tariffRepo.getBalancingOrders().size(), "correct number of bo");

    //Timeslot current = timeslotRepo.currentTimeslot();
    BalancingMarketService.DoubleWrapper report =
        balancingMarketService.makeDoubleWrapper();
    //BalanceReport report = new BalanceReport(current.getSerialNumber());
    Map<Broker, ChargeInfo> chargeInfos =
            balancingMarketService.balanceTimeslot(brokerList, report);
    assertEquals(3, chargeInfos.size(), "correct count");

    ChargeInfo c1b1 = findFirst(chargeInfos.values(),
                                new Predicate<ChargeInfo>() {
      @Override
      public boolean apply (ChargeInfo item)
      {
        return (item.getBroker() == b1);
      }
    });
    assertNotNull(c1b1, "found correct chargeInfo");
    List<BalancingOrder> orders = c1b1.getBalancingOrders();
    assertEquals(2, orders.size(), "found 2 balancing orders");
    assertTrue(orders.contains(bo1t1), "contains bo1t1");
    assertTrue(orders.contains(bo1t2), "contains bo1t2");

    ChargeInfo c1b2 = findFirst(chargeInfos.values(),
                                new Predicate<ChargeInfo>() {
      @Override
      public boolean apply (ChargeInfo item)
      {
        return (item.getBroker() == b2);
      }
    });
    assertNotNull(c1b2, "found correct chargeInfo");
    orders = c1b2.getBalancingOrders();
    assertEquals(1, orders.size(), "found 1 balancing order");
    assertTrue(orders.contains(bo2t1), "contains bo2t1");
  }


  class DoubleWrapper
  {
    double value = 0.0;

    DoubleWrapper()
    {
      super();
    }

    double add (double addend)
    {
      value += addend;
      return value;
    }

    double getValue ()
    {
      return value;
    }
  }
}
