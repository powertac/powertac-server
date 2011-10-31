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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.powertac.common.Orderbook;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.enumerations.ProductType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Repository for Orderbooks. Orderbooks are created with makeOrderbook().
 * Query methods include findByTimeslot().
 * @author John Collins
 */
@Repository
public class OrderbookRepo implements DomainRepo
{
  static private Logger log = Logger.getLogger(OrderbookRepo.class.getName());

  @Autowired
  private TimeService timeService;
  
  // local state - keep track of the current orderbook, as well as the
  // most recent one for a given timeslot with a non-empty clearing price
  private HashMap<Timeslot, Orderbook> timeslotIndex;
  private HashMap<Timeslot, Orderbook> spotIndex;
  
  /** Standard constructor */
  public OrderbookRepo ()
  {
    super();
    timeslotIndex = new HashMap<Timeslot, Orderbook>();
    spotIndex = new HashMap<Timeslot, Orderbook>();
  }
  
  /**
   * Creates a new Orderbook, with standard defaults (productType = Future,
   * dateExecuted = now).
   */
  public Orderbook makeOrderbook (Timeslot timeslot, Double clearingPrice)
  {
    Orderbook result = new Orderbook(timeslot, 
                                     ProductType.Future,
                                     clearingPrice,
                                     timeService.getCurrentTime());
    timeslotIndex.put(timeslot, result);
    if (clearingPrice != null)
      spotIndex.put(timeslot, result);
    log.debug("Created new Orderbook ts=" + timeslot.getSerialNumber() +
              ", clearingPrice=" + clearingPrice);
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
  
  /**
   * Returns the count of instances in this repo
   */
  public int size()
  {
    return timeslotIndex.size();
  }

  /** Clears the index in preparation for a new simulation */
  public void recycle ()
  {
    timeslotIndex.clear();
    spotIndex.clear();
  }

}
