package org.powertac.visualizer.domain;

import java.util.ArrayList;
import java.util.List;

import org.powertac.visualizer.domain.broker.DayState;

/**
 * Represents one-day overview of the competition.
 * @author Jurek
 *
 */
public class DayOverview {
	
	//collection contains daily state for each broker:
	private List<DayState> dayStates;
	private int day;
		
	public DayOverview(List<DayState> dayStates, int day) {
		this.dayStates=dayStates;
		this.day=day;
	}
	public List<DayState> getDayStates() {
		return dayStates;
	}
	public int getDay() {
		return day;
	}

}
