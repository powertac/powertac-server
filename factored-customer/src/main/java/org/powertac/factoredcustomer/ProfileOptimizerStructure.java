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
import org.powertac.factoredcustomer.CapacityProfile.PermutationRule;
import org.powertac.factoredcustomer.interfaces.StructureInstance;


/**
 * Data-holder class for parsed configuration elements that control the
 * capacity profile optimization of a capacity bundle. Relevant members
 * are declared final in the package scope.
 *
 * @author Prashant Reddy
 */
public final class ProfileOptimizerStructure implements StructureInstance
{
  public enum UsageChargeStance
  {
    NEUTRAL, BENEFIT, THRESHOLD
  }

  public enum ProfileSelectionMethod
  {
    BEST_UTILITY, LOGIT_CHOICE
  }

  private String name;

  @ConfigurableValue(valueType = "Boolean", dump = false)
  private boolean receiveRecommendations = false;
  @ConfigurableValue(valueType = "String", dump = false)
  private String permutationRule = null;
  @ConfigurableValue(valueType = "String", dump = false)
  private String profileSelectionMethod =
      ProfileSelectionMethod.BEST_UTILITY.toString();

  // factors controlling responsiveness to recommendation
  @ConfigurableValue(valueType = "Double", dump = false)
  private double reactivityFactor = 1.0;  // [0.0, 1.0]
  @ConfigurableValue(valueType = "Double", dump = false)
  private double receptivityFactor = 1.0;  // [0.0, 1.0]
  @ConfigurableValue(valueType = "Double", dump = false)
  private double rationalityFactor = 1.0;  // [0.0, 1.0]

  // required percent benefit in usage charge vs. forecast profile
  @ConfigurableValue(valueType = "String", dump = false)
  private String usageChargeStance = UsageChargeStance.BENEFIT.toString();
  @ConfigurableValue(valueType = "Double", dump = false)
  private double usageChargePercentBenefit= 0.01;  // 1% improvement
  @ConfigurableValue(valueType = "Double", dump = false)
  private double usageChargeThreshold = Double.NaN;  // [0.0, +inf)

  // scoring weights of other factors relative to fixed usage charge weight of +/-1.
  @ConfigurableValue(valueType = "Double", dump = false)
  private double profileChangeWeight = -1.0;  // (-inf, 0.0]
  @ConfigurableValue(valueType = "Double", dump = false)
  private double bundleValueWeight = +10.0;  //  [0.0, inf]

  public ProfileOptimizerStructure (String name)
  {
    this.name = name;
  }

  @Override
  public String getName ()
  {
    return name;
  }

  public boolean isReceiveRecommendations ()
  {
    return receiveRecommendations;
  }

  public PermutationRule getPermutationRule ()
  {
    return PermutationRule.valueOf(permutationRule);
  }

  public ProfileSelectionMethod getProfileSelectionMethod ()
  {
    return ProfileSelectionMethod.valueOf(profileSelectionMethod);
  }

  public double getReactivityFactor ()
  {
    return reactivityFactor;
  }

  public double getReceptivityFactor ()
  {
    return receptivityFactor;
  }

  public double getRationalityFactor ()
  {
    return rationalityFactor;
  }

  public UsageChargeStance getUsageChargeStance ()
  {
    return UsageChargeStance.valueOf(usageChargeStance);
  }

  public double getUsageChargePercentBenefit ()
  {
    return usageChargePercentBenefit;
  }

  public double getUsageChargeThreshold ()
  {
    return usageChargeThreshold;
  }

  public double getProfileChangeWeight ()
  {
    return profileChangeWeight;
  }

  public double getBundleValueWeight ()
  {
    return bundleValueWeight;
  }
}

