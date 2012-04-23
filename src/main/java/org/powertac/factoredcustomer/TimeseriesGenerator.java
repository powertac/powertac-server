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

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import org.joda.time.DateTime;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.apache.commons.io.IOUtils;

/**
 * Utility class that generates various time series patterns that can be 
 * used as base capacity series by implementations of @code{CapacityOriginator}.
 * 
 * @author Prashant Reddy
 */
final class TimeseriesGenerator
{
    enum ModelType { ARIMA_101x101 }
    enum DataSource { BUILTIN, CLASSPATH, FILEPATH }

    private RandomSeedRepo randomSeedRepo;

    private final Properties modelParams = new Properties();
    
    private final List<Double> refSeries = new ArrayList<Double>();
    private final Map<Integer, Double> genSeries = new HashMap<Integer, Double>();
    
    private final TimeseriesStructure tsStructure;
    
    private final int FORECAST_HORIZON = 2 * 24; // two days
    
    private double Y0;
    private double[] Yd;
    private double[] Yh;
    private double phi1;
    private double Phi1;
    private double theta1;
    private double Theta1;
    private double sigma;
    private double lambda;
    private double gamma;
    
    private Random arimaNoise;
    
    
    TimeseriesGenerator(TimeseriesStructure structure) 
    {
        tsStructure = structure;
        
        switch (tsStructure.modelType) {
        case ARIMA_101x101:
            initArima101x101();
            break;
        default: throw new Error("Unexpected timeseries model type: " + tsStructure.modelType);
        }
    }
    
    private void initArima101x101() 
    {   
        initArima101x101ModelParams();
        initArima101x101RefSeries();
    }
    
    private void initArima101x101ModelParams()
    {
        InputStream paramsStream;
        String paramsName = tsStructure.modelParamsName;
        switch (tsStructure.modelParamsSource) {
        case BUILTIN:
            throw new Error("Unknown builtin model parameters with name: " + paramsName);
            // break;
        case CLASSPATH:
            paramsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(paramsName);
            break;
        case FILEPATH:
            try {
                paramsStream = new FileInputStream(paramsName);  
            } catch (FileNotFoundException e) {
                throw new Error("Could not find file to initialize model parameters: " + paramsName);
            }
            break;
        default: 
            throw new Error("Unexpected reference timeseries source type: " + tsStructure.refSeriesSource);
         }
        if (paramsStream == null) throw new Error("Model parameters input stream is uninitialized!");

        try {
            modelParams.load(paramsStream);        
        } catch (java.io.IOException e) {
            throw new Error("Error reading model parameters from file: " + paramsName + "; caught IOException: " + e.toString());        
        }
        
        Y0 = Double.parseDouble((String) modelParams.get("Y0"));
        Yd = ParserFunctions.parseDoubleArray((String) modelParams.get("Yd"));
        Yh = ParserFunctions.parseDoubleArray((String) modelParams.get("Yh"));        
        phi1 = Double.parseDouble((String) modelParams.get("phi1"));
        Phi1 = Double.parseDouble((String) modelParams.get("Phi1"));
        theta1 = Double.parseDouble((String) modelParams.get("theta1"));
        Theta1 = Double.parseDouble((String) modelParams.get("Theta1"));
        lambda = Double.parseDouble((String) modelParams.get("lambda"));
        gamma = Double.parseDouble((String) modelParams.get("gamma"));
        sigma = Double.parseDouble((String) modelParams.get("sigma"));
        
        randomSeedRepo = (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");

        arimaNoise = new Random(randomSeedRepo.getRandomSeed("factoredcustomer.TimeseriesGenerator", 
                                                             tsStructure.hashCode(), "ArimaNoise").getValue());
    }
    
    private void initArima101x101RefSeries()
    {
        InputStream refStream;
        String seriesName = tsStructure.refSeriesName;
        switch (tsStructure.refSeriesSource) {
        case BUILTIN:
            throw new Error("Unknown builtin series name: " + seriesName);
            // break;
        case CLASSPATH:
            refStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(seriesName);
            break;
        case FILEPATH:
            try {
                refStream = new FileInputStream(seriesName);  
            } catch (FileNotFoundException e) {
                throw new Error("Could not find file to initialize reference timeseries: " + seriesName);
            }
            break;
        default: 
            throw new Error("Unexpected reference timeseries source type: " + tsStructure.refSeriesSource);
         }
        if (refStream == null) throw new Error("Reference timeseries input stream is uninitialized!");
            
        try {
            @SuppressWarnings("unchecked")
            List<String> series = (List<String>) IOUtils.readLines(refStream);
            for (String line: series) {
                Double element = Double.parseDouble(line);
                refSeries.add(element);
            }
        } catch (java.io.EOFException e) {
            final int MIN_TIMESERIES_LENGTH = 26;
            if (refSeries.size() < MIN_TIMESERIES_LENGTH) {
                throw new Error("Insufficient data in reference series; expected " + MIN_TIMESERIES_LENGTH 
                                + " elements, found only " +  genSeries.size());
            }
        } catch (java.io.IOException e) {
            throw new Error("Error reading timeseries data from file: " + seriesName + "; caught IOException: " + e.toString());        
        }
    }

    public double generateNext(Timeslot timeslot)
    {
        Double next;
        switch (tsStructure.modelType) {
        case ARIMA_101x101:
            if (genSeries.isEmpty()) {
                initArima101x101GenSeries(timeslot);
            }
            next = genSeries.get(timeslot.getSerialNumber());
            if (next == null) {
                next = generateNextArima101x101(timeslot);
                genSeries.put(timeslot.getSerialNumber(), next); 
            }
            break;
        default: throw new Error("Unexpected timeseries model type: " + tsStructure.modelType);
        }
        return next;
    }
    
    private void initArima101x101GenSeries(Timeslot timeslot)
    {
        int start = timeslot.getSerialNumber();
        for (int i=0; i < refSeries.size(); ++i) {
            genSeries.put(start + i, refSeries.get(i));
        }
    }
    
    private double generateNextArima101x101(Timeslot timeslot)
    {
        /** R code
        boostTimeSeries = function(Xt, lambda, t, N, Xht, Xdt, gamma) {
            return (Xt + (lambda * ((log(t-26))^2/(log(N-26))^2) * ((1 - gamma) * Xht + gamma * Xdt)))
        }
        for (t in compRange) {                  
            Zf[t] = Y0 + Yd[D[t]] + Yh[H[t]] + phi1 * Zf[t-1] + Phi1 * Zf[t-24] #+ rnorm(1, 0, sigma^2) + 
                          theta1 * (Zf[t-1] - Zf[t-2]) + Theta1 * (Zf[t-24] - Zf[t-25]) + 
                          theta1 * Theta1 * (Zf[t-25] - Zf[t-26]) 
            Zbf[t] = boostTimeSeries(Zf[t], lambda, t, N, Yh[H[t]], Yd[D[t]], gamma) #+ rnorm(1, 0, sigma^2)
        }
        **/
      
        DateTime now = timeslot.getStartInstant().toDateTime();
        int day = now.getDayOfWeek();  // 1=Monday, 7=Sunday
        int hour = now.getHourOfDay();  // 0-23
        
        int t = timeslot.getSerialNumber();
        
        double logNext = Y0 + Yd[day-1] + Yh[hour] + phi1 * getLog(t-1) + Phi1 * getLog(t-24) 
                         + theta1 * (getLog(t-1) - getLog(t-2)) + Theta1 * (getLog(t-24) - getLog(t-25)) 
                         + theta1 * Theta1 * (getLog(t-25) - getLog(t-26));
        logNext = logNext + (lambda * (Math.pow(Math.log(t-26), 2) / Math.pow(Math.log(FORECAST_HORIZON - 26), 2)) 
                                       * ((1 - gamma) * Yh[hour] + gamma * Yd[day-1]));
        logNext = logNext + Math.pow(sigma, 2) * arimaNoise.nextGaussian();
        double next = Math.exp(logNext);
        if (Double.isNaN(next)) throw new Error("Generated NaN as next time series element!");
        return next;
    }
    
    private double getLog(int timeslot)
    {
        return Math.log(genSeries.get(timeslot));
    }

} // end class
