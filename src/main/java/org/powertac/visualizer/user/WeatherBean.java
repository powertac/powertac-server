package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.powertac.common.WeatherReport;
import org.powertac.visualizer.services.WeatherInfoService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class WeatherBean implements Serializable {

	private String cloudCoverData;
	private String windSpeedData;
	private String temperatureData;

	@Autowired
	public WeatherBean(WeatherInfoService service) {
		
		Gson gson = new Gson();

		ArrayList<Object> clouds = new ArrayList<Object>();
		ArrayList<Object> winds = new ArrayList<Object>();
		ArrayList<Object> temps = new ArrayList<Object>();

		Set<Long> keys = new TreeSet<Long>(service.getReports().keySet());
		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
			long key = (Long) iterator.next();
			WeatherReport report = service.getReports().get(key);

			double[] cloud = { key, report.getCloudCover() };
			double[] wind = { key, report.getWindSpeed() };
			double[] temp = { key, report.getTemperature() };

			clouds.add(cloud);
			winds.add(wind);
			temps.add(temp);
		}
		cloudCoverData = gson.toJson(clouds);
		windSpeedData = gson.toJson(winds);
		temperatureData = gson.toJson(temps);
	}
	
	public String getCloudCoverData() {
		return cloudCoverData;
	}
	public String getTemperatureData() {
		return temperatureData;
	}
	public String getWindSpeedData() {
		return windSpeedData;
	}
}
