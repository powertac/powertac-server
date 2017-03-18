package org.powertac.visualizer.json;

import org.primefaces.json.JSONArray;

public class BrokersJSON {
	
	private JSONArray brokerSeriesColors = new JSONArray();
	private JSONArray seriesOptions = new JSONArray();
	private JSONArray cashChartData = new JSONArray();
	private JSONArray customerCountData = new JSONArray();
	
	
	public JSONArray getBrokerSeriesColors() {
		return brokerSeriesColors;
	}
	public void setBrokerSeriesColors(JSONArray brokerSeriesColors) {
		this.brokerSeriesColors = brokerSeriesColors;
	}
	public JSONArray getSeriesOptions() {
		return seriesOptions;
	}
	public void setSeriesOptions(JSONArray seriesOptions) {
		this.seriesOptions = seriesOptions;
	}
	public JSONArray getCashChartData() {
		return cashChartData;
	}
	public void setCashChartData(JSONArray cashChartData) {
		this.cashChartData = cashChartData;
	}
	public JSONArray getCustomerCountData() {
		return customerCountData;
	}
	public void setCustomerCountData(JSONArray customerCountData) {
		this.customerCountData = customerCountData;
	}
	
	
	
	

}
