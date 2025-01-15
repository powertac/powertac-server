/*
 * Copyright (c) 2015 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.common;

import java.time.Instant;

/**
 * Represents a customer usage or production profile over time. For population
 * models, the profile should represent the usage or production of a single
 * individual in the population. The time period is defined by the start
 * attribute and the length of the profile array. 
 * 
 * Models may generate profiles in two ways: (a) a "prototypical" profile
 * is independent of time and state, while a (b) "state-dependent" profile
 * is generated from the current state of the model and simulation. This
 * distinction is important, because tariff evaluations based on
 * state-dependent profiles cannot be compared to evaluations of other
 * tariffs based on profiles generated in different states. In other words
 * if a TariffEvaluator is given state-dependent profiles, it must consider
 * all applicable tariffs every time it is called to evaluate tariffs;
 * it cannot use cached evaluations. This can have a significant impact
 * on the performance of the tariff evaluation process.
 * 
 * Note that state-dependence is a customer-specific attribute, not a
 * profile-specific or tariff-specific attribute. Therefore, a customer that
 * submits state-dependent profiles must also set the the evaluateAllTariffs
 * attribute of its TariffEvaluator. 
 * 
 * Instances are immutable.
 * 
 * @author John Collins
 */
public class CapacityProfile
{
  // The usage/production profile. Positive values are usage, negative
  // values represent production.
  private double[] profile;

  // Index of the timeslot at which the profile starts. For state-dependent
  // profiles, this is typically the next timeslot.
  private Instant start;

  /**
   * Constructor requires all attributes
   */
  public CapacityProfile (double[] profile, Instant start)
  {
    super();
    this.profile = profile;
    this.start = start;
  }

  public double[] getProfile ()
  {
    return profile;
  }

  public Instant getStart()
  {
    return start;
  }

}
