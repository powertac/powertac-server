/* Copyright (c) 2011-2014 by the original author or authors.
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
package org.powertac.customer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import javax.annotation.Resource;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Broker;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Test cases for AbstractCustomer. Uses a Spring application context to access
 * autowired components. Need to mock: TariffMarket
 * 
 * @author Antonios Chrysopoulos
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
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

  CustomerServiceAccessor serviceAccessor;

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

  @BeforeEach
  public void setUp () throws Exception
  {
    customerRepo.recycle();
    tariffSubscriptionRepo.recycle();
    tariffRepo.recycle();
    serviceAccessor = new ServiceAccessor();
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
    defaultTariff.setState(Tariff.State.OFFERED);

    defaultTariffSpecControllable =
      new TariffSpecification(broker1, PowerType.INTERRUPTIBLE_CONSUMPTION)
              .withExpiration(exp).withMinDuration(TimeService.WEEK * 8)
              .addRate(new Rate().withValue(-0.121).withMaxCurtailment(0.3));
    defaultTariffControllable = new Tariff(defaultTariffSpecControllable);
    defaultTariffControllable.init();
    defaultTariffControllable.setState(Tariff.State.OFFERED);

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

    customer = new DummyCustomer(info.getName());
    customer.addCustomerInfo(info);
    customer.addCustomerInfo(info2);

    assertNotNull(customer, "not null");
    assertEquals(2, customer.getCustomerInfos().size(), "correct customerInfo size");
    assertEquals(PowerType.CONSUMPTION, customer.getCustomerInfos().get(0).getPowerType(), "correct powerType for first");
    assertEquals(PowerType.INTERRUPTIBLE_CONSUMPTION, customer.getCustomerInfos().get(1).getPowerType(), "correct powerType for second");
    // AbstractCustomer does not do this
    //assertEquals(2, customerRepo.list().size(), "two customers on repo");
  }

  @Test
  public void testFalseAddition ()
  {
    info = new CustomerInfo("Podunk", 100).withPowerType(PowerType.CONSUMPTION);
    info2 =
      new CustomerInfo("Philby", 200).withPowerType(PowerType.CONSUMPTION);

    customer = new DummyCustomer(info.getName());
    customer.addCustomerInfo(info);
    customer.addCustomerInfo(info2);

    assertNotNull(customer, "not null");
    assertEquals(2, customer.getCustomerInfos().size(), "correct customerInfo size");
    assertEquals(PowerType.CONSUMPTION, customer.getCustomerInfos().get(0).getPowerType(), "correct powerType for first");
    //assertEquals(1, customerRepo.list().size(), "one customer on repo");
  }

  @Test
  public void testDefaultSubscription ()
  {
    info = new CustomerInfo("Podunk", 100).withPowerType(PowerType.CONSUMPTION);
    info2 =
      new CustomerInfo("Philby", 200)
              .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
    // note that the population of the second PowerType is ignored

    customer = new DummyCustomer(info.getName());
    customer.addCustomerInfo(info);
    customer.addCustomerInfo(info2);

    TariffSubscription defaultSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(0),
                                             defaultTariff);
    defaultSub.subscribe(customer.getCustomerInfos().get(0).getPopulation());
    TariffSubscription defaultControllableSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(1),
                                             defaultTariffControllable);
    defaultControllableSub.subscribe(customer.getCustomerInfos().get(1)
            .getPopulation());

//    customer.subscribeDefault();
//    verify(mockTariffMarket).subscribeToTariff(defaultTariff, info, 100);
//    verify(mockTariffMarket).subscribeToTariff(defaultTariffControllable, info2, 200);

//    assertEquals(1,
//                 tariffSubscriptionRepo
//                         .findSubscriptionsForCustomer(customer.getCustomerInfo()
//                                                               .get(0)).size(),
//                   "one subscription for CONSUMPTION customerInfo");
//    assertEquals(1,
//                 tariffSubscriptionRepo
//                         .findSubscriptionsForCustomer(customer.getCustomerInfo()
//                                                               .get(1)).size(),
//                   "one subscription for INTERRUPTIBLE_CONSUMPTION customerInfo");
//
//    assertEquals(mockTariffMarket.getDefaultTariff(customer.getCustomerInfo()
//                         .get(0).getPowerType()),
//                 tariffSubscriptionRepo
//                         .findSubscriptionsForCustomer(customer.getCustomerInfo()
//                                                               .get(0)).get(0)
//                         .getTariff(),
//                   "customer on DefaultTariff");
  }

  @Test
  public void changeSubscription ()
  {
    info = new CustomerInfo("Podunk", 100).withPowerType(PowerType.CONSUMPTION);
    info2 =
      new CustomerInfo("Philby", 200)
              .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);

    customer = new DummyCustomer(info.getName());
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
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(0),
                                             defaultTariff);
    defaultSub.subscribe(customer.getCustomerInfos().get(0).getPopulation());
    TariffSubscription defaultControllableSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(1),
                                             defaultTariffControllable);
    defaultControllableSub.subscribe(customer.getCustomerInfos().get(1)
            .getPopulation());
//    when(
//         mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//            .thenReturn(defaultSub).thenReturn(defaultControllableSub);

    //customer.subscribeDefault();

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
    tariff1.setState(Tariff.State.OFFERED);
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    tariff2.setState(Tariff.State.OFFERED);
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();
    tariff3.setState(Tariff.State.OFFERED);

    assertEquals(5, tariffRepo.findAllTariffs().size(), "Five tariffs");

    // Changing from default to another tariff
    TariffSubscription sub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(0),
                                             tariff1);
    sub.subscribe(customer.getCustomerInfos().get(0).getPopulation());
    TariffSubscription sub2 =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(1),
                                             tariff2);
    sub2.subscribe(customer.getCustomerInfos().get(1).getPopulation());
//    when(
//         mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//            .thenReturn(sub).thenReturn(sub2);
    when(mockTariffMarket.getActiveTariffList(PowerType.CONSUMPTION))
            .thenReturn(tariffRepo.findActiveTariffs(PowerType.CONSUMPTION));
    when(mockTariffMarket.getActiveTariffList(PowerType.INTERRUPTIBLE_CONSUMPTION))
            .thenReturn(tariffRepo.findActiveTariffs(PowerType.INTERRUPTIBLE_CONSUMPTION));

//    customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer
//            .getCustomerInfo().get(0).getPowerType()), customer
//            .getCustomerInfo().get(0));
//    customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer
//            .getCustomerInfo().get(1).getPowerType()), customer
//            .getCustomerInfo().get(1));
//    assertFalse("Changed from default tariff for PowerType CONSUMPTION",
//                tariffSubscriptionRepo
//                        .findSubscriptionsForCustomer(customer.getCustomerInfo()
//                                                              .get(0)).get(1)
//                        .getTariff() == mockTariffMarket
//                        .getDefaultTariff(customer.getCustomerInfo().get(0)
//                                .getPowerType()));
//
//    assertFalse("Changed from default tariff for PowerType INTERRUPTIBLE_CONSUMPTION",
//                tariffSubscriptionRepo
//                        .findSubscriptionsForCustomer(customer.getCustomerInfo()
//                                                              .get(1)).get(1)
//                        .getTariff() == mockTariffMarket
//                        .getDefaultTariff(customer.getCustomerInfo().get(1)
//                                .getPowerType()));

    // Changing back from the new tariff to the default one in order to check
    // every
    // changeSubscription Method
    Tariff lastTariff =
      tariffSubscriptionRepo
              .findSubscriptionsForCustomer(customer.getCustomerInfos().get(0))
              .get(1).getTariff();
    Tariff lastTariff2 =
      tariffSubscriptionRepo
              .findSubscriptionsForCustomer(customer.getCustomerInfos().get(1))
              .get(1).getTariff();

//    when(
//         mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//            .thenReturn(defaultSub).thenReturn(defaultControllableSub);

    defaultSub.subscribe(customer.getCustomerInfos().get(0).getPopulation());
    defaultControllableSub.subscribe(customer.getCustomerInfos().get(1)
            .getPopulation());

    customer.setServiceAccessor(serviceAccessor);
    customer.setTariffMarket(mockTariffMarket);
    customer.changeSubscription(lastTariff,
                                mockTariffMarket.getDefaultTariff(customer
                                        .getCustomerInfos().get(0)
                                        .getPowerType()), customer
                                        .getCustomerInfos().get(0));
    customer.changeSubscription(lastTariff2,
                                mockTariffMarket.getDefaultTariff(customer
                                        .getCustomerInfos().get(1)
                                        .getPowerType()), customer
                                        .getCustomerInfos().get(1));

    assertTrue(tariffSubscriptionRepo
                       .findSubscriptionsForCustomer(customer.getCustomerInfos()
                                                             .get(0)).get(0)
                       .getTariff() == mockTariffMarket
                       .getDefaultTariff(PowerType.CONSUMPTION),
            "Changed to default tariff for CONSUMPTION");

    assertTrue(tariffSubscriptionRepo
                       .findSubscriptionsForCustomer(customer.getCustomerInfos()
                                                             .get(1)).get(0)
                       .getTariff() == mockTariffMarket
                       .getDefaultTariff(PowerType.INTERRUPTIBLE_CONSUMPTION),
            "Changed to default tariff for INTERRUPTIBLE_CONSUMPTION");

    // Last changeSubscription Method checked
//    when(
//         mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//            .thenReturn(sub).thenReturn(sub2);
//    customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer
//                                        .getCustomerInfo().get(0)
//                                        .getPowerType()), lastTariff, 5,
//                                customer.getCustomerInfo().get(0));
//    customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer
//                                        .getCustomerInfo().get(1)
//                                        .getPowerType()), lastTariff2, 5,
//                                customer.getCustomerInfo().get(1));
//
//    assertFalse(tariffSubscriptionRepo
//                        .findSubscriptionsForCustomer(customer.getCustomerInfo()
//                                                              .get(0)).get(1)
//                        .getTariff() == mockTariffMarket
//                        .getDefaultTariff(PowerType.CONSUMPTION),
//                    "Changed from default tariff for CONSUMPTION");
//
//    assertFalse(tariffSubscriptionRepo
//                        .findSubscriptionsForCustomer(customer.getCustomerInfo()
//                                                              .get(1)).get(1)
//                        .getTariff() == mockTariffMarket
//                        .getDefaultTariff(PowerType.INTERRUPTIBLE_CONSUMPTION),
//                    "Changed from default tariff for INTERRUPTIBLE_CONSUMPTION");

  }

  @Test
  public void revokeSubscription ()
  {
    info = new CustomerInfo("Podunk", 100).withPowerType(PowerType.CONSUMPTION);
    info2 =
      new CustomerInfo("Philby", 200)
              .withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);

    customer = new DummyCustomer(info.getName());
    customer.addCustomerInfo(info);
    customer.addCustomerInfo(info2);

    // capture subscription method args
//    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
//    ArgumentCaptor<CustomerInfo> customerArg =
//      ArgumentCaptor.forClass(CustomerInfo.class);
//    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);

    TariffSubscription defaultSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(0),
                                             defaultTariff);
    defaultSub.subscribe(customer.getCustomerInfos().get(0).getPopulation());
    TariffSubscription defaultControllableSub =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(1),
                                             defaultTariffControllable);
    defaultControllableSub.subscribe(customer.getCustomerInfos().get(1)
            .getPopulation());
//    when(
//         mockTariffMarket.subscribeToTariff(tariffArg.capture(),
//                                            customerArg.capture(),
//                                            countArg.capture()))
//            .thenReturn(defaultSub).thenReturn(defaultControllableSub);

    //customer.subscribeDefault();

    assertEquals(1, tariffSubscriptionRepo
            .findSubscriptionsForCustomer(customer.getCustomerInfos().get(0))
            .size(), "one subscription for CONSUMPTION");
    assertEquals(1, tariffSubscriptionRepo
                         .findSubscriptionsForCustomer(customer.getCustomerInfos()
                                                               .get(1)).size(),
            "one subscription for INTERRUPTIBLE_CONSUMPTION");

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
    tariff1.setState(Tariff.State.OFFERED);
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    tariff2.setState(Tariff.State.OFFERED);
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();
    tariff3.setState(Tariff.State.OFFERED);

    assertEquals(5, tariffRepo.findAllTariffs().size(), "Five consumption tariffs");

    TariffSubscription tsd =
      tariffSubscriptionRepo
              .findSubscriptionForTariffAndCustomer(defaultTariff, customer
                      .getCustomerInfos().get(0));
    TariffSubscription tsd2 =
      tariffSubscriptionRepo
              .findSubscriptionForTariffAndCustomer(defaultTariffControllable,
                                                    customer.getCustomerInfos()
                                                            .get(1));
    assertNotNull(tsd, "not null");
    assertNotNull(tsd2, "not null");

    customer.unsubscribe(tsd, 70);
    customer.unsubscribe(tsd2, 70);
    TariffSubscription sub1 =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(0),
                                             tariff1);
    sub1.subscribe(23);
    TariffSubscription sub2 =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(1),
                                             tariff2);
    sub2.subscribe(23);
    TariffSubscription sub3 =
      tariffSubscriptionRepo.getSubscription(customer.getCustomerInfos().get(0),
                                             tariff3);
    sub3.subscribe(24);

    assertEquals(5, tariffSubscriptionRepo
                         .findSubscriptionsForCustomer(customer.getCustomerInfos()
                                                               .get(0)).size()
                         + tariffSubscriptionRepo
                                 .findSubscriptionsForCustomer(customer.getCustomerInfos()
                                                                       .get(1))
                                 .size(),
            "Five subscriptions for customer");

    timeService.setCurrentTime(new Instant(timeService.getCurrentTime()
            .getMillis() + TimeService.HOUR));

    //TariffRevoke tex = new TariffRevoke(tsc2.getBroker(), tsc2);
    tariff2.setState(Tariff.State.KILLED);
    assertTrue(tariff2.isRevoked(), "tariff revoked");

    //TariffRevoke tex2 = new TariffRevoke(tsc3.getBroker(), tsc3);
    tariff3.setState(Tariff.State.KILLED);
    assertTrue(tariff3.isRevoked(), "tariff revoked");

  }

  class ServiceAccessor implements CustomerServiceAccessor
  {

    @Override
    public CustomerRepo getCustomerRepo ()
    {
      return customerRepo;
    }

    @Override
    public RandomSeedRepo getRandomSeedRepo ()
    {
      return null;
    }

    @Override
    public TariffRepo getTariffRepo ()
    {
      return tariffRepo;
    }

    @Override
    public TariffSubscriptionRepo getTariffSubscriptionRepo ()
    {
      return tariffSubscriptionRepo;
    }

    @Override
    public TimeslotRepo getTimeslotRepo ()
    {
      return null;
    }

    @Override
    public TimeService getTimeService()
    {
      return null;
    }

    @Override
    public WeatherReportRepo getWeatherReportRepo ()
    {
      return null;
    }

    @Override
    public ServerConfiguration getServerConfiguration ()
    {
      return null;
    }

    @Override
    public TariffMarket getTariffMarket ()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public XMLMessageConverter getMessageConverter ()
    {
      // TODO Auto-generated method stub
      return null;
    }
  }

  class DummyCustomer extends AbstractCustomer
  {

    public DummyCustomer (String name)
    {
      super(name);
    }

    @Override
    public void step ()
    {
    }

    @Override
    public void initialize ()
    {
    }

    @Override
    public void evaluateTariffs (List<Tariff> tariffs)
    {
    }
    
  }
}