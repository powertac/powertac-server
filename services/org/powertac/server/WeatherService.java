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
package org.powertac.server;

/**
 *  
 * @author Erik Onarheim
 */

// Import network java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.List;



// Import common
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;
import org.powertac.common.WeatherForecast;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Scope("singleton")
@Service
public class WeatherService implements InitializationService, TimeslotPhaseProcessor{ 
	
    private int weatherReqInterval = 12;
	private int daysOut = 1;
	private int currentWeatherId = 1;
	private String serverUrl =  "http://tac05.cs.umn.edu:8080/powertac-weather-server/weatherSet/weatherRequest?id=1&setname=default&weather_day=1&weather_id";
	private boolean requestFailed = false;
	
	
	// TODO auto wire definitions
	//def brokerProxyService
	//def timeService
	//def competitionControlService
	// Temp services
	
	@Autowired
	private TimeService timeService;
	
	@Autowired
	private CompetitionControlService competitionControlService;
	
	@Autowired
	private WeatherReportRepo weatherReportRepo;
	
	@Autowired
	private WeatherForecastRepo weatherForecastRepo;
	
	
	public WeatherService(){
		super();
	}
	
	public void init(){
		competitionControlService.registerTimeslotPhase(this, 3);
	}
	
	// Make actual web request to the weather-server
	public void activate(Instant time, int phaseNumber){
		// Error check the request interval
		if (weatherReqInterval > 24) {
				// log.error("weather request interval ${weatherRequestInterval} > 24 hr"
				weatherReqInterval = 24;
		}
		
		long msec = timeService.getCurrentTime().getMillis();
		
		if (msec % (weatherReqInterval * TimeService.HOUR) == 0) {
			
			// time to publish
			//log.info "Requesting Weather from " + serverUrl + "=" + currentWeatherId + " at time: " + time
			
			// Attempt to make a web request to the weather server
			try {
				// Need try/catch for invalid host strings
				
				//currentWeatherId+=(2*weatherRequestInterval) // 2 weather reports per hour
				webRequest(1,1); // TODO: Should be fixed to int
				requestFailed = false;
				
			}catch(Throwable e){
				//log.error "Unable to connect to host: " + serverUrl
				requestFailed = true;
				
			}	
		  }
		
	}
	
	// Forecasts are random and must be repeatable from the same seed
	private boolean webRequest(int time, int randomSeed) {
	    int currentTime = time;
	    boolean readingForecast = false;
	    
		try {
			// Create a URLConnection object for a URL and send request
			URL url = new URL(serverUrl+"="+currentWeatherId); // TODO: Needs to be fixed for tac06
			URLConnection conn = url.openConnection();
				
		    // Get the response
		    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		    String tmpLine;
		    while ((tmpLine = input.readLine()) != null) {
		        // Parse weather reports here
		    	if(tmpLine == "---Forecast Data---"){
		    		readingForecast = true;
		    	}
		    	
		    	// Remove brackets from response
		    	tmpLine.replace("[","");
		    	tmpLine.replace("]","");
		    	
		    	
		    	String[] weatherValue;
		    	
		    	if(!readingForecast){
		    		//Reading report values
		    		
		    		weatherValue = tmpLine.split(", ");
		    		for(int i = 0; i< weatherValue.length;i++){
		    			weatherValue[i] = weatherValue[i].split(":")[1].trim();
		    		}    		
		    		
		    		//TODO Access weatherReport Repo Here and write in current report
		    		WeatherReport newReport = new WeatherReport(
		    				Timeslot timeslot,
		    				weatherValue[0],//double temperature,
	                        weatherValue[1],//double windSpeed,
	                        weatherValue[2],//double windDirection,
	                        weatherValue[3]);//double cloudCover
		    		weatherReportRepo.add(newReport);
		    		
		    	}else{
		    		//TODO Reading forecast values
		    		
		    		
		    		//TODO Access weatherForecast Repo Here and write current forcast
		    		
		    	}	    	
		    }
		    
		    input.close();
		    return true;
		    
		} catch (Exception e){
			return false;
		}
	}

	public void setDefaults() {
		// None		
	}

	public String initialize(Competition competition,
			List<String> completedInits) {
		this.init();
		return null;
	}

}
