package org.powertac.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringWriter;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class WeatherReportTest {

	Timeslot ts1;

	@Before
	public void setUp() throws Exception {
	    Competition.newInstance("test");
		Instant baseTime = new DateTime().toInstant();
		ts1 = new Timeslot(1, baseTime);
	}
	
	@Test
	public void timeslotTest(){
		WeatherReport wr = new WeatherReport(ts1, 1, 2, 3, 4);
		assertEquals(ts1.getSerialNumber(),wr.getTimeslotIndex());
	}
	
	@Test
	public void tempTest(){
		WeatherReport wr = new WeatherReport(ts1, 1, 2, 3, 4);
		assertEquals(1,wr.getTemperature(),.0001);	
	}
	
	@Test
	public void windspeedTest(){
		WeatherReport wr = new WeatherReport(ts1, 1, 2, 3, 4);
		assertEquals(2,wr.getWindSpeed(),.0001);	
	}
	
	@Test
	public void winddirTest(){
		WeatherReport wr = new WeatherReport(ts1, 1, 2, 3, 4);
		assertEquals(3,wr.getWindDirection(),.0001);	
	}
	
	@Test
	public void cloudCoverTest(){
		WeatherReport wr = new WeatherReport(ts1, 1, 2, 3, 4);
		assertEquals(4,wr.getCloudCover(),.0001);	
	}

	@Test
	public void xmlSerializationTest() {
		
		WeatherReport w1 = new WeatherReport(42, 1, 2, 3, 4);
		
		XStream xstream = new XStream();
		StringWriter serialized = new StringWriter();
		serialized.write(xstream.toXML(w1));
		// System.out.println(serialized.toString());
		WeatherReport xw1 = (WeatherReport) xstream.fromXML(serialized.toString());
		assertNotNull("deserialized something", xw1);
		assertEquals("correct timeslot", 42, xw1.getTimeslotIndex());
		assertEquals("correct temp", 1, xw1.getTemperature(),.0001);
		assertEquals("correct windspeed", 2, xw1.getWindSpeed(),.0001);
		assertEquals("correct winddir", 3, xw1.getWindDirection(),.0001);
		assertEquals("correct cloudcover", 4, xw1.getCloudCover(),.0001);
	}

}
