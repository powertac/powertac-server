package org.powertac.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.apache.commons.configuration2.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.powertac.common.Competition;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.logtool.LogtoolCore;
import org.powertac.logtool.common.DomainBuilder;
import org.powertac.logtool.common.DomainObjectReader;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.TreeMap;

public class WeatherServiceTest
{
  //@Autowired
  private ServerConfiguration serverPropertiesService;

  //@Autowired
  private WeatherService weatherService;

  //@Autowired
  private TimeslotRepo timeslotRepo;

  //@Autowired
  private TimeService timeService;

  //@Autowired
  private WeatherReportRepo weatherReportRepo;

  //@Autowired
  private WeatherForecastRepo weatherForecastRepo;

  //@Autowired
  private LogtoolCore logtoolCore;

  Instant start;
  Instant next;

  private Competition comp;
  private Configurator config;

  @BeforeEach
  public void setUp() throws Exception
  {
    weatherService = new WeatherService();

    weatherReportRepo = new WeatherReportRepo();
    ReflectionTestUtils.setField(weatherService, "weatherReportRepo", weatherReportRepo);
    weatherReportRepo.recycle();

    weatherForecastRepo = new WeatherForecastRepo();
    ReflectionTestUtils.setField(weatherService, "weatherForecastRepo", weatherForecastRepo);
    weatherForecastRepo.recycle();
    
    timeService = new TimeService();
    timeslotRepo = new TimeslotRepo();
    ReflectionTestUtils.setField(timeslotRepo, "timeService", timeService);
    ReflectionTestUtils.setField(weatherReportRepo, "timeslotRepo", timeslotRepo);
    ReflectionTestUtils.setField(weatherForecastRepo, "timeslotRepo", timeslotRepo);
    ReflectionTestUtils.setField(weatherService, "timeslotRepo", timeslotRepo);
    timeslotRepo.recycle();

    BrokerProxy brokerProxyService = mock(BrokerProxy.class);
    ReflectionTestUtils.setField(weatherService, "brokerProxyService", brokerProxyService);

    serverPropertiesService = mock(ServerConfiguration.class);
    ReflectionTestUtils.setField(weatherService, "serverProps", serverPropertiesService);

    // needed by TimeslotPhaseProcessor
    CompetitionControl competitionControlService = mock(CompetitionControl.class);
    ReflectionTestUtils.setField(weatherService, "competitionControlService", competitionControlService);

    logtoolCore = new LogtoolCore();
    DomainObjectReader dor = new DomainObjectReader();
    ReflectionTestUtils.setField(logtoolCore, "reader", dor);
    DomainBuilder db = mock(DomainBuilder.class);
    ReflectionTestUtils.setField(logtoolCore, "domainBuilder", db);
    ReflectionTestUtils.setField(weatherService, "logtool", logtoolCore);

    //reset(serverPropertiesService);

    // Set the current instant to a time when we are requesting data
    start = new DateTime(2010, 4, 1, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
    comp = Competition.newInstance("WeatherService test")
            .withSimulationBaseTime(start);
    Competition.setCurrent(comp);
    next = start.plus(comp.getTimeslotDuration());
    timeService.setClockParameters(start.getMillis(),
                                   comp.getSimulationRate(),
                                   comp.getSimulationModulo());
    timeService.setCurrentTime(start);
    //if (timeslotRepo.currentTimeslot() == null) {
    //	for (int i = 0; i < 48; i++) {
    //		timeslotRepo.makeTimeslot(start.plus(comp.getTimeslotDuration()
    //				* i));
    //	}
    //}

    // set up the weather service under test
    //weatherService = new WeatherService();
    ReflectionTestUtils.setField(weatherService, "blocking", true);
    ReflectionTestUtils.setField(weatherService, "weatherData", "src/test/resources/config/weather.xml");

    //comp = Competition.newInstance("weather-test");

    // Set up serverProperties mock
    config = new Configurator();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(serverPropertiesService).configureMe(any());
  }

  // initialization without a configuration
  @Test
  public void testNormalInitialization()
  {
    String properUrl = "https://weather.powertac.org:8080/WeatherServer/faces/index.xhtml";
    String result = weatherService
            .initialize(comp, new ArrayList<String>());
    assertEquals(result, "WeatherService", "correct return value");
    assertEquals(24, weatherService.getWeatherReqInterval(), "correct req interval");
    assertEquals(24, weatherService.getForecastHorizon(), "correct forecast horizon");
    assertTrue(weatherService.isBlocking(), "correct blocking mode");
    assertEquals(properUrl, weatherService.getServerUrl(), "correct server url");
  }

  // config max/min
  @Test
  public void testConfigInitialization()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("server.weatherService.serverUrl", "localhost");
    map.put("server.weatherService.weatherReqInterval", "6");
    map.put("server.weatherService.blocking", "true");
    map.put("server.weatherService.forecastHorizon", "12");
    MapConfiguration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);

    String result = weatherService
            .initialize(comp, new ArrayList<String>());
    assertEquals(result, "WeatherService", "correct return value");
    assertEquals(0, "localhost".compareTo(weatherService.getServerUrl()), 1e-6, "correct url");
    assertEquals(6, weatherService.getWeatherReqInterval(), 1e-6, "correct weather req interval");
    assertEquals(true, weatherService.isBlocking(), "correct blocking mode");
    assertEquals(12, weatherService.getForecastHorizon(), "correct forecast horizon");
  }

  @Test
  public void dataFetchTest()
  {
    // Sanity check on autowire
    assertNotNull(timeslotRepo);
    assertNotNull(weatherReportRepo);
    assertNotNull(weatherForecastRepo);
    assertNotNull(weatherService);

    // Check that there are indeed 24 timeslots enabled
    assertEquals(24, timeslotRepo.enabledTimeslots().size());

    weatherService.activate(start, 1);

    // Check that 24 weather reports entered the repo
    assertEquals(24, weatherReportRepo.count());

    // Check that 24 weather forecast entered the repo
    assertEquals(24, weatherForecastRepo.count());
  }

  @Test
  public void currentTimeDataTest()
  {
    weatherService.activate(start, 1);

    assertEquals(24, weatherReportRepo.count());

    //timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");

    // Check to see that the weatherReportRepo only gives the current
    // timeslot
    // weather report
    int index = weatherReportRepo.currentWeatherReport().getTimeslotIndex();
    Timeslot slot = timeslotRepo.findBySerialNumber(index);
    assertEquals(start, slot.getStartInstant());
    assertEquals(timeslotRepo.currentTimeslot(), slot);

    // Check to see that the next timeslot is as expected
    timeService.setCurrentTime(next);
    index = weatherReportRepo.currentWeatherReport().getTimeslotIndex();
    slot = timeslotRepo.findBySerialNumber(index);
    assertEquals(next, slot.getStartInstant());
    assertEquals(timeslotRepo.currentTimeslot(), slot);

    // Check that we can read backwards only 2 timeslots (current +
    // previous)
    assertEquals(2, weatherReportRepo.allWeatherReports().size());

    // Check that the 2 timeslots are different in the repo
    assertFalse((weatherReportRepo.allWeatherReports().get(0).getTimeslotIndex() ==
            weatherReportRepo.allWeatherReports().get(1).getTimeslotIndex()),
            "different timeslots");
  }

  @Test
  public void currentForecastTest()
  {
    weatherService.activate(start, 1);
    // Check that there is a forecast for the current timeslot
    timeService.setCurrentTime(next);
    assertNotNull(weatherForecastRepo.currentWeatherForecast());

    // Check that we can read backwards only 2 timeslots (current +
    // previous)
    assertEquals(2, weatherForecastRepo.allWeatherForecasts().size());
  }

  @Test
  public void testReportValues()
  {
    weatherService.activate(start, 1);

    WeatherReport wr = weatherReportRepo.allWeatherReports().get(0);
    assertEquals(3.5, wr.getTemperature(), .0001);
    assertEquals(4.0, wr.getWindSpeed(), .0001);
    assertEquals(250.0, wr.getWindDirection(), .0001);
    assertEquals(1.0, wr.getCloudCover(), .0001);

    // Test that currentWeatherId increments correctly
    Instant reqTime = timeslotRepo.findBySerialNumber(24).getStartInstant();
    assertNotNull(reqTime);
    assertEquals(24, weatherReportRepo.count());

    timeService.setCurrentTime(reqTime);
    weatherService.activate(reqTime, 1);

    // Check that 48 weather reports and forecasts entered the repo
    assertEquals(48, weatherReportRepo.count());
    assertEquals(48, weatherForecastRepo.count());

    // Check beginning weather
    assertEquals(false, wr.getTemperature() == weatherReportRepo
            .allWeatherReports().get(24).getTemperature());

    // Check ending weather
    timeService.setCurrentTime(timeslotRepo.findBySerialNumber(47)
                               .getStartInstant());
    assertEquals(false,
                 weatherReportRepo.allWeatherReports().get(23).getTemperature() == weatherReportRepo
                 .allWeatherReports().get(47).getTemperature());
    assertEquals(false, wr.getId() == weatherReportRepo.allWeatherReports()
            .get(24).getId());
  }

  @Test
  public void testForecastValues()
  {
    weatherService.activate(start, 1);

    // There should be 24 predictions in the forecast
    assertEquals(24, weatherForecastRepo.currentWeatherForecast()
                 .getPredictions().size());

    // Predictions should increment by one each time
    int i = 1;
    for (WeatherForecastPrediction p : weatherForecastRepo
            .currentWeatherForecast().getPredictions()) {
      assertEquals(i, p.getForecastTime());
      i++;
    }
  }

  @Test
  public void testReadFromStatelog ()
  {
    String statelog = "src/test/resources/artifacts/weather-data.state";
    ReflectionTestUtils.setField(weatherService, "weatherData", statelog);
    weatherService.activate(start, 1);
    // There should be 24 weather reports
    assertEquals(24, weatherReportRepo.count());
  }

  @Test
  public void testReadFromCompressedStatelog ()
  {
    String statelog = "src/test/resources/artifacts/weather-data.tgz";
    ReflectionTestUtils.setField(weatherService, "weatherData", statelog);
    weatherService.activate(start, 1);
    // There should be 24 weather reports
    assertEquals(24, weatherReportRepo.count());
  }
}
