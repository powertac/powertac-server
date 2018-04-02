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

import org.powertac.common.config.ConfigurableValue;
import org.powertac.factoredcustomer.interfaces.StructureInstance;

import java.util.Map;


/**
 * Data-holder class for parsed configuration elements of one tariff subscriber,
 * which typically corresponds to one capapcity bundle. Relevant members are
 * declared final in the package scope.
 *
 * @author Prashant Reddy
 */
public final class TariffSubscriberStructure implements StructureInstance
{
  private enum AllocationMethod
  {
    TOTAL_ORDER, LOGIT_CHOICE
  }

  private String name;

  // inconvenience factors - all should be [0..1]
  private double inconvenienceWeight = 0.2;
  private double touFactor = 0.05;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double interruptibilityFactor = 0.5;
  private double variablePricingFactor = 0.7;
  private double tieredRateFactor = 0.1;
  private double tariffSwitchFactor = 0.1;
  private double brokerSwitchFactor = 0.02;
  private int expectedDuration = 14; // expected subscription duration, days

  @ConfigurableValue(valueType = "Double", dump = false)
  private Double expMeanPriceWeight;
  @ConfigurableValue(valueType = "Double", dump = false)
  private Double maxValuePriceWeight;
  @ConfigurableValue(valueType = "Double", dump = false)
  private Double realizedPriceWeight;
  @ConfigurableValue(valueType = "Double", dump = false)
  private Double tariffVolumeThreshold = 20000.0;
  @ConfigurableValue(valueType = "String", dump = false)
  private String allocationMethod;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double logitChoiceRationality;
  @ConfigurableValue(description = "Expected per-timeslot up-regulation (neg)",
          valueType = "Double", dump = false)
  private double expUpRegulation = 0.0;
  @ConfigurableValue(description = "Expected per-timeslot down-regulation (pos)",
          valueType = "Double", dump = false)
  private double expDownRegulation = 0.0;

  private ProbabilityDistribution inertiaDistribution;

  public TariffSubscriberStructure (String name)
  {
    this.name = name;
  }

  public void initialize (FactoredCustomerService service)
  {
    double divisor = touFactor + interruptibilityFactor + variablePricingFactor
        + tieredRateFactor + tariffSwitchFactor + brokerSwitchFactor;
    if (divisor != 0.0) {
      touFactor /= divisor;
      interruptibilityFactor /= divisor;
      variablePricingFactor /= divisor;
      tieredRateFactor /= divisor;
      tariffSwitchFactor /= divisor;
      brokerSwitchFactor /= divisor;
    }

    if (this.allocationMethod.equals(AllocationMethod.TOTAL_ORDER.toString())) {
      logitChoiceRationality = 1.0;
    }

    Map<String, StructureInstance> map =
        Config.getInstance().getStructures().get("ProbabilityDistribution");
    inertiaDistribution = (ProbabilityDistribution) map.get(name + "Inertia");
    if (inertiaDistribution != null) {
      inertiaDistribution.initialize(service);
    }
  }

  // =================== Accessors ====================

  @Override
  public String getName ()
  {
    return name;
  }

  public double getInconvenienceWeight ()
  {
    return inconvenienceWeight;
  }

  public double getTouFactor ()
  {
    return touFactor;
  }

  public double getInterruptibilityFactor ()
  {
    return interruptibilityFactor;
  }

  public double getVariablePricingFactor ()
  {
    return variablePricingFactor;
  }

  public double getTieredRateFactor ()
  {
    return tieredRateFactor;
  }

  public double getTariffSwitchFactor ()
  {
    return tariffSwitchFactor;
  }

  public double getBrokerSwitchFactor ()
  {
    return brokerSwitchFactor;
  }

  public int getExpectedDuration ()
  {
    return expectedDuration;
  }

  public Double getExpMeanPriceWeight ()
  {
    return expMeanPriceWeight;
  }

  public Double getMaxValuePriceWeight ()
  {
    return maxValuePriceWeight;
  }

  public Double getRealizedPriceWeight ()
  {
    return realizedPriceWeight;
  }

  public Double getTariffVolumeThreshold ()
  {
    return tariffVolumeThreshold;
  }

  public double getLogitChoiceRationality ()
  {
    return logitChoiceRationality;
  }

  public ProbabilityDistribution getInertiaDistribution ()
  {
    return inertiaDistribution;
  }

  public double getExpUpRegulation ()
  {
    return this.expUpRegulation;
  }

  public double getExpDownRegulation ()
  {
    return this.expDownRegulation;
  }
}
