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

/**
 * Generates numeric ID values for domain types. ID values are of the
 * form xA+B, where x is the multiplier, A is an integer prefix, and B is the 
 * value of a counter incremented with each call. The use of a prefix allows
 * the server to give each broker a space of ID values and prevent collisions.
 * 
 * @author John Collins
 */
public class IdGenerator 
{
  private static int prefix = 0; // invalid default value
  private static int counter = 0;
  private static int multiplier = 100000000;

  /**
   * Generates a numeric ID as xA+B.
   */
  public static long createId() 
  {
    //return multiplier * prefix + counter++;
    return (long)multiplier * (long)prefix + (long)counter++;
  }

  /**
   * Each entity living in a separate process must have a different prefix
   * value. These values are presumably set by the competition control service.
   */
  public static void setPrefix (int value)
  {
    prefix = value;
  }

  /**
   * Returns the id prefix - needed for testing.
   */
  public static int getPrefix ()
  {
    return prefix;
  }

  /**
   * Converts ID value to String as A.B.   
   */
  public static String getString (long id)
  {
    return Long.toString(id / multiplier) + "." + Long.toString(id % multiplier);
  }

  /**
   * Returns the multiplier for an id, needed to validate id values
   */
  public static int getMultiplier ()
  {
    return multiplier;
  }

  /**
   * Returns the id prefix for the given id.
   */
  public static int extractPrefix (long id)
  {
    return (int)(id / multiplier);
  }

  /**
   * Recycles the generator. This should only be used in a setting where multiple
   * sessions are run in a single process.
   */
  public static void recycle ()
  {
    counter = 0;
  }
}
