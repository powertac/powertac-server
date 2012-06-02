package org.powertac.visualizer.domain.broker;

import org.powertac.common.Rate;
import org.powertac.visualizer.json.RateInfoJSON;

public class RateInfo {
	
	private Rate rate;
	private RateInfoJSON json;
	
	public RateInfo(Rate rate) {
		this.rate=rate;
		json = new RateInfoJSON(rate);
	}
	
	public Rate getRate() {
		return rate;
	}
	public RateInfoJSON getJson() {
		return json;
	}
}
