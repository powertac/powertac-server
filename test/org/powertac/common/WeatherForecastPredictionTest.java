package org.powertac.common;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringWriter;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class WeatherForecastPredictionTest {

	
	@BeforeClass
	public static void setupLog() {
		PropertyConfigurator.configure("test/log.config");
	}

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void timeslotTest(){
		WeatherForecastPrediction wr = new WeatherForecastPrediction(1, 1.0, 2.0, 3.0, 4.0);
		assertEquals(1,wr.getForecastTime());
	}
	
	@Test
	public void tempTest(){
		WeatherForecastPrediction wr = new WeatherForecastPrediction(1, 1, 2, 3, 4);
		assertEquals(1,wr.getTemperature(),.0001);	
	}
	
	@Test
	public void windspeedTest(){
		WeatherForecastPrediction wr = new WeatherForecastPrediction(1, 1, 2, 3, 4);
		assertEquals(2,wr.getWindSpeed(),.0001);	
	}
	
	@Test
	public void winddirTest(){
		WeatherForecastPrediction wr = new WeatherForecastPrediction(1, 1, 2, 3, 4);
		assertEquals(3,wr.getWindDirection(),.0001);	
	}
	
	@Test
	public void cloudCoverTest(){
		WeatherForecastPrediction wr = new WeatherForecastPrediction(1, 1, 2, 3, 4);
		assertEquals(4,wr.getCloudCover(),.0001);	
	}

	@Test
	public void xmlSerializationTest() {
		
		WeatherForecastPrediction w1 = new WeatherForecastPrediction(1, 1, 2, 3, 4);
		
		XStream xstream = new XStream();
		xstream.processAnnotations(Timeslot.class);
		StringWriter serialized = new StringWriter();
		serialized.write(xstream.toXML(w1));
		// System.out.println(serialized.toString());
		WeatherForecastPrediction xw1 = (WeatherForecastPrediction) xstream.fromXML(serialized.toString());
		assertNotNull("deserialized something", xw1);
		assertEquals("correct timeslot", 1, xw1.getForecastTime());
		assertEquals("correct temp", 1, xw1.getTemperature(),.0001);
		assertEquals("correct windspeed", 2, xw1.getWindSpeed(),.0001);
		assertEquals("correct winddir", 3, xw1.getWindDirection(),.0001);
		assertEquals("correct cloudcover", 4, xw1.getCloudCover(),.0001);
	}

}
