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

import java.util.List;

import org.powertac.common.xml.TimeslotConverter;
import com.thoughtworks.xstream.annotations.*;

/**
* A collection of weatherReports giving hourly forecasts for future timeslot.
*
* @author Erik Onarheim and Josh Edeen
*/
@XStreamAlias("weather-forecast")
public class WeatherForecast 
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  
  /** the current or reference timeslot from which the weather (forecast) is generated */
  @XStreamAsAttribute
  @XStreamConverter(TimeslotConverter.class)
  private Timeslot currentTimeslot;

  @XStreamImplicit(itemFieldName = "prediction")
  private List<WeatherForecastPrediction> predictions;
  
  public WeatherForecast (Timeslot timeslot)
  {
    super();
    this.currentTimeslot = timeslot;
  }

  public List<WeatherForecastPrediction> getPredictions ()
  {
    return predictions;
  }

  public WeatherForecast setPredictions (List<WeatherForecastPrediction> predictions)
  {
    this.predictions = predictions;
    return this;
  }
  
  public WeatherForecast addPrediction (WeatherForecastPrediction prediction)
  {
    this.predictions.add(prediction);
    return this;
  }

  public long getId ()
  {
    return id;
  }

  public Timeslot getCurrentTimeslot ()
  {
    return currentTimeslot;
  }
  
  
}
