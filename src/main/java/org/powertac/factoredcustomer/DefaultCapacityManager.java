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

package org.powertac.factoredcustomer;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.DataInputStream;
import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.*;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.factoredcustomer.CapacityProfile.BaseCapacityType;
import org.powertac.factoredcustomer.CapacityProfile.CapacityType;
import org.powertac.factoredcustomer.CapacityProfile.InfluenceKind;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Key class responsible for drawing from a base capacity and ajusting that 
 * capacity in response to various static and dynamic factors for each timeslot.
 * 
 * @author Prashant Reddy
 */
final class DefaultCapacityManager implements CapacityManager
{
    private static Logger log = Logger.getLogger(DefaultCapacityManager.class.getName());

    @Autowired
    private TimeService timeService;
    
    @Autowired
    private WeatherReportRepo weatherReportRepo;
    
    static final int MAX_TIMESERIES_LENGTH = 2400;
    List<Double> baseTimeSeries = new ArrayList<Double>();
    
    private final CustomerProfile customerProfile;
    private final CapacityProfile capacityProfile;
    
    private double lastBaseCapacity = Double.NaN;
    private double lastAdjustedCapacity = Double.NaN;
    
    
    DefaultCapacityManager(CustomerProfile customer, CapacityBundle bundle, Element xml) 
    {
        customerProfile = customer;
        capacityProfile = new CapacityProfile(xml, bundle);
        
        timeService = (TimeService) SpringApplicationContext.getBean("timeService");
        weatherReportRepo = (WeatherReportRepo) SpringApplicationContext.getBean("weatherReportRepo");
        
        if (capacityProfile.baseCapacityType == BaseCapacityType.TIMESERIES) {
            initializeBaseTimeseries();
        }
    }
    
    private void initializeBaseTimeseries() 
    {
        InputStream inputStream = null;
        String seriesName = capacityProfile.baseTimeseriesModel.seriesName;
        switch (capacityProfile.baseTimeseriesModel.seriesSource) {
        case BUILTIN:
            try {
                inputStream = TimeseriesPatterns.generateTimeseriesStream(seriesName);
            } catch (java.io.IOException e) {
                log.error(getName() + ": Unexpected IOException caught from builtin timeseries stream: " + e);
                throw new Error("Caught IOException: " + e.toString());
            }
        case CLASSPATH:
            inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(seriesName);
            break;
        case FILEPATH:
            try {
                inputStream = new FileInputStream(seriesName);  
            } catch (FileNotFoundException e) {
                log.error(getName() + ": Could not find file to initialize base timeseries: " + seriesName);
            }
            break;
        default: throw new Error(getName() + ": Unexpected base timeseries source type!");
        }
        if (inputStream == null) throw new Error(getName() + ": Base timeseries input stream is uninitialized!");
        
        DataInputStream dataStream = new DataInputStream(inputStream);
        for (int i=0; i < MAX_TIMESERIES_LENGTH; ++i) {
            try {
                baseTimeSeries.add(dataStream.readDouble());
            } catch (java.io.EOFException e) {
                break;
            } catch (java.io.IOException e) {
                log.error(getName() + ": Error reading timeseries data from file: " + seriesName);
                e.printStackTrace();
                throw new Error(getName() + ": Caught IOException: " + e.toString());
            }
        }        
    }

    /** @Override @code{CapacityManager} **/
    public double drawBaseCapacitySample(Timeslot timeslot, int customerCount) 
    {    
        double baseCapacity = 0.0;
        switch (capacityProfile.baseCapacityType) {
        case POPULATION:
            double popRatio = customerCount / customerProfile.customerInfo.getPopulation();
            baseCapacity = popRatio * capacityProfile.basePopulationCapacity.drawSample();
            if (! Double.isNaN(lastBaseCapacity)) baseCapacity = (baseCapacity + lastBaseCapacity) / 2;  // smoothing
            break;
        case INDIVIDUAL:
            for (int i=0; i < customerCount; ++i) {
                double draw = capacityProfile.baseIndividualCapacity.drawSample();
                baseCapacity += draw;
            }
            if (! Double.isNaN(lastBaseCapacity)) baseCapacity = (baseCapacity + lastBaseCapacity) / 2;  // smoothing
            break;
        case TIMESERIES:
            baseCapacity = getBaseCapacityFromTimeseries(timeslot, customerCount);
            break;            
        default: throw new Error(getName() + ": Unexpected base capacity type: " + capacityProfile.baseCapacityType);
        }
        lastBaseCapacity = truncateTo2Decimals(baseCapacity);
        return (lastBaseCapacity);
    }
	
    private double drawBaseCapacitySample(Timeslot timeslot, TariffSubscription subscription) 
    {
        return drawBaseCapacitySample(timeslot, subscription.getCustomersCommitted());
    }
    
    private double getBaseCapacityFromTimeseries(Timeslot timeslot, int customerCount)
    {
        try {
            double popRatio = customerCount / customerProfile.customerInfo.getPopulation();
            return popRatio * baseTimeSeries.get(timeslot.getSerialNumber());
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error(getName() + ": Tried to get base capacity from time series at index beyond maximum!");
            throw e;
        }
    }
        
    /** Override @code{CapacityManager} **/
    public double computeCapacity(Timeslot timeslot, TariffSubscription subscription)
    {
        double baseCapacity = drawBaseCapacitySample(timeslot, subscription);
        
        double adjustedCapacity = baseCapacity;
        adjustedCapacity = adjustCapacityForPeriodicSkew(adjustedCapacity);
        adjustedCapacity = adjustCapacityForWeather(timeslot, adjustedCapacity);                
        adjustedCapacity = adjustCapacityForTariffRates(timeslot, subscription, adjustedCapacity);
   
        if (! Double.isNaN(lastAdjustedCapacity)) adjustedCapacity = (adjustedCapacity + lastAdjustedCapacity) / 2;  // smoothing
        
        lastAdjustedCapacity = truncateTo2Decimals(adjustedCapacity);
        
        log.info(getName() + ": Base capacity = " + baseCapacity + "; adjusted capacity = " + lastAdjustedCapacity);        
        return lastAdjustedCapacity;
    }

    private double adjustCapacityForPeriodicSkew(double capacity)
    {
        DateTime now = timeService.getCurrentDateTime();
        int day = now.getDayOfWeek();  // 1=Monday, 7=Sunday
        int hour = now.getHourOfDay();  // 0-23
        
        double periodicSkew = capacityProfile.dailySkew[day-1] * capacityProfile.hourlySkew[hour];
        log.debug(getName() + ": periodicSkew = " + periodicSkew);
        return capacity * periodicSkew;        
    }

    private double adjustCapacityForWeather(Timeslot timeslot, double capacity)
    {
        WeatherReport weather = weatherReportRepo.currentWeatherReport();
        log.debug(getName() + ": weather = (" + weather.getTemperature() + ", " 
                + weather.getWindSpeed() + ", " + weather.getWindDirection() + ", " + weather.getCloudCover() + ")");
        double weatherFactor = 1.0;
        if (capacityProfile.temperatureInfluence == InfluenceKind.DIRECT) {
            int temperature = (int) Math.round(weather.getTemperature());
            weatherFactor = weatherFactor * capacityProfile.temperatureMap.get(temperature);
        }
        else if (capacityProfile.temperatureInfluence == InfluenceKind.DEVIATION) {
            int curr = (int) Math.round(weather.getTemperature());
            int ref = (int) Math.round(capacityProfile.temperatureReference);
            double deviationFactor = 1.0;
            if (curr > ref) {
                for (int t = ref+1; t <= curr; ++t) {
                    deviationFactor += capacityProfile.temperatureMap.get(t);
                }
            } else if (curr < ref) {
                for (int t = curr; t < ref; ++t) {
                    deviationFactor += capacityProfile.temperatureMap.get(t);
                }                
            }
            weatherFactor = weatherFactor * deviationFactor;
        }
        if (capacityProfile.windSpeedInfluence == InfluenceKind.DIRECT) {
            int windSpeed = (int) Math.round(weather.getWindSpeed());
            weatherFactor = weatherFactor * capacityProfile.windSpeedMap.get(windSpeed);
        }
        if (capacityProfile.windDirectionInfluence == InfluenceKind.DIRECT) {
            int windDirection = (int) Math.round(weather.getWindDirection());
            weatherFactor = weatherFactor * capacityProfile.windDirectionMap.get(windDirection);
        }
        if (capacityProfile.cloudCoverInfluence == InfluenceKind.DIRECT) {
            int cloudCover = (int) Math.round(weather.getCloudCover());
            weatherFactor = weatherFactor * capacityProfile.cloudCoverMap.get(cloudCover);
        }
        log.debug(getName() + ": weatherFactor = " + weatherFactor);
        return capacity * weatherFactor;
    }
    
    private double adjustCapacityForTariffRates(Timeslot timeslot, TariffSubscription subscription, double baseCapacity)
    {
        double chargeForBase = subscription.getTariff().getUsageCharge(timeslot.getStartInstant(), 
                                                                       baseCapacity, subscription.getTotalUsage());
        double rateForBase = chargeForBase / baseCapacity;
        
        double benchmarkRate = capacityProfile.benchmarkRates.get(timeService.getHourOfDay());
        double rateRatio = rateForBase / benchmarkRate;

        double tariffRatesFactor = determineElasticityFactor(rateRatio);
        log.debug(getName() + ": tariffRatesFactor = " + tariffRatesFactor);
        return baseCapacity * tariffRatesFactor;
    }
	
    private double determineElasticityFactor(double rateRatio)
    {
        switch (capacityProfile.elasticityModelType) {
        case CONTINUOUS:
            return determineContinuousElasticityFactor(rateRatio);
        case STEPWISE:
            return determineStepwiseElasticityFactor(rateRatio);
        default: throw new Error("Unexpected elasticity model type: " + capacityProfile.elasticityModelType);
        }
    }
    
    private double determineContinuousElasticityFactor(double rateRatio)
    {
        double percentChange = (rateRatio - 1.0) / 0.01;
        double elasticityRatio = Double.parseDouble(capacityProfile.elasticityModelXml.getAttribute("ratio"));
        
        String range = capacityProfile.elasticityModelXml.getAttribute("range");
        String[] minmax = range.split("~");
        double low = Double.parseDouble(minmax[0]);
        double high = Double.parseDouble(minmax[1]);
        
        return Math.max(low, Math.min(high, 1.0 + (percentChange * elasticityRatio)));
    }
    
    private double determineStepwiseElasticityFactor(double rateRatio)
    {
        double[][] elasticity = null;
        if (elasticity == null) {
            elasticity = parseMapToDoubleArray(capacityProfile.elasticityModelXml.getAttribute("map"));
        }
        if (Math.abs(rateRatio - 1) < 0.01 || elasticity.length == 0) return 1.0;       
        if (capacityProfile.parentBundle.getCapacityType() == CapacityType.CONSUMPTION && rateRatio < 1.0) return 1.0;       
        if (capacityProfile.parentBundle.getCapacityType() == CapacityType.PRODUCTION && rateRatio > 1.0) return 1.0;
        
        final int RATE_RATIO_INDEX = 0;
        final int CAPACITY_FACTOR_INDEX = 1;
        double rateLowerBound = Double.NEGATIVE_INFINITY;
        double rateUpperBound = Double.POSITIVE_INFINITY;
        double lowerBoundCapacityFactor = 1.0;
        double upperBoundCapacityFactor = 1.0;
        for (int i=0; i < elasticity.length; ++i) {
            double r = elasticity[i][RATE_RATIO_INDEX];
            if (r <= rateRatio && r > rateLowerBound) {
                rateLowerBound = r;
                lowerBoundCapacityFactor = elasticity[i][CAPACITY_FACTOR_INDEX];
            }
            if (r >= rateRatio && r < rateUpperBound) {
                rateUpperBound = r;
                upperBoundCapacityFactor = elasticity[i][CAPACITY_FACTOR_INDEX];
            }
        }	
        return (rateRatio < 1) ? upperBoundCapacityFactor : lowerBoundCapacityFactor;
    }
    
    private static double[][] parseMapToDoubleArray(String input) {
        String[] pairs = input.split(",");
        double[][] ret = new double[pairs.length][2];
        for (int i=0; i < pairs.length; ++i) {
            String[] vals = pairs[i].split(":");
            ret[i][0] = Double.parseDouble(vals[0]);
            ret[i][1] = Double.parseDouble(vals[1]);
        }
        return ret;
    }
    
    private static double truncateTo2Decimals(double x)
    {
        double fract, whole;
        if (x > 0) {
            whole = Math.floor(x);
            fract = Math.floor((x - whole) * 100) / 100;
        } else {
            whole = Math.ceil(x);
            fract = Math.ceil((x - whole) * 100) / 100;
        }
        return whole + fract;
    }

    private String getName() 
    {
        return customerProfile.name;
    }    
}

