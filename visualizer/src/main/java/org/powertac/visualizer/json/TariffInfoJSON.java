package org.powertac.visualizer.json;

import org.primefaces.json.JSONArray;

public class TariffInfoJSON {

	private JSONArray ratesLineChartMinValue = new JSONArray();
	private JSONArray ratesLineChartMaxValue = new JSONArray();

	private JSONArray totalRevenueLineChart = new JSONArray();
	//private JSONArray totalRevenuePerUnitLineChart = new JSONArray();
	private JSONArray hourlyRevenueLineChart = new JSONArray();
	//private JSONArray hourlyRevenuePerUnitLineChart = new JSONArray();

	private JSONArray totalKWhLineChart = new JSONArray();
//	private JSONArray totalKWhPerUnitLineChart = new JSONArray();
	private JSONArray hourlyKWhLineChart = new JSONArray();
//	private JSONArray hourlyKWhPerUnitLineChart = new JSONArray();

	private JSONArray subscribedPopulationLineChart = new JSONArray();

	public JSONArray getRatesLineChartMaxValue() {
		return ratesLineChartMaxValue;
	}

	public JSONArray getRatesLineChartMinValue() {
		return ratesLineChartMinValue;
	}

	public JSONArray getTotalRevenueLineChart() {
		return totalRevenueLineChart;
	}

//	public JSONArray getTotalRevenuePerUnitLineChart() {
//		return totalRevenuePerUnitLineChart;
//	}

	public JSONArray getHourlyRevenueLineChart() {
		return hourlyRevenueLineChart;
	}

//	public JSONArray getHourlyRevenuePerUnitLineChart() {
//		return hourlyRevenuePerUnitLineChart;
//	}

	public JSONArray getTotalKWhLineChart() {
		return totalKWhLineChart;
	}

//	public JSONArray getTotalKWhPerUnitLineChart() {
//		return totalKWhPerUnitLineChart;
//	}

	public JSONArray getHourlyKWhLineChart() {
		return hourlyKWhLineChart;
	}

//	public JSONArray getHourlyKWhPerUnitLineChart() {
//		return hourlyKWhPerUnitLineChart;
//	}

	public JSONArray getSubscribedPopulationLineChart() {
		return subscribedPopulationLineChart;
	}
	
	public void setRatesLineChartMaxValue(JSONArray ratesLineChartMaxValue) {
		this.ratesLineChartMaxValue = ratesLineChartMaxValue;
	}
	public void setRatesLineChartMinValue(JSONArray ratesLineChartMinValue) {
		this.ratesLineChartMinValue = ratesLineChartMinValue;
	}

	@Override
	public String toString() {
		return "\n ratesLineChartMinValue" + ratesLineChartMinValue + "\n ratesLineChartMaxValue"
				+ ratesLineChartMinValue + "\n totalRevenueLineChart" + totalRevenueLineChart
				/*+ "\n totalRevenuePerUnitLineChart" + totalRevenuePerUnitLineChart*/ + "\n hourlyRevenueLineChart"
				+ hourlyRevenueLineChart /*+ "\n hourlyRevenuePerUnitLineChart" + hourlyRevenuePerUnitLineChart*/
				+ "\n totalKWhLineChart" + totalKWhLineChart /*+ "\n totalKWhPerUnitLineChart" + totalKWhPerUnitLineChart*/
				+ "\n hourlyKWhLineChart" + hourlyKWhLineChart /*+ "\n hourlyKWhPerUnitLineChart"
				+ hourlyKWhPerUnitLineChart*/ + "\n subscribedPopulationLineChart" + subscribedPopulationLineChart;
	}
}
