package org.powertac.visualizer.user;

import java.io.Serializable;

public class SelectedBrokerChartsBean implements Serializable {

	
	
	private static final long serialVersionUID = 3310169029425905901L;
	private boolean dailyValues;
	private boolean dailyAVGValues;
	private boolean allTimeslots = true;
	private boolean currentDayValues = true; //default true, display currentDayValues;
	
	public boolean isDailyValues() {
		return dailyValues;
	}
	public void setDailyValues(boolean dailyValues) {
		this.dailyValues = dailyValues;
	}
	public boolean isDailyAVGValues() {
		return dailyAVGValues;
	}
	public void setDailyAVGValues(boolean dailyAVGValues) {
		this.dailyAVGValues = dailyAVGValues;
	}
	public boolean isAllTimeslots() {
		return allTimeslots;
	}
	public void setAllTimeslots(boolean allTimeslots) {
		this.allTimeslots = allTimeslots;
	}
	public boolean isCurrentDayValues() {
		return currentDayValues;
	}
	public void setCurrentDayValues(boolean currentDayValues) {
		this.currentDayValues = currentDayValues;
	}
	
	

}
