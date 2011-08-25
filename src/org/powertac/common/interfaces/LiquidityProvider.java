/*
 * Copyright 2009-2011 the original author or authors.
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

import org.powertac.common.Competition;
import org.powertac.common.enumerations.ProductType;
import org.powertac.common.Timeslot;
import org.powertac.common.MarketTransaction;

import java.util.List;

/**
 * Interface for a liquidity provider.
 *
 * @author David Dauer, Carsten Block
 * @version 0.1 - January 2nd, 2011
 */
public interface LiquidityProvider {

  /**
   * This is method is called in order to make the LiquidityProvider
   * module generate quotes (i.e. buy and sell shouts) for all products
   * in all timeslots of a particular competition
   *
   * @param competition the competition to generate quotes for
   * @return List of {@link org.powertac.common.msg.ShoutDoCreateCmd}, {@link org.powertac.common.msg.ShoutDoUpdateCmd}, and {@link org.powertac.common.msg.ShoutDoDeleteCmd} objects to be processed by the auctioneer later on
   */
  public List createAllQuotesFor (Competition competition);

  /**
   * This is method is called in order to make the LiquidityProvider
   * module respond to a specific transaction that occurred in the market.
   *
   * @param marketTransaction the transactionLog to respond to
   * @return List of {@link org.powertac.common.msg.ShoutDoCreateCmd}, {@link org.powertac.common.msg.ShoutDoUpdateCmd}, and {@link org.powertac.common.msg.ShoutDoDeleteCmd} objects to be processed by the auctioneer later on
   */
  public List createQuoteFor(MarketTransaction marketTransaction);

  /**
   * This is method is called in order to make the LiquidityProvider
   * module generate / update its quote (i.e. buy and sell orders) for
   * a particular product in a particular timeslot.
   *
   * @param product the product to generate a quote for
   * @param timeslot the timeslot to generate a quote for
   * @return List of {@link org.powertac.common.msg.ShoutDoCreateCmd}, {@link org.powertac.common.msg.ShoutDoUpdateCmd}, and {@link org.powertac.common.msg.ShoutDoDeleteCmd} objects to be processed by the auctioneer later on
   */
  public List createQuoteFor(ProductType product, Timeslot timeslot);

}
