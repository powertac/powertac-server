package org.powertac.visualizer.statistical;

import java.util.ArrayList;
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
	private double cashPositive;
	private double cashNegative;
	private double energyPositive;
	private double energyNegative;

	private ConcurrentHashMap<Long, Order> orders = new ConcurrentHashMap<Long, Order>(
			24, 0.75f, 1);
	private ConcurrentHashMap<Long, MarketTransaction> marketTransactions = new ConcurrentHashMap<Long, MarketTransaction>(
			24, 0.75f, 1);

	public SingleTimeslotWholesaleData(BrokerModel model, long millis) {
		broker = model;
		timeslotMillis = millis;
	}

	public ConcurrentHashMap<Long, Order> getOrders() {
		return orders;
	}

	public void processOrder(Order order, long millisFrom) {
		orders.put(millisFrom, order);

	}

	public void processMarketTransaction(MarketTransaction tx, long millisFrom) {

		double energy = tx.getMWh();
		double cash = tx.getPrice();

		if (cash < 0) {
			cashNegative += cash;
		} else {
			cashPositive += cash;
		}

		if (energy < 0) {
			energyNegative += energy;
		} else {
			energyPositive += energy;
		}

		marketTransactions.put(millisFrom, tx);

	}

	public BrokerModel getBroker() {
		return broker;
	}

	public long getTimeslotMillis() {
		return timeslotMillis;
	}

	public double getCashPositive() {
		return cashPositive;
	}

	public double getCashNegative() {
		return cashNegative;
	}

	public double getEnergyPositive() {
		return energyPositive;
	}

	public double getEnergyNegative() {
		return energyNegative;
	}

}
