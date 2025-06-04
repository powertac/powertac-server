package org.powertac.common;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;

import java.time.ZonedDateTime;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thoughtworks.xstream.XStream;

public class WeatherReportTest {

	Timeslot ts1;
	int ts1Num;

	@BeforeEach
	public void setUp() throws Exception {
	    Competition.newInstance("test");
		Instant baseTime = ZonedDateTime.now().toInstant();
		ts1 = new Timeslot(1, baseTime);
		ts1Num = ts1.getSerialNumber();
	}
	
	@Test
	public void timeslotTest(){
		WeatherReport wr = new WeatherReport(ts1Num, 1, 2, 3, 4);
		assertEquals(ts1.getSerialNumber(),wr.getTimeslotIndex());
	}
	
	@Test
	public void tempTest(){
		WeatherReport wr = new WeatherReport(ts1Num, 1, 2, 3, 4);
		assertEquals(1,wr.getTemperature(),.0001);	
	}
	
	@Test
	public void windspeedTest(){
		WeatherReport wr = new WeatherReport(ts1Num, 1, 2, 3, 4);
		assertEquals(2,wr.getWindSpeed(),.0001);	
	}
	
	@Test
	public void winddirTest(){
		WeatherReport wr = new WeatherReport(ts1Num, 1, 2, 3, 4);
		assertEquals(3,wr.getWindDirection(),.0001);	
	}
	
	@Test
	public void cloudCoverTest(){
		WeatherReport wr = new WeatherReport(ts1Num, 1, 2, 3, 4);
		assertEquals(4,wr.getCloudCover(),.0001);	
	}

	@Test
	public void xmlSerializationTest() {
		
		WeatherReport w1 = new WeatherReport(42, 1, 2, 3, 4);

		XStream xstream = XMLMessageConverter.getXStream();
		StringWriter serialized = new StringWriter();
		serialized.write(xstream.toXML(w1));
		// System.out.println(serialized.toString());
		WeatherReport xw1 = (WeatherReport) xstream.fromXML(serialized.toString());
		assertNotNull(xw1, "deserialized something");
		assertEquals(42, xw1.getTimeslotIndex(), "correct timeslot");
		assertEquals(1, xw1.getTemperature(),.0001, "correct temp");
		assertEquals(2, xw1.getWindSpeed(),.0001, "correct windspeed");
		assertEquals(3, xw1.getWindDirection(),.0001, "correct winddir");
		assertEquals(4, xw1.getCloudCover(),.0001, "correct cloudcover");
	}

}
