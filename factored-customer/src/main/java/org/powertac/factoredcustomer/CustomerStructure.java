/*
 * Copyright 2011-2018 the original author or authors.
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


/**
 * Data-holder class for parsed configuration elements of one customer.
 *
 * @author Prashant Reddy, John Collins
 */
public final class CustomerStructure implements StructureInstance
{
  private String name;

  @ConfigurableValue(valueType = "String", dump = false)
  private String creatorKey;

  @ConfigurableValue(description = "deprecated, inaccessible",
          valueType = "Integer", dump = false)
  private int count = 1;

  @ConfigurableValue(valueType = "Integer", dump = false)
  private int bundleCount = 1;

  public CustomerStructure (String name)
  {
    this.name = name;
  }

  public String getName ()
  {
    return name;
  }

  public String getCreatorKey ()
  {
    return creatorKey;
  }

  // dead code
  //public int getCount ()
  //{
  //  return count;
  //}

  public int getBundleCount ()
  {
    return bundleCount;
  }
}
