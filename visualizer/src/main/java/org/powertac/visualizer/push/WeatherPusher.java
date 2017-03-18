package org.powertac.visualizer.push;

public class WeatherPusher
{

  private long millis;
  private double temperature;
  private double windSpeed;
  private double windDirection;
  private double cloudCover;
  private int timeslot;

  public WeatherPusher (long millis, double temperature, double windSpeed,
                        double windDirection, double cloudCover, int timeslot)
  {
    this.millis = millis;
    this.temperature = temperature;
    this.windSpeed = windSpeed;
    this.windDirection = windDirection;
    this.cloudCover = cloudCover;
    this.timeslot = timeslot;
  }

  public long getMillis ()
  {
    return millis;
  }

  public double getTemperature ()
  {
    return temperature;
  }

  public double getWindSpeed ()
  {
    return windSpeed;
  }

  public double getWindDirection ()
  {
    return windDirection;
  }

  public double getCloudCover ()
  {
    return cloudCover;
  }

  public int getTimeslot ()
  {
    return timeslot;
  }

}
