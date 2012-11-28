package org.powertac.visualizer.services.handlers;

import org.apache.log4j.Logger;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherReport;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.services.WeatherInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class WeatherMessageHandler implements Initializable {

	private Logger log = Logger.getLogger(WeatherMessageHandler.class);
	@Autowired
	private WeatherInfoService service;

	@Autowired
	private MessageDispatcher router;
	
	public void handleMessage(WeatherReport weatherReport) {
		service.setCurrentReport(weatherReport);
	}

	public void handleMessage(WeatherForecast weatherForecast) {
		service.setCurrentForecast(weatherForecast);
	}

	public void initialize() {
		for (Class<?> clazz : Arrays.asList(WeatherForecast.class, WeatherReport.class)) {
			router.registerMessageHandler(this, clazz);
		}
	}

}
