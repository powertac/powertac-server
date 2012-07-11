package org.powertac.visualizer.domain.wholesale;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;
import org.powertac.common.Timeslot;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.json.WholesaleSnapshotJSON;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;

/**
 * Represents the wholesale market per-timeslot snapshot for one timeslot.
 * 
 * @author Jurica Babic
 * 
 */
public class WholesaleSnapshot {

	private static final int MARKET_PRICE = 100;
	Logger log = Logger.getLogger(WholesaleSnapshot.class.getName());

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
	// OrderbookOrder.
	private Orderbook orders;
	// cleared trade, null if there is no trade
	private ClearedTrade clearedTrade;
	// standard orderbook:
	private Orderbook orderbook;
	/*
	 * indicates whether the observed snapshot is closed. Snapshot can be closed
	 * when there is null price in orderbook, or when the clearedTrade object is
	 * set.
	 */
	private boolean closed;
	private boolean cleared;

	private OrderbookOrder marketAskOrder;
	private OrderbookOrder marketBidOrder;

	private List<OrderbookOrder> beforeAsks;
	private List<OrderbookOrder> beforeBids;
	private List<OrderbookOrder> afterAsks;
	private List<OrderbookOrder> afterBids;

	public WholesaleSnapshot(long millisCreated, Timeslot timeslot, int timeslotSerialNumberCreated) {
		this.timeslotSerialNumber = timeslot.getSerialNumber();
		orders = new Orderbook(timeslot, null, null);
		this.timeslot = timeslot;
		this.timeslotSerialNumberCreated = timeslotSerialNumberCreated;
		this.millisCreated = millisCreated;
	}

	public void addOrder(Order order) {

		OrderbookOrder orderbookOrder = new OrderbookOrder(order.getMWh(), order.getLimitPrice());

		// bid:
		if (order.getMWh() > 0) {

			if (order.getLimitPrice() == null) {
				marketBidOrder = orderbookOrder;
			} else {

				orders.addBid(orderbookOrder);
			}
		} else {

			if (order.getLimitPrice() == null) {
				marketAskOrder = orderbookOrder;
			} else {
				orders.addAsk(orderbookOrder);
			}
		}
	}

	public Orderbook getOrders() {
		return orders;
	}

	public void setOrders(Orderbook orders) {
		this.orders = orders;
	}

	public ClearedTrade getClearedTrade() {
		return clearedTrade;
	}

	public void setClearedTrade(ClearedTrade clearedTrade) {
		this.clearedTrade = clearedTrade;
		cleared = true;
	}

	public Orderbook getOrderbook() {
		return orderbook;
	}

	public void setOrderbook(Orderbook orderbook) {
		this.orderbook = orderbook;
	}

	public Timeslot getTimeslot() {
		return timeslot;
	}

	public WholesaleSnapshotJSON getWholesaleSnapshotJSON() {
		return wholesaleSnapshotJSON;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub

		StringBuilder builder = new StringBuilder();
		builder.append("____________ORDERS:________\n");
		if (orders != null) {
			builder.append("ASKS:\n");
			SortedSet<OrderbookOrder> asks = orders.getAsks();
			for (Iterator iterator = asks.iterator(); iterator.hasNext();) {
				OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
				builder.append(orderbookOrder).append("\n");
			}
			builder.append("ORDERS:\nBIDS:");
			SortedSet<OrderbookOrder> bids = orders.getBids();
			for (Iterator iterator = bids.iterator(); iterator.hasNext();) {
				OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
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
			SortedSet<OrderbookOrder> asks = orderbook.getAsks();
			for (Iterator iterator = asks.iterator(); iterator.hasNext();) {
				OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
				builder.append(orderbookOrder).append("\n");
			}
			builder.append("BIDS:\n");
			SortedSet<OrderbookOrder> bids = orderbook.getBids();
			for (Iterator iterator = bids.iterator(); iterator.hasNext();) {
				OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
				builder.append(orderbookOrder).append("\n");
			}
		}

		return "" + builder + "\n\n" + clearedTrade;
	}

	public boolean isClosed() {
		return closed;
	}

	public int getTimeslotSerialNumber() {
		return timeslotSerialNumber;
	}

	/**
	 * Snapshot should be closed in one of the following scenarios: 1) Orderbook
	 * is received but does not have a clearing price (null) 2) ClearedTrade is
	 * received.
	 */
	public void close() {
		finish();
		closed = true;
	}

	/**
	 * Build JSON stuff for this snapshot.
	 */
	private void finish() {

		if (clearedTrade != null) {
			totalTradedQuantity = Helper.roundNumberTwoDecimal(clearedTrade.getExecutionMWh());
		}

		// create JSON for before-clearing graph:
		JSONArray graphDataBeforeClearing = new JSONArray();
		JSONArray seriesColorsBeforeClearing = new JSONArray();
		SortedSet<OrderbookOrder> asks = orders.getAsks();
		SortedSet<OrderbookOrder> bids = orders.getBids();

		modifyMarketOrders();

		buildBeforeData(asks, bids, graphDataBeforeClearing, seriesColorsBeforeClearing);

		JSONArray graphDataAfterClearing = new JSONArray();
		JSONArray seriesColorsAfterClearing = new JSONArray();
		SortedSet<OrderbookOrder> asksAfter = orderbook.getAsks();
		SortedSet<OrderbookOrder> bidsAfter = orderbook.getBids();

		buildAfterData(asksAfter, bidsAfter, graphDataAfterClearing, seriesColorsAfterClearing);

		WholesaleSnapshotJSON json = new WholesaleSnapshotJSON(graphDataBeforeClearing, seriesColorsBeforeClearing,
				graphDataAfterClearing, seriesColorsAfterClearing);
		wholesaleSnapshotJSON = json;

		log.debug("Snapshot " + this.getTimeslotSerialNumber() + "@" + this.getTimeslotSerialNumberCreated()
				+ " is closed: \n" + this.toString());

		// make arraylists:
		beforeAsks = new ArrayList<OrderbookOrder>(orders.getAsks());
		beforeBids = new ArrayList<OrderbookOrder>(orders.getBids());

		afterAsks = new ArrayList<OrderbookOrder>(orderbook.getAsks());
		afterBids = new ArrayList<OrderbookOrder>(orderbook.getBids());

	}

	private void modifyMarketOrders() {
		SortedSet<OrderbookOrder> asks = orders.getAsks();
		SortedSet<OrderbookOrder> bids = orders.getBids();

		OrderbookOrder lowestAsk = null;
		OrderbookOrder highestAsk = null;
		OrderbookOrder lowestBid = null;
		OrderbookOrder highestBid = null;

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
				newPrice = (Math.abs(highestBid.getLimitPrice()) < Math.abs(lowestAsk.getLimitPrice())) ? 0.8 * Math
						.abs(highestBid.getLimitPrice()) : 0.8 * Math.abs(lowestAsk.getLimitPrice());

			} else {
				newPrice = MARKET_PRICE;
			}
			marketAskOrder = new OrderbookOrder(marketAskOrder.getMWh(), newPrice);
		}
		if (marketBidOrder != null) {
			double newPrice;
			if (lowestBid != null && highestAsk != null) {
				newPrice = (Math.abs(lowestBid.getLimitPrice()) > Math.abs(highestAsk.getLimitPrice())) ? -1.2
						* Math.abs(lowestBid.getLimitPrice()) : -1.2 * Math.abs(highestAsk.getLimitPrice());
			} else {
				newPrice = -1 * MARKET_PRICE;
			}
			marketBidOrder = new OrderbookOrder(marketBidOrder.getMWh(), newPrice);
		}

	}

	private void buildAfterData(SortedSet<OrderbookOrder> asksAfter, SortedSet<OrderbookOrder> bidsAfter,
			JSONArray graphDataAfterClearing, JSONArray seriesColorsAfterClearing) {

		double clearedTradeOffset = 0;
		if (clearedTrade != null) {
			clearedTradeOffset = buildLine(Math.abs(clearedTrade.getExecutionMWh()),
					Math.abs(clearedTrade.getExecutionPrice()), graphDataAfterClearing, seriesColorsAfterClearing, 0,
					WholesaleSnapshotJSON.getClearedTradeColor());
		}

		// asks:
		double askOffset = clearedTradeOffset;
		for (Iterator iterator = asksAfter.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();

			// if a market order in orderbook can be found:
			if (orderbookOrder.getLimitPrice() == null) {
				OrderbookOrder newMarketAskOrder = modifyMarketOrder(orderbookOrder, asksAfter);
				askOffset = buildLine(Math.abs(newMarketAskOrder.getMWh()),
						Math.abs(newMarketAskOrder.getLimitPrice()), graphDataAfterClearing, seriesColorsAfterClearing,
						askOffset, WholesaleSnapshotJSON.getMarketAskOrderColor());
			} else {
				askOffset = buildLine(Math.abs(orderbookOrder.getMWh()), Math.abs(orderbookOrder.getLimitPrice()),
						graphDataAfterClearing, seriesColorsAfterClearing, askOffset,
						WholesaleSnapshotJSON.getLimitAskOrderColor());
			}

		}
		// bids:
		double bidOffset = clearedTradeOffset;
		for (Iterator iterator = bidsAfter.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
			// if a market order in orderbook can be found:
			if (orderbookOrder.getLimitPrice() == null) {
				OrderbookOrder newMarketAskOrder = modifyMarketOrder(orderbookOrder, bidsAfter);
				bidOffset = buildLine(Math.abs(newMarketAskOrder.getMWh()),
						Math.abs(newMarketAskOrder.getLimitPrice()), graphDataAfterClearing, seriesColorsAfterClearing,
						bidOffset, WholesaleSnapshotJSON.getMarketBidOrderColor());
			} else {

				bidOffset = buildLine(Math.abs(orderbookOrder.getMWh()), Math.abs(orderbookOrder.getLimitPrice()),
						graphDataAfterClearing, seriesColorsAfterClearing, bidOffset,
						WholesaleSnapshotJSON.getLimitBidOrderColor());
			}
		}

	}

	private void buildBeforeData(SortedSet<OrderbookOrder> asks, SortedSet<OrderbookOrder> bids, JSONArray graphData,
			JSONArray seriesColors) {
		double askOffset = 0;
		if (marketAskOrder != null) {

			askOffset = buildLine(Math.abs(marketAskOrder.getMWh()), Math.abs(marketAskOrder.getLimitPrice()),
					graphData, seriesColors, askOffset, WholesaleSnapshotJSON.getMarketAskOrderColor());
		}
		for (Iterator iterator = asks.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();

			askOffset = buildLine(Math.abs(orderbookOrder.getMWh()), Math.abs(orderbookOrder.getLimitPrice()),
					graphData, seriesColors, askOffset, WholesaleSnapshotJSON.getLimitAskOrderColor());

		}

		double bidOffset = 0;
		if (marketBidOrder != null) {
			bidOffset = buildLine(Math.abs(marketBidOrder.getMWh()), Math.abs(marketBidOrder.getLimitPrice()),
					graphData, seriesColors, bidOffset, WholesaleSnapshotJSON.getMarketBidOrderColor());
		}

		for (Iterator iterator = bids.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
			bidOffset = buildLine(Math.abs(orderbookOrder.getMWh()), Math.abs(orderbookOrder.getLimitPrice()),
					graphData, seriesColors, bidOffset, WholesaleSnapshotJSON.getLimitBidOrderColor());

		}

	}

	private OrderbookOrder modifyMarketOrder(OrderbookOrder marketOrder, SortedSet<OrderbookOrder> offers) {
		// get rid of a null price in market order:
		try {
			OrderbookOrder first = offers.first();
			Double limitPrice = first.getLimitPrice();
			double newMarketPrice;
			
			//First element in "offers" set is order with a null price:
			if (limitPrice == null) {
				// bid
				if (first.getMWh() > 0) {
					newMarketPrice = -1 * MARKET_PRICE;
				} else {
					// ask
					newMarketPrice = MARKET_PRICE;
				}
			} else {
				// First element in "offers" set does not have a null price, it is an actual number:
				newMarketPrice = (first.getMWh() > 0) ? (-1.2) * limitPrice : (0.8) * limitPrice;
			}

			return new OrderbookOrder(marketOrder.getMWh(), newMarketPrice);

		} catch (NoSuchElementException e) {
			log.debug("market order is forever alone.");
		}
		return new OrderbookOrder(marketOrder.getMWh(), 0.0);

	}

	private double buildLine(double x, double y, JSONArray graphData, JSONArray seriesColors, double offset,
			String color) {

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

		} catch (JSONException e) {
			log.warn("Building JSON Array failed.");
		}
		return 0;
	}

	public String getName() {
		return "Snapshot" + timeslotSerialNumberCreated;
	}

	public String getType() {
		return "Wholsale snapshot";
	}

	public String getTotalTradedQuantity() {
		return "" + totalTradedQuantity;
	}

	public int getTimeslotSerialNumberCreated() {
		return timeslotSerialNumberCreated;
	}

	public OrderbookOrder getMarketAskOrder() {
		return marketAskOrder;
	}

	public OrderbookOrder getMarketBidOrder() {
		return marketBidOrder;
	}

	public List<OrderbookOrder> getAfterAsks() {
		return afterAsks;
	}

	public List<OrderbookOrder> getAfterBids() {
		return afterBids;
	}

	public List<OrderbookOrder> getBeforeAsks() {
		return beforeAsks;
	}

	public List<OrderbookOrder> getBeforeBids() {
		return beforeBids;
	}
	
	public long getMillisCreated() {
		return millisCreated;
	}
	
	public boolean isCleared() {
		return cleared;
	}
}
