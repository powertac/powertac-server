package org.powertac.visualizer.json;

import java.awt.Color;

import org.primefaces.json.JSONArray;

public class WholesaleSnapshotJSON {
	private JSONArray graphDataBeforeClearing;
	private JSONArray seriesColorsBeforeClearing;
	private JSONArray graphDataAfterClearing;
	private JSONArray seriesColorsAfterClearing;
	// black
	private static final String MARKET_ASK_ORDER_COLOR = "#000000";
	// green
	private static final String LIMIT_ASK_ORDER_COLOR = "#00CC33";
	// black
	private static final String MARKET_BID_ORDER_COLOR = "#000000";
	// red
	private static final String LIMIT_BID_ORDER_COLOR = "#FF3333";
	// blue
	private static final String CLEARED_TRADE_COLOR = "#3333FF";

	private static final double MARKET_ORDER_OFFSET = 15;

	public WholesaleSnapshotJSON(JSONArray graphDataBeforeClearing, JSONArray seriesColorsBeforeClearing,
			JSONArray graphDataAfterClearing, JSONArray seriesColorsAfterClearing) {
		this.graphDataBeforeClearing = graphDataBeforeClearing;
		this.seriesColorsBeforeClearing = seriesColorsBeforeClearing;
		this.graphDataAfterClearing = graphDataAfterClearing;
		this.seriesColorsAfterClearing = seriesColorsAfterClearing;
	}

	public JSONArray getGraphDataBeforeClearing() {
		return graphDataBeforeClearing;
	}

	public JSONArray getSeriesColorsBeforeClearing() {
		return seriesColorsBeforeClearing;
	}

	public JSONArray getGraphDataAfterClearing() {
		return graphDataAfterClearing;
	}

	public JSONArray getSeriesColorsAfterClearing() {
		return seriesColorsAfterClearing;
	}

	public static String getMarketAskOrderColor() {
		return MARKET_ASK_ORDER_COLOR;
	}

	public static String getLimitAskOrderColor() {
		return LIMIT_ASK_ORDER_COLOR;
	}

	public static String getMarketBidOrderColor() {
		return MARKET_BID_ORDER_COLOR;
	}

	public static String getLimitBidOrderColor() {
		return LIMIT_BID_ORDER_COLOR;
	}

	public static String getClearedTradeColor() {
		return CLEARED_TRADE_COLOR;
	}

	public static double getMarketOrderOffset() {
		return MARKET_ORDER_OFFSET;
	}

}
