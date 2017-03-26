package org.powertac.visualizer.display;

import java.util.ArrayList;

public class GameOverviewTemplate {

	private String name;
	private ArrayList<Double> data;
	private String pointPlacement;

	public GameOverviewTemplate(String name, ArrayList<Double> data) {
		this.name = name;
		this.data = data;
		this.pointPlacement = "on";
	}

}
