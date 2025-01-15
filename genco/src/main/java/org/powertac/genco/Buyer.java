/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.genco;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.Order;
import org.powertac.common.Timeslot;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

/**
 * This is a simple buyer that provides some amount of market liquidity, as
 * well as a modest amount of volatility, to the wholesale market. It does this
 * by offering to buy some quantity at some price in each timeslot. The price
 * offered is distributed exponentially around a mean that should be lower than
 * the lowest genco offer price. The quantity requested is inversely
 * proportional to the price.
 * @author John Collins
 */
@Domain
public class Buyer extends Genco
{
  static private Logger log = LogManager.getLogger(Buyer.class.getName());

  private double priceBeta = 10.0;
  private double mwh = 100.0;

  public Buyer (String username)
  {
    super(username);
  }

  /**
   * Generates buy orders. Price is distributed exponentially with a mean value
   * of priceBeta. Quantity is mwh/price.
   */
  @Override
  public void generateOrders (Instant now, List<Timeslot> openSlots)
  {
    log.info("generate orders for " + getUsername());
    for (Timeslot slot : openSlots) {
      int slotNum = slot.getSerialNumber();
      double price = - priceBeta * Math.log(1.0 - seed.nextDouble());
      double qty = mwh / price;
      if (Math.abs(qty) < Competition.currentCompetition()
          .getMinimumOrderQuantity())
        return;
      Order offer = new Order(this, slotNum, qty, -price);
      log.debug(getUsername() + " wants " + qty +
                  " in " + slotNum + " for " + price);
      brokerProxyService.routeMessage(offer);
    }
  }

  // ---- getters and setters for configuration access -----
  public double getPriceBeta ()
  {
    return priceBeta;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Mean offer price for exponential distribution")
  @StateChange
  public void setPriceBeta (double priceBeta)
  {
    this.priceBeta = priceBeta;
  }

  public double getMwh ()
  {
    return mwh;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Offer quantity for price = 1.0")
  @StateChange
  public void setMwh (double mwh)
  {
    this.mwh = mwh;
  }
}
