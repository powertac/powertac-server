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
package org.powertac.common.config;

import java.util.Arrays;
import java.util.List;

/**
 * Example configurable-instance class for configurator test
 * @author John Collins
 */
public class ConfigInstance
{
  public String name;

  @ConfigurableValue(description = "sample state", valueType = "Integer",
      bootstrapState = true)
  int stateProp = 1;

  @ConfigurableValue(description = "sample prop", valueType = "Integer")
  int simpleProp = 2;

  int sequence = 3;

  @ConfigurableValue(description = "list example", valueType = "List",
      bootstrapState = true)
  List<String> coefficients = Arrays.asList(".1", "2.1");

  @ConfigurableValue(description = "sample factor", valueType = "Double",
      publish = true)
  double factor = 4.5;

  @ConfigurableValue(description = "non-dump", valueType = "Boolean",
      dump = false)
  private boolean booleanProperty = true;


  public ConfigInstance (String name)
  {
    super();
    this.name = name;
  }

  public String getName ()
  {
    return name;
  }

  public int getSequence ()
  {
    return sequence;
  }

  @ConfigurableValue(description = "sample seq", valueType = "Integer",
      bootstrapState = true)
  public void setSequence (int value)
  {
    sequence = value;
  }

  public List<String> getCoefficients ()
  {
    return coefficients;
  }
}
