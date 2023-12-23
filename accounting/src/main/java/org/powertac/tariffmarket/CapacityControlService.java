/*
 * Copyright (c) 2012-2014 by the original author
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.RegulationAccumulator;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.BalancingControlEvent;
import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.msg.EconomicControlEvent;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for managing balancing and economic controls.
 * @author John Collins
 */
@Service
public class CapacityControlService
extends TimeslotPhaseProcessor
implements CapacityControl, InitializationService
{
  static private Logger log =
          LogManager.getLogger(CapacityControlService.class.getSimpleName());

  @Autowired
  Accounting accountingService;

  @Autowired
  TariffRepo tariffRepo;

  @Autowired
  TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  TimeslotRepo timeslotRepo;

  @Autowired
  BrokerProxy brokerProxy;

  // future economic controls
  HashMap<Integer, List<EconomicControlEvent>> pendingEconomicControls =
      new HashMap<>();

  // ignore quantities less than epsilon
  private double epsilon = 1e-6;

  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    super.init();
    pendingEconomicControls.clear();
    return "CapacityControl";
  }

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.CapacityControl#exerciseBalancingControl(org.powertac.common.msg.BalancingOrder, double)
   * Note that positive kwh means up-regulation.
   */
  @Override
  public void exerciseBalancingControl (BalancingOrder order,
                                        double kwh, double payment)
  {
    if (Math.abs(kwh) < epsilon)
      return;
    Tariff tariff = tariffRepo.findTariffById(order.getTariffId());
    if (null == tariff) {
      // should not happen
      log.error("Null tariff " + order.getTariffId() + " for balancing control");
      return;
    }
    List<TariffSubscription> subs =
        tariffSubscriptionRepo.findSubscriptionsForTariff(tariff);
    // allocate control across subscriptions in proportion to their curtailable
    // usage.
    double available = 0.0;
    HashMap<TariffSubscription, Double> amts =
        new HashMap<TariffSubscription, Double>(); 
    for (TariffSubscription sub : subs) {
      if (sub.getCustomersCommitted() > 0) {
        RegulationAccumulator value = sub.getRemainingRegulationCapacity();
        if (kwh > 0) {
          // up-regulation
          amts.put(sub, value.getUpRegulationCapacity());
          available += value.getUpRegulationCapacity();
        }
        else {
          // down-regulation
          amts.put(sub, value.getDownRegulationCapacity());
          available += value.getDownRegulationCapacity();
        }
      }
    }
    if (Math.abs(available) < epsilon) {
      log.warn("Unable to exercise balancing control: available == 0");
      return;
    }
    for (TariffSubscription sub : subs) {
      if (sub.getCustomersCommitted() > 0)
        // subscription takes customer viewpoint.
        sub.postBalancingControl(-kwh * amts.get(sub) / available);
    }
    // send off the event to the broker
    BalancingControlEvent bce = 
        new BalancingControlEvent(tariff.getTariffSpec(), kwh, payment,
                                  timeslotRepo.currentTimeslot().getSerialNumber());
    accountingService.postBalancingControl(bce);
    brokerProxy.sendMessage(tariff.getBroker(), bce);
  }

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.CapacityControl#postEconomicControl(org.powertac.common.msg.EconomicControlEvent)
   */
  @Override
  public void postEconomicControl (EconomicControlEvent event)
  {
    int tsIndex = event.getTimeslotIndex();
    int current = timeslotRepo.currentTimeslot().getSerialNumber();
    if (tsIndex < current) {
      log.warn("attempt to save old economic control for ts " + tsIndex +
               " during timeslot " + current);
      return;
    }
    List<EconomicControlEvent> tsList = pendingEconomicControls.get(tsIndex);
    if (null == tsList) {
      tsList = new ArrayList<EconomicControlEvent>();
      pendingEconomicControls.put(tsIndex, tsList);
    }
    tsList.add(event);
  }

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.CapacityControl#getCurtailableUsage(org.powertac.common.msg.BalancingOrder)
   */
  @Override
  public RegulationAccumulator getRegulationCapacity (BalancingOrder order)
  {
    Tariff tariff = tariffRepo.findTariffById(order.getTariffId());
    if (null == tariff) {
      // broker error, most likely
      log.warn("Null tariff " + order.getTariffId() + " for balancing order");
      return new RegulationAccumulator(0.0, 0.0);
    }
    RegulationAccumulator result = new RegulationAccumulator(0.0, 0.0);
    List<TariffSubscription> subs =
        tariffSubscriptionRepo.findSubscriptionsForTariff(tariff);
    for (TariffSubscription sub : subs) {
      result.add(sub.getRemainingRegulationCapacity());
    }
    log.info("BalancingOrder " + order.getId() + " capacity = ("
             + result.getUpRegulationCapacity() + ","
             + result.getDownRegulationCapacity() + ")");
    return result;
  }

  /**
   * Exercises an economic control for the current timeslot.
   */
  void exerciseEconomicControl (EconomicControlEvent event)
  {
    Tariff tariff = tariffRepo.findTariffById(event.getTariffId());
    if (null == tariff) {
      // should not happen
      log.error("Null tariff " + event.getTariffId() + " for economic control");
      return;
    }
    List<TariffSubscription> subs =
        tariffSubscriptionRepo.findSubscriptionsForTariff(tariff);
    for (TariffSubscription sub : subs) {
      sub.postRatioControl(event.getCurtailmentRatio());
    }
  }

  /**
   * Activation applies pending ratio controls to subscriptions for the current
   * timeslot.
   */
  @Override
  public void activate (Instant time, int phaseNumber)
  {
    log.info("CapacityControlService.activate() phase {}", phaseNumber);
    // Find economic controls for current timeslot, communicate
    // them to their respective subscriptions.
    int tsIndex = timeslotRepo.currentTimeslot().getSerialNumber();
    // complain if there are any out-of-date controls
    List<EconomicControlEvent> controls = getControlsForTimeslot(tsIndex - 1);
    if (null != controls) {
      // expired controls
      for (EconomicControlEvent event : controls) {
        log.warn("Expired economic control, ts=" + (tsIndex - 1) +
                 ", broker=" + event.getBroker().getUsername());
      }
      pendingEconomicControls.remove(tsIndex - 1);
    }
    // now get the controls for the current timeslot
    controls = getControlsForTimeslot(tsIndex);
    if (null != controls) {
      for (EconomicControlEvent event : controls) {
        Tariff tariff = tariffRepo.findTariffById(event.getTariffId());
        if (null == tariff) {
          // no such tariff
          log.warn("Cannot find tariff " + event.getTariffId() +
                   " in EconomicControlEvent " + event.getId() +
                   " from " + event.getBroker().getUsername());
        }
        else {
          for (TariffSubscription sub :
               tariffSubscriptionRepo.findSubscriptionsForTariff(tariff)) {
            sub.postRatioControl(event.getCurtailmentRatio());
          }
        }
      }
    }
    pendingEconomicControls.remove(tsIndex);
  }

  // ---------------- Test support ------------------
  List<EconomicControlEvent> getControlsForTimeslot (int timeslotIndex)
  {
    return pendingEconomicControls.get(timeslotIndex);
  }
}
