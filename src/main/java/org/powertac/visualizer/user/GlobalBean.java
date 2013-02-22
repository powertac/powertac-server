package org.powertac.visualizer.user;

import java.io.Serializable;

import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.push.GlobalPusher;
import org.powertac.visualizer.push.NominationPusher;
import org.powertac.visualizer.push.WeatherPusher;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class GlobalBean implements Serializable {
	
	private GlobalPusher globalPusher;
	
	@Autowired
	public GlobalBean(VisualizerBean visualizerBean) {
		globalPusher = new GlobalPusher(visualizerBean.getWeatherPusher(),visualizerBean.getNominationPusher());
	}
	
	public String getGlobalPusher() {
		return new Gson().toJson(globalPusher);
	}
	
}
