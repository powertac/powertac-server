package org.powertac.visualizer.domain.wholesale;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.joda.time.Instant;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;
import org.powertac.common.Timeslot;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This is a convenient class for wholesale visualization. It is a copy of
 * Orderbook class. Purpose of this class is to avoid unnecessary entries
 * created by original Orderbook for Power TAC server STATE log.
 *
 * @author Jurica Babic
 */
public class VisualizerOrderbook {
  private Logger log = LogManager.getLogger(VisualizerOrderbook.class);
	
	private Instant dateExecuted;

	/** the timeslot this orderbook is generated for */
	private Timeslot timeslot;

	/**
	 * last clearing price, expressed as a positive number - the price/mwh paid
	 * to sellers. Null if the market did not clear.
	 */
	private Double clearingPrice;

	/**
	 * sorted set of OrderbookOrder instances representing bids. Since bid
	 * prices are negative, this is an ascending sort.
	 */
	private SortedSet<VisualizerOrderbookOrder> bids = new TreeSet<VisualizerOrderbookOrder>();

	/** sorted set of OrderbookOrders represenging asks ascending */
	private SortedSet<VisualizerOrderbookOrder> asks = new TreeSet<VisualizerOrderbookOrder>();

	/**
	 * Constructor with default product type.
	 */
	public VisualizerOrderbook(Timeslot timeslot, Double clearingPrice,
			Instant dateExecuted) {
		super();
		this.timeslot = timeslot;
		this.clearingPrice = clearingPrice;
		this.dateExecuted = dateExecuted;
	}
	
	/**
	 * Constructor for creating an object from the original Orderbook.
	 */
	public VisualizerOrderbook(Orderbook orderbook) {
		super();
		this.timeslot = orderbook.getTimeslot();
		this.clearingPrice = orderbook.getClearingPrice();
		this.dateExecuted = orderbook.getDateExecuted();

    SortedSet<OrderbookOrder> asks = orderbook.getAsks();
    if (asks != null) {
      for (OrderbookOrder orderbookOrder: asks) {
        this.addAsk(
            new VisualizerOrderbookOrder(orderbookOrder.getMWh(),
                                         orderbookOrder.getLimitPrice()));
      }
    }
    else {
      log.warn("No asks for VisualizerOrderbook " + orderbook);
    }

    SortedSet<OrderbookOrder> bids = orderbook.getBids();
    if (bids != null) {
      for (OrderbookOrder orderbookOrder: bids) {
        this.addBid(
            new VisualizerOrderbookOrder(orderbookOrder.getMWh(),
                                         orderbookOrder.getLimitPrice()));
      }
    }
    else {
      log.warn("No bids for VisualizerOrderbook " + orderbook);
    }
	}

	/**
	 * Returns the positive price at which the market cleared. This is the price
	 * paid to sellers, the negative of the price paid by buyers. Null if no
	 * trades were cleared.
	 */
	public Double getClearingPrice() {
		return clearingPrice;
	}

	/**
	 * The date when the market cleared.
	 */
	public Instant getDateExecuted() {
		return dateExecuted;
	}

	/**
	 * The timeslot in which energy commitments represented by cleared trades
	 * are due.
	 */
	public Timeslot getTimeslot() {
		return timeslot;
	}

	/**
	 * The set of bids (positive energy quantities) that were submitted and did
	 * not clear, ascending sort. Because bid prices are normally negative, the
	 * first element is the highest (most negative) price that did not clear.
	 * Null prices (market orders) sort ahead of non-null prices (limit orders).
	 */
	public SortedSet<VisualizerOrderbookOrder> getBids() {
		return bids;
	}


	public VisualizerOrderbook addBid(VisualizerOrderbookOrder bid) {
		bids.add(bid);
		return this;
	}

	/**
	 * The set of asks (negative energy quantities) that were submitted and did
	 * not clear, ascending sort. Null prices (market orders) sort ahead of
	 * non-null prices (limit orders).
	 */
	public SortedSet<VisualizerOrderbookOrder> getAsks() {
		return asks;
	}

	public VisualizerOrderbook addAsk(VisualizerOrderbookOrder ask) {
		asks.add(ask);
		return this;
	}
}
