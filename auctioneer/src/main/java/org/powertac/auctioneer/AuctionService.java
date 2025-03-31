/*
 * Copyright (c) 2011 - 2020 by the original author or authors.
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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import java.time.Instant;
import org.powertac.common.Broker;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.OrderStatus;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This is the wholesale day-ahead market. Energy is traded in future timeslots by
 * submitting MarketOrders representing bids and asks. Each specifies a price (minimum price
 * for asks, maximum (negative) price for bids) and a quantity in mWh. A bid is
 * defined as an Order with a positive value for mWh; an ask is an Order with a
 * negative mWh value. Once during each timeslot, the
 * market is cleared by matching bids with asks such that the bid price is no lower than
 * the ask price, and allocating quantities, until no matching bids or asks are
 * available. In general, the last matched bid will have a higher price than the last
 * matched ask. All trades are cleared at a price determined by splitting the difference
 * between the last bid and the last ask according to the value of sellerSurplusRatio,
 * which is a parameter set in the initialization process. 
 * <p>
 * Orders may be market orders (no specified price) as well as limit orders
 * (the normal case). Market orders are considered to have a "more attractive"
 * price than any limit order, so they are sorted first in the clearing process.
 * In case the clearing process needs to set a price by matching a market order
 * with a limit order, the clearing price is set by applying a "default margin"
 * to the limit order. If there are no limit orders in the match, then the
 * market clears at a fixed default clearing price. It's probably best if brokers
 * do not allow this to happen.</p>
 * @author John Collins
 */
@Service
public class AuctionService
  extends TimeslotPhaseProcessor
  implements InitializationService
{
  static private Logger log = LogManager.getLogger(AuctionService.class.getName());
  final Level BFAULT = Level.forName("BFAULT", 250);

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

  @Autowired
  private ServerConfiguration serverProps;

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "Default margin when matching market order with limit order")
  private double defaultMargin = 0.05; // used when one side has no limit price

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "Default price/mwh when matching only market orders")
  private double defaultClearingPrice = 40.00; // used when no limit prices

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "Proportion of market surplus allocated to the seller")
  private double sellerSurplusRatio = 0.5;

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "maximum seller margin")
  private double sellerMaxMargin = 0.05;

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "maximum market position at maximum leadtime")
  private double mktPosnLimitInitial = 90.0;

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "maximum market position at minimum leadtime")
  private double mktPosnLimitFinal = 143.0;

  private double epsilon = 1e-6; // position balance less than this is ignored

  private List<Order> incoming;

  private HashMap<Timeslot, ArrayList<OrderWrapper>> sortedBids;
  private HashMap<Timeslot, ArrayList<OrderWrapper>> sortedAsks;
  private List<Timeslot> enabledTimeslots = null;

  public AuctionService ()
  {
    super();
    incoming = new ArrayList<Order>();
  }

  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    incoming.clear();
    serverProps.configureMe(this);
    brokerProxyService.registerBrokerMessageListener(this, Order.class);
    super.init();
    serverProps.publishConfiguration(this);
    return "Auctioneer";
  }

  public double getSellerSurplusRatio ()
  {
    return sellerSurplusRatio;
  }

  public double getDefaultMargin ()
  {
    return defaultMargin;
  }

  public double getDefaultClearingPrice ()
  {
    return defaultClearingPrice;
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
  public void handleMessage (Order msg)
  {
    if (validateOrder(msg)) {
      // queue incoming message
      synchronized(incoming) {
        incoming.add(msg);
      }
      log.info("Received " + msg.toString());
    }
  }

  public boolean validateOrder (Order order)
  {
    if (order.getMWh().equals(Double.NaN) ||
        order.getMWh().equals(Double.POSITIVE_INFINITY) ||
        order.getMWh().equals(Double.NEGATIVE_INFINITY)) {
      log.log(BFAULT, "Order from " + order.getBroker().getUsername()
          + " with invalid quantity " + order.getMWh());
      return false;
    }

    double minQuantity = Competition.currentCompetition().getMinimumOrderQuantity();
    if (Math.abs(order.getMWh()) < minQuantity) {
      log.log(BFAULT, "Order from " + order.getBroker().getUsername()
               + " with quantity " + order.getMWh()
               + " < minimum quantity " + minQuantity);
      return false;
    }

    if (!timeslotRepo.isTimeslotEnabled(order.getTimeslot())) {
      OrderStatus status = new OrderStatus(order.getBroker(), order.getId());
      brokerProxyService.sendMessage(order.getBroker(), status);
      log.log(BFAULT, "Order from " + order.getBroker().getUsername()
               +" for disabled timeslot " + order.getTimeslot());
      return false;
    }
    return true;
  }

  // ------------------- Market clearing ------------------------
  /**
   * Processes incoming Order instances for each timeslot, generating the appropriate
   * MarketTransactions, Orderbooks, and ClearedTrade instances.
   */
  @Override
  public void activate (Instant time, int phaseNumber)
  {
    log.info("Activate");
    // Grab all the incoming marketOrders and sort them by price and timeslot
    ArrayList<OrderWrapper> orders;
    synchronized(incoming) {
      orders = new ArrayList<OrderWrapper>();
      for (Order order : incoming) {
        OrderWrapper sw = new OrderWrapper(order);
        if (sw.isValid()) {
          // ignore invalid orders
          orders.add(new OrderWrapper(order));
        }
        else {
          log.info("Ignoring invalid order " + order.getId() +
                   " from " + order.getBroker().getUsername());
        }
      }
      incoming.clear();
    }
    sortedAsks = new HashMap<Timeslot, ArrayList<OrderWrapper>>();
    sortedBids = new HashMap<Timeslot, ArrayList<OrderWrapper>>();
    // add bids and asks to the appropriate lists
    for (OrderWrapper sw : orders) {
      if (sw.isBuyOrder())
        addBid(sw);
      else
        addAsk(sw);
    }
    // then sort the lists
    for (ArrayList<OrderWrapper> list : sortedAsks.values()) {
      Collections.sort(list);
    }
    for (ArrayList<OrderWrapper> list : sortedBids.values()) {
      Collections.sort(list);
    }
    log.debug("activate: asks in " + sortedAsks.size() + " timeslots, bids in " +
        sortedBids.size() + " timeslots");
    
    // Iterate through the timeslots that were enabled at the end of the last
    // timeslot, and clear each one individually
    if (enabledTimeslots == null) {
      enabledTimeslots = timeslotRepo.enabledTimeslots();
    }
    collectAskRanges();
    for (Timeslot timeslot : enabledTimeslots) {
      clearTimeslot(timeslot);
    }

    // save a copy of the current set of enabled timeslots for the next clearing
    enabledTimeslots = new ArrayList<Timeslot>(timeslotRepo.enabledTimeslots());
  }

  private void clearTimeslot (Timeslot timeslot)
  {
    List<OrderWrapper> bids = sortedBids.get(timeslot);
    List<OrderWrapper> asks = sortedAsks.get(timeslot);
    if (null != bids)
      constrainMarketPositions(bids, timeslot.getSerialNumber());
    if (null != bids || null != asks) {
      // we have bids and/or asks to match up
      if (bids != null && asks != null)
        log.info("Timeslot " + timeslot.getSerialNumber() + 
                 ": Clearing " + asks.size() + " asks and " +
                 bids.size() + " bids");
      Double bidPrice = 0.0;
      Double askPrice = 0.0;
      double totalMWh = 0.0;
      ArrayList<PendingTrade> pendingTrades = new ArrayList<PendingTrade>();
      while (bids != null && !bids.isEmpty() &&
             asks != null && !asks.isEmpty() &&
             (bids.get(0).isMarketOrder() ||
                 asks.get(0).isMarketOrder() ||
                 -bids.get(0).getLimitPrice() >= asks.get(0).getLimitPrice())) {
        // transfer from ask to bid, keep track of qty
        OrderWrapper bid = bids.get(0);
        bidPrice = bid.getLimitPrice();
        OrderWrapper ask = asks.get(0);
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
        log.debug("bid remaining=" + (bid.getMWh() - bid.executionMWh));
        log.debug("ask remaining=" + (ask.getMWh() - ask.executionMWh));
        if (Math.abs(bid.getMWh() - bid.executionMWh) <= epsilon)
          bids.remove(bid);
        if (Math.abs(ask.getMWh() - ask.executionMWh) <= epsilon)
          asks.remove(ask);
      }
      double clearingPrice;
      if (bidPrice != null) {
        if (askPrice != null) {
          clearingPrice =
              askPrice + sellerSurplusRatio * (-bidPrice - askPrice);
          clearingPrice =
              Math.min(clearingPrice, askPrice * (1.0 + sellerMaxMargin));
        }
        else {
          // ask price is null
          clearingPrice = -bidPrice / (1.0 + defaultMargin);
          log.info("market clears at " + clearingPrice + " with null ask price");
        }
      }
      else {
        // bid price is null
        if (askPrice != null) {
          clearingPrice = askPrice * (1.0 + defaultMargin);
          log.info("market clears at " + clearingPrice + " with null bid price");
        }
        else {
          // both bid and ask are null
          clearingPrice = defaultClearingPrice;
          log.info("market clears at default clearing price"  + clearingPrice);
        }
      }
      for (PendingTrade trade : pendingTrades) {
        accountingService.addMarketTransaction(trade.from, timeslot,
                                               -trade.mWh, clearingPrice);
        accountingService.addMarketTransaction(trade.to, timeslot,
                                               trade.mWh, -clearingPrice);
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

  // Walks through a sorted list of bids, modifying quantities as necessary
  // to impose market position limits.
  private void constrainMarketPositions (List<OrderWrapper> bids, int ts)
  {
    HashMap<Broker, Double>remainingPosn = new HashMap<>();
    for (OrderWrapper bid: bids) {
      if (bid.getBroker().isWholesale())
        // Don't limit wholesale entities
        continue;
      double remaining = getRemaining(bid.getBroker(), remainingPosn, ts);
      remaining -= bid.getMWh();
      if (remaining < 0.0) {
        // adjust bid
        double qty = bid.getMWh() + remaining;
        remaining = 0.0;
        log.info("Adjusting bid of {} from {} to {}",
                 bid.getBroker().getUsername(),
                 bid.getMWh(), qty);
        bid.setMWh(qty);
      }
      remainingPosn.put(bid.getBroker(), remaining);
    }
  }

  // Returns remaining position for broker/timeslot
  private double getRemaining (Broker broker,
                               HashMap<Broker, Double> posns,
                               int ts)
  {
    Double result = posns.get(broker);
    if (null == result) {
      MarketPosition posn = broker.findMarketPositionByTimeslot(ts);
      // offset is zero for final ts
      int offset = ts - timeslotRepo.currentSerialNumber();
      double limit = mktPosnLimitFinal;
      if (enabledTimeslots.size() > 1) {
        limit -= (offset * (mktPosnLimitFinal - mktPosnLimitInitial)
                  / (enabledTimeslots.size() - 1));
      }
      result = Math.max(0.0, limit - posn.getOverallBalance());
      posns.put(broker, result);
    }
    return result;
  }

  private void addAsk (OrderWrapper marketOrder)
  {
    Timeslot timeslot = marketOrder.getTimeslot();
    if (sortedAsks.get(timeslot) == null) {
      sortedAsks.put(timeslot, new ArrayList<OrderWrapper>());
    }
    sortedAsks.get(timeslot).add(marketOrder);
  }

  private void addBid (OrderWrapper marketOrder)
  {
    Timeslot timeslot = marketOrder.getTimeslot();
    if (sortedBids.get(timeslot) == null) {
      sortedBids.put(timeslot, new ArrayList<OrderWrapper>());
    }
    sortedBids.get(timeslot).add(marketOrder);
  }

  // Collect min/max ask price ranges
  private void collectAskRanges ()
  {
    // Prepare to collect minimum ask prices
    Double[] minPriceArray = new Double[enabledTimeslots.size()];
    Double[] maxPriceArray = new Double[enabledTimeslots.size()];
    int timeslotIndex = 0;
    for (Timeslot timeslot : enabledTimeslots) {
      if (null == sortedAsks || null == sortedAsks.get(timeslot)) {
        minPriceArray[timeslotIndex] = null;
        maxPriceArray[timeslotIndex] = null;
      }
      else {
        int lastIndex = sortedAsks.get(timeslot).size() - 1;
        OrderWrapper minAsk = sortedAsks.get(timeslot).get(0);
        OrderWrapper maxAsk = sortedAsks.get(timeslot).get(lastIndex);
        if (null == minAsk || minAsk.isMarketOrder()) {
          minPriceArray[timeslotIndex] = null;
        }
        else {
          minPriceArray[timeslotIndex] = minAsk.getLimitPrice();
        }
        if (null == maxAsk || maxAsk.isMarketOrder()) {
          maxPriceArray[timeslotIndex] = null;
        }
        else {
          maxPriceArray[timeslotIndex] = maxAsk.getLimitPrice();
        }                  
      }
      timeslotIndex++;
    }
    // store min ask prices in orderbookRepo
    orderbookRepo.setMinAskPrices(minPriceArray);
    orderbookRepo.setMaxAskPrices(maxPriceArray);
  }

  // test support -- get rid of saved timeslots
  void clearEnabledTimeslots ()
  {
    enabledTimeslots = null;
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

  class OrderWrapper implements Comparable<OrderWrapper>
  {
    Order order;
    double executionMWh = 0.0;
    double adjustedMWh = 0.0;

    OrderWrapper(Order order)
    {
      super();
      this.order = order;
      this.adjustedMWh = order.getMWh();
    }

    // delegation API
    Broker getBroker ()
    {
      return order.getBroker();
    }

    // valid if qty is non-zero
    boolean isValid ()
    {
      return (adjustedMWh != 0.0);
    }

    boolean isMarketOrder ()
    {
      return (order.getLimitPrice() == null);
    }

    Double getLimitPrice ()
    {
      return order.getLimitPrice();
    }

    double getMWh ()
    {
      return adjustedMWh;
    }

    void setMWh (double newValue)
    {
      adjustedMWh = newValue;
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
    public int compareTo(OrderWrapper o) {
      OrderWrapper other = (OrderWrapper) o;
      // negative qty is ask
      double sign = Math.signum(this.getMWh());
      Double thisQty = sign * this.getMWh();
      Double otherQty = sign * other.getMWh();
      if (this.isMarketOrder())
        if (other.isMarketOrder())
          return compareQty(thisQty, otherQty);
        else
          return -1;
      else if (other.isMarketOrder())
        return 1;
      else
        if (this.getLimitPrice().equals(other.getLimitPrice())) {
          // qty is ascending for negative values, descending for positive values
          return compareQty(thisQty, otherQty);
        }
        else {
          // price is always ascending
          return (this.getLimitPrice() > other.getLimitPrice() ? 1 : -1);
        }
    }

    public boolean equals (OrderWrapper other) {
      if (this.isMarketOrder())
        if (other.isMarketOrder())
          return (this.getMWh() == other.getMWh());
        else
          return false;
      return (this.getLimitPrice().equals(other.getLimitPrice())
              && (this.getMWh() == other.getMWh()));
    }
  }

  private int compareQty (Double thisQty, Double otherQty)
  {
    return -thisQty.compareTo(otherQty);
  }
}
