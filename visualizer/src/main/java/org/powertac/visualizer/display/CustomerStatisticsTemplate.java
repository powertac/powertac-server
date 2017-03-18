package org.powertac.visualizer.display;

import java.util.ArrayList;

/**
 * A template class which resembles the structure needed for HighStock charts
 * (JSON library).
 * 
 * @author Jurica Babic
 * 
 */
public class CustomerStatisticsTemplate {

	private String name;
	private String color;
	private Object drilldown;
	private long y;

	public CustomerStatisticsTemplate(String name, String color, long y,
			Object drilldown) {
		this.y = y;
		this.name = name;
		this.color = color;
		this.drilldown = drilldown;
	}

}
