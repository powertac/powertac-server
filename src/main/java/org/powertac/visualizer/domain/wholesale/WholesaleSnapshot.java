package org.powertac.visualizer.domain.wholesale;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Timeslot;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.json.WholesaleSnapshotJSON;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;

import java.util.*;

/**
 * Represents the wholesale market per-timeslot snapshot for one timeslot.
 * 
 * @author Jurica Babic
 * 
 */
public class WholesaleSnapshot
{

  private static final int MARKET_PRICE = 100;
  Logger log = LogManager.getLogger(WholesaleSnapshot.class.getName());

  // timeslot serial number value in which the snapshot is created.
  private int timeslotSerialNumberCreated;
  // millis in which the snapshot is created.
  private long millisCreated;
  // snapshot is built for this timeslot:
  private int timeslotSerialNumber;
  private Timeslot timeslot;
  private double totalTradedQuantity;

  private WholesaleSnapshotJSON wholesaleSnapshotJSON;
  // filled with actual received Orders that are previously converted to
  // VisualizerOrderbookOrder.
  private VisualizerOrderbook orders;
  // cleared trade, null if there is no trade
  private ClearedTrade clearedTrade;
  // standard orderbook:
  private VisualizerOrderbook orderbook;
  /*
   * indicates whether the observed snapshot is closed. Snapshot can be closed
   * when there is null price in orderbook, or when the clearedTrade object is
   * set.
   */
  private boolean closed;
  private boolean cleared;

  private VisualizerOrderbookOrder marketAskOrder;
  private VisualizerOrderbookOrder marketBidOrder;

  private List<VisualizerOrderbookOrder> beforeAsks;
  private List<VisualizerOrderbookOrder> beforeBids;
  private List<VisualizerOrderbookOrder> afterAsks;
  private List<VisualizerOrderbookOrder> afterBids;

  public WholesaleSnapshot (long millisCreated, Timeslot timeslot,
                            int timeslotSerialNumberCreated)
  {
    this.timeslotSerialNumber = timeslot.getSerialNumber();
    orders = new VisualizerOrderbook(timeslot, null, null);
    this.timeslot = timeslot;
    this.timeslotSerialNumberCreated = timeslotSerialNumberCreated;
    this.millisCreated = millisCreated;
  }

  public void addOrder (Order order)
  {

    VisualizerOrderbookOrder orderbookOrder =
      new VisualizerOrderbookOrder(order.getMWh(), order.getLimitPrice());

    // bid:
    if (order.getMWh() > 0) {

      if (order.getLimitPrice() == null) {
        marketBidOrder = orderbookOrder;
      }
      else {

        orders.addBid(orderbookOrder);
      }
    }
    else {

      if (order.getLimitPrice() == null) {
        marketAskOrder = orderbookOrder;
      }
      else {
        orders.addAsk(orderbookOrder);
      }
    }
  }

  public VisualizerOrderbook getOrders ()
  {
    return orders;
  }

  public void setOrders (VisualizerOrderbook orders)
  {
    this.orders = orders;
  }

  public ClearedTrade getClearedTrade ()
  {
    return clearedTrade;
  }

  public void setClearedTrade (ClearedTrade clearedTrade)
  {
    this.clearedTrade = clearedTrade;
    cleared = true;
  }

  public VisualizerOrderbook getOrderbook ()
  {
    return orderbook;
  }

  public void setOrderbook (VisualizerOrderbook orderbook)
  {
    this.orderbook = orderbook;
  }

  public Timeslot getTimeslot ()
  {
    return timeslot;
  }

  public WholesaleSnapshotJSON getWholesaleSnapshotJSON ()
  {
    return wholesaleSnapshotJSON;
  }

  @Override
  public String toString ()
  {
    // TODO Auto-generated method stub

    StringBuilder builder = new StringBuilder();
    builder.append("____________ORDERS:________\n");
    if (orders != null) {
      builder.append("ASKS:\n");
      SortedSet<VisualizerOrderbookOrder> asks = orders.getAsks();
      for (VisualizerOrderbookOrder orderbookOrder : asks) {
        builder.append(orderbookOrder).append("\n");
      }
      builder.append("ORDERS:\nBIDS:");
      SortedSet<VisualizerOrderbookOrder> bids = orders.getBids();
      for (VisualizerOrderbookOrder orderbookOrder: bids) {
        builder.append(orderbookOrder).append("\n");
      }
      if (marketAskOrder != null) {
        builder.append("MARKET ASK ORDER:\n");
        builder.append(marketAskOrder).append("\n");
      }
      if (marketBidOrder != null) {
        builder.append("MARKET BID ORDER:\n");
        builder.append(marketBidOrder).append("\n");
      }

    }
    builder.append("____________ORDERBOOK:________\n");

    if (orderbook != null) {
      builder.append("ASKS:\n");
      SortedSet<VisualizerOrderbookOrder> asks = orderbook.getAsks();
      for (VisualizerOrderbookOrder orderbookOrder: asks) {
        builder.append(orderbookOrder).append("\n");
      }
      builder.append("BIDS:\n");
      SortedSet<VisualizerOrderbookOrder> bids = orderbook.getBids();
      for (VisualizerOrderbookOrder orderbookOrder: bids) {
        builder.append(orderbookOrder).append("\n");
      }
    }

    return "" + builder + "\n\n" + clearedTrade;
  }

  public boolean isClosed ()
  {
    return closed;
  }

  public int getTimeslotSerialNumber ()
  {
    return timeslotSerialNumber;
  }

  /**
   * Snapshot should be closed in one of the following scenarios: 1) Orderbook
   * is received but does not have a clearing price (null) 2) ClearedTrade is
   * received.
   */
  public void close ()
  {
    finish();
    closed = true;
  }

  /**
   * Build JSON stuff for this snapshot.
   */
  private void finish ()
  {
    if (clearedTrade != null) {
      totalTradedQuantity =
        Helper.roundNumberTwoDecimal(clearedTrade.getExecutionMWh());
    }

    // create JSON for before-clearing graph:
    JSONArray graphDataBeforeClearing = new JSONArray();
    JSONArray seriesColorsBeforeClearing = new JSONArray();
    SortedSet<VisualizerOrderbookOrder> asks = orders.getAsks();
    SortedSet<VisualizerOrderbookOrder> bids = orders.getBids();

    modifyMarketOrders();

    buildBeforeData(asks, bids, graphDataBeforeClearing,
                    seriesColorsBeforeClearing);

    JSONArray graphDataAfterClearing = new JSONArray();
    JSONArray seriesColorsAfterClearing = new JSONArray();
    SortedSet<VisualizerOrderbookOrder> asksAfter;
    SortedSet<VisualizerOrderbookOrder> bidsAfter;
    if (null != orderbook) {
      asksAfter = orderbook.getAsks();
      bidsAfter = orderbook.getBids();
    }
    else {
      log.warn("orderbook is null");
      asksAfter = new TreeSet<VisualizerOrderbookOrder>();
      bidsAfter = new TreeSet<VisualizerOrderbookOrder>();
    }

    buildAfterData(asksAfter, bidsAfter, graphDataAfterClearing,
                   seriesColorsAfterClearing);

    wholesaleSnapshotJSON =
      new WholesaleSnapshotJSON(graphDataBeforeClearing,
                                seriesColorsBeforeClearing,
                                graphDataAfterClearing,
                                seriesColorsAfterClearing);

    log.debug("Snapshot " + this.getTimeslotSerialNumber() + "@"
              + this.getTimeslotSerialNumberCreated() + " is closed: \n"
              + this.toString());

    // make arraylists:
    beforeAsks = new ArrayList<VisualizerOrderbookOrder>(orders.getAsks());
    beforeBids = new ArrayList<VisualizerOrderbookOrder>(orders.getBids());

    if (null != orderbook) {
      afterAsks = new ArrayList<VisualizerOrderbookOrder>(orderbook.getAsks());
      afterBids = new ArrayList<VisualizerOrderbookOrder>(orderbook.getBids());
    }
    else {
      afterAsks = new ArrayList<VisualizerOrderbookOrder>();
      afterBids = new ArrayList<VisualizerOrderbookOrder>();
    }
  }

  private void modifyMarketOrders ()
  {
    SortedSet<VisualizerOrderbookOrder> asks = orders.getAsks();
    SortedSet<VisualizerOrderbookOrder> bids = orders.getBids();

    VisualizerOrderbookOrder lowestAsk = null;
    VisualizerOrderbookOrder highestAsk = null;
    VisualizerOrderbookOrder lowestBid = null;
    VisualizerOrderbookOrder highestBid = null;

    if (!asks.isEmpty()) {
      lowestAsk = asks.first();
      highestAsk = asks.last();
    }
    if (!bids.isEmpty()) {
      highestBid = bids.first();
      lowestBid = bids.last();
    }

    if (marketAskOrder != null) {
      double newPrice;
      if (highestBid != null && lowestAsk != null) {
        newPrice =
          (Math.abs(highestBid.getLimitPrice()) < Math.abs(lowestAsk
                  .getLimitPrice()))? 0.8 * Math
                  .abs(highestBid.getLimitPrice()): 0.8 * Math.abs(lowestAsk
                  .getLimitPrice());

      }
      else {
        newPrice = MARKET_PRICE;
      }
      marketAskOrder =
        new VisualizerOrderbookOrder(marketAskOrder.getMWh(), newPrice);
    }
    if (marketBidOrder != null) {
      double newPrice;
      if (lowestBid != null && highestAsk != null) {
        newPrice =
          (Math.abs(lowestBid.getLimitPrice()) > Math.abs(highestAsk
                  .getLimitPrice()))? -1.2
                                      * Math.abs(lowestBid.getLimitPrice())
                                    : -1.2
                                      * Math.abs(highestAsk.getLimitPrice());
      }
      else {
        newPrice = -1 * MARKET_PRICE;
      }
      marketBidOrder =
        new VisualizerOrderbookOrder(marketBidOrder.getMWh(), newPrice);
    }

  }

  private void buildAfterData (SortedSet<VisualizerOrderbookOrder> asksAfter,
                               SortedSet<VisualizerOrderbookOrder> bidsAfter,
                               JSONArray graphDataAfterClearing,
                               JSONArray seriesColorsAfterClearing)
  {

    double clearedTradeOffset = 0;
    if (clearedTrade != null) {
      clearedTradeOffset =
        buildLine(Math.abs(clearedTrade.getExecutionMWh()),
                  Math.abs(clearedTrade.getExecutionPrice()),
                  graphDataAfterClearing, seriesColorsAfterClearing, 0,
                  WholesaleSnapshotJSON.getClearedTradeColor());
    }

    // asks:
    double askOffset = clearedTradeOffset;
    for (VisualizerOrderbookOrder orderbookOrder: asksAfter) {
      // if a market order in orderbook can be found:
      if (orderbookOrder.getLimitPrice() == null) {
        VisualizerOrderbookOrder newMarketAskOrder =
            modifyMarketOrder(orderbookOrder, asksAfter);
        askOffset =
            buildLine(Math.abs(newMarketAskOrder.getMWh()),
                Math.abs(newMarketAskOrder.getLimitPrice()),
                graphDataAfterClearing, seriesColorsAfterClearing,
                askOffset, WholesaleSnapshotJSON.getMarketAskOrderColor());
      }
      else {
        askOffset =
            buildLine(Math.abs(orderbookOrder.getMWh()),
                Math.abs(orderbookOrder.getLimitPrice()),
                graphDataAfterClearing, seriesColorsAfterClearing,
                askOffset, WholesaleSnapshotJSON.getLimitAskOrderColor());
      }

    }
    // bids:
    double bidOffset = clearedTradeOffset;
    for (VisualizerOrderbookOrder orderbookOrder: bidsAfter) {
      // if a market order in orderbook can be found:
      if (orderbookOrder.getLimitPrice() == null) {
        VisualizerOrderbookOrder newMarketAskOrder =
            modifyMarketOrder(orderbookOrder, bidsAfter);
        bidOffset =
            buildLine(Math.abs(newMarketAskOrder.getMWh()),
                Math.abs(newMarketAskOrder.getLimitPrice()),
                graphDataAfterClearing, seriesColorsAfterClearing,
                bidOffset, WholesaleSnapshotJSON.getMarketBidOrderColor());
      }
      else {
        bidOffset =
            buildLine(Math.abs(orderbookOrder.getMWh()),
                Math.abs(orderbookOrder.getLimitPrice()),
                graphDataAfterClearing, seriesColorsAfterClearing,
                bidOffset, WholesaleSnapshotJSON.getLimitBidOrderColor());
      }
    }

  }

  private void buildBeforeData (SortedSet<VisualizerOrderbookOrder> asks,
                                SortedSet<VisualizerOrderbookOrder> bids,
                                JSONArray graphData, JSONArray seriesColors)
  {
    double askOffset = 0;
    if (marketAskOrder != null) {
      askOffset =
        buildLine(Math.abs(marketAskOrder.getMWh()),
                  Math.abs(marketAskOrder.getLimitPrice()), graphData,
                  seriesColors, askOffset,
                  WholesaleSnapshotJSON.getMarketAskOrderColor());
    }
    for (VisualizerOrderbookOrder orderbookOrder: asks) {
      askOffset =
          buildLine(Math.abs(orderbookOrder.getMWh()),
              Math.abs(orderbookOrder.getLimitPrice()), graphData,
              seriesColors, askOffset,
              WholesaleSnapshotJSON.getLimitAskOrderColor());
    }

    double bidOffset = 0;
    if (marketBidOrder != null) {
      bidOffset =
        buildLine(Math.abs(marketBidOrder.getMWh()),
                  Math.abs(marketBidOrder.getLimitPrice()), graphData,
                  seriesColors, bidOffset,
                  WholesaleSnapshotJSON.getMarketBidOrderColor());
    }

    for (VisualizerOrderbookOrder orderbookOrder: bids) {
      bidOffset =
          buildLine(Math.abs(orderbookOrder.getMWh()),
              Math.abs(orderbookOrder.getLimitPrice()), graphData,
              seriesColors, bidOffset,
              WholesaleSnapshotJSON.getLimitBidOrderColor());

    }
  }

  private VisualizerOrderbookOrder
    modifyMarketOrder (VisualizerOrderbookOrder marketOrder,
                       SortedSet<VisualizerOrderbookOrder> offers)
  {
    // get rid of a null price in market order:
    try {
      VisualizerOrderbookOrder first = offers.first();
      Double limitPrice = first.getLimitPrice();
      double newMarketPrice;

      // First element in "offers" set is order with a null price:
      if (limitPrice == null) {
        // bid
        if (first.getMWh() > 0) {
          newMarketPrice = -1 * MARKET_PRICE;
        }
        else {
          // ask
          newMarketPrice = MARKET_PRICE;
        }
      }
      else {
        // First element in "offers" set does not have a null price, it is an
        // actual number:
        newMarketPrice =
          (first.getMWh() > 0)? (-1.2) * limitPrice: (0.8) * limitPrice;
      }

      return new VisualizerOrderbookOrder(marketOrder.getMWh(), newMarketPrice);
    }
    catch (NoSuchElementException e) {
      log.debug("market order is forever alone.");
    }
    return new VisualizerOrderbookOrder(marketOrder.getMWh(), 0.0);
  }

  private double
    buildLine (double x, double y, JSONArray graphData, JSONArray seriesColors,
               double offset, String color)
  {

    try {
      double leftX_Axis = offset;
      JSONArray lineData = new JSONArray();
      JSONArray leftCoordData = new JSONArray();
      JSONArray rightCoordData = new JSONArray();

      leftCoordData.put(leftX_Axis).put(y);
      offset += x;
      rightCoordData.put(offset).put(y);

      // make line:
      lineData.put(leftCoordData).put(rightCoordData);
      // add it to graphData:
      graphData.put(lineData);

      // color:
      seriesColors.put(color);

      return offset;

    }
    catch (JSONException e) {
      log.warn("Building JSON Array failed.");
    }
    return 0;
  }

  public String getName ()
  {
    return "Snapshot" + timeslotSerialNumberCreated;
  }

  public String getType ()
  {
    return "Wholsale snapshot";
  }

  public String getTotalTradedQuantity ()
  {
    return "" + totalTradedQuantity;
  }

  public int getTimeslotSerialNumberCreated ()
  {
    return timeslotSerialNumberCreated;
  }

  public VisualizerOrderbookOrder getMarketAskOrder ()
  {
    return marketAskOrder;
  }

  public VisualizerOrderbookOrder getMarketBidOrder ()
  {
    return marketBidOrder;
  }

  public List<VisualizerOrderbookOrder> getAfterAsks ()
  {
    return afterAsks;
  }

  public List<VisualizerOrderbookOrder> getAfterBids ()
  {
    return afterBids;
  }

  public List<VisualizerOrderbookOrder> getBeforeAsks ()
  {
    return beforeAsks;
  }

  public List<VisualizerOrderbookOrder> getBeforeBids ()
  {
    return beforeBids;
  }

  public long getMillisCreated ()
  {
    return millisCreated;
  }

  public boolean isCleared ()
  {
    return cleared;
  }
}
