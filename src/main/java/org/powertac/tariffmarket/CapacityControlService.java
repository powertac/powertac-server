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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
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
  static private Logger log = Logger.getLogger(CapacityControlService.class.getName());

  @Autowired
  Accounting accountingService;

  @Autowired
  TariffRepo tariffRepo;
  
  @Autowired
  TariffSubscriptionRepo tariffSubscriptionRepo;
  
  @Autowired
  TimeslotRepo timeslotRepo;
  
  // tariff transaction tracking
  int lastValidTimeslot = -1;
  HashMap<TariffSpecification, List<TariffTransaction>> pendingTariffTransactions =
      new HashMap<TariffSpecification, List<TariffTransaction>>();
  
  // future economic controls
  HashMap<Integer, List<EconomicControlEvent>> pendingEconomicControls =
      new HashMap<Integer, List<EconomicControlEvent>>();

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.CapacityControl#exerciseBalancingControl(org.powertac.common.msg.BalancingOrder, double)
   */
  @Override
  public void exerciseBalancingControl (BalancingOrder order, double kwh)
  {
    Tariff tariff = tariffRepo.findTariffById(order.getTariffId());
    if (null == tariff) {
      // should not happen
      log.error("Null tariff " + order.getTariffId() + " for balancing control");
      return;
    }
    List<TariffSubscription> subs =
        tariffSubscriptionRepo.findSubscriptionsForTariff(tariff);
    for (TariffSubscription sub : subs) {
      sub.postBalancingControl(kwh);
    }    
  }

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.CapacityControl#postEconomicControl(org.powertac.common.msg.EconomicControlEvent)
   */
  @Override
  public void postEconomicControl (EconomicControlEvent event)
  {
    int tsIndex = event.getTimeslotIndex();
    List<EconomicControlEvent> tsList = pendingEconomicControls.get(tsIndex);
    if (null == tsList) {
      tsList = new ArrayList<EconomicControlEvent>();
      pendingEconomicControls.put(tsIndex, tsList);
    }
    tsList.add(event);
  }

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.CapacityControl#getCurrentUsage(org.powertac.common.msg.BalancingOrder)
   */
  @Override
  public double getCurtailableUsage (BalancingOrder order)
  {
    Tariff tariff = tariffRepo.findTariffById(order.getTariffId());
    if (null == tariff) {
      // broker error, most likely
      log.warn("Null tariff " + order.getTariffId() + " for balancing order");
      return 0.0;
    }
    double result = 0.0;
    List<TariffSubscription> subs =
        tariffSubscriptionRepo.findSubscriptionsForTariff(tariff);
    for (TariffSubscription sub : subs) {
      result += sub.getMaxRemainingCurtailment();
    }
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

  // Returns the list of TariffTransactions in the current timeslot for
  // the specified tariff
  private List<TariffTransaction> getPendingTariffTransactions (Tariff tariff)
  {
    ensureCurrentTxList();
    return pendingTariffTransactions.get(tariff.getTariffSpec());
  }
  
  // Retrieves and filters TariffTransactions for the current timeslot, just
  // in case we do not already have them. Only CONSUME and PRODUCE transactions
  // are included.
  private void ensureCurrentTxList()
  {
    int currentTs = timeslotRepo.currentTimeslot().getSerialNumber();
    if (currentTs == lastValidTimeslot) 
      return;
    lastValidTimeslot = currentTs;
    pendingTariffTransactions.clear();
    for (TariffTransaction ttx : accountingService.getPendingTariffTransactions()) {
      if (ttx.getTxType() == TariffTransaction.Type.CONSUME ||
          ttx.getTxType() == TariffTransaction.Type.PRODUCE) {
        List<TariffTransaction> record = 
            pendingTariffTransactions.get(ttx.getTariffSpec());
        if (null == record) {
          record = new ArrayList<TariffTransaction>();
          pendingTariffTransactions.put(ttx.getTariffSpec(), record);
        }
        record.add(ttx);
      }
    }
  }

  /**
   * Activation applies ratio controls to subscriptions for the current
   * timeslot.
   */
  @Override
  public void activate (Instant time, int phaseNumber)
  {
    // Find economic controls for current timeslot, communicate
    // them to their respective subscriptions.
    int tsIndex = timeslotRepo.currentTimeslot().getSerialNumber();
    // complain if there are any out-of-date controls
    List<EconomicControlEvent> controls = pendingEconomicControls.get(tsIndex - 1);
    if (null != controls) {
      // expired controls
      for (EconomicControlEvent event : controls) {
        log.warn("Expired economic control, ts=" + (tsIndex - 1) +
                 ", broker=" + event.getBroker().getUsername());
      }
      pendingEconomicControls.remove(tsIndex - 1);
    }
    // now get the controls for the current timeslot
    controls = pendingEconomicControls.get(tsIndex);
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
  }

  @Override
  public void setDefaults ()
  {
    // Nothing to do at pre-game
  }

  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    pendingTariffTransactions.clear();
    pendingEconomicControls.clear();
    return "CapacityControl";
  }
}
