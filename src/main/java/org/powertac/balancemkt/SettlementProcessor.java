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
package org.powertac.balancemkt;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.repo.TariffRepo;

/**
 * Computes charges to settle broker imbalances.
 * @author John Collins
 */
public abstract class SettlementProcessor
{
  protected static Logger log = LogManager.getLogger(SettlementProcessor.class.getName());

  protected TariffRepo tariffRepo;
  protected CapacityControl capacityControlService;
  protected double epsilon = 1e-6; // 1 milliwatt-hour

  SettlementProcessor (TariffRepo tariffRepo, CapacityControl capacityControl)
  {
    super();
    this.tariffRepo = tariffRepo;
    this.capacityControlService = capacityControl;
  }
  
  /**
   * Computes charges to settle broker imbalances. 
   * The brokers and their imbalances, along with results from the
   * settlement, are represented by a Collection of ChargeInfo instances.
   * Requires access to utility methods in BalancingMarketService.
   */
  public abstract void settle(SettlementContext service,
                     List<ChargeInfo> brokerData);
}
