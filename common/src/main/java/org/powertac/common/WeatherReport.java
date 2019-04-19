/*
* Copyright 2011-2013 the original author or authors.
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

import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.ChainedConstructor;
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
* A weather report instance that describes the weather data for one hour of the
* simulation
*
* @author Erik Onarheim, Josh Edeen
*
* @version 1.0 - 03/May/2011
*/
@Domain(fields = {"timeslot", "temperature", "windSpeed", "windDirection", "cloudCover"})
@XStreamAlias("weather-report")
public class WeatherReport
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  
  /** the current or reference timeslot from which the weather (forecast) is generated */
  @XStreamAsAttribute
  private int currentTimeslot;
  
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

  public WeatherReport (int timeslot, double temperature,
                        double windSpeed, double windDirection,
                        double cloudCover)
  {
    super();
    this.currentTimeslot = timeslot;
    this.temperature = temperature;
    this.windSpeed = windSpeed;
    this.windDirection = windDirection;
    this.cloudCover = cloudCover;
  }

  /**
   * Constructor that uses timeslot is deprecated
   */
  @Deprecated
  @ChainedConstructor
  public WeatherReport (Timeslot timeslot, double temperature,
                        double windSpeed, double windDirection,
                        double cloudCover)
  {
    this(timeslot.getSerialNumber(), temperature, windSpeed,
         windDirection, cloudCover);
  }

  public long getId ()
  {
    return id;
  }
  
  public int getTimeslotIndex ()
  {
    return currentTimeslot;
  }

  @Deprecated
  public Timeslot getCurrentTimeslot ()
  {
    return getTimeslotRepo().findBySerialNumber(currentTimeslot);
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
  
  // access to TimeslotRepo
  private static TimeslotRepo timeslotRepo;
  
  private static TimeslotRepo getTimeslotRepo()
  {
    if (null == timeslotRepo) {
      timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    }
    return timeslotRepo;
  }
}