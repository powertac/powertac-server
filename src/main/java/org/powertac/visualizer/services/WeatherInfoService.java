package org.powertac.visualizer.services;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherReport;
import org.powertac.visualizer.interfaces.Recyclable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Service for weather-related data
 * 
 * @author Jurica Babic
 * 
 */
@Service
public class WeatherInfoService implements Recyclable {

	private static Logger log = Logger.getLogger(WeatherInfoService.class);

	private ConcurrentHashMap<Long, WeatherReport> reports;

	private WeatherReport currentReport;
	private WeatherForecast currentForecast;

	public WeatherInfoService() {
		recycle();
	}

	public void recycle() {
		reports = new ConcurrentHashMap<Long, WeatherReport>(1000, 0.75f, 1);
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

	public ConcurrentHashMap<Long, WeatherReport> getReports() {
		return reports;
	}

	public void addReport(WeatherReport weatherReport) {
		currentReport = weatherReport;
		reports.put(weatherReport.getCurrentTimeslot().getStartInstant().getMillis(), weatherReport);

	}

}
