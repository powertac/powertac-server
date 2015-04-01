/*
 * Copyright (c) 2013 by John Collins
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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test cases for TariffEvaluator.
 * 
 * @author John Collins
 */
public class TariffEvaluatorTest
{
  // foundation components
  private Competition competition;
  private TimeService timeService;
  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  private CustomerInfo customer;
  
  // mocks
  private TariffMarket tariffMarket;
  private Accounting accountingService;

  // brokers and tariffs
  private Broker defaultBroker;
  private Tariff defaultConsumption;
  private Tariff defaultProduction;
  private Broker bob;
  private Broker jim;

  // customer model accessor
  private TestAccessor cma;

  // unit under test
  private TariffEvaluator evaluator;

  @Before
  public void setUp () throws Exception
  {
    competition = Competition.newInstance("tariff-evaluator-test");
    timeService = new TimeService();
    timeService.setCurrentTime(competition.getSimulationBaseTime()
                               .plus(TimeService.HOUR * 7));
    //tariffRepo = new TariffRepo();
    tariffSubscriptionRepo = new TariffSubscriptionRepo();

    // set up mocks
    makeMocks();

    // satisfy dependencies
    ReflectionTestUtils.setField(tariffSubscriptionRepo,
                                 "tariffRepo", tariffRepo);
    //ReflectionTestUtils.setField(tariffSubscriptionRepo,
    //                             "tariffMarketService", tariffMarket);

    // set up default tariffs
    defaultBroker = new Broker("default");
    TariffSpecification dcSpec =
            new TariffSpecification(defaultBroker,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.6));
    defaultConsumption = new Tariff(dcSpec);
    initTariff(defaultConsumption);
    when(tariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
        .thenReturn(defaultConsumption);

    TariffSpecification dpSpec =
            new TariffSpecification(defaultBroker,
                                    PowerType.PRODUCTION).
                                    addRate(new Rate().withValue(0.1));
    defaultProduction = new Tariff(dpSpec);
    initTariff(defaultProduction);
    when(tariffMarket.getDefaultTariff(PowerType.PRODUCTION))
        .thenReturn(defaultProduction);

    // other brokers
    jim = new Broker("Jim");
    bob = new Broker("Bob");

    // set up customer
    customer = new CustomerInfo("Guinea Pig", 10000)
      .withMultiContracting(true);
    cma = new TestAccessor();

    // uut setup
    evaluator = new TariffEvaluator(cma).
            withPreferredContractDuration(4).withRationality(0.8);
    ReflectionTestUtils.setField(evaluator,
                                 "tariffRepo", tariffRepo);
    ReflectionTestUtils.setField(evaluator,
                                 "tariffMarket", tariffMarket);
    ReflectionTestUtils.setField(evaluator,
                                 "tariffSubscriptionRepo", tariffSubscriptionRepo);
  }

  private TariffSubscription subscribeTo (Tariff tariff, int count)
  {
    TariffSubscription subscription =
            new TariffSubscription(customer, tariff);
    initSubscription(subscription);
    subscription.subscribe(count);
    tariffSubscriptionRepo.add(subscription);
    return subscription;
  }

  private void makeMocks ()
  {
    tariffRepo = mock(TariffRepo.class);
    tariffMarket = mock(TariffMarket.class);
    accountingService = mock(Accounting.class);
  }
  
  // initializes a tariff. It needs dependencies injected
  private void initTariff (Tariff tariff)
  {
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
  }
  
  // initializes a tariff subscription.
  private void initSubscription (TariffSubscription sub)
  {
    ReflectionTestUtils.setField(sub, "timeService", timeService);
    ReflectionTestUtils.setField(sub, "tariffMarketService", tariffMarket);
    ReflectionTestUtils.setField(sub, "accountingService", accountingService);
  }

  // ------------------------- tests -----------------------------
  /**
   * Test for no new tariffs case.
   */
  @Test
  public void noTariffTest ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    evaluator.withChunkSize(5000); // just two chunks
    evaluator.evaluateTariffs();
  }

  @Test
  public void singleNewTariffConsumption ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    TariffSpecification newTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.59));
    Tariff newTariff = new Tariff(newTS);
    initTariff(newTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(newTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);
    
    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(5000); // just two chunks
    evaluator.evaluateTariffs();
    assertEquals("two tariffs", 2, calls.size());
    assertEquals("-5000 for default",
                 new Integer(-5000), calls.get(defaultConsumption));
    assertEquals("+5000 for new",
                 new Integer(5000), calls.get(newTariff));
  }

  @Test
  public void singleNewTariffSmallChunk ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    TariffSpecification newTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.59));
    Tariff newTariff = new Tariff(newTS);
    initTariff(newTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(newTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);
    
    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(50); // 200 chunks
    evaluator.evaluateTariffs();
    assertEquals("two tariffs", 2, calls.size());
    assertEquals("-5000 for default",
                 new Integer(-5000), calls.get(defaultConsumption));
    assertEquals("+5000 for new",
                 new Integer(5000), calls.get(newTariff));
  }

  @Test
  public void twoTariffSplit ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 5).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    tariffs.add(jimTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);
    
    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(50); // 200 chunks
    evaluator.evaluateTariffs();
    assertEquals("three tariffs", 3, calls.size());
    assertEquals("-10000 for default",
                 new Integer(-10000), calls.get(defaultConsumption));
    assertEquals("+5000 for bob",
                 new Integer(5000), calls.get(bobTariff));
    assertEquals("+5000 for jim",
                 new Integer(5000), calls.get(jimTariff));
  }

  @Test
  public void withdrawCost ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 5).
                                    withEarlyWithdrawPayment(-500.0).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    tariffs.add(jimTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(50); // 200 chunks
    evaluator.evaluateTariffs();
    assertEquals("two tariffs", 2, calls.size());
    assertEquals("-10000 for default",
                 new Integer(-10000), calls.get(defaultConsumption));
    assertEquals("+10000 for bob",
                 new Integer(10000), calls.get(bobTariff));
  }

  @Test
  public void BogusWithdrawCost ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 5).
                                    withEarlyWithdrawPayment(1e20).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(jimTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);
    
    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(50); // 200 chunks
    evaluator.evaluateTariffs();
    assertEquals("two tariffs", 2, calls.size());
    assertEquals("-10000 for default",
                 new Integer(-10000), calls.get(defaultConsumption));
    assertEquals("+10000 for bob",
                 new Integer(10000), calls.get(jimTariff));
  }

  @Test
  public void signupBonus ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    TariffSpecification bobTS =
      new TariffSpecification(bob, PowerType.CONSUMPTION).addRate(new Rate()
          .withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
      new TariffSpecification(jim, PowerType.CONSUMPTION)
          .withSignupPayment(200.0).addRate(new Rate().withValue(-0.4));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    tariffs.add(jimTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(50); // 200 chunks
    evaluator.evaluateTariffs();
    assertEquals("two tariffs", 2, calls.size());
    assertEquals("-10000 for default",
                 new Integer(-10000), calls.get(defaultConsumption));
    assertEquals("+10000 for jim",
                 new Integer(10000), calls.get(jimTariff));
  }

  @Test
  public void signupCharge ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    TariffSpecification bobTS =
      new TariffSpecification(bob, PowerType.CONSUMPTION).addRate(new Rate()
          .withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
      new TariffSpecification(jim, PowerType.CONSUMPTION)
          .withSignupPayment(-200.0).addRate(new Rate().withValue(-0.4));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    tariffs.add(jimTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(50); // 200 chunks
    evaluator.evaluateTariffs();
    assertEquals("two tariffs", 2, calls.size());
    assertEquals("-10000 for default",
                 new Integer(-10000), calls.get(defaultConsumption));
    assertEquals("+10000 for bob",
                 new Integer(10000), calls.get(bobTariff));
  }

  @Test
  public void signupCharge2 ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    TariffSpecification bobTS =
      new TariffSpecification(bob, PowerType.CONSUMPTION).addRate(new Rate()
          .withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
      new TariffSpecification(jim, PowerType.CONSUMPTION)
          .withSignupPayment(-15.0).addRate(new Rate().withValue(-0.2));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    tariffs.add(jimTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {10.0, 20.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(50); // 200 chunks
    evaluator.evaluateTariffs();
    assertEquals("two tariffs", 2, calls.size());
    assertEquals("-10000 for default",
                 new Integer(-10000), calls.get(defaultConsumption));
    assertEquals("+10000 for bob",
                 new Integer(10000), calls.get(bobTariff));
  }

  @Test
  public void signupCharge3 ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    TariffSpecification bobTS =
      new TariffSpecification(bob, PowerType.CONSUMPTION).addRate(new Rate()
          .withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
      new TariffSpecification(jim, PowerType.CONSUMPTION)
          .withSignupPayment(-15.0).addRate(new Rate().withValue(-0.2));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    tariffs.add(jimTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {2.0, 4.0, 2.0, 4.0, 2.0, 4.0, 2.0, 4.0, 2.0, 4.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(50); // 200 chunks
    evaluator.evaluateTariffs();
    assertEquals("two tariffs", 2, calls.size());
    assertEquals("-10000 for default",
                 new Integer(-10000), calls.get(defaultConsumption));
    assertEquals("+10000 for bob",
                 new Integer(10000), calls.get(bobTariff));
  }

  @Test
  public void twoTariffInertia ()
  {
    // do two evals to bump up inertia
    this.noTariffTest();
    evaluator.evaluateTariffs();

    // inertia should now be 0.4
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 5).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    tariffs.add(jimTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);
    cma.setInertiaSamples(0.35, 0.45); // half should skip

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(50); // 200 chunks
    evaluator.evaluateTariffs();
    assertEquals("three tariffs", 3, calls.size());
    assertEquals("-5000 for default",
                 new Integer(-5000), calls.get(defaultConsumption));
    assertEquals("+2500 for bob",
                 new Integer(2500), calls.get(bobTariff));
    assertEquals("+2500 for jim",
                 new Integer(2500), calls.get(jimTariff));
  }

  // Test min contract duration. Two tariffs from Jim have equal signup
  // bonus and withdrawal payment, but one has a minDuration half the
  // customer's preferred duration. This one should get all the action.
  @Test
  public void minDurationTest ()
  {
    subscribeTo(defaultConsumption, customer.getPopulation());
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 2).
                                    withEarlyWithdrawPayment(-50.0).
                                    withSignupPayment(50.0).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    TariffSpecification jimLong =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 4).
                                    withEarlyWithdrawPayment(-50.0).
                                    withSignupPayment(50.0).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimTariffL = new Tariff(jimLong);
    initTariff(jimTariffL);
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    tariffs.add(jimTariff);
    tariffs.add(jimTariffL);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(50); // 200 chunks
    evaluator.evaluateTariffs();
    assertEquals("two changes", 2, calls.size());
    assertEquals("-10000 for default",
                 new Integer(-10000), calls.get(defaultConsumption));
    assertEquals("+10000 for jim-long",
                 new Integer(10000), calls.get(jimTariff));
  }

  // Revoke to default tariff
  @Test
  public void revokeToDefault ()
  {
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 5).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.4, 0.6);

    // distribute all customers across jim & bob
    subscribeTo(bobTariff, 5000);
    subscribeTo(jimTariff, 5000);

    // revoke Jim's tariff - should move everyone to Bob
    jimTariff.setState(Tariff.State.KILLED);
    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    evaluator.evaluateTariffs();
    assertEquals("two calls", 2, calls.size());
    assertEquals("-5000 for jim",
                 new Integer(-5000), calls.get(jimTariff));
    assertEquals("none for default", null, calls.get(defaultConsumption));
    assertEquals("+5000 for bob",
                 new Integer(5000), calls.get(bobTariff));
  }

  // Revoke to superseding tariff
  @Test
  public void revokeSuperseding ()
  {
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 5).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.45, 0.55);

    // distribute all customers across jim & bob
    subscribeTo(bobTariff, 5000);
    subscribeTo(jimTariff, 5000);

    // revoke Jim's tariff, supersede it
    jimTariff.setState(Tariff.State.KILLED);
    TariffSpecification jimSTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 5).
                                    addSupersedes(jimTariff.getId()).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimSuper = new Tariff(jimSTS);
    when(tariffRepo.findTariffById(jimTariff.getId()))
        .thenReturn(jimTariff);
    initTariff(jimSuper);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    tariffs.add(jimSuper);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    evaluator.evaluateTariffs();
    assertEquals("three calls", 3, calls.size());
    assertEquals("-5000 for jim",
                 new Integer(-5000), calls.get(jimTariff));
    assertEquals("+2500 for jimSuper",
                 new Integer(2500), calls.get(jimSuper));
    assertEquals("+2500 for bob",
                 new Integer(2500), calls.get(bobTariff));
  }

  // Revoke to superseding tariff
  @Test
  public void revokeToKilledSuperseding ()
  {
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 5).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);

    double[] profile = {1.0, 2.0};
    cma.capacityProfile = profile;
    cma.setChoiceSamples(0.45, 0.55);

    // distribute all customers across jim & bob
    subscribeTo(bobTariff, 5000);
    subscribeTo(jimTariff, 5000);

    // revoke Jim's tariff, supersede it
    jimTariff.setState(Tariff.State.KILLED);
    TariffSpecification jimSTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(TimeService.DAY * 5).
                                    addSupersedes(jimTariff.getId()).
                                    addRate(new Rate().withValue(-0.4));
    Tariff jimSuper = new Tariff(jimSTS);
    when(tariffRepo.findTariffById(jimTariff.getId()))
        .thenReturn(jimTariff);
    initTariff(jimSuper);
    // Revoke the superseding tariff
    jimSuper.setState(Tariff.State.KILLED);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    tariffs.add(bobTariff);
    //tariffs.add(jimSuper);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
        .thenReturn(tariffs);

    evaluator.evaluateTariffs();
    assertEquals("two calls", 2, calls.size());
    assertEquals("-5000 for jim",
                 new Integer(-5000), calls.get(jimTariff));
    assertEquals("+5000 for defaultConsumption",
                 new Integer(5000), calls.get(bobTariff));
  }

  // Revoke to better tariff

  // Revoke to superseding tariff, with inertia

  // --------------- model accessor ------------------------------------
  class TestAccessor implements CustomerModelAccessor
  {
    // values to return
    double[] capacityProfile;
    double brokerSwitchFactor = 0.05;
    
    double[] choiceSamples = {0.5};
    int choicePtr = 0;
    double[] inertiaSamples = {0.5};
    int inertiaPtr = 0;
    
    TestAccessor ()
    {
      super();
    }
    
    @Override
    public CustomerInfo getCustomerInfo ()
    {
      return customer;
    }

    @Override
    public double[] getCapacityProfileStartingNextTimeSlot (Tariff tariff)
    {
      return capacityProfile;
    }

    @Override
    public double getBrokerSwitchFactor (boolean isSuperseding)
    {
      double multiplier = isSuperseding? 0.0: 1.0;
      return multiplier * brokerSwitchFactor;
    }

    @Override
    public double getTariffChoiceSample ()
    {
      if (choicePtr >= choiceSamples.length)
        choicePtr = 0;
      return choiceSamples[choicePtr++];
    }

    @Override
    public double getInertiaSample ()
    {
      if (inertiaPtr >= inertiaSamples.length)
        inertiaPtr = 0;
      return inertiaSamples[inertiaPtr++];
    }
    
    // sets the choice sequence
    void setChoiceSamples (double... samples)
    {
      choiceSamples = samples;
    }
    
    // sets the inertia sequence
    void setInertiaSamples (double... samples)
    {
      inertiaSamples = samples;
    }

    @Override
    public double getShiftingInconvenienceFactor(Tariff tariff) {
      // TODO Auto-generated method stub
      return 0;
    }
  }
}
