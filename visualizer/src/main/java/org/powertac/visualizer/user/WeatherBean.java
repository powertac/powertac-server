package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.powertac.common.WeatherReport;
import org.powertac.visualizer.services.WeatherInfoService;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class WeatherBean implements Serializable {

	private String temperatureData;
	private String windSpeedData;
	private String windDirectionData;
	private String cloudCoverData;
	
	@Autowired
	public WeatherBean(WeatherInfoService service,VisualizerHelperService helper) {
		
		Gson gson = new Gson();

		
		ArrayList<Object> temps = new ArrayList<Object>();
		ArrayList<Object> windSpeeds = new ArrayList<Object>();
		ArrayList<Object> windDirections = new ArrayList<Object>();
		ArrayList<Object> clouds = new ArrayList<Object>();
		
 
		Set<Integer> keys = new TreeSet<Integer>(service.getReports().keySet());
		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
			int key = (Integer) iterator.next();
			WeatherReport report = service.getReports().get(key);
			long millis = helper.getMillisForIndex(key);

			double[] cloud = { millis, report.getCloudCover() };
			double[] windSpeed = { millis, report.getWindSpeed() };
			double[] windDirection = { millis, report.getWindDirection() };
			double[] temp = { millis, report.getTemperature() };

			clouds.add(cloud);
			windSpeeds.add(windSpeed);
			windDirections.add(windDirection);
			temps.add(temp);
		}
		if(keys.size()==0){
			//dummy:
			double[] dummy = { helper.getMillisForIndex(0), 0};
			clouds.add(dummy);
			windSpeeds.add(dummy);
			windDirections.add(dummy);
			temps.add(dummy);
		}
		cloudCoverData = gson.toJson(clouds);
		windSpeedData = gson.toJson(windSpeeds);
		temperatureData = gson.toJson(temps);
		windDirectionData = gson.toJson(windDirections);
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
	public String getWindDirectionData() {
		return windDirectionData;
	}
}
