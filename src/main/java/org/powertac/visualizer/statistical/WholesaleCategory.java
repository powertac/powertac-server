package org.powertac.visualizer.statistical;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.MarketTransaction;
import org.powertac.common.Order;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.interfaces.ClosedWholesaleWrapper;

public class WholesaleCategory extends AbstractPerformanceCategory implements ClosedWholesaleWrapper {

	/**
	 * "For date" is the key for this map.
	 */

	private int noOrders;
	private int noMarketTransactions;

	private ConcurrentHashMap<Long, SingleTimeslotWholesaleData> singleTimeslotWholesaleMap = new ConcurrentHashMap<Long, SingleTimeslotWholesaleData>(
			2000, 0.75f, 1);

	public WholesaleCategory(BrokerModel broker) {
		super(broker);
	}

	public ConcurrentHashMap<Long, SingleTimeslotWholesaleData> getSingleTimeslotWholesaleMap() {
		return singleTimeslotWholesaleMap;
	}
	
	public HashMap<Long, SingleTimeslotWholesaleData> getClosedSingleTimeslotWholesaleMap(){
		
		HashMap<Long, SingleTimeslotWholesaleData> resultMap = new HashMap<Long, SingleTimeslotWholesaleData>();
		
		Set<Entry<Long,SingleTimeslotWholesaleData>> set = singleTimeslotWholesaleMap.entrySet();
		for (Iterator<Entry<Long, SingleTimeslotWholesaleData>> iterator = set.iterator(); iterator.hasNext();) {
			Entry<Long, SingleTimeslotWholesaleData> entry = (Entry<Long, SingleTimeslotWholesaleData>) iterator
					.next();
			long millis = entry.getKey();
			SingleTimeslotWholesaleData data = entry.getValue();
			if(data.isClosed()){
				resultMap.put(millis, data);
			}			
		}
		return resultMap;
	}

	public void processOrder(Order order, long millisFrom) {
		noOrders++;
		singleTimeslotWholesaleMap.putIfAbsent(order.getTimeslot()
				.getStartInstant().getMillis(),
				new SingleTimeslotWholesaleData(broker, order.getTimeslot()
						.getStartInstant().getMillis()));
		SingleTimeslotWholesaleData data = singleTimeslotWholesaleMap.get(order.getTimeslot().getStartInstant().getMillis());
		data.processOrder(order, millisFrom);	
	}
		
	public void processMarketTransaction(MarketTransaction tx, long millisFrom){
		noMarketTransactions++;
		singleTimeslotWholesaleMap.get(tx.getTimeslot().getStartInstant().getMillis()).processMarketTransaction(tx, millisFrom);
	}

	public int getNoOrders() {
		return noOrders;
	}

	public int getNoMarketTransactions() {
		return noMarketTransactions;
	}
	
	

}
