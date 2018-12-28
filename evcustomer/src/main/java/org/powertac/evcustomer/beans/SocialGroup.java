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
 * @author Govert Buijs
 * @version 0.5, Date: 2013.11.08
 */
public class SocialGroup
{
  private String name;

  @ConfigurableValue(valueType = "Integer",
          description = "Group ID", dump = false)
  private int id;

  public SocialGroup (String name)
  {
    super();
    this.name = name;
  }
  
  public SocialGroup (int id, String name)
  {
    this.id = id;
    this.name = name;
  }

  public int getId ()
  {
    return id;
  }

  public String getName ()
  {
    return name;
  }
}