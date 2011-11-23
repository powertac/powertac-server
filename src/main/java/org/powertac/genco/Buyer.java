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

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.PluginConfig;
import org.powertac.common.Order;
import org.powertac.common.Timeslot;

/**
 * This is a simple buyer that provides some amount of market liquidity, as
 * well as a modest amount of volatility, to the wholesale market. It does this
 * by offering to buy some quantity at some price in each timeslot. The price
 * offered is distributed exponentially around a mean that should be lower than
 * the lowest genco offer price. The quantity requested is inversely
 * proportional to the price.
 * @author John Collins
 */
public class Buyer extends Genco
{
  static private Logger log = Logger.getLogger(Buyer.class.getName());

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
      double price = - priceBeta * Math.log(1.0 - seed.nextDouble());
      double qty = mwh / price;
      Order offer = new Order(this, slot, qty, -price);
      log.debug(getUsername() + " wants " + qty +
                  " in " + slot.getSerialNumber() + " for " + price);
      brokerProxyService.routeMessage(offer);
    }
  }
  
  @Override
  void configure (PluginConfig config)
  {
    priceBeta = config.getDoubleValue("priceBeta", priceBeta);
    mwh = config.getDoubleValue("mwh", mwh);
  }
}
