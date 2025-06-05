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

import java.util.List;

import org.powertac.common.config.ConfigurableValue;

/**
 * Dummy class to test configuration outside the org.powertac hierarchy.
 * @author jcollins
 */
public class ConfigTestDummy
{
  @ConfigurableValue(valueType = "Integer",
      bootstrapState = true,
      description = "int property")
  private int intProperty = 0;

  @ConfigurableValue(valueType = "String",
      publish = true,
      description = "string property")
  public String stringProperty = "dummy";

  @ConfigurableValue(valueType = "Double",
      publish = true,
      bootstrapState = true,
      description = "Fixed cost/kWh")
  private double fixedPerKwh = -0.06;

  @ConfigurableValue(valueType = "List",
      description = "list type")
  private List<String> listProperty;

  @ConfigurableValue(valueType = "XML", description = "xml test")
  private List<Object> xmlProperty;

  private List<String> secondList;

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

  public List<String> getListProperty ()
  {
    return listProperty;
  }

  public List<String> getSecondList ()
  {
    return secondList;
  }

  @ConfigurableValue(valueType = "List", description = "list setter")
  public void setSecondList (List<String> list)
  {
    secondList = list;
  }

  public List<Object> getXmlProperty ()
  {
    return xmlProperty;
  }
}
