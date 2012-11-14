package org.powertac.visualizer.json;

import org.mortbay.util.ajax.JSON;
import org.primefaces.json.JSONArray;

/**
 * @author Jurica Babic
 *
 */
public class PerformanceJSON {
	public JSONArray historyGrades;
	
	public PerformanceJSON() {
		historyGrades = new JSONArray();
	}
	
	
}
