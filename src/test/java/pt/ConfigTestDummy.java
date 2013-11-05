/*
 * Copyright (c) 2013 by the original author
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
package pt;

import org.powertac.common.config.ConfigurableValue;

/**
 * Dummy class to test configuration outside the org.powertac hierarchy.
 * @author jcollins
 */
public class ConfigTestDummy
{
  @ConfigurableValue(valueType = "Integer",
          description = "int property")
  private int intProperty = 0;

  @ConfigurableValue(valueType = "String",
          description = "string property")
  public String stringProperty = "dummy";

  @ConfigurableValue(valueType = "Double",
          description = "Fixed cost/kWh")
  private double fixedPerKwh = -0.06;

  public ConfigTestDummy ()
  {
    super();
  }

  public int getIntProperty ()
  {
    return intProperty;
  }

  public double getFixedPerKwh ()
  {
    return fixedPerKwh;
  }
}
