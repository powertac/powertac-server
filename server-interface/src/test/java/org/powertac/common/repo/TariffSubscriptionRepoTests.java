package org.powertac.common.repo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

@SpringJUnitConfig(locations = {"classpath:tsr-config.xml"})
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

  @BeforeEach
  public void setUp () throws Exception
  {
    reset(mockTariffRepo);
    //reset(mockTariffMarket);
    repo = new TariffSubscriptionRepo();
    //ReflectionTestUtils.setField(repo, "tariffMarketService", mockTariffMarket);
    ReflectionTestUtils.setField(repo, "tariffRepo", mockTariffRepo);
    baseTime = ZonedDateTime.of(1972, 9, 6, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();;
    timeService.setCurrentTime(baseTime);
    Competition.newInstance("tst").withSimulationBaseTime(baseTime);
    b1 = new Broker("Bob");
    b2 = new Broker("Barb");
    c1 = new CustomerInfo("Podunk", 23).withPowerType(PowerType.CONSUMPTION);
    c2 = new CustomerInfo("Sticks", 230).withPowerType(PowerType.CONSUMPTION);
    ts1 = new TariffSpecification(b1, PowerType.CONSUMPTION)
        .withExpiration(baseTime.plusMillis(TimeService.DAY * 10))
        .withMinDuration(TimeService.DAY * 5)
        .addRate(new Rate().withValue(-0.11));
    ts2 = new TariffSpecification(b2, PowerType.CONSUMPTION)
        .withExpiration(baseTime.plusMillis(TimeService.DAY * 11))
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
    assertEquals(0, subs.size(), "none found for customer");
    subs = repo.findSubscriptionsForTariff(t1);
    assertEquals(0, subs.size(), "none found for tariff");
    TariffSubscription sub1 = new TariffSubscription(c1, t1);
    repo.add(sub1);
    subs = repo.findSubscriptionsForCustomer(c1);
    assertEquals(1, subs.size(), "one found for customer 1");
    assertEquals(sub1, subs.get(0), "correct sub 1");
    subs = repo.findSubscriptionsForCustomer(c2);
    assertEquals(0, subs.size(), "non found for customer 2");
    subs = repo.findSubscriptionsForTariff(t1);
    assertEquals(1, subs.size(), "one found for tariff");
    assertEquals(sub1, subs.get(0), "correct sub 2");
    subs = repo.findSubscriptionsForBroker(b1);
    assertEquals(1, subs.size(), "one found for Bob");
    assertEquals(sub1, subs.get(0), "correct sub 3");
    subs = repo.findSubscriptionsForBroker(b2);
    assertEquals(0, subs.size(), "none found for Barb");

    TariffSubscription sub2 = new TariffSubscription(c2, t1);
    repo.add(sub2);
    subs = repo.findSubscriptionsForCustomer(c2);
    assertEquals(1, subs.size(), "one found for customer 2");
    assertEquals(sub2, subs.get(0), "correct sub 3");
    subs = repo.findSubscriptionsForTariff(t1);
    assertEquals(2, subs.size(), "two found for tariff");
    subs = repo.findSubscriptionsForBroker(b1);
    assertEquals(2, subs.size(), "two found for Bob");
    subs = repo.findSubscriptionsForBroker(b2);
    assertEquals(0, subs.size(), "none found for Barb");
  }

  @Test
  public void testGetSubscription ()
  {
    List<TariffSubscription> subs;
    Tariff t1 = new Tariff(ts1);
    t1.init();
    TariffSubscription sub = repo.getSubscription(c1, t1);
    assertEquals(c1, sub.getCustomer(), "correct customer");
    assertEquals(t1, sub.getTariff(), "correct tariff");
    assertEquals(0, sub.getCustomersCommitted(), "no subscribers");
    subs = repo.findSubscriptionsForCustomer(c1);
    assertEquals(1, subs.size(), "one found for customer");
    assertEquals(sub, subs.get(0), "correct sub 1");
    subs = repo.findSubscriptionsForTariff(t1);
    assertEquals(1, subs.size(), "one found for tariff");
    assertEquals(sub, subs.get(0), "correct sub 2");
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
    assertEquals(2, subs.size(), "two found");
    subs = repo.findActiveSubscriptionsForCustomer(c1);
    assertEquals(1, subs.size(), "one found");
    assertEquals(sub1, subs.get(0), "correct sub 1");
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
    assertEquals(sub1, repo.findSubscriptionForTariffAndCustomer(t1, c1), "found s1");
    assertEquals(sub2, repo.findSubscriptionForTariffAndCustomer(t2, c1), "found s2");
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
    assertEquals(1, subs.size(), "one killed");
    assertEquals(sub2, subs.get(0), "t2 killed");
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
    assertEquals(sub1, repo.findSubscriptionForTariffAndCustomer(t1, c1), "found s1");
    assertEquals(sub2, repo.findSubscriptionForTariffAndCustomer(t2, c1), "found s2");
    repo.recycle();
    subs = repo.findSubscriptionsForCustomer(c1);
    assertEquals(0, subs.size(), "no subs found");
  }

}
