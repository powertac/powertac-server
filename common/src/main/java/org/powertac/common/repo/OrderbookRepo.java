/*
 * Copyright (c) 2011 by the original author
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
package org.powertac.common.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.Orderbook;
import org.powertac.common.Timeslot;
import org.springframework.stereotype.Service;

/**
 * Repository for Orderbooks. Orderbooks are created with makeOrderbook().
 * Query methods include findByTimeslot().
 * @author John Collins
 */
@Service
public class OrderbookRepo extends ManagedRepo
{
  static private Logger log = LogManager.getLogger(OrderbookRepo.class.getName());
  
  // local state - keep track of orderbooks by timeslot,
  // the current orderbook, as well as the
  // most recent one for a given timeslot with a non-empty clearing price
  private TreeMap<Timeslot, List<Orderbook>> orderbookIndex;
  private TreeMap<Timeslot, Orderbook> timeslotIndex;
  private TreeMap<Timeslot, Orderbook> spotIndex;
  private Double[] minAskPrices;
  private Double[] maxAskPrices;
  private int lookback = 168 * 2 + 60; // keep (at least) two days worth of Orderbooks around
  private int orderbookCount = 0;
  private int lastTimeslotSerial = 0;
  
  /** Standard constructor */
  public OrderbookRepo ()
  {
    super();
    orderbookIndex = new TreeMap<Timeslot, List<Orderbook>>();
    timeslotIndex = new TreeMap<Timeslot, Orderbook>();
    spotIndex = new TreeMap<Timeslot, Orderbook>();
  }
  
  /**
   * Creates a new Orderbook, with standard defaults (productType = Future,
   * dateExecuted = now).
   */
  public Orderbook makeOrderbook (Timeslot timeslot, Double clearingPrice)
  {
    setup();
    Orderbook result = new Orderbook(timeslot,
                                     clearingPrice,
                                     timeService.getCurrentTime());
    timeslotIndex.put(timeslot, result);
    if (clearingPrice != null)
      spotIndex.put(timeslot, result);
    List<Orderbook> obList = orderbookIndex.get(timeslot);
    if (obList == null) {
      obList = new ArrayList<Orderbook>();
      orderbookIndex.put(timeslot, obList);
    }
    obList.add(result);
    log.debug("Created new Orderbook ts=" + timeslot.getSerialNumber() +
              ", clearingPrice=" + clearingPrice);
    orderbookCount += 1;
    if (timeslot.getSerialNumber() > lastTimeslotSerial)
      lastTimeslotSerial = timeslot.getSerialNumber();
    return result;
  }
  
  /**
   * Returns the most recent Orderbook that has been created for the specified
   * timeslot.
   */
  public Orderbook findByTimeslot (Timeslot timeslot)
  {
    return timeslotIndex.get(timeslot);
  }
  
  /**
   * Returns the most recent orderbook with a non-null clearing price.
   * Note that this can return null if this timeslot has never cleared.
   */
  public Orderbook findSpotByTimeslot (Timeslot timeslot)
  {
    return spotIndex.get(timeslot);
  }
  
  public List<Orderbook> findAllByTimeslot (Timeslot timeslot)
  {
    return orderbookIndex.get(timeslot);
  }
  
  /**
   * Set the minAskPrices array. This is presumably done by the auctioneer
   * when clearing the market, and so the data reflects the prices for
   * the most recent market clearing.
   */
  public void setMinAskPrices(Double[] values)
  {
    minAskPrices = values;
  }
  
  /**
   * 
   * @param values
   */
  public void setMaxAskPrices(Double[] values)
  {
	  this.maxAskPrices = values;
  }
  
  /**
   * Returns the minAskPrices array, representing the minimum ask prices
   * for the most recent clearing of the wholesale market.
   */
  public Double[] getMinAskPrices()
  {
    return minAskPrices;
  }
  
  /**
   * Returns maxAskPrices array, representing the maximum ask prices
   * for the most recent clearing of the wholesale market.
   * @return maximum ask prices.
   */
  public Double[] getMaxAskPrices()
  {
	  return this.maxAskPrices;
  }
  
  /**
   * Returns the count of instances in this repo
   */
  public int size()
  {
    return timeslotIndex.size();
  }

  /** Clears the index in preparation for a new simulation */
  @Override
  public void recycle ()
  {
    orderbookIndex.clear();
    timeslotIndex.clear();
    spotIndex.clear();
    minAskPrices = null;
    maxAskPrices = null;
    // Start memory usage recorder
    orderbookCount = 0;
    super.recycle();
  }

  // Records total usage once every 12 hours, starting in 3 hours
  // See Issue #1031 for details
  private void recordUsage ()
  {
    log.debug("Orderbook count = {}", orderbookCount);
  }

  @Override
  protected void doCleanup ()
  {
    if (orderbookIndex.size() > 0) {
      int oldest = lastTimeslotSerial - lookback;
      while (orderbookIndex.firstKey().getSerialNumber() < oldest) {
        Timeslot old = orderbookIndex.firstKey();
        //System.out.println("removing " + old.getSerialNumber());
        List<Orderbook> books = orderbookIndex.remove(old);
        orderbookCount -= books.size();
        timeslotIndex.remove(old);
        spotIndex.remove(old);
      }
      recordUsage();
    }
  }

  // Test support
  int getOrderbookCount ()
  {
    return orderbookCount;
  }
}
