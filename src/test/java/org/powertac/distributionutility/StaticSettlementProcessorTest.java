package org.powertac.distributionutility;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.InstanceOf;
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
  double pminus = 0.0;
  
  Broker b1, b2;
  
  StaticSettlementProcessor uut;
  ArrayList<ChargeInfo> brokerData;

  @Before
  public void setUp () throws Exception
  {
    tariffRepo = new TariffRepo();
    capacityControlService = mock(CapacityControl.class);
    context = new MockSettlementContext();
    uut = new StaticSettlementProcessor();
    b1 = new Broker("Sally");
    b2 = new Broker("Mary");
    brokerData = new ArrayList<ChargeInfo>();
  }

  // Simple balancing, no imbalance
  @Test
  public void testSettle0 ()
  {
    ChargeInfo ci1 = new ChargeInfo(b1, 0.0);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 0.0);
    brokerData.add(ci2);
    pplus = 60.0;
    pminus = 15.0;
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
    pplus = 0.06;
    pminus = 0.015;
    uut.settle(context, brokerData);
    assertEquals("-0.6 for b1", -0.6, ci1.getBalanceCharge(), 1e-6);
    assertEquals("0.15 for b2", 0.15, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, net imbalance
  @Test
  public void testSettleNet ()
  {
    ChargeInfo ci1 = new ChargeInfo(b1, -20.0);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 10.0);
    brokerData.add(ci2);
    pplus = 0.06;
    pminus = 0.015;
    uut.settle(context, brokerData);
    assertEquals("-1.2 for b1", -1.2, ci1.getBalanceCharge(), 1e-6);
    assertEquals("0.15 for b2", 0.15, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for b1
  @Test
  public void testSingleBO ()
  {
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
    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(5.0);
    
    pplus = 0.06;
    pminus = 0.015;
    uut.settle(context, brokerData);
    verify(capacityControlService).exerciseBalancingControl(bo1, 5.0, 0.3);
    assertEquals("-0.9 for b1", -0.9, ci1.getBalanceCharge(), 1e-6);
    assertEquals(".15 for b2", 0.15, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for b1
  // that is not exercised
  @Test
  public void testSingleBO_NoExercise ()
  {
    TariffSpecification spec =
            new TariffSpecification(b1, PowerType.INTERRUPTIBLE_CONSUMPTION);
    Rate rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec.addRate(rate);
    Tariff tariff = new Tariff(spec);
    tariffRepo.addTariff(tariff);
    BalancingOrder bo1 = new BalancingOrder(b1, spec, 0.6, 0.061);
    tariffRepo.addBalancingOrder(bo1);
    ChargeInfo ci1 = new ChargeInfo(b1, -20.0);
    ci1.addBalancingOrder(bo1);
    brokerData.add(ci1);
    ChargeInfo ci2 = new ChargeInfo(b2, 10.0);
    brokerData.add(ci2);
    when(capacityControlService.getCurtailableUsage(bo1)).thenReturn(5.0);
    
    pplus = 0.06;
    pminus = 0.015;
    uut.settle(context, brokerData);
    verify(capacityControlService, never()).exerciseBalancingControl(isA(BalancingOrder.class), anyDouble(), anyDouble());
    assertEquals("-1.2 for b1", -1.2, ci1.getBalanceCharge(), 1e-6);
    assertEquals(".15 for b2", 0.15, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for b1
  @Test
  public void testSingleBO_HighCapacity ()
  {
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
    
    pplus = 0.06;
    pminus = 0.015;
    uut.settle(context, brokerData);
    verify(capacityControlService).exerciseBalancingControl(bo1, 10.0, 0.6);
    assertEquals("-0.6 for b1", -0.6, ci1.getBalanceCharge(), 1e-6);
    assertEquals(".15 for b2", 0.15, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for each broker
  @Test
  public void test2BO_LowCapacity ()
  {
    TariffSpecification spec1 =
            new TariffSpecification(b1, PowerType.INTERRUPTIBLE_CONSUMPTION);
    Rate rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec1.addRate(rate);
    Tariff tariff1 = new Tariff(spec1);
    tariffRepo.addTariff(tariff1);
    BalancingOrder bo1 = new BalancingOrder(b1, spec1, 0.6, 0.04);
    tariffRepo.addBalancingOrder(bo1);
    
    TariffSpecification spec2 =
            new TariffSpecification(b2, PowerType.INTERRUPTIBLE_CONSUMPTION);
    rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec2.addRate(rate);
    Tariff tariff2 = new Tariff(spec2);
    tariffRepo.addTariff(tariff2);
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
    
    pplus = 0.06;
    pminus = 0.015;
    uut.settle(context, brokerData);
    verify(capacityControlService).exerciseBalancingControl(bo1, 10.0, 0.6);
    verify(capacityControlService).exerciseBalancingControl(bo2, 5.0, 0.3);
    assertEquals("-0.6 for b1", -0.6, ci1.getBalanceCharge(), 1e-6);
    assertEquals(".15 for b2", -0.3, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for each broker
  @Test
  public void test2BO_HighCapacity ()
  {
    TariffSpecification spec1 =
            new TariffSpecification(b1, PowerType.INTERRUPTIBLE_CONSUMPTION);
    Rate rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec1.addRate(rate);
    Tariff tariff1 = new Tariff(spec1);
    tariffRepo.addTariff(tariff1);
    BalancingOrder bo1 = new BalancingOrder(b1, spec1, 0.6, 0.04);
    tariffRepo.addBalancingOrder(bo1);
    
    TariffSpecification spec2 =
            new TariffSpecification(b2, PowerType.INTERRUPTIBLE_CONSUMPTION);
    rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec2.addRate(rate);
    Tariff tariff2 = new Tariff(spec2);
    tariffRepo.addTariff(tariff2);
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
    
    pplus = 0.06;
    pminus = 0.015;
    uut.settle(context, brokerData);
    verify(capacityControlService).exerciseBalancingControl(bo1, 16.0, 0.94);
    verify(capacityControlService).exerciseBalancingControl(bo2, 14.0, 0.84);
    assertEquals("-0.26 for b1", -0.26, ci1.getBalanceCharge(), 1e-6);
    assertEquals(".022 for b2", 0.24, ci2.getBalanceCharge(), 1e-6);
  }

  // Simple balancing, net imbalance, single balancing order for each broker
  @Test
  public void test3BO_LowCapacity ()
  {
    TariffSpecification spec1 =
            new TariffSpecification(b1, PowerType.INTERRUPTIBLE_CONSUMPTION);
    Rate rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec1.addRate(rate);
    Tariff tariff1 = new Tariff(spec1);
    tariffRepo.addTariff(tariff1);
    BalancingOrder bo1 = new BalancingOrder(b1, spec1, 0.6, 0.04);
    tariffRepo.addBalancingOrder(bo1);
    
    TariffSpecification spec1a =
            new TariffSpecification(b1, PowerType.INTERRUPTIBLE_CONSUMPTION);
    rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec1a.addRate(rate);
    Tariff tariff1a = new Tariff(spec1a);
    tariffRepo.addTariff(tariff1a);
    BalancingOrder bo1a = new BalancingOrder(b1, spec1a, 0.6, 0.045);
    tariffRepo.addBalancingOrder(bo1a);
    
    TariffSpecification spec2 =
            new TariffSpecification(b2, PowerType.INTERRUPTIBLE_CONSUMPTION);
    rate = new Rate().withFixed(true).withValue(0.11).withMaxCurtailment(0.5);
    spec2.addRate(rate);
    Tariff tariff2 = new Tariff(spec2);
    tariffRepo.addTariff(tariff2);
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
    
    pplus = 0.06;
    pminus = 0.015;
    uut.settle(context, brokerData);
    verify(capacityControlService).exerciseBalancingControl(bo1, 10.0, 0.6);
    verify(capacityControlService).exerciseBalancingControl(bo2, 5.0, 0.3);
    assertEquals("-0.6 for b1", -0.6, ci1.getBalanceCharge(), 1e-6);
    assertEquals(".15 for b2", -0.3, ci2.getBalanceCharge(), 1e-6);
  }

  // --------------------------------------------------------

  class MockSettlementContext implements SettlementContext
  {
    @Override
    public TariffRepo getTariffRepo ()
    {
      return tariffRepo;
    }

    @Override
    public CapacityControl getCapacityControlService ()
    {
      return capacityControlService;
    }

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
  }
}
