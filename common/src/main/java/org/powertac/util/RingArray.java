/**
 * Copyright (c) 2022 by John Collins
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
package org.powertac.util;

import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implements essentially an ArrayList of fixed size in which only the elements from (high-size) to high
 * are kept. So for example, if you want to keep at most 48 elements indexed by timeslot, every time you
 * add an element at some timeslot t, all elements older than t-48 would be discarded. Attempts to retrieve
 * elements outside the current "window" will return null. Attempts to insert elements below the current
 * window will produce an IndexOutOfBoundsException.
 * 
 * @author John Collins
 */
public class RingArray <T>
{
  private Object[] data; // Java does not support generic arrays
  private int size = 0;
  private int maxIndex = 0;

  // Creates a ring of the given size
  public RingArray (int size)
  {
    super();
    this.size = size;
    data = new Object[size];
    clear();
  }

  /**
   * Clears all data out of the array
   */
  public void clear ()
  {
    for (int i = 0; i < size; i++) {
      data[i] = null;
    }
    maxIndex = 0;
  }

  /**
   * Cleans the "unused" portion of the array, from the current maxIndex to the given index
   */
  public void clean (int startIndex)
  {
    for (int i = maxIndex + 1; i < startIndex + size; i++) {
      data[i % size] = null;
    }
  }

  /**
   * Returns the "active length" of the array starting at the specified index
   */
  public int getActiveLength (int startIndex)
  {
    return maxIndex - startIndex + 1;
  }

  /**
   * Sets the element at the given index, potentially overwriting elements with index less than
   * index-size. Attempts to add elements below the current window will fail silently.
   */
  public void set (int index, T element)
  {
    if (index > maxIndex - size) {
      data[index % size] = element;
      maxIndex = Math.max(maxIndex, index);
    }
  }

  /**
   * Returns the element at the given index, or null if index is out of range
   */
  @SuppressWarnings("unchecked")
  public T get (int index)
  {
    if (index > maxIndex || index <= maxIndex - size)
      return null;
    else
      return (T) data[index % size];
  }

  /**
   * Returns the "active" elements of the ring as a simple Array
   */
  @SuppressWarnings("unchecked")
  public T[] getActiveArray (int startIndex)
  {
    int len = getActiveLength(startIndex);
    if (len <= 0)
      return null;
    T[] result = (T[]) new Object[len];
    for (int i = 0; i < len; i++) {
      result[i] = (T) data[(startIndex + i) % size];
    }
    return result;
  }

  /**
   *  Returns the current active elements as an AbstractList. Note that this list is a
   *  snapshot and will no longer be valid after elements are added or removed from the ring.
   */
  public List<T> getActiveList (int start)
  {
    return new RaList<T>(start);
  }

  class RaList <T> extends AbstractList<T>
  {
    private int start = 0;
    private RingArray<T> ring;

    // Constructor takes a snapshot
    @SuppressWarnings("unchecked")
    RaList (int startIndex)
    {
      super();
      ring = (RingArray<T>) RingArray.this;
      this.start = startIndex;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get (int index)
    {
      return (T) ring.get(index + start);
    }

    @Override
    public int size ()
    {
      return ring.maxIndex - start + 1;
    }
    
  }
}
