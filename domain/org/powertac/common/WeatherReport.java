/*
* Copyright 2011 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an
* "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
* either express or implied. See the License for the specific language
* governing permissions and limitations under the License.
*/

package org.powertac.common;

import org.powertac.common.state.Domain;
import org.powertac.common.xml.TimeslotConverter;
import com.thoughtworks.xstream.annotations.*;

/**
* A weather report instance that describes the weather data for one hour of the
* simulation
*
* @author Erik Onarheim, Josh Edeen
*
* @version 1.0 - 03/May/2011
*/
@Domain
@XStreamAlias("weather-report")
public class WeatherReport
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  
  /** the current or reference timeslot from which the weather (forecast) is generated */
  @XStreamAsAttribute
  @XStreamConverter(TimeslotConverter.class)
  private Timeslot currentTimeslot;
  
  /** the current timeslot's temperature*/
  @XStreamAsAttribute
  private double temperature;
  
  /**  the current timeslot's windSpeed*/
  @XStreamAsAttribute
  private double windSpeed;
  
  /** the current timeslot's windDirection*/
  @XStreamAsAttribute
  private double windDirection;
  
  /** the current timeslot's cloudCover*/
  @XStreamAsAttribute
  private double cloudCover;

  public WeatherReport (Timeslot timeslot, double temperature,
                        double windSpeed, double windDirection,
                        double cloudCover)
  {
    this.currentTimeslot = timeslot;
    this.temperature = temperature;
    this.windSpeed = windSpeed;
    this.windDirection = windDirection;
    this.cloudCover = cloudCover;
  }

  public long getId ()
  {
    return id;
  }

  public Timeslot getCurrentTimeslot ()
  {
    return currentTimeslot;
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
  
}