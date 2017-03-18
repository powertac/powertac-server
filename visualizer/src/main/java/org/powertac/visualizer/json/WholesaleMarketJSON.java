package org.powertac.visualizer.json;

import org.primefaces.json.JSONArray;

public class WholesaleMarketJSON {
	private JSONArray clearingPrices = new JSONArray();
	private JSONArray clearingVolumes = new JSONArray();
	
	public JSONArray getClearingPrices() {
		return clearingPrices;
	}
	
	public JSONArray getClearingVolumes() {
		return clearingVolumes;
	}
	
	public void setClearingPrices(JSONArray clearingPrices) {
		this.clearingPrices = clearingPrices;
	}
	public void setClearingVolumes(JSONArray clearingVolumes) {
		this.clearingVolumes = clearingVolumes;
	}

}
