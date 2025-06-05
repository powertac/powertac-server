/*
 * Copyright 2009-2011 the original author or authors.
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
package org.powertac.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A collection of static methods intended to simplify list processing
 * tasks. Much of the content is modeled on web resources.
 * @author John Collins
 */
public class ListTools
{
  /**
   * Filters a Collection using a Predicate, returning a List.
   */
  public static <T> List<T> filter (Collection<T> target, Predicate<T> pred)
  {
    List<T> result = new ArrayList<T>();
    for (T item : target) {
      if (pred.apply(item)) {
        result.add(item);
      }
    }
    return result;
  }
  
  /**
   * Returns the first element of a Collection that satisfies a Predicate.
   */
  public static <T> T findFirst (Collection<T> target, Predicate<T> pred)
  {
    for (T item : target) {
      if (pred.apply(item)) {
        return item;
      }
    }
    return null;
  }
}
