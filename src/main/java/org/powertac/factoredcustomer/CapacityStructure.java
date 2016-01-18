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

import java.util.HashMap;
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
  private double[] dailySkew;
  private double[] hourlySkew;

  // Weather factors
  @ConfigurableValue(valueType = "String")
  private String temperatureInfluence;
  private Map<Integer, Double> temperatureMap = new HashMap<>();  // key: degree Celsius
  @ConfigurableValue(valueType = "Double")
  private double temperatureReference = Double.NaN;
  @ConfigurableValue(valueType = "String")
  private String windSpeedInfluence;
  private Map<Integer, Double> windSpeedMap = new HashMap<>();  // key: speed in m/s
  @ConfigurableValue(valueType = "String")
  private String windDirectionInfluence;
  private Map<Integer, Double> windDirectionMap = new HashMap<>();  // key: angle 0-360
  @ConfigurableValue(valueType = "String")
  private String cloudCoverInfluence;
  private Map<Integer, Double> cloudCoverMap = new HashMap<>();  // key: 0 (clear) - 100 (cloudy)

  // Market factors
  private Map<Integer, Double> benchmarkRates = new HashMap<>();  // key: hour of day
  @ConfigurableValue(valueType = "String")
  private String elasticityModelType;
  @ConfigurableValue(valueType = "Double")
  private double elasticityRatio;
  private double[] elasticityRange;
  private double[][] elasticityMap;

  private double[] curtailmentShifts;  // index = timeslot

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

  // =================== Setters ======================

  @ConfigurableValue(valueType = "List")
  public void setDailySkew (List<String> data)
  {
    dailySkew = data.stream().mapToDouble(Double::parseDouble).toArray();
  }

  @ConfigurableValue(valueType = "List")
  public void setHourlySkew (List<String> data)
  {
    hourlySkew = data.stream().mapToDouble(Double::parseDouble).toArray();
  }

  @ConfigurableValue(valueType = "List")
  public void setTemperatureMap (List<String> data)
  {
    String tmp = ("" + data).replace("[", "").replace("]", "");
    temperatureMap = ParserFunctions.parseRangeMap(tmp);
  }

  @ConfigurableValue(valueType = "List")
  public void setWindSpeedMap (List<String> data)
  {
    String tmp = ("" + data).replace("[", "").replace("]", "");
    windSpeedMap = ParserFunctions.parseRangeMap(tmp);
  }

  @ConfigurableValue(valueType = "List")
  public void setWindDirectionMap (List<String> data)
  {
    String tmp = ("" + data).replace("[", "").replace("]", "");
    windDirectionMap = ParserFunctions.parseRangeMap(tmp);
  }

  @ConfigurableValue(valueType = "List")
  public void setCloudCoverMap (List<String> data)
  {
    String tmp = ("" + data).replace("[", "").replace("]", "");
    cloudCoverMap = ParserFunctions.parseRangeMap(tmp);
  }

  @ConfigurableValue(valueType = "List")
  public void setBenchmarkRates (List<String> data)
  {
    String tmp = ("" + data).replace("[", "").replace("]", "");
    benchmarkRates = ParserFunctions.parseRangeMap(tmp);
  }

  @ConfigurableValue(valueType = "String")
  public void setElasticityRange (String data)
  {
    String[] minMax = data.split("~");
    double low = Double.parseDouble(minMax[0]);
    double high = Double.parseDouble(minMax[1]);
    elasticityRange = new double[]{low, high};
  }

  @ConfigurableValue(valueType = "List")
  public void setElasticityMap (List<String> data)
  {
    String tmp = ("" + data).replace("[", "").replace("]", "");
    elasticityMap = ParserFunctions.parseMapToDoubleArray(tmp);
  }

  @ConfigurableValue(valueType = "List")
  public void setCurtailmentShifts (List<String> data)
  {
    curtailmentShifts = data.stream().mapToDouble(Double::parseDouble).toArray();
  }

  // =================== Accessors ====================

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

  public double[] getDailySkew ()
  {
    return dailySkew;
  }

  public double[] getHourlySkew ()
  {
    return hourlySkew;
  }

  public InfluenceKind getTemperatureInfluence ()
  {
    return InfluenceKind.valueOf(temperatureInfluence);
  }

  public Map<Integer, Double> getTemperatureMap ()
  {
    return temperatureMap;
  }

  public double getTemperatureReference ()
  {
    return temperatureReference;
  }

  public InfluenceKind getWindSpeedInfluence ()
  {
    return InfluenceKind.valueOf(windSpeedInfluence);
  }

  public Map<Integer, Double> getWindSpeedMap ()
  {
    return windSpeedMap;
  }

  public InfluenceKind getWindDirectionInfluence ()
  {
    return InfluenceKind.valueOf(windDirectionInfluence);
  }

  public Map<Integer, Double> getWindDirectionMap ()
  {
    return windDirectionMap;
  }

  public InfluenceKind getCloudCoverInfluence ()
  {
    return InfluenceKind.valueOf(cloudCoverInfluence);
  }

  public Map<Integer, Double> getCloudCoverMap ()
  {
    return cloudCoverMap;
  }

  public Map<Integer, Double> getBenchmarkRates ()
  {
    return benchmarkRates;
  }

  public ElasticityModelType getElasticityModelType ()
  {
    return ElasticityModelType.valueOf(elasticityModelType);
  }

  public double getElasticityRatio ()
  {
    return elasticityRatio;
  }

  public double[] getElasticityRange ()
  {
    return elasticityRange;
  }

  public double[][] getElasticityMap ()
  {
    return elasticityMap;
  }

  public double[] getCurtailmentShifts ()
  {
    return curtailmentShifts;
  }
}


