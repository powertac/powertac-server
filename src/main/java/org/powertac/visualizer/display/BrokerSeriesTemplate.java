package org.powertac.visualizer.display;

import java.util.ArrayList;

/**
 * A template class which resembles the structure needed for HighStock charts
 * (JSON library).
 * 
 * @author Jurica Babic
 * 
 */
public class BrokerSeriesTemplate {

	private String name;
	private String color;
	private ArrayList<Object> data;
	
	public BrokerSeriesTemplate(String name, String color, ArrayList<Object> data) {
		this.name = name;
		this.color = color;
		this.data = data;
	}
}
