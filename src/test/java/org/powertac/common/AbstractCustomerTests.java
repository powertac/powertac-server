/* Copyright (c) 2011 by the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import javax.annotation.Resource;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test cases for AbstractCustomer. Uses a Spring application context to access autowired
 * components. Need to mock: TariffMarket
 * @author Antonios Chrysopoulos
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
@DirtiesContext
public class AbstractCustomerTests
{
  @Autowired
  TimeService timeService;

  @Autowired
  TariffRepo tariffRepo;

  @Autowired
  CustomerRepo customerRepo;

  @Autowired
  TariffSubscriptionRepo tariffSubscriptionRepo;

  @Resource
  TariffMarket mockTariffMarket;

  Instant exp;
  // Instant start;
  Broker broker1;
  Broker broker2;
  CustomerInfo info;
  AbstractCustomer customer;
  Instant now;
  TariffSpecification defaultTariffSpec;
  Tariff tariff;
  Tariff defaultTariff;

  @Before
  public void setUp () throws Exception
  {
    customerRepo.recycle();
    tariffSubscriptionRepo.recycle();
    tariffRepo.recycle();
    broker1 = new Broker("Joe");
    broker1 = new Broker("Anna");

    now = new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(now.toInstant());

    exp = new Instant(now.getMillis() + TimeService.WEEK * 10);

    defaultTariffSpec = new TariffSpecification(broker1, PowerType.CONSUMPTION)
      .withExpiration(exp)
      .withMinDuration(TimeService.WEEK * 8)
      .addRate(new Rate().withValue(0.222));
    defaultTariff = new Tariff(defaultTariffSpec);
    defaultTariff.init();

    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
      .thenReturn(defaultTariff);

  }

  @Test
  public void testCreation ()
  {
    info = new CustomerInfo("Podunk", 100).addPowerType(PowerType.CONSUMPTION);
    customer = new AbstractCustomer(info);
    assertNotNull("not null", customer);
    assertEquals("correct customerInfo", info, customer.getCustomerInfo());
    assertEquals("correct id", info.getId(), customer.getId());
    assertEquals("correct powerType", PowerType.CONSUMPTION,
                 customer.getCustomerInfo().getPowerTypes().get(0));
    assertEquals("one customer on repo", 1, customerRepo.list().size());
  }

  @Test
  public void testDefaultSubscription ()
  {
    info = new CustomerInfo("Podunk", 100).addPowerType(PowerType.CONSUMPTION);
    customer = new AbstractCustomer(info);

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = 
        ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg = 
        ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = 
        ArgumentCaptor.forClass(Integer.class);
    TariffSubscription defaultSub =
        tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(),
                                               defaultTariff);
    when(mockTariffMarket.subscribeToTariff(tariffArg.capture(),
                                            customerArg.capture(),
                                            countArg.capture()))
        .thenReturn(defaultSub);

    customer.subscribeDefault();

    assertEquals("one subscription for our customer", 1,
                 tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());
    assertEquals("customer on DefaultTariff",
                 mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)),
                 tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff());
  }

  @Test
  public void changeSubscription ()
  {
    info = new CustomerInfo("Podunk", 100).addPowerType(PowerType.CONSUMPTION);
    customer = new AbstractCustomer(info);

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg =
        ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg =
        ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg =
        ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg =
        ArgumentCaptor.forClass(PowerType.class);
    TariffSubscription defaultSub = 
        tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(),
                                               defaultTariff);
    when(mockTariffMarket.subscribeToTariff(tariffArg.capture(),
                                            customerArg.capture(),
                                            countArg.capture()))
        .thenReturn(defaultSub);

    customer.subscribeDefault();

    Rate r2 = new Rate().withValue(0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1,
                                                       PowerType.CONSUMPTION)
        .withExpiration(now.plus(3 * TimeService.DAY))
        .withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();

    assertEquals("Four tariffs", 4, tariffRepo.findAllTariffs().size());

    // Changing from default to another tariff
    TariffSubscription sub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff1);
    when(mockTariffMarket.subscribeToTariff(tariffArg.capture(),
                                            customerArg.capture(),
                                            countArg.capture()))
        .thenReturn(sub);
    when(mockTariffMarket.getActiveTariffList(powerArg.capture()))
        .thenReturn(tariffRepo.findAllTariffs());

    customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));
    assertFalse("Changed from default tariff",
        tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(1).getTariff()
        == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

    // Changing back from the new tariff to the default one in order to check every
    // changeSubscription Method
    Tariff lastTariff = tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(1).getTariff();

    when(mockTariffMarket.subscribeToTariff(tariffArg.capture(),
                                            customerArg.capture(),
                                            countArg.capture()))
        .thenReturn(defaultSub);
    customer.changeSubscription(lastTariff,
                                mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

    assertTrue("Changed from default tariff",
        tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff() == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

    // Last changeSubscription Method checked
    when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(sub);
    customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)), lastTariff, 5);

    assertFalse("Changed from default tariff",
        tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(1).getTariff()
        == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

  }

  @Test
  public void revokeSubscription ()
  {
    info = new CustomerInfo("Podunk", 100).addPowerType(PowerType.CONSUMPTION);
    customer = new AbstractCustomer(info);

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<TariffRevoke> tariffRevokeArg = ArgumentCaptor.forClass(TariffRevoke.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    //ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);
    TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
    when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);

    customer.subscribeDefault();

    assertEquals("one subscription", 1, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());

    Rate r2 = new Rate().withValue(0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + 2 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(new Instant(now.getMillis() + 3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8)
        .addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();

    assertEquals("Four consumption tariffs", 4, tariffRepo.findAllTariffs().size());

    //TariffStatus st = new TariffStatus(broker1, tariff2.getId(), tariff2.getId(), TariffStatus.Status.success);
    //when(mockTariffMarket.processTariff(tariffRevokeArg.capture())).thenReturn(st);
    TariffSubscription tsd = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(defaultTariff, customer.getCustomerInfo());

    assertNotNull("not null", tsd);

    customer.unsubscribe(tsd, 70);
    TariffSubscription sub1 = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff1);
    sub1.subscribe(23);
    TariffSubscription sub2 = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff2);
    sub2.subscribe(23);
    TariffSubscription sub3 = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff3);
    sub3.subscribe(24);

    assertEquals("Four subscriptions for customer", 4, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());

    timeService.setCurrentTime(new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR));
    TariffRevoke tex = new TariffRevoke(tsc2.broker, tsc2);
    //TariffStatus status = mockTariffMarket.processTariff(tex);
    tariff2.setState(Tariff.State.KILLED);
    //assertNotNull("non-null status", status);
    //assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertTrue("tariff revoked", tariff2.isRevoked());

  }
}