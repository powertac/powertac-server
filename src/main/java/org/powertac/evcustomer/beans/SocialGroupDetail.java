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

/**
 * @author Govert Buijs
 * @version 0.2, Date: 2013.05.17
 */
public class SocialGroupDetail {
  private int id;
  private double probability;
  private double maleProbability;

  public SocialGroupDetail (int id, double probability, double maleProbability)
  {
    this.id = id;
    this.probability = probability;
    this.maleProbability = maleProbability;
  }

  public int getId () {
    return id;
  }

  public double getProbability () {
    return probability;
  }

  public double getMaleProbability () {
    return maleProbability;
  }
}