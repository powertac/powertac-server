package org.powertac.server;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import java.util.ArrayList;
import java.util.TreeMap;

//import javax.annotation.Resource;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Competition;
import org.powertac.common.TimeService;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:weather-test-config.xml" })
@DirtiesContext
public class WeatherServiceTest
{
  @Autowired
  private ServerConfiguration serverPropertiesService;

  //@Autowired
  private WeatherService weatherService;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private TimeService timeService;

  @Autowired
  private WeatherReportRepo weatherReportRepo;

  @Autowired
  private WeatherForecastRepo weatherForecastRepo;

  @Autowired
  private BrokerProxy brokerProxyService;
  
  @Autowired
  private CompetitionControl competitionControlService;

  Instant start;
  Instant next;

  private Competition comp;
  private Configurator config;

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    Logger.getRootLogger().setLevel(Level.DEBUG);
  }

  @Before
  public void setUp () throws Exception
  {
    weatherService = new WeatherService();
    timeslotRepo.recycle();
    weatherReportRepo.recycle();
    weatherForecastRepo.recycle();
    // reset(weatherService);
    reset(serverPropertiesService);

    // Set the current instant to a time when we are requesting data
    Competition comp = Competition.newInstance("WeatherService test");
    start = new DateTime(2009, 4, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    next = start.plus(comp.getTimeslotDuration());
    timeService.setCurrentTime(start);
    if (timeslotRepo.currentTimeslot() == null) {
      for (int i = 0; i < 48; i++) {
        timeslotRepo.makeTimeslot(start.plus(comp.getTimeslotDuration() * i));
      }
    }
    
    // set up the weather service under test
    weatherService = new WeatherService();
    ReflectionTestUtils.setField(weatherService,
                                 "timeslotRepo", timeslotRepo);
    ReflectionTestUtils.setField(weatherService,
                                 "weatherReportRepo", weatherReportRepo);
    ReflectionTestUtils.setField(weatherService,
                                 "weatherForecastRepo", weatherForecastRepo);
    ReflectionTestUtils.setField(weatherService,
                                 "brokerProxyService", brokerProxyService);
    ReflectionTestUtils.setField(weatherService,
                                 "serverProps", serverPropertiesService);
    ReflectionTestUtils.setField(weatherService,
                                 "competitionControlService", competitionControlService);

    comp = Competition.newInstance("weather-test");
    
    // Set up serverProperties mock
    config = new Configurator();
    doAnswer(new Answer() {
      @Override
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(serverPropertiesService).configureMe(anyObject());

  }

  /*
   * private void initializeService () { String result =
   * weatherService.initialize(comp, new ArrayList<String>());
   * assertEquals("correct return", "WeatherService", result); }
   */
  // initialization without a configuration
  @Test
  public void testNormalInitialization ()
  {
    String properUrl = "http://tac06.cs.umn.edu:8080/"
                       + "powertac-weather-server/weatherSet/weatherRequest?"
                       + "id=0&setname=default&weather_days=1&weather_id=";
    String result = weatherService.initialize(comp, new ArrayList<String>());
    assertEquals("correct return value", "WeatherService", result);
    assertTrue("correct req interval",
               weatherService.getWeatherReqInterval() == 12);
    assertTrue("correct forecast horizon",
               weatherService.getForecastHorizon() == 24);
    assertTrue("correct blocking mode", weatherService.isBlocking() == true);
    assertTrue("corrent server url", weatherService.getServerUrl()
                                                   .compareTo(properUrl) == 0);
  }

  // config max/min
  @Test
  public void testConfigInitialization ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("server.weatherService.serverUrl", "localhost");
    map.put("server.weatherService.weatherReqInterval", "6");
    map.put("server.weatherService.blocking", "false");
    map.put("server.weatherService.forecastHorizon", "12");
    Configuration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);

    String result = weatherService.initialize(comp, new ArrayList<String>());
    assertEquals("correct return value", "WeatherService", result);
    assertEquals("correct url", 0,
                 "localhost".compareTo(weatherService.getServerUrl()), 1e-6);
    assertEquals("correct weather req interval", 6,
                 weatherService.getWeatherReqInterval(), 1e-6);
    assertEquals("correct blocking mode", false, weatherService.isBlocking());
    assertEquals("correct forecast horizon", 12,
                 weatherService.getForecastHorizon());
  }

  @Test
  public void dataFetchTest ()
  {

    // Sanity check on autowire
    assertNotNull(timeslotRepo);
    assertNotNull(weatherReportRepo);
    assertNotNull(weatherForecastRepo);
    assertNotNull(weatherService);

    // Check that there are indeed 24 timeslots enabled
    assertEquals(48, timeslotRepo.enabledTimeslots().size());

    weatherService.activate(start, 1);

    // Check that 24 weather reports entered the repo
    assertEquals(24, weatherReportRepo.count());

    // Check that 24 weather forecast enterd the repo
    assertEquals(24, weatherForecastRepo.count());

  }

  @Test
  public void currentTimeDataTest ()
  {
    weatherService.activate(start, 1);

    assertEquals(24, weatherReportRepo.count());

    // Check to see that the weatherReportRepo only gives the current timeslot
    // weather report
    assertEquals(start, weatherReportRepo.currentWeatherReport()
                                         .getCurrentTimeslot()
                                         .getStartInstant());
    assertEquals(timeslotRepo.currentTimeslot(),
                 weatherReportRepo.currentWeatherReport().getCurrentTimeslot());

    // Check to see that the next timeslot is as expected

    timeService.setCurrentTime(next);
    assertEquals(next, weatherReportRepo.currentWeatherReport()
                                        .getCurrentTimeslot().getStartInstant());
    assertEquals(timeslotRepo.currentTimeslot(),
                 weatherReportRepo.currentWeatherReport().getCurrentTimeslot());

    // Check that we can read backwards only 2 timeslots (current + previous)
    assertEquals(2, weatherReportRepo.allWeatherReports().size());

    // Check that the 2 timeslots are different in the repo
    assertEquals(false,
                 weatherReportRepo.allWeatherReports().get(0)
                                  .getCurrentTimeslot() == weatherReportRepo.allWeatherReports()
                                                                            .get(1)
                                                                            .getCurrentTimeslot());
    // System.out.println(weatherReportRepo.allWeatherReports().get(0).getCurrentTimeslot());
    // System.out.println(weatherReportRepo.allWeatherReports().get(1).getCurrentTimeslot());
  }

  @Test
  public void currentForecastTest ()
  {
    weatherService.activate(start, 1);
    // Check that there is a forecast for the current timeslot
    timeService.setCurrentTime(next);
    assertNotNull(weatherForecastRepo.currentWeatherForecast());

    // Check that we can read backwards only 2 timeslots (current + previous)
    assertEquals(2, weatherForecastRepo.allWeatherForecasts().size());

  }

  @Test
  public void testReportValues ()
  {
    weatherService.activate(start, 1);

    WeatherReport wr = weatherReportRepo.allWeatherReports().get(0);
    System.out.println(wr.getCurrentTimeslot());
    assertEquals(17.2, wr.getTemperature(), .0001);
    assertEquals(4.6, wr.getWindSpeed(), .0001);
    assertEquals(150, wr.getWindDirection(), .0001);
    assertEquals(0.0, wr.getCloudCover(), .0001);

    // Test that currentWeatherId increments correctly
    Instant reqTime = timeslotRepo.enabledTimeslots().get(24).getStartInstant();
    assertNotNull(reqTime);
    System.out.println(timeslotRepo.currentTimeslot());
    assertEquals(24, weatherReportRepo.count());

    timeService.setCurrentTime(reqTime);
    weatherService.activate(reqTime, 1);
    // Check that 48 weather reports entered the repo
    assertEquals(48, weatherReportRepo.count());

    // Check that 48 weather forecast enterd the repo
    assertEquals(48, weatherForecastRepo.count());

    System.out.println(timeslotRepo.currentTimeslot());
    System.out.println(weatherReportRepo.currentWeatherReport()
                                        .getCurrentTimeslot());

    assertEquals(48, weatherReportRepo.count());
    // Check beginning weather
    assertEquals(false,
                 wr.getTemperature() == weatherReportRepo.allWeatherReports()
                                                         .get(24)
                                                         .getTemperature());

    // Check ending weather
    timeService.setCurrentTime(timeslotRepo.enabledTimeslots().get(47)
                                           .getStartInstant());
    assertEquals(false,
                 weatherReportRepo.allWeatherReports().get(23).getTemperature() == weatherReportRepo.allWeatherReports()
                                                                                                    .get(47)
                                                                                                    .getTemperature());
    assertEquals(false, wr.getId() == weatherReportRepo.allWeatherReports()
                                                       .get(24).getId());

  }

  @Test
  public void testForecastValues ()
  {
    weatherService.activate(start, 1);

    // There should be 24 predictions in the forecast
    assertEquals(24, weatherForecastRepo.currentWeatherForecast()
                                        .getPredictions().size());

    // Predictions should increment by one each time
    int i = 1;
    for (WeatherForecastPrediction p : weatherForecastRepo.currentWeatherForecast()
                                                          .getPredictions()) {
      assertEquals(i, p.getForecastTime());
      i++;
    }
  }

}
