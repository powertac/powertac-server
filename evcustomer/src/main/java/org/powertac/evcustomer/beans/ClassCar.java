/*
 * Copyright (c) 2014 by the original author
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
package org.powertac.evcustomer.beans;

import org.powertac.common.config.ConfigurableValue;

/**
 * Join table between EvSocialClass and CarType
 * @author jcollins
 */
public class ClassCar
{
  protected String name;

  @ConfigurableValue(valueType = "String", dump = false,
      description = "Social class name")
  protected String socialClass;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Probability of a member of the class owning the car")
  protected double probability;

  @ConfigurableValue(valueType = "String", dump = false,
      description = "Car type name")
  private String car;

  public ClassCar (String name)
  {
    super();
    this.name = name;
  }

  public String getCarName ()
  {
    return car;
  }

  public String getName ()
  {
    return name;
  }

  public String getSocialClassName ()
  {
    return socialClass;
  }

  public double getProbability ()
  {
    return probability;
  }
}
