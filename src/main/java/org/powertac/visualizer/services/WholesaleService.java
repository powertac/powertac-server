package org.powertac.visualizer.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.Timeslot;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.json.WholesaleServiceJSON;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.stereotype.Service;

@Service
public class WholesaleService implements Serializable, Recyclable {

	private static final long serialVersionUID = 1L;

	private Map<Integer, WholesaleMarket> wholesaleMarkets;

	private double totalTradedQuantityMWh;
	
	private WholesaleServiceJSON json;

	public WholesaleService() {
		recycle();

	}

	public WholesaleMarket findWholesaleMarket(Integer timeslotSerialNumber) {
		return wholesaleMarkets.get(timeslotSerialNumber);
	}

	public Map<Integer, WholesaleMarket> getWholesaleMarkets() {
		return wholesaleMarkets;
	}

	public double getTotalTradedQuantityMWh() {
		return totalTradedQuantityMWh;
	}

	public void addTradedQuantityMWh(double quantity) {
		totalTradedQuantityMWh += quantity;
		totalTradedQuantityMWh = Helper.roundNumberTwoDecimal(totalTradedQuantityMWh);
	}

	public String getName() {
		return "Root";
	}

	public String getType() {
		return "wholesale model";
	}

	public String getTotalTradedQuantity() {
		return "" + totalTradedQuantityMWh;
	}

	public void recycle() {
		wholesaleMarkets = new ConcurrentSkipListMap<Integer, WholesaleMarket>();
		totalTradedQuantityMWh = 0;
		json = new WholesaleServiceJSON();

	}
	
	public WholesaleServiceJSON getJson() {
		return json;
	}

}
