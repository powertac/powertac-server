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

/**
 * Data-holder class for parsed configuration elements of one capacity.
 * All members are declared final in the package scope.
 * 
 * @author Prashant Reddy
 */
public final class CapacityStructure
{	
    public enum InfluenceKind { DIRECT, DEVIATION, NONE }
    
    public enum BaseCapacityType { POPULATION, INDIVIDUAL, TIMESERIES }

    public enum ElasticityModelType { CONTINUOUS, STEPWISE }
        
    final String capacityName;
    final String description;
    
    final BaseCapacityType baseCapacityType;
    final ProbabilityDistribution basePopulationCapacity;
    final ProbabilityDistribution baseIndividualCapacity;
    final TimeseriesStructure baseTimeseriesStructure;
    
    final double[] dailySkew;
    final double[] hourlySkew;

    final InfluenceKind temperatureInfluence;
    final Map<Integer, Double> temperatureMap = new HashMap<Integer, Double>();  // key: degree Celsius
    final double temperatureReference;
    final InfluenceKind windSpeedInfluence;
    final Map<Integer, Double> windSpeedMap = new HashMap<Integer, Double>();  // key: speed in m/s
    final InfluenceKind windDirectionInfluence;
    final Map<Integer, Double> windDirectionMap = new HashMap<Integer, Double>();  // key: angle 0-360
    final InfluenceKind cloudCoverInfluence;
    final Map<Integer, Double> cloudCoverMap = new HashMap<Integer, Double>();  // key: 0 (clear) - 100 (cloudy) 
        
    final Map<Integer, Double> benchmarkRates = new HashMap<Integer, Double>();  // key: hour of day
    final ElasticityModelType elasticityModelType;
    final Element elasticityModelXml;
        
    final double[] curtailmentShifts;  // index = timeslot
    
    
    CapacityStructure(String name, Element xml, DefaultCapacityBundle bundle) 
    {
        capacityName = name;
        description = xml.getAttribute("description");
        
        Element baseCapacityElement = (Element) xml.getElementsByTagName("baseCapacity").item(0);
        baseCapacityType = Enum.valueOf(BaseCapacityType.class, baseCapacityElement.getAttribute("type"));
        switch (baseCapacityType) {
        case POPULATION: 
            Element populationCapacityElement = (Element) baseCapacityElement.getElementsByTagName("populationCapacity").item(0);
            basePopulationCapacity = new ProbabilityDistribution(populationCapacityElement);
            baseIndividualCapacity = null;
            baseTimeseriesStructure = null;
            break;
        case INDIVIDUAL: 
            basePopulationCapacity = null;
            Element individualCapacityElement = (Element) baseCapacityElement.getElementsByTagName("individualCapacity").item(0);
            baseIndividualCapacity = new ProbabilityDistribution(individualCapacityElement);
            baseTimeseriesStructure = null;
            break;
        case TIMESERIES: 
            basePopulationCapacity = null;
            baseIndividualCapacity = null;
            Element timeseriesModelElement = (Element) baseCapacityElement.getElementsByTagName("timeseriesModel").item(0);
            baseTimeseriesStructure = new TimeseriesStructure(timeseriesModelElement);
            break;
        default: throw new Error("Unexpected base capacity type: " + baseCapacityType);
        }
        
        Element dailySkewElement = (Element) xml.getElementsByTagName("dailySkew").item(0);
        dailySkew = ParserFunctions.parseDoubleArray(dailySkewElement.getAttribute("array"));
        
        Element hourlySkewElement = (Element) xml.getElementsByTagName("hourlySkew").item(0);
        hourlySkew = ParserFunctions.parseDoubleArray(hourlySkewElement.getAttribute("array"));
        
        Element temperatureInfluenceElement = (Element) xml.getElementsByTagName("temperature").item(0);
        temperatureInfluence = Enum.valueOf(InfluenceKind.class, temperatureInfluenceElement.getAttribute("influence"));
        if (temperatureInfluence != InfluenceKind.NONE) {
            ParserFunctions.parseRangeMap(temperatureInfluenceElement.getAttribute("rangeMap"), temperatureMap);
            if (temperatureInfluence == InfluenceKind.DEVIATION) {
                temperatureReference = Double.parseDouble(temperatureInfluenceElement.getAttribute("reference"));
            } else temperatureReference = Double.NaN;
        } else temperatureReference = Double.NaN;

        Element windSpeedInfluenceElement = (Element) xml.getElementsByTagName("windSpeed").item(0);
        windSpeedInfluence = Enum.valueOf(InfluenceKind.class, windSpeedInfluenceElement.getAttribute("influence"));
        if (windSpeedInfluence != InfluenceKind.NONE) {
            ParserFunctions.parseRangeMap(windSpeedInfluenceElement.getAttribute("rangeMap"), windSpeedMap);
        }
          
        Element windDirectionInfluenceElement = (Element) xml.getElementsByTagName("windDirection").item(0);
        windDirectionInfluence = Enum.valueOf(InfluenceKind.class, windDirectionInfluenceElement.getAttribute("influence"));
        if (windDirectionInfluence != InfluenceKind.NONE) {
            ParserFunctions.parseRangeMap(windDirectionInfluenceElement.getAttribute("rangeMap"), windDirectionMap);
        }
        
        Element cloudCoverInfluenceElement = (Element) xml.getElementsByTagName("cloudCover").item(0);
        cloudCoverInfluence = Enum.valueOf(InfluenceKind.class, cloudCoverInfluenceElement.getAttribute("influence"));
        if (cloudCoverInfluence != InfluenceKind.NONE) {
            ParserFunctions.parseRangeMap(cloudCoverInfluenceElement.getAttribute("percentMap"), cloudCoverMap);
        }

        Element priceElasticityElement = (Element) xml.getElementsByTagName("priceElasticity").item(0);
        Element benchmarkRatesElement = (Element) priceElasticityElement.getElementsByTagName("benchmarkRates").item(0);
        ParserFunctions.parseRangeMap(benchmarkRatesElement.getAttribute("rangeMap"), benchmarkRates);
        
        elasticityModelXml = (Element) priceElasticityElement.getElementsByTagName("elasticityModel").item(0);
        elasticityModelType = Enum.valueOf(ElasticityModelType.class, elasticityModelXml.getAttribute("type"));
        
        Element curtailmentElement = (Element) xml.getElementsByTagName("curtailment").item(0);
        curtailmentShifts = (curtailmentElement != null) ? 
                ParserFunctions.parseDoubleArray(curtailmentElement.getAttribute("shifts")) : null;
    }
    
} // end class

