/* Copyright 2011 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.*;
import org.powertac.common.enumerations.PowerType;

/**
 * Data-holder class for parsed configuration elements of one capacity.
 * All members are declared final in the package scope.
 * 
 * @author Prashant Reddy
 */
final class CapacityProfile
{	
    enum CapacityType { CONSUMPTION, PRODUCTION, STORAGE };
    enum CapacitySubType { NONE, INTERRUPTIBLE, THERMAL_STORAGE, 
                           SOLAR,  WIND, RUN_OF_RIVER, PUMPED_STORAGE, CHP, FOSSIL, 
                           BATTERY_STORAGE, ELECTRIC_VEHICLE };
                          
    enum InfluenceKind { DIRECT, DEVIATION, NONE };
    
    enum BaseCapacityType { POPULATION, INDIVIDUAL, TIMESERIES }
    enum BaseTimeseriesSource { BUILTIN, CLASSPATH, FILEPATH }

    enum ElasticityModelType { CONTINUOUS, STEPWISE }
    
    class TimeseriesModel 
    {
        String modelKey;
        String seriesName;
        
        BaseTimeseriesSource seriesSource; 
        Map<String, Double> arimaCoeffs = new HashMap<String, Double>();
        double[] dailyCoeffs;
        double[] hourlyCoeffs;
    }
    
    final String capacityId;
    final String description;
    final CapacityBundle parentBundle;
    
    final BaseCapacityType baseCapacityType;
    final ProbabilityDistribution basePopulationCapacity;
    final ProbabilityDistribution baseIndividualCapacity;
    final TimeseriesModel baseTimeseriesModel;
    
    final double[] dailySkew;
    final double[] hourlySkew;

    final InfluenceKind temperatureInfluence;
    final Map<Integer, Double> temperatureMap = new HashMap<Integer, Double>();  // key: degree Celsius
    final double temperatureReference;
    final InfluenceKind windSpeedInfluence;
    final Map<Integer, Double> windSpeedMap = new HashMap<Integer, Double>();  // key: 0 (calm) - 12 (hurricane) Beaufort scale 
    final InfluenceKind windDirectionInfluence;
    final Map<Integer, Double> windDirectionMap = new HashMap<Integer, Double>();  // key: angle 0-360
    final InfluenceKind cloudCoverInfluence;
    final Map<Integer, Double> cloudCoverMap = new HashMap<Integer, Double>();  // key: 0 (cloudy) - 255 (clear) 
        
    final Map<Integer, Double> benchmarkRates = new HashMap<Integer, Double>();  // key: hour of day
    final ElasticityModelType elasticityModelType;
    final Element elasticityModelXml;
        
	
    CapacityProfile(Element xml, CapacityBundle bundle) 
    {
        capacityId = xml.getAttribute("id");
        description = xml.getAttribute("description");
        parentBundle = bundle;
        
        Element baseCapacityElement = (Element) xml.getElementsByTagName("baseCapacity").item(0);
        baseCapacityType = Enum.valueOf(BaseCapacityType.class, baseCapacityElement.getAttribute("type"));
        switch (baseCapacityType) {
        case POPULATION: 
            Element populationCapacityElement = (Element) baseCapacityElement.getElementsByTagName("populationCapacity").item(0);
            basePopulationCapacity = new ProbabilityDistribution(populationCapacityElement);
            baseIndividualCapacity = null;
            baseTimeseriesModel = null;
            break;
        case INDIVIDUAL: 
            basePopulationCapacity = null;
            Element individualCapacityElement = (Element) baseCapacityElement.getElementsByTagName("individualCapacity").item(0);
            baseIndividualCapacity = new ProbabilityDistribution(individualCapacityElement);
            baseTimeseriesModel = null;
            break;
        case TIMESERIES: 
            basePopulationCapacity = null;
            baseIndividualCapacity = null;
            baseTimeseriesModel = parseTimeseriesModel(baseCapacityElement);
            break;
        default: throw new Error("Unexpected base capacity type: " + baseCapacityType);
        }
        
        Element dailySkewElement = (Element) xml.getElementsByTagName("dailySkew").item(0);
        dailySkew = parseDoubleArray(dailySkewElement.getAttribute("array"));
        
        Element hourlySkewElement = (Element) xml.getElementsByTagName("hourlySkew").item(0);
        hourlySkew = parseDoubleArray(hourlySkewElement.getAttribute("array"));
        
        Element temperatureInfluenceElement = (Element) xml.getElementsByTagName("temperature").item(0);
        temperatureInfluence = Enum.valueOf(InfluenceKind.class, temperatureInfluenceElement.getAttribute("influence"));
        if (temperatureInfluence != InfluenceKind.NONE) {
            parseRangeMap(temperatureInfluenceElement.getAttribute("rangeMap"), temperatureMap);
            if (temperatureInfluence == InfluenceKind.DEVIATION) {
                temperatureReference = Double.parseDouble(temperatureInfluenceElement.getAttribute("reference"));
            } else temperatureReference = Double.NaN;
        } else temperatureReference = Double.NaN;

        Element windSpeedInfluenceElement = (Element) xml.getElementsByTagName("windSpeed").item(0);
        windSpeedInfluence = Enum.valueOf(InfluenceKind.class, windSpeedInfluenceElement.getAttribute("influence"));
        if (windSpeedInfluence != InfluenceKind.NONE) {
            parseRangeMap(windSpeedInfluenceElement.getAttribute("rangeMap"), windSpeedMap);
        }
          
        Element windDirectionInfluenceElement = (Element) xml.getElementsByTagName("windDirection").item(0);
        windDirectionInfluence = Enum.valueOf(InfluenceKind.class, windDirectionInfluenceElement.getAttribute("influence"));
        if (windDirectionInfluence != InfluenceKind.NONE) {
            parseRangeMap(windDirectionInfluenceElement.getAttribute("rangeMap"), windDirectionMap);
        }
        
        Element cloudCoverInfluenceElement = (Element) xml.getElementsByTagName("cloudCover").item(0);
        cloudCoverInfluence = Enum.valueOf(InfluenceKind.class, cloudCoverInfluenceElement.getAttribute("influence"));
        if (cloudCoverInfluence != InfluenceKind.NONE) {
            parseRangeMap(cloudCoverInfluenceElement.getAttribute("rangeMap"), cloudCoverMap);
        }

        Element priceElasticityElement = (Element) xml.getElementsByTagName("priceElasticity").item(0);
        Element benchmarkRatesElement = (Element) priceElasticityElement.getElementsByTagName("benchmarkRates").item(0);
        parseRangeMap(benchmarkRatesElement.getAttribute("rangeMap"), benchmarkRates);
        
        elasticityModelXml = (Element) priceElasticityElement.getElementsByTagName("elasticityModel").item(0);
        elasticityModelType = Enum.valueOf(ElasticityModelType.class, elasticityModelXml.getAttribute("type"));
    }
    
    protected TimeseriesModel parseTimeseriesModel(Element xml) 
    {
        TimeseriesModel model = new TimeseriesModel();
        model.modelKey = xml.getAttribute("modelkey");

        Element baseSeriesElement = (Element) xml.getElementsByTagName("baseSeries").item(0);
        model.seriesName = baseSeriesElement.getAttribute("name");
        model.seriesSource = Enum.valueOf(BaseTimeseriesSource.class, baseSeriesElement.getAttribute("source"));
        
        Element arimaCoeffsElement = (Element) xml.getElementsByTagName("arimaCoeffs").item(0);
        NamedNodeMap arimaAttrs = arimaCoeffsElement.getAttributes();
        for (int i=0; i < arimaAttrs.getLength(); ++i) {
            Node arimaAttr = arimaAttrs.item(i);
            model.arimaCoeffs.put(arimaAttr.getNodeName(), Double.parseDouble(arimaAttr.getNodeValue()));
        }
        
        Element hourlyCoeffsElement = (Element) xml.getElementsByTagName("hourlyCoeffs").item(0);
        model.hourlyCoeffs = parseDoubleArray(hourlyCoeffsElement.getAttribute("array"));
        Element dailyCoeffsElement = (Element) xml.getElementsByTagName("dailyCoeffs").item(0);
        model.dailyCoeffs = parseDoubleArray(dailyCoeffsElement.getAttribute("array"));
        return model;
    }

    protected void parseRangeMap(String input, Map<Integer, Double> map) 
    {
        String[] pairs = input.split(",");
        for (int i=0; i < pairs.length; ++i) {
            String[] parts = pairs[i].split(":");
            Double value = Double.parseDouble(parts[1]);
            String[] range = parts[0].split("~");
            Integer start = Integer.parseInt(range[0].trim());
            Integer end = Integer.parseInt(range[1].trim());
            for (Integer key=start; key <= end; ++key) {
                map.put(key, value);
            }
        }        
    }
    
    protected static double[] parseDoubleArray(String input) {
        String[] items = input.split(",");
        double[] ret = new double[items.length];
        for (int i=0; i < items.length; ++i) {
            ret[i] = Double.parseDouble(items[i]);
        }
        return ret;
    }
    
    static PowerType reportPowerType(CapacityType capacityType, CapacitySubType capacitySubType)
    {
        switch (capacityType) {
        case CONSUMPTION:
            switch (capacitySubType) {
            case NONE:
                return PowerType.CONSUMPTION;
            case INTERRUPTIBLE:
                return PowerType.INTERRUPTIBLE_CONSUMPTION;
            case THERMAL_STORAGE:
                return PowerType.INTERRUPTIBLE_CONSUMPTION;
            default: throw new Error("Incompatible capacity subType: " + capacitySubType);
            }
        case PRODUCTION:
            switch (capacitySubType) {
            case NONE:
                return PowerType.PRODUCTION;
            case SOLAR:
                return PowerType.SOLAR_PRODUCTION;
            case WIND:
                return PowerType.WIND_PRODUCTION;
            case RUN_OF_RIVER:
                return PowerType.RUN_OF_RIVER_PRODUCTION;
            case PUMPED_STORAGE:
                return PowerType.PUMPED_STORAGE_PRODUCTION;
            case CHP:
                return PowerType.CHP_PRODUCTION;
            case FOSSIL:
                return PowerType.FOSSIL_PRODUCTION;
            default: throw new Error("Incompatible capacity subType: " + capacitySubType);
            }
        case STORAGE:
            switch (capacitySubType) {
            case BATTERY_STORAGE:
                return PowerType.BATTERY_STORAGE;
            case ELECTRIC_VEHICLE:
                return PowerType.ELECTRIC_VEHICLE;
            default: throw new Error("Incompatible capacity subType: " + capacitySubType);
            }	
        default: throw new Error("Incompatible capacity type: " + capacityType);        
        }	
    }
	
    static CapacityType reportCapacityType(PowerType powerType)
    {
      if (powerType.isConsumption())
        return CapacityType.CONSUMPTION;
      if (powerType.isProduction())
        return CapacityType.PRODUCTION;
      if (powerType.isStorage())
        return CapacityType.STORAGE;
      throw new Error("Unexpected powerType: " + powerType);
    }	
	
} // end class

