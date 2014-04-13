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

/**
 * Example configurable-instance class for configurator test
 * @author John Collins
 */
public class ConfigInstance
{
  public String name;

  @ConfigurableValue(valueType = "Integer", bootstrapState = true)
  int stateProp = 0;

  @ConfigurableValue(valueType = "Integer")
  int simpleProp = 0;

  @ConfigurableValue(valueType = "Integer", bootstrapState = true)
  int sequence = 0;

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

  public void setSequence (int value)
  {
    sequence = value;
  }
}
