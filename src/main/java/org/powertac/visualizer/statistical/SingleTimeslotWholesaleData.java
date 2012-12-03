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
	
	private ArrayList<Order> orders = new ArrayList<Order>(24);
	private ArrayList<MarketTransaction> marketTransactions = new ArrayList<MarketTransaction>(24);
	private boolean immutable = false;
	
	public SingleTimeslotWholesaleData(BrokerModel model, long millis) {
		broker = model;
		timeslotMillis = millis;
	}
	
	/**
	 * @return A list of Order objects if there are will not be any updates to a list. Otherwise, returns null;
	 */
	public ArrayList<Order> getOrders() {
		if(immutable){
		return orders;}
		else{
			return null;
		}
	}
	
	public void processOrder(Order order){
		if(!immutable){
		orders.add(order);
		}
	}
	
	public void processMarketTransaction(MarketTransaction tx){
		if(!immutable){
		double energy = tx.getMWh();
		double cash = tx.getPrice();
		
		if (cash<0) {
			cashNegative+=cash;
		} else {
			cashPositive+=cash;
		}
		
		if (energy<0) {
			energyNegative+=energy;
		} else {
			energyPositive+=energy;
		}
		
		marketTransactions.add(tx);
		}
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
	
	public void setImmutable(boolean immutable) {
		this.immutable = immutable;
	}
	
	public boolean isImmutable() {
		return immutable;
	}
	
	

}
