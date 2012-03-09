package org.powertac.visualizer.wholesale;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.Timeslot;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

public class WholesaleModel implements Serializable, WholesaleTreeView {
	
	private static final long serialVersionUID = 1L;

	private Map<Integer, WholesaleMarket> wholesaleMarkets;
	
	private TreeNode root;
	
	
			
	private double totalTradedQuantityMWh;

		
	public WholesaleModel() {
		wholesaleMarkets = new TreeMap<Integer, WholesaleMarket>();
		root = new DefaultTreeNode(this, null); 
		
	}
	
	public WholesaleMarket findWholesaleMarket(Integer timeslotSerialNumber){
		return wholesaleMarkets.get(timeslotSerialNumber);
	}
	
	public Map<Integer, WholesaleMarket> getWholesaleMarkets() {
		return wholesaleMarkets;
	}
	
	public double getTotalTradedQuantityMWh() {
		return totalTradedQuantityMWh;
	}
	
	public void addTradedQuantityMWh(double quantity){
		totalTradedQuantityMWh+=quantity;
	}
	
	public TreeNode getRoot() {
		return root;
	}

	public String getName() {
		return "Root";
	}

	public String getType() {
		return "wholesale model";
	}

	public String getTotalTradedQuantity() {
		return ""+totalTradedQuantityMWh;
	}	
	
}
