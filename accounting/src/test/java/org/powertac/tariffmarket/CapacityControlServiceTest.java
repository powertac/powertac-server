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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.RegulationAccumulator;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Test cases for CapacityControlService
 * @author John Collins
 */
@SpringJUnitConfig(locations = {"classpath:tariff-test-config.xml"})
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
  
  @BeforeEach
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
    }).when(mockProxy).sendMessage(isA(Broker.class), any());

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
    assertNull(capacityControl.getControlsForTimeslot(0), "no controls ts 0");
    assertNull(capacityControl.getControlsForTimeslot(1), "no controls ts 1");
    capacityControl.postEconomicControl(ece1);
    List<EconomicControlEvent> controls =
        capacityControl.getControlsForTimeslot(0);
    assertNotNull(controls, "some controls ts 0");
    assertEquals(1, controls.size(), "one control");
    assertEquals(ece1, controls.get(0), "correct control");
    assertNull(capacityControl.getControlsForTimeslot(2), "no controls ts 2");
    capacityControl.postEconomicControl(ece3);
    capacityControl.postEconomicControl(ece4);
    controls = capacityControl.getControlsForTimeslot(0);
    assertNotNull(controls, "some controls ts 0");
    assertEquals(1, controls.size(), "one control");
    assertEquals(ece1, controls.get(0), "correct control");
    controls = capacityControl.getControlsForTimeslot(2);
    assertNotNull(controls, "some controls ts 2");
    assertEquals(1, controls.size(), "one control");
    assertEquals(ece3, controls.get(0), "correct control");
    controls = capacityControl.getControlsForTimeslot(3);
    assertNotNull(controls, "some controls ts 3");
    assertEquals(1, controls.size(), "one control");
    assertEquals(ece4, controls.get(0), "correct control");
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
    assertNull(controls, "old timeslot, rejected");
    EconomicControlEvent ece2 = new EconomicControlEvent(spec, 0.21, 1);
    capacityControl.postEconomicControl(ece2);
    controls = capacityControl.getControlsForTimeslot(1);
    assertNotNull(controls, "some controls ts 1");
    assertEquals(1, controls.size(), "one control");
    assertEquals(ece2, controls.get(0), "correct control");
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
    RegulationAccumulator cap = capacityControl.getRegulationCapacity(order); 
    assertEquals(0.4 * 500.0, cap.getUpRegulationCapacity(), 1e-6, "correct up-regulation");
    assertEquals(0.0, cap.getDownRegulationCapacity(), 1e-6, "correct down-regulation");
  }

  // check regulation capacity with regulation-rates
  @Test
  public void regulationCapacityRegRate ()
  {
    TariffSpecification specRR =
      new TariffSpecification(broker, PowerType.THERMAL_STORAGE_CONSUMPTION)
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
    RegulationAccumulator cap = capacityControl.getRegulationCapacity(order); 
    assertEquals(5.0, cap.getUpRegulationCapacity(), 1e-6, "correct up-regulation");
    assertEquals(-2.6, cap.getDownRegulationCapacity(), 1e-6, "correct down-regulation");
  }

  /**
   * Up-regulation test, no regulation rate
   */
  @Test
  public void testExerciseBalancingControlUp ()
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
    RegulationAccumulator cap = capacityControl.getRegulationCapacity(order); 
    assertEquals(0.4 * 500.0, cap.getUpRegulationCapacity(), 1e-6, "correct up-regulation");
    assertEquals(0.0, cap.getDownRegulationCapacity(), 1e-6, "correct down-regulation");
    // exercise the control
    assertEquals(0, msgs.size(), "no messages yet");
    reset(mockAccounting);
    final HashMap<CustomerInfo, Object[]> answers =
        new HashMap<CustomerInfo, Object[]>();
    when(mockAccounting.addRegulationTransaction(any(Tariff.class),
                                             any(CustomerInfo.class),
                                             anyInt(),
                                             anyDouble(),
                                             anyDouble()))
        .thenAnswer(new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) {
            Object[] args = invocation.getArguments();
            CustomerInfo customer = (CustomerInfo)args[1];
            answers.put(customer, args);
            return null;
          }
        });
                                             
    capacityControl.exerciseBalancingControl(order, 100.0, 11.0);
    // check the outgoing message
    assertEquals(1, msgs.size(), "one message");
    assertTrue(msgs.get(0) instanceof BalancingControlEvent, "correct type");
    BalancingControlEvent bce = (BalancingControlEvent)msgs.get(0);
    assertEquals(broker, bce.getBroker(), "correct broker");
    assertEquals(spec.getId(), bce.getTariffId(), "correct tariff");
    assertEquals(100.0, bce.getKwh(), 1e-6, "correct amount");
    assertEquals(11.0, bce.getPayment(), 1e-6, "correct payment");
    assertEquals(0, bce.getTimeslotIndex(), "correct timeslot");
    // Check regulation
    assertEquals(-40.0, sub1.getRegulation(), 1e-6, "correct regulation sub1");
    assertEquals(-60.0, sub2.getRegulation(), 1e-6, "correct regulation sub2");
    // check tariff transactions
    assertEquals(2, answers.size(), "correct # of calls");
    Object[] args = answers.get(customer1);
    assertNotNull(args, "customer1 included");
    assertEquals(5, args.length, "correct arg count");
    assertEquals(tariff, (Tariff) args[0], "correct tariff");
    // customer provided 40, is paid 4.4
    assertEquals(40.0, (Double) args[3], 1e-6, "correct kwh");
    assertEquals(4.4, (Double) args[4], 1e-6, "correct charge");
    
    args = answers.get(customer2);
    assertNotNull(args, "customer2 included");
    assertEquals(5, args.length, "correct arg count");
    assertEquals(tariff, (Tariff) args[0], "correct tariff");
    assertEquals(60.0, (Double) args[3], 1e-6, "correct kwh");
    assertEquals(6.6, (Double) args[4], 1e-6, "correct charge");
    // check for postBalancingControl()
    verify(mockAccounting).postBalancingControl(bce);
  }

  /**
   * down-regulation test
   */
  @Test
  public void testExerciseBalancingControlDown ()
  {
    TariffSpecification specRR1 =
        new TariffSpecification(broker, PowerType.THERMAL_STORAGE_CONSUMPTION)
    .addRate(new Rate().withValue(-0.11))
    .addRate(new RegulationRate().withUpRegulationPayment(0.15)
             .withDownRegulationPayment(-0.05));
    Tariff tariffRR1 = new Tariff(specRR1);
    tariffRR1.init();
    TariffSubscription sub1 =
        tariffSubscriptionRepo.getSubscription(customer1, tariffRR1);
    sub1.subscribe(100);
    sub1.usePower(200); // avail down-regulation is -150
    sub1.setRegulationCapacity(new RegulationCapacity(sub1, 100.0, -150.0));
    TariffSubscription sub2 =
        tariffSubscriptionRepo.getSubscription(customer2, tariffRR1);
    sub2.subscribe(200);
    sub2.usePower(300); // avail down-regulation is -220
    sub2.setRegulationCapacity(new RegulationCapacity(sub2, 100.0, -220.0));
    
    BalancingOrder order = new BalancingOrder(broker, specRR1, -0.2, -0.04);
    RegulationAccumulator cap = capacityControl.getRegulationCapacity(order); 
    assertEquals(200.0, cap.getUpRegulationCapacity(), 1e-6, "correct up-regulation");
    assertEquals(-370.0, cap.getDownRegulationCapacity(), 1e-6, "correct down-regulation");

    // exercise the control
    assertEquals(0, msgs.size(), "no messages yet");
    reset(mockAccounting);
    final HashMap<CustomerInfo, Object[]> answers =
        new HashMap<CustomerInfo, Object[]>();
    when(mockAccounting.addRegulationTransaction(any(Tariff.class),
                                                 any(CustomerInfo.class),
                                                 anyInt(),
                                                 anyDouble(),
                                                 anyDouble()))
    .thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        CustomerInfo customer = (CustomerInfo)args[1];
        answers.put(customer, args);
        return null;
      }
    });

    // Exercise the balancing control with a higher cost and larger
    // ratio than specified by the BalancingOrder.
    capacityControl.exerciseBalancingControl(order, -100.0, -6.0);
    // check the outgoing message
    assertEquals(1, msgs.size(), "one message");
    assertTrue(msgs.get(0) instanceof BalancingControlEvent, "correct type");
    BalancingControlEvent bce = (BalancingControlEvent)msgs.get(0);
    assertEquals(broker, bce.getBroker(), "correct broker");
    assertEquals(specRR1.getId(), bce.getTariffId(), "correct tariff");
    assertEquals(-100.0, bce.getKwh(), 1e-6, "correct amount");
    assertEquals(-6.0, bce.getPayment(), 1e-6, "correct payment");
    assertEquals(0, bce.getTimeslotIndex(), "correct timeslot");
    // Check regulation
    double ratio = 100.0/370.0; //portion of capacity used = 0.270270
    assertEquals(150.0 * ratio, sub1.getRegulation(), 1e-6, "correct regulation -0.4054 sub1");
    assertEquals(220.0 * ratio, sub2.getRegulation(), 1e-6, "correct regulation -0.2973 sub2");
    // check tariff transactions
    assertEquals(2, answers.size(), "correct # of calls");
    Object[] args = answers.get(customer1);
    assertNotNull(args, "customer1 included");
    assertEquals(5, args.length, "correct arg count");
    //assertEquals(TariffTransaction.Type.PRODUCE, (TariffTransaction.Type) args[0],
    //             "correct type");
    assertEquals(tariffRR1, (Tariff) args[0], "correct tariff");
    assertEquals(-150.0 * ratio, (Double) args[3], 1e-6, "correct kwh");
    assertEquals(-0.05 * 150.0 * ratio, (Double) args[4], 1e-6, "correct charge");

    args = answers.get(customer2);
    assertNotNull(args, "customer2 included");
    assertEquals(5, args.length, "correct arg count");
    //assertEquals("correct type", TariffTransaction.Type.CONSUME,
    //             (TariffTransaction.Type) args[0]);
    assertEquals(tariffRR1, (Tariff) args[0], "correct tariff");
    assertEquals(-220.0 * ratio, (Double) args[3], 1e-6, "correct kwh");
    assertEquals(-0.05 * 220.0 * ratio, (Double) args[4], 1e-6, "correct charge");
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

    assertEquals(2, answers.size(), "correct # of calls");
    Object[] args = answers.get(customer1);
    assertNotNull(args, "customer1 included");
    assertEquals(6, args.length, "correct arg count");
    assertEquals(TariffTransaction.Type.CONSUME, (TariffTransaction.Type) args[0], "correct type");
    assertEquals(tariff, (Tariff) args[1], "correct tariff");
    assertEquals(-160.0, (Double) args[4], 1e-6, "correct kwh");
    assertEquals(160.0 * 0.11, (Double) args[5], 1e-6, "correct charge");

    args = answers.get(customer2);
    assertNotNull(args, "customer2 included");
    assertEquals(6, args.length, "correct arg count");
    assertEquals(TariffTransaction.Type.CONSUME, (TariffTransaction.Type) args[0], "correct type");
    assertEquals(tariff, (Tariff) args[1], "correct tariff");
    assertEquals(-240.0, (Double) args[4], 1e-6, "correct kwh");
    assertEquals(240.0 * 0.11, (Double) args[5], 1e-6, "correct charge");
    
    // check regulation info
    assertEquals(40.0, sub1.getRegulation(), 1e-6, "correct regulation sub1");
    assertEquals(60.0, sub2.getRegulation(), 1e-6, "correct regulation sub1");
  }

}
