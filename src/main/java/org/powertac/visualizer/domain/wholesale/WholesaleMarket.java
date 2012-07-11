package org.powertac.visualizer.domain.wholesale;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.log4j.Logger;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.Timeslot;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.json.WholesaleMarketJSON;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.SelectableDataModel;
import org.primefaces.model.TreeNode;

/**
 * 
 * WholesaleMarket contains information about the wholesale market for one
 * timeslot.
 * 
 * @author Jurica Babic
 * 
 */
public class WholesaleMarket  {
	
	private static Logger log = Logger.getLogger(WholesaleMarket.class);
	private Timeslot timeslot;
	private int timeslotSerialNumber;
	private Map<Integer, WholesaleSnapshot> snapshotsMap = new ConcurrentSkipListMap<Integer, WholesaleSnapshot>();
	private WholesaleSnapshot lastWholesaleSnapshotWithClearing;
	private double totalTradedQuantityMWh;
	private double weightSumTradedQuantityMWh;
	private double avgWeightPrice;
	private boolean closed;
	private WholesaleMarketJSON json = new WholesaleMarketJSON();

	public WholesaleMarket(Timeslot timeslot, Integer timeslotSerialNumber) {
		this.timeslotSerialNumber = timeslotSerialNumber;
		this.timeslot = timeslot;
	}

	public int getTimeslotSerialNumber() {
		return timeslotSerialNumber;
	}

	/**
	 * Returns WholesaleSnapshot by given timeslot serial number. If there is no
	 * snapshot available, the new WholsaleSnapshot object with specified serial
	 * number index is created.
	 * 
	 * @param timeslotSerialNumber
	 * @return
	 */
	public WholesaleSnapshot findSnapshot(int timeslotSerialNumber) {

		return snapshotsMap.get(timeslotSerialNumber);
	}

	public Map<Integer, WholesaleSnapshot> getSnapshotsMap() {
		return snapshotsMap;
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
		log.debug("Market: TS serial num:"+this.getTimeslotSerialNumber()+"Total traded quantity:"+this.getTotalTradedQuantityMWh()+" is closed");
		
	}

	private void finish() {
		Collection<WholesaleSnapshot> wholesaleSnapshots = snapshotsMap.values();
		JSONArray clearingPrices = new JSONArray();
		JSONArray clearingVolumes = new JSONArray();
		for (Iterator iterator = wholesaleSnapshots.iterator(); iterator.hasNext();) {
			WholesaleSnapshot wholesaleSnapshot = (WholesaleSnapshot) iterator.next();

			// build statistics:
			if (wholesaleSnapshot.getClearedTrade() != null) {
				double quantity = wholesaleSnapshot.getClearedTrade().getExecutionMWh();
				double price = wholesaleSnapshot.getClearedTrade().getExecutionPrice();
				totalTradedQuantityMWh += quantity;
				weightSumTradedQuantityMWh += quantity * price;
			}
			if(wholesaleSnapshot.isCleared()){
				
					
				try {
					clearingPrices.put(new JSONArray().put(wholesaleSnapshot.getMillisCreated()).put(wholesaleSnapshot.getClearedTrade().getExecutionPrice()));
					clearingVolumes.put(new JSONArray().put(wholesaleSnapshot.getMillisCreated()).put(wholesaleSnapshot.getClearedTrade().getExecutionMWh()));
				}
				catch (JSONException e) {
					e.printStackTrace();
				}
				}
		}
		
		
		
		
		json.setClearingPrices(clearingPrices);
		json.setClearingVolumes(clearingVolumes);

		if (totalTradedQuantityMWh != 0) {
			avgWeightPrice = weightSumTradedQuantityMWh / totalTradedQuantityMWh;
		} else {
			avgWeightPrice = 0;
		}
		truncateNumbers();
	}

	private void truncateNumbers() {
		avgWeightPrice = Helper.roundNumberTwoDecimal(avgWeightPrice);
		totalTradedQuantityMWh = Helper.roundNumberTwoDecimal(totalTradedQuantityMWh);
		weightSumTradedQuantityMWh = Helper.roundNumberTwoDecimal(weightSumTradedQuantityMWh);		
	}

	@Override
	public String toString() {
		return "TS Number: " + timeslotSerialNumber + " AVG WEIGHT PRICE: " + avgWeightPrice + " totalTradedQuantity: "
				+ totalTradedQuantityMWh;
	}

	public String getName() {
		return "Market" + timeslotSerialNumber;
	}

	public String getType() {
		return "Wholesale Market";
	}

	public String getTotalTradedQuantity() {
		return "" + totalTradedQuantityMWh;
	}
	
	public ArrayList<WholesaleSnapshot> getSnapshots(){
		
		return new ArrayList<WholesaleSnapshot>(snapshotsMap.values());		
	}
	
	public Timeslot getTimeslot() {
		return timeslot;
	}
	
	public WholesaleSnapshot getLastWholesaleSnapshotWithClearing() {
		return lastWholesaleSnapshotWithClearing;
	}
	
	public void setLastWholesaleSnapshotWithClearing(
			WholesaleSnapshot lastWholesaleSnapshotWithClearing) {
		this.lastWholesaleSnapshotWithClearing = lastWholesaleSnapshotWithClearing;
	}
	
	public WholesaleMarketJSON getJson() {
		return json;
	}
}
