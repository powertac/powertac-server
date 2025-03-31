/*
 * Copyright (c) 2011-2022 by the original authors
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
 * @author Erik Onarheim, Govert Buijs, John Collins
 */

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.ZoneOffset;

import org.powertac.common.*;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.exceptions.PowerTacException;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.SimEnd;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.logtool.LogtoolContext;
import org.powertac.logtool.LogtoolCore;
import org.powertac.logtool.ifc.ObjectReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WeatherService extends TimeslotPhaseProcessor
implements InitializationService
{
  static private Logger log = LogManager.getLogger(WeatherService.class);

  @ConfigurableValue(valueType = "String", bootstrapState = true,
          description = "Location of weather data to be reported")
  private String weatherLocation = "rotterdam";

  @ConfigurableValue(valueType = "String", bootstrapState = true,
          description = "Location of weather server rest url")
  private String serverUrl = "https://weather.powertac.org:8080/WeatherServer/faces/index.xhtml";

  // If network requests should be made asynchronously or not.
  @ConfigurableValue(valueType = "Boolean",
          description = "If network calls to weather server should block until finished")
  private boolean blocking = true;

  @ConfigurableValue(valueType = "String",
          description = "Location of weather file (XML or state) or URL (state)")
  private String weatherData = null;

  // length of reports and forecasts. Can't really change this
  @ConfigurableValue(valueType = "Integer",
          description = "Timeslot interval to make requests")
  private int weatherReqInterval = 24;

  @ConfigurableValue(valueType = "Integer",
          description = "Length of forecasts (in hours)")
  private int forecastHorizon = 24; // 24 hours

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private WeatherReportRepo weatherReportRepo;

  @Autowired
  private WeatherForecastRepo weatherForecastRepo;

  @Autowired
  private BrokerProxy brokerProxyService;

  @Autowired
  private ServerConfiguration serverProps;

  @Autowired
  private LogtoolCore logtool;

  // These dates need to be fetched when using not blocking
  private List<ZonedDateTime> aheadDays;
  private ZonedDateTime simulationBaseTime;
  private int daysAhead = 3;

  // if we're extracting from a state log, we cannot create multiple instances
  // of the StateFileExtractor. So this one is either null or not.
  private StateFileExtractor sfe = null;

  public int getWeatherReqInterval ()
  {
    return weatherReqInterval;
  }

  public String getServerUrl ()
  {
    return serverUrl;
  }

  public boolean isBlocking ()
  {
    return blocking;
  }

  public int getForecastHorizon ()
  {
    return forecastHorizon;
  }

  private String dateString (ZonedDateTime dateTime)
  {
    // Parse out year, month, day, and hour out of DateTime
    int y = dateTime.getYear();
    int m = dateTime.getMonthValue();
    int d = dateTime.getDayOfMonth();
    int h = dateTime.getHour() % 24;

    return String.format("%04d%02d%02d%02d", y, m, d, h);
  }

  private String dateStringLong (ZonedDateTime dateTime)
  {
    // Parse out year, month, day, and hour out of ZonedDateTime
    int y = dateTime.getYear();
    int m = dateTime.getMonthValue();
    int d = dateTime.getDayOfMonth();
    int h = dateTime.getHour() % 24;

    return String.format("%04d-%02d-%02d %02d:00", y, m, d, h);
  }

  private int getTimeIndex (ZonedDateTime dateTime)
  {
    // Used for testing
    if (simulationBaseTime == null) {
      simulationBaseTime = timeslotRepo.currentTimeslot().getStartTime();
    }

    long diff = dateTime.toInstant().toEpochMilli() - simulationBaseTime.toInstant().toEpochMilli();
    // WARNING:This assumes one-hour timeslots, but matches weather data granularity
    return (int) (diff / TimeService.HOUR);
  }

  // Make weather request if needed. Always broadcast reports and forecasts.
  @Override
  public void activate (Instant time, int phaseNumber)
  {
    long msec = time.toEpochMilli();
    if (msec % (getWeatherReqInterval() * TimeService.HOUR) != 0) {
      log.info("WeatherService reports not time to grab weather data.");
    }
    else {
      log.info("Timeslot "
          + timeslotRepo.currentTimeslot().getId()
          + " WeatherService reports time to make request for weather data");

      ZonedDateTime dateTime = timeslotRepo.currentTimeslot().getStartTime();
      if (blocking) {
        WeatherRequester wr = new WeatherRequester(dateTime);
        wr.run();
      }
      else {
        aheadDays.add(dateTime.plusDays(daysAhead));
        while (aheadDays.size() > 0) {
          WeatherRequester wr = new WeatherRequester(aheadDays.remove(0));
          new Thread(wr).start();
        }
      }
    }

    broadcastWeatherReports();
    broadcastWeatherForecasts();
  }

  private void broadcastWeatherReports ()
  {
    WeatherReport report = null;
    try {
      report = weatherReportRepo.currentWeatherReport();
    }
    catch (PowerTacException e) {
      log.error("Weather Service reports Weather Report Repo empty");
    }
    if (report == null) {
      // In the event of an error return a default
      log.error("null weather-report for : "
          + timeslotRepo.currentSerialNumber() +" "
          + timeslotRepo.currentTimeslot());
      brokerProxyService.broadcastMessage(new WeatherReport(timeslotRepo.currentSerialNumber(),
          0.0, 0.0, 0.0, 0.0));
    }
    else {
      brokerProxyService.broadcastMessage(report);
    }
  }

  private void broadcastWeatherForecasts ()
  {
    WeatherForecast forecast = null;
    try {
      forecast = weatherForecastRepo.currentWeatherForecast();
    }
    catch (PowerTacException e) {
      log.error("Weather Service reports Weather Forecast Repo emtpy");
    }
    if (forecast == null) {
      log.error("null weather-forecast for : "
          + timeslotRepo.currentSerialNumber() +" "
          + timeslotRepo.currentTimeslot());
      // In the event of an error return a default
      List<WeatherForecastPrediction> currentPredictions = new ArrayList<WeatherForecastPrediction>();
      for (int j = 1; j <= getForecastHorizon(); j++) {
        currentPredictions.add(
            new WeatherForecastPrediction(j, 0.0, 0.0, 0.0, 0.0));
      }
      brokerProxyService.broadcastMessage(new WeatherForecast(
          timeslotRepo.currentSerialNumber(), currentPredictions));
    }
    else {
      brokerProxyService.broadcastMessage(forecast);
    }
  }

  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    super.init();
    aheadDays = new CopyOnWriteArrayList<ZonedDateTime>();
    serverProps.configureMe(this);
    weatherReqInterval = Math.min(24, weatherReqInterval);
    simulationBaseTime = competition.getSimulationBaseTime().atZone(ZoneOffset.UTC);

    if (weatherData != null) {
      log.info("read from file in blocking mode");
      blocking = true;
    }

    if (!blocking) {
      log.info("Not blocking");
      ZonedDateTime dateTime = timeslotRepo.currentTimeslot().getStartTime();
      // Get the first 3 days of weather, blocking!
      for (int i = 0; i < daysAhead; i++) {
        WeatherRequester weatherRequester = new WeatherRequester(dateTime);
        weatherRequester.run();
        dateTime = dateTime.plusDays(1);
      }
    }

    return "WeatherService";
  }

  private class WeatherRequester implements Runnable
  {
    private ZonedDateTime requestDate;

    public WeatherRequester (ZonedDateTime requestDate)
    {
      this.requestDate = requestDate;
    }

    @Override
    public void run ()
    {
      String currentMethod = "";
      try {
        Data data = null;

        if (weatherData != null && weatherData.endsWith(".xml")) {
          currentMethod = "xml file";
          WeatherXmlExtractor wxe = new WeatherXmlExtractor(weatherData);
          String weatherXml = wxe.extractPartialXml(requestDate);
          data = parseXML(weatherXml);
        }
        else if (weatherData != null) {
          // could be URL, tar, tar.gz, .gz, or uncompressed state log
          // We cannot run multiple extractors in parallel on the same file;
          // this essentially negates the "blocking" feature
          currentMethod = "state file";
          log.info("Reading weather data from {}", weatherData);
          if (null == sfe)
            sfe = new StateFileExtractor(weatherData);
          data = sfe.extractData();
        }
        else {
          currentMethod = "web";
          data = webRequest();
        }

        processData(data);

        log.debug("Got data via a " + currentMethod + " request");
      }
      catch (Exception e) {
        log.error("Unable to get weather from weather : " + currentMethod);
        if (!blocking) {
          log.warn("Retrying : " + dateStringLong(requestDate));
          aheadDays.add(requestDate);
        }

        log.error(e.getMessage());
      }
    }

    private Data webRequest ()
    {
      String queryDate = dateString(requestDate);
      log.info("Query datetime value for REST call: " + queryDate);

      String urlString = String.format("%s?weatherDate=%s&weatherLocation=%s",
          getServerUrl(), queryDate, weatherLocation);
      log.info("Weather request URL {}", urlString);

      try {
        // Create a URLConnection object for a URL and send request
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        conn.setReadTimeout(10 * 1000);

        // Get the response in xml
        BufferedReader input = new BufferedReader(
            new InputStreamReader(conn.getInputStream()));

        return parseXML(input);
      }
      catch (FileNotFoundException fnfe) {
        log.error("FileNotFoundException on {}", urlString);
      }
      catch (SocketTimeoutException ste) {
        log.error("SocketTimeoutException on {}", urlString);
      }
      catch (IOException ioe) {
        log.error("IO Exception on URL {}", urlString);
      }
      catch (Exception e) {
        log.error("Exception Raised during network call on {}", urlString);
        e.printStackTrace();
      }

      return null;
    }

    private Data parseXML (Object input)
    {
      if (input == null) {
        log.warn("Input to parseXML was null");
        return null;
      }

      Data data = null;
      try {
        // Set up stream and aliases
        XStream xstream = XMLMessageConverter.getXStream();
        xstream.alias("data", Data.class);
        xstream.alias("weatherReport", WeatherReport.class);
        xstream.alias("weatherForecast", WeatherForecastPrediction.class);

        // Xml uses attributes for more compact data
        xstream.useAttributeFor(WeatherReport.class);
        xstream.registerConverter(new WeatherReportConverter(requestDate));

        // Xml uses attributes for more compact data
        xstream.useAttributeFor(WeatherForecastPrediction.class);
        xstream.registerConverter(new WeatherForecastConverter());

        // Unmarshall the xml input and place it into data container object
        try {
        if (input.getClass().equals(BufferedReader.class)) {
          data = (Data) xstream.fromXML((BufferedReader) input);
        }
        else if (input.getClass().equals(String.class)) {
          data = (Data) xstream.fromXML((String) input);
        }
        } catch (Exception ex) {
          log.error("Exception {} converting input from {}", ex.toString(), serverUrl);
        }

        if (data != null && (data.weatherReports.size() != weatherReqInterval ||
            data.weatherForecasts.size() != weatherReqInterval*forecastHorizon)) {
          data = null;
        }
      }
      catch (Exception e) {
        log.error("Exception Raised parsing XML : " + e.toString());
        e.printStackTrace();
        data = null;
      }

      return data;
    }

    private void processData (Data data) throws Exception
    {
      processWeatherData(data);
      processForecastData(data);
    }

    private void processWeatherData (Data data) throws NullPointerException
    {
      for (WeatherReport report : data.getWeatherReports()) {
        weatherReportRepo.add(report);
      }

      log.info(data.getWeatherReports().size() + " WeatherReports fetched");
    }

    private void processForecastData (Data data) throws Exception
    {
      int timeIndex = getTimeIndex(requestDate);

      List<WeatherForecastPrediction> currentPredictions =
          new ArrayList<WeatherForecastPrediction>();
      for (WeatherForecastPrediction prediction : data.getWeatherForecasts()) {
        currentPredictions.add(prediction);

        if (currentPredictions.size() == forecastHorizon) {
          // Add a forecast to the repo, increment to the next timeslot
          WeatherForecast newForecast =
              new WeatherForecast(timeIndex++, currentPredictions);
          weatherForecastRepo.add(newForecast);
          currentPredictions = new ArrayList<WeatherForecastPrediction>();
        }
      }

      log.info(data.getWeatherForecasts().size()
          + " WeatherForecasts fetched from xml response.");
    }
  }

  // Helper classes
  // This works only if you create a new one, or init the timeIndex,
  // prior to processing a batch
  private class WeatherReportConverter implements Converter
  {
    private int timeIndex;

    public WeatherReportConverter (ZonedDateTime requestDate)
    {
      super();
      this.timeIndex = getTimeIndex(requestDate);
    }

    @Override
    public boolean canConvert (@SuppressWarnings("rawtypes") Class clazz)
    {
      return clazz.equals(WeatherReport.class);
    }

    @Override
    public void marshal (Object source, HierarchicalStreamWriter writer,
                         MarshallingContext context)
    {
    }

    @Override
    public Object unmarshal (HierarchicalStreamReader reader,
                             UnmarshallingContext context)
    {
      String temp = reader.getAttribute("temp");
      String wind = reader.getAttribute("windspeed");
      String dir = reader.getAttribute("winddir");
      String cloudCvr = reader.getAttribute("cloudcover");

      return new WeatherReport(timeIndex++,
          Double.parseDouble(temp), Double.parseDouble(wind),
          Double.parseDouble(dir), Double.parseDouble(cloudCvr));
    }
  }

  private class WeatherForecastConverter implements Converter
  {
    public WeatherForecastConverter ()
    {
      super();
    }

    @Override
    public boolean canConvert (@SuppressWarnings("rawtypes") Class clazz)
    {
      return clazz.equals(WeatherForecastPrediction.class);
    }

    @Override
    public void marshal (Object source, HierarchicalStreamWriter writer,
                         MarshallingContext context)
    {
    }

    @Override
    public Object unmarshal (HierarchicalStreamReader reader,
                             UnmarshallingContext context)
    {
      String id = reader.getAttribute("id");
      String temp = reader.getAttribute("temp");
      String wind = reader.getAttribute("windspeed");
      String dir = reader.getAttribute("winddir");
      String cloudCvr = reader.getAttribute("cloudcover");

      return new WeatherForecastPrediction(Integer.parseInt(id),
          Double.parseDouble(temp), Double.parseDouble(wind),
          Double.parseDouble(dir), Double.parseDouble(cloudCvr));
    }
  }

  // Deprecated, but cannot be deleted until tests and test data are fixed.
  private class EnergyReport
  {
  }

  private class Data
  {
    private List<WeatherReport> weatherReports = new ArrayList<WeatherReport>();
    private List<WeatherForecastPrediction> weatherForecasts = new ArrayList<WeatherForecastPrediction>();
    private List<EnergyReport> energyReports = new ArrayList<EnergyReport>();

    public List<WeatherReport> getWeatherReports ()
    {
      return weatherReports;
    }

    public List<WeatherForecastPrediction> getWeatherForecasts ()
    {
      return weatherForecasts;
    }

    @SuppressWarnings("unused")
    public List<EnergyReport> getEnergyReports ()
    {
      return energyReports;
    }
  }

  private class WeatherXmlExtractor
  {
    /**
     * This class extracts a part of a weather-xml, which contacins the weather
     * for the complete duration of the simulation.
     * It returns 24 reports and 24 * 24 forecasts
     */
    private NodeList nodeListRead = null;
    private Document documentWrite;
    private Element weatherReports;
    private Element weatherForecasts;

    public WeatherXmlExtractor (String fileName)
    {
      try {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
            .newInstance();
        DocumentBuilder docBuilderRead = docBuilderFactory.newDocumentBuilder();
        Document documentRead = docBuilderRead.parse(new File(fileName));
        Node rootNode = documentRead.getDocumentElement();
        nodeListRead = rootNode.getChildNodes();

        // Output document
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilderWrite = docFactory.newDocumentBuilder();
        documentWrite = docBuilderWrite.newDocument();
        documentWrite.setXmlStandalone(true);
        Element rootElement = documentWrite.createElement("data");
        documentWrite.appendChild(rootElement);

        // weatherReports and weatherForecasts elements
        weatherReports = documentWrite.createElement("weatherReports");
        rootElement.appendChild(weatherReports);
        weatherForecasts = documentWrite.createElement("weatherForecasts");
        rootElement.appendChild(weatherForecasts);
      }
      catch (Exception ignored) {
      }
    }

    private String extractPartialXml (ZonedDateTime requestDate)
    {
      if (nodeListRead == null) {
        return null;
      }

      try {
        // Find 24 weatherReport starting at startDate
        for (int i = 0; i < nodeListRead.getLength(); i++) {
          Node currentNode = nodeListRead.item(i);

          if (!currentNode.getNodeName().equals("weatherReports")) {
            continue;
          }

          NodeList nodeListReports = currentNode.getChildNodes();
          findReports(nodeListReports, dateStringLong(requestDate));
        }

        // Find all weatherForecasts belonging to the 24 reports
        for (int i = 0; i < weatherReqInterval; i++) {
          for (int j = 0; j < nodeListRead.getLength(); j++) {
            Node currentNode = nodeListRead.item(j);

            if (!currentNode.getNodeName().equals("weatherForecasts")) {
              continue;
            }

            String origin = dateStringLong(requestDate.plusHours(i));
            NodeList nodes = currentNode.getChildNodes();
            findForecasts(nodes, origin);
          }
        }

        if (weatherReports.getChildNodes().getLength() != weatherReqInterval ||
            weatherForecasts.getChildNodes().getLength() !=
                weatherReqInterval * forecastHorizon) {
          return null;
        }

        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter buffer = new StringWriter();
        transformer.transform(new DOMSource(documentWrite), new StreamResult(buffer));

        return buffer.toString();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }

    private void findReports (NodeList nodes, String startDate)
    {
      // Find reports starting at startDate, copy 24 reports to output document
      for (int j = 0; j < nodes.getLength(); j++) {
        Node report = nodes.item(j);
        if (!report.getNodeName().equals("weatherReport")) {
          continue;
        }

        // TODO We shouldn't assume the reports are ordered on date
        String date = ((Element) report).getAttribute("date");
        if (date.compareTo(startDate) < 0) {
          continue;
        }

        Node temp = documentWrite.importNode(report, true);
        weatherReports.appendChild(temp);

        if (weatherReports.getChildNodes().getLength() == weatherReqInterval) {
          break;
        }
      }
    }

    private void findForecasts (NodeList nodes, String target)
    {
      // Find all forecasts belonging to a report-date, copy to output document
      for (int i = 0; i < nodes.getLength(); i++) {
        Node forecast = nodes.item(i);

        if (!forecast.getNodeName().equals("weatherForecast")) {
          continue;
        }

        String origin = ((Element) forecast).getAttribute("origin");
        if (!origin.equals(target)) {
          continue;
        }

        Node temp = documentWrite.importNode(forecast, true);
        weatherForecasts.appendChild(temp);
      }
    }
  }

  /**
   * This class extracts a part of a state file (or URL).
   * It returns $weatherReqInterval reports
   * and $weatherReqInterval forecasts, each with $forecastHorizon predictions
   */
  private class StateFileExtractor extends LogtoolContext
  {
    private String weatherSource = null;
    ObjectReader reader;
    WeatherReport saved = null;
    boolean initialized = false;

    public StateFileExtractor (String weatherData)
    {
      if (null == weatherData)
        // can't do much here
        return;
      weatherSource = weatherData;
    }

    private void initMaybe ()
    {
      if (initialized)
        return;
      logtool.resetDOR(false);
      logtool.includeClassname(WeatherReport.class.getName());
      logtool.includeClassname(WeatherForecastPrediction.class.getName());
      logtool.includeClassname(WeatherForecast.class.getName());
      reader = logtool.getObjectReader(weatherSource);
      initialized = true;
    }

    public Data extractData ()
    {
      initMaybe();
      // We read weather data in "blocks" because that's how it appears in a log.
      // We assume (and check) that the next weather report has a timeslot that matches
      // the startIndex.
      log.info("Extracting weather data from {}", weatherSource);
      if (weatherSource == null) {
        log.error("Cannot extract weather data from null");
        return null;
      }
      
      // If saved is non-null and not a WeatherReport, we've reached the end of the 
      // available weather data
      if (null != saved && saved.getClass() != WeatherReport.class)
        return null;
      
      Data data = new Data();

      saved = loadBlock(saved, data);
      
      return data;
    }

    public WeatherReport loadBlock (WeatherReport saved, Data data)
    {
      WeatherReport result = null;

      // if we've saved one from the last batch, handle it now
      if (null != saved) {
        data.getWeatherReports().add(saved);
      }

      Object next = reader.getNextObject();
      if (!(next.getClass() == WeatherReport.class))
      {
        if (next.getClass() == SimEnd.class) {
          // end of file reached
          try {
            log.info("SimEnd on weather data, aborting");
            File abortFile = new File("abort");
            PrintWriter writer = new PrintWriter(abortFile);
            writer.println("Out of weather data -- abort");
            writer.close();
          } catch (FileNotFoundException fnf) {
            log.error("Cannot open abort file");
          }
        }
        else {
          log.error("Bad weather data {}", next);
        }
      }
      else {
        // it's the first WeatherReport in this block

        while (next.getClass() == WeatherReport.class) {
          data.getWeatherReports().add((WeatherReport) next);
          next = reader.getNextObject();
        }
        while (next.getClass() == WeatherForecastPrediction.class) {
          data.getWeatherForecasts().add((WeatherForecastPrediction) next);
          next = reader.getNextObject();
        }
        // now we have some WeatherForecast instances that we'll ignore
        while (next.getClass() == WeatherForecast.class) {
          next = reader.getNextObject();
        }
        if (next.getClass() == WeatherReport.class) {
          // save it for the beginning of the next block
          result = (WeatherReport) next;
        }
      }
      return result;
    }
  }
}
