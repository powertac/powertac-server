package org.powertac.visualizer.wholesale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.Timeslot;

/**
 * 
 * WholesaleMarket contains information about the wholesale for one timeslot.
 * 
 * @author Jurica Babic
 * 
 */
public class WholesaleMarket {

	private int timeslotSerialNumber;
	private int relativeTimeslotIndex;
	private Map<Integer,WholesaleSnapshot> snapshots = new TreeMap<Integer, WholesaleSnapshot>(); 
	private ClearedTrade lastClearedTrade;
	private double totalQuantityTradedMWh;

		
	public WholesaleMarket(Integer timeslotSerialNumber, int relativeTimeslotIndex) {
		this.timeslotSerialNumber = timeslotSerialNumber;
		this.relativeTimeslotIndex = relativeTimeslotIndex;
	}
	
	public int getTimeslotSerialNumber() {
		return timeslotSerialNumber;
	}
	
	/**
	 * Returns WholesaleSnapshot by given relative timeslot index. If there is no snapshot available, the new WholsaleSnapshot object with specified relative timeslot index is created.
	 * @param relativeTimeslotIndex
	 * @return
	 */
	public WholesaleSnapshot findSnapshot(int relativeTimeslotIndex){
		
		return snapshots.get(relativeTimeslotIndex);
	}
	
	public Map<Integer, WholesaleSnapshot> getSnapshots() {
		return snapshots;
	}

	public void addOrder(Order order, int relativeTimeslotIndex) {
		if(!snapshots.containsKey(relativeTimeslotIndex)){
			snapshots.put(relativeTimeslotIndex, new WholesaleSnapshot());
		}
		findSnapshot(relativeTimeslotIndex).addOrder(order);
	}

	public void setClearedTrade(ClearedTrade clearedTrade, int relativeTimeslotIndex) {
		findSnapshot(relativeTimeslotIndex).setClearedTrade(clearedTrade);
		lastClearedTrade=clearedTrade;
		totalQuantityTradedMWh+=clearedTrade.getExecutionMWh();
	}

	public void setOrderbook(Orderbook orderbook, int relativeTimeslotIndex) {
		findSnapshot(relativeTimeslotIndex).setOrderbook(orderbook);
	}
	
	public ClearedTrade getLastClearedTrade() {
		return lastClearedTrade;
	}
	
	public double getTotalQuantityTradedMWh() {
		return totalQuantityTradedMWh;
	}
	
	public int getRelativeTimeslotIndex() {
		return relativeTimeslotIndex;
	}
	
//	public WholesaleSnapshot getLastClosedWholesaleSnapshot(){
//		TreeMap<Integer, WholesaleSnapshot> fake = new TreeMap<Integer, WholesaleSnapshot>();
//		
//		
//	}

}
