package org.powertac.visualizer.wholesale;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;
import org.powertac.common.Timeslot;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;

/**
 * Represents the wholesale market per-timeslot snapshot for one timeslot.
 * 
 * @author Jurica Babic
 * 
 */
public class WholesaleSnapshot {
	Logger log = Logger.getLogger(WholesaleSnapshot.class.getName());
	// relative index in which snapshot was created.
	private int relativeTimeslotIndex;
	// snapshot is built for this timeslot:
	private int timeslotSerialNumber;
	private Timeslot timeslot;

	private WholesaleMarketJSON wholesaleMarketJSON;
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

	private OrderbookOrder marketAskOrder;
	private OrderbookOrder marketBidOrder;

	private boolean closed;

	public WholesaleSnapshot(Timeslot timeslot, int relativeTimeslotIndex) {
		this.timeslotSerialNumber = timeslot.getSerialNumber();
		orders = new Orderbook(timeslot, null, null);
		this.timeslot = timeslot;
		this.relativeTimeslotIndex = relativeTimeslotIndex;
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

	public int getRelativeTimeslotIndex() {
		return relativeTimeslotIndex;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub

		StringBuilder builder = new StringBuilder();

		if (orders != null) {
			builder.append("ORDERS:\nASKS:");
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
		}

		if (orderbook != null) {
			builder.append("\nORDERBOOK:\nASKS:");
			SortedSet<OrderbookOrder> asks = orderbook.getAsks();
			for (Iterator iterator = asks.iterator(); iterator.hasNext();) {
				OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
				builder.append(orderbookOrder).append("\n");
			}
			builder.append("\nORDERBOOK:\nBIDS:");
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
		// create JSON for before-clearing graph:
		JSONArray graphDataBeforeClearing = new JSONArray();
		JSONArray seriesColorsBeforeClearing = new JSONArray();

		SortedSet<OrderbookOrder> asks = orders.getAsks();
		double askOffset = 0;
		if (marketAskOrder != null) {
			OrderbookOrder newMarketAskOrder = modifyMarketOrder(marketAskOrder, asks);
			askOffset = buildLine(
					Math.abs(newMarketAskOrder.getMWh()),
							Math.abs(newMarketAskOrder.getLimitPrice()), graphDataBeforeClearing,
					seriesColorsBeforeClearing, askOffset, "#FF0000");
		}
		for (Iterator iterator = asks.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
			
			askOffset = buildLine(
					Math.abs(orderbookOrder.getMWh()),
							Math.abs(orderbookOrder.getLimitPrice()), graphDataBeforeClearing,
					seriesColorsBeforeClearing, askOffset, "#00FF00");
			
		}
		

		SortedSet<OrderbookOrder> bids = orders.getBids();
		double bidOffset = 0;
		if (marketBidOrder != null) {
			OrderbookOrder newMarketAskOrder = modifyMarketOrder(marketBidOrder, bids);
			bidOffset = buildLine(
					Math.abs(newMarketAskOrder.getMWh()),
							Math.abs(newMarketAskOrder.getLimitPrice()), graphDataBeforeClearing,
					seriesColorsBeforeClearing, bidOffset,"#FF0000");
		}
		
		for (Iterator iterator = bids.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
			bidOffset = buildLine(
					Math.abs(orderbookOrder.getMWh()),
							Math.abs(orderbookOrder.getLimitPrice()), graphDataBeforeClearing,
					seriesColorsBeforeClearing, bidOffset, "#0000FF");
			
		}
		
		WholesaleMarketJSON json = new WholesaleMarketJSON(graphDataBeforeClearing,seriesColorsBeforeClearing);
		System.out.println(json.getGraphDataBeforeClearing().toString()+json.getSeriesColorsBeforeClearing().toString());
		wholesaleMarketJSON= json;

	}

	private OrderbookOrder modifyMarketOrder(OrderbookOrder marketOrder, SortedSet<OrderbookOrder> offers) {
		// if there is market order give her some real value.
		try {
			OrderbookOrder first = offers.first();
			double newMarketPrice = first.getLimitPrice() - 1;
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
			
			//color:
			seriesColors.put(color);
			
			return offset;

		} catch (JSONException e) {
			log.info("Building JSON Array failed.");
		}
		return 0;
	}

}
