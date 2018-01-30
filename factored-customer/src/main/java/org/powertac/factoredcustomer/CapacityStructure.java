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

import org.powertac.common.config.ConfigurableValue;
import org.powertac.factoredcustomer.interfaces.StructureInstance;

import java.util.List;
import java.util.Map;


/**
 * Data-holder class for parsed configuration elements of one capacity.
 *
 * @author Prashant Reddy, Govert Buijs
 */
public final class CapacityStructure implements StructureInstance
{
  public enum InfluenceKind
  {
    DIRECT, DEVIATION, NONE
  }

  public enum BaseCapacityType
  {
    POPULATION, INDIVIDUAL, TIMESERIES
  }

  public enum ElasticityModelType
  {
    CONTINUOUS, STEPWISE
  }

  private String name;

  @ConfigurableValue(valueType = "String")
  private String baseCapacityType;
  private ProbabilityDistribution basePopulationCapacity;
  private ProbabilityDistribution baseIndividualCapacity;

  // Calendar factors
  @ConfigurableValue(valueType = "List")
  private List<String> dailySkew;
  @ConfigurableValue(valueType = "List")
  private List<String> hourlySkew;

  // Regulation capability
  @ConfigurableValue(description="Lower limit on expected consumption",
      valueType="Double")
  private double upRegulationLimit = Double.MAX_VALUE;
  @ConfigurableValue(description="Upper limit on expected consumption",
      valueType="Double")
  private double downRegulationLimit = -Double.MAX_VALUE;

  // Weather factors
  @ConfigurableValue(valueType = "String")
  private String temperatureInfluence;
  @ConfigurableValue(valueType = "List")
  private List<String> temperatureMap;
  @ConfigurableValue(valueType = "Double")
  private double temperatureReference = Double.NaN;
  @ConfigurableValue(valueType = "String")
  private String windSpeedInfluence;
  @ConfigurableValue(valueType = "List")
  private List<String> windSpeedMap;
  @ConfigurableValue(valueType = "String")
  private String windDirectionInfluence;
  @ConfigurableValue(valueType = "List")
  private List<String> windDirectionMap;
  @ConfigurableValue(valueType = "String")
  private String cloudCoverInfluence;
  @ConfigurableValue(valueType = "List")
  private List<String> cloudCoverMap;

  // Market factors
  @ConfigurableValue(valueType = "List")
  private List<String> benchmarkRates;
  @ConfigurableValue(valueType = "String")
  private String elasticityModelType;
  @ConfigurableValue(valueType = "Double")
  private double elasticityRatio;
  @ConfigurableValue(valueType = "String")
  private String elasticityRange;
  @ConfigurableValue(valueType = "List")
  private List<String> elasticityMap;

  @ConfigurableValue(valueType = "List")
  private List<String> curtailmentShifts;

  public CapacityStructure (String name)
  {
    this.name = name;
  }

  public void initialize (FactoredCustomerService service)
  {
    Map<String, StructureInstance> map =
        Config.getInstance().getStructures().get("ProbabilityDistribution");

    switch (BaseCapacityType.valueOf(baseCapacityType)) {
      case POPULATION:
        basePopulationCapacity = (ProbabilityDistribution)
            map.get(name + "Population");
        if (basePopulationCapacity != null) {
          basePopulationCapacity.initialize(service);
        }
        break;
      case INDIVIDUAL:
        baseIndividualCapacity = (ProbabilityDistribution)
            map.get(name + "Population");
        if (baseIndividualCapacity != null) {
          baseIndividualCapacity.initialize(service);
        }
        break;
      case TIMESERIES:
        break;
      default:
        throw new Error("Unexpected base capacity type: " + baseCapacityType);
    }
  }

  // =================== Accessors ====================

  @Override
  public String getName ()
  {
    return name;
  }

  public BaseCapacityType getBaseCapacityType ()
  {
    return BaseCapacityType.valueOf(baseCapacityType);
  }

  public ProbabilityDistribution getBasePopulationCapacity ()
  {
    return basePopulationCapacity;
  }

  public ProbabilityDistribution getBaseIndividualCapacity ()
  {
    return baseIndividualCapacity;
  }

  public double getUpRegulationLimit ()
  {
    return upRegulationLimit;
  }

  public double getDownRegulationLimit ()
  {
    return downRegulationLimit;
  }

  public double getPeriodicSkew (int day, int hour)
  {
    return Double.parseDouble(dailySkew.get(day - 1)) *
           Double.parseDouble(hourlySkew.get(hour));
  }

  public InfluenceKind getTemperatureInfluence ()
  {
    return InfluenceKind.valueOf(temperatureInfluence);
  }

  public double getTemperatureFactor (int temperature)
  {
    String tmp = ("" + temperatureMap).replace("[", "").replace("]", "");
    return ParserFunctions.parseRangeMap(tmp).get(temperature);
  }

  public double getTemperatureReference ()
  {
    return temperatureReference;
  }

  public InfluenceKind getWindSpeedInfluence ()
  {
    return InfluenceKind.valueOf(windSpeedInfluence);
  }

  public double getWindspeedFactor (int windspeed)
  {
    String tmp = ("" + windSpeedMap).replace("[", "").replace("]", "");
    return ParserFunctions.parseRangeMap(tmp).get(windspeed);
  }

  public InfluenceKind getWindDirectionInfluence ()
  {
    return InfluenceKind.valueOf(windDirectionInfluence);
  }

  public double getWindDirectionFactor (int windDirection)
  {
    String tmp = ("" + windDirectionMap).replace("[", "").replace("]", "");
    return ParserFunctions.parseRangeMap(tmp).get(windDirection);
  }

  public InfluenceKind getCloudCoverInfluence ()
  {
    return InfluenceKind.valueOf(cloudCoverInfluence);
  }

  public double getCloudCoverFactor (int cloudCover)
  {
    String tmp = ("" + cloudCoverMap).replace("[", "").replace("]", "");
    return ParserFunctions.parseRangeMap(tmp).get(cloudCover);
  }

  public double getBenchmarkRate (int hour)
  {
    String tmp = ("" + benchmarkRates).replace("[", "").replace("]", "");
    return ParserFunctions.parseRangeMap(tmp).get(hour);
  }

  public ElasticityModelType getElasticityModelType ()
  {
    return ElasticityModelType.valueOf(elasticityModelType);
  }

  public double determineContinuousElasticityFactor (double rateRatio)
  {
    double percentChange = (rateRatio - 1.0);
    String[] minMax = elasticityRange.split("~");
    double low = Double.parseDouble(minMax[0]);
    double high = Double.parseDouble(minMax[1]);
    return Math.max(low,
        Math.min(high, 1.0 + (percentChange * elasticityRatio)));
  }

  public double[][] getElasticity ()
  {
    String tmp = ("" + elasticityMap).replace("[", "").replace("]", "");
    return ParserFunctions.parseMapToDoubleArray(tmp);
  }

  public List<String> getCurtailmentShifts ()
  {
    return curtailmentShifts;
  }
}


