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
import static org.mockito.Mockito.*;

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
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test cases for AbstractCustomer. Uses a Spring application context to access
 * autowired components. Need to mock: TariffMarket
 * 
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
  CustomerInfo info, info2;
  AbstractCustomer customer;
  Instant now;
  TariffSpecification defaultTariffSpec, defaultTariffSpecControllable;
  Tariff tariff;
  Tariff defaultTariff, defaultTariffControllable;

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

    defaultTariffSpec =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(exp).withMinDuration(TimeService.WEEK * 8)
              .addRate(new Rate().withValue(-0.222));
    defaultTariff = new Tariff(defaultTariffSpec);
    defaultTariff.init();

    defaultTariffSpecControllable =
      new TariffSpecification(broker1, PowerType.INTERRUPTIBLE_CONSUMPTION)
              .withExpiration(exp).withMinDuration(TimeService.WEEK * 8)
              .addRate(new Rate().withValue(-0.121).withMaxCurtailment(0.3));
    defaultTariffControllable = new Tariff(defaultTariffSpecControllable);
    defaultTariffControllable.init();

    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
            .thenReturn(defaultTariff);
    when(mockTariffMarket.getDefaultTariff(PowerType.INTERRUPTIBLE_CONSUMPTION))
            .thenReturn(defaultTariffControllable);

  }

  @Test
  public void testCreation ()
  {
    info = new CustomerInfo("Podunk", 100).withPowerType(PowerType.CONSUMPTION);
    info2 =
      new CustomerInfo("Philby", 200)
              .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);

    customer = new AbstractCustomer(info.getName());
    customer.addCustomerInfo(info);
    customer.addCustomerInfo(info2);

    assertNotNull("not null", customer);
    assertEquals("correct customerInfo size", 2, customer.getCustomerInfo()
            .size());
    assertEquals("correct powerType for first", PowerType.CONSUMPTION, customer
            .getCustomerInfo().get(0).getPowerType());
    assertEquals("correct powerType for second",
                 PowerType.INTERRUPTIBLE_CONSUMPTION, customer
                         .getCustomerInfo().get(1).getPowerType());
    assertEquals("two customers on repo", 2, customerRepo.list().size());
  }

  @Test
  public void testFalseAddition ()
  {
    info = new CustomerInfo("Podunk", 100).withPowerType(PowerType.CONSUMPTION);
    info2 =
      new CustomerInfo("Philby", 200).withPowerType(PowerType.CONSUMPTION);

    customer = new AbstractCustomer(info.getName());
    customer.addCustomerInfo(info);
    customer.addCustomerInfo(info2);

    assertNotNull("not null", customer);
    assertEquals("correct customerInfo size", 1, customer.getCustomerInfo()
            .size());
    assertEquals("correct powerType for first", PowerType.CONSUMPTION, customer
            .getCustomerInfo().get(0).getPowerType());
    assertEquals("one customer on repo", 1, customerRepo.list().size());
  }

  @Test
  public void testDefaultSubscription ()
  {
    info = new CustomerInfo("Podunk", 100).withPowerType(PowerType.CONSUMPTION);
    info2 =
      new CustomerInfo("Philby", 200)
              .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
    // note that the population of the second PowerType is ignored

    customer = new AbstractCustomer(info.getName());
    customer.addCustomerInfo(info);
    customer.addCustomerInfo(info2);

    TariffSubscription defaultSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(0),
                                             defaultTariff);
    defaultSub.subscribe(customer.getCustomerInfo().get(0).getPopulation());
    TariffSubscription defaultControllableSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(1),
                                             defaultTariffControllable);
    defaultControllableSub.subscribe(customer.getCustomerInfo().get(1)
            .getPopulation());

    customer.subscribeDefault();
    verify(mockTariffMarket).subscribeToTariff(defaultTariff, info, 100);
    verify(mockTariffMarket).subscribeToTariff(defaultTariffControllable, info2, 100);

//    assertEquals("one subscription for CONSUMPTION customerInfo",
//                 1,
//                 tariffSubscriptionRepo
//                         .findSubscriptionsForCustomer(customer.getCustomerInfo()
//                                                               .get(0)).size());
//    assertEquals("one subscription for INTERRUPTIBLE_CONSUMPTION customerInfo",
//                 1,
//                 tariffSubscriptionRepo
//                         .findSubscriptionsForCustomer(customer.getCustomerInfo()
//                                                               .get(1)).size());
//
//    assertEquals("customer on DefaultTariff",
//                 mockTariffMarket.getDefaultTariff(customer.getCustomerInfo()
//                         .get(0).getPowerType()),
//                 tariffSubscriptionRepo
//                         .findSubscriptionsForCustomer(customer.getCustomerInfo()
//                                                               .get(0)).get(0)
//                         .getTariff());
  }

  @Test
  public void changeSubscription ()
  {
    info = new CustomerInfo("Podunk", 100).withPowerType(PowerType.CONSUMPTION);
    info2 =
      new CustomerInfo("Philby", 200)
              .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);

    customer = new AbstractCustomer(info.getName());
    customer.addCustomerInfo(info);
    customer.addCustomerInfo(info2);

    // capture subscription method args
//    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
//    ArgumentCaptor<CustomerInfo> customerArg =
//      ArgumentCaptor.forClass(CustomerInfo.class);
//    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
//    ArgumentCaptor<PowerType> powerArg =
//      ArgumentCaptor.forClass(PowerType.class);

    TariffSubscription defaultSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(0),
                                             defaultTariff);
    defaultSub.subscribe(customer.getCustomerInfo().get(0).getPopulation());
    TariffSubscription defaultControllableSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(1),
                                             defaultTariffControllable);
    defaultControllableSub.subscribe(customer.getCustomerInfo().get(1)
            .getPopulation());
//    when(
//         mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//            .thenReturn(defaultSub).thenReturn(defaultControllableSub);

    customer.subscribeDefault();

    Rate r2 = new Rate().withValue(-0.222);
    Rate r3 = new Rate().withValue(-0.08).withMaxCurtailment(0.1);

    TariffSpecification tsc1 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plus(TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 =
      new TariffSpecification(broker1, PowerType.INTERRUPTIBLE_CONSUMPTION)
              .withExpiration(now.plus(TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r3);
    TariffSpecification tsc3 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plus(3 * TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();

    assertEquals("Five tariffs", 5, tariffRepo.findAllTariffs().size());

    // Changing from default to another tariff
    TariffSubscription sub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(0),
                                             tariff1);
    sub.subscribe(customer.getCustomerInfo().get(0).getPopulation());
    TariffSubscription sub2 =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(1),
                                             tariff2);
    sub2.subscribe(customer.getCustomerInfo().get(1).getPopulation());
//    when(
//         mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//            .thenReturn(sub).thenReturn(sub2);
    when(mockTariffMarket.getActiveTariffList(PowerType.CONSUMPTION))
            .thenReturn(tariffRepo.findActiveTariffs(PowerType.CONSUMPTION));
    when(mockTariffMarket.getActiveTariffList(PowerType.INTERRUPTIBLE_CONSUMPTION))
            .thenReturn(tariffRepo.findActiveTariffs(PowerType.INTERRUPTIBLE_CONSUMPTION));

    customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer
            .getCustomerInfo().get(0).getPowerType()), customer
            .getCustomerInfo().get(0));
    customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer
            .getCustomerInfo().get(1).getPowerType()), customer
            .getCustomerInfo().get(1));
    assertFalse("Changed from default tariff for PowerType CONSUMPTION",
                tariffSubscriptionRepo
                        .findSubscriptionsForCustomer(customer.getCustomerInfo()
                                                              .get(0)).get(1)
                        .getTariff() == mockTariffMarket
                        .getDefaultTariff(customer.getCustomerInfo().get(0)
                                .getPowerType()));

    assertFalse("Changed from default tariff for PowerType INTERRUPTIBLE_CONSUMPTION",
                tariffSubscriptionRepo
                        .findSubscriptionsForCustomer(customer.getCustomerInfo()
                                                              .get(1)).get(1)
                        .getTariff() == mockTariffMarket
                        .getDefaultTariff(customer.getCustomerInfo().get(1)
                                .getPowerType()));

    // Changing back from the new tariff to the default one in order to check
    // every
    // changeSubscription Method
    Tariff lastTariff =
      tariffSubscriptionRepo
              .findSubscriptionsForCustomer(customer.getCustomerInfo().get(0))
              .get(1).getTariff();
    Tariff lastTariff2 =
      tariffSubscriptionRepo
              .findSubscriptionsForCustomer(customer.getCustomerInfo().get(1))
              .get(1).getTariff();

//    when(
//         mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//            .thenReturn(defaultSub).thenReturn(defaultControllableSub);

    defaultSub.subscribe(customer.getCustomerInfo().get(0).getPopulation());
    defaultControllableSub.subscribe(customer.getCustomerInfo().get(1)
            .getPopulation());

    customer.changeSubscription(lastTariff,
                                mockTariffMarket.getDefaultTariff(customer
                                        .getCustomerInfo().get(0)
                                        .getPowerType()), customer
                                        .getCustomerInfo().get(0));
    customer.changeSubscription(lastTariff2,
                                mockTariffMarket.getDefaultTariff(customer
                                        .getCustomerInfo().get(1)
                                        .getPowerType()), customer
                                        .getCustomerInfo().get(1));

    assertTrue("Changed to default tariff for CONSUMPTION",
               tariffSubscriptionRepo
                       .findSubscriptionsForCustomer(customer.getCustomerInfo()
                                                             .get(0)).get(0)
                       .getTariff() == mockTariffMarket
                       .getDefaultTariff(PowerType.CONSUMPTION));

    assertTrue("Changed to default tariff for INTERRUPTIBLE_CONSUMPTION",
               tariffSubscriptionRepo
                       .findSubscriptionsForCustomer(customer.getCustomerInfo()
                                                             .get(1)).get(0)
                       .getTariff() == mockTariffMarket
                       .getDefaultTariff(PowerType.INTERRUPTIBLE_CONSUMPTION));

    // Last changeSubscription Method checked
//    when(
//         mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//            .thenReturn(sub).thenReturn(sub2);
    customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer
                                        .getCustomerInfo().get(0)
                                        .getPowerType()), lastTariff, 5,
                                customer.getCustomerInfo().get(0));
    customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer
                                        .getCustomerInfo().get(1)
                                        .getPowerType()), lastTariff2, 5,
                                customer.getCustomerInfo().get(1));

    assertFalse("Changed from default tariff for CONSUMPTION",
                tariffSubscriptionRepo
                        .findSubscriptionsForCustomer(customer.getCustomerInfo()
                                                              .get(0)).get(1)
                        .getTariff() == mockTariffMarket
                        .getDefaultTariff(PowerType.CONSUMPTION));

    assertFalse("Changed from default tariff for INTERRUPTIBLE_CONSUMPTION",
                tariffSubscriptionRepo
                        .findSubscriptionsForCustomer(customer.getCustomerInfo()
                                                              .get(1)).get(1)
                        .getTariff() == mockTariffMarket
                        .getDefaultTariff(PowerType.INTERRUPTIBLE_CONSUMPTION));

  }

  @Test
  public void revokeSubscription ()
  {
    info = new CustomerInfo("Podunk", 100).withPowerType(PowerType.CONSUMPTION);
    info2 =
      new CustomerInfo("Philby", 200)
              .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);

    customer = new AbstractCustomer(info.getName());
    customer.addCustomerInfo(info);
    customer.addCustomerInfo(info2);

    // capture subscription method args
//    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
//    ArgumentCaptor<CustomerInfo> customerArg =
//      ArgumentCaptor.forClass(CustomerInfo.class);
//    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);

    TariffSubscription defaultSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(0),
                                             defaultTariff);
    defaultSub.subscribe(customer.getCustomerInfo().get(0).getPopulation());
    TariffSubscription defaultControllableSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(1),
                                             defaultTariffControllable);
    defaultControllableSub.subscribe(customer.getCustomerInfo().get(1)
            .getPopulation());
//    when(
//         mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//            .thenReturn(defaultSub).thenReturn(defaultControllableSub);

    customer.subscribeDefault();

    assertEquals("one subscription for CONSUMPTION", 1, tariffSubscriptionRepo
            .findSubscriptionsForCustomer(customer.getCustomerInfo().get(0))
            .size());
    assertEquals("one subscription for INTERRUPTIBLE_CONSUMPTION",
                 1,
                 tariffSubscriptionRepo
                         .findSubscriptionsForCustomer(customer.getCustomerInfo()
                                                               .get(1)).size());

    Rate r2 = new Rate().withValue(-0.222);
    Rate r3 = new Rate().withValue(-0.08).withMaxCurtailment(0.1);

    TariffSpecification tsc1 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plus(TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 =
      new TariffSpecification(broker1, PowerType.INTERRUPTIBLE_CONSUMPTION)
              .withExpiration(now.plus(TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r3);
    TariffSpecification tsc3 =
      new TariffSpecification(broker1, PowerType.CONSUMPTION)
              .withExpiration(now.plus(3 * TimeService.DAY))
              .withMinDuration(TimeService.WEEK * 8).addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();

    assertEquals("Five consumption tariffs", 5, tariffRepo.findAllTariffs()
            .size());

    TariffSubscription tsd =
      tariffSubscriptionRepo
              .findSubscriptionForTariffAndCustomer(defaultTariff, customer
                      .getCustomerInfo().get(0));
    TariffSubscription tsd2 =
      tariffSubscriptionRepo
              .findSubscriptionForTariffAndCustomer(defaultTariffControllable,
                                                    customer.getCustomerInfo()
                                                            .get(1));
    assertNotNull("not null", tsd);
    assertNotNull("not null", tsd2);

    customer.unsubscribe(tsd, 70);
    customer.unsubscribe(tsd2, 70);
    TariffSubscription sub1 =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(0),
                                             tariff1);
    sub1.subscribe(23);
    TariffSubscription sub2 =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(1),
                                             tariff2);
    sub2.subscribe(23);
    TariffSubscription sub3 =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo().get(0),
                                             tariff3);
    sub3.subscribe(24);

    assertEquals("Five subscriptions for customer",
                 5,
                 tariffSubscriptionRepo
                         .findSubscriptionsForCustomer(customer.getCustomerInfo()
                                                               .get(0)).size()
                         + tariffSubscriptionRepo
                                 .findSubscriptionsForCustomer(customer.getCustomerInfo()
                                                                       .get(1))
                                 .size());

    timeService.setCurrentTime(new Instant(timeService.getCurrentTime()
            .getMillis() + TimeService.HOUR));

    TariffRevoke tex = new TariffRevoke(tsc2.broker, tsc2);
    tariff2.setState(Tariff.State.KILLED);
    assertTrue("tariff revoked", tariff2.isRevoked());

    TariffRevoke tex2 = new TariffRevoke(tsc3.broker, tsc3);
    tariff3.setState(Tariff.State.KILLED);
    assertTrue("tariff revoked", tariff3.isRevoked());

  }

}
