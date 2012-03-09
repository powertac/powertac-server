package org.powertac.visualizer.wholesale;

import org.primefaces.json.JSONArray;

public class WholesaleMarketJSON {
	private JSONArray graphDataBeforeClearing;
	private JSONArray seriesColorsBeforeClearing;

	public WholesaleMarketJSON(JSONArray graphDataBeforeClearing, JSONArray seriesColorsBeforeClearing) {
		this.graphDataBeforeClearing = graphDataBeforeClearing;
		this.seriesColorsBeforeClearing = seriesColorsBeforeClearing;
	}
	
	public JSONArray getGraphDataBeforeClearing() {
		return graphDataBeforeClearing;
	}
	public JSONArray getSeriesColorsBeforeClearing() {
		return seriesColorsBeforeClearing;
	}
}
