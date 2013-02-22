package org.powertac.visualizer.push;


public class WeatherPusher {

	private long millis;
	private double temperature;
	private double windSpeed;
	private double windDirection;
	private double cloudCover;

	public WeatherPusher(long millis, double temperature, double windSpeed,
			double windDirection, double cloudCover) {
		this.millis = millis;
		this.temperature = temperature;
		this.windSpeed = windSpeed;
		this.windDirection = windDirection;
		this.cloudCover = cloudCover;
	}

	public long getMillis() {
		return millis;
	}

	public double getTemperature() {
		return temperature;
	}

	public double getWindSpeed() {
		return windSpeed;
	}

	public double getWindDirection() {
		return windDirection;
	}

	public double getCloudCover() {
		return cloudCover;
	}
	
	

}
