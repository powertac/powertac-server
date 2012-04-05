package org.powertac.visualizer.json;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

public class BrokerJSON {

	private JSONArray customersBubbleJson;
	private JSONArray cashBalanceJson;
	private JSONArray energyBalanceJson;
	// daily values (end of day values):
	private JSONArray cashBalanceDailyJson;
	// daily average values
	private JSONArray energyBalanceDailyAVGJson;
	
	private JSONArray customerTicks;

	private JSONObject seriesOptions;

	public BrokerJSON(JSONObject seriesOptions) {
		this.seriesOptions = seriesOptions;
		cashBalanceJson = new JSONArray();
		energyBalanceJson = new JSONArray();
		// daily values (end of day values):
		cashBalanceDailyJson = new JSONArray();
		// Helper.updateJSON(cashBalanceDailyAVGJson, 0, 0);
		energyBalanceDailyAVGJson = new JSONArray();
		// Helper.updateJSON(energyBalanceDailyAVGJson, 0, 0);

		customerTicks = new JSONArray();
	}

	public JSONArray getCustomersBubbleJson() {
		return customersBubbleJson;
	}

	public void setCustomersBubbleJson(JSONArray customersBubbleJson) {
		this.customersBubbleJson = customersBubbleJson;
	}

	public JSONArray getCashBalanceJson() {
		return cashBalanceJson;
	}

	public void setCashBalanceJson(JSONArray cashBalanceJson) {
		this.cashBalanceJson = cashBalanceJson;
	}

	public JSONArray getEnergyBalanceJson() {
		return energyBalanceJson;
	}

	public void setEnergyBalanceJson(JSONArray energyBalanceJson) {
		this.energyBalanceJson = energyBalanceJson;
	}

	public JSONArray getCashBalanceDailyJson() {
		return cashBalanceDailyJson;
	}

	public void setCashBalanceDailyJson(JSONArray cashBalanceDailyJson) {
		this.cashBalanceDailyJson = cashBalanceDailyJson;
	}

	public JSONArray getEnergyBalanceDailyAVGJson() {
		return energyBalanceDailyAVGJson;
	}

	public void setEnergyBalanceDailyAVGJson(JSONArray energyBalanceDailyAVGJson) {
		this.energyBalanceDailyAVGJson = energyBalanceDailyAVGJson;
	}

	public JSONArray getCustomerTicks() {
		return customerTicks;
	}

	public void setCustomerTicks(JSONArray customerTicks) {
		this.customerTicks = customerTicks;
	}

	public JSONObject getSeriesOptions() {
		return seriesOptions;
	}
	public void setSeriesOptions(JSONObject seriesOptions) {
		this.seriesOptions = seriesOptions;
	}
	
	

}
