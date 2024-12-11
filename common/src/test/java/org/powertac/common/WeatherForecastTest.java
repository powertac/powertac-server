package org.powertac.common;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import java.time.ZonedDateTime;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.thoughtworks.xstream.XStream;

public class WeatherForecastTest 
{

	@Autowired
	TimeService timeService;
	
	Timeslot ts1;
	int ts1Num;
	List<WeatherForecastPrediction> tlist;

	@BeforeEach
	public void setUp() throws Exception
	{
	    Competition.newInstance("test");
		Instant baseTime = ZonedDateTime.now().toInstant();
		tlist = new ArrayList<WeatherForecastPrediction>();
		ts1 = new Timeslot(1, baseTime);
		ts1Num = ts1.getSerialNumber();
		tlist.add(new WeatherForecastPrediction(0, 0, 0, 0, 0));
	}
	
	@Test
	public void timeslotTest(){
		WeatherForecast wr = new WeatherForecast(ts1Num, tlist);
		assertEquals(ts1.getSerialNumber(),wr.getTimeslotIndex());
	}
	
	@Test
	public void tempTest(){
		WeatherForecast wr = new WeatherForecast(ts1Num, tlist);
		assertEquals(1,wr.getPredictions().size());	
	}
	

	@Test
	public void xmlSerializationTest() {
		
		WeatherForecast w1 = new WeatherForecast(42, tlist);

		XStream xstream = XMLMessageConverter.getXStream();
		StringWriter serialized = new StringWriter();
		serialized.write(xstream.toXML(w1));
		// System.out.println(serialized.toString());
		WeatherForecast xw1 = (WeatherForecast) xstream.fromXML(serialized.toString());
		assertNotNull(xw1, "deserialized something");
		assertEquals(42, xw1.getTimeslotIndex(), "correct timeslot");
		assertEquals(tlist.get(0).getForecastTime(), xw1.getPredictions().get(0).getForecastTime(), "correct list");
	}

}
