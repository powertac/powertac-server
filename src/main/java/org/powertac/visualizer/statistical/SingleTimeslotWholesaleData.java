package org.powertac.visualizer.statistical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.MarketTransaction;
import org.powertac.common.Order;
import org.powertac.visualizer.domain.broker.BrokerModel;

/**
 * Aggregate wholesale data (all the clearings) for one timeslot from broker's
 * perspective.
 * 
 * @author Jurica Babic
 * 
 */
public class SingleTimeslotWholesaleData {

	private BrokerModel broker;
	private long timeslotMillis;
	private double profit;
	private double netMWh;

	private boolean closed;

	private HashMap<Long, Order> orders = new HashMap<Long, Order>(24);
	private HashMap<Long, MarketTransaction> marketTransactions = new HashMap<Long, MarketTransaction>(
			24);

	public SingleTimeslotWholesaleData(BrokerModel model, long millis) {
		broker = model;
		timeslotMillis = millis;
	}

	/**
	 * @return Returns the map of orders if the SingleTimeslotWholesaleData
	 *         object is closed for modification.
	 */
	public HashMap<Long, Order> getOrders() {
		if (closed) {
			return orders;
		} else {
			return null;
		}
	}

	public void processOrder(Order order, long millisFrom) {
		if (!closed) {
			orders.put(millisFrom, order);
		}
	}

	public synchronized void processMarketTransaction(MarketTransaction tx, long millisFrom) {
		if (!closed) {
			double energy = tx.getMWh();
			double cash = tx.getPrice();

			profit+=cash;
			netMWh+=energy;

			marketTransactions.put(millisFrom, tx);
		}
	}

	public BrokerModel getBroker() {
		return broker;
	}

	public long getTimeslotMillis() {
		return timeslotMillis;
	}
	
	public HashMap<Long, MarketTransaction> getMarketTransactions() {
		return marketTransactions;
	}
	
	public double getNetMWh() {
		return netMWh;
	}
	
	public double getProfit() {
		return profit;
	}
	
	public boolean isClosed() {
		return closed;
	}
	public void setClosed(boolean closed) {
		this.closed = closed;
	}
	
}
