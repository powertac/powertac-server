/*
 * Copyright (c) 2011-2013 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.common.repo;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.WeatherReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.powertac.common.exceptions.PowerTacException;

/**
 * Repository for WeatherReports. The weather reports are indexed by the
 * timeslot that they are issued for. This allows them to be quickly accessed
 * via a hashMap. The parameter maxReportCount controls the number of
 * weather reports kept in memory.
 * 
 * @author Erik Onarheim
 */
@Service
public class WeatherReportRepo implements DomainRepo
{
  static private Logger log = LogManager.getLogger(WeatherReportRepo.class
          .getName());

  // storage
  private Map<Integer, WeatherReport> indexedWeatherReports;
  private int maxReportCount = 168 * 2 + 48; // enough for a boot record

  // Check if the weather service has run at least once
  private boolean hasRunOnce = false;

  @Autowired
  private TimeslotRepo timeslotRepo;

  /** standard constructor */
  public WeatherReportRepo ()
  {
    super();
    indexedWeatherReports =
        new ConcurrentHashMap<Integer, WeatherReport>(2000, 0.9f, 1);
  }

  /**
   * Adds a WeatherReport to the repo, keeping only maxReportCount around
   */
  public void add (WeatherReport weather)
  {
    runOnce();
    int index = weather.getTimeslotIndex();
    indexedWeatherReports.put(index, weather);
    indexedWeatherReports.remove(index - maxReportCount);
  }

  /**
   * Returns the current weatherReport
   */
  public WeatherReport currentWeatherReport () throws PowerTacException
  {
    if (!hasRunOnce) {
      log.error("Weather Service has yet to run, cannot retrieve report");
      throw new PowerTacException(
                                  "Attempt to retrieve report before data available");
    }
    // Returns the weather report for the current timeslot
    return indexedWeatherReports.get(timeslotRepo.currentSerialNumber());
  }

  /**
   * Returns a list of all the issued weather reports up to the
   * currentTimeslot
   */
  public List<WeatherReport> allWeatherReports ()
  {
    Integer current = timeslotRepo.currentSerialNumber();
    // Some weather reports exist in the repo for the future
    // but have not been issued for the current timeslot.
    ArrayList<WeatherReport> issuedReports = new ArrayList<WeatherReport>();
    for (WeatherReport w: indexedWeatherReports.values()) {
      if (w.getTimeslotIndex() < current) {
        issuedReports.add(w);
      }
    }
    issuedReports.add(this.currentWeatherReport());

    return (List<WeatherReport>) issuedReports;
  }

  /**
   * Returns the number of weatherReports that have been successfully added.
   */
  public int count ()
  {
    return indexedWeatherReports.size();
  }

  /**
   * Called by weather service to indicate weather exists
   */
  public void runOnce ()
  {
    hasRunOnce = true;
  }

  @Override
  public void recycle ()
  {
    hasRunOnce = false;
    indexedWeatherReports.clear();
  }
}
