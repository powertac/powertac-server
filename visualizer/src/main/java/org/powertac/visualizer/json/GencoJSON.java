package org.powertac.visualizer.json;

import org.primefaces.json.JSONArray;

public class GencoJSON {
	private JSONArray cashPositions;
	
	public GencoJSON() {
	cashPositions = new JSONArray();
	}
	
	public JSONArray getCashPositions() {
		return cashPositions;
	}

}
