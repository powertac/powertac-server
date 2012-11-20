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
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.powertac.common.*;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

// Import common

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

	static private Logger log = Logger.getLogger(WeatherService.class.getName());

	private Timeslot currentTime;

	// Read from configuration
	@ConfigurableValue(valueType = "Integer", description = "Timeslot interval to make requests")
	private int weatherReqInterval = 12;

	@ConfigurableValue(valueType = "String", description = "Location of weather data to be reported")
	private String weatherLocation = "rotterdam";

	@ConfigurableValue(valueType = "String", description = "Location of weather server rest url")
  private String serverUrl = "http://wolf-08.fbk.eur.nl:8080/WeatherServer/faces/index.xhtml";

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

	// Make actual web request to the weather-server or get from file
	@Override
	public void activate(Instant time, int phaseNumber) {
		// Error check the request interval
		if (weatherReqInterval > 24) {
			weatherReqInterval = 24;
		}

    long msec = time.getMillis();
    if (msec % (getWeatherReqInterval() * TimeService.HOUR) != 0) {
      log.info("WeatherService reports not time to grab weather data.");
    } else {
      requestData();
    }

		broadcastWeatherReports();
    broadcastWeatherForecasts();
	}

  private void requestData () {
    log.info("Timeslot "
        + timeslotRepo.currentTimeslot().getId()
        + " WeatherService reports time to make network request for weather "
        + "data in blocking = " + isBlocking() + " mode.");

    String currentMethod = "";
    try {
      Data data = webRequest(timeslotRepo.currentTimeslot(), 1);
      processData(data, timeslotRepo.currentTimeslot());
    } catch (Throwable e) {
      log.error("Unable to get weather from weather " + currentMethod) ;
      log.error(e.getMessage());
    }
  }

  private Data webRequest(Timeslot time, int randomSeed) {
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
          DateTimeFieldType.clockhourOfDay()) % 24;

      // Create date query string
      String queryDate = String.format(
          "%04d%02d%02d%02d", year, month, day, hour);

      log.info("Query datetime value for REST call: " + queryDate);

      URL url = new URL(getServerUrl() + "?weatherDate=" + queryDate
          + "&weatherLocation=" + weatherLocation);

      URLConnection conn = url.openConnection();

      // Get the response in xml
      BufferedReader input = new BufferedReader(new InputStreamReader(
          conn.getInputStream()));

      XStream xstream = new XStream();

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
      Data data = (Data) xstream.fromXML(input);

      return data;
    } catch (Exception e) {
      log.error("Exception Raised during newtork call: " + e.toString());
      System.out.println("Exception Raised: " + e.toString());
      e.printStackTrace();
      return null;
    }
  }

  private void processData(Data data, Timeslot time) {
    processWeatherData(data, time);
    processForecastDataNew(data, time);
  }

  private void processWeatherData (Data data, Timeslot time) {
    for (WeatherReport report: data.getWeatherReports()) {
      weatherReportRepo.add(report);
    }

    currentTime = time;
    log.info(data.getWeatherReports().size()
        + " WeatherReports fetched from xml response.");
    weatherReportRepo.runOnce();
  }

  private void processForecastDataNew (Data data, Timeslot time) {
    List<WeatherForecastPrediction> currentPredictions =
        new ArrayList<WeatherForecastPrediction>();
    int j = 0;
    for (WeatherForecastPrediction forecast: data.getWeatherForecasts()) {
      currentPredictions.add(
          new WeatherForecastPrediction( (j % 24) + 1,
              forecast.getTemperature(), forecast.getWindSpeed(),
              forecast.getWindDirection(), forecast.getCloudCover()));

      j++;

      if ((j % forecastHorizon) == 0) {
        // Add a forecast to the repo, increment to the next timeslot
        WeatherForecast newForecast = new WeatherForecast(currentTime,
            currentPredictions);
        weatherForecastRepo.add(newForecast);
        currentPredictions = new ArrayList<WeatherForecastPrediction>();

        if (currentTime == null) {
          log.error("Null timeslot when adding forecasts to weatherForecastRepo");
        } else {
          currentTime = currentTime.getNext();
        }
      }
    }

    log.info(data.getWeatherForecasts().size()
        + " WeatherForecasts fetched from xml response.");
    weatherForecastRepo.runOnce();
  }

  private void broadcastWeatherReports () {
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
  }

  private void broadcastWeatherForecasts () {
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

	@Override
	public void setDefaults() {}

	@Override
	public String initialize(Competition competition, List<String> completedInits)
  {
		super.init();
		serverProps.configureMe(this);
		return "WeatherService";
	}

  // Helper classes
  public class WeatherReportConverter implements Converter {

    @Override
    public boolean canConvert(Class clazz) {
      return clazz.equals(WeatherReport.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    @SuppressWarnings("static-access")
    @Override
    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
      //String date = reader.getAttribute("date");
      String temp = reader.getAttribute("temp");
      String wind = reader.getAttribute("windspeed");
      String dir = reader.getAttribute("winddir");
      String cloudCvr = reader.getAttribute("cloudcover");
      //String location = reader.getAttribute("location");

      WeatherReport wr = new WeatherReport(currentTime,
          Double.parseDouble(temp), Double.parseDouble(wind),
          Double.parseDouble(dir), Double.parseDouble(cloudCvr));

      try {
        currentTime = currentTime.getNext();
        return wr;
      } catch (Exception e) {
        return null;
      }
    }
  }

  public class WeatherForecastConverter implements Converter {
		private int idCounter = 0;

    @Override
    public boolean canConvert(Class clazz) {
      return clazz.equals(WeatherForecastPrediction.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
      //String date = reader.getAttribute("date");
      String temp = reader.getAttribute("temp");
      String wind = reader.getAttribute("windspeed");
      String dir = reader.getAttribute("winddir");
      String cloudCvr = reader.getAttribute("cloudcover");
      //String location = reader.getAttribute("location");

      return new WeatherForecastPrediction(idCounter++,
          Double.parseDouble(temp), Double.parseDouble(wind),
          Double.parseDouble(dir), Double.parseDouble(cloudCvr));
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
}