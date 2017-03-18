package org.powertac.common.repo;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
//import org.powertac.common.interfaces.TariffMarket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:tsr-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class TariffSubscriptionRepoTests
{
  @Autowired
  private TimeService timeService;
  
  //@Autowired
  //private TariffMarket mockTariffMarket;
  
  @Autowired
  private TariffRepo mockTariffRepo;
  
  TariffSubscriptionRepo repo;
  Instant baseTime;
  TariffSpecification ts1;
  TariffSpecification ts2;
  CustomerInfo c1;
  CustomerInfo c2;
  Broker b1;
  Broker b2;

  @Before
  public void setUp () throws Exception
  {
    reset(mockTariffRepo);
    //reset(mockTariffMarket);
    repo = new TariffSubscriptionRepo();
    //ReflectionTestUtils.setField(repo, "tariffMarketService", mockTariffMarket);
    ReflectionTestUtils.setField(repo, "tariffRepo", mockTariffRepo);
    baseTime = new DateTime(1972, 9, 6, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(baseTime);
    Competition.newInstance("tst").withSimulationBaseTime(baseTime);
    b1 = new Broker("Bob");
    b2 = new Broker("Barb");
    c1 = new CustomerInfo("Podunk", 23).withPowerType(PowerType.CONSUMPTION);
    c2 = new CustomerInfo("Sticks", 230).withPowerType(PowerType.CONSUMPTION);
    ts1 = new TariffSpecification(b1, PowerType.CONSUMPTION)
        .withExpiration(baseTime.plus(TimeService.DAY * 10))
        .withMinDuration(TimeService.DAY * 5)
        .addRate(new Rate().withValue(-0.11));
    ts2 = new TariffSpecification(b2, PowerType.CONSUMPTION)
        .withExpiration(baseTime.plus(TimeService.DAY * 11))
        .withMinDuration(TimeService.DAY * 7)
        .addRate(new Rate().withValue(-0.10));
  }

  // create and add a subscription
  @Test
  public void testAdd ()
  {
    List<TariffSubscription> subs;
    Tariff t1 = new Tariff(ts1);
    t1.init();
    subs = repo.findSubscriptionsForCustomer(c1);
    assertEquals("none found for customer", 0, subs.size());
    subs = repo.findSubscriptionsForTariff(t1);
    assertEquals("none found for tariff", 0, subs.size());
    TariffSubscription sub1 = new TariffSubscription(c1, t1);
    repo.add(sub1);
    subs = repo.findSubscriptionsForCustomer(c1);
    assertEquals("one found for customer 1", 1, subs.size());
    assertEquals("correct sub 1", sub1, subs.get(0));
    subs = repo.findSubscriptionsForCustomer(c2);
    assertEquals("non found for customer 2", 0, subs.size());
    subs = repo.findSubscriptionsForTariff(t1);
    assertEquals("one found for tariff", 1, subs.size());
    assertEquals("correct sub 2", sub1, subs.get(0));
    subs = repo.findSubscriptionsForBroker(b1);
    assertEquals("one found for Bob", 1, subs.size());
    assertEquals("correct sub 3", sub1, subs.get(0));
    subs = repo.findSubscriptionsForBroker(b2);
    assertEquals("none found for Barb", 0, subs.size());

    TariffSubscription sub2 = new TariffSubscription(c2, t1);
    repo.add(sub2);
    subs = repo.findSubscriptionsForCustomer(c2);
    assertEquals("one found for customer 2", 1, subs.size());
    assertEquals("correct sub 3", sub2, subs.get(0));
    subs = repo.findSubscriptionsForTariff(t1);
    assertEquals("two found for tariff", 2, subs.size());
    subs = repo.findSubscriptionsForBroker(b1);
    assertEquals("two found for Bob", 2, subs.size());
    subs = repo.findSubscriptionsForBroker(b2);
    assertEquals("none found for Barb", 0, subs.size());
  }

  @Test
  public void testGetSubscription ()
  {
    List<TariffSubscription> subs;
    Tariff t1 = new Tariff(ts1);
    t1.init();
    TariffSubscription sub = repo.getSubscription(c1, t1);
    assertEquals("correct customer", c1, sub.getCustomer());
    assertEquals("correct tariff", t1, sub.getTariff());
    assertEquals("no subscribers", 0, sub.getCustomersCommitted());
    subs = repo.findSubscriptionsForCustomer(c1);
    assertEquals("one found for customer", 1, subs.size());
    assertEquals("correct sub 1", sub, subs.get(0));
    subs = repo.findSubscriptionsForTariff(t1);
    assertEquals("one found for tariff", 1, subs.size());
    assertEquals("correct sub 2", sub, subs.get(0));
  }

  @Test
  public void testFindActiveSubscriptionsForCustomer ()
  {
    List<TariffSubscription> subs;
    Tariff t1 = new Tariff(ts1);
    Tariff t2 = new Tariff(ts2);
    t1.init();
    t2.init();
    TariffSubscription sub1 = repo.getSubscription(c1, t1);
    @SuppressWarnings("unused")
    TariffSubscription sub2 = repo.getSubscription(c1, t2);
    sub1.subscribe(19);
    subs = repo.findSubscriptionsForCustomer(c1);
    assertEquals("two found", 2, subs.size());
    subs = repo.findActiveSubscriptionsForCustomer(c1);
    assertEquals("one found", 1, subs.size());
    assertEquals("correct sub 1", sub1, subs.get(0));
  }

  @Test
  public void testFindSubscriptionForTariffAndCustomer ()
  {
    Tariff t1 = new Tariff(ts1);
    Tariff t2 = new Tariff(ts2);
    t1.init();
    t2.init();
    TariffSubscription sub1 = repo.getSubscription(c1, t1);
    TariffSubscription sub2 = repo.getSubscription(c1, t2);
    assertEquals("found s1", sub1, repo.findSubscriptionForTariffAndCustomer(t1, c1));
    assertEquals("found s2", sub2, repo.findSubscriptionForTariffAndCustomer(t2, c1));
  }

  @Test
  public void testGetRevokedSubscriptionList ()
  {
    List<TariffSubscription> subs;
    Tariff t1 = new Tariff(ts1);
    Tariff t2 = new Tariff(ts2);
    t1.init();
    t2.init();
    TariffSubscription sub1 = repo.getSubscription(c1, t1);
    TariffSubscription sub2 = repo.getSubscription(c1, t2);
    sub1.subscribe(11);
    sub2.subscribe(3);
    t2.setState(Tariff.State.KILLED);
    when(mockTariffRepo.findTariffById(ts1.getId())).thenReturn(t1);
    when(mockTariffRepo.findTariffById(ts2.getId())).thenReturn(t2);
    subs = repo.getRevokedSubscriptionList(c1);
    assertEquals("one killed", 1, subs.size());
    assertEquals("t2 killed", sub2, subs.get(0));
  }

  @Test
  public void testRecycle ()
  {
    List<TariffSubscription> subs;
    Tariff t1 = new Tariff(ts1);
    Tariff t2 = new Tariff(ts2);
    t1.init();
    t2.init();
    TariffSubscription sub1 = repo.getSubscription(c1, t1);
    TariffSubscription sub2 = repo.getSubscription(c1, t2);
    assertEquals("found s1", sub1, repo.findSubscriptionForTariffAndCustomer(t1, c1));
    assertEquals("found s2", sub2, repo.findSubscriptionForTariffAndCustomer(t2, c1));
    repo.recycle();
    subs = repo.findSubscriptionsForCustomer(c1);
    assertEquals("no subs found", 0, subs.size());
  }

}
