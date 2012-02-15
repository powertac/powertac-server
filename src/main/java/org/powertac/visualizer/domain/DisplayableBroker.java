package org.powertac.visualizer.domain;

public interface DisplayableBroker {
	
	public String getName();
	
	public String getId();
	
	public Appearance getAppearance();
	
	public DayState getDisplayableDayState();

}
