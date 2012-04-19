/*
 * Copyright (c) 2011 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.server;

/**
 *  This is the Power TAC simulator weather service that queries an existing
 *  weather server for weather data and serves it to the brokers logged into 
 *  the game.
 *  
 * @author Erik Onarheim
 */

// Import network java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

// Import common
import org.apache.log4j.Logger;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.joda.time.format.ISODateTimeFormat;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.WeatherForecast;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.exceptions.PowerTacException;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

//TODO: Create issue Asynchronous and Blocking modes that expose the flags
//xTODO: Create log messages in weatherService
//TxODO: Implement WeatherTests with some default data
//xTODO: XML serialization test for WeatherReport and WeatherForecast WeatherForecastPrediction, see org.powertac.common.msg tests (JUnit4 tests)
//xTODO: Repo tests copy those
//XTODO: WeatherService Tests BEEANS!!
//xTODO: Pull request Tests, WeatherService, Repos
//xTODO: Basic JSF MVC application
//XTODO: Switch implements to extends in timeslotphaseprocessor
//XTODO: Plugin Config object for weatherServers indicating location and date range, place in PluginConfigRepo

@Service
public class WeatherService extends TimeslotPhaseProcessor implements
		InitializationService {

	static private Logger log = Logger
			.getLogger(WeatherService.class.getName());
	private boolean requestFailed = false;

	// Xstream to convert xml to objects
	private XStream xstream;

	private Timeslot currentTime;

	// Read from configuration
	@ConfigurableValue(valueType = "Integer", description = "Timeslot interval to make requests")
	private int weatherReqInterval = 12;

	// private int daysOut = 1;
	private int currentWeatherId = 1;

	@ConfigurableValue(valueType = "String", description = "Location of weather data to be reported")
	private String weatherLocation = "minneapolis";

	@ConfigurableValue(valueType = "String", description = "Location of weather server rest url")
	private String serverUrl = "http://tac05.cs.umn.edu:8080/WeatherServer/faces/index.xhtml";

	// If network requests should be made asynchronously or not.
	@ConfigurableValue(valueType = "Boolean", description = "If network calls to weather server should block until finished")
	private boolean blocking = true;

	// length of forecasts
	@ConfigurableValue(valueType = "Integer", description = "Length of forecasts (in hours)")
	private int forecastHorizon = 24; // 24 hours

	@Autowired
	private TimeslotRepo timeslotRepo;

	// @Autowired
	// private PluginConfigRepo pluginConfigRepo;

	@Autowired
	private WeatherReportRepo weatherReportRepo;

	@Autowired
	private WeatherForecastRepo weatherForecastRepo;

	@Autowired
	private BrokerProxy brokerProxyService;

	@Autowired
	private ServerConfiguration serverProps;

	public int getWeatherReqInterval() {
		return weatherReqInterval;
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public boolean isBlocking() {
		return blocking;
	}

	public int getForecastHorizon() {
		return forecastHorizon;
	}

	public class WeatherReportConverter implements Converter {

		@Override
		public boolean canConvert(Class clazz) {
			return clazz.equals(WeatherReport.class);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			// TODO Auto-generated method stub

		}

		@SuppressWarnings("static-access")
		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			WeatherReport wr;// = new WeatherReport();
			String Id = reader.getAttribute("id");
			String Date = reader.getAttribute("date");
			String Temp = reader.getAttribute("temp");
			String Wind = reader.getAttribute("windspeed");
			String Dir = reader.getAttribute("winddir");
			String CloudCvr = reader.getAttribute("cloudcover");
			String Location = reader.getAttribute("location");

			double cvr = 0.0;
			if (CloudCvr.equalsIgnoreCase("clr")) {
				cvr = 0.0;
			} else if (CloudCvr.equalsIgnoreCase("sct")) {
				cvr = 3.0 / 8.0;
			} else if (CloudCvr.equalsIgnoreCase("bkn")) {
				cvr = 6.0 / 8.0;
			} else if (CloudCvr.equalsIgnoreCase("ovc")) {
				cvr = 1.0;
			} else if (CloudCvr.equalsIgnoreCase("obs")) {
				cvr = 1.0;
			} else {
				cvr = 1.0;
			}
			// Instant i = Instant.parse(Date.replace(" ", "T").replace(".0",
			// ""), ISODateTimeFormat.dateHourMinuteSecond());
			// Timeslot t = timeslotRepo.findByInstant(i);
			wr = new WeatherReport(currentTime, Double.parseDouble(Temp),
					Double.parseDouble(Wind), Double.parseDouble(Dir
							.equalsIgnoreCase("***") ? "0.0" : Dir), cvr);

			currentTime = currentTime.getNext();

			return wr;
		}

	}

	public class WeatherForecastConverter implements Converter {

		@Override
		public boolean canConvert(Class clazz) {
			return clazz.equals(WeatherForecastPrediction.class);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			// TODO Auto-generated method stub

		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			WeatherForecastPrediction wp;// = new WeatherReport();
			String Id = reader.getAttribute("id");
			String Date = reader.getAttribute("date");
			String Temp = reader.getAttribute("temp");
			String Wind = reader.getAttribute("windspeed");
			String Dir = reader.getAttribute("winddir");
			String CloudCvr = reader.getAttribute("cloudcover");
			String Location = reader.getAttribute("location");

			double cvr = 0.0;
			if (CloudCvr.equalsIgnoreCase("clr")) {
				cvr = 0.0;
			} else if (CloudCvr.equalsIgnoreCase("***")) {
				cvr = 0.0;
			} else if (CloudCvr.equalsIgnoreCase("sct")) {
				cvr = 3.0 / 8.0;
			} else if (CloudCvr.equalsIgnoreCase("bkn")) {
				cvr = 6.0 / 8.0;
			} else if (CloudCvr.equalsIgnoreCase("ovc")) {
				cvr = 1.0;
			} else if (CloudCvr.equalsIgnoreCase("obs")) {
				cvr = 1.0;
			} else {
				cvr = 1.0;
			}

			wp = new WeatherForecastPrediction(Integer.parseInt(Id),
					Double.parseDouble(Temp), Double.parseDouble(Wind),
					Double.parseDouble(Dir), cvr);

			return wp;
		}
	}

	private class EnergyReport {

	}

	public class Data {
		private List<WeatherReport> weatherReports = new ArrayList<WeatherReport>();
		private List<WeatherForecastPrediction> weatherForecasts = new ArrayList<WeatherForecastPrediction>();
		private List<EnergyReport> energyReports = new ArrayList<EnergyReport>();

		public List<WeatherReport> getWeatherReports() {
			return weatherReports;
		}

		public void setWeatherReports(List<WeatherReport> weatherReports) {
			this.weatherReports = weatherReports;
		}

		public List<WeatherForecastPrediction> getWeatherForecasts() {
			return weatherForecasts;
		}

		public void setWeatherForecasts(
				List<WeatherForecastPrediction> weatherForecasts) {
			this.weatherForecasts = weatherForecasts;
		}

		public List<EnergyReport> getEnergyReports() {
			return energyReports;
		}

		public void setEnergyReports(List<EnergyReport> energyReports) {
			this.energyReports = energyReports;
		}

	}

	// Make actual web request to the weather-server
	@Override
	public void activate(Instant time, int phaseNumber) {
		// Error check the request interval
		if (getWeatherReqInterval() > 24) {
			// log.error("weather request interval ${weatherRequestInterval} > 24 hr"
			weatherReqInterval = 24;
		}

		// request weather data periodically
		long msec = time.getMillis();// timeService.getCurrentTime().getMillis();
		if (msec % (getWeatherReqInterval() * TimeService.HOUR) == 0) {
			log.info("Timeslot "
					+ timeslotRepo.currentTimeslot().getId()
					+ " WeatherService reports time to make network request for weather data in blocking = "
					+ isBlocking() + " mode.");

			// time to publish
			// log.info "Requesting Weather from " + serverUrl + "=" +
			// currentWeatherId + " at time: " + time

			// Attempt to make a web request to the weather server
			try {
				// Need try/catch for invalid host strings

				// currentWeatherId+=(2*weatherReqInterval) // 2 weather
				// reports per hour
				// System.out.println("Instant Time: " + time);
				// System.out.println("Timeslot Time: " +
				// timeslotRepo.currentTimeslot());
				webRequest(timeslotRepo.currentTimeslot(), 1);
				currentWeatherId += (2 * getWeatherReqInterval());
				requestFailed = false;

			} catch (Throwable e) {
				// log.error "Unable to connect to host: " + serverUrl
				requestFailed = true;
			}
		} else {
			log.info("WeatherService reports not time to grab weather data.");
		}

		// broadcast weather data to brokers
		WeatherReport report = null;
		try {
			report = weatherReportRepo.currentWeatherReport();
		} catch (PowerTacException e) {
			log.error("Weather Service reports Weather Report Repo empty");
		}
		if (report == null) {
			// In the event of an error return a default
			log.error("null weather-report!");
			brokerProxyService.broadcastMessage(new WeatherReport(timeslotRepo
					.currentTimeslot(), 0.0, 0.0, 0.0, 0.0));
		} else {
			brokerProxyService.broadcastMessage(report);
		}

		WeatherForecast forecast = null;
		try {
			forecast = weatherForecastRepo.currentWeatherForecast();
		} catch (PowerTacException e) {
			log.error("Weather Service reports Weather Forecast Repo emtpy");
		}
		if (forecast == null) {
			log.error("null weather-forecast!");
			// In the event of an error return a default
			List<WeatherForecastPrediction> currentPredictions = new ArrayList<WeatherForecastPrediction>();
			for (int j = 1; j <= getForecastHorizon(); j++) {
				currentPredictions.add(new WeatherForecastPrediction(j, 0.0,
						0.0, 0.0, 0.0));
			}

			brokerProxyService.broadcastMessage(new WeatherForecast(
					timeslotRepo.currentTimeslot(), currentPredictions));
		} else {
			brokerProxyService.broadcastMessage(forecast);
		}
	}

	// Forecasts are random and must be repeatable from the same seed
	private boolean webRequest(Timeslot time, int randomSeed) {
		currentTime = time;


		try {
			// Create a URLConnection object for a URL and send request

			// Parse out year, month, day, and hour out of Timeslot
			int year = currentTime.getStartInstant().get(
					DateTimeFieldType.year());
			int month = currentTime.getStartInstant().get(
					DateTimeFieldType.monthOfYear());
			int day = currentTime.getStartInstant().get(
					DateTimeFieldType.dayOfMonth());
			int hour = currentTime.getStartInstant().get(
					DateTimeFieldType.clockhourOfDay()) - 1;// Weather server
															// parses hours from
															// 0-23

			// Create date query string
			String queryDate = String.format("%02d%02d%02d%04d", hour, day,
					month, year);

			log.info("Query datetime value for REST call: " + queryDate);

			URL url = new URL(getServerUrl() + "?weatherDate=" + queryDate
					+ "&weatherLocation=" + weatherLocation);

			URLConnection conn = url.openConnection();

			// Get the response in xml
			BufferedReader input = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			
	
			xstream = new XStream();

			// Set up alias
			xstream.alias("data", Data.class);
			xstream.alias("weatherReport", WeatherReport.class);
			xstream.alias("weatherForecast", WeatherForecastPrediction.class);

			// Xml uses attributes for more compact data
			xstream.useAttributeFor(WeatherReport.class);
			xstream.registerConverter(new WeatherReportConverter());

			// Xml uses attributes for more compact data
			xstream.useAttributeFor(WeatherForecastPrediction.class);
			xstream.registerConverter(new WeatherForecastConverter());

			// Unmarshall the xml input and place it into data container object
			Data d = (Data) xstream.fromXML(input);

			// Debug
			// System.out.println("Reports Gathered: " +
			// d.getWeatherReports().size());
			// System.out.println("Forecasts Gathered: " +
			// d.getWeatherReports().size());
			for (WeatherReport r : d.getWeatherReports()) {
				weatherReportRepo.add(r);
			}

			currentTime = time;
			log.info(d.getWeatherReports().size() + " WeatherReports fetched from xml response.");
			weatherReportRepo.runOnce();

			List<WeatherForecastPrediction> currentPredictions;
			for (int i = 1; i <= 2 * getWeatherReqInterval(); i++) {
				currentPredictions = new ArrayList<WeatherForecastPrediction>();

				int j = 1;
				for (WeatherForecastPrediction f : d.getWeatherForecasts()) {
					if (f.getId() > i && j <= getForecastHorizon()) {
						currentPredictions
								.add(new WeatherForecastPrediction(j, f
										.getTemperature(), f.getWindSpeed(), f
										.getWindDirection(), f.getCloudCover()));
					}
					j++;
				}
				if(currentPredictions.size() != getForecastHorizon()){
					log.error("Forecast horizon does not match the predictions parsed!");
				}
				WeatherForecast newForecast = new WeatherForecast(currentTime,
						currentPredictions);
				// Add a forecast to the repo, increment to the next timeslot
				weatherForecastRepo.add(newForecast);

				if (currentTime == null) {
					log.error("Null timeslot when adding forecasts to weatherForecastRepo");
				} else {
					currentTime = currentTime.getNext();
				}
			}
			log.info(d.getWeatherForecasts().size() + " WeatherForecasts fetched from xml response.");
			weatherForecastRepo.runOnce();
		} catch (Exception e) {

			log.error("Exception Raised during newtork call: " + e.toString());
			System.out.println("Exception Raised: " + e.toString());
			e.printStackTrace();
			return false;
		}

		return true;

	}

	@Override
	public void setDefaults() {
	}

	@Override
	public String initialize(Competition competition,
			List<String> completedInits) {
		super.init();
		serverProps.configureMe(this);
		return "WeatherService";
	}

	public String getWeatherLocation() {
		return weatherLocation;
	}

	public void setWeatherLocation(String weatherLocation) {
		this.weatherLocation = weatherLocation;
	}
}