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

import org.powertac.common.Shout;

/**
 * Interface that defines the publicly accessible methods
 * a Power TAC auctioneer has to implement.
 *
 * @author Carsten Block
 * @version 0.1, Date: 01.12.10
 */
public interface Auctioneer
{

  /**
   * Process an incoming shout, e.g. by matching it against existing shouts
   * As a CDA market auctioneer this is the method to match the <code>shoutDoCreate</shout>
   * against existing shouts in order to execute it immediately. As a call (PDA) market
   * auctioner this method could be used to store the new shout and to wait for a
   * <code>Auctioneer.clearMarket()</code> method invocation in order to start the matching
   * and execution process.
   * <p/>
   * In both cases orderbook updates, quote updates, shout status updates etc may be created
   * and returned in a list so that other components (e.g. {@link Accounting})
   * can further process these objects later on.
   */
  public void processShout(Shout shout); // throws ShoutCreationException;

  /**
   * Deletes the shout specified by {@link Shout}
   *
   * @param shoutDoDeleteCmd command object that contains the shoutId that should be deleted
   * @return List of objects, which can consist of {@link Shout} (deleted shout object), {@link org.powertac.common.MarketTransaction}, {@link org.powertac.common.Orderbook}
   * @throws org.powertac.common.exceptions.ShoutDeletionException thrown if the shout deletion fails
   */
  //List processShoutDelete(ShoutDoDeleteCmd shoutDoDeleteCmd) throws ShoutDeletionException;

  /**
   * Updates the shout specified by the {@link ShoutDoUpdateCmd}. Changeable
   * shout attributes are limit price and quantity only.
   *
   * @param shoutDoUpdateCmd the shout object to update
   * @return a List of objects, which can include {@link org.powertac.common.CashPosition}, {@link org.powertac.common.MarketPosition}, {@link org.powertac.common.Orderbook},{@link org.powertac.common.MarketTransaction}, {@link Shout} (matched / partially matched shout object), or an empty list.
   * @throws org.powertac.common.exceptions.ShoutUpdateException thrown if the shout update fails
   */
  //List processShoutUpdate(ShoutDoUpdateCmd shoutDoUpdateCmd) throws ShoutUpdateException;

  /**
   * This method is required for periodic clearing auctions and essentially tells
   * the Auctioneer module to clear the market matching all open shouts in the orderbooks.
   * Resulting transactions are created by calling addMarketTransaction() on the Accounting service.
   */
  public void clearMarket();

}
