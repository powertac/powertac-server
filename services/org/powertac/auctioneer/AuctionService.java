/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.auctioneer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Broker;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;
import org.powertac.common.PluginConfig;
import org.powertac.common.Order;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.state.StateChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This is the wholesale day-ahead market. Energy is traded in future timeslots by
 * submitting MarketOrders representing bids and asks. Each specifies a price (minimum price
 * for asks, maximum price for bids) and a quantity in mWh. Once during each timeslot, the
 * market is cleared by matching bids with asks such that the bid price is no lower than
 * the ask price, and allocating quantities, until no matching bids or asks are
 * available. In general, the last matched bid will have a higher price than the last
 * matched ask. All trades are cleared at a price determined by splitting the difference
 * between the last bid and the last ask according to the value of sellerSurplusRatio,
 * which is a parameter set in the initialization process. 
 * @author John Collins
 */
@Service
public class AuctionService
  extends TimeslotPhaseProcessor
  implements BrokerMessageListener
{
  static private Logger log = Logger.getLogger(AuctionService.class.getName());

  //@Autowired
  //private TimeService timeService;
  
  @Autowired
  private Accounting accountingService;
  
  @Autowired
  private BrokerProxy brokerProxyService;
  
  @Autowired
  private TimeService timeService;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private OrderbookRepo orderbookRepo;
  
  private double defaultSellerSurplus = 0.5;
  private double defaultMargin = 0.05; // used when one side has no limit price
  private double defaultClearingPrice = 40.00; // used when no limit prices
  private double sellerSurplusRatio;

  private List<Order> incoming;
  
  private HashMap<Timeslot, TreeSet<OrderWrapper>> sortedBids;
  private HashMap<Timeslot, TreeSet<OrderWrapper>> sortedAsks;
  
  public AuctionService ()
  {
    super();
    incoming = new ArrayList<Order>();
  }
  
  /**
   * Registers for phase 2 activation, to drive tariff publication
   */
  public void init (PluginConfig config)
  {
    incoming.clear();
    setSellerSurplusRatio(config.getDoubleValue("sellerSurplus",
                                                defaultSellerSurplus));
    brokerProxyService.registerBrokerMarketListener(this);
    super.init();
  }

  public double getDefaultSellerSurplus ()
  {
    return defaultSellerSurplus;
  }

  void setDefaultSellerSurplus (double defaultSellerSurplus)
  {
    this.defaultSellerSurplus = defaultSellerSurplus;
  }

  double getSellerSurplusRatio ()
  {
    return sellerSurplusRatio;
  }

  @StateChange
  private void setSellerSurplusRatio (Double number)
  {
    sellerSurplusRatio = number;
  }

  List<Order> getIncoming ()
  {
    return incoming;
  }

  // ----------------- Broker message API --------------------
  /**
   * Receives, validates, and queues an incoming Order message. Processing the incoming
   * marketOrders happens during Phase 2 in each timeslot.
   */
  public void receiveMessage (Object msg)
  {
    if (msg != null && msg instanceof Order) {
      if (validateOrder((Order)msg)) {
        // queue incoming message
        synchronized(incoming) {
          incoming.add((Order)msg);
        }
      }
    }
  }
  
  public boolean validateOrder (Order order)
  {
    // TODO - give feedback to broker if possible.
    if (!order.getTimeslot().isEnabled()) {
      log.error("Order submitted for disabled timeslot");
      return false;
    }
    return true;
  }

  // ------------------- Market clearing ------------------------
  /**
   * Processes incoming Order instances for each timeslot, generating the appropriate
   * MarketTransactions, Orderbooks, and ClearedTrade instances.
   */
  public void activate (Instant time, int phaseNumber)
  {
    // Grab all the incoming marketOrders and sort them by price and timeslot
    ArrayList<OrderWrapper> orders;
    synchronized(incoming) {
      orders = new ArrayList<OrderWrapper>();
      for (Order order : incoming) {
        orders.add(new OrderWrapper(order));
      }
      incoming.clear();
    }
    sortedAsks = new HashMap<Timeslot, TreeSet<OrderWrapper>>();
    sortedBids = new HashMap<Timeslot, TreeSet<OrderWrapper>>();
    for (OrderWrapper sw : orders) {
      if (sw.isBuyOrder())
        addBid(sw);
      else
        addAsk(sw);
    }
    log.debug("activate: " + sortedAsks.size() + " asks, " +
              sortedBids.size() + " bids");
    
    // Iterate through the enabled timeslots, and clear each one individually
    for (Timeslot timeslot : timeslotRepo.enabledTimeslots()) {
      clearTimeslot(timeslot);
    }
  }

  private void clearTimeslot (Timeslot timeslot)
  {
    SortedSet<OrderWrapper> bids = sortedBids.get(timeslot);
    SortedSet<OrderWrapper> asks = sortedAsks.get(timeslot);
    if (bids != null || asks != null) {
      // we have bids and/or asks to match up
      if (bids != null && asks != null)
        log.info("Timeslot " + timeslot.getSerialNumber() + 
                 ": Clearing " + asks.size() + " asks and " +
                 bids.size() + " bids");
      double bidPrice = 0.0;
      double askPrice = 0.0;
      double totalMWh = 0.0;
      ArrayList<PendingTrade> pendingTrades = new ArrayList<PendingTrade>();
      while (bids != null && bids.size() > 0 && 
             asks != null && asks.size() > 0 &&
             -bids.first().getLimitPrice() >= asks.first().getLimitPrice()) {
        // transfer from ask to bid, keep track of qty
        OrderWrapper bid = bids.first();
        bidPrice = bid.getLimitPrice();
        OrderWrapper ask = asks.first();
        askPrice = ask.getLimitPrice();
        // amount to transfer is minimum of remaining bid qty and remaining ask qty
        log.debug("ask: " + ask.executionMWh + " used out of " + ask.getMWh() +
                  "; bid: " + bid.executionMWh + " used out of " + bid.getMWh());
        double transfer = Math.min(bid.getMWh() - bid.executionMWh,
                                   -ask.getMWh() + ask.executionMWh);
        if (transfer > 0.0) {
          log.debug("transfer " + transfer + " from " + 
                    ask.getBroker().getUsername() + " at " + askPrice + " to " +
                    bid.getBroker().getUsername() + " at " + bidPrice);
          totalMWh += transfer;
          pendingTrades.add(new PendingTrade(ask.getBroker(), bid.getBroker(), transfer));
          bid.executionMWh += transfer;
          ask.executionMWh -= transfer;
        }
        if (bid.getMWh() - bid.executionMWh <= 0.0)
          bids.remove(bid);
        if (ask.getMWh() - ask.executionMWh >= 0.0)
          asks.remove(ask);
      }
      double clearingPrice = askPrice + sellerSurplusRatio * (-bidPrice - askPrice);
      for (PendingTrade trade : pendingTrades) {
        accountingService.addMarketTransaction(trade.from, timeslot,
                                               clearingPrice, -trade.mWh);
        accountingService.addMarketTransaction(trade.to, timeslot,
                                               -clearingPrice, trade.mWh);
      }
      // create the orderbook and cleared-trade, send to brokers
      Orderbook orderbook = 
          orderbookRepo.makeOrderbook(timeslot,
                                      (pendingTrades.size() > 0
                                          ? clearingPrice : null));
      if (bids != null) {
        for (OrderWrapper bid : bids) {
          orderbook.addBid(new OrderbookOrder(bid.getMWh() - bid.executionMWh,
                                              bid.getLimitPrice()));
        }
      }
      if (asks != null) {
        for (OrderWrapper ask : asks) {
          orderbook.addAsk(new OrderbookOrder(ask.getMWh() - ask.executionMWh,
                                              ask.getLimitPrice()));
        }
      }
      brokerProxyService.broadcastMessage(orderbook);
      if (totalMWh > 0.0) {
        ClearedTrade trade = new ClearedTrade(timeslot, totalMWh, clearingPrice,
                                              timeService.getCurrentTime());
        log.info(trade.toString());
        brokerProxyService.broadcastMessage(trade);
      }
    }
  }
  
  // TODO - add cases for market orders on both sides

  private void addAsk (OrderWrapper marketOrder)
  {
    Timeslot timeslot = marketOrder.getTimeslot();
    if (sortedAsks.get(timeslot) == null) {
      sortedAsks.put(timeslot, new TreeSet<OrderWrapper>());
    }
    sortedAsks.get(timeslot).add(marketOrder);
  }

  private void addBid (OrderWrapper marketOrder)
  {
    Timeslot timeslot = marketOrder.getTimeslot();
    if (sortedBids.get(timeslot) == null) {
      sortedBids.put(timeslot, new TreeSet<OrderWrapper>());
    }
    sortedBids.get(timeslot).add(marketOrder);
  }
  
  class PendingTrade
  {
    Broker from;
    Broker to;
    double mWh;
    
    PendingTrade (Broker from, Broker to, double mWh)
    {
      super();
      this.from = from;
      this.to = to;
      this.mWh = mWh;
    }
  }
  
  class OrderWrapper implements Comparable
  {
    Order order;
    double executionMWh = 0.0;
    
    OrderWrapper(Order order)
    {
      super();
      this.order = order;
    }
    
    // delegation API
    Broker getBroker ()
    {
      return order.getBroker();
    }
    
    Double getLimitPrice ()
    {
      return order.getLimitPrice();
    }
    
    double getMWh ()
    {
      return order.getMWh();
    }
    
    Timeslot getTimeslot ()
    {
      return order.getTimeslot();
    }
    
    boolean isBuyOrder ()
    {
      return (order.getMWh() > 0.0);
    }

    @Override
    public int compareTo(Object o) {
      if (!(o instanceof OrderWrapper)) 
        return 1;
      OrderWrapper other = (OrderWrapper) o;
      if (this.getLimitPrice() == null)
        if (other.getLimitPrice() == null)
          return 0;
        else
          return -1;
      else if (other.getLimitPrice() == null)
        return 1;
      else
        return (this.getLimitPrice() == 
                (other.getLimitPrice()) ? 0 :
                  (this.getLimitPrice() < other.getLimitPrice() ? -1 : 1));
    }
  }
}
