package org.powertac.common.interfaces;

import java.util.List;

/**
 * Interface that extends the <code>Auctioneer</code> interface
 * by defining a <code>clearMarket</code> method. This can be
 * triggered from outside in order to initiate a market clearing process.
 * The auctioneer then tries to compute the set of executable shouts as well
 * as clearing prices using all valid (i.e. non-outdated and unmatched) shouts
 * he received up to the point where the <code>clearMarket</code> method
 * was invoked.
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 * @see org.powertac.common.interfaces.Auctioneer
 */
public interface CallMarketAuctioneer extends Auctioneer {

  List clearMarket();

}
