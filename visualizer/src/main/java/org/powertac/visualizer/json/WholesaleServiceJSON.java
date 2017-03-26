package org.powertac.visualizer.json;

import org.primefaces.json.JSONArray;

public class WholesaleServiceJSON {
	
	private JSONArray globalLastClearingPrices = new JSONArray();
	private JSONArray globalLastClearingVolumes = new JSONArray();
	
	private JSONArray globalTotalClearingVolumes = new JSONArray();

	public JSONArray getGlobalLastClearingPrices() {
		return globalLastClearingPrices;
	}


	public JSONArray getGlobalLastClearingVolumes() {
		return globalLastClearingVolumes;
	}

	public JSONArray getGlobalTotalClearingVolumes() {
		return globalTotalClearingVolumes;
	}

	
	
}
