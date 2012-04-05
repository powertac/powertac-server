package org.powertac.visualizer.interfaces;

import org.powertac.visualizer.domain.Appearance;
import org.powertac.visualizer.domain.broker.DayState;

public interface DisplayableBroker {
	
	public String getName();
	
	public String getId();
	
	public Appearance getAppearance();
	
	public DayState getDisplayableDayState();

}
