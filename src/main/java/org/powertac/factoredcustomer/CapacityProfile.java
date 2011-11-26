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
import org.powertac.common.enumerations.PowerType;

/**
 * @author Prashant Reddy
 */
class CapacityProfile
{
    enum CapacityType { CONSUMPTION, PRODUCTION, STORAGE };
    enum CapacitySubType { NONE, INTERRUPTIBLE, THERMAL_STORAGE, 
                           SOLAR,  WIND, RUN_OF_RIVER, PUMPED_STORAGE, CHP, FOSSIL, 
                           BATTERY_STORAGE, ELECTRIC_VEHICLE };
    enum SpecType { BEHAVIORS, FACTORED };
	
    String capacityId;
    CapacityType capacityType;
    CapacitySubType capacitySubType;
    SpecType specType;
    String description;
    double baseBenchmarkRate;
	
    CapacityProfile(Element xml, Random random) 
    {
        capacityId = xml.getAttribute("id");
        capacityType = Enum.valueOf(CapacityType.class, xml.getAttribute("type"));
        capacitySubType = Enum.valueOf(CapacitySubType.class, xml.getAttribute("subType"));
        specType = Enum.valueOf(SpecType.class, xml.getAttribute("specType"));
        description = xml.getAttribute("description");
        Element baseBenchmarkRateElement = (Element) xml.getElementsByTagName("baseBenchmarkRate").item(0);
        baseBenchmarkRate = Double.parseDouble(baseBenchmarkRateElement.getAttribute("value"));
    }

    PowerType determinePowerType() {
	return reportPowerType(capacityType, capacitySubType);
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
        switch (powerType) {
        case CONSUMPTION: 
        case INTERRUPTIBLE_CONSUMPTION:
        case THERMAL_STORAGE_CONSUMPTION:
            return CapacityType.CONSUMPTION;
        case PRODUCTION:
        case SOLAR_PRODUCTION:
        case WIND_PRODUCTION:
        case RUN_OF_RIVER_PRODUCTION:
        case PUMPED_STORAGE_PRODUCTION:
        case CHP_PRODUCTION:
        case FOSSIL_PRODUCTION:
            return CapacityType.PRODUCTION;
	case BATTERY_STORAGE:
	case ELECTRIC_VEHICLE:
	    return CapacityType.STORAGE;
	default: throw new Error("Unexpected powerType: " + powerType);
	}
    }	
	
    protected double[][] pairsAsDoubleArray(String input) {
        String[] pairs = input.split(",");
        double[][] ret = new double[pairs.length][2];
	for (int i=0; i < pairs.length; ++i) {
	    String[] vals = pairs[i].split(":");
	    ret[i][0] = Double.parseDouble(vals[0]);
	    ret[i][1] = Double.parseDouble(vals[1]);
	}
	return ret;
    }
    
    static SpecType reportSpecType(Element xml)
    {
        return Enum.valueOf(SpecType.class, xml.getAttribute("specType"));
    }
	
} // end class

