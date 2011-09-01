/*
 * Copyright 2011 the original author or authors.
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

package org.powertac.common;

import java.util.UUID;

/**
 * Generates numeric ID values for domain types.
 * @author John Collins
 */
public class IdGenerator 
{
  private static int prefix = 0; // invalid default value
  private static int counter = 0;
  private static int multiplier = 100000000;
  
  /**
   * Generates a numeric ID as xA+B, where x is the multiplier, A is the
   * prefix, and B is the value of a counter;
   */
  public static long createId() 
  {
    return multiplier * prefix + counter++;
  }
  
  public static void setPrefix (int value)
  {
    prefix = value;
  }
}
