/*
 * Copyright 2013 the original author or authors.
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

package org.powertac.evcustomer.beans;

import org.powertac.common.config.ConfigurableValue;

/**
 * Join table between EvSocialClass and SocialGroup
 * @author Govert Buijs
 * @version 0.5, Date: 2013.11.08
 */
public class SocialGroupDetail
{
  private String name;
  
  @ConfigurableValue(valueType = "String",
      description = "Foreign key: name of associated EvSocialClass")
  private String socialClassName;

  @ConfigurableValue(valueType = "Integer",
      description = "Foreign key: id of associated SocialGroup")
  private int groupId;

  @ConfigurableValue(valueType = "Double",
      description = "Probability of this association")
  private double probability;

  @ConfigurableValue(valueType = "Double",
      description = "Probability of actor being male")
  private double maleProbability;

  /***
   * Constructor for auto-configuration
   */
  public SocialGroupDetail (String name)
  {
    super();
    this.name = name;
  }
  
  public void initialize (int id, double probability, double maleProbability)
  {
    this.groupId = id;
    this.probability = probability;
    this.maleProbability = maleProbability;
  }

  public String getSocialClassName ()
  {
    return socialClassName;
  }

  public int getGroupId ()
  {
    return groupId;
  }

  public double getProbability ()
  {
    return probability;
  }

  public double getMaleProbability ()
  {
    return maleProbability;
  }
}