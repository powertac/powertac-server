package org.powertac.visualizer.json;

import org.primefaces.json.JSONArray;

public class CustomerJSON {
	private JSONArray bootstrapLineChartData = new JSONArray();

	private JSONArray totalChargeLineChartData = new JSONArray();
	private JSONArray inflowChargeLineChartData = new JSONArray();
	private JSONArray outflowChargeLineChartData = new JSONArray();

	private JSONArray totalkWhLineChartData = new JSONArray();
	private JSONArray productionKWhLineChartData = new JSONArray();
	private JSONArray consumptionKWhLineChartData = new JSONArray();
	
	public void setBootstrapLineChartData(JSONArray bootstrapLineChartData) {
		this.bootstrapLineChartData = bootstrapLineChartData;
	}

	public String getBootstrapLineChartDataText() {
		return bootstrapLineChartData.toString();
	}

	public String getConsumptionKWhLineChartDataText() {
		return consumptionKWhLineChartData.toString();
	}

	public String getInflowChargeLineChartDataText() {
		return inflowChargeLineChartData.toString();
	}

	public String getOutflowChargeLineChartDataText() {
		return outflowChargeLineChartData.toString();
	}

	public String getProductionKWhLineChartDataText() {
		return productionKWhLineChartData.toString();
	}

	public String getTotalChargeLineChartDataText() {
		return totalChargeLineChartData.toString();
	}

	public String getTotalkWhLineChartDataText() {
		return totalkWhLineChartData.toString();
	}
	
	public JSONArray getBootstrapLineChartData() {
		return bootstrapLineChartData;
	}

	public JSONArray getConsumptionKWhLineChartData() {
		return consumptionKWhLineChartData;
	}

	public JSONArray getInflowChargeLineChartData() {
		return inflowChargeLineChartData;
	}

	public JSONArray getOutflowChargeLineChartData() {
		return outflowChargeLineChartData;
	}

	public JSONArray getProductionKWhLineChartData() {
		return productionKWhLineChartData;
	}

	public JSONArray getTotalChargeLineChartData() {
		return totalChargeLineChartData;
	}

	public JSONArray getTotalkWhLineChartData() {
		return totalkWhLineChartData;
	}
	


}
