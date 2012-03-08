package org.powertac.visualizer.wholesale;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.Timeslot;

public class WholesaleModel {
	

	private Map<Integer, WholesaleMarket> wholesaleMarkets;
	
	private double totalTradedQuantity;

	public WholesaleModel() {
		wholesaleMarkets = new TreeMap<Integer, WholesaleMarket>();
	}
	
	public WholesaleMarket findWholesaleMarket(Integer timeslotSerialNumber){
		return wholesaleMarkets.get(timeslotSerialNumber);
	}
	
	public void addOrder(Order order, int relativeTimeslotIndex){
		
		int timeslotSerialNumber = order.getTimeslot().getSerialNumber();
		
		if(!wholesaleMarkets.containsKey(timeslotSerialNumber)){
			//create new WholesaleMarket
			WholesaleMarket market = new WholesaleMarket(timeslotSerialNumber,relativeTimeslotIndex);
			wholesaleMarkets.put(timeslotSerialNumber, market);
		}
		
		findWholesaleMarket(timeslotSerialNumber).addOrder(order,relativeTimeslotIndex); 
	}
	
	public void setClearedTrade(ClearedTrade clearedTrade, int relativeTimeslotIndex){
		findWholesaleMarket(clearedTrade.getTimeslot().getSerialNumber()).setClearedTrade(clearedTrade,relativeTimeslotIndex);
		totalTradedQuantity+=clearedTrade.getExecutionMWh();
	}
	
	public void setOrderbook(Orderbook orderbook, int relativeTimeslotIndex){
		findWholesaleMarket(orderbook.getTimeslot().getSerialNumber()).setOrderbook(orderbook, relativeTimeslotIndex);
	}
	
	public double getTotalTradedQuantity() {
		return totalTradedQuantity;
	}
}
