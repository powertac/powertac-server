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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.factoredcustomer.interfaces.*;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.CapacityStructure.BaseCapacityType;
import org.powertac.factoredcustomer.CapacityStructure.InfluenceKind;

/**
 * Key class responsible for drawing from a base capacity and ajusting that 
 * capacity in response to various static and dynamic factors for each timeslot.
 * 
 * @author Prashant Reddy
 */
@Domain
class DefaultCapacityOriginator implements CapacityOriginator
{
    protected Logger log = Logger.getLogger(DefaultCapacityOriginator.class.getName());

    protected final FactoredCustomerService factoredCustomerService;
    protected final TimeService timeService;
    protected final TimeslotRepo timeslotRepo;
    protected final WeatherReportRepo weatherReportRepo;
    protected final WeatherForecastRepo weatherForecastRepo;
    
    private final double SMOOTHING_WEIGHT = 0.4;  // 0.0 => ignore previous value

    private final TimeseriesGenerator tsGenerator;
    
    private final CapacityStructure capacityStructure;
    private final CapacityBundle parentBundle;
    
    protected final String logIdentifier;
    
    private final Map<Integer, Double> baseCapacities = new HashMap<Integer, Double>();
    protected final Map<Integer, Double> forecastCapacities = new HashMap<Integer, Double>();
    protected final Map<Integer, Double> actualCapacities = new HashMap<Integer, Double>();
    protected final Map<Integer, Double> curtailedCapacities = new HashMap<Integer, Double>();
    protected final Map<Integer, Double> shiftedCurtailments = new HashMap<Integer, Double>();
    
    
    DefaultCapacityOriginator(CapacityStructure structure, CapacityBundle bundle) 
    {
        capacityStructure = structure;
        parentBundle = bundle;
        
        logIdentifier = capacityStructure.capacityName.isEmpty() ? 
                bundle.getName() : bundle.getName() + "#" + capacityStructure.capacityName;
        
        factoredCustomerService = (FactoredCustomerService) SpringApplicationContext.getBean("factoredCustomerService");
        timeService = (TimeService) SpringApplicationContext.getBean("timeService");
        timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
        weatherReportRepo = (WeatherReportRepo) SpringApplicationContext.getBean("weatherReportRepo");
        weatherForecastRepo = (WeatherForecastRepo) SpringApplicationContext.getBean("weatherForecastRepo");
        
        if (capacityStructure.baseCapacityType == BaseCapacityType.TIMESERIES) {
            tsGenerator = new TimeseriesGenerator(capacityStructure.baseTimeseriesStructure);
        } 
        else tsGenerator = null; 
    }
    
    @Override
    public CapacityProfile getCurrentForecast() 
    {
        Timeslot timeslot = timeslotRepo.currentTimeslot();
        List<Double> values = new ArrayList<Double>();
        for (int i=0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
            Double forecastCapacity = forecastCapacities.get(timeslot.getSerialNumber());
            if (forecastCapacity != null) {
                values.add(forecastCapacity);
            }
            else {
                values.add(getForecastCapacity(timeslot));
            }
            timeslot = timeslot.getNext();
        }
        return new CapacityProfile(values);
    }
    
    protected double getForecastCapacity(Timeslot timeslot)
    {
        Double ret = forecastCapacities.get(timeslot.getSerialNumber());
        if (ret == null) ret = computeForecastCapacity(timeslot);
        return ret;
    }
    
    private double computeForecastCapacity(Timeslot future)
    {
        Timeslot now = timeslotRepo.currentTimeslot();
        int timeToFuture = future.getSerialNumber() - now.getSerialNumber();
        Weather weather = null;
        if (timeToFuture == 0) {
            weather = new Weather(weatherReportRepo.currentWeatherReport()); 
        } 
        else {
            WeatherForecast forecast = weatherForecastRepo.currentWeatherForecast();        
            List<WeatherForecastPrediction> predictions = forecast.getPredictions();
            for (WeatherForecastPrediction prediction: predictions) {
                if (prediction.getForecastTime() == timeToFuture) {
                    weather = new Weather(prediction);
                }
            }
        }
        if (weather == null) throw new Error("Could not find weather forecast for timeslot " + future.getSerialNumber());
        
        double baseCapacity = getBaseCapacity(future);
        if (Double.isNaN(baseCapacity)) throw new Error("Base capacity is NaN!");

        // Compute for full population ignoring current tariff rates
        double forecastCapacity = baseCapacity;
        forecastCapacity = adjustCapacityForPeriodicSkew(forecastCapacity, future.getStartInstant().toDateTime(), false);
        forecastCapacity = adjustCapacityForWeather(forecastCapacity, weather, false); 
        if (Double.isNaN(forecastCapacity)) throw new Error("Adjusted capacity is NaN for base capacity = " + baseCapacity);
        
        forecastCapacity = truncateTo2Decimals(forecastCapacity);
        forecastCapacities.put(future.getSerialNumber(), forecastCapacity);
        log.debug(logIdentifier + ": Forecast capacity for timeslot " + future.getSerialNumber() + " = " + forecastCapacity);        
        return forecastCapacity;
    }
    
    private double getBaseCapacity(Timeslot timeslot) 
    {
        Double ret = baseCapacities.get(timeslot.getSerialNumber());
        if (ret == null) ret = drawBaseCapacitySample(timeslot);
        return ret;
    }
    
    private double drawBaseCapacitySample(Timeslot timeslot) 
    {    
        double baseCapacity = 0.0;
        switch (capacityStructure.baseCapacityType) {
        case POPULATION:
            baseCapacity = capacityStructure.basePopulationCapacity.drawSample();
            break;
        case INDIVIDUAL:
            for (int i=0; i < parentBundle.getPopulation(); ++i) {
                double draw = capacityStructure.baseIndividualCapacity.drawSample();
                baseCapacity += draw;
            }
            break;
        case TIMESERIES:
            baseCapacity = getBaseCapacityFromTimeseries(timeslot);
            break;            
        default: throw new Error(logIdentifier + ": Unexpected base capacity type: " + capacityStructure.baseCapacityType);
        }
        Double prevCapacity = baseCapacities.get(timeslot.getSerialNumber() - 1);
        if (prevCapacity != null) {
            baseCapacity = SMOOTHING_WEIGHT * prevCapacity + (1 - SMOOTHING_WEIGHT) * baseCapacity;
        }
        baseCapacity = truncateTo2Decimals(baseCapacity);
        baseCapacities.put(timeslot.getSerialNumber(), baseCapacity);
        return baseCapacity;
    }
	
    private double getBaseCapacityFromTimeseries(Timeslot timeslot)
    {
        try {
            return tsGenerator.generateNext(timeslot);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error(logIdentifier + ": Tried to get base capacity from time series at index beyond maximum!");
            throw e;
        }
    }
        
    @Override
    public double useCapacity(TariffSubscription subscription)
    {
        Timeslot timeslot = timeslotRepo.currentTimeslot();
        
        double baseCapacity = getBaseCapacity(timeslot);
        if (Double.isNaN(baseCapacity)) throw new Error("Base capacity is NaN!");
        logCapacityDetails(logIdentifier + ": Base capacity for timeslot " + timeslot.getSerialNumber() + " = " + baseCapacity);        

        // total adjusted capacity
        double adjustedCapacity = baseCapacity;
        if (parentBundle.getPowerType().isInterruptible()) {
            adjustedCapacity = adjustCapacityForCurtailments(timeslot, adjustedCapacity, subscription);
        }
        adjustedCapacity = adjustCapacityForPeriodicSkew(adjustedCapacity, timeService.getCurrentDateTime(), true);
        adjustedCapacity = adjustCapacityForCurrentWeather(adjustedCapacity, true);        
        
        // capacity for this subscription
        adjustedCapacity = adjustCapacityForSubscription(timeslot, adjustedCapacity, subscription);
        if (Double.isNaN(adjustedCapacity)) {
            throw new Error("Adjusted capacity is NaN for base capacity = " + baseCapacity);
        }
        
        adjustedCapacity = truncateTo2Decimals(adjustedCapacity);
        actualCapacities.put(timeslot.getSerialNumber(), adjustedCapacity);        
        log.info(logIdentifier + ": Adjusted capacity for tariff " + subscription.getTariff().getId() + " = " + adjustedCapacity);        
        return adjustedCapacity;
    }

    private double adjustCapacityForCurtailments(Timeslot timeslot, double capacity, TariffSubscription subscription)
    {
        double lastCurtailment = subscription.getCurtailment();
        if (Math.abs(lastCurtailment) > 0.01) {  // != 0
            curtailedCapacities.put(timeslot.getSerialNumber() - 1, lastCurtailment);
            if (capacityStructure.curtailmentShifts != null) {
                for (int i=0; i < capacityStructure.curtailmentShifts.length; ++i) {
                    double shiftingFactor = capacityStructure.curtailmentShifts[i];
                    double shiftedCapacity = lastCurtailment * shiftingFactor; 
                    Double previousShifts = shiftedCurtailments.get(timeslot.getSerialNumber() + i); 
                    if (previousShifts == null) {
                        shiftedCurtailments.put(timeslot.getSerialNumber() + i, shiftedCapacity);
                    } else {
                        shiftedCurtailments.put(timeslot.getSerialNumber() + i, previousShifts + shiftedCapacity);
                    }
                }
            }
        }
        Double currentShift = shiftedCurtailments.get(timeslot.getSerialNumber());
        return (currentShift == null) ? capacity : capacity + currentShift;
    }

    private double adjustCapacityForPeriodicSkew(double capacity, DateTime when, boolean verbose)
    {
        int day = when.getDayOfWeek();  // 1=Monday, 7=Sunday
        int hour = when.getHourOfDay();  // 0-23
        
        double periodicSkew = capacityStructure.dailySkew[day-1] * capacityStructure.hourlySkew[hour];
        if (verbose) logCapacityDetails(logIdentifier + ": periodic skew = " + periodicSkew);
        return capacity * periodicSkew;        
    }

    private double adjustCapacityForCurrentWeather(double capacity, boolean verbose)
    {
        WeatherReport weatherReport = weatherReportRepo.currentWeatherReport();
        return adjustCapacityForWeather(capacity, new Weather(weatherReport), verbose);
    }
    
    private double adjustCapacityForWeather(double capacity, Weather weather, boolean verbose)
        {
        if (verbose) logCapacityDetails(logIdentifier + ": weather = (" + weather.getTemperature() + ", " 
                + weather.getWindSpeed() + ", " + weather.getWindDirection() + ", " + weather.getCloudCover() + ")");
        
        double weatherFactor = 1.0;
        if (capacityStructure.temperatureInfluence == InfluenceKind.DIRECT) {
            int temperature = (int) Math.round(weather.getTemperature());
            weatherFactor = weatherFactor * capacityStructure.temperatureMap.get(temperature);
        }
        else if (capacityStructure.temperatureInfluence == InfluenceKind.DEVIATION) {
            int curr = (int) Math.round(weather.getTemperature());
            int ref = (int) Math.round(capacityStructure.temperatureReference);
            double deviationFactor = 1.0;
            if (curr > ref) {
                for (int t = ref+1; t <= curr; ++t) {
                    deviationFactor += capacityStructure.temperatureMap.get(t);
                }
            } else if (curr < ref) {
                for (int t = curr; t < ref; ++t) {
                    deviationFactor += capacityStructure.temperatureMap.get(t);
                }                
            }
            weatherFactor = weatherFactor * deviationFactor;
        }
        if (capacityStructure.windSpeedInfluence == InfluenceKind.DIRECT) {
            int windSpeed = (int) Math.round(weather.getWindSpeed());
            weatherFactor = weatherFactor * capacityStructure.windSpeedMap.get(windSpeed);
            if (windSpeed > 0.0 && capacityStructure.windDirectionInfluence == InfluenceKind.DIRECT) {
                int windDirection = (int) Math.round(weather.getWindDirection());
                weatherFactor = weatherFactor * capacityStructure.windDirectionMap.get(windDirection);
            }
        }
        if (capacityStructure.cloudCoverInfluence == InfluenceKind.DIRECT) {
            int cloudCover = (int) Math.round(100 * weather.getCloudCover());  // [0,1] to ##%
            weatherFactor = weatherFactor * capacityStructure.cloudCoverMap.get(cloudCover);
        }
        if (verbose) logCapacityDetails(logIdentifier + ": weather factor = " + weatherFactor);
        return capacity * weatherFactor;
    }
    
    public double adjustCapacityForSubscription(Timeslot timeslot, double totalCapacity, TariffSubscription subscription)
    {
        double subCapacity = adjustCapacityForPopulationRatio(totalCapacity, subscription);
        return adjustCapacityForTariffRates(timeslot, subCapacity, subscription);
    }
    
    private double adjustCapacityForPopulationRatio(double capacity, TariffSubscription subscription)
    {
        double popRatio = getPopulationRatio(subscription.getCustomersCommitted(), parentBundle.getPopulation());
        logCapacityDetails(logIdentifier + ": population ratio = " + popRatio);
        return capacity * popRatio;     
    }

    private double getPopulationRatio(int customerCount, int population) 
    {
        return ((double) customerCount) / ((double) population);
    }
    
    protected double adjustCapacityForTariffRates(Timeslot timeslot, double baseCapacity, TariffSubscription subscription)
    {
        if ((baseCapacity - 0.0) < 0.01) return baseCapacity;
        
        double chargeForBase = subscription.getTariff().getUsageCharge(timeslot.getStartInstant(), 
                                                                       baseCapacity, subscription.getTotalUsage());
        double rateForBase = chargeForBase / baseCapacity;
        
        double benchmarkRate = capacityStructure.benchmarkRates.get(timeService.getHourOfDay());
        double rateRatio = rateForBase / benchmarkRate;

        double tariffRatesFactor = determineTariffRatesFactor(rateRatio);
        logCapacityDetails(logIdentifier + ": tariff rates factor = " + tariffRatesFactor);
        return baseCapacity * tariffRatesFactor;
    }
	
    private double determineTariffRatesFactor(double rateRatio)
    {
        switch (capacityStructure.elasticityModelType) {
        case CONTINUOUS:
            return determineContinuousElasticityFactor(rateRatio);
        case STEPWISE:
            return determineStepwiseElasticityFactor(rateRatio);
        default: throw new Error("Unexpected elasticity model type: " + capacityStructure.elasticityModelType);
        }
    }
    
    private double determineContinuousElasticityFactor(double rateRatio)
    {
        double percentChange = (rateRatio - 1.0) / 0.01;
        double elasticityRatio = Double.parseDouble(capacityStructure.elasticityModelXml.getAttribute("ratio"));
        
        String range = capacityStructure.elasticityModelXml.getAttribute("range");
        String[] minmax = range.split("~");
        double low = Double.parseDouble(minmax[0]);
        double high = Double.parseDouble(minmax[1]);
        
        return Math.max(low, Math.min(high, 1.0 + (percentChange * elasticityRatio)));
    }
    
    private double determineStepwiseElasticityFactor(double rateRatio)
    {
        double[][] elasticity = null;
        if (elasticity == null) {
            elasticity = ParserFunctions.parseMapToDoubleArray(capacityStructure.elasticityModelXml.getAttribute("map"));
        }
        if (Math.abs(rateRatio - 1) < 0.01 || elasticity.length == 0) return 1.0;  
        PowerType powerType = parentBundle.getPowerType();
        if (powerType.isConsumption() && rateRatio < 1.0) return 1.0;       
        if (powerType.isProduction() && rateRatio > 1.0) return 1.0;
        
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
      
    @Override
    public String getCapacityName()
    {
        return capacityStructure.capacityName;
    }

    @Override
    public CapacityBundle getParentBundle()
    {
        return parentBundle;
    }
    
    protected double truncateTo2Decimals(double x)
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

    protected void logCapacityDetails(String msg) 
    {
        if (factoredCustomerService.getCapacityDetailsLogging() == true) {
            log.info(msg);
        }
    }
    
    @Override
    public String toString()
    {
        return this.getClass().getCanonicalName() + ":" + logIdentifier;
    }
    
    // Convenience class to unify the interface to 
    // WeatherReport and WeatherForecastPrediction.
    private class Weather
    {
        final double temperature;
        final double windSpeed;
        final double windDirection;
        final double cloudCover;
        
        Weather(WeatherReport report)
        {
            temperature = report.getTemperature(); 
            windSpeed = report.getWindSpeed();
            windDirection = report.getWindDirection();
            cloudCover = report.getCloudCover();
        }
        
        Weather(WeatherForecastPrediction prediction)
        {
            temperature = prediction.getTemperature(); 
            windSpeed = prediction.getWindSpeed();
            windDirection = prediction.getWindDirection();
            cloudCover = prediction.getCloudCover();
        }
        
        double getTemperature() { return temperature; }
        double getWindSpeed() { return windSpeed; }
        double getWindDirection() { return windDirection; }
        double getCloudCover() { return cloudCover; }
        
    }
    
} // end class



