/*
 * Copyright 2009-2010 the original author or authors.
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

import org.powertac.common.Order;

/**
 * Interface that defines the publicly accessible methods
 * a Power TAC auctioneer has to implement.
 *
 * @author Carsten Block, John Collins
 */
public interface Auctioneer
{
  /**
   * Processes an incoming order, typically by saving it on a list for the
   * next market clearing.
   */
  public void processOrder (Order order); // throws ShoutCreationException;

  /**
   * Clears the market by matching all Orders that have arrived since the
   * last market clearing. Resulting transactions are created by calling 
   * addMarketTransaction() on the Accounting service. Orderbooks and
   * ClearedTrade instances are also created and broadcast to brokers.
   */
  public void clearMarket();
}
