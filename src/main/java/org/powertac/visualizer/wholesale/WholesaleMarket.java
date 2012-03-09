package org.powertac.visualizer.wholesale;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.Timeslot;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 * 
 * WholesaleMarket contains information about the wholesale market for one
 * timeslot.
 * 
 * @author Jurica Babic
 * 
 */
public class WholesaleMarket implements WholesaleTreeView {

	private int timeslotSerialNumber;
	private Map<Integer, WholesaleSnapshot> snapshots = new TreeMap<Integer, WholesaleSnapshot>();
	private ClearedTrade lastClearedTrade;
	private double totalTradedQuantityMWh;
	private double weightSumTradedQuantityMWh;
	private double avgWeightPrice;
	private boolean closed;
	
	private TreeNode marketNode;

	public WholesaleMarket(Integer timeslotSerialNumber) {
		this.timeslotSerialNumber = timeslotSerialNumber;
		marketNode = new DefaultTreeNode(this, null);

	}

	public int getTimeslotSerialNumber() {
		return timeslotSerialNumber;
	}

	/**
	 * Returns WholesaleSnapshot by given relative timeslot index. If there is
	 * no snapshot available, the new WholsaleSnapshot object with specified
	 * relative timeslot index is created.
	 * 
	 * @param relativeTimeslotIndex
	 * @return
	 */
	public WholesaleSnapshot findSnapshot(int relativeTimeslotIndex) {

		return snapshots.get(relativeTimeslotIndex);
	}

	public Map<Integer, WholesaleSnapshot> getSnapshots() {
		return snapshots;
	}

	public ClearedTrade getLastClearedTrade() {
		return lastClearedTrade;
	}

	public double getTotalTradedQuantityMWh() {
		return totalTradedQuantityMWh;
	}
	
	public double getAvgWeightPrice() {
		return avgWeightPrice;
	}
	
	public double getWeightSumTradedQuantityMWh() {
		return weightSumTradedQuantityMWh;
	}
	
	public TreeNode getMarketNode() {
		return marketNode;
	}

	public boolean isClosed() {
		return closed;
	}

	/**
	 * Wholesale Market should be closed when all of its snapshots have been
	 * closed and the current relative index is equal to market's relative
	 * index.
	 */
	public void close() {
		finish();
		closed = true;

	}

	private void finish() {
		Collection<WholesaleSnapshot> wholesaleSnapshots = snapshots.values();
		for (Iterator iterator = wholesaleSnapshots.iterator(); iterator.hasNext();) {
			WholesaleSnapshot wholesaleSnapshot = (WholesaleSnapshot) iterator.next();
			
			//build statistics:
			if (wholesaleSnapshot.getClearedTrade() != null) {
				double quantity = wholesaleSnapshot.getClearedTrade().getExecutionMWh();
				double price = wholesaleSnapshot.getClearedTrade().getExecutionPrice();
				totalTradedQuantityMWh += quantity;
				weightSumTradedQuantityMWh += quantity * price;
			}
			// tree hook-up:
			wholesaleSnapshot.getSnapshotNode().setParent(marketNode);
		}

		if (totalTradedQuantityMWh != 0) {
			avgWeightPrice = weightSumTradedQuantityMWh / totalTradedQuantityMWh;
		} else {
			avgWeightPrice = 0;
		}

	}
	
	@Override
	public String toString() {
		return "TS Number: "+timeslotSerialNumber+" AVG WEIGHT PRICE: "+avgWeightPrice+" totalTradedQuantity: "+totalTradedQuantityMWh;
	}

	public String getName() {
		return "Market"+timeslotSerialNumber;
	}

	public String getType() {
		return "Wholesale Market";
	}

	public String getTotalTradedQuantity() {
		return ""+totalTradedQuantityMWh;
	}

}
