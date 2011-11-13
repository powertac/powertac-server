package org.powertac.distributionutility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.PluginConfig;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.distributionutility.DistributionUtilityService.ChargeInfo;
import org.powertac.common.interfaces.TariffMarket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:src/test/resources/test-config.xml" })
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
  private PluginConfigRepo pluginConfigRepo;

  @Autowired
  private TariffMarket tariffMarketService;

  @Autowired
  private DistributionUtilityService distributionUtilityService;

  @Autowired
  private DistributionUtilityInitializationService distributionUtilityInitializationService;

  private Accounting accountingService;
  private TariffRepo tariffRepo;
  private Competition comp;
  private List<Broker> brokerList = new ArrayList<Broker>();
  private List<TariffSpecification> tariffSpecList = new ArrayList<TariffSpecification>();
  private List<Tariff> tariffList = new ArrayList<Tariff>();
  private Instant exp;
  private DateTime start;
  private CustomerInfo customerInfo;

  @Before
  public void setUp ()
  {
    // create a Competition, needed for initialization
    comp = Competition.newInstance("du-test");

    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC);
    timeService.setCurrentTime(start.toInstant());
    timeslotRepo.makeTimeslot(start.toInstant());
    timeslotRepo.currentTimeslot().disable();// enabled: false);
    exp = new Instant(start.getMillis() + TimeService.WEEK * 10);

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

    // Create customer
    customerInfo = new CustomerInfo("Wilfred", 1);
  }

  @After
  public void tearDown ()
  {
    // clear pending transactions list
    accountingService = (Accounting) SpringApplicationContext.getBean("accountingService");
    accountingService.activate(new Instant(), 1);

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
    distributionUtilityInitializationService.setDefaults();
    PluginConfig duConfig = pluginConfigRepo.findByRoleName("DistributionUtility");
    duConfig.addConfiguration("balancingCostMin", "-0.06")
            .addConfiguration("balancingCostMax", "-0.06");
    distributionUtilityInitializationService.initialize(comp,
                                                        new ArrayList<String>());
  }

  @Test
  public void testGetMarketBalance ()
  {
    initializeService();
    double balance = 0.0;

    // Create two tariff specifications, one for consumption and one for
    // production
    TariffSpecification tariffSpec1 = new TariffSpecification(brokerList.get(0),
                                                              PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec1.addRate(new Rate().withValue(-0.1));

    TariffSpecification tariffSpec2 = new TariffSpecification(brokerList.get(0),
                                                              PowerType.PRODUCTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec2.addRate(new Rate().withValue(0.1));

    // Create a tariff for each specification
    Tariff tariff1 = new Tariff(tariffSpec1);
    tariff1.init();

    Tariff tariff2 = new Tariff(tariffSpec2);
    tariff2.init();

    // Subscribe customers to each tariff
    TariffSubscription tsubConsume = tariffMarketService.subscribeToTariff(tariff1,
                                                                           customerInfo,
                                                                           1);
    TariffSubscription tsubProduce = tariffMarketService.subscribeToTariff(tariff2,
                                                                           customerInfo,
                                                                           1);

    tsubConsume.usePower(500.0);
    balance -= 500.0;
    assertEquals("correct balance",
                 balance,
                 distributionUtilityService.getMarketBalance(brokerList.get(0)),
                 1e-6);

    tsubProduce.usePower(-500.0);
    balance += 500.0;
    assertEquals("correct balance",
                 balance,
                 distributionUtilityService.getMarketBalance(brokerList.get(0)),
                 1e-6);

    tsubConsume.usePower(50000);
    balance -= 50000;
    assertEquals("correct balance",
                 balance,
                 distributionUtilityService.getMarketBalance(brokerList.get(0)),
                 1e-6);
  }

  @Test
  public void testNegImbalancedMarket ()
  {
    initializeService();
    double powerUse = 50.0;

    // Create a tariff specification for each broker
    TariffSpecification tariffSpec1 = new TariffSpecification(brokerList.get(0),
                                                              PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec1.addRate(new Rate().withValue(-0.1));
    tariffSpecList.add(tariffSpec1);

    TariffSpecification tariffSpec2 = new TariffSpecification(brokerList.get(1),
                                                              PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec2.addRate(new Rate().withValue(-0.1));
    tariffSpecList.add(tariffSpec2);

    TariffSpecification tariffSpec3 = new TariffSpecification(brokerList.get(2),
                                                              PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec3.addRate(new Rate().withValue(-0.1));
    tariffSpecList.add(tariffSpec3);

    // Create a tariff for each specification
    Tariff tariff1 = new Tariff(tariffSpecList.get(0));
    tariff1.init();
    tariffList.add(tariff1);

    Tariff tariff2 = new Tariff(tariffSpecList.get(1));
    tariff2.init();
    tariffList.add(tariff2);

    Tariff tariff3 = new Tariff(tariffSpecList.get(2));
    tariff3.init();
    tariffList.add(tariff3);

    // Subscribe customers to each tariff
    TariffSubscription tsub1 = tariffMarketService.subscribeToTariff(tariffList.get(0),
                                                                     customerInfo,
                                                                     1);
    TariffSubscription tsub2 = tariffMarketService.subscribeToTariff(tariffList.get(1),
                                                                     customerInfo,
                                                                     1);
    TariffSubscription tsub3 = tariffMarketService.subscribeToTariff(tariffList.get(2),
                                                                     customerInfo,
                                                                     1);

    // Negatively balance market, each broker has equal load (in kWh).
    tsub1.usePower(powerUse);
    tsub2.usePower(powerUse);
    tsub3.usePower(powerUse);

    double marketBalance = powerUse * -3; // Compute market balance

    List<ChargeInfo> theChargeInfoList = distributionUtilityService.balanceTimeslot(timeslotRepo.currentTimeslot(),
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
    double powerUse = -50.0;

    // Create a production tariff specification for each broker
    TariffSpecification tariffSpec1 = new TariffSpecification(
                                                              brokerList.get(0),
                                                              PowerType.PRODUCTION).withExpiration(exp)
                                                                                   .withMinDuration(TimeService.WEEK * 8);
    tariffSpec1.addRate(new Rate().withValue(0.1));
    tariffSpecList.add(tariffSpec1);

    TariffSpecification tariffSpec2 = new TariffSpecification(
                                                              brokerList.get(1),
                                                              PowerType.PRODUCTION).withExpiration(exp)
                                                                                   .withMinDuration(TimeService.WEEK * 8);
    tariffSpec2.addRate(new Rate().withValue(0.1));
    tariffSpecList.add(tariffSpec2);

    TariffSpecification tariffSpec3 = new TariffSpecification(
                                                              brokerList.get(2),
                                                              PowerType.PRODUCTION).withExpiration(exp)
                                                                                   .withMinDuration(TimeService.WEEK * 8);
    tariffSpec3.addRate(new Rate().withValue(0.1));
    tariffSpecList.add(tariffSpec3);

    // Create a tariff for each specification
    Tariff tariff1 = new Tariff(tariffSpecList.get(0));
    tariff1.init();
    tariffList.add(tariff1);

    Tariff tariff2 = new Tariff(tariffSpecList.get(1));
    tariff2.init();
    tariffList.add(tariff2);

    Tariff tariff3 = new Tariff(tariffSpecList.get(2));
    tariff3.init();
    tariffList.add(tariff3);

    // Subscribe customers to each tariff
    TariffSubscription tsub1 = tariffMarketService.subscribeToTariff(tariffList.get(0),
                                                                     customerInfo,
                                                                     1);
    TariffSubscription tsub2 = tariffMarketService.subscribeToTariff(tariffList.get(1),
                                                                     customerInfo,
                                                                     1);
    TariffSubscription tsub3 = tariffMarketService.subscribeToTariff(tariffList.get(2),
                                                                     customerInfo,
                                                                     1);

    // Negatively balance market, each broker has equal load (in kWh).
    tsub1.usePower(powerUse);
    tsub2.usePower(powerUse);
    tsub3.usePower(powerUse);

    double marketBalance = powerUse * -3; // Compute market balance

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

    // Create tariff specifications for each broker
    TariffSpecification tariffSpec1 = new TariffSpecification(brokerList.get(0),
                                                              PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec1.addRate(new Rate().withValue(-0.1));

    TariffSpecification tariffSpec2 = new TariffSpecification(brokerList.get(1),
                                                              PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec2.addRate(new Rate().withValue(-0.1));

    TariffSpecification tariffSpec3 = new TariffSpecification(brokerList.get(2),
                                                              PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec3.addRate(new Rate().withValue(-0.1));

    // Create a tariff for each specification
    Tariff tariff1 = new Tariff(tariffSpec1);
    tariff1.init();

    Tariff tariff2 = new Tariff(tariffSpec2);
    tariff2.init();

    Tariff tariff3 = new Tariff(tariffSpec3);
    tariff3.init();

    // Subscribe customers to each tariff
    TariffSubscription tsub1 = tariffMarketService.subscribeToTariff(tariff1,
                                                                     customerInfo,
                                                                     1);
    TariffSubscription tsub2 = tariffMarketService.subscribeToTariff(tariff2,
                                                                     customerInfo,
                                                                     1);
    TariffSubscription tsub3 = tariffMarketService.subscribeToTariff(tariff3,
                                                                     customerInfo,
                                                                     1);

    // Create positively balanced broker
    tsub1.usePower(19654852);
    tsub1.usePower(-54862); // this probably should not work

    // Create balanced broker
    tsub2.usePower(500000);
    tsub2.usePower(-500000);

    // Create negatively balanced broker
    tsub3.usePower(-8796542);
    tsub3.usePower(5423);

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
    // Create a tariff specification for each broker
    TariffSpecification tariffSpec1 = new TariffSpecification(brokerList.get(0),
                                                              PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec1.addRate(new Rate().withValue(-0.1));
    tariffSpecList.add(tariffSpec1);

    TariffSpecification tariffSpec2 = new TariffSpecification(brokerList.get(1),
                                                              PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec2.addRate(new Rate().withValue(-0.1));
    tariffSpecList.add(tariffSpec2);

    TariffSpecification tariffSpec3 = new TariffSpecification(brokerList.get(2),
                                                              PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8);
    tariffSpec3.addRate(new Rate().withValue(-0.1));
    tariffSpecList.add(tariffSpec3);

    // Create a tariff for each specification
    Tariff tariff1 = new Tariff(tariffSpecList.get(0));
    tariff1.init();
    tariffList.add(tariff1);

    Tariff tariff2 = new Tariff(tariffSpecList.get(1));
    tariff2.init();
    tariffList.add(tariff2);

    Tariff tariff3 = new Tariff(tariffSpecList.get(2));
    tariff3.init();
    tariffList.add(tariff3);

    // Subscribe customers to each tariff
    TariffSubscription tsub1 = tariffMarketService.subscribeToTariff(tariffList.get(0),
                                                                     customerInfo,
                                                                     1);
    TariffSubscription tsub2 = tariffMarketService.subscribeToTariff(tariffList.get(1),
                                                                     customerInfo,
                                                                     1);
    TariffSubscription tsub3 = tariffMarketService.subscribeToTariff(tariffList.get(2),
                                                                     customerInfo,
                                                                     1);

    // Balance brokers such that balances are 2, -4, and 0 (MWh)
    // respectively
    tsub1.usePower(-200);
    tsub2.usePower(400);

    // List solution =
    // distributionUtilityService.computeNonControllableBalancingCharges(brokerList)
    List<ChargeInfo> theChargeInfoList = distributionUtilityService.balanceTimeslot(timeslotRepo.currentTimeslot(),
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
