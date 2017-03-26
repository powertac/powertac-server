package org.powertac.visualizer.json;

import org.primefaces.json.JSONArray;

public class DayStateJSON {
	
	private JSONArray dayCashBalancesJson;
	private JSONArray dayEnergyBalancesJson;
	public boolean isInitialized = false;
	
	public JSONArray getDayCashBalancesJson() {
		return dayCashBalancesJson;
	}

	public JSONArray getDayEnergyBalancesJson() {
		return dayEnergyBalancesJson;
	}
	
	public synchronized void addDayCashAndEnergyPoint(JSONArray cash, JSONArray energy){
		if(!isInitialized){
			lazyInit();
		}
		dayCashBalancesJson.put(cash);
		dayEnergyBalancesJson.put(energy);
		
	}
	
	
	private void lazyInit(){
		dayCashBalancesJson=new JSONArray();
		dayEnergyBalancesJson = new JSONArray();
		isInitialized=true;
	}
}
