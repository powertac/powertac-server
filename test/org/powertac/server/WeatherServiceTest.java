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
import org.powertac.common.repo.TimeslotRepo;
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

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		PropertyConfigurator.configure("test/logger.config");
	    Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	@Before
	public void setUp() throws Exception {
		
	}
	
	@Test
	public void test(){
		assertEquals("sanity check", true,true);
		assertNotNull(timeslotRepo);
		assertNotNull(weatherService);
		Instant i1 = (new Instant()).withMillis(0);
		timeService.setCurrentTime(i1);
		for(int i = 0; i < 24; i++){
			timeslotRepo.makeTimeslot((new Instant().withMillis(i)), (new Instant()).withMillis(i+1));
			
		}
		assertEquals(24, timeslotRepo.enabledTimeslots().size());
		//Set the current instant to a time when we are requesting data
		
		weatherService.activate(i1, 1);
	}

}
