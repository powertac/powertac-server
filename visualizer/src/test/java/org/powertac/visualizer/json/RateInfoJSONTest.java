package org.powertac.visualizer.json;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Rate;

public class RateInfoJSONTest {
	
	private RateInfoJSON rateInfoJSON;

	@Before
	public void setUp(){}
	
	@Test
	public void testEveryDayEveryHour() {
		
		Rate rate = new Rate();
		rate.withValue(5);
		rateInfoJSON = new RateInfoJSON(rate);
	
	
		System.out.println("\nEvery day, every hour JSON: ");
		System.out.println("Max Values:\n"+rateInfoJSON.getRateLineChartMaxValue());
		System.out.println("Min Values:\n"+rateInfoJSON.getRateLineChartMinValue());
		
		assertEquals("Should be 7 entries for min chart", 7,rateInfoJSON.getRateLineChartMinValue().length());
		assertEquals("Should be 7 entries for max chart", 7,rateInfoJSON.getRateLineChartMinValue().length());
		
	}
	
	@Test
	public void testBeginDayGreaterThanEndDay() {
		
		
		
		rateInfoJSON = new RateInfoJSON(new Rate().withValue(5).withWeeklyBegin(5).withWeeklyEnd(4));
	
		
		
		System.out.println("\nBegin Day Greater Than End Day JSON: ");
		System.out.println("Max Values:\n"+rateInfoJSON.getRateLineChartMaxValue());
		System.out.println("Min Values:\n"+rateInfoJSON.getRateLineChartMinValue());
		
		assertEquals("Should be 7 entries for min chart", 7,rateInfoJSON.getRateLineChartMinValue().length());
		assertEquals("Should be 7 entries for max chart", 7,rateInfoJSON.getRateLineChartMinValue().length());
	}
	
	@Test
	public void testOneDay() {
		
		Rate rate = new Rate();
		rate.withValue(5);
		
		rate.withWeeklyBegin(5);
		rate.withWeeklyEnd(5);
		
		rateInfoJSON = new RateInfoJSON(rate);
	
		
		
		System.out.println("\nOne day JSON: ");
		System.out.println("Max Values:\n"+rateInfoJSON.getRateLineChartMaxValue());
		System.out.println("Min Values:\n"+rateInfoJSON.getRateLineChartMinValue());
		
		assertEquals("Should be 1 entry for min chart", 1,rateInfoJSON.getRateLineChartMinValue().length());
		assertEquals("Should be 5 entry for max chart", 1,rateInfoJSON.getRateLineChartMinValue().length());
	}
	
}
