package org.powertac.common;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.thoughtworks.xstream.XStream;

public class WeatherForecastTest 
{

	@Autowired
	TimeService timeService;
	
	Timeslot ts1;
	List<WeatherForecastPrediction> tlist;

	@Before
	public void setUp() throws Exception
	{
	    Competition.newInstance("test");
		Instant baseTime = new DateTime().toInstant();
		tlist = new ArrayList<WeatherForecastPrediction>();
		ts1 = new Timeslot(1, baseTime, null);
		tlist.add(new WeatherForecastPrediction(0, 0, 0, 0, 0));
	}
	
	@Test
	public void timeslotTest(){
		WeatherForecast wr = new WeatherForecast(ts1, tlist);
		assertEquals(ts1,wr.getCurrentTimeslot());
	}
	
	@Test
	public void tempTest(){
		WeatherForecast wr = new WeatherForecast(ts1, tlist);
		assertEquals(1,wr.getPredictions().size());	
	}
	

	@Test
	public void xmlSerializationTest() {
		
		WeatherForecast w1 = new WeatherForecast(null, tlist);
		
		XStream xstream = new XStream();
		xstream.processAnnotations(Timeslot.class);
		StringWriter serialized = new StringWriter();
		serialized.write(xstream.toXML(w1));
		// System.out.println(serialized.toString());
		WeatherForecast xw1 = (WeatherForecast) xstream.fromXML(serialized.toString());
		assertNotNull("deserialized something", xw1);
		assertEquals("correct timeslot", null, xw1.getCurrentTimeslot());
		assertEquals("correct list", tlist.get(0).getForecastTime(), xw1.getPredictions().get(0).getForecastTime());
	}

}
