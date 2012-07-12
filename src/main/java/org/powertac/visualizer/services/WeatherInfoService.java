package org.powertac.visualizer.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Broker;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherReport;
import org.powertac.common.repo.DomainRepo;
import org.powertac.visualizer.domain.genco.Genco;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.powertac.visualizer.json.WeatherServiceJson;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.springframework.stereotype.Service;

/**
 * Service for weather-related data
 * 
 * @author Jurica Babic
 * 
 */
@Service
public class WeatherInfoService implements TimeslotCompleteActivation, Recyclable {

	private static Logger log = Logger.getLogger(WeatherInfoService.class);

	private ArrayList<WeatherReport> reports;
	private ArrayList<WeatherForecast> forecasts;
	
	private WeatherReport currentReport;
	private WeatherForecast currentForecast;
	private WeatherServiceJson json;

	public WeatherInfoService() {
		recycle();
	}

	
	public void activate(int timeslotIndex, Instant postedTime) {
		if(currentReport!=null){
			reports.add(currentReport);
			updateJsonWithReport();
		}
		if(currentForecast!=null){
			forecasts.add(currentForecast);
		}

	}

	private void updateJsonWithReport() {
		try {
			json.getCloudCoverData().put(new JSONArray().put(currentReport.getCurrentTimeslot().getStartInstant().getMillis()).put(currentReport.getCloudCover()));
			json.getTemperatureData().put(new JSONArray().put(currentReport.getCurrentTimeslot().getStartInstant().getMillis()).put(currentReport.getTemperature()));
			json.getWindSpeedData().put(new JSONArray().put(currentReport.getCurrentTimeslot().getStartInstant().getMillis()).put(currentReport.getWindSpeed()));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}


	public void recycle() {
		reports = new ArrayList<WeatherReport>();
		forecasts = new ArrayList<WeatherForecast>();
		json = new WeatherServiceJson();
		currentForecast = null;
		currentReport = null;
	}


	public WeatherReport getCurrentReport() {
		return currentReport;
	}


	public void setCurrentReport(WeatherReport currentReport) {
		this.currentReport = currentReport;
	}


	public WeatherForecast getCurrentForecast() {
		return currentForecast;
	}


	public void setCurrentForecast(WeatherForecast currentForecast) {
		this.currentForecast = currentForecast;
	}


	public ArrayList<WeatherReport> getReports() {
		return reports;
	}


	public ArrayList<WeatherForecast> getForecasts() {
		return forecasts;
	}
	
	public WeatherServiceJson getJson() {
		return json;
	}
	
	

}
