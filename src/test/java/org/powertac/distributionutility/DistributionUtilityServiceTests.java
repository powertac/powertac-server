package org.powertac.distributionutility;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
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
  private List<Broker> brokerList = new ArrayList<Broker>();
  private List<TariffSpecification> tariffSpecList = new ArrayList<TariffSpecification>();
  private List<Tariff> tariffList = new ArrayList<Tariff>();
  private DateTime start;

  @Before
  public void setUp ()
  {
    // create a Competition, needed for initialization
    comp = Competition.newInstance("du-test");
    Competition.setCurrent(comp);
    
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
    distributionUtilityService.setDefaults();
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("distributionutility.distributionUtilityService.balancingCostMin", "-0.06");
    map.put("distributionutility.distributionUtilityService.balancingCostMax", "-0.06");
    Configuration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    distributionUtilityService.initialize(comp, new ArrayList<String>());
  }

  private void updatePrices ()
  {
    // add some new timeslots
    Timeslot ts0 = timeslotRepo.currentTimeslot();
    long start = timeService.getCurrentTime().getMillis();
    Timeslot ts1 = timeslotRepo.findByInstant(new Instant(start - TimeService.HOUR * 3));
    Timeslot ts2 = timeslotRepo.findByInstant(new Instant(start - TimeService.HOUR * 2));
    Timeslot ts3 = timeslotRepo.findByInstant(new Instant(start - TimeService.HOUR));

    // add some orderbooks
    orderbookRepo.makeOrderbook(ts3, 33.0);
    orderbookRepo.makeOrderbook(ts3, 32.0);
    orderbookRepo.makeOrderbook(ts0, 20.2);
    orderbookRepo.makeOrderbook(ts0, 21.2);
    orderbookRepo.makeOrderbook(ts0, 19.8);
    // this should be the spot price
    orderbookRepo.makeOrderbook(ts0, 20.1);
  }
}
