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

import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.repo.TariffRepo;

/**
 * DU settlement processor for Scenario 3 - controllable capacities, dynamic
 * solution over multiple timeslots.
 * @author John Collins
 */
public class DynamicSettlementProcessor extends SettlementProcessor
{
  
  DynamicSettlementProcessor (TariffRepo tariffRepo, CapacityControl capacityControl)
  {
    super(tariffRepo, capacityControl);
  }

  /* (non-Javadoc)
   * @see org.powertac.balancemkt.SettlementProcessor#settle(java.util.Collection)
   */
  @Override
  public void settle (SettlementContext service,
                      List<ChargeInfo> brokerData)
  {
    // TODO Auto-generated method stub
    
  }

}
