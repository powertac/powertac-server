package org.powertac.visualizer.push;

public class GlobalPusher {

	private WeatherPusher weather;
	private NominationPusher nominations;

	public GlobalPusher(WeatherPusher weatherPusher,
			NominationPusher nominationPusher) {
		this.weather = weatherPusher;
		this.nominations = nominationPusher;
	}

	public NominationPusher getNominations() {
		return nominations;
	}

	public WeatherPusher getWeather() {
		return weather;
	}

}
