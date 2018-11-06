package org.powertac.common;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thoughtworks.xstream.XStream;

public class WeatherForecastPredictionTest 
{

	@BeforeEach
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

		XStream xstream = XMLMessageConverter.getXStream();
		xstream.processAnnotations(Timeslot.class);
		StringWriter serialized = new StringWriter();
		serialized.write(xstream.toXML(w1));
		// System.out.println(serialized.toString());
		WeatherForecastPrediction xw1 = (WeatherForecastPrediction) xstream.fromXML(serialized.toString());
		assertNotNull(xw1, "deserialized something");
		assertEquals(1, xw1.getForecastTime(), "correct timeslot");
		assertEquals(1, xw1.getTemperature(),.0001, "correct temp");
		assertEquals(2, xw1.getWindSpeed(),.0001, "correct windspeed");
		assertEquals(3, xw1.getWindDirection(),.0001, "correct winddir");
		assertEquals(4, xw1.getCloudCover(),.0001, "correct cloudcover");
	}

}
