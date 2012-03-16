/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.common.interfaces;

import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.msg.EconomicControlEvent;

/**
 * Support for balancing market interactions between DU and customers/brokers.
 *
 * @author John Collins
 */
public interface CapacityControl {

  // -------------------- DU API ------------------------
  /**
   * Creates BalancingControlEvent, posts it on correct TariffSubscription
   * instances.
   */
  public void exerciseBalancingControl (BalancingOrder order, double kwh);

  /**
   * Gathers up power usage data for the current timeslot that could be
   * subject to a BalancingOrder. Return value is in kwh. 
   */
  public double getCurtailableUsage (BalancingOrder order);
  
  // ------------------- TariffMarket API -----------------
  /**
   * Posts an EconomicControlEvent on the correct TariffSubscription instances.
   */
  public void postEconomicControl (EconomicControlEvent event);
}