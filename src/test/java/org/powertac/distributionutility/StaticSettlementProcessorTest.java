package org.powertac.distributionutility;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalMatchers.eq;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Broker;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.repo.TariffRepo;

public class StaticSettlementProcessorTest
{
  TariffRepo tariffRepo;
  CapacityControl capacityControlService;
  SettlementContext context;

  double pplus = 0.0;
  double pplusPrime = 0.0;
  double pminus = 0.0;
  double pminusPrime = 0.0;
  
  Broker b1, b2, b3, b4;
  TariffSpecification spec1, spec2, spec3, spec4;
  
  StaticSettlementProcessor uut;
  ArrayList<ChargeInfo> brokerData;

  @Before
  public void setUp () throws Exception
  {
    tariffRepo = new TariffRepo();
    capacityControlService = mock(CapacityControl.class);
    context = new MockSettlementContext();
    uut = new StaticSettlementProcessor(tariffRepo, capacityControlService);
    b1 = new Broker("A1");
    b2 = new Broker("A2");
    b3 = new Broker("A3");
    b4 = new Broker("A4");
    brokerData = new ArrayList<ChargeInfo>();
    pplus = 0.06;
    //pplusPrime = 0.00001;
    pminus = 0.015;
    //pminusPrime = 0.00001;
    spec1 = new TariffSpecification(b1, PowerType.INTERRUPTIBLE_CONSUMPTION);
    Rate rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec1.addRate(rate);
    Tariff tariff = new Tariff(spec1);
    tariffRepo.addTariff(tariff);
    spec2 = new TariffSpecification(b2, PowerType.INTERRUPTIBLE_CONSUMPTION);
    rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec2.addRate(rate);
    tariff = new Tariff(spec2);
    tariffRepo.addTariff(tariff);
    spec3 = new TariffSpecification(b3, PowerType.INTERRUPTIBLE_CONSUMPTION);
    rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec3.addRate(rate);
    tariff = new Tariff(spec3);
    tariffRepo.addTariff(tariff);
    spec4 = new TariffSpecification(b4, PowerType.INTERRUPTIBLE_CONSUMPTION);
    rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec4.addRate(rate);
    tariff = new Tariff(spec4);
    tariffRepo.addTariff(tariff);
  }

  // Example 1 Niels/John, no balancing orders
  @Test
  public void ex1_noBO ()
  {
    ChargeInfo ci1 = new ChargeInfo(b1, -1.5);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, -1.5);
    brokerData.add(ci2);
    pplus = 3.0;
    pplusPrime = 1.0;
    pminus = 1.0;
    pminusPrime = -1.0;
    uut.settle(context, brokerData);
    assertEquals("b1 pays 9", -9.0, ci1.getBalanceCharge(), 1e-6);
    assertEquals("b2 pays 9", -9.0, ci2.getBalanceCharge(), 1e-6);
  }

  // Example 1 spec, slope = 0
  @Test
  public void ex1 ()
  {
    BalancingOrder bo1 = new BalancingOrder(b1, spec1, 0.6, 0.03);
    tariffRepo.addBalancingOrder(bo1);
    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(3.5);

    BalancingOrder bo2 = new BalancingOrder(b3, spec3, 0.6, 0.042);
    tariffRepo.addBalancingOrder(bo2);
    when(capacityControlService.getCurtailableUsage(bo2)).thenReturn(2.0);

    BalancingOrder bo3 = new BalancingOrder(b2, spec2, 0.6, 0.051);
    tariffRepo.addBalancingOrder(bo3);
    when(capacityControlService.getCurtailableUsage(bo3)).thenReturn(6.7);

    BalancingOrder bo4 = new BalancingOrder(b3, spec3, 0.6, 0.062);
    tariffRepo.addBalancingOrder(bo4);
    when(capacityControlService.getCurtailableUsage(bo4)).thenReturn(3.9);

    BalancingOrder bo5 = new BalancingOrder(b2, spec2, 0.6, 0.08);
    tariffRepo.addBalancingOrder(bo5);
    when(capacityControlService.getCurtailableUsage(bo5)).thenReturn(3.0);

    BalancingOrder bo6 = new BalancingOrder(b1, spec1, 0.6, 0.091);
    tariffRepo.addBalancingOrder(bo6);
    when(capacityControlService.getCurtailableUsage(bo6)).thenReturn(6.2);

    ChargeInfo ci1 = new ChargeInfo(b1, 0);
    ci1.addBalancingOrder(bo1);
    ci1.addBalancingOrder(bo6);
    brokerData.add(ci1);

    ChargeInfo ci2 = new ChargeInfo(b2, 4);
    ci2.addBalancingOrder(bo3);
    ci2.addBalancingOrder(bo5);
    brokerData.add(ci2);
    
    ChargeInfo ci3 = new ChargeInfo(b3, -8);
    ci3.addBalancingOrder(bo2);
    ci3.addBalancingOrder(bo4);
    brokerData.add(ci3);
    
    ChargeInfo ci4 = new ChargeInfo(b4, -14);
    brokerData.add(ci4);
    
    pplus = 0.1;
    pplusPrime = 0.0;
    pminus = 1.0;
    pminusPrime = 0.0;
    uut.settle(context, brokerData);

    //  (broker, imbalance, p_1,    p_2
    //    1:       0:       0.0:    0.218
    //    2:       4:       0.2622: 0.8042
    //    3:      -8:      -0.8:    0.5248
    //    4:     -14:      -1.4:    0.0
    
    assertEquals("b1.p2 = 0.328",   0.328, ci1.getBalanceChargeP2(), 1e-6);
    assertEquals("b2.p2 = 0.8042", 0.8042, ci2.getBalanceChargeP2(), 1e-6);
    assertEquals("b3.p2 = 0.5248", 0.5248, ci3.getBalanceChargeP2(), 1e-6);
    assertEquals("b4.p2 = 0",         0.0, ci4.getBalanceChargeP2(), 1e-6);

    //System.out.println("P1 values (b1,b2,b3,b4): ("
    //                   + ci1.getBalanceChargeP1()
    //                   + "," + ci2.getBalanceChargeP1()
    //                   + "," + ci3.getBalanceChargeP1()
    //                   + "," + ci4.getBalanceChargeP1()
    //                   + ")");
    assertEquals("b1.p1 = 0", 0.0, ci1.getBalanceChargeP1(), 1e-4);
    assertEquals("b2.p1 = 0.4", 0.4, ci2.getBalanceChargeP1(), 1e-4);
    assertEquals("b3.p1 = -0.8", -0.8, ci3.getBalanceChargeP1(), 1e-4);
    assertEquals("b4.p1 = -1.4", -1.4, ci4.getBalanceChargeP1(), 1e-4);
  }

  //problem 3 from https://docs.google.com/spreadsheet/ccc?key=0AnOwYcSnDi0ZdDVNWjN4Q1FRTGdUTHhSLW9WVmF5Snc
  @Test
  public void ex_prob3 ()
  {
    Broker b0 = new Broker("A0");
    TariffSpecification spec0 =
            new TariffSpecification(b0, PowerType.INTERRUPTIBLE_CONSUMPTION);
    Rate rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec0.addRate(rate);
    Tariff tariff = new Tariff(spec0);
    tariffRepo.addTariff(tariff);

    BalancingOrder bo1 = new BalancingOrder(b0, spec0, 0.6, 0.003);
    tariffRepo.addBalancingOrder(bo1);
    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(35.0);

    BalancingOrder bo2 = new BalancingOrder(b0, spec0, 0.6, 0.0091);
    tariffRepo.addBalancingOrder(bo2);
    when(capacityControlService.getCurtailableUsage(bo2)).thenReturn(62.0);

    BalancingOrder bo3 = new BalancingOrder(b1, spec1, 0.6, 0.0051);
    tariffRepo.addBalancingOrder(bo3);
    when(capacityControlService.getCurtailableUsage(bo3)).thenReturn(67.0);

    BalancingOrder bo4 = new BalancingOrder(b1, spec1, 0.6, 0.008);
    tariffRepo.addBalancingOrder(bo4);
    when(capacityControlService.getCurtailableUsage(bo4)).thenReturn(30.0);

    BalancingOrder bo5 = new BalancingOrder(b2, spec2, 0.6, 0.0042);
    tariffRepo.addBalancingOrder(bo5);
    when(capacityControlService.getCurtailableUsage(bo5)).thenReturn(20.0);

    BalancingOrder bo6 = new BalancingOrder(b2, spec2, 0.6, 0.0062);
    tariffRepo.addBalancingOrder(bo6);
    when(capacityControlService.getCurtailableUsage(bo6)).thenReturn(39.0);

    ChargeInfo ci0 = new ChargeInfo(b0, 0);
    ci0.addBalancingOrder(bo1);
    ci0.addBalancingOrder(bo2);
    brokerData.add(ci0);

    ChargeInfo ci1 = new ChargeInfo(b1, 40);
    ci1.addBalancingOrder(bo3);
    ci1.addBalancingOrder(bo4);
    brokerData.add(ci1);

    ChargeInfo ci2 = new ChargeInfo(b2, -80);
    ci2.addBalancingOrder(bo5);
    ci2.addBalancingOrder(bo6);
    brokerData.add(ci2);

    ChargeInfo ci3 = new ChargeInfo(b3, -140);
    brokerData.add(ci3);


    pplus = 0.01;
    pplusPrime = 0.001;
    pminus = 1.0;
    pminusPrime = 0.0;
    uut.settle(context, brokerData);

    // (broker  p_1,       p_2,       operating cost, utility)
    //   0      0          -0.628     0.105           0.523
    //   1      -2.748222  -1.1042    0.4937          3.358722
    //   2      8.04        -0.5248   0.3258          -7.841
    //   3      9.618778    0         0               -9.618778
    //   

    System.out.println("P2 values (b0,b1,b2,b3,): ("
            + ci0.getBalanceChargeP2()
            + "," + ci1.getBalanceChargeP2()
            + "," + ci2.getBalanceChargeP2()
            + "," + ci3.getBalanceChargeP2()
            + ")");
    assertEquals("b0.p2 = 0.628",   0.628, ci0.getBalanceChargeP2(), 1e-6);
    assertEquals("b1.p2 = 1.1042",   1.1042, ci1.getBalanceChargeP2(), 1e-6);
    assertEquals("b2.p2 = 0.5248", 0.5248, ci2.getBalanceChargeP2(), 1e-6);
    assertEquals("b3.p2 = 0", 0, ci3.getBalanceChargeP2(), 1e-6);

    System.out.println("P1 values (b0,b1,b2,b3,): ("
            + ci0.getBalanceChargeP1()
            + "," + ci1.getBalanceChargeP1()
            + "," + ci2.getBalanceChargeP1()
            + "," + ci3.getBalanceChargeP1()
            + ")");
    assertEquals("b0.p1 = 0", 0, ci0.getBalanceChargeP1(), 1e-4);
    assertEquals("b1.p1 = 2.748222", 2.748222, ci1.getBalanceChargeP1(), 1e-4);
    assertEquals("b2.p1 = -8.04", -8.04, ci2.getBalanceChargeP1(), 1e-4);
    assertEquals("b3.p1 = -9.618778", -9.618778, ci3.getBalanceChargeP1(), 1e-4);
  }

  // Example from spec
//  @Test
//  public void exSpec ()
//  {
//    // first, we need a third broker
//    Broker b3 = new Broker("Sara");
//    TariffSpecification spec3 =
//            new TariffSpecification(b3, PowerType.INTERRUPTIBLE_CONSUMPTION);
//    Rate rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
//    spec3.addRate(rate);
//    Tariff tariff = new Tariff(spec3);
//    tariffRepo.addTariff(tariff);
//    
//    // next, the balancing orders
//    BalancingOrder bo1 = new BalancingOrder(b1, spec1, 0.6, 0.03);
//    tariffRepo.addBalancingOrder(bo1);
//    BalancingOrder bo2 = new BalancingOrder(b3, spec3, 0.6, 0.042);
//    tariffRepo.addBalancingOrder(bo2);
//    BalancingOrder bo3 = new BalancingOrder(b2, spec2, 0.6, 0.051);
//    tariffRepo.addBalancingOrder(bo3);
//    BalancingOrder bo4 = new BalancingOrder(b3, spec3, 0.6, 0.062);
//    tariffRepo.addBalancingOrder(bo4);
//    BalancingOrder bo5 = new BalancingOrder(b2, spec2, 0.6, 0.08);
//    tariffRepo.addBalancingOrder(bo5);
//    BalancingOrder bo6 = new BalancingOrder(b1, spec1, 0.6, 0.091);
//    
//    // ChargeInfo instances give the imbalance values
//    ChargeInfo ci1 = new ChargeInfo(b1, 4.0);
//    ci1.addBalancingOrder(bo1);
//    ci1.addBalancingOrder(bo6);
//    brokerData.add(ci1);
//    ChargeInfo ci2 = new ChargeInfo(b2, -8.0);
//    ci2.addBalancingOrder(bo3);
//    ci2.addBalancingOrder(bo5);
//    brokerData.add(ci2);
//    ChargeInfo ci3 = new ChargeInfo(b3, -14.0);
//    ci3.addBalancingOrder(bo2);
//    ci3.addBalancingOrder(bo4);
//    brokerData.add(ci3);
//    
//    // Regulating market data
//    pplus = 0.1;
//    pplusPrime = 0.01;
//    pminus = 0.05;
//    pminusPrime = -0.005;
//
//    // Balancing capacity
//    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(3.5);
//    when(capacityControlService.getCurtailableUsage(bo2)).thenReturn(2.0);
//    when(capacityControlService.getCurtailableUsage(bo3)).thenReturn(6.7);
//    when(capacityControlService.getCurtailableUsage(bo4)).thenReturn(3.9);
//    when(capacityControlService.getCurtailableUsage(bo5)).thenReturn(3.0);
//    when(capacityControlService.getCurtailableUsage(bo6)).thenReturn(6.2);
//
//    // Run the market and check results
//    uut.settle(context, brokerData);
//    System.out.println(ci1.toString());
//    assertEquals("b1 pays -1.118",  1.1180, ci1.getBalanceCharge(), 1e-4);
//    assertEquals("b2 pays 1.2001", -1.2001, ci2.getBalanceCharge(), 1e-4);
//    assertEquals("b3 pays 1.6704", -1.6704, ci3.getBalanceCharge(), 1e-4);
//  }
  
  // Simple balancing, no imbalance
  @Test
  public void testSettle0 ()
  {
    ChargeInfo ci1 = new ChargeInfo(b1, 0.0);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 0.0);
    brokerData.add(ci2);
    uut.settle(context, brokerData);
    assertEquals("no charge for b1", 0.0, ci1.getBalanceCharge(), 1e-6);
    assertEquals("no charge for b2", 0.0, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, no net imbalance
  @Test
  public void testSettleNoNet ()
  {
    ChargeInfo ci1 = new ChargeInfo(b1, -10.0);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 10.0);
    brokerData.add(ci2);
    uut.settle(context, brokerData);
    assertEquals("-0.6 for b1", -0.6, ci1.getBalanceCharge(), 1e-6);
    assertEquals("0.15 for b2", 0.15, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, no net imbalance, slope != 0.0
  @Test
  public void testSettleNoNetA ()
  {
    pplusPrime = 0.01;
    pminusPrime = -0.01;
    ChargeInfo ci1 = new ChargeInfo(b1, -10.0);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 10.0);
    brokerData.add(ci2);
    uut.settle(context, brokerData);
    assertEquals("-0.6 for b1", -0.6, ci1.getBalanceCharge(), 1e-6);
    assertEquals("0.15 for b2", 0.15, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, negative net imbalance, no slope
  @Test
  public void testSettleNetNegZ ()
  {
    pplusPrime = 0.0;
    pminusPrime = 0.0;
    ChargeInfo ci1 = new ChargeInfo(b1, -20.0);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 10.0);
    brokerData.add(ci2);
    uut.settle(context, brokerData);
    assertEquals("-1.2 for b1", -1.2, ci1.getBalanceCharge(), 1e-6);
    assertEquals("0.6 for b2", 0.6, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, net imbalance
  @Test
  public void testSettleNetNeg ()
  {
    pplusPrime = 0.00005;
    pminusPrime = -0.00005;
    ChargeInfo ci1 = new ChargeInfo(b1, -20.0);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 10.0);
    brokerData.add(ci2);
    uut.settle(context, brokerData);
    assertEquals("-1.21 for b1", -1.21, ci1.getBalanceCharge(), 1e-6);
    assertEquals("0.605 for b2", 0.605, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, positive net imbalance
  @Test
  public void testSettleNetPos ()
  {
    ChargeInfo ci1 = new ChargeInfo(b1, -10.0);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 20.0);
    brokerData.add(ci2);
    uut.settle(context, brokerData);
    assertEquals("-.15 for b1", -0.15, ci1.getBalanceCharge(), 1e-6);
    assertEquals("0.3 for b2", 0.3, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for b1
  @Test
  public void testSingleBO ()
  {
    pplusPrime = 0.00005;
    pminusPrime = -0.00005;
    BalancingOrder bo1 = new BalancingOrder(b1, spec1, 0.6, 0.05);
    tariffRepo.addBalancingOrder(bo1);
    ChargeInfo ci1 = new ChargeInfo(b1, -20.0);
    ci1.addBalancingOrder(bo1);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 10.0);
    brokerData.add(ci2);
    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(5.0);
    uut.settle(context, brokerData);
    verify(capacityControlService).exerciseBalancingControl(bo1, 5.0, 0.300625);
    assertEquals("-1.21 for b1", -1.21, ci1.getBalanceChargeP1(), 1e-6);
    assertEquals(".6019 for b2", 0.601875, ci2.getBalanceChargeP1(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for b1
  // that is not exercised - price is too high
  @Test
  public void testSingleBO_NoExercise ()
  {
    pplusPrime = 0.00001;
    pminusPrime = -0.00001;
    BalancingOrder bo1 = new BalancingOrder(b1, spec1, 0.6, 0.061);
    tariffRepo.addBalancingOrder(bo1);
    ChargeInfo ci1 = new ChargeInfo(b1, -20.0);
    ci1.addBalancingOrder(bo1);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 10.0);
    brokerData.add(ci2);
    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(5.0);
    uut.settle(context, brokerData);
    verify(capacityControlService, never()).exerciseBalancingControl(isA(BalancingOrder.class), anyDouble(), anyDouble());
    assertEquals("-1.202 for b1", -1.202, ci1.getBalanceCharge(), 1e-6);
    assertEquals(".61 for b2", 0.61, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for b1
  @Test
  public void testSingleBO_HighCapacity ()
  {
    pplusPrime = 0.00001;
    pminusPrime = -0.00001;
    TariffSpecification spec =
            new TariffSpecification(b1, PowerType.INTERRUPTIBLE_CONSUMPTION);
    Rate rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec.addRate(rate);
    Tariff tariff = new Tariff(spec);
    tariffRepo.addTariff(tariff);
    BalancingOrder bo1 = new BalancingOrder(b1, spec, 0.6, 0.05);
    tariffRepo.addBalancingOrder(bo1);
    ChargeInfo ci1 = new ChargeInfo(b1, -20.0);
    ci1.addBalancingOrder(bo1);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 10.0);
    brokerData.add(ci2);
    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(15.0);
    uut.settle(context, brokerData);
    verify(capacityControlService).exerciseBalancingControl(bo1, 10.0, 0.6005);
    assertEquals("-1.202 for b1", -1.202, ci1.getBalanceChargeP1(), 1e-6);
    assertEquals(".6005 for b2", 0.6005, ci2.getBalanceChargeP1(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for each broker
  @Test
  public void test2BO_LowCapacity ()
  {
    pplusPrime = 0.00001;
    pminusPrime = -0.00001;
    BalancingOrder bo1 = new BalancingOrder(b1, spec1, 0.6, 0.04);
    tariffRepo.addBalancingOrder(bo1);
    BalancingOrder bo2 = new BalancingOrder(b2, spec2, 0.6, 0.05);
    tariffRepo.addBalancingOrder(bo2);
    
    ChargeInfo ci1 = new ChargeInfo(b1, -20.0);
    ci1.addBalancingOrder(bo1);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, -10.0);
    ci2.addBalancingOrder(bo2);
    brokerData.add(ci2);
    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(10.0);
    when(capacityControlService.getCurtailableUsage(bo2)).thenReturn(5.0);
    
    uut.settle(context, brokerData);
    verify(capacityControlService).exerciseBalancingControl(bo1, 10.0, 0.6005);
    verify(capacityControlService).exerciseBalancingControl(bo2, 5.0, 0.300125);
    assertEquals("-1.20425 for b1", -1.20425, ci1.getBalanceChargeP1(), 1e-6);
    assertEquals(".6015 for b2", -0.6015, ci2.getBalanceChargeP1(), 1e-4);
  }

  // Simple balancing, net imbalance, single balancing order for each broker
  @Test
  public void test2BO_HighCapacity ()
  {
    pplusPrime = 0.00001;
    pminusPrime = -0.00001;
    BalancingOrder bo1 = new BalancingOrder(b1, spec1, 0.6, 0.04);
    tariffRepo.addBalancingOrder(bo1);
    BalancingOrder bo2 = new BalancingOrder(b2, spec2, 0.6, 0.05);
    tariffRepo.addBalancingOrder(bo2);
    
    ChargeInfo ci1 = new ChargeInfo(b1, -20.0);
    ci1.addBalancingOrder(bo1);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, -10.0);
    ci2.addBalancingOrder(bo2);
    brokerData.add(ci2);
    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(16.0);
    when(capacityControlService.getCurtailableUsage(bo2)).thenReturn(16.0);
    
    uut.settle(context, brokerData);
    verify(capacityControlService).exerciseBalancingControl(eq(bo1), 
                                                            eq(16.0, 1e-6),
                                                            eq(0.94098, 1e-6));
    verify(capacityControlService).exerciseBalancingControl(bo2, 14.0, 0.84098);
    assertEquals("-1.20216 for b1", -1.20216, ci1.getBalanceChargeP1(), 1e-5);
    assertEquals("-0.60108 for b2", -0.60108, ci2.getBalanceChargeP1(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for each broker
  @Test
  public void test3BO_LowCapacity ()
  {
    pplusPrime = 0.00001;
    pminusPrime = -0.00001;
    BalancingOrder bo1 = new BalancingOrder(b1, spec1, 0.6, 0.04);
    tariffRepo.addBalancingOrder(bo1);
    
    TariffSpecification spec1a =
            new TariffSpecification(b1, PowerType.INTERRUPTIBLE_CONSUMPTION);
    Rate rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec1a.addRate(rate);
    Tariff tariff1a = new Tariff(spec1a);
    tariffRepo.addTariff(tariff1a);
    BalancingOrder bo1a = new BalancingOrder(b1, spec1a, 0.6, 0.045);
    tariffRepo.addBalancingOrder(bo1a);
    
    BalancingOrder bo2 = new BalancingOrder(b2, spec2, 0.6, 0.05);
    tariffRepo.addBalancingOrder(bo2);
    
    ChargeInfo ci1 = new ChargeInfo(b1, -20.0);
    ci1.addBalancingOrder(bo1);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, -10.0);
    ci2.addBalancingOrder(bo2);
    brokerData.add(ci2);
    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(10.0);
    when(capacityControlService.getCurtailableUsage(bo1a)).thenReturn(0.0);
    when(capacityControlService.getCurtailableUsage(bo2)).thenReturn(5.0);
    
    uut.settle(context, brokerData);
    //System.out.println("(b1.p2,b2.p2) = ("
    //        + ci1.getBalanceChargeP2()
    //        + "," + ci2.getBalanceChargeP2() + ")");
    //System.out.println("(b1.p1,b2.p1) = ("
    //        + ci1.getBalanceChargeP1()
    //        + "," + ci2.getBalanceChargeP1() + ")");
    verify(capacityControlService).exerciseBalancingControl(bo1, 10.0, 0.6005);
    verify(capacityControlService).exerciseBalancingControl(bo2, 5.0, 0.300125);
    assertEquals("-1.20425 for b1", -1.20425, ci1.getBalanceChargeP1(), 1e-6);
    assertEquals("-0.6015 for b2", -0.6015, ci2.getBalanceChargeP1(), 1e-6);
  }

  // --------------------------------------------------------

  class MockSettlementContext implements SettlementContext
  {
    @Override
    public double getPPlus ()
    {
      return pplus;
    }

    @Override
    public double getPMinus ()
    {
      return pminus;
    }

    @Override
    public Double getBalancingCost ()
    {
      return 0.0;
    }

    @Override
    public double getPPlusPrime ()
    {
      // TODO Auto-generated method stub
      return pplusPrime;
    }

    @Override
    public double getPMinusPrime ()
    {
      // TODO Auto-generated method stub
      return pminusPrime;
    }    
  }
}
