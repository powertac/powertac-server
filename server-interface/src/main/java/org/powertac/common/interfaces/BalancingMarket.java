/*
 * Copyright 2013-2014 the original author or authors.
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

import org.powertac.common.Broker;

/**
 * Interface that defines the publicly accessible methods
 * a Power TAC balancing market has to implement.
 *
 * @author John Collins
 */
public interface BalancingMarket
{
  /**
   * Returns the market balance for a given broker.
   */
  public double getMarketBalance (Broker broker);

  /**
   * Returns the net regulation energy (positive for up-regulation,
   * negative for down-regulation) for a given broker.
   */
  public double getRegulation (Broker broker);

  /**
   * Returns the slope of cost curve for up-regulation. Total cost for
   * up-regulation by x kwh is pPlus + x * pPlusPrime. Note that x is
   * positive for up-regulation.
   */
  public double getPPlusPrime();

  /**
   * Returns the slope of cost curve for down-regulation. Total cost for
   * down-regulation by x kwh is pMinus + x * pMinusPrime. Note that x is
   * negative for down-regulation.
   */
  public double getPMinusPrime();

  /**
   * Returns the per-timeslot charge for running the balancing market
   */
  public Double getBalancingCost ();

  /**
   * Returns the value used for spot price per MWh if unavailable from
   * wholesale market.
   */
  public double getDefaultSpotPrice ();
}
