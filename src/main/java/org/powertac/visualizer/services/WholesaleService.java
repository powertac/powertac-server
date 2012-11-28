package org.powertac.visualizer.services;

import org.powertac.visualizer.Helper;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.json.WholesaleServiceJSON;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

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
