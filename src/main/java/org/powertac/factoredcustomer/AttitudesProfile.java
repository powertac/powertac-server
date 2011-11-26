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

import org.w3c.dom.*;
import java.util.Random;

/**
 * @author Prashant Reddy
 */
class AttitudesProfile extends BehaviorsProfile
{
    enum InfluenceKind { DIRECT, DEVIATION, NONE };
    enum WeatherGranularity { MONTHLY, HOURLY, NONE };
	
    long minCapacityPerCustomer;
    long maxCapacityPerCustomer;
    
    ProbabilityDistribution curtailableCapacity;
    double curtailingThreshold;
    ProbabilityDistribution shiftableCapacity;
    double shiftingThreshold;
    
    double[] capacityRatioByMonth;
    double[] capacityRatioByDay;
    double[] capacityRatioByHour;

    InfluenceKind temperatureInfluence;
    double temperatureCorrelation;
    WeatherGranularity temperatureGranularity;
    InfluenceKind windSpeedInfluence;
    double windSpeedCorrelation;
    InfluenceKind cloudCoverInfluence;
    double cloudCoverCorrelation;
	
    AttitudesProfile(Element xml, Random random) 
    {
        super(xml, random);

        Element minCapacityPerCustomerElement = (Element) xml.getElementsByTagName("minCapacityPerCustomer").item(0);
        minCapacityPerCustomer = Long.parseLong(minCapacityPerCustomerElement.getAttribute("value"));
        Element maxCapacityPerCustomerElement = (Element) xml.getElementsByTagName("maxCapacityPerCustomer").item(0);
        maxCapacityPerCustomer = Long.parseLong(maxCapacityPerCustomerElement.getAttribute("value"));
	
        Element curtailableCapacityElement = (Element) xml.getElementsByTagName("curtailableCapacity").item(0);
	curtailableCapacity = new ProbabilityDistribution(curtailableCapacityElement, random.nextLong());
        Element curtailingThresholdElement = (Element) xml.getElementsByTagName("curtailingThreshold").item(0);
	curtailingThreshold = Double.parseDouble(curtailingThresholdElement.getAttribute("changeRatio"));
	
        Element shiftableCapacityElement = (Element) xml.getElementsByTagName("shiftableCapacity").item(0);
	shiftableCapacity = new ProbabilityDistribution(shiftableCapacityElement, random.nextLong());
        Element shiftingThresholdElement = (Element) xml.getElementsByTagName("shiftingThreshold").item(0);
	shiftingThreshold = Double.parseDouble(shiftingThresholdElement.getAttribute("changeRatio"));
	
        Element capacityRatioByMonthElement = (Element) xml.getElementsByTagName("capacityRatioByMonth").item(0);
	capacityRatioByMonth = parseDoubleArray(capacityRatioByMonthElement.getAttribute("array"));
        Element capacityRatioByDayElement = (Element) xml.getElementsByTagName("capacityRatioByDay").item(0);
	capacityRatioByDay = parseDoubleArray(capacityRatioByDayElement.getAttribute("array"));
        Element capacityRatioByHourElement = (Element) xml.getElementsByTagName("capacityRatioByHour").item(0);
	capacityRatioByHour = parseDoubleArray(capacityRatioByHourElement.getAttribute("array"));
	
        Element temperatureInfluenceElement = (Element) xml.getElementsByTagName("temperatureInfluence").item(0);
	temperatureInfluence = Enum.valueOf(InfluenceKind.class, temperatureInfluenceElement.getAttribute("kind"));
	temperatureCorrelation = Double.parseDouble(temperatureInfluenceElement.getAttribute("correlation"));
	temperatureGranularity = Enum.valueOf(WeatherGranularity.class, temperatureInfluenceElement.getAttribute("granularity"));

	Element windSpeedInfluenceElement = (Element) xml.getElementsByTagName("windSpeedInfluence").item(0);
	windSpeedInfluence = Enum.valueOf(InfluenceKind.class, windSpeedInfluenceElement.getAttribute("kind"));
	windSpeedCorrelation = Double.parseDouble(windSpeedInfluenceElement.getAttribute("correlation"));
	
        Element cloudCoverInfluenceElement = (Element) xml.getElementsByTagName("cloudCoverInfluence").item(0);
	cloudCoverInfluence = Enum.valueOf(InfluenceKind.class, cloudCoverInfluenceElement.getAttribute("kind"));
	cloudCoverCorrelation = Double.parseDouble(cloudCoverInfluenceElement.getAttribute("correlation"));
    }

    protected double[] parseDoubleArray(String input) {
        String[] items = input.split(",");
        double[] ret = new double[items.length];
        for (int i=0; i < items.length; ++i) {
            ret[i] = Double.parseDouble(items[i]);
        }
        return ret;
    }

} // end class

