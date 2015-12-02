package org.powertac.visualizer.services;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.joda.time.Instant;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherReport;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.powertac.visualizer.push.TariffMarketPusher;
import org.powertac.visualizer.push.WeatherPusher;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Service for weather-related data
 * 
 * @author Jurica Babic
 * 
 */
@Service
public class WeatherInfoService implements Recyclable,
		TimeslotCompleteActivation {

	private static Logger log = LogManager.getLogger(WeatherInfoService.class);

	private ConcurrentHashMap<Integer, WeatherReport> reports;

	private WeatherReport currentReport;
	private WeatherForecast currentForecast;
	@Autowired
	private VisualizerHelperService helper;
	@Autowired
	private VisualizerBean visualizerBean;

	public WeatherInfoService() {
		recycle();
	}

	public void recycle() {
		reports = new ConcurrentHashMap<Integer, WeatherReport>(1000, 0.75f, 1);
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

	public ConcurrentHashMap<Integer, WeatherReport> getReports() {
		return reports;
	}

	public void addReport(WeatherReport weatherReport) {
		currentReport = weatherReport;
		reports.put(weatherReport.getCurrentTimeslot().getSerialNumber(),
				weatherReport);

	}

	@Override
	public void activate(int timeslotIndex, Instant postedTime) {
		if (currentReport != null) {
			// // do the push:
			PushContext pushContext = PushContextFactory.getDefault()
					.getPushContext();
			Gson gson = new Gson();
			WeatherPusher weather = new WeatherPusher(
					helper.getMillisForIndex(currentReport.getCurrentTimeslot()
							.getSerialNumber()),
					currentReport.getTemperature(),
					currentReport.getWindSpeed(),
					currentReport.getWindDirection(),
					currentReport.getCloudCover(), currentReport.getTimeslotIndex());
			visualizerBean.setWeatherPusher(weather);
			String weatherReportPush = gson.toJson(weather);
			pushContext.push("/weather", weatherReportPush);
			
		}

	}
}
