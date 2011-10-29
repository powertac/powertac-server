package org.powertac.server;


import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/cc-config.xml"})
public class WeatherServiceTest {
	@Autowired
	private WeatherService weatherService;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//PropertyConfigurator.configure("test/logger.config");
	    //Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	@Before
	public void setUp() throws Exception {
		
	}
	
	@Test
	public void test(){
		assertEquals("sanity check", true,true);
	}

}
