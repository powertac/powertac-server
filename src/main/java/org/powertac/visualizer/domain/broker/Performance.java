package org.powertac.visualizer.domain.broker;

import java.util.ArrayList;

import org.powertac.visualizer.json.PerformanceJSON;

/**
 * This abstract class is used as a template to build more detailed performance category for a broker.
 * 
 * @author Jurica Babic
 *
 */
public abstract class Performance {
	Enum<Grade> grade;
	PerformanceJSON performanceJson;
	
	public Performance() {
		performanceJson = new PerformanceJSON();
	}
	
	public Enum<Grade> getGrade() {
		return grade;
	}
	public PerformanceJSON getPerformanceJson() {
		return performanceJson;
	}
}
