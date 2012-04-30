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
package org.powertac.distributionutility;

import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.repo.TariffRepo;

/**
 * Context interface for settlement processors. This is needed for testing,
 * and to limit the accessibility of the DistributionUtilityService to
 * settlement processors. Methods have package visibility, because we assume
 * settlement processors will be in the same package with the service.
 * @author jcollins
 */
public interface SettlementContext
{
  /**
   * Returns the current TariffRepo.
   */
  public TariffRepo getTariffRepo ();
  
  /**
   * Returns the current CapacityControlService.
   */
  public CapacityControl getCapacityControlService ();
  
  /**
   * Returns the current value of pPlus.
   */
  public double getPPlus ();
  
  /**
   * Returns the current value of pMinus.
   */
  public double getPMinus ();
  
  /**
   * Returns the DU charge for running the balancing market
   */
  public Double getBalancingCost ();
}
