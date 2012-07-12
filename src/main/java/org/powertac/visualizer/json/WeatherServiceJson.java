package org.powertac.visualizer.json;

import org.primefaces.json.JSONArray;

public class WeatherServiceJson {
	
	JSONArray windSpeedData = new JSONArray();
	JSONArray cloudCoverData= new JSONArray();
	JSONArray temperatureData= new JSONArray();
	
	public JSONArray getCloudCoverData() {
		return cloudCoverData;
	}
	
	public JSONArray getTemperatureData() {
		return temperatureData;
	}
	public JSONArray getWindSpeedData() {
		return windSpeedData;
	}

}
