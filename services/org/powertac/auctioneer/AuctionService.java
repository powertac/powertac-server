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
import org.powertac.common.Shout;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.enumerations.ProductType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.state.StateChange;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the wholesale day-ahead market. Energy is traded in future timeslots by
 * submitting Shouts representing bids and asks. Each specifies a price (minimum price
 * for asks, maximum price for bids) and a quantity in mWh. Once during each timeslot, the
 * market is cleared by matching bids with asks such that the bid price is no lower than
 * the ask price, and allocating quantities, until no matching bids or asks are
 * available. In general, the last matched bid will have a higher price than the last
 * matched ask. All trades are cleared at a price determined by splitting the difference
 * between the last bid and the last ask according to the value of sellerSurplusRatio,
 * which is a parameter set in the initialization process. 
 * @author John Collins
 */
public class AuctionService implements BrokerMessageListener, TimeslotPhaseProcessor
{
  static private Logger log = Logger.getLogger(AuctionService.class.getName());

  //@Autowired
  //private TimeService timeService;
  
  @Autowired
  private Accounting accountingService;
  
  @Autowired
  private CompetitionControl competitionControlService;
  
  @Autowired
  private BrokerProxy brokerProxyService;
  
  @Autowired
  private TimeService timeService;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  private double defaultSellerSurplus = 0.5;
  private double sellerSurplusRatio;
  
  int simulationPhase = 2;

  private List<Shout> incoming;
  
  private HashMap<Timeslot, TreeSet<ShoutWrapper>> sortedBids;
  private HashMap<Timeslot, TreeSet<ShoutWrapper>> sortedAsks;
  
  public AuctionService ()
  {
    super();
    incoming = new ArrayList<Shout>();
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
    competitionControlService.registerTimeslotPhase(this, simulationPhase);  
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

  int getSimulationPhase ()
  {
    return simulationPhase;
  }

  List<Shout> getIncoming ()
  {
    return incoming;
  }

  // ----------------- Broker message API --------------------
  /**
   * Receives, validates, and queues an incoming Shout message. Processing the incoming
   * Shouts happens during Phase 2 in each timeslot.
   */
  public void receiveMessage (Object msg)
  {
    if (msg != null && msg instanceof Shout) {
      if (validateShout((Shout)msg)) {
        // queue incoming message
        synchronized(incoming) {
          incoming.add((Shout)msg);
        }
      }
    }
  }
  
  public boolean validateShout (Shout shout)
  {
    // TODO - give feedback to broker if possible.
    if (!shout.getTimeslot().isEnabled()) {
      log.error("Shout submitted for disabled timeslot");
      return false;
    }
    else if (ProductType.Future != shout.getProduct()) {
      log.error("Shout for unsupported product type " + shout.getProduct());
      return false;
    }
    return true;
  }

  // ------------------- Market clearing ------------------------
  /**
   * Processes incoming Shout instances for each timeslot, generating the appropriate
   * MarketTransactions, Orderbooks, and ClearedTrade instances.
   */
  public void activate (Instant time, int phaseNumber)
  {
    // Grab all the incoming Shouts and sort them by price and timeslot
    ArrayList<ShoutWrapper> shouts;
    synchronized(incoming) {
      shouts = new ArrayList<ShoutWrapper>();
      for (Shout shout : incoming) {
        shouts.add(new ShoutWrapper(shout));
      }
      incoming.clear();
    }
    sortedAsks = new HashMap<Timeslot, TreeSet<ShoutWrapper>>();
    sortedBids = new HashMap<Timeslot, TreeSet<ShoutWrapper>>();
    for (ShoutWrapper sw : shouts) {
      if (sw.shout.getOrderType() == Shout.OrderType.BUY)
        addBid(sw);
      else
        addAsk(sw);
    }
    
    // Iterate through the enabled timeslots, and clear each one individually
    for (Timeslot timeslot : timeslotRepo.enabledTimeslots()) {
      clearTimeslot(timeslot);
    }
  }

  private void clearTimeslot (Timeslot timeslot)
  {
    SortedSet<ShoutWrapper> bids = sortedBids.get(timeslot);
    SortedSet<ShoutWrapper> asks = sortedAsks.get(timeslot);
    if (bids != null || asks != null) {
      // we have bids and/or asks to match up
      double bidPrice = 0.0;
      double askPrice = 0.0;
      double totalMWh = 0.0;
      ArrayList<PendingTrade> pendingTrades = new ArrayList<PendingTrade>();
      while (bids != null && bids.size() > 0 && 
             asks != null && asks.size() > 0 &&
             bids.first().getLimitPrice() >= asks.first().getLimitPrice()) {
        // transfer from ask to bid, keep track of qty
        ShoutWrapper bid = bids.first();
        bidPrice = bid.getLimitPrice();
        ShoutWrapper ask = asks.first();
        askPrice = ask.getLimitPrice();
        // amount to transfer is minimum of remaining bid qty and remaining ask qty
        log.debug("ask: " + ask.executionMWh + " used out of " + ask.getMWh() +
                  "; bid: " + bid.executionMWh + " used out of " + bid.getMWh());
        double transfer = Math.min(bid.getMWh() - bid.executionMWh,
                                   ask.getMWh() - ask.executionMWh);
        if (transfer > 0.0) {
          log.debug("transfer " + transfer + " from " + 
                    ask.getBroker().getUsername() + " at " + askPrice + " to " +
                    bid.getBroker().getUsername() + " at " + bidPrice);
          totalMWh += transfer;
          pendingTrades.add(new PendingTrade(ask.getBroker(), bid.getBroker(), transfer));
          bid.executionMWh += transfer;
          ask.executionMWh += transfer;
        }
        if (bid.getMWh() - bid.executionMWh <= 0.0)
          bids.remove(bid);
        if (ask.getMWh() - ask.executionMWh <= 0.0)
          asks.remove(ask);
      }
      double clearingPrice = askPrice + sellerSurplusRatio * (bidPrice - askPrice);
      for (PendingTrade trade : pendingTrades) {
        accountingService.addMarketTransaction(trade.from, timeslot,
                                               clearingPrice, trade.mWh);
        accountingService.addMarketTransaction(trade.to, timeslot,
                                               clearingPrice, trade.mWh);
      }
      // create the orderbook and cleared-trade, send to brokers
      Orderbook orderbook = new Orderbook(timeslot, ProductType.Future,
                                          (pendingTrades.size() > 0
                                              ? clearingPrice : null),
                                          timeService.getCurrentTime());
      if (bids != null) {
        for (ShoutWrapper shout : bids) {
          orderbook.addBid(new OrderbookOrder(shout.getOrderType(), shout.getLimitPrice(),
                                               shout.getMWh() - shout.executionMWh));
        }
      }
      if (asks != null) {
        for (ShoutWrapper shout : asks) {
          orderbook.addAsk(new OrderbookOrder(shout.getOrderType(), shout.getLimitPrice(),
                                               shout.getMWh() - shout.executionMWh));
        }
      }
      brokerProxyService.broadcastMessage(orderbook);
      if (totalMWh > 0.0) {
        ClearedTrade trade = new ClearedTrade(timeslot, ProductType.Future,
                                              clearingPrice, totalMWh,
                                              timeService.getCurrentTime());
        brokerProxyService.broadcastMessage(trade);
      }
    }
  }

  private void addAsk (ShoutWrapper shout)
  {
    Timeslot timeslot = shout.getTimeslot();
    if (sortedAsks.get(timeslot) == null) {
      sortedAsks.put(timeslot, new TreeSet<ShoutWrapper>(new ShoutSorter()));
    }
    sortedAsks.get(timeslot).add(shout);
  }

  private void addBid (ShoutWrapper shout)
  {
    Timeslot timeslot = shout.getTimeslot();
    if (sortedBids.get(timeslot) == null) {
      sortedBids.put(timeslot, new TreeSet<ShoutWrapper>(new ShoutSorter()));
    }
    sortedBids.get(timeslot).add(shout);
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
  
  class ShoutWrapper
  {
    Shout shout;
    double executionMWh = 0.0;
    
    ShoutWrapper(Shout shout)
    {
      super();
      this.shout = shout;
    }
    
    // delegation API
    Broker getBroker ()
    {
      return shout.getBroker();
    }
    
    double getLimitPrice ()
    {
      return shout.getLimitPrice();
    }
    
    double getMWh ()
    {
      return shout.getMWh();
    }
    
    Timeslot getTimeslot ()
    {
      return shout.getTimeslot();
    }
    
    Shout.OrderType getOrderType()
    {
      return shout.getOrderType();
    }
  }
  
  class ShoutSorter implements Comparator<ShoutWrapper>
  {
    double mult = 1000.0;
    
    public int compare (ShoutWrapper shout0, ShoutWrapper shout1)
    {
      if (shout0.getOrderType() == Shout.OrderType.BUY)
        return (int)(mult * (shout1.getLimitPrice() - shout0.getLimitPrice()));
      else
        return (int)(mult * (shout0.getLimitPrice() - shout1.getLimitPrice()));
    }
  }
}
