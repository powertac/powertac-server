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
public class SingleTimeslotWholesaleData extends DynamicData {

	private boolean closed;
	private HashMap<Integer, MarketTransaction> marketTransactions = new HashMap<Integer, MarketTransaction>(
			24);	

	public SingleTimeslotWholesaleData(int tsIndex, double energy, double profit) {
		super(tsIndex, energy, profit);	
	}
	
	public HashMap<Integer, MarketTransaction> getMarketTransactions() {
		return marketTransactions;
	}
	public void setClosed(boolean closed) {
		this.closed = closed;
	}
	
	public void addMarketTransaction(MarketTransaction mtx){
		marketTransactions.put(mtx.getPostedTimeslotIndex(),mtx);
		update(mtx.getMWh(), mtx.getPrice());
	}
		
	public boolean isClosed() {
		return closed;
	}
	

	
}
