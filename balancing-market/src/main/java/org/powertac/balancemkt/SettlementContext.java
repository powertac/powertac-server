/*
 * Copyright (c) 2012-2014 by John Collins
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

import org.powertac.common.interfaces.BalancingMarket;

/**
 * Context interface for settlement processors. This is needed for testing,
 * and to limit the accessibility of the BalancingMarketService to
 * settlement processors. Methods have package visibility, because we assume
 * settlement processors will be in the same package with the service.
 * @author John Collins
 */
public interface SettlementContext extends BalancingMarket
{  
  /**
   * Returns the current value of pPlus. This is the marginal cost for
   * up-regulating power. Value is normally negative, because brokers must
   * pay. Cost increases (becomes more negative) with quantity according 
   * to pPlusPrime.
   */
  public double getPPlus ();

  /**
   * Returns the current value of pMinus. This is the marginal cost for
   * down-regulation. Value is normally positive, because brokers are paid.
   * Cost increases with quantity (becomes less positive, eventually becoming
   * negative) with quantity accoring to the value of pMinusPrime.
   */
  public double getPMinus ();
}
