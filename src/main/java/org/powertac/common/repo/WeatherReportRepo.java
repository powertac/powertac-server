/*
 * Copyright (c) 2011 by the original author
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
import java.util.HashMap;
import java.util.List;

//import org.apache.log4j.Logger;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.powertac.common.exceptions.PowerTacException;

/**
 * Repository for WeatherReports. The weather reports are indexed by the
 * timeslot that they are issued for. This allows them to be quickly accessed
 * via a hashMap.
 * 
 * @author Erik Onarheim
 */
@Repository
public class WeatherReportRepo implements DomainRepo {
	// static private Logger log =
	// Logger.getLogger(WeatherReportRepo.class.getName());

	// storage
	private HashMap<Timeslot, WeatherReport> indexedWeatherReports;

	// Check if the weather service has run at least once
	private boolean hasRunOnce = false;

	@Autowired
	private TimeslotRepo timeslotRepo;

	/** standard constructor */
	public WeatherReportRepo() {
		super();
		indexedWeatherReports = new HashMap<Timeslot, WeatherReport>();
	}

	/**
	 * Adds a WeatherReport to the repo
	 */
	public void add(WeatherReport weather) {
		runOnce();
		indexedWeatherReports.put(weather.getCurrentTimeslot(), weather);
	}

	/**
	 * Returns the current weatherReport
	 */
	public WeatherReport currentWeatherReport() throws PowerTacException {
		if (!hasRunOnce) {
			throw new PowerTacException();
		}
		// Returns the weather report for the current timeslot
		return indexedWeatherReports.get(timeslotRepo.currentTimeslot());
	}

	/**
	 * Returns a list of all the issued weather reports up to the
	 * currentTimeslot
	 */
	public List<WeatherReport> allWeatherReports() throws PowerTacException {
		try {
			Timeslot current = timeslotRepo.currentTimeslot();
			// Some weather reports exist in the repo for the future
			// but have not been issued for the current timeslot.
			ArrayList<WeatherReport> issuedReports = new ArrayList<WeatherReport>();
			for (WeatherReport w : indexedWeatherReports.values()) {
				if (w.getCurrentTimeslot().getStartInstant()
						.isBefore(current.getStartInstant())) {
					issuedReports.add(w);
				}
			}
			issuedReports.add(this.currentWeatherReport());

			return (List<WeatherReport>) issuedReports;
		} catch (PowerTacException p) {
			throw p;
		}
	}

	/**
	 * Returns the number of weatherReports that have been successfully added.
	 */
	public int count() {
		return indexedWeatherReports.size();
	}

	/**
	 * Called by weather service to indicate weather exists
	 */
	public void runOnce() {
		hasRunOnce = true;
	}

	public void recycle() {
		indexedWeatherReports.clear();
	}
}
