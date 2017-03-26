package org.powertac.visualizer.display;

import java.util.ArrayList;

/**
 * A template class which resembles the structure needed for HighStock charts
 * (JSON library).
 * 
 * @author Jurica Babic
 * 
 */
public class DrillDownTemplate2 {

	private String name;
	private long y;
	//private String color;
	private Object drilldown;

	public DrillDownTemplate2(String name, long y, Object drilldown) {
		this.name = name;
		//this.color = color;
		this.drilldown = drilldown;
		this.y = y;
	}

}
