package org.powertac.server;


import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/cc-config.xml"})
public class WeatherServiceTest {
	@Autowired
	private WeatherService weatherService;
	
	@Autowired
	private TimeslotRepo timeslotRepo;
	
	@Autowired
	private TimeService timeService;
	
	@Autowired
	private WeatherReportRepo weatherReportRepo;
	
	@Autowired
	private WeatherForecastRepo weatherForecastRepo;
	
	
	Instant i1;
	Instant next;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		PropertyConfigurator.configure("test/logger.config");
	    Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	@Before
	public void setUp() throws Exception {
		//Set the current instant to a time when we are requesting data
		i1 = (new Instant()).withMillis(0);
		next = i1.withMillis(1);
		
		timeService.setCurrentTime(i1);
		for(int i = 0; i < 24; i++){
			timeslotRepo.makeTimeslot((new Instant().withMillis(i)), (new Instant()).withMillis(i+1));
			
		}
	}
	
	@Test
	public void dataFetchTest(){
		
		// Sanity check on autowire
		assertNotNull(timeslotRepo);
		assertNotNull(weatherReportRepo);
		assertNotNull(weatherForecastRepo);
		assertNotNull(weatherService);
		
		
		// Check that there are indeed 24 timeslots enabled
		assertEquals(24, timeslotRepo.enabledTimeslots().size());
				
		
		weatherService.activate(i1, 1);
		
		// Check that 24 weather reports entered the repo
		assertEquals(24, weatherReportRepo.count());
		
		// Check that 24 weather forecast enterd the repo
		assertEquals(24, weatherForecastRepo.count());
		
	}
	
	@Test
	public void currentTimeDataTest(){
		assertEquals(24, weatherReportRepo.count());
		
		//Check to see that the weatherReportRepo only gives the current timeslot weather report
		assertEquals(i1, weatherReportRepo.currentWeatherReport().getCurrentTimeslot().getStartInstant());
		assertEquals(timeslotRepo.currentTimeslot(), weatherReportRepo.currentWeatherReport().getCurrentTimeslot());
		
		//Check to see that the next timeslot is as expected
		
		timeService.setCurrentTime(next);
		assertEquals(next, weatherReportRepo.currentWeatherReport().getCurrentTimeslot().getStartInstant());
		assertEquals(timeslotRepo.currentTimeslot(), weatherReportRepo.currentWeatherReport().getCurrentTimeslot());
		
		//Check that we can read backwards only 2 timeslots (current + previous)
		assertEquals(2, weatherReportRepo.allWeatherReports().size());
	}
	
	@Test
	public void currentForecastTest(){
		//Check that there is a forecast for the current timeslot
		timeService.setCurrentTime(next);
		assertNotNull(weatherForecastRepo.currentWeatherForecast());
		
		//Check that we can read backwards only 2 timeslots (current + previous)
		assertEquals(2, weatherForecastRepo.allWeatherForecasts().size());
		
	}
	
	@Test
	public void testReportValues(){
		timeService.setCurrentTime(i1);
		assertEquals(17.2, weatherReportRepo.currentWeatherReport().getTemperature(),.0001);
		assertEquals(4.6, weatherReportRepo.currentWeatherReport().getWindSpeed(),.0001);
		assertEquals(150, weatherReportRepo.currentWeatherReport().getWindDirection(),.0001);
		assertEquals(0.0, weatherReportRepo.currentWeatherReport().getCloudCover(),.0001);
		
	}
	
	@Test
	public void testForecastValues(){
		timeService.setCurrentTime(i1);
		// There should be 46 predictions in the forecast
		assertEquals(46, weatherForecastRepo.currentWeatherForecast().getPredictions().size());
		
		// Predictions should increment by one each time
		int i = 1;
		for(WeatherForecastPrediction p : weatherForecastRepo.currentWeatherForecast().getPredictions()){
			assertEquals(i,p.getForecastTime());
			i++;
		}
	}

}
