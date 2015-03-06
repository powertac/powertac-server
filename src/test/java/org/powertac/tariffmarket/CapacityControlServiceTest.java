/*
 * Copyright (c) 2012 by the original author
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
package org.powertac.tariffmarket;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.RegulationRate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.msg.BalancingControlEvent;
import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.msg.EconomicControlEvent;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test cases for CapacityControlService
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:tariff-test-config.xml"})
@DirtiesContext
public class CapacityControlServiceTest
{

  @Autowired
  private BrokerProxy mockProxy;
  
  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  
  @Autowired
  private TimeService timeService;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private Accounting mockAccounting;
  
  @Autowired
  private CapacityControlService capacityControl;
  
  private Instant baseTime;
  private Broker broker;
  private CustomerInfo customer1;
  private CustomerInfo customer2;
  private TariffSpecification spec;
  private Tariff tariff;
  private List<Object> msgs = new ArrayList<Object>();
  
  @Before
  public void setUp () throws Exception
  {
    reset(mockAccounting);
    baseTime = new DateTime(1972, 9, 6, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(baseTime);
    Competition competition =
        Competition.newInstance("tst").withSimulationBaseTime(baseTime);
    capacityControl.initialize(competition, new ArrayList<String>());
    // set up some timeslots
    timeService.setCurrentTime(baseTime);
    timeslotRepo.makeTimeslot(baseTime);
    timeslotRepo.findOrCreateBySerialNumber(10);
    // broker and customers
    broker = new Broker("Jenny");
    customer1 = new CustomerInfo("Podunk", 200).withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
    customer2 = new CustomerInfo("Nowhere", 400).withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
    // a tariff for interruptible consumption
    spec = new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
        .withExpiration(baseTime.plus(TimeService.DAY * 10))
        .withMinDuration(TimeService.DAY * 5)
        .addRate(new Rate().withValue(-0.11).withMaxCurtailment(0.4));
    tariff = new Tariff(spec);
    tariff.init();
    
    // mock the brokerProxyService, capturing the messages sent out
    msgs.clear();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        msgs.add(args[1]);
        return null;
      }
    }).when(mockProxy).sendMessage(isA(Broker.class), anyObject());

  }

  /**
   * Test method for {@link org.powertac.tariffmarket.CapacityControlService#postEconomicControl(org.powertac.common.msg.EconomicControlEvent)}.
   */
  @Test
  public void testPostEconomicControl ()
  {
    EconomicControlEvent ece1 = new EconomicControlEvent(spec, 0.20, 0);
    //EconomicControlEvent ece2 = new EconomicControlEvent(spec, 0.21, 1);
    EconomicControlEvent ece3 = new EconomicControlEvent(spec, 0.22, 2);
    EconomicControlEvent ece4 = new EconomicControlEvent(spec, 0.23, 3);
    assertNull("no controls ts 0", capacityControl.getControlsForTimeslot(0));
    assertNull("no controls ts 1", capacityControl.getControlsForTimeslot(1));
    capacityControl.postEconomicControl(ece1);
    List<EconomicControlEvent> controls =
        capacityControl.getControlsForTimeslot(0);
    assertNotNull("some controls ts 0", controls);
    assertEquals("one control", 1, controls.size());
    assertEquals("correct control", ece1, controls.get(0));
    assertNull("no controls ts 2", capacityControl.getControlsForTimeslot(2));
    capacityControl.postEconomicControl(ece3);
    capacityControl.postEconomicControl(ece4);
    controls = capacityControl.getControlsForTimeslot(0);
    assertNotNull("some controls ts 0", controls);
    assertEquals("one control", 1, controls.size());
    assertEquals("correct control", ece1, controls.get(0));
    controls = capacityControl.getControlsForTimeslot(2);
    assertNotNull("some controls ts 2", controls);
    assertEquals("one control", 1, controls.size());
    assertEquals("correct control", ece3, controls.get(0));
    controls = capacityControl.getControlsForTimeslot(3);
    assertNotNull("some controls ts 3", controls);
    assertEquals("one control", 1, controls.size());
    assertEquals("correct control", ece4, controls.get(0));
  }

  // bogus control for past timeslot
  @Test
  public void postBogusEconomicControl ()
  {
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));
    EconomicControlEvent ece1 = new EconomicControlEvent(spec, 0.20, 0);
    capacityControl.postEconomicControl(ece1);
    List<EconomicControlEvent> controls =
        capacityControl.getControlsForTimeslot(0);
    assertNull("old timeslot, rejected", controls);
    EconomicControlEvent ece2 = new EconomicControlEvent(spec, 0.21, 1);
    capacityControl.postEconomicControl(ece2);
    controls = capacityControl.getControlsForTimeslot(1);
    assertNotNull("some controls ts 1", controls);
    assertEquals("one control", 1, controls.size());
    assertEquals("correct control", ece2, controls.get(0));
  }

  // check regulation capacity
  @Test
  public void regulationCapacity ()
  {
    TariffSubscription sub1 =
        tariffSubscriptionRepo.getSubscription(customer1, tariff);
    sub1.subscribe(100);
    TariffSubscription sub2 =
        tariffSubscriptionRepo.getSubscription(customer2, tariff);
    sub2.subscribe(200);
    sub1.usePower(200);
    sub2.usePower(300);
    BalancingOrder order = new BalancingOrder(broker, spec, 1.0, 0.1);
    RegulationCapacity cap = capacityControl.getRegulationCapacity(order); 
    assertEquals("correct up-regulation", 0.4 * 500.0,
                 cap.getUpRegulationCapacity(), 1e-6);
    assertEquals("correct down-regulation", 0.0,
                 cap.getDownRegulationCapacity(), 1e-6);
  }

  // check regulation capacity with regulation-rates
  @Test
  public void regulationCapacityRegRate ()
  {
    TariffSpecification specRR =
      new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION)
          .withExpiration(baseTime.plus(TimeService.DAY * 10))
          .withMinDuration(TimeService.DAY * 5)
          .addRate(new Rate().withValue(-0.11))
          .addRate(new RegulationRate().withUpRegulationPayment(0.15)
                   .withDownRegulationPayment(-0.05));
    Tariff tariffRR = new Tariff(specRR);
    tariffRR.init();
    TariffSubscription sub1 =
        tariffSubscriptionRepo.getSubscription(customer1, tariffRR);
    sub1.subscribe(100);
    TariffSubscription sub2 =
        tariffSubscriptionRepo.getSubscription(customer2, tariffRR);
    sub2.subscribe(200);
    sub1.setRegulationCapacity(new RegulationCapacity(sub1, 3.0, -1.5));
    sub1.usePower(200);
    sub2.setRegulationCapacity(new RegulationCapacity(sub2, 2.0, -1.1));
    sub2.usePower(300);
    BalancingOrder order = new BalancingOrder(broker, specRR, 1.0, 0.1);
    RegulationCapacity cap = capacityControl.getRegulationCapacity(order); 
    assertEquals("correct up-regulation", 700.0,
                 cap.getUpRegulationCapacity(), 1e-6);
    assertEquals("correct down-regulation", -370.0,
                 cap.getDownRegulationCapacity(), 1e-6);
  }

  /**
   * Test method for {@link org.powertac.tariffmarket.CapacityControlService#exerciseBalancingControl(org.powertac.common.msg.BalancingOrder, double)}.
   */
  @Test
  public void testExerciseBalancingControl ()
  {
    TariffSubscription sub1 =
        tariffSubscriptionRepo.getSubscription(customer1, tariff);
    sub1.subscribe(100);
    TariffSubscription sub2 =
        tariffSubscriptionRepo.getSubscription(customer2, tariff);
    sub2.subscribe(200);
    sub1.usePower(200);
    sub2.usePower(300);
    BalancingOrder order = new BalancingOrder(broker, spec, 1.0, 0.1);
    RegulationCapacity cap = capacityControl.getRegulationCapacity(order); 
    assertEquals("correct up-regulation", 0.4 * 500.0,
                 cap.getUpRegulationCapacity(), 1e-6);
    assertEquals("correct down-regulation", 0.0,
                 cap.getDownRegulationCapacity(), 1e-6);
    // exercise the control
    assertEquals("no messages yet", 0, msgs.size());
    reset(mockAccounting);
    final HashMap<CustomerInfo, Object[]> answers =
        new HashMap<CustomerInfo, Object[]>();
    when(mockAccounting.addTariffTransaction(any(TariffTransaction.Type.class),
                                             any(Tariff.class),
                                             any(CustomerInfo.class),
                                             anyInt(),
                                             anyDouble(),
                                             anyDouble()))
        .thenAnswer(new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) {
            Object[] args = invocation.getArguments();
            CustomerInfo customer = (CustomerInfo)args[2];
            answers.put(customer, args);
            return null;
          }
        });
                                             
    capacityControl.exerciseBalancingControl(order, 100.0, 11.0);
    // check the outgoing message
    assertEquals("one message", 1, msgs.size());
    assertTrue("correct type", msgs.get(0) instanceof BalancingControlEvent);
    BalancingControlEvent bce = (BalancingControlEvent)msgs.get(0);
    assertEquals("correct broker", broker, bce.getBroker());
    assertEquals("correct tariff", spec.getId(), bce.getTariffId());
    assertEquals("correct amount", 100.0, bce.getKwh(), 1e-6);
    assertEquals("correct payment", 11.0, bce.getPayment(), 1e-6);
    assertEquals("correct timeslot", 0, bce.getTimeslotIndex());
    // Check regulation
    assertEquals("correct regulation sub1", 40.0, sub1.getCurtailment(), 1e-6);
    assertEquals("correct regulation sub2", 60.0, sub2.getCurtailment(), 1e-6);
    // check tariff transactions
    assertEquals("correct # of calls", 2, answers.size());
    Object[] args = answers.get(customer1);
    assertNotNull("customer1 included", args);
    assertEquals("correct arg count", 6, args.length);
    assertEquals("correct type", TariffTransaction.Type.PRODUCE,
                 (TariffTransaction.Type) args[0]);
    assertEquals("correct tariff", tariff, (Tariff) args[1]);
    assertEquals("correct kwh", 40.0, (Double) args[4], 1e-6);
    assertEquals("correct charge", -4.4, (Double) args[5], 1e-6);
    
    args = answers.get(customer2);
    assertNotNull("customer2 included", args);
    assertEquals("correct arg count", 6, args.length);
    assertEquals("correct type", TariffTransaction.Type.PRODUCE,
                 (TariffTransaction.Type) args[0]);
    assertEquals("correct tariff", tariff, (Tariff) args[1]);
    assertEquals("correct kwh", 60.0, (Double) args[4], 1e-6);
    assertEquals("correct charge", -6.6, (Double) args[5], 1e-6);
  }

  /**
   * Test method for {@link org.powertac.tariffmarket.CapacityControlService#activate(org.joda.time.Instant, int)}.
   */
  @Test
  public void testActivate ()
  {
    // set up subscriptions
    TariffSubscription sub1 =
        tariffSubscriptionRepo.getSubscription(customer1, tariff);
    sub1.subscribe(100);
    TariffSubscription sub2 =
        tariffSubscriptionRepo.getSubscription(customer2, tariff);
    sub2.subscribe(200);

    // post a couple of economic controls
    EconomicControlEvent ece0 = new EconomicControlEvent(spec, 0.20, 0);
    EconomicControlEvent ece1 = new EconomicControlEvent(spec, 0.25, 1);
    capacityControl.postEconomicControl(ece0);
    capacityControl.postEconomicControl(ece1);

    // activate the capacity control
    capacityControl.activate(timeService.getCurrentTime(), 1);
    
    // capture calls to Accounting
    reset(mockAccounting);
    final HashMap<CustomerInfo, Object[]> answers =
        new HashMap<CustomerInfo, Object[]>();
    when(mockAccounting.addTariffTransaction(any(TariffTransaction.Type.class),
                                             any(Tariff.class),
                                             any(CustomerInfo.class),
                                             anyInt(),
                                             anyDouble(),
                                             anyDouble()))
        .thenAnswer(new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) {
            Object[] args = invocation.getArguments();
            CustomerInfo customer = (CustomerInfo)args[2];
            answers.put(customer, args);
            return null;
          }
        });

    // now the customer model is primed. Both customers should be curtailed
    // by 20% in this timeslot.
    sub1.usePower(200);
    sub2.usePower(300);

    assertEquals("correct # of calls", 2, answers.size());
    Object[] args = answers.get(customer1);
    assertNotNull("customer1 included", args);
    assertEquals("correct arg count", 6, args.length);
    assertEquals("correct type", TariffTransaction.Type.CONSUME,
                 (TariffTransaction.Type) args[0]);
    assertEquals("correct tariff", tariff, (Tariff) args[1]);
    assertEquals("correct kwh", -160.0, (Double) args[4], 1e-6);
    assertEquals("correct charge", 160.0 * 0.11, (Double) args[5], 1e-6);

    args = answers.get(customer2);
    assertNotNull("customer2 included", args);
    assertEquals("correct arg count", 6, args.length);
    assertEquals("correct type", TariffTransaction.Type.CONSUME,
                 (TariffTransaction.Type) args[0]);
    assertEquals("correct tariff", tariff, (Tariff) args[1]);
    assertEquals("correct kwh", -240.0, (Double) args[4], 1e-6);
    assertEquals("correct charge", 240.0 * 0.11, (Double) args[5], 1e-6);
    
    // check regulation info
    assertEquals("correct regulation sub1", 40.0, sub1.getCurtailment(), 1e-6);
    assertEquals("correct regulation sub1", 60.0, sub2.getCurtailment(), 1e-6);
  }

}
